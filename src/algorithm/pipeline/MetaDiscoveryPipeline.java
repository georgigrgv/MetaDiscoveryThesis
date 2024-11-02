package algorithm.pipeline;

import algorithm.preprocessing.EventLogFilters;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;


public class MetaDiscoveryPipeline {

    public EventLogFilters filters = new EventLogFilters();

    public XLog processLog(PluginContext context, XLog log) {
        // Apply preprocessing and return list of logs
        return filters.filterMinCases(context, log, XLogInfoFactory.createLogInfo(log).getEventClasses(), null, 0.1);
        // Here you would typically add discovery algorithms and ranking steps
    }
}
