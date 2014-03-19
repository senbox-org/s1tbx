/*
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
package org.jlinda.nest.gpf.unwrapping.external;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;
import org.jlinda.core.Constants;
import org.jlinda.core.unwrapping.mcf.utils.UnwrapUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "Stitch",
        category = "InSAR\\Unwrapping",
        description = "Unwrap phase for each tile",
        internal = false)
public class StitchOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private boolean phaseDifferenceComputed = false;
    private double[][] solution = null;

    private static final String UNW_PHASE_BAND_NAME = "unwrapped_phase";

    private int tileWidth = 16;
    private int tileHeight = 16;
    
    // 
    private String arch;
    private String name;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {

            archFlavor();
            
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            createTargetProduct();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void archFlavor() {
        arch = System.getProperty("os.arch");
        name = System.getProperty("os.name");

        if (name.startsWith("Linux") && arch.equals("amd64")) {

            // extend tiles
            tileWidth = 64;
            tileHeight = 64;
        }
    }
    
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        final int numSrcBands = sourceProduct.getNumBands();
        for (int i = 0; i < numSrcBands; ++i) {
            final Band srcBand = sourceProduct.getBandAt(i);
            final Band targetBand = targetProduct.addBand(srcBand.getName(), srcBand.getDataType());
            targetBand.setUnit(srcBand.getUnit());
            ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
            if (!srcBand.getUnit().equals(Unit.ABS_PHASE)) {
                targetBand.setSourceImage(srcBand.getSourceImage());
            }
        }

        targetProduct.setPreferredTileSize(tileWidth, tileHeight);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        try {
            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final int tileIndexX = (int) FastMath.ceil(targetRectangle.x / (double) tileWidth);
            final int tileIndexY = (int) FastMath.ceil(targetRectangle.y / (double) tileHeight);

            Band phaseBand = null;
            for (Band band : sourceProduct.getBands()) {
                if (band.getUnit().equals(Unit.ABS_PHASE)) {
                    phaseBand = band;
                    break;
                }
            }

            if (!phaseDifferenceComputed) {
                computePhaseDifference(phaseBand, tileWidth, tileHeight);
            }

            final Band[] targetBands = targetProduct.getBands();
            final int numBands = targetBands.length;
            Tile targetTile = null;
            for (int i = 0; i < numBands; i++) {
                if (targetBands[i].getUnit().equals(Unit.ABS_PHASE)) {
                    targetTile = targetTileMap.get(targetBands[i]);
                    break;
                }
            }

            if (targetTile == null) {
                throw new OperatorException("No phase band exist in source product");
            }

            final Tile phaseRaster = getSourceTile(phaseBand, targetRectangle);
            final ProductData phaseData = phaseRaster.getRawSamples();
            final int num = phaseData.getNumElems();
            final float[] unWrapped = new float[num];
            final double solutionValue = solution[tileIndexY][tileIndexX];
            for (int i = 0; i < num; ++i) {
                unWrapped[i] = (float) (phaseData.getElemDoubleAt(i) + solutionValue);
            }

            targetTile.setRawSamples(new ProductData.Float(unWrapped));

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private synchronized void computePhaseDifference(final Band phaseBand, final int tileWidth, final int tileHeight) {

        if (phaseDifferenceComputed) return;

        final int tileCountX = (int) FastMath.ceil(sourceImageWidth / (double) tileWidth);
        final int tileCountY = (int) FastMath.ceil(sourceImageHeight / (double) tileHeight);

        final int xRef = tileCountX / 2;
        final int yRef = tileCountY / 2;
        final short[][] flags = new short[tileCountY][tileCountX];
        solution = new double[tileCountY][tileCountX];
        flags[yRef][xRef] |= Mask.UNWRAPPED;
        solution[yRef][xRef] = 0.0;

        // set initial active front
        int numActiveFrontPixels = 0;
        if (yRef - 1 >= 0) {
            flags[yRef - 1][xRef] |= Mask.ACTIVE;
            numActiveFrontPixels++;
        }

        if (yRef + 1 < tileCountY) {
            flags[yRef + 1][xRef] |= Mask.ACTIVE;
            numActiveFrontPixels++;
        }

        if (xRef - 1 >= 0) {
            flags[yRef][xRef - 1] |= Mask.ACTIVE;
            numActiveFrontPixels++;
        }

        if (xRef + 1 < tileCountX) {
            flags[yRef][xRef + 1] |= Mask.ACTIVE;
            numActiveFrontPixels++;
        }

        while (numActiveFrontPixels > 0) {
            //System.out.println("numActiveFrontPixels = " + numActiveFrontPixels);

            for (int tileY = 0; tileY < tileCountY; tileY++) {

                for (int tileX = 0; tileX < tileCountX; tileX++) {

                    if ((flags[tileY][tileX] & Mask.ACTIVE) <= 0) {
                        continue;
                    }

                    //System.out.println();
                    //System.out.println("Found active front tile: tileY = " + tileY + ", tileX = " + tileX);
                    final int x0 = tileX * tileWidth;
                    final int y0 = tileY * tileHeight;
                    final int w = Math.min(tileWidth, sourceImageWidth - x0);
                    final int h = Math.min(tileHeight, sourceImageHeight - y0);

                    final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
                    final Tile sourceTile = getSourceTile(phaseBand, sourceRectangle);
                    final ProductData dataBuffer = sourceTile.getDataBuffer();

                    boolean unwrapSucceed = false;

                    // First search down for a phase unwrapped tile
                    if (tileY + 1 <= tileCountY - 1 && (flags[tileY + 1][tileX] & Mask.UNWRAPPED) > 0) {

                        final double[] wrappedPhases = new double[w];
                        final double[] unWrappedPhases = new double[w];

                        getWrappedPhaseData(x0, y0 + h - 1, w, 1, sourceTile, dataBuffer, wrappedPhases);
                        getUnwrappedPhaseData(x0, y0 + h, w, 1, sourceTile, dataBuffer, unWrappedPhases, solution[tileY + 1][tileX]);

                        unwrapSucceed = unwrapPhase(wrappedPhases, unWrappedPhases, tileX, tileY, solution);

                        if (unwrapSucceed) {
                            flags[tileY][tileX] |= Mask.UNWRAPPED;
                            flags[tileY][tileX] ^= Mask.ACTIVE;
                            numActiveFrontPixels--;
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile below, succeed");

                            // find new active front pixels in other 3 directions: up, left, right
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX, tileY - 1, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX - 1, tileY, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX + 1, tileY, tileCountX, tileCountY);
                        } else {
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile below, failed");
                        }
                    }

                    // Second search up for a phase unwrapped tile
                    if (!unwrapSucceed && tileY > 0 && (flags[tileY - 1][tileX] & Mask.UNWRAPPED) > 0) {

                        final double[] wrappedPhases = new double[w];
                        final double[] unWrappedPhases = new double[w];

                        getWrappedPhaseData(x0, y0, w, 1, sourceTile, dataBuffer, wrappedPhases);
                        getUnwrappedPhaseData(x0, y0 - 1, w, 1, sourceTile, dataBuffer, unWrappedPhases, solution[tileY - 1][tileX]);

                        unwrapSucceed = unwrapPhase(wrappedPhases, unWrappedPhases, tileX, tileY, solution);

                        if (unwrapSucceed) {
                            flags[tileY][tileX] |= Mask.UNWRAPPED;
                            flags[tileY][tileX] ^= Mask.ACTIVE;
                            numActiveFrontPixels--;
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile above, succeed");

                            // find new water front pixels in other 3 directions: down, left, right
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX, tileY + 1, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX - 1, tileY, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX + 1, tileY, tileCountX, tileCountY);
                        } else {
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile above, failed");
                        }
                    }

                    // Third search right for a phase unwrapped pixel
                    if (!unwrapSucceed && tileX < tileCountX - 1 && (flags[tileY][tileX + 1] & Mask.UNWRAPPED) > 0) {

                        final double[] wrappedPhases = new double[h];
                        final double[] unWrappedPhases = new double[h];

                        getWrappedPhaseData(x0 + w - 1, y0, 1, h, sourceTile, dataBuffer, wrappedPhases);
                        getUnwrappedPhaseData(x0 + w, y0, 1, h, sourceTile, dataBuffer, unWrappedPhases, solution[tileY][tileX + 1]);

                        unwrapSucceed = unwrapPhase(wrappedPhases, unWrappedPhases, tileX, tileY, solution);

                        if (unwrapSucceed) {
                            flags[tileY][tileX] |= Mask.UNWRAPPED;
                            flags[tileY][tileX] ^= Mask.ACTIVE;
                            numActiveFrontPixels--;
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile right, succeed");

                            // find new water front pixels in other 3 directions: up, down, left
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX, tileY - 1, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX, tileY + 1, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX - 1, tileY, tileCountX, tileCountY);
                        } else {
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile right, failed");
                        }

                    }

                    // Finally search left for a phase unwrapped pixel
                    if (!unwrapSucceed && tileX > 0 && (flags[tileY][tileX - 1] & Mask.UNWRAPPED) > 0) {

                        final double[] wrappedPhases = new double[h];
                        final double[] unWrappedPhases = new double[h];

                        getWrappedPhaseData(x0, y0, 1, h, sourceTile, dataBuffer, wrappedPhases);
                        getUnwrappedPhaseData(x0 - 1, y0, 1, h, sourceTile, dataBuffer, unWrappedPhases, solution[tileY][tileX - 1]);

                        unwrapSucceed = unwrapPhase(wrappedPhases, unWrappedPhases, tileX, tileY, solution);

                        if (unwrapSucceed) {
                            flags[tileY][tileX] |= Mask.UNWRAPPED;
                            flags[tileY][tileX] ^= Mask.ACTIVE;
                            numActiveFrontPixels--;
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile left, succeed");

                            // find new water front pixels in other 3 directions: up, down, right
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX, tileY - 1, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX, tileY + 1, tileCountX, tileCountY);
                            numActiveFrontPixels += addActiveFrontPixel(flags, tileX + 1, tileY, tileCountX, tileCountY);
                        } else {
                            //System.out.println("Unwrap tile: tileY = " + tileY + ", tileX = " + tileX + " from the tile left, failed");
                        }
                    }

                    if (!unwrapSucceed) {
                        flags[tileY][tileX] ^= Mask.ACTIVE;
                        numActiveFrontPixels--;
                    }
                }
            }
        }
        phaseDifferenceComputed = true;
    }

    private static int addActiveFrontPixel(final short[][] bitFlags, final int tileX, final int tileY,
                                           final int tileCountX, final int tileCountY) {

        if (tileX >= 0 && tileX < tileCountX && tileY >= 0 && tileY < tileCountY &&
                (bitFlags[tileY][tileX] & Mask.UNWRAPPED) <= 0 &&
                (bitFlags[tileY][tileX] & Mask.ACTIVE) <= 0) {
            bitFlags[tileY][tileX] |= Mask.ACTIVE;
            return 1;
        }
        return 0;
    }

    /**
     * Get the source tile rectangle.
     *
     * @param x0 The x coordinate of the pixel on the upper left corner of current tile.
     * @param y0 The y coordinate of the pixel on the upper left corner of current tile.
     * @param w  The width of current tile.
     * @param h  The height of current tile.
     * @return The rectangle.
     */
    private Rectangle getSourceRectangle(final int x0, final int y0, final int w, final int h) {

        final int sx0 = Math.max(x0 - 1, 0);
        final int sy0 = Math.max(y0 - 1, 0);
        final int sxMax = Math.min(x0 + w, sourceImageWidth - 1);
        final int syMax = Math.min(y0 + h, sourceImageHeight - 1);
        final int sw = sxMax - sx0 + 1;
        final int sh = syMax - sy0 + 1;

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * Get wrapped phase data from source tile for given rectangle.
     *
     * @param x0         X coordinate of the upper left pixel of the given rectangle
     * @param y0         Y coordinate of the upper left pixel of the given rectangle
     * @param w          Width of the given rectangle
     * @param h          Height of the given rectangle
     * @param sourceTile The source tile
     * @param dataBuffer The source data
     * @param phaseData  The wrapped phase data array
     */
    private static void getWrappedPhaseData(final int x0, final int y0, final int w, final int h,
                                            final Tile sourceTile, final ProductData dataBuffer, double[] phaseData) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        int k = 0;
        for (int y = y0; y < y0 + h; y++) {
            srcIndex.calculateStride(y);
            for (int x = x0; x < x0 + w; x++) {
                phaseData[k++] = dataBuffer.getElemDoubleAt(srcIndex.getIndex(x));
            }
        }
    }

    /**
     * Get unwrapped phase data from source tile for given rectangle.
     *
     * @param x0         X coordinate of the upper left pixel of the given rectangle
     * @param y0         Y coordinate of the upper left pixel of the given rectangle
     * @param w          Width of the given rectangle
     * @param h          Height of the given rectangle
     * @param sourceTile The source tile
     * @param dataBuffer The source data
     * @param refPhase   The reference unwrapped phase
     * @param phaseData  The unwrapped phase data array
     */
    private static void getUnwrappedPhaseData(final int x0, final int y0, final int w, final int h, final Tile sourceTile,
                                              final ProductData dataBuffer, double[] phaseData, double refPhase) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        int k = 0;
        for (int y = y0; y < y0 + h; y++) {
            srcIndex.calculateStride(y);
            for (int x = x0; x < x0 + w; x++) {
                phaseData[k++] = dataBuffer.getElemDoubleAt(srcIndex.getIndex(x)) + refPhase;
            }
        }
    }

    /**
     * Unwrap a given wrapped phase array using a reference unwrapped phase array provided
     *
     * @param wrappedPhases      The wrapped phase data array
     * @param refUnWrappedPhases The reference unwrapped phase data array
     * @param tileX              Tile index in range direction
     * @param tileY              Tile index in azimuth direction
     * @param solution           The unwrapped phase for the given tile
     * @return Boolean flag indicating if the phase unwrapping is successful
     */
    private static boolean unwrapPhase(final double[] wrappedPhases, final double[] refUnWrappedPhases,
                                       final int tileX, final int tileY,
                                       double[][] solution) {

        final int len = wrappedPhases.length;
        if (len != refUnWrappedPhases.length) {
            throw new OperatorException("WrappedPhase array must have the same length as refUnWrappedPhases array");
        }

        final Map<Integer, Long> count = new HashMap<>(len);
        for (int i = 0; i < len; i++) {
            final double unWrappedPhase = UnwrapUtils.unwrap(refUnWrappedPhases[i], wrappedPhases[i]);

            final int numCycles = Math.round((float) ((unWrappedPhase - wrappedPhases[i]) / Constants._TWO_PI));
            if (count.containsKey(numCycles)) {
                count.put(numCycles, count.get(numCycles) + 1);
            } else {
                count.put(numCycles, 1L);
            }
        }

        long countMax = 0;
        int nc = 0;
        for (int n : count.keySet()) {
            if (count.get(n) > countMax) {
                countMax = count.get(n);
                nc = n;
            }
        }

        //System.out.println("countMax = " + countMax + ", len/2 = " + (len/2));
        if (countMax > len * 2 / 3) {
            solution[tileY][tileX] = nc * Constants._TWO_PI;
            return true;
        } else {
            solution[tileY][tileX] = 0.0;
            return false;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(StitchOp.class);
        }
    }
}