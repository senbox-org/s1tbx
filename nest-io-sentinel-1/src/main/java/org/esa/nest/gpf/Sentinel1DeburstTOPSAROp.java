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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
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
import org.esa.nest.util.MathUtils;


import java.awt.*;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * De-Burst a Sentinel-1 TOPSAR product
 */
@OperatorMetadata(alias = "DeburstTOPSAR",
                  category = "SAR Tools",
                  authors = "NEST team", copyright = "(c) 2012 by Array Systems Computing Inc.",
                  description="Debursts a Sentinel-1 TOPSAR product")
public final class Sentinel1DeburstTOPSAROp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false", label="Mean Average Intensities")
    private boolean average = false;

    private MetadataElement absRoot = null;
    private String acquisitionMode = null;

    private int numOfSubSwath = 0;
    private int targetWidth = 0;
    private int targetHeight = 0;

    private double targetFirstLineTime = 0;
    private double targetLastLineTime = 0;
    private double targetLineTimeInterval = 0;
    private double targetSlantRangeTimeToFirstPixel = 0;
    private double targetSlantRangeTimeToLastPixel = 0;
    private double targetDeltaSlantRangeTime = 0;
    private double nodatavalue = 0;

    private SubSwathInfo[] subSwath = null;
    private String[] polarizations = new String[2];


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public Sentinel1DeburstTOPSAROp() {
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
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getAcquisitionMode();

            getProductPolarizations();

            getSubSwathParameters();

            getSubSwathNoiseVectors();

            computeTargetStartEndTime();

            computeTargetSlantRangeTimeToFirstAndLastPixels();

            computeTargetWidthAndHeight();

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void getAcquisitionMode() {

        acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

        if (acquisitionMode.equals("IW")) {
            numOfSubSwath = 3;
        } else if (acquisitionMode.equals("EW")){
            numOfSubSwath = 5;
        } else {
            throw new OperatorException("Acquisition mode is not IW or EW");
        }
    }

    private void getProductPolarizations() {

        final MetadataElement[] elems = absRoot.getElements();
        final String subSwathName = acquisitionMode + "1";
        int k = 0;
        for(MetadataElement elem : elems) {
            if(elem.getName().contains(subSwathName)) {
                polarizations[k++] = elem.getAttributeString("polarization");
            }
        }
    }

    private void getSubSwathParameters() {

        subSwath = new SubSwathInfo[numOfSubSwath];
        for (int i = 0; i < numOfSubSwath; i++) {
            subSwath[i] = new SubSwathInfo();
            subSwath[i].subSwathName = acquisitionMode + (i+1);
            final MetadataElement subSwathMetadata = getSubSwathMetadata(subSwath[i].subSwathName);
            setParameters(subSwathMetadata, subSwath[i]);
        }
    }

    private MetadataElement getSubSwathMetadata(final String subSwathName) {

        final MetadataElement root = sourceProduct.getMetadataRoot();
        if (root == null) {
            throw new OperatorException("Root Metadata not found");
        }

        MetadataElement Original_Product_Metadata = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

        MetadataElement annotation = Original_Product_Metadata.getElement("annotation");
        if (annotation == null) {
            throw new OperatorException("Annotation Metadata not found");
        }

        final MetadataElement[] elems = annotation.getElements();
        for(MetadataElement elem : elems) {
            if(elem.getName().contains(subSwathName.toLowerCase())) {
                return elem;
            }
        }

        return null;
    }

    private void setParameters(final MetadataElement subSwathMetadata, SubSwathInfo subSwath) {

        MetadataElement product = subSwathMetadata.getElement("product");
        MetadataElement imageAnnotation = product.getElement("imageAnnotation");
        MetadataElement imageInformation = imageAnnotation.getElement("imageInformation");
        MetadataElement swathTiming = product.getElement("swathTiming");
        MetadataElement burstList = swathTiming.getElement("burstList");
        final MetadataElement[] burstListElem = burstList.getElements();

        subSwath.firstLineTime = Sentinel1Utils.getTime(imageInformation, "productFirstLineUtcTime").getMJD();
        subSwath.lastLineTime = Sentinel1Utils.getTime(imageInformation, "productLastLineUtcTime").getMJD();
        subSwath.numOfSamples = imageInformation.getAttributeInt("numberOfSamples");
        subSwath.numOfLines = imageInformation.getAttributeInt("numberOfLines");
        subSwath.azimuthTimeInterval = imageInformation.getAttributeDouble("azimuthTimeInterval")/86400; // s to day
        subSwath.rangePixelSpacing = imageInformation.getAttributeDouble("rangePixelSpacing");
        subSwath.slrTimeToFirstPixel = imageInformation.getAttributeDouble("slantRangeTime")/2; // 2-way to 1-way
        subSwath.slrTimeToLastPixel = subSwath.slrTimeToFirstPixel +
                (subSwath.numOfSamples - 1)*subSwath.rangePixelSpacing/Constants.lightSpeed;

        subSwath.numOfBursts = burstList.getAttributeInt("count");
        subSwath.linesPerBurst = swathTiming.getAttributeInt("linesPerBurst");
        subSwath.samplesPerBurst = swathTiming.getAttributeInt("samplesPerBurst");
        subSwath.burstFirstLineTime = new double[subSwath.numOfBursts];
        subSwath.burstLastLineTime = new double[subSwath.numOfBursts];
        subSwath.firstValidSample = new int[subSwath.numOfBursts][];
        subSwath.lastValidSample = new int[subSwath.numOfBursts][];

        int k = 0;
        for (MetadataElement listElem : burstListElem) {
            subSwath.burstFirstLineTime[k] = Sentinel1Utils.getTime(listElem, "azimuthTime").getMJD();
            subSwath.burstLastLineTime[k] = subSwath.burstFirstLineTime[k] +
                    (subSwath.linesPerBurst - 1)*subSwath.azimuthTimeInterval;
            MetadataElement firstValidSampleElem = listElem.getElement("firstValidSample");
            MetadataElement lastValidSampleElem = listElem.getElement("lastValidSample");
            subSwath.firstValidSample[k] = Sentinel1Utils.getIntArray(firstValidSampleElem, "firstValidSample");
            subSwath.lastValidSample[k] = Sentinel1Utils.getIntArray(lastValidSampleElem, "lastValidSample");
            k++;
        }
    }

    private void getSubSwathNoiseVectors() {

        for (int i = 0; i < numOfSubSwath; i++) {
            for (String pol:polarizations) {
                final Band srcBand = getSourceBand(subSwath[i].subSwathName, pol);
                Sentinel1Utils.NoiseVector[] noiseVectors = Sentinel1Utils.getNoiseVector(srcBand);
                subSwath[i].noise.put(pol, noiseVectors);
            }
        }

    }

    private Band getSourceBand(final String subSwathName, final String polarization) {

        final Band[] sourceBands = sourceProduct.getBands();
        for (Band band:sourceBands) {
            if (band.getName().contains(subSwathName + "_" + polarization)) {
                return band;
            }
        }
        return null;
    }

    private void computeTargetStartEndTime() {

        targetFirstLineTime = subSwath[0].firstLineTime;
        targetLastLineTime = subSwath[0].lastLineTime;
        for (int i = 1; i < numOfSubSwath; i++) {
            if (targetFirstLineTime > subSwath[i].firstLineTime) {
                targetFirstLineTime = subSwath[i].firstLineTime;
            }

            if (targetLastLineTime < subSwath[i].lastLineTime) {
                targetLastLineTime = subSwath[i].lastLineTime;
            }
        }
        targetLineTimeInterval = subSwath[0].azimuthTimeInterval; // in days
    }

    private void computeTargetSlantRangeTimeToFirstAndLastPixels() {

        targetSlantRangeTimeToFirstPixel = subSwath[0].slrTimeToFirstPixel;
        targetSlantRangeTimeToLastPixel = subSwath[numOfSubSwath-1].slrTimeToLastPixel;
        targetDeltaSlantRangeTime = subSwath[0].rangePixelSpacing/Constants.lightSpeed;
    }

    private void computeTargetWidthAndHeight() {

        targetHeight = (int)Math.round((targetLastLineTime - targetFirstLineTime)/subSwath[0].azimuthTimeInterval);

        targetWidth = (int)Math.round((targetSlantRangeTimeToLastPixel - targetSlantRangeTimeToFirstPixel)/
                targetDeltaSlantRangeTime);
    }

    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(), "deburst", targetWidth, targetHeight);

        final Band[] sourceBands = sourceProduct.getBands();

        for (String pol:polarizations) {

            final Band trgI = targetProduct.addBand("i_" + pol, sourceBands[0].getDataType());
            trgI.setUnit(Unit.REAL);
            trgI.setNoDataValueUsed(true);
            trgI.setNoDataValue(nodatavalue);

            final Band trgQ = targetProduct.addBand("q_" + pol, sourceBands[0].getDataType());
            trgQ.setUnit(Unit.IMAGINARY);
            trgQ.setNoDataValueUsed(true);
            trgQ.setNoDataValue(nodatavalue);

            ReaderUtils.createVirtualIntensityBand(targetProduct, trgI, trgQ, '_' + pol);
            ReaderUtils.createVirtualPhaseBand(targetProduct, trgI, trgQ, '_' + pol);
        }

        copyMetaData(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        targetProduct.setStartTime(new ProductData.UTC(targetFirstLineTime));
        targetProduct.setEndTime(new ProductData.UTC(targetLastLineTime));
        targetProduct.setDescription(sourceProduct.getDescription());

        createTiePointGrids();
    }

    private static void copyMetaData(final MetadataElement source, final MetadataElement target) {

        for (final MetadataElement element : source.getElements()) {
            target.addElement(element.createDeepClone());
        }

        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
    }

    private void createTiePointGrids() {
        // todo
    }

    private void updateTargetProductMetadata() {
        // todo
    }


    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles The current tiles to be computed for each target band.
     * @param pm          A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final double tileFirstLineTime = targetFirstLineTime + ty0*targetLineTimeInterval;
            final double tileLastLineTime = targetFirstLineTime + (ty0 + th - 1)*targetLineTimeInterval;
            final double tileSlrtToFirstPixel = targetSlantRangeTimeToFirstPixel + tx0*targetDeltaSlantRangeTime;
            final double tileSlrtToLastPixel = targetSlantRangeTimeToFirstPixel + (tx0+tw-1)*targetDeltaSlantRangeTime;
            // System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            // determine subswaths covered by the tile
            int firstSubSwathIndex = 0;
            for (int i = 0; i < numOfSubSwath; i++) {
                if (tileSlrtToFirstPixel >= subSwath[i].slrTimeToFirstPixel &&
                    tileSlrtToFirstPixel <= subSwath[i].slrTimeToLastPixel) {
                    firstSubSwathIndex = i + 1;
                    break;
                }
            }

            int lastSubSwathIndex = 0;
            for (int i = 0; i < numOfSubSwath; i++) {
                if (tileSlrtToLastPixel >= subSwath[i].slrTimeToFirstPixel &&
                    tileSlrtToLastPixel <= subSwath[i].slrTimeToLastPixel) {
                    lastSubSwathIndex = i + 1;
                }
            }

            final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
            boolean tileInOneSubSwath = (numOfSourceTiles == 1);

            Rectangle[] sourceRectangle = new Rectangle[numOfSourceTiles];
            int k = 0;
            for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                 sourceRectangle[k++] = getSourceRectangle(tx0, ty0, tw, th, i);
            }

            for (String pol:polarizations) {

                final Band tgtBandI = targetProduct.getBand("i_" + pol);
                final Band tgtBandQ = targetProduct.getBand("q_" + pol);
                final Tile targetTileI = targetTiles.get(tgtBandI);
                final Tile targetTileQ = targetTiles.get(tgtBandQ);
                final ProductData dataI = targetTileI.getDataBuffer();
                final ProductData dataQ = targetTileQ.getDataBuffer();
                final TileIndex tgtIndex = new TileIndex(targetTileI);

                if (tileInOneSubSwath) {

                    final Band srcBandI = sourceProduct.getBand("i_" + acquisitionMode + firstSubSwathIndex + "_" + pol);
                    final Band srcBandQ = sourceProduct.getBand("q_" + acquisitionMode + firstSubSwathIndex + "_" + pol);
                    final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[0]);
                    final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[0]);
                    final ProductData srcDataI = sourceRasterI.getDataBuffer();
                    final ProductData srcDataQ = sourceRasterQ.getDataBuffer();
                    final TileIndex srcTileIndex = new TileIndex(sourceRasterI);

                    final int yMin = computeYMin(subSwath[firstSubSwathIndex - 1]);
                    final int yMax = computeYMax(subSwath[firstSubSwathIndex-1]);
                    int[] srcPixelIndices = new int[2];
                    for (int y = Math.max(ty0, yMin); y < Math.min(ty0 + th, yMax + 1); y++) {
                        tgtIndex.calculateStride(y);
                        for (int x = tx0; x < tx0 + tw; x++) {
                            final int tIdx = tgtIndex.getIndex(x);
                            getSourceIndex(x, y, firstSubSwathIndex, srcTileIndex, srcPixelIndices);
                            saveToTarget(srcPixelIndices, srcDataI, srcDataQ, tIdx, dataI, dataQ);
                        }
                    }

                    if (yMin > ty0) {
                        for (int y = ty0; y < yMin; y++) {
                            tgtIndex.calculateStride(y);
                            for (int x = tx0; x < tx0 + tw; x++) {
                                final int tIdx = tgtIndex.getIndex(x);
                                dataI.setElemDoubleAt(tIdx, nodatavalue);
                                dataQ.setElemDoubleAt(tIdx, nodatavalue);
                            }
                        }
                    }

                    if (yMax + 1 < ty0 + th) {
                        for (int y = yMax + 1; y < ty0 + th; y++) {
                            tgtIndex.calculateStride(y);
                            for (int x = tx0; x < tx0 + tw; x++) {
                                final int tIdx = tgtIndex.getIndex(x);
                                dataI.setElemDoubleAt(tIdx, nodatavalue);
                                dataQ.setElemDoubleAt(tIdx, nodatavalue);
                            }
                        }
                    }

                } else {

                    Band[] srcBandI = new Band[numOfSourceTiles];
                    Band[] srcBandQ = new Band[numOfSourceTiles];
                    Tile[] sourceRasterI = new Tile[numOfSourceTiles];
                    Tile[] sourceRasterQ = new Tile[numOfSourceTiles];
                    ProductData[] srcDataI = new ProductData[numOfSourceTiles];
                    ProductData[] srcDataQ = new ProductData[numOfSourceTiles];
                    TileIndex[] srcTileIndex = new TileIndex[numOfSourceTiles];

                    k = 0;
                    for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                        srcBandI[k] = sourceProduct.getBand("i_" + acquisitionMode + i + "_" + pol);
                        srcBandQ[k] = sourceProduct.getBand("q_" + acquisitionMode + i + "_" + pol);
                        sourceRasterI[k] = getSourceTile(srcBandI[k], sourceRectangle[k]);
                        sourceRasterQ[k] = getSourceTile(srcBandQ[k], sourceRectangle[k]);
                        srcDataI[k] = sourceRasterI[k].getDataBuffer();
                        srcDataQ[k] = sourceRasterQ[k].getDataBuffer();
                        srcTileIndex[k] = new TileIndex(sourceRasterI[k]);
                        k++;
                    }

                    int[] srcPixelIndices = new int[2];
                    for (int y = ty0; y < ty0 + th; y++) {
                        tgtIndex.calculateStride(y);
                        for (int x = tx0; x < tx0 + tw; x++) {
                            final int tIdx = tgtIndex.getIndex(x);
                            final int subswathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex, pol);
                            if (subswathIndex == -1) {
                                dataI.setElemDoubleAt(tIdx, nodatavalue);
                                dataQ.setElemDoubleAt(tIdx, nodatavalue);
                                continue;
                            }
                            k = subswathIndex - firstSubSwathIndex;
                            getSourceIndex(x, y, subswathIndex, srcTileIndex[k], srcPixelIndices);
                            saveToTarget(srcPixelIndices, srcDataI[k], srcDataQ[k], tIdx, dataI, dataQ);
                        }
                    }

                }
            }
        } catch(Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void getSourceIndex(final int tx, final int ty, final int subSwathIndex, final TileIndex srcTileIndex,
                               int[] srcPixelIndices) {

        final int sx = getSampleIndexInSourceProduct(tx, subSwath[subSwathIndex-1]);
        int[] sy = new int[2];
        getLineIndicesInSourceProduct(ty, subSwath[subSwathIndex-1], sy);
        for (int i = 0; i < 2; i++) {
            if (sy[i] != -1) {
                srcTileIndex.calculateStride(sy[i]);
                srcPixelIndices[i] = srcTileIndex.getIndex(sx);
            } else {
                srcPixelIndices[i] = -1;
            }
        }
    }

    /**
     * Get source tile rectangle.
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw The target tile width.
     * @param th The target tile height.
     * @param subSwathIndex The subswath index.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceRectangle(
            final int tx0, final int ty0, final int tw, final int th, final int subSwathIndex) {

        final SubSwathInfo sw = subSwath[subSwathIndex-1];
        final int x0 = getSampleIndexInSourceProduct(tx0, sw);
        final int xMax = getSampleIndexInSourceProduct(tx0 + tw - 1, sw);

        int[] sy = new int[2];
        getLineIndicesInSourceProduct(ty0, sw, sy);
        int y0;
        if (sy[0] == -1 && sy[1] == -1) {
            y0 = 0;
        } else {
            y0 = sy[0];
        }

        getLineIndicesInSourceProduct(ty0 + th - 1, sw, sy);
        int yMax;
        if (sy[0] == -1 && sy[1] == -1) {
            yMax = sw.numOfLines - 1;
        } else {
            yMax = Math.max(sy[0], sy[1]);
        }

        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;

        return new Rectangle(x0, y0, w, h);
    }

    private int getSampleIndexInSourceProduct(final int tx, final SubSwathInfo subSwath) {
        final double targetSampleSlrTime = targetSlantRangeTimeToFirstPixel + tx*targetDeltaSlantRangeTime;
        int sx = (int)Math.round((targetSampleSlrTime - subSwath.slrTimeToFirstPixel)/targetDeltaSlantRangeTime);
        if (sx < 0) {
            return 0;
        } else if (sx > subSwath.numOfSamples - 1) {
            return subSwath.numOfSamples - 1;
        } else {
            return sx;
        }
    }

    private void getLineIndicesInSourceProduct(final int ty, final SubSwathInfo subSwath, int[] sy) {

        final double targetLineTime = targetFirstLineTime + ty*targetLineTimeInterval;
        sy[0] = -1;
        sy[1] = -1;
        int k = 0;
        for (int i = 0; i < subSwath.numOfBursts; i++) {
            if (targetLineTime >= subSwath.burstFirstLineTime[i] && targetLineTime < subSwath.burstLastLineTime[i]) {
                sy[k++] = i*subSwath.linesPerBurst +
                        (int)Math.round((targetLineTime - subSwath.burstFirstLineTime[i])/subSwath.azimuthTimeInterval);
                if (k == 2) {
                    break;
                }
            }
        }
    }

    private int computeYMin(final SubSwathInfo subSwath) {

        return (int)((subSwath.firstLineTime - targetFirstLineTime)/targetLineTimeInterval);
    }

    private int computeYMax(final SubSwathInfo subSwath) {

        return (int)((subSwath.lastLineTime - targetFirstLineTime)/targetLineTimeInterval);
    }

    private int getSubSwathIndex(
            final int tx, final int ty, final int firstSubSwathIndex, final int lastSubSwathIndex, final String pol) {

        final double targetSampleSlrTime = targetSlantRangeTimeToFirstPixel + tx*targetDeltaSlantRangeTime;
        final double targetLineTime = targetFirstLineTime + ty*targetLineTimeInterval;

        int subSwathIndex = -1;
        double noiseMin = Double.MAX_VALUE;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            if (targetLineTime >= subSwath[i - 1].firstLineTime &&
                targetLineTime <= subSwath[i - 1].lastLineTime &&
                targetSampleSlrTime >= subSwath[i - 1].slrTimeToFirstPixel &&
                targetSampleSlrTime <= subSwath[i - 1].slrTimeToLastPixel) {

                final double noise = getSubSwathNoise(tx, ty, subSwath[i - 1], pol);
                if (noise < noiseMin) {
                    noiseMin = noise;
                    subSwathIndex = i;
                }
            }
        }

        return subSwathIndex;
    }

    private void saveToTarget(final int [] srcPixelIndices, final ProductData srcDataI, final ProductData srcDataQ,
                              final int tIdx, final ProductData dataI, final ProductData dataQ) {

        if (srcPixelIndices[0] == -1 && srcPixelIndices[1] == -1) {

            dataI.setElemDoubleAt(tIdx, nodatavalue);

            dataQ.setElemDoubleAt(tIdx, nodatavalue);

        } else if (srcPixelIndices[0] != -1 && srcPixelIndices[1] == -1) {

            dataI.setElemDoubleAt(tIdx, srcDataI.getElemDoubleAt(srcPixelIndices[0]));

            dataQ.setElemDoubleAt(tIdx, srcDataQ.getElemDoubleAt(srcPixelIndices[0]));

        } else {

            dataI.setElemDoubleAt(tIdx, 0.5*(srcDataI.getElemDoubleAt(srcPixelIndices[0]) +
                                             srcDataI.getElemDoubleAt(srcPixelIndices[1])));

            dataQ.setElemDoubleAt(tIdx, 0.5*(srcDataQ.getElemDoubleAt(srcPixelIndices[0]) +
                                             srcDataQ.getElemDoubleAt(srcPixelIndices[1])));
        }
    }

    private double getSubSwathNoise(final int tx, final int ty, final SubSwathInfo sw, final String pol) {

        Sentinel1Utils.NoiseVector[] vectorList = sw.noise.get(pol);

        final int sx = getSampleIndexInSourceProduct(tx, sw);
        final double targetLineTime = targetFirstLineTime + ty*targetLineTimeInterval;
        final int sy = (int)((targetLineTime - vectorList[0].time.getMJD()) / targetLineTimeInterval);

        int l0 = -1, l1 = -1;
        int vectorIdx0 = -1, vectorIdx1 = -1;
        if (sy < vectorList[0].line) {

            l0 = vectorList[0].line;
            l1 = l0;
            vectorIdx0 = 0;
            vectorIdx1 = vectorIdx0;

        } else if (sy >= vectorList[vectorList.length - 1].line) {

            l0 = vectorList[vectorList.length - 1].line;
            l1 = l0;
            vectorIdx0 = vectorList.length - 1;
            vectorIdx1 = vectorIdx0;

        } else {

            for (int i = 0; i < vectorList.length - 1; i++) {
                if (sy >= vectorList[i].line && sy < vectorList[i+1].line) {
                    l0 = vectorList[i].line;
                    l1 = vectorList[i+1].line;
                    vectorIdx0 = i;
                    vectorIdx1 = i + 1;
                    break;
                }
            }
        }

        final int[] pixels = vectorList[vectorIdx0].pixels;
        int p0 = -1, p1 = -1;
        int pixelIdx0 = -1, pixelIdx1 = -1;
        if (sx < pixels[0]) {

            p0 = pixels[0];
            p1 = p0;
            pixelIdx0 = 0;
            pixelIdx1 = pixelIdx0;

        } else if (sx >= pixels[pixels.length - 1]) {

            p0 = pixels[pixels.length - 1];
            p1 = p0;
            pixelIdx0 = pixels.length - 1;
            pixelIdx1 = pixelIdx0;

        } else {

            for (int i = 0; i < pixels.length - 1; i++) {
                if (sx >= pixels[i] && sx < pixels[i+1]) {
                    p0 = pixels[i];
                    p1 = pixels[i+1];
                    pixelIdx0 = i;
                    pixelIdx1 = i + 1;
                    break;
                }
            }
        }

        final float[] noiseLUT0 = vectorList[vectorIdx0].noiseLUT;
        final float[] noiseLUT1 = vectorList[vectorIdx1].noiseLUT;
        final double n00 = noiseLUT0[pixelIdx0];
        final double n01 = noiseLUT0[pixelIdx1];
        final double n10 = noiseLUT1[pixelIdx0];
        final double n11 = noiseLUT1[pixelIdx1];
        double dx;
        if (p0 == p1) {
            dx = 0;
        } else {
            dx = (sx - p0)/(p1 - p0);
        }

        double dy;
        if (l0 == l1) {
            dy = 0;
        } else {
            dy = (sy - l0)/(l1 - l0);
        }

        return MathUtils.interpolationBiLinear(n00, n01, n10, n11, dx, dy);
    }


    private static class SubSwathInfo {

        // subswath info
        public String subSwathName;
        public int numOfLines;
        public int numOfSamples;
        public double firstLineTime;
        public double lastLineTime;
        public double slrTimeToFirstPixel;
        public double slrTimeToLastPixel;
        public double azimuthTimeInterval;
        public double rangePixelSpacing;

        // bursts info
        public int numOfBursts;
        public int linesPerBurst;
        public int samplesPerBurst;
        public double[] burstFirstLineTime;
        public double[] burstLastLineTime;
        public int[][] firstValidSample;
        public int[][] lastValidSample;
        public Map<String, Sentinel1Utils.NoiseVector[]> noise = new HashMap<String, Sentinel1Utils.NoiseVector[]>();
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
            super(Sentinel1DeburstTOPSAROp.class);
        }
    }
}