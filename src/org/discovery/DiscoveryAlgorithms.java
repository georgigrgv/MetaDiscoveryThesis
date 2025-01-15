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
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.util.Pair;
import org.processmining.hybridilpminer.connections.XLogHybridILPMinerParametersConnection;
import org.processmining.hybridilpminer.dialogs.ConnectionsClassifierEngineAndDefaultConfigurationDialogImpl;
import org.processmining.hybridilpminer.parameters.DiscoveryStrategyType;
import org.processmining.hybridilpminer.parameters.XLogHybridILPMinerParametersImpl;
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
        String variant = request.getString(ParamsConstants.ALGORITHM_VARIANT);
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
        XEventClassifier defaultClassifier;
        if (log.getClassifiers().isEmpty()) {
            XEventClassifier nameCl = new XEventNameClassifier();
            XEventClassifier lifeTransCl = new XEventLifeTransClassifier();
            defaultClassifier = new XEventAndClassifier(nameCl, lifeTransCl);
        } else {
            defaultClassifier = log.getClassifiers().get(0);
        }
        XLogInfo loginfo = new XLogInfoImpl(log, defaultClassifier, log.getClassifiers());
        ParametersPanel parameters = new ParametersPanel(loginfo.getEventClassifiers());
        HeuristicsMinerSettings settings = HeuristicsMinerVariant.createHeuristicsMinerParameters(parameters.getSettings(), request);
        return HeuristicsNetToPetriNetConverter.converter(factory.getContext(),
                FlexibleHeuristicsMinerPlugin.run(factory.getContext(), log, settings, loginfo));
    }

    public Object[] obtainPetriNetUsingAlphaMiner(XLog xLog, JSONObject request) {
        PluginContextFactory factory = new PluginContextFactory();
        Object[] ret = new Object[2];
        XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
        String variant = request.getString(ParamsConstants.ALGORITHM_VARIANT);

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

    public Object[] obtainPetriNetUsingHybridILPMiner(XLog xLog) throws Exception {
        UIPluginContextFactory contextFactory = new UIPluginContextFactory();
        Object[] result = null;
        Collection<XLogHybridILPMinerParametersConnection> connections = new HashSet<>();
        try {
            connections = contextFactory.getContext().getConnectionManager().getConnections(XLogHybridILPMinerParametersConnection.class,
                    contextFactory.getContext(), xLog);
        } catch (ConnectionCannotBeObtained e) {
        }
        XEventClassifier defaultClassifier;
        if (xLog.getClassifiers().isEmpty()) {
            XEventClassifier nameCl = new XEventNameClassifier();
            XEventClassifier lifeTransCl = new XEventLifeTransClassifier();
            defaultClassifier = new XEventAndClassifier(nameCl, lifeTransCl);
        } else {
            defaultClassifier = xLog.getClassifiers().get(0);
        }
        final String startLabel = "[start>@" + System.currentTimeMillis();
        final String endLabel = "[end]@" + System.currentTimeMillis();
        XLog artifLog = XLogUtils.addArtificialStartAndEnd(xLog, startLabel, endLabel);
        Dialog<XLogHybridILPMinerParametersImpl> firstDialog = new ConnectionsClassifierEngineAndDefaultConfigurationDialogImpl(
                contextFactory.getContext(), null, artifLog, connections);
        WizardResult<XLogHybridILPMinerParametersImpl> wizardResult = Wizard.show(contextFactory.getContext(), firstDialog);
        XLogHybridILPMinerParametersImpl params = wizardResult.getParameters();
        params.setEventClassifier(defaultClassifier);
        result = discoverWithArtificialStartEnd(contextFactory.getContext(), xLog, artifLog, params);
        if (!params.getDiscoveryStrategy().getDiscoveryStrategyType()
                .equals(DiscoveryStrategyType.CAUSAL_E_VERBEEK)) {
            Connection paramsConnection = new XLogHybridILPMinerParametersConnection(xLog, params);
            contextFactory.getContext().getConnectionManager().addConnection(paramsConnection);
        }
        return result;
    }

    public Object[] obtainPetriNetUsingSplitMiner(XLog xLog, JSONObject request) {
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
    }
}
