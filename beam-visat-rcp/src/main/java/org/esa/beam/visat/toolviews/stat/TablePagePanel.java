/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.AbstractBand;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Thomas Storm
 */
class TablePagePanel extends PagePanel {

    private static final String DEFAULT_INFORMATION_TEXT = "No information available."; /*I18N*/
    private static final String TITLE_PREFIX = "Information";  /*I18N*/
    private static final String NO_PRODUCT_READER_MESSAGE = "No product reader set";

    private final InformationTableModel tableModel;
    private final TableModel emptyTableModel;
    private JTable table;
    private TableCellRenderer firstColumnRenderer;
    private TableCellRenderer secondColumnRenderer;

    TablePagePanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId, TITLE_PREFIX);
        tableModel = new InformationTableModel();
        emptyTableModel = new DefaultTableModel(1, 1);
        emptyTableModel.setValueAt(DEFAULT_INFORMATION_TEXT, 0, 0);
    }

    @Override
    protected void initComponents() {
        table = new JTable(emptyTableModel);
        table.setTableHeader(null);
        table.addMouseListener(new PopupHandler());
        table.setShowGrid(false);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        firstColumnRenderer = new AlternatingRowsRenderer();
        secondColumnRenderer = new TooltipAwareRenderer();
        add(new JScrollPane(table));
    }

    @Override
    protected String getDataAsText() {
        StringBuilder builder = new StringBuilder();
        final List<TableRow> rows = tableModel.rows;
        for (int i = 0; i < rows.size(); i++) {
            final TableRow row = rows.get(i);
            builder.append(row.label)
                    .append("\t")
                    .append(row.value)
                    .append("\t")
                    .append(StringUtils.isNotNullAndNotEmpty(row.unit) ? row.unit : "");
            if (i < rows.size() - 1) {
                    builder.append("\n");
            }
        }
        return builder.toString();
    }

    @Override
    protected void updateComponents() {
        ensureTableModel();
        tableModel.clear();
        if (getRaster() instanceof AbstractBand) {
            final Band band = (Band) getRaster();

            appendEntry("Name:", band.getName(), "");
            appendEntry("Type:", "Band", "");
            appendEntry("Description:", band.getDescription(), "");
            appendEntry("Geophysical unit:", band.getUnit(), "");
            appendEntry("Geophysical data type:", ProductData.getTypeString(band.getGeophysicalDataType()), "");
            appendEntry("Raw data type:", ProductData.getTypeString(band.getDataType()), "");
            appendEntry("Raster width:", String.valueOf(band.getRasterWidth()), "pixels");
            appendEntry("Raster height:", String.valueOf(band.getRasterHeight()), "pixels");
            appendEntry("Scaling factor:", String.valueOf(band.getScalingFactor()), "");
            appendEntry("Scaling offset:", String.valueOf(band.getScalingOffset()), "");
            appendEntry("Is log 10 scaled:", String.valueOf(band.isLog10Scaled()), "");
            appendEntry("Is no-data value used:", String.valueOf(band.isNoDataValueUsed()), "");
            appendEntry("No-data value:", String.valueOf(band.getNoDataValue()), "");
            appendEntry("Geophysical no-data value:", String.valueOf(band.getGeophysicalNoDataValue()), "");
            appendEntry("Valid pixel expression:", String.valueOf(band.getValidPixelExpression()), "");
            appendEntry("Spectral band index:", String.valueOf(band.getSpectralBandIndex() + 1), "");
            appendEntry("Wavelength:", String.valueOf(band.getSpectralWavelength()), "nm");
            appendEntry("Bandwidth:", String.valueOf(band.getSpectralBandwidth()), "nm");
            appendEntry("Solar flux:", String.valueOf(band.getSolarFlux()), "mW/(m^2*nm)");
        } else if (getRaster() instanceof TiePointGrid) {
            final TiePointGrid grid = (TiePointGrid) getRaster();
            appendEntry("Name:", grid.getName(), "");
            appendEntry("Type:", "Tie Point Grid", "");
            appendEntry("Description:", grid.getDescription(), "");
            appendEntry("Geophysical unit:", grid.getUnit(), "");
            appendEntry("Geophysical data type:", ProductData.getTypeString(grid.getGeophysicalDataType()), null);
            appendEntry("Offset X:", String.valueOf(grid.getOffsetX()), "pixels");
            appendEntry("Offset Y:", String.valueOf(grid.getOffsetY()), "pixels");
            appendEntry("Sub-sampling X:", String.valueOf(grid.getSubSamplingX()), "pixels");
            appendEntry("Sub-sampling Y:", String.valueOf(grid.getSubSamplingY()), "pixels");
            appendEntry("Raster width:", String.valueOf(grid.getRasterWidth()), "tie points");
            appendEntry("Raster height:", String.valueOf(grid.getRasterHeight()), "tie points");
        }

        final Product product = getProduct();

        if (product == null) {
            table.setModel(emptyTableModel);
            return;
        }

        appendEntry("Product name:", product.getName(), null);
        appendEntry("Product type:", product.getProductType(), null);
        appendEntry("Product description:", product.getDescription(), null);

        final String productFormatName = getProductFormatName(product);
        final String productFormatNameString = productFormatName != null ? productFormatName : "unknown";
        appendEntry("Product format:", productFormatNameString, null);

        appendEntry("Product reader:", getProductReaderName(product), null);
        appendEntry("Product reader class:", getProductReaderClass(product), null);
        appendEntry("Product reader module:", getProductReaderModule(product), null);

        appendEntry("Product file location:",
                    product.getFileLocation() != null ? product.getFileLocation().getPath() : "Not yet saved", null);
        appendEntry("Product scene width:", String.valueOf(product.getSceneRasterWidth()), "pixels");
        appendEntry("Product scene height:", String.valueOf(product.getSceneRasterHeight()), "pixels");

        final String startTimeString = product.getStartTime() != null ?
                                       product.getStartTime().getElemString() : "Not available";
        appendEntry("Product start time (UTC):", startTimeString, null);

        final String stopTimeString = product.getEndTime() != null ?
                                      product.getEndTime().getElemString() : "Not available";
        appendEntry("Product end time (UTC):", stopTimeString, null);
    }

    private void ensureTableModel() {
        if (table.getModel() != tableModel) {
            table.setModel(tableModel);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            table.getColumnModel().getColumn(0).setPreferredWidth(150);
            table.getColumnModel().getColumn(0).setMinWidth(150);
            table.getColumnModel().getColumn(0).setMaxWidth(150);
            table.getColumnModel().getColumn(0).setCellRenderer(firstColumnRenderer);
            table.getColumnModel().getColumn(1).setCellRenderer(secondColumnRenderer);
        }
    }

    private void appendEntry(final String label, final String value,
                             final String unit) {

        String formattedLabel = String.format("%1$-30s \t", label);
        TableRow row = new TableRow(formattedLabel, value, unit);
        tableModel.addRow(row);
    }

    private static String getProductReaderName(final Product product) {
        final ProductReader productReader = product.getProductReader();
        if (productReader != null) {
            final ProductReaderPlugIn readerPlugIn = productReader.getReaderPlugIn();
            if (readerPlugIn != null) {
                String description = readerPlugIn.getDescription(null);
                if (description != null) {
                    return description;
                }
            }
        }
        return NO_PRODUCT_READER_MESSAGE;
    }

    private static String getProductReaderClass(final Product product) {
        final ProductReader productReader = product.getProductReader();
        if (productReader != null) {
            final ProductReaderPlugIn readerPlugIn = productReader.getReaderPlugIn();
            if (readerPlugIn != null) {
                return readerPlugIn.getClass().getName();
            }
        }
        return NO_PRODUCT_READER_MESSAGE;
    }

    private static String getProductReaderModule(final Product product) {
        final ProductReader productReader = product.getProductReader();
        if (productReader != null) {
            Logger logger = BeamLogManager.getSystemLogger();
            ModuleReader moduleReader = new ModuleReader(logger);
            URL moduleLocation = productReader.getClass().getProtectionDomain().getCodeSource().getLocation();
            try {
                Module module = moduleReader.readFromLocation(moduleLocation);
                return module.getSymbolicName() + "  v " + module.getVersion().toString();
            } catch (Exception e) {
                logger.warning("Could not read " + moduleLocation.toString());
                return "unknown";
            }
        }
        return NO_PRODUCT_READER_MESSAGE;
    }

    private static String getProductFormatName(final Product product) {
        final ProductReader productReader = product.getProductReader();
        if (productReader == null) {
            return null;
        }
        final ProductReaderPlugIn readerPlugIn = productReader.getReaderPlugIn();
        if (readerPlugIn != null) {
            return getProductFormatName(readerPlugIn);
        }
        return null;
    }

    // todo - make this a method in ProductReader and ProductWriter
    private static String getProductFormatName(final ProductReaderPlugIn readerPlugIn) {
        final String[] formatNames = readerPlugIn.getFormatNames();
        if (formatNames != null && formatNames.length > 0) {
            return formatNames[0];
        }
        return null;
    }

    /**
     * Notified when a node changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        if (event.getSourceNode() == getRaster() || event.getSourceNode() == getProduct()) {
            updateComponents();
        }
    }

    private static class TableRow {

        String label;
        String value;
        String unit;

        public TableRow(String label, String value, String unit) {
            this.label = label;
            this.value = value;
            this.unit = unit;
        }
    }

    private static class InformationTableModel implements TableModel {

        List<TableRow> rows = new ArrayList<>();
        List<TableModelListener> listeners = new ArrayList<>();

        public void addRow(TableRow row) {
            rows.add(row);
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this, rows.size() - 1));
            }
        }

        public void clear() {
            rows.clear();
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this));
            }
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Name";
                case 1:
                    return "Value and Unit";
            }
            throw new IllegalStateException("Should never come here");
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TableRow tableRow = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return tableRow.label;
                case 1:
                    return tableRow.value +
                           (StringUtils.isNotNullAndNotEmpty(tableRow.unit) ? " " + tableRow.unit : "");
            }

            throw new IllegalStateException("Invalid index: row=" + rowIndex + "; column=" + columnIndex);
        }

        @Override
        public void setValueAt(Object invalid1, int invalid2, int invalid3) {
            throw new IllegalStateException("Table must be non-editable!");
        }

        @Override
        public void addTableModelListener(TableModelListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTableModelListener(TableModelListener listener) {
            listeners.remove(listener);
        }
    }

    private static class AlternatingRowsRenderer extends DefaultTableCellRenderer {

        private Color brightBackground;
        private Color mediumBackground;

        public AlternatingRowsRenderer() {
            brightBackground = Color.white;
            mediumBackground = new Color((14 * brightBackground.getRed()) / 15,
                                         (14 * brightBackground.getGreen()) / 15,
                                         (14 * brightBackground.getBlue()) / 15);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBackground(getBackground(row));
            return label;
        }

        private Color getBackground(int row) {
            if (row % 2 == 0) {
                return mediumBackground;
            } else {
                return brightBackground;
            }
        }

    }

    private static class TooltipAwareRenderer extends AlternatingRowsRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setToolTipText(label.getText());
            return label;
        }
    }

}
