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
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.Guardian;
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
import java.util.List;
import java.util.ArrayList;

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

    //@Parameter(description = "Do not demodulate master image", label = "Exclude Master",
    //        defaultValue = "true")
    private boolean excludeMaster = true;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Band> complexTgtMap = new HashMap<>(10);
    private final Map<Band, Band> demodPhaseMap = new HashMap<>(10);

    private static final String PRODUCT_SUFFIX = "_Demod";
    private static final String DEMOD_PHASE_PREFIX = "DemodPhase";

    private String imagingMode;

    // Spotlight attributes
    private final Map<Band, Double> azimuthTimeOffsetMap = new HashMap<>(10);
    private final Map<Band, double[]> dopplerCentroidArrayMap = new HashMap<>(10);
    private final Map<Band, double[]> dopplerRateArrayMap = new HashMap<>(10);

    // Stripmap attributes
    private static final int MAX_NR_DOPPLER_COEFFICIENTS = 3;
    private final Map<Band, Double> rangeTimeOffsetMap = new HashMap<>(10);
    private final Map<Band, Double> rangeTimeIntervalMap = new HashMap<>(10);
    private final Map<Band, double[]> dopplerCoefficientsTimesMap = new HashMap<>(10);
    private final Map<Band, double[][]> dopplerCoefficientsMap = new HashMap<>(10);

    // Spotlight/Stripmap attributes
    private final Map<Band, int[]> offsetMap = new HashMap<>(10);
    private final Map<Band, Double> azimuthTimeIntervalMap = new HashMap<>(10);

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
            // Get acquisition mode
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            imagingMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

            // Create target product and load metadata
            createTargetProduct();
            getProductMetadata();
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

        // Define source bands
        Band sourceBandI = null;
        Band sourceBandQ = null;

        // Master
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            if (bandName.contains("i_")) {
                sourceBandI = sourceProduct.getBand(bandName);
            } else {
                sourceBandQ = sourceProduct.getBand(bandName);
            }
        }
        createTargetBands(sourceBandI, sourceBandQ, excludeMaster);

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
            createTargetBands(sourceBandI, sourceBandQ, false);
        }
    }

    private void createTargetBands(final Band sourceBandI, final Band sourceBandQ, boolean copy) {

        // Define target bands
        final Band targetBandI;
        final Band targetBandQ;

        if (copy) {
            targetBandI = ProductUtils.copyBand(sourceBandI.getName(), sourceProduct, targetProduct, false);
            targetBandI.setSourceImage(sourceBandI.getSourceImage());
            targetBandQ = ProductUtils.copyBand(sourceBandQ.getName(), sourceProduct, targetProduct, false);
            targetBandQ.setSourceImage(sourceBandQ.getSourceImage());
        } else {
            // Add bands to targetProduct
            targetBandI = targetProduct.addBand(sourceBandI.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandI, targetBandI);
            targetBandQ = targetProduct.addBand(sourceBandQ.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandQ, targetBandQ);

            // Add (demodulation phase) band to targetProduct
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

    private void getProductMetadata() {

        // Master
        if (!excludeMaster) {
            String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
            for (String bandName : masterBandNames) {
                if (bandName.contains("i_")) {
                    final Band sourceBandI = sourceProduct.getBand(bandName);
                    final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
                    getBandMetadata(sourceBandI, abs);
                    break;
                }
            }
        }

        // Slaves
        final String[] slaveProductNames = StackUtils.getSlaveProductNames(sourceProduct);
        for (String slaveProductName : slaveProductNames) { // for each slave
            final String[] slvBandNames = StackUtils.getSlaveBandNames(sourceProduct, slaveProductName);
            for (String bandName : slvBandNames) {
                if (bandName.contains("i_")) {
                    final Band sourceBandI = sourceProduct.getBand(bandName);
                    final MetadataElement abs = AbstractMetadata.getSlaveMetadata(sourceProduct.getMetadataRoot())
                            .getElement(slaveProductName);
                    getBandMetadata(sourceBandI, abs);
                    break;
                }
            }
        }
    }

    private void getBandMetadata(final Band sourceBandI, final MetadataElement abs) {

        // If Spotlight
        if (imagingMode.equalsIgnoreCase("Spotlight")) {
            // Load Doppler-related attributes
            final List<Double> dopplerCentroidArrayList = new ArrayList<>();
            final List<Double> dopplerRateArrayList = new ArrayList<>();
            final double azimuthTimeOffset = getDopplerMetadataSpotlight(abs, dopplerCentroidArrayList, dopplerRateArrayList);
            final double[] dopplerCentroidArray = dopplerCentroidArrayList.stream().mapToDouble(Double::doubleValue).toArray();
            final double[] dopplerRateArray = dopplerRateArrayList.stream().mapToDouble(Double::doubleValue).toArray();

            // Store metadata in HashMaps
            azimuthTimeOffsetMap.put(sourceBandI, azimuthTimeOffset); // (source band I: azimuth time offset) pairs
            dopplerCentroidArrayMap.put(sourceBandI, dopplerCentroidArray); // (source band I: doppler centroid array) pairs
            dopplerRateArrayMap.put(sourceBandI, dopplerRateArray); // (source band I: doppler rate array) pairs
        } else { // Assume Stripmap
            // Load Doppler-related attributes
            MetadataElement dopplerCoefficientsElem = abs.getElement(AbstractMetadata.dop_coefficients);
            int numberOfDopplerEstimates = dopplerCoefficientsElem.getNumElements();
            double[] dopplerCoefficientsTimes = new double[numberOfDopplerEstimates];
            double[][] dopplerCoefficients = new double[numberOfDopplerEstimates][MAX_NR_DOPPLER_COEFFICIENTS];
            final double rangeTimeOffset = getDopplerMetadataStripmap(abs, dopplerCoefficientsTimes, dopplerCoefficients);

            // Store metadata in HashMaps
            rangeTimeOffsetMap.put(sourceBandI, rangeTimeOffset); // (source band I: range time offset) pairs
            rangeTimeIntervalMap.put(sourceBandI, 1.0 / (abs.getAttributeDouble(AbstractMetadata.range_sampling_rate) * 1E6)); // (source band I: range time interval) pairs
            dopplerCoefficientsTimesMap.put(sourceBandI, dopplerCoefficientsTimes); // (source band I: doppler coefficients times) pairs
            dopplerCoefficientsMap.put(sourceBandI, dopplerCoefficients); // (source band I: doppler coefficients) pairs

        }
        // Store metadata in HashMaps
        offsetMap.put(sourceBandI, getInitOffset(sourceBandI)); // (source band I: initial orbit-based offsets) pairs
        azimuthTimeIntervalMap.put(sourceBandI, abs.getAttributeDouble(AbstractMetadata.line_time_interval)); // (source band I: azimuth time interval) pairs
    }

    private double getDopplerMetadataSpotlight(final MetadataElement abs,
                                               final List<Double> dopplerCentroidArrayList,
                                               final List<Double> dopplerRateArrayList) {

        // Load dopplerSpotlight element
        final MetadataElement dopplerSpotlight = abs.getElement("dopplerSpotlight");

        // Load Doppler-related attributes
        final String[] dopplerCentroidSpotlight = dopplerSpotlight.getAttributeString("dopplerCentroidSpotlight").split(",");
        final String[] dopplerRateSpotlight = dopplerSpotlight.getAttributeString("dopplerRateSpotlight").split(",");
        for (int i = 0; i < dopplerCentroidSpotlight.length; i++) {
            dopplerCentroidArrayList.add(Double.parseDouble(dopplerCentroidSpotlight[i]));
            dopplerRateArrayList.add(Double.parseDouble(dopplerRateSpotlight[i]));
        }

        // Load azimuthTimeZdSpotlight element and attribute related to azimuth time
        final MetadataElement azimuthTimeZd = dopplerSpotlight.getElement("azimuthTimeZdSpotlight");
        return azimuthTimeZd.getAttributeDouble("AzimuthTimeZdOffset");
    }

    private double getDopplerMetadataStripmap(final MetadataElement abs, final double[] dopplerCoefficientsTimes,
                                              final double[][] dopplerCoefficients) {

        MetadataElement dopplerCoefficientsElem = abs.getElement(AbstractMetadata.dop_coefficients);
        final double[] referenceRangeTimes = new double[dopplerCoefficientsTimes.length];

        for (int i = 0; i < dopplerCoefficients.length; i++) {
            // Get doppler coefficient element
            MetadataElement dopplerCoefficient = dopplerCoefficientsElem.getElement(AbstractMetadata.dop_coef_list + "." + (i + 1));

            // Get coefficients times
            ProductData.UTC zeroDopplerTime = dopplerCoefficient.getAttributeUTC(AbstractMetadata.dop_coef_time);
            ProductData.UTC firstLineTime = abs.getAttributeUTC(AbstractMetadata.first_line_time);
            dopplerCoefficientsTimes[i] = (zeroDopplerTime.getMJD() - firstLineTime.getMJD()) * 24.0 * 3600.0;
            referenceRangeTimes[i] = dopplerCoefficient.getAttributeDouble(AbstractMetadata.slant_range_time) * Constants.oneBillionth;

            // Get coefficients
            for (int j = 0; j < dopplerCoefficients[0].length; j++) {
                MetadataElement coefficient = dopplerCoefficient.getElement("coefficient." + (j + 1));
                dopplerCoefficients[i][j] = coefficient == null ? 0.0 : coefficient.getAttributeDouble(AbstractMetadata.dop_coef);
            }
        }

        final double rangeTimeToFirstPixel = abs.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel) / Constants.halfLightSpeed;

        Guardian.assertTrue("Reference range time is zero",
                            referenceRangeTimes[0] != 0.0);

        return rangeTimeToFirstPixel - referenceRangeTimes[0];
    }

    private int[] getInitOffset(final Band sourceBand) {

        final int[] offset = {0, 0};

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final MetadataElement orbitOffsets = absRoot.getElement("Orbit_Offsets");
        final String suffix = StackUtils.getBandSuffix(sourceBand.getName());
        final String offsetsName = "init_offsets" + suffix;
        if (orbitOffsets != null && orbitOffsets.containsElement(offsetsName)) {
            offset[0] = orbitOffsets.getElement(offsetsName).getAttributeInt("init_offset_X");
            offset[1] = orbitOffsets.getElement(offsetsName).getAttributeInt("init_offset_Y");
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

        for (Band targetBandI : complexTgtMap.keySet()) { // for each SLC

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
            final double[][] demodPhase;
            if (imagingMode.equalsIgnoreCase("Spotlight")) {
                demodPhase = computeDemodulationPhaseSpotlight(targetRectangle,
                                                               azimuthTimeOffsetMap.get(sourceBandI),
                                                               dopplerCentroidArrayMap.get(sourceBandI),
                                                               dopplerRateArrayMap.get(sourceBandI),
                                                               offsetMap.get(sourceBandI),
                                                               azimuthTimeIntervalMap.get(sourceBandI));
            } else { // Assume Stripmap
                demodPhase = computeDemodulationPhaseStripmap(targetRectangle,
                                                              rangeTimeOffsetMap.get(sourceBandI),
                                                              rangeTimeIntervalMap.get(sourceBandI),
                                                              dopplerCoefficientsTimesMap.get(sourceBandI),
                                                              dopplerCoefficientsMap.get(sourceBandI),
                                                              offsetMap.get(sourceBandI),
                                                              azimuthTimeIntervalMap.get(sourceBandI));
            }

            // Demodulate and write demodulation phase
            demodulate(sourceTileI, sourceTileQ, targetTileI, targetTileQ,
                       targetDemodPhaseTile, targetRectangle, demodPhase);
        }
    }

    private double[][] computeDemodulationPhaseSpotlight(final Rectangle rectangle,
                                                         final double azimuthTimeOffset,
                                                         final double[] dopplerCentroidArray,
                                                         final double[] dopplerRateArray,
                                                         final int[] offset,
                                                         final double azimuthTimeInterval) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final double[][] phase = new double[h][w];

        for (int y = y0; y < yMax; y++) {
            final int line = y + offset[1];
            final double ta = azimuthTimeOffset + line * azimuthTimeInterval;
            for (int x = x0; x < xMax; x++) {
                final int pixel = Math.min(Math.max(0, x + offset[0]), dopplerCentroidArray.length - 1);
                phase[y - y0][x - x0] = -Constants.TWO_PI * dopplerCentroidArray[pixel] * ta;
                phase[y - y0][x - x0] += -Constants.PI * dopplerRateArray[pixel] * FastMath.pow(ta, 2);
            }
        }

        return phase;
    }

    private double[][] computeDemodulationPhaseStripmap(final Rectangle rectangle,
                                                        final double rangeTimeOffset,
                                                        final double rangeTimeInterval,
                                                        final double[] dopplerCoefficientsTimes,
                                                        final double[][] dopplerCoefficients,
                                                        final int[] offset,
                                                        final double azimuthTimeInterval) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final double[][] phase = new double[h][w];

        final int midRangePixel =  Math.round(sourceProduct.getSceneRasterWidth() / 2) + offset[0];
        final double midRangeTime =  rangeTimeOffset + midRangePixel * rangeTimeInterval;

        for (int y = y0; y < yMax; y++) {
            final int line = y + offset[1];
            final double ta = line * azimuthTimeInterval;
            final double dopplerCentroid = computeDopplerCentroid(ta,
                                                                  midRangeTime,
                                                                  dopplerCoefficientsTimes,
                                                                  dopplerCoefficients);
            for (int x = x0; x < xMax; x++) {
                phase[y - y0][x - x0] = -Constants.TWO_PI * dopplerCentroid * ta;
            }
        }

        return phase;
    }

    private double computeDopplerCentroid(final double azimuthTime,
                                          final double rangeTime,
                                          final double[] dopplerCoefficientsTimes,
                                          final double[][] dopplerCoefficients) {

        // Get Doppler coefficients to use
        final double[] dopplerCoefficientsToUse = new double[MAX_NR_DOPPLER_COEFFICIENTS];
        final int numberOfDopplerEstimates = dopplerCoefficientsTimes.length;

        if (azimuthTime <= dopplerCoefficientsTimes[0]) {
            // Use coefficients corresponding to first estimate
            for (int j = 0; j < MAX_NR_DOPPLER_COEFFICIENTS; j++) {
                dopplerCoefficientsToUse[j] = dopplerCoefficients[0][j];
            }
        } else if (azimuthTime > dopplerCoefficientsTimes[numberOfDopplerEstimates - 1]) {
            // Use coefficients corresponding to last estimate
            for (int j = 0; j < MAX_NR_DOPPLER_COEFFICIENTS; j++) {
                dopplerCoefficientsToUse[j] = dopplerCoefficients[numberOfDopplerEstimates - 1][j];
            }
        } else {
            // Use interpolated coefficients
            for (int i = 0; i < numberOfDopplerEstimates - 1; i++) {
                if (azimuthTime > dopplerCoefficientsTimes[i]) {
                    for (int j = 0; j < MAX_NR_DOPPLER_COEFFICIENTS; j++) {
                        final double dopplerCoefficient1 = dopplerCoefficients[i][j];
                        final double dopplerCoefficient2 = dopplerCoefficients[i + 1][j];
                        final double slope = (dopplerCoefficient2 - dopplerCoefficient1)
                                / (dopplerCoefficientsTimes[i + 1] - dopplerCoefficientsTimes[i]);
                        dopplerCoefficientsToUse[j] = dopplerCoefficient1 + slope * (azimuthTime - dopplerCoefficientsTimes[i]);
                    }
                }
            }
        }

        // Evaluate Doppler polynomial using selected coefficients
        double dopplerCentroid = 0;
        for (int j = 0; j < MAX_NR_DOPPLER_COEFFICIENTS; j++) {
            dopplerCentroid += dopplerCoefficientsToUse[j] * Math.pow(rangeTime, j);
        }

        return dopplerCentroid;
    }

    private void demodulate(final Tile sourceTileI, final Tile sourceTileQ,
                            final Tile targetTileI, final Tile targetTileQ,
                            final Tile targetDemodPhaseTile, final Rectangle rectangle,
                            final double[][] demodPhase) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int xMax = x0 + rectangle.width;
        final int yMax = y0 + rectangle.height;

        final ProductData sourceBufferI = sourceTileI.getDataBuffer();
        final ProductData sourceBufferQ = sourceTileQ.getDataBuffer();
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
                final double valueI = sourceBufferI.getElemDoubleAt(sourceIdx);
                final double valueQ = sourceBufferQ.getElemDoubleAt(sourceIdx);

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
