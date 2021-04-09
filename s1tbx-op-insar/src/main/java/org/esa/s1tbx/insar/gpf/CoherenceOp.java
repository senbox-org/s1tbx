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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jlinda.core.GeoPoint;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;
import org.jlinda.core.geom.DemTile;
import org.jlinda.core.geom.TopoPhase;
import org.jlinda.core.utils.*;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

@OperatorMetadata(alias = "Coherence",
        category = "Radar/Interferometric/Products",
        authors = "Petar Marinkovic, Jun Lu",
        version = "1.0",
        copyright = "Copyright (C) 2013 by PPO.labs",
        description = "Estimate coherence from stack of coregistered images")
public class CoherenceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(interval = "(1, 90]",
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "10",
            label = "Coherence Azimuth Window Size")
    private int cohWinAz = 10;

    @Parameter(interval = "(1, 90]",
            description = "Size of coherence estimation window in Range direction",
            defaultValue = "10",
            label = "Coherence Range Window Size")
    private int cohWinRg = 10;

    @Parameter(defaultValue = "false", label = "Subtract flat-earth phase in coherence phase")
    private boolean subtractFlatEarthPhase = false;

    @Parameter(valueSet = {"1", "2", "3", "4", "5", "6", "7", "8"},
            description = "Order of 'Flat earth phase' polynomial",
            defaultValue = "5",
            label = "Degree of \"Flat Earth\" polynomial")
    private int srpPolynomialDegree = 5;

    @Parameter(valueSet = {"301", "401", "501", "601", "701", "801", "901", "1001"},
            description = "Number of points for the 'flat earth phase' polynomial estimation",
            defaultValue = "501",
            label = "Number of \"Flat Earth\" estimation points")
    private int srpNumberPoints = 501;

    @Parameter(valueSet = {"1", "2", "3", "4", "5"},
            description = "Degree of orbit (polynomial) interpolator",
            defaultValue = "3",
            label = "Orbit interpolation degree")
    private int orbitDegree = 3;

    @Parameter(description = "Use ground square pixel", defaultValue = "true", label = "Square Pixel")
    private Boolean squarePixel = true;

    @Parameter(defaultValue="false", label="Subtract topographic phase")
    private boolean subtractTopographicPhase = false;
    /*
        @Parameter(interval = "(1, 10]",
                description = "Degree of orbit interpolation polynomial",
                defaultValue = "3",
                label = "Orbit Interpolation Degree")
        private int orbitDegree = 3;
    */
    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec",
            label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(label = "External DEM Apply EGM", defaultValue = "true")
    private Boolean externalDEMApplyEGM = true;

    @Parameter(label = "Tile Extension [%]",
            description = "Define extension of tile for DEM simulation (optimization parameter).",
            defaultValue = "100")
    private String tileExtensionPercent = "100";

    @Parameter(label = "Single Master", defaultValue = "true")
    private Boolean singleMaster = true;

    // source
    private Map<String, CplxContainer> masterMap = new HashMap<>();
    private Map<String, CplxContainer> slaveMap = new HashMap<>();

    private String[] polarisations;
    private String[] subswaths = new String[]{""};

    // target
    private Map<String, ProductContainer> targetMap = new HashMap<>();
    private Map<Band, Band> detectedSlaveMap = new HashMap<>();

    private boolean isComplex;
    private boolean isTOPSARBurstProduct = false;
    private String productTag = null;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int numSubSwaths = 0;
    private int subSwathIndex = 0;

    private MetadataElement mstRoot = null;
    private MetadataElement slvRoot = null;
    private org.jlinda.core.Point[] mstSceneCentreXYZ = null;
    private HashMap<String, DoubleMatrix> flatEarthPolyMap = new HashMap<>();
    private int sourceImageWidth;
    private int sourceImageHeight;

    private ElevationModel dem = null;
    private double demNoDataValue = 0;
    private double demSamplingLat;
    private double demSamplingLon;

    private static final int ORBIT_DEGREE = 3; // hardcoded
    private static final String PRODUCT_SUFFIX = "_Coh";
    private static final boolean OUTPUT_PHASE = false;
    private static final String FLAT_EARTH_PHASE = "flat_earth_phase";
    private static final String TOPO_PHASE = "topo_phase";

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
            productTag = "coh";

            mstRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            final MetadataElement slaveElem =
                    sourceProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
            if (slaveElem != null) {
                slvRoot = slaveElem.getElements()[0];
            }

            if(singleMaster == null) {
                singleMaster = true;
            }

            checkUserInput();

            constructSourceMetadata();

            constructTargetMetadata();

            createTargetProduct();

            if (isComplex && subtractFlatEarthPhase) {
                if (isTOPSARBurstProduct) {
                    getMstApproxSceneCentreXYZ();
                    constructFlatEarthPolynomialsForTOPSARProduct();
                } else {
                    constructFlatEarthPolynomials();
                }
            }

            if (isComplex && subtractTopographicPhase) {
                defineDEM();
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkUserInput() {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfCoregisteredStack();
            isTOPSARBurstProduct = validator.isTOPSARProduct() && !validator.isDebursted();

            isComplex = AbstractMetadata.getAbstractedMetadata(sourceProduct).
                    getAttributeString(AbstractMetadata.SAMPLE_TYPE).contains("COMPLEX");

            if (isTOPSARBurstProduct) {
                su = new Sentinel1Utils(sourceProduct);
                subswaths = su.getSubSwathNames();
                subSwath = su.getSubSwath();
                numSubSwaths = su.getNumOfSubSwath();
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            }

            final String[] polarisationsInBandNames = OperatorUtils.getPolarisations(sourceProduct);
            polarisations = InterferogramOp.getPolsSharedByMstSlv(sourceProduct, polarisationsInBandNames);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "mst";
        final String slaveTag = "slv";

        // get sourceMaster & sourceSlave MetadataElement

        // put sourceMaster metadata into the masterMap
        metaMapPut(masterTag, mstRoot, sourceProduct, masterMap);

        // plug sourceSlave metadata into slaveMap
        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        if (slaveElem == null) {
            slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
        }
        MetadataElement[] slaveRoot = slaveElem.getElements();
        for (MetadataElement meta : slaveRoot) {
            if(!meta.getName().contains(AbstractMetadata.ORIGINAL_PRODUCT_METADATA)) {
                metaMapPut(slaveTag, meta, sourceProduct, slaveMap);
            }
        }
    }

    private void metaMapPut(final String tag,
                            final MetadataElement root,
                            final Product product,
                            final Map<String, CplxContainer> map) throws Exception {

        for (String swath : subswaths) {
            final String subswath = swath.isEmpty() ? "" : '_' + swath.toUpperCase();

            for (String polarisation : polarisations) {
                final String pol = polarisation.isEmpty() ? "" : '_' + polarisation.toUpperCase();

                // map key: ORBIT NUMBER
                String mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT) + subswath + pol;

                // metadata: construct classes and define bands
                final String date = OperatorUtils.getAcquisitionDate(root);
                final SLCImage meta = new SLCImage(root, product);
                final Orbit orbit = new Orbit(root, ORBIT_DEGREE);
                Band bandReal = null;
                Band bandImag = null;

                // loop through all band names(!) : and pull out only one that matches criteria
                for (String bandName : product.getBandNames()) {
                    if (bandName.contains(tag) && bandName.contains(date)) {
                        if (subswath.isEmpty() || bandName.contains(subswath)) {
                            if (pol.isEmpty() || bandName.contains(pol)) {
                                final Band band = product.getBand(bandName);
                                if (BandUtilsDoris.isBandReal(band)) {
                                    bandReal = band;
                                } else if (BandUtilsDoris.isBandImag(band)) {
                                    bandImag = band;
                                }
                            }
                        }
                    }
                }
                if(bandReal != null && bandImag != null) {
                    map.put(mapKey, new CplxContainer(date, meta, orbit, bandReal, bandImag));
                }
            }
        }
    }

    private void constructTargetMetadata() {

        if(singleMaster) {
            for (String keyMaster : masterMap.keySet()) {

                CplxContainer master = masterMap.get(keyMaster);

                for (String keySlave : slaveMap.keySet()) {
                    final CplxContainer slave = slaveMap.get(keySlave);

                    if ((master.polarisation == null || slave.polarisation == null) ||
                            (master.polarisation != null && slave.polarisation != null &&
                                    master.polarisation.equals(slave.polarisation))) {
                        // generate name for product bands
                        final String productName = keyMaster + '_' + keySlave;

                        final ProductContainer productContainer = new ProductContainer(productName, master, slave, false);

                        // put ifg-product bands into map
                        targetMap.put(productName, productContainer);
                    }
                }
            }
        } else {
            final SortedSet<String> allKeys = new TreeSet<>();
            allKeys.addAll(masterMap.keySet());
            allKeys.addAll(slaveMap.keySet());
            String[] keys  = allKeys.toArray(new String[0]);

            for(int i=0; i < keys.length-1; ++i) {
                String keyMaster = keys[i];
                CplxContainer master = masterMap.get(keyMaster);
                if(master == null) {
                    master = slaveMap.get(keyMaster);
                }
                String keySlave = keys[i+1];
                CplxContainer slave = slaveMap.get(keySlave);

                if ((master.polarisation == null || slave.polarisation == null) ||
                        (master.polarisation != null && slave.polarisation != null &&
                                master.polarisation.equals(slave.polarisation))) {
                    // generate name for product bands
                    final String productName = keyMaster + '_' + keySlave;

                    final ProductContainer productContainer = new ProductContainer(productName, master, slave, false);

                    // put ifg-product bands into map
                    targetMap.put(productName, productContainer);
                }
            }
        }
    }

    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if (isComplex) {
            List<String> sortedKeys = new ArrayList<>();
            sortedKeys.addAll(targetMap.keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                final java.util.List<String> targetBandNames = new ArrayList<>();

                final ProductContainer container = targetMap.get(key);
                final CplxContainer master = container.sourceMaster;
                final CplxContainer slave = container.sourceSlave;

                final String subswath = master.subswath.isEmpty() ? "" : '_' + master.subswath.toUpperCase();
                final String pol = InterferogramOp.getPolarisationTag(master);
                final String tag = subswath + pol + '_' + master.date + '_' + slave.date;

                final String coherenceBandName = productTag + tag;
                final Band coherenceBand = targetProduct.addBand(coherenceBandName, ProductData.TYPE_FLOAT32);
                coherenceBand.setNoDataValueUsed(true);
                coherenceBand.setNoDataValue(master.realBand.getNoDataValue());
                container.addBand(Unit.COHERENCE, coherenceBand.getName());
                coherenceBand.setUnit(Unit.COHERENCE);
                targetBandNames.add(coherenceBand.getName());

                if (subtractTopographicPhase && OUTPUT_PHASE) {
                    final String targetBandTgp = "tgp" + tag;
                    final Band tgpBand = targetProduct.addBand(targetBandTgp, ProductData.TYPE_FLOAT32);
                    container.addBand(Unit.PHASE, tgpBand.getName());
                    tgpBand.setUnit(Unit.PHASE);
                    targetBandNames.add(tgpBand.getName());
                }

                if (subtractFlatEarthPhase && OUTPUT_PHASE) {
                    final String targetBandFep = "fep" + tag;
                    final Band fepBand = targetProduct.addBand(targetBandFep, ProductData.TYPE_FLOAT32);
                    container.addBand(Unit.PHASE, fepBand.getName());
                    fepBand.setUnit(Unit.PHASE);
                    targetBandNames.add(fepBand.getName());
                }

                String slvProductName = StackUtils.findOriginalSlaveProductName(sourceProduct, container.sourceSlave.realBand);
                StackUtils.saveSlaveProductBandNames(targetProduct, slvProductName,
                                                     targetBandNames.toArray(new String[0]));
            }
        } else {
            final int numSrcBands = sourceProduct.getNumBands();
            String[] bandNames = sourceProduct.getBandNames();
            if (numSrcBands < 2) {
                throw new OperatorException("To create a coherence image, more than 2 bands are needed.");
            }
            //masterBand = sourceProduct.getBand(findBandName(bandNames, "mst"));
            //addTargetBand(masterBand.getName(), masterBand.getDataType(), masterBand.getUnit());

            // add slave and coherence bands
            for (int i = 1; i <= numSrcBands; i++) {

                final String slaveBandName = findBandName(bandNames, "slv" + i);
                if (slaveBandName == null) {
                    break;
                }
                final Band slaveBand = sourceProduct.getBand(slaveBandName);
                //addTargetBand(slaveBandName, slaveBand.getDataType(), slaveBand.getUnit());

                final Band coherenceBand = targetProduct.addBand("Coherence_slv" + i, ProductData.TYPE_FLOAT32);
                coherenceBand.setUnit("coherence");
                detectedSlaveMap.put(coherenceBand, slaveBand);
            }
        }
    }

    private static String findBandName(String[] bandNames, String namePattern) {

        String bandName = null;
        for (String name : bandNames) {
            if (name.contains(namePattern)) {
                bandName = name;
                break;
            }
        }
        return bandName;
    }

    private void getMstApproxSceneCentreXYZ() {

        final int numOfBursts = subSwath[subSwathIndex - 1].numOfBursts;
        mstSceneCentreXYZ = new Point[numOfBursts];

        for (int b = 0; b < numOfBursts; b++) {
            final double firstLineTime = subSwath[subSwathIndex - 1].burstFirstLineTime[b];
            final double lastLineTime = subSwath[subSwathIndex - 1].burstLastLineTime[b];
            final double slrTimeToFirstPixel = subSwath[subSwathIndex - 1].slrTimeToFirstPixel;
            final double slrTimeToLastPixel = subSwath[subSwathIndex - 1].slrTimeToLastPixel;
            final double latUL = su.getLatitude(firstLineTime, slrTimeToFirstPixel, subSwathIndex);
            final double latUR = su.getLatitude(firstLineTime, slrTimeToLastPixel, subSwathIndex);
            final double latLL = su.getLatitude(lastLineTime, slrTimeToFirstPixel, subSwathIndex);
            final double latLR = su.getLatitude(lastLineTime, slrTimeToLastPixel, subSwathIndex);

            final double lonUL = su.getLongitude(firstLineTime, slrTimeToFirstPixel, subSwathIndex);
            final double lonUR = su.getLongitude(firstLineTime, slrTimeToLastPixel, subSwathIndex);
            final double lonLL = su.getLongitude(lastLineTime, slrTimeToFirstPixel, subSwathIndex);
            final double lonLR = su.getLongitude(lastLineTime, slrTimeToLastPixel, subSwathIndex);

            final double lat = (latUL + latUR + latLL + latLR) / 4.0;
            final double lon = (lonUL + lonUR + lonLL + lonLR) / 4.0;

            final PosVector mstSceneCenter = new PosVector();
            GeoUtils.geo2xyzWGS84(lat, lon, 0.0, mstSceneCenter);
            mstSceneCentreXYZ[b] = new Point(mstSceneCenter.toArray());
        }
    }

    private void constructFlatEarthPolynomialsForTOPSARProduct() throws Exception {

        for (String key : targetMap.keySet()) {

            final ProductContainer container = targetMap.get(key);
            final CplxContainer master = container.sourceMaster;
            final CplxContainer slave = container.sourceSlave;

            for (int s = 0; s < numSubSwaths; s++) {

                final int numBursts = subSwath[s].numOfBursts;

                for (int b = 0; b < numBursts; b++) {

                    final String polynomialName = slave.name + '_' + s + '_' + b;

                    flatEarthPolyMap.put(polynomialName, InterferogramOp.estimateFlatEarthPolynomial(
                            master, slave, s + 1, b, mstSceneCentreXYZ, orbitDegree, srpPolynomialDegree,
                            srpNumberPoints, subSwath, su));
                }
            }
        }
    }

    private void constructFlatEarthPolynomials() throws Exception {

        for (String key : targetMap.keySet()) {

            final ProductContainer container = targetMap.get(key);
            final CplxContainer master = container.sourceMaster;
            final CplxContainer slave = container.sourceSlave;

            flatEarthPolyMap.put(slave.name, InterferogramOp.estimateFlatEarthPolynomial(
                        master.metaData, master.orbit, slave.metaData, slave.orbit, sourceImageWidth,
                        sourceImageHeight, srpPolynomialDegree, srpNumberPoints, sourceProduct));
        }
    }

    private void defineDEM() throws IOException {

        String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

        if (externalDEMFile == null) {
            dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
            demNoDataValue = dem.getDescriptor().getNoDataValue();
            demSamplingLat = dem.getDescriptor().getTileWidthInDegrees() * (1.0f /
                    dem.getDescriptor().getTileWidth()) * org.jlinda.core.Constants.DTOR;

            demSamplingLon = demSamplingLat;

        } else {

            dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
            ((FileElevationModel) dem).applyEarthGravitionalModel(externalDEMApplyEGM);
            demNoDataValue = externalDEMNoDataValue;
            demName = externalDEMFile.getName();

            try {
                demSamplingLat =
                        (dem.getGeoPos(new PixelPos(0, 1)).getLat() - dem.getGeoPos(new PixelPos(0, 0)).getLat()) *
                                org.jlinda.core.Constants.DTOR;
                demSamplingLon =
                        (dem.getGeoPos(new PixelPos(1, 0)).getLon() - dem.getGeoPos(new PixelPos(0, 0)).getLon()) *
                                org.jlinda.core.Constants.DTOR;
            } catch (Exception e) {
                throw new OperatorException("The DEM '" + demName + "' cannot be properly interpreted.");
            }
        }
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        if (isTOPSARBurstProduct) {
            computeTileForTOPSARProduct(targetTileMap, targetRectangle, pm);
        } else if (!isComplex) {
            computeTileForDetectedProduct(targetTileMap, targetRectangle, pm);
        } else {
            computeTileForNormalProduct(targetTileMap, targetRectangle, pm);
        }
    }

    private void computeTileForDetectedProduct(final Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                               ProgressMonitor pm) throws OperatorException {
        try {
            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            final int maxX = x0 + w;
            final int maxY = y0 + h;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            for (Band targetBand : targetTileMap.keySet()) {
                final Tile targetTile = targetTileMap.get(targetBand);

                final Band srcBand = sourceProduct.getBand(targetBand.getName());
                if (!targetBand.getUnit().contains("coherence")) { // master and slave bands

                    final Tile srcRaster = getSourceTile(srcBand, targetRectangle);
                    final ProductData srcData = srcRaster.getDataBuffer();
                    final ProductData targetData = targetTile.getDataBuffer();
                    for (int y = y0; y < maxY; y++) {
                        for (int x = x0; x < maxX; x++) {
                            final int index = srcRaster.getDataBufferIndex(x, y);
                            targetData.setElemFloatAt(targetTile.getDataBufferIndex(x, y),
                                                      srcData.getElemFloatAt(index));
                        }
                    }

                } else { // coherence bands
                    String[] bandNames = sourceProduct.getBandNames();
                    Band masterBand = sourceProduct.getBand(findBandName(bandNames, "mst"));

                    final Band slaveBand = detectedSlaveMap.get(targetBand);
                    final float[] dataArray = new float[w * h];
                    final RealCoherenceData realData = new RealCoherenceData();
                    int k = 0;
                    for (int y = y0; y < maxY; y++) {
                        for (int x = x0; x < maxX; x++) {
                            getMasterSlaveDataForCurWindow(x, y, masterBand, slaveBand, realData);
                            dataArray[k++] = computeCoherence(realData);
                        }
                    }

                    final ProductData rawTargetData = ProductData.createInstance(dataArray);
                    targetTile.setRawSamples(rawTargetData);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMasterSlaveDataForCurWindow(
            int xC, int yC, Band masterBand, Band slaveBand, RealCoherenceData realData) {

        // compute upper left corner coordinate (xUL, yUL)
        final int halfWindowSizeAz = cohWinAz / 2;
        final int halfWindowSizeRg = cohWinRg / 2;
        final int xUL = Math.max(xC - halfWindowSizeRg, 0);
        final int yUL = Math.max(yC - halfWindowSizeAz, 0);

        // compute lower right corner coordinate (xLR, yLR)
        final int xLR = Math.min(xC + halfWindowSizeRg, sourceImageWidth - 1);
        final int yLR = Math.min(yC + halfWindowSizeAz, sourceImageHeight - 1);

        // compute actual window width (w) and height (h)
        final int w = xLR - xUL + 1;
        final int h = yLR - yUL + 1;

        realData.m = new double[w * h];
        realData.s = new double[w * h];

        final Rectangle windowRectangle = new Rectangle(xUL, yUL, w, h);
        final Tile masterRaster = getSourceTile(masterBand, windowRectangle);
        final Tile slaveRaster = getSourceTile(slaveBand, windowRectangle);
        final ProductData masterData = masterRaster.getDataBuffer();
        final ProductData slaveData = slaveRaster.getDataBuffer();

        int k = 0;
        for (int y = yUL; y <= yLR; y++) {
            for (int x = xUL; x <= xLR; x++) {
                final int index = masterRaster.getDataBufferIndex(x, y);
                realData.m[k] = masterData.getElemDoubleAt(index);
                realData.s[k] = slaveData.getElemDoubleAt(index);
                k++;
            }
        }
    }

    private static float computeCoherence(final RealCoherenceData realData) {

        double sum1 = 0.0, sum2 = 0.0, sum3 = 0.0;
        for (int i = 0; i < realData.m.length; i++) {
            final double m = realData.m[i];
            final double s = realData.s[i];
            sum1 += m * s;
            sum2 += m * m;
            sum3 += s * s;
        }
        return (float) (Math.abs(sum1) / Math.sqrt(sum2 * sum3));
    }

    private void computeTileForNormalProduct(
            final Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final int y0 = targetRectangle.y;
            final int yN = y0 + targetRectangle.height - 1;
            final int x0 = targetRectangle.x;
            final int xN = targetRectangle.x + targetRectangle.width - 1;
            //System.out.println("x0 = " + x0 +", y0 = " + y0 + ", w = " + targetRectangle.width + ", h = " + targetRectangle.height);

            final int cohx0 = targetRectangle.x - (cohWinRg - 1) / 2;
            final int cohy0 = targetRectangle.y - (cohWinAz - 1) / 2;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            final Rectangle extRect = new Rectangle(cohx0, cohy0, cohw, cohh);

            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(
                    cohy0, cohy0 + cohh - 1, cohx0, cohx0 + cohw - 1);

            DemTile demTile = null;
            if (subtractTopographicPhase) {
                demTile = TopoPhase.getDEMTile(tileWindow, targetMap, dem, demNoDataValue,
                        demSamplingLat, demSamplingLon, tileExtensionPercent);

                if (demTile.getData().length < 3 || demTile.getData()[0].length < 3) {
                    throw new OperatorException("The resolution of the selected DEM is too low, " +
                            "please select DEM with higher resolution.");
                }
            }

            for (String cohKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(cohKey);

                final Tile mstTileReal = getSourceTile(product.sourceMaster.realBand, extRect, border);
                final Tile mstTileImag = getSourceTile(product.sourceMaster.imagBand, extRect, border);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal, mstTileImag);

                final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, extRect, border);
                final Tile slvTileImag = getSourceTile(product.sourceSlave.imagBand, extRect, border);
                final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal, slvTileImag);

                if (subtractFlatEarthPhase) {
                    final DoubleMatrix flatEarthPhase = computeFlatEarthPhase(
                            cohx0, cohx0 + cohw - 1, cohw, cohy0, cohy0 + cohh - 1, cohh,
                            0, sourceImageWidth - 1, 0, sourceImageHeight - 1, product.sourceSlave.name);

                    final ComplexDoubleMatrix complexReferencePhase = new ComplexDoubleMatrix(
                            MatrixFunctions.cos(flatEarthPhase), MatrixFunctions.sin(flatEarthPhase));

                    dataSlave.muli(complexReferencePhase);

                    if (OUTPUT_PHASE) {
                        saveFlatEarthPhase(x0, xN, y0, yN, flatEarthPhase, product, targetTileMap);
                    }
                }

                if (subtractTopographicPhase) {
                    final TopoPhase topoPhase = TopoPhase.computeTopoPhase(
                            product, tileWindow, demTile, false);

                    final ComplexDoubleMatrix ComplexTopoPhase = new ComplexDoubleMatrix(
                            MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                            MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                    dataSlave.muli(ComplexTopoPhase);

                    if (OUTPUT_PHASE) {
                        saveTopoPhase(x0, xN, y0, yN, topoPhase.demPhase, product, targetTileMap);
                    }
                }

                for (int i = 0; i < dataMaster.length; i++) {
                    double tmp = norm(dataMaster.get(i));
                    dataMaster.put(i, dataMaster.get(i).mul(dataSlave.get(i).conj()));
                    dataSlave.put(i, new ComplexDouble(norm(dataSlave.get(i)), tmp));
                }

                DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster, dataSlave, cohWinAz, cohWinRg);

                saveCoherence(cohMatrix, product, targetTileMap, targetRectangle);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private DoubleMatrix computeFlatEarthPhase(final int xMin, final int xMax, final int xSize,
                                               final int yMin, final int yMax, final int ySize,
                                               final int minPixel, final int maxPixel,
                                               final int minLine, final int maxLine,
                                               final String polynomialName) {

        DoubleMatrix rangeAxisNormalized = DoubleMatrix.linspace(xMin, xMax, xSize);
        rangeAxisNormalized = InterferogramOp.normalizeDoubleMatrix(rangeAxisNormalized, minPixel, maxPixel);

        DoubleMatrix azimuthAxisNormalized = DoubleMatrix.linspace(yMin, yMax, ySize);
        azimuthAxisNormalized = InterferogramOp.normalizeDoubleMatrix(azimuthAxisNormalized, minLine, maxLine);

        final DoubleMatrix polyCoeffs = flatEarthPolyMap.get(polynomialName);

        return PolyUtils.polyval(azimuthAxisNormalized, rangeAxisNormalized,
                polyCoeffs, PolyUtils.degreeFromCoefficients(polyCoeffs.length));
    }

    private void saveTopoPhase(final int x0, final int xN, final int y0, final int yN, final double[][] topoPhase,
                               final ProductContainer product, final Map<Band, Tile> targetTileMap) {

        final Band topoPhaseBand = targetProduct.getBand(product.getBandName(TOPO_PHASE));
        final Tile topoPhaseTile = targetTileMap.get(topoPhaseBand);
        final ProductData topoPhaseData = topoPhaseTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(topoPhaseTile);

        for (int y = y0; y <= yN; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0 + (cohWinAz - 1) / 2;
            for (int x = x0; x <= xN; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0 + (cohWinRg - 1) / 2;
                topoPhaseData.setElemFloatAt(tgtIdx, (float)topoPhase[yy][xx]);
            }
        }
    }

    private void saveFlatEarthPhase(final int x0, final int xN, final int y0, final int yN, final DoubleMatrix refPhase,
                                    final ProductContainer product, final Map<Band, Tile> targetTileMap) {

        final Band flatEarthPhaseBand = targetProduct.getBand(product.getBandName(FLAT_EARTH_PHASE));
        final Tile flatEarthPhaseTile = targetTileMap.get(flatEarthPhaseBand);
        final ProductData flatEarthPhaseData = flatEarthPhaseTile.getDataBuffer();

        final TileIndex tgtIndex = new TileIndex(flatEarthPhaseTile);
        for (int y = y0; y <= yN; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0 + (cohWinAz - 1) / 2;
            for (int x = x0; x <= xN; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0 + (cohWinRg - 1) / 2;
                flatEarthPhaseData.setElemFloatAt(tgtIdx, (float)refPhase.get(yy, xx));
            }
        }
    }

    private void saveCoherence(final DoubleMatrix cohMatrix, final ProductContainer product,
                               final Map<Band, Tile> targetTileMap, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int maxX = x0 + targetRectangle.width;
        final int maxY = y0 + targetRectangle.height;

        final Band coherenceBand = targetProduct.getBand(product.getBandName(Unit.COHERENCE));
        final Tile coherenceTile = targetTileMap.get(coherenceBand);
        final ProductData coherenceData = coherenceTile.getDataBuffer();

        final double srcNoDataValue = product.sourceMaster.realBand.getNoDataValue();
        final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle);
        final ProductData srcSlvData = slvTileReal.getDataBuffer();
        final TileIndex srcSlvIndex = new TileIndex(slvTileReal);

        final TileIndex tgtIndex = new TileIndex(coherenceTile);
        for (int y = y0; y < maxY; y++) {
            tgtIndex.calculateStride(y);
            srcSlvIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < maxX; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0;

                if (srcSlvData.getElemDoubleAt(srcSlvIndex.getIndex(x)) == srcNoDataValue) {
                    coherenceData.setElemFloatAt(tgtIdx, (float) srcNoDataValue);
                } else {
                    final double coh = cohMatrix.get(yy, xx);
                    coherenceData.setElemFloatAt(tgtIdx, (float) coh);
                }
            }
        }
    }

    private void computeTileForTOPSARProduct(
            final Map<Band, Tile> targetTileMap, final Rectangle targetRectangle, final ProgressMonitor pm)
            throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final int txMax = tx0 + tw;
            final int tyMax = ty0 + th;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            for (int burstIndex = 0; burstIndex < subSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
                final int firstLineIdx = burstIndex * subSwath[subSwathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + subSwath[subSwathIndex - 1].linesPerBurst - 1;

                if (tyMax <= firstLineIdx || ty0 > lastLineIdx) {
                    continue;
                }

                final int ntx0 = tx0;
                final int ntw = tw;
                final int nty0 = Math.max(ty0, firstLineIdx);
                final int ntyMax = Math.min(tyMax, lastLineIdx + 1);
                final int nth = ntyMax - nty0;
                final Rectangle partialTileRectangle = new Rectangle(ntx0, nty0, ntw, nth);
                //System.out.println("burst = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

                computePartialTile(subSwathIndex, burstIndex, firstLineIdx, partialTileRectangle, targetTileMap);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computePartialTile(final int subSwathIndex, final int burstIndex,
                                    final int firstLineIdx, final Rectangle targetRectangle,
                                    final Map<Band, Tile> targetTileMap) throws OperatorException {

        try {
            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final int y0 = targetRectangle.y;
            final int yN = y0 + targetRectangle.height - 1;
            final int x0 = targetRectangle.x;
            final int xN = x0 + targetRectangle.width - 1;

            final int cohx0 = targetRectangle.x - (cohWinRg - 1) / 2;
            final int cohy0 = targetRectangle.y - (cohWinAz - 1) / 2;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            final Rectangle extRect = new Rectangle(cohx0, cohy0, cohw, cohh);

            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(
                    cohy0 - firstLineIdx, cohy0 + cohh - 1 - firstLineIdx, cohx0, cohx0 + cohw - 1);

            final SLCImage mstMeta = targetMap.values().iterator().next().sourceMaster.metaData.clone();
            updateMstMetaData(burstIndex, mstMeta);
            final Orbit mstOrbit = targetMap.values().iterator().next().sourceMaster.orbit;

            DemTile demTile = null;
            if (subtractTopographicPhase) {
                demTile = TopoPhase.getDEMTile(tileWindow, mstMeta, mstOrbit, dem,
                        demNoDataValue, demSamplingLat, demSamplingLon, tileExtensionPercent);

                if (demTile == null) {
                    throw new OperatorException("The selected DEM has no overlap with the image or is invalid.");
                }

                if (demTile.getData().length < 3 || demTile.getData()[0].length < 3) {
                    throw new OperatorException("The resolution of the selected DEM is too low, " +
                            "please select DEM with higher resolution.");
                }
            }

            final int minLine = 0;
            final int maxLine = subSwath[subSwathIndex - 1].linesPerBurst - 1;
            final int minPixel = 0;
            final int maxPixel = subSwath[subSwathIndex - 1].samplesPerBurst - 1;

            for (String cohKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(cohKey);
                final SLCImage slvMeta = product.sourceSlave.metaData.clone();
                updateSlvMetaData(product, burstIndex, slvMeta);
                final Orbit slvOrbit = product.sourceSlave.orbit;

                final Tile mstTileReal = getSourceTile(product.sourceMaster.realBand, extRect, border);
                final Tile mstTileImag = getSourceTile(product.sourceMaster.imagBand, extRect, border);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal, mstTileImag);

                final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, extRect, border);
                final Tile slvTileImag = getSourceTile(product.sourceSlave.imagBand, extRect, border);
                final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal, slvTileImag);

                final String polynomialName = product.sourceSlave.name + '_' + (subSwathIndex - 1) + '_' + burstIndex;
                if (subtractFlatEarthPhase) {
                    final DoubleMatrix flatEarthPhase = computeFlatEarthPhase(
                            cohx0, cohx0 + cohw - 1, cohw, cohy0 - firstLineIdx, cohy0 + cohh - 1 - firstLineIdx, cohh,
                            minPixel, maxPixel, minLine, maxLine, polynomialName);

                    final ComplexDoubleMatrix complexReferencePhase = new ComplexDoubleMatrix(
                            MatrixFunctions.cos(flatEarthPhase), MatrixFunctions.sin(flatEarthPhase));

                    dataSlave.muli(complexReferencePhase);

                    if (OUTPUT_PHASE) {
                        saveFlatEarthPhase(x0, xN, y0, yN, flatEarthPhase, product, targetTileMap);
                    }
                }

                if (subtractTopographicPhase) {
                    TopoPhase topoPhase = TopoPhase.computeTopoPhase(
                            mstMeta, mstOrbit, slvMeta, slvOrbit, tileWindow, demTile, false);

                    final ComplexDoubleMatrix ComplexTopoPhase = new ComplexDoubleMatrix(
                            MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                            MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                    dataSlave.muli(ComplexTopoPhase);

                    if (OUTPUT_PHASE) {
                        saveTopoPhase(x0, xN, y0, yN, topoPhase.demPhase, product, targetTileMap);
                    }
                }

                for (int i = 0; i < dataMaster.length; i++) {
                    double tmp = norm(dataMaster.get(i));
                    dataMaster.put(i, dataMaster.get(i).mul(dataSlave.get(i).conj()));
                    dataSlave.put(i, new ComplexDouble(norm(dataSlave.get(i)), tmp));
                }

                DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster, dataSlave, cohWinAz, cohWinRg);

                saveCoherence(cohMatrix, product, targetTileMap, targetRectangle);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void updateMstMetaData(final int burstIndex, final SLCImage mstMeta) {

        final double burstFirstLineTimeMJD = subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex] /
                Constants.secondsInDay;

        final double burstFirstLineTimeSecondsOfDay = (burstFirstLineTimeMJD - (int)burstFirstLineTimeMJD) *
                Constants.secondsInDay;

        mstMeta.settAzi1(burstFirstLineTimeSecondsOfDay);

        mstMeta.setCurrentWindow(new org.jlinda.core.Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));

        mstMeta.setOriginalWindow(new org.jlinda.core.Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));

        mstMeta.setApproxGeoCentreOriginal(getApproxGeoCentre(subSwathIndex, burstIndex));
    }

    private void updateSlvMetaData(final ProductContainer product, final int burstIndex, final SLCImage slvMeta) {

        final double slvBurstFirstLineTimeMJD = slvMeta.getMjd() - product.sourceMaster.metaData.getMjd() +
                subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex] / Constants.secondsInDay;

        final double slvBurstFirstLineTimeSecondsOfDay = (slvBurstFirstLineTimeMJD - (int)slvBurstFirstLineTimeMJD) *
                Constants.secondsInDay;

        slvMeta.settAzi1(slvBurstFirstLineTimeSecondsOfDay);

        slvMeta.setCurrentWindow(new org.jlinda.core.Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));

        slvMeta.setOriginalWindow(new org.jlinda.core.Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));
    }

    private GeoPoint getApproxGeoCentre(final int subSwathIndex, final int burstIndex) {

        final int cols = subSwath[subSwathIndex - 1].latitude[0].length;

        double lat = 0.0, lon = 0.0;
        for (int r = burstIndex; r <= burstIndex + 1; r++) {
            for (int c = 0; c < cols; c++) {
                lat += subSwath[subSwathIndex - 1].latitude[r][c];
                lon += subSwath[subSwathIndex - 1].longitude[r][c];
            }
        }

        return new GeoPoint(lat / (2*cols), lon / (2*cols));
    }

    private static double norm(final ComplexDouble number) {
        return number.real() * number.real() + number.imag() * number.imag();
    }

    private static double norm(final double real, final double imag) {
        return real * real + imag * imag;
    }

    public static DoubleMatrix coherence(final double[] iMst, final double[] qMst, final double[] iSlv,
                                         final double[] qSlv, final int winL, final int winP, int w, int h) {

        final ComplexDoubleMatrix input = new ComplexDoubleMatrix(h, w);
        final ComplexDoubleMatrix norms = new ComplexDoubleMatrix(h, w);
        for (int y = 0; y < h; y++) {
            final int stride = y * w;
            for (int x = 0; x < w; x++) {
                input.put(y, x, new ComplexDouble(iMst[stride + x],
                                                  qMst[stride + x]));
                norms.put(y, x, new ComplexDouble(iSlv[stride + x], qSlv[stride + x]));
            }
        }

        if (input.rows != norms.rows) {
            throw new IllegalArgumentException("coherence: not the same dimensions.");
        }

        // allocate output :: account for window overlap
        final int extent_RG = input.columns;
        final int extent_AZ = input.rows - winL + 1;
        final DoubleMatrix result = new DoubleMatrix(input.rows - winL + 1, input.columns - winP + 1);

        // temp variables
        int i, j, k, l;
        ComplexDouble sum;
        ComplexDouble power;
        final int leadingZeros = (winP - 1) / 2;  // number of pixels=0 floor...
        final int trailingZeros = (winP) / 2;     // floor...

        for (j = leadingZeros; j < extent_RG - trailingZeros; j++) {

            sum = new ComplexDouble(0);
            power = new ComplexDouble(0);

            //// Compute sum over first data block ////
            int minL = j - leadingZeros;
            int maxL = minL + winP;
            for (k = 0; k < winL; k++) {
                for (l = minL; l < maxL; l++) {
                    //sum.addi(input.get(k, l));
                    //power.addi(norms.get(k, l));
                    int inI = 2 * input.index(k, l);
                    sum.set(sum.real() + input.data[inI], sum.imag() + input.data[inI + 1]);
                    power.set(power.real() + norms.data[inI], power.imag() + norms.data[inI + 1]);
                }
            }
            result.put(0, minL, coherenceProduct(sum, power));

            //// Compute (relatively) sum over rest of data blocks ////
            final int maxI = extent_AZ - 1;
            for (i = 0; i < maxI; i++) {
                final int iwinL = i + winL;
                for (l = minL; l < maxL; l++) {
                    //sum.addi(input.get(iwinL, l).sub(input.get(i, l)));
                    //power.addi(norms.get(iwinL, l).sub(norms.get(i, l)));

                    int inI = 2 * input.index(i, l);
                    int inWinL = 2 * input.index(iwinL, l);
                    sum.set(sum.real() + (input.data[inWinL] - input.data[inI]), sum.imag() +
                            (input.data[inWinL + 1] - input.data[inI + 1]));
                    power.set(power.real() + (norms.data[inWinL] - norms.data[inI]),
                            power.imag() + (norms.data[inWinL + 1] - norms.data[inI + 1]));
                }
                result.put(i + 1, j - leadingZeros, coherenceProduct(sum, power));
            }
        }
        return result;
    }

    static double coherenceProduct(final ComplexDouble sum, final ComplexDouble power) {
        final double product = power.real() * power.imag();
//        return (product > 0.0) ? Math.sqrt(Math.pow(sum.abs(),2) / product) : 0.0;
        return (product > 0.0) ? sum.abs() / Math.sqrt(product) : 0.0;
    }

    public static void getDerivedParameters(Product srcProduct, DerivedParams param) throws Exception {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        double rangeSpacing = abs.getAttributeDouble(AbstractMetadata.range_spacing, 1);
        double azimuthSpacing = abs.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);

        double groundRangeSpacing = rangeSpacing;
        if (rangeSpacing == AbstractMetadata.NO_METADATA) {
            azimuthSpacing = 1;
            groundRangeSpacing = 1;
        } else if (!srgrFlag) {
            final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(srcProduct);
            if (incidenceAngle != null) {
                final int sourceImageWidth = srcProduct.getSceneRasterWidth();
                final int sourceImageHeight = srcProduct.getSceneRasterHeight();
                final int x = sourceImageWidth / 2;
                final int y = sourceImageHeight / 2;
                final double incidenceAngleAtCentreRangePixel = incidenceAngle.getPixelDouble(x, y);
                groundRangeSpacing /= FastMath.sin(incidenceAngleAtCentreRangePixel * Constants.DTOR);
            }
        }

        final double cohWinAz = param.cohWinRg * groundRangeSpacing / azimuthSpacing;
        if (cohWinAz < 1.0) {
            param.cohWinAz = 2;
            param.cohWinRg = (int) Math.round(azimuthSpacing / groundRangeSpacing);
        } else {
            param.cohWinAz = (int) Math.round(cohWinAz);
        }
    }

    public static class DerivedParams {
        public int cohWinAz = 0;
        public int cohWinRg = 0;
    }

    private static class RealCoherenceData {
        private double[] m = null;          // real master data for coherence computation
        private double[] s = null;          // real slave data for coherence computation
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
            super(CoherenceOp.class);
        }
    }
}
