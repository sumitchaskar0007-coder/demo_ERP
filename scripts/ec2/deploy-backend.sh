#!/usr/bin/env bash
set -euo pipefail

# Runs on the production EC2 instance through SSM. The JSON configuration
# contains resource identifiers only; secret values are fetched on the host
# and written to root-only tmpfs files under /run.

if [ "${EUID}" -ne 0 ]; then
  echo "deploy-backend.sh must run as root" >&2
  exit 1
fi

CONFIG_FILE="${1:-}"
if [ -z "${CONFIG_FILE}" ] || [ ! -f "${CONFIG_FILE}" ]; then
  echo "Usage: deploy-backend.sh /path/to/deployment.json" >&2
  exit 1
fi

for command_name in aws docker flock jq; do
  command -v "${command_name}" >/dev/null || {
    echo "Required command is missing: ${command_name}" >&2
    exit 1
  }
done

export AWS_RETRY_MODE=standard
export AWS_MAX_ATTEMPTS=10
export AWS_PAGER=""

config_string() {
  jq -er --arg key "$1" \
    '.[$key] | select(type == "string" and length > 0)' \
    "${CONFIG_FILE}"
}

config_optional_string() {
  jq -er --arg key "$1" \
    '.[$key] // "" | select(type == "string")' \
    "${CONFIG_FILE}"
}

AWS_REGION="$(config_string aws_region)"
IMAGE_URI="$(config_string image_uri)"
APP_RELEASE="$(config_string release)"
APPLICATION_SECRET_ID="$(config_string application_secret_id)"
MAIL_SECRET_ID="$(config_optional_string mail_secret_id)"
RUNTIME_DATABASE_SECRET_ID="$(config_string runtime_database_secret_id)"
DB_ENDPOINT="$(config_string db_endpoint)"
DB_PORT="$(jq -er '.db_port | tostring | select(test("^[0-9]+$"))' "${CONFIG_FILE}")"
DB_NAME="$(config_string db_name)"
DOCUMENTS_BUCKET="$(config_string documents_bucket)"
FRONTEND_URL="$(config_string frontend_url)"
CORS_ALLOWED_ORIGINS="$(config_string cors_allowed_origins)"
REDIS_IMAGE="$(config_string redis_image)"

if [[ ! "${DB_ENDPOINT}" =~ ^[A-Za-z0-9.-]+$ ]]; then
  echo "db_endpoint must be a hostname without a scheme or port" >&2
  exit 1
fi
if [ "${DB_PORT}" != "5432" ]; then
  echo "Production database port must be 5432" >&2
  exit 1
fi
if [[ ! "${DB_NAME}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  echo "db_name is not a valid PostgreSQL identifier" >&2
  exit 1
fi
if [[ ! "${APP_RELEASE}" =~ ^[a-f0-9]{12}$ ]]; then
  echo "release must be the 12-character Git commit tag" >&2
  exit 1
fi
if [[ ! "${FRONTEND_URL}" =~ ^https:// ]]; then
  echo "frontend_url must use HTTPS" >&2
  exit 1
fi
if [[ ! "${REDIS_IMAGE}" =~ ^(redis|docker\.io/library/redis)@sha256:[a-f0-9]{64}$ ]]; then
  echo "redis_image must be the official Redis image pinned to an immutable sha256 digest" >&2
  exit 1
fi

PROJECT_NAME="jadhavr-erp"
LOG_GROUP="/ec2/${PROJECT_NAME}-production-ec2"
RUN_DIR="/run/${PROJECT_NAME}"
BACKEND_ENV="${RUN_DIR}/backend.env"
BACKEND_ENV_BACKUP="${RUN_DIR}/backend.previous.env"
BACKEND_SECRETS_DIR="${RUN_DIR}/backend-secrets"
BACKEND_SECRETS_BACKUP_DIR="${RUN_DIR}/backend-secrets-previous"
REDIS_SECRETS_DIR="${RUN_DIR}/redis-secrets"
REDIS_CONFIG="${REDIS_SECRETS_DIR}/redis.conf"
REDIS_CONFIG_BACKUP="${RUN_DIR}/redis.previous.conf"
NETWORK_NAME="jadhavr-runtime"
REDIS_CONTAINER="jadhavr-redis"
PREVIOUS_REDIS_CONTAINER="jadhavr-redis-previous"
BACKEND_CONTAINER="jadhavr-backend"
PREVIOUS_CONTAINER="jadhavr-backend-previous"
REDIS_VOLUME="jadhavr-redis-data"

umask 077
install -d -m 0700 "${RUN_DIR}"
exec 9>"${RUN_DIR}/deploy.lock"
if ! flock -n 9; then
  echo "Another Jadhavr ERP deployment is already running" >&2
  exit 1
fi

AWS_ACCOUNT_ID="$(
  aws sts get-caller-identity \
    --region "${AWS_REGION}" \
    --query Account \
    --output text
)"
if [[ ! "${AWS_ACCOUNT_ID}" =~ ^[0-9]{12}$ ]]; then
  echo "Unable to determine the 12-digit AWS account ID" >&2
  exit 1
fi
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
if [[ "${IMAGE_URI}" != "${ECR_REGISTRY}/"* ]]; then
  echo "image_uri must use the current account and region ECR registry" >&2
  exit 1
fi
backend_image_reference="${IMAGE_URI#"${ECR_REGISTRY}/"}"
if [[ ! "${backend_image_reference}" =~ ^jadhavr-erp-production-backend@sha256:[a-f0-9]{64}$ ]]; then
  echo "image_uri must use the production backend ECR repository and an immutable sha256 digest" >&2
  exit 1
fi

docker_pull_with_retry() {
  local image="$1"
  local attempt
  local delay=2
  for attempt in $(seq 1 5); do
    if docker pull "${image}" >/dev/null; then
      return 0
    fi
    if [ "${attempt}" -lt 5 ]; then
      echo "Docker pull failed for ${image}; retrying (${attempt}/5)" >&2
      sleep "${delay}"
      delay=$((delay * 2))
    fi
  done
  echo "Docker pull failed for ${image} after 5 attempts" >&2
  return 1
}

wait_for_container_health() {
  local container="$1"
  local attempts="$2"
  local interval="$3"
  local status
  local attempt
  for attempt in $(seq 1 "${attempts}"); do
    status="$(
      docker inspect \
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
        "${container}" 2>/dev/null ||
        true
    )"
    if [ "${status}" = "healthy" ]; then
      return 0
    fi
    if [ "${status}" = "exited" ] || [ "${status}" = "dead" ]; then
      return 1
    fi
    sleep "${interval}"
  done
  return 1
}

application_secret_json="$(
  aws secretsmanager get-secret-value \
    --region "${AWS_REGION}" \
    --secret-id "${APPLICATION_SECRET_ID}" \
    --query SecretString \
    --output text
)"
database_secret_json="$(
  aws secretsmanager get-secret-value \
    --region "${AWS_REGION}" \
    --secret-id "${RUNTIME_DATABASE_SECRET_ID}" \
    --query SecretString \
    --output text
)"
mail_secret_json=""
if [ -n "${MAIL_SECRET_ID}" ]; then
  mail_secret_json="$(
    aws secretsmanager get-secret-value \
      --region "${AWS_REGION}" \
      --secret-id "${MAIL_SECRET_ID}" \
      --query SecretString \
      --output text
  )"
fi

secret_value() {
  local secret_json="$1"
  local key="$2"
  printf '%s' "${secret_json}" |
    jq -er --arg key "${key}" \
      '.[$key] | select(type == "string" and length > 0)'
}

secret_optional_value() {
  local secret_json="$1"
  local key="$2"
  printf '%s' "${secret_json}" |
    jq -er --arg key "${key}" \
      '.[$key] // "" | select(type == "string")'
}

JWT_SECRET="$(secret_value "${application_secret_json}" JWT_SECRET)"
RATE_LIMIT_KEY_SECRET="$(secret_value "${application_secret_json}" RATE_LIMIT_KEY_SECRET)"
REDIS_PASSWORD="$(secret_value "${application_secret_json}" REDIS_PASSWORD)"
DB_USERNAME="$(secret_value "${database_secret_json}" username)"
DB_PASSWORD="$(secret_value "${database_secret_json}" password)"
MAIL_ENABLED=false
if [ -n "${mail_secret_json}" ]; then
  MAIL_HOST="$(secret_value "${mail_secret_json}" host)"
  MAIL_PORT="$(secret_value "${mail_secret_json}" port)"
  MAIL_USERNAME="$(secret_value "${mail_secret_json}" username)"
  MAIL_PASSWORD="$(secret_value "${mail_secret_json}" password)"
  MAIL_FROM_ADDRESS="$(secret_value "${mail_secret_json}" from_address)"
  MAIL_REPLY_TO="$(secret_value "${mail_secret_json}" reply_to)"
  MAIL_FROM_NAME="$(secret_optional_value "${mail_secret_json}" from_name)"
  MAIL_CONFIGURATION_SET="$(secret_optional_value "${mail_secret_json}" configuration_set)"
  if [ -z "${MAIL_FROM_NAME}" ]; then MAIL_FROM_NAME="College ERP Institute"; fi
  if [ "${MAIL_PORT}" != "587" ]; then
    echo "Production SMTP port must be 587 with STARTTLS" >&2
    exit 1
  fi
  if [[ ! "${MAIL_HOST}" =~ ^[A-Za-z0-9.-]+$ ]] ||
    [[ ! "${MAIL_FROM_ADDRESS}" =~ ^[^[:space:]@]+@[^[:space:]@]+$ ]] ||
    [[ ! "${MAIL_REPLY_TO}" =~ ^[^[:space:]@]+@[^[:space:]@]+$ ]]; then
    echo "Production mail secret contains an invalid host or email address" >&2
    exit 1
  fi
  if [ -n "${MAIL_CONFIGURATION_SET}" ] &&
    [[ ! "${MAIL_CONFIGURATION_SET}" =~ ^[A-Za-z0-9_-]{1,64}$ ]]; then
    echo "Production SES configuration set name is invalid" >&2
    exit 1
  fi
  MAIL_ENABLED=true
fi

if [ "${#JWT_SECRET}" -lt 32 ] || [ "${#RATE_LIMIT_KEY_SECRET}" -lt 32 ] ||
  [ "${#REDIS_PASSWORD}" -lt 16 ]; then
  echo "Application secrets do not meet the production length requirements" >&2
  exit 1
fi

reject_line_break() {
  local name="$1"
  local value="$2"
  if [[ "${value}" == *$'\n'* || "${value}" == *$'\r'* ]]; then
    echo "${name} must not contain line breaks" >&2
    exit 1
  fi
}

for secret_name in JWT_SECRET RATE_LIMIT_KEY_SECRET REDIS_PASSWORD DB_USERNAME DB_PASSWORD; do
  reject_line_break "${secret_name}" "${!secret_name}"
done
if [ "${MAIL_ENABLED}" = "true" ]; then
  for secret_name in MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_FROM_ADDRESS MAIL_REPLY_TO MAIL_FROM_NAME MAIL_CONFIGURATION_SET; do
    reject_line_break "${secret_name}" "${!secret_name}"
  done
fi
if [[ ! "${REDIS_PASSWORD}" =~ ^[A-Za-z0-9+/=_-]+$ ]]; then
  echo "REDIS_PASSWORD must use a shell- and Redis-safe random character set" >&2
  exit 1
fi

backend_env_tmp="$(mktemp "${RUN_DIR}/backend.env.XXXXXX")"
backend_secrets_tmp="$(mktemp -d "${RUN_DIR}/backend-secrets.XXXXXX")"
redis_config_tmp=""
deployment_committed=false
cleanup() {
  local exit_status=$?
  trap - EXIT
  if [ "${exit_status}" -ne 0 ] &&
    [ "${deployment_committed:-false}" != "true" ] &&
    declare -F restore_backend_material >/dev/null; then
    restore_backend_material || true
  fi
  if [ -n "${backend_env_tmp:-}" ]; then rm -f "${backend_env_tmp}"; fi
  if [ -n "${redis_config_tmp:-}" ]; then rm -f "${redis_config_tmp}"; fi
  if [ -n "${backend_secrets_tmp:-}" ]; then rm -rf "${backend_secrets_tmp}"; fi
  if [ "${redis_config_backup_available:-false}" = "true" ]; then
    rm -f "${REDIS_CONFIG_BACKUP}"
  fi
  if [ "${backend_material_backup_available:-false}" = "true" ]; then
    rm -f "${BACKEND_ENV_BACKUP}"
    rm -rf "${BACKEND_SECRETS_BACKUP_DIR}"
  fi
  if [ "${ecr_logged_in:-false}" = "true" ]; then
    docker logout "${ECR_REGISTRY}" >/dev/null 2>&1 || true
  fi
  unset application_secret_json database_secret_json mail_secret_json
  unset JWT_SECRET RATE_LIMIT_KEY_SECRET REDIS_PASSWORD DB_USERNAME DB_PASSWORD
  unset MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_FROM_ADDRESS MAIL_REPLY_TO
  unset MAIL_FROM_NAME MAIL_CONFIGURATION_SET
  exit "${exit_status}"
}
trap cleanup EXIT

backend_material_backup_available=false
rm -f "${BACKEND_ENV_BACKUP}"
rm -rf "${BACKEND_SECRETS_BACKUP_DIR}"
if [ -f "${BACKEND_ENV}" ] && [ -d "${BACKEND_SECRETS_DIR}" ]; then
  backend_material_backup_available=true
  cp -p "${BACKEND_ENV}" "${BACKEND_ENV_BACKUP}"
  cp -a "${BACKEND_SECRETS_DIR}" "${BACKEND_SECRETS_BACKUP_DIR}"
fi

restore_backend_material() {
  if [ "${backend_material_backup_available}" != "true" ]; then
    return 0
  fi
  mv -f "${BACKEND_ENV_BACKUP}" "${BACKEND_ENV}"
  rm -rf "${BACKEND_SECRETS_DIR}"
  mv "${BACKEND_SECRETS_BACKUP_DIR}" "${BACKEND_SECRETS_DIR}"
  backend_material_backup_available=false
}

{
  printf 'SPRING_PROFILES_ACTIVE=production\n'
  printf 'APP_RELEASE=%s\n' "${APP_RELEASE}"
  printf 'SERVER_PORT=8081\n'
  printf 'DB_URL=jdbc:postgresql://%s:%s/%s?sslmode=verify-full\n' \
    "${DB_ENDPOINT}" "${DB_PORT}" "${DB_NAME}"
  printf 'DB_SSL_ROOT_CERT=/etc/ssl/certs/rds-ca-bundle.pem\n'
  printf 'SPRING_CONFIG_IMPORT=configtree:/run/secrets/\n'
  printf 'FLYWAY_ENABLED=false\n'
  printf 'BOOTSTRAP_ENABLED=false\n'
  printf 'REDIS_HOST=%s\n' "${REDIS_CONTAINER}"
  printf 'REDIS_PORT=6379\n'
  printf 'REDIS_SSL_ENABLED=false\n'
  printf 'RATE_LIMIT_REDIS_ENABLED=true\n'
  printf 'RATE_LIMIT_REQUIRED=true\n'
  printf 'AUTHORIZATION_CACHE_ENABLED=true\n'
  printf 'NOTICE_REDIS_ENABLED=true\n'
  # This EC2 path exposes the API ALB directly, so a client-supplied
  # CloudFront-Viewer-Address header must never be trusted.
  printf 'CLOUDFRONT_VIEWER_ADDRESS_ENABLED=false\n'
  printf 'CACHE_ENVIRONMENT=production\n'
  printf 'AWS_REGION=%s\n' "${AWS_REGION}"
  printf 'AWS_PRIVATE_UPLOAD_BUCKET=%s\n' "${DOCUMENTS_BUCKET}"
  printf 'AWS_SECRETS_NAME=%s\n' "${APPLICATION_SECRET_ID}"
  printf 'FRONTEND_URL=%s\n' "${FRONTEND_URL}"
  printf 'CORS_ALLOWED_ORIGINS=%s\n' "${CORS_ALLOWED_ORIGINS}"
  printf 'MAIL_ENABLED=%s\n' "${MAIL_ENABLED}"
  printf 'MAIL_HEALTH_ENABLED=%s\n' "${MAIL_ENABLED}"
  printf 'AUTH_COOKIE_SAME_SITE=Strict\n'
  printf 'TZ=UTC\n'
} >"${backend_env_tmp}"

printf '%s' "${DB_USERNAME}" >"${backend_secrets_tmp}/DB_USERNAME"
printf '%s' "${DB_PASSWORD}" >"${backend_secrets_tmp}/DB_PASSWORD"
printf '%s' "${JWT_SECRET}" >"${backend_secrets_tmp}/JWT_SECRET"
printf '%s' "${RATE_LIMIT_KEY_SECRET}" >"${backend_secrets_tmp}/RATE_LIMIT_KEY_SECRET"
printf '%s' "${REDIS_PASSWORD}" >"${backend_secrets_tmp}/REDIS_PASSWORD"
if [ "${MAIL_ENABLED}" = "true" ]; then
  printf '%s' "${MAIL_HOST}" >"${backend_secrets_tmp}/MAIL_HOST"
  printf '%s' "${MAIL_PORT}" >"${backend_secrets_tmp}/MAIL_PORT"
  printf '%s' "${MAIL_USERNAME}" >"${backend_secrets_tmp}/MAIL_USERNAME"
  printf '%s' "${MAIL_PASSWORD}" >"${backend_secrets_tmp}/MAIL_PASSWORD"
  printf '%s' "${MAIL_FROM_ADDRESS}" >"${backend_secrets_tmp}/MAIL_FROM_ADDRESS"
  printf '%s' "${MAIL_REPLY_TO}" >"${backend_secrets_tmp}/MAIL_REPLY_TO"
  printf '%s' "${MAIL_FROM_NAME}" >"${backend_secrets_tmp}/MAIL_FROM_NAME"
  if [ -n "${MAIL_CONFIGURATION_SET}" ]; then
    printf '%s' "${MAIL_CONFIGURATION_SET}" >"${backend_secrets_tmp}/MAIL_CONFIGURATION_SET"
  fi
fi

chmod 0600 "${backend_env_tmp}"
chown root:root "${backend_env_tmp}"
chmod 0400 "${backend_secrets_tmp}"/*
chown -R 10001:10001 "${backend_secrets_tmp}"
chmod 0500 "${backend_secrets_tmp}"
mv -f "${backend_env_tmp}" "${BACKEND_ENV}"
rm -rf "${BACKEND_SECRETS_DIR}"
mv "${backend_secrets_tmp}" "${BACKEND_SECRETS_DIR}"
backend_env_tmp=""
backend_secrets_tmp=""

docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1 ||
  docker network create "${NETWORK_NAME}" >/dev/null
docker volume inspect "${REDIS_VOLUME}" >/dev/null 2>&1 ||
  docker volume create "${REDIS_VOLUME}" >/dev/null

docker_pull_with_retry "${REDIS_IMAGE}"
redis_uid="$(
  docker run --rm --network none --entrypoint sh "${REDIS_IMAGE}" -ec 'id -u redis'
)"
redis_gid="$(
  docker run --rm --network none --entrypoint sh "${REDIS_IMAGE}" -ec 'id -g redis'
)"
docker rm -f "${PREVIOUS_REDIS_CONTAINER}" >/dev/null 2>&1 || true
had_previous_redis=false
redis_config_backup_available=false
if docker container inspect "${REDIS_CONTAINER}" >/dev/null 2>&1; then
  had_previous_redis=true
  previous_redis_user="$(
    docker inspect --format '{{.Config.User}}' "${REDIS_CONTAINER}"
  )"
  if [ "${previous_redis_user}" != "${redis_uid}:${redis_gid}" ]; then
    echo "Redis image UID/GID changed; refusing an unsafe in-place volume ownership change" >&2
    exit 1
  fi
  if [ -f "${REDIS_CONFIG}" ]; then
    cp -p "${REDIS_CONFIG}" "${REDIS_CONFIG_BACKUP}"
    redis_config_backup_available=true
  fi
fi
docker run --rm \
  --network none \
  --mount "type=volume,source=${REDIS_VOLUME},target=/data" \
  "${REDIS_IMAGE}" \
  sh -ec 'chown -R redis:redis /data'

install -d -m 0500 -o "${redis_uid}" -g "${redis_gid}" "${REDIS_SECRETS_DIR}"
redis_config_tmp="$(mktemp "${RUN_DIR}/redis.conf.XXXXXX")"
{
  printf 'appendonly yes\n'
  printf 'appendfsync everysec\n'
  printf 'requirepass %s\n' "${REDIS_PASSWORD}"
} >"${redis_config_tmp}"
chmod 0400 "${redis_config_tmp}"
chown "${redis_uid}:${redis_gid}" "${redis_config_tmp}"
mv -f "${redis_config_tmp}" "${REDIS_CONFIG}"
redis_config_tmp=""

if [ "${had_previous_redis}" = "true" ]; then
  if ! docker stop --time 30 "${REDIS_CONTAINER}" >/dev/null; then
    echo "The existing Redis container could not be stopped" >&2
    exit 1
  fi
  if ! docker rename "${REDIS_CONTAINER}" "${PREVIOUS_REDIS_CONTAINER}"; then
    docker start "${REDIS_CONTAINER}" >/dev/null 2>&1 || true
    echo "The existing Redis container could not be staged for replacement" >&2
    exit 1
  fi
fi

restore_previous_redis() {
  docker rm -f "${REDIS_CONTAINER}" >/dev/null 2>&1 || true
  if [ "${had_previous_redis}" != "true" ]; then
    echo "The Redis candidate failed; no previous Redis container was available" >&2
    return 1
  fi
  if [ "${redis_config_backup_available}" = "true" ]; then
    mv -f "${REDIS_CONFIG_BACKUP}" "${REDIS_CONFIG}"
    redis_config_backup_available=false
  fi
  if ! docker rename "${PREVIOUS_REDIS_CONTAINER}" "${REDIS_CONTAINER}"; then
    echo "The Redis candidate failed and the previous container could not be renamed" >&2
    return 1
  fi
  if ! docker start "${REDIS_CONTAINER}" >/dev/null; then
    echo "The Redis candidate failed and the previous container could not be started" >&2
    return 1
  fi
  if ! wait_for_container_health "${REDIS_CONTAINER}" 30 2; then
    docker logs --tail 100 "${REDIS_CONTAINER}" >&2 || true
    echo "The Redis candidate failed and the previous container did not recover" >&2
    return 1
  fi
  echo "The Redis candidate failed; the previous healthy container was restored" >&2
}

if ! docker run -d \
  --name "${REDIS_CONTAINER}" \
  --restart unless-stopped \
  --network "${NETWORK_NAME}" \
  --mount "type=volume,source=${REDIS_VOLUME},target=/data" \
  --mount "type=bind,source=${REDIS_CONFIG},target=/run/secrets/redis.conf,readonly" \
  --user "${redis_uid}:${redis_gid}" \
  --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,nodev,size=32m \
  --cap-drop ALL \
  --security-opt no-new-privileges:true \
  --memory 512m \
  --cpus 0.5 \
  --pids-limit 256 \
  --log-driver awslogs \
  --log-opt "awslogs-region=${AWS_REGION}" \
  --log-opt "awslogs-group=${LOG_GROUP}" \
  --log-opt "awslogs-stream=redis" \
  --log-opt awslogs-create-group=false \
  --log-opt mode=non-blocking \
  --log-opt max-buffer-size=4m \
  --health-cmd='REDISCLI_AUTH="$(awk "/^requirepass /{print \$2}" /run/secrets/redis.conf)" redis-cli ping | grep -q PONG' \
  --health-interval=10s \
  --health-timeout=3s \
  --health-retries=6 \
  "${REDIS_IMAGE}" \
  redis-server /run/secrets/redis.conf \
  >/dev/null; then
  restore_previous_redis || true
  restore_backend_material
  exit 1
fi

if ! wait_for_container_health "${REDIS_CONTAINER}" 30 2; then
  docker logs --tail 100 "${REDIS_CONTAINER}" >&2 || true
  echo "Redis did not become healthy" >&2
  restore_previous_redis || true
  restore_backend_material
  exit 1
fi

ecr_logged_in=false
if ! aws ecr get-login-password --region "${AWS_REGION}" |
  docker login --username AWS --password-stdin "${ECR_REGISTRY}" >/dev/null; then
  restore_previous_redis || true
  restore_backend_material
  exit 1
fi
ecr_logged_in=true
if ! docker_pull_with_retry "${IMAGE_URI}"; then
  restore_previous_redis || true
  restore_backend_material
  exit 1
fi
if [ "${ecr_logged_in}" = "true" ]; then
  docker logout "${ECR_REGISTRY}" >/dev/null
  ecr_logged_in=false
fi
docker rm -f "${PREVIOUS_CONTAINER}" >/dev/null 2>&1 || true
had_previous=false
if docker container inspect "${BACKEND_CONTAINER}" >/dev/null 2>&1; then
  had_previous=true
  if ! docker stop --time 45 "${BACKEND_CONTAINER}" >/dev/null; then
    restore_previous_redis || true
    echo "The existing backend container could not be stopped" >&2
    exit 1
  fi
  if ! docker rename "${BACKEND_CONTAINER}" "${PREVIOUS_CONTAINER}"; then
    docker start "${BACKEND_CONTAINER}" >/dev/null 2>&1 || true
    restore_previous_redis || true
    echo "The existing backend container could not be staged for replacement" >&2
    exit 1
  fi
fi

restore_previous() {
  docker rm -f "${BACKEND_CONTAINER}" >/dev/null 2>&1 || true
  if [ "${had_previous}" != "true" ]; then
    echo "The new release failed readiness; no previous release was available" >&2
    return 1
  fi
  restore_backend_material
  if ! docker rename "${PREVIOUS_CONTAINER}" "${BACKEND_CONTAINER}"; then
    echo "The new release failed and the previous container could not be renamed" >&2
    return 1
  fi
  if ! docker start "${BACKEND_CONTAINER}" >/dev/null; then
    echo "The new release failed and the previous container could not be started" >&2
    return 1
  fi
  if ! wait_for_container_health "${BACKEND_CONTAINER}" 48 5; then
    docker logs --tail 100 "${BACKEND_CONTAINER}" >&2 || true
    echo "The new release failed and the previous container did not recover" >&2
    return 1
  fi
  echo "The new release failed readiness; the previous healthy container was restored" >&2
}

if ! docker run -d \
  --name "${BACKEND_CONTAINER}" \
  --restart unless-stopped \
  --init \
  --network "${NETWORK_NAME}" \
  --env-file "${BACKEND_ENV}" \
  --mount "type=bind,source=${BACKEND_SECRETS_DIR},target=/run/secrets,readonly" \
  --publish 8081:8081 \
  --read-only \
  --tmpfs /app/tmp:rw,noexec,nosuid,nodev,size=64m,uid=10001,gid=10001 \
  --user 10001:10001 \
  --cap-drop ALL \
  --security-opt no-new-privileges:true \
  --memory 2g \
  --cpus 1 \
  --pids-limit 512 \
  --stop-timeout 45 \
  --log-driver awslogs \
  --log-opt "awslogs-region=${AWS_REGION}" \
  --log-opt "awslogs-group=${LOG_GROUP}" \
  --log-opt "awslogs-stream=backend" \
  --log-opt awslogs-create-group=false \
  --log-opt mode=non-blocking \
  --log-opt max-buffer-size=4m \
  "${IMAGE_URI}" \
  >/dev/null; then
  restore_previous_redis || true
  restore_previous || true
  exit 1
fi

if ! wait_for_container_health "${BACKEND_CONTAINER}" 48 5; then
  docker logs --tail 100 "${BACKEND_CONTAINER}" >&2 || true
  restore_previous_redis || true
  restore_previous || true
  exit 1
fi

deployment_committed=true
docker rm -f "${PREVIOUS_CONTAINER}" >/dev/null 2>&1 || true
docker rm -f "${PREVIOUS_REDIS_CONTAINER}" >/dev/null 2>&1 || true

install -d -m 0750 /opt/jadhavr-erp/bin /etc/jadhavr-erp
if [ "$(readlink -f "$0")" != "/opt/jadhavr-erp/bin/deploy-backend.sh" ]; then
  install -m 0700 "$0" /opt/jadhavr-erp/bin/deploy-backend.sh
fi
if [ "$(readlink -f "${CONFIG_FILE}")" != "/etc/jadhavr-erp/deployment.json" ]; then
  install -m 0600 "${CONFIG_FILE}" /etc/jadhavr-erp/deployment.json
fi
unit_tmp="$(mktemp /etc/systemd/system/jadhavr-erp.service.XXXXXX)"
{
  printf '[Unit]\n'
  printf 'Description=Jadhavr ERP backend containers\n'
  printf 'Wants=network-online.target\n'
  printf 'After=network-online.target docker.service\n'
  printf 'Requires=docker.service\n\n'
  printf '[Service]\n'
  printf 'Type=oneshot\n'
  printf 'ExecStart=/opt/jadhavr-erp/bin/deploy-backend.sh /etc/jadhavr-erp/deployment.json\n'
  printf 'RemainAfterExit=yes\n'
  printf 'Restart=on-failure\n'
  printf 'RestartSec=30s\n'
  printf 'TimeoutStartSec=900\n\n'
  printf '[Install]\n'
  printf 'WantedBy=multi-user.target\n'
} >"${unit_tmp}"
chmod 0644 "${unit_tmp}"
mv -f "${unit_tmp}" /etc/systemd/system/jadhavr-erp.service
systemctl daemon-reload
systemctl enable jadhavr-erp.service >/dev/null

echo "Deployed ${IMAGE_URI}; readiness is healthy"
