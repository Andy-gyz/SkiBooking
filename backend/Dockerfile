FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S snowalpine && adduser -S snowalpine -G snowalpine
WORKDIR /app
COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar
USER snowalpine
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
