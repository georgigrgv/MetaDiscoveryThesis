package org.preprocessing;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.discovery.utils.ParamsConstants;
import org.json.JSONObject;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.logfiltering.algorithms.FilterBasedOnRelationMatrixK;
import org.processmining.logfiltering.algorithms.FilterBasedOnSequence;
import org.processmining.logfiltering.parameters.MatrixFilterParameter;
import org.processmining.logfiltering.parameters.SequenceFilterParameter;
import org.processmining.logfiltering.plugins.RepairLog;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.XEventCondition;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EventLogFilters {


    public XLog repairEventLog(XLog log, JSONObject request){
        MatrixFilterParameter parameter = new MatrixFilterParameter();
        parameter.setProbabilityOfRemoval(request.getFloat(ParamsConstants.PROBABILITY_OF_REMOVAL_RL));
        parameter.setSubsequenceLength(request.getInt(ParamsConstants.SUBSEQUENCE_LENGTH_RL));
        return RepairLog.run(null, log, parameter);
    }

    /**
     * Filtering traces based on % occurrence.
     */
    public XLog filterByTracePercentage(XLog log, JSONObject request) {
        XLog clonedLog = (XLog) log.clone();
        Map<List<String>, Integer> traceOccurrenceMap = new HashMap<>();
        Map<XTrace, List<String>> traceActivitiesMap = new HashMap<>();

        int totalTraces = clonedLog.size();
        for (XTrace trace : clonedLog) {
            List<String> activities = new ArrayList<>();
            for (XEvent event : trace) {
                activities.add(event.getAttributes().get("concept:name").toString());
            }
            traceActivitiesMap.put(trace, activities);
            traceOccurrenceMap.put(activities, traceOccurrenceMap.getOrDefault(activities, 0) + 1);
        }

        Map<List<String>, Double> tracePercentageMap = new HashMap<>();
        for (Map.Entry<List<String>, Integer> entry : traceOccurrenceMap.entrySet()) {
            double percentage = (entry.getValue() / (double) totalTraces) * 100;
            tracePercentageMap.put(entry.getKey(), percentage);
        }

        XLog filteredLog = (XLog) clonedLog.clone();
        filteredLog.clear();

        for (XTrace trace : clonedLog) {
            List<String> activities = traceActivitiesMap.get(trace);
            if (tracePercentageMap.get(activities) >= request.getFloat(ParamsConstants.MIN_OCCURANCE_THRESHOLD)) {
                filteredLog.add(trace);
            }
        }
        return filteredLog;
    }

    /**
     * Filtering traces based on % occurrence.
     */
    public XLog filterByEventPercentage(PluginContext context, XLog log, final XEventClasses allEventClasses,
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
        for (XTrace trace : log) {
            for (XEvent event : trace) {
                XEventClass eventClass = allEventClasses.getClassOf(event);
                eventCounts.put(eventClass, eventCounts.getOrDefault(eventClass, 0) + 1);
                totalEvents++;
            }
        }
        Map<XEventClass, Double> eventFrequency = new HashMap<>();
        for (Map.Entry<XEventClass, Integer> entry : eventCounts.entrySet()) {
            double frequency = (double) entry.getValue() / totalEvents;
            eventFrequency.put(entry.getKey(), frequency);
        }

        return eventFrequency;
    }


    public XLog preprocessUsingMatrixFilter(XLog log, JSONObject request){
        MatrixFilterParameter parameter = new MatrixFilterParameter();
        parameter.setProbabilityOfRemoval(request.getFloat(ParamsConstants.PROBABILITY_OF_REMOVAL_MF));
        parameter.setSubsequenceLength(request.getInt(ParamsConstants.SUBSEQUENCE_LENGTH_MF));
        return FilterBasedOnRelationMatrixK.apply(log, parameter);
    }

    public XLog preprocessUsingSequenceFilter(XLog log, JSONObject request) throws IOException {
        SequenceFilterParameter parameter = new SequenceFilterParameter();
        //Parameters to set
        parameter.setHighSupportPattern(request.getFloat(ParamsConstants.HIGH_SUPPORT_PATTERN));
        parameter.setOddDistance(request.getInt(ParamsConstants.ODD_DISTANCE));
        parameter.setConfHighConfRules(request.getFloat(ParamsConstants.CONF_HIGH_CONF_RULES));
        parameter.setSuppHighConfRules(request.getFloat(ParamsConstants.SUPP_HIGH_CONF_RULES));
        parameter.setConfOridnaryRules(request.getFloat(ParamsConstants.CONF_ORDINARY_RULES));
        return FilterBasedOnSequence.apply(log, parameter);
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
