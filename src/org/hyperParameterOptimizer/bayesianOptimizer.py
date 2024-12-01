import requests
import optuna
import sys

# Define the Java service URL
JAVA_SERVICE_URL = "http://localhost:8080/pipeline"  # Update to match your setup if using Docker


# Define the objective function for Optuna
def objective(trial):
    # Suggest a value for the hyperparameter
    hyper_param_filter = trial.suggest_float("hyperParamFilter", 0.01, 0.2)

    # Create the JSON payload to send to the Java server
    payload = {"hyperParamFilter": hyper_param_filter}

    try:
        # Send the request to the Java server
        response = requests.post(JAVA_SERVICE_URL, json=payload)

        # Raise an exception for non-200 responses
        response.raise_for_status()

        # Parse the fitness value from the response JSON
        result = response.json()
        fitness = result.get("fitness")
        if fitness is None:
            raise ValueError("Invalid response from server: 'fitness' field is missing.")

        # Return the fitness value to Optuna
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
