package org.tests;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.discovery.ExportPetriNet;
import org.discovery.alphaMiner.AlphaMinerVariant;
import org.discovery.inductiveMiner.InductiveMinerVariant;
import org.discovery.utils.ParamsConstants;
import org.evaluate.PetriNetEvaluator;
import org.json.JSONObject;
import org.pipeline.MetaDiscoveryPipeline;
import org.preprocessing.EventLogFilters;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.dataawarecnetminer.interactive.InteractiveDataAwareCausalMiner;
import org.processmining.dataawarecnetminer.interactive.InteractiveDataAwareCausalMinerPlugin;
import org.processmining.dataawareexplorer.explorer.ExplorerContext;
import org.processmining.dataawareexplorer.explorer.ExplorerInterface;
import org.processmining.dataawareexplorer.explorer.model.ExplorerModel;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerPlugin;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Plugin;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_UI;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.tbr.TokenBasedReplay;
import org.processmining.tbr.TokenBasedReplayResultLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;


public class Test {
    public static void main(String[] args) throws Exception {
        EventLogFilters eventLogFilters = new EventLogFilters();
        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
        PluginContextFactory factory = new PluginContextFactory();
        UIPluginContextFactory factory1 = new UIPluginContextFactory();

        System.out.println(System.getProperty("java.library.path"));

        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");


//        IMMiningDialog dialog = new IMMiningDialog(xLog);
//
//        Object[] obj = IMPetriNet.minePetriNet(factory.getContext(), xLog, dialog.getMiningParameters());
        Object[] ret = new Object[2];
        XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
        AlphaMinerParameters parameters =  AlphaMinerVariant.createAlphaMinerParameters(String.valueOf(AlphaMinerVariant.CLASSIC));
        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> miner =
                AlphaMinerFactory.createAlphaMiner(factory.getContext(), xLog, classifier, parameters);

        Pair<Petrinet, Marking> markedNet = miner.run();
        ret[0] = markedNet.getFirst();
        ret[1] = markedNet.getSecond();
//
//        PetriNetEvaluator.calculateMetrics(xLog, ret, factory);

        final Marking initialMarking = PetrinetUtils.guessInitialMarking(markedNet.getFirst());
        final Marking finalMarking = PetrinetUtils.guessFinalMarking(markedNet.getFirst());

        PNRepResult result = PetriNetEvaluator.executeAlignments(xLog, markedNet.getFirst(), initialMarking, finalMarking);
        Map<String, Object> result1 = result.getInfo();

//        ExportPetriNet.exportPetrinetToPNMLorEPNMLFile((PetrinetGraph) objects[0], Pnml.PnmlType.PNML, (Marking) objects[1], "/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/splitminer.pnml");

        System.out.println("exported");
    }
}
