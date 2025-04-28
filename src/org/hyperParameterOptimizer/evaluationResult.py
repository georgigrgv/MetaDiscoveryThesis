import optuna

study1 = optuna.load_study(
    storage="postgresql+psycopg2://postgres:postgres@localhost:5432",
    study_name="HOSPITAL_TPE_500")
study2 = optuna.load_study(
    storage="postgresql+psycopg2://postgres:postgres@localhost:5432",
    study_name="BPI2012_NO_PREPROCESSING")
study3 = optuna.load_study(
    storage="postgresql+psycopg2://postgres:postgres@localhost:5432",
    study_name="BPI2017_TPE_500")

study_300 = optuna.create_study(
    study_name="SEPSIS_TPE_300",
    directions=study2.directions
)

# Add the first 300 trials
for trial in study2.trials[:300]:
    study_300.add_trial(trial)


fig = optuna.visualization.plot_pareto_front(study2, target_names=["f1-score", "simplicity", "generalization"],
                                             targets=lambda t: (t.values[2], t.values[3], t.values[4]))
fig.show()
