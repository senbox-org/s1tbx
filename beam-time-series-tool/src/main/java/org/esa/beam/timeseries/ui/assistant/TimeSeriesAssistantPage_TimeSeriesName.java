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

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.ProductNode;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;

class TimeSeriesAssistantPage_TimeSeriesName extends AbstractTimeSeriesAssistantPage {

    private JTextField field;

    TimeSeriesAssistantPage_TimeSeriesName(TimeSeriesAssistantModel assistantModel) {
        super("Set Product Name", assistantModel);
    }

    @Override
    protected Component createPageComponent() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 1.0);

        final JPanel panel = new JPanel(tableLayout);
        final JLabel label = new JLabel("Time Series Name:");
        field = new JTextField(getAssistantModel().getTimeSeriesName());
        panel.add(label);
        panel.add(field);
        return panel;
    }

    @Override
    public boolean validatePage() {
        if (super.validatePage()) {
            final String name = field.getText();
            if (!name.isEmpty() && ProductNode.isValidNodeName(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean performFinish() {
        getAssistantModel().setTimeSeriesName(field.getText());
        return super.performFinish();
    }
}
