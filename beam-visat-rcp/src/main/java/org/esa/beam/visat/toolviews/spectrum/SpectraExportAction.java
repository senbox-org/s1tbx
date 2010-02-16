package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphIO;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

class SpectraExportAction extends AbstractAction {

    private SpectrumToolView spectrumToolView;

    public SpectraExportAction(SpectrumToolView spectrumToolView) {
        super("exportSpectra");
        this.spectrumToolView = spectrumToolView;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        exportSpectra();
    }


    private void exportSpectra() {
        SpectraDiagram spectraDiagram = spectrumToolView.getSpectraDiagram();
        if (spectraDiagram == null) {
            return;
        }

        DiagramGraph[] graphs = spectraDiagram.getGraphs();
        ArrayList<DiagramGraph> pinGraphList = new ArrayList<DiagramGraph>(graphs.length);
        for (DiagramGraph graph : graphs) {
            SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
            if (spectrumGraph.getPlacemark() != null) {
                pinGraphList.add(spectrumGraph);
            }
        }
        DiagramGraph[] pinGraphs = pinGraphList.toArray(new DiagramGraph[0]);
        DiagramGraphIO.writeGraphs(spectrumToolView.getPaneControl(),
                                   "Export Pin Spectra",
                                   new BeamFileFilter[] {DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                   VisatApp.getApp().getPreferences(), pinGraphs);
    }

}
