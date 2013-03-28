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

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Simple model interface for variable selections.
 *
 * @author Marco Peters
 */
public interface VariableSelectionPaneModel extends ListModel, Serializable {

    @Override
    Variable getElementAt(int index);

    /**
     * Clears the model and sets the given {@link Variable}s.
     * @param variables the variables to set
     */
    void set(Variable... variables);

    /**
     * Adds the given {@link Variable}s to the model.
     * @param variables the variables to add
     */
    void add(Variable... variables);

    /**
     * Sets the {@link Variable} at the given index to <code>selected</code>.
     * @param index the index of the variable
     * @param selected true if the variable shall be selected
     */
    void setSelectedVariableAt(int index, boolean selected);

    /**
     * Returns the names of the selected variables.
     * @return the names of the selected variables
     */
    List<String> getSelectedVariableNames();
}
