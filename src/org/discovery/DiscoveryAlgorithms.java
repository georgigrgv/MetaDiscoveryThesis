package org.discovery;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.framework.packages.PackageManager;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.heuristics.HeuristicsNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree2AcceptingPetriNet;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduceParametersForPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.IM;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.InductiveMiner.reduceacceptingpetrinet.ReduceAcceptingPetriNetKeepLanguage;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToPetriNetConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.HeuristicsMiner;
import org.processmining.plugins.ilpminer.ILPMiner;
import org.processmining.plugins.inductiveminer2.logs.IMLog;
import org.processmining.plugins.inductiveminer2.mining.InductiveMiner;
import org.processmining.plugins.inductiveminer2.mining.MiningParameters;
import org.processmining.plugins.inductiveminer2.plugins.InductiveMinerPlugin;
import processmining.log.LogParser;
import processmining.log.SimpleLog;
import processmining.splitminer.SplitMiner;
import processmining.splitminer.dfgp.DirectlyFollowGraphPlus;
import processmining.splitminer.ui.dfgp.DFGPUIResult;

public class DiscoveryAlgorithms {

    public Object[] obtainPetriNetUsingInductiveMiner(XLog log) throws
            Exception {
        UIPluginContextFactory factory = new UIPluginContextFactory();
        IMMiningDialog dialog = new IMMiningDialog(log);
        return IMPetriNet.minePetriNet(factory.getContext(), log, dialog.getMiningParameters());
    }

    public Object[] obtainPetriNetUsingHeuristicsMiner(XLog log) throws
            Exception {
        PluginContextFactory factory = new PluginContextFactory();
        HeuristicsMiner miner = new HeuristicsMiner(factory.getContext(), log);
        HeuristicsNet heuristicsNet = miner.mine();
        return HeuristicsNetToPetriNetConverter.converter(factory.getContext(), heuristicsNet);
    }

    public Object[] obtainPetriNetUsingAlphaMiner(XLog xLog) {
        PluginContextFactory factory = new PluginContextFactory();
        Object[] ret = new Object[2];
        XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
        AlphaMinerParameters parameters = new AlphaMinerParameters();
        parameters.setVersion(AlphaVersion.CLASSIC);
        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ?
                extends AlphaMinerParameters> miner = AlphaMinerFactory
                .createAlphaMiner(factory.getContext(), xLog, classifier, parameters);
        Pair<Petrinet, Marking> markedNet = miner.run();
        ret[0] = markedNet.getFirst();
        ret[1] = markedNet.getSecond();
        return ret;
    }

    public Object[] obtainPetriNetUsingILPMiner(XLog xLog) throws Exception {
        ILPMiner miner = new ILPMiner();
        UIPluginContextFactory factory = new UIPluginContextFactory();
        return miner.doILPMining(factory.getContext(), xLog);
    }

    public Object[] obtainPetriNetUsingSplitMiner(XLog xLog) {
        Object[] ret = new Object[1];
        double eta = 1.0;
        double epsilon = 0.5;
        boolean parallelismFirst = true;
        boolean replaceIORs = false;
        boolean removeLoopActivities = false;
        SimpleLog cLog = LogParser.getComplexLog(xLog, new XEventNameClassifier());
        DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(cLog, eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst);
        dfgp.buildDFGP();
        SplitMiner sm = new SplitMiner(replaceIORs, removeLoopActivities);
        BPMNDiagram output = sm.discoverFromDFGP(dfgp);
        ret[0] = output;
        return ret;
    }
}
