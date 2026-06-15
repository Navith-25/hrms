# Use an official Maven image to build the project directly
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
# direct mvn use karanawa (mvnw nemei)
RUN mvn clean package -DskipTests

# Run the app
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]