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
import com.jidesoft.grid.CellSpan;
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
    private int minWidthOfNameColumn = -1;

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
        tableModel.clear();
        if (getRaster() instanceof AbstractBand) {
            final Band band = (Band) getRaster();

            addEntry("Name:", band.getName(), "");
            addEntry("Type:", "Band", "");
            addEntry("Description:", band.getDescription(), "");
            addEntry("Geophysical unit:", band.getUnit(), "");
            addEntry("Geophysical data type:", ProductData.getTypeString(band.getGeophysicalDataType()), "");
            addEntry("Raw data type:", ProductData.getTypeString(band.getDataType()), "");
            addEntry("Raster width:", String.valueOf(band.getRasterWidth()), "pixels");
            addEntry("Raster height:", String.valueOf(band.getRasterHeight()), "pixels");
            addEntry("Scaling factor:", String.valueOf(band.getScalingFactor()), "");
            addEntry("Scaling offset:", String.valueOf(band.getScalingOffset()), "");
            addEntry("Is log 10 scaled:", String.valueOf(band.isLog10Scaled()), "");
            addEntry("Is no-data value used:", String.valueOf(band.isNoDataValueUsed()), "");
            addEntry("No-data value:", String.valueOf(band.getNoDataValue()), "");
            addEntry("Geophysical no-data value:", String.valueOf(band.getGeophysicalNoDataValue()), "");
            addEntry("Valid pixel expression:", String.valueOf(band.getValidPixelExpression()), "");
            addEntry("Spectral band index:", String.valueOf(band.getSpectralBandIndex() + 1), "");
            addEntry("Wavelength:", String.valueOf(band.getSpectralWavelength()), "nm");
            addEntry("Bandwidth:", String.valueOf(band.getSpectralBandwidth()), "nm");
            addEntry("Solar flux:", String.valueOf(band.getSolarFlux()), "mW/(m^2*nm)");
        } else if (getRaster() instanceof TiePointGrid) {
            final TiePointGrid grid = (TiePointGrid) getRaster();
            addEntry("Name:", grid.getName(), "");
            addEntry("Type:", "Tie Point Grid", "");
            addEntry("Description:", grid.getDescription(), "");
            addEntry("Geophysical unit:", grid.getUnit(), "");
            addEntry("Geophysical data type:", ProductData.getTypeString(grid.getGeophysicalDataType()), null);
            addEntry("Offset X:", String.valueOf(grid.getOffsetX()), "pixels");
            addEntry("Offset Y:", String.valueOf(grid.getOffsetY()), "pixels");
            addEntry("Sub-sampling X:", String.valueOf(grid.getSubSamplingX()), "pixels");
            addEntry("Sub-sampling Y:", String.valueOf(grid.getSubSamplingY()), "pixels");
            addEntry("Raster width:", String.valueOf(grid.getRasterWidth()), "tie points");
            addEntry("Raster height:", String.valueOf(grid.getRasterHeight()), "tie points");
        }

        final Product product = getProduct();

        if (product == null) {
            showNoInformationAvailableMessage();
            return;
        }

        addEmptyRow();

        addEntry("Product name:", product.getName(), null);
        addEntry("Product type:", product.getProductType(), null);
        addEntry("Product description:", product.getDescription(), null);

        final String productFormatName = getProductFormatName(product);
        final String productFormatNameString = productFormatName != null ? productFormatName : "unknown";
        addEntry("Product format:", productFormatNameString, null);

        addEntry("Product reader:", getProductReaderName(product), null);
        addEntry("Product reader class:", getProductReaderClass(product), null);
        addEntry("Product reader module:", getProductReaderModule(product), null);

        addEntry("Product file location:",
                 product.getFileLocation() != null ? product.getFileLocation().getPath() : "Not yet saved", null);
        addEntry("Product scene width:", String.valueOf(product.getSceneRasterWidth()), "pixels");
        addEntry("Product scene height:", String.valueOf(product.getSceneRasterHeight()), "pixels");

        final String startTimeString = product.getStartTime() != null ?
                                       product.getStartTime().getElemString() : "Not available";
        addEntry("Product start time (UTC):", startTimeString, null);

        final String stopTimeString = product.getEndTime() != null ?
                                      product.getEndTime().getElemString() : "Not available";
        addEntry("Product end time (UTC):", stopTimeString, null);

        ensureTableModel();
    }

    private void ensureTableModel() {
        if (getTable().getModel() != tableModel) {
            getTable().setModel(tableModel);
            getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            getTable().getColumnModel().getColumn(0).setPreferredWidth(minWidthOfNameColumn);
            getTable().getColumnModel().getColumn(0).setMinWidth(minWidthOfNameColumn);
            getTable().getColumnModel().getColumn(0).setMaxWidth(minWidthOfNameColumn);
            setColumnRenderer(0, RendererFactory.createRenderer(RendererFactory.ALTERNATING_ROWS));
            setColumnRenderer(1, RendererFactory.createRenderer(RendererFactory.ALTERNATING_ROWS
                                                                | RendererFactory.TOOLTIP_AWARE));
        }
    }

    private void addEntry(final String label, final String value, final String unit) {
        String formattedLabel = String.format("%1$-30s \t", label);
        minWidthOfNameColumn =
                getFontMetrics(getFont()).stringWidth(formattedLabel) + 10; // needs a bit of cushion, obviously...
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
        public CellSpan getCellspan(int rowIndex, int columnIndex, TableModel model) {
            return new CellSpan(rowIndex, columnIndex, 1, 1);
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
            TableRow row = rows.get(rowIndex);
            if (!(row instanceof InformationTableRow)) {
                return row.toString();
            }
            InformationTableRow tableRow = (InformationTableRow) row;
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
