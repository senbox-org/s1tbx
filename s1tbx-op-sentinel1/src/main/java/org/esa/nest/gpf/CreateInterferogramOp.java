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
import org.apache.commons.math3.util.FastMath;
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
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.Solve;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.PolyUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


@OperatorMetadata(alias = "Create-Interferogram",
        category = "SAR Processing/SENTINEL-1",
        description = "Compute interferograms from stack of coregistered S-1 images", internal = false)
public class CreateInterferogramOp extends Operator {
    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

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

    @Parameter(defaultValue="false", label="Do NOT subtract flat-earth phase from interferogram.")
    private boolean doNotSubtract = false;

    // flat_earth_polynomial container
    private HashMap<String, DoubleMatrix> flatEarthPolyMap = new HashMap<String, DoubleMatrix>();

    // source
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();

    // target
    private HashMap<String, ProductContainer> targetMap = new HashMap<String, ProductContainer>();

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
            if (doNotSubtract) {
                productName = "ifgs";
                productTag = "ifg";
            } else {
                productName = "srp_ifgs";
                productTag = "ifg_srp";
            }

            constructSourceMetadata();
            constructTargetMetadata();
            createTargetProduct();

            getSourceImageDimension();

            if (!doNotSubtract) {
                final InputProductValidator validator = new InputProductValidator(sourceProduct);
                validator.checkIfCoregisteredStack();
                isTOPSARBurstProduct = validator.isTOPSARBurstProduct();

                if (isTOPSARBurstProduct) {
                    su = new Sentinel1Utils(sourceProduct);
                    subSwath = su.getSubSwath();
                    numSubSwaths = su.getNumOfSubSwath();
                    subSwathIndex = 1; // subSwathIndex is always 1 because of split product

                    getMstApproxSceneCentreXYZ();
                    getSlvApproxSceneCentreAzimuthTime();
                    constructFlatEarthPolynomialsForTOPSARProduct();
                } else {
                    constructFlatEarthPolynomials();
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
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

        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement slvRoot = AbstractMetadata.getSlaveMetadata(root);

        final double firstLineTime = slvRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD() *
                Constants.secondsInDay;

        final double lastLineTime = slvRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD() *
                Constants.secondsInDay;

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
                                master.metaData, master.orbit, slave.metaData, slave.orbit, s, b));
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

                product.targetBandName_I = "i_" + productTag + "_" + master.date + "_" + slave.date;
                product.targetBandName_Q = "q_" + productTag + "_" + master.date + "_" + slave.date;

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

        for (final Band band : targetProduct.getBands()) {
            targetProduct.removeBand(band);
        }

        for (String key : targetMap.keySet()) {

            String targetBandName_I = targetMap.get(key).targetBandName_I;
            targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT32);
            targetProduct.getBand(targetBandName_I).setUnit(Unit.REAL);

            String targetBandName_Q = targetMap.get(key).targetBandName_Q;
            targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT32);
            targetProduct.getBand(targetBandName_Q).setUnit(Unit.IMAGINARY);

            final String tag0 = targetMap.get(key).sourceMaster.date;
            final String tag1 = targetMap.get(key).sourceSlave.date;
            if (CREATE_VIRTUAL_BAND) {
                String countStr = "_" + productTag + "_" + tag0 + "_" + tag1;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
            }
        }

        // For testing: the optimal results with 1024x1024 pixels tiles, not clear whether it's platform dependent?
        // targetProduct.setPreferredTileSize(512, 512);

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

            final double mstAzTime = subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex] +
                    (line - burstIndex * subSwath[subSwathIndex - 1].linesPerBurst) *
                            subSwath[subSwathIndex - 1].azimuthTimeInterval;

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
            Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
            int y0 = targetRectangle.y;
            int yN = y0 + targetRectangle.height - 1;
            int x0 = targetRectangle.x;
            int xN = targetRectangle.x + targetRectangle.width - 1;
            final Window tileWindow = new Window(y0, yN, x0, xN);

            Band targetBand_I;
            Band targetBand_Q;

            for (String ifgKey : targetMap.keySet()) {

                ProductContainer product = targetMap.get(ifgKey);

                /// check out results from source ///
                Tile tileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle);
                Tile tileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle);
                ComplexDoubleMatrix complexMaster = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                /// check out results from source ///
                tileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle);
                tileImag = getSourceTile(product.sourceSlave.imagBand, targetRectangle);
                ComplexDoubleMatrix complexSlave = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                if (!doNotSubtract) {
                    // normalize range and azimuth axis
                    DoubleMatrix rangeAxisNormalized = DoubleMatrix.linspace(x0, xN, complexMaster.columns);
                    rangeAxisNormalized = normalizeDoubleMatrix(rangeAxisNormalized, 0, sourceImageWidth - 1);

                    DoubleMatrix azimuthAxisNormalized = DoubleMatrix.linspace(y0, yN, complexMaster.rows);
                    azimuthAxisNormalized = normalizeDoubleMatrix(azimuthAxisNormalized, 0, sourceImageHeight - 1);

                    // pull polynomial from the map
                    DoubleMatrix polyCoeffs = flatEarthPolyMap.get(product.sourceSlave.name);

                    // estimate the phase on the grid
                    DoubleMatrix realReferencePhase =
                            PolyUtils.polyval(azimuthAxisNormalized, rangeAxisNormalized,
                                    polyCoeffs, PolyUtils.degreeFromCoefficients(polyCoeffs.length));

                    // compute the reference phase
                    ComplexDoubleMatrix complexReferencePhase =
                            new ComplexDoubleMatrix(MatrixFunctions.cos(realReferencePhase),
                                    MatrixFunctions.sin(realReferencePhase));

                    complexSlave.muli(complexReferencePhase); // no conjugate here!
                }

                complexMaster.muli(complexSlave.conji());

                /// commit to target ///
                targetBand_I = targetProduct.getBand(product.targetBandName_I);
                Tile tileOutReal = targetTileMap.get(targetBand_I);
                TileUtilsDoris.pushDoubleMatrix(complexMaster.real(), tileOutReal, targetRectangle);

                targetBand_Q = targetProduct.getBand(product.targetBandName_Q);
                Tile tileOutImag = targetTileMap.get(targetBand_Q);
                TileUtilsDoris.pushDoubleMatrix(complexMaster.imag(), tileOutImag, targetRectangle);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void computeTileStackForTOPSARProduct(
            Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

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
                System.out.println("burst = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

                computePartialTile(subSwathIndex, burstIndex, partialTileRectangle, targetTileMap, pm);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computePartialTile(final int subSwathIndex, final int burstIndex, final Rectangle targetRectangle,
                                    final Map<Band, Tile> targetTileMap, ProgressMonitor pm) throws Exception {

        try {
            int y0 = targetRectangle.y;
            int yN = y0 + targetRectangle.height - 1;
            int x0 = targetRectangle.x;
            int xN = targetRectangle.x + targetRectangle.width - 1;

            long minLine = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
            long maxLine = minLine + subSwath[subSwathIndex - 1].linesPerBurst - 1;
            long minPixel = 0;
            long maxPixel = subSwath[subSwathIndex - 1].samplesPerBurst - 1;

            Band targetBand_I;
            Band targetBand_Q;

            for (String ifgKey : targetMap.keySet()) {

                ProductContainer product = targetMap.get(ifgKey);

                /// check out results from source ///
                Tile tileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle);
                Tile tileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle);
                ComplexDoubleMatrix complexMaster = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                /// check out results from source ///
                tileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle);
                tileImag = getSourceTile(product.sourceSlave.imagBand, targetRectangle);
                ComplexDoubleMatrix complexSlave = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                if (!doNotSubtract) {
                    // normalize range and azimuth axis
                    DoubleMatrix rangeAxisNormalized = DoubleMatrix.linspace(x0, xN, complexMaster.columns);
                    rangeAxisNormalized = normalizeDoubleMatrix(rangeAxisNormalized, minPixel, maxPixel);

                    DoubleMatrix azimuthAxisNormalized = DoubleMatrix.linspace(y0, yN, complexMaster.rows);
                    azimuthAxisNormalized = normalizeDoubleMatrix(azimuthAxisNormalized, minLine, maxLine);

                    // pull polynomial from the map
                    final String polynomialName = product.sourceSlave.name + "_" + (subSwathIndex - 1) + "_" + burstIndex;
                    DoubleMatrix polyCoeffs = flatEarthPolyMap.get(polynomialName);

                    // estimate the phase on the grid
                    DoubleMatrix realReferencePhase =
                            PolyUtils.polyval(azimuthAxisNormalized, rangeAxisNormalized,
                                    polyCoeffs, PolyUtils.degreeFromCoefficients(polyCoeffs.length));

                    // compute the reference phase
                    ComplexDoubleMatrix complexReferencePhase =
                            new ComplexDoubleMatrix(MatrixFunctions.cos(realReferencePhase),
                                    MatrixFunctions.sin(realReferencePhase));

                    complexSlave.muli(complexReferencePhase); // no conjugate here!
                }

                complexMaster.muli(complexSlave.conji());

                /// commit to target ///
                targetBand_I = targetProduct.getBand(product.targetBandName_I);
                Tile tileOutReal = targetTileMap.get(targetBand_I);
                TileUtilsDoris.pushDoubleMatrix(complexMaster.real(), tileOutReal, targetRectangle);

                targetBand_Q = targetProduct.getBand(product.targetBandName_Q);
                Tile tileOutImag = targetTileMap.get(targetBand_Q);
                TileUtilsDoris.pushDoubleMatrix(complexMaster.imag(), tileOutImag, targetRectangle);
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
