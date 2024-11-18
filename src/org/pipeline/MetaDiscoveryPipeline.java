package org.pipeline;

import org.discovery.DiscoveryAlgorithms;
import org.evaluate.PetriNetEvaluator;
import org.json.JSONObject;
import org.preprocessing.EventLogFilters;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.framework.plugin.PluginContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;



public class MetaDiscoveryPipeline {

    public EventLogFilters filters = new EventLogFilters();
    private Map<Object, Double> dataStorage = new HashMap<>();

    public double pipeline(XLog log, double hyperParamFilter) throws Exception {
        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        PluginContextFactory factory = new PluginContextFactory();

        XLog filteredXlog = filters.filterWithMinOccFreq(factory.getContext(), log, XLogInfoFactory.createLogInfo(log).getEventClasses(),
                XLogInfoFactory.createLogInfo(log).getEventClasses().getClasses().toArray(new XEventClass[0]),
                hyperParamFilter);

        Object[] objects = algorithms.obtainPetriNetUsingInductiveMiner(filteredXlog);

        //calculate fitness
        Double fitness = PetriNetEvaluator.calculateFitness(log, objects, factory);

        dataStorage.put(objects, fitness);

        return fitness;
        // Here you would typically add discovery algorithms and ranking steps
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java MetaDiscoveryPipeline <logPath>");
            return;
        }

        String logPath = "/Users/georgegeorgiev/Desktop/MetaDiscoveryThesis/src/org/tests/exampleLogs/Road_Traffic_Fine_Management_Process.xes"; // Path to the XES log file
        String pythonScript = "/Users/georgegeorgiev/Desktop/MetaDiscoveryThesis/src/org/hyperParameterOptimizer/bayesianOptimizer.py"; // Relative path to the Python script

        // Command to execute the Python script
        String[] command = {
                "arch", // Specify architecture
                "-arm64", // Force x86_64 mode
                "python3", // Python binary
                pythonScript, // Python script
                logPath // Argument to the Python script
        };
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // Redirect error and output streams
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line); // Print Python script output
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Python script executed successfully!");
        } else {
            System.err.println("Python script execution failed with exit code " + exitCode);
        }
    }
}
