package algorithm.preprocessing;

import jdk.jfr.internal.tool.Main;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.log.logfilters.impl.EventLogFilter;

import java.io.File;
import java.util.List;

public class EventLogFilters {

    public XLog filterMinCases(PluginContext context, XLog xLog, XEventClasses eventClasses,
                               XEventClass[] eventClassesToKeep, final Double minCases){
        final EventLogFilter filter = new EventLogFilter();
        return filter.filterWithMinOccFreq(context, xLog, eventClasses, eventClassesToKeep, minCases);
    }

    public XLog loadXLog(String xesFile) throws Exception {
        File initialFile = new File(xesFile);
        XesXmlParser parser = new XesXmlParser();
        if (parser.canParse(initialFile)) {
            List<XLog> logs = parser.parse(initialFile);
            if (!logs.isEmpty()) {
                return logs.get(0);
            }
        }
        throw new Exception("Failed to parse XES file.");
    }
}
