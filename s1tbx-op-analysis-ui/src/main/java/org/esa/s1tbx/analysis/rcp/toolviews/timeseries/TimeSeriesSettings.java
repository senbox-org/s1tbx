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

import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs.VectorGraph;
import org.esa.snap.core.datamodel.Band;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Parameter settings for the time series
 */
public class TimeSeriesSettings {

    private boolean showGrid = true;
    private boolean showLegend = true;
    private String[] selectedBandNames = null;
    private String[] selectedPinNames = null;
    private String[] selectedVectorNames = null;
    private final Map<String, Color> bandColorMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Color> pinColorMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Color> vectorColorMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, BasicStroke> bandStrokeMap = Collections.synchronizedMap(new HashMap<>());

    public final static BasicStroke solidStroke = new BasicStroke(2);

    private VectorGraph.TYPE vectorStatistic = VectorGraph.TYPE.AVERAGE;
    private final List<GraphData> graphDataList = Collections.synchronizedList(new ArrayList<>(2));

    public TimeSeriesSettings() {
        graphDataList.add(new GraphData("Graph 1"));      // always have at least an empty list
    }

    public Map<String, Color> getBandColorMap() {
        return bandColorMap;
    }

    public Map<String, Color> getPinColorMap() {
        return pinColorMap;
    }

    public Map<String, Color> getVectorColorMap() {
        return vectorColorMap;
    }

    public Map<String, BasicStroke> getBandStrokeMap() {
        return bandStrokeMap;
    }

    public VectorGraph.TYPE getVectorStatistic() {
        return vectorStatistic;
    }

    public void setVectorStatistic(final VectorGraph.TYPE stat) {
        vectorStatistic = stat;
    }

    public boolean isShowingGrid() {
        return showGrid;
    }

    public void setGridShown(final boolean flag) {
        showGrid = flag;
    }

    public boolean isShowingLegend() {
        return showLegend;
    }

    public void setLegendShown(final boolean flag) {
        showLegend = flag;
    }

    public String[] getSelectedBands() {
        return selectedBandNames;
    }

    public void setSelectedBands(final String[] selectedBands) {
        this.selectedBandNames = selectedBands;
        this.bandStrokeMap.clear();
        if(selectedBands != null) {
            for (String bandName : selectedBands) {
                getBandStroke(bandName);
            }
        }
    }

    public String[] getSelectedPinNames() {
        return selectedPinNames;
    }

    public void setSelectedPins(final String[] selectedPins) {
        this.selectedPinNames = selectedPins;
    }

    public String[] getSelectedVectorNames() {
        return selectedVectorNames;
    }

    public void setSelectedVectors(final String[] selectedVectors) {
        this.selectedVectorNames = selectedVectors;
    }

    public Color getBandColor(final String bandName) {
        if(!bandColorMap.containsKey(bandName)) {
            bandColorMap.put(bandName, RandomColor());
        }
        return bandColorMap.get(bandName);
    }

    public Color getPinColor(final String pinName) {
        if(!pinColorMap.containsKey(pinName)) {
            pinColorMap.put(pinName, RandomColor());
        }
        return pinColorMap.get(pinName);
    }

    public Color getVectorColor(final String vectorName) {
        if(!vectorColorMap.containsKey(vectorName)) {
            vectorColorMap.put(vectorName, RandomColor());
        }
        return vectorColorMap.get(vectorName);
    }

    public BasicStroke getBandStroke(final String bandName) {
        if(!bandStrokeMap.containsKey(bandName)) {
            bandStrokeMap.put(bandName, createStroke(2, bandStrokeMap.size()));
        }
        return bandStrokeMap.get(bandName);
    }

    private Color RandomColor() {
        final Random random = new Random();
        final float hue = random.nextFloat();
        final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
        final float luminance = 1.0f; //1.0 for brighter, 0.0 for black
        return Color.getHSBColor(hue, saturation, luminance);
    }

    private BasicStroke createStroke(final int width, final int count) {
        if(count == 0) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        } else if(count == 1) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.f}, 0.0f);
        } else if(count == 2) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.f}, 0.0f);
        } else if(count == 3) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{12.f}, 0.0f);
        } else if(count == 4) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f,8f}, 0.0f);
        } else if(count == 5) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{8f,4f}, 0.0f);
        } else if(count == 6) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{1f,1f,3f,3f}, 0.0f);
        } else if(count == 7) {
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{1f,3f,4f}, 0.0f);
        }
        return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{count}, 0.0f);
    }

    public void setGraphDataList(final List<GraphData> dataList) {
        graphDataList.clear();
        graphDataList.addAll(dataList);
    }

    public final List<GraphData> getGraphDataList() {
        return graphDataList;
    }

    public boolean hasProducts() {
        return !graphDataList.isEmpty() && graphDataList.get(0).getProducts() != null;
    }

    public void populateColorMaps(final Band[] bands, final String[] allPins, final String[] allVectors) {
        if(bands != null) {
            for (Band band : bands) {
                getBandColor(band.getName());
            }
        }
        if(allPins != null) {
            for (String pin : allPins) {
                getPinColor(pin);
            }
        }
        if(allVectors != null) {
            for (String vector : allVectors) {
                getVectorColor(vector);
            }
        }
    }
}
