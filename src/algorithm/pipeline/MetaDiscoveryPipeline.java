package algorithm.pipeline;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.impl.PluginContextIDImpl;

import java.util.List;

@Plugin(
        name = "ProMPipelinePlugin",
        parameterLabels = {"Event Log"},
        returnLabels = {"Ranked Petri Nets"},
        returnTypes = {List.class}
)
@UIExportPlugin(description = "Event Log Processing and Petri Net Discovery", extension = "pnml")
public class MetaDiscoveryPipeline {

//    @Plugin(name = "Process Event Log with Multiple Preprocessing and Discovery Methods", returnLabels = {"Ranked Petri Nets"}, returnTypes = {List.class}, parameterLabels = {})
//    public List<PetriNetEvaluation> processLog(PluginContext context, XLog log) {
//        List<PetriNetEvaluation> list = null;
//        return list;
//    }
}