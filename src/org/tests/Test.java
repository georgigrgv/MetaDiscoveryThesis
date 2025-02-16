package org.tests;

import com.kitfox.svg.A;
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
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentPlugin;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.dataawarecnetminer.interactive.InteractiveDataAwareCausalMiner;
import org.processmining.dataawarecnetminer.interactive.InteractiveDataAwareCausalMinerPlugin;
import org.processmining.dataawareexplorer.explorer.ExplorerContext;
import org.processmining.dataawareexplorer.explorer.ExplorerInterface;
import org.processmining.dataawareexplorer.explorer.ExplorerUpdater;
import org.processmining.dataawareexplorer.explorer.exception.NetVisualizationException;
import org.processmining.dataawareexplorer.explorer.model.ExplorerModel;
import org.processmining.dataawareexplorer.explorer.netview.NetView;
import org.processmining.dataawareexplorer.explorer.netview.impl.ViewMode;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerPlugin;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.dataawarereplayer.precision.PrecisionResult;
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
import org.processmining.plugins.balancedconformance.controlflow.ControlFlowAlignmentException;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Plugin;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_UI;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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


        IMMiningDialog dialog = new IMMiningDialog(xLog);
        Object[] obj = IMPetriNet.minePetriNet(factory.getContext(), xLog, dialog.getMiningParameters());
//        Object[] ret = new Object[2];
//        XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
//        AlphaMinerParameters parameters =  AlphaMinerVariant.createAlphaMinerParameters(String.valueOf(AlphaMinerVariant.CLASSIC));
//        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> miner =
//                AlphaMinerFactory.createAlphaMiner(factory.getContext(), xLog, classifier, parameters);
//
//        Pair<Petrinet, Marking> markedNet = miner.run();

        double[] result = PetriNetEvaluator.executeAlignments(xLog, (PetrinetGraph) obj[0], factory);


//        ExportPetriNet.exportPetrinetToPNMLorEPNMLFile((PetrinetGraph) objects[0], Pnml.PnmlType.PNML, (Marking) objects[1], "/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/splitminer.pnml");

        System.out.println("exported");


// TODO: DON'T DELETE
//        public static double[] calculateMetrics(XLog xLog, Object[] model, PluginContextFactory factory)
//            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException,
//                InstantiationException, IllegalAccessException, NetVisualizationException, NoSuchFieldException,
//                ControlFlowAlignmentException {
//
//            double[] metrics = new double[3];
//            Class<?> clazz = DataAwareExplorerPlugin.class;
//            Method method = clazz.getDeclaredMethod("wrapPetrinet", PetrinetGraph.class);
//            method.setAccessible(true);
//
//            Class<?> clazz2 = DataAwareExplorerViewsPlugin.class;
//            Method method2 = clazz2.getDeclaredMethod("computeAlignment", PluginContext.class, ExplorerModel.class, ExplorerContext.class);
//            method2.setAccessible(true);
//
//            Method method3 = clazz2.getDeclaredMethod("createNetView", ViewMode.class, ExplorerModel.class, ExplorerUpdater.class, ExplorerContext.class);
//            method3.setAccessible(true);
//
//            Class<?> explorerInterfaceClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerInterfaceHeadlessImpl");
//            Constructor<?> explorerInterfaceConstructor = explorerInterfaceClass.getDeclaredConstructor();
//            explorerInterfaceConstructor.setAccessible(true);
//            ExplorerInterface explorerInterface = (ExplorerInterface) explorerInterfaceConstructor.newInstance();
//
//            Class<?> explorerContextClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerContextHeadlessImpl");
//            Constructor<?> explorerContextConstructor = explorerContextClass.getDeclaredConstructor(PluginContext.class, ExplorerInterface.class);
//            explorerContextConstructor.setAccessible(true);
//            ExplorerContext explorerContext = (ExplorerContext) explorerContextConstructor.newInstance(factory.getContext(), explorerInterface);
//
//            Class<?> updatableExplorerClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerUpdaterNoOpImpl");
//            Constructor<?> updatableExplorerConstructor = updatableExplorerClass.getDeclaredConstructor();
//            updatableExplorerConstructor.setAccessible(true);
//            ExplorerUpdater updatableExplorer = (ExplorerUpdater) updatableExplorerConstructor.newInstance();
//
//            PetrinetGraph graph = (PetrinetGraph) model[0];
//            DataPetriNetsWithMarkings dpn = (DataPetriNetsWithMarkings) method.invoke(null, graph);
//            ExplorerModel explorerModel = new ExplorerModel(xLog, dpn);
//
//            method2.invoke(new DataAwareExplorerViewsPlugin(), factory.getContext(), explorerModel, explorerContext);
//            explorerModel.filter();
//            NetView performanceView = (NetView) method3.invoke(new DataAwareExplorerViewsPlugin(), ViewMode.PRECISION, explorerModel, updatableExplorer, explorerContext);
//            metrics[0] = explorerModel.getAlignmentInformation().averageFitness;
//            Class<?> clazzNetPrecision = Class.forName("org.processmining.dataawareexplorer.explorer.netview.impl.NetViewPrecisionImpl");
//            Field field = clazzNetPrecision.getDeclaredField("precisionResult");
//            field.setAccessible(true);
//            PrecisionResult result = (PrecisionResult) field.get(performanceView);
//            metrics[1] = result.getPrecision();
//            metrics[2] = calculateF1Score(metrics[0], metrics[1]);
//            return metrics;
//        }
    }
}
