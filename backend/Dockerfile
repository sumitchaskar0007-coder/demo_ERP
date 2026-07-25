# syntax=docker/dockerfile:1.7

ARG JAVA_VERSION=17

FROM maven:3.9.11-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests clean package

FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

LABEL org.opencontainers.image.title="Jadhavar ERP Backend" \
      org.opencontainers.image.description="Spring Boot backend for Jadhavar ERP"

RUN apt-get update \
    && apt-get install --no-install-recommends -y ca-certificates tzdata wget \
    && groupadd --gid 10001 erp \
    && useradd --uid 10001 --gid erp --no-create-home --shell /usr/sbin/nologin erp \
    && mkdir -p /app/uploads /app/tmp \
    && chown -R erp:erp /app \
    && wget -q https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
        -O /etc/ssl/certs/rds-ca-bundle.pem \
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
