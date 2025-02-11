package org.discovery.alphaMiner;

import org.discovery.utils.ParamsConstants;
import org.json.JSONObject;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaRobustMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;

public enum AlphaMinerVariant {

    // Exists in ProM no need to use this, for inductive miner it wasn't that simple

    CLASSIC("Alpha"), ALPHA_PLUS("Alpha+"), ALPHA_PLUS_PLUS("Alpha++"), ALPHA_SHARP("Alpha#"), ALPHA_ROBUST("AlphaR");

    private final String name;

    AlphaMinerVariant(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static AlphaVersion fromString(String name) {
        for (AlphaVersion version : AlphaVersion.values()) {
            if (version.toString().equals(name)) {
                return version;
            }
        }
        throw new IllegalArgumentException("No enum constant with name " + name);
    }
    public static AlphaMinerParameters createAlphaMinerParameters(String variant) {
        AlphaMinerParameters parameters = new AlphaMinerParameters();
        parameters.setVersion(AlphaMinerVariant.fromString(variant));
        return parameters;
    }

    public static AlphaRobustMinerParameters createAlphaRobustMinerParameters(String variant, JSONObject request) {
        AlphaRobustMinerParameters paramsR = new AlphaRobustMinerParameters(AlphaMinerVariant.fromString(variant));
        paramsR.setCausalThreshold(request.getDouble(ParamsConstants.CAUSAL_THRESHOLD));
        paramsR.setNoiseThresholdLeastFreq(request.getDouble(ParamsConstants.NOISE_THRESHOLD_LEAST_FREQ));
        paramsR.setNoiseThresholdMostFreq(request.getDouble(ParamsConstants.NOISE_THRESHOLD_MOST_FREQ));
        return paramsR;
    }
}
