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
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.Constants;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Oversample
 */

@OperatorMetadata(alias="Oversample",
        category = "Utilities\\Resampling",
        authors = "NEST team", copyright = "(C) 2013 by Array Systems Computing Inc.",
        description="Oversample the datset")
public class OversamplingOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {UndersamplingOp.IMAGE_SIZE, UndersamplingOp.RATIO, UndersamplingOp.PIXEL_SPACING},
            defaultValue = UndersamplingOp.RATIO, label="Output Image By:")
    private String outputImageBy = UndersamplingOp.RATIO;

    @Parameter(description = "The row dimension of the output image", defaultValue = "1000", label="Output Image Rows")
    private int targetImageHeight = 1000;
    @Parameter(description = "The col dimension of the output image", defaultValue = "1000", label="Output Image Columns")
    private int targetImageWidth = 1000;

    @Parameter(description = "The width ratio of the output/input images", defaultValue = "2.0", label="Width Ratio")
    private float widthRatio = 2.0f;
    @Parameter(description = "The height ratio of the output/input images", defaultValue = "2.0", label="Height Ratio")
    private float heightRatio = 2.0f;

    @Parameter(description = "The range pixel spacing", defaultValue = "12.5", label="Range Spacing")
    private float rangeSpacing = 12.5f;
    @Parameter(description = "The azimuth pixel spacing", defaultValue = "12.5", label="Azimuth Spacing")
    private float azimuthSpacing = 12.5f;

    @Parameter(description = "use PRF as azimuth tile size and range line as range tile size", defaultValue = "false",
                label="Use PRF Tile Size")
    private boolean usePRFTileSize = false;

    private MetadataElement abs; // root of the abstracted metadata
    private String productFormat;

    private boolean isDetectedSampleType = false;

    private int sourceImageWidth;
    private int sourceImageHeight;

    private float srcRangeSpacing; // range pixel spacing of source image
    private float srcAzimuthSpacing; // azimuth pixel spacing of source image

    private double prf; // pulse repetition frequency in Hz
    private double[] dopplerCentroidFreq; // Doppler centroid frequencies for all columns in a range line
    private double widthRatioByHeightRatio;

    private static final double nsTOs = Constants.oneBillionth; // ns to s
    private static final String CEOS = "CEOS";
    private static final String ENVISAT = "ENVISAT";
    private static final String OTHER = "OTHER";

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
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            getSrcImagePixelSpacings();
            getSampleType();
            getPRF();

            if (!isDetectedSampleType) {
                getProductFormat();
                computeDopplerCentroidFrequencies();
            }

            computeTargetImageSizeAndPixelSpacings();
            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get the range and azimuth spacings (in meter).
     */
    private void getSrcImagePixelSpacings() {

        srcRangeSpacing = (float)abs.getAttributeDouble(AbstractMetadata.range_spacing);
        //System.out.println("Range spacing is " + srcRangeSpacing);

        srcAzimuthSpacing = (float)abs.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        //System.out.println("Azimuth spacing is " + srcAzimuthSpacing);
    }

    /**
     * Get the sample type.
     */
    void getSampleType() {

        final String sampleType = abs.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        //System.out.println("Sample type is " + sampleType);
        isDetectedSampleType = sampleType.contains("DETECTED");
    }

    /**
     * Get the pulse repetition frequency.
     */
    void getPRF() {

        prf = abs.getAttributeDouble(AbstractMetadata.pulse_repetition_frequency);
        if(prf == 0) {
            System.out.println("PRF is 0");
            prf = 100;
        }
        //System.out.println("PRF is " + prf);
    }

    /**
     * Get Product format.
     */
    private void getProductFormat() {

        final String productType = abs.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (productType.contains("ERS")) {
            productFormat = CEOS;
        } else if (productType.contains("ASA") || productType.contains("SAR")) {
            productFormat = ENVISAT;
        } else {
            productFormat = OTHER;
        }
        //System.out.println("product format is " + productFormat);
    }

    /**
     * Compute Doppler centroid frequency for all columns in a range line.
     */
    private void computeDopplerCentroidFrequencies() {

        if (productFormat.contains(CEOS)) { // CEOS
            computeDopplerCentroidFreqForERSProd();
        } else if (productFormat.contains(ENVISAT)) { // ENVISAT
            computeDopplerCentroidFreqForENVISATProd();
        } else {
            computeDopplerCentroidFreqForOtherProd();
        }
    }

    /**
     * Compute Doppler centroid frequency for all columns for ERS product.
     */
    private void computeDopplerCentroidFreqForERSProd() {

        // get range sampling rate (in Hz)
        final double samplingRate = abs.getAttributeDouble(AbstractMetadata.range_sampling_rate)*1000000; // MHz to Hz

        // Get coefficients of Doppler frequency polynomial from
        // fields 105, 106 and 107 in PRI Data Set Summary Record
        final MetadataElement facility = AbstractMetadata.getOriginalProductMetadata(sourceProduct).getElement("Leader").getElement("Scene Parameters");
        if (facility == null) {
            throw new OperatorException("Scene Parameters not found");
        }

        MetadataAttribute attr = facility.getAttribute("Cross track Doppler frequency centroid constant term");
        if (attr == null) {
            throw new OperatorException("Cross track Doppler frequency centroid constant term not found");
        }
        double a0 = attr.getData().getElemDouble();

        attr = facility.getAttribute("Cross track Doppler frequency centroid linear term");
        if (attr == null) {
            throw new OperatorException("Cross track Doppler frequency centroid linear term not found");
        }
        double a1 = attr.getData().getElemDouble();

        attr = facility.getAttribute("Cross track Doppler frequency centroid quadratic term");
        if (attr == null) {
            throw new OperatorException("Cross track Doppler frequency centroid quadratic term not found");
        }
        double a2 = attr.getData().getElemDouble();
        //System.out.println("Doppler frequency polynomial coefficients are " + a0 + ", " + a1 + ", " + a2);

        // compute Doppler centroid frequencies
        dopplerCentroidFreq = new double[sourceImageWidth];
        for (int c = 0; c < sourceImageWidth; c++) {
            final double dt = (c - sourceImageWidth*0.5) / samplingRate;
            dopplerCentroidFreq[c] = a0 + a1*dt + a2*dt*dt;
        }
    }

    /**
     * Compute Doppler centroid frequency for all columns for ENVISAT product.
     */
    private void computeDopplerCentroidFreqForENVISATProd() {

        // get slant range time origin in second
        final MetadataElement dsd = AbstractMetadata.getOriginalProductMetadata(sourceProduct).getElement("DOP_CENTROID_COEFFS_ADS");
        if (dsd == null) {
            throw new OperatorException("DOP_CENTROID_COEFFS_ADS not found");
        }

        final MetadataAttribute srtAttr = dsd.getAttribute("slant_range_time");
        if (srtAttr == null) {
            throw new OperatorException("slant_range_time not found");
        }

        final double t0 = srtAttr.getData().getElemFloat() * nsTOs;

        // get Doppler centroid coefficients: d0, d1, d2, d3 and d4
        final MetadataAttribute coefAttr = dsd.getAttribute("dop_coef");
        if (coefAttr == null) {
            throw new OperatorException("dop_coef not found");
        }

        final double d0 = coefAttr.getData().getElemFloatAt(0);
        final double d1 = coefAttr.getData().getElemFloatAt(1);
        final double d2 = coefAttr.getData().getElemFloatAt(2);
        final double d3 = coefAttr.getData().getElemFloatAt(3);
        final double d4 = coefAttr.getData().getElemFloatAt(4);

        // compute Doppler centroid frequencies for all columns in a range line
        final TiePointGrid slantRangeTime = OperatorUtils.getSlantRangeTime(sourceProduct);
        dopplerCentroidFreq = new double[sourceImageWidth];
        for (int c = 0; c < sourceImageWidth; c++) {
            final double tSR = slantRangeTime.getPixelDouble(c, 0) * nsTOs;
            final double dt = tSR - t0;
            dopplerCentroidFreq[c] = d0 + d1*dt + d2*Math.pow(dt, 2.0) + d3*Math.pow(dt, 3.0) + d4*Math.pow(dt, 4.0);
        }
        /*
        for (double v:dopplerCentroidFreq) {
            System.out.print(v + ",");
        }
        System.out.println();
        */
    }

    /**
     * Set Doppler centroid frequency to zero for all products other than CEOS or ENVISAT product.
     */
    private void computeDopplerCentroidFreqForOtherProd() {
        dopplerCentroidFreq = new double[sourceImageWidth];
        for (int c = 0; c < sourceImageWidth; c++) {
            dopplerCentroidFreq[c] = 0.0;
        }
    }
    
    /**
     * Compute target image size and range/azimuth spacings.
     *
     * @throws OperatorException The exceptions.
     */
    private void computeTargetImageSizeAndPixelSpacings() throws OperatorException {

        if (outputImageBy.equals(UndersamplingOp.IMAGE_SIZE)) {

            if (targetImageHeight < sourceImageHeight || targetImageWidth < sourceImageWidth) {
                throw new OperatorException("Output image size must not be smaller than the source image size");
            }

            widthRatio = (float)targetImageWidth / (float)sourceImageWidth;
            heightRatio = (float)targetImageHeight / (float)sourceImageHeight;

            rangeSpacing = srcRangeSpacing / widthRatio;
            azimuthSpacing = srcAzimuthSpacing / heightRatio;

        } else if (outputImageBy.equals(UndersamplingOp.RATIO)) {

            if (widthRatio < 1 || heightRatio < 1) {
                throw new OperatorException("The width or height ratio must not be smaller than 1");
            }

            targetImageHeight = (int)(heightRatio * sourceImageHeight + 0.5f);
            targetImageWidth = (int)(widthRatio * sourceImageWidth + 0.5f);

            rangeSpacing = srcRangeSpacing / widthRatio;
            azimuthSpacing = srcAzimuthSpacing / heightRatio;

        } else if (outputImageBy.equals(UndersamplingOp.PIXEL_SPACING)) {

            if (rangeSpacing <= 0.0f || rangeSpacing > srcRangeSpacing ||
                azimuthSpacing <= 0.0f || azimuthSpacing > srcAzimuthSpacing) {
                throw new OperatorException("The azimuth or range spacing must be positive and no greater than the source spacing");
            }

            widthRatio = srcRangeSpacing / rangeSpacing;
            heightRatio = srcAzimuthSpacing / azimuthSpacing;

            targetImageHeight = (int)(heightRatio * sourceImageHeight + 0.5);
            targetImageWidth = (int)(widthRatio * sourceImageWidth + 0.5);

        } else {
            throw new OperatorException("Please specify output image size, or row and column ratios, or pixel spacings");
        }

        widthRatioByHeightRatio = widthRatio*heightRatio;
    }

    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    targetImageWidth,
                                    targetImageHeight);

        addSelectedBands();

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        //ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        //ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        addGeoCoding();
        updateTargetProductMetadata();

        final int sourceImageTileWidth = sourceImageWidth;
        final int sourceImageTileHeight = Math.min((int)(prf+0.5), sourceImageHeight);

        final int targetImageTileWidth = (int)(sourceImageTileWidth * widthRatio + 0.5f);
        final int targetImageTileHeight = (int)(sourceImageTileHeight * heightRatio + 0.5f);

        if (usePRFTileSize) {
            targetProduct.setPreferredTileSize(targetImageTileWidth, targetImageTileHeight);
        }
    }

    private void addSelectedBands() {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        for (int i = 0; i < sourceBands.length; i++) {

            String unit = sourceBands[i].getUnit();
            if(unit == null) {
                unit = Unit.AMPLITUDE;  // assume amplitude
            }

            if (unit.contains(Unit.REAL)) {
                if(i+1 >= sourceBands.length) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
                String nextUnit = sourceBands[i+1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }

                final Band targetBandI = targetProduct.addBand(sourceBands[i].getName(), ProductData.TYPE_FLOAT32);
                targetBandI.setUnit(unit);

                final Band targetBandQ = targetProduct.addBand(sourceBands[i+1].getName(), ProductData.TYPE_FLOAT32);
                targetBandQ.setUnit(nextUnit);

                String suffix = OperatorUtils.getSuffixFromBandName(sourceBands[i].getName());
                if (suffix == null) {
                    suffix = "";
                } else {
                    suffix = '_' + suffix;
                }
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, suffix);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, suffix);
                i++;
            } else {
                final String targetBandName = sourceBands[i].getName();
                if (targetProduct.getBand(targetBandName) == null) {
                    final Band targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
                    targetBand.setUnit(unit);
                }
            }
        }
    }

    private void addGeoCoding() {

        final TiePointGrid lat = OperatorUtils.getLatitude(sourceProduct);
        final TiePointGrid lon = OperatorUtils.getLongitude(sourceProduct);
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        final TiePointGrid slantRgTime = OperatorUtils.getSlantRangeTime(sourceProduct);
        if (lat == null || lon == null || incidenceAngle == null || slantRgTime == null) { // for unit test
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
            return;
        }

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float subSamplingX = targetImageWidth / (gridWidth - 1.0f);
        final float subSamplingY = targetImageHeight / (gridHeight - 1.0f);
        final PixelPos[] newTiePointPos = new PixelPos[gridWidth*gridHeight];

        int k = 0;
        for (int j = 0; j < gridHeight; j++) {
            final float ty = Math.min(j*subSamplingY, targetImageHeight - 1);
            final float y = (int)(ty / heightRatio + 0.5f);
            for (int i = 0; i < gridWidth; i++) {
                final float tx = Math.min(i*subSamplingX, targetImageWidth - 1);
                final float x = (int)(tx / widthRatio + 0.5f);
                newTiePointPos[k] = new PixelPos();
                newTiePointPos[k].x = x;
                newTiePointPos[k].y = y;
                k++;
            }
        }

        OperatorUtils.createNewTiePointGridsAndGeoCoding(
                sourceProduct,
                targetProduct,
                gridWidth,
                gridHeight,
                subSamplingX,
                subSamplingY,
                newTiePointPos);
    }

    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, azimuthSpacing);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, rangeSpacing);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);

        final float oldLineTimeInterval = (float)absTgt.getAttributeDouble(AbstractMetadata.line_time_interval);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.line_time_interval, oldLineTimeInterval/heightRatio);
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

            final Band[] targetBands = targetProduct.getBands();
            for (int i = 0; i < targetBands.length; i++) {

                if (targetBands[i] instanceof VirtualBand) {
                    continue;
                }

                if (targetBands[i].getUnit().equals(Unit.REAL)) {

                    if (i+1 >= targetBands.length) {
                        throw new OperatorException("q band is missing from target product");
                    }

                    computeOverSampledTileForComplexImage(targetBands[i].getName(),
                                                          targetBands[i+1].getName(),
                                                          targetTileMap.get(targetBands[i]),
                                                          targetTileMap.get(targetBands[i+1]));
                    i++;

                } else {

                    computeOverSampledTileForRealImage(targetBands[i].getName(),
                                                       targetTileMap.get(targetBands[i]));
                }

            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computeOverSampledTileForRealImage(String targetBandName, Tile targetTile) {

        final ProductData tgtData = targetTile.getDataBuffer();

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int tx0 = targetTileRectangle.x;
        final int ty0 = targetTileRectangle.y;
        final int targetTileWidth = targetTileRectangle.width;
        final int targetTileHeight = targetTileRectangle.height;

        final Rectangle sourceTileRectangle = getSourceTileRectangle(targetTileRectangle);
        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sourceTileWidth = sourceTileRectangle.width;
        final int sourceTileHeight = sourceTileRectangle.height;

        final double[][]tmpI = new double[targetTileHeight][sourceTileWidth];
        final double[][]tmpQ = new double[targetTileHeight][sourceTileWidth];

        final Band srcBand = sourceProduct.getBand(targetBandName);
        final Tile srcRaster = getSourceTile(srcBand, sourceTileRectangle);
        final ProductData srcData = srcRaster.getDataBuffer();

        final double[] rowArray = new double[sourceTileWidth*2];

        // perform 1-D FFT on each row
        final DoubleFFT_1D src_row_fft = new DoubleFFT_1D(sourceTileWidth);
        for (int y = 0; y < sourceTileHeight; y++) {
            getRowData(sy0 + y, sx0, sourceTileWidth, srcData, srcRaster, rowArray);
            src_row_fft.complexForward(rowArray);
            for (int x = 0; x < sourceTileWidth; x++) {
                tmpI[y][x] = rowArray[2*x];
                tmpQ[y][x] = rowArray[2*x+1];
            }
        }

        final int d = (int)(sourceTileHeight/2 + 0.5);

        final double[] colArray = new double[2*sourceTileHeight];
        final double[] zeroPaddedColSpec = new double[2*targetTileHeight];

        // perform 1-D FFT, zero padding and IFFT on each column
        final DoubleFFT_1D src_col_fft = new DoubleFFT_1D(sourceTileHeight);
        final DoubleFFT_1D tgt_col_fft = new DoubleFFT_1D(targetTileHeight);
        for (int x = 0; x < sourceTileWidth; x++) {
            getColData(x, sourceTileHeight, colArray, tmpI, tmpQ);
            src_col_fft.complexForward(colArray);
            paddingZeros(colArray, sourceTileHeight, targetTileHeight, d, zeroPaddedColSpec);
            tgt_col_fft.complexInverse(zeroPaddedColSpec, true);
            saveOverSampledCol(zeroPaddedColSpec, x, targetTileHeight, tmpI, tmpQ);
        }

        final double[] tgtRow = new double[targetTileWidth*2];

        // perform 1-D IFFT on each row
        final DoubleFFT_1D tgt_row_fft = new DoubleFFT_1D(targetTileWidth);
        for (int y = 0; y < targetTileHeight; y++) {
            getRowData(y, sourceTileWidth, targetTileWidth, tgtRow, tmpI, tmpQ);
            tgt_row_fft.complexInverse(tgtRow, true);
            saveOverSampledComplexImage(tgtRow, ty0 + y, tx0, targetTileWidth,
                                        widthRatioByHeightRatio, tgtData, targetTile);
        }
    }

    private Rectangle getSourceTileRectangle(Rectangle targetTileRectangle) {

        final int sx0 = (int)(targetTileRectangle.x / widthRatio + 0.5f);
        final int sy0 = (int)(targetTileRectangle.y / heightRatio + 0.5f);
        final int sw  = (int)(targetTileRectangle.width / widthRatio + 0.5f);
        final int sh  = (int)(targetTileRectangle.height / heightRatio + 0.5f);
        //System.out.println("x0 = " + targetTileRectangle.x + ", y0 = " + targetTileRectangle.y +
        //        ", w = " + targetTileRectangle.width + ", h = " + targetTileRectangle.height);

        return new Rectangle(sx0, sy0, sw, sh);
    }

    //==================================================================================================================

    private void computeOverSampledTileForComplexImage(
            String iBandName, String qBandName, Tile iTargetTile, Tile qTargetTile) {

        final ProductData iTgtData = iTargetTile.getDataBuffer();
        final ProductData qTgtData = qTargetTile.getDataBuffer();

        final Rectangle targetTileRectangle = iTargetTile.getRectangle();
        final int tx0 = targetTileRectangle.x;
        final int ty0 = targetTileRectangle.y;
        final int targetTileWidth = targetTileRectangle.width;
        final int targetTileHeight = targetTileRectangle.height;

        final Rectangle sourceTileRectangle = getSourceTileRectangle(targetTileRectangle);
        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sourceTileWidth = sourceTileRectangle.width;
        final int sourceTileHeight = sourceTileRectangle.height;

        final double[][]tmpI = new double[targetTileHeight][sourceTileWidth];
        final double[][]tmpQ = new double[targetTileHeight][sourceTileWidth];

        final Band iBand = sourceProduct.getBand(iBandName);
        final Band qBand = sourceProduct.getBand(qBandName);

        final Tile iRaster = getSourceTile(iBand, sourceTileRectangle);
        final Tile qRaster = getSourceTile(qBand, sourceTileRectangle);

        final ProductData iSrcData = iRaster.getDataBuffer();
        final ProductData qSrcData = qRaster.getDataBuffer();

        final double[] rowArray = new double[sourceTileWidth*2];

        // perform 1-D FFT on each row
        final DoubleFFT_1D src_row_fft = new DoubleFFT_1D(sourceTileWidth);
        for (int y = 0; y < sourceTileHeight; y++) {
            getRowData(sy0 + y, sx0, sourceTileWidth, iSrcData, qSrcData, iRaster, rowArray);
            src_row_fft.complexForward(rowArray);
            for (int x = 0; x < sourceTileWidth; x++) {
                tmpI[y][x] = rowArray[2*x];
                tmpQ[y][x] = rowArray[2*x+1];
            }
        }

        final double[] colArray = new double[2*sourceTileHeight];
        final double[] zeroPaddedColSpec = new double[2*targetTileHeight];

        final int halfHeight = sourceTileHeight/2;
        final double heightByPRF = sourceTileHeight / prf;

        // perform 1-D FFT, zero padding and IFFT on each column
        final DoubleFFT_1D src_col_fft = new DoubleFFT_1D(sourceTileHeight);
        final DoubleFFT_1D tgt_col_fft = new DoubleFFT_1D(targetTileHeight);
        for (int x = 0; x < sourceTileWidth; x++) {
            getColData(x, sourceTileHeight, colArray, tmpI, tmpQ);
            src_col_fft.complexForward(colArray);

            final int idxFdc = (int)(dopplerCentroidFreq[sx0 + x] * heightByPRF + 0.5);
            final int d = (idxFdc + halfHeight) % sourceTileHeight;

            paddingZeros(colArray, sourceTileHeight, targetTileHeight, d, zeroPaddedColSpec);
            tgt_col_fft.complexInverse(zeroPaddedColSpec, true);
            saveOverSampledCol(zeroPaddedColSpec, x, targetTileHeight, tmpI, tmpQ);
        }

        final double[] tgtRow = new double[targetTileWidth*2];

        // perform 1-D IFFT on each row
        final DoubleFFT_1D tgt_row_fft = new DoubleFFT_1D(targetTileWidth);
        for (int y = 0; y < targetTileHeight; y++) {
            getRowData(y, sourceTileWidth, targetTileWidth, tgtRow, tmpI, tmpQ);
            tgt_row_fft.complexInverse(tgtRow, true);
            saveOverSampledComplexImage(tgtRow, ty0 + y, tx0, targetTileWidth, widthRatioByHeightRatio,
                                        iTgtData, qTgtData, iTargetTile);
        }
    }

    private static void getRowData(final int sy, final int sx0, final int sw,
                                       final ProductData srcData, final Tile srcRaster,
                                       final double[] array) {

        int k = 0;
        final int length = sx0 + sw;
        for (int sx = sx0; sx < length; ++sx) {
            array[k++] = srcData.getElemDoubleAt(srcRaster.getDataBufferIndex(sx, sy));
            array[k++] = 0.0;
        }
    }

    private static void getRowData(final int sy, final int sx0, final int sw,
                                   final ProductData iData, final ProductData qData,
                                   final Tile iRaster, final double[] array) {

        int index;
        int k = 0;
        final int length = sx0 + sw;
        for (int sx = sx0; sx < length; ++sx) {
            index = iRaster.getDataBufferIndex(sx, sy);
            array[k++] = iData.getElemDoubleAt(index);
            array[k++] = qData.getElemDoubleAt(index);
        }
    }

    private static void getColData(final int x, final int sourceTileHeight, final double[] array,
                            final double[][] tmpI, final double[][] tmpQ) {

        int k = 0;
        for (int y = 0; y < sourceTileHeight; ++y) {
            array[k++] = tmpI[y][x];
            array[k++] = tmpQ[y][x];
        }
    }

    private static void paddingZeros(final double[] colSpec, final int sourceTileHeight, final int targetTileHeight,
                                         final int d, final double[] array) {

        Arrays.fill(array, 0.0);
        final int s1 = 0;
        final int s2 = d*2;
        final int S1 = 0;
        final int S2 = 2*(targetTileHeight - sourceTileHeight + d);
        System.arraycopy(colSpec, s1, array, S1, s2);
        System.arraycopy(colSpec, s2, array, S2, (sourceTileHeight - d)*2);
    }

    private static void saveOverSampledCol(final double[] overSampledCol, final int x, final int targetTileHeight,
                                    final double[][] tmpI, final double[][] tmpQ) {

        int k = 0;
        for (int y = 0; y < targetTileHeight; ++y) {
            tmpI[y][x] = overSampledCol[k++];
            tmpQ[y][x] = overSampledCol[k++];
        }
    }

    private static void getRowData(final int y, final int sourceTileWidth, final int targetTileWidth, final double[] array,
                            final double[][] tmpI, final double[][] tmpQ) {

        Arrays.fill(array, 0.0);

        final int firstHalfSourceTileWidth = (int)(sourceTileWidth/2 + 0.5);
        int k = 0;
        for (int x = 0; x < firstHalfSourceTileWidth; ++x) {
            array[k++] = tmpI[y][x];
            array[k++] = tmpQ[y][x];
        }

        final int secondHalfSourceTileWidth = sourceTileWidth - firstHalfSourceTileWidth;
        k = 2*(targetTileWidth - secondHalfSourceTileWidth);
        for (int x = firstHalfSourceTileWidth; x < sourceTileWidth; ++x) {
            array[k++] = tmpI[y][x];
            array[k++] = tmpQ[y][x];
        }
    }

    private static void saveOverSampledComplexImage(final double[] overSampledRow, final int ty, final int tx0,
                                                    final int tw, final double widthRatioByHeightRatio,
                                                    final ProductData tgtData, final Tile targetTile) {

        int k = 0;
        for (int tx = tx0; tx < tx0 + tw; ++tx) {
            final double i = overSampledRow[k++];
            final double q = overSampledRow[k++];
            tgtData.setElemDoubleAt(targetTile.getDataBufferIndex(tx, ty),
                    widthRatioByHeightRatio*Math.sqrt(i*i + q*q));
        }
    }

    private static void saveOverSampledComplexImage(final double[] overSampledRow, final int ty, final int tx0, final int tw,
                                             final double widthRatioByHeightRatio,
                                             final ProductData iData, final ProductData qData, final Tile iTargetTile) {

        int k = 0;
        for (int tx = tx0; tx < tx0 + tw; ++tx) {
            final int index = iTargetTile.getDataBufferIndex(tx, ty);
            iData.setElemDoubleAt(index, widthRatioByHeightRatio*overSampledRow[k++]);
            qData.setElemDoubleAt(index, widthRatioByHeightRatio*overSampledRow[k++]);
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
            super(OversamplingOp.class);
            setOperatorUI(OversamplingOpUI.class);
        }
    }
}