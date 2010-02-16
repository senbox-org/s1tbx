package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import com.jidesoft.grid.SortableTable;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;

class Discrete1BandTabularForm implements ColorManipulationChildForm {
    private static final String[] COLUMN_NAMES = new String[]{"Label", "Colour", "Value", "Frequency", "Description"};
    private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{String.class, Color.class, String.class, Double.class, String.class};

    private final ColorManipulationForm parentForm;
    private JComponent contentPanel;
    private ImageInfoTableModel tableModel;
    private final TableModelListener applyEnablerTML;
    private MoreOptionsForm moreOptionsForm;

    public Discrete1BandTabularForm(ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        tableModel = new ImageInfoTableModel();
        applyEnablerTML = parentForm.createApplyEnablerTableModelListener();
        moreOptionsForm = new MoreOptionsForm(parentForm, false);

        final JTable table = new SortableTable(tableModel);
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        table.setDefaultRenderer(Color.class, colorCellRenderer);
        table.setDefaultEditor(Color.class, new ColorCellEditor());
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setCellRenderer(new PercentageRenderer());

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setPreferredSize(table.getPreferredSize());
        contentPanel = tableScrollPane;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
        tableModel.addTableModelListener(applyEnablerTML);
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        tableModel.removeTableModelListener(applyEnablerTML);
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        parentForm.getStx(productSceneView.getRaster());
        tableModel.fireTableDataChanged();
    }

    @Override
    public void resetFormModel(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[0];
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getProductSceneView().getRasters();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    private static class PercentageRenderer extends DefaultTableCellRenderer {

        private final NumberFormat formatter;

        public PercentageRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
            formatter = NumberFormat.getPercentInstance();
            formatter.setMinimumFractionDigits(3);
            formatter.setMaximumFractionDigits(3);
        }

        @Override
        public void setValue(Object value) {
            setText(formatter.format(value));
        }
    }

    private class ImageInfoTableModel extends AbstractTableModel {

        private ImageInfoTableModel() {
        }

        public ImageInfo getImageInfo() {
            return parentForm.getImageInfo();
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
            if (getImageInfo() == null) {
                return 0;
            }
            return getImageInfo().getColorPaletteDef().getNumPoints();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (getImageInfo() == null) {
                return null;
            }
            final ColorPaletteDef.Point point = getImageInfo().getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                return point.getLabel();
            } else if (columnIndex == 1) {
                final Color color = point.getColor();
                return color.equals(ImageInfo.NO_COLOR) ? null : color;
            } else if (columnIndex == 2) {
                return Double.isNaN(point.getSample()) ? "Uncoded" : "" + (int) point.getSample();
            } else if (columnIndex == 3) {
                final RasterDataNode raster = parentForm.getProductSceneView().getRaster();
                final Stx stx = raster.getStx();
                Assert.notNull(stx, "stx");
                final int[] frequencies = stx.getHistogramBins();
                Assert.notNull(frequencies, "frequencies");
                if (raster instanceof Band) {
                    Band band = (Band) raster;
                    final IndexCoding indexCoding = band.getIndexCoding();
                    if (indexCoding != null && rowIndex < indexCoding.getSampleCount()) {
                        final int sampleValue = indexCoding.getSampleValue(rowIndex);
                        if (sampleValue < frequencies.length) {
                            final double frequency = frequencies[sampleValue];
                            return frequency / stx.getSampleCount();
                        }
                    }
                }
                return 0.0;
            } else if (columnIndex == 4) {
                final RasterDataNode raster = parentForm.getProductSceneView().getRaster();
                if (raster instanceof Band) {
                    Band band = (Band) raster;
                    final IndexCoding indexCoding = band.getIndexCoding();
                    if (indexCoding != null && rowIndex < indexCoding.getSampleCount()) {
                        final String text = indexCoding.getAttributeAt(rowIndex).getDescription();
                        return text != null ? text : "";
                    }
                }
                return "";
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (getImageInfo() == null) {
                return;
            }
            final ColorPaletteDef.Point point = getImageInfo().getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                point.setLabel((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            } else if (columnIndex == 1) {
                final Color color = (Color) aValue;
                point.setColor(color == null ? ImageInfo.NO_COLOR : color);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 1;
        }

    }
}
