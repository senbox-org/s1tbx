package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;

class Discrete1BandTabularForm implements PaletteEditorForm {
    private JComponent contentPanel;

    private ImageInfoTableModel tableModel;

    public Discrete1BandTabularForm(final ColorManipulationForm colorManipulationForm) {
        tableModel = new ImageInfoTableModel(null);
        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                colorManipulationForm.setApplyEnabled(true);
            }
        });

        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        table.setDefaultRenderer(Color.class, colorCellRenderer);
        table.setDefaultEditor(Color.class, new ColorCellEditor());

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setPreferredSize(table.getPreferredSize());
        contentPanel = tableScrollPane;
    }


    public void performApply(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        productSceneView.getRaster().setImageInfo(tableModel.getImageInfo().createDeepCopy());
    }

    public void performReset(ProductSceneView productSceneView) {
    }

    public void handleFormShown(ProductSceneView productSceneView) {
        tableModel.setImageInfo(productSceneView.getRaster().getImageInfo().createDeepCopy());
    }

    public void handleFormHidden() {
        tableModel.setImageInfo(null);
    }

    public void updateState(ProductSceneView productSceneView) {
    }


    public AbstractButton[] getButtons() {
        return new AbstractButton[]{};
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public ImageInfo getCurrentImageInfo() {
        return tableModel.getImageInfo();
    }

    public String getTitle(ProductSceneView productSceneView) {
        return "Indexed";
    }

    private static class PercentageRenderer extends DefaultTableCellRenderer {

        private final NumberFormat formatter;

        public PercentageRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
            formatter = NumberFormat.getPercentInstance();
            formatter.setMinimumFractionDigits(1);
            formatter.setMaximumFractionDigits(3);
        }

        @Override
        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    private static class ImageInfoTableModel extends AbstractTableModel {

        private ImageInfo imageInfo;
        private static final String[] COLUMN_NAMES = new String[]{"Label", "Colour", "Value", "Freq."};
        private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{String.class, Color.class, String.class, Double.class};

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
                return point.getLabel();
            } else if (columnIndex == 1) {
                return point.getColor();
            } else if (columnIndex == 2) {
                return Double.isNaN(point.getSample()) ? "Uncoded" : "" + (int) point.getSample();
            } else if (columnIndex == 3) {
                // todo - return abundance percentage
                if (imageInfo == null
                        || imageInfo.getHistogramBins() == null
                        || imageInfo.getHistogramBins().length >= getRowCount()) {
                    return Double.NaN;
                }
                return imageInfo.getHistogramBins()[rowIndex];
            } else {
                return 0;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                point.setLabel((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            } else if (columnIndex == 1) {
                point.setColor((Color) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 1;
        }

    }
}