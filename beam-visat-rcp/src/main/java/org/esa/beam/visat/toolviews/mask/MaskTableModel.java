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
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Color;

class MaskTableModel extends AbstractTableModel {

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
            80,
            40,
            60,
            40,
            320,
    };


    private ProductNodeGroup<Mask> maskGroup;
    private RasterDataNode visibleBand;

    MaskTableModel() {
    }

    ProductNodeGroup<Mask> getMaskGroup() {
        return maskGroup;
    }

    void reconfigure(ProductNodeGroup<Mask> maskGroup, RasterDataNode visibleBand) {
        this.maskGroup = maskGroup;
        this.visibleBand = visibleBand;
        fireTableStructureChanged();
    }


    void clear() {
        reconfigure(null, null);
    }


    void setMaskGroup(ProductNodeGroup<Mask> maskGroup) {
        if (this.maskGroup != maskGroup) {
            this.maskGroup = maskGroup;
            fireTableStructureChanged();
        }
    }

    RasterDataNode getVisibleBand() {
        return visibleBand;
    }


    void setVisibleBand(RasterDataNode visibleBand) {
        if (this.visibleBand != visibleBand) {
            if (visibleBand != null && visibleBand.getProduct() != null) {
                this.maskGroup = visibleBand.getProduct().getMaskGroup();
                this.visibleBand = visibleBand;
            } else {
                this.maskGroup = null;
                this.visibleBand = null;
            }
            fireTableStructureChanged();
        }
    }

    boolean isVisibleFlagColumnEnabled() {
        return visibleBand != null;
    }

    String getToolTipText(int rowIndex) {
        Mask mask = getMaskGroup().get(rowIndex);
        // todo - return appropriate info text
        return mask.getImageType().getClass().getName();
    }

    void configureColumnModel(TableColumnModel columnModel) {
        if (isVisibleFlagColumnEnabled()) {
            columnModel.getColumn(0).setHeaderRenderer(new VisibleFlagHeaderRenderer());
            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                columnModel.getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
            }
        } else {
            for (int i = 0; i < COLUMN_WIDTHS.length - 1; i++) {
                columnModel.getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i + 1]);
            }
        }
    }

    private int getColumnOffset() {
        return (isVisibleFlagColumnEnabled() ? 0 : 1);
    }

    /////////////////////////////////////////////////////////////////////////
    // TableModel Implementation

    @Override
    public Class getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[columnIndex + getColumnOffset()];
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex + getColumnOffset()];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return COLUMN_EDITABLE_STATES[columnIndex + getColumnOffset()];
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length - getColumnOffset();
    }

    @Override
    public int getRowCount() {
        return maskGroup != null ? maskGroup.getNodeCount() : 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int column = isVisibleFlagColumnEnabled() ? columnIndex : columnIndex + 1;

        Mask mask = getMaskGroup().get(rowIndex);

        if (column == 0) {
            if (visibleBand != null) {
                return visibleBand.getImageInfo(ProgressMonitor.NULL).containsMaskReference(mask.getName()) ? Boolean.TRUE : Boolean.FALSE;
            }
            return Boolean.FALSE;
        } else if (column == 1) {
            return mask.getName();
        } else if (column == 2) {
            return mask.getImageType().getName();
        } else if (column == 3) {
            return mask.getImageColor();
        } else if (column == 4) {
            return mask.getImageTransparency();
        } else if (column == 5) {
            return mask.getDescription();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        int column = isVisibleFlagColumnEnabled() ? columnIndex : columnIndex + 1;

        Mask mask = getMaskGroup().get(rowIndex);

        if (column == 0) {
            boolean visible = (Boolean) aValue;
            if (visible) {
                visibleBand.getImageInfo().addMaskReference(mask.getName());
            } else {
                visibleBand.getImageInfo().removeMaskReference(mask.getName());
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == 1) {
            mask.setName((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == 2) {
            // type is not editable!
        } else if (column == 3) {
            mask.setImageColor((Color) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == 4) {
            mask.setImageTransparency((Float) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (column == 5) {
            mask.setDescription((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

    }
}