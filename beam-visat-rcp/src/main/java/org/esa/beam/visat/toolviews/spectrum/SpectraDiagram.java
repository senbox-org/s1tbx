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

import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramGraph;

import java.awt.*;


class SpectraDiagram extends Diagram {
    private Product product;
    private Band[] bands;
    private boolean userSelection;

    public SpectraDiagram(Product product) {
        this.product = product;
        this.bands = new Band[0];
        setXAxis(new DiagramAxis("Wavelength", "nm"));
        setYAxis(new DiagramAxis("", "1"));
    }

    public Band[] getBands() {
        return bands;
    }

    public boolean isUserSelection() {
        return userSelection;
    }

    public SpectrumGraph getCursorSpectrumGraph() {
        return getSpectrumGraph(null);
    }

    public SpectrumGraph getSpectrumGraph(Placemark placemark) {
        for (DiagramGraph graph : getGraphs()) {
            SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
            if (spectrumGraph.getPlacemark() == placemark) {
                return spectrumGraph;
            }
        }
        return null;
    }

    public void addCursorSpectrumGraph() {
        final SpectrumGraph cursorSpectrumGraph = getCursorSpectrumGraph();
        if (cursorSpectrumGraph == null) {
            addSpectrumGraph(null);
        }
    }

    public void addSpectrumGraph(Placemark placemark) {
        SpectrumGraph spectrumGraph = new SpectrumGraph(placemark, getBands());
        DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) spectrumGraph.getStyle();
        if (placemark != null) {
            final FigureStyle figureStyle = DefaultFigureStyle.createFromCss(placemark.getStyleCss());
            style.setOutlineColor(figureStyle.getFillColor());
            style.setOutlineStroke(new BasicStroke(1.5f));
            style.setFillPaint(figureStyle.getFillPaint());
        } else {
            style.setOutlineColor(Color.BLACK);
            style.setOutlineStroke(new BasicStroke(1.5f));
            style.setFillPaint(Color.WHITE);
        }
        addGraph(spectrumGraph);
    }

    public void removeCursorSpectrumGraph() {
        final SpectrumGraph cursorSpectrumGraph = getCursorSpectrumGraph();
        if (cursorSpectrumGraph != null) {
            removeGraph(cursorSpectrumGraph);
        }
    }

    public void setBands(Band[] bands, boolean userSelection) {
        this.bands = bands;
        this.userSelection = userSelection;
        for (DiagramGraph graph : getGraphs()) {
            ((SpectrumGraph) graph).setBands(bands);
        }
        getYAxis().setUnit(getUnit(this.bands));
        adjustAxes(true);
        invalidate();
    }

    public void updateSpectra(int pixelX, int pixelY, int level) {
        DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            ((SpectrumGraph) graph).readValues(pixelX, pixelY, level);
        }
        adjustAxes(false);
        invalidate();
    }

    private static String getUnit(final Band[] bands) {
        String unit = null;
        for (final Band band : bands) {
            if (unit == null) {
                unit = band.getUnit();
            } else if (!unit.equals(band.getUnit())) {
                unit = " mixed units "; /*I18N*/
                break;
            }
        }
        return unit != null ? unit : "?";
    }

    @Override
    public void dispose() {
        if (product != null) {
            product = null;
            bands = null;
            for (DiagramGraph graph : getGraphs()) {
                SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
                spectrumGraph.dispose();   // todo - care! is SpectraDiagram always owner of its graphs?
            }
        }
        super.dispose();
    }

}
