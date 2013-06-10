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

package org.esa.beam.timeseries.ui;

import javax.swing.AbstractListModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default implementation of {@link VariableSelectionPaneModel}.
 *
 * @author Marco Peters
 */
public class DefaultVariableSelectionPaneModel extends AbstractListModel implements VariableSelectionPaneModel {

    private final List<Variable> variableList;

    public DefaultVariableSelectionPaneModel() {
        variableList = new ArrayList<Variable>();
    }

    @Override
    public int getSize() {
        return variableList.size();
    }

    @Override
    public Variable getElementAt(int index) {
        return variableList.get(index);
    }

    @Override
    public void set(Variable... variables) {
        final int index = variableList.size();
        variableList.clear();
        fireIntervalRemoved(this, 0, index);
        add(variables);
    }

    @Override
    public void add(Variable... variables) {
        final int startIndex = variableList.size();
        variableList.addAll(Arrays.asList(variables));
        final int stopIndex = Math.max(0, variableList.size() - 1);
        fireIntervalAdded(this, startIndex, stopIndex);
    }

    @Override
    public void setSelectedVariableAt(int index, boolean selected) {
        final Variable variable = variableList.get(index);
        if (variable.isSelected() != selected) {
            variable.setSelected(selected);
            fireContentsChanged(this, index, index);
        }
    }

    @Override
    public List<String> getSelectedVariableNames() {
        final List<String> nameList = new ArrayList<String>();

        for (Variable variable : variableList) {
            if(variable.isSelected()) {
                nameList.add(variable.getName());
            }
        }
        return nameList;
    }

}
