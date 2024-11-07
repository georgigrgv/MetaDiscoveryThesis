package algorithm.discovery;

import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.base.FullPnmlElementFactory;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.base.PnmlElement;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class ExportPetriNet {

    public static String exportPetrinetToPNMLorEPNMLString(PetrinetGraph net, Pnml.PnmlType
            type, Marking marking) {
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

    public static void exportPetrinetToPNMLorEPNMLFile(PetrinetGraph net, Pnml.PnmlType
            type, Marking marking, String targetName) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(targetName);
        out.write(exportPetrinetToPNMLorEPNMLString(net, type, marking));
        out.flush();
        out.close();
    }
}
