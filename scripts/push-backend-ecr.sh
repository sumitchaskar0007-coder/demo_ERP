#!/usr/bin/env bash
set -euo pipefail

# Builds and pushes an immutable backend image using the caller's AWS identity.
# Required: AWS CLI credentials, Docker, and a Terraform workspace or ECR_REPOSITORY_URI.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AWS_REGION="${AWS_REGION:-$(terraform -chdir="${ROOT_DIR}/infra/terraform" output -raw aws_region 2>/dev/null || true)}"
AWS_REGION="${AWS_REGION:-ap-south-1}"
ECR_REPOSITORY_URI="${ECR_REPOSITORY_URI:-$(terraform -chdir="${ROOT_DIR}/infra/terraform" output -raw ecr_repository_url 2>/dev/null || true)}"
IMAGE_TAG="${IMAGE_TAG:-$(git -C "${ROOT_DIR}" rev-parse --short=12 HEAD)}"

command -v aws >/dev/null || { echo "aws CLI is required" >&2; exit 1; }
command -v docker >/dev/null || { echo "Docker CLI is required" >&2; exit 1; }
[ -n "${ECR_REPOSITORY_URI}" ] || { echo "Set ECR_REPOSITORY_URI or apply Terraform first" >&2; exit 1; }

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="${ECR_REPOSITORY_URI%%/*}"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${REGISTRY}"

IMAGE="${ECR_REPOSITORY_URI}:${IMAGE_TAG}"
docker build --file "${ROOT_DIR}/backend/Dockerfile" --tag "${IMAGE}" "${ROOT_DIR}/backend"
docker push "${IMAGE}"

echo "Pushed ${IMAGE}"
echo "AWS account: ${ACCOUNT_ID}"
echo "Update backend_image to this immutable image before the ECS deployment."
