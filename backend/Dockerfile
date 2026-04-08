# 1. Change the Build Stage to use JDK 21
FROM gradle:7.6.3-jdk21 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew build -x test

# 2. Ensure your Run Stage also uses Java 21 (as we discussed before)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Adjust this path based on your actual JAR name
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]