# Render (Web Service Docker, plan gratuito):
# La API y PostgreSQL deben estar en la misma cuenta y región. En el Web Service define:
#   SPRING_PROFILES_ACTIVE=prod
#   SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/DB
#   SPRING_DATASOURCE_USERNAME=USER
#   SPRING_DATASOURCE_PASSWORD=PASSWORD
# Usa INTERNAL_DB_URL (red privada de Render), no EXTERNAL_DB_URL.
# Render entrega postgres://USER:PASSWORD@HOST:5432/DB; hay que pasarla a JDBC y separar usuario y clave.
# EXTERNAL_DB_URL sirve para conectar desde tu máquina y suele exigir ?sslmode=require.
# No definas PORT: Render inyecta $PORT y server.port lo lee. Flyway crea el esquema al arrancar.

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN sed -i 's/\r$//' gradlew \
	&& chmod +x gradlew \
	&& ./gradlew bootJar -x test --no-daemon \
	&& find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /workspace/app.jar /app/app.jar
USER spring
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
