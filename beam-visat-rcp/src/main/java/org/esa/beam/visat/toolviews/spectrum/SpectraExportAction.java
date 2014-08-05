/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphIO;
import org.esa.beam.framework.ui.product.spectrum.DisplayableSpectrum;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
        final List<DisplayableSpectrum> selectedSpectra = spectrumToolView.getSelectedSpectra();
        Placemark[] pins = spectrumToolView.getDisplayedPins();
        final List<SpectrumGraph> spectrumGraphList = new ArrayList<SpectrumGraph>();
        for (Placemark pin : pins) {
            for (DisplayableSpectrum spectrumInDisplay : selectedSpectra) {
                final SpectrumGraph spectrumGraph = new SpectrumGraph(pin, spectrumInDisplay.getSelectedBands());
                spectrumGraph.readValues();
                spectrumGraphList.add(spectrumGraph);
            }
        }
        DiagramGraph[] pinGraphs = spectrumGraphList.toArray(new DiagramGraph[0]);
        DiagramGraphIO.writeGraphs(spectrumToolView.getPaneControl(),
                                   "Export Pin Spectra",
                                   new BeamFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                   VisatApp.getApp().getPreferences(), pinGraphs);
    }

}
