FROM python:3.11-slim

WORKDIR /app

COPY bayesianOptimizer.py .
COPY params.json .
COPY requirements.txt .

RUN pip install -r requirements.txt

# Install Docker CLI and dependencies
RUN apt-get update && \
    apt-get install -y curl gnupg lsb-release ca-certificates

RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get install -y docker-ce-cli

# Install Docker Compose v2 plugin
RUN mkdir -p /usr/local/lib/docker/cli-plugins && \
    ARCH=$(uname -m) && \
    curl -SL https://github.com/docker/compose/releases/download/v2.27.1/docker-compose-linux-${ARCH} \
    -o /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

ENV CONFIG_PATH="/app/params.json"

EXPOSE 5000

CMD ["python", "bayesianOptimizer.py"]
