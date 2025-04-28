package org.conformance;

import nl.tue.astar.AStarException;
import org.conformance.alignments.Alignments;
import org.conformance.alignments.AlignmentsResult;
import org.conformance.generalization.ModelGeneralization;
import org.conformance.precision.ModelPrecision;
import org.conformance.simplicity.ModelSimplicity;
import org.deckfour.xes.model.XLog;
import org.discovery.utils.ExportPetriNet;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import java.io.FileNotFoundException;

public class PetriNetEvaluator {

    private static final ModelGeneralization generalization = new ModelGeneralization();

    public static Object[] evaluate(XLog log, PetrinetGraph net, PluginContextFactory factory, int trial)
            throws AStarException, FileNotFoundException {

        Object[] metrics = new Object[6];

        try {
            AlignmentsResult alignmentsResult = Alignments.computeAlignments(log, net);

            if (alignmentsResult.alignment == null || !Alignments.areAlignmentsReliable(alignmentsResult.alignment)) {
                return new Object[]{ConformanceErrors.ALIGNMENT_ERROR};
            }

            double fitness = (Double) alignmentsResult.alignment.getInfo().get(PNRepResult.TRACEFITNESS);
            if (Double.isNaN(fitness) || fitness <= 0.0) {
                return new Object[]{ConformanceErrors.FITNESS_ERROR};
            }
            metrics[0] = fitness;

            metrics[1] = ModelSimplicity.computeSimplicityNodeCount((Petrinet) net, alignmentsResult.logInfo);

            double precision = 0;
            String precisionComputation = null;
            try {
                precision = ModelPrecision.computeEscapingEdgesPrecision((Petrinet) net, factory.getContext(), alignmentsResult.alignment);
                precisionComputation = "EscapingEdges";
            } catch (Exception e) {
                try {
                    precision = ModelPrecision.computeAntiAlignmentsPrecision(factory.getContext(), (Petrinet) net,
                            alignmentsResult.initialMarking, alignmentsResult.finalMarking, log,
                            alignmentsResult.alignment, alignmentsResult.mapping);
                    precisionComputation = "AntiAlignments";
                } catch (Exception ignored) {
                }
            }

            if (Double.isNaN(precision) || precision <= 0.0) {
                return new Object[]{ConformanceErrors.PRECISION_ERROR};
            }

            metrics[2] = precision;
            metrics[3] = calculateF1Score((Double) metrics[0], (Double) metrics[2]);
            metrics[4] = precisionComputation;
            metrics[5] = generalization.measureGeneralization((Petrinet) net, alignmentsResult.alignment);
            // Export only petri nets that have valid results
            ExportPetriNet.exportPetrinetToPNMLorEPNMLFile(net, alignmentsResult.initialMarking, System.getenv("SAVE_RESULTS") + "/" + "trial" + trial + ".pnml");
        } catch (Exception e) {
            e.printStackTrace();
            return new Object[]{ConformanceErrors.CONFORMANCE_ERROR};
        }

        return metrics;
    }

    private static double calculateF1Score(double fitness, double precision) {
        return 2 * (fitness * precision) / (fitness + precision);
    }
}
