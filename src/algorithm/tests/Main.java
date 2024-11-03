package algorithm.tests;

import algorithm.factory.PluginContextFactory;
import algorithm.pipeline.MetaDiscoveryPipeline;
import algorithm.preprocessing.EventLogFilters;
import algorithm.preprocessing.FilterConfig;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.log.logfilters.impl.EventLogFilter;

public class Main {
    public static void main(String[] args) throws Exception {
        EventLogFilters eventLogFilters = new EventLogFilters();
        MetaDiscoveryPipeline discoveryPipeline = new MetaDiscoveryPipeline();
        PluginContextFactory factory = new PluginContextFactory();

        XLog xLog = eventLogFilters.loadXLog("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");
        XLog xlog = discoveryPipeline.processLog(factory.getContext(), xLog);
        System.out.println(XLogInfoFactory.createLogInfo(xlog).getEventClasses().getClasses());

        XLog log2 = eventLogFilters.filterTracesByMinOcc(xLog, FilterConfig.createFilterParameters());
        System.out.println(log2);

    }
}
