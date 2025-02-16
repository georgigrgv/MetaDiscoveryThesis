import os
import json
import random
import string

import requests
import optuna
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("Logger")

JAVA_SERVICE_URL = os.environ.get("JAVA_SERVICE_URL", "http://java-service:8081/pipeline")
CONFIG_PATH = os.environ.get("CONFIG_PATH", "params.json")
DB_URL = os.environ.get("DB_URL", "postgresql+psycopg2://postgres:postgres@db:5432")


def load_config(config_file):
    with open(config_file, "r") as file:
        return json.load(file)


def objective(trial, config):
    # Load algorithm and variant options
    algorithm_variant_choices = config["algorithm_variants"]
    chosen_key = trial.suggest_categorical("algorithm_variants", algorithm_variant_choices)

    chosen_combination = algorithm_variant_choices[chosen_key]

    # Extract algorithm and variant
    chosen_algorithm = chosen_combination["algorithm"]
    chosen_variant = chosen_combination["variant"]

    # Initialize payload
    payload = {
        "algorithm": chosen_algorithm,
        "variant": chosen_variant,
        "hyperParamFilter": trial.suggest_float(
            "hyperParamFilter",
            config["parameters"]["hyperParamFilter"]["low"],
            config["parameters"]["hyperParamFilter"]["high"]
        )
    }

    for param, param_config in config["parameters"].items():
        # Use the exact parameter name (camelCase) for the requires key
        requires_key = f"requires{param[:1].upper()}{param[1:]}"
        if chosen_combination.get(requires_key, False):  # Check if this parameter is required
            if param_config["type"] == "float":
                payload[param] = trial.suggest_float(param, param_config["low"], param_config["high"])
            elif param_config["type"] == "categorical":
                payload[param] = trial.suggest_categorical(param, param_config["choices"])

    try:
        response = requests.post(JAVA_SERVICE_URL, json=payload)
        response.raise_for_status()
        result = response.json()

        fitness = result.get("fitness", -1.0)
        precision = result.get("precision", -1.0)
        f1_score = result.get("f1-score", -1.0)
        simplicity = result.get("simplicity", -1.0)
        generalization = result.get("generalization", -1.0)

        if fitness == -1.0:
            logger.warning(f"Trial {trial.number}: Fitness could not be computed. Defaulting to -1.0.")
            trial.set_user_attr("status", "Alignments/Fitness could not be computed")
            raise optuna.TrialPruned()
        if precision == -1.0:
            logger.warning(f"Trial {trial.number}: Precision could not be computed. Defaulting to -1.0.")
            trial.set_user_attr("status", "Alignments/Precision could not be computed")
            raise optuna.TrialPruned()

        return fitness, precision, f1_score, simplicity, generalization

    except requests.exceptions.RequestException as e:
        logger.error(f"Error communicating with Java server: {e}")
        raise


def main():
    config = load_config(CONFIG_PATH)

    resume_study = os.environ.get("OPTUNA_STUDY_NAME",
                                  ''.join(random.choices(string.ascii_letters + string.digits, k=8)))

    study = optuna.create_study(
        storage=DB_URL,
        study_name=resume_study,
        directions=["maximize", "maximize", "maximize", "maximize", "maximize"],
        load_if_exists=True
    )

    study.set_metric_names(["Fitness", "Precision", "F1-Score", "Simplicity", "Generalization"])

    study.optimize(lambda trial: objective(trial, config), n_trials=100)

    logger.info("Best algorithm and hyperparameters:")
    for trial in study.best_trials:
        logger.info(f"Trial {trial.number}: {trial.values}, Params: {trial.params}")


if __name__ == "__main__":
    main()
