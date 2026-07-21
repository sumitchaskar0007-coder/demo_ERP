FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget \
    && addgroup -S erp && adduser -S -G erp -u 10001 erp
RUN wget -q https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -O /etc/ssl/certs/rds-ca-bundle.pem \
    && chmod 0444 /etc/ssl/certs/rds-ca-bundle.pem
WORKDIR /app
COPY --from=build --chown=erp:erp /workspace/target/college-erp-*.jar /app/application.jar
USER 10001:10001
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/application.jar"]
