package playground.pipeline;

import org.deckfour.xes.model.XLog;
import org.jbpt.petri.PetriNet;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.UIExportPlugin;
import org.processmining.plugins.InductiveMiner.mining.Miner;
import org.processmining.plugins.inductiveminer2.mining.InductiveMiner;
import org.processmining.plugins.petrinet.PetriNet;
import org.processmining.plugins.petrinet.replayfitness.ConformanceChecker;
import org.processmining.plugins.petrinet.replayfitness.PrecisionEvaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Plugin(
        name = "ProMPipelinePlugin",
        parameterLabels = {"Event Log"},
        returnLabels = {"Ranked Petri Nets"},
        returnTypes = {List.class}
)
@UIExportPlugin(description = "Event Log Processing and Petri Net Discovery", extension = "pnml")
public class MetaDiscoveryPipeline {

    @Plugin(
            name = "Process Event Log with Multiple Preprocessing and Discovery Methods", returnLabels = {"Ranked Petri Nets"}, returnTypes = {List.class}, parameterLabels = {})
    public List<PetriNetEvaluation> processLog(PluginContext context, XLog log) {

    }
}