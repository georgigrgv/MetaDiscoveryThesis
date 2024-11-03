package algorithm.pipeline;

import algorithm.preprocessing.EventLogFilters;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;


public class MetaDiscoveryPipeline {

    public EventLogFilters filters = new EventLogFilters();

    public XLog processLog(PluginContext context, XLog log) {
        // Apply preprocessing and return list of logs
        return filters.filterWithMinOccFreq(context, log, XLogInfoFactory.createLogInfo(log).getEventClasses(),
                XLogInfoFactory.createLogInfo(log).getEventClasses().getClasses().toArray(new XEventClass[0]),
                0.05);
        // Here you would typically add discovery algorithms and ranking steps
    }
}
