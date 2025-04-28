import os
import json
import random
import string
import time
import logging
import subprocess
from itertools import permutations

import requests
import optuna

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("OptunaLogger")

# Environment variables
JAVA_SERVICE_URL = os.environ.get("JAVA_SERVICE_URL", "http://java-service:8081/pipeline")
CONFIG_PATH = os.environ.get("CONFIG_PATH", "params.json")
DB_URL = os.environ.get("DB_URL", "postgresql+psycopg2://postgres:postgres@db:5432")


def restart_container(container_name="docker-java-service-1"):
    logger.info(f"Restarting container: {container_name}")
    subprocess.run(["docker", "restart", container_name], check=True)


def wait_for_container_healthy(container_name, timeout=60):
    logger.info(f"Waiting for {container_name} to become healthy...")
    for _ in range(timeout):
        result = subprocess.run(
            ["docker", "inspect", "-f", "{{.State.Health.Status}}", container_name],
            capture_output=True,
            text=True
        )
        if result.returncode == 0 and result.stdout.strip() == "healthy":
            logger.info(f"{container_name} is healthy.")
            return True
        time.sleep(1)
    raise TimeoutError(f"{container_name} did not become healthy in time.")


def load_config(config_file):
    with open(config_file, "r") as file:
        return json.load(file)


def sample_required_parameters(trial, required_params, config, payload):
    for param in required_params:
        param_config = config["parameters"][param]
        if param_config["type"] == "float":
            payload[param] = trial.suggest_float(param, param_config["low"], param_config["high"])
        elif param_config["type"] == "categorical":
            payload[param] = trial.suggest_categorical(param, param_config["choices"])
        elif param_config["type"] == "integer":
            payload[param] = trial.suggest_int(param, param_config["low"], param_config["high"])


def objective(trial, config):
    algorithm_variant_choices = config["algorithm_variants"]
    chosen_key = trial.suggest_categorical("algorithm_variants", list(algorithm_variant_choices.keys()))
    chosen_combination = algorithm_variant_choices[chosen_key]

    chosen_algorithm = chosen_combination["algorithm"]
    variant_param_name = chosen_combination.get("variantParameterName")

    if variant_param_name:
        chosen_variant = trial.suggest_categorical(
            variant_param_name,
            config["parameters"][variant_param_name]["choices"]
        )
    else:
        chosen_variant = chosen_combination.get("variant", None)

    # Preprocessing selection
    preprocessing_keys = list(config["preprocessing_variants"].keys())
    preprocessing_combo_choices = ["+".join(p) for r in range(1, len(preprocessing_keys) - 1)
                                   for p in permutations(preprocessing_keys, r)
                                   ]
    chosen_combo_key = trial.suggest_categorical("preprocessing_combo", preprocessing_combo_choices)
    chosen_combo = chosen_combo_key.split("+")

    preprocessing_methods = [config["preprocessing_variants"][key]["method"] for key in chosen_combo]

    # Build payload
    payload = {
        "algorithm": chosen_algorithm,
        "variant": chosen_variant,
        "preprocessing": preprocessing_methods,
        "trial": trial.number
    }

    required_params = []
    for key in chosen_combo:
        method_conf = config["preprocessing_variants"][key]
        for param, param_conf in config["parameters"].items():
            requires_key = f"requires{param[0].upper()}{param[1:]}"
            if method_conf.get(requires_key, False):
                required_params.append(param)

    for param, param_conf in config["parameters"].items():
        requires_key = f"requires{param[0].upper()}{param[1:]}"
        if chosen_combination.get(requires_key, False):
            required_params.append(param)

    variant_requirements = config.get("variantRequirements", {})
    variant_specific = variant_requirements.get(chosen_algorithm, {}).get(chosen_variant, [])
    required_params.extend(variant_specific)

    # Handle special ILP filter logic
    if "lPFilter" in required_params:
        lp_filter = trial.suggest_categorical("lPFilter", config["parameters"]["lPFilter"]["choices"])
        payload["LPFilter"] = lp_filter
        required_params.remove("lPFilter")
        if lp_filter == "Slack Variable Filter":
            required_params = [p for p in required_params if p != "sequenceEncodingCutoffLevel"]
        elif lp_filter == "Sequence Encoding Filter":
            required_params = [p for p in required_params if p != "slackVariableFilterThreshold"]

    sample_required_parameters(trial, required_params, config, payload)

    try:
        response = requests.post(JAVA_SERVICE_URL, json=payload)
        response.raise_for_status()
        result = response.json()

        # ProM doesn't listen to interrupts outside of ProM's context, in a case when a timeout occurs,
        # the container needs to be restarted otherwise the "pipeline" will continue to run,
        # problem with that is that cpu usage skyrockets and this computation is useless because even if a result
        # is returned at some point it won't be used
        if error := result.get("error"):
            trial.set_user_attr("status", error)
            if "TimeoutException" in error:
                logger.warning("Timeout exception. Restarting container")
                restart_container()
                time.sleep(15)
            raise optuna.TrialPruned()

        fitness = result.get("fitness", -1.0)
        precision = result.get("precision", -1.0)
        f1_score = result.get("f1-score", -1.0)
        simplicity = result.get("simplicity", -1.0)
        generalization = result.get("generalization", -1.0)

        if any(metric == -1.0 for metric in (fitness, precision, simplicity, generalization)):
            trial.set_user_attr("status", "Missing required metrics. Pruning trial.")
            raise optuna.TrialPruned()

        trial.set_user_attr("status", f"Precision plugin: {result.get('precisionPlugin', '')}, F1-Score: {f1_score}")
        return fitness, precision, f1_score, simplicity, generalization

    except requests.exceptions.RequestException as e:
        logger.error(f"Communication error with Java server: {e}")
        raise


def main():
    config = load_config(CONFIG_PATH)

    study_name = os.environ.get(
        "OPTUNA_STUDY_NAME",
        ''.join(random.choices(string.ascii_letters + string.digits, k=8))
    )

    sampler = optuna.samplers.TPESampler()
    logger.info(f"Using Optuna sampler: {type(sampler).__name__}")

    study = optuna.create_study(
        sampler=sampler,
        storage=DB_URL,
        study_name=study_name,
        directions=["maximize"] * 5,
        load_if_exists=True
    )

    study.set_metric_names(["Fitness", "Precision", "F1-Score", "Simplicity", "Generalization"])
    study.optimize(lambda trial: objective(trial, config), n_trials=300)


if __name__ == "__main__":
    main()
