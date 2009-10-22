/*
 * $Id: MaskTableModel.java,v 1.1 2007/04/19 10:41:38 norman Exp $
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

package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Color;

class MaskTableModel extends AbstractTableModel {

    private static final int IDX_VIS = 0;
    private static final int IDX_NAME = 1;
    private static final int IDX_TYPE = 2;
    private static final int IDX_COLOR = 3;
    private static final int IDX_TRANSP = 4;
    private static final int IDX_DESCR = 5;

    private static final int[] IDXS_MODE_1 = {
            IDX_NAME,
            IDX_COLOR,
            IDX_TRANSP,
            IDX_DESCR,
    };

    private static final int[] IDXS_MODE_2 = {
            IDX_VIS,
            IDX_NAME,
            IDX_COLOR,
            IDX_TRANSP,
            IDX_DESCR,
    };

    private static final int[] IDXS_MODE_3 = {
            IDX_NAME,
            IDX_TYPE,
            IDX_COLOR,
            IDX_TRANSP,
            IDX_DESCR,
    };

    private static final int[] IDXS_MODE_4 = {
            IDX_VIS,
            IDX_TYPE,
            IDX_NAME,
            IDX_COLOR,
            IDX_TRANSP,
            IDX_DESCR,
    };

    private static final Class[] COLUMN_CLASSES = {
            Boolean.class,
            String.class,
            String.class,
            Color.class,
            Float.class,
            String.class,
    };

    private static final String[] COLUMN_NAMES = {
            "Vis",
            "Name",
            "Type",
            "Color",
            "Transparency",
            "Description",
    };

    private static final boolean[] COLUMN_EDITABLE_STATES = {
            true,
            true,
            false,
            true,
            true,
            true,
    };

    static int[] COLUMN_WIDTHS = {
            24,
            60,
            60,
            60,
            40,
            320,
    };


    private final boolean noTypeMode;
    private int[] modeIdxs;
    private Product product;
    private RasterDataNode visibleBand;

    MaskTableModel(boolean noTypeMode) {
        this.noTypeMode = noTypeMode;
        updateModeIdxs();
    }

    Product getProduct() {
        return product;
    }

    ProductNodeGroup<Mask> getMaskGroup() {
        return product != null ? product.getMaskGroup() : null;
    }

    void reconfigure(Product product, RasterDataNode visibleBand) {
        this.product = product;
        this.visibleBand = visibleBand;
        updateModeIdxs();
        fireTableStructureChanged();
    }

    private void updateModeIdxs() {
        this.modeIdxs = noTypeMode
                ? (this.visibleBand == null ? IDXS_MODE_1 : IDXS_MODE_2)
                : (this.visibleBand == null ? IDXS_MODE_3 : IDXS_MODE_4);
    }

    void clear() {
        reconfigure(null, null);
    }

    String getToolTipText(int rowIndex) {
        Mask mask = getMaskGroup().get(rowIndex);
        // todo - return appropriate info text
        return mask.getImageType().getClass().getName();
    }

    void configureColumnModel(TableColumnModel columnModel) {
        if (modeIdxs[0] == IDX_VIS) {
            columnModel.getColumn(0).setHeaderRenderer(new VisibleFlagHeaderRenderer());
        }
        for (int i = 0; i < modeIdxs.length; i++) {
            columnModel.getColumn(i).setPreferredWidth(COLUMN_WIDTHS[modeIdxs[i]]);
        }
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
        return maskGroup != null ? maskGroup.getNodeCount() : 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        Mask mask = getMaskGroup().get(rowIndex);
        int column = modeIdxs[columnIndex];

        if (column == IDX_VIS) {
            if (visibleBand.getImageInfo(ProgressMonitor.NULL).containsMaskReference(mask.getName())) {
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
        } else if (column == IDX_TRANSP) {
            return mask.getImageTransparency();
        } else if (column == IDX_DESCR) {
            return mask.getDescription();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        Mask mask = getMaskGroup().get(rowIndex);
        int column = modeIdxs[columnIndex];

        if (column == IDX_VIS) {
            boolean visible = (Boolean) aValue;
            if (visible) {
                visibleBand.getImageInfo().addMaskReference(mask.getName());
            } else {
                visibleBand.getImageInfo().removeMaskReference(mask.getName());
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_NAME) {
            mask.setName((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_TYPE) {
            // type is not editable!
        } else if (column == IDX_COLOR) {
            mask.setImageColor((Color) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_TRANSP) {
            mask.setImageTransparency((Float) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == IDX_DESCR) {
            mask.setDescription((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

    }
}