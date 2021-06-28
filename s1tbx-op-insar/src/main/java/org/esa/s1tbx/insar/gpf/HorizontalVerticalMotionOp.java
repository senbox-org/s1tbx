/*
 * Copyright (C) 2021 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.insar.gpf;

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
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Computation of Horizontal/Vertical Motion Components
 */
@OperatorMetadata(alias = "HorizontalVerticalMotion",
        category = "Radar/Interferometric/Products",
        authors = "Esteban Aguilera, Carlos Hernandez",
        version = "1.0",
        copyright = "Copyright (C) 2021 by SENSAR",
        description = "Computation of Horizontal/Vertical Motion Components")
public class HorizontalVerticalMotionOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "X position for reference pixel",
            defaultValue = "0",
            label = "Reference pixel X position")
    private int refPixelX = 0;

    @Parameter(description = "Y position for reference pixel",
            defaultValue = "0",
            label = "Reference pixel Y position")
    private int refPixelY = 0;

    // Target bands
    private Band targetBandHorizontalMotion;
    private Band targetBandVerticalMotion;

    // Source bands and values to be loaded on initialization
    private Band unwrappedPhaseDsc;
    private Band unwrappedPhaseAsc;
    private Band incidenceAngleDsc;
    private Band incidenceAngleAsc;
    private double wavelength;
    private double headingDsc;
    private double headingAsc;

    // Values to be loaded during execution
    private double refOffsetDsc;
    private double refOffsetAsc;
    private boolean areRefOffsetsAvailable = false;

    // Constants
    private static final String PRODUCT_SUFFIX = "_hvm";
    private static final String HORIZONTAL_MOTION_BAND_NAME = "horizontalMotion";
    private static final String VERTICAL_MOTION_BAND_NAME = "verticalMotion";
    private static final String INCIDENCE_ANGLE_BAND_NAME_PREFIX = "incidenceAngleFromEllipsoid";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public HorizontalVerticalMotionOp() {

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

            // Load metadata
            getProductMetadata();

            // Get source bands
            getSourceBands();

            // Create target product
            createTargetProduct();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getProductMetadata() {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        wavelength = Constants.lightSpeed / abs.getAttributeDouble(AbstractMetadata.radar_frequency) / 1E6;

        final MetadataElement slavesHeadingAnglesElem = abs.getElement("Slaves_Heading_Angles");
        if (slavesHeadingAnglesElem == null) {
            throw new OperatorException("Heading angles are missing for collocated slaves.");
        }
        if (abs.getAttributeString("PASS").equals("DESCENDING")) { // if collocation master is DSC
            headingDsc = abs.getAttributeDouble("centre_heading");
            headingAsc = slavesHeadingAnglesElem.getAttributeDouble("centre_heading_0");
        } else {
            headingAsc = abs.getAttributeDouble("centre_heading");
            headingDsc = slavesHeadingAnglesElem.getAttributeDouble("centre_heading_0");
        }
    }

    private void getSourceBands() {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        for (String bandName : sourceProduct.getBandNames()) {

            if (abs.getAttributeString("PASS").equals("DESCENDING")) { // if collocation master was DSC
                if (bandName.contains("Phase_ifg")) {
                    if (bandName.endsWith("_M")) {
                        unwrappedPhaseDsc = sourceProduct.getBand(bandName);
                    } else {
                        unwrappedPhaseAsc = sourceProduct.getBand(bandName);
                    }
                } else if (bandName.startsWith(INCIDENCE_ANGLE_BAND_NAME_PREFIX)) {
                    if (bandName.endsWith("_M")) {
                        incidenceAngleDsc = sourceProduct.getBand(bandName);
                    } else {
                        incidenceAngleAsc = sourceProduct.getBand(bandName);
                    }
                }
            } else { // if collocation master was actually ASC
                if (bandName.contains("Phase_ifg")) {
                    if (bandName.endsWith("_M")) {
                        unwrappedPhaseAsc = sourceProduct.getBand(bandName);
                    } else {
                        unwrappedPhaseDsc = sourceProduct.getBand(bandName);
                    }
                } else if (bandName.startsWith(INCIDENCE_ANGLE_BAND_NAME_PREFIX)) {
                    if (bandName.endsWith("_M")) {
                        incidenceAngleAsc = sourceProduct.getBand(bandName);
                    } else {
                        incidenceAngleDsc = sourceProduct.getBand(bandName);
                    }
                }
            }
        }

        if (unwrappedPhaseDsc == null || unwrappedPhaseAsc == null) {
            throw new OperatorException("Phase bands are missing in input product.");
        }

        if (incidenceAngleDsc == null || incidenceAngleAsc == null) {
            throw new OperatorException(String.format("Incidence angle bands with prefix %s are missing in input product.",
                                                      INCIDENCE_ANGLE_BAND_NAME_PREFIX));
        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        // Band for horizontal motion
        targetBandHorizontalMotion = targetProduct.addBand(HORIZONTAL_MOTION_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetBandHorizontalMotion.setNoDataValue(Double.NaN);
        targetBandHorizontalMotion.setNoDataValueUsed(true);
        targetBandHorizontalMotion.setUnit("millimeters");
        targetBandHorizontalMotion.setDescription("Horizontal motion");

        // Band for vertical motion
        targetBandVerticalMotion = targetProduct.addBand(VERTICAL_MOTION_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetBandVerticalMotion.setNoDataValue(Double.NaN);
        targetBandVerticalMotion.setNoDataValueUsed(true);
        targetBandVerticalMotion.setUnit("millimeters");
        targetBandVerticalMotion.setDescription("Vertical motion");
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
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        // Get reference offsets
        if (!areRefOffsetsAvailable) {
            getReferenceOffsets();
        }

        // Get tiles
        final Tile unwrappedPhaseTileDsc = getSourceTile(unwrappedPhaseDsc, targetRectangle);
        final Tile unwrappedPhaseTileAsc = getSourceTile(unwrappedPhaseAsc, targetRectangle);
        final Tile incidenceAngleTileDsc = getSourceTile(incidenceAngleDsc, targetRectangle);
        final Tile incidenceAngleTileAsc = getSourceTile(incidenceAngleAsc, targetRectangle);

        final Tile horizontalMotionTile = targetTileMap.get(targetBandHorizontalMotion);
        final Tile verticalMotionTile = targetTileMap.get(targetBandVerticalMotion);

        // Compute motion components
        computeHorizontalVerticalMotion(wavelength, headingDsc, headingAsc,
                                        refOffsetDsc, refOffsetAsc,
                                        unwrappedPhaseTileDsc, unwrappedPhaseTileAsc,
                                        incidenceAngleTileDsc, incidenceAngleTileAsc,
                                        horizontalMotionTile, verticalMotionTile,
                                        targetRectangle);
    }

    private synchronized void getReferenceOffsets() {

        if (areRefOffsetsAvailable) {
            return;
        }

        final Rectangle rectangle = new Rectangle(refPixelX - 5, refPixelY - 5, 10, 10);
        final Tile unwrappedPhaseTileDsc = getSourceTile(unwrappedPhaseDsc, rectangle);
        final Tile unwrappedPhaseTileAsc = getSourceTile(unwrappedPhaseAsc, rectangle);
        refOffsetDsc = unwrappedPhaseTileDsc.getSampleDouble(refPixelX, refPixelY);
        refOffsetAsc = unwrappedPhaseTileAsc.getSampleDouble(refPixelX, refPixelY);

        areRefOffsetsAvailable = true;
    }

    private void computeHorizontalVerticalMotion(final double wavelength, final double headingDsc, final double headingAsc,
                                                 final double refOffsetDsc, final double refOffsetAsc,
                                                 final Tile unwrappedPhaseTileDsc, final Tile unwrappedPhaseTileAsc,
                                                 final Tile incidenceAngleTileDsc, final Tile incidenceAngleTileAsc,
                                                 final Tile horizontalMotionTile, final Tile verticalMotionTile,
                                                 final Rectangle rectangle) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int xMax = x0 + rectangle.width;
        final int yMax = y0 + rectangle.height;

        final ProductData sourceBufferUnwrappedPhaseDsc = unwrappedPhaseTileDsc.getDataBuffer();
        final ProductData sourceBufferUnwrappedPhaseAsc = unwrappedPhaseTileAsc.getDataBuffer();
        final ProductData sourceBufferIncidenceAngleDsc = incidenceAngleTileDsc.getDataBuffer();
        final ProductData sourceBufferIncidenceAngleAsc = incidenceAngleTileAsc.getDataBuffer();

        final ProductData targetBufferHorizontalMotion = horizontalMotionTile.getDataBuffer();
        final ProductData targetBufferVerticalMotion = verticalMotionTile.getDataBuffer();

        final TileIndex sourceIndex = new TileIndex(unwrappedPhaseTileDsc);
        final TileIndex targetIndex = new TileIndex(verticalMotionTile);

        for (int y = y0; y < yMax; y++) {
            sourceIndex.calculateStride(y);
            targetIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int sourceIdx = sourceIndex.getIndex(x);
                final int targetIdx = targetIndex.getIndex(x);

                // Get values
                final double valueUnwPhaseDsc = sourceBufferUnwrappedPhaseDsc.getElemDoubleAt(sourceIdx);
                final double valueUnwPhaseAsc = sourceBufferUnwrappedPhaseAsc.getElemDoubleAt(sourceIdx);
                final double valueIncAngleDsc = sourceBufferIncidenceAngleDsc.getElemDoubleAt(sourceIdx);
                final double valueIncAngleAsc = sourceBufferIncidenceAngleAsc.getElemDoubleAt(sourceIdx);

                // Compute LOS defo
                final double defoDsc = (valueUnwPhaseDsc - refOffsetDsc) * wavelength / 4 / Constants.PI * 1E3;
                final double defoAsc = (valueUnwPhaseAsc - refOffsetAsc) * wavelength / 4 / Constants.PI * 1E3;

                // Perform inversion
                final double a1 = -FastMath.cos(FastMath.toRadians(headingDsc)) * FastMath.sin(FastMath.toRadians(valueIncAngleDsc));
                final double a2 = -FastMath.cos(FastMath.toRadians(headingAsc)) * FastMath.sin(FastMath.toRadians(valueIncAngleAsc));
                final double b1 = FastMath.cos(FastMath.toRadians(valueIncAngleDsc));
                final double b2 = FastMath.cos(FastMath.toRadians(valueIncAngleAsc));

                final double[] solution = invertSystem(a1, a2, b1, b2, defoDsc, defoAsc);
                final double horizontalMotion = solution[0];
                final double verticalMotion = solution[1];

                // Write to product
                targetBufferHorizontalMotion.setElemDoubleAt(targetIdx, horizontalMotion);
                targetBufferVerticalMotion.setElemDoubleAt(targetIdx, verticalMotion);
            }
        }
    }

    /**
     * Solves for y1 and y2, given the following system of equations:
     * x1 = a1 * y1 + b1 * y2
     * x2 = a2 * y1 + b2 * y2
     */
    private static double[] invertSystem(final double a1, final double a2,
                                         final double b1, final double b2,
                                         final double x1, final double x2) {

        final double denominator = a1 * b2 - b1 * a2;
        final double y1 = (x1 * b2 - b1 * x2) / denominator;
        final double y2 = (a1 * x2 - x1 * a2) / denominator;
        return new double[]{y1, y2};
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

            super(HorizontalVerticalMotionOp.class);
        }
    }
}
