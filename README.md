# Meta Discovery Algorithm

There are two ways to set up and run the Meta Discovery Algorithm:

## Option 1: Prebuilt Docker Images

1. Copy the provided `.env` and `docker-compose.yml` files to your working directory.
2. Open a terminal in the directory where these files are located.
3. Start the containers by running:
   ```bash
   docker compose up -d
   ```
   This command will pull the latest built images from the Docker repository.

## Option 2: Build the Project Yourself
(might require some additonal setup depending on the OS)

1.Build the project using Apache Ant:
   ```bash
   ant buildHudson
   ant build-docker
   ```
2.Navigate to the `docker` directory:
   ```bash
   cd docker
   ```
3.Start the containers:
   ```bash
   docker compose up -d
   ```

## Configuration

The `.env` file contains important environment variables that you need to adjust according to your local setup before starting the containers.

## Access the Optuna Dashboard

After the containers are up and running, you can access the Optuna dashboard by running:
```bash
optuna-dashboard postgresql+psycopg2://postgres:postgres@localhost:5432
```
This command will open the dashboard connected to the PostgreSQL database. From there you can follow the results from the study.

---

