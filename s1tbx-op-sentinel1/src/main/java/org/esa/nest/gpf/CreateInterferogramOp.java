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

import Jama.Matrix;
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
import org.esa.nest.gpf.geometric.SARGeocoding;
import org.esa.nest.gpf.geometric.SARUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Create interferogram using co-registered master and slave images.
 *
 * If "subtract flat-earth phase" option is not selected, then the interferogram is computed by multiplying
 * master image with the complex conjugate of the slave image.
 *
 * If the "subtract flat-earth phase" option is selected, then first a 2-variable polynomial is created for
 * each burst. The interferogram for each burst is computed later using polynomial fitting.
 */

@OperatorMetadata(alias = "Create-Interferogram",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Compute interferogram using co-registered master and slave images", internal=true)
public class CreateInterferogramOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct = null;

    @Parameter(valueSet = {"1", "2", "3", "4", "5"},
            description = "Order of 'Flat earth phase' polynomial",
            defaultValue = "5",
            label = "Degree of \"Flat Earth\" polynomial")
    private int flatEarthPhasePolynomialDegree = 5;

    @Parameter(valueSet = {"301", "401", "501", "601", "701", "801", "901", "1001"},
            description = "Number of points for the 'flat earth phase' polynomial estimation",
            defaultValue = "501",
            label = "Number of 'Flat earth' estimation points")
    private int flatEarthPhaseEstPoints = 501;

    @Parameter(valueSet = {"1", "2", "3", "4", "5"},
            description = "Degree of orbit (polynomial) interpolator",
            defaultValue = "3",
            label = "Orbit interpolation degree")
    private int orbitDegree = 3;

    @Parameter(defaultValue="true", label="Subtract flat-earth phase from interferogram")
    private boolean subtractFlatEarthPhase = false;

    @Parameter(defaultValue="false", label="Output flat-earth phase")
    private boolean outputFlatEarthPhase = false;

    private SARGeocoding.Orbit mOrbit = null;
    private SARGeocoding.Orbit sOrbit = null;

    private String tgtIfgIBandName = null;
    private String tgtIfgQBandName = null;
    private String tgtFlatEarthPhaseBandName = null;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int slvImageHeight = 0;
    private double[] flatEarthPhasePolynomial = null;
    private double[] mstSceneCentreXYZ = null;
    private double slvWavelength = 0.0;
    private double slvFirstLineTime = 0.0;
    private double slvLineTimeInterval = 0.0;
    private double mstWavelength = 0.0;
    private double mstFirstLineTime = 0.0;
    private double mstLineTimeInterval = 0.0;
    private double mstSlantRangeToFirstPixel = 0.0;
    private double mstSlantRangePixelSpacing = 0.0;

    private final static double noDataValue = 0.0;

    private final static double ell_a = 6378137.000;
    private final static double ell_b = 6356752.314245;
    private final static int MAXITER = 10;
    private final static double CRITERPOS = 1e-6;
    private final static double CRITERTIM = 1e-10;


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CreateInterferogramOp() {
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
            if (!subtractFlatEarthPhase && outputFlatEarthPhase) {
                throw new OperatorException("Please select \"Subtract flat-earth phase\" in order to output the phase");
            }

            if (subtractFlatEarthPhase) {
                getMasterMetadata();

                getSlaveMetadata();
            }

            getApproxSceneCentreXYZ();

            createFlatEarthPhasePolynomials();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMasterMetadata() throws Exception {

        final MetadataElement mstAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(mstAbsRoot);
        mstFirstLineTime = mstAbsRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD() *
                Constants.secondsInDay % (24*3600); // sec in day;
        mstLineTimeInterval = mstAbsRoot.getAttributeDouble(AbstractMetadata.line_time_interval); // in s;
        mstSlantRangeToFirstPixel = mstAbsRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel);
        mstSlantRangePixelSpacing = mstAbsRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        mstWavelength = SARUtils.getRadarFrequency(mstAbsRoot);
        mOrbit = new SARGeocoding.Orbit(orbitStateVectors, orbitDegree, mstFirstLineTime);

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    private void getSlaveMetadata() throws Exception {

        final MetadataElement slaveMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);

        final MetadataElement slvAbsRoot = slaveMetadataRoot.getElementAt(0);
        final OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(slvAbsRoot);
        slvFirstLineTime = slvAbsRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD() *
                Constants.secondsInDay % (24*3600); // sec in day;
        slvLineTimeInterval = slvAbsRoot.getAttributeDouble(AbstractMetadata.line_time_interval); // in s;
        slvWavelength = SARUtils.getRadarFrequency(slvAbsRoot);
        sOrbit = new SARGeocoding.Orbit(orbitStateVectors, orbitDegree, slvFirstLineTime);
        slvImageHeight = slvAbsRoot.getAttributeInt(AbstractMetadata.num_output_lines);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final String[] srcBandNames = sourceProduct.getBandNames();

        String masterTimeStamp = null;
        for (String srcBandName : srcBandNames) {
            if (srcBandName.contains("_mst")) {
                masterTimeStamp = srcBandName.substring(srcBandName.lastIndexOf("_"));
                break;
            }
        }

        String slaveTimeStamp = null;
        int width = 0, height = 0;
        for (String srcBandName : srcBandNames) {
            if (srcBandName.contains("_slv")) {
                slaveTimeStamp = srcBandName.substring(srcBandName.lastIndexOf("_"));
                final Band band = sourceProduct.getBand(srcBandName);
                width = band.getSceneRasterWidth();
                height = band.getSceneRasterHeight();
            }
        }

        tgtIfgIBandName = "i_ifg" + masterTimeStamp + slaveTimeStamp;
        final Band tgtIfgIBand = new Band(tgtIfgIBandName, ProductData.TYPE_FLOAT32, width, height);
        tgtIfgIBand.setUnit(Unit.REAL);
        targetProduct.addBand(tgtIfgIBand);

        tgtIfgQBandName = "q_ifg" + masterTimeStamp + slaveTimeStamp;
        final Band tgtIfgQBand = new Band(tgtIfgQBandName, ProductData.TYPE_FLOAT32, width, height);
        tgtIfgQBand.setUnit(Unit.IMAGINARY);
        targetProduct.addBand(tgtIfgQBand);

        if (outputFlatEarthPhase) {
            tgtFlatEarthPhaseBandName = "flatEarthPhase" + masterTimeStamp + slaveTimeStamp;
            final Band flatEarthPhaseBand = new Band(
                    tgtFlatEarthPhaseBandName, ProductData.TYPE_FLOAT32, width, height);
            flatEarthPhaseBand.setUnit(Unit.PHASE);
            targetProduct.addBand(flatEarthPhaseBand);
        }

        final String suffix = "_" + OperatorUtils.getSuffixFromBandName(tgtIfgIBandName);
        ReaderUtils.createVirtualIntensityBand(
                targetProduct,
                targetProduct.getBand(tgtIfgIBandName),
                targetProduct.getBand(tgtIfgQBandName),
                suffix);

        ReaderUtils.createVirtualPhaseBand(
                targetProduct,
                targetProduct.getBand(tgtIfgIBandName),
                targetProduct.getBand(tgtIfgQBandName),
                suffix);
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

        try {
            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            Band masterBandI = null;
            Band masterBandQ = null;
            Band slaveBandI = null;
            Band slaveBandQ = null;
            final String[] bandNames = sourceProduct.getBandNames();
            for (String bandName : bandNames) {
                if (bandName.contains("i_") && bandName.contains("_mst")) {
                    masterBandI = sourceProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains("_mst")) {
                    masterBandQ = sourceProduct.getBand(bandName);
                } else if (bandName.contains("i_") && bandName.contains("_slv")) {
                    slaveBandI = sourceProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains("_slv")) {
                    slaveBandQ = sourceProduct.getBand(bandName);
                }
            }

            final Tile mstTileI = getSourceTile(masterBandI, targetRectangle);
            final Tile mstTileQ = getSourceTile(masterBandQ, targetRectangle);
            final ProductData mstDataI = mstTileI.getDataBuffer();
            final ProductData mstDataQ = mstTileQ.getDataBuffer();

            final Tile slvTileI = getSourceTile(slaveBandI, targetRectangle);
            final Tile slvTileQ = getSourceTile(slaveBandQ, targetRectangle);
            final ProductData slvDataI = slvTileI.getDataBuffer();
            final ProductData slvDataQ = slvTileQ.getDataBuffer();

            final Band targetBandI = targetProduct.getBand(tgtIfgIBandName);
            final Band targetBandQ = targetProduct.getBand(tgtIfgQBandName);
            final Tile tgtTileI = targetTileMap.get(targetBandI);
            final Tile tgtTileQ = targetTileMap.get(targetBandQ);
            final ProductData tgtBufferI = tgtTileI.getDataBuffer();
            final ProductData tgtBufferQ = tgtTileQ.getDataBuffer();

            Band flatEarthPhaseBand = null;
            Tile flatEarthPhaseTile = null;
            ProductData flatEarthPhaseData = null;
            if (outputFlatEarthPhase) {
                flatEarthPhaseBand = targetProduct.getBand(tgtFlatEarthPhaseBandName);
                flatEarthPhaseTile = targetTileMap.get(flatEarthPhaseBand);
                flatEarthPhaseData = flatEarthPhaseTile.getDataBuffer();
            }

            final TileIndex srcIndex = new TileIndex(mstTileI);
            final TileIndex tgtIndex = new TileIndex(tgtTileI);

            int srcIdx, tgtIdx;
            double phase, mI, mQ, sI, sQ, ifgI, ifgQ, c, s, tmpI, tmpQ;
            for (int y = y0; y < yMax; y++) {
                srcIndex.calculateStride(y);
                tgtIndex.calculateStride(y);
                for (int x = x0; x < xMax; x++) {
                    srcIdx = srcIndex.getIndex(x);
                    tgtIdx = tgtIndex.getIndex(x);

                    sI = slvDataI.getElemDoubleAt(srcIdx);
                    sQ = slvDataQ.getElemDoubleAt(srcIdx);
                    if (sI == noDataValue && sQ == noDataValue) {
                        continue;
                    }

                    mI = mstDataI.getElemDoubleAt(srcIdx);
                    mQ = mstDataQ.getElemDoubleAt(srcIdx);

                    ifgI = mI*sI + mQ*sQ;
                    ifgQ = mQ*sI - mI*sQ;

                    if (subtractFlatEarthPhase) {
                        final double xNormalized = normalize2(x, 0, sourceImageWidth);
                        final double yNormalized = normalize2(y, 0, sourceImageHeight);
                        phase = polyval(xNormalized, yNormalized);
                        c = Math.cos(phase);
                        s = Math.sin(phase);
                        tmpI = ifgI*c + ifgQ*s;
                        tmpQ = ifgQ*c - ifgI*s;
                        ifgI = tmpI;
                        ifgQ = tmpQ;

                        if (outputFlatEarthPhase) {
                            flatEarthPhaseData.setElemFloatAt(tgtIdx, (float)Math.atan2(s, c));
                        }
                    }

                    tgtBufferI.setElemFloatAt(tgtIdx, (float)ifgI);
                    tgtBufferQ.setElemFloatAt(tgtIdx, (float)ifgQ);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private synchronized void createFlatEarthPhasePolynomials() throws Exception {

        final int numberOfCoefficients = numberOfCoefficients(flatEarthPhasePolynomialDegree);

        int[][] position = distributePoints(flatEarthPhaseEstPoints, sourceImageWidth, sourceImageHeight);

        final Matrix A = new Matrix(flatEarthPhaseEstPoints, numberOfCoefficients);
        final Matrix b = new Matrix(flatEarthPhaseEstPoints, 1);

        final double mstR2P = 4*Math.PI/mstWavelength;
        final double slvR2P = 4*Math.PI/slvWavelength;

        for (int i = 0; i < flatEarthPhaseEstPoints; ++i) {

            final double y = position[i][0];
            final double x = position[i][1];
            final double mstSlantRange = mstSlantRangeToFirstPixel + x*mstSlantRangePixelSpacing;
            final double mstAzTime = mstFirstLineTime + y*mstLineTimeInterval;

            final double[] xyzMaster = lph2xyz(mstAzTime, mstSlantRange);

            final double slvSlantRange = computeSlantRange(
                    xyzMaster, slvFirstLineTime, slvLineTimeInterval, slvWavelength, sOrbit);

            final double phase = mstSlantRange*mstR2P - slvSlantRange*slvR2P;
            b.set(i, 0, phase);

            final double posL = normalize2(y, 0, sourceImageHeight);
            final double posP = normalize2(x, 0, sourceImageWidth);
            int index = 0;
            for (int j = 0; j <= flatEarthPhasePolynomialDegree; j++) {
                for (int k = 0; k <= j; k++) {
                    A.set(i, index, (Math.pow(posL, (double) (j - k)) * Math.pow(posP, (double) k)));
                    index++;
                }
            }
        }

        final Matrix coefficients = A.solve(b);

        // Order coefficients: A00 A10 A01 A20 A11 A02 A30 A21 A12 A03 for degree=3
        flatEarthPhasePolynomial = coefficients.getColumnPackedCopy();
    }

    private static int numberOfCoefficients(final int degree) {
        return (int) (0.5 * (Math.pow(degree + 1, 2) + degree + 1));
    }

    private static int[][] distributePoints(
            final int numOfPoints, final int sourceImageWidth, final int sourceImageHeight) {

        final float lines = sourceImageHeight + 1;
        final float pixels = sourceImageWidth + 1;

        int[][] result = new int[numOfPoints][2];

        // Distribution for dl=dp
        float winP = (float) Math.sqrt(numOfPoints / (lines / pixels));   // wl: #windows in line direction
        float winL = numOfPoints / winP;                     // wp: #windows in pixel direction
        if (winL < winP) {
            // switch wl,wp : later back
            winL = winP;
        }

        final int winL_int = (int) Math.floor(winL); // round
        final float deltaLin = (lines - 1) / (float) (winL_int - 1);
        final int totalPix = (int) Math.floor(pixels * winL_int);
        final float deltaPix = (float) (totalPix - 1) / (float) (numOfPoints - 1);

        float pix = -deltaPix;
        float lin;
        int lCounter = 0;
        for (int i = 0; i < numOfPoints; i++) {
            pix += deltaPix;
            while (Math.floor(pix) >= pixels) {
                pix -= pixels;
                lCounter++;
            }
            lin = lCounter * deltaLin;

            // also correct distribution to window
            result[i][0] = (int) (Math.floor(lin) + 0);
            result[i][1] = (int) (Math.floor(pix) + 0);
        }
        return result;
    }

    private static double normalize2(double data, final double min, final double max) {
        data -= (0.5 * (min + max));
        data /= (0.25 * (max - min));
        return data;
    }

    private double polyval(final double nx, final double ny) {

        double c00, c10, c01, c20, c11, c02, c30, c21, c12, c03, c40, c31, c22, c13, c04, c50,
               c41, c32, c23, c14, c05;

        final double nx2 = nx*nx;
        final double ny2 = ny*ny;
        final double nx3 = nx2*nx;
        final double ny3 = ny2*ny;
        final double nx4 = nx2*nx2;
        final double ny4 = ny2*ny2;
        final double nx5 = nx3*nx2;
        final double ny5 = ny3*ny2;

        switch (flatEarthPhasePolynomialDegree) {
            case 0:
                return flatEarthPhasePolynomial[0];
            case 1:
                c00 = flatEarthPhasePolynomial[0];
                c10 = flatEarthPhasePolynomial[1];
                c01 = flatEarthPhasePolynomial[2];
                return c00 + c10*ny + c01*nx;
            case 2:
                c00 = flatEarthPhasePolynomial[0];
                c10 = flatEarthPhasePolynomial[1];
                c01 = flatEarthPhasePolynomial[2];
                c20 = flatEarthPhasePolynomial[3];
                c11 = flatEarthPhasePolynomial[4];
                c02 = flatEarthPhasePolynomial[5];
                return c00 + c10*ny + c01*nx + c20*ny*ny + c11*ny*nx + c02*nx*nx;
            case 3:
                c00 = flatEarthPhasePolynomial[0];
                c10 = flatEarthPhasePolynomial[1];
                c01 = flatEarthPhasePolynomial[2];
                c20 = flatEarthPhasePolynomial[3];
                c11 = flatEarthPhasePolynomial[4];
                c02 = flatEarthPhasePolynomial[5];
                c30 = flatEarthPhasePolynomial[6];
                c21 = flatEarthPhasePolynomial[7];
                c12 = flatEarthPhasePolynomial[8];
                c03 = flatEarthPhasePolynomial[9];
                return c00 + c10*ny + c01*nx + c20*ny2 + c11*ny*nx + c02*nx2 +
                        c30*ny3 + c21*ny2*nx + c12*ny*nx2 + c03*nx3;

            case 4:
                c00 = flatEarthPhasePolynomial[0];
                c10 = flatEarthPhasePolynomial[1];
                c01 = flatEarthPhasePolynomial[2];
                c20 = flatEarthPhasePolynomial[3];
                c11 = flatEarthPhasePolynomial[4];
                c02 = flatEarthPhasePolynomial[5];
                c30 = flatEarthPhasePolynomial[6];
                c21 = flatEarthPhasePolynomial[7];
                c12 = flatEarthPhasePolynomial[8];
                c03 = flatEarthPhasePolynomial[9];
                c40 = flatEarthPhasePolynomial[10];
                c31 = flatEarthPhasePolynomial[11];
                c22 = flatEarthPhasePolynomial[12];
                c13 = flatEarthPhasePolynomial[13];
                c04 = flatEarthPhasePolynomial[14];

                return c00 + c10*ny + c01*nx + c20*ny2 + c11*ny*nx + c02*nx2 +
                        c30*ny3 + c21*ny2*nx + c12*ny*nx2 + c03*nx3 +
                        c40*ny4 + c31*ny3*nx + c22*ny2*nx2 + c13*ny*nx3 + c04*nx4;
            case 5:
                c00 = flatEarthPhasePolynomial[0];
                c10 = flatEarthPhasePolynomial[1];
                c01 = flatEarthPhasePolynomial[2];
                c20 = flatEarthPhasePolynomial[3];
                c11 = flatEarthPhasePolynomial[4];
                c02 = flatEarthPhasePolynomial[5];
                c30 = flatEarthPhasePolynomial[6];
                c21 = flatEarthPhasePolynomial[7];
                c12 = flatEarthPhasePolynomial[8];
                c03 = flatEarthPhasePolynomial[9];
                c40 = flatEarthPhasePolynomial[10];
                c31 = flatEarthPhasePolynomial[11];
                c22 = flatEarthPhasePolynomial[12];
                c13 = flatEarthPhasePolynomial[13];
                c04 = flatEarthPhasePolynomial[14];
                c50 = flatEarthPhasePolynomial[15];
                c41 = flatEarthPhasePolynomial[16];
                c32 = flatEarthPhasePolynomial[17];
                c23 = flatEarthPhasePolynomial[18];
                c14 = flatEarthPhasePolynomial[19];
                c05 = flatEarthPhasePolynomial[20];

                return c00 + c10*ny + c01*nx + c20*ny2 + c11*ny*nx + c02*nx2 +
                        c30*ny3 + c21*ny2*nx + c12*ny*nx2 + c03*nx3 +
                        c40*ny4 + c31*ny3*nx + c22*ny2*nx2 + c13*ny*nx3 + c04*nx4 +
                        c50*ny5 + c41*ny4*nx + c32*ny3*nx2 + c23*ny2*nx3 + c14*ny*nx4 + c05*nx5;

            default:
                break;
        } // switch degree

        return 0.0;
    }

    private double[] lph2xyz(final double mstAzTime, final double mstSlantRange) throws Exception {

        try {
            double[] satellitePosition = new double[3];
            double[] satelliteVelocity = new double[3];

            //mOrbit.getPosition(mstAzTime, satellitePosition);
            //mOrbit.getVelocity(mstAzTime, satelliteVelocity);

            // allocate matrices
            double[] equationSet = new double[3];
            double[][] partialsXYZ = new double[3][3];
            double[] ellipsoidPosition = new double[3];
            ellipsoidPosition[0] = mstSceneCentreXYZ[0];
            ellipsoidPosition[1] = mstSceneCentreXYZ[1];
            ellipsoidPosition[2] = mstSceneCentreXYZ[2];

            for (int iter = 0; iter <= MAXITER; iter++) {

                double[] dsat = new double[3];
                dsat[0] = ellipsoidPosition[0] - satellitePosition[0];
                dsat[1] = ellipsoidPosition[1] - satellitePosition[1];
                dsat[2] = ellipsoidPosition[2] - satellitePosition[2];

                equationSet[0] = -dotProd(satelliteVelocity, dsat);
                equationSet[1] = -(dotProd(dsat, dsat) - mstSlantRange*mstSlantRange);
                equationSet[2] = -(((Math.pow(ellipsoidPosition[0], 2) + Math.pow(ellipsoidPosition[1], 2)) /
                        Math.pow(ell_a, 2)) + Math.pow(ellipsoidPosition[2] / ell_b, 2) - 1.0);

                partialsXYZ[0][0] = satelliteVelocity[0];
                partialsXYZ[0][1] = satelliteVelocity[1];
                partialsXYZ[0][2] = satelliteVelocity[2];
                partialsXYZ[1][0] = 2 * dsat[0];
                partialsXYZ[1][1] = 2 * dsat[1];
                partialsXYZ[1][2] = 2 * dsat[2];
                partialsXYZ[2][0] = (2 * ellipsoidPosition[0]) / (Math.pow(ell_a, 2));
                partialsXYZ[2][1] = (2 * ellipsoidPosition[1]) / (Math.pow(ell_a, 2));
                partialsXYZ[2][2] = (2 * ellipsoidPosition[2]) / (Math.pow(ell_b, 2));

                double[] ellipsoidPositionSolution = solve33(partialsXYZ, equationSet);

                ellipsoidPosition[0] += ellipsoidPositionSolution[0];
                ellipsoidPosition[1] += ellipsoidPositionSolution[1];
                ellipsoidPosition[2] += ellipsoidPositionSolution[2];

                if (Math.abs(ellipsoidPositionSolution[0]) < CRITERPOS &&
                        Math.abs(ellipsoidPositionSolution[1]) < CRITERPOS &&
                        Math.abs(ellipsoidPositionSolution[2]) < CRITERPOS) {

                    break;
                }

                if (iter > MAXITER) {
                    System.out.println();
                }
            }

            return ellipsoidPosition;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("lph2xyz", e);
        }

        return null;
    }

    private void getApproxSceneCentreXYZ() throws Exception {

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

        mstSceneCentreXYZ = new double[3];
        GeoUtils.geo2xyzWGS84(lat, lon, 0.0, mstSceneCentreXYZ);
    }

    private double dotProd(final double[] v1, final double[] v2) {

        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += v1[i]*v2[i];
        }
        return sum;
    }

    public static double[] solve33(double[][] A, double[] rhs) throws IllegalArgumentException {

        try {
            double[] result = new double[3];

            if (A[0].length != 3 || A.length != 3) {
                throw new IllegalArgumentException("solve33: input: size of A not 33.");
            }
            if (rhs.length != 3) {
                throw new IllegalArgumentException("solve33: input: size rhs not 3x1.");
            }

            // real8 L10, L20, L21: used lower matrix elements
            // real8 U11, U12, U22: used upper matrix elements
            // real8 b0,  b1,  b2:  used Ux=b
            final double L10 = A[1][0] / A[0][0];
            final double L20 = A[2][0] / A[0][0];
            final double U11 = A[1][1] - L10 * A[0][1];
            final double L21 = (A[2][1] - (A[0][1] * L20)) / U11;
            final double U12 = A[1][2] - L10 * A[0][2];
            final double U22 = A[2][2] - L20 * A[0][2] - L21 * U12;

            // ______ Solution: forward substitution ______
            final double b0 = rhs[0];
            final double b1 = rhs[1] - b0 * L10;
            final double b2 = rhs[2] - b0 * L20 - b1 * L21;

            // ______ Solution: backwards substitution ______
            result[2] = b2 / U22;
            result[1] = (b1 - U12 * result[2]) / U11;
            result[0] = (b0 - A[0][1] * result[1] - A[0][2] * result[2]) / A[0][0];

            return result;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("solve33", e);
        }

        return null;
    }

    private double computeSlantRange(final double[] pointOnEllips, final double firstLineTime,
                                            final double lineTimeInterval, final double wavelength,
                                            final SARGeocoding.Orbit orbit) {

        try {
            double timeAzimuth = firstLineTime + 0.25*slvImageHeight*lineTimeInterval;

            double[] satellitePosition = new double[3];
            double[] satelliteVelocity = new double[3];
            double[] satelliteAcceleration = new double[3];
            double[] delta = new double[3];
            for (int iter = 0; iter <= MAXITER; ++iter) {
                //orbit.getPosition(timeAzimuth, satellitePosition);
                //orbit.getVelocity(timeAzimuth, satelliteVelocity);
                //orbit.getAcceleration(timeAzimuth, satelliteAcceleration);

                delta[0] = pointOnEllips[0] - satellitePosition[0];
                delta[1] = pointOnEllips[1] - satellitePosition[1];
                delta[2] = pointOnEllips[2] - satellitePosition[2];

                // update solution
                final double solution = -dotProd(satelliteVelocity, delta) /
                        (dotProd(satelliteAcceleration, delta) - dotProd(satelliteVelocity, satelliteVelocity));

                timeAzimuth += solution;

                if (Math.abs(solution) < CRITERTIM) {
                    break;
                }
            }

            //orbit.getPosition(timeAzimuth, satellitePosition);
            delta[0] = pointOnEllips[0] - satellitePosition[0];
            delta[1] = pointOnEllips[1] - satellitePosition[1];
            delta[2] = pointOnEllips[2] - satellitePosition[2];

            return Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1] + delta[2]*delta[2]);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeSlantRange", e);
        }

        return 0.0;
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
