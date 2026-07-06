FROM eclipse-temurin:22-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline --no-transfer-progress || true

COPY src src
RUN ./mvnw clean package -DskipTests --no-transfer-progress

FROM eclipse-temurin:22-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=prod -Dserver.port=${PORT:-8080} -jar app.jar"]
