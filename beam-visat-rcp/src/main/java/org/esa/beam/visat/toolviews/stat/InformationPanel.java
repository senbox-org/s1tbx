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
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Thomas Storm
 */
class InformationPanel extends TablePagePanel {

    private static final String DEFAULT_INFORMATION_TEXT = "No information available.";
    private static final String TITLE_PREFIX = "Information";
    private static final String NO_PRODUCT_READER_MESSAGE = "No product reader set";

    private InformationTableModel tableModel;

    InformationPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId, TITLE_PREFIX, DEFAULT_INFORMATION_TEXT);
    }

    @Override
    protected void initComponents() {
        tableModel = new InformationTableModel();
        getTable().setTableHeader(null);
        getTable().addMouseListener(new PopupHandler());
        getTable().setShowGrid(false);
        getTable().setRowSelectionAllowed(false);
        getTable().setColumnSelectionAllowed(false);
        add(new JScrollPane(getTable()));
    }

    @Override
    protected String getDataAsText() {
        StringBuilder builder = new StringBuilder();
        final List<TableRow> rows = tableModel.rows;
        for (int i = 0; i < rows.size(); i++) {
            InformationTableRow row = (InformationTableRow) rows.get(i);
            builder.append(row.label)
                    .append("\t")
                    .append(row.value)
                    .append(StringUtils.isNotNullAndNotEmpty(row.unit) ? "\t" + row.unit : "");
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
            showNoInformationAvailableMessage();
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
        if (getTable().getModel() != tableModel) {
            getTable().setModel(tableModel);
            getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            getTable().getColumnModel().getColumn(0).setPreferredWidth(150);
            getTable().getColumnModel().getColumn(0).setMinWidth(150);
            getTable().getColumnModel().getColumn(0).setMaxWidth(150);
            setCellRenderer(0, new AlternatingRowsRenderer());
            setCellRenderer(1, new TooltipAwareRenderer());
        }
    }

    private void appendEntry(final String label, final String value,
                             final String unit) {

        String formattedLabel = String.format("%1$-30s \t", label);
        TableRow row = new InformationTableRow(formattedLabel, value, unit);
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

    static class InformationTableRow implements TablePagePanel.TableRow {

        String label;
        String value;
        String unit;

        public InformationTableRow(String label, String value, String unit) {
            this.label = label;
            this.value = value;
            this.unit = unit;
        }

        @Override
        public int getColspan(int columnIndex, TableModel model) {
            return 1;
        }
    }

    private static class InformationTableModel extends TablePagePanelModel {

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
        public Object getValueAt(int rowIndex, int columnIndex) {
            InformationTableRow tableRow = (InformationTableRow) rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return tableRow.label;
                case 1:
                    return tableRow.value +
                           (StringUtils.isNotNullAndNotEmpty(tableRow.unit) ? " " + tableRow.unit : "");
            }

            throw new IllegalStateException("Invalid index: row=" + rowIndex + "; column=" + columnIndex);
        }
    }
}
