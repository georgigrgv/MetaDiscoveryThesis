package org.evaluate;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.dataawareexplorer.explorer.ExplorerContext;
import org.processmining.dataawareexplorer.explorer.ExplorerInterface;
import org.processmining.dataawareexplorer.explorer.exception.NetVisualizationException;
import org.processmining.dataawareexplorer.explorer.model.ExplorerModel;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerPlugin;
import org.processmining.dataawareexplorer.plugin.DataAwareExplorerViewsPlugin;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PetriNetEvaluator {

    public static Double calculateFitness(XLog xLog, Object[] model, PluginContextFactory factory)
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException, NetVisualizationException {

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
        return explorerModel.getAlignmentInformation().averageFitness;
    }

}
