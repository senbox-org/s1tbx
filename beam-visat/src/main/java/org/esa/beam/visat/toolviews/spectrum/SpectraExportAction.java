package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphIO;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class SpectraExportAction extends AbstractAction {
    private SpectrumToolView spectrumToolView;

    public SpectraExportAction(SpectrumToolView spectrumToolView) {
        super("exportSpectra");
        this.spectrumToolView = spectrumToolView;
    }

    public void actionPerformed(ActionEvent e) {
        exportSpectra();
    }


    private void exportSpectra() {

        SpectraDiagram spectraDiagram = spectrumToolView.getSpectraDiagram();
        if (spectraDiagram == null) {
            return;
        }

        DiagramGraph[] graphs = spectraDiagram.getGraphs();
        ArrayList<SpectrumGraph> pinGraphList = new ArrayList<SpectrumGraph>(graphs.length);
        for (DiagramGraph graph : graphs) {
            SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
            if (spectrumGraph.getPin() != null) {
                pinGraphList.add(spectrumGraph);
            }
        }
        DiagramGraph[] pinGraphs = pinGraphList.toArray(new DiagramGraph[0]);
        if (pinGraphs.length == 0) {
            JOptionPane.showMessageDialog(spectrumToolView.getContentPane(),
                                          "Nothing to export.");
            return;
        }

        File selectedFile = selectFile();
        if (selectedFile == null) {
            return;
        }

        try {
            FileWriter fileWriter = new FileWriter(selectedFile);
            try {
                DiagramGraphIO.writeGraphs(pinGraphs, fileWriter);
            } finally {
                fileWriter.close();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(spectrumToolView.getContentPane(),
                                          "I/O error: " + e.getMessage());
        }
    }

    private File selectFile() {
        PropertyMap preferences = VisatApp.getApp().getPreferences();
        String lastDirPath = preferences.getPropertyString("spectrumView.lastDir", ".");
        BeamFileChooser fileChooser = new BeamFileChooser(new File(lastDirPath));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileFilter(new BeamFileFilter("Spectra-CSV", ".csv", "Spectra CSV"));
        fileChooser.setDialogTitle("Export Pin Spectra");
        File selectedFile = null;
        while (true) {
            int i = fileChooser.showSaveDialog(spectrumToolView.getContentPane());
            if (i == BeamFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.exists()) {
                    break;
                }
                i = JOptionPane.showConfirmDialog(spectrumToolView.getContentPane(),
                                                  "The file\n" + selectedFile + "\nalready exists.\nOverwrite?",
                                                  "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
                if (i == JOptionPane.CANCEL_OPTION) {
                    // Canceled
                    selectedFile = null;
                    break;
                } else if (i == JOptionPane.YES_OPTION) {
                    // Overwrite existing file
                    break;
                }
            } else {
                // Canceled
                selectedFile = null;
                break;
            }
        }
        if (selectedFile != null) {
            preferences.setPropertyString("spectrumView.lastDir", selectedFile.getParent());
        }
        return selectedFile;
    }
}
