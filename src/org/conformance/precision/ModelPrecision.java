package org.conformance.precision;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.parameters.ConvertPetriNetToAcceptingPetriNetParameters;
import org.processmining.acceptingpetrinet.plugins.ConvertPetriNetToAcceptingPetriNetPlugin;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentParameterUI;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.precision.models.EscapingEdgesPrecisionResult;
import org.processmining.precision.plugins.EscapingEdgesPrecisionPlugin;

import java.util.Map;

public class ModelPrecision {

    private ModelPrecision(){
    }

    public static AcceptingPetriNet convertPetriNet(Petrinet net) {
        ConvertPetriNetToAcceptingPetriNetPlugin convertPetriNetToAcceptingPetriNetPlugin = new ConvertPetriNetToAcceptingPetriNetPlugin();
        ConvertPetriNetToAcceptingPetriNetParameters accpetriNetParams = new ConvertPetriNetToAcceptingPetriNetParameters(net);
        return convertPetriNetToAcceptingPetriNetPlugin.apply(null, net, accpetriNetParams);
    }

    // Escaping Edges - default
    public static double computeEscapingEdgesPrecision(Petrinet net, PluginContext context, PNRepResult alignment) throws IllegalTransitionException {
        EscapingEdgesPrecisionPlugin precisionPlugin = new EscapingEdgesPrecisionPlugin();
        AcceptingPetriNet acceptingPetriNet = convertPetriNet(net);
        EscapingEdgesPrecisionResult precisionResult = precisionPlugin.runDefault(context, alignment, acceptingPetriNet);
        return precisionResult.getPrecision();
    }

    // Anti Alignments - fallback/additional
    public static double computeAntiAlignmentsPrecision(PluginContext context, Petrinet net, Marking initialMarking, Marking finalMarking, XLog log,
                                            PNRepResult alignment, TransEvClassMapping mapping) {
        AntiAlignmentPlugin plugin = new AntiAlignmentPlugin();
        AntiAlignmentParameterUI ui = new AntiAlignmentParameterUI();
        PNRepResult replayRes = plugin.basicCodeStructureWithAlignments(context.getProgress(), net, initialMarking,
                finalMarking, log, alignment, mapping, ui.getParameters());
        Map<String, Object> results = replayRes.getInfo();
        return Double.parseDouble((String) results.get("Precision"));
    }
}
