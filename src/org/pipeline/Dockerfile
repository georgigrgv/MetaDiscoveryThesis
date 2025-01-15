FROM openjdk:8-jdk-slim

# Install necessary tools and libraries for building lp_solve
RUN apt-get update && apt-get install -y \
    wget \
    tar \
    gcc \
    make \
    unzip \
    g++ \
    libfreetype6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY MetaDiscoveryThesis.jar /app/MetaDiscoveryThesis.jar
COPY lib /app/lib
COPY libs /app/libs
COPY ivy /app/ivy
COPY LpSolve_mac /app/lpsolve

WORKDIR /app

RUN mkdir -p /usr/lib/lpsolve \
    && cp /app/lpsolve/mac/liblpsolve55.so /usr/local/lib \
    && cp /app/lpsolve/mac/liblpsolve55j.so /usr/local/lib \
    && chmod 755 /usr/local/lib/liblpsolve55.so

ENV LD_LIBRARY_PATH="/usr/local/lib:$LD_LIBRARY_PATH"
ENV JAVA_LIBRARY_PATH="/usr/local/lib:$JAVA_LIBRARY_PATH"

ENV CLASSPATH="/app/MetaDiscoveryThesis.jar:/app/ivy/*:/app/lib/*:/app/libs/*"

EXPOSE 8080 5005

ENV JAVA_OPTS_DEFAULT="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

#CMD ["java","-Xms512m", "-Xmx4G", "-cp", "/app/MetaDiscoveryThesis.jar:/app/lib/*:/app/libs/*:/app/ivy/*", "-Djava.library.path=/usr/local/lib", "org.pipeline.MetaDiscoveryPipeline"]
#Can be used for debug mode but doesnt work?
CMD sh -c "java -Xms512m -Xmx4G ${JAVA_OPTS:-} -cp /app/MetaDiscoveryThesis.jar:/app/lib/*:/app/libs/*:/app/ivy/* -Djava.library.path=/usr/local/lib org.pipeline.MetaDiscoveryPipeline"
