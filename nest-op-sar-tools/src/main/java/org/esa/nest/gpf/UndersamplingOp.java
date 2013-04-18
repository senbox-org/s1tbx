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
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
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
import org.esa.nest.util.ResourceUtils;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This operator down samples a real or complex image using sub-sampling method or kernel filtering method.
 *
 * With sub-sampling method, the image is down sampled with user specified sub-sampling rates in both range
 * and azimuth directions. For complex image, the i and q bands in the image are down sampled separately,
 * and the down sampled image is still a complex image.
 *
 * With kernel filtering method, the image is down sampled with a kernel moving across the image with a
 * step-size determined by the size of the required output image. The kernel can be selected from pre-
 * defined shapes or defined by the user. For complex image, intensity image is computed from the i and
 * q bands before kernel filtering is applied. The down sampled image is always real image. The user can
 * determine the output image size by specifying the output image size, or the pixel spacings, or the
 * down sampling ratios.
 *
 * The parameters used by the operator are as the follows:
 *
 * Source Band: All bands (real or virtual) of the source product.
 * Under-Sampling Method: Sub-Sampling method or Kernel Filtering method
 *
 * For Sub-Sampling method, the following parameters are used:
 *
 * Sub-Sampling in X: User provided sub-sampling rate in range.
 * Sub-Sampling in Y: User provided sub-sampling rate in azimuth.
 *
 * For Kernel Filtering method, the following parameters are used:
 *
 * Filter Type: The kernel filter type.
 * Filter Size: The kernel filter size.
 * Kernel File: The user defined kernel.
 * Output Image Rows: The row size of the down sampled image.
 * Output Image Columns: The column size of the down sampled image.
 * Width Ratio: The ratio of the down sampled image width and the source image width.
 * Height Ratio: The ratio of the down sampled image height and the source image height.
 * Range Spacing: The range pixel spacing of the down sampled image.
 * Azimuth Spacing: The azimuth pixel spacing of the down sampled image.
 *
 */

@OperatorMetadata(alias="Undersample",
        category = "Utilities\\Resampling",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Undersample the datset")
public class UndersamplingOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {SUB_SAMPLING, KERNEL_FILTERING}, defaultValue = KERNEL_FILTERING, label="Under-Sampling Method")
    private String method = KERNEL_FILTERING;

//    @Parameter(valueSet = {SUMMARY, EDGE_DETECT, EDGE_ENHANCEMENT, LOW_PASS, HIGH_PASS, HORIZONTAL, VERTICAL, USER_DEFINED},
//               defaultValue = LOW_PASS, label="Filter Type")
    private String filterType = LOW_PASS;

    @Parameter(valueSet = {FILTER_SIZE_3x3, FILTER_SIZE_5x5, FILTER_SIZE_7x7},
               defaultValue = FILTER_SIZE_3x3, label="Filter Size")
    private String filterSize = FILTER_SIZE_3x3;

//    @Parameter(description = "The kernel file", label="Kernel File")
    private File kernelFile = null;

    @Parameter(defaultValue = "2", label="Sub-Sampling in X")
    private int subSamplingX = 2;
    @Parameter(defaultValue = "2", label="Sub-Sampling in Y")
    private int subSamplingY = 2;

    @Parameter(valueSet = {IMAGE_SIZE, RATIO, PIXEL_SPACING}, defaultValue = IMAGE_SIZE, label="Output Image By:")
    private String outputImageBy = RATIO;

    @Parameter(description = "The row dimension of the output image", defaultValue = "1000", label="Output Image Rows")
    private int targetImageHeight = 1000;
    @Parameter(description = "The col dimension of the output image", defaultValue = "1000", label="Output Image Columns")
    private int targetImageWidth = 1000;

    @Parameter(description = "The width ratio of the output/input images", defaultValue = "0.5", label="Width Ratio")
    private float widthRatio = 0.5f;
    @Parameter(description = "The height ratio of the output/input images", defaultValue = "0.5", label="Height Ratio")
    private float heightRatio = 0.5f;

    @Parameter(description = "The range pixel spacing", defaultValue = "12.5", label="Range Spacing")
    private float rangeSpacing = 12.5f;
    @Parameter(description = "The azimuth pixel spacing", defaultValue = "12.5", label="Azimuth Spacing")
    private float azimuthSpacing = 12.5f;

    private ProductReader subsetReader = null;
    private MetadataElement absRoot = null;

    private int filterWidth;
    private int filterHeight;
    private int sourceImageWidth;
    private int sourceImageHeight;

    private double stepRange; // step size in range direction for moving window filtering
    private double stepAzimuth; // step size in azimuth direction for moving window filtering

    private float srcRangeSpacing; // range pixel spacing of source image
    private float srcAzimuthSpacing; // azimuth pixel spacing of source image
    private float[][] kernel; // kernel for filtering
    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();

    public static final String SUB_SAMPLING = "Sub-Sampling";
    public static final String KERNEL_FILTERING = "LowPass Filtering";
    public static final String SUMMARY = "Summary";
    public static final String EDGE_DETECT = "Edge Detect";
    public static final String EDGE_ENHANCEMENT = "Edge Enhancement";
    public static final String LOW_PASS = "Low Pass";
    public static final String HIGH_PASS = "High Pass";
    public static final String HORIZONTAL = "Horizontal";
    public static final String VERTICAL = "Vertical";
    private static final String USER_DEFINED = "User Defined";

    public static final String IMAGE_SIZE = "Image Size";
    public static final String RATIO = "Ratio";
    public static final String PIXEL_SPACING = "Pixel Spacing";

    public static final String FILTER_SIZE_3x3 = "3x3";
    public static final String FILTER_SIZE_5x5 = "5x5";
    public static final String FILTER_SIZE_7x7 = "7x7";

    @Override
    public void initialize() throws OperatorException {

        GeoCoding sourceGeoCoding = sourceProduct.getGeoCoding();
        if (sourceGeoCoding instanceof CrsGeoCoding) {
            throw new OperatorException("Undersampling is not intended for map projected products");
        }

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        if (method.equals(SUB_SAMPLING)) {
            initializeForSubSampling();
        } else if (method.equals(KERNEL_FILTERING)) {
            initializeForKernelFiltering();
        } else {
            throw new OperatorException("Unknown undersampling method: " + method);
        }
    }

    /**
     * Initialization for sub-sampling.
     *
     * @throws OperatorException The exceptions.
     */
    private void initializeForSubSampling() throws OperatorException {

        try {
            if (sourceBandNames == null || sourceBandNames.length == 0) {
                final Band[] bands = sourceProduct.getBands();
                final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
                for (Band band : bands) {
                    bandNameList.add(band.getName());
                }
                sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
            }

            for (int i = 0; i < sourceBandNames.length; i++) {
                final String unit1 = sourceProduct.getBand(sourceBandNames[i]).getUnit();
                if (unit1 != null && unit1.contains(Unit.REAL)) {
                    if (i+1 < sourceBandNames.length &&
                        sourceProduct.getBand(sourceBandNames[i+1]).getUnit().contains(Unit.IMAGINARY)) {
                        i++;
                    } else {
                        throw new OperatorException("Real and imaginary bands should be selected in pairs");
                    }
                }
            }

            subsetReader = new ProductSubsetBuilder();
            final ProductSubsetDef subsetDef = new ProductSubsetDef();

            subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
            subsetDef.addNodeNames(sourceBandNames);
            subsetDef.setSubSampling(subSamplingX, subSamplingY);
            subsetDef.setIgnoreMetadata(false);
            subsetDef.setTreatVirtualBandsAsRealBands(true);

            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);

            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());

            updateTargetProductMetadata(subSamplingX, subSamplingY);

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void updateTargetProductMetadata(int subSamplingX, int subSamplingY) {

        getSrcImagePixelSpacings();

        targetImageWidth = (sourceImageWidth - 1) / subSamplingX + 1;
        targetImageHeight = (sourceImageHeight - 1) / subSamplingY + 1;

        rangeSpacing = srcRangeSpacing * sourceImageWidth / targetImageWidth;
        azimuthSpacing = srcAzimuthSpacing * sourceImageHeight / targetImageHeight;

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, azimuthSpacing);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, rangeSpacing);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);

        final float oldLineTimeInterval = (float)absTgt.getAttributeDouble(AbstractMetadata.line_time_interval);
        AbstractMetadata.setAttribute(
                absTgt, AbstractMetadata.line_time_interval, oldLineTimeInterval*(float)subSamplingY);
    }

    /**
     * Initialization for kernel filtering.
     *
     * @throws OperatorException The exceptions.
     */
    private void initializeForKernelFiltering() throws OperatorException {
        try {

            getFilterDimension();

            getSrcImagePixelSpacings();

            computeTargetImageSizeAndPixelSpacings();

            computeRangeAzimuthStepSizes();

            getKernelFile();

            createTargetProduct();
        } catch(Exception e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void getFilterDimension() {

        if (filterSize.equals(FILTER_SIZE_3x3)) {
            filterWidth = 3;
            filterHeight = 3;
        } else if (filterSize.equals(FILTER_SIZE_5x5)) {
            filterWidth = 5;
            filterHeight = 5;
        } else if (filterSize.equals(FILTER_SIZE_7x7)) {
            filterWidth = 7;
            filterHeight = 7;
        } else {
            throw new OperatorException("Unknown filter size: " + filterSize);
        }
    }

    /**
     * Get the range and azimuth spacings (in meter).
     * @throws Exception when metadata is missing
     */
    void getSrcImagePixelSpacings() {

        srcRangeSpacing = (float)absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        //System.out.println("Range spacing is " + srcRangeSpacing);

        srcAzimuthSpacing = (float)absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        //System.out.println("Azimuth spacing is " + srcAzimuthSpacing);
    }

    /**
     * Compute target image size and range/azimuth spacings.
     *
     * @throws OperatorException The exceptions.
     */
    private void computeTargetImageSizeAndPixelSpacings() throws OperatorException {

        if (outputImageBy.equals(IMAGE_SIZE)) {

            if (targetImageHeight <= 0 || targetImageHeight >= sourceImageHeight ||
                targetImageWidth <= 0 || targetImageWidth >= sourceImageWidth) {
                throw new OperatorException("Output image size must be positive and smaller than the source image size");
            }

            rangeSpacing = srcRangeSpacing * sourceImageWidth / targetImageWidth;
            azimuthSpacing = srcAzimuthSpacing * sourceImageHeight / targetImageHeight;

        } else if (outputImageBy.equals(RATIO)) {

            if (widthRatio <= 0 || widthRatio > 1 || heightRatio <= 0 || heightRatio > 1) {
                throw new OperatorException("The width or height ratio must be within range (0, 1)");
            }

            targetImageHeight = (int)(heightRatio * sourceImageHeight + 0.5f);
            targetImageWidth = (int)(widthRatio * sourceImageWidth + 0.5f);

            rangeSpacing = srcRangeSpacing / widthRatio;
            azimuthSpacing = srcAzimuthSpacing / heightRatio;

        } else if (outputImageBy.equals(PIXEL_SPACING)) {

            if (rangeSpacing <= srcRangeSpacing || azimuthSpacing <= srcAzimuthSpacing) {
                throw new OperatorException("The azimuth or range spacing must be greater than the source spacing");
            }

            targetImageHeight = (int)(srcRangeSpacing / rangeSpacing * sourceImageHeight + 0.5);
            targetImageWidth = (int)(srcAzimuthSpacing / azimuthSpacing * sourceImageWidth + 0.5);

        } else {
            throw new OperatorException("Please specify output image size, or row and column ratios, or pixel spacings");
        }
    }

    /**
     * Compute range and azimuth step size for kernel filtering.
     */
    private void computeRangeAzimuthStepSizes() {

        stepAzimuth = (double)(sourceImageHeight - filterHeight) / (double)(targetImageHeight - 1);
        stepRange = (double)(sourceImageWidth - filterWidth) / (double)(targetImageWidth - 1);
    }

    /**
     * Read pre-defined or user defined kernel file.
     */
    private void getKernelFile() {

        String fileName = "";
        boolean isPreDefinedKernel;

        if (filterType.equals(USER_DEFINED)) { // user defined kernel file

            isPreDefinedKernel = false;
            
        } else { // pre-defined kernel file with user specified filter diemnsion

            isPreDefinedKernel = true;

            if(filterType.equals(SUMMARY)) {
                fileName = "sum_" + filterHeight + "_" + filterWidth + ".ker";
            } else if (filterType.equals(EDGE_DETECT)) {
                fileName = "edd_" + filterHeight + "_" + filterWidth + ".ker";
            } else if (filterType.equals(EDGE_ENHANCEMENT)) {
                fileName = "ede_" + filterHeight + "_" + filterWidth + ".ker";
            } else if (filterType.equals(LOW_PASS)) {
                fileName = "lop_" + filterHeight + "_" + filterWidth + ".ker";
            } else if (filterType.equals(HIGH_PASS)) {
                fileName = "hip_" + filterHeight + "_" + filterWidth + ".ker";
            } else if (filterType.equals(HORIZONTAL)) {
                fileName = "hor_" + filterHeight + "_" + filterWidth + ".ker";
            } else if (filterType.equals(VERTICAL)) {
                fileName = "ver_" + filterHeight + "_" + filterWidth + ".ker";
            } else {
                throw new OperatorException("Incorrect filter type: " + filterType);
            }

            kernelFile = getResFile(fileName);
        }

        kernel = readFile(kernelFile.getAbsolutePath());
        if (isPreDefinedKernel) {
            if (filterHeight != kernel.length || filterWidth != kernel[0].length) {
                throw new OperatorException("Kernel size does not match given filter size");
            }
        } else { // user defined kernel
            filterHeight = kernel.length;
            filterWidth = kernel[0].length;
        }
    }

    private static File getResFile(String fileName) {
        final String homeUrl = ResourceUtils.findHomeFolder().getAbsolutePath();
        final String path = homeUrl + File.separator + "res" + File.separator + "kernels" + File.separator + fileName;
        return new File(path);
    }
    
    /**
     * Read data from kernel file and save them in a 2D array.
     * @param fileName The kernel file name
     * @return array The 2D array holding kernel data
     */
    public static float[][] readFile(String fileName) {

        // get reader
        FileInputStream stream;
        try {
            stream = new FileInputStream(fileName);
        } catch(FileNotFoundException e) {
            throw new OperatorException("File not found: " + fileName);
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        // read data from file and save them in 2-D array
        String line = "";
        StringTokenizer st;
        float[][] array;
        int rowIdx = 0;

        try {
            // get the 1st line
            if ((line = reader.readLine()) == null) {
                throw new OperatorException("Empty file: " + fileName);
            }

            st = new StringTokenizer(line);
            if (st.countTokens() != 2) {
                throw new OperatorException("Incorrect file format: " + fileName);
            }

            final int numRows = Integer.parseInt(st.nextToken());
            final int numCols = Integer.parseInt(st.nextToken());
            array = new float[numRows][numCols];

            // get the rest numRows lines
            while((line = reader.readLine()) != null) {

                st = new StringTokenizer(line);
                if (st.countTokens() != numCols) {
                    throw new OperatorException("Incorrect file format: " + fileName);
                }

                for (int j = 0; j < numCols; j++) {
                    array[rowIdx][j] = Float.parseFloat(st.nextToken());
                }
                rowIdx++;
            }

            if (numRows != rowIdx) {
                throw new OperatorException("Incorrect number of lines in file: " + fileName);
            }

            reader.close();
            stream.close();

        } catch (IOException e) {
            throw new OperatorException(e);
        }
        return array;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    targetImageWidth,
                                    targetImageHeight);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        //ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        //ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        addGeoCoding();

        updateTargetProductMetadata();
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
            final float y = (int)(ty*stepAzimuth + 0.5) + (int)(filterHeight*0.5);
            for (int i = 0; i < gridWidth; i++) {
                final float tx = Math.min(i*subSamplingX, targetImageWidth - 1);
                final float x = (int)(tx*stepRange + 0.5) + (int)(filterWidth*0.5);
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

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, azimuthSpacing);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, rangeSpacing);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);

        final float oldLineTimeInterval = (float)absTgt.getAttributeDouble(AbstractMetadata.line_time_interval);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.line_time_interval, oldLineTimeInterval*(float)stepAzimuth);

        final String oldFirstLineTime = absTgt.getAttributeString(AbstractMetadata.first_line_time);
        final int idx = oldFirstLineTime.lastIndexOf(':') + 1;
        final String oldSecondsStr = oldFirstLineTime.substring(idx);
        final double oldSeconds = Double.parseDouble(oldSecondsStr);
        final double newSeconds = oldSeconds + oldLineTimeInterval*(filterHeight - 1)/2.0;
        final String newFirstLineTime = String.valueOf(oldFirstLineTime.subSequence(0, idx)) + newSeconds + "000000";
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_line_time,
            AbstractMetadata.parseUTC(newFirstLineTime.substring(0,27)));
//          AbstractMetadata.parseUTC(newFirstLineTime.substring(0,27), "dd-MMM-yyyy HH:mm:ss"));
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            if (method.equals(SUB_SAMPLING)) {
                computeTileUsingSubSampling(targetBand, targetTile, pm);
            } else if (method.equals(KERNEL_FILTERING)) {
                computeTileUsingKernelFiltering(targetBand, targetTile);
            } else {
                throw new OperatorException("Unknown undersampling method: " + method);
            }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computeTileUsingSubSampling(Band targetBand, Tile targetTile, ProgressMonitor pm) {

        final ProductData destBuffer = targetTile.getRawSamples();
        final Rectangle rectangle = targetTile.getRectangle();
        try {
            subsetReader.readBandRasterData(targetBand,
                                            rectangle.x,
                                            rectangle.y,
                                            rectangle.width,
                                            rectangle.height,
                                            destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private void computeTileUsingKernelFiltering(Band targetBand, Tile targetTile) {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int tx0 = targetTileRectangle.x;
        final int ty0 = targetTileRectangle.y;
        final int tw  = targetTileRectangle.width;
        final int th  = targetTileRectangle.height;
        //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

        final int x0 = (int)(tx0 * stepRange + 0.5f);
        final int y0 = (int)(ty0 * stepAzimuth + 0.5f);
        final int w = (int)((tx0 + tw - 1)*stepRange + 0.5f) + filterWidth - (int)(tx0*stepRange + 0.5f);
        final int h = (int)((ty0 + th - 1)*stepAzimuth + 0.5f) + filterHeight - (int)(ty0*stepAzimuth + 0.5f);
        final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        Tile sourceRaster1 = null;
        Tile sourceRaster2 = null;
        Band sourceBand1;

        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster1 = getSourceTile(sourceBand1, sourceTileRectangle);
            if (sourceRaster1 == null) {
                throw new OperatorException("Cannot get source tile");
            }
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceRaster1 = getSourceTile(sourceBand1, sourceTileRectangle);
            sourceRaster2 = getSourceTile(sourceBand2, sourceTileRectangle);
            if (sourceRaster1 == null || sourceRaster2 == null) {
                throw new OperatorException("Cannot get source tile");
            }            
        }

        final Unit.UnitType bandUnitType = Unit.getUnitType(sourceBand1);

        final ProductData trgData = targetTile.getDataBuffer();

        double filteredValue;
        final int maxy = ty0 + th;
        final int maxx = tx0 + tw;
        for (int ty = ty0; ty < maxy; ty++) {
            for (int tx = tx0; tx < maxx; tx++) {
                filteredValue = getFilteredValue(tx, ty, sourceRaster1, sourceRaster2, bandUnitType);
                trgData.setElemDoubleAt(targetTile.getDataBufferIndex(tx, ty), filteredValue);
            }
        }
    }

    private double getFilteredValue(int tx, int ty, Tile sourceRaster1, Tile sourceRaster2, Unit.UnitType bandUnitType) {

        final int x0 = (int)(tx * stepRange + 0.5);
        final int y0 = (int)(ty * stepAzimuth + 0.5);
        final int maxY = y0 + filterHeight;
        final int maxX = x0 + filterWidth;

        final ProductData srcData1 = sourceRaster1.getDataBuffer();
        ProductData srcData2 = null;
        if(sourceRaster2 != null)
            srcData2 = sourceRaster2.getDataBuffer();

        float numPixels = filterWidth*filterHeight;
        double filteredValue = 0.0;
        for (int y = y0; y < maxY; y++) {
            for (int x = x0; x < maxX; x++) {

                final int index = sourceRaster1.getDataBufferIndex(x, y);
                final float weight = kernel[maxY - 1 - y][maxX - 1 - x] / numPixels;

                if (bandUnitType == Unit.UnitType.INTENSITY_DB || bandUnitType == Unit.UnitType.AMPLITUDE_DB) {

                    final double dn = srcData1.getElemDoubleAt(index);
                    filteredValue += Math.pow(10, dn / 10.0)*weight; // dB to linear

                } else if (bandUnitType == Unit.UnitType.AMPLITUDE || bandUnitType == Unit.UnitType.INTENSITY) {

                    filteredValue += srcData1.getElemDoubleAt(index)*weight;

                } else { // COMPLEX

                    final double i = srcData1.getElemDoubleAt(index);
                    final double q = srcData2.getElemDoubleAt(index);
                    filteredValue += (i*i + q*q)*weight;
                }
            }
        }

        if (bandUnitType == Unit.UnitType.INTENSITY_DB || bandUnitType == Unit.UnitType.AMPLITUDE_DB) {
            filteredValue = 10.0*Math.log10(filteredValue); // linear to dB
        }
        return filteredValue;
    }

    /**
     * Set undersampling method. The function is for unit test only.
     * @param samplingMethod The undersampling method.
     */
    public void setUndersamplingMethod(String samplingMethod) {
        method = samplingMethod;
    }

    /**
     * Set sub-sampling rate for both x and y. The function is for unit test only.
     * @param subSamplingRateX The sub-sampling rate for x.
     * @param subSamplingRateY The sub-sampling rate for y.
     */
    public void setSubSamplingRate(int subSamplingRateX, int subSamplingRateY) {

        subSamplingX = subSamplingRateX;
        subSamplingY = subSamplingRateY;
    }

    /**
     * Set filter type. The function is for unit test only.
     * @param type The filter type.
     */
    public void setFilterType(String type) {
        filterType = type;
    }

    /**
     * Set filter size. The function is for unit test only.
     * @param size The filter size.
     */
    public void setFilterSize(String size) {
        filterSize = size;
    }

    /**
     * Set the output image dimension. The function is for unit test only.
     * @param numRows The number of rows.
     * @param numCols The number of columns.
     */
    public void setOutputImageSize(int numRows, int numCols) {
        targetImageHeight = numRows;
        targetImageWidth = numCols;
    }

    /**
     * Set the output image method. The function is for unit test only.
     * @param method The output image method.
     */
    public void setOutputImageBy(String method) {
        outputImageBy = method;
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
            super(UndersamplingOp.class);
            setOperatorUI(UndersamplingOpUI.class);
        }
    }
}