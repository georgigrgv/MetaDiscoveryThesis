package org.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.processmining.filterd.parameters.Parameter;
import org.processmining.filterd.parameters.ParameterOneFromSet;
import org.processmining.filterd.parameters.ParameterRangeFromRange;

public class FilterConfig {

    // Test Parameters for second filter plugin
    public static List<Parameter> createFilterParameters() {
        List<Parameter> parameters = new ArrayList<>();

        // Select classifier parameter ("Event Name" in this case)
        List<String> classifierOptions = Collections.singletonList("Event Name");
        ParameterOneFromSet classifierParam = new ParameterOneFromSet("classifier", "Select classifier", "Event Name", classifierOptions);
        parameters.add(classifierParam);

        // Threshold type parameter ("frequency" in this case)
        List<String> thresholdTypeOptions = Arrays.asList("frequency", "occurrence");
        ParameterOneFromSet thresholdTypeParam = new ParameterOneFromSet("FreqOcc", "Threshold type", "frequency", thresholdTypeOptions);
        parameters.add(thresholdTypeParam);

        // Filter mode parameter ("in" in this case)
        List<String> filterModeOptions = Arrays.asList("in", "out");
        ParameterOneFromSet filterModeParam = new ParameterOneFromSet("filterInOut", "Filter mode", "in", filterModeOptions);
        parameters.add(filterModeParam);

        // Threshold range parameter (0.0 to 100.0 in this case)
        List<Double> rangeFreqDefault = Arrays.asList(0.0, 1.0);
        List<Double> rangeFreqOptions = Arrays.asList(0.0, 100.0);  // Set the min and max values for the range
        ParameterRangeFromRange<Double> rangeFreqParam = new ParameterRangeFromRange<>("rangeFreq", "Threshold", rangeFreqDefault, rangeFreqOptions, Double.class);
        parameters.add(rangeFreqParam);

        return parameters;
    }
}
