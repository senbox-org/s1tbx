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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.TileIndex;
import org.jblas.*;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.PolyUtils;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


@OperatorMetadata(alias = "Interferogram",
        category = "SAR Processing/Interferometric/Products",
        authors = "Petar Marinkovic, Jun Lu",
        description = "Compute interferograms from stack of coregistered S-1 images", internal = false)
public class CreateInterferogramOp extends Operator {
    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue="true", label="Subtract flat-earth phase from interferogram")
    private boolean subtractFlatEarthPhase = true;

    @Parameter(valueSet = {"1", "2", "3", "4", "5", "6", "7", "8"},
            description = "Order of 'Flat earth phase' polynomial",
            defaultValue = "5",
            label = "Degree of \"Flat Earth\" polynomial")
    private int srpPolynomialDegree = 5;

    @Parameter(valueSet = {"301", "401", "501", "601", "701", "801", "901", "1001"},
            description = "Number of points for the 'flat earth phase' polynomial estimation",
            defaultValue = "501",
            label = "Number of 'Flat earth' estimation points")
    private int srpNumberPoints = 501;

    @Parameter(valueSet = {"1", "2", "3", "4", "5"},
            description = "Degree of orbit (polynomial) interpolator",
            defaultValue = "3",
            label = "Orbit interpolation degree")
    private int orbitDegree = 3;

    @Parameter(defaultValue="true", label="Include coherence estimation")
    private boolean includeCoherence = true;

    @Parameter(interval = "(1, 40]",
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "10",
            label = "Coherence Azimuth Window Size")
    private int cohWinAz = 10;

    @Parameter(interval = "(1, 40]",
            description = "Size of coherence estimation window in Range direction",
            defaultValue = "10",
            label = "Coherence Range Window Size")
    private int cohWinRg = 10;

    // flat_earth_polynomial container
    private HashMap<String, DoubleMatrix> flatEarthPolyMap = new HashMap<>();

    // source
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<>();

    // target
    private HashMap<String, ProductContainer> targetMap = new HashMap<>();

    // operator tags
    private static final boolean CREATE_VIRTUAL_BAND = true;
    private String productName;
    public String productTag;
    private int sourceImageWidth;
    private int sourceImageHeight;

    private boolean isTOPSARBurstProduct = false;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int numSubSwaths = 0;
    private org.jlinda.core.Point mstSceneCentreXYZ = null;
    private double slvScenseCentreAzimuthTime = 0.0;
    private int subSwathIndex = 0;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            // rename product if no subtraction of the flat-earth phase
            if (!subtractFlatEarthPhase) {
                productName = "ifgs";
                productTag = "ifg";
            } else {
                productName = "srp_ifgs";
                productTag = "ifg_srp";
            }

            checkUserInput();

            constructSourceMetadata();
            constructTargetMetadata();
            createTargetProduct();

            getSourceImageDimension();

            if (subtractFlatEarthPhase) {

                if (isTOPSARBurstProduct) {

                    getMstApproxSceneCentreXYZ();
                    getSlvApproxSceneCentreAzimuthTime();
                    constructFlatEarthPolynomialsForTOPSARProduct();
                } else {
                    constructFlatEarthPolynomials();
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkUserInput() {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfCoregisteredStack();
            isTOPSARBurstProduct = validator.isTOPSARBurstProduct();

            if (isTOPSARBurstProduct) {
                su = new Sentinel1Utils(sourceProduct);
                subSwath = su.getSubSwath();
                numSubSwaths = su.getNumOfSubSwath();
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product

                final String topsarTag = getTOPSARTag(sourceProduct);
                productTag = productTag + "_" + topsarTag;
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    public static String getTOPSARTag(final Product sourceProduct) {

        final Band[] bands = sourceProduct.getBands();
        for (Band band:bands) {
            final String bandName = band.getName();
            if (bandName.contains("i_") && bandName.contains("_mst")) {
                return bandName.substring(bandName.indexOf("i_")+2, bandName.indexOf("_mst"));
            }
        }
        return "";
    }

    private void getMstApproxSceneCentreXYZ() throws Exception {

        final MetadataElement mstRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        final double firstNearLat = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.first_near_lat);
        final double firstFarLat = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.first_far_lat);
        final double lastNearLat = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.last_near_lat);
        final double lastFarLat = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.last_far_lat);

        final double firstNearLon = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.first_near_long);
        final double firstFarLon = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.first_far_long);
        final double lastNearLon = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.last_near_long);
        final double lastFarLon = AbstractMetadata.getAttributeDouble(mstRoot, AbstractMetadata.last_far_long);

        final double lat = (firstNearLat + firstFarLat + lastNearLat + lastFarLat) / 4.0;
        final double lon = (firstNearLon + firstFarLon + lastNearLon + lastFarLon) / 4.0;

        final double[] mstSceneCenter = new double[3];
        GeoUtils.geo2xyzWGS84(lat, lon, 0.0, mstSceneCenter);
        mstSceneCentreXYZ = new Point(mstSceneCenter);
    }

    private void getSlvApproxSceneCentreAzimuthTime() throws Exception {

        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        MetadataElement[] slaveRoot = slaveElem.getElements();
        final MetadataElement slvRoot = slaveRoot[0];

        final double firstLineTimeInDays = slvRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD();
        final double firstLineTime = (firstLineTimeInDays - (int)firstLineTimeInDays) * Constants.secondsInDay;
        final double lastLineTimeInDays = slvRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD();
        final double lastLineTime = (lastLineTimeInDays - (int)lastLineTimeInDays) * Constants.secondsInDay;

        slvScenseCentreAzimuthTime = 0.5*(firstLineTime + lastLineTime);
    }

    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    private void constructFlatEarthPolynomials() throws Exception {

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                CplxContainer slave = slaveMap.get(keySlave);

                flatEarthPolyMap.put(slave.name, estimateFlatEarthPolynomial(
                        master.metaData, master.orbit, slave.metaData, slave.orbit));
            }
        }
    }

    private void constructFlatEarthPolynomialsForTOPSARProduct() throws Exception {

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                CplxContainer slave = slaveMap.get(keySlave);

                for (int s = 0; s < numSubSwaths; s++) {

                    final int numBursts = subSwath[s].numOfBursts;

                    for (int b = 0; b < numBursts; b++) {

                        final String polynomialName = slave.name + "_" + s + "_" + b;

                        flatEarthPolyMap.put(polynomialName, estimateFlatEarthPolynomial(
                                master.metaData, master.orbit, slave.metaData, slave.orbit, s+1, b));
                    }
                }
            }
        }
    }

    private void constructTargetMetadata() {

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                // generate name for product bands
                final String productName = keyMaster.toString() + "_" + keySlave.toString();

                final CplxContainer slave = slaveMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, master, slave, true);

                product.addBand(Unit.REAL, "i_" + productTag + "_" + master.date + "_" + slave.date);
                product.addBand(Unit.IMAGINARY, "q_" + productTag + "_" + master.date + "_" + slave.date);

                if(includeCoherence) {
                    String cohTag = "coh";
                    if (isTOPSARBurstProduct) {
                        final String topsarTag = getTOPSARTag(sourceProduct);
                        cohTag += "_" + topsarTag;
                    }
                    product.addBand(Unit.COHERENCE, cohTag + "_" + master.date + "_" + slave.date);
                }

                // put ifg-product bands into map
                targetMap.put(productName, product);
            }
        }
    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "mst";
        final String slaveTag = "slv";

        // get sourceMaster & sourceSlave MetadataElement
        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;

        /* organize metadata */
        // put sourceMaster metadata into the masterMap
        metaMapPut(masterTag, masterMeta, sourceProduct, masterMap);

        // pug sourceSlave metadata into slaveMap
        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot);
        if(slaveElem == null) {
            slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
        }
        MetadataElement[] slaveRoot = slaveElem.getElements();
        for (MetadataElement meta : slaveRoot) {
            metaMapPut(slaveTag, meta, sourceProduct, slaveMap);
        }
    }

    private void metaMapPut(final String tag,
                            final MetadataElement root,
                            final Product product,
                            final HashMap<Integer, CplxContainer> map) throws Exception {

        // TODO: include polarization flags/checks!
        // pull out band names for this product
        final String[] bandNames = product.getBandNames();
        final int numOfBands = bandNames.length;

        // map key: ORBIT NUMBER
        int mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT);

        // metadata: construct classes and define bands
        final String date = OperatorUtils.getAcquisitionDate(root);
        final SLCImage meta = new SLCImage(root);
        final Orbit orbit = new Orbit(root, orbitDegree);

        // TODO: resolve multilook factors
        meta.setMlAz(1);
        meta.setMlRg(1);

        Band bandReal = null;
        Band bandImag = null;

        for (int i = 0; i < numOfBands; i++) {
            String bandName = bandNames[i];
            if (bandName.contains(tag) && bandName.contains(date)) {
                final Band band = product.getBandAt(i);
                if (BandUtilsDoris.isBandReal(band)) {
                    bandReal = band;
                } else if (BandUtilsDoris.isBandImag(band)) {
                    bandImag = band;
                }
            }
        }
        try {
            map.put(mapKey, new CplxContainer(date, meta, orbit, bandReal, bandImag));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTargetProduct() {

        // construct target product
        targetProduct = new Product(productName,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : targetMap.keySet()) {

            final String targetBandName_I = targetMap.get(key).getBandName(Unit.REAL);
            final Band iBand = targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT32);
            iBand.setUnit(Unit.REAL);

            final String targetBandName_Q = targetMap.get(key).getBandName(Unit.IMAGINARY);
            final Band qBand = targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT32);
            qBand.setUnit(Unit.IMAGINARY);

            final String tag0 = targetMap.get(key).sourceMaster.date;
            final String tag1 = targetMap.get(key).sourceSlave.date;
            if (CREATE_VIRTUAL_BAND) {
                final String countStr = "_" + productTag + "_" + tag0 + "_" + tag1;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
            }

            if(includeCoherence) {
                final String targetBandCoh = targetMap.get(key).getBandName(Unit.COHERENCE);
                final Band cohBand = targetProduct.addBand(targetBandCoh, ProductData.TYPE_FLOAT32);
                cohBand.setUnit(Unit.COHERENCE);
            }
        }
    }

    private DoubleMatrix estimateFlatEarthPolynomial(
            SLCImage masterMetadata, Orbit masterOrbit, SLCImage slaveMetadata, Orbit slaveOrbit) throws Exception {

        long minLine = 0;
        long maxLine = sourceImageHeight;
        long minPixel = 0;
        long maxPixel = sourceImageWidth;

        int numberOfCoefficients = PolyUtils.numberOfCoefficients(srpPolynomialDegree);

        int[][] position = MathUtils.distributePoints(srpNumberPoints, new Window(minLine,maxLine,minPixel,maxPixel));

        // setup observation and design matrix
        DoubleMatrix y = new DoubleMatrix(srpNumberPoints);
        DoubleMatrix A = new DoubleMatrix(srpNumberPoints, numberOfCoefficients);

        double masterMinPi4divLam = (-4 * Math.PI * org.jlinda.core.Constants.SOL) / masterMetadata.getRadarWavelength();
        double slaveMinPi4divLam = (-4 * Math.PI * org.jlinda.core.Constants.SOL) / slaveMetadata.getRadarWavelength();

        // Loop through vector or distributedPoints()
        for (int i = 0; i < srpNumberPoints; ++i) {

            double line = position[i][0];
            double pixel = position[i][1];

            // compute azimuth/range time for this pixel
            final double masterTimeRange = masterMetadata.pix2tr(pixel + 1);

            // compute xyz of this point : sourceMaster
            org.jlinda.core.Point xyzMaster = masterOrbit.lp2xyz(line + 1, pixel + 1, masterMetadata);
            org.jlinda.core.Point slaveTimeVector = slaveOrbit.xyz2t(xyzMaster, slaveMetadata);

            final double slaveTimeRange = slaveTimeVector.x;

            // observation vector
            y.put(i, (masterMinPi4divLam * masterTimeRange) - (slaveMinPi4divLam * slaveTimeRange));

            // set up a system of equations
            // ______Order unknowns: A00 A10 A01 A20 A11 A02 A30 A21 A12 A03 for degree=3______
            double posL = PolyUtils.normalize2(line, minLine, maxLine);
            double posP = PolyUtils.normalize2(pixel, minPixel, maxPixel);

            int index = 0;

            for (int j = 0; j <= srpPolynomialDegree; j++) {
                for (int k = 0; k <= j; k++) {
                    A.put(i, index, (Math.pow(posL, (double) (j - k)) * Math.pow(posP, (double) k)));
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
    private DoubleMatrix estimateFlatEarthPolynomial(
            SLCImage masterMetadata, Orbit masterOrbit, SLCImage slaveMetadata, Orbit slaveOrbit,
            final int subSwathIndex, final int burstIndex) throws Exception {

        long minLine = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
        long maxLine = minLine + subSwath[subSwathIndex - 1].linesPerBurst - 1;
        long minPixel = 0;
        long maxPixel = subSwath[subSwathIndex - 1].samplesPerBurst - 1;

        int numberOfCoefficients = PolyUtils.numberOfCoefficients(srpPolynomialDegree);

        int[][] position = MathUtils.distributePoints(srpNumberPoints, new Window(minLine,maxLine,minPixel,maxPixel));

        // setup observation and design matrix
        DoubleMatrix y = new DoubleMatrix(srpNumberPoints);
        DoubleMatrix A = new DoubleMatrix(srpNumberPoints, numberOfCoefficients);

        double masterMinPi4divLam = (-4 * Constants.PI * Constants.lightSpeed) / masterMetadata.getRadarWavelength();
        double slaveMinPi4divLam = (-4 * Constants.PI * Constants.lightSpeed) / slaveMetadata.getRadarWavelength();

        // Loop through vector or distributedPoints()
        for (int i = 0; i < srpNumberPoints; ++i) {

            double line = position[i][0];
            double pixel = position[i][1];

            // compute azimuth/range time for this pixel
            final double mstRgTime = subSwath[subSwathIndex - 1].slrTimeToFirstPixel +
                    pixel*su.rangeSpacing/Constants.lightSpeed;

            final double mstAzTime = line2AzimuthTime(line, subSwathIndex, burstIndex);

            // compute xyz of this point : sourceMaster
            org.jlinda.core.Point xyzMaster = masterOrbit.lph2xyz(mstAzTime, mstRgTime, 0.0, mstSceneCentreXYZ);
            org.jlinda.core.Point slaveTimeVector = slaveOrbit.xyz2t(xyzMaster, slvScenseCentreAzimuthTime);

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
                    A.put(i, index, (Math.pow(posL, (double) (j - k)) * Math.pow(posP, (double) k)));
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

    private double line2AzimuthTime(final double line, final int subSwathIndex, final int burstIndex) {

        final double firstLineTimeInDays = subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex] /
                Constants.secondsInDay;

        final double firstLineTime = (firstLineTimeInDays - (int)firstLineTimeInDays)*Constants.secondsInDay;

        return firstLineTime + (line - burstIndex * subSwath[subSwathIndex - 1].linesPerBurst) *
                        subSwath[subSwathIndex - 1].azimuthTimeInterval;
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

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
            final int rgOffset = (cohWinRg - 1) / 2;
            final int azOffset = (cohWinAz - 1) / 2;
            final int cohx0 = targetRectangle.x - rgOffset;
            final int cohy0 = targetRectangle.y - azOffset;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            targetRectangle = new Rectangle(cohx0, cohy0, cohw, cohh);

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final int y0 = targetRectangle.y;
            final int yN = y0 + targetRectangle.height - 1;
            final int x0 = targetRectangle.x;
            final int xN = targetRectangle.x + targetRectangle.width - 1;

            for (String ifgKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(ifgKey);

                /// check out results from master ///
                final Tile mstTileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle, border);
                final Tile mstTileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle, border);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal, mstTileImag);

                /// check out results from slave ///
                final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle, border);
                final Tile slvTileImag = getSourceTile(product.sourceSlave.imagBand, targetRectangle, border);
                final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal, slvTileImag);

                ComplexDoubleMatrix dataMaster2 = null, dataSlave2 = null;
                if(includeCoherence) {
                    dataMaster2 = new ComplexDoubleMatrix(mstTileReal.getHeight(), mstTileReal.getWidth());
                    dataSlave2 = new ComplexDoubleMatrix(slvTileReal.getHeight(), slvTileReal.getWidth());
                    dataMaster2.copy(dataMaster);
                    dataSlave2.copy(dataSlave);
                }

                if (subtractFlatEarthPhase) {
                    // normalize range and azimuth axis
                    DoubleMatrix rangeAxisNormalized = DoubleMatrix.linspace(x0, xN, dataMaster.columns);
                    rangeAxisNormalized = normalizeDoubleMatrix(rangeAxisNormalized, 0, sourceImageWidth - 1);

                    DoubleMatrix azimuthAxisNormalized = DoubleMatrix.linspace(y0, yN, dataMaster.rows);
                    azimuthAxisNormalized = normalizeDoubleMatrix(azimuthAxisNormalized, 0, sourceImageHeight - 1);

                    // pull polynomial from the map
                    final DoubleMatrix polyCoeffs = flatEarthPolyMap.get(product.sourceSlave.name);

                    // estimate the phase on the grid
                    final DoubleMatrix realReferencePhase =
                            PolyUtils.polyval(azimuthAxisNormalized, rangeAxisNormalized,
                                    polyCoeffs, PolyUtils.degreeFromCoefficients(polyCoeffs.length));

                    // compute the reference phase
                    final ComplexDoubleMatrix complexReferencePhase =
                            new ComplexDoubleMatrix(MatrixFunctions.cos(realReferencePhase),
                                    MatrixFunctions.sin(realReferencePhase));

                    dataSlave.muli(complexReferencePhase); // no conjugate here!
                }

                dataMaster.muli(dataSlave.conji());

                /// commit to target ///
                final Band targetBand_I = targetProduct.getBand(product.getBandName(Unit.REAL));
                final Tile tileOutReal = targetTileMap.get(targetBand_I);

                final Band targetBand_Q = targetProduct.getBand(product.getBandName(Unit.IMAGINARY));
                final Tile tileOutImag = targetTileMap.get(targetBand_Q);

                // coherence
                DoubleMatrix cohMatrix = null;
                ProductData samplesCoh = null;
                if(includeCoherence) {
                    for (int i = 0; i < dataMaster.length; i++) {
                        double tmp = norm(dataMaster2.get(i));
                        dataMaster2.put(i, dataMaster2.get(i).mul(dataSlave2.get(i).conj()));
                        dataSlave2.put(i, new ComplexDouble(norm(dataSlave2.get(i)), tmp));
                    }

                    cohMatrix = SarUtils.coherence2(dataMaster2, dataSlave2, cohWinAz, cohWinRg);

                    final Band targetBandCoh = targetProduct.getBand(product.getBandName(Unit.COHERENCE));
                    final Tile tileOutCoh = targetTileMap.get(targetBandCoh);

                    samplesCoh = tileOutCoh.getDataBuffer();
                }

                // push all

                final ProductData samplesReal = tileOutReal.getDataBuffer();
                final ProductData samplesImag = tileOutImag.getDataBuffer();
                final DoubleMatrix dataReal = dataMaster.real();
                final DoubleMatrix dataImag = dataMaster.imag();

                final Rectangle rect = tileOutReal.getRectangle();
                final int maxX = rect.x + rect.width;
                final int maxY = rect.y + rect.height;
                final TileIndex tgtIndex = new TileIndex(tileOutReal);
                for (int y = rect.y; y < maxY; y++) {
                    tgtIndex.calculateStride(y);
                    final int yy = y - rect.y;
                    final int yy2 = yy+azOffset;
                    for (int x = rect.x; x < maxX; x++) {
                        final int trgIndex = tgtIndex.getIndex(x);
                        final int xx = x - rect.x;
                        samplesReal.setElemFloatAt(trgIndex, (float)dataReal.get(yy2, xx+rgOffset));
                        samplesImag.setElemFloatAt(trgIndex, (float)dataImag.get(yy2, xx+rgOffset));
                        if(samplesCoh != null) {
                            samplesCoh.setElemFloatAt(trgIndex, (float) cohMatrix.get(yy, xx));
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private static double norm(final ComplexDouble number) {
        return number.real()*number.real() + number.imag()*number.imag();
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
                final int firstLineIdx = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
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

                computePartialTile(subSwathIndex, burstIndex, partialTileRectangle, targetTileMap);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computePartialTile(final int subSwathIndex, final int burstIndex, Rectangle targetRectangle,
                                    final Map<Band, Tile> targetTileMap) throws Exception {

        try {
            final int rgOffset = (cohWinRg - 1) / 2;
            final int azOffset = (cohWinAz - 1) / 2;
            final int cohx0 = targetRectangle.x - rgOffset;
            final int cohy0 = targetRectangle.y - azOffset;
            final int cohw = targetRectangle.width + cohWinRg - 1;
            final int cohh = targetRectangle.height + cohWinAz - 1;
            final Rectangle rect = new Rectangle(cohx0, cohy0, cohw, cohh);

            final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

            final int y0 = rect.y;
            final int yN = y0 + rect.height - 1;
            final int x0 = rect.x;
            final int xN = rect.x + rect.width - 1;

            final long minLine = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
            final long maxLine = minLine + subSwath[subSwathIndex - 1].linesPerBurst - 1;
            final long minPixel = 0;
            final long maxPixel = subSwath[subSwathIndex - 1].samplesPerBurst - 1;

            Band targetBand_I;
            Band targetBand_Q;

            for (String ifgKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(ifgKey);

                /// check out results from source ///
                final Tile mstTileReal = getSourceTile(product.sourceMaster.realBand, rect, border);
                final Tile mstTileImag = getSourceTile(product.sourceMaster.imagBand, rect, border);
                final ComplexDoubleMatrix dataMaster = TileUtilsDoris.pullComplexDoubleMatrix(mstTileReal, mstTileImag);

                /// check out results from source ///
                final Tile slvTileReal = getSourceTile(product.sourceSlave.realBand, rect, border);
                final Tile slvTileImag = getSourceTile(product.sourceSlave.imagBand, rect, border);
                final ComplexDoubleMatrix dataSlave = TileUtilsDoris.pullComplexDoubleMatrix(slvTileReal, slvTileImag);

                final double srcNoDataValue = product.sourceMaster.realBand.getNoDataValue();

                ComplexDoubleMatrix dataMaster2 = null, dataSlave2 = null;
                if(includeCoherence) {
                    dataMaster2 = new ComplexDoubleMatrix(mstTileReal.getHeight(), mstTileReal.getWidth());
                    dataSlave2 = new ComplexDoubleMatrix(slvTileReal.getHeight(), slvTileReal.getWidth());
                    dataMaster2.copy(dataMaster);
                    dataSlave2.copy(dataSlave);
                }

                if (subtractFlatEarthPhase) {
                    // normalize range and azimuth axis
                    DoubleMatrix rangeAxisNormalized = DoubleMatrix.linspace(x0, xN, dataMaster.columns);
                    rangeAxisNormalized = normalizeDoubleMatrix(rangeAxisNormalized, minPixel, maxPixel);

                    DoubleMatrix azimuthAxisNormalized = DoubleMatrix.linspace(y0, yN, dataMaster.rows);
                    azimuthAxisNormalized = normalizeDoubleMatrix(azimuthAxisNormalized, minLine, maxLine);

                    // pull polynomial from the map
                    final String polynomialName = product.sourceSlave.name + "_" + (subSwathIndex - 1) + "_" + burstIndex;
                    final DoubleMatrix polyCoeffs = flatEarthPolyMap.get(polynomialName);

                    // estimate the phase on the grid
                    final DoubleMatrix realReferencePhase =
                            PolyUtils.polyval(azimuthAxisNormalized, rangeAxisNormalized,
                                    polyCoeffs, PolyUtils.degreeFromCoefficients(polyCoeffs.length));

                    // compute the reference phase
                    final ComplexDoubleMatrix complexReferencePhase =
                            new ComplexDoubleMatrix(MatrixFunctions.cos(realReferencePhase),
                                    MatrixFunctions.sin(realReferencePhase));

                    dataSlave.muli(complexReferencePhase); // no conjugate here!
                }

                dataMaster.muli(dataSlave.conji());

                /// commit to target ///
                targetBand_I = targetProduct.getBand(product.getBandName(Unit.REAL));
                Tile tileOutReal = targetTileMap.get(targetBand_I);

                targetBand_Q = targetProduct.getBand(product.getBandName(Unit.IMAGINARY));
                Tile tileOutImag = targetTileMap.get(targetBand_Q);

                // coherence
                DoubleMatrix cohMatrix = null;
                ProductData samplesCoh = null;
                if(includeCoherence) {
                    for (int i = 0; i < dataMaster.length; i++) {
                        double tmp = norm(dataMaster2.get(i));
                        dataMaster2.put(i, dataMaster2.get(i).mul(dataSlave2.get(i).conj()));
                        dataSlave2.put(i, new ComplexDouble(norm(dataSlave2.get(i)), tmp));
                    }

                    cohMatrix = SarUtils.coherence2(dataMaster2, dataSlave2, cohWinAz, cohWinRg);

                    final Band targetBandCoh = targetProduct.getBand(product.getBandName(Unit.COHERENCE));
                    final Tile tileOutCoh = targetTileMap.get(targetBandCoh);

                    samplesCoh = tileOutCoh.getDataBuffer();
                }

                // push all
                final ProductData samplesReal = tileOutReal.getDataBuffer();
                final ProductData samplesImag = tileOutImag.getDataBuffer();
                final ProductData srcSlvData = slvTileReal.getDataBuffer();
                final DoubleMatrix dataReal = dataMaster.real();
                final DoubleMatrix dataImag = dataMaster.imag();

                final int maxX = targetRectangle.x + targetRectangle.width;
                final int maxY = targetRectangle.y + targetRectangle.height;
                final TileIndex tgtIndex = new TileIndex(tileOutReal);
                final TileIndex srcSlvIndex = new TileIndex(slvTileReal);

                for (int y = targetRectangle.y; y < maxY; y++) {
                    tgtIndex.calculateStride(y);
                    srcSlvIndex.calculateStride(y);
                    final int yy = y - targetRectangle.y;
                    final int yy2 = yy+azOffset;
                    for (int x = targetRectangle.x; x < maxX; x++) {
                        final int trgIdx = tgtIndex.getIndex(x);
                        final int xx = x - targetRectangle.x;

                        if (srcSlvData.getElemDoubleAt(srcSlvIndex.getIndex(x)) == srcNoDataValue) {
                            samplesReal.setElemFloatAt(trgIdx, (float)srcNoDataValue);
                            samplesImag.setElemFloatAt(trgIdx, (float)srcNoDataValue);
                            if(samplesCoh != null) {
                                samplesCoh.setElemFloatAt(trgIdx, (float)srcNoDataValue);
                            }
                        } else {
                            samplesReal.setElemFloatAt(trgIdx, (float)dataReal.get(yy2, xx+rgOffset));
                            samplesImag.setElemFloatAt(trgIdx, (float)dataImag.get(yy2, xx+rgOffset));
                            if(samplesCoh != null) {
                                samplesCoh.setElemFloatAt(trgIdx, (float) cohMatrix.get(yy, xx));
                            }
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static DoubleMatrix normalizeDoubleMatrix(DoubleMatrix matrix, final double min, final double max) {
        matrix.subi(0.5 * (min + max));
        matrix.divi(0.25 * (max - min));
        return matrix;
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
            super(CreateInterferogramOp.class);
        }
    }

}
