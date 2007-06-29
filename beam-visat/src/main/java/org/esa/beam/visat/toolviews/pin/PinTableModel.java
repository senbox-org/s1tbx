/*
 * $Id: PinTableModel.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package org.esa.beam.visat.toolviews.pin;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.math.MathUtils;

import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;

public class PinTableModel implements TableModel {

    public static final String[] DEFAULT_COLUMN_NAMES = new String[]{"X", "Y", "Lon", "Lat", "Label"};
    public static final Class[] DEFAULT_COLUMN_TYPES = new Class[]{Float.class, Float.class, Float.class, Float.class, String.class};
    private final Product _product;
    private final Band[] _selectedBands;
    private final TiePointGrid[] _selectedGrids;

    public PinTableModel(Product product, Band[] selectedBands, TiePointGrid[] selectedGrids) {
        _product = product;
        _selectedBands = selectedBands;
        _selectedGrids = selectedGrids;
    }

    public int getRowCount() {
        if (_product != null) {
            return _product.getNumPins();
        }
        return 0;
    }

    public int getColumnCount() {
        int count = DEFAULT_COLUMN_NAMES.length;
        if (_selectedBands != null) {
            count += _selectedBands.length;
        }
        if (_selectedGrids != null) {
            count += _selectedGrids.length;
        }
        return count;
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex < DEFAULT_COLUMN_NAMES.length) {
            return DEFAULT_COLUMN_NAMES[columnIndex];
        }
        int newIndex = columnIndex - DEFAULT_COLUMN_NAMES.length;
        if (newIndex < getNumSelectedBands()) {
            return _selectedBands[newIndex].getName();
        }
        newIndex -= getNumSelectedBands();
        if (_selectedGrids != null && newIndex < _selectedGrids.length) {
            return _selectedGrids[newIndex].getName();
        }
        return "?";
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex < DEFAULT_COLUMN_TYPES.length) {
            return DEFAULT_COLUMN_TYPES[columnIndex];
        }
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (_product != null) {
            final int width = _product.getSceneRasterWidth();
            final int height = _product.getSceneRasterHeight();
            Pin pin = _product.getPinAt(rowIndex);
            if (columnIndex == 0) {
                return pin.getPixelPos().x;
            } else if (columnIndex == 1) {
                return pin.getPixelPos().y;
            } else if (columnIndex == 2) {
                return pin.getGeoPos().lon;
            } else if (columnIndex == 3) {
                return pin.getGeoPos().lat;
            } else if (columnIndex == 4) {
                return pin.getLabel();
            } else {
                int newIndex = columnIndex - 5;
                PixelPos pixelPos = pin.getPixelPos();
                final int x = MathUtils.floorInt(pixelPos.getX());
                final int y = MathUtils.floorInt(pixelPos.getY());
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return "No-data";
                }
                if (newIndex < getNumSelectedBands()) {
                    final Band band = _selectedBands[newIndex];
                    if (band.isPixelValid(x, y)) {
                        float[] value = null;
                        try {
                            value = band.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                            return String.valueOf(value[0]);
                        } catch (IOException e) {
                            return "I/O-error";
                        }
                    } else {
                        return "No-data";
                    }
                }
                newIndex -= getNumSelectedBands();
                if (_selectedGrids != null && newIndex < _selectedGrids.length) {
                    final TiePointGrid grid = _selectedGrids[newIndex];
                    float[] value = null;
                    try {
                        value = grid.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                        return String.valueOf(value[0]);
                    } catch (IOException e) {
                        return "I/O-error";
                    }
                }
            }
        }
        return "?";
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public void addTableModelListener(TableModelListener l) {
    }

    public void removeTableModelListener(TableModelListener l) {
    }

    private int getNumSelectedBands() {
        return _selectedBands != null ? _selectedBands.length : 0;
    }
}