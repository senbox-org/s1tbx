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

package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.table.AbstractTableModel;
import java.awt.Color;

class MaskTableModel extends AbstractTableModel {

    private static final int IDX_VISIBILITY = 0;
    private static final int IDX_ROI = 1;
    private static final int IDX_NAME = 2;
    private static final int IDX_TYPE = 3;
    private static final int IDX_COLOR = 4;
    private static final int IDX_TRANSPARENCY = 5;
    private static final int IDX_DESCRIPTION = 6;

    /**
     * Mask management mode, no visibility control.
     */
    private static final int[] IDXS_MODE_MANAG_NO_BAND = {
            IDX_NAME,
            IDX_TYPE,
            IDX_COLOR,
            IDX_TRANSPARENCY,
            IDX_DESCRIPTION,
    };

    /**
     * Mask management mode, with visibility control.
     */
    private static final int[] IDXS_MODE_MANAG_BAND = {
            IDX_VISIBILITY,
            IDX_ROI,
            IDX_NAME,
            IDX_TYPE,
            IDX_COLOR,
            IDX_TRANSPARENCY,
            IDX_DESCRIPTION,
    };

    /**
     * List only, no mask type management, no visibility control.
     */
    private static final int[] IDXS_MODE_NO_MANAG_NO_BAND = {
            IDX_NAME,
            IDX_COLOR,
            IDX_TRANSPARENCY,
            IDX_DESCRIPTION,
    };

    /**
     * List only, no mask type management, with visibility control.
     */
    private static final int[] IDXS_MODE_NO_MANAG_BAND = {
            IDX_VISIBILITY,
            IDX_NAME,
            IDX_COLOR,
            IDX_TRANSPARENCY,
            IDX_DESCRIPTION,
    };

    private static final Class[] COLUMN_CLASSES = {
            Boolean.class,
            Boolean.class,
            String.class,
            String.class,
            Color.class,
            Double.class,
            String.class,
    };

    private static final String[] COLUMN_NAMES = {
            "Visibility",
            "ROI",
            "Name",
            "Type",
            "Colour",
            "Transparency",
            "Description",
    };

    private static final boolean[] COLUMN_EDITABLE_STATES = {
            true,
            true,
            true,
            false,
            true,
            true,
            true,
    };

    static int[] COLUMN_WIDTHS = {
            24,
            36,
            60,
            60,
            60,
            40,
            320,
    };


    private final boolean inManagmentMode;
    private int[] modeIdxs;
    private Product product;
    private RasterDataNode visibleBand;
    private final MaskPNL maskPNL;

    MaskTableModel(boolean inManagmentMode) {
        this.inManagmentMode = inManagmentMode;
        updateModeIdxs();
        maskPNL = new MaskPNL();
    }

    Product getProduct() {
        return product;
    }

    void setProduct(Product product) {
        setProduct(product, null);
    }

    void setProduct(Product product, RasterDataNode visibleBand) {
        if (this.product != product) {
            if (this.product != null) {
                this.product.removeProductNodeListener(maskPNL);
            }
            this.product = product;
            if (this.product != null) {
                this.product.addProductNodeListener(maskPNL);
            }
        }
        this.visibleBand = visibleBand;
        updateModeIdxs();
        fireTableStructureChanged();
    }

    RasterDataNode getVisibleBand() {
        return visibleBand;
    }

    Mask getMask(int selectedRow) {
        ProductNodeGroup<Mask> maskGroup = getMaskGroup();
        int modelIndex = convertRow2ModelIndex(maskGroup, selectedRow);
        return maskGroup.get(modelIndex);
    }

    int getMaskIndex(String name) {
        int modelIndex = getMaskGroup().indexOf(name);
        int rowIndex = convertModel2RowIndex(getMaskGroup(), modelIndex);
        return rowIndex;
    }

    void addMask(Mask mask) {
        getProduct().getMaskGroup().add(mask);
        fireTableDataChanged();
    }

    public void addMask(Mask mask, int index) {
        ProductNodeGroup<Mask> maskGroup = getProduct().getMaskGroup();
        int modelIndex = convertRow2ModelIndex(maskGroup, index);
        maskGroup.add(modelIndex, mask);
        fireTableDataChanged();
    }

    void removeMask(Mask mask) {
        getProduct().getMaskGroup().remove(mask);
        fireTableDataChanged();
    }

    private ProductNodeGroup<Mask> getMaskGroup() {
        return product != null ? product.getMaskGroup() : null;
    }

    boolean isInManagmentMode() {
        return product != null && inManagmentMode;
    }

    int getVisibilityColumnIndex() {
        for (int i = 0; i < modeIdxs.length; i++) {
            if (modeIdxs[i] == IDX_VISIBILITY) {
                return i;
            }
        }
        return -1;
    }

    int getPreferredColumnWidth(int columnIndex) {
        return COLUMN_WIDTHS[modeIdxs[columnIndex]];
    }

    private void updateModeIdxs() {
        this.modeIdxs =
                inManagmentMode
                        ? (this.visibleBand != null ? IDXS_MODE_MANAG_BAND : IDXS_MODE_MANAG_NO_BAND)
                        : (this.visibleBand != null ? IDXS_MODE_NO_MANAG_BAND : IDXS_MODE_NO_MANAG_NO_BAND);
    }

    void clear() {
        setProduct(null, null);
    }

    /////////////////////////////////////////////////////////////////////////
    // TableModel Implementation

    @Override
    public Class getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[modeIdxs[columnIndex]];
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[modeIdxs[columnIndex]];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return COLUMN_EDITABLE_STATES[modeIdxs[columnIndex]];
    }

    @Override
    public int getColumnCount() {
        return modeIdxs.length;
    }

    @Override
    public int getRowCount() {
        ProductNodeGroup<Mask> maskGroup = getMaskGroup();
        int maskCount = 0;
        if (maskGroup != null) {
            maskCount = maskGroup.getNodeCount();
            if(isPlacemarkGroupEmpty(maskGroup.getProduct().getPinGroup())) {
                maskCount--;
            }
            if(isPlacemarkGroupEmpty(maskGroup.getProduct().getGcpGroup())) {
                maskCount--;
            }
        }
        return maskCount;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final ProductNodeGroup<Mask> maskGroup = getMaskGroup();
        int modelIndex = convertRow2ModelIndex(maskGroup, rowIndex);
        Mask mask = maskGroup.get(modelIndex);
        int column = modeIdxs[columnIndex];

        if (column == IDX_VISIBILITY) {
            if (visibleBand.getOverlayMaskGroup().contains(mask)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else if (column == IDX_ROI) {
            if (visibleBand.getRoiMaskGroup().contains(mask)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else if (column == IDX_NAME) {
            return mask.getName();
        } else if (column == IDX_TYPE) {
            return mask.getImageType().getName();
        } else if (column == IDX_COLOR) {
            return mask.getImageColor();
        } else if (column == IDX_TRANSPARENCY) {
            return mask.getImageTransparency();
        } else if (column == IDX_DESCRIPTION) {
            return mask.getDescription();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        final ProductNodeGroup<Mask> maskGroup = getMaskGroup();
        int modelIndex = convertRow2ModelIndex(maskGroup, rowIndex);
        Mask mask = maskGroup.get(modelIndex);
        int column = modeIdxs[columnIndex];

        if (column == IDX_VISIBILITY) {
            boolean visible = (Boolean) aValue;
            final ProductNodeGroup<Mask> overlayMaskGroup = visibleBand.getOverlayMaskGroup();
            if (visible) {
                if (!overlayMaskGroup.contains(mask)) {
                    overlayMaskGroup.add(mask);
                }
            } else {
                overlayMaskGroup.remove(mask);
            }
            visibleBand.fireImageInfoChanged();
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_ROI) {
            boolean isRoi = (Boolean) aValue;
            final ProductNodeGroup<Mask> roiMaskGroup = visibleBand.getRoiMaskGroup();
            if (isRoi) {
                if (!roiMaskGroup.contains(mask)) {
                    roiMaskGroup.add(mask);
                }
            } else {
                roiMaskGroup.remove(mask);
            }
            visibleBand.fireImageInfoChanged();
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_NAME) {
            mask.setName((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_TYPE) {
            // type is not editable!
        } else if (column == IDX_COLOR) {
            mask.setImageColor((Color) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_TRANSPARENCY) {
            mask.setImageTransparency((Double) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_DESCRIPTION) {
            mask.setDescription((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

    }

    private int convertModel2RowIndex(ProductNodeGroup<Mask> maskGroup, int modelIndex) {
        final PlacemarkGroup pinGroup = maskGroup.getProduct().getPinGroup();
        final int pinIndex = maskGroup.indexOf(Product.PIN_MASK_NAME);

        final PlacemarkGroup gcpGroup = maskGroup.getProduct().getGcpGroup();
        final int gcpIndex = maskGroup.indexOf(Product.GCP_MASK_NAME);
        int rowIndex = modelIndex;

        if(isPlacemarkGroupEmpty(pinGroup) && rowIndex >= pinIndex ) {
            rowIndex--;
        }
        if( isPlacemarkGroupEmpty(gcpGroup) && rowIndex >= gcpIndex ) {
            rowIndex--;
        }
        return rowIndex;
    }

    private int convertRow2ModelIndex(ProductNodeGroup<Mask> maskGroup, int rowIndex) {
        final PlacemarkGroup pinGroup = maskGroup.getProduct().getPinGroup();
        final int pinIndex = maskGroup.indexOf(Product.PIN_MASK_NAME);

        final PlacemarkGroup gcpGroup = maskGroup.getProduct().getGcpGroup();
        final int gcpIndex = maskGroup.indexOf(Product.GCP_MASK_NAME);
        int modelIndex = rowIndex;

        if(isPlacemarkGroupEmpty(pinGroup) && modelIndex >= pinIndex ) {
            modelIndex++;
        }
        if( isPlacemarkGroupEmpty(gcpGroup) && modelIndex >= gcpIndex ) {
            modelIndex++;
        }
        return modelIndex;
    }

    private boolean isPlacemarkGroupEmpty(PlacemarkGroup placemarkGroup) {
        return placemarkGroup.getNodeCount() == 0;
    }

    private class MaskPNL extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            processEvent(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            processEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            processEvent(event);
        }

        private void processEvent(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Mask) {
                fireTableDataChanged();
            }else if (event.getSourceNode() instanceof Placemark){
                fireTableDataChanged();
            }else if (event.getSourceNode() == visibleBand
                    && event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_IMAGE_INFO)) {
                fireTableDataChanged();
            }
        }

    }

}