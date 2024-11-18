import subprocess
import optuna
import sys
import os
import numpy as np

# Ensure the working directory is the Java project's root
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Get the log file path from Java
if len(sys.argv) < 2:
    print("Usage: python3 optuna_pipeline.py <logPath>")
    sys.exit(1)

log_path = sys.argv[1]  # The log file path passed by the Java program


# Define the objective function for Optuna
def objective(trial):
    # Suggest a value for the hyperparameter
    hyper_param_filter = trial.suggest_float("hyperParamFilter", 0.01, 0.2)

    # Command to call the Java pipeline function
    java_command = [
        "java",
        "-classpath",
        "/Users/georgegeorgiev/Desktop/MetaDiscoveryThesis/src/dist/MetaDiscoveryThesis.jar:/Users/georgegeorgiev"
        "/Desktop/MetaDiscoveryThesis/src/lib/*:/Users/georgegeorgiev/Desktop/MetaDiscoveryThesis/src/libs/*:/Users"
        "/georgegeorgiev/Desktop/MetaDiscoveryThesis/src/ivy/*",
        "/Users/georgegeorgiev/Desktop/MetaDiscoveryThesis/src/org/pipeline/MetaDiscoveryPipeline.java",
        log_path,
        str(hyper_param_filter)
    ]

    # Run the Java pipeline
    try:
        result = subprocess.run(
            java_command,
            capture_output=True,
            text=True,
            cwd=project_root  # Set the working directory to the project root
        )

        # Check for errors
        if result.returncode != 0:
            raise RuntimeError(f"Java process failed: {result.stderr}")

        # Parse the output as a float
        fitness = float(result.stdout.strip())
        return fitness

    except Exception as e:
        print(f"Error during Java pipeline execution: {e}")
        raise


# Run the optimization using Optuna
def main():
    print("heio")
    study = optuna.create_study(direction="maximize")
    study.optimize(objective, n_trials=1)

    # Print the best parameters
    print("Best hyperparameters:", study.best_params)


if __name__ == "__main__":
    main()
