package org.conformance.alignments;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.dataawareexplorer.utils.PetrinetUtils;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResultImpl;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

public class Alignments {

    public static AlignmentsResult computeAlignments(XLog log, PetrinetGraph net) throws Exception {
        Marking initialMarking = PetrinetUtils.guessInitialMarking(net);
        Marking finalMarking = PetrinetUtils.guessFinalMarking(net);

        XEventClassifier eventClassifier = XLogInfoImpl.NAME_CLASSIFIER;
        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);
        XEventClass dummyClass = new XEventClass("DUMMY", -1);
        TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, dummyClass);

        for (XEventClass ec : logInfo.getEventClasses().getClasses()) {
            for (Transition t : net.getTransitions()) {
                String label = ec.toString();
                if (label.contains("+")) {
                    label = label.substring(0, label.indexOf("+"));
                }
                if (t.getLabel().equals(label)) {
                    mapping.put(t, ec);
                }
            }
        }

        CostBasedCompleteParam param = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(), dummyClass, net.getTransitions(), 1, 1);
        param.setGUIMode(false);
        param.setCreateConn(false);
        param.setInitialMarking(initialMarking);
        param.setFinalMarkings(finalMarking);
        param.setMaxNumOfStates(200000);

        PNRepResultImpl alignment = (PNRepResultImpl) new PNLogReplayer().replayLog(null, net, log, mapping, new PetrinetReplayerWithILP(), param);

        return new AlignmentsResult(alignment, initialMarking, finalMarking, mapping, logInfo);
    }

    public static boolean areAlignmentsReliable(PNRepResultImpl alignment) {
        int reliable = 0;
        for (SyncReplayResult traceAlignment : alignment) {
            if (traceAlignment.isReliable()) {
                reliable++;
            }
        }
        return reliable > 0;
    }
}
