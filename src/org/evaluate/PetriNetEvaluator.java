package org.evaluate;

import nl.tue.astar.AStarException;
import org.apache.log4j.lf5.LogLevel;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.discovery.ExportPetriNet;
import org.jbpt.petri.PetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.contexts.uitopia.PluginContextFactory;
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
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.balancedconformance.controlflow.ControlFlowAlignmentException;
import org.processmining.plugins.balancedconformance.controlflow.UnreliableControlFlowAlignmentException;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.tbr.TokenBasedReplay;
import org.processmining.tbr.TokenBasedReplayResultLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PetriNetEvaluator {

    public static double[] calculateMetrics(XLog xLog, Object[] model, PluginContextFactory factory)
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException, NetVisualizationException, NoSuchFieldException,
            ControlFlowAlignmentException {

        double[] metrics = new double[2];
        Class<?> clazz = DataAwareExplorerPlugin.class;
        Method method = clazz.getDeclaredMethod("wrapPetrinet", PetrinetGraph.class);
        method.setAccessible(true);

        Class<?> clazz2 = DataAwareExplorerViewsPlugin.class;
        Method method2 = clazz2.getDeclaredMethod("computeAlignment", PluginContext.class, ExplorerModel.class, ExplorerContext.class);
        method2.setAccessible(true);

        Method method3 = clazz2.getDeclaredMethod("createNetView", ViewMode.class, ExplorerModel.class, ExplorerUpdater.class, ExplorerContext.class);
        method3.setAccessible(true);

        Class<?> explorerInterfaceClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerInterfaceHeadlessImpl");
        Constructor<?> explorerInterfaceConstructor = explorerInterfaceClass.getDeclaredConstructor();
        explorerInterfaceConstructor.setAccessible(true);
        ExplorerInterface explorerInterface = (ExplorerInterface) explorerInterfaceConstructor.newInstance();

        Class<?> explorerContextClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerContextHeadlessImpl");
        Constructor<?> explorerContextConstructor = explorerContextClass.getDeclaredConstructor(PluginContext.class, ExplorerInterface.class);
        explorerContextConstructor.setAccessible(true);
        ExplorerContext explorerContext = (ExplorerContext) explorerContextConstructor.newInstance(factory.getContext(), explorerInterface);

        Class<?> updatableExplorerClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerUpdaterNoOpImpl");
        Constructor<?> updatableExplorerConstructor = updatableExplorerClass.getDeclaredConstructor();
        updatableExplorerConstructor.setAccessible(true);
        ExplorerUpdater updatableExplorer = (ExplorerUpdater) updatableExplorerConstructor.newInstance();

        PetrinetGraph graph = (PetrinetGraph) model[0];
        DataPetriNetsWithMarkings dpn = (DataPetriNetsWithMarkings) method.invoke(null, graph);
        ExplorerModel explorerModel = new ExplorerModel(xLog, dpn);

        method2.invoke(new DataAwareExplorerViewsPlugin(), factory.getContext(), explorerModel, explorerContext);
        explorerModel.filter();
        NetView performanceView = (NetView) method3.invoke(new DataAwareExplorerViewsPlugin(), ViewMode.PRECISION, explorerModel, updatableExplorer, explorerContext);
        metrics[0] = explorerModel.getAlignmentInformation().averageFitness;
        Class<?> clazzNetPrecision = Class.forName("org.processmining.dataawareexplorer.explorer.netview.impl.NetViewPrecisionImpl");
        Field field = clazzNetPrecision.getDeclaredField("precisionResult");
        field.setAccessible(true);
        PrecisionResult result = (PrecisionResult) field.get(performanceView);
        metrics[1] = result.getPrecision();
        return metrics;
    }

    public static PNRepResult executeAlignments(XLog log, PetrinetGraph net,
                                                Marking initialMarking, Marking finalMarking) throws AStarException {
        XEventClass evClassDummy = new XEventClass("DUMMY",
                -1);
        TransEvClassMapping mapping = new
                TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER, evClassDummy);
        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
        for (XEventClass ec : ((XLogInfo) logInfo).getEventClasses().getClasses()) {
            for (Transition t : net.getTransitions()) {
                if (t.getLabel().equals(ec.toString().substring(0,
                        ec.toString().length() - 1))) {
                    mapping.put(t, ec);
                }
            }
        }
        CostBasedCompleteParam parameter = new
                CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
                evClassDummy, net.getTransitions(), 2, 5);
        parameter.setGUIMode(false);
        parameter.setCreateConn(false);
        parameter.setInitialMarking(initialMarking);
        parameter.setFinalMarkings(finalMarking);
        parameter.setMaxNumOfStates(200000);
        PluginContext context = null;
        PetrinetReplayerWithILP replWithoutILP = new
                PetrinetReplayerWithILP();
        PNLogReplayer replayer = new PNLogReplayer();
        PNRepResult pnRepResult = replayer.replayLog(context, net, log,
                mapping, replWithoutILP, parameter);
       return pnRepResult;
    }

    public static double[] tokenBasedReplayFitness(XLog log, Petrinet petriNet, PluginContextFactory context){
        ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
        AcceptingPetriNet acceptingPetriNet = convertPetriNetToAcceptingPetriNetPlugin.runDefault(context.getContext(), petriNet);
        return new double[]{TokenBasedReplay.apply(context.getContext(), log, acceptingPetriNet).logFitness, Math.random()};
    }

    public static boolean checkForMarkings(Petrinet petrinet){
        final Marking initialMarking = PetrinetUtils.guessInitialMarking(petrinet);
        final Marking finalMarking = PetrinetUtils.guessFinalMarking(petrinet);

        // Log the problem and continue with TokenBasedReplay
        return initialMarking != null && finalMarking != null;
    }
}
