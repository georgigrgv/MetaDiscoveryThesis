package org.pipeline;

import org.discovery.DiscoveryAlgorithms;
import org.evaluate.PetriNetEvaluator;
import org.json.JSONObject;
import org.preprocessing.EventLogFilters;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.PluginContextFactory;

import static spark.Spark.port;
import static spark.Spark.post;

public class MetaDiscoveryPipeline {

    private static final EventLogFilters filters = new EventLogFilters();
    private static XLog cachedLog = null;

    public static void main(String[] args) throws Exception {
        // Load the event log at the beginning
        String logPath = System.getenv("EVENT_LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            System.out.println("Event log path not specified. Set EVENT_LOG_PATH environment variable.");
            System.exit(1);
        }

        cachedLog = filters.loadXLog(logPath);

//        // Start the HTTP server
        port(8080);

        // Define the pipeline endpoint
        post("/pipeline", (req, res) -> {
            JSONObject requestBody = new JSONObject(req.body());
            double hyperParamFilter = requestBody.getDouble("hyperParamFilter");

            // Execute the pipeline
            double fitness = pipeline(cachedLog, hyperParamFilter);

            // Return the result as JSON
            JSONObject response = new JSONObject();
            response.put("fitness", fitness);
            return response.toString();
        });

        System.out.println("Java HTTP server running on port 8080...");
    }


    public static double pipeline(XLog log, double hyperParamFilter) throws Exception {
        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        PluginContextFactory factory = new PluginContextFactory();

        XLog filteredXlog = filters.filterWithMinOccFreq(factory.getContext(), log, XLogInfoFactory.createLogInfo(log).getEventClasses(),
                XLogInfoFactory.createLogInfo(log).getEventClasses().getClasses().toArray(new XEventClass[0]),
                hyperParamFilter);

        Object[] objects = algorithms.obtainPetriNetUsingInductiveMiner(filteredXlog);

        // Calculate fitness
        return PetriNetEvaluator.calculateFitness(log, objects, factory);
    }
}
