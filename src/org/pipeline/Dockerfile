FROM openjdk:8-jdk-slim

WORKDIR /app

COPY MetaDiscoveryThesis.jar /app/MetaDiscoveryThesis.jar
COPY exampleLogs /app/exampleLogs
COPY lib /app/lib
COPY libs /app/libs
COPY ivy /app/ivy

ENV CLASSPATH="/app/MetaDiscoveryThesis.jar:/app/ivy/*:/app/lib/*:/app/libs/*"

EXPOSE 8080

ENV EVENT_LOG_PATH="/app/exampleLogs/Road_Traffic_Fine_Management_Process.xes"

CMD ["java", "-cp", "/app/MetaDiscoveryThesis.jar:/app/lib/*:/app/libs/*:/app/ivy/*", "org.pipeline.MetaDiscoveryPipeline"]