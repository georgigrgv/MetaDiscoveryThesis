package algorithm.preprocessing;

import jdk.jfr.internal.tool.Main;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;

import java.io.File;
import java.util.List;

public class EventLogFilter {
    public static void filterLogLeastOccurringActivities(final XLog log){
        System.out.println(log.get(2).getAttributes().keySet());
    }
    public static XLog loadXLog(File xesFile) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        if (parser.canParse(xesFile)) {
            List<XLog> logs = parser.parse(xesFile);
            if (!logs.isEmpty()) {
                return logs.get(0); // Return the first log found in the file
            }
        }
        throw new Exception("Failed to parse the XES file.");
    }

    public static class Main{
        @SuppressWarnings("unknown")
        public static void main(String[] args) throws Exception {
            File xesFile = new File("/Users/georgegeorgiev/Downloads/Road_Traffic_Fine_Management_Process.xes");
            XLog log = loadXLog(xesFile);
            EventLogFilter.filterLogLeastOccurringActivities(log);
        }
    }
}
