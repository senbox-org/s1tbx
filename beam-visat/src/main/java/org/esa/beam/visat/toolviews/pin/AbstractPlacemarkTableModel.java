package org.esa.beam.visat.toolviews.pin;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.math.MathUtils;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;


abstract class AbstractPlacemarkTableModel extends DefaultTableModel {

    protected final PlacemarkDescriptor placemarkDescriptor;

    protected final Product product;

    protected final Band[] selectedBands;

    protected final TiePointGrid[] selectedGrids;

    protected PlacemarkListener placemarkListener;

    public AbstractPlacemarkTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
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

    public abstract String[] getStandardColumnNames();

    public int getRowCount() {
        if (product != null) {
            return placemarkDescriptor.getPlacemarkGroup(product).getNodeCount();
        }
        return 0;
    }

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

    public Class getColumnClass(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < getStandardColumnNames().length - 1) {
            return Float.class;
        }
        return String.class;
    }

    public abstract boolean isCellEditable(int rowIndex, int columnIndex);

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (product != null) {
            if (columnIndex < getStandardColumnNames().length) {
                return getStandardColumnValueAt(rowIndex, columnIndex);
            } else {
                final Pin pin = placemarkDescriptor.getPlacemarkGroup(product).get(rowIndex);
                int index = columnIndex - getStandardColumnNames().length + 1;
                PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos == null) {
                    return "No-data";
                }

                final int x = MathUtils.floorInt(pixelPos.getX());
                final int y = MathUtils.floorInt(pixelPos.getY());
                final int width = product.getSceneRasterWidth();
                final int height = product.getSceneRasterHeight();

                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return "No-data";
                }

                if (index < getNumSelectedBands()) {
                    final Band band = selectedBands[index];
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
                index -= getNumSelectedBands();
                if (selectedGrids != null && index < selectedGrids.length) {
                    final TiePointGrid grid = selectedGrids[index];
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

    protected abstract Object getStandardColumnValueAt(int rowIndex, int columnIndex);

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (value == null) {
            return;
        }
        if (columnIndex < getStandardColumnNames().length) {
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
            } else if (columnIndex == getStandardColumnNames().length - 1) {
                String strValue = value.toString();
                pin.setLabel(strValue);
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
    }

    private int getNumSelectedBands() {
        return selectedBands != null ? selectedBands.length : 0;
    }

    protected class PlacemarkListener implements ProductNodeListener {

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
            if ((event.getSourceNode().getOwner() == placemarkDescriptor.getPlacemarkGroup(product) ||
                 event.getSourceNode() instanceof Product) &&
                                                           !ProductNode.PROPERTY_NAME_SELECTED.equals(
                                                                   event.getPropertyName())) {
                AbstractPlacemarkTableModel.this.fireTableDataChanged();
            }
        }
    }
}
