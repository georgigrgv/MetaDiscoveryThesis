package org.evaluate;

import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.PNRepResultImpl;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelGeneralization {
    // Use the PM4Py impl, or more accurate the literature formula for Generalization

    // General inf:
    // node instance: all the transitions/events that were involved in the alignments
    // step Types: for all transitions/events the move accordingly
    // trace Index: the indices of traces that share the same result

    public double measureGeneralization(Petrinet petrinet, PNRepResult alignment){
        Map<Transition, Integer> transExecutions = new HashMap<>();

        // Add the possible transitions
        for (Transition t : petrinet.getTransitions()){
            transExecutions.put(t, 0);
        }

        // Only viable alignements and add all executions for a transition from
        // an alignement on all traces
        alignment.stream().filter(SyncReplayResult::isReliable).forEach(syncReplayResult -> {
            List<Object> nodeInstances = syncReplayResult.getNodeInstance();
            int tracesCurrAlign = syncReplayResult.getTraceIndex().size();
            nodeInstances.forEach(instance -> {
                if(instance instanceof Transition){
                    if(transExecutions.containsKey(instance)){
                        int currentCountExec = transExecutions.get(instance);
                        transExecutions.put((Transition) instance, currentCountExec + tracesCurrAlign);
                    }
                }
            });
        });

        // Clear the map out of unsed transitions
        transExecutions.entrySet().removeIf(entry -> entry.getValue() == 0);

        // compute values for formula same as PM4Py
        double invSqrtOccSum = 0.0;
        for(int traceOcc : transExecutions.values()){
            invSqrtOccSum += 1.0 / Math.sqrt(traceOcc);
        }
        for(Transition t: petrinet.getTransitions()){
            if (!transExecutions.containsKey(t)) {
                invSqrtOccSum += 1.0;
            }
        }

        return 1 - invSqrtOccSum / petrinet.getTransitions().size();
    }
}
