package org.esa.beam.visat.toolviews.imageinfo;

import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.awt.Component;

class Continuous1BandTabularForm implements ColorManipulationChildForm {
    private final ColorManipulationForm parentForm;
    private ImageInfoTableModel tableModel;
    private JScrollPane contentPanel;
    private final TableModelListener applyEnablerTML;

    public Continuous1BandTabularForm(ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        tableModel = new ImageInfoTableModel(null);
        applyEnablerTML = parentForm.createApplyEnablerTableModelListener();

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

    public AbstractButton[] getButtons() {
        return new AbstractButton[]{};  // todo - ?
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public void handleFormShown(ProductSceneView productSceneView) {
        tableModel.setImageInfo(parentForm.getImageInfo());
        tableModel.addTableModelListener(applyEnablerTML);
    }

    public void handleFormHidden(ProductSceneView productSceneView) {
        tableModel.removeTableModelListener(applyEnablerTML);
        tableModel.setImageInfo(null);
    }

    public void updateFormModel(ProductSceneView productSceneView) {
        tableModel.setImageInfo(parentForm.getImageInfo());
    }

    private static class ImageInfoTableModel extends AbstractTableModel {

        private ImageInfo imageInfo;
        private static final String[] COLUMN_NAMES = new String[]{"Color", "Value"};
        private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{Color.class, Double.class};

        private ImageInfoTableModel(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        public void setImageInfo(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
            fireTableDataChanged();
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

        public int getRowCount() {
            return imageInfo != null ? imageInfo.getColorPaletteDef().getNumPoints() : 0;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                return point.getColor();
            } else if (columnIndex == 1) {
                return point.getSample();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                point.setColor((Color) aValue);
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