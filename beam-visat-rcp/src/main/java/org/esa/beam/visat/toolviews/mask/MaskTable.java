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

import com.jidesoft.combobox.ColorExComboBox;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;

class MaskTable extends JTable {

    private final VisibilityHR visibilityHR;

    public MaskTable(boolean maskManagmentMode) {
        super(new MaskTableModel(maskManagmentMode));
        visibilityHR = new VisibilityHR();
        setName("maskTable");
        setAutoCreateColumnsFromModel(true);
        setPreferredScrollableViewportSize(new Dimension(200, 150));
        setDefaultRenderer(Color.class, new ColorCR());
        setDefaultEditor(Color.class, new ColorCE());
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        ToolTipMIL toolTipSetter = new ToolTipMIL();
        addMouseListener(toolTipSetter);
        addMouseMotionListener(toolTipSetter);
        setRowHeight(this.getRowHeight() + 4);
        reconfigureColumnModel();
    }

    @Override
    public MaskTableModel getModel() {
        return (MaskTableModel) super.getModel();
    }

    Product getProduct() {
        return getModel().getProduct();
    }

    void setProduct(Product product, RasterDataNode visibleBand) {
        saveColumnWidths();
        getModel().setProduct(product, visibleBand);
        reconfigureColumnModel();
    }

    Mask getSelectedMask() {
        int selectedRow = getSelectedRow();
        return selectedRow >= 0 ? getMask(selectedRow) : null;
    }

    Mask[] getSelectedMasks() {
        int[] rows = getSelectedRows();
        Mask[] masks = new Mask[rows.length];
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            masks[i] = getMask(row);
        }
        return masks;
    }

    void clear() {
        getModel().clear();
    }

    boolean isInManagmentMode() {
        return getModel().isInManagmentMode();
    }

    Mask getMask(int rowIndex) {
        return getModel().getMask(rowIndex);
    }

    void addMask(Mask mask) {
        getModel().addMask(mask);
        int rowIndex = getModel().getMaskIndex(mask.getName());
        getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
        scrollRectToVisible(getCellRect(rowIndex, 0, true));
    }

    public void insertMask(Mask mask, int index) {
        getModel().addMask(mask, index);
    }

    void removeMask(Mask mask) {
        getModel().removeMask(mask);
    }

    private void saveColumnWidths() {
        if (getRowCount() > 0) {
            MaskTableModel maskTableModel = getModel();
            for (int i = 0; i < maskTableModel.getColumnCount(); i++) {
                maskTableModel.setPreferredColumnWidth(i, columnModel.getColumn(i).getPreferredWidth());
            }
        }
    }

    private void reconfigureColumnModel() {
        createDefaultColumnsFromModel();
        TableColumnModel columnModel = getColumnModel();
        MaskTableModel maskTableModel = getModel();
        int vci = maskTableModel.getVisibilityColumnIndex();
        if (vci >= 0) {
            columnModel.getColumn(vci).setHeaderRenderer(visibilityHR);
        }
        for (int i = 0; i < maskTableModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(maskTableModel.getPreferredColumnWidth(i));
        }
    }

    private class ToolTipMIL extends MouseInputAdapter {

        private int currentRowIndex;

        ToolTipMIL() {
            currentRowIndex = -1;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            currentRowIndex = -1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int rowIndex = rowAtPoint(e.getPoint());
            if (rowIndex != this.currentRowIndex) {
                this.currentRowIndex = rowIndex;
                if (this.currentRowIndex >= 0 && this.currentRowIndex < getRowCount()) {
                    setToolTipText(getToolTipText(rowIndex));
                }
            }
        }

        private String getToolTipText(int rowIndex) {
            Mask mask = getMask(rowIndex);
            return mask.getDescription();
        }
    }

    private static class ColorCE extends ColorCellEditor {
        @Override
        protected ColorExComboBox createColorComboBox() {
            ColorExComboBox comboBox = super.createColorComboBox();
            comboBox.setColorValueVisible(true);
            comboBox.setColorIconVisible(true);
            comboBox.setInvalidValueAllowed(false);
            comboBox.setAllowDefaultColor(true);
            comboBox.setAllowMoreColors(true);
            return comboBox;
        }
    }

    private static class ColorCR extends ColorCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setColorIconVisible(true);
            setColorValueVisible(true);
            setCrossBackGroundStyle(true);
            return super.getTableCellRendererComponent(table, value,
                                                       isSelected, hasFocus, row, column);
        }
    }

    private static class VisibilityHR extends JLabel implements TableCellRenderer {

        VisibilityHR() {
            ImageIcon icon = UIUtils.loadImageIcon("icons/EyeIcon10.gif");
            this.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            this.setText(null);
            this.setIcon(icon);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setPreferredSize(this.getPreferredSize());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            return this;
        }
    }
/*
    private class ColumnReconfigurerTML implements TableModelListener {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE
                    && e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                reconfigureColumnModel();
            }
        }
    }
*/
}