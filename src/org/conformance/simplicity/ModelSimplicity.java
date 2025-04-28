package org.conformance.simplicity;

import org.deckfour.xes.info.XLogInfo;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModelSimplicity {

    private ModelSimplicity() {
    }

    public static double computeSimplicityEdges(Petrinet net, double k) {
        double totalDegree = 0;
        int nodeCount = 0;

        for (PetrinetNode node : net.getNodes()) {
            int inDegree = net.getInEdges(node).size();
            int outDegree = net.getOutEdges(node).size();
            totalDegree += inDegree + outDegree;
            nodeCount++;
        }

        double meanDegree = nodeCount > 0 ? totalDegree / nodeCount : 0;
        double adjusted = Math.max(meanDegree - k, 0);
        return 1.0 / (1.0 + adjusted);
    }

    public static double computeSimplicity(Petrinet net) {
        return computeSimplicityEdges(net, 2.0); // default k=2
    }

    public static double computeSimplicityNodeCount(Petrinet petriNet, XLogInfo eventLogInfo) {
        Set<String> eventClasses = new HashSet<>();
        eventLogInfo.getEventClasses().getClasses().forEach(ec -> eventClasses.add(ec.getId()));
        HashSet<String> modelActivities = new HashSet<>();
        Map<String, Integer> activityCount = new HashMap<>();

        for (Transition t : petriNet.getTransitions()) {
            if (!t.isInvisible()) {
                String label = t.getLabel();
                modelActivities.add(label);
                activityCount.put(label, activityCount.getOrDefault(label, 0) + 1);
            }
        }

        int duplicateActivities = 0;
        for (int count : activityCount.values()) {
            if (count > 1) {
                duplicateActivities += (count - 1);
            }
        }
        int missingActivities = 0;
        for (String logActivity : eventClasses) {
            if (!modelActivities.contains(logActivity)) {
                missingActivities++;
            }
        }

        int nodeCount = petriNet.getTransitions().size() + petriNet.getPlaces().size();
        int totalDenominator = nodeCount + eventClasses.size();

        if (totalDenominator == 0) return 1.0;

        return 1.0 - ((double) (duplicateActivities + missingActivities) / totalDenominator);
    }
}
