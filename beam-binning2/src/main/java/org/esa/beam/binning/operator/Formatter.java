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


package org.esa.beam.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.binning.*;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.*;
import java.io.File;

/**
 * Utility class used to format the results of a binning given by a {@link TemporalBinSource}.
 *
 * @author Norman Fomferra
 */
public class Formatter {

    public static void format(BinningContext binningContext,
                              TemporalBinSource temporalBinSource,
                              FormatterConfig formatterConfig,
                              Geometry roiGeometry,
                              ProductData.UTC startTime,
                              ProductData.UTC stopTime,
                              MetadataElement ... metadataElements) throws Exception {

        if (binningContext.getBinManager().getAggregatorCount() == 0) {
            throw new IllegalArgumentException("Illegal binning context: aggregatorCount == 0");
        }

        final File outputFile = new File(formatterConfig.getOutputFile());
        final String outputType = formatterConfig.getOutputType();
        final String outputFormat = getOutputFormat(formatterConfig, outputFile);

        final PlanetaryGrid planetaryGrid = binningContext.getPlanetaryGrid();
        final Rectangle outputRegion = Reprojector.computeRasterSubRegion(planetaryGrid, roiGeometry);

        final TemporalBinRenderer temporalBinRenderer;
        if (outputType.equalsIgnoreCase("Product")) {
            temporalBinRenderer = new ProductTemporalBinRenderer(binningContext,
                                                                 outputFile,
                                                                 outputFormat,
                                                                 outputRegion,
                                                                 Reprojector.getRasterPixelSize(planetaryGrid),
                                                                 startTime,
                                                                 stopTime,
                                                                 metadataElements);
        } else {
            temporalBinRenderer = new ImageTemporalBinRenderer(binningContext,
                                                               outputFile,
                                                               outputFormat,
                                                               outputRegion,
                                                               formatterConfig.getBandConfigurations(),
                                                               outputType.equalsIgnoreCase("RGB"));
        }

        Reprojector.reproject(binningContext, temporalBinSource, temporalBinRenderer);
    }

    private static String getOutputFormat(FormatterConfig formatterConfig, File outputFile) {
        final String fileName = outputFile.getName();
        final int extPos = fileName.lastIndexOf(".");
        String outputFileNameExt = fileName.substring(extPos + 1);
        String outputFormat = formatterConfig.getOutputFormat();
        if (outputFormat == null) {
            outputFormat = outputFileNameExt.equalsIgnoreCase("nc") ? "NetCDF"
                    : outputFileNameExt.equalsIgnoreCase("dim") ? "BEAM-DIMAP"
                    : outputFileNameExt.equalsIgnoreCase("tiff") ? "GeoTIFF"
                    : outputFileNameExt.equalsIgnoreCase("png") ? "PNG"
                    : outputFileNameExt.equalsIgnoreCase("jpg") ? "JPEG" : null;
        }
        if (outputFormat == null) {
            throw new IllegalArgumentException("No output format given");
        }
        if (!outputFormat.startsWith("NetCDF")
                && !outputFormat.equalsIgnoreCase("BEAM-DIMAP")
                && !outputFormat.equalsIgnoreCase("GeoTIFF")
                && !outputFormat.equalsIgnoreCase("PNG")
                && !outputFormat.equalsIgnoreCase("JPEG")) {
            throw new IllegalArgumentException("Unknown output format: " + outputFormat);
        }
        if (outputFormat.equalsIgnoreCase("NetCDF")) {
            outputFormat = "NetCDF-BEAM"; // use NetCDF with beam extensions
        }
        return outputFormat;
    }


}
