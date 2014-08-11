/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.MathUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;

/**
 * Common functions for readers
 */
public final class ReaderUtils {

    public static void createVirtualPhaseBand(final Product product, final Band bandI, final Band bandQ, final String countStr) {
        final String expression = "atan2(" + bandQ.getName() + ',' + bandI.getName() + ')';

        final VirtualBand virtBand = new VirtualBand("Phase" + countStr,
                ProductData.TYPE_FLOAT32,
                bandI.getSceneRasterWidth(),
                bandI.getSceneRasterHeight(),
                expression);
        virtBand.setUnit(Unit.PHASE);
        virtBand.setDescription("Phase from complex data");
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
    }

    public static void createVirtualIntensityBand(final Product product, final Band bandI, final Band bandQ, final String countStr) {
        final String expression = bandI.getName() + " * " + bandI.getName() + " + " +
                bandQ.getName() + " * " + bandQ.getName();

        final VirtualBand virtBand = new VirtualBand("Intensity" + countStr,
                ProductData.TYPE_FLOAT32,
                bandI.getSceneRasterWidth(),
                bandI.getSceneRasterHeight(),
                expression);
        virtBand.setUnit(Unit.INTENSITY);
        virtBand.setDescription("Intensity from complex data");
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);

        if (bandI.getGeoCoding() != product.getGeoCoding()) {
            virtBand.setGeoCoding(bandI.getGeoCoding());
        }
        // set as band to use for quicklook
        product.setQuicklookBandName(virtBand.getName());
    }

    /**
     * Returns a <code>File</code> if the given input is a <code>String</code> or <code>File</code>,
     * otherwise it returns null;
     *
     * @param input an input object of unknown type
     * @return a <code>File</code> or <code>null</code> it the input can not be resolved to a <code>File</code>.
     */
    public static File getFileFromInput(final Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    public static void addGeoCoding(final Product product, final float[] latCorners, final float[] lonCorners) {

        if (latCorners == null || lonCorners == null) return;

        final int gridWidth = 10;
        final int gridHeight = 10;

        final float[] fineLatTiePoints = new float[gridWidth * gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, latCorners, fineLatTiePoints);

        float subSamplingX = (float) product.getSceneRasterWidth() / (gridWidth - 1);
        float subSamplingY = (float) product.getSceneRasterHeight() / (gridHeight - 1);
        if (subSamplingX == 0 || subSamplingY == 0)
            return;

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, fineLatTiePoints);
        latGrid.setUnit(Unit.DEGREES);

        final float[] fineLonTiePoints = new float[gridWidth * gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, lonCorners, fineLonTiePoints);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, fineLonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(tpGeoCoding);
    }

    public static void createFineTiePointGrid(final int coarseGridWidth,
                                              final int coarseGridHeight,
                                              final int fineGridWidth,
                                              final int fineGridHeight,
                                              final float[] coarseTiePoints,
                                              final float[] fineTiePoints) {

        if (coarseTiePoints == null || coarseTiePoints.length != coarseGridWidth * coarseGridHeight) {
            throw new IllegalArgumentException(
                    "coarse tie point array size does not match 'coarseGridWidth' x 'coarseGridHeight'");
        }

        if (fineTiePoints == null || fineTiePoints.length != fineGridWidth * fineGridHeight) {
            throw new IllegalArgumentException(
                    "fine tie point array size does not match 'fineGridWidth' x 'fineGridHeight'");
        }

        int k = 0;
        for (int r = 0; r < fineGridHeight; r++) {

            final float lambdaR = (float) (r) / (float) (fineGridHeight - 1);
            final float betaR = lambdaR * (coarseGridHeight - 1);
            final int j0 = (int) (betaR);
            final int j1 = Math.min(j0 + 1, coarseGridHeight - 1);
            final float wj = betaR - j0;

            for (int c = 0; c < fineGridWidth; c++) {

                final float lambdaC = (float) (c) / (float) (fineGridWidth - 1);
                final float betaC = lambdaC * (coarseGridWidth - 1);
                final int i0 = (int) (betaC);
                final int i1 = Math.min(i0 + 1, coarseGridWidth - 1);
                final float wi = betaC - i0;

                fineTiePoints[k++] = MathUtils.interpolate2D(wi, wj,
                        coarseTiePoints[i0 + j0 * coarseGridWidth],
                        coarseTiePoints[i1 + j0 * coarseGridWidth],
                        coarseTiePoints[i0 + j1 * coarseGridWidth],
                        coarseTiePoints[i1 + j1 * coarseGridWidth]);
            }
        }
    }

    public static double getLineTimeInterval(final ProductData.UTC startUTC, final ProductData.UTC endUTC, final int sceneHeight) {
        if (startUTC == null || endUTC == null)
            return 0;
        final double startTime = startUTC.getMJD() * 24.0 * 3600.0;
        final double stopTime = endUTC.getMJD() * 24.0 * 3600.0;
        return (stopTime - startTime) / (double) (sceneHeight - 1);
    }

    public static void addMetadataProductSize(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));
        }
    }

    public static int getTotalSize(final Product product) {
        return (int) (product.getRawStorageSize() / (1024.0f * 1024.0f));
    }

    public static void addMetadataIncidenceAngles(final Product product) {
        final TiePointGrid tpg = product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE);
        if (tpg == null)
            return;

        final int midAz = product.getSceneRasterHeight() / 2;
        final float inc1 = tpg.getPixelFloat(0, midAz);
        final float inc2 = tpg.getPixelFloat(product.getSceneRasterWidth(), midAz);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, Math.min(inc1, inc2));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, Math.max(inc1, inc2));
    }

    public static ProductData.UTC getTime(final MetadataElement elem, final String tag, final DateFormat timeFormat) {
        if (elem == null)
            return AbstractMetadata.NO_METADATA_UTC;
        final String timeStr = createValidUTCString(elem.getAttributeString(tag, " ").toUpperCase(),
                new char[]{':', '.', '-'}, ' ').trim();
        return AbstractMetadata.parseUTC(timeStr, timeFormat);
    }

    private static String createValidUTCString(final String name, final char[] validChars, final char replaceChar) {
        Guardian.assertNotNull("name", name);
        char[] sortedValidChars = null;
        if (validChars == null) {
            sortedValidChars = new char[5];
        } else {
            sortedValidChars = (char[]) validChars.clone();
        }
        Arrays.sort(sortedValidChars);
        final StringBuilder validName = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char ch = name.charAt(i);
            if (Character.isDigit(ch)) {
                validName.append(ch);
            } else if (Arrays.binarySearch(sortedValidChars, ch) >= 0) {
                validName.append(ch);
            } else {
                validName.append(replaceChar);
            }
        }
        return validName.toString();
    }

    public static String findExtensionForFormat(final String formatName) {
        final ProductWriter writer = ProductIO.getProductWriter(formatName);
        return writer.getWriterPlugIn().getDefaultFileExtensions()[0];
    }
}
