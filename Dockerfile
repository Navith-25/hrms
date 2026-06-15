FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy all project files
COPY . .

# Grant execution permission to maven wrapper and build the project
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Expose the port your app runs on
EXPOSE 8080

# Command to run the application
CMD ["sh", "-c", "java -jar target/*.jar"]