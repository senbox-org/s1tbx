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
import org.apache.commons.collections.map.ListOrderedMap;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramGraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


class SpectraDiagram extends Diagram {

    private Band[] bands;
    private Band[][] spectra;
    private Set<Placemark> placemarks;
    private boolean userSelection;

    public SpectraDiagram() {
        this.bands = new Band[0];
        this.spectra = new Band[0][];
        this.placemarks = new HashSet<Placemark>();
        setXAxis(new DiagramAxis("Wavelength", "nm"));
        setYAxis(new DiagramAxis("", "1"));
    }

    public void setBands(Band[] bands, boolean userSelection) {
        this.bands = bands;
        this.spectra = extractSpectra(bands);
        this.userSelection = userSelection;
        reinitializeGraphs();
        getYAxis().setUnit(getUnit(this.bands));
        adjustAxes(true);
        invalidate();
    }

    public Band[] getBands() {
        return bands;
    }

    public boolean isUserSelection() {
        return userSelection;
    }

    public void addCursorSpectrumGraphs() {
        if (!hasCursorSpectrumGraphs()) {
            addSpectrumGraph(null);
        }
    }

    public void removeCursorSpectrumGraph() {
        if (hasCursorSpectrumGraphs()) {
            placemarks.remove(null);
            reinitializeGraphs();
        }
    }

    public void addSpectrumGraph(Placemark placemark) {
        placemarks.add(placemark);
        for (Band[] spectrum : spectra) {
            SpectrumGraph spectrumGraph = new SpectrumGraph(placemark, spectrum);
            styleGraph(spectrumGraph);
            addGraph(spectrumGraph);
        }
    }

    public void updateSpectra(int pixelX, int pixelY, int level) {
        DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            ((SpectrumGraph) graph).readValues(pixelX, pixelY, level);
        }
        adjustAxes(false);
        invalidate();
    }

    @Override
    public void dispose() {
        bands = null;
        spectra = null;
        placemarks.clear();
        placemarks = null;
        removeAndDisposeAllGraphs();
        super.dispose();
    }

    public static Band[][] extractSpectra(Band[] bands) {
        if (bands == null || bands.length == 0) {
            return new Band[0][];
        }
        final Map<Product.AutoGrouping, Map<Integer, List<Band>>> graphsMap = new ListOrderedMap();
        final ArrayList<Band> ungroupedBands = new ArrayList<Band>();
        for (Band band : bands) {
            final Product.AutoGrouping autoGrouping = band.getProduct().getAutoGrouping();
            if (autoGrouping != null) {
                Map<Integer, List<Band>> indexedBandMap = graphsMap.get(autoGrouping);
                if (indexedBandMap == null) {
                    indexedBandMap = new TreeMap<Integer, List<Band>>();
                    graphsMap.put(autoGrouping, indexedBandMap);
                }
                final int index = autoGrouping.indexOf(band.getName());
                if (index == -1) {
                    ungroupedBands.add(band);
                } else {
                    List<Band> bandsList = indexedBandMap.get(index);
                    if (bandsList == null) {
                        bandsList = new ArrayList<Band>();
                        indexedBandMap.put(index, bandsList);
                    }
                    bandsList.add(band);
                }
            } else {
                ungroupedBands.add(band);
            }
        }
        final List<Band[]> spectraList = new ArrayList<Band[]>();
        if (ungroupedBands.size() > 0) {
            spectraList.add(ungroupedBands.toArray(new Band[ungroupedBands.size()]));
        }
        for (Map<Integer, List<Band>> integerListMap : graphsMap.values()) {
            for (List<Band> bandList : integerListMap.values()) {
                if (bandList.size() > 0) {
                    spectraList.add(bandList.toArray(new Band[bandList.size()]));
                }
            }
        }
        return spectraList.toArray(new Band[spectraList.size()][]);
    }

    private void removeAndDisposeAllGraphs() {
        final DiagramGraph[] graphs = getGraphs();
        removeAllGraphs();
        for (DiagramGraph graph : graphs) {
            SpectrumGraph spectrumGraph = (SpectrumGraph) graph;
            spectrumGraph.dispose();   // todo - care! is SpectraDiagram always owner of its graphs?
        }
    }

    private boolean hasCursorSpectrumGraphs() {
        return placemarks.contains(null);
    }

    private void styleGraph(SpectrumGraph spectrumGraph) {
        DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) spectrumGraph.getStyle();
        final Placemark placemark = spectrumGraph.getPlacemark();
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
    }

    private void reinitializeGraphs() {
        removeAndDisposeAllGraphs();
        for (Placemark placemark : placemarks) {
            addSpectrumGraph(placemark);
        }
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
}
