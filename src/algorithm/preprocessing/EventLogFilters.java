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
        return filter.filterWithMinCases(context, xLog, eventClasses, eventClassesToKeep, minCases);
    }

    public static XLog loadXLog(File xesFile) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        if (parser.canParse(xesFile)) {
            List<XLog> logs = parser.parse(xesFile);
            if (!logs.isEmpty()) {
                return logs.get(0);
            }
        }
        throw new Exception("Failed to parse XES file.");
    }
}
