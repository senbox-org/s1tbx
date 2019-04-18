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

import java.util.ArrayList;
import java.util.List;

/**
 * Parameter settings for the time series
 */
public class TimeSeriesSettings {

    private boolean showGrid = true;
    private boolean showLegend = true;
    private final List<GraphData> graphDataList = new ArrayList<>(2);

    public TimeSeriesSettings() {
        graphDataList.add(new GraphData("Graph 1"));      // always have at least an empty list
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

    public void setGraphDataList(final List<GraphData> dataList) {
        graphDataList.clear();
        graphDataList.addAll(dataList);
    }

    public final List<GraphData> getGraphDataList() {
        return graphDataList;
    }
}
