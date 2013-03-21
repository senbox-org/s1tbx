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

package org.esa.beam.timeseries.ui.assistant;

import org.esa.beam.timeseries.ui.DefaultProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.DefaultVariableSelectionPaneModel;
import org.esa.beam.timeseries.ui.DefaultProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.DefaultVariableSelectionPaneModel;
import org.esa.beam.timeseries.ui.ProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.VariableSelectionPaneModel;
import org.esa.beam.timeseries.ui.ProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.VariableSelectionPaneModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

class TimeSeriesAssistantModel {

    private final ProductLocationsPaneModel productLocationsModel;
    private final VariableSelectionPaneModel variableSelectionPaneModel;
    private String timeSeriesName;
    private List<ChangeListener> changeListenerList;

    TimeSeriesAssistantModel() {
        this(new DefaultProductLocationsPaneModel(), new DefaultVariableSelectionPaneModel());
    }

    private TimeSeriesAssistantModel(ProductLocationsPaneModel productLocationsModel,
                                     VariableSelectionPaneModel variableSelectionPaneModel) {
        this.productLocationsModel = productLocationsModel;
        this.variableSelectionPaneModel = variableSelectionPaneModel;
        this.timeSeriesName = "TimeSeries";
        final ListDataListenerDelegate dataListenerDelegate = new ListDataListenerDelegate();
        productLocationsModel.addListDataListener(dataListenerDelegate);
        variableSelectionPaneModel.addListDataListener(dataListenerDelegate);

    }

    public ProductLocationsPaneModel getProductLocationsModel() {
        return productLocationsModel;
    }

    public VariableSelectionPaneModel getVariableSelectionModel() {
        return variableSelectionPaneModel;
    }

    public void setTimeSeriesName(String timeSeriesName) {
        if (!this.timeSeriesName.equals(timeSeriesName)) {
            this.timeSeriesName = timeSeriesName;
            fireChangeEvent();
        }
    }

    public String getTimeSeriesName() {
        return timeSeriesName;
    }

    public void addChangeListener(ChangeListener changeListener) {
        if (changeListenerList == null) {
            changeListenerList = new ArrayList<ChangeListener>();
        }
        if(!changeListenerList.contains(changeListener)) {
            changeListenerList.add(changeListener);
        }
    }

    public void removeChangeListener(ChangeListener changeListener) {
        if(changeListenerList != null) {
            changeListenerList.remove(changeListener);
        }
    }

    private void fireChangeEvent() {
        if(changeListenerList != null) {
            for (ChangeListener changeListener : changeListenerList) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    private class ListDataListenerDelegate implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent e) {
            fireChangeEvent();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            fireChangeEvent();

        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            fireChangeEvent();
        }
    }
}
