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
    # Load the precomputed static list of algorithm-variant combinations
    algorithm_variant_choices = config["algorithm_variant"]

    # Suggest a combination of algorithm and variant
    chosen_combination = trial.suggest_categorical("algorithm_variant", algorithm_variant_choices)

    # Extract the algorithm and variant from the chosen combination
    chosen_algorithm = chosen_combination["algorithm"]
    chosen_variant = chosen_combination["variant"]

    # Initialize the payload
    payload = {
        "algorithm": chosen_algorithm,
        "variant": chosen_variant,
        "hyperParamFilter": trial.suggest_float(
            "hyperParamFilter",
            config["parameters"]["hyperParamFilter"]["low"],
            config["parameters"]["hyperParamFilter"]["high"]
        )
    }

    # Add noiseThreshold if required by the chosen variant
    if chosen_combination.get("requiresNoiseThreshold", False):
        payload["noiseThreshold"] = trial.suggest_float(
            "noiseThreshold",
            config["parameters"]["noiseThreshold"]["low"],
            config["parameters"]["noiseThreshold"]["high"]
        )

    # Add AlphaR-specific parameters if required
    if chosen_combination.get("requiresCausalThreshold", False):
        payload["causalThreshold"] = trial.suggest_float(
            "causalThreshold",
            config["parameters"]["causalThreshold"]["low"],
            config["parameters"]["causalThreshold"]["high"]
        )

    if chosen_combination.get("requiresNoiseThresholdLeastFreq", False):
        payload["noiseThresholdLeastFreq"] = trial.suggest_float(
            "noiseThresholdLeastFreq",
            config["parameters"]["noiseThresholdLeastFreq"]["low"],
            config["parameters"]["noiseThresholdLeastFreq"]["high"]
        )

    if chosen_combination.get("requiresNoiseThresholdMostFreq", False):
        payload["noiseThresholdMostFreq"] = trial.suggest_float(
            "noiseThresholdMostFreq",
            config["parameters"]["noiseThresholdMostFreq"]["low"],
            config["parameters"]["noiseThresholdMostFreq"]["high"]
        )

        # Add HeuristicsMiner-specific parameters if required
    if chosen_combination.get("requiresRelativeToBest", False):
        payload["relativeToBest"] = trial.suggest_float(
            "relativeToBest",
            config["parameters"]["relativeToBest"]["low"],
            config["parameters"]["relativeToBest"]["high"]
        )
    if chosen_combination.get("requiresDependencyThreshold", False):
        payload["dependencyThreshold"] = trial.suggest_float(
            "dependencyThreshold",
            config["parameters"]["dependencyThreshold"]["low"],
            config["parameters"]["dependencyThreshold"]["high"]
        )
    if chosen_combination.get("requiresLengthOneLoopsThreshold", False):
        payload["lengthOneLoopsThreshold"] = trial.suggest_float(
            "lengthOneLoopsThreshold",
            config["parameters"]["lengthOneLoopsThreshold"]["low"],
            config["parameters"]["lengthOneLoopsThreshold"]["high"]
        )
    if chosen_combination.get("requiresLengthTwoLoopsThreshold", False):
        payload["lengthTwoLoopsThreshold"] = trial.suggest_float(
            "lengthTwoLoopsThreshold",
            config["parameters"]["lengthTwoLoopsThreshold"]["low"],
            config["parameters"]["lengthTwoLoopsThreshold"]["high"]
        )
    if chosen_combination.get("requiresLongDistanceThreshold", False):
        payload["longDistanceThreshold"] = trial.suggest_float(
            "longDistanceThreshold",
            config["parameters"]["longDistanceThreshold"]["low"],
            config["parameters"]["longDistanceThreshold"]["high"]
        )

    # Add HeuristicsMiner-specific heuristics (checkboxes)
    if chosen_combination.get("requiresAllTasksConnected", False):
        payload["allTasksConnected"] = trial.suggest_categorical(
            "allTasksConnected", [True, False]
        )
    if chosen_combination.get("requiresLongDistanceDependency", False):
        payload["longDistanceDependency"] = trial.suggest_categorical(
            "longDistanceDependency", [True, False]
        )
    if chosen_combination.get("requiresIgnoreLoopDependencyThresholds", False):
        payload["ignoreLoopDependencyThresholds"] = trial.suggest_categorical(
            "ignoreLoopDependencyThresholds", [True, False]
        )

    # Add SplitMiner-specific heuristics (checkboxes)
    if chosen_combination.get("requiresEta", False):
        payload["eta"] = trial.suggest_float(
            "eta",
            config["parameters"]["eta"]["low"],
            config["parameters"]["eta"]["high"]
        )
    if chosen_combination.get("requiresEpsilon", False):
        payload["epsilon"] = trial.suggest_float(
            "epsilon",
            config["parameters"]["epsilon"]["low"],
            config["parameters"]["epsilon"]["high"]
        )
    if chosen_combination.get("requiresParallelismFirst", False):
        payload["parallelismFirst"] = trial.suggest_categorical(
            "parallelismFirst", [True, False]
        )
    if chosen_combination.get("requiresReplaceIORs", False):
        payload["replaceIORs"] = trial.suggest_categorical(
            "replaceIORs", [True, False]
        )
    if chosen_combination.get("requiresRemoveLoopActivities", False):
        payload["removeLoopActivities"] = trial.suggest_categorical(
            "removeLoopActivities", [True, False]
        )

    try:
        # Send the payload to the Java service
        response = requests.post(JAVA_SERVICE_URL, json=payload)
        response.raise_for_status()
        result = response.json()

        # Validate and return fitness
        fitness = result.get("fitness")
        if fitness is None:
            raise ValueError("Invalid response from server: 'fitness' field is missing.")
        precision = result.get("precision")
        if precision is None:
            raise ValueError("Invalid response from server: 'fitness' field is missing.")
        return fitness, precision

    except requests.exceptions.RequestException as e:
        print(f"Error communicating with Java server: {e}")
        raise


def main():
    config = load_config(CONFIG_PATH)

    study = optuna.create_study(storage="postgresql+psycopg2://postgres:postgres@db:5432", directions=["maximize", "maximize"])

    study.optimize(lambda trial: objective(trial, config), n_trials=15)

    print("Best algorithm and hyperparameters:", study.best_trials)

    # remove later used for tests now
    while True:
        print("Check results:")


if __name__ == "__main__":
    main()
