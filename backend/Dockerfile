# --- Build stage ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Warm the dependency cache before copying sources so application changes do
# not invalidate downloaded dependencies.
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --create-home --uid 10001 appuser
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
RUN mkdir -p /app/data && chown -R appuser:appuser /app

USER appuser
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=local
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
