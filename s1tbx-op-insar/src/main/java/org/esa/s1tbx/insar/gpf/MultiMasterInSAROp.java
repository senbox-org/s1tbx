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
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;

import javax.media.jai.BorderExtender;
import java.awt.Rectangle;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Multi-master InSAR processing
 */
@OperatorMetadata(alias = "MultiMasterInSAR",
        category = "Radar/Interferometric/Products",
        authors = "Carlos Hernandez, Esteban Aguilera",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SENSAR",
        description = "Multi-master InSAR processing")
public class MultiMasterInSAROp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"1", "2", "3", "4", "5"},
            description = "Degree of orbit (polynomial) interpolator",
            defaultValue = "4",
            label = "Orbit interpolation degree")
    private int orbitDegree = 4;

    @Parameter(description = "List of interferometric pairs", label = "InSAR pairs")
    private String[] pairs; // {"ddMMMyyyy-ddMMMyyyy", "ddMMMyyyy-ddMMMyyyy", ...,"ddMMMyyyy-ddMMMyyyy"}
    private String[][] parsedPairs; // {{"ddMMMyyyy", "ddMMMyyyy"}, {"ddMMMyyyy", "ddMMMyyyy"}, ...,{"ddMMMyyyy", "ddMMMyyyy"}}

    @Parameter(defaultValue = "true", label = "Include wavenumber")
    private boolean includeWavenumber = true;

    @Parameter(defaultValue = "true", label = "Include incidence angle")
    private boolean includeIncidenceAngle = true;

    @Parameter(defaultValue = "true", label = "Include latitude and longitude")
    private boolean includeLatLon = true;

    @Parameter(description = "Size of coherence estimation window in azimuth",
            defaultValue = "10",
            label = "Coherence azimuth window size")
    private int cohWindowAz = 10;

    @Parameter(description = "Size of coherence estimation window in range",
            defaultValue = "10",
            label = "Coherence range window size")
    private int cohWindowRg = 10;

    // Metadata maps
    private SLCImage slcImageMaster;
    private Orbit orbitMaster;
    private final Map<Band, SLCImage> slcImageSlaveMap = new HashMap<>(10);
    private final Map<Band, Orbit> orbitSlaveMap = new HashMap<>(10);
    private final Map<String, Band> dateMap = new HashMap<>(10);

    // These apply per SLC
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Band> wavenumberMap = new HashMap<>(10); // the master is excluded

    // These apply to the whole stack
    private Band sourceBandElevation;
    private Band targetBandIncidenceAngle;
    private Band targetBandLatitude;
    private Band targetBandLongitude;

    // These apply per interferometric pair
    private Map<Band, List<Band>> interferogramPairMap = new HashMap<>(10);
    private Map<Band, List<Band>> interferogramMap = new HashMap<>(10);
    private Map<Band, Band> complexInterferogramMap = new HashMap<>(10);
    private Map<Band, List<Band>> coherenceMap = new HashMap<>(10);

    // Constants
    private static final String WAVENUMBER_BAND_NAME_PREFIX = "wavenumber";

    private static final String DEM_BAND_NAME_PREFIX = "elevation";
    private static final String INCIDENCE_ANGLE_BAND_NAME = "incidenceAngle";
    private static final String LAT_BAND_NAME = "lat";
    private static final String LON_BAND_NAME = "lon";

    private static final String IFG_BAND_NAME_TAG = "ifg";
    private static final String COHERENCE_BAND_NAME_PREFIX = "coh";

    private static final String PRODUCT_SUFFIX = "_mmifg";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public MultiMasterInSAROp() {

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

            // Parse list of pairs
            List<String> datesInPairsList = getPairs();

            // Create target product
            createTargetProduct(datesInPairsList);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product
     */
    private void createTargetProduct(final List<String> datesInPairsList) {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        // Define source bands
        Band sourceBandI = null;
        Band sourceBandQ = null;

        // Get source I/Q bands (master); skip if not in any pair
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            if (bandName.contains("i_")) {
                sourceBandI = sourceProduct.getBand(bandName);
            } else {
                sourceBandQ = sourceProduct.getBand(bandName);
            }
        }
        for (String date : datesInPairsList) {
            final Band sourceBandToCheck = dateMap.get(date);
            if (sourceBandI == sourceBandToCheck) {
                complexSrcMap.put(sourceBandI, sourceBandQ); // (source I: source Q) band pairs
                datesInPairsList.remove(date);
                break;
            }
        }

        // Get source I/Q bands (slaves) and create wavenumber band; skip if not in any pair
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
            for (String date : datesInPairsList) {
                final Band sourceBandToCheck = dateMap.get(date);
                if (sourceBandI == sourceBandToCheck) {
                    complexSrcMap.put(sourceBandI, sourceBandQ); // (source I: source Q) band pairs
                    if (includeWavenumber) {
                        // Add wavenumber band to targetProduct
                        final String wavenumberBandName = WAVENUMBER_BAND_NAME_PREFIX + StackUtils.getBandSuffix(sourceBandI.getName());
                        final Band targetBandWavenumber = targetProduct.addBand(wavenumberBandName, ProductData.TYPE_FLOAT32);
                        targetBandWavenumber.setUnit("radians/meter");
                        targetBandWavenumber.setDescription("Vertical wavenumber");

                        // Store in a HashMap
                        wavenumberMap.put(sourceBandI, targetBandWavenumber); // (source I: wavenumber) band pairs
                    }
                    datesInPairsList.remove(date);
                    break;
                }
            }
        }

        // Check if there are invalid pairs (i.e., pairs whose dates are not found in the source product)
        if (datesInPairsList.size() > 0) {
            throw new OperatorException("Check pairs. Invalid dates found: " + datesInPairsList.toString());
        }

        // Create band for DEM
        String elevationBandName = null;
        for (String bandName : sourceProduct.getBandNames()) {
            if (bandName.startsWith(DEM_BAND_NAME_PREFIX)) {
                ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
                elevationBandName = bandName;
                sourceBandElevation = sourceProduct.getBand(bandName);
            }
        }
        if (elevationBandName == null) {
            throw new OperatorException("Elevation band is missing in input product.");
        }

        // Create band for incidence angle
        if (includeIncidenceAngle) {
            targetBandIncidenceAngle = targetProduct.addBand(INCIDENCE_ANGLE_BAND_NAME, ProductData.TYPE_FLOAT32);
            targetBandIncidenceAngle.setNoDataValue(Double.NaN);
            targetBandIncidenceAngle.setNoDataValueUsed(true);
            targetBandIncidenceAngle.setUnit(Unit.DEGREES);
            targetBandIncidenceAngle.setDescription("Incidence angle");
        }

        // Create bands for latitude and longitude
        if (includeLatLon) {
            targetBandLatitude = targetProduct.addBand(LAT_BAND_NAME, ProductData.TYPE_FLOAT32);
            targetBandLatitude.setNoDataValue(Double.NaN);
            targetBandLatitude.setNoDataValueUsed(true);
            targetBandLatitude.setUnit(Unit.DEGREES);
            targetBandLatitude.setDescription("Latitude");

            targetBandLongitude = targetProduct.addBand(LON_BAND_NAME, ProductData.TYPE_FLOAT32);
            targetBandLongitude.setNoDataValue(Double.NaN);
            targetBandLongitude.setNoDataValueUsed(true);
            targetBandLongitude.setUnit(Unit.DEGREES);
            targetBandLongitude.setDescription("Longitude");
        }

        // Create bands for interferometric phasors and coherence
        createIfgTargetBands(); // it uses the `complexSrcMap` map

        // Set valid pixels based on invalid DEM data
        for (Band targetBand : targetProduct.getBands()) {
            if (!targetBand.getName().equals(elevationBandName)) {
                targetBand.setValidPixelExpression(targetProduct.getBand(elevationBandName).getValidMaskExpression());
            }
        }
    }

    private void createIfgTargetBands() {

        // Define I/Q bands
        Band sourceBandI0;
        Band sourceBandQ0;
        Band sourceBandI1;
        Band sourceBandQ1;

        for (int i = 0; i < parsedPairs.length; i++) {
            // Get source bands (I and Q) for each image in the pair
            sourceBandI0 = dateMap.get(parsedPairs[i][0]);
            sourceBandQ0 = complexSrcMap.get(sourceBandI0);
            sourceBandI1 = dateMap.get(parsedPairs[i][1]);

            // Add interferometric I/Q bands to targetProduct
            final String pairDates = String.join("_", parsedPairs[i]);

            final String ifgBandNameI = String.join("_", "i", IFG_BAND_NAME_TAG, pairDates);
            final Band targetBandIfgI = targetProduct.addBand(ifgBandNameI, ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandI0, targetBandIfgI);
            targetBandIfgI.setDescription("Interferogram I");

            final String ifgBandNameQ = String.join("_", "q", IFG_BAND_NAME_TAG, pairDates);
            final Band targetBandIfgQ = targetProduct.addBand(ifgBandNameQ, ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandQ0, targetBandIfgQ);
            targetBandIfgQ.setDescription("Interferogram Q");

            // Add virtual bands to targetProduct
            final String virtualBandSuffix = "_" + String.join("_", IFG_BAND_NAME_TAG, pairDates);
            ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandIfgI, targetBandIfgQ, virtualBandSuffix);
            final Band phaseBand = ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandIfgI, targetBandIfgQ, virtualBandSuffix);
            phaseBand.setNoDataValueUsed(true);

            // Store in HashMap
            interferogramPairMap.computeIfAbsent(sourceBandI0, k -> new ArrayList<>()).add(sourceBandI1); // (source I0: list of source I1) pairs
            interferogramMap.computeIfAbsent(sourceBandI0, k -> new ArrayList<>()).add(targetBandIfgI); // (source I0: list of target ifg I) pairs
            complexInterferogramMap.put(targetBandIfgI, targetBandIfgQ); // (target ifg I: target ifg Q) pairs

            // Add coherence band to target product
            final String coherenceBandName = String.join("_", COHERENCE_BAND_NAME_PREFIX, pairDates);
            final Band targetBandCoherence = targetProduct.addBand(coherenceBandName, ProductData.TYPE_FLOAT32);
            targetBandCoherence.setNoDataValue(Double.NaN);
            targetBandCoherence.setNoDataValueUsed(true);
            targetBandCoherence.setUnit(Unit.COHERENCE);
            targetBandCoherence.setDescription("Coherence");

            // Store in HashMap
            coherenceMap.computeIfAbsent(sourceBandI0, k -> new ArrayList<>()).add(targetBandCoherence); // (source I: list of target coherence) pairs
        }
    }

    private void getProductMetadata() throws Exception {

        // Master
        String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            if (bandName.contains("i_")) {
                final Band sourceBandI = sourceProduct.getBand(bandName);
                final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
                getMasterBandMetadata(sourceBandI, abs);
                break;
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
                    getSlaveBandMetadata(sourceBandI, abs);
                    break;
                }
            }
        }
    }

    private void getMasterBandMetadata(final Band sourceBandI, final MetadataElement abs) throws Exception {

        // Get SLCImage and Orbit
        slcImageMaster = new SLCImage(abs, sourceProduct);
        orbitMaster = new Orbit(abs, orbitDegree);

        // Get date
        final String date = OperatorUtils.getAcquisitionDate(abs);
        dateMap.put(date, sourceBandI); // (date: source I) pairs
    }

    private void getSlaveBandMetadata(final Band sourceBandI, final MetadataElement abs) throws Exception {

        // Get SLCImage and Orbit
        final SLCImage slcImage = new SLCImage(abs, sourceProduct);
        final Orbit orbit = new Orbit(abs, orbitDegree);
        slcImageSlaveMap.put(sourceBandI, slcImage); // (source I: SLCImage) pairs
        orbitSlaveMap.put(sourceBandI, orbit); // (source I: Orbit) pairs

        // Get date
        final String date = OperatorUtils.getAcquisitionDate(abs);
        dateMap.put(date, sourceBandI); // (date: source I) pairs
    }

    /**
     * Get InSAR pairs
     */
    private List<String> getPairs() throws Exception {
        // If no pairs have been provided, define sequential pairs
        if (pairs != null && pairs.length > 0) {
            parsedPairs = new String[pairs.length][2];
            for (int i = 0; i < parsedPairs.length; i++) {
                parsedPairs[i] = pairs[i].replaceAll("\\s+", "").split("-");
            }
        } else {
            // Define date format
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("ddMMMyyyy");

            // Load all dates (as Date objects) in a list
            final List<Date> dateList = new ArrayList<>();
            for (String date : dateMap.keySet()) {
                dateList.add(dateFormat.parse(date));
            }

            // Sort list
            Collections.sort(dateList);

            // Generate sequential pairs
            parsedPairs = new String[dateList.size() - 1][2];
            for (int i = 0; i < parsedPairs.length; i++) {
                parsedPairs[i] = new String[]{dateFormat.format(dateList.get(i)),
                        dateFormat.format(dateList.get(i + 1))};
            }
        }

        // Get list of (unique) dates present in pairs
        final List<String> datesInPairsList = new ArrayList<>();
        for (int i = 0; i < parsedPairs.length; i++) {
            datesInPairsList.add(parsedPairs[i][0]);
            datesInPairsList.add(parsedPairs[i][1]);
        }
        return datesInPairsList.stream().distinct().collect(Collectors.toList());
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

        // Compute latitude and longitude
        if (includeLatLon) {
            try {
                final Tile elevationTile = getSourceTile(sourceBandElevation, targetRectangle);
                final Tile latTile = targetTileMap.get(targetBandLatitude);
                final Tile lonTile = targetTileMap.get(targetBandLongitude);
                computeLatLon(elevationTile, latTile, lonTile, targetRectangle);
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }

        // Compute incidence angle
        if (includeIncidenceAngle) {
            try {
                final Tile elevationTile = getSourceTile(sourceBandElevation, targetRectangle);
                final Tile incidenceAngleTile = targetTileMap.get(targetBandIncidenceAngle);
                computeIncidenceAngle(elevationTile, incidenceAngleTile, targetRectangle);
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }

        // Compute wavenumber
        if (includeWavenumber) {
            try {
                final Tile elevationTile = getSourceTile(sourceBandElevation, targetRectangle);
                for (Band sourceBandI : complexSrcMap.keySet()) { // for each SLC
                    final Band targetBandWavenumber = wavenumberMap.get(sourceBandI);
                    if (targetBandWavenumber != null) { // if it's not the master
                        final Tile wavenumberTile = targetTileMap.get(targetBandWavenumber);
                        computeWavenumber(elevationTile, wavenumberTile, targetRectangle,
                                          slcImageSlaveMap.get(sourceBandI),
                                          orbitSlaveMap.get(sourceBandI));
                    }
                }
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }

        // Compute interferometric data
        try {
            // Add borders for coherence computation
            final Rectangle sourceRectangle = new Rectangle(targetRectangle.x - Math.floorDiv(cohWindowRg, 2),
                                                            targetRectangle.y - Math.floorDiv(cohWindowAz, 2),
                                                            targetRectangle.width + Math.floorDiv(cohWindowRg, 2) * 2,
                                                            targetRectangle.height + Math.floorDiv(cohWindowAz, 2) * 2);
            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            // Get elevation tile
            final Tile elevationTile = getSourceTile(sourceBandElevation, sourceRectangle, border);

            // Pre-compute reference phases
            final Map<Band, double[][]> referencePhaseMap = new HashMap<>(10);
            for (Band sourceBandI0 : complexSrcMap.keySet()) { // for each SLC
                if (slcImageSlaveMap.get(sourceBandI0) == null) { // if it's the master
                    referencePhaseMap.put(sourceBandI0, new double[sourceRectangle.height][sourceRectangle.width]);
                } else { // if it's a slave
                    final double[][] referencePhase = computeReferencePhase(elevationTile, sourceRectangle,
                                                                            slcImageSlaveMap.get(sourceBandI0),
                                                                            orbitSlaveMap.get(sourceBandI0));
                    referencePhaseMap.put(sourceBandI0, referencePhase);
                }
            }

            // Compute interferometric phasors and coherence
            for (Band sourceBandI0 : interferogramPairMap.keySet()) {
                final Band sourceBandQ0 = complexSrcMap.get(sourceBandI0);
                List<Band> sourceBandI1List = interferogramPairMap.get(sourceBandI0);
                List<Band> targetBandIfgIList = interferogramMap.get(sourceBandI0);
                List<Band> targetBandCoherenceList = coherenceMap.get(sourceBandI0);
                Guardian.assertTrue("Interferogram mismatch",
                                    sourceBandI1List.size() == targetBandIfgIList.size()
                                            && sourceBandI1List.size() == targetBandCoherenceList.size());
                for (int i = 0; i < sourceBandI1List.size(); i++) { // for each interferogram involving the current slave
                    final Band sourceBandI1 = sourceBandI1List.get(i);
                    final Band sourceBandQ1 = complexSrcMap.get(sourceBandI1);
                    final Band targetBandIfgI = targetBandIfgIList.get(i);
                    final Band targetBandIfgQ = complexInterferogramMap.get(targetBandIfgI);
                    final Band targetBandCoherence = targetBandCoherenceList.get(i);
                    final Tile sourceTileI0 = getSourceTile(sourceBandI0, sourceRectangle, border);
                    final Tile sourceTileQ0 = getSourceTile(sourceBandQ0, sourceRectangle, border);
                    final Tile sourceTileI1 = getSourceTile(sourceBandI1, sourceRectangle, border);
                    final Tile sourceTileQ1 = getSourceTile(sourceBandQ1, sourceRectangle, border);
                    final Tile ifgTileI = targetTileMap.get(targetBandIfgI);
                    final Tile ifgTileQ = targetTileMap.get(targetBandIfgQ);
                    final Tile coherenceTile = targetTileMap.get(targetBandCoherence);
                    computeInterferogram(sourceTileI0, sourceTileQ0, sourceTileI1, sourceTileQ1,
                                         ifgTileI, ifgTileQ, coherenceTile,
                                         referencePhaseMap.get(sourceBandI0), referencePhaseMap.get(sourceBandI1),
                                         cohWindowAz, cohWindowRg,
                                         sourceRectangle, targetRectangle);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void computeLatLon(final Tile elevationTile, final Tile latTile, final Tile lonTile,
                               final Rectangle rectangle) throws Exception {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final ProductData targetBufferLat = latTile.getDataBuffer();
        final ProductData targetBufferLon = lonTile.getDataBuffer();
        final ProductData sourceBufferElevation = elevationTile.getDataBuffer();

        final TileIndex elevationIndex = new TileIndex(elevationTile);
        final TileIndex targetIndex = new TileIndex(latTile);

        for (int y = y0; y < yMax; y++) {
            elevationIndex.calculateStride(y);
            targetIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                final int elevationIdx = elevationIndex.getIndex(x);
                final double heightWrtEllipsoid = sourceBufferElevation.getElemDoubleAt(elevationIdx);

                // Compute lat/lon
                final double[] latLonHeight = orbitMaster.lph2ell(y, x, heightWrtEllipsoid, slcImageMaster);

                targetBufferLat.setElemDoubleAt(targetIdx, Math.toDegrees(latLonHeight[0]));
                targetBufferLon.setElemDoubleAt(targetIdx, Math.toDegrees(latLonHeight[1]));
            }
        }
    }

    private void computeIncidenceAngle(final Tile elevationTile, final Tile incidenceAngleTile, final Rectangle rectangle) throws Exception {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final double rangeSpacing = abs.getAttributeDouble("range_spacing");

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final ProductData targetBufferIncidenceAngle = incidenceAngleTile.getDataBuffer();
        final ProductData sourceBufferElevation = elevationTile.getDataBuffer();

        final TileIndex elevationIndex = new TileIndex(elevationTile);
        final TileIndex targetIndex = new TileIndex(incidenceAngleTile);

        for (int y = y0; y < yMax; y++) {
            elevationIndex.calculateStride(y);
            targetIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                final int elevationIdx = elevationIndex.getIndex(x);
                final double heightWrtEllipsoid = sourceBufferElevation.getElemDoubleAt(elevationIdx);

                // Compute incidence angle
                final Point xyzPositionNextPixel = orbitMaster.lph2xyz(y, x + 1, heightWrtEllipsoid, slcImageMaster);
                final Point xyzPositionPixel = orbitMaster.lph2xyz(y, x, heightWrtEllipsoid, slcImageMaster);
                final double rangeSpacingGround = xyzPositionNextPixel.distance(xyzPositionPixel);
                final double incidenceAngle = Math.toDegrees(Math.asin(rangeSpacing / rangeSpacingGround));

                targetBufferIncidenceAngle.setElemDoubleAt(targetIdx, incidenceAngle);
            }
        }
    }

    private void computeWavenumber(final Tile elevationTile, final Tile wavenumberTile,
                                   final Rectangle rectangle, final SLCImage slcImageSlave,
                                   final Orbit orbitSlave) throws Exception {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final ProductData targetBufferWavenumber = wavenumberTile.getDataBuffer();
        final ProductData sourceBufferElevation = elevationTile.getDataBuffer();

        final TileIndex elevationIndex = new TileIndex(elevationTile);
        final TileIndex targetIndex = new TileIndex(wavenumberTile);

        for (int y = y0; y < yMax; y++) {
            elevationIndex.calculateStride(y);
            targetIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                final int elevationIdx = elevationIndex.getIndex(x);
                final double heightWrtEllipsoid = sourceBufferElevation.getElemDoubleAt(elevationIdx);

                // Compute vertical wavenumber
                final Point xyzPosition = orbitMaster.lph2xyz(y, x, heightWrtEllipsoid, slcImageMaster);
                final Point xyzPositionUp = orbitMaster.lph2xyz(y, x, heightWrtEllipsoid + 1, slcImageMaster);
                final double slaveOneWayRangeTime = orbitSlave.xyz2t(xyzPosition, slcImageSlave).x;
                final double slaveOneWayRangeTimeUp = orbitSlave.xyz2t(xyzPositionUp, slcImageSlave).x;
                final double forwardDifference = Constants.lightSpeed * (slaveOneWayRangeTimeUp - slaveOneWayRangeTime);
                final double wavenumber = -4 * Constants.PI / slcImageSlave.getRadarWavelength() * forwardDifference;

                targetBufferWavenumber.setElemDoubleAt(targetIdx, wavenumber);
            }
        }
    }

    private double[][] computeReferencePhase(final Tile elevationTile, final Rectangle rectangle,
                                             final SLCImage slcImageSlave,
                                             final Orbit orbitSlave) throws Exception {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final double[][] phase = new double[h][w];

        final ProductData sourceBufferElevation = elevationTile.getDataBuffer();
        final TileIndex elevationIndex = new TileIndex(elevationTile);

        for (int y = y0; y < yMax; y++) {
            elevationIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int elevationIdx = elevationIndex.getIndex(x);
                final double masterOneWayRangeTime = slcImageMaster.pix2tr(x);
                final double heightWrtEllipsoid = sourceBufferElevation.getElemDoubleAt(elevationIdx);

                // Compute reference distance
                Point xyzPosition = orbitMaster.lph2xyz(y, x, heightWrtEllipsoid, slcImageMaster);
                final double slaveOneWayRangeTime = orbitSlave.xyz2t(xyzPosition, slcImageSlave).x;
                final double referenceDistance = Constants.lightSpeed * (slaveOneWayRangeTime - masterOneWayRangeTime);

                phase[y - y0][x - x0] = -4 * Constants.PI / slcImageSlave.getRadarWavelength() * referenceDistance;
            }
        }

        return phase;
    }

    private void computeInterferogram(final Tile sourceTileI0, final Tile sourceTileQ0,
                                      final Tile sourceTileI1, final Tile sourceTileQ1,
                                      final Tile ifgTileI, final Tile ifgTileQ, final Tile coherenceTile,
                                      final double[][] referencePhase0, final double[][] referencePhase1,
                                      final int cohWinAz, final int cohWinRg,
                                      final Rectangle sourceRectangle, final Rectangle targetRectangle) {

        int x0 = sourceRectangle.x;
        int y0 = sourceRectangle.y;
        int w = sourceRectangle.width;
        int h = sourceRectangle.height;
        int xMax = x0 + w;
        int yMax = y0 + h;

        final ProductData sourceBufferI0 = sourceTileI0.getDataBuffer();
        final ProductData sourceBufferQ0 = sourceTileQ0.getDataBuffer();
        final ProductData sourceBufferI1 = sourceTileI1.getDataBuffer();
        final ProductData sourceBufferQ1 = sourceTileQ1.getDataBuffer();

        final TileIndex sourceIndex = new TileIndex(sourceTileI0);

        // Compute interferometric phasor and intensities
        final double[][] ifgPhasorI = new double[h][w];
        final double[][] ifgPhasorQ = new double[h][w];
        final double[][] intensity0 = new double[h][w];
        final double[][] intensity1 = new double[h][w];
        for (int y = y0; y < yMax; y++) {
            sourceIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; x++) {
                final int sourceIdx = sourceIndex.getIndex(x);
                final int xx = x - x0;

                // Get value of real and imaginary bands
                final double valueI0 = sourceBufferI0.getElemDoubleAt(sourceIdx);
                final double valueQ0 = sourceBufferQ0.getElemDoubleAt(sourceIdx);
                final double valueI1 = sourceBufferI1.getElemDoubleAt(sourceIdx);
                final double valueQ1 = sourceBufferQ1.getElemDoubleAt(sourceIdx);

                final double[] values = computeIfgPhasorAndIntensities(valueI0, valueQ0, valueI1, valueQ1,
                                                                       referencePhase0[yy][xx], referencePhase1[yy][xx]);
                ifgPhasorI[yy][xx] = values[0];
                ifgPhasorQ[yy][xx] = values[1];
                intensity0[yy][xx] = values[2];
                intensity1[yy][xx] = values[3];
            }
        }

        // Compute coherence
        final double[][] coherence = computeCoherence(ifgPhasorI, ifgPhasorQ, intensity0, intensity1,
                                                      cohWinAz, cohWinRg);

        // Save interferometric phasor and coherence
        final int overlapX = Math.floorDiv((sourceRectangle.width - targetRectangle.width), 2);
        final int overlapY = Math.floorDiv((sourceRectangle.height - targetRectangle.height), 2);
        x0 = targetRectangle.x;
        y0 = targetRectangle.y;
        w = targetRectangle.width;
        h = targetRectangle.height;
        xMax = x0 + w;
        yMax = y0 + h;

        final ProductData targetBufferIfgI = ifgTileI.getDataBuffer();
        final ProductData targetBufferIfgQ = ifgTileQ.getDataBuffer();
        final ProductData targetBufferCoherence = coherenceTile.getDataBuffer();
        final TileIndex targetIndex = new TileIndex(ifgTileI);
        for (int y = y0; y < yMax; y++) {
            targetIndex.calculateStride(y);
            final int yy = y - y0 + overlapY;
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                final int xx = x - x0 + overlapX;

                targetBufferIfgI.setElemDoubleAt(targetIdx, ifgPhasorI[yy][xx]);
                targetBufferIfgQ.setElemDoubleAt(targetIdx, ifgPhasorQ[yy][xx]);
                targetBufferCoherence.setElemDoubleAt(targetIdx, coherence[yy][xx]);
            }
        }
    }

    private double[] computeIfgPhasorAndIntensities(final double valueI0, final double valueQ0,
                                                    final double valueI1, final double valueQ1,
                                                    final double referencePhase0, final double referencePhase1) {

        final double ifgPhasorI = valueI0 * valueI1 + valueQ0 * valueQ1;
        final double ifgPhasorQ = -valueI0 * valueQ1 + valueQ0 * valueI1;
        final double angle = referencePhase0 - referencePhase1;
        final double flattenedIfgPhasorI = ifgPhasorI * FastMath.cos(angle) + ifgPhasorQ * FastMath.sin(angle);
        final double flattenedIfgPhasorQ = -ifgPhasorI * FastMath.sin(angle) + ifgPhasorQ * FastMath.cos(angle);

        return new double[]{flattenedIfgPhasorI, flattenedIfgPhasorQ, valueI0 * valueI0 + valueQ0 * valueQ0,
                valueI1 * valueI1 + valueQ1 * valueQ1};
    }

    private double[][] computeCoherence(final double[][] ifgPhasorI, final double[][] ifgPhasorQ,
                                        final double[][] intensity0, final double[][] intensity1,
                                        final int cohWinAz, int cohWinRg) {

        final int sizeY = ifgPhasorI.length;
        final int sizeX = ifgPhasorI[0].length;
        final double[][] coherence = new double[sizeY][sizeX];
        final int halfCohWinAz = Math.floorDiv(cohWinAz, 2);
        final int halfCohWinRg = Math.floorDiv(cohWinRg, 2);
        int correctionWinAz = 1;
        int correctionWinRg = 1;
        if (2 * halfCohWinAz == cohWinAz) { // if `cohWinAz` is even
            correctionWinAz = 0;
        }
        if (2 * halfCohWinRg == cohWinRg) { // if `cohWinRg` is even
            correctionWinRg = 0;
        }

        for (int y = halfCohWinAz; y < sizeY - halfCohWinAz; y++) {
            for (int x = halfCohWinRg; x < sizeX - halfCohWinRg; x++) {
                double ifgPhasorSumI = 0;
                double ifgPhasorSumQ = 0;
                double intensitySum0 = 0;
                double intensitySum1 = 0;
                for (int r = -halfCohWinAz; r < halfCohWinAz + correctionWinAz; r++) {
                    for (int c = -halfCohWinRg; c < halfCohWinRg + correctionWinRg; c++) {
                        ifgPhasorSumI += ifgPhasorI[y + r][x + c];
                        ifgPhasorSumQ += ifgPhasorQ[y + r][x + c];
                        intensitySum0 += intensity0[y + r][x + c];
                        intensitySum1 += intensity1[y + r][x + c];
                    }
                }
                coherence[y][x] = Math.sqrt((ifgPhasorSumI * ifgPhasorSumI + ifgPhasorSumQ * ifgPhasorSumQ)
                                                    / (intensitySum0 * intensitySum1));
            }
        }

        return coherence;
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

            super(MultiMasterInSAROp.class);
        }
    }
}