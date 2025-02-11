package org.evaluate;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.HashSet;

public class ModelSimplicity {

    public static double calculateSimplicity(Petrinet petriNet, XLog eventLog) {
        int nodesInPetriNet = countNodes(petriNet);
        int duplicateActivities = countDuplicateActivities(petriNet);
        int missingActivities = countMissingActivities(petriNet, eventLog);
        int eventClassCount = extractEventClasses(eventLog).size();

        if ((nodesInPetriNet + eventClassCount) == 0) return 1.0; // Avoid division by zero

        return 1 - ((double) (duplicateActivities + missingActivities) / (nodesInPetriNet + eventClassCount));
    }

    private static int countNodes(Petrinet petriNet) {
        return petriNet.getPlaces().size() + petriNet.getTransitions().size();
    }

    private static int countDuplicateActivities(Petrinet petriNet) {
        HashMap<String, Integer> activityCount = new HashMap<>();
        int duplicates = 0;
        for (Transition transition : petriNet.getTransitions()) {
            if (!transition.isInvisible()) { // Ignore silent transitions
                String label = transition.getLabel();
                activityCount.put(label, activityCount.getOrDefault(label, 0) + 1);
                if (activityCount.get(label) > 1) duplicates++;
            }
        }
        return duplicates;
    }

    private static int countMissingActivities(Petrinet petriNet, XLog eventLog) {
        HashSet<String> eventClasses = extractEventClasses(eventLog);
        HashSet<String> modelActivities = new HashSet<>();

        for (Transition transition : petriNet.getTransitions()) {
            if (!transition.isInvisible()) {
                modelActivities.add(transition.getLabel());
            }
        }

        int missing = 0;
        for (String event : eventClasses) {
            if (!modelActivities.contains(event)) missing++;
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