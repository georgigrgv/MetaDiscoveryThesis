import os
import json
import random
import string

import requests
import optuna
import logging
from itertools import permutations

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

    chosen_algorithm = chosen_combination["algorithm"]
    variant_param_name = chosen_combination.get("variantParameterName")

    # If algorithm has a variant param (e.g., alphaVariant), choose one
    if variant_param_name:
        chosen_variant = trial.suggest_categorical(
            variant_param_name,
            config["parameters"][variant_param_name]["choices"]
        )
    else:
        chosen_variant = chosen_combination["variant"]

    # Preprocessing combination logic
    preprocessing_keys = list(config["preprocessing_variants"].keys())
    preprocessing_combo_choices = []
    for r in range(1, len(preprocessing_keys)):
        for permutation in permutations(preprocessing_keys, r):
            preprocessing_combo_choices.append("+".join(permutation))

    chosen_combo_key = trial.suggest_categorical("preprocessing_combo", preprocessing_combo_choices)
    chosen_combo = chosen_combo_key.split("+")

    preprocessing_methods = []
    # Collect required parameters
    required_params = []

    for key in chosen_combo:
        method_conf = config["preprocessing_variants"][key]
        preprocessing_methods.append(method_conf["method"])

        for param, param_conf in config["parameters"].items():
            requires_key = f"requires{param[0].upper()}{param[1:]}"
            if method_conf.get(requires_key, False):
                required_params.append(param)

    # Initialize payload
    payload = {
        "algorithm": chosen_algorithm,
        "variant": chosen_variant,
        "preprocessing": preprocessing_methods
    }

    # Check for general "requires*" flags
    for param, param_config in config["parameters"].items():
        requires_key = f"requires{param[:1].upper()}{param[1:]}"
        if chosen_combination.get(requires_key, False):
            required_params.append(param)

    # Include variant-specific parameters if defined
    variant_requirements = config.get("variantRequirements", {})
    variant_specific = variant_requirements.get(chosen_algorithm, {}).get(chosen_variant, [])
    required_params.extend(variant_specific)

    # Sample only required parameters
    for param in required_params:
        param_config = config["parameters"][param]
        if param_config["type"] == "float":
            payload[param] = trial.suggest_float(param, param_config["low"], param_config["high"])
        elif param_config["type"] == "categorical":
            payload[param] = trial.suggest_categorical(param, param_config["choices"])
        elif param_config["type"] == "integer":
            payload[param] = trial.suggest_int(param, param_config["low"], param_config["high"])

    try:
        response = requests.post(JAVA_SERVICE_URL, json=payload)
        response.raise_for_status()
        result = response.json()

        error = result.get("error", "")

        precision_plugin = result.get("precisionPlugin", "")

        if len(error) != 0:
            trial.set_user_attr("status", error)
            raise optuna.TrialPruned()

        fitness = result.get("fitness", -1.0)
        precision = result.get("precision", -1.0)
        f1_score = result.get("f1-score", -1.0)
        simplicity = result.get("simplicity", -1.0)
        generalization = result.get("generalization", -1.0)

        # Only fitness and simpilcity
        if fitness != -1.0 and simplicity != -1.0 and precision == -1.0:
            status = f"Only Fitness and Simplicity available | "f"Fitness: {fitness:.4f}, Simplicity: {simplicity:.4f}"
            trial.set_user_attr("status", status)
            raise optuna.TrialPruned()
        if fitness == -1.0:
            logger.warning(f"Trial {trial.number}: Fitness could not be computed. Defaulting to -1.0.")
            trial.set_user_attr("status", "Alignments/Fitness could not be computed")
            raise optuna.TrialPruned()
        if precision == -1.0:
            logger.warning(f"Trial {trial.number}: Precision could not be computed. Defaulting to -1.0.")
            trial.set_user_attr("status", "Alignments/Precision could not be computed")
            raise optuna.TrialPruned()
        if simplicity == -1.0:
            logger.warning(f"Trial {trial.number}: Simplicity could not be computed. Defaulting to -1.0.")
            trial.set_user_attr("status", "Simplicity could not be computed")
            raise optuna.TrialPruned()

        trial.set_user_attr("status", f"Precision plugin used: {precision_plugin}")
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

    study.optimize(lambda trial: objective(trial, config), n_trials=1000)

    logger.info("Best algorithm and hyperparameters:")
    for trial in study.best_trials:
        logger.info(f"Trial {trial.number}: {trial.values}, Params: {trial.params}")


if __name__ == "__main__":
    main()
