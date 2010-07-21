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

package org.esa.beam.visat.toolviews.bitmask;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.util.Debug;

public class BitmaskDefTableModel extends AbstractTableModel {

    private static final Class[] _COLUMN_CLASSES = new Class[]{Boolean.class,
                                                               String.class,
                                                               Color.class,
                                                               String.class,
                                                               String.class};
    private static final String[] _COLUMN_NAMES = new String[]{"Vis", "Name", "Color", "Transparency", "Description"};

    private boolean _visibleFlagColumnEnabled;
    private final List _visibleFlagList;
    private final List _bitmaskDefList;

    public BitmaskDefTableModel() {
        this(true);
    }

    public BitmaskDefTableModel(boolean visibleFlagColumnEnabled) {
        _visibleFlagColumnEnabled = visibleFlagColumnEnabled;
        _visibleFlagList = new ArrayList();
        _bitmaskDefList = new ArrayList();
    }

    public boolean isVisibleFlagColumnEnabled() {
        return _visibleFlagColumnEnabled;
    }

    public void setVisibleFlagColumnEnabled(boolean visibleFlagColumnEnabled) {
        _visibleFlagColumnEnabled = visibleFlagColumnEnabled;
        fireTableStructureChanged();
    }


    /**
     * Returns all bitmask definitions in this model.
     */
    public BitmaskDef[] getBitmaskDefs() {
        return (BitmaskDef[]) _bitmaskDefList.toArray(new BitmaskDef[_bitmaskDefList.size()]);
    }

    public BitmaskDef getBitmaskDefAt(int rowIndex) {
        return (BitmaskDef) _bitmaskDefList.get(rowIndex);
    }

    /**
     * Returns all bitmask visible flags in this model.
     */
    public Boolean[] getVisibleFlags() {
        return (Boolean[]) _visibleFlagList.toArray(new Boolean[_bitmaskDefList.size()]);
    }


    public boolean getVisibleFlagAt(int rowIndex) {
        return ((Boolean) _visibleFlagList.get(rowIndex)).booleanValue();
    }

    public void setVisibleFlag(boolean visibleFlag, int rowIndex) {
        setRowAt(visibleFlag, getBitmaskDefAt(rowIndex), rowIndex);
    }

    public void setBitmaskDefAt(BitmaskDef bitmaskDef, int rowIndex) {
        setRowAt(getVisibleFlagAt(rowIndex), bitmaskDef, rowIndex);
    }

    public void setRowAt(boolean visibleFlag, BitmaskDef bitmaskDef, int rowIndex) {
        _visibleFlagList.set(rowIndex, visibleFlag ? Boolean.TRUE : Boolean.FALSE);
        _bitmaskDefList.set(rowIndex, bitmaskDef);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void addRow(BitmaskDef bitmaskDef) {
        addRow(false, bitmaskDef);
    }

    public void addRow(boolean visibleFlag, BitmaskDef bitmaskDef) {
        insertRowAt(visibleFlag, bitmaskDef, _bitmaskDefList.size());
    }

    public void insertRowAt(BitmaskDef bitmaskDef, int rowIndex) {
        insertRowAt(false, bitmaskDef, rowIndex);
    }

    public void insertRowAt(boolean visibleFlag, BitmaskDef bitmaskDef, int rowIndex) {
        _visibleFlagList.add(rowIndex, visibleFlag ? Boolean.TRUE : Boolean.FALSE);
        _bitmaskDefList.add(rowIndex, bitmaskDef);
        fireTableRowsInserted(rowIndex, rowIndex);
    }

    public void removeRowAt(int rowIndex) {
        _visibleFlagList.remove(rowIndex);
        _bitmaskDefList.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public int getRowIndex(String bitmaskName) {
        for (int i = 0; i < getRowCount(); i++) {
            BitmaskDef bitmaskDef = getBitmaskDefAt(i);
            if (bitmaskDef.getName().equalsIgnoreCase(bitmaskName)) {
                return i;
            }
        }
        return -1;
    }

    public int getRowIndex(BitmaskDef bitmaskDef) {
        for (int i = 0; i < getRowCount(); i++) {
            BitmaskDef bitmaskDef2 = getBitmaskDefAt(i);
            if (bitmaskDef2 == bitmaskDef) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        int rowCount = getRowCount();
        if (rowCount > 0) {
            _visibleFlagList.clear();
            _bitmaskDefList.clear();
            fireTableRowsDeleted(0, rowCount - 1);
        }
    }

    /**
     * Returns a column's data type.
     *
     * @param columnIndex the column being queried
     *
     * @return column data type
     */
    @Override
    public Class getColumnClass(int columnIndex) {
        return _COLUMN_CLASSES[columnIndex + (isVisibleFlagColumnEnabled() ? 0 : 1)];
    }

    /**
     * Returns the name for the column.
     *
     * @param columnIndex the column being queried
     *
     * @return a string containing the default name of <code>column</code>
     */
    @Override
    public String getColumnName(int columnIndex) {
        return _COLUMN_NAMES[columnIndex + (isVisibleFlagColumnEnabled() ? 0 : 1)];
    }

    /**
     * Returns <code>true</code> if this table model allows to switch BMD visiblity and column index is zero.
     *
     * @param rowIndex    the row being queried
     * @param columnIndex the column being queried
     *
     * @return false
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return isVisibleFlagColumnEnabled() && columnIndex == 0;
    }


    /**
     * Returns the number of columns in the model. A <code>JTable</code> uses this method to determine how many columns
     * it should create and display by default.
     *
     * @return the number of columns in the model
     *
     * @see #getRowCount
     */
    public int getColumnCount() {
        return _COLUMN_NAMES.length - (isVisibleFlagColumnEnabled() ? 0 : 1);
    }

    /**
     * Returns the number of rows in the model. A <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it is called frequently during rendering.
     *
     * @return the number of rows in the model
     *
     * @see #getColumnCount
     */
    public int getRowCount() {
        Debug.assertTrue(_bitmaskDefList.size() == _visibleFlagList.size());
        return _bitmaskDefList.size();
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     * @param	columnIndex the column whose value is to be queried
     * @return	the value Object at the specified cell
     */
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (!isVisibleFlagColumnEnabled()) {
            columnIndex++;
        }

        boolean visibleFlag = getVisibleFlagAt(rowIndex);
        BitmaskDef bitmaskDef = getBitmaskDefAt(rowIndex);

        if (columnIndex == 0) {
            return visibleFlag ? Boolean.TRUE : Boolean.FALSE;
        } else if (columnIndex == 1) {
            return bitmaskDef.getName();
        } else if (columnIndex == 2) {
            return bitmaskDef.getColor();
        } else if (columnIndex == 3) {
            return String.valueOf(bitmaskDef.getTransparency());
        } else if (columnIndex == 4) {
            return String.valueOf(bitmaskDef.getDescription());
        }

        return null;
    }

    /**
     * This empty implementation is provided so users don't have to implement this method if their data model is not
     * editable.
     *
     * @param aValue      value to assign to cell
     * @param rowIndex    row of cell
     * @param columnIndex column of cell
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}
