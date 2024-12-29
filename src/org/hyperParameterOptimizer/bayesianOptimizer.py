import os

import requests
import optuna
import sys

JAVA_SERVICE_URL = os.environ.get("JAVA_SERVICE_URL", "http://localhost:8080/pipeline")  # Update to match your setup if using Docker


def objective(trial):
    hyper_param_filter = trial.suggest_float("hyperParamFilter", 0.01, 0.2)

    payload = {"hyperParamFilter": hyper_param_filter}

    try:
        response = requests.post(JAVA_SERVICE_URL, json=payload)

        response.raise_for_status()

        result = response.json()
        fitness = result.get("fitness")
        if fitness is None:
            raise ValueError("Invalid response from server: 'fitness' field is missing.")

        return fitness

    except requests.exceptions.RequestException as e:
        print(f"Error communicating with Java server: {e}")
        raise


# Main function to run the optimization
def main():
    # Create an Optuna study
    study = optuna.create_study(direction="maximize")

    # Optimize the objective function
    study.optimize(objective, n_trials=10)

    # Print the best parameters and fitness value
    print("Best hyperparameters:", study.best_params)
    print("Best fitness:", study.best_value)


if __name__ == "__main__":
    main()
