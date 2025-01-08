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
        EventLogFilters eventLogFilters = new EventLogFilters();
        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
        PluginContextFactory factory = new PluginContextFactory();
        UIPluginContextFactory factory1 = new UIPluginContextFactory();

        System.out.println(System.getProperty("java.library.path"));

        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");


        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        Object[] objects = algorithms.obtainPetriNetUsingSplitMiner(xLog);

        ExportPetriNet.exportPetrinetToPNMLorEPNMLFile((PetrinetGraph) objects[0], Pnml.PnmlType.PNML, (Marking) objects[1], "/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/splitminer.pnml");

        System.out.println("exported");
    }
}
