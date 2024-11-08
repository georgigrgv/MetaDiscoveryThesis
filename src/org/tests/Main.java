package org.tests;

import org.discovery.DiscoveryAlgorithms;
import org.discovery.ExportPetriNet;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.pipeline.MetaDiscoveryPipeline;
import org.preprocessing.EventLogFilters;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.base.Pnml;

public class Main {
    public static void main(String[] args) throws Exception {
        EventLogFilters eventLogFilters = new EventLogFilters();
        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
        PluginContextFactory factory = new PluginContextFactory();

        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");
//        XLog xlog = discoveryPipeline.processLog(factory.getContext(), xLog);
//        System.out.println(XLogInfoFactory.createLogInfo(xlog).getEventClasses().getClasses());
//
//        XLog log2 = eventLogFilters.filterTracesByMinOcc(xLog, FilterConfig.createFilterParameters());
//        System.out.println(log2);

        DiscoveryAlgorithms algorithms = new DiscoveryAlgorithms();
        Object[] obj = algorithms.obtainPetriNetUsingILPMiner(xLog);

        ExportPetriNet.exportPetrinetToPNMLorEPNMLFile((PetrinetGraph) obj[0], Pnml.PnmlType.PNML, (Marking) obj[1], "/Users/georgegeorgiev/Desktop/PADS_THESIS_TEST/petri2.pnml" );
    }
}
