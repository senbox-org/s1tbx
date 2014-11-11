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

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.AbstractSelection;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;

/**
 * A default implementation of the {@link com.bc.ceres.swing.selection.Selection Selection} interface.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class DefaultSelection<T> extends AbstractSelection {

    private T[] selectedValues;

    public DefaultSelection(T... selectedValues) {
        this.selectedValues = selectedValues;
    }

    @Override
    public T getSelectedValue() {
        return selectedValues.length > 0 ? selectedValues[0] : null;
    }

    @Override
    public T[] getSelectedValues() {
        return selectedValues.clone();
    }

    @Override
    public boolean isEmpty() {
        return selectedValues.length == 0;
    }

    @Override
    public String getPresentationName() {
        Object value = getSelectedValue();
        return value != null ? value.toString() : "";
    }

    @Override
    public Transferable createTransferable(boolean snapshot) {
        Object value = getSelectedValue();
        // todo - handle multiple selections
        return value != null ? new StringSelection(value.toString()) : null;
    }

    @Override
    public DefaultSelection<T> clone() {
        DefaultSelection<T> selection = (DefaultSelection<T>) super.clone();
        selection.selectedValues = selectedValues.clone();
        return selection;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultSelection)) {
            return false;
        }

        DefaultSelection that = (DefaultSelection) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(selectedValues, that.selectedValues);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(selectedValues);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[selectedValues={");
        for (int i = 0; i < selectedValues.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(selectedValues[i]);
        }
        sb.append("}]");
        return sb.toString();
    }
}