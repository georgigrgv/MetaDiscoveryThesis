package org.evaluate;

import org.apache.log4j.lf5.LogLevel;
import org.deckfour.xes.model.XLog;
import org.discovery.ExportPetriNet;
import org.jbpt.petri.PetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.dataawareexplorer.explorer.ExplorerContext;
import org.processmining.dataawareexplorer.explorer.ExplorerInterface;
import org.processmining.dataawareexplorer.explorer.exception.NetVisualizationException;
import org.processmining.dataawareexplorer.explorer.model.ExplorerModel;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerPlugin;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.tbr.TokenBasedReplay;
import org.processmining.tbr.TokenBasedReplayResultLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PetriNetEvaluator {

    public static double[] calculateMetrics(XLog xLog, Object[] model, PluginContextFactory factory)
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException, NetVisualizationException {


        double[] metrics = new double[2];
        Class<?> clazz = DataAwareExplorerPlugin.class;
        Method method = clazz.getDeclaredMethod("wrapPetrinet", PetrinetGraph.class);
        method.setAccessible(true);

        Class<?> clazz2 = DataAwareExplorerViewsPlugin.class;
        Method method2 = clazz2.getDeclaredMethod("computeAlignment", PluginContext.class, ExplorerModel.class, ExplorerContext.class);
        method2.setAccessible(true);

        Class<?> explorerInterfaceClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerInterfaceHeadlessImpl");
        Constructor<?> explorerInterfaceConstructor = explorerInterfaceClass.getDeclaredConstructor();
        explorerInterfaceConstructor.setAccessible(true);
        ExplorerInterface explorerInterface = (ExplorerInterface) explorerInterfaceConstructor.newInstance();

        Class<?> explorerContextClass = Class.forName("org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin$ExplorerContextHeadlessImpl");
        Constructor<?> explorerContextConstructor = explorerContextClass.getDeclaredConstructor(PluginContext.class, ExplorerInterface.class);
        explorerContextConstructor.setAccessible(true);
        ExplorerContext explorerContext = (ExplorerContext) explorerContextConstructor.newInstance(factory.getContext(), explorerInterface);

        PetrinetGraph graph = (PetrinetGraph) model[0];
        DataPetriNetsWithMarkings dpn = (DataPetriNetsWithMarkings) method.invoke(null, graph);
        ExplorerModel explorerModel = new ExplorerModel(xLog, dpn);

        method2.invoke(new DataAwareExplorerViewsPlugin(), factory.getContext(), explorerModel, explorerContext);
        explorerModel.filter();
        metrics[0] = explorerModel.getAlignmentInformation().averageFitness;
        metrics[1] = (double) (explorerModel.getAlignmentInformation().numGoodMoves / (explorerModel.getAlignmentInformation().numGoodMoves + explorerModel.getAlignmentInformation().numModelMoves));
        return metrics;
    }

    public static double[] tokenBasedReplayFitness(XLog log, Petrinet petriNet, PluginContextFactory context){
        ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
        AcceptingPetriNet acceptingPetriNet = convertPetriNetToAcceptingPetriNetPlugin.runDefault(context.getContext(), petriNet);
        return new double[]{TokenBasedReplay.apply(context.getContext(), log, acceptingPetriNet).logFitness};
    }

    public static boolean checkForMarkings(Petrinet petrinet){
        final Marking initialMarking = PetrinetUtils.guessInitialMarking(petrinet);
        final Marking finalMarking = PetrinetUtils.guessFinalMarking(petrinet);

        // Log the problem and continue with TokenBasedReplay
        return initialMarking != null && finalMarking != null;
    }
}
