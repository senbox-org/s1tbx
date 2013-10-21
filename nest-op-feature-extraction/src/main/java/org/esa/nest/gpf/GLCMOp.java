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
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;
import org.junit.Ignore;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The operator extracts 14 texture features using GLCM:
 * 1. Contrast
 * 2. Dissimilarity (DIS)
 * 3. Homogeneity (HOM)
 * 4. Angular Second Moment (ASM)
 * 5. Energy
 * 6. Maximum Probability (MAX)
 * 7. Entropy (ENT)
 * 8. GLCM Mean
 * 9. GLCM Variance
 * 10. GLCM Correlation
 *
 * [1] Robert M. Haralick, K. Shanmugam, and Its’hak Dinstein. “Textural Features for Image Classification”
 *     IEEE Trans. on Systems, Man and Cybernetics, Vol 3 , No. 6, pp. 610-621, Nov. 1973.
 */

@OperatorMetadata(alias="GLCM",
                  category = "Classification\\Feature Extraction",
                  description="Extract Texture Features")
public final class GLCMOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
            defaultValue = WINDOW_SIZE_9x9, label="Window Size")
    private String windowSizeStr = WINDOW_SIZE_9x9;

    @Parameter(valueSet = {ANGLE_0, ANGLE_45, ANGLE_90, ANGLE_135},
            defaultValue = ANGLE_0, label="Angle")
    private String angleStr = ANGLE_0;

    @Parameter(valueSet = {QUANTIZATION_LEVELS_16, QUANTIZATION_LEVELS_32, QUANTIZATION_LEVELS_64},
            defaultValue = QUANTIZATION_LEVELS_16, label="Quantization Levels")
    private String quantizationLevelsStr = QUANTIZATION_LEVELS_64;

    @Parameter(description = "Pixel displacement", interval = "[1, 10]", defaultValue = "1", label="Displacement")
    private int displacement = 1;

    @Parameter(description = "Output Contrast", defaultValue = "true",
            label="Contrast")
    private boolean outputContrast = true;

    @Parameter(description = "Output Dissimilarity", defaultValue = "true",
            label="Dissimilarity")
    private boolean outputDissimilarity = true;

    @Parameter(description = "Output Homogeneity", defaultValue = "true",
            label="Homogeneity")
    private boolean outputHomogeneity = true;

    @Parameter(description = "Output Angular Second Moment", defaultValue = "true",
            label="Angular Second Moment")
    private boolean outputASM = true;

    @Parameter(description = "Output Energy", defaultValue = "true",
            label="Energy")
    private boolean outputEnergy = true;

    @Parameter(description = "Output Maximum Probability", defaultValue = "true",
            label="Maximum Probability")
    private boolean outputMAX = true;

    @Parameter(description = "Output Entropy", defaultValue = "true",
            label="Entropy")
    private boolean outputEntropy = true;

    @Parameter(description = "Output GLCM Mean", defaultValue = "true",
            label="GLCM Mean")
    private boolean outputMean = true;

    @Parameter(description = "Output GLCM Variance", defaultValue = "true",
            label="GLCM Variance")
    private boolean outputVariance = true;

    @Parameter(description = "Output GLCM Correlation", defaultValue = "true",
            label="GLCM Correlation")
    private boolean outputCorrelation = true;

    private int windowSize = 0;
    private int halfWindowSize = 0;
    private int numQuantLevels = 0;
    private int displacementX = 0;
    private int displacementY = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private double delta = 0.0;

    private List<String> targetBandNameList = new ArrayList<String>(10);

    private static final String ANGLE_0 = "0";
    private static final String ANGLE_45 = "45";
    private static final String ANGLE_90 = "90";
    private static final String ANGLE_135 = "135";

    private static final String QUANTIZATION_LEVELS_16 = "16";
    private static final String QUANTIZATION_LEVELS_32 = "32";
    private static final String QUANTIZATION_LEVELS_64 = "64";

    private static final String WINDOW_SIZE_5x5 = "5x5";
    private static final String WINDOW_SIZE_7x7 = "7x7";
    private static final String WINDOW_SIZE_9x9 = "9x9";
    private static final String WINDOW_SIZE_11x11 = "11x11";


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
            if (!outputContrast && !outputDissimilarity && !outputHomogeneity &&
                !outputASM && !outputEnergy && !outputMAX && !outputEntropy &&
                !outputMean && !outputVariance && !outputCorrelation) {
                throw new OperatorException("Please select output features.");
            }

            setWindowSize();

            setQuantizationLevels();

            setXYDisplacements();

            createTargetProduct();

            getImageMaxMin();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Set Window size.
     */
    private void setWindowSize() {

        switch (windowSizeStr) {
            case WINDOW_SIZE_5x5:
                windowSize = 5;
                break;
            case WINDOW_SIZE_7x7:
                windowSize = 7;
                break;
            case WINDOW_SIZE_9x9:
                windowSize = 9;
                break;
            case WINDOW_SIZE_11x11:
                windowSize = 11;
                break;
            default:
                throw new OperatorException("Unknown window size: " + windowSizeStr);
        }

        halfWindowSize = windowSize/2;

        if (displacement >= windowSize) {
            throw new OperatorException("Displacement should not be larger than window size.");
        }
    }

    /**
     * Set number of quantization levels.
     */
    private void setQuantizationLevels() {

        switch (quantizationLevelsStr) {
            case QUANTIZATION_LEVELS_16:
                numQuantLevels = 16;
                break;
            case QUANTIZATION_LEVELS_32:
                numQuantLevels = 32;
                break;
            case QUANTIZATION_LEVELS_64:
                numQuantLevels = 64;
                break;
            default:
                throw new OperatorException("Unknown number of quantization levels: " + quantizationLevelsStr);
        }
    }

    /**
     * Set pixel displacements in X and Y direction based on user selected angle.
     */
    private void setXYDisplacements() {

        switch (angleStr) {
            case ANGLE_0:
                displacementX = displacement;
                displacementY = 0;
                break;
            case ANGLE_45:
                displacementX = displacement;
                displacementY = displacement;
                break;
            case ANGLE_90:
                displacementX = 0;
                displacementY = displacement;
                break;
            case ANGLE_135:
                displacementX = -displacement;
                displacementY = displacement;
                break;
            default:
                throw new OperatorException("Unknown angle: " + angleStr);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        addSelectedBands();

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        // if user did not select any band, use the first intensity band
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            for (Band band : bands) {
                if(band.getUnit() != null && band.getUnit().equals(Unit.INTENSITY)) {
                    sourceBandNames = new String[1];
                    sourceBandNames[0] = band.getName();
                    break;
                }
            }
        }

        if (sourceBandNames.length > 1 || !sourceProduct.getBand(sourceBandNames[0]).getUnit().equals(Unit.INTENSITY)) {
            throw new OperatorException("Please select one intensity band.");
        }

        final Band targetBand = ProductUtils.copyBand(sourceBandNames[0], sourceProduct, targetProduct, false);
        targetBand.setSourceImage(sourceProduct.getBand(sourceBandNames[0]).getSourceImage());

        final String[] targetBandNames = getTargetBandNames();
        PolBandUtils.addBands(targetProduct, targetBandNames, "");
    }

    private String[] getTargetBandNames() {

        if (outputContrast) {
            targetBandNameList.add("Contrast");
        }

        if (outputDissimilarity) {
            targetBandNameList.add("Dissimilarity");
        }

        if (outputHomogeneity) {
            targetBandNameList.add("Homogeneity");
        }

        if (outputASM) {
            targetBandNameList.add("ASM");
        }

        if (outputEnergy) {
            targetBandNameList.add("Energy");
        }

        if (outputMAX) {
            targetBandNameList.add("MAX");
        }

        if (outputEntropy) {
            targetBandNameList.add("Entropy");
        }

        if (outputMean) {
            targetBandNameList.add("GLCMMean");
        }

        if (outputVariance) {
            targetBandNameList.add("GLCMVariance");
        }

        if (outputCorrelation) {
            targetBandNameList.add("GLCMCorrelation");
        }

        return targetBandNameList.toArray(new String[targetBandNameList.size()]);
    }

    private void getImageMaxMin() {
        final Band srcBand = sourceProduct.getBand(sourceBandNames[0]);
        delta = (srcBand.getStx().getMaximum() - srcBand.getStx().getMinimum())/numQuantLevels;
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final int tx0 = targetRectangle.x;
        final int ty0 = targetRectangle.y;
        final int tw  = targetRectangle.width;
        final int th  = targetRectangle.height;
        final int maxY = ty0 + th;
        final int maxX = tx0 + tw;
        //System.out.println("x0 = " + tx0 + ", y0 = " + ty0 + ", w = " + tw + ", h = " + th);

        try {
            final TileIndex trgIndex = new TileIndex(targetTiles.get(targetTiles.keySet().iterator().next()));
            final TileData[] tileDataList = new TileData[targetBandNameList.size()];
            int i = 0;
            for (String targetBandName : targetBandNameList){
                final Band targetBand = targetProduct.getBand(targetBandName);
                final Tile targetTile = targetTiles.get(targetBand);
                tileDataList[i++] = new TileData(targetTile, targetBand.getName());
            }

            final Rectangle sourceTileRectangle = getSourceTileRectangle(tx0, ty0, tw, th);
            final String srcBandName = sourceBandNames[0];
            final Band sourceBand = sourceProduct.getBand(srcBandName);
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcData = sourceTile.getDataBuffer();
            final double noDataValue = sourceBand.getNoDataValue();

            for (int ty = ty0; ty < maxY; ty++) {
                trgIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxX; tx++) {
                    final int idx = trgIndex.getIndex(tx);

                    final double[][] GLCM = computeGLCM(tx, ty, sourceTile, srcData, noDataValue);

                    TextureFeatures tf = computeTextureFeatures(GLCM);

                    for(final TileData tileData : tileDataList) {

                        if (outputContrast && tileData.bandName.equals("Contrast")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.Contrast);
                        } else if (outputDissimilarity && tileData.bandName.equals("Dissimilarity")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.Dissimilarity);
                        } else if (outputHomogeneity && tileData.bandName.equals("Homogeneity")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.Homogeneity);
                        } else if (outputASM && tileData.bandName.equals("ASM")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.ASM);
                        } else if (outputEnergy && tileData.bandName.equals("Energy")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.Energy);
                        } else if (outputMAX && tileData.bandName.equals("MAX")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.MAX);
                        } else if (outputEntropy && tileData.bandName.equals("Entropy")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.Entropy);
                        } else if (outputMean && tileData.bandName.equals("GLCMMean")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.GLCMMean);
                        } else if (outputVariance && tileData.bandName.equals("GLCMVariance")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.GLCMVariance);
                        } else if (outputCorrelation && tileData.bandName.equals("GLCMCorrelation")) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tf.GLCMCorrelation);
                        }
                    }
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source tile rectangle.
     * @param x0 X coordinate of pixel at the upper left corner of the target tile.
     * @param y0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param w The width of the target tile.
     * @param h The height of the target tile.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfWindowSize) {
            sx0 -= halfWindowSize;
            sw += halfWindowSize;
        }

        if (y0 >= halfWindowSize) {
            sy0 -= halfWindowSize;
            sh += halfWindowSize;
        }

        if (x0 + w + halfWindowSize <= sourceImageWidth) {
            sw += halfWindowSize;
        }

        if (y0 + h + halfWindowSize <= sourceImageHeight) {
            sh += halfWindowSize;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    private double[][] computeGLCM(final int tx, final int ty, final Tile sourceTile, final ProductData srcData,
                                   final double noDataValue) {

        final int x0 = Math.max(tx - halfWindowSize, 0);
        final int y0 = Math.max(ty - halfWindowSize, 0);
        final int w  = Math.min(tx + halfWindowSize, sourceImageWidth - 1) - x0 + 1;
        final int h  = Math.min(ty + halfWindowSize, sourceImageHeight - 1) - y0 + 1;
        final int maxy = Math.min(y0 + h, sourceTile.getMaxY() - 1) - displacementX;
        final int maxx = Math.min(x0 + w, sourceTile.getMaxX() - 1) - displacementY;

        final double[][] GLCM = new double[numQuantLevels][numQuantLevels];
        int counter = 0;
        for (int y = y0; y < maxy; y++) {
            for (int x = x0; x < maxx; x++) {
                final double vi = srcData.getElemDoubleAt(sourceTile.getDataBufferIndex(x,y));
                if (vi == noDataValue) {
                    continue;
                }

                final double vj = srcData.getElemDoubleAt(
                        sourceTile.getDataBufferIndex(x + displacementX, y + displacementY));
                if (vj == noDataValue) {
                    continue;
                }

                final int i = quantize(vi);
                final int j = quantize(vj);

                GLCM[i][j]++;
                GLCM[j][i]++;
                counter++;
            }
        }

        if (counter > 0) {
            for (int i = 0; i < numQuantLevels; i++) {
                for (int j = 0; j < numQuantLevels; j++) {
                    GLCM[i][j] /= counter;
                }
            }
        }

        return GLCM;
    }

    private TextureFeatures computeTextureFeatures(final double[][] GLCM) {

        TextureFeatures tf = new TextureFeatures();

        double Contrast = 0.0, Dissimilarity = 0.0, Homogeneity = 0.0, ASM = 0.0, MAX = 0.0, Entropy = 0.0,
               GLCMMeanX = 0.0,GLCMMeanY = 0.0;
        for (int i = 0; i < numQuantLevels; i++) {
            for (int j = 0; j < numQuantLevels; j++) {
                Contrast += GLCM[i][j]*(i-j)*(i-j);
                Dissimilarity += GLCM[i][j]*Math.abs(i-j);
                Homogeneity += GLCM[i][j]/(1 + (i-j)*(i-j));
                ASM += GLCM[i][j]*GLCM[i][j];

                if (MAX < GLCM[i][j])
                    MAX = GLCM[i][j];

                Entropy += -GLCM[i][j]*Math.log(GLCM[i][j] + Constants.EPS);
                GLCMMeanY += GLCM[i][j]*i;
                GLCMMeanX += GLCM[i][j]*j;
            }
        }

        double GLCMVarianceX = 0.0, GLCMVarianceY = 0.0;
        for (int i = 0; i < numQuantLevels; i++) {
            for (int j = 0; j < numQuantLevels; j++) {
                GLCMVarianceX += GLCM[i][j]*(j - GLCMMeanX)*(j - GLCMMeanX);
                GLCMVarianceY += GLCM[i][j]*(i - GLCMMeanY)*(i - GLCMMeanY);
            }
        }

        double GLCMCorrelation = 0.0;
        for (int i = 0; i < numQuantLevels; i++) {
            for (int j = 0; j < numQuantLevels; j++) {
                GLCMCorrelation += GLCM[i][j]*(i - GLCMMeanY)*(j - GLCMMeanX)/Math.sqrt(GLCMVarianceX*GLCMVarianceY);
            }
        }

        tf.Contrast = Contrast;
        tf.Dissimilarity = Dissimilarity;
        tf.Homogeneity = Homogeneity;
        tf.ASM = ASM;
        tf.Energy = Math.sqrt(ASM);
        tf.MAX = MAX;
        tf.Entropy = Entropy;
        tf.GLCMMean = (GLCMMeanX + GLCMMeanY)/2.0;
        tf.GLCMVariance = (GLCMVarianceX + GLCMVarianceY)/2.0;
        tf.GLCMCorrelation = GLCMCorrelation;

        return tf;
    }

    private int quantize(final double v) {
        return Math.min((int)(v/delta), numQuantLevels-1);
    }

    private static class TileData {
        final Tile tile;
        final ProductData dataBuffer;
        final String bandName;

        public TileData(final Tile tile, final String bandName) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.bandName = bandName;
        }
    }

    private static class TextureFeatures {

        public double Contrast;
        public double Dissimilarity;
        public double Homogeneity;
        public double ASM;
        public double Energy;
        public double MAX;
        public double Entropy;
        public double GLCMMean;
        public double GLCMVariance;
        public double GLCMCorrelation;
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
            super(GLCMOp.class);
        }
    }
}