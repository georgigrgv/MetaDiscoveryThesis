package org.evaluate;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.HashSet;

public class ModelSimplicity {

    public static double calculateHybridSimplicity(Petrinet petriNet, XLog eventLog, double alpha) {
        HashSet<String> eventClasses = extractEventClasses(eventLog);

        double labelSimplicity = calculateLabelBasedSimplicity(petriNet, eventClasses);
        double edgeSimplicity = calculateEdgeBasedSimplicity(petriNet, eventClasses);

        return alpha * labelSimplicity + (1.0 - alpha) * edgeSimplicity;
    }

    public static double calculateLabelBasedSimplicity(Petrinet petriNet, HashSet<String> eventClasses) {
        int nodesInPetriNet = countNodes(petriNet);
        int duplicateActivities = countDuplicateActivities(petriNet);
        int missingActivities = countMissingActivities(petriNet, eventClasses);
        int eventClassCount = eventClasses.size();

        if ((nodesInPetriNet + eventClassCount) == 0) return 1.0;

        return 1.0 - ((double) (duplicateActivities + missingActivities) / (nodesInPetriNet + eventClassCount));
    }

    public static double calculateEdgeBasedSimplicity(Petrinet petriNet, HashSet<String> eventClasses) {
        int edgeCount = petriNet.getEdges().size();
        int eventCount = eventClasses.size();
        if (eventCount == 0) return 1.0;
        return 1.0 - Math.min((double) edgeCount / eventCount, 1.0);
    }

    private static int countNodes(Petrinet petriNet) {
        return petriNet.getPlaces().size() + petriNet.getTransitions().size();
    }

    private static int countDuplicateActivities(Petrinet petriNet) {
        HashMap<String, Integer> activityCount = new HashMap<>();
        for (Transition transition : petriNet.getTransitions()) {
            if (!transition.isInvisible()) {
                String label = transition.getLabel();
                activityCount.put(label, activityCount.getOrDefault(label, 0) + 1);
            }
        }
        int duplicates = 0;
        for (int count : activityCount.values()) {
            if (count > 1) {
                duplicates += (count - 1);
            }
        }
        return duplicates;
    }

    private static int countMissingActivities(Petrinet petriNet, HashSet<String> eventClasses) {
        HashSet<String> modelActivities = new HashSet<>();
        for (Transition transition : petriNet.getTransitions()) {
            if (!transition.isInvisible()) {
                modelActivities.add(transition.getLabel());
            }
        }
        int missing = 0;
        for (String event : eventClasses) {
            if (!modelActivities.contains(event)) {
                missing++;
            }
        }
        return missing;
    }

    private static HashSet<String> extractEventClasses(XLog eventLog) {
        HashSet<String> eventClasses = new HashSet<>();
        for (XTrace trace : eventLog) {
            for (XEvent event : trace) {
                eventClasses.add(event.getAttributes().get("concept:name").toString());
            }
        }
        return eventClasses;
    }
}
