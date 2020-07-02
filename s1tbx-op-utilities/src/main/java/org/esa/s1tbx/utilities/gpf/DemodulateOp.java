/*
 * Copyright (C) 2020 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.utilities.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * Demodulation/deramping of SLC data
 */
@OperatorMetadata(alias = "Demodulate",
        category = "Radar/SAR Utilities/Resampling",
        authors = "Carlos Hernandez, Esteban Aguilera, Reinier Oost, David A. Monge",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SENSAR",
        description = "Demodulation and deramping of SLC data")
public class DemodulateOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Band> complexTgtMap = new HashMap<>(10);
    private final Map<Band, Band> demodPhaseMap = new HashMap<>(10);

    private final Map<Band, int[]> offsetMap = new HashMap<>(10);
    private final Map<Band, Double> azimuthTimeIntervalMap = new HashMap<>(10);
    private final Map<Band, Double> azimuthTimeZdOffsetMap = new HashMap<>(10);
    private final Map<Band, double[]> dopplerCentroidArrayMap = new HashMap<>(10);
    private final Map<Band, double[]> dopplerRateArrayMap = new HashMap<>(10);

    private static final String PRODUCT_SUFFIX = "_Demod";
    private static final String DEMOD_PHASE_PREFIX = "DemodPhase";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public DemodulateOp() {

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
            createTargetProduct();
            getSlavesMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

    }

    /**
     * Create target product
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        // Define source and target bands
        Band sourceBandI = null;
        Band sourceBandQ = null;
        Band targetBandI;
        Band targetBandQ;

        // Master
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            if (bandName.contains("i_")) {
                sourceBandI = sourceProduct.getBand(bandName);
            } else {
                sourceBandQ = sourceProduct.getBand(bandName);
            }
        }
        targetBandI = ProductUtils.copyBand(sourceBandI.getName(), sourceProduct, targetProduct, false);
        targetBandI.setSourceImage(sourceBandI.getSourceImage());
        targetBandQ = ProductUtils.copyBand(sourceBandQ.getName(), sourceProduct, targetProduct, false);
        targetBandQ.setSourceImage(sourceBandQ.getSourceImage());

        // Slaves
        final String[] slaveProductNames = StackUtils.getSlaveProductNames(sourceProduct);
        for (String slaveProductName : slaveProductNames) {
            final String[] slvBandNames = StackUtils.getSlaveBandNames(sourceProduct, slaveProductName);
            for (String bandName : slvBandNames) {
                if (bandName.contains("i_")) {
                    sourceBandI = sourceProduct.getBand(bandName);
                } else {
                    sourceBandQ = sourceProduct.getBand(bandName);
                }
            }

            // Add slave bands to targetProduct
            targetBandI = targetProduct.addBand(sourceBandI.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandI, targetBandI);
            targetBandQ = targetProduct.addBand(sourceBandQ.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandQ, targetBandQ);

            // Add slave (demodulation phase) band to targetProduct
            final String demodBandName = DEMOD_PHASE_PREFIX + StackUtils.getBandSuffix(sourceBandQ.getName());
            Band targetDemodPhaseBand = targetProduct.addBand(demodBandName, ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandQ, targetDemodPhaseBand);
            targetDemodPhaseBand.setUnit(Unit.RADIANS);
            targetDemodPhaseBand.setDescription("Demodulation Phase");

            // Store source and target bands in HashMaps
            sourceRasterMap.put(targetBandI, sourceBandI); // (target I: source I) band pairs
            sourceRasterMap.put(targetBandQ, sourceBandQ); // (target Q: source Q) band pairs
            complexSrcMap.put(sourceBandI, sourceBandQ); // (source I: source Q) band pairs
            complexTgtMap.put(targetBandI, targetBandQ); // (target I: target Q) band pairs
            demodPhaseMap.put(targetBandI, targetDemodPhaseBand); // (target I: target demod phase band) pairs
        }
    }

    private void getSlavesMetadata() {

        Band sourceBandI = null;
        final String[] slaveProductNames = StackUtils.getSlaveProductNames(sourceProduct);
        for (String slaveProductName : slaveProductNames) { // for each slave
            final String[] slvBandNames = StackUtils.getSlaveBandNames(sourceProduct, slaveProductName);
            for (String bandName : slvBandNames) {
                if (bandName.contains("i_")) {
                    sourceBandI = sourceProduct.getBand(bandName);
                    break;
                }
            }

            // Get slave's metadata element
            final MetadataElement abs = AbstractMetadata.getSlaveMetadata(sourceProduct.getMetadataRoot())
                    .getElement(slaveProductName);

            // Validate acquisition mode
            final String imagingMode = abs.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
            if (!imagingMode.equalsIgnoreCase("spotlight")) {
                throw new OperatorException("Only spotlight mode supported");
            }

            // Load dopplerSpotlight element
            final MetadataElement dopplerSpotlight = abs.getElement("dopplerSpotlight");

            // Load azimuthTimeZdSpotlight element and attribute related to azimuth time
            final MetadataElement azimuthTimeZd = dopplerSpotlight.getElement("azimuthTimeZdSpotlight");
            final double azimuthTimeZdOffset = azimuthTimeZd.getAttributeDouble("AzimuthTimeZdOffset");

            // Load Doppler-related attributes
            final String[] dopplerCentroidSpotlight = dopplerSpotlight.getAttributeString("dopplerCentroidSpotlight").split(",");
            final String[] dopplerRateSpotlight = dopplerSpotlight.getAttributeString("dopplerRateSpotlight").split(",");
            final int sourceImageWidth = sourceBandI.getRasterWidth();
            final double[] dopplerCentroidArray = new double[sourceImageWidth];
            final double[] dopplerRateArray = new double[sourceImageWidth];
            for (int i = 0; i < sourceImageWidth; i++) {
                dopplerCentroidArray[i] = Double.parseDouble(dopplerCentroidSpotlight[i]);
                dopplerRateArray[i] = Double.parseDouble(dopplerRateSpotlight[i]);
            }

            // Store slave metadata in HashMaps
            azimuthTimeZdOffsetMap.put(sourceBandI, azimuthTimeZdOffset); // (source band I: azimuth time offset) pairs
            azimuthTimeIntervalMap.put(sourceBandI, abs.getAttributeDouble(AbstractMetadata.line_time_interval)); // (source band I: azimuth time interval) pairs
            dopplerCentroidArrayMap.put(sourceBandI, dopplerCentroidArray); // (source band I: doppler centroid array) pairs
            dopplerRateArrayMap.put(sourceBandI, dopplerRateArray); // (source band I: doppler rate array) pairs
            offsetMap.put(sourceBandI, getInitOffset(sourceBandI)); // (source band I: initial orbit-based offsets) pairs
        }
    }

    private int[] getInitOffset(final Band sourceBand) {

        final int[] offset = {0, 0};

        // Define orbit offsets name
        final String suffix = StackUtils.getBandSuffix(sourceBand.getName());
        final String offsetsName = "init_offsets" + suffix;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (absRoot.containsElement("Orbit_Offsets") &&
                absRoot.getElement("Orbit_Offsets").containsElement(offsetsName)) {
            offset[0] = absRoot.getElement("Orbit_Offsets").getElement(offsetsName).getAttributeInt("init_offset_X");
            offset[1] = absRoot.getElement("Orbit_Offsets").getElement(offsetsName).getAttributeInt("init_offset_Y");
        }

        return offset;
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        for (Band targetBandI : complexTgtMap.keySet()) { // for each slave

            // Get source and target bands
            final Band sourceBandI = sourceRasterMap.get(targetBandI);
            final Band sourceBandQ = complexSrcMap.get(sourceBandI);
            final Band targetBandQ = complexTgtMap.get(targetBandI);
            final Band targetDemodPhaseBand = demodPhaseMap.get(targetBandI);

            // Get source tiles
            final Tile sourceTileI = getSourceTile(sourceBandI, targetRectangle);
            final Tile sourceTileQ = getSourceTile(sourceBandQ, targetRectangle);

            // Get target tiles
            final Tile targetTileI = targetTileMap.get(targetBandI);
            final Tile targetTileQ = targetTileMap.get(targetBandQ);
            final Tile targetDemodPhaseTile = targetTileMap.get(targetDemodPhaseBand);

            // Calculate demodulation phase
            final double[][] demodPhase = computeDemodulationPhase(targetRectangle,
                                                                   azimuthTimeZdOffsetMap.get(sourceBandI),
                                                                   azimuthTimeIntervalMap.get(sourceBandI),
                                                                   dopplerCentroidArrayMap.get(sourceBandI),
                                                                   dopplerRateArrayMap.get(sourceBandI),
                                                                   offsetMap.get(sourceBandI));

            // Demodulate and write demodulation phase
            demodulate(sourceTileI, sourceTileQ, targetTileI, targetTileQ,
                       targetDemodPhaseTile, targetRectangle, demodPhase);
        }

    }

    private double[][] computeDemodulationPhase(final Rectangle rectangle, final double azimuthTimeZdOffset,
                                                final double azimuthTimeInterval, final double[] dopplerCentroidArray,
                                                final double[] dopplerRateArray, final int[] offset) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final double[][] phase = new double[h][w];

        for (int y = y0; y < yMax; y++) {
            final int line = y + offset[1];
            final double ta = azimuthTimeZdOffset + line * azimuthTimeInterval;
            for (int x = x0; x < xMax; x++) {
                final int pixel = Math.min(Math.max(0, x + offset[0]), dopplerCentroidArray.length - 1);
                phase[y - y0][x - x0] = -Constants.TWO_PI * dopplerCentroidArray[pixel] * ta;
                phase[y - y0][x - x0] += -Constants.PI * dopplerRateArray[pixel] * FastMath.pow(ta, 2);
            }
        }

        return phase;
    }

    private void demodulate(final Tile sourceTileI, final Tile sourceTileQ,
                            final Tile targetTileI, final Tile targetTileQ,
                            final Tile targetDemodPhaseTile, final Rectangle rectangle,
                            final double[][] demodPhase) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int xMax = x0 + rectangle.width;
        final int yMax = y0 + rectangle.height;

        final ProductData sourceDataI = sourceTileI.getDataBuffer();
        final ProductData sourceDataQ = sourceTileQ.getDataBuffer();
        final ProductData targetBufferI = targetTileI.getDataBuffer();
        final ProductData targetBufferQ = targetTileQ.getDataBuffer();
        final ProductData targetDemodPhaseBuffer = targetDemodPhaseTile.getDataBuffer();

        final TileIndex sourceIndex = new TileIndex(sourceTileI);
        final TileIndex targetIndex = new TileIndex(targetTileI);

        for (int y = y0; y < yMax; y++) {
            sourceIndex.calculateStride(y);
            targetIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; x++) {
                final int sourceIdx = sourceIndex.getIndex(x);
                final int targetIdx = targetIndex.getIndex(x);
                final int xx = x - x0;

                // Get value of real and imaginary bands
                final double valueI = sourceDataI.getElemDoubleAt(sourceIdx);
                final double valueQ = sourceDataQ.getElemDoubleAt(sourceIdx);

                // Compute cos and sin of demodulation phase
                final double cosPhase = FastMath.cos(demodPhase[yy][xx]);
                final double sinPhase = FastMath.sin(demodPhase[yy][xx]);

                // Calculate demodulated real and imaginary parts
                final double demodI = valueI * cosPhase - valueQ * sinPhase;
                final double demodQ = valueI * sinPhase + valueQ * cosPhase;

                // Write to product
                targetBufferI.setElemDoubleAt(targetIdx, demodI);
                targetBufferQ.setElemDoubleAt(targetIdx, demodQ);
                targetDemodPhaseBuffer.setElemDoubleAt(targetIdx, demodPhase[yy][xx]);
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

            super(DemodulateOp.class);
        }
    }
}
