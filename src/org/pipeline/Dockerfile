# Use an OpenJDK base image
FROM openjdk:8-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the compiled Java program JAR and dependencies
COPY MetaDiscoveryThesis.jar /app/MetaDiscoveryThesis.jar
COPY exampleLogs /app/exampleLogs

# Expose the Java service's port
EXPOSE 8080

# Set environment variable for event log
ENV EVENT_LOG_PATH=/src/org/pipeline/exampleLogs/Road_Traffic_Fine_Management_Process.xes

# Command to start the Java service
CMD ["java", "-jar", "MetaDiscoveryThesis.jar"]
