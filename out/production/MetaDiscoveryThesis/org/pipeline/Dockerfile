FROM openjdk:8-jdk-slim

# Install necessary tools and libraries for building lp_solve
RUN apt-get update && apt-get install -y \
    wget \
    tar \
    gcc \
    make \
    unzip \
    libfreetype6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

#So file
RUN wget https://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.11/lp_solve_5.5.2.11_dev_ux64.tar.gz \
    && tar -xzf lp_solve_5.5.2.11_dev_ux64.tar.gz \
    && rm lp_solve_5.5.2.11_dev_ux64.tar.gz

#Wrapper
RUN wget https://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.0.15/lp_solve_5.5.0.15_java.zip \
    && unzip lp_solve_5.5.0.15_java.zip \
    && rm lp_solve_5.5.0.15_java.zip


# Copy application files
COPY MetaDiscoveryThesis.jar /app/MetaDiscoveryThesis.jar
COPY exampleLogs /app/exampleLogs
COPY lib /app/lib
COPY libs /app/libs
COPY ivy /app/ivy
COPY LpSolve_mac /app/lpsolve

# Move the shared library to the correct location
RUN mkdir -p /usr/lib/lpsolve \
    && cp /app/lpsolve/mac/liblpsolve55j.jnilib /usr/lib/lpsolve \
    && cp /app/lpsolve/mac/liblpsolve55.jnilib /usr/lib/lpsolve \
    && chmod 755 /usr/lib/lpsolve/liblpsolve55j.jnilib \
    && chmod 755 /usr/lib/lpsolve/liblpsolve55.jnilib


# Set environment variables for lp_solve
ENV LD_LIBRARY_PATH="/usr/lib/lpsolve:$LD_LIBRARY_PATH"
ENV JAVA_LIBRARY_PATH="/usr/lib/lpsolve:$JAVA_LIBRARY_PATH"


# Set the CLASSPATH to include application and lp_solve libraries
ENV CLASSPATH="/app/MetaDiscoveryThesis.jar:/app/ivy/*:/app/lib/*:/app/libs/*:/usr/lib/lpsolve/*"

# Expose application port
EXPOSE 8080

# Set environment variable for event log path
ENV EVENT_LOG_PATH="/app/exampleLogs/Road_Traffic_Fine_Management_Process.xes"

# Start the Java application
CMD ["java", "-cp", "/app/MetaDiscoveryThesis.jar:/app/lib/*:/app/libs/*:/app/ivy/*", "-Djava.library.path=/usr/lib/lpsolve", "org.pipeline.MetaDiscoveryPipeline"]
