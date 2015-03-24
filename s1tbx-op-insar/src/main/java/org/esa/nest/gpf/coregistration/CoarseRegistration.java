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
package org.esa.nest.gpf.coregistration;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.math.MathUtils;
import org.esa.snap.gpf.TileIndex;

import javax.media.jai.*;
import javax.media.jai.operator.DFTDescriptor;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.DataBufferDouble;
import java.awt.image.renderable.ParameterBlock;
import java.util.Hashtable;

/**
 * Image co-registration is fundamental for Interferometry SAR (InSAR) imaging and its applications, such as
 * DEM map generation and analysis. To obtain a high quality InSAR image, the individual complex images need
 * to be co-registered to sub-pixel accuracy. The co-registration is accomplished through an alignment of a
 * master image with a slave image.
 * <p/>
 * To achieve the alignment of master and slave images, the first step is to generate a set of uniformly
 * spaced ground control points (GCPs) in the master image, along with the corresponding GCPs in the slave
 * image. These GCP pairs are used in constructing a warp distortion function, which establishes a map
 * between pixels in the slave and master images.
 * <p/>
 * This operator computes the slave GCPS for given master GCPs. First the geometric information of the
 * master GCPs is used in determining the initial positions of the slave GCPs. Then a cross-correlation
 * is performed between imagettes surrounding each master GCP and its corresponding slave GCP to obtain
 * accurate slave GCP position. This step is repeated several times until the slave GCP position is
 * accurate enough.
 */

public class CoarseRegistration {

    private int maxIteration = 2;
    private double gcpTolerance = 0.5;
    private Tile mTile = null; // master tile
    private Tile sTile = null; // slave tile
    private ProductData mData = null; // master band data
    private ProductData sData = null; // slave band data
    private int cWindowWidth = 0; // row dimension for master and slave imagette for cross correlation, must be power of 2
    private int cWindowHeight = 0; // column dimension for master and slave imagette for cross correlation, must be power of 2
    private int rowUpSamplingFactor = 0; // cross correlation interpolation factor in row direction, must be power of 2
    private int colUpSamplingFactor = 0; // cross correlation interpolation factor in column direction, must be power of 2
    private int cHalfWindowWidth;
    private int cHalfWindowHeight;
    private int slaveImageWidth = 0;
    private int slaveImageHeight = 0;


    public CoarseRegistration(final int cWindowWidth, final int cWindowHeight, final int rowUpSamplingFactor,
                              final int colUpSamplingFactor, final int maxIteration, final double gcpTolerance,
                              final Tile mTile, final ProductData mData, final Tile sTile, final ProductData sData,
                              final int slaveImageWidth, final int slaveImageHeight) {

        if (!isPowerOfTwo(cWindowWidth) || !isPowerOfTwo(cWindowHeight)) {
            throw new OperatorException("Dimension of window for cross-xorrelation is not power of 2");
        }

        if (!isPowerOfTwo(rowUpSamplingFactor) || !isPowerOfTwo(colUpSamplingFactor)) {
            throw new OperatorException("Row or column up sampling factor is not power of 2");
        }

        this.cWindowWidth = cWindowWidth;
        this.cWindowHeight = cWindowHeight;

        cHalfWindowWidth = cWindowWidth / 2;
        cHalfWindowHeight = cWindowHeight / 2;

        this.rowUpSamplingFactor = rowUpSamplingFactor;
        this.colUpSamplingFactor = colUpSamplingFactor;

        this.maxIteration = maxIteration;
        this.gcpTolerance = gcpTolerance;

        final double achievableAccuracy = 1.0 / (double) Math.min(rowUpSamplingFactor, colUpSamplingFactor);
        if (gcpTolerance < achievableAccuracy) {
            throw new OperatorException("The achievable accuracy with current interpolation factors is " +
                    achievableAccuracy + ", GCP Tolerance is below it.");
        }

        this.mTile = mTile;
        this.sTile = sTile;
        this.mData = mData;
        this.sData = sData;

        this.slaveImageWidth = slaveImageWidth;
        this.slaveImageHeight = slaveImageHeight;
    }

    private boolean isPowerOfTwo(final int value) {
        return ((value & -value) == value);
    }

    /**
     * Check if a given slave GCP imagette is within the image.
     *
     * @param pixelPos The GCP pixel position.
     * @return flag Return true if the GCP is within the image, false otherwise.
     */
    private boolean checkSlaveGCPValidity(final PixelPos pixelPos) {

        return (pixelPos.x - cHalfWindowWidth + 1 >= 0 && pixelPos.x + cHalfWindowWidth <= slaveImageWidth - 1) &&
                (pixelPos.y - cHalfWindowHeight + 1 >= 0 && pixelPos.y + cHalfWindowHeight <= slaveImageHeight - 1);
    }

    public boolean getCoarseSlaveGCPPosition(final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos) {

        final double[] mI = getMasterImagette(mTile, mData, mGCPPixelPos);

        double rowShift = gcpTolerance + 1;
        double colShift = gcpTolerance + 1;
        int numIter = 0;

        while (Math.abs(rowShift) >= gcpTolerance || Math.abs(colShift) >= gcpTolerance) {

            if (numIter >= maxIteration) {
                return false;
            }

            if (!checkSlaveGCPValidity(sGCPPixelPos)) {
                return false;
            }

            final double[] sI = getSlaveImagette(sTile, sData, sGCPPixelPos);

            final double[] shift = {0, 0};
            if (!getSlaveGCPShift(shift, mI, sI)) {
                return false;
            }

            if (Math.abs(shift[0]) > 10 || Math.abs(shift[1]) > 10) {
                return false;
            }
            rowShift = shift[0];
            colShift = shift[1];
            sGCPPixelPos.x += (float) colShift;
            sGCPPixelPos.y += (float) rowShift;
            numIter++;
        }

        return true;
    }

    private double[] getMasterImagette(final Tile tile, final ProductData data, final PixelPos gcpPixelPos) {

        final double[] imagette = new double[cWindowWidth * cWindowHeight];
        final int xul = (int) gcpPixelPos.x - cHalfWindowWidth + 1;
        final int yul = (int) gcpPixelPos.y - cHalfWindowHeight + 1;
        final TileIndex index = new TileIndex(tile);

        int k = 0;
        for (int j = 0; j < cWindowHeight; j++) {
            index.calculateStride(yul + j);
            for (int i = 0; i < cWindowWidth; i++) {
                imagette[k++] = data.getElemDoubleAt(index.getIndex(xul + i));
            }
        }

        return imagette;
    }

    private double[] getSlaveImagette(final Tile tile, final ProductData data, final PixelPos gcpPixelPos)
            throws OperatorException {

        final double[] sI = new double[cWindowWidth * cWindowHeight];
        final double xx = gcpPixelPos.x;
        final double yy = gcpPixelPos.y;
        int k = 0;

        try {
            final TileIndex index = new TileIndex(tile);

            for (int j = 0; j < cWindowHeight; j++) {
                final double y = yy - cHalfWindowHeight + j + 1;
                final int y0 = (int) y;
                final int y1 = y0 + 1;
                final int offset0 = index.calculateStride(y0);
                final int offset1 = index.calculateStride(y1);
                final double wy = (double) (y - y0);
                for (int i = 0; i < cWindowWidth; i++) {
                    final double x = xx - cHalfWindowWidth + i + 1;
                    final int x0 = (int) x;
                    final int x1 = x0 + 1;
                    final double wx = x - x0;

                    final int x00 = x0 - offset0;
                    final int x01 = x0 - offset1;
                    final int x10 = x1 - offset0;
                    final int x11 = x1 - offset1;

                    sI[k] = MathUtils.interpolate2D(wy, wx, data.getElemDoubleAt(x00),
                            data.getElemDoubleAt(x01),
                            data.getElemDoubleAt(x10),
                            data.getElemDoubleAt(x11));
                    ++k;
                }
            }
            return sI;

        } catch (Exception e) {
            System.out.println("Error in getSlaveImagette");
            throw new OperatorException(e);
        }
    }

    private boolean getSlaveGCPShift(final double[] shift, final double[] mI, final double[] sI) {

        try {
            final PlanarImage crossCorrelatedImage = computeCrossCorrelatedImage(mI, sI);
            final int w = crossCorrelatedImage.getWidth();
            final int h = crossCorrelatedImage.getHeight();

            final Raster idftData = crossCorrelatedImage.getData();
            final double[] real = idftData.getSamples(0, 0, w, h, 0, (double[]) null);

            int peakRow = 0;
            int peakCol = 0;
            double peak = real[0];
            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    final int k = r * w + c;
                    if (real[k] > peak) {
                        peak = real[k];
                        peakRow = r;
                        peakCol = c;
                    }
                }
            }

            if (peakRow <= h / 2) {
                shift[0] = (double) (-peakRow) / (double) rowUpSamplingFactor;
            } else {
                shift[0] = (double) (h - peakRow) / (double) rowUpSamplingFactor;
            }

            if (peakCol <= w / 2) {
                shift[1] = (double) (-peakCol) / (double) colUpSamplingFactor;
            } else {
                shift[1] = (double) (w - peakCol) / (double) colUpSamplingFactor;
            }

            return true;
        } catch (Throwable t) {
            System.out.println("getSlaveGCPShift failed " + t.getMessage());
            return false;
        }
    }

    private PlanarImage computeCrossCorrelatedImage(final double[] mI, final double[] sI) {

        // get master imagette spectrum
        final RenderedImage masterImage = createRenderedImage(mI, cWindowWidth, cWindowHeight);
        final PlanarImage masterSpectrum = dft(masterImage);

        // get slave imagette spectrum
        final RenderedImage slaveImage = createRenderedImage(sI, cWindowWidth, cWindowHeight);
        final PlanarImage slaveSpectrum = dft(slaveImage);

        // get conjugate slave spectrum
        final PlanarImage conjugateSlaveSpectrum = conjugate(slaveSpectrum);

        // multiply master spectrum and conjugate slave spectrum
        final PlanarImage crossSpectrum = multiplyComplex(masterSpectrum, conjugateSlaveSpectrum);

        // upsampling cross spectrum
        final RenderedImage upsampledCrossSpectrum = upsampling(crossSpectrum);

        // perform IDF on the cross spectrum
        final PlanarImage correlatedImage = idft(upsampledCrossSpectrum);

        // compute the magnitode of the cross correlated image
        return magnitude(correlatedImage);
    }

    private static RenderedImage createRenderedImage(final double[] array, final int w, final int h) {

        // create rendered image with demension being width by height
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, w, h, 1);
        final ColorModel colourModel = PlanarImage.createColorModel(sampleModel);
        final DataBufferDouble dataBuffer = new DataBufferDouble(array, array.length);
        final WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));

        return new BufferedImage(colourModel, raster, false, new Hashtable());
    }

    private static PlanarImage dft(final RenderedImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(DFTDescriptor.SCALING_NONE);
        pb.add(DFTDescriptor.REAL_TO_COMPLEX);
        return JAI.create("dft", pb, null);
    }

    private static PlanarImage idft(final RenderedImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(DFTDescriptor.SCALING_DIMENSIONS);
        pb.add(DFTDescriptor.COMPLEX_TO_COMPLEX);
        return JAI.create("idft", pb, null);
    }

    private static PlanarImage conjugate(final PlanarImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        return JAI.create("conjugate", pb, null);
    }

    private static PlanarImage multiplyComplex(final PlanarImage image1, final PlanarImage image2) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image1);
        pb.addSource(image2);
        return JAI.create("MultiplyComplex", pb, null);
    }

    private RenderedImage upsampling(final PlanarImage image) {

        final int w = image.getWidth();  // w is power of 2
        final int h = image.getHeight(); // h is power of 2
        final int newWidth = rowUpSamplingFactor * w; // rowInterpFactor should be power of 2 to avoid zero padding in idft
        final int newHeight = colUpSamplingFactor * h; // colInterpFactor should be power of 2 to avoid zero padding in idft

        // create shifted image
        final ParameterBlock pb1 = new ParameterBlock();
        pb1.addSource(image);
        pb1.add(w / 2);
        pb1.add(h / 2);
        PlanarImage shiftedImage = JAI.create("PeriodicShift", pb1, null);

        // create zero padded image
        final ParameterBlock pb2 = new ParameterBlock();
        final int leftPad = (newWidth - w) / 2;
        final int rightPad = leftPad;
        final int topPad = (newHeight - h) / 2;
        final int bottomPad = topPad;
        pb2.addSource(shiftedImage);
        pb2.add(leftPad);
        pb2.add(rightPad);
        pb2.add(topPad);
        pb2.add(bottomPad);
        pb2.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
        final PlanarImage zeroPaddedImage = JAI.create("border", pb2);

        // reposition zero padded image so the image origin is back at (0,0)
        final ParameterBlock pb3 = new ParameterBlock();
        pb3.addSource(zeroPaddedImage);
        pb3.add(1.0f * leftPad);
        pb3.add(1.0f * topPad);
        final PlanarImage zeroBorderedImage = JAI.create("translate", pb3, null);

        // shift the zero padded image
        final ParameterBlock pb4 = new ParameterBlock();
        pb4.addSource(zeroBorderedImage);
        pb4.add(newWidth / 2);
        pb4.add(newHeight / 2);
        final PlanarImage shiftedZeroPaddedImage = JAI.create("PeriodicShift", pb4, null);

        return shiftedZeroPaddedImage;
    }

    private static PlanarImage magnitude(final PlanarImage image) {

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        return JAI.create("magnitude", pb, null);
    }
}
