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

import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.timeseries.ui.Variable;
import org.esa.beam.timeseries.ui.VariableSelectionPane;
import org.esa.beam.timeseries.ui.VariableSelectionPaneModel;
import org.esa.beam.timeseries.ui.Variable;
import org.esa.beam.timeseries.ui.VariableSelectionPane;
import org.esa.beam.timeseries.ui.VariableSelectionPaneModel;

import java.awt.Component;

class TimeSeriesAssistantPage_VariableSelection extends AbstractTimeSeriesAssistantPage {

    TimeSeriesAssistantPage_VariableSelection(TimeSeriesAssistantModel assistantModel) {
        super("Select Variables", assistantModel);
    }

    @Override
    protected Component createPageComponent() {
        return new VariableSelectionPane(getAssistantModel().getVariableSelectionModel());
    }

    @Override
    public boolean validatePage() {
        if (super.validatePage()) {
            VariableSelectionPaneModel variableModel = getAssistantModel().getVariableSelectionModel();
            for (int i = 0; i < variableModel.getSize(); i++) {
                final Variable variable = variableModel.getElementAt(i);
                if (variable.isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canFinish() {
        return false;

    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AssistantPage getNextPage() {
        removeModeListener();
        final TimeSeriesAssistantModel model = getAssistantModel();
        final VariableSelectionPaneModel variableModel = model.getVariableSelectionModel();
        for (int i = 0; i < variableModel.getSize(); i++) {
            final Variable variable = variableModel.getElementAt(i);
            if (variable.isSelected()) {
                model.setTimeSeriesName("TimeSeries_" + variable.getName());
                break;
            }
        }
        return new TimeSeriesAssistantPage_TimeSeriesName(model);
    }

}
