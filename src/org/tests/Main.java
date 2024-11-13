package org.tests;

import org.deckfour.xes.model.XLog;
import org.discovery.DiscoveryAlgorithms;
import org.discovery.ExportPetriNet;
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
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
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
        ExportPetriNet.exportPetrinetToPNMLorEPNMLFile((PetrinetGraph) objects[0], Pnml.PnmlType.PNML, (Marking) objects[1], "/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/petriInductiveMiner.pnml");

        // Using reflection get the private methods
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


        PetrinetGraph graph = (PetrinetGraph) objects[0];
        DataPetriNetsWithMarkings dpn = (DataPetriNetsWithMarkings) method.invoke(null, graph);
        ExplorerModel explorerModel = new ExplorerModel(xLog, dpn);

        method2.invoke(new DataAwareExplorerViewsPlugin(), factory.getContext(), explorerModel, explorerContext);
        explorerModel.filter();
        double averageFitness = explorerModel.getAlignmentInformation().averageFitness;
        System.out.println(averageFitness);

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
