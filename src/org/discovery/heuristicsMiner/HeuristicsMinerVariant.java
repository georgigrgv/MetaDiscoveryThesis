package org.discovery.heuristicsMiner;

import org.discovery.utils.ParamsConstants;
import org.json.JSONObject;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.settings.HeuristicsMinerSettings;

public class HeuristicsMinerVariant {

    public static HeuristicsMinerSettings createHeuristicsMinerParameters(HeuristicsMinerSettings settings, JSONObject request){
        settings.setRelativeToBestThreshold(request.getDouble(ParamsConstants.RELATIVE_TO_BEST));
        settings.setDependencyThreshold(request.getDouble(ParamsConstants.DEPENDENCY_THRESHOLD));
        settings.setL1lThreshold(request.getDouble(ParamsConstants.LENGTH_ONE_LOOPS_THRESHOLD));
        settings.setL2lThreshold(request.getDouble(ParamsConstants.LENGTH_TWO_LOOPS_THRESHOLD));
        settings.setLongDistanceThreshold(request.getDouble(ParamsConstants.LONG_DISTANCE_THRESHOLD));
        settings.setUseAllConnectedHeuristics(request.getBoolean(ParamsConstants.ALL_TASKS_CONNECTED));
        settings.setUseLongDistanceDependency(request.getBoolean(ParamsConstants.LONG_DISTANCE_DEPENDENCY));
        settings.setCheckBestAgainstL2L(request.getBoolean(ParamsConstants.IGNORE_LOOP_DEPENDENCY_THRESHOLDS));
        return settings;
    }
}
