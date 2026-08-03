# ---------------------------------------------------------------------------
# Build stage — compiles only the :backend module.
# A minimal settings file is generated so the Android/KMP modules (which need
# the Android SDK) are never configured inside the container.
# Build from the REPOSITORY ROOT:  docker build -f backend/Dockerfile .
# ---------------------------------------------------------------------------
FROM gradle:8.13-jdk17 AS build
WORKDIR /home/gradle/src

COPY gradle ./gradle
COPY gradle.properties ./gradle.properties
COPY backend ./backend
RUN printf '%s\n' \
    'pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }' \
    'rootProject.name = "RecipeApp"' \
    'dependencyResolutionManagement { repositories { mavenCentral() } }' \
    'include(":backend")' \
    > settings.gradle.kts

RUN gradle :backend:buildFatJar --no-daemon

# ---------------------------------------------------------------------------
# Runtime stage — small JRE image, non-root user.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home appuser
USER appuser
WORKDIR /app

COPY --from=build /home/gradle/src/backend/build/libs/*-all.jar app.jar
# Migrations as plain files — Flyway classpath scanning inside fat jars is
# unreliable, so FLYWAY_LOCATIONS points here (see docker-compose.yml).
COPY backend/src/main/resources/db/migration /app/db/migration

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
    CMD curl -sf http://localhost:8080/healthz || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
