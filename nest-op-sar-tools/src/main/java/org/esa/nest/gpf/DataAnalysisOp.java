/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.util.ResourceUtils;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * The operator evaluates the following local statistics for the user selected area of the image, and produces
 * an image file of the result:
 *
 * 1. Mean
 * 2. Standard deviation
 * 3. Coefficient of variation
 * 4. Equivalent number of looks
 *
 * @todo the computed statistics should be outout to file
 */

/**
 * The sample operator implementation for an algorithm
 * that can compute bands independently of each other.
 */
@OperatorMetadata(alias="Data-Analysis",
        category = "Analysis",
        description="Computes statistics",
        authors = "NEST team", copyright = "(C) 2013 by Array Systems Computing Inc.",
        internal=true)
public class DataAnalysisOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private final boolean writeToFile = true;

    private boolean statsCalculated = false;
    private boolean sampleTypeIsComplex;
    private int numOfBands;
    private int numOfPixels; // total number of pixel values
    private double[] min;    // min of all pixel values for each band
    private double[] max;    // max of all pixel values for each band
    private double[] sum;    // summation of all pixel values for each band
    private double[] sum2;   // summation of all pixel value squares for each band
    private double[] sum4;   // summation of all pixel value to the power of 4 for each band
    private double[] mean;   // mean for each band
    private double[] coefVar;// coefficient of variation for each band
    private double[] std;    // standard deviation for each band
    private double[] enl;    // equivalent number of looks for each band

    private final HashMap<String, Integer> statisticsBandIndex = new HashMap<String, Integer>();


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public DataAnalysisOp() {
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
            //final MetadataElement abs = OperatorUtils.getAbstractedMetadata(sourceProduct);
            //sampleTypeIsComplex = abs.getAttributeString("sample_type").contains("COMPLEX");

            getNumOfBandsForStatistics();

            setInitialValues();

            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get the number of bands for which statistics are computed.
     */
    void getNumOfBandsForStatistics() {

        numOfBands = 0;
        for(Band band : sourceProduct.getBands()) {
            statisticsBandIndex.put(band.getName(), numOfBands);
            numOfBands++;
        }
    }

    /**
     * Set initial values to some internal variables.
     */
    void setInitialValues() {

        min = new double[numOfBands];
        max = new double[numOfBands];
        mean = new double[numOfBands];
        coefVar = new double[numOfBands];
        std = new double[numOfBands];
        enl = new double[numOfBands];
        sum = new double[numOfBands];
        sum2 = new double[numOfBands];
        sum4 = new double[numOfBands];
        for (int i = 0; i < numOfBands; i++) {
            min[i] = Double.MAX_VALUE;
            max[i] = 0.0;
            sum[i] = 0.0;
            sum2[i] = 0.0;
            sum4[i] = 0.0;
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

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for(Band band : sourceProduct.getBands()) {
            ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, false);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        computeStatistics(targetBand, targetTile, targetTile.getRectangle());
    }

    /**
     * Compute statistics for given source tile.
     * @param targetBand
     * @param targetTile
     * @param targetTileRectangle
     */
    void computeStatistics(Band targetBand, Tile targetTile, Rectangle targetTileRectangle) {

        final Band sourceBand1 = sourceProduct.getBand(targetBand.getName());
        final Tile sourceRaster1 = getSourceTile(sourceBand1, targetTileRectangle);
        final ProductData rawSamples1 = sourceRaster1.getRawSamples();

        final int idx = statisticsBandIndex.get(targetBand.getName());
        final int n = rawSamples1.getNumElems();
        double v, v2;
        for (int i = 0; i < n; i++) {

            if(sampleTypeIsComplex) {
                // todo
            }
            v = rawSamples1.getElemDoubleAt(i);
            if(v > max[idx])
                max[idx] = v;
            if(v < min[idx])
                min[idx] = v;
            v2 = v*v;
            sum[idx] += v;
            sum2[idx] += v2;
            sum4[idx] += v2*v2;
        }

        // copy source data to target
        targetTile.setRawSamples(rawSamples1);

        statsCalculated = true;
    }

    /**
     * Compute statistics for the whole image.
     */
    @Override
    public void dispose() {

        if (!statsCalculated) {
            return;
        }

        completeStatistics();

        if(writeToFile)
            writeStatsToFile();
    }

    private void completeStatistics() {
        for (String bandName : statisticsBandIndex.keySet())  {

                final int bandIdx = statisticsBandIndex.get(bandName);
                final double m = sum[bandIdx] / numOfPixels;
                final double m2 = sum2[bandIdx] / numOfPixels;
                final double m4 = sum4[bandIdx] / numOfPixels;

                mean[bandIdx] = m;
                std[bandIdx] = Math.sqrt(m2 - m*m);
                coefVar[bandIdx] = Math.sqrt(m4 - m2*m2) / m2;
                enl[bandIdx] = m2*m2 / (m4 - m2*m2);
        }
    }

    private void writeStatsToFile() {
        String fileName = sourceProduct.getName() + "_statistics.txt";
        try {
            final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
            if(!appUserDir.exists()) {
                appUserDir.mkdirs();
            }
            fileName = appUserDir.toString() + File.separator + fileName;
            final FileOutputStream out = new FileOutputStream(fileName);

            // Connect print stream to the output stream
            final PrintStream p = new PrintStream(out);

            p.println();
            for (String bandName : statisticsBandIndex.keySet())  {

                int bandIdx = statisticsBandIndex.get(bandName);

                p.println();
                p.println("Band: " + bandName);
                p.format("Total pixels = %d", numOfPixels);
                p.println();
                p.format("Min = %8.3f", min[bandIdx]);
                p.println();
                p.format("Max = %15.3f", max[bandIdx]);
                p.println();
                //p.format("Sum = %15.3f", sum[bandIdx]);
                //p.println();
                p.format("Mean = %8.3f", mean[bandIdx]);
                p.println();
                p.format("Standard deviation = %8.3f", std[bandIdx]);
                p.println();
                p.format("Coefficient of variation = %8.3f", coefVar[bandIdx]);
                p.println();
                p.format("Equivalent number of looks = %8.3f", enl[bandIdx]);
                p.println();
            }

            p.close();

        } catch(IOException exc) {
            throw new OperatorException(exc);
        }
    }

    private void writeStatsToStdOut() {
        /*
        for (String bandName : statisticsBandIndex.keySet())  {

            int bandIdx = statisticsBandIndex.get(bandName);

            System.out.println();
            System.out.println("Band: " + bandName);
            System.out.println("Total pixels = " + numOfPixels);
            System.out.println("min[" + bandIdx + "] = " + min[bandIdx]);
            System.out.println("max[" + bandIdx + "] = " + max[bandIdx]);
            System.out.println("sum[" + bandIdx + "] = " + sum[bandIdx]);
            System.out.println("mean[" + bandIdx + "] = " + mean[bandIdx]);
            System.out.println("std[" + bandIdx + "] = " + std[bandIdx]);
            System.out.println("coefVar[" + bandIdx + "] = " + coefVar[bandIdx]);
            System.out.println("enl[" + bandIdx + "] = " + enl[bandIdx]);
            System.out.println();
        }
        */
    }


    // The following functions are for unit test only.
    public int getNumOfBands() {
        return numOfBands;
    }

    public double getMin(int bandIdx) {
        return min[bandIdx];
    }

    public double getMax(int bandIdx) {
        return max[bandIdx];
    }

    public double getMean(int bandIdx) {
        return mean[bandIdx];
    }

    public double getStd(int bandIdx) {
        return std[bandIdx];
    }

    public double getVarCoef(int bandIdx) {
        return coefVar[bandIdx];
    }

    public double getENL(int bandIdx) {
        return enl[bandIdx];
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(DataAnalysisOp.class);
        }
    }
}
