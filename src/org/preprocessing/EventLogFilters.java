package org.preprocessing;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.filterd.filters.FilterdTraceFrequencyFilter;
import org.processmining.filterd.parameters.Parameter;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.XEventCondition;

import java.io.File;
import java.util.*;

public class EventLogFilters {


    /**
    / Filtering traces with different configs based on the Parameters
    */
    public XLog filterTracesByMinOcc(XLog log, List<Parameter> parameters){

        //Using filter from the plugin Filter Event Log
        FilterdTraceFrequencyFilter filter = new FilterdTraceFrequencyFilter();

        return filter.filter(log, parameters);
    }

    /**
     * Filtering events based on % occurance.
     */
    public XLog filterWithMinOccFreq(PluginContext context, XLog log, final XEventClasses allEventClasses,
                                     final XEventClass[] eventClassesToKeep, final Double minOccurrence) {

        final HashSet<XEventClass> toKeep = new HashSet<>(Arrays.asList(eventClassesToKeep));
        final Map<XEventClass, Double> count = getOccurrenceFrequency(log, allEventClasses);

        return LogFilter.filter(context.getProgress(), 100, log, XLogInfoFactory.createLogInfo(log),
                (XEventCondition) event -> {
                    XEventClass c = allEventClasses.getClassOf(event);
                    if (!toKeep.contains(c)) {
                        return false;
                    }
                    double percentage = count.get(c);
                    return percentage >= minOccurrence;
                });
    }

    /**
     * Calculates the occurrence frequency of each event class in the provided log.
     */
    public Map<XEventClass, Double> getOccurrenceFrequency(XLog log, XEventClasses allEventClasses) {
        Map<XEventClass, Integer> eventCounts = new HashMap<>();
        int totalEvents = 0;

        // Iterate through each trace and event to count occurrences of each event class
        for (XTrace trace : log) {
            for (XEvent event : trace) {
                XEventClass eventClass = allEventClasses.getClassOf(event);
                eventCounts.put(eventClass, eventCounts.getOrDefault(eventClass, 0) + 1);
                totalEvents++;
            }
        }

        // Calculate the frequency as a percentage of the total number of events
        Map<XEventClass, Double> eventFrequency = new HashMap<>();
        for (Map.Entry<XEventClass, Integer> entry : eventCounts.entrySet()) {
            double frequency = (double) entry.getValue() / totalEvents;
            eventFrequency.put(entry.getKey(), frequency);
        }

        return eventFrequency;
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
