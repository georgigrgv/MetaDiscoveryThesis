# MetaDiscoveryThesis

Problems: LPSolver library


Additional feature:
- Can find information about trials in optuna-dashboard optuna-dashboard sqlite:////Users/georgegeorgiev/Desktop/MetaDiscoveryThesis/hyperparamopt.sqlite 



Event log:
Needs to be added to a certain directory so it can be loaded by java


Mount in the docker compose yml is uses a folder on your pc to store db data, you cna remove it or keep it to have all the trials available
optuna-dashboard postgresql+psycopg2://postgres:postgres@localhost:5432



Other way to compute preicison:
EscapingEdgesPrecisionPlugin precisionPlugin = new EscapingEdgesPrecisionPlugin();
ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
AcceptingPetriNet acceptingPetriNet = convertPetriNetToAcceptingPetriNetPlugin.runDefault(context, (Petrinet) net);

//        EscapingEdgesPrecisionResult precisionResult = precisionPlugin.runDefault(context, result, acceptingPetriNet);
//        double precision = precisionResult.getPrecision();