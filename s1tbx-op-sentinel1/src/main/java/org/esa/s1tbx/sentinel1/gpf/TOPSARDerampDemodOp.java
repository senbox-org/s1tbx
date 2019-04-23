/*
 * Copyright (C) 2019 by SkyWatch Space Applications
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
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This operator performs deramp and demodulation to the input S-1 TOPS SLC product.
 */
@OperatorMetadata(alias = "TOPSAR-DerampDemod",
        category = "Radar/Sentinel-1 TOPS",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2019 by SkyWatch Space Applications",
        description = "Bursts co-registration using orbit and DEM")
public final class TOPSARDerampDemodOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false", label = "Output Deramp and Demod Phase")
    private boolean outputDerampDemodPhase = false;

    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
	private int subSwathIndex = 0;
    private String swathIndexStr = null;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public TOPSARDerampDemodOp() {
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
            if (sourceProduct == null) {
                return;
            }

            checkSourceProductValidity();

            su = new Sentinel1Utils(sourceProduct);
            subSwath = su.getSubSwath();
            subSwath = su.getSubSwath();
            su.computeDopplerRate();
            su.computeReferenceTime();

			final String[] mSubSwathNames = su.getSubSwathNames();

			subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            swathIndexStr = mSubSwathNames[0].substring(2);
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Check source product validity.
     */
    private void checkSourceProductValidity() throws OperatorException {

        final InputProductValidator validator = new InputProductValidator(sourceProduct);
        validator.checkIfSARProduct();
        validator.checkIfSentinel1Product();
        validator.checkIfSLC();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(
                sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        
        final String[] sourceBandNames = sourceProduct.getBandNames();
        for (String bandName : sourceBandNames) {
            final Band srcBand = sourceProduct.getBand(bandName);
            if (srcBand instanceof VirtualBand) {
                continue;
            }

            final Band targetBand = new Band(bandName, ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
            targetBand.setUnit(srcBand.getUnit());
            targetBand.setDescription(srcBand.getDescription());
            targetProduct.addBand(targetBand);

            if (targetBand.getUnit().equals(Unit.IMAGINARY)) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBandAt(idx - 1), targetBand, "");
            }
        }

        if (outputDerampDemodPhase) {
            final Band phaseBand = new Band("derampDemodPhase", ProductData.TYPE_FLOAT32,
                    sourceImageWidth, sourceImageHeight);
            phaseBand.setUnit("radian");
            targetProduct.addBand(phaseBand);
        }
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, "deramp_demod", 1);
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
            final int yMax = y0 + h;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            for (int burstIndex = 0; burstIndex < subSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
                final int firstLineIdx = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + subSwath[subSwathIndex - 1].linesPerBurst - 1;

                if (yMax <= firstLineIdx || y0 > lastLineIdx) {
                    continue;
                }

				final int nx0 = x0;
				final int nw = w;
                final int ny0 = Math.max(y0, firstLineIdx);
                final int nyMax = Math.min(yMax, lastLineIdx + 1);
                final int nth = nyMax - ny0;
                //System.out.println("burstIndex = " + burstIndex + ": nx0 = " + nx0 + ", ny0 = " + ny0 + ", nw = " + nw + ", nh = " + nh);

                computePartialTile(subSwathIndex, burstIndex, nx0, ny0, nw, nth, targetTileMap);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computePartialTile(final int subSwathIndex, final int burstIndex,
                                    final int x0, final int y0, final int w, final int h,
                                    final Map<Band, Tile> targetTileMap) throws Exception {

        final Rectangle targetRectangle = new Rectangle(x0, y0, w, h);
        final double[][] derampDemodPhase = su.computeDerampDemodPhase(subSwath, subSwathIndex, burstIndex, targetRectangle);

        if (derampDemodPhase == null) {
            return;
        }

        if (outputDerampDemodPhase) {
            saveDrampDemodPhase(x0, y0, w, h, targetTileMap, derampDemodPhase);
        }

        for(String polarization : su.getPolarizations()) {
            final Band bandI = getBand(sourceProduct, "i_", swathIndexStr, polarization);
            final Band bandQ = getBand(sourceProduct, "q_", swathIndexStr, polarization);
            final Tile tileI = getSourceTile(bandI, targetRectangle);
            final Tile tileQ = getSourceTile(bandQ, targetRectangle);

            if (tileI == null || tileQ == null) {
                return;
            }

            final double[][] derampDemodI = new double[targetRectangle.height][targetRectangle.width];
            final double[][] derampDemodQ = new double[targetRectangle.height][targetRectangle.width];

            performDerampDemod(tileI, tileQ, targetRectangle, derampDemodPhase, derampDemodI, derampDemodQ);

            saveTargetBands(x0, y0, w, h, targetTileMap, derampDemodI, derampDemodQ, polarization);
        }
    }

    private static void performDerampDemod(final Tile tileI, final Tile tileQ,
                                           final Rectangle targetRectangle, final double[][] derampDemodPhase,
                                           final double[][] derampDemodI, final double[][] derampDemodQ) {

        try {
            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int xMax = x0 + targetRectangle.width;
            final int yMax = y0 + targetRectangle.height;

            final ProductData dataI = tileI.getDataBuffer();
            final ProductData dataQ = tileQ.getDataBuffer();
            final TileIndex index = new TileIndex(tileI);

            for (int y = y0; y < yMax; y++) {
                index.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; x++) {
                    final int idx = index.getIndex(x);
                    final int xx = x - x0;
                    final double valueI = dataI.getElemDoubleAt(idx);
                    final double valueQ = dataQ.getElemDoubleAt(idx);
                    final double cosPhase = FastMath.cos(derampDemodPhase[yy][xx]);
                    final double sinPhase = FastMath.sin(derampDemodPhase[yy][xx]);
                    derampDemodI[yy][xx] = valueI*cosPhase - valueQ*sinPhase;
                    derampDemodQ[yy][xx] = valueI*sinPhase + valueQ*cosPhase;
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("performDerampDemod", e);
        }
    }

    private void saveTargetBands(final int x0, final int y0, final int w, final int h,
                                 final Map<Band, Tile> targetTileMap, final double[][] derampDemodI,
                                 final double[][] derampDemodQ, final String polarization) throws OperatorException {

        try {
            final Band bandI = getBand(targetProduct, "i_", swathIndexStr, polarization);
            final Band bandQ = getBand(targetProduct, "q_", swathIndexStr, polarization);

            if (bandI == null || bandQ == null) {
                throw new OperatorException("Unable to find target band " + bandI.getName() +" or "+ bandQ.getName());
            }

            final Tile tgtTileI = targetTileMap.get(bandI);
            final Tile tgtTileQ = targetTileMap.get(bandQ);
            final ProductData tgtBufferI = tgtTileI.getDataBuffer();
            final ProductData tgtBufferQ = tgtTileQ.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileI);

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int xx = x - x0;
                    final int tgtIdx = tgtIndex.getIndex(x);
                    tgtBufferI.setElemDoubleAt(tgtIdx, derampDemodI[yy][xx]);
                    tgtBufferQ.setElemDoubleAt(tgtIdx, derampDemodQ[yy][xx]);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("saveTargetBands", e);
        }
    }

    private static Band getBand(
            final Product product, final String prefix, final String swathIndexStr, final String polarization) {

        final String[] bandNames = product.getBandNames();
        for (String bandName:bandNames) {
            if (bandName.contains(prefix) && bandName.contains(swathIndexStr) && bandName.contains(polarization)) {
                return product.getBand(bandName);
            }
        }
        return null;
    }

    private void saveDrampDemodPhase(final int x0, final int y0, final int w, final int h,
                                     final Map<Band, Tile> targetTileMap, final double[][] derampDemodPhase) {

        final Band phaseBand = targetProduct.getBand("derampDemodPhase");
        if(phaseBand == null) {
            throw new OperatorException("Target band derampDemodPhase not found");
        }

        final Tile tgtTilePhase = targetTileMap.get(phaseBand);
        final ProductData tgtBufferPhase = tgtTilePhase.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(tgtTilePhase);

        for (int y = y0; y < y0 + h; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < x0 + w; x++) {
                final int xx = x - x0;
                final int tgtIdx = tgtIndex.getIndex(x);
                tgtBufferPhase.setElemDoubleAt(tgtIdx, derampDemodPhase[yy][xx]);
            }
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(TOPSARDerampDemodOp.class);
        }
    }
}
