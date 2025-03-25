package org.discovery;

import org.deckfour.xes.classification.*;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.discovery.alphaMiner.AlphaMinerVariant;
import org.discovery.heuristicsMiner.HeuristicsMinerVariant;
import org.discovery.inductiveMiner.InductiveMinerVariant;
import org.discovery.utils.ParamsConstants;
import org.json.JSONObject;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaRobustMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;
import org.processmining.causalactivitygraphcreator.algorithms.DiscoverCausalActivityGraphAlgorithm;
import org.processmining.causalactivitygraphcreator.parameters.DiscoverCausalActivityGraphParameters;
import org.processmining.causalactivitymatrixminer.miners.MatrixMinerManager;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.util.Pair;
import org.processmining.hybridilpminer.connections.XLogHybridILPMinerParametersConnection;
import org.processmining.hybridilpminer.dialogs.ConnectionsClassifierEngineAndDefaultConfigurationDialogImpl;
import org.processmining.hybridilpminer.parameters.*;
import org.processmining.hybridilpminer.utils.XLogUtils;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Plugin;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToPetriNetConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.FlexibleHeuristicsMinerPlugin;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.gui.ParametersPanel;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.settings.HeuristicsMinerSettings;
import org.processmining.widgets.wizard.Dialog;
import org.processmining.widgets.wizard.Wizard;
import org.processmining.widgets.wizard.WizardResult;
import processmining.log.LogParser;
import processmining.log.SimpleLog;
import processmining.splitminer.SplitMiner;
import processmining.splitminer.dfgp.DirectlyFollowGraphPlus;
import processmining.splitminer.ui.dfgp.DFGPUIResult;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import static org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin.applyExpress;
import static org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin.discoverWithArtificialStartEnd;

public class DiscoveryAlgorithms {

    public Object[] obtainPetriNetUsingInductiveMiner(XLog log, JSONObject request) throws
            Exception {
        UIPluginContextFactory factory = new UIPluginContextFactory();
        String variant = request.getString(ParamsConstants.VARIANT);
        float threshold = 0.2f;
        boolean existThreshold = false;
        if (request.has(ParamsConstants.NOISE_THRESHOLD)) {
            threshold = request.getFloat(ParamsConstants.NOISE_THRESHOLD);
            existThreshold = true;
        }

        IMMiningDialog dialog = new IMMiningDialog(log);

        Field parametersWrapperField = IMMiningDialog.class.getDeclaredField("p");
        parametersWrapperField.setAccessible(true);
        IMMiningDialog.ParametersWrapper parametersWrapper = (IMMiningDialog.ParametersWrapper) parametersWrapperField.get(dialog);

        parametersWrapper.parameters = Objects.requireNonNull(InductiveMinerVariant.variant(variant, dialog)).getMiningParameters();
        if (existThreshold) {
            parametersWrapper.parameters.setNoiseThreshold(threshold);
        }
        parametersWrapper.variant = InductiveMinerVariant.variant(variant, dialog);
        parametersWrapper.parameters.setClassifier(MiningParameters.getDefaultClassifier());

        return IMPetriNet.minePetriNet(factory.getContext(), log, dialog.getMiningParameters());
    }

    public Object[] obtainPetriNetUsingHeuristicsMiner(XLog log, JSONObject request) throws
            Exception {
        UIPluginContextFactory factory = new UIPluginContextFactory();
        XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
        XLogInfo loginfo = new XLogInfoImpl(log, classifier, log.getClassifiers());
        ParametersPanel parameters = new ParametersPanel(loginfo.getEventClassifiers());
        HeuristicsMinerSettings settings = HeuristicsMinerVariant.createHeuristicsMinerParameters(parameters.getSettings(), request);
        return HeuristicsNetToPetriNetConverter.converter(factory.getContext(),
                FlexibleHeuristicsMinerPlugin.run(factory.getContext(), log, settings, loginfo));
    }

    public Object[] obtainPetriNetUsingAlphaMiner(XLog xLog, JSONObject request) {
        PluginContextFactory factory = new PluginContextFactory();
        Object[] ret = new Object[2];
        XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
        String variant = request.getString(ParamsConstants.VARIANT);

        AlphaMinerParameters parameters;

        if (variant.equals(AlphaMinerVariant.ALPHA_ROBUST.toString())) {
            parameters = AlphaMinerVariant.createAlphaRobustMinerParameters(variant, request);
        } else {
            parameters = AlphaMinerVariant.createAlphaMinerParameters(variant);
        }

        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> miner =
                AlphaMinerFactory.createAlphaMiner(factory.getContext(), xLog, classifier, parameters);

        Pair<Petrinet, Marking> markedNet = miner.run();
        ret[0] = markedNet.getFirst();
        ret[1] = markedNet.getSecond();
        return ret;
    }

    public Object[] obtainPetriNetUsingHybridILPMiner(XLog xLog, JSONObject request) throws Exception {
        UIPluginContextFactory contextFactory = new UIPluginContextFactory();
        Object[] result = null;
        XEventClassifier defaultClassifier = new XEventNameClassifier();
        final String startLabel = "[start>@" + System.currentTimeMillis();
        final String endLabel = "[end]@" + System.currentTimeMillis();
        XLog artifLog = XLogUtils.addArtificialStartAndEnd(xLog, startLabel, endLabel);
        XLogHybridILPMinerParametersImpl params = new XLogHybridILPMinerParametersImpl(contextFactory.getContext());
        params.setNetClass(NetClass.PT_NET);
        params.setEventClassifier(defaultClassifier);

        String objectiveStr = request.getString(ParamsConstants.LP_OBJECTIVE);
        LPObjectiveType objectiveType = null;
        for (LPObjectiveType type : LPObjectiveType.values()) {
            if (type.toString().equals(objectiveStr)) {
                objectiveType = type;
                break;
            }
        }
        if (objectiveType == null) {
            throw new IllegalArgumentException("Invalid LPObjectiveType: " + objectiveStr);
        }
        params.setObjectiveType(objectiveType);

        final String lpFilter = request.getString(ParamsConstants.LP_FILTER);
        LPFilterType selectedFilter = LPFilterType.NONE;
        double threshold = 0.0;
        for (LPFilterType type : LPFilterType.values()) {
            if (type.toString().equals(lpFilter)) {
                selectedFilter = type;
                break;
            }
        }

        // Assign the correct threshold value based on the filter type
        if (selectedFilter == LPFilterType.SLACK_VAR) {
            threshold = request.optDouble(ParamsConstants.SLACK_VARIABLE_FILTER_THRESHOLD, selectedFilter.getDefaultThreshold());
        } else if (selectedFilter == LPFilterType.SEQUENCE_ENCODING) {
            threshold = request.optDouble(ParamsConstants.SEQUENCE_ENCODING_CUTOFF_LEVEL, selectedFilter.getDefaultThreshold());
        }

        params.setFilter(new LPFilter(selectedFilter, threshold));

        DiscoveryStrategy strategy = new DiscoveryStrategy();
        strategy.setDiscoveryStrategyType(DiscoveryStrategyType.TRANSITION_PAIR);
        params.setDiscoveryStrategy(strategy);

        String variableStr = request.getString(ParamsConstants.LP_VARIABLE_TYPE);
        LPVariableType variableType = null;
        for (LPVariableType type : LPVariableType.values()) {
            if (type.toString().equals(variableStr)) {
                variableType = type;
                break;
            }
        }
        params.setVariableType(variableType);

        // set the log to the params
        params.setLog(artifLog);

        DiscoveryStrategy discoveryStrategy = new DiscoveryStrategy();
        DiscoverCausalActivityGraphAlgorithm algorithm = new DiscoverCausalActivityGraphAlgorithm();
        // Experimental
        discoveryStrategy.setDiscoveryStrategyType(DiscoveryStrategyType.CAUSAL_E_VERBEEK);

        DiscoverCausalActivityGraphParameters graphParams = new DiscoverCausalActivityGraphParameters(params.getLog());
        graphParams.setMiner(MatrixMinerManager.getInstance().getMiner(null).getName());
        graphParams.setClassifier(defaultClassifier);
        graphParams.setShowClassifierPanel(false);

        discoveryStrategy.setCausalActivityGraphParameters(graphParams);
        discoveryStrategy.setCausalActivityGraph(algorithm.apply(null, params.getLog(), graphParams));

        params.setDiscoveryStrategy(discoveryStrategy);

        result = discoverWithArtificialStartEnd(contextFactory.getContext(), xLog, artifLog, params);
        return result;
    }

    public Object[] obtainPetriNetUsingSplitMiner(XLog xLog, JSONObject request) {
        try {
            UIPluginContextFactory factory = new UIPluginContextFactory();
            BPMN2PetriNetConverter_Configuration config = new BPMN2PetriNetConverter_Configuration();
            ExportPetriNet exportPetriNet = new ExportPetriNet();
            double eta = request.getDouble(ParamsConstants.ETA);
            double epsilon = request.getDouble(ParamsConstants.EPSILON);
            boolean parallelismFirst = request.getBoolean(ParamsConstants.PARALLELISM_FIRST);
            boolean replaceIORs = request.getBoolean(ParamsConstants.REPLACE_IORS);
            boolean removeLoopActivities = request.getBoolean(ParamsConstants.REMOVE_LOOP_ACTIVITIES);
            SimpleLog cLog = LogParser.getComplexLog(xLog, new XEventNameClassifier());
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(cLog, eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst);
            dfgp.buildDFGP();
            SplitMiner sm = new SplitMiner(replaceIORs, removeLoopActivities);
            BPMNDiagram output = sm.discoverFromDFGP(dfgp);
            return exportPetriNet.convertBPMNToPetriNet(factory.getContext(), output, config);
        }catch (Exception e){
            return new Object[]{"noModel"};
        }
    }
}
