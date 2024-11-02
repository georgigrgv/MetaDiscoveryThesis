package algorithm.tests;

import algorithm.factory.PluginContextFactory;
import algorithm.pipeline.MetaDiscoveryPipeline;
import algorithm.preprocessing.EventLogFilters;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.log.logfilters.impl.EventLogFilter;

public class Main {
    public static void main(String[] args) throws Exception {
        EventLogFilters eventLogFilters = new EventLogFilters();
        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
        PluginContextFactory factory = new PluginContextFactory();

        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");
        XLog xlog = discoveryPipeline.processLog(factory.getContext(), xLog);
        xlog.forEach(System.out::println);
    }
}
