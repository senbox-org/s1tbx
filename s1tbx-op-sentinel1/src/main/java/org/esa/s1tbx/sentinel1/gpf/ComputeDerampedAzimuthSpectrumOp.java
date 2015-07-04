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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.insar.gpf.Sentinel1Utils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Perform deramping for each burst, then compute azimuth spectrum.
 * The output azimuth spectrum can be used in verifying the deramp phase computed.
 */
@OperatorMetadata(alias = "Azimuth-Spectrum",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Compute azimuth spectrum for each deramped burst",
        internal = true)
public final class ComputeDerampedAzimuthSpectrumOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "true", label = "Perform Deramp Only")
    private boolean derampOnly = true; // perform deramp only, no demodulation is performed

    @Parameter(defaultValue = "false", label = "Output Deramp Phase")
    private boolean outputDerampPhase = false;

    @Parameter(defaultValue = "false", label = "Output Demodulation Phase")
    private boolean outputDemodPhase = false;

    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;

	private int subSwathIndex = 0;
    private String swathIndexStr = null;
    private String polarization = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ComputeDerampedAzimuthSpectrumOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.framework.datamodel.Product} annotated with the
     * {@link org.esa.snap.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during operator initialisation.
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
            su.computeDopplerRate();
            su.computeReferenceTime();
            subSwath = su.getSubSwath();
			
			final String[] subSwathNames = su.getSubSwathNames();
			if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            }

			subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            swathIndexStr = subSwathNames[0].substring(2);

			final String[] polarizations = su.getPolarizations();
			if (polarizations.length != 1) {
                throw new OperatorException("Split product with one polarization is expected.");
            }

			polarization = polarizations[0];

            createTargetProduct();

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    /**
     * Check source product validity.
     */
    private void checkSourceProductValidity() {

        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            throw new OperatorException("Source product has invalid mission for Sentinel1 product");
        }

        final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (!productType.equals("SLC")) {
            throw new OperatorException("Source product should be SLC product");
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        final int sourceImageWidth = sourceProduct.getSceneRasterWidth();
        final int sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(
                sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        final Band azSpecBand = new Band(
                "azSpec",
                ProductData.TYPE_FLOAT32,
                sourceImageWidth,
                sourceImageHeight);

        azSpecBand.setUnit(Unit.INTENSITY);
        targetProduct.addBand(azSpecBand);

        if (outputDerampPhase) {
            final Band derampPhaseBand = new Band(
                    "derampPhase",
                    ProductData.TYPE_FLOAT32,
                    sourceImageWidth,
                    sourceImageHeight);

            derampPhaseBand.setUnit("radian");
            targetProduct.addBand(derampPhaseBand);
        }

        if (outputDemodPhase) {
            final Band demodPhaseBand = new Band(
                    "demodPhase",
                    ProductData.TYPE_FLOAT32,
                    sourceImageWidth,
                    sourceImageHeight);

            demodPhaseBand.setUnit("radian");
            targetProduct.addBand(demodPhaseBand);
        }

        targetProduct.setPreferredTileSize(512, subSwath[0].linesPerBurst);
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.framework.gpf.OperatorException
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
            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final int burstIndex = y0 / subSwath[subSwathIndex - 1].linesPerBurst;

            final double[][] derampDemodPhase = computeDerampDemodPhase(
                    subSwathIndex, burstIndex, targetRectangle, targetTileMap);

            if (derampDemodPhase == null) {
                return;
            }

            final Band srcBandI = getBand(sourceProduct, "i_", swathIndexStr, polarization);
            final Band srcBandQ = getBand(sourceProduct, "q_", swathIndexStr, polarization);
            final Tile srcTileI = getSourceTile(srcBandI, targetRectangle);
            final Tile srcTileQ = getSourceTile(srcBandQ, targetRectangle);

            final double[][] derampDemodI = new double[h][w];
            final double[][] derampDemodQ = new double[h][w];

            performDerampDemod(srcTileI, srcTileQ, targetRectangle, derampDemodPhase, derampDemodI, derampDemodQ);

            computeAzimuthSpectrum(w, h, derampDemodI, derampDemodQ, targetTileMap);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeTile", e);
        }
    }

    private double[][] computeDerampDemodPhase(final int subSwathIndex, final int burstIndex,
                                               final Rectangle rectangle, final Map<Band, Tile> targetTileMap) {

        try {
            final int x0 = rectangle.x;
            final int y0 = rectangle.y;
            final int w = rectangle.width;
            final int h = rectangle.height;
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            final int s = subSwathIndex - 1;

            Band derampPhaseBand = null;
            Tile derampPhaseTile = null;
            ProductData derampPhaseBuffer = null;
            if (outputDerampPhase) {
                derampPhaseBand = targetProduct.getBand("derampPhase");
                derampPhaseTile = targetTileMap.get(derampPhaseBand);
                derampPhaseBuffer = derampPhaseTile.getDataBuffer();
            }

            Band demodPhaseBand = null;
            Tile demodPhaseTile = null;
            ProductData demodPhaseBuffer = null;
            if (outputDemodPhase) {
                demodPhaseBand = targetProduct.getBand("demodPhase");
                demodPhaseTile = targetTileMap.get(demodPhaseBand);
                demodPhaseBuffer = demodPhaseTile.getDataBuffer();
            }

            TileIndex tgtIndex = null;
            if (derampPhaseTile != null) {
                tgtIndex = new TileIndex(derampPhaseTile);
            } else if (demodPhaseTile != null) {
                tgtIndex = new TileIndex(demodPhaseTile);
            }

            final double[][] phase = new double[h][w];
            final int firstLineInBurst = burstIndex*subSwath[s].linesPerBurst;

            if (outputDerampPhase || outputDemodPhase) {
                for (int y = y0; y < yMax; y++) {
                    tgtIndex.calculateStride(y);
                    final int yy = y - y0;
                    final double ta = (y - firstLineInBurst)*subSwath[s].azimuthTimeInterval;
                    for (int x = x0; x < xMax; x++) {
                        final int tgtIdx = tgtIndex.getIndex(x);
                        final int xx = x - x0;
                        final double kt = subSwath[s].dopplerRate[burstIndex][x];
                        final double deramp = -Math.PI * kt * Math.pow(ta - subSwath[s].referenceTime[burstIndex][x], 2);
                        final double demod = -2 * Math.PI * subSwath[s].dopplerCentroid[burstIndex][x] * ta;
                        if (derampOnly) {
                            phase[yy][xx] = deramp;
                        } else {
                            phase[yy][xx] = deramp + demod;
                        }

                        if (outputDerampPhase) {
                            derampPhaseBuffer.setElemDoubleAt(tgtIdx, deramp);
                        }

                        if (outputDemodPhase) {
                            demodPhaseBuffer.setElemDoubleAt(tgtIdx, demod);
                        }
                    }
                }

            } else {

                for (int y = y0; y < yMax; y++) {
                    final int yy = y - y0;
                    final double ta = (y - firstLineInBurst)*subSwath[s].azimuthTimeInterval;
                    for (int x = x0; x < xMax; x++) {
                        final int xx = x - x0;
                        final double kt = subSwath[s].dopplerRate[burstIndex][x];
                        final double deramp = -Math.PI * kt * Math.pow(ta - subSwath[s].referenceTime[burstIndex][x], 2);
                        if (derampOnly) {
                            phase[yy][xx] = deramp;
                        } else {
                            final double demod = -2 * Math.PI * subSwath[s].dopplerCentroid[burstIndex][x] * ta;
                            phase[yy][xx] = deramp + demod;
                        }
                    }
                }
            }

            return phase;
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeDerampDemodPhase", e);
        }

        return null;
    }

    private void performDerampDemod(final Tile srcTileI, final Tile srcTileQ,
                                    final Rectangle rectangle, final double[][] derampDemodPhase,
                                    final double[][] derampDemodI, final double[][] derampDemodQ) {

        try {
            final int x0 = rectangle.x;
            final int y0 = rectangle.y;
            final int xMax = x0 + rectangle.width;
            final int yMax = y0 + rectangle.height;

            final ProductData srcDataI = srcTileI.getDataBuffer();
            final ProductData srcDataQ = srcTileQ.getDataBuffer();
            final TileIndex srcIndex = new TileIndex(srcTileI);

            for (int y = y0; y < yMax; y++) {
                srcIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; x++) {
                    final int idx = srcIndex.getIndex(x);
                    final int xx = x - x0;
                    final double valueI = srcDataI.getElemDoubleAt(idx);
                    final double valueQ = srcDataQ.getElemDoubleAt(idx);
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

    private void computeAzimuthSpectrum(final int w, final int h, final double[][] derampDemodI,
                                        final double[][] derampDemodQ, final Map<Band, Tile> targetTileMap) {

        try {
            final Band targetBand = targetProduct.getBand("azSpec");
            final Tile targetTile = targetTileMap.get(targetBand);
            final float[] tgtArray  = (float[]) targetTile.getDataBuffer().getElems();

            final double[] col = new double[2*h];
            final DoubleFFT_1D col_fft = new DoubleFFT_1D(h);
            final int h2 = h*h;

            for (int c = 0; c < w; c++) {
                for (int r = 0; r < h; r++) {
                    col[2*r] = derampDemodI[r][c];
                    col[2*r + 1] = derampDemodQ[r][c];
                }

                col_fft.complexForward(col);

                for (int r = 0; r < h; r++) {
                    tgtArray[r*w + c] = (float)(col[2*r]*col[2*r] + col[2*r + 1]*col[2*r + 1])/h2;
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeAzimuthSpectrum", e);
        }
    }

    private Band getBand(
            final Product product, final String prefix, final String swathIndexStr, final String polarization) {

        final String[] bandNames = product.getBandNames();
        for (String bandName:bandNames) {
            if (bandName.contains(prefix) && bandName.contains(swathIndexStr) && bandName.contains(polarization)) {
                return product.getBand(bandName);
            }
        }
        return null;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ComputeDerampedAzimuthSpectrumOp.class);
        }
    }
}
