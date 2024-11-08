package org.discovery;

import org.processmining.contexts.uitopia.PluginContextFactory;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;
import org.processmining.contexts.uitopia.UIPluginContextFactory;
import org.processmining.framework.packages.PackageManager;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.heuristics.HeuristicsNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree2AcceptingPetriNet;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduceParametersForPetriNet;
import org.processmining.plugins.InductiveMiner.reduceacceptingpetrinet.ReduceAcceptingPetriNetKeepLanguage;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToPetriNetConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.HeuristicsMiner;
import org.processmining.plugins.ilpminer.ILPMiner;
import org.processmining.plugins.inductiveminer2.logs.IMLog;
import org.processmining.plugins.inductiveminer2.mining.InductiveMiner;
import org.processmining.plugins.inductiveminer2.mining.MiningParameters;
import org.processmining.plugins.inductiveminer2.plugins.InductiveMinerPlugin;

public class DiscoveryAlgorithms {

    public Object[] obtainPetriNetUsingInductiveMiner(XLog log) throws
            Exception {
        Object[] ret = new Object[2];
        MiningParameters parameters = new MyMiningParameters();
        InductiveMinerPlugin im = new InductiveMinerPlugin();
        IMLog imlog = parameters.getIMLog(log);
        EfficientTree tree = InductiveMiner.mineEfficientTree(imlog, parameters,
                () -> false);
        AcceptingPetriNet petri = null;
        if (tree != null) {
            EfficientTreeReduce.reduce(tree, new
                    EfficientTreeReduceParametersForPetriNet(false));
            petri = EfficientTree2AcceptingPetriNet.convert(tree);
        }
        PackageManager.Canceller canceller = () -> false;

        ReduceAcceptingPetriNetKeepLanguage.reduce(petri, canceller);

        Petrinet net = petri.getNet();
        Marking marking = petri.getInitialMarking();
        ret[0] = net;
        ret[1] = marking;

        return ret;
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
        return null;
    }
}
