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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;

import java.awt.*;
import java.util.ArrayList;

/**
 * Apply thermal noise correction to Level-1 Sentinel products
 */
@OperatorMetadata(alias = "RemoveThermalNoise",
        category = "SAR Tools\\SENTINEL-1",
        authors = "Cecilia Wong, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description="Removes thermal noise from Sentinel-1 products")
public final class RemoveThermalNoiseOp extends Operator {

    // One source product
    @SourceProduct
    private Product sourceProduct;

    // One target product
    @TargetProduct
    private Product targetProduct;

    static private final String ANNOTATION_FLAG_NAME = "thermalNoiseCorrectionPerformed";

    // To indicate if thermal noise should be removed or added back in.
    // true means remove thermal noise
    // false means add thermal noise back in
    private boolean performCorrection = true;

    // The lines in the image for which noise values are available.
    // An image line is uniquely identified by a y-value. noiseLine contain these y-values.
    private int[] noiseLine = null;

    // For each image line, noiseLine[i], that has noise values, there is an array of x-values,
    // noisePixel.get(i), and the array of noise values, noiseValue.get(i), at the pixel at (x,y).
    private ArrayList<int[]> noisePixel = new ArrayList<>();
    private ArrayList<double[]> noiseValue = new ArrayList<>();

    private int[] calibrationLine = null;

    private ArrayList<int[]> calibrationPixel = new ArrayList<>();
    private ArrayList<double[]> sigma0 = new ArrayList<>();
    private ArrayList<double[]> beta0 = new ArrayList<>();
    private ArrayList<double[]> gamma = new ArrayList<>();
    private ArrayList<double[]> dn = new ArrayList<>();

    private enum NOISE_BAND_TYPE { SIGMA0, BETA0, GAMMA, DN, INVALID };

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public RemoveThermalNoiseOp() {
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

            final MetadataElement oriProdMetadata = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

            final MetadataElement finalMetadataElem = getParentMetadataElemForFlag(oriProdMetadata);
            final String flag = finalMetadataElem.getAttributeString(ANNOTATION_FLAG_NAME);

            performCorrection = flag.toLowerCase().equals("false");

            createTargetProduct();

            updateTargetProductMetadata();

            readLUTs(oriProdMetadata);

            //System.out.println("performCorrection = " + performCorrection);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private MetadataElement getParentMetadataElemForFlag(final MetadataElement oriProdMetadata) {

        final MetadataElement metadataElem1 = getMetadataElement(oriProdMetadata, "annotation");
        final MetadataElement metadataElem2 = metadataElem1.getElementAt(0);
        if (metadataElem2 == null) {
            throw new OperatorException("Metadata element annotation is empty");
        }
        final MetadataElement metadataElem3 = getMetadataElement(metadataElem2, "product");
        final MetadataElement metadataElem4 = getMetadataElement(metadataElem3, "imageAnnotation");
        return getMetadataElement(metadataElem4, "processingInformation");
    }

    private MetadataElement getMetadataElement(final MetadataElement parent, final String childName) {

        MetadataElement child = parent.getElement(childName);
        if (child == null) {
            throw new OperatorException("No metadata element " + childName + " in " + parent.getName());
        }
        return child;
    }

    private boolean shouldApplyCorrection(final Band band) {

        if (band instanceof VirtualBand) { // Never apply thermal noise correction to virtual bands

            return false;

        } else if (getNoiseBandType(band.getName()) == NOISE_BAND_TYPE.INVALID) {

            return false;

        } else {

            return true;
        }
    }

    private NOISE_BAND_TYPE getNoiseBandType(final String bandName) {

        // TBD

        final String bname = bandName.toLowerCase();

        if (bname.contains("sigma")) {

            return NOISE_BAND_TYPE.SIGMA0;

        } else if (bname.contains("beta")) {

            return NOISE_BAND_TYPE.BETA0;

        } else if (bname.contains("gamma")) {

            return NOISE_BAND_TYPE.GAMMA;

        } else if (bandName.toLowerCase().contains("amplitude")) {

            return NOISE_BAND_TYPE.DN;

        } else {

            return NOISE_BAND_TYPE.INVALID;
        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        //System.out.println("createTargetProduct: new targetProduct -- " + targetProduct.getName() + " h = " + targetProduct.getSceneRasterHeight() + " w = " + targetProduct.getSceneRasterWidth());

        final Band[] sourceBands = sourceProduct.getBands();

        for (Band srcBand : sourceBands) {

            if ((srcBand instanceof VirtualBand))  {

                // Cannot use ProductUtils.copyBand() to copy virtual bands, they will become real bands.

                final VirtualBand srcVirtualBand = (VirtualBand) srcBand;
                final VirtualBand newVirtualBand = new VirtualBand(srcBand.getName(),
                        srcBand.getDataType(),
                        srcBand.getSceneRasterWidth(),
                        srcBand.getRasterHeight(),
                        srcVirtualBand.getExpression());
                targetProduct.addBand(newVirtualBand);
                ProductUtils.copyRasterDataNodeProperties(srcBand, newVirtualBand);

            } else if (shouldApplyCorrection(srcBand)) {

                final Band newBand = new Band(srcBand.getName(),
                                              srcBand.getDataType(),
                                              srcBand.getSceneRasterWidth(),
                                              srcBand.getRasterHeight());
                targetProduct.addBand(newBand);
                ProductUtils.copyRasterDataNodeProperties(srcBand, newBand);

                //System.out.println("createTargetProduct: newBand -- " + newBand.getName() + " h = " + newBand.getRasterHeight() + " w = " + newBand.getRasterWidth() + " src h = " + srcBand.getRasterHeight() + " src w = " + srcBand.getRasterWidth());

            } else {

                ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
            }
        }

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    private void updateTargetProductMetadata() {

        final MetadataElement oriProdMetadata = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        final MetadataElement finalMetadataElem = getParentMetadataElemForFlag(oriProdMetadata);

        if (performCorrection) {

            finalMetadataElem.setAttributeString(ANNOTATION_FLAG_NAME, "true");

        } else {

            finalMetadataElem.setAttributeString(ANNOTATION_FLAG_NAME, "false");
        }
    }

    // This reads one row of values in the LUT.
    // One row is called a vector.
    // In each x-y point in the LUT, there are stored multiple fields, each field can be integer or double.
    // Returns line (i.e. y value) of the vector.
    // intNames[i] corresponds to intValues.get(i)
    // doubleNames[i] corresponds to doubleValues.get(i)
    private int readValuesForOneVector(MetadataElement vector,
                                       String intNames[], ArrayList<int[]> intValues,
                                       String doubleNames[], ArrayList<double[]> doubleValues) {

        if (vector == null || intNames.length == 0 && doubleNames.length == 0) {

            return -1;
        }

        // For some reason this line does not work for subsets
        //final int line = vector.getAttributeInt("line");
        final String lineStr = vector.getAttributeString("line");
        final int line = Integer.parseInt(lineStr);

        //System.out.println(vector.getName() + " line str = " + vector.getAttributeString("line") + " line = " + line);

        // This is the width of the LUT, i.e. number of columns.
        final int count = (intNames.length > 0) ?
                            Integer.parseInt(getMetadataElement(vector, intNames[0]).getAttributeString("count")) :
                            Integer.parseInt(getMetadataElement(vector, doubleNames[0]).getAttributeString("count"));

        for (String name : intNames) {

            final String[] valuesAsArrayOfStrings = readValuesAsArrayOfStrings(vector, name, count);
            final int values[] = new int[count];
            for (int i = 0; i < count; i++ ) {
                values[i] = Integer.parseInt(valuesAsArrayOfStrings[i]);
            }
            intValues.add(values);
        }

        for (String name : doubleNames) {

            final String[] valuesAsArrayOfStrings = readValuesAsArrayOfStrings(vector, name, count);
            final double values[] = new double[count];
            for (int i = 0; i < count; i++ ) {
                values[i] = Double.parseDouble(valuesAsArrayOfStrings[i]);
            }
            doubleValues.add(values);
        }

        return line;
    }

    private String[] readValuesAsArrayOfStrings(final MetadataElement vector, final String name, final int count) {

        final MetadataElement elem = getMetadataElement(vector, name);

        // This does not work?!
        //final int cnt = elem.getAttributeInt("count");
        final String cntStr = elem.getAttributeString("count");
        final int cnt = Integer.parseInt(cntStr);

        if (cnt != count) {

            throw new OperatorException("readValuesAsArrayOfStrings: " + name + " has " + cnt + " elements, expecting " + count);
        }

        return elem.getAttributeString(name).split("\\s+");
    }

    private void readLUTs(final MetadataElement oriProdMetadata) {

        readNoiseLUT(oriProdMetadata);
        readCalibrationLUT(oriProdMetadata);
    }

    private void readNoiseLUT(final MetadataElement oriProdMetadata) {

        final MetadataElement metadataElem1 = getMetadataElement(oriProdMetadata, "noise");
        final MetadataElement metadataElem2 = metadataElem1.getElementAt(0);
        if (metadataElem2 == null) {
            throw new OperatorException("Metadata element noise is empty");
        }
        final MetadataElement metadataElem3 = getMetadataElement(metadataElem2, "noise");
        final MetadataElement noiseVectorList = getMetadataElement(metadataElem3, "noiseVectorList");

        final int numNoiseVectors = noiseVectorList.getNumElements();

        if (numNoiseVectors < 2) {

            throw new OperatorException("Too few noise vectors: " + numNoiseVectors);
        }

        //System.out.println("numNoiseVectors = " + numNoiseVectors);

        noiseLine = new int[numNoiseVectors];

        for (int i = 0; i < numNoiseVectors; i++) {

            final MetadataElement noiseVector = noiseVectorList.getElementAt(i);

            final String intNames[] = {"pixel"};
            final String doubleNames[] = {"noiseLUT"};
            ArrayList<int[]> intValues = new ArrayList<>();
            ArrayList<double[]> doubleValues = new ArrayList<>();
            noiseLine[i] = readValuesForOneVector(noiseVector, intNames, intValues, doubleNames, doubleValues);
            noisePixel.add(intValues.get(0));
            noiseValue.add(doubleValues.get(0));
        }
    }

    private void readCalibrationLUT(final MetadataElement oriProdMetadata) {

        final MetadataElement metadataElem1 = getMetadataElement(oriProdMetadata, "calibration");
        final MetadataElement metadataElem2 = metadataElem1.getElementAt(0);
        if (metadataElem2 == null) {
            throw new OperatorException("Metadata element calibration is empty");
        }

        final MetadataElement metadataElem3 = getMetadataElement(metadataElem2, "calibration");
        final MetadataElement calibrationVectorList = getMetadataElement(metadataElem3, "calibrationVectorList");
        final int numCalibrationVectors = calibrationVectorList.getNumElements();

        if (numCalibrationVectors < 2) {

            throw new OperatorException("Too few noise vectors: " + numCalibrationVectors);
        }

        calibrationLine = new int[numCalibrationVectors];

        for (int i = 0; i < numCalibrationVectors; i++) {

            final MetadataElement calibrationVector = calibrationVectorList.getElementAt(i);

            final String intNames[] = {"pixel"};
            final String doubleNames[] = {"sigmaNought", "betaNought", "gamma", "dn"};
            ArrayList<int[]> intValues = new ArrayList<>();
            ArrayList<double[]> doubleValues = new ArrayList<>();
            calibrationLine[i] = readValuesForOneVector(calibrationVector, intNames, intValues, doubleNames, doubleValues);
            calibrationPixel.add(intValues.get(0));
            sigma0.add(doubleValues.get(0));
            beta0.add(doubleValues.get(1));
            gamma.add((doubleValues.get(2)));
            dn.add(doubleValues.get(3));
        }
    }

    private static int findLeftOfBracket(final int val, final int[] values) {

        if (values.length < 2) {

            throw new OperatorException("Invalid values.length: " + values.length);
        }

        if (val < 0 || val > values[values.length-1]) {

            // This should never happen.
            // If this happens, one possibility is the noise LUT in the metadata is not complete, i.e.,
            // it is missing the last line of the image.
            return -1;
        }

        int leftIdx = 0;
        int rightIdx = values.length-1;

        int cnt = 0;

        while (rightIdx >= leftIdx) {

            if (val == values[leftIdx]) {

                return leftIdx;
            }

            if (val == values[rightIdx]) {

                return rightIdx;
            }

            if ((leftIdx+1) == rightIdx) {

                return leftIdx;
            }

            int midIdx = (leftIdx + rightIdx) / 2;

            if (val > values[midIdx]) {

                leftIdx = midIdx;

            } else {

                rightIdx = midIdx;
            }

            cnt++;

            if (cnt > values.length) {

                // This should never happen
                throw new OperatorException("Possible infinite loop");
            }
        }

        return -1; // Should never reach here
    }

    private static double computeValue(final int x, final int y, final int[] line, final ArrayList<int[]> pixel, final ArrayList<double[]> value) {

        // TBD Optimize!!!!!

        final int leftYIdx = findLeftOfBracket(y, line);
        final int rightYIdx = (y == line[leftYIdx]) ? leftYIdx : leftYIdx+1;

        double val = 0.0;

        checkBracket(leftYIdx, rightYIdx, line.length-1);

        if (leftYIdx == rightYIdx) {

            if (y != line[leftYIdx]) {

                throw new OperatorException("computeValue: y = " + y + " leftYIdx == rightYIdx = " + leftYIdx);
            }

        } else if (y <= line[leftYIdx] || y >= line[rightYIdx])  {

            throw new OperatorException("computeValue: y = " + y + " line[" + leftYIdx + "] = " + line[leftYIdx] + " line[" + rightYIdx + "] = " + line[rightYIdx]);
        }

        // It is not clear if it can be assumed that each line will have noise values at the same pixels.
        // Say the image is 100 rows by 200 columns.
        // The way the metadata is set up, the following scenario is possible...
        // Say we have noise values for y = 0, 75, 99. (The lines do not have to evenly spaced.)
        // For y = 0, we can have noise values at x = 0, 5, 9, 50, 78, 150, 199.
        // For y = 75, we can have noise values at x = 0, 79, 167, 199.
        // for y = 99, we can have noise values at x = 0, 120, 199.

        // Do linear interpolation in x direction along the top line.
        final double topVal = linearInterpolateAlongLine(x, leftYIdx, pixel, value);

        // Do interpolation in x direction along the bottom line.
        final double bottomVal = linearInterpolateAlongLine(x, rightYIdx, pixel, value);

        // Do interpolation in y direction
        val = linearInterpolate(line[leftYIdx], line[rightYIdx], y, topVal, bottomVal);

        return val;
    }


    private static double linearInterpolateAlongLine(final int x, final int yIdx,
                                                     final ArrayList<int[]> pixel, final ArrayList<double[]> value) {

        final int leftXIdx = findLeftOfBracket(x, pixel.get(yIdx));
        final int rightXIdx = (x == pixel.get(yIdx)[leftXIdx]) ? leftXIdx : leftXIdx+1;

        checkBracket(leftXIdx, rightXIdx, pixel.get(yIdx).length );

        final double leftNoise = value.get(yIdx)[leftXIdx];
        final double rightNoise =  value.get(yIdx)[rightXIdx];

        return linearInterpolate(pixel.get(yIdx)[leftXIdx], pixel.get(yIdx)[rightXIdx], x, leftNoise, rightNoise);
    }

    private static void checkBracket(final int left, final int right, final int max) {

        if (left < 0 || right < 0 || left > max || right > max) {

            throw new OperatorException("RemoveThermalNoiseOp::checkBracket: left = " + left + " right = " + right + " max = " + max);
        }
    }

    private static double linearInterpolate(final int x0, final int x1, final int x, final double y0, final double y1) {

        if (x1 == x0) {
            return y0;
        } else {
            return y0 + (y1-y0)*(x-x0)/(x1-x0);
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

        //System.out.println("computeTile: targetBand -- " + targetBand.getName() + " h = " + targetBand.getRasterHeight() + " w = " + targetBand.getRasterWidth());

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int tx0 = targetTileRectangle.x;
        final int ty0 = targetTileRectangle.y;
        final int tw  = targetTileRectangle.width;
        final int th  = targetTileRectangle.height;

        //System.out.println("tx0 = " + tx0 + " ty0 = " + ty0 + " tw = " + tw + " th = " + th);

        // Source tile has the same dimension as the target tile.
        final Rectangle sourceTileRectangle = new Rectangle(tx0, ty0, tw, th);

        try {

            final String bandName = targetBand.getName();
            final Band srcBand = sourceProduct.getBand(bandName);

            //System.out.println("computeTile: srctBand -- " + srcBand.getName() + " h = " + srcBand.getRasterHeight() + " w = " + srcBand.getRasterWidth());

            final Tile srcTile = getSourceTile(srcBand, sourceTileRectangle);
            if (srcTile == null) {
                throw new OperatorException("Failed to get source tile " + targetBand.getName());
            }

            final ProductData srcData = srcTile.getDataBuffer();
            final ProductData tgtData = targetTile.getDataBuffer();

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            ArrayList<double []> calibrationValue = null;

            final NOISE_BAND_TYPE noiseBandType = getNoiseBandType(bandName);

            switch (noiseBandType) {

                case SIGMA0:
                    calibrationValue = sigma0;
                    break;
                case BETA0:
                    calibrationValue = beta0;
                    break;
                case GAMMA:
                    calibrationValue = gamma;
                    break;
                case DN:
                    calibrationValue = dn;
                    break;
                case INVALID:
                    throw new OperatorException("computeTile should not be called for this band: " + bandName);
            }

            // Process pixel by pixel in the tile
            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    final int index = targetTile.getDataBufferIndex(x, y);

                    final Double srcValue = srcData.getElemDoubleAt(index);

                    final double eta = computeValue(x, y, noiseLine, noisePixel, noiseValue);

                    final double A = computeValue(x, y, calibrationLine, calibrationPixel, calibrationValue);

                    final double noise = performCorrection? eta/A : -eta/A;

                    final double tgtValue = srcValue + noise;

                    tgtData.setElemDoubleAt(index, tgtValue);
                    //tgtData.setElemDoubleAt(index, eta); // TBD debug
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }

        //System.out.println("computeTile: DONE computeTile targetBand: " + targetBand.getName() + " h = " + targetBand.getRasterHeight() + " w = " + targetBand.getRasterWidth());
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
            super(RemoveThermalNoiseOp.class);
        }
    }
}