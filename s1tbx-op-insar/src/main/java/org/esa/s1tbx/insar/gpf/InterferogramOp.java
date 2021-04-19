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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.*;
import org.jblas.*;
import org.jlinda.core.*;
import org.jlinda.core.Point;
import org.jlinda.core.Window;
import org.jlinda.core.geom.DemTile;
import org.jlinda.core.geom.TopoPhase;
import org.jlinda.core.utils.*;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@OperatorMetadata(alias = "Interferogram",
        category = "Radar/Interferometric/Products",
        authors = "Petar Marinkovic, Jun Lu",
        version = "1.0",
        description = "Compute interferograms from stack of coregistered S-1 images", internal = false)
public class InterferogramOp extends Operator {
    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "true", label = "Subtract flat-earth phase")
    private boolean subtractFlatEarthPhase = true;

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

    @Parameter(defaultValue = "true", label = "Include coherence estimation")
    private boolean includeCoherence = true;

    @Parameter(description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "10",
            label = "Coherence Azimuth Window Size")
    private int cohWinAz = 10;

    @Parameter(description = "Size of coherence estimation window in Range direction",
            defaultValue = "10",
            label = "Coherence Range Window Size")
    private int cohWinRg = 10;

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

    @Parameter(defaultValue = "false", label = "Output Elevation")
    private boolean outputElevation = false;

    @Parameter(defaultValue = "false", label = "Output Lat/Lon")
    private boolean outputLatLon = false;

    // flat_earth_polynomial container
    private Map<String, DoubleMatrix> flatEarthPolyMap = new HashMap<>();
    private boolean flatEarthEstimated = false;

    // source
    private Map<String, CplxContainer> masterMap = new HashMap<>();
    private Map<String, CplxContainer> slaveMap = new HashMap<>();

    private String[] polarisations;
    private String[] subswaths = new String[]{""};

    // target
    private Map<String, ProductContainer> targetMap = new HashMap<>();

    // operator tags
    private String productTag = "ifg";
    private int sourceImageWidth;
    private int sourceImageHeight;

    private ElevationModel dem = null;
    private double demNoDataValue = 0;
    private double demSamplingLat;
    private double demSamplingLon;

    private boolean isTOPSARBurstProduct = false;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int numSubSwaths = 0;
    private org.jlinda.core.Point[] mstSceneCentreXYZ = null;
    private int subSwathIndex = 0;
    private MetadataElement mstRoot = null;

    private static final boolean CREATE_VIRTUAL_BAND = true;
    private static final boolean OUTPUT_PHASE = false;
    private static final String PRODUCT_SUFFIX = "_Ifg";
    private static final String FLAT_EARTH_PHASE = "flat_earth_phase";
    private static final String TOPO_PHASE = "topo_phase";
    private static final String COHERENCE = "coherence";
    private static final String ELEVATION = "elevation";
    private static final String LATITUDE = " orthorectifiedLat";
    private static final String LONGITUDE = "orthorectifiedLon";

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
            if(AbstractMetadata.getAbstractedMetadata(sourceProduct).containsAttribute("multimaster_split")){
                mstRoot = sourceProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElementAt(0);
            }
            else{
                mstRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            }

            checkUserInput();

            constructSourceMetadata();
            constructTargetMetadata();

            if (subtractTopographicPhase) {
                defineDEM();
            }

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkUserInput() {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfCoregisteredStack();
            validator.checkIfSLC();
            isTOPSARBurstProduct = validator.isTOPSARProduct() && !validator.isDebursted();

            if (isTOPSARBurstProduct) {
                final String mProcSysId = mstRoot.getAttributeString(AbstractMetadata.ProcessingSystemIdentifier);
                final float mVersion = Float.valueOf(mProcSysId.substring(mProcSysId.lastIndexOf(' ')));

                MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
                if (slaveElem == null) {
                    slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
                }
                MetadataElement[] slaveRoot = slaveElem.getElements();
                for (MetadataElement slvRoot : slaveRoot) {
                    final String sProcSysId = slvRoot.getAttributeString(AbstractMetadata.ProcessingSystemIdentifier);
                    final float sVersion = Float.valueOf(sProcSysId.substring(sProcSysId.lastIndexOf(' ')));
                    if ((mVersion < 2.43 && sVersion >= 2.43 && mstRoot.getAttribute("EAP Correction") == null) ||
                            (sVersion < 2.43 && mVersion >= 2.43 && slvRoot.getAttribute("EAP Correction") == null)) {
                        throw new OperatorException("Source products cannot be InSAR pairs: one is EAP phase corrected" +
                                " and the other is not. Apply EAP Correction.");
                    }
                }

                su = new Sentinel1Utils(sourceProduct);
                subswaths = su.getSubSwathNames();
                subSwath = su.getSubSwath();
                numSubSwaths = su.getNumOfSubSwath();
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            }

            final String[] polarisationsInBandNames = OperatorUtils.getPolarisations(sourceProduct);
            polarisations = getPolsSharedByMstSlv(sourceProduct, polarisationsInBandNames);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    public static String[] getPolsSharedByMstSlv(final Product sourceProduct, final String[] polarisationsInBandNames) {

        final String masterTag = "mst";
        final String slaveTag = "slv";
        final List<String> polarisations = new ArrayList<>();

        for (String pol : polarisationsInBandNames) {
            if (checkPolarisation(sourceProduct, masterTag, pol) && checkPolarisation(sourceProduct, slaveTag, pol)) {
                polarisations.add(pol);
            }
        }

        if (polarisations.size() > 0) {
            return polarisations.toArray(new String[0]);
        } else {
            return new String[]{""};
        }
    }

    private static boolean checkPolarisation(final Product product, final String tag, final String polarisation) {

        for (String name:product.getBandNames()) {
            if (name.toLowerCase().contains(tag.toLowerCase()) &&
                    name.toLowerCase().contains(polarisation.toLowerCase())) {
                return true;
            }
        }
        return false;
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

    private void constructFlatEarthPolynomials() throws Exception {

        for (String keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (String keySlave : slaveMap.keySet()) {

                CplxContainer slave = slaveMap.get(keySlave);

                flatEarthPolyMap.put(slave.name, estimateFlatEarthPolynomial(
                        master.metaData, master.orbit, slave.metaData, slave.orbit, sourceImageWidth,
                        sourceImageHeight, srpPolynomialDegree, srpNumberPoints, sourceProduct));
            }
        }
    }

    private void constructFlatEarthPolynomialsForTOPSARProduct() throws Exception {

        for (String keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (String keySlave : slaveMap.keySet()) {

                CplxContainer slave = slaveMap.get(keySlave);

                for (int s = 0; s < numSubSwaths; s++) {

                    final int numBursts = subSwath[s].numOfBursts;

                    for (int b = 0; b < numBursts; b++) {

                        final String polynomialName = slave.name + '_' + s + '_' + b;

                        flatEarthPolyMap.put(polynomialName, estimateFlatEarthPolynomial(
                                master, slave, s + 1, b, mstSceneCentreXYZ, orbitDegree, srpPolynomialDegree,
                                srpNumberPoints, subSwath, su));
                    }
                }
            }
        }
    }

    private void constructTargetMetadata() {

        for (String keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (String keySlave : slaveMap.keySet()) {
                final CplxContainer slave = slaveMap.get(keySlave);

                if (master.polarisation == null || master.polarisation.equals(slave.polarisation)) {
                    // generate name for product bands
                    final String productName = keyMaster + '_' + keySlave;

                    final ProductContainer product = new ProductContainer(productName, master, slave, true);

                    // put ifg-product bands into map
                    targetMap.put(productName, product);
                }
            }
        }
    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "mst";
        final String slaveTag = "slv";

        // get sourceMaster & sourceSlave MetadataElement
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;

        // organize metadata
        // put sourceMaster metadata into the masterMap
        metaMapPut(masterTag, mstRoot, sourceProduct, masterMap);

        // put sourceSlave metadata into slaveMap
        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot);
        if (slaveElem == null) {
            slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
        }
        MetadataElement[] slaveRoot = slaveElem.getElements();
        for (MetadataElement meta : slaveRoot) {
            if (!meta.getName().equals(AbstractMetadata.ORIGINAL_PRODUCT_METADATA))
                metaMapPut(slaveTag, meta, sourceProduct, slaveMap);
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
                final Orbit orbit = new Orbit(root, orbitDegree);

                // TODO: resolve multilook factors
                meta.setMlAz(1);
                meta.setMlRg(1);

                Band bandReal = null;
                Band bandImag = null;
                for (String bandName : product.getBandNames()) {
                    if (tag == "mst" && bandName.contains(tag) || (bandName.contains(tag) && bandName.contains(date))) {
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

    private void createTargetProduct() throws Exception {

        // construct target product
        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        for (String key : targetMap.keySet()) {
            final List<String> targetBandNames = new ArrayList<>();

            final ProductContainer container = targetMap.get(key);
            final CplxContainer master = container.sourceMaster;
            final CplxContainer slave = container.sourceSlave;

            final String subswath = master.subswath.isEmpty() ? "" : '_' + master.subswath.toUpperCase();
            final String pol = getPolarisationTag(master);
            final String tag = subswath + pol + '_' + master.date + '_' + slave.date;
            final String targetBandName_I = "i_" + productTag + tag;
            final Band iBand = targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT32);
            container.addBand(Unit.REAL, iBand.getName());
            iBand.setUnit(Unit.REAL);
            targetBandNames.add(iBand.getName());

            final String targetBandName_Q = "q_" + productTag + tag;
            final Band qBand = targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT32);
            container.addBand(Unit.IMAGINARY, qBand.getName());
            qBand.setUnit(Unit.IMAGINARY);
            targetBandNames.add(qBand.getName());

            if (CREATE_VIRTUAL_BAND) {
                final String countStr = '_' + productTag + tag;
                ReaderUtils.createVirtualIntensityBand(targetProduct,
                        targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);

                Band phaseBand = ReaderUtils.createVirtualPhaseBand(targetProduct,
                        targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);

                targetProduct.setQuicklookBandName(phaseBand.getName());
                phaseBand.setNoDataValueUsed(true);
                targetBandNames.add(phaseBand.getName());
            }

            if (includeCoherence) {
                final String targetBandCoh = "coh" + tag;
                final Band coherenceBand = targetProduct.addBand(targetBandCoh, ProductData.TYPE_FLOAT32);
                coherenceBand.setNoDataValueUsed(true);
                coherenceBand.setNoDataValue(master.realBand.getNoDataValue());
                container.addBand(COHERENCE, coherenceBand.getName());
                coherenceBand.setUnit(Unit.COHERENCE);
                targetBandNames.add(coherenceBand.getName());
            }

            if (subtractTopographicPhase && OUTPUT_PHASE) {
                final String targetBandTgp = "tgp" + tag;
                final Band tgpBand = targetProduct.addBand(targetBandTgp, ProductData.TYPE_FLOAT32);
                container.addBand(TOPO_PHASE, tgpBand.getName());
                tgpBand.setUnit(Unit.PHASE);
                targetBandNames.add(tgpBand.getName());
            }

            if (subtractFlatEarthPhase && OUTPUT_PHASE) {
                final String targetBandFep = "fep" + tag;
                final Band fepBand = targetProduct.addBand(targetBandFep, ProductData.TYPE_FLOAT32);
                container.addBand(FLAT_EARTH_PHASE, fepBand.getName());
                fepBand.setUnit(Unit.PHASE);
                targetBandNames.add(fepBand.getName());
            }

            if (subtractTopographicPhase && outputElevation && targetProduct.getBand("elevation") == null) {
                final Band elevBand = targetProduct.addBand("elevation", ProductData.TYPE_FLOAT32);
                elevBand.setNoDataValueUsed(true);
                elevBand.setNoDataValue(demNoDataValue);
                container.addBand(ELEVATION, elevBand.getName());
                elevBand.setUnit(Unit.METERS);
                targetBandNames.add(elevBand.getName());
            }

            if (subtractTopographicPhase && outputLatLon && targetProduct.getBand("orthorectifiedLat") == null) {
                // add latitude band
                final Band latBand = targetProduct.addBand("orthorectifiedLat", ProductData.TYPE_FLOAT32);
                latBand.setNoDataValueUsed(true);
                latBand.setNoDataValue(Double.NaN);
                container.addBand(LATITUDE, latBand.getName());
                latBand.setUnit(Unit.DEGREES);
                targetBandNames.add(latBand.getName());
            }

            if (subtractTopographicPhase && outputLatLon && targetProduct.getBand("orthorectifiedLon") == null) {
                // add longitude band
                final Band lonBand = targetProduct.addBand("orthorectifiedLon", ProductData.TYPE_FLOAT32);
                lonBand.setNoDataValueUsed(true);
                lonBand.setNoDataValue(Double.NaN);
                container.addBand(LONGITUDE, lonBand.getName());
                lonBand.setUnit(Unit.DEGREES);
                targetBandNames.add(lonBand.getName());
            }

            String slvProductName = StackUtils.findOriginalSlaveProductName(sourceProduct, container.sourceSlave.realBand);
            StackUtils.saveSlaveProductBandNames(targetProduct, slvProductName,
                                                 targetBandNames.toArray(new String[0]));
        }

        for(String bandName : sourceProduct.getBandNames()) {
            if(bandName.startsWith("elevation")) {
                ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
            }
        }
    }

    static String getPolarisationTag(final CplxContainer master) {
        return (master.polarisation == null || master.polarisation.isEmpty()) ? "" : '_' + master.polarisation.toUpperCase();
    }

    public static DoubleMatrix estimateFlatEarthPolynomial(
            final SLCImage masterMetadata, final Orbit masterOrbit, final SLCImage slaveMetadata,
            final Orbit slaveOrbit, final int sourceImageWidth, final int sourceImageHeight,
            final int srpPolynomialDegree, final int srpNumberPoints, final Product sourceProduct)
            throws Exception {

        long minLine = 0;
        long maxLine = sourceImageHeight;
        long minPixel = 0;
        long maxPixel = sourceImageWidth;

        int numberOfCoefficients = PolyUtils.numberOfCoefficients(srpPolynomialDegree);

        int[][] position = MathUtils.distributePoints(srpNumberPoints, new Window(minLine, maxLine, minPixel, maxPixel));

        // setup observation and design matrix
        DoubleMatrix y = new DoubleMatrix(srpNumberPoints);
        DoubleMatrix A = new DoubleMatrix(srpNumberPoints, numberOfCoefficients);

        double masterMinPi4divLam = (-4 * Math.PI * org.jlinda.core.Constants.SOL) / masterMetadata.getRadarWavelength();
        double slaveMinPi4divLam = (-4 * Math.PI * org.jlinda.core.Constants.SOL) / slaveMetadata.getRadarWavelength();
        final boolean isBiStaticStack = StackUtils.isBiStaticStack(sourceProduct);

        // Loop through vector or distributedPoints()
        for (int i = 0; i < srpNumberPoints; ++i) {

            double line = position[i][0];
            double pixel = position[i][1];

            // compute azimuth/range time for this pixel
            final double masterTimeRange = masterMetadata.pix2tr(pixel + 1);

            // compute xyz of this point : sourceMaster
            org.jlinda.core.Point xyzMaster = masterOrbit.lp2xyz(line + 1, pixel + 1, masterMetadata);
            org.jlinda.core.Point slaveTimeVector = slaveOrbit.xyz2t(xyzMaster, slaveMetadata);

            double slaveTimeRange;
            if (isBiStaticStack) {
                slaveTimeRange = 0.5 * (slaveTimeVector.x + masterTimeRange);
            } else {
                slaveTimeRange = slaveTimeVector.x;
            }

            // observation vector
            y.put(i, (masterMinPi4divLam * masterTimeRange) - (slaveMinPi4divLam * slaveTimeRange));

            // set up a system of equations
            // ______Order unknowns: A00 A10 A01 A20 A11 A02 A30 A21 A12 A03 for degree=3______
            double posL = PolyUtils.normalize2(line, minLine, maxLine);
            double posP = PolyUtils.normalize2(pixel, minPixel, maxPixel);

            int index = 0;

            for (int j = 0; j <= srpPolynomialDegree; j++) {
                for (int k = 0; k <= j; k++) {
                    A.put(i, index, (FastMath.pow(posL, (double) (j - k)) * FastMath.pow(posP, (double) k)));
                    index++;
                }
            }
        }

        // Fit polynomial through computed vector of phases
        DoubleMatrix Atranspose = A.transpose();
        DoubleMatrix N = Atranspose.mmul(A);
        DoubleMatrix rhs = Atranspose.mmul(y);

        return Solve.solve(N, rhs);
    }

    /**
     * Create a flat earth phase polynomial for a given burst in TOPSAR product.
     */
    public static DoubleMatrix estimateFlatEarthPolynomial(
            final CplxContainer master, final CplxContainer slave, final int subSwathIndex, final int burstIndex,
            final Point[] mstSceneCentreXYZ, final int orbitDegree, final int srpPolynomialDegree,
            final int srpNumberPoints, final Sentinel1Utils.SubSwathInfo[] subSwath, final Sentinel1Utils su)
            throws Exception {

        final double[][] masterOSV = getAdjacentOrbitStateVectors(master, mstSceneCentreXYZ[burstIndex]);
        final double[][] slaveOSV = getAdjacentOrbitStateVectors(slave, mstSceneCentreXYZ[burstIndex]);
        final Orbit masterOrbit = new Orbit(masterOSV, orbitDegree);
        final Orbit slaveOrbit = new Orbit(slaveOSV, orbitDegree);

        long minLine = 0;
        long maxLine = subSwath[subSwathIndex - 1].linesPerBurst - 1;
        long minPixel = 0;
        long maxPixel = subSwath[subSwathIndex - 1].samplesPerBurst - 1;

        int numberOfCoefficients = PolyUtils.numberOfCoefficients(srpPolynomialDegree);

        int[][] position = MathUtils.distributePoints(srpNumberPoints, new Window(minLine, maxLine, minPixel, maxPixel));

        // setup observation and design matrix
        DoubleMatrix y = new DoubleMatrix(srpNumberPoints);
        DoubleMatrix A = new DoubleMatrix(srpNumberPoints, numberOfCoefficients);

        double masterMinPi4divLam = (-4 * Constants.PI * Constants.lightSpeed) / master.metaData.getRadarWavelength();
        double slaveMinPi4divLam = (-4 * Constants.PI * Constants.lightSpeed) / slave.metaData.getRadarWavelength();

        // Loop through vector or distributedPoints()
        for (int i = 0; i < srpNumberPoints; ++i) {

            double line = position[i][0];
            double pixel = position[i][1];

            // compute azimuth/range time for this pixel
            final double mstRgTime = subSwath[subSwathIndex - 1].slrTimeToFirstPixel +
                    pixel * su.rangeSpacing / Constants.lightSpeed;

            final double mstAzTime = line2AzimuthTime(line, subSwathIndex, burstIndex, subSwath);

            // compute xyz of this point : sourceMaster
            Point xyzMaster = masterOrbit.lph2xyz(
                    mstAzTime, mstRgTime, 0.0, mstSceneCentreXYZ[burstIndex]);

            Point slaveTimeVector = slaveOrbit.xyz2t(xyzMaster, slave.metaData.getSceneCentreAzimuthTime());

            final double slaveTimeRange = slaveTimeVector.x;

            // observation vector
            y.put(i, (masterMinPi4divLam * mstRgTime) - (slaveMinPi4divLam * slaveTimeRange));

            // set up a system of equations
            // ______Order unknowns: A00 A10 A01 A20 A11 A02 A30 A21 A12 A03 for degree=3______
            double posL = PolyUtils.normalize2(line, minLine, maxLine);
            double posP = PolyUtils.normalize2(pixel, minPixel, maxPixel);

            int index = 0;

            for (int j = 0; j <= srpPolynomialDegree; j++) {
                for (int k = 0; k <= j; k++) {
                    A.put(i, index, (FastMath.pow(posL, (double) (j - k)) * FastMath.pow(posP, (double) k)));
                    index++;
                }
            }
        }

        // Fit polynomial through computed vector of phases
        DoubleMatrix Atranspose = A.transpose();
        DoubleMatrix N = Atranspose.mmul(A);
        DoubleMatrix rhs = Atranspose.mmul(y);

        return Solve.solve(N, rhs);
    }

    private static double[][] getAdjacentOrbitStateVectors(
            final CplxContainer container, final Point sceneCentreXYZ) {

        try {
            double[] time = container.orbit.getTime();
            double[] dataX = container.orbit.getData_X();
            double[] dataY = container.orbit.getData_Y();
            double[] dataZ = container.orbit.getData_Z();

            final int numOfOSV = dataX.length;
            double minDistance = 0.0;
            int minIdx = 0;
            for (int i = 0; i < numOfOSV; i++) {
                final double dx = dataX[i] - sceneCentreXYZ.x;
                final double dy = dataY[i] - sceneCentreXYZ.y;
                final double dz = dataZ[i] - sceneCentreXYZ.z;
                final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) / 1000.0;
                if (i == 0) {
                    minDistance = distance;
                    minIdx = i;
                    continue;
                }

                if (distance < minDistance) {
                    minDistance = distance;
                    minIdx = i;
                }
            }

            int stIdx, edIdx;
            if (minIdx < 3) {
                stIdx = 0;
                edIdx = Math.min(7, numOfOSV - 1);
            } else if (minIdx > numOfOSV - 5) {
                stIdx = Math.max(numOfOSV - 8, 0);
                edIdx = numOfOSV - 1;
            } else {
                stIdx = minIdx - 3;
                edIdx = minIdx + 4;
            }

            final double[][] adjacentOSV = new double[edIdx - stIdx + 1][4];
            int k = 0;
            for (int i = stIdx; i <= edIdx; i++) {
                adjacentOSV[k][0] = time[i];
                adjacentOSV[k][1] = dataX[i];
                adjacentOSV[k][2] = dataY[i];
                adjacentOSV[k][3] = dataZ[i];
                k++;
            }

            return adjacentOSV;
        } catch (Throwable e) {
            SystemUtils.LOG.warning("Unable to getAdjacentOrbitStateVectors " + e.getMessage());
        }
        return null;
    }

    private static double line2AzimuthTime(final double line, final int subSwathIndex, final int burstIndex,
                                           final Sentinel1Utils.SubSwathInfo[] subSwath) {

        final double firstLineTimeInDays = subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex] /
                Constants.secondsInDay;

        final double firstLineTime = (firstLineTimeInDays - (int) firstLineTimeInDays) * Constants.secondsInDay;

        return firstLineTime + line * subSwath[subSwathIndex - 1].azimuthTimeInterval;
    }

    private synchronized void estimateFlatEarth() throws OperatorException {
        if(flatEarthEstimated)
            return;
        if (subtractFlatEarthPhase) {
            try {
                if (isTOPSARBurstProduct) {

                    getMstApproxSceneCentreXYZ();
                    constructFlatEarthPolynomialsForTOPSARProduct();
                } else {
                    constructFlatEarthPolynomials();
                }
                flatEarthEstimated = true;
            } catch (Exception e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
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
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
            if (subtractFlatEarthPhase && !flatEarthEstimated) {
                estimateFlatEarth();
            }

            if (isTOPSARBurstProduct) {
                computeTileStackForTOPSARProduct(targetTileMap, targetRectangle, pm);
            } else {
                computeTileStackForNormalProduct(targetTileMap, targetRectangle, pm);
            }
    }

    private void computeTileStackForNormalProduct(
            final Map<Band, Tile> targetTileMap, Rectangle targetRectangle, final ProgressMonitor pm)
            throws OperatorException {
        try {
            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final int y0 = targetRectangle.y;
            final int yN = y0 + targetRectangle.height - 1;
            final int x0 = targetRectangle.x;
            final int xN = targetRectangle.x + targetRectangle.width - 1;
            final Window tileWindow = new Window(y0, yN, x0, xN);

            DemTile demTile = null;
            if (subtractTopographicPhase) {
                demTile = TopoPhase.getDEMTile(tileWindow, targetMap, dem, demNoDataValue,
                        demSamplingLat, demSamplingLon, tileExtensionPercent);

                if (demTile.getData().length < 3 || demTile.getData()[0].length < 3) {
                    throw new OperatorException("The resolution of the selected DEM is too low, " +
                            "please select DEM with higher resolution.");
                }
            }

            // parameters for coherence calculation
            final int cohx0 = targetRectangle.x - (cohWinRg - 1) / 2;
            final int cohy0 = targetRectangle.y - (cohWinAz - 1) / 2;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            final Rectangle rect = new Rectangle(cohx0, cohy0, cohw, cohh);

            final Window cohTileWindow = new Window(
                    cohy0, cohy0 + cohh - 1, cohx0, cohx0 + cohw - 1);

            DemTile cohDemTile = null;
            if (subtractTopographicPhase) {
                cohDemTile = TopoPhase.getDEMTile(cohTileWindow, targetMap, dem, demNoDataValue,
                        demSamplingLat, demSamplingLon, tileExtensionPercent);
            }

            for (String ifgKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(ifgKey);

                final Tile mstTileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle, border);
                final Tile mstTileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle, border);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal, mstTileImag);

                final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle, border);
                final Tile slvTileImag = getSourceTile(product.sourceSlave.imagBand, targetRectangle, border);
                final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal, slvTileImag);

                if (subtractFlatEarthPhase) {
                    final DoubleMatrix flatEarthPhase = computeFlatEarthPhase(
                            x0, xN, dataMaster.columns, y0, yN, dataMaster.rows,
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
                            product, tileWindow, demTile, outputElevation, false);

                    final ComplexDoubleMatrix ComplexTopoPhase = new ComplexDoubleMatrix(
                            MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                            MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                    dataSlave.muli(ComplexTopoPhase);

                    if (OUTPUT_PHASE) {
                        saveTopoPhase(x0, xN, y0, yN, topoPhase.demPhase, product, targetTileMap);
                    }

                    if (outputElevation) {
                        saveElevation(x0, xN, y0, yN, topoPhase.elevation, product, targetTileMap);
                    }

                    if (outputLatLon) {
                        final TopoPhase topoPhase1 = TopoPhase.computeTopoPhase(
                                product, tileWindow, demTile, false, true);

                        saveLatLon(x0, xN, y0, yN, topoPhase1.latitude, topoPhase1.longitude, product, targetTileMap);
                    }
                }

                dataMaster.muli(dataSlave.conji());

                saveInterferogram(dataMaster, product, targetTileMap, targetRectangle);

                // coherence calculation
                if (includeCoherence) {
                    final Tile mstTileReal2 = getSourceTile(product.sourceMaster.realBand, rect, border);
                    final Tile mstTileImag2 = getSourceTile(product.sourceMaster.imagBand, rect, border);
                    final Tile slvTileReal2 = getSourceTile(product.sourceSlave.realBand, rect, border);
                    final Tile slvTileImag2 = getSourceTile(product.sourceSlave.imagBand, rect, border);
                    final ComplexDoubleMatrix dataMaster2 =
                            TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal2, mstTileImag2);

                    final ComplexDoubleMatrix dataSlave2 =
                            TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal2, slvTileImag2);

                    if (subtractFlatEarthPhase) {
                        final DoubleMatrix flatEarthPhase = computeFlatEarthPhase(
                                cohx0, cohx0 + cohw - 1, cohw, cohy0, cohy0 + cohh - 1, cohh,
                                0, sourceImageWidth - 1, 0, sourceImageHeight - 1, product.sourceSlave.name);

                        final ComplexDoubleMatrix complexReferencePhase = new ComplexDoubleMatrix(
                                MatrixFunctions.cos(flatEarthPhase), MatrixFunctions.sin(flatEarthPhase));

                        dataSlave2.muli(complexReferencePhase);
                    }

                    if (subtractTopographicPhase) {
                        final TopoPhase topoPhase = TopoPhase.computeTopoPhase(
                                product, cohTileWindow, cohDemTile, false);

                        final ComplexDoubleMatrix ComplexTopoPhase = new ComplexDoubleMatrix(
                                MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                                MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                        dataSlave2.muli(ComplexTopoPhase);
                    }

                    for (int i = 0; i < dataMaster2.length; i++) {
                        double tmp = norm(dataMaster2.get(i));
                        dataMaster2.put(i, dataMaster2.get(i).mul(dataSlave2.get(i).conj()));
                        dataSlave2.put(i, new ComplexDouble(norm(dataSlave2.get(i)), tmp));
                    }

                    DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster2, dataSlave2, cohWinAz, cohWinRg);

                    saveCoherence(cohMatrix, product, targetTileMap, targetRectangle);
                }
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
        rangeAxisNormalized = normalizeDoubleMatrix(rangeAxisNormalized, minPixel, maxPixel);

        DoubleMatrix azimuthAxisNormalized = DoubleMatrix.linspace(yMin, yMax, ySize);
        azimuthAxisNormalized = normalizeDoubleMatrix(azimuthAxisNormalized, minLine, maxLine);

        final DoubleMatrix polyCoeffs = flatEarthPolyMap.get(polynomialName);

        return PolyUtils.polyval(azimuthAxisNormalized, rangeAxisNormalized,
                polyCoeffs, PolyUtils.degreeFromCoefficients(polyCoeffs.length));
    }

    private void saveElevation(final int x0, final int xN, final int y0, final int yN, final double[][] elevation,
                               final ProductContainer product, final Map<Band, Tile> targetTileMap) {
        if (product.getBandName(ELEVATION) == null) {
            return;
        }
        final Band elevationBand = targetProduct.getBand(product.getBandName(ELEVATION));
        final Tile elevationTile = targetTileMap.get(elevationBand);
        final ProductData elevationData = elevationTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(elevationTile);
        for (int y = y0; y <= yN; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x <= xN; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0;
                elevationData.setElemFloatAt(tgtIdx, (float)elevation[yy][xx]);
            }
        }
    }

    private void saveLatLon(final int x0, final int xN, final int y0, final int yN,
                            final double[][] latitude, final double[][] longitude,
                            final ProductContainer product, final Map<Band, Tile> targetTileMap) {

        if (product.getBandName(LATITUDE) == null || product.getBandName(LONGITUDE) == null) {
            return;
        }

        final Band latBand = targetProduct.getBand(product.getBandName(LATITUDE));
        final Tile latTile = targetTileMap.get(latBand);
        final ProductData latData = latTile.getDataBuffer();
        final Band lonBand = targetProduct.getBand(product.getBandName(LONGITUDE));
        final Tile lonTile = targetTileMap.get(lonBand);
        final ProductData lonData = lonTile.getDataBuffer();

        final TileIndex tgtIndex = new TileIndex(latTile);

        for (int y = y0; y <= yN; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x <= xN; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0;

                latData.setElemFloatAt(tgtIdx, (float) (latitude[yy][xx] * 180.0/Math.PI));
                lonData.setElemFloatAt(tgtIdx, (float) (longitude[yy][xx] * 180.0/Math.PI));
            }
        }
    }

    private void saveTopoPhase(final int x0, final int xN, final int y0, final int yN, final double[][] topoPhase,
                               final ProductContainer product, final Map<Band, Tile> targetTileMap) {

        final Band topoPhaseBand = targetProduct.getBand(product.getBandName(TOPO_PHASE));
        final Tile topoPhaseTile = targetTileMap.get(topoPhaseBand);
        final ProductData topoPhaseData = topoPhaseTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(topoPhaseTile);

        for (int y = y0; y <= yN; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x <= xN; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0;
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
            final int yy = y - y0;
            for (int x = x0; x <= xN; x++) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final int xx = x - x0;
                flatEarthPhaseData.setElemFloatAt(tgtIdx, (float)refPhase.get(yy, xx));
            }
        }
    }

    // Save flat-earth phase in [-PI, PI]
//    private void saveFlatEarthPhase(final int x0, final int xN, final int y0, final int yN, final ComplexDoubleMatrix complexReferencePhase,
//                                    final ProductContainer product, final Map<Band, Tile> targetTileMap) {
//
//        final Band flatEarthPhaseBand = targetProduct.getBand(product.getBandName(FLAT_EARTH_PHASE));
//        final Tile flatEarthPhaseTile = targetTileMap.get(flatEarthPhaseBand);
//        final ProductData flatEarthPhaseData = flatEarthPhaseTile.getDataBuffer();
//
//        final TileIndex tgtIndex = new TileIndex(flatEarthPhaseTile);
//        for (int y = y0; y <= yN; y++) {
//            tgtIndex.calculateStride(y);
//            final int yy = y - y0;
//            for (int x = x0; x <= xN; x++) {
//                final int tgtIdx = tgtIndex.getIndex(x);
//                final int xx = x - x0;
//                final double real = complexReferencePhase.get(yy, xx).real();
//                final double imag = complexReferencePhase.get(yy, xx).imag();
//                flatEarthPhaseData.setElemFloatAt(tgtIdx, (float)Math.atan2(imag, real));
//            }
//        }
//    }

    private void saveInterferogram(final ComplexDoubleMatrix dataMaster, final ProductContainer product,
                                   final Map<Band, Tile> targetTileMap, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int maxX = x0 + targetRectangle.width;
        final int maxY = y0 + targetRectangle.height;
        final Band targetBand_I = targetProduct.getBand(product.getBandName(Unit.REAL));
        final Tile tileOutReal = targetTileMap.get(targetBand_I);

        final Band targetBand_Q = targetProduct.getBand(product.getBandName(Unit.IMAGINARY));
        final Tile tileOutImag = targetTileMap.get(targetBand_Q);

        final ProductData samplesReal = tileOutReal.getDataBuffer();
        final ProductData samplesImag = tileOutImag.getDataBuffer();
        final DoubleMatrix dataReal = dataMaster.real();
        final DoubleMatrix dataImag = dataMaster.imag();
        final TileIndex tgtIndex = new TileIndex(tileOutReal);

        final Tile mstRealTile = getSourceTile(product.sourceMaster.realBand, targetRectangle);
        final ProductData mstRealData = mstRealTile.getDataBuffer();
        final TileIndex srcIndexMst = new TileIndex(mstRealTile);

        final Tile slvRealTile = getSourceTile(product.sourceSlave.realBand, targetRectangle);
        final ProductData slvRealData = slvRealTile.getDataBuffer();
        final TileIndex srcIndexSlv = new TileIndex(slvRealTile);

        final boolean mstNoDataValueUsed = product.sourceMaster.realBand.isNoDataValueUsed();
        final boolean slvNoDataValueUsed = product.sourceSlave.realBand.isNoDataValueUsed();

        if (mstNoDataValueUsed || slvNoDataValueUsed) {

            double mstNoDataValue = 0.0, slvNoDataValue = 0.0;
            if (mstNoDataValueUsed) {
                mstNoDataValue = product.sourceMaster.realBand.getNoDataValue();
            }
            if (slvNoDataValueUsed) {
                slvNoDataValue = product.sourceSlave.realBand.getNoDataValue();
            }

            for (int y = y0; y < maxY; y++) {
                tgtIndex.calculateStride(y);
                srcIndexMst.calculateStride(y);
                srcIndexSlv.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < maxX; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int xx = x - x0;
                    final int srcIdxMst = srcIndexMst.getIndex(x);
                    final int srcIdxSlv = srcIndexSlv.getIndex(x);

                    if (mstNoDataValueUsed && mstRealData.getElemDoubleAt(srcIdxMst) == mstNoDataValue ||
                            slvNoDataValueUsed && slvRealData.getElemDoubleAt(srcIdxSlv) == slvNoDataValue) {
                        samplesReal.setElemFloatAt(tgtIdx, (float) mstNoDataValue);
                        samplesImag.setElemFloatAt(tgtIdx, (float) mstNoDataValue);
                    } else {
                        samplesReal.setElemFloatAt(tgtIdx, (float) dataReal.get(yy, xx));
                        samplesImag.setElemFloatAt(tgtIdx, (float) dataImag.get(yy, xx));
                    }
                }
            }

        } else {

            for (int y = y0; y < maxY; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < maxX; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int xx = x - x0;
                    samplesReal.setElemFloatAt(tgtIdx, (float) dataReal.get(yy, xx));
                    samplesImag.setElemFloatAt(tgtIdx, (float) dataImag.get(yy, xx));
                }
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

    private static double norm(final ComplexDouble number) {
        return number.real() * number.real() + number.imag() * number.imag();
    }

    private void computeTileStackForTOPSARProduct(
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
                                    final Map<Band, Tile> targetTileMap) {

        try {
            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final int y0 = targetRectangle.y;
            final int yN = y0 + targetRectangle.height - 1;
            final int x0 = targetRectangle.x;
            final int xN = x0 + targetRectangle.width - 1;

            final Window tileWindow = new Window(y0 - firstLineIdx, yN - firstLineIdx, x0, xN);
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

            final int cohx0 = targetRectangle.x - (cohWinRg - 1) / 2;
            final int cohy0 = targetRectangle.y - (cohWinAz - 1) / 2;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            final Rectangle rect = new Rectangle(cohx0, cohy0, cohw, cohh);

            final Window cohTileWindow = new Window(
                    cohy0 - firstLineIdx, cohy0 + cohh - 1 - firstLineIdx, cohx0, cohx0 + cohw - 1);

            DemTile cohDemTile = null;
            if (subtractTopographicPhase) {
                cohDemTile = TopoPhase.getDEMTile(cohTileWindow, mstMeta, mstOrbit, dem,
                        demNoDataValue, demSamplingLat, demSamplingLon, tileExtensionPercent);
            }

            final int minLine = 0;
            final int maxLine = subSwath[subSwathIndex - 1].linesPerBurst - 1;
            final int minPixel = 0;
            final int maxPixel = subSwath[subSwathIndex - 1].samplesPerBurst - 1;

            for (String ifgKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(ifgKey);
                final SLCImage slvMeta = product.sourceSlave.metaData.clone();
                updateSlvMetaData(product, burstIndex, slvMeta);
                final Orbit slvOrbit = product.sourceSlave.orbit;

                /// check out results from master ///
                final Tile mstTileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle, border);
                final Tile mstTileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle, border);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal, mstTileImag);

                /// check out results from slave ///
                final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle, border);
                final Tile slvTileImag = getSourceTile(product.sourceSlave.imagBand, targetRectangle, border);
                final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal, slvTileImag);

                final String polynomialName = product.sourceSlave.name + '_' + (subSwathIndex - 1) + '_' + burstIndex;
                if (subtractFlatEarthPhase) {
                    final DoubleMatrix flatEarthPhase = computeFlatEarthPhase(
                            x0, xN, dataMaster.columns, y0 - firstLineIdx, yN - firstLineIdx, dataMaster.rows,
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
                            mstMeta, mstOrbit, slvMeta, slvOrbit, tileWindow, demTile, outputElevation, false);

                    final ComplexDoubleMatrix ComplexTopoPhase = new ComplexDoubleMatrix(
                            MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                            MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                    dataSlave.muli(ComplexTopoPhase);

                    if (OUTPUT_PHASE) {
                        saveTopoPhase(x0, xN, y0, yN, topoPhase.demPhase, product, targetTileMap);
                    }

                    if (outputElevation) {
                        saveElevation(x0, xN, y0, yN, topoPhase.elevation, product, targetTileMap);
                    }

                    if (outputLatLon) {
                        TopoPhase topoPhase1 = TopoPhase.computeTopoPhase(
                                mstMeta, mstOrbit, slvMeta, slvOrbit, tileWindow, demTile, false, true);

                        saveLatLon(x0, xN, y0, yN, topoPhase1.latitude, topoPhase1.longitude, product, targetTileMap);
                    }
                }

                dataMaster.muli(dataSlave.conji());

                saveInterferogram(dataMaster, product, targetTileMap, targetRectangle);

                // coherence calculation
                if (includeCoherence) {
                    final Tile mstTileReal2 = getSourceTile(product.sourceMaster.realBand, rect, border);
                    final Tile mstTileImag2 = getSourceTile(product.sourceMaster.imagBand, rect, border);
                    final Tile slvTileReal2 = getSourceTile(product.sourceSlave.realBand, rect, border);
                    final Tile slvTileImag2 = getSourceTile(product.sourceSlave.imagBand, rect, border);
                    final ComplexDoubleMatrix dataMaster2 =
                            TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal2, mstTileImag2);

                    final ComplexDoubleMatrix dataSlave2 =
                            TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal2, slvTileImag2);

                    if (subtractFlatEarthPhase) {
                        final DoubleMatrix flatEarthPhase = computeFlatEarthPhase(
                                cohx0, cohx0 + cohw - 1, cohw, cohy0 - firstLineIdx, cohy0 + cohh - 1 - firstLineIdx, cohh,
                                minPixel, maxPixel, minLine, maxLine, polynomialName);

                        final ComplexDoubleMatrix complexReferencePhase = new ComplexDoubleMatrix(
                                MatrixFunctions.cos(flatEarthPhase), MatrixFunctions.sin(flatEarthPhase));

                        dataSlave2.muli(complexReferencePhase);
                    }

                    if (subtractTopographicPhase) {
                        TopoPhase topoPhase = TopoPhase.computeTopoPhase(
                                mstMeta, mstOrbit, slvMeta, slvOrbit, cohTileWindow, cohDemTile, false);

                        final ComplexDoubleMatrix ComplexTopoPhase = new ComplexDoubleMatrix(
                                MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                                MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                        dataSlave2.muli(ComplexTopoPhase);
                    }

                    for (int i = 0; i < dataMaster2.length; i++) {
                        double tmp = norm(dataMaster2.get(i));
                        dataMaster2.put(i, dataMaster2.get(i).mul(dataSlave2.get(i).conj()));
                        dataSlave2.put(i, new ComplexDouble(norm(dataSlave2.get(i)), tmp));
                    }

                    DoubleMatrix cohMatrix = SarUtils.coherence2(dataMaster2, dataSlave2, cohWinAz, cohWinRg);

                    saveCoherence(cohMatrix, product, targetTileMap, targetRectangle);
                }
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

        mstMeta.setCurrentWindow(new Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));

        mstMeta.setOriginalWindow(new Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));

        mstMeta.setApproxGeoCentreOriginal(getApproxGeoCentre(subSwathIndex, burstIndex));
    }

    private void updateSlvMetaData(final ProductContainer product, final int burstIndex, final SLCImage slvMeta) {

        final double slvBurstFirstLineTimeMJD = slvMeta.getMjd() - product.sourceMaster.metaData.getMjd() +
                subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex] / Constants.secondsInDay;

        final double slvBurstFirstLineTimeSecondsOfDay = (slvBurstFirstLineTimeMJD - (int)slvBurstFirstLineTimeMJD) *
                Constants.secondsInDay;

        slvMeta.settAzi1(slvBurstFirstLineTimeSecondsOfDay);

        slvMeta.setCurrentWindow(new Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
                0, subSwath[subSwathIndex - 1].samplesPerBurst - 1));

        slvMeta.setOriginalWindow(new Window(0, subSwath[subSwathIndex - 1].linesPerBurst - 1,
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

    public static DoubleMatrix normalizeDoubleMatrix(DoubleMatrix matrix, final double min, final double max) {
        matrix.subi(0.5 * (min + max));
        matrix.divi(0.25 * (max - min));
        return matrix;
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
            super(InterferogramOp.class);
        }
    }

}
