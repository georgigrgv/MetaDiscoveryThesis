package org.evaluate;

import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.discovery.ExportPetriNet;
import org.pipeline.MetaDiscoveryPipeline;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.parameters.ConvertPetriNetToAcceptingPetriNetParameters;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentParameterUI;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentPlugin;
import org.processmining.contexts.uitopia.PluginContextFactory;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.PNRepResultImpl;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.precision.models.EscapingEdgesPrecisionResult;
import org.processmining.precision.plugins.EscapingEdgesPrecisionPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class PetriNetEvaluator {
    public static ModelGeneralization generalization = new ModelGeneralization();

    public static Object[] executeAlignments(XLog log, PetrinetGraph net, PluginContextFactory factory, int trial)
            throws AStarException, IllegalTransitionException, FileNotFoundException {
        Object[] metrics = new Object[6];
        try {
            final Marking initialMarking = PetrinetUtils.guessInitialMarking(net);
            final Marking finalMarking = PetrinetUtils.guessFinalMarking(net);
            XEventClass evClassDummy = new XEventClass("DUMMY",
                    -1);
            XEventClassifier eventClassifier = XLogInfoImpl.NAME_CLASSIFIER;
            TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, evClassDummy);
            XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);
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
            PNRepResultImpl alignment = (PNRepResultImpl) new PNLogReplayer().replayLog(null, net, log, mapping,
                    new PetrinetReplayerWithILP(), parameter);
            double fitness = 0;
            if (alignment != null && alignment.getInfo() != null && alignment.getInfo().containsKey(PNRepResult.TRACEFITNESS) && checkIfAlignemntsAreReliable(alignment)) {
                fitness = (Double) alignment.getInfo().get(PNRepResult.TRACEFITNESS);
            }
            if(Double.isNaN(fitness) || fitness <= 0.0){
                return new Object[]{"Alignments/Fitness could not be computed. Proceed with next trial"};
            }
            metrics[0] = fitness;

            // Simplicity
            metrics[1] = ModelSimplicity.calculateHybridSimplicity((Petrinet) net, log, 0.5);

            // Try to compute precision
            double precision = 0;
            String precisionComputation = null;
            try {
                precision =  computeEEPrecision((Petrinet) net, context, alignment);
                precisionComputation = "EscapingEdges";
            }catch (Exception ignored){}
            if(Double.isNaN(precision) || precision <= 0.0){
                try{
                    precision =  computeAAPrecision(context, (Petrinet) net, initialMarking, finalMarking, log, alignment, mapping);
                    precisionComputation = "AntiAlignments";
                }catch (Exception a){
                    a.printStackTrace();
                    ExportPetriNet.exportPetrinetToPNMLorEPNMLFile(net, initialMarking, System.getenv("SAVE_RESULTS") + "/" + "trial" + trial + ".pnml");
                    return metrics;
                }
            }
            if(Double.isNaN(precision) || precision <= 0.0){
                ExportPetriNet.exportPetrinetToPNMLorEPNMLFile(net, initialMarking, System.getenv("SAVE_RESULTS") + "/" + "trial" + trial + ".pnml");
                return metrics;
            }
            metrics[2] = precision;
            metrics[3] = calculateF1Score((Double) metrics[0], (Double) metrics[2]);
            // Save what plugin computed the precision
            metrics[4] = precisionComputation;
            metrics[5] = generalization.measureGeneralization((Petrinet) net, alignment);
//            boolean conformanceResults = Arrays.stream(metrics).allMatch(n -> (double) n > 0);
//            if (conformanceResults) {
                ExportPetriNet.exportPetrinetToPNMLorEPNMLFile(net, initialMarking, System.getenv("SAVE_RESULTS") + "/" + "trial" + trial + ".pnml");
//            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            return new Object[]{e.toString()};
        }
        return metrics;
    }

    public static double calculateF1Score(double fitness, double precision) {
        return 2 * (fitness * precision) / (fitness + precision);
    }
    public static AcceptingPetriNet convertPetriNet(Petrinet net){
        ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
        ConvertPetriNetToAcceptingPetriNetParameters accpetriNetParams = new ConvertPetriNetToAcceptingPetriNetParameters(net);
        return convertPetriNetToAcceptingPetriNetPlugin.apply(null, net, accpetriNetParams);
    }
    public static double computeEEPrecision(Petrinet net, PluginContext context, PNRepResult alignment) throws IllegalTransitionException {
        EscapingEdgesPrecisionPlugin precisionPlugin = new EscapingEdgesPrecisionPlugin();
        AcceptingPetriNet acceptingPetriNet = convertPetriNet(net);
        EscapingEdgesPrecisionResult precisionResult = precisionPlugin.runDefault(context, alignment, acceptingPetriNet);
        return precisionResult.getPrecision();
    }
    public static double computeAAPrecision(PluginContext context, Petrinet net, Marking initialMarking, Marking finalMarking, XLog log,
                                            PNRepResult alignment, TransEvClassMapping mapping){
        AntiAlignmentPlugin plugin = new AntiAlignmentPlugin();
        AntiAlignmentParameterUI ui = new AntiAlignmentParameterUI();
        PNRepResult replayRes = plugin.basicCodeStructureWithAlignments(context.getProgress(), net, initialMarking,
                finalMarking, log, alignment, mapping, ui.getParameters());
        Map<String, Object> results = replayRes.getInfo();
        return Double.parseDouble((String) results.get("Precision"));
    }
    public static boolean checkIfAlignemntsAreReliable(PNRepResult alignments) {
        int reliable = 0;
        for (SyncReplayResult traceAlignment : alignments) {
            if (traceAlignment.isReliable()) {
                // only go through alignments that are reliable
                reliable++;
            }
        }
        return reliable > 0;
    }
}
