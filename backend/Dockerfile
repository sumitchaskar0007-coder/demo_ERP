# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17@sha256:e4a7ace3dc0d645ed97f8d9ad0b0d3f0b14fa8d150138f27f116d7105a639b82 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true clean package

FROM eclipse-temurin:17-jre-jammy@sha256:89e68b9bb83713510b63e2059a415792a7fc77e14b739a7d7ede97f6d9ca2c38 AS runtime

ARG RDS_CA_SHA256=e5bb2084ccf45087bda1c9bffdea0eb15ee67f0b91646106e466714f9de3c7e3

LABEL org.opencontainers.image.title="College ERP Backend" \
      org.opencontainers.image.description="Spring Boot backend for College ERP"

RUN apt-get update \
    && apt-get install --no-install-recommends -y ca-certificates tzdata wget \
    && groupadd --gid 10001 erp \
    && useradd --uid 10001 --gid erp --no-create-home --shell /usr/sbin/nologin erp \
    && mkdir -p /app/uploads /app/tmp \
    && chown -R erp:erp /app \
    && wget -q https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
        -O /etc/ssl/certs/rds-ca-bundle.pem \
    && echo "${RDS_CA_SHA256}  /etc/ssl/certs/rds-ca-bundle.pem" | sha256sum -c - \
    && chmod 0444 /etc/ssl/certs/rds-ca-bundle.pem \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/college-erp-*.jar /app/application.jar

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=UTC \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/app/tmp -Djava.security.egd=file:/dev/urandom"

USER 10001:10001
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/readiness || exit 1

STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
