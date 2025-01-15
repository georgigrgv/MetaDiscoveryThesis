FROM python:3.11-slim

WORKDIR /app

COPY bayesianOptimizer.py .
COPY params.json .
COPY requirements.txt .

RUN pip install -r requirements.txt

ENV CONFIG_PATH="/app/params.json"

EXPOSE 5000

CMD ["python", "bayesianOptimizer.py"]
