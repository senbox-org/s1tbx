/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
import Jama.SingularValueDecomposition;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.util.ResourceUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * The operator performs the following operations for all master/slave pairs that user selected:
 *
 * 1. For both PCA images read in the min values computed in the previous step by PCAMinOp;
 * 2. Read also the eigendevector matrix saved in the temporary metadata;
 * 3. Compute the PCA images again and substract the min value from each;
 * 4. Output the final PCA images to target product.
 */

@OperatorMetadata(alias="PCA", description="Principle Component Analysis", internal = false, category="Analysis")
public class PCAOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {EIGENVALUE_THRESHOLD, NUMBER_EIGENVALUES},
            defaultValue = EIGENVALUE_THRESHOLD, label="Select Eigenvalues By:")
    private String selectEigenvaluesBy = EIGENVALUE_THRESHOLD;

    @Parameter(description = "The threshold for selecting eigenvalues", interval = "(0, 100]", defaultValue = "100",
                label="Eigenvalue Threshold (%)")
    private double eigenvalueThreshold = 100.0;

    @Parameter(description = "The number of PCA images output", interval = "(0, 100]", defaultValue = "1",
                label="Number Of PCA Images")
    private int numPCA = 1;

    @Parameter(description = "Show the eigenvalues", defaultValue = "1", label="Show Eigenvalues")
    private boolean showEigenvalues = false;

    @Parameter(description = "Subtract mean image", defaultValue = "1", label="Subtract Mean Image")
    private boolean subtractMeanImage = false;

    private boolean statsCalculated = false;
    private int numOfPixels = 0;        // total number of pixel values
    private int numOfSourceBands = 0;   // number of user selected bands
    private double[] sum = null;        // summation of pixel values for each band
    private double[][] sumCross = null; // summation of the dot product of each band and the master band
    private double[] mean = null;       // mean of pixel values for each band
    private double[][] meanCross = null;// mean of the dot product of each band and the master band

    public static final String EIGENVALUE_THRESHOLD = "Eigenvalue Threshold";
    public static final String NUMBER_EIGENVALUES = "Number of Eigenvalues";
    private static final String meanImageBandName = "Mean_Image";

    private double totalEigenvalues; // summation of all eigenvalues

    private boolean pcaImageComputed = false;
    private double[][] eigenVectorMatrices = null; // eigenvector matrices for all slave bands
    private double[] eigenValues = null; // eigenvalues for all slave bands
    private double[] minPCA = null; // min value for first and second PCA images for all master/slave band pairs

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public PCAOp() {
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (selectEigenvaluesBy.equals(NUMBER_EIGENVALUES) && numPCA > sourceBandNames.length) {
                throw new OperatorException("The number of eigenvalues should not be greater than the number of selected bands");
            }

            createTargetProduct();

            addSelectedBands();

            setInitialValues();
        } catch(Throwable e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Set initial values to some internal variables.
     */
    private void setInitialValues() {

        mean = new double[numOfSourceBands];
        meanCross = new double[numOfSourceBands][numOfSourceBands];
        sum = new double[numOfSourceBands];
        sumCross = new double[numOfSourceBands][numOfSourceBands];
        for (int i = 0; i < numOfSourceBands; i++) {
            sum[i] = 0.0;
            mean[i] = 0.0;
            for (int j = 0; j < numOfSourceBands; j++) {
                sumCross[i][j] = 0.0;
                meanCross[i][j] = 0.0;
            }
        }

        numOfPixels = sourceProduct.getSceneRasterWidth() * sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setDescription(sourceProduct.getDescription());
    }

    /**
     * Add user selected slave bands to target product.
     */
    private void addSelectedBands() {

        // if no source band is selected by user, then select all bands
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        numOfSourceBands = sourceBandNames.length;

        if (numOfSourceBands <= 1) {
            throw new OperatorException("For PCA, more than one band should be selected");
        }

        // add PCA bands in target product
        final Band sourcerBand = sourceProduct.getBand(sourceBandNames[0]);
        if (sourcerBand == null) {
            throw new OperatorException("Source band not found: " + sourcerBand);
        }

        if (selectEigenvaluesBy.equals(EIGENVALUE_THRESHOLD)) {
            numPCA = numOfSourceBands; 
        }

        final int imageWidth = sourcerBand.getRasterWidth();
        final int imageHeight = sourcerBand.getRasterHeight();
        final String unit = sourcerBand.getUnit();

        for (int i = 0; i < numPCA; i++) {
            final String targetBandName = "PC" + i;
            final Band targetBand = new Band(targetBandName, ProductData.TYPE_FLOAT32, imageWidth, imageHeight);
            targetBand.setUnit(unit);
            targetProduct.addBand(targetBand);
        }

        if (subtractMeanImage) {
            createMeanImageVirtualBand(sourceProduct, sourceBandNames, meanImageBandName);
        }
    }

    /**
     * Create mean image as a virtual band from user selected bands.
     * @param sourceProduct The source product.
     * @param sourceBandNames The user selected band names.
     * @param meanImageBandName The mean image band name.
     */
    private static void createMeanImageVirtualBand(final Product sourceProduct,
                  final String[] sourceBandNames, final String meanImageBandName) {

        if (sourceProduct.getBand(meanImageBandName) != null) {
            return;
        }

        boolean isFirstBand = true;
        String unit = "";
        String expression = "( ";
        for (String bandName : sourceBandNames) {
            if (isFirstBand) {
                expression += bandName;
                unit = sourceProduct.getBand(bandName).getUnit();
                isFirstBand = false;
            } else {
                expression += " + " + bandName;
            }
        }
        expression += " ) / " + sourceBandNames.length;

        final VirtualBand band = new VirtualBand(meanImageBandName,
                                                 ProductData.TYPE_FLOAT32,
                                                 sourceProduct.getSceneRasterWidth(),
                                                 sourceProduct.getSceneRasterHeight(),
                                                 expression);
        band.setUnit(unit);
        band.setDescription("Mean image");
        sourceProduct.addBand(band);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
                                throws OperatorException {

        try {
            if(!statsCalculated) {
                calculateStatistics();
            }

            final ProductData[] bandsRawSamples = new ProductData[numOfSourceBands];
            for (int i = 0; i < numOfSourceBands; i++) {
                bandsRawSamples[i] =
                        getSourceTile(sourceProduct.getBand(sourceBandNames[i]), targetRectangle).getRawSamples();
            }
            final int n = bandsRawSamples[0].getNumElems();

            for (int i = 0; i < numPCA; i++) {

                final Band targetBand = targetProduct.getBand("PC" + i);
                final Tile targetTile = targetTileMap.get(targetBand);
                final ProductData trgData = targetTile.getDataBuffer();

                for (int k = 0; k < n; k++) {
                    double vPCA = 0.0;
                    for (int j = 0; j < numOfSourceBands; j++) {
                        vPCA += bandsRawSamples[j].getElemDoubleAt(k)*eigenVectorMatrices[j][i];
                    }
                    trgData.setElemDoubleAt(k, vPCA - minPCA[i]);
                }
            }

        } catch(Throwable e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }

        pcaImageComputed = true;
    }

    private synchronized void calculateStatistics() {

        if (statsCalculated) {
            return;
        }

        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = getAllTileRectangles(sourceProduct, tileSize);

        processStatistics(tileRectangles);

        processMin(tileRectangles);

        statsCalculated = true;
    }

    /**
     * Get an array of rectangles for all source tiles of the image
     * @param sourceProduct the input product
     * @param tileSize the rect sizes
     * @return Array of rectangles
     */
    private static Rectangle[] getAllTileRectangles(final Product sourceProduct, final Dimension tileSize) {

        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        final int rasterWidth = sourceProduct.getSceneRasterWidth();

        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);

        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);

        final Rectangle[] rectangles = new Rectangle[tileCountX * tileCountY];
        int index = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                final Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                              tileY * tileSize.height,
                                                              tileSize.width,
                                                              tileSize.height);
                final Rectangle intersection = boundary.intersection(tileRectangle);
                rectangles[index] = intersection;
                index++;
            }
        }
        return rectangles;
    }

    private void processStatistics(final Rectangle[] tileRectangles) {
        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Computing Statistics... ");
        int tileCnt = 0;

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (final Rectangle rectangle : tileRectangles) {

                Thread worker = new Thread() {
                    final ProductData[] bandsRawSamples = new ProductData[numOfSourceBands];
                    final double[] tileSum = new double[numOfSourceBands];
                    final double[][] tileSumCross = new double[numOfSourceBands][numOfSourceBands];

                    @Override
                    public void run() {
                        for (int i = 0; i < numOfSourceBands; i++) {
                            bandsRawSamples[i] =
                                    getSourceTile(sourceProduct.getBand(sourceBandNames[i]), rectangle).getRawSamples();
                        }

                        if (subtractMeanImage) {

                            final ProductData meanBandRawSamples =
                                    getSourceTile(sourceProduct.getBand(meanImageBandName), rectangle).getRawSamples();

                            computeTileStatisticsWithMeanImageSubstract(numOfSourceBands,
                                    bandsRawSamples, meanBandRawSamples, tileSum, tileSumCross);

                        } else {

                            computeTileStatisticsWithoutMeanImageSubstract(numOfSourceBands,
                                    bandsRawSamples, tileSum, tileSumCross);
                        }

                        synchronized(sum) {
                            computeImageStatistics(tileSum, tileSumCross);
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(tileCnt++);
            }
            threadManager.finish();

            completeStatistics();

        } catch(Throwable e) {
            throw new OperatorException(e);
        } finally {
            status.done();
        }
    }

    private void processMin(final Rectangle[] tileRectangles) {
        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Computing Min... ");
        int tileCnt = 0;

        final ThreadManager threadManager = new ThreadManager();

        try {
            initializeMin();

            for(final Rectangle rectangle : tileRectangles) {
                Thread worker = new Thread() {
                    final double[] tileMinPCA = new double[numOfSourceBands];
                    final ProductData[] bandsRawSamples = new ProductData[numOfSourceBands];

                    @Override
                    public void run() {
                        for (int i = 0; i < numOfSourceBands; i++) {
                            bandsRawSamples[i] =
                                    getSourceTile(sourceProduct.getBand(sourceBandNames[i]), rectangle).getRawSamples();
                        }
                        final int n = bandsRawSamples[0].getNumElems();

                        Arrays.fill(tileMinPCA, Double.MAX_VALUE);

                        for (int i = 0; i < numPCA; i++) {
                            for (int k = 0; k < n; k++) {
                                double vPCA = 0.0;
                                for (int j = 0; j < numOfSourceBands; j++) {
                                    vPCA += bandsRawSamples[j].getElemDoubleAt(k)*eigenVectorMatrices[j][i];
                                }
                                if(vPCA < tileMinPCA[i])
                                    tileMinPCA[i] = vPCA;
                            }
                        }

                        synchronized (minPCA) {
                            computePCAMin(tileMinPCA);
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(tileCnt++);
            }

            threadManager.finish();

        } catch(Throwable e) {
            throw new OperatorException(e);
        } finally {
            status.done();
        }
    }

    /**
     * Compute summation and cross-summation for all bands for a given tile.
     * @param numOfSourceBands numnber of bands
     * @param bandsRawSamples The raw data for all bands for the given tile.
     * @param tileSum The summation for all bands for the given tile.
     * @param tileSumCross The cross-summation for all bands for the given tile.
     */
    private static void computeTileStatisticsWithoutMeanImageSubstract (final int numOfSourceBands,
            final ProductData[] bandsRawSamples, final double[] tileSum, final double[][] tileSumCross) {

        Arrays.fill(tileSum, 0.0);
        final int n = bandsRawSamples[0].getNumElems();

        double vi, vj;
        for (int i = 0; i < numOfSourceBands; i++) {
            Arrays.fill(tileSumCross[i], 0.0);

            for (int j = 0; j <= i; j++) {

                //System.out.println("i = " + i + ", j = " + j);
                if (j < i) {

                    for (int k = 0; k < n; k++) {
                        vi = bandsRawSamples[i].getElemDoubleAt(k);
                        vj = bandsRawSamples[j].getElemDoubleAt(k);
                        tileSumCross[i][j] += vi*vj;
                    }

                } else { // j == i

                    for (int k = 0; k < n; k++) {
                        vi = bandsRawSamples[i].getElemDoubleAt(k);
                        tileSum[i] += vi;
                        tileSumCross[i][j] += vi*vi;
                    }
                }
            }
        }
    }

    /**
     * Compute summation and cross-summation for all bands for a given tile with mean image substracted.
     * @param numOfSourceBands numnber of bands
     * @param bandsRawSamples The raw data for all bands for the given tile.
     * @param meanBandRawSamples The raw data for the band of mean image for the given tile.
     * @param tileSum The summation for all bands for the given tile.
     * @param tileSumCross The cross-summation for all bands for the given tile.
     */
    private static void computeTileStatisticsWithMeanImageSubstract (
            final int numOfSourceBands,
            final ProductData[] bandsRawSamples, final ProductData meanBandRawSamples,
            final double[] tileSum, final double[][] tileSumCross) {

        Arrays.fill(tileSum, 0.0);
        final int n = bandsRawSamples[0].getNumElems();

        double vi, vj, vm;
        for (int i = 0; i < numOfSourceBands; i++) {
            Arrays.fill(tileSumCross[i], 0.0);

            for (int j = 0; j <= i; j++) {

                //System.out.println("i = " + i + ", j = " + j);
                if (j < i) {

                    for (int k = 0; k < n; k++) {
                        vm = meanBandRawSamples.getElemDoubleAt(k);
                        vi = bandsRawSamples[i].getElemDoubleAt(k) - vm;
                        vj = bandsRawSamples[j].getElemDoubleAt(k) - vm;
                        tileSumCross[i][j] += vi*vj;
                    }

                } else { // j == i

                    for (int k = 0; k < n; k++) {
                        vm = meanBandRawSamples.getElemDoubleAt(k);
                        vi = bandsRawSamples[i].getElemDoubleAt(k) - vm;
                        tileSum[i] += vi;
                        tileSumCross[i][j] += vi*vi;
                    }
                }
            }
        }
    }

    /**
     * Compute summation and cross-summation for the whole image.
     * @param tileSum The summation computed for each tile.
     * @param tileSumCross The cross-summation computed for each tile.
     */
    private void computeImageStatistics (final double[] tileSum, final double[][] tileSumCross) {

        for (int i = 0; i < numOfSourceBands; i++) {
            for (int j = 0; j <= i; j++) {
                if (j < i) {
                    sumCross[i][j] += tileSumCross[i][j];
                } else { // j == i
                    sum[i] += tileSum[i];
                    sumCross[i][j] += tileSumCross[i][j];
                }
            }
        }
    }

    private void completeStatistics() {
        for (int i = 0; i < numOfSourceBands; i++)  {
            mean[i] = sum[i] / numOfPixels;
            for (int j = 0; j <= i; j++) {
                meanCross[i][j] = sumCross[i][j] / numOfPixels;
                if (j != i) {
                    meanCross[j][i] = meanCross[i][j];
                }
            }
        }
    }

    /////////////
    // Min

    /**
     * Set initial values to some internal variables.
     */
    private void initializeMin() {

        minPCA = new double[numOfSourceBands];
        for (int i = 0; i < numOfSourceBands; i++) {
            minPCA[i] = Double.MAX_VALUE;
        }

        computeEigenDecompositionOfCovarianceMatrix();
    }

    /**
     * Compute minimum values for all PCA images.
     * @param tileMinPCA The minimum values for all PCA images for a given tile.
     */
    private void computePCAMin(final double[] tileMinPCA) {
        for (int i = 0; i < numPCA; i++) {
            if (tileMinPCA[i] < minPCA[i]) {
                minPCA[i] = tileMinPCA[i];
            }
        }
    }

    /**
     * Compute covariance matrices and perform EVD on each of them.
     */
    private void computeEigenDecompositionOfCovarianceMatrix() {

        eigenVectorMatrices = new double[numOfSourceBands][numOfSourceBands];
        eigenValues = new double[numOfSourceBands];

        final double[][] cov = new double[numOfSourceBands][numOfSourceBands];
        for (int i = 0; i < numOfSourceBands; i++) {
            for (int j = 0; j < numOfSourceBands; j++) {
                cov[i][j] = meanCross[i][j] - mean[i]*mean[j];
            }
        }

        final Matrix Cov = new Matrix(cov);
        final SingularValueDecomposition Svd = Cov.svd(); // Cov = USV'
        final Matrix S = Svd.getS();
        final Matrix U = Svd.getU();
        //final Matrix V = Svd.getV();

        totalEigenvalues = 0.0;
        for (int i = 0; i < numOfSourceBands; i++) {
            eigenValues[i] = S.get(i,i);
            totalEigenvalues += eigenValues[i];
            for (int j = 0; j < numOfSourceBands; j++) {
                eigenVectorMatrices[i][j] = U.get(i,j);
            }
        }

        if (selectEigenvaluesBy.equals(EIGENVALUE_THRESHOLD)) {
            double sum = 0.0;
            for (int i = 0; i < numOfSourceBands; i++) {
                sum += eigenValues[i];
                if (sum / totalEigenvalues >= eigenvalueThreshold) {
                    numPCA = i + 1;
                    break;
                }
            }
        }
    }

    /**
     * Compute statistics for the whole image.
     */
    @Override
    public void dispose() {

        if (!pcaImageComputed) {
            return;
        }
        createReportFile();
    }

    private void createReportFile() {

        final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        if(!appUserDir.exists()) {
            appUserDir.mkdirs();
        }

        final File reportFile = new File(appUserDir, sourceProduct.getName() + "_pca_report.txt");
        try {
            final FileOutputStream out = new FileOutputStream(reportFile);

            // Connect print stream to the output stream
            final PrintStream p = new PrintStream(out);

            p.println();
            p.println("User Selected Bands: ");
            for (int i = 0; i < numOfSourceBands; i++) {
                p.println("    " + sourceBandNames[i]);
            }
            p.println();
            if (selectEigenvaluesBy.equals(EIGENVALUE_THRESHOLD)) {
                p.println("User Input Eigenvalue Threshold: " + eigenvalueThreshold + " %");
                p.println();
            }
            p.println("Number of PCA Images Output: " + numPCA);
            p.println();
            p.println("Normalized Eigenvalues: ");
            for (int i = 0; i < numOfSourceBands; i++)  {
                p.println("    " + eigenValues[i]);
            }
            p.println();
            p.close();

            if(showEigenvalues) {
                Desktop.getDesktop().edit(reportFile);
            }
        } catch(IOException exc) {
            throw new OperatorException(exc);
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PCAOp.class);
            //super.setOperatorUI(PCAStatisticsOpUI.class);
        }
    }
}