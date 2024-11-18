package org.tests;

import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.discovery.ExportPetriNet;
import org.evaluate.PetriNetEvaluator;
import org.json.JSONObject;
import org.pipeline.MetaDiscoveryPipeline;
import org.preprocessing.EventLogFilters;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.dataawarecnetminer.interactive.InteractiveDataAwareCausalMiner;
import org.processmining.dataawarecnetminer.interactive.InteractiveDataAwareCausalMinerPlugin;
import org.processmining.dataawareexplorer.explorer.ExplorerContext;
import org.processmining.dataawareexplorer.explorer.ExplorerInterface;
import org.processmining.dataawareexplorer.explorer.model.ExplorerModel;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerPlugin;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Plugin;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_UI;
import org.processmining.plugins.pnml.base.Pnml;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class Test {
    public static void main(String[] args) throws Exception {
//        EventLogFilters eventLogFilters = new EventLogFilters();
//        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
//        PluginContextFactory factory = new PluginContextFactory();
//        UIPluginContextFactory factory1 = new UIPluginContextFactory();
//
//        System.out.println(System.getProperty("java.library.path"));
//
//        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");
//
//        BpmnExportPlugin exportPlugin = new BpmnExportPlugin();
//        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
//        Object[] objects = algorithms.obtainPetriNetUsingInductiveMiner(xLog);
//
//        // Using reflection get the private methods
//
//
//        //System.out.println(PetriNetEvaluator.calculateFitness(xLog, objects, factory));
//
//        //TODO: This is important right now
////        explorerModel.setEventClassifier(classifier);
////        explorerModel.setFilterConfiguration(filterConfiguration);
//
//
////        XLog xlog = discoveryPipeline.processLog(factory.getContext(), xLog);
////        System.out.println(XLogInfoFactory.createLogInfo(xlog).getEventClasses().getClasses());
////
////        XLog log2 = eventLogFilters.filterTracesByMinOcc(xLog, FilterConfig.createFilterParameters());
////        System.out.println(log2);
//
////        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
////        Object[] obj = algorithms.obtainPetriNetUsingSplitMiner(xLog);
////
////
//        ExportPetriNet.exportPetrinetToPNMLorEPNMLFile((PetrinetGraph) objects[0], Pnml.PnmlType.PNML, (Marking) objects[1], "/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/ilpminer.pnml");
        try {
            // Paths and parameters
            String pythonExecutable = "python3"; // Use "python" if python3 is the default
            String pythonScriptPath = "path/to/optimizer.py";
            String jarPath = "path/to/pipeline.jar";
            String logPath = "path/to/log.xes";
            int nTrials = 50;

            // Build the Python command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    pythonScriptPath,
                    jarPath,
                    logPath,
                    String.valueOf(nTrials)
            );

            // Start the Python process
            Process process = processBuilder.start();

            // Read the Python script's output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // Wait for the Python process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Parse the JSON result
                JSONObject result = new JSONObject(output.toString());
                JSONObject bestParams = result.getJSONObject("best_params");
                double bestFitness = result.getDouble("best_fitness");

                // Output the results
                System.out.println("Best Hyperparameters: " + bestParams);
                System.out.println("Best Fitness: " + bestFitness);
            } else {
                System.err.println("Python script failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
