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

    public static final String[] DEFAULT_COLUMN_NAMES = new String[]{"X,Y", "Lon,Lat", "Name/Description"};
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
        int count = 3;
        if (_selectedBands != null) {
            count += _selectedBands.length;
        }
        if (_selectedGrids != null) {
            count += _selectedGrids.length;
        }
        return count;
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return DEFAULT_COLUMN_NAMES[0];
        } else if (columnIndex == 1) {
            return DEFAULT_COLUMN_NAMES[1];
        } else if (columnIndex == 2) {
            return DEFAULT_COLUMN_NAMES[2];
        } else {
            int newIndex = columnIndex - 3;
            if (newIndex < getNumSelectedBands()) {
                return _selectedBands[newIndex].getName();
            }
            newIndex -= getNumSelectedBands();
            if (_selectedGrids != null && newIndex < _selectedGrids.length) {
                return _selectedGrids[newIndex].getName();
            }
        }
        return new JTable().getColumnName(columnIndex);
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return PixelPos.class;
        } else if (columnIndex == 1) {
            return GeoPos.class;
        } else if (columnIndex == 2) {
            return String[].class;
        } else {
            return String.class;
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (_product != null) {
            final int width = _product.getSceneRasterWidth();
            final int height = _product.getSceneRasterHeight();
            Pin pin = _product.getPinAt(rowIndex);
            final PixelPos pixelPos = pin.getPixelPos();
            if (columnIndex == 0) {
                return pixelPos;
            } else if (columnIndex == 1) {
                return pin.getGeoPos();
            } else if (columnIndex == 2) {
                return new String[]{pin.getName(), pin.getDescription()};
            } else {
                int newIndex = columnIndex - 3;
                final int x = MathUtils.floorInt(pixelPos.getX());
                final int y = MathUtils.floorInt(pixelPos.getY());
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return "no data";
                }
                if (newIndex < getNumSelectedBands()) {
                    final Band band = _selectedBands[newIndex];
                    float[] value = null;
                    try {
                        value = band.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                    } catch (IOException e) {
                    }
                    if (value != null) {
                        return String.valueOf(value[0]);
                    } else {
                        return "null";
                    }
                }
                newIndex -= getNumSelectedBands();
                if (_selectedGrids != null && newIndex < _selectedGrids.length) {
                    final TiePointGrid grid = _selectedGrids[newIndex];
                    float[] value = null;
                    try {
                        value = grid.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                    } catch (IOException e) {
                    }
                    if (value != null) {
                        return String.valueOf(value[0]);
                    } else {
                        return "null";
                    }
                }
            }
        }
        return null;
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