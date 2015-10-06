/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs.CursorGraph;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs.PlacemarkGraph;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs.VectorGraph;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Placemark;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.VectorDataNode;
import org.esa.snap.framework.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.snap.framework.ui.diagram.Diagram;
import org.esa.snap.framework.ui.diagram.DiagramAxis;
import org.esa.snap.framework.ui.diagram.DiagramGraph;

import java.awt.*;
import java.util.ArrayList;


class TimeSeriesDiagram extends Diagram {
    private Product product;
    private final DateAxis dateAxis;

    public TimeSeriesDiagram(final Product product) {
        this.product = product;
        dateAxis = new DateAxis("Band", "");
        setXAxis(dateAxis);
        setYAxis(new DiagramAxis("", "1"));
    }

    public TimeSeriesGraph[] getCursorGraphs() {
        final ArrayList<TimeSeriesGraph> graphs = new ArrayList<>(5);
        for (DiagramGraph graph : getGraphs()) {
            if (graph instanceof CursorGraph)
                graphs.add((CursorGraph) graph);
        }
        return graphs.toArray(new TimeSeriesGraph[graphs.size()]);
    }

    public TimeSeriesGraph addCursorGraph(final GraphData graphData) {
        final TimeSeriesGraph graph = new CursorGraph();
        final DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) graph.getStyle();
        style.setOutlineColor(graphData.getColor());
        style.setOutlineStroke(new BasicStroke(1.5f));
        style.setFillPaint(Color.WHITE);
        addGraph(graph);
        return graph;
    }

    public TimeSeriesGraph addPlacemarkGraph(final Placemark placemark, final GraphData graphData) {
        final TimeSeriesGraph graph = new PlacemarkGraph(placemark, graphData.getTitle());
        final DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) graph.getStyle();
        final FigureStyle figureStyle = DefaultFigureStyle.createFromCss(placemark.getStyleCss());
        style.setOutlineColor(graphData.getColor());
        style.setOutlineStroke(new BasicStroke(1.2f));
        style.setFillPaint(figureStyle.getFillPaint());
        addGraph(graph);
        return graph;
    }

    public TimeSeriesGraph addVectorGraph(final VectorDataNode vectorNode, final GraphData graphData, final VectorGraph.TYPE type) {
        final TimeSeriesGraph graph = new VectorGraph(vectorNode, type);
        final DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) graph.getStyle();
        final FigureStyle figureStyle = DefaultFigureStyle.createFromCss(vectorNode.getDefaultStyleCss());
        style.setOutlineColor(graphData.getColor());
        style.setOutlineStroke(new BasicStroke(3.5f));
        style.setFillPaint(figureStyle.getFillPaint());
        addGraph(graph);
        return graph;
    }

    public void removeCursorGraph() {
        final TimeSeriesGraph[] cursorGraphs = getCursorGraphs();
        if (cursorGraphs != null) {
            for (TimeSeriesGraph graph : cursorGraphs) {
                removeGraph(graph);
            }
        }
    }

    public void initAxis(final TimeSeriesTimes times, final Band[] bands) {
        dateAxis.setTimes(times);
        getYAxis().setUnit(getUnit(bands));
        adjustAxes(true);
        invalidate();
    }

    public void updateDiagram(final ImageLayer imageLayer, final int pixelX, final int pixelY, final int level) {
        // get lat lons
        final GeoPos geoPos = product.getGeoCoding().getGeoPos(new PixelPos(pixelX + 0.5f, pixelY + 0.5f), null);

        final DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            ((TimeSeriesGraph) graph).readValues(imageLayer, geoPos, level);
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
            for (DiagramGraph graph : getGraphs()) {
                final TimeSeriesGraph tsGraph = (TimeSeriesGraph) graph;
                tsGraph.dispose();
            }
        }
        super.dispose();
    }

}