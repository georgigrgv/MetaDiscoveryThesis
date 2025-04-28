package org.pipeline;

import org.conformance.PetriNetEvaluator;
import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.discovery.utils.ExportPetriNet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.preprocessing.EventLogFilters;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class MetaDiscoveryPipeline {

    private static final int HTTP_PORT = 8081;
    private static final int TIMEOUT_MINUTES = 5;
    private static final Logger logger = LoggerFactory.getLogger(MetaDiscoveryPipeline.class);
    private static final EventLogFilters filters = new EventLogFilters();
    private static final Map<String, DiscoveryFunction> discoveryMethods = new HashMap<>();
    private static XLog cachedLog = null;

    // Functional interface for discovery methods
    @FunctionalInterface
    interface DiscoveryFunction {
        Object[] discover(XLog log, JSONObject request) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        initializeEnvironment();
        initializeDiscoveryMethods();

        Spark.port(HTTP_PORT);

        Spark.get("/health", (req, res) -> {
            res.status(200);
            return "OK";
        });

        Spark.post("/pipeline", (req, res) -> {
            JSONObject requestBody = new JSONObject(req.body());
            Object[] metrics = runPipeline(requestBody);
            JSONObject response = buildResponse(metrics);
            return response.toString();
        });

        logger.info("Java HTTP server running on port {}...", HTTP_PORT);
    }

    private static void initializeEnvironment() throws Exception {
        String logPath = System.getenv("EVENT_LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            logger.error("Event log path not specified. Set EVENT_LOG_PATH environment variable.");
            System.exit(1);
        }

        ExportPetriNet.createFolderForResults(
                System.getenv("DISCOVERY_RESULTS_FOLDER"),
                System.getenv("DISCOVERY_RESULTS_FOLDER_NAME")
        );

        cachedLog = filters.loadXLog(logPath);
    }

    private static void initializeDiscoveryMethods() {
        DiscoveryAlgorithms discovery = new DiscoveryAlgorithms();
        discoveryMethods.put("InductiveMiner", discovery::obtainPetriNetUsingInductiveMiner);
        discoveryMethods.put("HeuristicsMiner", discovery::obtainPetriNetUsingHeuristicsMiner);
        discoveryMethods.put("AlphaMiner", discovery::obtainPetriNetUsingAlphaMiner);
        discoveryMethods.put("HybridILPMiner", discovery::obtainPetriNetUsingHybridILPMiner);
        discoveryMethods.put("SplitMiner", discovery::obtainPetriNetUsingSplitMiner);
    }

    private static Object[] runPipeline(JSONObject request) throws Exception {
        JSONArray preprocessingSteps = request.getJSONArray("preprocessing");
        String algorithm = request.getString("algorithm");
        int trial = request.getInt("trial");

        XLog filteredLog;
        try {
            filteredLog = applyPreprocessing(cachedLog, preprocessingSteps, request);
        } catch (Exception e) {
            logger.error("Preprocessing failed: ", e);
            return new Object[]{"Preprocessing error: " + e.getMessage()};
        }

        if (filteredLog.isEmpty()) {
            logger.warn("Event log is empty after preprocessing.");
            return new Object[]{"Event Log is empty after preprocessing"};
        }

        Object[] discoveryResult = discoverPetriNet(filteredLog, algorithm, request);
        if (discoveryResult.length == 1 && discoveryResult[0] instanceof String) {
            return discoveryResult;
        }

        return evaluatePetriNet(cachedLog, (PetrinetGraph) discoveryResult[0], trial);
    }

    private static XLog applyPreprocessing(XLog log, JSONArray steps, JSONObject request) throws Exception {
        XLog filteredLog = log;
        for (Object step : steps) {
            String method = step.toString();
            switch (method) {
                case "Matrix Filtering":
                    filteredLog = filters.preprocessUsingMatrixFilter(filteredLog, request);
                    break;
                case "Variant Log Filter":
                    filteredLog = filters.variantFilter(filteredLog, request);
                    break;
                case "Repair Log Filter":
                    filteredLog = filters.repairEventLog(filteredLog, request);
                    break;
                case "Projection Log Filter":
                    filteredLog = filters.projectTracesOnEvents(filteredLog, request);
                    break;
                default:
                    logger.warn("Unknown preprocessing step: {}", method);
            }
        }
        return filteredLog;
    }


    private static Object[] discoverPetriNet(XLog log, String algorithm, JSONObject request) {
        DiscoveryFunction discoveryFunction = discoveryMethods.get(algorithm);
        if (discoveryFunction == null) {
            logger.error("Unsupported algorithm: {}", algorithm);
            return new Object[]{"Unsupported algorithm: " + algorithm};
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Future<Object[]> future = executor.submit(() -> discoveryFunction.discover(log, request));

        try {
            return future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.error("Discovery timeout: ", e);
            return new Object[]{e.toString()};
        } catch (Exception e) {
            logger.error("Discovery error: ", e);
            return new Object[]{e.toString()};
        } finally {
            executor.shutdownNow();
        }
    }

    private static Object[] evaluatePetriNet(XLog log, PetrinetGraph net, int trial) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        PluginContextFactory contextFactory = new PluginContextFactory();
        Future<Object[]> future = executor.submit(() -> {
            try {
                return PetriNetEvaluator.evaluate(log, net, contextFactory, trial);
            } catch (Exception e) {
                logger.error("Evaluation error: ", e);
                return new Object[]{e.toString()};
            }
        });

        try {
            return future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true);
            logger.error("Evaluation timeout: ", e);
            return new Object[]{e.toString()};
        } finally {
            executor.shutdownNow();
        }
    }

    private static JSONObject buildResponse(Object[] metricsResult) {
        JSONObject response = new JSONObject();
        if (metricsResult.length == 6) {
            response.put("fitness", metricsResult[0]);
            response.put("simplicity", metricsResult[1]);
            response.put("precision", metricsResult[2]);
            response.put("f1-score", metricsResult[3]);
            response.put("precisionPlugin", metricsResult[4]);
            response.put("generalization", metricsResult[5]);
        } else if (metricsResult.length == 1) {
            response.put("error", metricsResult[0]);
        }
        return response;
    }

}
