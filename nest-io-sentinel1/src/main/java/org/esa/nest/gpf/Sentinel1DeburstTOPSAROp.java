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
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;
import org.esa.nest.util.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * De-Burst a Sentinel-1 TOPSAR product
 */
@OperatorMetadata(alias = "DeburstTOPSAR",
                  category = "SAR Tools",
                  authors = "NEST team", copyright = "(C) 2013 by Array Systems Computing Inc.",
                  description="Debursts a Sentinel-1 TOPSAR product")
public final class Sentinel1DeburstTOPSAROp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false", label="Azimuth De-burst only")
    private boolean deburstOnly = false;

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

        switch (acquisitionMode) {
            case "IW":
                numOfSubSwath = 3;
                break;
            case "EW":
                numOfSubSwath = 5;
                break;
            default:
                throw new OperatorException("Acquisition mode is not IW or EW");
        }
    }

    private void getProductPolarizations() {

        final MetadataElement[] elems = absRoot.getElements();
        final String subSwathName = acquisitionMode + '1';
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

        final MetadataElement Original_Product_Metadata = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

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

    private static void setParameters(final MetadataElement subSwathMetadata, SubSwathInfo subSwath) {

        final MetadataElement product = subSwathMetadata.getElement("product");
        final MetadataElement imageAnnotation = product.getElement("imageAnnotation");
        final MetadataElement imageInformation = imageAnnotation.getElement("imageInformation");
        final MetadataElement swathTiming = product.getElement("swathTiming");
        final MetadataElement burstList = swathTiming.getElement("burstList");
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
            final MetadataElement firstValidSampleElem = listElem.getElement("firstValidSample");
            final MetadataElement lastValidSampleElem = listElem.getElement("lastValidSample");
            subSwath.firstValidSample[k] = Sentinel1Utils.getIntArray(firstValidSampleElem, "firstValidSample");
            subSwath.lastValidSample[k] = Sentinel1Utils.getIntArray(lastValidSampleElem, "lastValidSample");
            k++;
        }
    }

    private void getSubSwathNoiseVectors() {

        for (int i = 0; i < numOfSubSwath; i++) {
            for (String pol:polarizations) {
                final Band srcBand = getSourceBand(subSwath[i].subSwathName, pol);
                final Sentinel1Utils.NoiseVector[] noiseVectors = Sentinel1Utils.getNoiseVector(srcBand);
                subSwath[i].noise.put(pol, noiseVectors);
            }
        }

    }

    private Band getSourceBand(final String subSwathName, final String polarization) {

        final Band[] sourceBands = sourceProduct.getBands();
        for (Band band:sourceBands) {
            if (band.getName().contains(subSwathName + '_' + polarization)) {
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
            final boolean tileInOneSubSwath = (numOfSourceTiles == 1);

            final Rectangle[] sourceRectangle = new Rectangle[numOfSourceTiles];
            int k = 0;
            for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                 sourceRectangle[k++] = getSourceRectangle(tx0, ty0, tw, th, i);
            }

            final BurstInfo burstInfo = new BurstInfo();
            final int lastX = tx0 + tw;
            final String bandNameI = "i_" + acquisitionMode;
            final String bandNameQ = "q_" + acquisitionMode;

            for (String pol:polarizations) {

                final Band tgtBandI = targetProduct.getBand("i_" + pol);
                final Band tgtBandQ = targetProduct.getBand("q_" + pol);
                final Tile targetTileI = targetTiles.get(tgtBandI);
                final Tile targetTileQ = targetTiles.get(tgtBandQ);
                final ProductData dataI = targetTileI.getDataBuffer();
                final ProductData dataQ = targetTileQ.getDataBuffer();
                final TileIndex tgtIndex = new TileIndex(targetTileI);

                if (tileInOneSubSwath) {
                    final int yMin = computeYMin(subSwath[firstSubSwathIndex - 1]);
                    final int yMax = computeYMax(subSwath[firstSubSwathIndex-1]);
                    int firstY = Math.max(ty0, yMin);
                    int lastY = Math.min(ty0 + th, yMax + 1);

                    if(firstY >= lastY)
                        continue;

                    final Band srcBandI = sourceProduct.getBand(bandNameI + firstSubSwathIndex + '_' + pol);
                    final Band srcBandQ = sourceProduct.getBand(bandNameQ + firstSubSwathIndex + '_' + pol);
                    final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[0]);
                    final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[0]);
                    final ProductData srcDataI = sourceRasterI.getDataBuffer();
                    final ProductData srcDataQ = sourceRasterQ.getDataBuffer();
                    final TileIndex srcTileIndex = new TileIndex(sourceRasterI);

                    for (int y = firstY; y < lastY; y++) {
                        tgtIndex.calculateStride(y);
                        final boolean valid = getLineIndicesInSourceProduct(y, subSwath[firstSubSwathIndex-1], burstInfo);
                        if(!valid) {
                            continue;
                        }
                        for (int x = tx0; x < lastX; x++) {
                            setPixel(x, firstSubSwathIndex, srcTileIndex, burstInfo,
                                    srcDataI, srcDataQ, tgtIndex.getIndex(x), dataI, dataQ);
                        }
                    }

                } else {

                    final ProductData[] srcDataI = new ProductData[numOfSourceTiles];
                    final ProductData[] srcDataQ = new ProductData[numOfSourceTiles];
                    final TileIndex[] srcTileIndex = new TileIndex[numOfSourceTiles];

                    k = 0;
                    for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                        final Band srcBandI = sourceProduct.getBand(bandNameI + i + '_' + pol);
                        final Band srcBandQ = sourceProduct.getBand(bandNameQ + i + '_' + pol);
                        final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[k]);
                        final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[k]);
                        srcDataI[k] = sourceRasterI.getDataBuffer();
                        srcDataQ[k] = sourceRasterQ.getDataBuffer();
                        srcTileIndex[k] = new TileIndex(sourceRasterI);
                        k++;
                    }

                    for (int y = ty0; y < ty0 + th; y++) {
                        tgtIndex.calculateStride(y);
                        for (int x = tx0; x < lastX; x++) {
                            int subswathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex, pol, burstInfo);
                            if (subswathIndex == -1) {
                                continue;
                            }
                            final boolean valid = getLineIndicesInSourceProduct(y, subSwath[subswathIndex-1], burstInfo);
                            if(!valid) {
                                continue;
                            }

                            k = subswathIndex - firstSubSwathIndex;
                            boolean validSwath = setPixel(x, subswathIndex, srcTileIndex[k], burstInfo,
                                    srcDataI[k], srcDataQ[k], tgtIndex.getIndex(x), dataI, dataQ);
                            if(!validSwath && burstInfo.swath1 != -1) {
                                // try again with the other swath
                                if(subswathIndex == burstInfo.swath0)
                                    subswathIndex = burstInfo.swath1;
                                else
                                    subswathIndex = burstInfo.swath0;
                                getLineIndicesInSourceProduct(y, subSwath[subswathIndex-1], burstInfo);

                                k = subswathIndex - firstSubSwathIndex;
                                setPixel(x, subswathIndex, srcTileIndex[k], burstInfo,
                                        srcDataI[k], srcDataQ[k], tgtIndex.getIndex(x), dataI, dataQ);
                            }
                        }
                    }

                }
            }
        } catch(Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private boolean setPixel(final int tx, final int subSwathIndex, final TileIndex srcTileIndex,
                          final BurstInfo burstInfo,
                          final ProductData srcDataI, final ProductData srcDataQ,
                          final int tIdx, final ProductData dataI, final ProductData dataQ) {

        int sx = -1;
        int srcIndex0 = -1;
        int srcIndex1 = -1;
        if (burstInfo.sy0 != -1) {
            srcTileIndex.calculateStride(burstInfo.sy0);
            sx = getSampleIndexInSourceProduct(tx, subSwath[subSwathIndex-1]);
            srcIndex0 = srcTileIndex.getIndex(sx);
        }
        if (burstInfo.sy1 != -1) {
            srcTileIndex.calculateStride(burstInfo.sy1);
            if(sx == -1)
                sx = getSampleIndexInSourceProduct(tx, subSwath[subSwathIndex-1]);
            srcIndex1 = srcTileIndex.getIndex(sx);
        }

        int idx;
        if (srcIndex1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
            idx = srcIndex1;
        } else {
            idx = srcIndex0;
        }

        double iVal = srcDataI.getElemDoubleAt(idx);
        double qVal = srcDataQ.getElemDoubleAt(idx);
        if(iVal == 0 && qVal == 0) {
            // edge of swaths found therefore use other swath
            return false;
        }
        dataI.setElemDoubleAt(tIdx, iVal);
        dataQ.setElemDoubleAt(tIdx, qVal);
        return true;
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

        final BurstInfo burstTimes = new BurstInfo();
        getLineIndicesInSourceProduct(ty0, sw, burstTimes);
        int y0;
        if (burstTimes.sy0 == -1 && burstTimes.sy1 == -1) {
            y0 = 0;
        } else {
            y0 = burstTimes.sy0;
        }

        getLineIndicesInSourceProduct(ty0 + th - 1, sw, burstTimes);
        int yMax;
        if (burstTimes.sy0 == -1 && burstTimes.sy1 == -1) {
            yMax = sw.numOfLines - 1;
        } else {
            yMax = Math.max(burstTimes.sy0, burstTimes.sy1);
        }

        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;

        return new Rectangle(x0, y0, w, h);
    }

    private int getSampleIndexInSourceProduct(final int tx, final SubSwathInfo subSwath) {
        final double targetSampleSlrTime = targetSlantRangeTimeToFirstPixel + tx*targetDeltaSlantRangeTime;
        final int sx = (int)Math.round((targetSampleSlrTime - subSwath.slrTimeToFirstPixel)/targetDeltaSlantRangeTime);
        return sx < 0 ? 0 : sx > subSwath.numOfSamples - 1 ? subSwath.numOfSamples - 1 : sx;
    }

    private boolean getLineIndicesInSourceProduct(final int ty, final SubSwathInfo subSwath, final BurstInfo burstTimes) {

        final double targetLineTime = targetFirstLineTime + ty*targetLineTimeInterval;
        burstTimes.targetTime = targetLineTime;
        burstTimes.sy0 = -1;
        burstTimes.sy1 = -1;
        int k = 0;
        for (int i = 0; i < subSwath.numOfBursts; i++) {
            if (targetLineTime >= subSwath.burstFirstLineTime[i] && targetLineTime < subSwath.burstLastLineTime[i]) {
                final int sy = i*subSwath.linesPerBurst +
                        (int)Math.round((targetLineTime - subSwath.burstFirstLineTime[i])/subSwath.azimuthTimeInterval);
                if(k==0) {
                    burstTimes.sy0 = sy;
                    burstTimes.burstNum0 = i;
                } else {
                    burstTimes.sy1 = sy;
                    burstTimes.burstNum1 = i;
                }
                ++k;
                if (k == 2) {
                    break;
                }
            }
        }
        if(burstTimes.sy0 != -1 && burstTimes.sy1 != -1) {
            // find time between bursts midTime
            // use first burst if targetLineTime is before midTime
            burstTimes.midTime = (subSwath.burstLastLineTime[burstTimes.burstNum0] +
                    subSwath.burstFirstLineTime[burstTimes.burstNum1])/2.0;
        }
        return burstTimes.sy0 != -1 || burstTimes.sy1 != -1;
    }

    private int computeYMin(final SubSwathInfo subSwath) {

        return (int)((subSwath.firstLineTime - targetFirstLineTime)/targetLineTimeInterval);
    }

    private int computeYMax(final SubSwathInfo subSwath) {

        return (int)((subSwath.lastLineTime - targetFirstLineTime)/targetLineTimeInterval);
    }

    private int getSubSwathIndex(final int tx, final int ty, final int firstSubSwathIndex, final int lastSubSwathIndex,
                                 final String pol, final BurstInfo burstInfo) {

        final double targetSampleSlrTime = targetSlantRangeTimeToFirstPixel + tx*targetDeltaSlantRangeTime;
        final double targetLineTime = targetFirstLineTime + ty*targetLineTimeInterval;

        burstInfo.swath0 = -1;
        burstInfo.swath1 = -1;
        int cnt = 0;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            if (targetLineTime >= subSwath[i - 1].firstLineTime &&
                targetLineTime <= subSwath[i - 1].lastLineTime &&
                targetSampleSlrTime >= subSwath[i - 1].slrTimeToFirstPixel &&
                targetSampleSlrTime <= subSwath[i - 1].slrTimeToLastPixel) {

                if(cnt==0) {
                    burstInfo.swath0 = i;
                } else {
                    burstInfo.swath1 = i;
                    break;
                }
                ++cnt;
            }
        }

        if(burstInfo.swath1 != -1) {
            final double noise0 = getSubSwathNoise(tx, ty, targetLineTime, subSwath[burstInfo.swath0 - 1], pol);
            final double noise1 = getSubSwathNoise(tx, ty, targetLineTime, subSwath[burstInfo.swath1 - 1], pol);
            if (noise0 > noise1) {
                return burstInfo.swath1;
            }
        }
        return burstInfo.swath0;
    }

    private double getSubSwathNoise(final int tx, final int ty, final double targetLineTime,
                                    final SubSwathInfo sw, final String pol) {

        final Sentinel1Utils.NoiseVector[] vectorList = sw.noise.get(pol);

        final int sx = getSampleIndexInSourceProduct(tx, sw);
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

    private static class BurstInfo {
        public int sy0 = -1;
        public int sy1 = -1;
        public int swath0;
        public int swath1;
        public int burstNum0 = 0;
        public int burstNum1 = 0;

        public double targetTime;
        public double midTime;

        public BurstInfo(){
        }
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