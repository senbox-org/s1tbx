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

package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.math.MathUtils;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public abstract class AbstractPlacemarkTableModel extends DefaultTableModel {

    private final PlacemarkDescriptor placemarkDescriptor;

    private Product product;
    private Band[] selectedBands;
    private TiePointGrid[] selectedGrids;

    private final PlacemarkListener placemarkListener;
    private final ArrayList<Placemark> placemarkList;

    protected AbstractPlacemarkTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                                       TiePointGrid[] selectedGrids) {
        this.placemarkDescriptor = placemarkDescriptor;
        this.product = product;
        initSelectedBands(selectedBands);
        initSelectedGrids(selectedGrids);
        placemarkList = new ArrayList<Placemark>(10);
        placemarkListener = new PlacemarkListener();
        if (product != null) {
            product.addProductNodeListener(placemarkListener);
        }
        initPlacemarkList(product);
    }

    public Placemark[] getPlacemarks() {
        return placemarkList.toArray(new Placemark[placemarkList.size()]);
    }

    public Placemark getPlacemarkAt(int modelRow) {
        return placemarkList.get(modelRow);
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        if (this.product == product) {
            return;
        }
        if (this.product != null) {
            this.product.removeProductNodeListener(placemarkListener);
        }
        this.product = product;
        if (this.product != null) {
            this.product.addProductNodeListener(placemarkListener);
        }

        placemarkList.clear();
        initPlacemarkList(this.product);
        selectedBands = new Band[0];
        selectedGrids = new TiePointGrid[0];
        fireTableStructureChanged();
    }

    public Band[] getSelectedBands() {
        return selectedBands;
    }

    public void setSelectedBands(Band[] selectedBands) {
        this.selectedBands = selectedBands != null ? selectedBands : new Band[0];
        fireTableStructureChanged();
    }

    public TiePointGrid[] getSelectedGrids() {
        return selectedGrids;
    }

    public void setSelectedGrids(TiePointGrid[] selectedGrids) {
        this.selectedGrids = selectedGrids != null ? selectedGrids : new TiePointGrid[0];
        fireTableStructureChanged();
    }

    public boolean addPlacemark(Placemark placemark) {
        if (placemarkList.add(placemark)) {
            final int insertedRowIndex = placemarkList.indexOf(placemark);
            fireTableRowsInserted(insertedRowIndex, insertedRowIndex);
            return true;
        }
        return false;
    }

    public boolean removePlacemark(Placemark placemark) {
        final int index = placemarkList.indexOf(placemark);
        if (index != -1) {
            placemarkList.remove(placemark);
            fireTableRowsDeleted(index, index);
            return true;
        }
        return false;
    }

    public void removePlacemarkAt(int index) {
        if (placemarkList.size() > index) {
            final Placemark placemark = placemarkList.get(index);
            removePlacemark(placemark);
        }
    }

    public abstract String[] getStandardColumnNames();

    @Override
    public abstract boolean isCellEditable(int rowIndex, int columnIndex);

    protected abstract Object getStandardColumnValueAt(int rowIndex, int columnIndex);

    @Override
    public int getRowCount() {
        if (placemarkList == null) {
            return 0;
        }
        return placemarkList.size();
    }

    @Override
    public int getColumnCount() {
        int count = getStandardColumnNames().length;
        if (selectedBands != null) {
            count += selectedBands.length;
        }
        if (selectedGrids != null) {
            count += selectedGrids.length;
        }
        return count;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex < getStandardColumnNames().length) {
            return getStandardColumnNames()[columnIndex];
        }
        int newIndex = columnIndex - getStandardColumnNames().length;
        if (newIndex < getNumSelectedBands()) {
            return selectedBands[newIndex].getName();
        }
        newIndex -= getNumSelectedBands();
        if (selectedGrids != null && newIndex < selectedGrids.length) {
            return selectedGrids[newIndex].getName();
        }
        return "?";
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < getStandardColumnNames().length - 1) {
            return Float.class;
        }
        return Object.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex < getStandardColumnNames().length) {
            return getStandardColumnValueAt(rowIndex, columnIndex);
        }

        final Placemark placemark = placemarkList.get(rowIndex);
        int index = columnIndex - getStandardColumnNames().length;
        PixelPos pixelPos = placemark.getPixelPos();
        if (pixelPos == null) {
            return "No-data";
        }

        final int x = MathUtils.floorInt(pixelPos.getX());
        final int y = MathUtils.floorInt(pixelPos.getY());
        if (product != null) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();

            if (x < 0 || x >= width || y < 0 || y >= height) {
                return "No-data";
            }
        }

        if (index < getNumSelectedBands()) {
            final Band band = selectedBands[index];
            if (band.isPixelValid(x, y)) {
                try {
                    float[] value = null;
                    value = band.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                    return value[0];
                } catch (IOException ignored) {
                    return "I/O-error";
                }
            } else {
                return "NaN";
            }
        }
        index -= getNumSelectedBands();
        if (index < selectedGrids.length) {
            final TiePointGrid grid = selectedGrids[index];
            try {
                float[] value = null;
                value = grid.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                return value[0];
            } catch (IOException ignored) {
                return "I/O-error";
            }
        }

        return "";
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (value == null) {
            return;
        }
        if (columnIndex < getStandardColumnNames().length) {
            Placemark placemark = placemarkList.get(rowIndex);
            if (columnIndex == 0) {
                setPixelPosX(value, placemark);
            } else if (columnIndex == 1) {
                setPixelPosY(value, placemark);
            } else if (columnIndex == 2) {
                this.setGeoPosLon(value, placemark);
            } else if (columnIndex == 3) {
                setGeoPosLat(value, placemark);
            } else if (columnIndex == getStandardColumnNames().length - 1) {
                String strValue = value.toString();
                placemark.setLabel(strValue);
            } else {
                throw new IllegalStateException(
                        "Column[" + columnIndex + "] '" + getColumnName(columnIndex) + "' is not editable");
            }
        }
    }

    public void dispose() {
        if (product != null) {
            product.removeProductNodeListener(placemarkListener);
        }
        selectedBands = null;
        selectedGrids = null;
        placemarkList.clear();
    }

    protected void setGeoPosLat(Object lat, Placemark placemark) {
        if (lat instanceof Float) {
            float lon;
            if (placemark.getGeoPos() == null) {
                lon = Float.NaN;
            } else {
                lon = placemark.getGeoPos().lon;
            }
            placemark.setGeoPos(new GeoPos((Float) lat, lon));
        }
    }

    protected void setGeoPosLon(Object lon, Placemark placemark) {
        if (lon instanceof Float) {
            float lat;
            if (placemark.getGeoPos() == null) {
                lat = Float.NaN;
            } else {
                lat = placemark.getGeoPos().lat;
            }
            placemark.setGeoPos(new GeoPos(lat, (Float) lon));
        }
    }

    protected void setPixelPosY(Object value, Placemark placemark) {
        if (value instanceof Float) {
            float pixelX;
            if (placemark.getPixelPos() == null) {
                pixelX = -1;
            } else {
                pixelX = placemark.getPixelPos().x;
            }
            placemark.setPixelPos(new PixelPos(pixelX, (Float) value));
        }
    }

    protected void setPixelPosX(Object value, Placemark placemark) {
        if (value instanceof Float) {
            float pixelY;
            if (placemark.getPixelPos() == null) {
                pixelY = -1;
            } else {
                pixelY = placemark.getPixelPos().y;
            }
            placemark.setPixelPos(new PixelPos((Float) value, pixelY));
        }
    }

    private void initSelectedBands(Band[] selectedBands) {
        this.selectedBands = selectedBands != null ? selectedBands : new Band[0];
    }

    private void initSelectedGrids(TiePointGrid[] selectedGrids) {
        this.selectedGrids = selectedGrids != null ? selectedGrids : new TiePointGrid[0];
    }

    private void initPlacemarkList(Product product) {
        if (product != null) {
            Placemark[] placemarks = placemarkDescriptor.getPlacemarkGroup(product).toArray(new Placemark[0]);
            placemarkList.addAll(Arrays.asList(placemarks));
        }
    }

    private int getNumSelectedBands() {
        return selectedBands != null ? selectedBands.length : 0;
    }

    private class PlacemarkListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            fireTableDataChanged(event);
        }
        
        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                Band sourceBand = (Band) event.getSourceNode();
                if (selectedBands != null) {
                    for (Band band : selectedBands) {
                        if (band == sourceBand) {
                            AbstractPlacemarkTableModel.this.fireTableDataChanged();
                            return;
                        }
                    }
                }
            }
            if (event.getSourceNode() instanceof TiePointGrid) {
                TiePointGrid sourceTPG = (TiePointGrid) event.getSourceNode();
                if (selectedGrids != null) {
                    for (TiePointGrid tpg : selectedGrids) {
                        if (tpg == sourceTPG) {
                            AbstractPlacemarkTableModel.this.fireTableDataChanged();
                            return;
                        }
                    }
                }
            }
        }

        private void fireTableDataChanged(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Placemark) {
                Placemark placemark = (Placemark) event.getSourceNode();
                // BEAM-1117: VISAT slows down using pins with GCP geo-coded images
                final int index = placemarkList.indexOf(placemark);
                if (index != -1) {
                    AbstractPlacemarkTableModel.this.fireTableRowsUpdated(index, index);
                }
            }
        }
    }
}
