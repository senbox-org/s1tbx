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
package org.esa.nest.gpf.orbits;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.orbits.*;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.datamodel.Orbits;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.gpf.OperatorUtils;
import org.jlinda.core.Ellipsoid;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;

import java.io.File;

/**
 * This operator applies orbit file to a given product.
 * <p/>
 * The following major processing steps are implemented:
 * <p/>
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
        category = "Utilities",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Apply orbit file")
public final class ApplyOrbitFileOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {SentinelPODOrbitFile.PRECISE, SentinelPODOrbitFile.RESTITUTED,
            DorisOrbitFile.DORIS_POR + " (ENVISAT)", DorisOrbitFile.DORIS_VOR + " (ENVISAT)",
            DelftOrbitFile.DELFT_PRECISE + " (ENVISAT, ERS1&2)", PrareOrbitFile.PRARE_PRECISE + " (ERS1&2)"},
            defaultValue = DorisOrbitFile.DORIS_VOR + " (ENVISAT)", label = "Orbit State Vectors")
    private String orbitType = null;

    @Parameter(label = "Polynomial Degree", defaultValue = "3")
    private int polyDegree = 3;

    private MetadataElement absRoot = null;
    private MetadataElement tgtAbsRoot = null;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private int targetTiePointGridHeight;
    private int targetTiePointGridWidth;

    private double firstLineUTC;
    private double lineTimeInterval;

    private TiePointGrid slantRangeTime = null;
    private TiePointGrid incidenceAngle = null;
    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;

    private String mission;

    private OrbitFile orbitProvider = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ApplyOrbitFileOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            getSourceMetadata();

            if (orbitType == null) {
                if (mission.equals("ENVISAT")) {
                    orbitType = DorisOrbitFile.DORIS_VOR;
                } else if (mission.equals("ERS1") || mission.equals("ERS2")) {
                    orbitType = PrareOrbitFile.PRARE_PRECISE;
                } else if (mission.startsWith("SENTINEL")) {
                    orbitType = SentinelPODOrbitFile.PRECISE;
                }
            }
            if(mission.equals("ENVISAT")) {
                if (!orbitType.startsWith(DelftOrbitFile.DELFT_PRECISE) && !orbitType.startsWith(DorisOrbitFile.DORIS_POR) &&
                        !orbitType.startsWith(DorisOrbitFile.DORIS_VOR)) {
                    //throw new OperatorException(orbitType + " is not suitable for an ENVISAT product");
                    orbitType = DorisOrbitFile.DORIS_VOR;
                }
            } else if(mission.startsWith("ERS")) {
                    if (!orbitType.startsWith(DelftOrbitFile.DELFT_PRECISE) && !orbitType.startsWith(PrareOrbitFile.PRARE_PRECISE)) {
                        //throw new OperatorException(orbitType + " is not suitable for an ERS1 product");
                        orbitType = SentinelPODOrbitFile.PRECISE;
                    }
            } else if(mission.startsWith("SENTINEL")) {
                if (!orbitType.startsWith("Sentinel")) {
                    //throw new OperatorException(orbitType + " is not suitable for an ERS1 product");
                    orbitType = PrareOrbitFile.PRARE_PRECISE;
                }
            } else {
                    throw new OperatorException(orbitType + " is not suitable for a " + mission + " product");
            }

            if (orbitType.contains("DORIS")) {
                orbitProvider = new DorisOrbitFile(orbitType, absRoot, sourceProduct);
            } else if (orbitType.contains("DELFT")) {
                orbitProvider = new DelftOrbitFile(orbitType, absRoot, sourceProduct);
            } else if (orbitType.contains("PRARE")) {
                orbitProvider = new PrareOrbitFile(orbitType, absRoot, sourceProduct);
            } else if (orbitType.contains("Sentinel")) {
                orbitProvider = new SentinelPODOrbitFile(orbitType, absRoot, sourceProduct, polyDegree);
            }

            getTiePointGrid();

            createTargetProduct();

            setTargetMetadata();

            updateOrbitStateVectors();

            updateTargetProductGEOCodingJLinda();

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
        slantRangeTime = OperatorUtils.getSlantRangeTime(sourceProduct);
        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);

        targetTiePointGridWidth = latitude.getRasterWidth();
        targetTiePointGridHeight = latitude.getRasterHeight();
    }

    /**
     * Get source metadata
     */
    private void getSourceMetadata() {
        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD();
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day
    }

    /**
     * Set target metadata
     */
    private void setTargetMetadata() {
        tgtAbsRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (Band srcBand : sourceProduct.getBands()) {
            if (srcBand instanceof VirtualBand) {
                OperatorUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
            } else {
                final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());
            }
        }
    }

    /**
     * Update target product GEOCoding. A new tie point grid is generated.
     *
     * @throws Exception The exceptions.
     */
    @Deprecated
    private void updateTargetProductGEOCoding() throws Exception {

        final float[] targetLatTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];
        final float[] targetLonTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];
        final float[] targetIncidenceAngleTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];
        final float[] targetSlantRangeTimeTiePoints = new float[targetTiePointGridHeight * targetTiePointGridWidth];

        final int subSamplingX = sourceImageWidth / (targetTiePointGridWidth - 1);
        final int subSamplingY = sourceImageHeight / (targetTiePointGridHeight - 1);

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

            final double curLineUTC = firstLineUTC + y * lineTimeInterval;
            //System.out.println((new ProductData.UTC(curLineUTC)).toString());

            // compute the satellite position and velocity for the zero Doppler time using cubic interpolation
            final Orbits.OrbitData data = orbitProvider.getOrbitData(curLineUTC);

            for (int c = 0; c < targetTiePointGridWidth; c++) {

                final int x = getSampleIndex(c, subSamplingX);
                targetIncidenceAngleTiePoints[k] = (float)incidenceAngle.getPixelDouble((float) x, (float) y);
                targetSlantRangeTimeTiePoints[k] = (float)slantRangeTime.getPixelDouble((float) x, (float) y);

                final double slrgTime = targetSlantRangeTimeTiePoints[k] / Constants.oneBillion; // ns to s;
                final GeoPos geoPos = computeLatLon(x, y, slrgTime, data);
                targetLatTiePoints[k] = (float)geoPos.lat;
                targetLonTiePoints[k] = (float)geoPos.lon;
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

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        for (TiePointGrid tpg : targetProduct.getTiePointGrids()) {
            targetProduct.removeTiePointGrid(tpg);
        }

        targetProduct.addTiePointGrid(angleGrid);
        targetProduct.addTiePointGrid(slrgtGrid);
        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.setGeoCoding(tpGeoCoding);
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
     * Compute accurate target geo position.
     *
     * @param x        The x coordinate of the given pixel.
     * @param y        The y coordinate of the given pixel.
     * @param slrgTime The slant range time of the given pixel.
     * @param data     The orbit data.
     * @return The geo position of the target.
     */
    @Deprecated
    private GeoPos computeLatLon(final int x, final int y, final double slrgTime, final Orbits.OrbitData data) {

        final double[] xyz = new double[3];
        final double lat = latitude.getPixelDouble((float)x, (float)y);
        final double lon = longitude.getPixelDouble((float)x, (float)y);
        final GeoPos geoPos = new GeoPos(lat, lon);

        // compute initial (x,y,z) coordinate from lat/lon
        GeoUtils.geo2xyz(geoPos, xyz);

        // compute accurate (x,y,z) coordinate using Newton's method
        GeoUtils.computeAccurateXYZ(data, xyz, slrgTime);

        // compute (lat, lon, alt) from accurate (x,y,z) coordinate
        final GeoPos newGeoPos = new GeoPos();
        GeoUtils.xyz2geo(xyz, newGeoPos);

        //System.out.println("prev: "+geoPos.toString() +" new: "+newGeoPos.toString());
        //System.out.println("prev lat: "+geoPos.getLat() +" new lat: "+newGeoPos.getLat());
        //System.out.println("prev lon: "+geoPos.getLon() +" new lon: "+newGeoPos.getLon());

        return newGeoPos;
    }

    /**
     * Update orbit state vectors using data from the orbit file.
     *
     * @throws Exception The exceptions.
     */
    private void updateOrbitStateVectors() throws Exception {

        // get original orbit state vectors
        final OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(tgtAbsRoot);

        // compute new orbit state vectors
        for (OrbitStateVector orbitStateVector : orbitStateVectors) {
            final double time = orbitStateVector.time_mjd;
            final Orbits.OrbitData orbitData = orbitProvider.getOrbitData(time);
            orbitStateVector.x_pos = orbitData.xPos; // m
            orbitStateVector.y_pos = orbitData.yPos; // m
            orbitStateVector.z_pos = orbitData.zPos; // m
            orbitStateVector.x_vel = orbitData.xVel; // m/s
            orbitStateVector.y_vel = orbitData.yVel; // m/s
            orbitStateVector.z_vel = orbitData.zVel; // m/s
        }

        // save new orbit state vectors
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

        // put NEST abstracted_metadata into jLinda metadata containers
        final SLCImage metaData = new SLCImage(tgtAbsRoot);
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

                targetIncidenceAngleTiePoints[k] = (float)(incAngle * Constants.RTOD);
                targetSlantRangeTimeTiePoints[k] = (float)slantRangeTime;
                targetLatTiePoints[k] = (float)lat;
                targetLonTiePoints[k] = (float)lon;
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

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        for (TiePointGrid tpg : targetProduct.getTiePointGrids()) {
            targetProduct.removeTiePointGrid(tpg);
        }

        targetProduct.addTiePointGrid(angleGrid);
        targetProduct.addTiePointGrid(slrgtGrid);
        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.setGeoCoding(tpGeoCoding);
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
            super(ApplyOrbitFileOp.class);
        }
    }
}