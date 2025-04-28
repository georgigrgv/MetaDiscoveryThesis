package org.conformance.alignments;

import org.deckfour.xes.info.XLogInfo;
import org.processmining.plugins.petrinet.replayresult.PNRepResultImpl;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayresult.PNRepResultImpl;

public class AlignmentsResult {

    public final PNRepResultImpl alignment;
    public final Marking initialMarking;
    public final Marking finalMarking;
    public final TransEvClassMapping mapping;
    public final XLogInfo logInfo;

    public AlignmentsResult(PNRepResultImpl alignment, Marking initialMarking, Marking finalMarking, TransEvClassMapping mapping, XLogInfo logInfo) {
        this.alignment = alignment;
        this.initialMarking = initialMarking;
        this.finalMarking = finalMarking;
        this.mapping = mapping;
        this.logInfo = logInfo;
    }
}