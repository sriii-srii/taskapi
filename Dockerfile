# Use an OpenJDK base from GHCR instead of Docker Hub
FROM ghcr.io/adoptium/temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy the executable JAR built by Maven
COPY target/taskapi-1.0-SNAPSHOT.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java","-jar","app.jar"]
