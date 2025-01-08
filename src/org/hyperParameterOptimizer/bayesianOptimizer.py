import os
import json
import requests
import optuna

JAVA_SERVICE_URL = os.environ.get("JAVA_SERVICE_URL", "http://java-service:8080/pipeline")
CONFIG_PATH = os.environ.get("CONFIG_PATH", "params.json")


def load_config(config_file):
    with open(config_file, "r") as file:
        return json.load(file)


def objective(trial, config):
    algorithms = list(config["algorithms"].keys())
    chosen_algorithm = trial.suggest_categorical("algorithm", algorithms)

    # Get the parameters for the chosen algorithm
    parameters = config["algorithms"][chosen_algorithm]["parameters"]

    # Build the payload
    payload = {"algorithm": chosen_algorithm}  # Include the chosen algorithm in the payload
    hyper_param_filter = trial.suggest_float("hyperParamFilter", 0.01, 0.2)
    payload["hyperParamFilter"] = hyper_param_filter

    # Get variant choices and their metadata
    variants = parameters["variant"]["choices"]
    variant_names = [variant["name"] for variant in variants]
    chosen_variant = trial.suggest_categorical("variant", variant_names)

    # Add the chosen variant to the payload
    payload["variant"] = chosen_variant

    # Check if the chosen variant requires a noiseThreshold
    for variant in variants:
        if variant["name"] == chosen_variant:
            if variant.get("requiresNoiseThreshold", False):
                payload["noiseThreshold"] = trial.suggest_float(
                    "noiseThreshold",
                    parameters["noiseThreshold"]["low"],
                    parameters["noiseThreshold"]["high"]
                )
            break

    # Add other hyperparameters dynamically
    for param_name, param_details in parameters.items():
        if param_name == "variant":  # Skip as it's already handled
            continue
        if param_name == "noiseThreshold":  # Skip as it's conditionally handled
            continue

        param_type = param_details["type"]
        if param_type == "float":
            payload[param_name] = trial.suggest_float(param_name, param_details["low"], param_details["high"])
        elif param_type == "int":
            payload[param_name] = trial.suggest_int(param_name, param_details["low"], param_details["high"])
        elif param_type == "categorical":
            payload[param_name] = trial.suggest_categorical(param_name, param_details["choices"])

    try:
        # Send the payload to the Java service
        response = requests.post(JAVA_SERVICE_URL, json=payload)
        response.raise_for_status()
        result = response.json()

        # Validate and return fitness
        fitness = result.get("fitness")
        if fitness is None:
            raise ValueError("Invalid response from server: 'fitness' field is missing.")
        return fitness

    except requests.exceptions.RequestException as e:
        print(f"Error communicating with Java server: {e}")
        raise


def main():
    # Load algorithm and parameter configuration
    config = load_config(CONFIG_PATH)

    # Create an Optuna study
    study = optuna.create_study(direction="maximize")

    # Optimize the objective function
    study.optimize(lambda trial: objective(trial, config), n_trials=1000)

    # Print the best parameters and fitness value
    print("Best algorithm and hyperparameters:", study.best_params)
    print("Best fitness:", study.best_value)


if __name__ == "__main__":
    main()
