package org.pipeline;

import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.discovery.ExportPetriNet;
import org.evaluate.PetriNetEvaluator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.preprocessing.EventLogFilters;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static spark.Spark.*;

public class MetaDiscoveryPipeline {

    private static final EventLogFilters filters = new EventLogFilters();
    private static XLog cachedLog = null;
    private static int trial = 0;
    private static Logger logger = LoggerFactory.getLogger(MetaDiscoveryPipeline.class);

    public static void main(String[] args) throws Exception {
        String logPath = System.getenv("EVENT_LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            System.out.println("Event log path not specified. Set EVENT_LOG_PATH environment variable.");
            System.exit(1);
        }
        ExportPetriNet.createFolderForResults(System.getenv("DISCOVERY_RESULTS_FOLDER"),
                System.getenv("DISCOVERY_RESULTS_FOLDER_NAME"));

        cachedLog = filters.loadXLog(logPath);

        port(8081);

        get("/health", (req, res) -> {
            res.status(200);
            return "OK";
        });

        post("/pipeline", (req, res) -> {
            JSONObject requestBody = new JSONObject(req.body());
           JSONArray preprocessing = requestBody.getJSONArray("preprocessing");
            String algorithm = (String) requestBody.get("algorithm");

            Object[] metricsResult = pipeline(cachedLog, preprocessing, algorithm, requestBody, trial++);

            JSONObject response = new JSONObject();
            if (metricsResult.length == 6) {
                response.put("fitness", metricsResult[0]);
                response.put("simplicity", metricsResult[1]);
                response.put("precision", metricsResult[2]);
                response.put("f1-score", metricsResult[3]);
                response.put("precisionPlugin", metricsResult[4]);
                response.put("generalization", metricsResult[5]);
            }
            if(metricsResult.length == 2){
                response.put("fitness", metricsResult[0]);
                response.put("simplicity", metricsResult[1]);
            }
            if(metricsResult.length == 1){
                response.put("error", metricsResult[0]);
            }
            return response.toString();
        });

        System.out.println("Java HTTP server running on port 8080...");
    }


    public static Object[] pipeline(XLog log, JSONArray preprocessing, String algorithm, JSONObject request, int trial) throws Exception {
        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        PluginContextFactory factory = new PluginContextFactory();

//        JSONArray erroeArray= (JSONArray) jsonObject.get("errors");
//        Iterator<String> iterator = erroeArray.iterator();
//        while (iterator.hasNext()) {
//            //yourcode here
//        }

        XLog filteredXlog = log ;
        for (Object preprocessingStep : preprocessing) {
            if ("Matrix Filtering".equals(preprocessingStep)) {
                filteredXlog = filters.preprocessUsingMatrixFilter(filteredXlog, request);
            } else if ("Trace Filter".equals(preprocessingStep)) {
                filteredXlog = filters.filterByTracePercentage(filteredXlog, request);
            } else if ("Repair Log Filter".equals(preprocessingStep)) {
                filteredXlog = filters.repairEventLog(filteredXlog, request);
            } else {
                System.out.println("Unknown preprocessing step: " + preprocessingStep);
            }
        }


        ScheduledExecutorService discoveryExecutor = Executors.newSingleThreadScheduledExecutor();
        XLog finalFilteredXlog = filteredXlog;
        Future<Object[]> discoveryFuture = discoveryExecutor.submit(() -> {
            switch (algorithm) {
                case "InductiveMiner":
                    return algorithms.obtainPetriNetUsingInductiveMiner(finalFilteredXlog, request);
                case "HeuristicsMiner":
                    return algorithms.obtainPetriNetUsingHeuristicsMiner(finalFilteredXlog, request);
                case "AlphaMiner":
                    return algorithms.obtainPetriNetUsingAlphaMiner(finalFilteredXlog, request);
                case "HybridILPMiner":
                    return algorithms.obtainPetriNetUsingHybridILPMiner(finalFilteredXlog, request);
                case "SplitMiner":
                    return algorithms.obtainPetriNetUsingSplitMiner(finalFilteredXlog, request);
                default:
                    throw new UnsupportedOperationException("Unsupported algorithm: " + algorithm);
            }
        });

        Object[] objects;
        try {
            objects = discoveryFuture.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            logger.info(String.valueOf(e));
            discoveryFuture.cancel(true);
            return new Object[]{e.toString()};
        } catch (Exception e) {
            e.printStackTrace();
            return new Object[]{e.toString()};
        }finally {
            discoveryExecutor.shutdownNow();
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Object[] finalObjects = objects;

        Future<Object[]> future = executor.submit(() -> {
            try {
                return PetriNetEvaluator.executeAlignments(log, (PetrinetGraph) finalObjects[0], factory, trial);
            } catch (Exception e) {
                e.printStackTrace();
                return new Object[]{e.toString()};
            }
        });
        try {
            return future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.info(String.valueOf(e));
            return new Object[]{e.toString()};
        } finally {
            executor.shutdownNow();
        }

    }
}
