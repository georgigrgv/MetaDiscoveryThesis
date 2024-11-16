package org.tests;

import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.discovery.ExportPetriNet;
import org.evaluate.PetriNetEvaluator;
import org.pipeline.MetaDiscoveryPipeline;
import org.preprocessing.EventLogFilters;
import org.processmining.contexts.uitopia.PluginContextFactory;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class Main {
    public static void main(String[] args) throws Exception {
        EventLogFilters eventLogFilters = new EventLogFilters();
        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
        PluginContextFactory factory = new PluginContextFactory();

        System.out.println(System.getProperty("java.library.path"));

        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");

        BpmnExportPlugin exportPlugin = new BpmnExportPlugin();
        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        Object[] objects = algorithms.obtainPetriNetUsingInductiveMiner(xLog);

        // Using reflection get the private methods

//        BPMN2PetriNetConverter_Configuration config = new BPMN2PetriNetConverter_Configuration();
//        BPMN2PetriNetConverter_UI ui = new BPMN2PetriNetConverter_UI(config);
//        BPMN2PetriNetConverter_Plugin converter_plugin = new BPMN2PetriNetConverter_Plugin();
//        Object[] converted_bpmn= converter_plugin.convert( factory.getContext(), (BPMNDiagram) objects[0], config);

        System.out.println(PetriNetEvaluator.calculateFitness(xLog, objects, factory));

        //TODO: This is important right now
//        explorerModel.setEventClassifier(classifier);
//        explorerModel.setFilterConfiguration(filterConfiguration);


//        XLog xlog = discoveryPipeline.processLog(factory.getContext(), xLog);
//        System.out.println(XLogInfoFactory.createLogInfo(xlog).getEventClasses().getClasses());
//
//        XLog log2 = eventLogFilters.filterTracesByMinOcc(xLog, FilterConfig.createFilterParameters());
//        System.out.println(log2);

//        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
//        Object[] obj = algorithms.obtainPetriNetUsingSplitMiner(xLog);
//
//
//        exportPlugin.export(factory.getContext(), (BPMNDiagram) obj[0], new File("/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/split.bpmn"));


    }
}
