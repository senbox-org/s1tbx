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
package org.esa.beam.visat.actions.pin;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.Guardian;

import java.awt.Point;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * The TabSeperatedPixelsWriter can be used to write pixel values to an output data stream.
 *
 * @author Maximilian Aulinger
 * @version Revision 1.3.1
 */
class TabSeparatedPinPixelsWriter {

    private Band[] bands;
    private GeoCoding geoCoding;
    private PrintWriter printWriter;

    /**
     * Initialises a new instance of this class.
     */
    TabSeparatedPinPixelsWriter(final Writer writer, final Band[] bands, final GeoCoding geoCoding) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertNotNull("rasters", bands);
        Guardian.assertNotNull("geoCoding", geoCoding);
        printWriter = new PrintWriter(writer);
        this.bands = bands;
        this.geoCoding = geoCoding;
    }

    /**
     * Writes the pixel values an annotation data into the output stream.
     */
    boolean writePlacemarkPixels(final int size, final String expression, final Map<Placemark, Object[]> pixelMap, ProgressMonitor pm) throws
                                                                                                                IOException {
        boolean success = true;
        Point[] pixels;
        Boolean[] pixelRelevanceInformation = null;
        writeAnnotationData(size, expression);
        Iterator<Placemark> iterator = pixelMap.keySet().iterator();

        pm.beginTask("Writing pixel data...", pixelMap.size());
        try {
            while (iterator.hasNext()) {
                pixels = null;
                if (pm.isCanceled()) {
                    success = false;
                    break;
                }
                Placemark keyPlacemark = iterator.next();
                Object pixelDataObject = pixelMap.get(keyPlacemark);
                if (pixelDataObject != null && pixelDataObject instanceof Point[]) {
                    pixels = (Point[]) pixelDataObject;
                } else if (pixelDataObject != null) {
                    Object[] pixelData = (Object[]) pixelDataObject;
                    pixels = (Point[]) pixelData[0];
                    pixelRelevanceInformation = (Boolean[]) (pixelData[1]);
                }
                writeRegionName(keyPlacemark.getLabel());

                if (pixels != null) {
                    writeHeaderLine(printWriter, geoCoding, bands, (pixelRelevanceInformation != null));
                    ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
                    subPm.beginTask("Writing pixel data...", pixels.length);
                    try {
                        for (int i = 0; i < pixels.length; i++) {
                            if (pixelRelevanceInformation != null) {
                                writeDataLine(printWriter, geoCoding, bands, pixels[i],
                                              pixelRelevanceInformation[i]);
                            } else {
                                writeDataLine(printWriter, geoCoding, bands, pixels[i], null);
                            }
                            subPm.worked(1);
                        }
                    } finally {
                        subPm.done();
                    }
                } else {
                    printWriter.print("There are no valid pixels values for this pin.");
                    printWriter.println();
                }
            }
        } finally {
            printWriter.close();
            pm.done();
        }
        return success;
    }

    /**
     * Writes the pin pixels header line into the output stream.
     */
    private void writeAnnotationData(final int size, final String expression) {

        printWriter.print("Exported region size in pixels:\t" + size + " x " + size);
        printWriter.print("\t");
        if (expression != null) {
            printWriter.print("Used expression:\t" + expression);
        } else {
            printWriter.print("No expression used");
        }

        printWriter.println();

    }

    /**
     * Writes the region name into the output stream.
     */
    private void writeRegionName(final String name) {
        printWriter.println();
        printWriter.println(name);
    }


    /**
     * Writes the header line of the dataset to be exported.
     *
     * @param out                     the data output writer
     * @param geoCoding               the product's geo-coding
     * @param bands                   the array of bands to be considered
     * @param addPixelRelevanceColumn a column header "Relevance" is added if set.
     */
    private static void writeHeaderLine(final PrintWriter out,
                                        final GeoCoding geoCoding,
                                        final Band[] bands, boolean addPixelRelevanceColumn) {

        if (addPixelRelevanceColumn) {
            out.print("Relevance");
            out.print("\t");
        }

        out.print("Pixel-X");
        out.print("\t");
        out.print("Pixel-Y");
        out.print("\t");
        if (geoCoding != null) {
            out.print("Longitude");
            out.print("\t");
            out.print("Latitude");
            out.print("\t");
        }
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            out.print(band.getName());
            if (i < bands.length - 1) {
                out.print("\t");
            }
        }
        out.print("\n");
    }

    /**
     * Writes a data line of the dataset to be exported for the given pixel position.
     *
     * @param out             the data output writer
     * @param geoCoding       the product's geo-coding
     * @param bands           the array of bands that provide pixel values
     * @param pixel           the pixel coordinates
     * @param isRelevantPixel if <code>false</code> the pixel is filtered or marked as irrelevant
     */
    private static void writeDataLine(final PrintWriter out,
                                      final GeoCoding geoCoding,
                                      final Band[] bands,
                                      Point pixel, Boolean isRelevantPixel) throws IOException {
        final PixelPos pixelPos = new PixelPos(pixel.x + 0.5f, pixel.y + 0.5f);
        final int[] intPixel = new int[1];
        final float[] floatPixel = new float[1];

        if (isRelevantPixel != null) {

            if (isRelevantPixel.booleanValue()) {
                out.print("1");
                out.print("\t");
            } else {
                out.print("0");
                out.print("\t");
            }
        }

        out.print(String.valueOf(pixelPos.x));
        out.print("\t");
        out.print(String.valueOf(pixelPos.y));
        out.print("\t");
        if (geoCoding != null) {
            final GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
            out.print(String.valueOf(geoPos.lon));
            out.print("\t");
            out.print(String.valueOf(geoPos.lat));
            out.print("\t");
        }
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            if (band.isPixelValid(pixel.x, pixel.y)) {
                if (band.isFloatingPointType()) {
                    band.readPixels(pixel.x, pixel.y, 1, 1, floatPixel, ProgressMonitor.NULL);
                    out.print(floatPixel[0]);
                } else {
                    band.readPixels(pixel.x, pixel.y, 1, 1, intPixel, ProgressMonitor.NULL);
                    out.print(intPixel[0]);
                }
            } else {
                out.print("NaN");
            }
            if (i < bands.length - 1) {
                out.print("\t");
            }
        }
        out.print("\n");
    }


}
