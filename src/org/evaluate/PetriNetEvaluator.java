package org.evaluate;

import com.kitfox.svg.A;
import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.discovery.ExportPetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentParameters;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentPlugin;
import org.processmining.antialignments.ilp.antialignment.HeuristicAntiAlignmentAlgorithm;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
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
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGen;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.precision.models.EscapingEdgesPrecisionResult;
import org.processmining.precision.plugins.EscapingEdgesPrecisionPlugin;

import java.io.FileNotFoundException;
import java.util.Arrays;


public class PetriNetEvaluator {

    public static double[] executeAlignments(XLog log, PetrinetGraph net, PluginContextFactory factory, int trial) throws AStarException, IllegalTransitionException, FileNotFoundException {
        double[] metrics = new double[5];
        try{
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

            AlignmentPrecGen precGen = new AlignmentPrecGen();
            AlignmentPrecGenRes res = precGen.measureConformanceAssumingCorrectAlignment(context, mapping, result, (Petrinet) net, initialMarking, false);
            double precision = res.getPrecision();
            double generalization = res.getGeneralization();

            metrics[1] = precision > 0.0 ? precision : -1.0;
            metrics[2] = calculateF1Score(metrics[0], metrics[1]);
            metrics[3] = ModelSimplicity.calculateSimplicity((Petrinet) net, log);
            metrics[4] = generalization > 0.0 ? generalization : -1.0;

            boolean conformanceResults = Arrays.stream(metrics).allMatch(n -> n > 0);
            if (conformanceResults) {
                ExportPetriNet.exportPetrinetToPNMLorEPNMLFile(net, initialMarking, System.getenv("SAVE_RESULTS") + "/" +"trial"+trial+".pnml");
            }

        } catch (NullPointerException e){
            return new double[] { -1.0, -1.0, -1.0, -1.0, -1.0 };
        }

        return metrics;
    }

    public static double calculateF1Score(double fitness, double precision) {
        return 2 * (fitness * precision) / (fitness + precision);
    }
}
