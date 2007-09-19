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
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.PlacemarkDescriptor;
import org.esa.beam.util.math.MathUtils;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;

public class PinTableModel extends DefaultTableModel {

    public static final String[] DEFAULT_COLUMN_NAMES = new String[]{"X", "Y", "Lon", "Lat", "Label"};

    private final PlacemarkDescriptor placemarkDescriptor;
    private final Product product;
    private final Band[] selectedBands;
    private final TiePointGrid[] selectedGrids;
    private PinTableModel.PlacemarkListener placemarkListener;

    public PinTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                         TiePointGrid[] selectedGrids) {
        this.placemarkDescriptor = placemarkDescriptor;
        this.product = product;
        this.selectedBands = selectedBands;
        this.selectedGrids = selectedGrids;
        placemarkListener = new PlacemarkListener();
        if (product != null) {
            product.addProductNodeListener(placemarkListener);
        }
    }


    public int getRowCount() {
        if (product != null) {
            return placemarkDescriptor.getPlacemarkGroup(product).getNodeCount();
        }
        return 0;
    }

    public int getColumnCount() {
        int count = DEFAULT_COLUMN_NAMES.length;
        if (selectedBands != null) {
            count += selectedBands.length;
        }
        if (selectedGrids != null) {
            count += selectedGrids.length;
        }
        return count;
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex < DEFAULT_COLUMN_NAMES.length) {
            return DEFAULT_COLUMN_NAMES[columnIndex];
        }
        int newIndex = columnIndex - DEFAULT_COLUMN_NAMES.length;
        if (newIndex < getNumSelectedBands()) {
            return selectedBands[newIndex].getName();
        }
        newIndex -= getNumSelectedBands();
        if (selectedGrids != null && newIndex < selectedGrids.length) {
            return selectedGrids[newIndex].getName();
        }
        return "?";
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex >= 0 && columnIndex <= 3) {
            return Float.class;
        }
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < DEFAULT_COLUMN_NAMES.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (product != null) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            Pin pin = placemarkDescriptor.getPlacemarkGroup(product).get(rowIndex);

            if (columnIndex == 0) {
                PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos == null) {
                    return Float.NaN;
                }
                return pixelPos.x;
            } else if (columnIndex == 1) {
                PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos == null) {
                    return Float.NaN;
                }
                return pixelPos.y;
            } else if (columnIndex == 2) {
                GeoPos geoPos = pin.getGeoPos();
                if (geoPos == null) {
                    return Float.NaN;
                }
                return geoPos.lon;
            } else if (columnIndex == 3) {
                GeoPos geoPos = pin.getGeoPos();
                if (geoPos == null) {
                    return Float.NaN;
                }
                return geoPos.lat;
            } else if (columnIndex == 4) {
                return pin.getLabel();
            } else {
                int newIndex = columnIndex - 5;
                PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos == null) {
                    return "No-data";
                }
                final int x = MathUtils.floorInt(pixelPos.getX());
                final int y = MathUtils.floorInt(pixelPos.getY());
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return "No-data";
                }
                if (newIndex < getNumSelectedBands()) {
                    final Band band = selectedBands[newIndex];
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
                if (selectedGrids != null && newIndex < selectedGrids.length) {
                    final TiePointGrid grid = selectedGrids[newIndex];
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
        return "";
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (value == null) {
            return;
        }
        if (columnIndex < DEFAULT_COLUMN_NAMES.length) {
            Pin pin = placemarkDescriptor.getPlacemarkGroup(product).get(rowIndex);
            if (columnIndex == 0) {
                if (value instanceof Float) {
                    float pixelY;
                    if (pin.getPixelPos() == null) {
                        pixelY = -1;
                    } else {
                        pixelY = pin.getPixelPos().y;
                    }
                    pin.setPixelPos(new PixelPos((Float) value, pixelY));
                    GeoPos geoPos = placemarkDescriptor.updateGeoPos(product.getGeoCoding(),
                                                                     pin.getPixelPos(), pin.getGeoPos());
                    pin.setGeoPos(geoPos);
                }
            } else if (columnIndex == 1) {
                if (value instanceof Float) {
                    float pixelX;
                    if (pin.getPixelPos() == null) {
                        pixelX = -1;
                    } else {
                        pixelX = pin.getPixelPos().x;
                    }
                    pin.setPixelPos(new PixelPos(pixelX, (Float) value));
                    GeoPos geoPos = placemarkDescriptor.updateGeoPos(product.getGeoCoding(),
                                                                     pin.getPixelPos(), pin.getGeoPos());
                    pin.setGeoPos(geoPos);
                }
            } else if (columnIndex == 2) {
                if (value instanceof Float) {
                    float lat;
                    if (pin.getGeoPos() == null) {
                        lat = Float.NaN;
                    } else {
                        lat = pin.getGeoPos().lat;
                    }
                    pin.setGeoPos(new GeoPos(lat, (Float) value));
                    PixelPos pixelPos = placemarkDescriptor.updatePixelPos(product.getGeoCoding(),
                                                                           pin.getGeoPos(), pin.getPixelPos());
                    pin.setPixelPos(pixelPos);
                }
            } else if (columnIndex == 3) {
                if (value instanceof Float) {
                    float lon;
                    if (pin.getGeoPos() == null) {
                        lon = Float.NaN;
                    } else {
                        lon = pin.getGeoPos().lon;
                    }
                    pin.setGeoPos(new GeoPos((Float) value, lon));
                    PixelPos pixelPos = placemarkDescriptor.updatePixelPos(product.getGeoCoding(),
                                                                           pin.getGeoPos(), pin.getPixelPos());
                    pin.setPixelPos(pixelPos);
                }
            } else if (columnIndex == 4) {
                String strValue = (String) value;
                pin.setLabel(strValue);
            } else {
                throw new IllegalStateException("Column '" + getColumnName(columnIndex) + "' is not editable");
            }
        }
    }

    public void dispose() {
        if(product != null) {
            product.removeProductNodeListener(placemarkListener);
        }
    }

    private int getNumSelectedBands() {
        return selectedBands != null ? selectedBands.length : 0;
    }

    private class PlacemarkListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            fireTableDataChanged(event);
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            fireTableDataChanged(event);
        }

        public void nodeAdded(ProductNodeEvent event) {
            fireTableDataChanged(event);
        }

        public void nodeRemoved(ProductNodeEvent event) {
            fireTableDataChanged(event);
        }

        private void fireTableDataChanged(ProductNodeEvent event) {
            if (event.getSourceNode().getOwner() == placemarkDescriptor.getPlacemarkGroup(product) &&
                !ProductNode.PROPERTY_NAME_SELECTED.equals(event.getPropertyName())) {
                PinTableModel.this.fireTableDataChanged();
            }
        }
    }
}