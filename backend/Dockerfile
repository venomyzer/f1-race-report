# 1. Build Stage - Use Gradle 8.5 to match Spring Boot 3.2.5
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy only the gradle files first to leverage Docker caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy the rest of the source code
COPY src src

# Build the application
# We use the system's gradle (8.5) instead of the wrapper if the wrapper is corrupted
RUN gradle build -x test --no-daemon

# 2. Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# This copies the executable jar from the build stage
# Note: Spring Boot 3.2+ often creates a "plain" jar and a "boot" jar.
# We want the one that isn't "-plain.jar"
COPY --from=build /app/build/libs/*[!plain].jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", \
  "-Dspring.datasource.url=${DB_URL}", \
  "-Dspring.datasource.username=${DB_USERNAME}", \
  "-Dspring.datasource.password=${DB_PASSWORD}", \
  "-Dgroq.api.key=${GROQ_API_KEY}", \
  "-Dserver.port=${PORT:-8080}", \
  "-jar", "app.jar"]