package org.discovery.utils;


import org.python.antlr.ast.Str;

public class ParamsConstants {
    public static final String ALGORITHM = "algorithm";
    public static final String FILTER_VALUE = "hyperParamFilter";
    public static final String VARIANT = "variant";
    //IM
    public static final String ALGORITHM_VARIANT_INDUCTIVE = "inductiveVariant";
    public static final String NOISE_THRESHOLD = "noiseThreshold";
    // Alpha Miner
    public static final String CAUSAL_THRESHOLD = "causalThreshold";
    public static final String NOISE_THRESHOLD_LEAST_FREQ = "noiseThresholdLeastFreq";
    public static final String NOISE_THRESHOLD_MOST_FREQ = "noiseThresholdMostFreq";
    public static final String ALGORITHM_VARIANT_ALPHA = "alphaVariant";
    // Heuristics Miner
    public static final String RELATIVE_TO_BEST = "relativeToBest";
    public static final String DEPENDENCY_THRESHOLD = "dependencyThreshold";
    public static final String LENGTH_ONE_LOOPS_THRESHOLD = "lengthOneLoopsThreshold";
    public static final String LENGTH_TWO_LOOPS_THRESHOLD = "lengthTwoLoopsThreshold";
    public static final String LONG_DISTANCE_THRESHOLD = "longDistanceThreshold";
    public static final String ALL_TASKS_CONNECTED = "allTasksConnected";
    public static final String LONG_DISTANCE_DEPENDENCY = "longDistanceDependency";
    public static final String IGNORE_LOOP_DEPENDENCY_THRESHOLDS = "ignoreLoopDependencyThresholds";
    // Split Miner
    public static final String ETA = "eta";
    public static final String EPSILON = "epsilon";
    public static final String PARALLELISM_FIRST = "parallelismFirst";
    public static final String REPLACE_IORS = "replaceIORs";
    public static final String REMOVE_LOOP_ACTIVITIES = "removeLoopActivities";
    // LP Miner
    public static final String LP_OBJECTIVE = "lPObjective";
    public static final String LP_FILTER = "lPFilter";
    public static final String SLACK_VARIABLE_FILTER_THRESHOLD = "slackVariableFilterThreshold";
    public static final String SEQUENCE_ENCODING_CUTOFF_LEVEL = "sequenceEncodingCutoffLevel";
    public static final String LP_VARIABLE_TYPE = "lPVariableType";
    public static final String DISCOVERY_STRATEGY = "discoveryStrategy";
    // Matrix filter
    public static final String PROBABILITY_OF_REMOVAL_MF = "probabilityOfRemovalMF";
    public static final String SUBSEQUENCE_LENGTH_MF = "subsequenceLengthMF";
    //Repair Log Filter
    public static final String PROBABILITY_OF_REMOVAL_RL = "probabilityOfRemovalRL";
    public static final String SUBSEQUENCE_LENGTH_RL = "subsequenceLengthRL";
    // Trace filter
    public static final String MIN_OCCURANCE_THRESHOLD = "percentageOccurance";
    // Projection filter
    public static final String KEEP_THRESHOLD = "keepThresholdP";
    // Variant filter
    public static final String KEEP_THRESHOLD_VF = "keepThresholdVF";

 }
