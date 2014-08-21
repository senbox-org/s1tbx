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

package org.esa.beam.visat.toolviews.imageinfo;

import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.awt.Component;

class Continuous1BandTabularForm implements ColorManipulationChildForm {

    private static final String[] COLUMN_NAMES = new String[]{"Colour", "Value"};
    private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{Color.class, Double.class};

    private final ColorManipulationForm parentForm;
    private ImageInfoTableModel tableModel;
    private JScrollPane contentPanel;
    private final MoreOptionsForm moreOptionsForm;
    private TableModelListener tableModelListener;
    private final DiscreteCheckBox discreteCheckBox;

    public Continuous1BandTabularForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        tableModel = new ImageInfoTableModel();
        tableModelListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                tableModel.removeTableModelListener(tableModelListener);
                parentForm.applyChanges();
                tableModel.addTableModelListener(tableModelListener);
            }
        };
        moreOptionsForm = new MoreOptionsForm(parentForm, parentForm.getFormModel().canUseHistogramMatching());
        discreteCheckBox = new DiscreteCheckBox(parentForm);
        moreOptionsForm.addRow(discreteCheckBox);
        parentForm.getFormModel().modifyMoreOptionsForm(moreOptionsForm);

        final JTable table = new JTable(tableModel);
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        table.setDefaultRenderer(Color.class, colorCellRenderer);
        table.setDefaultEditor(Color.class, new ColorCellEditor());
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setPreferredSize(table.getPreferredSize());
        contentPanel = tableScrollPane;
    }

    @Override
    public void handleFormShown(FormModel formModel) {
        updateFormModel(formModel);
        tableModel.addTableModelListener(tableModelListener);
    }

    @Override
    public void handleFormHidden(FormModel formModel) {
        tableModel.removeTableModelListener(tableModelListener);
    }

    @Override
    public void updateFormModel(FormModel formModel) {
        tableModel.fireTableDataChanged();
        discreteCheckBox.setDiscreteColorsMode(parentForm.getFormModel().getModifiedImageInfo().getColorPaletteDef().isDiscrete());
    }

    @Override
    public void resetFormModel(FormModel formModel) {
        tableModel.fireTableDataChanged();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[0];
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getFormModel().getRasters();
    }

    private class ImageInfoTableModel extends AbstractTableModel {

        private ImageInfoTableModel() {
        }

        public ImageInfo getImageInfo() {
            return  parentForm.getFormModel().getModifiedImageInfo();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMN_TYPES[columnIndex];
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            if (getImageInfo() == null) {
                return 0;
            }
            return getImageInfo().getColorPaletteDef().getNumPoints();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = getImageInfo().getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                final Color color = point.getColor();
                return color.equals(ImageInfo.NO_COLOR) ? null : color;
            } else if (columnIndex == 1) {
                return point.getSample();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = getImageInfo().getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                final Color color = (Color) aValue;
                point.setColor(color == null ? ImageInfo.NO_COLOR : color);
                fireTableCellUpdated(rowIndex, columnIndex);
            } else if (columnIndex == 1) {
                point.setSample((Double) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 1;
        }

    }
}
