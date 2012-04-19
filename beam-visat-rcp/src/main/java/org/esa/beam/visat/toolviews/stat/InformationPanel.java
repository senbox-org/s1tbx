/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.StringUtils;


/**
 * The information pane within the statistcs window.
 */
class InformationPanel extends TextPagePanel {

    private static final String DEFAULT_INFORMATION_TEXT = "No information available."; /*I18N*/
    private static final String TITLE_PREFIX = "Information";  /*I18N*/

    InformationPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, DEFAULT_INFORMATION_TEXT, helpID, TITLE_PREFIX);
    }

    @Override
    protected String createText() {
        final StringBuffer sb = new StringBuffer(1024);

        sb.append('\n');

        if (getRaster() instanceof AbstractBand) {
            final Band band = (Band) getRaster();

            appendEntry(sb, "Name:", band.getName(), "");
            appendEntry(sb, "Type:", "Band", "");
            appendEntry(sb, "Description:", band.getDescription(), "");
            appendEntry(sb, "Geophysical unit:", band.getUnit(), "");
            appendEntry(sb, "Geophysical data type:", ProductData.getTypeString(band.getGeophysicalDataType()), "");
            appendEntry(sb, "Raw data type:", ProductData.getTypeString(band.getDataType()), "");
            appendEntry(sb, "Raster width:", String.valueOf(band.getRasterWidth()), "pixels");
            appendEntry(sb, "Raster height:", String.valueOf(band.getRasterHeight()), "pixels");
            appendEntry(sb, "Scaling factor:", String.valueOf(band.getScalingFactor()), "");
            appendEntry(sb, "Scaling offset:", String.valueOf(band.getScalingOffset()), "");
            appendEntry(sb, "Is log 10 scaled:", String.valueOf(band.isLog10Scaled()), "");
            appendEntry(sb, "Is no-data value used:", String.valueOf(band.isNoDataValueUsed()), "");
            appendEntry(sb, "No-data value:", String.valueOf(band.getNoDataValue()), "");
            appendEntry(sb, "Geophysical no-data value:", String.valueOf(band.getGeophysicalNoDataValue()), "");
            appendEntry(sb, "Valid pixel expression:", String.valueOf(band.getValidPixelExpression()), "");
            appendEntry(sb, "Spectral band index:", String.valueOf(band.getSpectralBandIndex() + 1), "");
            appendEntry(sb, "Wavelength:", String.valueOf(band.getSpectralWavelength()), "nm");
            appendEntry(sb, "Bandwidth:", String.valueOf(band.getSpectralBandwidth()), "nm");
            appendEntry(sb, "Solar flux:", String.valueOf(band.getSolarFlux()), "mW/(m^2*nm)");

        } else if (getRaster() instanceof TiePointGrid) {
            final TiePointGrid grid = (TiePointGrid) getRaster();

            appendEntry(sb, "Name:", grid.getName(), "");
            appendEntry(sb, "Type:", "Tie Point Grid", "");
            appendEntry(sb, "Description:", grid.getDescription(), "");
            appendEntry(sb, "Geophysical unit:", grid.getUnit(), "");
            appendEntry(sb, "Geophysical data type:", ProductData.getTypeString(grid.getGeophysicalDataType()), "");
            appendEntry(sb, "Offset X:", String.valueOf(grid.getOffsetX()), "pixels");
            appendEntry(sb, "Offset Y:", String.valueOf(grid.getOffsetY()), "pixels");
            appendEntry(sb, "Sub-sampling X:", String.valueOf(grid.getSubSamplingX()), "pixels");
            appendEntry(sb, "Sub-sampling Y:", String.valueOf(grid.getSubSamplingY()), "pixels");
            appendEntry(sb, "Raster width:", String.valueOf(grid.getRasterWidth()), "tie points");
            appendEntry(sb, "Raster height:", String.valueOf(grid.getRasterHeight()), "tie points");

        }

        final Product product = getProduct();

        if (product == null) {
            return DEFAULT_INFORMATION_TEXT;
        }
        sb.append('\n');

        appendEntry(sb, "Product name:", product.getName(), null);
        appendEntry(sb, "Product type:", product.getProductType(), null);
        appendEntry(sb, "Product description:", product.getDescription(), null);

        final String productFormatName = getProductFormatName(product);
        final String productFormatNameString = productFormatName != null ? productFormatName : "unknown";
        appendEntry(sb, "Product format:", productFormatNameString, null);

        final String productReaderName = getProductReaderName(product);
        final String productReaderNameString = productReaderName != null ? productReaderName : "unknown";
        appendEntry(sb, "Product reader:", productReaderNameString, null);

        appendEntry(sb, "Product file location:",
                    product.getFileLocation() != null ? product.getFileLocation().getPath() : "Not yet saved", "");
        appendEntry(sb, "Product scene width:", String.valueOf(product.getSceneRasterWidth()), "pixels");
        appendEntry(sb, "Product scene height:", String.valueOf(product.getSceneRasterHeight()), "pixels");

        final String startTimeString = product.getStartTime() != null ?
                product.getStartTime().getElemString() : "Not available";
        appendEntry(sb, "Product start time (UTC):", startTimeString, null);

        final String stopTimeString = product.getEndTime() != null ?
                product.getEndTime().getElemString() : "Not available";
        appendEntry(sb, "Product end time (UTC):", stopTimeString, null);

        return sb.toString();
    }

    private void appendEntry(final StringBuffer sb, final String label, final String value,
                             final String unit) {
        sb.append(String.format("%1$-30s \t", label));
        sb.append(value);
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            sb.append("\t ").append(unit);
        }
        sb.append('\n');
    }

    private static String getProductReaderName(final Product product) {
        final ProductReader productReader = product.getProductReader();
        if (productReader == null) {
            return null;
        }
        final ProductReaderPlugIn readerPlugIn = productReader.getReaderPlugIn();
        if (readerPlugIn != null) {
            return readerPlugIn.getDescription(null);
        }
        return null;
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
}

