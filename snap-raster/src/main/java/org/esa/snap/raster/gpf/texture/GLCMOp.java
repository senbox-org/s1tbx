/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.raster.gpf.texture;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import javax.media.jai.Histogram;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The operator extracts 10 texture features using GLCM:
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
 * <p>
 * [1] Robert M. Haralick, K. Shanmugam, and Its'hak Dinstein. "Textural Features for Image Classification"
 * IEEE Trans. on Systems, Man and Cybernetics, Vol 3 , No. 6, pp. 610-621, Nov. 1973.
 */

@OperatorMetadata(alias = "GLCM",
        category = "Raster/Image Analysis/Texture Analysis",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Extract Texture Features")
public final class GLCMOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
            defaultValue = WINDOW_SIZE_9x9, label = "Window Size")
    private String windowSizeStr = WINDOW_SIZE_9x9;

    @Parameter(valueSet = {ANGLE_0, ANGLE_45, ANGLE_90, ANGLE_135, ANGLE_ALL},
            defaultValue = ANGLE_ALL, label = "Angle")
    private String angleStr = ANGLE_ALL;

    @Parameter(valueSet = {EQUAL_DISTANCE_QUANTIZER, PROBABILISTIC_QUANTIZER},
            defaultValue = PROBABILISTIC_QUANTIZER, label = "Quantizer")
    private String quantizerStr = PROBABILISTIC_QUANTIZER;

    @Parameter(valueSet = {QUANTIZATION_LEVELS_8, QUANTIZATION_LEVELS_16, QUANTIZATION_LEVELS_32,
            QUANTIZATION_LEVELS_64, QUANTIZATION_LEVELS_128},
            defaultValue = QUANTIZATION_LEVELS_32, label = "Quantization Levels")
    private String quantizationLevelsStr = QUANTIZATION_LEVELS_64;

    @Parameter(description = "Pixel displacement", interval = "[1, 10]", defaultValue = "1", label = "Displacement")
    private int displacement = 1;

    @Parameter(description = "Output Contrast", defaultValue = "true", label = "Contrast")
    private Boolean outputContrast = true;

    @Parameter(description = "Output Dissimilarity", defaultValue = "true", label = "Dissimilarity")
    private Boolean outputDissimilarity = true;

    @Parameter(description = "Output Homogeneity", defaultValue = "true", label = "Homogeneity")
    private Boolean outputHomogeneity = true;

    @Parameter(description = "Output Angular Second Moment", defaultValue = "true", label = "Angular Second Moment")
    private Boolean outputASM = true;

    @Parameter(description = "Output Energy", defaultValue = "true", label = "Energy")
    private Boolean outputEnergy = true;

    @Parameter(description = "Output Maximum Probability", defaultValue = "true", label = "Maximum Probability")
    private Boolean outputMAX = true;

    @Parameter(description = "Output Entropy", defaultValue = "true", label = "Entropy")
    private Boolean outputEntropy = true;

    @Parameter(description = "Output GLCM Mean", defaultValue = "true", label = "GLCM Mean")
    private Boolean outputMean = true;

    @Parameter(description = "Output GLCM Variance", defaultValue = "true", label = "GLCM Variance")
    private Boolean outputVariance = true;

    @Parameter(description = "Output GLCM Correlation", defaultValue = "true", label = "GLCM Correlation")
    private Boolean outputCorrelation = true;

    private int halfWindowSize = 0;
    private int numQuantLevels = 0;
    private int displacementX = 0;
    private int displacementY = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private boolean useProbabilisticQuantizer = false;
    private boolean quantizerAvailable = false;
    private boolean computeGLCPWithAllAngles = false;

    private Quantizer quantizer;
    private String[] targetBandNames;

    private static final String ANGLE_0 = "0";
    private static final String ANGLE_45 = "45";
    private static final String ANGLE_90 = "90";
    private static final String ANGLE_135 = "135";
    private static final String ANGLE_ALL = "ALL";

    private static final String EQUAL_DISTANCE_QUANTIZER = "Equal Distance Quantizer";
    private static final String PROBABILISTIC_QUANTIZER = "Probabilistic Quantizer";

    private static final String QUANTIZATION_LEVELS_8 = "8";
    private static final String QUANTIZATION_LEVELS_16 = "16";
    private static final String QUANTIZATION_LEVELS_32 = "32";
    private static final String QUANTIZATION_LEVELS_64 = "64";
    private static final String QUANTIZATION_LEVELS_96 = "96";
    private static final String QUANTIZATION_LEVELS_128 = "128";

    private static final String WINDOW_SIZE_5x5 = "5x5";
    private static final String WINDOW_SIZE_7x7 = "7x7";
    private static final String WINDOW_SIZE_9x9 = "9x9";
    private static final String WINDOW_SIZE_11x11 = "11x11";

    enum GLCM_TYPES {
        Contrast,
        Dissimilarity,
        Homogeneity,
        ASM,
        Energy,
        MAX,
        Entropy,
        GLCMMean,
        GLCMVariance,
        GLCMCorrelation,
        Unknown
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
            if (!outputContrast && !outputDissimilarity && !outputHomogeneity &&
                    !outputASM && !outputEnergy && !outputMAX && !outputEntropy &&
                    !outputMean && !outputVariance && !outputCorrelation) {
                throw new OperatorException("Please select output features.");
            }

            setWindowSize();

            setQuantizer();

            setQuantizationLevels();

            setXYDisplacements();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Set Window size.
     */
    private void setWindowSize() {

        int windowSize = 0;
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

        halfWindowSize = windowSize / 2;

        if (displacement >= windowSize) {
            throw new OperatorException("Displacement should not be larger than window size.");
        }
    }

    /**
     * Set quantizer flag.
     */
    private void setQuantizer() {
        useProbabilisticQuantizer = quantizerStr.equals(PROBABILISTIC_QUANTIZER);
    }

    /**
     * Set number of quantization levels.
     */
    private void setQuantizationLevels() {

        switch (quantizationLevelsStr) {
            case QUANTIZATION_LEVELS_8:
                numQuantLevels = 8;
                break;
            case QUANTIZATION_LEVELS_16:
                numQuantLevels = 16;
                break;
            case QUANTIZATION_LEVELS_32:
                numQuantLevels = 32;
                break;
            case QUANTIZATION_LEVELS_64:
                numQuantLevels = 64;
                break;
            case QUANTIZATION_LEVELS_96:
                numQuantLevels = 96;
                break;
            case QUANTIZATION_LEVELS_128:
                numQuantLevels = 128;
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
                displacementX = -displacement;
                displacementY = displacement;
                break;
            case ANGLE_90:
                displacementX = 0;
                displacementY = displacement;
                break;
            case ANGLE_135:
                displacementX = displacement;
                displacementY = displacement;
                break;
            case ANGLE_ALL:
                computeGLCPWithAllAngles = true;
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

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    private void getSourceBands() {
        final List<String> srcBandNameList = new ArrayList<>();
        if (sourceBandNames != null) {
            // remove band names specific to another run
            for (String srcBandName : sourceBandNames) {
                final Band srcBand = sourceProduct.getBand(srcBandName);
                if (srcBand != null) {
                    srcBandNameList.add(srcBand.getName());
                }
            }
        }

        // if user did not select any band, use the first intensity band
        if (srcBandNameList.isEmpty()) {

            final Band[] srcBands = sourceProduct.getBands();
            for (Band srcBand : srcBands) {
                String bandUnit = srcBand.getUnit();
                if (bandUnit != null && (bandUnit.contains(Unit.INTENSITY)) || bandUnit.contains(Unit.AMPLITUDE)) {
                    srcBandNameList.add(srcBand.getName());
                }
            }
            if (srcBandNameList.isEmpty()) {
                srcBandNameList.add(sourceProduct.getBandAt(0).getName());
            }
        }
        sourceBandNames = srcBandNameList.toArray(new String[srcBandNameList.size()]);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        getSourceBands();

        targetBandNames = getTargetBandNames();
        final Band[] bands = OperatorUtils.addBands(targetProduct, targetBandNames, "");
        for (Band band : bands) {
            band.setNoDataValueUsed(true);
        }
    }

    private String[] getTargetBandNames() {

        final List<String> trgBandNames = new ArrayList<>();
        for (String srcBandName : sourceBandNames) {
            if (outputContrast) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.Contrast.toString());
            }
            if (outputDissimilarity) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.Dissimilarity.toString());
            }
            if (outputHomogeneity) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.Homogeneity.toString());
            }
            if (outputASM) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.ASM.toString());
            }
            if (outputEnergy) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.Energy.toString());
            }
            if (outputMAX) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.MAX.toString());
            }
            if (outputEntropy) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.Entropy.toString());
            }
            if (outputMean) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.GLCMMean.toString());
            }
            if (outputVariance) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.GLCMVariance.toString());
            }
            if (outputCorrelation) {
                trgBandNames.add(srcBandName + '_' + GLCM_TYPES.GLCMCorrelation.toString());
            }
        }

        return trgBandNames.toArray(new String[trgBandNames.size()]);
    }

    private synchronized void createQuantizer() {

        if (quantizerAvailable) {
            return;
        }

        final Band srcBand = sourceProduct.getBand(sourceBandNames[0]);
        if (useProbabilisticQuantizer) {
            quantizer = new ProbabilityQuantizer(srcBand, numQuantLevels);
        } else {
            quantizer = new EqualDistanceQuantizer(srcBand, numQuantLevels);
        }

        quantizerAvailable = true;
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        if (!quantizerAvailable) {
            createQuantizer();
        }

        final int tx0 = targetRectangle.x;
        final int ty0 = targetRectangle.y;
        final int tw = targetRectangle.width;
        final int th = targetRectangle.height;
        final int maxY = ty0 + th;
        final int maxX = tx0 + tw;
        //System.out.println("x0 = " + tx0 + ", y0 = " + ty0 + ", w = " + tw + ", h = " + th);

        try {
            final TileIndex trgIndex = new TileIndex(targetTiles.get(targetTiles.keySet().iterator().next()));
            final Rectangle sourceTileRectangle = getSourceTileRectangle(tx0, ty0, tw, th);
            final SrcInfo[] srcInfoList = new SrcInfo[sourceBandNames.length];

            int cnt = 0;
            for (String srcBandName : sourceBandNames) {
                final Band sourceBand = sourceProduct.getBand(srcBandName);
                srcInfoList[cnt] = new SrcInfo(numQuantLevels, sourceBand, getSourceTile(sourceBand, sourceTileRectangle));

                final List<TileData> tileDataList = new ArrayList<>();
                for (String targetBandName : targetBandNames) {
                    if (targetBandName.startsWith(srcBandName)) {
                        final Band targetBand = targetProduct.getBand(targetBandName);
                        final Tile targetTile = targetTiles.get(targetBand);
                        tileDataList.add(new TileData(targetTile, targetBand.getName()));
                    }
                }
                srcInfoList[cnt].tileDataList = tileDataList.toArray(new TileData[tileDataList.size()]);

                ++cnt;
            }

            for (int ty = ty0; ty < maxY; ty++) {
                trgIndex.calculateStride(ty);
                final int y0 = Math.max(ty - halfWindowSize, 0);
                final int h = Math.min(ty + halfWindowSize, sourceImageHeight - 1) - y0 + 1;
                final int yMax = y0 + h;

                for (int tx = tx0; tx < maxX; tx++) {
                    final int idx = trgIndex.getIndex(tx);

                    final int x0 = Math.max(tx - halfWindowSize, 0);
                    final int w = Math.min(tx + halfWindowSize, sourceImageWidth - 1) - x0 + 1;
                    final int xMax = x0 + w;

                    for (SrcInfo srcInfo : srcInfoList) {
                        srcInfo.reset(w, h);
                    }

                    computeQuantizedImages(quantizer, x0, y0, xMax, yMax, srcInfoList);
                    computeGLCM(x0, y0, xMax, yMax, srcInfoList);

                    for (SrcInfo srcInfo : srcInfoList) {


                        if (srcInfo.totals.totalCount == 0) {
                            writeData(srcInfo, srcInfo.tfNoData, idx);
                        } else {
                            writeData(srcInfo, computeTextureFeatures(srcInfo.GLCM, srcInfo.totals), idx);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void writeData(final SrcInfo srcInfo, final TextureFeatures tf, final int idx) {
        for (final TileData tileData : srcInfo.tileDataList) {

            if (tileData.doContrast) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.Contrast);
            } else if (tileData.doDissimilarity) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.Dissimilarity);
            } else if (tileData.doHomogeneity) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.Homogeneity);
            } else if (tileData.doASM) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.ASM);
            } else if (tileData.doEnergy) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.Energy);
            } else if (tileData.doMax) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.MAX);
            } else if (tileData.doEntropy) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.Entropy);
            } else if (tileData.doMean) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.GLCMMean);
            } else if (tileData.doVariance) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.GLCMVariance);
            } else if (tileData.doCorrelation) {
                tileData.dataBuffer.setElemFloatAt(idx, (float) tf.GLCMCorrelation);
            }
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0 X coordinate of pixel at the upper left corner of the target tile.
     * @param y0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param w  The width of the target tile.
     * @param h  The height of the target tile.
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

    private void computeGLCM(final int x0, final int y0, final int xMax, final int yMax,
                             final SrcInfo[] srcInfoList) {

        int xx, yy, i, j;
        if (computeGLCPWithAllAngles) {

            for (int y = y0; y < yMax; y++) {
                yy = y - y0;
                final int yyDisp = yy + displacement;
                final boolean withinY0 = y >= y0 && y < yMax;
                final boolean withinY = y + displacement >= y0 && y + displacement < yMax;

                for (int x = x0; x < xMax; x++) {
                    xx = x - x0;
                    final int xDisp = x + displacement;
                    final boolean withinX = xDisp >= x0 && xDisp < xMax;
                    final boolean within45 = withinY && x + -displacement >= x0 && x + -displacement < xMax;
                    final boolean within90 = withinY && x >= x0 && x < xMax;

                    for(SrcInfo srcInfo : srcInfoList) {
                        i = srcInfo.quantizedImage[yy][xx];
                        if (i < 0)
                            continue;

                        // 0
                        if (withinY0 && withinX) {
                            j = srcInfo.quantizedImage[yy][xx + displacement];
                            if (j >= 0) {
                                addElements(srcInfo.GLCM, i, j, srcInfo.totals);
                            }
                        }

                        // 45
                        if (within45) {
                            j = srcInfo.quantizedImage[yyDisp][xx - displacement];
                            if (j >= 0) {
                                addElements(srcInfo.GLCM, i, j, srcInfo.totals);
                            }
                        }

                        // 90
                        if (within90) {
                            j = srcInfo.quantizedImage[yyDisp][xx];
                            if (j >= 0) {
                                addElements(srcInfo.GLCM, i, j, srcInfo.totals);
                            }
                        }

                        // 135
                        if (withinY && withinX) {
                            j = srcInfo.quantizedImage[yyDisp][xx + displacement];
                            if (j >= 0) {
                                addElements(srcInfo.GLCM, i, j, srcInfo.totals);
                            }
                        }
                    }
                }
            }

        } else {

            for (int y = y0; y < yMax; y++) {
                yy = y - y0;
                boolean withinY = y + displacementY >= y0 && y + displacementY < yMax;
                for (int x = x0; x < xMax; x++) {
                    xx = x - x0;
                    boolean withinImage = withinY && x + displacementX >= x0 && x + displacementX < xMax;

                    for(SrcInfo srcInfo : srcInfoList) {
                        i = srcInfo.quantizedImage[yy][xx];
                        if (i < 0) {
                            continue;
                        }

                        if (withinImage) {
                            j = srcInfo.quantizedImage[yy + displacementY][xx + displacementX];
                            if (j >= 0) {
                                addElements(srcInfo.GLCM, i, j, srcInfo.totals);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addElements(final GLCMElem[] GLCM, int i, int j, Totals totals) {

        GLCMElem elem = GLCM[i * numQuantLevels + j];
        if (!elem.init) {
            elem.setPos(i, j);
            totals.numElems++;
        }
        elem.value++;

        elem = GLCM[j * numQuantLevels + i];
        if (!elem.init) {
            elem.setPos(j, i);
            totals.numElems++;
        }
        elem.value++;

        totals.totalCount++;
    }

    private static void computeQuantizedImages(final Quantizer quantizer,
                                               final int x0, final int y0, final int xMax, final int yMax,
                                               final SrcInfo[] srcInfoList) {

        final TileIndex srcIndex = new TileIndex(srcInfoList[0].sourceTile);
        double v;

        for (int y = y0; y < yMax; y++) {
            int yy = y - y0;
            srcIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                int xx = x - x0;
                final int index = srcIndex.getIndex(x);
                for (SrcInfo srcInfo : srcInfoList) {
                    v = srcInfo.srcData.getElemDoubleAt(index);
                    srcInfo.quantizedImage[yy][xx] = v == srcInfo.noDataValue ? -1 : quantizer.compute(v);
                }
            }
        }
    }

    private TextureFeatures computeTextureFeatures(final GLCMElem[] GLCM, final Totals totals) {

        double Contrast = 0.0, Dissimilarity = 0.0, Homogeneity = 0.0, ASM = 0.0, Energy = 0.0,
                MAX = 0.0, Entropy = 0.0, GLCMMeanX = 0.0, GLCMMeanY = 0.0,
                GLCMMean = 0.0, GLCMVariance = 0.0;

        final boolean doContrast = outputContrast;
        final boolean doDissimilarity = outputDissimilarity;
        final boolean doHomogeneity = outputHomogeneity;
        final boolean doASM = outputASM || outputEnergy;
        final boolean doEntropy = outputEntropy;
        final boolean doVariance = outputVariance;
        final boolean doCorrelation = outputCorrelation;
        GLCMElem[] glcmList2 = null;
        if (doVariance || doCorrelation) {
            glcmList2 = new GLCMElem[totals.numElems];
        }

        int cnt = 0;
        for (GLCMElem e : GLCM) {
            if (e.init && e.value > 0.0) {
                final int ij = e.row - e.col;
                e.prob = e.value / (double) totals.totalCount;
                final double GLCMval = e.prob;

                if (doContrast)
                    Contrast += GLCMval * ij * ij;

                if (doDissimilarity)
                    Dissimilarity += GLCMval * Math.abs(ij);

                if (doHomogeneity)
                    Homogeneity += GLCMval / (1 + ij * ij);

                if (doASM)
                    ASM += GLCMval * GLCMval;

                if (MAX < GLCMval)
                    MAX = GLCMval;

                if (doEntropy)
                    Entropy += -GLCMval * Math.log(GLCMval + Constants.EPS);

                GLCMMeanY += GLCMval * e.row;
                GLCMMeanX += GLCMval * e.col;

                if (doVariance || doCorrelation) {
                    glcmList2[cnt++] = e;
                }
            }
        }

        if (doASM) {
            Energy = Math.sqrt(ASM);
        }

        if (outputMean) {
            GLCMMean = (GLCMMeanX + GLCMMeanY) / 2.0;
        }

        double GLCMVarianceX = 0.0, GLCMVarianceY = 0.0;
        double GLCMCorrelation = 0.0;
        if (doVariance || doCorrelation) {
            for (GLCMElem e : glcmList2) {
                e.diff_col_GLCMMeanX = e.col - GLCMMeanX;
                e.diff_row_GLCMMeanY = e.row - GLCMMeanY;
                GLCMVarianceX += e.prob * e.diff_col_GLCMMeanX * e.diff_col_GLCMMeanX;
                GLCMVarianceY += e.prob * e.diff_row_GLCMMeanY * e.diff_row_GLCMMeanY;
            }
            if (doVariance) {
                GLCMVariance = (GLCMVarianceX + GLCMVarianceY) / 2.0;
            }
            if (doCorrelation) {
                double sqrtOfGLCMVariance = Math.sqrt(GLCMVarianceX * GLCMVarianceY);
                for (GLCMElem e : glcmList2) {
                    GLCMCorrelation += e.prob * e.diff_row_GLCMMeanY * e.diff_col_GLCMMeanX / sqrtOfGLCMVariance;
                }
            }
        }

        return new TextureFeatures(
                Contrast,
                Dissimilarity,
                Homogeneity,
                ASM,
                Energy,
                MAX,
                Entropy,
                GLCMMean,
                GLCMVariance,
                GLCMCorrelation);
    }

    private interface Quantizer {
        int compute(final double v);
    }

    private static class ProbabilityQuantizer implements Quantizer {
        private final double[] newBinLowValues;
        private final int numQuantLevels;
        private final double minBin, maxBin;

        public ProbabilityQuantizer(final Band srcBand, final int numQuantLevels) {
            this.numQuantLevels = numQuantLevels;

            final Histogram hist = srcBand.getStx().getHistogram();
            int numBins = hist.getNumBins(0);
            int[] bins = hist.getBins(0);
            int totalNumPixels = 0;
            for (int i = 0; i < numBins; i++) {
                totalNumPixels += bins[i];
            }

            final int newBinSize = totalNumPixels / numQuantLevels;
            newBinLowValues = new double[numQuantLevels + 1];
            newBinLowValues[0] = hist.getBinLowValue(0, 0);
            int k = 1;
            int sum = 0;
            for (int i = 0; i < numBins; i++) {
                sum += bins[i];
                if (sum >= k * newBinSize) {
                    newBinLowValues[k] = hist.getBinLowValue(0, i);
                    if (k < numQuantLevels - 1) {
                        k++;
                    } else {
                        newBinLowValues[numQuantLevels] = hist.getHighValue(0);
                        break;
                    }
                }
            }

            minBin = newBinLowValues[0];
            maxBin = newBinLowValues[numQuantLevels];
        }

        public int compute(final double v) {

            if (v < minBin) {
                return 0;
            }
            if (v >= maxBin) {
                return numQuantLevels - 1;
            }

            int low = 0;
            int high = numQuantLevels;
            int mid = -1;
            double midValue;
            while (low < high) {
                mid = (low + high) / 2;
                midValue = newBinLowValues[mid];
                if (v >= midValue && v < newBinLowValues[mid + 1]) {
                    break;
                } else if (v < midValue) {
                    high = mid;
                } else {
                    low = mid;
                }
            }
            return mid;
        }
    }

    private static class EqualDistanceQuantizer implements Quantizer {
        private final double bandMin;
        private final double delta;
        private final int max;

        public EqualDistanceQuantizer(final Band srcBand, final int numQuantLevels) {
            final Stx stx = srcBand.getStx(true, ProgressMonitor.NULL);
            bandMin = stx.getMinimum();
            delta = (stx.getMaximum() - bandMin) / numQuantLevels;
            max = numQuantLevels - 1;
        }

        public int compute(final double v) {
            return Math.min((int) ((v - bandMin) / delta), max);
        }
    }

    private static class SrcInfo {
        public final Tile sourceTile;
        public final TileIndex srcIndex;
        public final ProductData srcData;
        public final float noDataValue;
        public final TextureFeatures tfNoData;
        public TileData[] tileDataList;
        public int[][] quantizedImage;
        public Totals totals;
        public GLCMElem[] GLCM;
        private final int numQuantLevels;

        public SrcInfo(final int numQuantLevels, final Band srcBand, final Tile srcTile) {
            this.numQuantLevels = numQuantLevels;
            this.sourceTile = srcTile;
            this.srcIndex = new TileIndex(sourceTile);
            this.srcData = sourceTile.getDataBuffer();
            this.noDataValue = (float) srcBand.getNoDataValue();
            this.tfNoData = new TextureFeatures(
                    noDataValue, noDataValue, noDataValue,
                    noDataValue, noDataValue, noDataValue,
                    noDataValue, noDataValue, noDataValue,
                    noDataValue);
        }

        public void reset(int w, int h) {
            totals = new Totals();
            quantizedImage = new int[h][w];
            GLCM = new GLCMElem[numQuantLevels * numQuantLevels];
            for (int i = 0; i < GLCM.length; ++i) {
                GLCM[i] = new GLCMElem();
            }
        }
    }

    private class TileData {
        final ProductData dataBuffer;
        final boolean doContrast;
        final boolean doDissimilarity;
        final boolean doHomogeneity;
        final boolean doASM;
        final boolean doEnergy;
        final boolean doMax;
        final boolean doEntropy;
        final boolean doMean;
        final boolean doVariance;
        final boolean doCorrelation;

        public TileData(final Tile tile, final String bandName) {
            this.dataBuffer = tile.getDataBuffer();

            doContrast = outputContrast && bandName.endsWith(GLCM_TYPES.Contrast.toString());
            doDissimilarity = outputDissimilarity && bandName.endsWith(GLCM_TYPES.Dissimilarity.toString());
            doHomogeneity = outputHomogeneity && bandName.endsWith(GLCM_TYPES.Homogeneity.toString());
            doASM = outputASM && bandName.endsWith(GLCM_TYPES.ASM.toString());
            doEnergy = outputEnergy && bandName.endsWith(GLCM_TYPES.Energy.toString());
            doMax = outputMAX && bandName.endsWith(GLCM_TYPES.MAX.toString());
            doEntropy = outputEntropy && bandName.endsWith(GLCM_TYPES.Entropy.toString());
            doMean = outputMean && bandName.endsWith(GLCM_TYPES.GLCMMean.toString());
            doVariance = outputVariance && bandName.endsWith(GLCM_TYPES.GLCMVariance.toString());
            doCorrelation = outputCorrelation && bandName.endsWith(GLCM_TYPES.GLCMCorrelation.toString());
        }
    }

    private static class TextureFeatures {

        public final double Contrast;
        public final double Dissimilarity;
        public final double Homogeneity;
        public final double ASM;
        public final double Energy;
        public final double MAX;
        public final double Entropy;
        public final double GLCMMean;
        public final double GLCMVariance;
        public final double GLCMCorrelation;

        TextureFeatures(
                final double Contrast,
                final double Dissimilarity,
                final double Homogeneity,
                final double ASM,
                final double Energy,
                final double MAX,
                final double Entropy,
                final double GLCMMean,
                final double GLCMVariance,
                final double GLCMCorrelation) {
            this.Contrast = Contrast;
            this.Dissimilarity = Dissimilarity;
            this.Homogeneity = Homogeneity;
            this.ASM = ASM;
            this.Energy = Energy;
            this.MAX = MAX;
            this.Entropy = Entropy;
            this.GLCMMean = GLCMMean;
            this.GLCMVariance = GLCMVariance;
            this.GLCMCorrelation = GLCMCorrelation;
        }
    }

    private static class Totals {
        public int numElems = 0;
        public int totalCount = 0;
    }

    private static class GLCMElem {

        public int row;
        public int col;
        public boolean init;
        public int value;
        public double prob;
        public double diff_col_GLCMMeanX;
        public double diff_row_GLCMMeanY;

        void setPos(final int row, final int col) {
            this.row = row;
            this.col = col;
            init = true;
        }
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
            super(GLCMOp.class);
        }
    }
}
