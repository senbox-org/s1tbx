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
package org.esa.s1tbx.sar.gpf.orbits;

import org.esa.s1tbx.io.orbits.OrbitFile;
import org.esa.s1tbx.io.orbits.delft.DelftOrbitFile;
import org.esa.s1tbx.io.orbits.doris.DorisOrbitFile;
import org.esa.s1tbx.io.orbits.k5.K5OrbitFile;
import org.esa.s1tbx.io.orbits.prare.PrareOrbitFile;
import org.esa.s1tbx.io.orbits.sentinel1.SentinelPODOrbitFile;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.jlinda.core.Ellipsoid;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;

import java.io.File;

/**
 * This operator applies orbit file to a given product.
 * <p>
 * The following major processing steps are implemented:
 * <p>
 * 1. Get orbit file with valid time period and user specified orbit file type.
 * 2. Get the old tie point grid of the source image: latitude, longitude, slant range time and incidence angle.
 * 3. Repeat the following steps for each new tie point in the new tie point grid:
 * 1)  Get the range line index y for the tie point;
 * 2)  Get zero Doppler time t for the range line.
 * 3)  Compute satellite position and velocity for the zero Doppler time t using cubic interpolation. (dorisReader)
 * 4)  Get sample number x (index in the range line).
 * 5)  Get slant range time for pixel (x, y) from the old slant range time tie point grid.
 * 6)  Get incidence angle for pixel (x, y) from the old incidence angle tie point grid.
 * 7)  Get latitude for pixel (x, y) from the old latitude tie point grid.
 * 8)  Get longitude for pixel (x, y) from the old longitude tie point grid.
 * 9)  Convert (latitude, longitude, h = 0) to global Cartesian coordinate (x0, y0, z0).
 * 10) Solve Range equation, Doppler equation and Earth equation system for accurate (x, y, z) using Newton's
 * method with (x0, y0, z0) as initial point.
 * 11) Convert (x, y, z) back to (latitude, longitude, h).
 * 12) Save the new latitude and longitude for current tie point.
 * 4. Create new geocoding with the newly computed latitude and longitude tie points.
 * 5. Update orbit state vectors in the metadata:
 * 1) Get zero Doppler time for each orbit state vector in the metadata of the source image.
 * 2) Compute new orbit state vector for the zero Doppler time using cubic interpolation.
 * 3) Save the new orbit state vector in the target product.
 */

@OperatorMetadata(alias = "Apply-Orbit-File",
        category = "Radar",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Apply orbit file")
public final class ApplyOrbitFileOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {SentinelPODOrbitFile.PRECISE + " (Auto Download)", SentinelPODOrbitFile.RESTITUTED + " (Auto Download)",
            DorisOrbitFile.DORIS_POR + " (ENVISAT)", DorisOrbitFile.DORIS_VOR + " (ENVISAT)" + " (Auto Download)",
            DelftOrbitFile.DELFT_PRECISE + " (ENVISAT, ERS1&2)" + " (Auto Download)",
            PrareOrbitFile.PRARE_PRECISE + " (ERS1&2)" + " (Auto Download)",
            K5OrbitFile.PRECISE },
            defaultValue = SentinelPODOrbitFile.PRECISE + " (Auto Download)", label = "Orbit State Vectors")
    private String orbitType = null;

    @Parameter(label = "Polynomial Degree", defaultValue = "3")
    private int polyDegree = 3;

    @Parameter(label = "Do not fail if new orbit file is not found", defaultValue = "false")
    private Boolean continueOnFail = false;

    private MetadataElement absRoot = null;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private int targetTiePointGridHeight;
    private int targetTiePointGridWidth;

    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;

    private String mission;
    private boolean isSLC;

    private OrbitFile orbitProvider = null;

    private boolean productUpdated = false;

    private static final String PRODUCT_SUFFIX = "_Orb";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ApplyOrbitFileOp() {
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
            isSLC = validator.isComplex();

            getSourceMetadata();

            if (orbitType == null) {
                if (mission.equals("ENVISAT")) {
                    orbitType = DorisOrbitFile.DORIS_VOR;
                } else if (mission.equals("ERS1") || mission.equals("ERS2")) {
                    orbitType = PrareOrbitFile.PRARE_PRECISE;
                } else if (mission.startsWith("SENTINEL")) {
                    orbitType = SentinelPODOrbitFile.PRECISE;
                } else if (mission.startsWith("Kompsat5")) {
                    orbitType = K5OrbitFile.PRECISE;
                } else {
                    throw new OperatorException("Please select an orbit file type");
                }
            }
            if (mission.equals("ENVISAT")) {
                if (!orbitType.startsWith(DelftOrbitFile.DELFT_PRECISE) && !orbitType.startsWith(DorisOrbitFile.DORIS_POR) &&
                        !orbitType.startsWith(DorisOrbitFile.DORIS_VOR)) {
                    orbitType = DorisOrbitFile.DORIS_VOR;
                }
            } else if (mission.startsWith("ERS")) {
                if (!orbitType.startsWith(DelftOrbitFile.DELFT_PRECISE) && !orbitType.startsWith(PrareOrbitFile.PRARE_PRECISE)) {
                    orbitType = PrareOrbitFile.PRARE_PRECISE;
                }
            } else if (mission.startsWith("SENTINEL")) {
                if (!orbitType.startsWith("Sentinel")) {
                    orbitType = SentinelPODOrbitFile.PRECISE;
                }
            } else if (mission.startsWith("Kompsat5")) {
                if (!orbitType.startsWith(K5OrbitFile.PRECISE)) {
                    orbitType = K5OrbitFile.PRECISE;
                }
            } else {
                throw new OperatorException(orbitType + " is not suitable for a " + mission + " product");
            }

            if (orbitType.contains("DORIS")) {
                orbitProvider = new DorisOrbitFile(absRoot, sourceProduct);
            } else if (orbitType.contains("DELFT")) {
                orbitProvider = new DelftOrbitFile(absRoot, sourceProduct);
            } else if (orbitType.contains("PRARE")) {
                orbitProvider = new PrareOrbitFile(absRoot, sourceProduct);
            } else if (orbitType.contains("Sentinel")) {
                orbitProvider = new SentinelPODOrbitFile(absRoot, polyDegree);
            } else if (orbitType.contains("Kompsat5")) {
                orbitProvider = new K5OrbitFile(absRoot, polyDegree);
            }

            getTiePointGrid();

            createTargetProduct();

            if (!productUpdated) {
                try {
                    updateOrbits();
                } catch (Exception e) {
                    if (continueOnFail != null && continueOnFail) {
                        SystemUtils.LOG.warning("ApplyOrbit ignoring error and continuing: " + e.toString());
                        productUpdated = true;
                    } else {
                        throw e;
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source product tie point grids for latitude, longitude, incidence angle and slant range time.
     */
    private void getTiePointGrid() {

        latitude = OperatorUtils.getLatitude(sourceProduct);
        longitude = OperatorUtils.getLongitude(sourceProduct);

        targetTiePointGridWidth = latitude.getGridWidth();
        targetTiePointGridHeight = latitude.getGridHeight();
    }

    /**
     * Get source metadata
     */
    private void getSourceMetadata() {
        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (Band srcBand : sourceProduct.getBands()) {
            if (srcBand instanceof VirtualBand) {
                ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
            } else {
                ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
            }
        }
    }

    private synchronized void updateOrbits() throws Exception {
        if (productUpdated)
            return;

        try {
            orbitProvider.retrieveOrbitFile(orbitType);
        } catch (Exception e) {
            SystemUtils.LOG.warning(e.getMessage());
            // try other orbit file types
            boolean tryAnotherType = false;
            for(String type : orbitProvider.getAvailableOrbitTypes()) {
                if(!orbitType.startsWith(type)) {
                    try {
                        orbitProvider.retrieveOrbitFile(type);
                    } catch (Exception e2) {
                        throw e;
                    }
                    SystemUtils.LOG.warning("Using "+type+' '+orbitProvider.getOrbitFile()+" instead");
                    tryAnotherType = true;
                }
            }
            if(!tryAnotherType) {
                throw e;
            }
        }

        updateOrbitStateVectors();

        if (isSLC && (orbitProvider instanceof PrareOrbitFile || orbitProvider instanceof DorisOrbitFile ||
                orbitProvider instanceof DelftOrbitFile)) {
            updateTargetProductGEOCodingJLinda();
        }

        productUpdated = true;
    }

    /**
     * Get corresponding sample index for a given column index in the new tie point grid.
     *
     * @param colIdx       The column index in the new tie point grid.
     * @param subSamplingX the x sub sampling
     * @return The sample index.
     */
    private int getSampleIndex(final int colIdx, final int subSamplingX) {

        if (colIdx == targetTiePointGridWidth - 1) { // last column
            return sourceImageWidth - 1;
        } else { // other columns
            return colIdx * subSamplingX;
        }
    }

    /**
     * Update orbit state vectors using data from the orbit file.
     *
     * @throws Exception The exceptions.
     */
    private void updateOrbitStateVectors() throws Exception {

        final double firstLineUTC = AbstractMetadata.parseUTC(
                absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        final double lastLineUTC = AbstractMetadata.parseUTC(
                absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days

        final double delta = 1.0 / Constants.secondsInDay ; // time interval = 1s

        final int numExtraVectors = 10; // # of vectors before and after acquisition period

        final double firstVectorTime = firstLineUTC - numExtraVectors*delta; // in days

        final double lastVectorTime = lastLineUTC + numExtraVectors*delta; // in days

        final int numVectors = (int)((lastVectorTime - firstVectorTime) / delta);

        OrbitStateVector[] orbitStateVectors = new OrbitStateVector[numVectors];

        // compute new orbit state vectors
        for (int i = 0; i < numVectors; ++i) {
            final double time = firstVectorTime + i*delta;
            final Orbits.OrbitVector orbitData = orbitProvider.getOrbitData(time);
            final ProductData.UTC utc = new ProductData.UTC(time);

            orbitStateVectors[i] = new OrbitStateVector(utc, orbitData.xPos, orbitData.yPos, orbitData.zPos,
                    orbitData.xVel, orbitData.yVel, orbitData.zVel);
        }

        // save new orbit state vectors
        final MetadataElement tgtAbsRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setOrbitStateVectors(tgtAbsRoot, orbitStateVectors);

        // save orbit file name
        String orbType = orbitType;
        if (orbType.contains("("))
            orbType = orbitType.substring(0, orbitType.indexOf('('));
        final File orbitFile = orbitProvider.getOrbitFile();
        tgtAbsRoot.setAttributeString(AbstractMetadata.orbit_state_vector_file, orbType + ' ' + orbitFile.getName());
    }

    /**
     * Update target product GEOCoding using jLinda core classes. A new tie point grid for latitude, longitude, slant range
     * time and incidence angle is generated.
     *
     * @throws Exception The exceptions.
     */
    private void updateTargetProductGEOCodingJLinda() throws Exception {

        final float[] targetLatTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];
        final float[] targetLonTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];
        final float[] targetIncidenceAngleTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];
        final float[] targetSlantRangeTimeTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];

        final int subSamplingX = sourceImageWidth / (targetTiePointGridWidth - 1);
        final int subSamplingY = sourceImageHeight / (targetTiePointGridHeight - 1);

        final MetadataElement tgtAbsRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);

        // put abstracted_metadata into jLinda metadata containers
        final SLCImage metaData = new SLCImage(tgtAbsRoot, targetProduct);
        final Orbit orbit = new Orbit(tgtAbsRoot, polyDegree); // New Orbits - assumed metadata updated!
        final Orbit oldOrbit = new Orbit(absRoot, polyDegree); // Old Orbits

        // Create new tie point grid
        int k = 0;
        for (int r = 0; r < targetTiePointGridHeight; r++) {

            // get the zero Doppler time for the rth line
            int y;
            if (r == targetTiePointGridHeight - 1) { // last row
                y = sourceImageHeight - 1;
            } else { // other rows
                y = r * subSamplingY;
            }

            for (int c = 0; c < targetTiePointGridWidth; c++) {

                // Note: pixel for the tie-point-grid defined by (range = x, azimuth =y)
                final int x = getSampleIndex(c, subSamplingX);

                // get reference point - works with geo annotations
                final double refLat = latitude.getPixelDouble((float) x, (float) y);
                final double refLon = longitude.getPixelDouble((float) x, (float) y);

                final Point refSarPoint = oldOrbit.ell2lp(new double[]{refLat * Constants.DTOR, refLon * Constants.DTOR, 0}, metaData);
                final double[] refGeoPoint = orbit.lp2ell(refSarPoint, metaData);
                final Point refXyzPoint = Ellipsoid.ell2xyz(refGeoPoint);

                final int line = (int) refSarPoint.y;
                final int pixel = (int) refSarPoint.x;

                final double curLineTime = metaData.line2ta(line); // work in satellite time

                final Point refPointOrbit = orbit.getXYZ(curLineTime);
                final Point rangeDist = refPointOrbit.min(refXyzPoint);
                final double incAngle = refXyzPoint.angle(rangeDist);
                final double slantRangeTime = metaData.pix2tr(pixel) * 2 * Constants.oneBillion;

                final double lat = refGeoPoint[0] * Constants.RTOD;
                final double lon = refGeoPoint[1] * Constants.RTOD;

                targetIncidenceAngleTiePoints[k] = (float) (incAngle * Constants.RTOD);
                targetSlantRangeTimeTiePoints[k] = (float) slantRangeTime;
                targetLatTiePoints[k] = (float) lat;
                targetLonTiePoints[k] = (float) lon;
                k++;
            }
        }

        final TiePointGrid angleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, targetTiePointGridWidth, targetTiePointGridHeight,
                                                        0.0f, 0.0f, subSamplingX, subSamplingY, targetIncidenceAngleTiePoints);
        angleGrid.setUnit(Unit.DEGREES);

        final TiePointGrid slrgtGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, targetTiePointGridWidth, targetTiePointGridHeight,
                                                        0.0f, 0.0f, subSamplingX, subSamplingY, targetSlantRangeTimeTiePoints);
        slrgtGrid.setUnit(Unit.NANOSECONDS);

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, targetTiePointGridWidth, targetTiePointGridHeight,
                                                      0.0f, 0.0f, subSamplingX, subSamplingY, targetLatTiePoints);
        latGrid.setUnit(Unit.DEGREES);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, targetTiePointGridWidth, targetTiePointGridHeight,
                                                      0.0f, 0.0f, subSamplingX, subSamplingY, targetLonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        for (TiePointGrid tpg : targetProduct.getTiePointGrids()) {
            targetProduct.removeTiePointGrid(tpg);
        }

        targetProduct.addTiePointGrid(angleGrid);
        targetProduct.addTiePointGrid(slrgtGrid);
        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.setSceneGeoCoding(tpGeoCoding);
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
            super(ApplyOrbitFileOp.class);
        }
    }
}
