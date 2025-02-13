package org.discovery;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnml.base.FullPnmlElementFactory;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.base.PnmlElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ExportPetriNet {

    public static void createFolderForResults(String parentDirectory, String newFolderName){
        File folder = new File(parentDirectory, newFolderName);
        folder.mkdir();
    }

    public static String exportPetrinetToPNMLorEPNMLString(PetrinetGraph net, Marking marking) {
        Collection<Marking> finalMarkings = new HashSet<Marking>();
        GraphLayoutConnection layout;
        layout = new GraphLayoutConnection(net);
        HashMap<PetrinetGraph, Marking> markedNets = new HashMap<PetrinetGraph,
                Marking>();
        HashMap<PetrinetGraph, Collection<Marking>> finalMarkedNets = new
                HashMap<PetrinetGraph, Collection<Marking>>();
        markedNets.put(net, marking);
        finalMarkedNets.put(net, finalMarkings);
        Pnml pnml = new Pnml();
        FullPnmlElementFactory factory = new FullPnmlElementFactory();
        synchronized (factory) {
            PnmlElement.setFactory(factory);
            pnml = pnml.convertFromNet(markedNets, finalMarkedNets, layout);
        }
        return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                pnml.exportElement(pnml);
    }

    public static void exportPetrinetToPNMLorEPNMLFile(PetrinetGraph net, Marking marking, String targetName) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(targetName);
        out.write(exportPetrinetToPNMLorEPNMLString(net, marking));
        out.flush();
        out.close();
    }

    public Object[] convertBPMNToPetriNet(PluginContext context, BPMNDiagram bpmn, BPMN2PetriNetConverter_Configuration config) {

        BPMN2PetriNetConverter conv = new BPMN2PetriNetConverter(bpmn, config);

        Progress progress = context.getProgress();
        progress.setCaption("Converting BPMN diagram to Petri net");

        boolean success = conv.convert();

        if (success) {
            Petrinet net = conv.getPetriNet();
            Marking m = conv.getMarking();
            context.getConnectionManager().addConnection(new InitialMarkingConnection(net, m));

            List<Place> finalPlaces = conv.getFinalPlaces();
            if (finalPlaces.size() == 1) {
                Marking mf = new Marking(finalPlaces);
                context.getConnectionManager().addConnection(new FinalMarkingConnection(net, mf));
                context.getProvidedObjectManager().createProvidedObject("Final marking of the PN from " + bpmn.getLabel(), mf, context);
            }

            return new Object[] { net, m };
        }
        return new Object[]{};
    }
}
