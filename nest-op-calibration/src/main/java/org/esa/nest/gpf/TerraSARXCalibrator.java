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
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.BaseCalibrator;
import org.esa.nest.datamodel.Calibrator;
import org.esa.nest.datamodel.Unit;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calibration for TerraSAR-X data products.
 */

public class TerraSARXCalibrator extends BaseCalibrator implements Calibrator {

    private String productType = null;
    private Boolean useIncidenceAngleFromGIM = false;
    private double firstLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days
    private int sourceImageWidth = 0;
    private TiePointGrid incidenceAngle = null;
    private TiePointGrid slantRangeTime = null;
    private final HashMap<String, Double> calibrationFactor = new HashMap<String, Double>(2);
    private final HashMap<String, NoiseRecord[]> noiseRecord = new HashMap<String, NoiseRecord[]>(2);
    private final HashMap<String, int[]> rangeLineIndex = new HashMap<String, int[]>(2); // y indices of noise records
    private final HashMap<String, double[][]> rangeLineNoise = new HashMap<String, double[][]>(2);
    private Product sourceGIMProduct = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public TerraSARXCalibrator() {
    }

    /**
     * Set external auxiliary file.
     */
    public void setExternalAuxFile(File file) throws OperatorException {
        if (file != null) {
            throw new OperatorException("TerraSARXCalibrator: No external auxiliary file should be selected for TerraSAT-X product");
        }
    }

    /**
     * Set auxiliary file flag.
     */
    public void setAuxFileFlag(String file) {
    }

    /**

     */
    public void initialize(final Operator op, final Product srcProduct, final Product tgtProduct,
                           final boolean mustPerformRetroCalibration, final boolean mustUpdateMetadata)
            throws OperatorException {
        try {
            calibrationOp = op;
            sourceProduct = srcProduct;
            targetProduct = tgtProduct;

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            sourceImageWidth = sourceProduct.getSceneRasterWidth();

            getMission();

            getProductType();

            getCalibrationFlag();

            getSampleType();

            getMetadata();

            getCalibrationFactor();

            getNoiseRecords();

            getTiePointGridData();

            if (useIncidenceAngleFromGIM) {
                getGIMProduct();
            }

            computeNoiseForRangeLines();

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch(Exception e) {
            throw new OperatorException("TerraSARXCalibrator: " + e);
        }
    }

    /**
     * Get mission.
     */
    private void getMission() {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if(!(mission.contains("TSX") || mission.contains("TDX")))
            throw new OperatorException("TerraSARXCalibrator: " + mission +
                    " is not a valid mission for TerraSAT-X Calibration");
    }

    /**
     * Get product type.
     */
    private void getProductType() {
        productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (productType.contains("EEC")) {
            useIncidenceAngleFromGIM = true;
        }
    }

    /**
     * Get calibration factors for all polarizations.
     */
    private void getCalibrationFactor() {
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement level1Product = origProdRoot.getElement("level1Product");
        final MetadataElement calibrationElem = level1Product.getElement("calibration");
        final MetadataElement[] subElems = calibrationElem.getElements();
        for (MetadataElement ele : subElems) {
            if (ele.getName().contains("calibrationConstant")) {
                final String pol = ele.getAttributeString("polLayer").toUpperCase();
                final double factor = Double.parseDouble(ele.getAttributeString("calFactor"));
                calibrationFactor.put(pol, factor);
            }
        }
    }

    private void getMetadata() {
        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day
        if (lineTimeInterval == 0.0) {
            throw new OperatorException("Invalid input for Line Time Interval: " + lineTimeInterval);
        }
    }

    /**
     * Get image noise records.
     */
    private void getNoiseRecords() {

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement level1Product = origProdRoot.getElement("level1Product");
        final MetadataElement[] subElems = level1Product.getElements();
        for (MetadataElement ele : subElems) {
            if (!ele.getName().contains("noise")) {
                continue;
            }

            final String pol = ele.getAttributeString("polLayer").toUpperCase();
            final int numOfNoiseRecords = Integer.parseInt(ele.getAttributeString("numberOfNoiseRecords"));
            final MetadataElement[] imageNoiseElem = ele.getElements();
            if (numOfNoiseRecords != imageNoiseElem.length) {
                throw new OperatorException(
                        "TerraSARXCalibrator: The number of noise records does not match the record number.");
            }

            NoiseRecord[] record = new NoiseRecord[numOfNoiseRecords];
            for (int i = 0; i < numOfNoiseRecords; ++i) {
                record[i] = new NoiseRecord();
                record[i].timeUTC = ReaderUtils.getTime(imageNoiseElem[i], "timeUTC", AbstractMetadata.dateFormat).getMJD();
                record[i].noiseEstimateConfidence = Double.parseDouble(imageNoiseElem[i].getAttributeString("noiseEstimateConfidence"));

                final MetadataElement noiseEstimate = imageNoiseElem[i].getElement("noiseEstimate");
                record[i].validityRangeMin = Double.parseDouble(noiseEstimate.getAttributeString("validityRangeMin"));
                record[i].validityRangeMax = Double.parseDouble(noiseEstimate.getAttributeString("validityRangeMax"));
                record[i].referencePoint = Double.parseDouble(noiseEstimate.getAttributeString("referencePoint"));
                record[i].polynomialDegree = Integer.parseInt(noiseEstimate.getAttributeString("polynomialDegree"));

                final MetadataElement[] coefficientElem = noiseEstimate.getElements();
                if (record[i].polynomialDegree+1 != coefficientElem.length) {
                    throw new OperatorException(
                            "TerraSARXCalibrator: The number of coefficients does not match the polynomial degree.");
                }

                record[i].coefficient = new double[record[i].polynomialDegree+1];
                for (int j = 0; j < coefficientElem.length; ++j) {
                    record[i].coefficient[j] = Double.parseDouble(coefficientElem[j].getAttributeString("coefficient"));
                }
            }

            noiseRecord.put(pol, record);
        }
    }

    /**
     * Compute noise for the whole range lines corresponding to the noise records for all polarizations.
     */
    private void computeNoiseForRangeLines() {

        Set<Map.Entry<String, NoiseRecord[]>> set = noiseRecord.entrySet();
        for (Map.Entry<String, NoiseRecord[]> elem : set) {
            final String pol = elem.getKey();
            final NoiseRecord[] record = elem.getValue();
            final int numOfNoiseRecords = record.length;
            double[][] noise = new double[numOfNoiseRecords][sourceImageWidth];
            int [] index = new int[numOfNoiseRecords];

            for (int i = 0; i < numOfNoiseRecords; ++i) {
                index[i] = (int)((record[i].timeUTC - firstLineUTC) / lineTimeInterval + 0.5);
                for (int j = 0; j < sourceImageWidth; ++j) {
                    final double slantRgTime = slantRangeTime.getPixelDouble(j, index[i])/1.0e9; // ns to s
                    if (slantRgTime < record[i].validityRangeMin || slantRgTime > record[i].validityRangeMax) {
                        throw new OperatorException("TerraSARXCalibrator: Invalid slant range time: " + slantRgTime);
                    }
                    noise[i][j] = org.esa.nest.util.MathUtils.computePolynomialValue(
                            slantRgTime - record[i].referencePoint, record[i].coefficient);
                }
            }
            rangeLineIndex.put(pol, index);
            rangeLineNoise.put(pol, noise);
        }
    }

    /**
     * Get incidence angle and slant range time tie point grids.
     */
    private void getTiePointGridData() {
        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        slantRangeTime = OperatorUtils.getSlantRangeTime(sourceProduct);
    }

    /**
     * Get GIM product.
     */
    private void getGIMProduct() {
        try {
            File sourceGIMFile =
                    new File(sourceProduct.getFileLocation().getParentFile(), "AUXRASTER" + File.separator + "GIM.tif");

            if (sourceGIMFile.exists()) {
                sourceGIMProduct = ProductIO.readProduct(sourceGIMFile);
            } else {
                useIncidenceAngleFromGIM = false;
            }

        } catch(Exception e) {
            throw new OperatorException("TerraSARXCalibrator: " + e);
        }
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);

        abs.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);
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
    public void computeTile(Band targetBand, Tile targetTile,
                            HashMap<String, String[]> targetBandNameToSourceBandName,
                            ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);
        
        Tile sourceRaster1 = null;
        ProductData srcData1 = null;
        ProductData srcData2 = null;
        Band sourceBand1 = null;

        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster1 = calibrationOp.getSourceTile(sourceBand1, targetTileRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceRaster1 = calibrationOp.getSourceTile(sourceBand1, targetTileRectangle);
            final Tile sourceRaster2 = calibrationOp.getSourceTile(sourceBand2, targetTileRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
            srcData2 = sourceRaster2.getDataBuffer();
        }

        Tile srcGIMTile = null;
        ProductData srcGIMData = null;
        if (useIncidenceAngleFromGIM) {
            srcGIMTile = calibrationOp.getSourceTile(sourceGIMProduct.getBand("band_1"), targetTileRectangle);
            srcGIMData = srcGIMTile.getDataBuffer();
        }

        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
        final double noDataValue = sourceBand1.getNoDataValue();

        // copy band if unit is phase
        if(bandUnit == Unit.UnitType.PHASE) {
            targetTile.setRawSamples(sourceRaster1.getRawSamples());
            return;
        }

        final String pol = OperatorUtils.getBandPolarization(srcBandNames[0], absRoot).toUpperCase();
        double Ks = 0.0;
        if (pol != null) {
            Ks = calibrationFactor.get(pol);
        }

        double[][] tileNoise = new double[h][w];
        computeTileNoise(pol, x0, y0, w, h, tileNoise);

        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);

        final int maxY = y0 + h;
        final int maxX = x0 + w;

        double sigma, dn, i, q;
        int index;

        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                index = srcIndex.getIndex(x);

                if (bandUnit == Unit.UnitType.AMPLITUDE) {
                    dn = srcData1.getElemDoubleAt(index);
                    if (dn == noDataValue) { // skip noDataValue
                        continue;
                    }
                    sigma = dn*dn;
                } else if (bandUnit == Unit.UnitType.INTENSITY) {
                    sigma = srcData1.getElemDoubleAt(index);
                } else if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
                    i = srcData1.getElemDoubleAt(index);
                    q = srcData2.getElemDoubleAt(index);
                    sigma = i * i + q * q;
                } else {
                    throw new OperatorException("TerraSARXCalibrator: unhandled unit");
                }

                if (!useIncidenceAngleFromGIM) {
                    sigma = Ks*(sigma - tileNoise[y-y0][x-x0])*Math.sin(incidenceAngle.getPixelDouble(x, y)*MathUtils.DTOR);
                } else {
                    final int gim = srcGIMData.getElemIntAt(index);
                    final double inciAng = (gim - (gim%10))/100.0;
                    sigma = Ks*(sigma - tileNoise[y-y0][x-x0])*Math.sin(inciAng*MathUtils.DTOR);
                }

                if (outputImageScaleInDb) { // convert calibration result to dB
                    if (sigma < underFlowFloat) {
                        sigma = -underFlowFloat;
                    } else {
                        sigma = 10.0 * Math.log10(sigma);
                    }
                }

                trgData.setElemDoubleAt(index, sigma);
            }
        }
    }

    /**
     * Compute noise for each pixel in the tile.
     * @param pol Polarization string.
     * @param x0 X coordinate for pixel at the upper left corner of the tile.
     * @param y0 Y coordinate for pixel at the upper left corner of the tile.
     * @param w Tile width.
     * @param h Tile height.
     * @param tileNoise Array holding noise for the tile.
     */
    private void computeTileNoise(final String pol, final int x0, final int y0, final int w,
                                  final int h, double[][] tileNoise) {

        final int[] indexArray = rangeLineIndex.get(pol);
        final double[][] noise = rangeLineNoise.get(pol);

        int i1 = 0, i2 = 0;
        int y1 = 0, y2 = 0;
        for (int y = y0; y < y0 + h; ++y) {

            for (int i = 0; i < indexArray.length; ++i) {
                if (indexArray[i] <= y) {
                    i1 = i;
                    y1 = indexArray[i];
                } else {
                    i2 = i;
                    y2 = indexArray[i];
                    break;
                }
            }

            if (y1 == indexArray[indexArray.length-1]) {
                y2 = y1;
                i2 = i1;
            } else if (y1 > y2) {
                throw new OperatorException("TerraSARXCalibrator: No noise is defined for pixel with y = " + y);
            }

            for (int x = x0; x < x0 + w; ++x) {
                final double n1 = noise[i1][x];
                final double n2 = noise[i2][x];
                double mu = 0.0;
                if (y1 != y2) {
                    mu = (double)(y - y1) / (double)(y2 - y1);
                }
                tileNoise[y-y0][x-x0] = org.esa.nest.util.MathUtils.interpolationLinear(n1, n2, mu);
            }
        }
    }

    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre,final double localIncidenceAngle,
            final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        double sigma = 0.0;
        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            sigma = v*v;
        } else if (bandUnit == Unit.UnitType.INTENSITY || bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
            sigma = v;
        } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
            sigma = Math.pow(10, v/10.0); // convert dB to linear scale
        } else {
            throw new OperatorException("TerraSARXCalibrator: Unknown band unit");
        }

        final double Ks = calibrationFactor.get(bandPolar.toUpperCase());
        sigma *= Ks*Math.sin(localIncidenceAngle*MathUtils.DTOR);
        return sigma;
    }

    public double applyRetroCalibration(
            int x, int y, double v, String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {
        return v;
    }

    public void removeFactorsForCurrentTile(Band targetBand, Tile targetTile, String srcBandName)
            throws OperatorException {

        Band sourceBand = sourceProduct.getBand(targetBand.getName());
        Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }

    private final static class NoiseRecord {
        public double timeUTC;
        public double noiseEstimateConfidence;
        public double validityRangeMin;
        public double validityRangeMax;
        public double referencePoint;
        public int polynomialDegree;
        public double[] coefficient;
    }
}