# Use a Python base image
FROM python:3.11-slim

# Set the working directory
WORKDIR /app

# Copy the Python script, configuration, and requirements
COPY bayesianOptimizer.py .
COPY params.json .
COPY requirements.txt .

# Install dependencies
RUN pip install -r requirements.txt

# Set an environment variable for the config file path
ENV CONFIG_PATH="/app/params.json"

# Expose any needed port (if required for debugging, not necessary here)
EXPOSE 5000

# Command to start the Python script
CMD ["python", "bayesianOptimizer.py"]
