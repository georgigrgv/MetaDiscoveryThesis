package org.pipeline;

import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.evaluate.PetriNetEvaluator;
import org.json.JSONObject;
import org.preprocessing.EventLogFilters;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.balancedconformance.controlflow.ControlFlowAlignmentException;
import org.processmining.plugins.balancedconformance.controlflow.UnreliableControlFlowAlignmentException;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;

import static spark.Spark.*;

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

        port(8081);

        get("/health", (req, res) -> {
            res.status(200);
            return "OK";
        });

        // Define the pipeline endpoint
        post("/pipeline", (req, res) -> {
            JSONObject requestBody = new JSONObject(req.body());
            double hyperParamFilter = requestBody.getDouble("hyperParamFilter");
            String algorithm = (String) requestBody.get("algorithm");

            // Execute the pipeline
            double[] metricsResult = pipeline(cachedLog, hyperParamFilter, algorithm, requestBody);

            JSONObject response = new JSONObject();
            if (metricsResult.length == 4) {
                response.put("fitness", metricsResult[0]);
                response.put("precision", metricsResult[1]);
                response.put("f1-score", metricsResult[2]);
                response.put("simplicity", metricsResult[3]);
            }
            return response.toString();
        });

        System.out.println("Java HTTP server running on port 8080...");
    }


    public static double[] pipeline(XLog log, double hyperParamFilter, String algorithm, JSONObject request) throws Exception {
        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        PluginContextFactory factory = new PluginContextFactory();

        XLog filteredXlog = filters.filterWithMinOccFreq(factory.getContext(), log, XLogInfoFactory.createLogInfo(log).getEventClasses(),
                XLogInfoFactory.createLogInfo(log).getEventClasses().getClasses().toArray(new XEventClass[0]),
                hyperParamFilter);

        Object[] objects = new Object[2];
        switch (algorithm) {
            case "InductiveMiner":
                objects = algorithms.obtainPetriNetUsingInductiveMiner(filteredXlog, request);
                break;
            case "HeuristicsMiner":
                objects = algorithms.obtainPetriNetUsingHeuristicsMiner(filteredXlog, request);
                break;
            case "AlphaMiner":
                objects = algorithms.obtainPetriNetUsingAlphaMiner(filteredXlog, request);
                break;
            case "HybridILPMiner":
                objects = algorithms.obtainPetriNetUsingHybridILPMiner(filteredXlog);
                break;
            case "SplitMiner":
                objects = algorithms.obtainPetriNetUsingSplitMiner(filteredXlog, request);
                break;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Object[] finalObjects = objects;
        Future<double[]> future = executor.submit(() -> {
            try {
                return PetriNetEvaluator.executeAlignments(log, (PetrinetGraph) finalObjects[0], factory);
            } catch (AStarException e) {
                return new double[]{-1.0, -1.0, -1.0, -1.0};
            }
        });

        try {
            return future.get(10, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            return new double[]{-1.0, -1.0, -1.0, -1.0};
        }  finally {
            executor.shutdownNow();
        }

    }
}
