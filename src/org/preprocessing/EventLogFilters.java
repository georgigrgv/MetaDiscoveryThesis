package org.preprocessing;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.discovery.utils.ParamsConstants;
import org.json.JSONObject;
import org.processmining.filterd.filters.FilterdEventRateFilter;
import org.processmining.filterd.parameters.Parameter;
import org.processmining.filterd.parameters.ParameterMultipleFromSet;
import org.processmining.filterd.parameters.ParameterOneFromSet;
import org.processmining.filterd.parameters.ParameterValueFromRange;
import org.processmining.filterd.tools.Toolbox;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.logfiltering.algorithms.FilterBasedOnRelationMatrixK;
import org.processmining.logfiltering.algorithms.FilterBasedOnSequence;
import org.processmining.logfiltering.parameters.MatrixFilterParameter;
import org.processmining.logfiltering.parameters.SequenceFilterParameter;
import org.processmining.logfiltering.plugins.RepairLog;
import org.processmining.logfiltering.plugins.VariantCounterPlugin;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.XEventCondition;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EventLogFilters {

    public XLog projectTracesOnEvents(XLog log, JSONObject request){
        FilterdEventRateFilter filter = new FilterdEventRateFilter();

        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER);

        XEventClasses eventClasses = logInfo.getEventClasses();
        List<Parameter> parameters = new ArrayList<>();

        ParameterOneFromSet rate = new ParameterOneFromSet("rate", "rate", "Frequency", Arrays.asList("Frequency", "Occurrence"));
        ParameterOneFromSet selectionType = new ParameterOneFromSet("selectionType", "selectionType", "Filter in", Arrays.asList("Filter in", "Filter out"));
        ParameterValueFromRange<Integer> threshold = new ParameterValueFromRange<Integer>("threshold", "threshold", request.getInt(ParamsConstants.KEEP_THRESHOLD), new ArrayList<>(Arrays.asList(0, 100)));

        // Get the function to generate the,
        List<String> selection = Toolbox.computeDesiredEventsFromThreshold(threshold, rate, eventClasses);

        //retrieve the name of the eventClass to be displayed in the ParameterMultipleFromSet desiredEvents
        List<String> allValuesDesiredEvents = new ArrayList<>();
        for (XEventClass eventClass : eventClasses.getClasses()) {
            allValuesDesiredEvents.add(eventClass.toString());
        }

        // by default all are included
        ParameterMultipleFromSet desiredEvents = new ParameterMultipleFromSet("desiredEvents",
                "Selected classes according to the threshold", selection, allValuesDesiredEvents);

        parameters.add(rate);
        parameters.add(selectionType);
        parameters.add(threshold);
        parameters.add(desiredEvents);
        return filter.filter(log, parameters);
    }

    public XLog variantFilter(XLog log, JSONObject request){
        List<XEventClassifier> logClassifier = log.getClassifiers();
        MatrixFilterParameter parameter = new  MatrixFilterParameter(request.getInt(ParamsConstants.KEEP_THRESHOLD_VF),
                XLogInfoImpl.NAME_CLASSIFIER);
        XLog filteredLog = VariantCounterPlugin.run(null, log, parameter);
        filteredLog.getClassifiers().addAll(logClassifier);
        return filteredLog;
    }


    public XLog repairEventLog(XLog log, JSONObject request){
        List<XEventClassifier> logClassifier = log.getClassifiers();
        MatrixFilterParameter parameter = new MatrixFilterParameter();
        parameter.setProbabilityOfRemoval(request.getFloat(ParamsConstants.PROBABILITY_OF_REMOVAL_RL));
        parameter.setSubsequenceLength(request.getInt(ParamsConstants.SUBSEQUENCE_LENGTH_RL));
        XLog filteredLog = RepairLog.run(null, log, parameter);
        filteredLog.getClassifiers().addAll(logClassifier);
        return filteredLog;
    }

    public XLog preprocessUsingMatrixFilter(XLog log, JSONObject request){
        List<XEventClassifier> logClassifier = log.getClassifiers();
        MatrixFilterParameter parameter = new MatrixFilterParameter();
        parameter.setProbabilityOfRemoval(request.getFloat(ParamsConstants.PROBABILITY_OF_REMOVAL_MF));
        parameter.setSubsequenceLength(request.getInt(ParamsConstants.SUBSEQUENCE_LENGTH_MF));
        XLog filteredLog = FilterBasedOnRelationMatrixK.apply(log, parameter);
        filteredLog.getClassifiers().addAll(logClassifier);
        return filteredLog;
    }

// Not currently used, but can be
//    public XLog preprocessUsingSequenceFilter(XLog log, JSONObject request) throws IOException {
//        SequenceFilterParameter parameter = new SequenceFilterParameter();
//        //Parameters to set
//        parameter.setHighSupportPattern(request.getFloat(ParamsConstants.HIGH_SUPPORT_PATTERN));
//        parameter.setOddDistance(request.getInt(ParamsConstants.ODD_DISTANCE));
//        parameter.setConfHighConfRules(request.getFloat(ParamsConstants.CONF_HIGH_CONF_RULES));
//        parameter.setSuppHighConfRules(request.getFloat(ParamsConstants.SUPP_HIGH_CONF_RULES));
//        parameter.setConfOridnaryRules(request.getFloat(ParamsConstants.CONF_ORDINARY_RULES));
//        return FilterBasedOnSequence.apply(log, parameter);
//    }

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
