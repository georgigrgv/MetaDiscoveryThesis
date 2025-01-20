package org.evaluate;

import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.PNRepResultImpl;
import org.processmining.precision.models.EscapingEdgesPrecisionResult;
import org.processmining.precision.plugins.EscapingEdgesPrecisionPlugin;
import org.processmining.tbr.TokenBasedReplay;

import java.util.Map;


public class PetriNetEvaluator {

    public static double[] executeAlignments(XLog log, PetrinetGraph net, PluginContextFactory factory) throws AStarException, IllegalTransitionException {
        double[] metrics = new double[3];
        final Marking initialMarking = PetrinetUtils.guessInitialMarking(net);
        final Marking finalMarking = PetrinetUtils.guessFinalMarking(net);
        XEventClass evClassDummy = new XEventClass("DUMMY",
                -1);
        TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER, evClassDummy);
        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER);
        for (XEventClass ec : logInfo.getEventClasses().getClasses()) {
            for (Transition t : net.getTransitions()) {
                String n = ec.toString();
                if (n.contains("+")) {
                    n = n.substring(0, n.indexOf("+"));
                }
                if (t.getLabel().equals(n)) {
                    mapping.put(t, ec);
                }
            }
        }
        CostBasedCompleteParam parameter = new
                CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
                evClassDummy, net.getTransitions(), 1, 1);
        parameter.setGUIMode(false);
        parameter.setCreateConn(false);
        parameter.setInitialMarking(initialMarking);
        parameter.setFinalMarkings(finalMarking);
        parameter.setMaxNumOfStates(200000);
        PluginContext context = factory.getContext();
        PNRepResultImpl result = (PNRepResultImpl) new PNLogReplayer().replayLog(null, net, log, mapping,
                new PetrinetReplayerWithoutILP(), parameter);
        Double fitness = (Double) result.getInfo().get(PNRepResult.TRACEFITNESS);
        metrics[0] = ((fitness != null && fitness > 0.0) || Double.isNaN(fitness)) ? fitness : -1.0;
        EscapingEdgesPrecisionPlugin precisionPlugin = new EscapingEdgesPrecisionPlugin();
        ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
        AcceptingPetriNet acceptingPetriNet = convertPetriNetToAcceptingPetriNetPlugin.runDefault(context, (Petrinet) net);

        EscapingEdgesPrecisionResult precisionResult = precisionPlugin.runDefault(context, result, acceptingPetriNet);
        double precision = precisionResult.getPrecision();
        metrics[1] = !Double.isNaN(precision) ? precision : -1.0;

        metrics[2] = calculateF1Score(metrics[0], metrics[1]);
        return metrics;
    }

// TODO: REMOVE

//    public static double[] tokenBasedReplayFitness(XLog log, Petrinet petriNet, PluginContextFactory context) {
//        ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
//        AcceptingPetriNet acceptingPetriNet = convertPetriNetToAcceptingPetriNetPlugin.runDefault(context.getContext(), petriNet);
//        double fitness = TokenBasedReplay.apply(context.getContext(), log, acceptingPetriNet).logFitness;
//        return new double[]{fitness, 0, calculateF1Score(fitness, 0)};
//    }
//
//    public static boolean checkForMarkings(Petrinet petrinet) {
//        final Marking initialMarking = PetrinetUtils.guessInitialMarking(petrinet);
//        final Marking finalMarking = PetrinetUtils.guessFinalMarking(petrinet);
//
//        // Log the problem and continue with TokenBasedReplay
//        return initialMarking != null && finalMarking != null;
//    }

    public static double calculateF1Score(double fitness, double precision) {
        return 2 * (fitness * precision) / (fitness + precision);
    }
}
