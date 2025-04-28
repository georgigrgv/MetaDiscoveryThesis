package org.discovery;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.discovery.alphaMiner.AlphaMinerVariant;
import org.discovery.heuristicsMiner.HeuristicsMinerVariant;
import org.discovery.inductiveMiner.InductiveMinerVariant;
import org.discovery.utils.ExportPetriNet;
import org.discovery.utils.ParamsConstants;
import org.json.JSONObject;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.causalactivitygraphcreator.algorithms.DiscoverCausalActivityGraphAlgorithm;
import org.processmining.causalactivitygraphcreator.parameters.DiscoverCausalActivityGraphParameters;
import org.processmining.causalactivitymatrixminer.miners.MatrixMinerManager;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.util.Pair;
import org.processmining.hybridilpminer.parameters.*;
import org.processmining.hybridilpminer.utils.XLogUtils;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToPetriNetConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.FlexibleHeuristicsMinerPlugin;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.gui.ParametersPanel;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.settings.HeuristicsMinerSettings;
import org.processmining.plugins.petrinet.reduction.Murata;
import processmining.log.LogParser;
import processmining.log.SimpleLog;
import processmining.splitminer.SplitMiner;
import processmining.splitminer.dfgp.DirectlyFollowGraphPlus;
import processmining.splitminer.ui.dfgp.DFGPUIResult;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin.discoverWithArtificialStartEnd;

public class DiscoveryAlgorithms {

    private final Murata murata = new Murata();

    public Object[] obtainPetriNetUsingInductiveMiner(XLog log, JSONObject request) throws Exception {
        UIPluginContextFactory factory = createUIContextFactory();
        IMMiningDialog dialog = new IMMiningDialog(log);

        Field parametersWrapperField = IMMiningDialog.class.getDeclaredField("p");
        parametersWrapperField.setAccessible(true);
        IMMiningDialog.ParametersWrapper parametersWrapper = (IMMiningDialog.ParametersWrapper) parametersWrapperField.get(dialog);

        String variant = request.getString(ParamsConstants.VARIANT);
        parametersWrapper.variant = Objects.requireNonNull(InductiveMinerVariant.variant(variant, dialog));
        parametersWrapper.parameters = parametersWrapper.variant.getMiningParameters();

        if (request.has(ParamsConstants.NOISE_THRESHOLD)) {
            float threshold = request.getFloat(ParamsConstants.NOISE_THRESHOLD);
            parametersWrapper.parameters.setNoiseThreshold(threshold);
        }

        parametersWrapper.parameters.setClassifier(MiningParameters.getDefaultClassifier());

        Object[] petriNet = IMPetriNet.minePetriNet(factory.getContext(), log, dialog.getMiningParameters());
        return murata.runPreserveBehavior(null, (Petrinet) petriNet[0], (Marking) petriNet[1]);
    }

    public Object[] obtainPetriNetUsingHeuristicsMiner(XLog log, JSONObject request) throws Exception {
        UIPluginContextFactory factory = createUIContextFactory();
        XLogInfo loginfo = new XLogInfoImpl(log, getDefaultClassifier(), log.getClassifiers());
        ParametersPanel parameters = new ParametersPanel(loginfo.getEventClassifiers());

        HeuristicsMinerSettings settings = HeuristicsMinerVariant.createHeuristicsMinerParameters(parameters.getSettings(), request);
        Object[] petriNet = HeuristicsNetToPetriNetConverter.converter(factory.getContext(),
                FlexibleHeuristicsMinerPlugin.run(factory.getContext(), log, settings, loginfo));
        return murata.runPreserveBehavior(null, (Petrinet) petriNet[0], (Marking) petriNet[1]);
    }

    public Object[] obtainPetriNetUsingAlphaMiner(XLog xLog, JSONObject request) {
        PluginContextFactory factory = new PluginContextFactory();
        XEventClassifier classifier = getDefaultClassifier();
        String variant = request.getString(ParamsConstants.VARIANT);

        AlphaMinerParameters parameters = variant.equals(AlphaMinerVariant.ALPHA_ROBUST.toString())
                ? AlphaMinerVariant.createAlphaRobustMinerParameters(variant, request)
                : AlphaMinerVariant.createAlphaMinerParameters(variant);

        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> miner =
                AlphaMinerFactory.createAlphaMiner(factory.getContext(), xLog, classifier, parameters);

        Pair<Petrinet, Marking> markedNet = miner.run();
        return new Object[]{markedNet.getFirst(), markedNet.getSecond()};
    }

    public Object[] obtainPetriNetUsingHybridILPMiner(XLog xLog, JSONObject request) throws Exception {
        UIPluginContextFactory factory = createUIContextFactory();
        XEventClassifier classifier = new XEventNameClassifier();
        String startLabel = "[start>@" + System.currentTimeMillis();
        String endLabel = "[end]@" + System.currentTimeMillis();

        XLog artifLog = XLogUtils.addArtificialStartAndEnd(xLog, startLabel, endLabel);
        XLogHybridILPMinerParametersImpl params = new XLogHybridILPMinerParametersImpl(factory.getContext());
        params.setNetClass(NetClass.PT_NET);
        params.setEventClassifier(classifier);
        params.setObjectiveType(parseEnum(LPObjectiveType.class, request.getString(ParamsConstants.LP_OBJECTIVE)));
        params.setVariableType(parseEnum(LPVariableType.class, request.getString(ParamsConstants.LP_VARIABLE_TYPE)));
        params.setFilter(createLPFilter(request));

        DiscoveryStrategy discoveryStrategy = createDiscoveryStrategy(request, artifLog, classifier);
        params.setDiscoveryStrategy(discoveryStrategy);
        params.setLog(artifLog);

        return discoverWithArtificialStartEnd(factory.getContext(), xLog, artifLog, params);
    }

    public Object[] obtainPetriNetUsingSplitMiner(XLog xLog, JSONObject request) throws ConnectionCannotBeObtained {
        UIPluginContextFactory factory = createUIContextFactory();
        BPMN2PetriNetConverter_Configuration config = new BPMN2PetriNetConverter_Configuration();
        ExportPetriNet exportPetriNet = new ExportPetriNet();

        double eta = request.getDouble(ParamsConstants.ETA);
        double epsilon = request.getDouble(ParamsConstants.EPSILON);
        boolean parallelismFirst = request.getBoolean(ParamsConstants.PARALLELISM_FIRST);
        boolean replaceIORs = request.getBoolean(ParamsConstants.REPLACE_IORS);
        boolean removeLoopActivities = request.getBoolean(ParamsConstants.REMOVE_LOOP_ACTIVITIES);

        SimpleLog cLog = LogParser.getComplexLog(xLog, getDefaultClassifier());
        DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(cLog, eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst);
        dfgp.buildDFGP();

        SplitMiner sm = new SplitMiner(replaceIORs, removeLoopActivities);
        BPMNDiagram output = sm.discoverFromDFGP(dfgp);
        Object[] petriNet = exportPetriNet.convertBPMNToPetriNet(factory.getContext(), output, config);

        return murata.runPreserveBehavior(null, (Petrinet) petriNet[0], (Marking) petriNet[1]);
    }

    private UIPluginContextFactory createUIContextFactory() {
        return new UIPluginContextFactory();
    }

    private XEventClassifier getDefaultClassifier() {
        return XLogInfoImpl.NAME_CLASSIFIER;
    }

    // Helper methods for the HILP Miner

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value) {
        for (T constant : enumType.getEnumConstants()) {
            if (constant.toString().equals(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(value);
    }

    private LPFilter createLPFilter(JSONObject request) {
        LPFilterType filterType = parseEnum(LPFilterType.class, request.getString(ParamsConstants.LP_FILTER));
        double threshold = 0.0;
        if (filterType == LPFilterType.SLACK_VAR) {
            threshold = request.optDouble(ParamsConstants.SLACK_VARIABLE_FILTER_THRESHOLD, filterType.getDefaultThreshold());
        } else if (filterType == LPFilterType.SEQUENCE_ENCODING) {
            threshold = request.optDouble(ParamsConstants.SEQUENCE_ENCODING_CUTOFF_LEVEL, filterType.getDefaultThreshold());
        }
        return new LPFilter(filterType, threshold);
    }

    private DiscoveryStrategy createDiscoveryStrategy(JSONObject request, XLog log, XEventClassifier classifier) {
        DiscoveryStrategy strategy = new DiscoveryStrategy();
        DiscoverCausalActivityGraphAlgorithm algorithm = new DiscoverCausalActivityGraphAlgorithm();
        strategy.setDiscoveryStrategyType(DiscoveryStrategyType.CAUSAL_E_VERBEEK);

        DiscoverCausalActivityGraphParameters graphParams = new DiscoverCausalActivityGraphParameters(log);
        graphParams.setMiner(MatrixMinerManager.getInstance().getMiner(request.getString(ParamsConstants.DISCOVERY_STRATEGY)).getName());
        graphParams.setClassifier(classifier);
        graphParams.setShowClassifierPanel(false);
        strategy.setCausalActivityGraphParameters(graphParams);
        strategy.setCausalActivityGraph(algorithm.apply(null, log, graphParams));
        return strategy;
    }
}
