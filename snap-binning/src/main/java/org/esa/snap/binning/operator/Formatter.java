/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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


package org.esa.snap.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.esa.snap.binning.MosaickingGrid;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.ProductCustomizer;
import org.esa.snap.binning.Reprojector;
import org.esa.snap.binning.TemporalBinRenderer;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.binning.support.CrsGrid;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.Rectangle;
import java.io.File;

/**
 * Utility class used to format the results of a binning given by a {@link TemporalBinSource}.
 *
 * @author Norman Fomferra
 */
public class Formatter {

    public static void format(PlanetaryGrid planetaryGrid,
                              TemporalBinSource temporalBinSource,
                              String[] featureNames,
                              FormatterConfig formatterConfig,
                              Geometry roiGeometry,
                              ProductData.UTC startTime,
                              ProductData.UTC stopTime,
                              MetadataElement... metadataElements) throws Exception {

        if (featureNames.length == 0) {
            throw new IllegalArgumentException("Illegal binning context: featureNames.length == 0");
        }

        final File outputFile = new File(formatterConfig.getOutputFile());
        final String outputType = formatterConfig.getOutputType();
        final String outputFormat = getOutputFormat(formatterConfig, outputFile);

        final Rectangle outputRegion;
        if (planetaryGrid instanceof CrsGrid) {
            if (roiGeometry == null) {
                outputRegion = new Rectangle(planetaryGrid.getNumCols(0), planetaryGrid.getNumRows());
            } else {
                CrsGrid crsGrid = (CrsGrid) planetaryGrid;
                if (roiGeometry instanceof LinearRing) {
                    LinearRing linearRing = (LinearRing) roiGeometry;
                    roiGeometry = new GeometryFactory().createPolygon(linearRing);
                }
                System.out.println("roiGeometry = " + roiGeometry);
                Geometry imageGeometry = crsGrid.getImageGeometry(roiGeometry);
                System.out.println("imageGeometry = " + imageGeometry);
                if (imageGeometry == null) {
                    outputRegion = new Rectangle(planetaryGrid.getNumCols(0), planetaryGrid.getNumRows());
                } else {
                    outputRegion = crsGrid.getBounds(imageGeometry);
                }
            }
        } else {
            outputRegion = Reprojector.computeRasterSubRegion(planetaryGrid, roiGeometry);
        }
        System.out.println("outputRegion = " + outputRegion);

        ProductCustomizer productCustomizer = formatterConfig.getProductCustomizer();

        final TemporalBinRenderer temporalBinRenderer;
        if (outputType.equalsIgnoreCase("Product")) {
            GeoCoding geoCoding;
            if (planetaryGrid instanceof MosaickingGrid) {
                MosaickingGrid mosaickingGrid = (MosaickingGrid) planetaryGrid;
                geoCoding = mosaickingGrid.getGeoCoding(outputRegion);
            } else {
                double pixelSize = Reprojector.getRasterPixelSize(planetaryGrid);
                geoCoding = ProductTemporalBinRenderer.createMapGeoCoding(outputRegion, pixelSize);
            }
            temporalBinRenderer = new ProductTemporalBinRenderer(featureNames,
                                                                 outputFile,
                                                                 outputFormat,
                                                                 outputRegion,
                                                                 geoCoding,
                                                                 startTime,
                                                                 stopTime,
                                                                 productCustomizer,
                                                                 metadataElements);
        } else {
            temporalBinRenderer = new ImageTemporalBinRenderer(featureNames,
                                                               outputFile,
                                                               outputFormat,
                                                               outputRegion,
                                                               formatterConfig.getBandConfigurations(),
                                                               outputType.equalsIgnoreCase("RGB"));
        }

        Reprojector.reproject(planetaryGrid, temporalBinSource, temporalBinRenderer);
    }

    static String getOutputFormat(FormatterConfig formatterConfig, File outputFile) {
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
                && !outputFormat.startsWith("GeoTIFF")
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
