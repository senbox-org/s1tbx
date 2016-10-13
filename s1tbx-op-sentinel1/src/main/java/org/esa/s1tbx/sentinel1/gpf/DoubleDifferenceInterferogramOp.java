/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.insar.gpf.support.Sentinel1Utils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * This operator computes the double difference interferogram for co-registered S-1 TOPS product for overlapped
 * area between adjacent bursts.
 */

@OperatorMetadata(alias = "Double-Difference-Interferogram",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Compute double difference interferogram")
public class DoubleDifferenceInterferogramOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(description = "Output coherence for overlapped area", defaultValue = "false",
            label = "Output coherence")
    private boolean outputCoherence = false;

    @Parameter(valueSet = {"3", "5", "9", "11"}, defaultValue = "5", label = "Coherence Window Size")
    private String cohWinSize = "5";

    private Sentinel1Utils su;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;
    private int cohWin = 0;
    private int numOverlaps = 0;
    private Band mstBandI = null;
    private Band mstBandQ = null;
    private Band slvBandI = null;
    private Band slvBandQ = null;
    private Band ddiBand = null;
    private Band cohBand = null;

    private String[] subSwathNames = null;


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public DoubleDifferenceInterferogramOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfSentinel1Product();

            su = new Sentinel1Utils(sourceProduct);
            su.computeDopplerRate();
            subSwath = su.getSubSwath();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            }

            cohWin = Integer.parseInt(cohWinSize);
            numOverlaps = subSwath[subSwathIndex - 1].numOfBursts - 1;

            mstBandI = getSourceBand(StackUtils.MST, Unit.REAL);
            mstBandQ = getSourceBand(StackUtils.MST, Unit.IMAGINARY);
            slvBandI = getSourceBand(StackUtils.SLV, Unit.REAL);
            slvBandQ = getSourceBand(StackUtils.SLV, Unit.IMAGINARY);

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        final int sourceImageWidth = sourceProduct.getSceneRasterWidth();
        final int sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(
                sourceProduct.getName(), sourceProduct.getProductType(), sourceImageWidth, sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        ddiBand = new Band("DDIPhase", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        ddiBand.setUnit("radian");
        ddiBand.setNoDataValue(0.0);
        ddiBand.setNoDataValueUsed(true);
        targetProduct.addBand(ddiBand);

        if (outputCoherence) {
            cohBand = new Band("coherence", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
            cohBand.setUnit(Unit.COHERENCE);
            cohBand.setNoDataValue(0.0);
            cohBand.setNoDataValueUsed(true);
            targetProduct.addBand(cohBand);
        }

        targetProduct.setPreferredTileSize(512, subSwath[subSwathIndex - 1].linesPerBurst);
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
     public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
             throws OperatorException {

        try {
            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            final int overlapIndex = y0 / subSwath[subSwathIndex - 1].linesPerBurst;
            if (overlapIndex > numOverlaps - 1) {
                return;
            }

            final Rectangle overlapInBurstOneRectangle =  new Rectangle();
            final Rectangle overlapInBurstTwoRectangle = new Rectangle();

            final boolean successful = getOverlappedRectangles(
                    overlapIndex, targetRectangle, overlapInBurstOneRectangle, overlapInBurstTwoRectangle);

            if (!successful) {
                return;
            }

            final double[][] ddiPhase = computeDDIPhase(overlapInBurstOneRectangle, overlapInBurstTwoRectangle);

            double[][] data = new double[h][w];
            final int x0DDI = overlapInBurstOneRectangle.x;
            final int y0DDI = overlapInBurstOneRectangle.y;
            final int xMaxDDI = x0DDI + overlapInBurstOneRectangle.width;
            final int yMaxDDI = y0DDI + overlapInBurstOneRectangle.height;
            for (int y = y0DDI; y < yMaxDDI; ++y) {
                final int r = y - y0;
                final int rr = y - y0DDI;
                for (int x = x0DDI; x < xMaxDDI; ++x) {
                    final int c = x - x0;
                    final int cc = x - x0DDI;
                    data[r][c] = ddiPhase[rr][cc];
                }
            }

            saveData(data, ddiBand, targetTileMap, targetRectangle);

            if (outputCoherence) {
                final double[][] coh = computeCoherence(
                        overlapInBurstOneRectangle, mstBandI, mstBandQ, slvBandI, slvBandQ, cohWin);

                for (int y = y0DDI; y < yMaxDDI; ++y) {
                    final int r = y - y0;
                    final int rr = y - y0DDI;
                    for (int x = x0DDI; x < xMaxDDI; ++x) {
                        final int c = x - x0;
                        final int cc = x - x0DDI;
                        data[r][c] = coh[rr][cc];
                    }
                }

                saveData(data, cohBand, targetTileMap, targetRectangle);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void saveData(final double[][] data, final Band tgtBand, Map<Band, Tile> targetTileMap,
                          final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        if (data.length != h || data[0].length != w) {
            throw new OperatorException("The target data to save has different dimension than the processing tile");
        }

        final Tile tgtTile = targetTileMap.get(tgtBand);
        final ProductData tgtData = tgtTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(tgtTile);
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        for (int y = y0; y < yMax; ++y) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                tgtData.setElemDoubleAt(tgtIndex.getIndex(x), data[yy][x - x0]);
            }
        }
    }

    private Band getSourceBand(final String suffix, final String bandUnit) {

        final String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (!bandName.contains(suffix)) {
                continue;
            }
            final Band band = sourceProduct.getBand(bandName);
            if (band.getUnit().contains(bandUnit)) {
                return band;
            }
        }
        return null;
    }

    /**
     * Compute double difference interferogram for given tile
     */
    private double[][] computeDDIPhase(final Rectangle overlapInBurstOneRectangle,
                                       final Rectangle overlapInBurstTwoRectangle) {

        try {
            final int w = overlapInBurstOneRectangle.width;
            final int h = overlapInBurstOneRectangle.height;
            if (overlapInBurstTwoRectangle.width != w || overlapInBurstTwoRectangle.height != h) {
                throw new OperatorException("Forward and backward rectangles have difference dimension");
            }

            final double[][] mIBack = getSourceData(mstBandI, overlapInBurstTwoRectangle);
            final double[][] mQBack = getSourceData(mstBandQ, overlapInBurstTwoRectangle);
            final double[][] sIBack = getSourceData(slvBandI, overlapInBurstTwoRectangle);
            final double[][] sQBack = getSourceData(slvBandQ, overlapInBurstTwoRectangle);

            final double[][] mIFor = getSourceData(mstBandI, overlapInBurstOneRectangle);
            final double[][] mQFor = getSourceData(mstBandQ, overlapInBurstOneRectangle);
            final double[][] sIFor = getSourceData(slvBandI, overlapInBurstOneRectangle);
            final double[][] sQFor = getSourceData(slvBandQ, overlapInBurstOneRectangle);

            final double[][] backIntReal = new double[h][w];
            final double[][] backIntImag = new double[h][w];
            complexArrayMultiplication(mIBack, mQBack, sIBack, sQBack, backIntReal, backIntImag);

            final double[][] forIntReal = new double[h][w];
            final double[][] forIntImag = new double[h][w];
            complexArrayMultiplication(mIFor, mQFor, sIFor, sQFor, forIntReal, forIntImag);

            final double[][] diffIntReal = new double[h][w];
            final double[][] diffIntImag = new double[h][w];
            complexArrayMultiplication(forIntReal, forIntImag, backIntReal, backIntImag, diffIntReal, diffIntImag);

            final double[][] ddiPhase = new double[h][w];
            for (int i = 0; i < h; ++i) {
                for (int j = 0; j < w; ++j) {
                    ddiPhase[i][j] = Math.atan2(diffIntImag[i][j], diffIntReal[i][j]);
                }
            }

            return ddiPhase;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeDDIPhase", e);
        }
        
        return null;
    }

    private boolean getOverlappedRectangles(final int overlapIndex,
                                            final Rectangle targetRectangle,
                                            final Rectangle overlapInBurstOneRectangle,
                                            final Rectangle overlapInBurstTwoRectangle) {

        final int firstValidPixelOfBurstOne = getBurstFirstValidPixel(overlapIndex);
        final int lastValidPixelOfBurstOne = getBurstLastValidPixel(overlapIndex);
        final int firstValidPixelOfBurstTwo = getBurstFirstValidPixel(overlapIndex + 1);
        final int lastValidPixelOfBurstTwo = getBurstLastValidPixel(overlapIndex + 1);
        final int firstValidPixel = Math.max(firstValidPixelOfBurstOne, firstValidPixelOfBurstTwo);
        final int lastValidPixel = Math.min(lastValidPixelOfBurstOne, lastValidPixelOfBurstTwo);
        final int x0 = Math.max(firstValidPixel, targetRectangle.x);
        final int xN = Math.min(lastValidPixel, targetRectangle.x + targetRectangle.width - 1);
        final int w = xN - x0 + 1;
        if (w <= 0) {
            return false;
        }

        final int numOfInvalidLinesInBurstOne = subSwath[subSwathIndex - 1].linesPerBurst -
                subSwath[subSwathIndex - 1].lastValidLine[overlapIndex] - 1;

        final int numOfInvalidLinesInBurstTwo = subSwath[subSwathIndex - 1].firstValidLine[overlapIndex + 1];

        final int numOverlappedLines = computeBurstOverlapSize(overlapIndex);

        final int h = numOverlappedLines - numOfInvalidLinesInBurstOne - numOfInvalidLinesInBurstTwo;

        final int y0BurstOne =
                subSwath[subSwathIndex - 1].linesPerBurst * (overlapIndex + 1) - numOfInvalidLinesInBurstOne - h;

        final int y0BurstTwo =
                subSwath[subSwathIndex - 1].linesPerBurst * (overlapIndex + 1) + numOfInvalidLinesInBurstTwo;

        overlapInBurstOneRectangle.setBounds(x0, y0BurstOne, w, h);
        overlapInBurstTwoRectangle.setBounds(x0, y0BurstTwo, w, h);
        return true;
    }

    private int getBurstFirstValidPixel(final int burstIndex) {

        for (int lineIdx = 0; lineIdx < subSwath[subSwathIndex - 1].firstValidSample[burstIndex].length; lineIdx++) {
            if (subSwath[subSwathIndex - 1].firstValidSample[burstIndex][lineIdx] != -1) {
                return subSwath[subSwathIndex - 1].firstValidSample[burstIndex][lineIdx];
            }
        }
        return -1;
    }

    private int getBurstLastValidPixel(final int burstIndex) {

        for (int lineIdx = 0; lineIdx < subSwath[subSwathIndex - 1].lastValidSample[burstIndex].length; lineIdx++) {
            if (subSwath[subSwathIndex - 1].lastValidSample[burstIndex][lineIdx] != -1) {
                return subSwath[subSwathIndex - 1].lastValidSample[burstIndex][lineIdx];
            }
        }
        return -1;
    }

    /**
     * Compute the number of lines in the overlapped area of given adjacent bursts.
     * @return The number of lines in the overlapped area.
     */
    private int computeBurstOverlapSize(final int overlapIndex) {

        final double endTime = subSwath[subSwathIndex - 1].burstLastLineTime[overlapIndex];
        final double startTime = subSwath[subSwathIndex - 1].burstFirstLineTime[overlapIndex + 1];
        return (int)((endTime - startTime) / subSwath[subSwathIndex - 1].azimuthTimeInterval);
    }

    private double[][] getSourceData(final Band srcBand, final Rectangle rectangle) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final Tile srcTile = getSourceTile(srcBand, rectangle);
        final ProductData srcData = srcTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(srcTile);
        final double[][] dataArray = new double[h][w];

        for (int y = y0; y < yMax; ++y) {
            srcIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                dataArray[yy][x - x0] = srcData.getElemDoubleAt(srcIndex.getIndex(x));
            }
        }

        return dataArray;
    }

    private static void complexArrayMultiplication(final double[][] realArray1, final double[][] imagArray1,
                                                   final double[][] realArray2, final double[][] imagArray2,
                                                   final double[][] realOutput, final double[][] imagOutput) {

        final int h = realArray1.length;
        final int w = realArray1[0].length;
        if (imagArray1.length != h || realArray2.length != h || imagArray2.length != h || realOutput.length != h ||
                imagOutput.length != h || imagArray1[0].length != w || realArray2[0].length != w ||
                imagArray2[0].length != w || realOutput[0].length != w || imagOutput[0].length != w) {
            throw new OperatorException("Arrays of the same dimension are expected.");
        }

        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                realOutput[i][j] = realArray1[i][j] * realArray2[i][j] + imagArray1[i][j] * imagArray2[i][j];
                imagOutput[i][j] = imagArray1[i][j] * realArray2[i][j] - realArray1[i][j] * imagArray2[i][j];
            }
        }
    }

    private double[][] computeCoherence(final Rectangle rectangle, final Band mBandI, final Band mBandQ,
                                        final Band sBandI, final Band sBandQ, final int cohWin) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        final int halfWindowSize = cohWin / 2;
        final double[][] coherence = new double[h][w];

        final Tile mstTileI = getSourceTile(mBandI, rectangle);
        final Tile mstTileQ = getSourceTile(mBandQ, rectangle);
        final ProductData mstDataBufferI = mstTileI.getDataBuffer();
        final ProductData mstDataBufferQ = mstTileQ.getDataBuffer();

        final Tile slvTileI = getSourceTile(sBandI, rectangle);
        final Tile slvTileQ = getSourceTile(sBandQ, rectangle);
        final ProductData slvDataBufferI = slvTileI.getDataBuffer();
        final ProductData slvDataBufferQ = slvTileQ.getDataBuffer();

        final TileIndex srcIndex = new TileIndex(mstTileI);

        final double[][] cohReal = new double[h][w];
        final double[][] cohImag = new double[h][w];
        final double[][] mstPower = new double[h][w];
        final double[][] slvPower = new double[h][w];
        for (int y = y0; y < yMax; ++y) {
            srcIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int srcIdx = srcIndex.getIndex(x);
                final int xx = x - x0;

                final float mI = mstDataBufferI.getElemFloatAt(srcIdx);
                final float mQ = mstDataBufferQ.getElemFloatAt(srcIdx);
                final float sI = slvDataBufferI.getElemFloatAt(srcIdx);
                final float sQ = slvDataBufferQ.getElemFloatAt(srcIdx);

                cohReal[yy][xx] = mI * sI + mQ * sQ;
                cohImag[yy][xx] = mQ * sI - mI * sQ;
                mstPower[yy][xx] = mI * mI + mQ * mQ;
                slvPower[yy][xx] = sI * sI + sQ * sQ;
            }
        }

        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;

                final int rowSt = Math.max(yy - halfWindowSize, 0);
                final int rowEd = Math.min(yy + halfWindowSize, h - 1);
                final int colSt = Math.max(xx - halfWindowSize, 0);
                final int colEd = Math.min(xx + halfWindowSize, w - 1);

                double cohRealSum = 0.0f, cohImagSum = 0.0f, mstPowerSum = 0.0f, slvPowerSum = 0.0f;
                int count = 0;
                for (int r = rowSt; r <= rowEd; r++) {
                    for (int c = colSt; c <= colEd; c++) {
                        cohRealSum += cohReal[r][c];
                        cohImagSum += cohImag[r][c];
                        mstPowerSum += mstPower[r][c];
                        slvPowerSum += slvPower[r][c];
                        count++;
                    }
                }

                if (count > 0 && mstPowerSum != 0.0 && slvPowerSum != 0.0) {
                    final double cohRealMean = cohRealSum / (double)count;
                    final double cohImagMean = cohImagSum / (double)count;
                    final double mstPowerMean = mstPowerSum / (double)count;
                    final double slvPowerMean = slvPowerSum / (double)count;
                    coherence[yy][xx] = Math.sqrt((cohRealMean * cohRealMean + cohImagMean * cohImagMean) /
                            (mstPowerMean * slvPowerMean));
                }
            }
        }
        return coherence;
    }

    private static class AzimuthShiftData {
        int overlapIndex;
        int blockIndex;
        double shift;

        public AzimuthShiftData(final int overlapIndex, final int blockIndex, final double shift) {
            this.overlapIndex = overlapIndex;
            this.blockIndex = blockIndex;
            this.shift = shift;
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(DoubleDifferenceInterferogramOp.class);
        }
    }

}
