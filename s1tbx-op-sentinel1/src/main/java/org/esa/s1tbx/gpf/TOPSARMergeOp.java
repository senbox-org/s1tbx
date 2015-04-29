/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.TiePointGeoCoding;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.dataop.maptransf.Datum;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProducts;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.Maths;
import org.esa.snap.util.ProductUtils;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;

/**
 * Merge subswaths of a Sentinel-1 TOPSAR product.
 */
@OperatorMetadata(alias = "TOPSAR-Merge",
        category = "SAR Processing/Sentinel-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Merge subswaths of a Sentinel-1 TOPSAR product")
public final class TOPSARMergeOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    private String acquisitionMode = null;
    private String productType = null;
    private int numOfSubSwath = 0;
    private int refSubSwathIndex = 0;
    private int targetWidth = 0;
    private int targetHeight = 0;

    private double targetFirstLineTime = 0;
    private double targetLastLineTime = 0;
    private double targetLineTimeInterval = 0;
    private double targetSlantRangeTimeToFirstPixel = 0;
    private double targetSlantRangeTimeToLastPixel = 0;
    private double targetDeltaSlantRangeTime = 0;
    private SubSwathEffectStartEndPixels[] subSwathEffectStartEndPixels = null;

    private Sentinel1Utils[] su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private BiMap<Integer, Integer> sourceProductIndexToSubSwathIndexMap = HashBiMap.create();

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public TOPSARMergeOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.framework.datamodel.Product} annotated with the
     * {@link org.esa.snap.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (sourceProduct == null) {
                return;
            }

            checkSourceProductValidity();

            getSubSwathParameters();

            computeTargetStartEndTime();

            computeTargetSlantRangeTimeToFirstAndLastPixels();

            computeTargetWidthAndHeight();

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Check source product validity.
     */
    private void checkSourceProductValidity() {

        if (sourceProduct.length < 2) {
            throw new OperatorException("Please select split sub-swaths of the same Sentinel-1 products");
        }

        // check if all sub-swaths are from the same s-1 product
        MetadataElement absRoot0 = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);
        final String mission = absRoot0.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            throw new OperatorException("Source product should be Sentinel-1 product");
        }

        numOfSubSwath = sourceProduct.length;
        final int numOfBands0 = sourceProduct[0].getNumBands();
        final int[] subSwathIndexArray = new int[numOfSubSwath];
        final String product0 = absRoot0.getAttributeString(AbstractMetadata.PRODUCT);
        acquisitionMode = absRoot0.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        productType = absRoot0.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        final String subSwathName0 = absRoot0.getAttributeString(AbstractMetadata.swath);
        if (subSwathName0.equals("")) {
            throw new OperatorException("Cannot get \"swath\" information from source product abstracted metadata");
        }
        subSwathIndexArray[0] = getSubSwathIndex(subSwathName0);
        sourceProductIndexToSubSwathIndexMap.put(0, subSwathIndexArray[0]);

        for (int p = 1; p < numOfSubSwath; p++) {
            MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[p]);
            final String product = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
            if (!product.equals(product0)) {
                throw new OperatorException("Source products are not from the same Sentinel-1 product");
            }

            final String acMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
            if (!acMode.equals(acquisitionMode)) {
                throw new OperatorException("Source products do not have the same acquisition mode");
            }

            final int numOfBands = sourceProduct[p].getNumBands();
            if (numOfBands != numOfBands0) {
                throw new OperatorException("Source products do not have the same number of bands");
            }

            final String subSwathName = absRoot.getAttributeString(AbstractMetadata.swath);
            if (subSwathName.equals("")) {
                throw new OperatorException("Cannot get \"swath\" information from source product abstracted metadata");
            }
            subSwathIndexArray[p] = getSubSwathIndex(subSwathName);
            sourceProductIndexToSubSwathIndexMap.put(p, subSwathIndexArray[p]);
        }

        Arrays.sort(subSwathIndexArray);
        refSubSwathIndex = subSwathIndexArray[0];
        for (int s = 0; s < numOfSubSwath - 1; s++) {
            if (subSwathIndexArray[s+1] - subSwathIndexArray[s] != 1) {
                throw new OperatorException("Isolate sub-swath detected in source products");
            }
        }
    }

    private int getSubSwathIndex(final String subswath) {
        final String idxStr = subswath.substring(2);
        return Integer.parseInt(idxStr);
    }

    private void getSubSwathParameters() {

        try {
            su = new Sentinel1Utils[numOfSubSwath];
            subSwath = new Sentinel1Utils.SubSwathInfo[numOfSubSwath];
            for (int p = 0; p < numOfSubSwath; p++) {
                final int s = sourceProductIndexToSubSwathIndexMap.get(p) - refSubSwathIndex;
                su[s] = new Sentinel1Utils(sourceProduct[p]);
                subSwath[s] = su[s].getSubSwath()[0];
                if (selectedPolarisations == null || selectedPolarisations.length == 0) {
                    selectedPolarisations = su[s].getPolarizations();
                }

                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[p]);
                subSwath[s].firstValidPixel = AbstractMetadata.getAttributeInt(absRoot, "firstValidPixel");
                subSwath[s].lastValidPixel = AbstractMetadata.getAttributeInt(absRoot, "lastValidPixel");
                subSwath[s].slrTimeToFirstValidPixel = AbstractMetadata.getAttributeDouble(absRoot, "slrTimeToFirstValidPixel");
                subSwath[s].slrTimeToLastValidPixel = AbstractMetadata.getAttributeDouble(absRoot, "slrTimeToLastValidPixel");
                subSwath[s].firstValidLineTime = AbstractMetadata.getAttributeDouble(absRoot, "firstValidLineTime");
                subSwath[s].lastValidLineTime = AbstractMetadata.getAttributeDouble(absRoot, "lastValidLineTime");
            }
        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    /**
     * Compute azimuth time for the first and last line in the target product.
     */
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
        targetLineTimeInterval = subSwath[0].azimuthTimeInterval;
    }

    /**
     * Compute slant range time to the first and last pixels in the target product.
     */
    private void computeTargetSlantRangeTimeToFirstAndLastPixels() {

        targetSlantRangeTimeToFirstPixel = subSwath[0].slrTimeToFirstValidPixel;
        targetSlantRangeTimeToLastPixel = subSwath[numOfSubSwath - 1].slrTimeToLastValidPixel;
        targetDeltaSlantRangeTime = subSwath[0].rangePixelSpacing / Constants.lightSpeed;
    }

    /**
     * Compute target product dimension.
     */
    private void computeTargetWidthAndHeight() {

        targetHeight = (int)((targetLastLineTime - targetFirstLineTime) / targetLineTimeInterval);

        targetWidth = (int)((targetSlantRangeTimeToLastPixel - targetSlantRangeTimeToFirstPixel) /
                targetDeltaSlantRangeTime);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        final int prodIdx = sourceProductIndexToSubSwathIndexMap.inverse().get(refSubSwathIndex);

        targetProduct = new Product(sourceProduct[prodIdx].getName(), productType, targetWidth, targetHeight);

        final Band[] sourceBands = sourceProduct[prodIdx].getBands();

        // source band name is assumed in format: name_acquisitionModeAndSubSwathIndex_polarization_prefix
        // target band name is then in format: name_polarization_prefix
        boolean hasVirtualPhaseBand = false;
        for (Band srcBand:sourceBands) {
            final String srcBandName = srcBand.getName();
            if (!containSelectedPolarisations(srcBandName)) {
                continue;
            }

            if (srcBand instanceof VirtualBand) {
                if (srcBandName.toLowerCase().contains("phase")) {
                    hasVirtualPhaseBand = true;
                }
                continue;
            }

            final String tgtBandName = getTargetBandNameFromSourceBandName(srcBandName);
            if (!targetProduct.containsBand(tgtBandName)) {
                final Band trgBand = targetProduct.addBand(tgtBandName, srcBand.getDataType());
                trgBand.setUnit(srcBand.getUnit());
                trgBand.setNoDataValueUsed(true);
                trgBand.setNoDataValue(srcBand.getNoDataValue());
            }
        }

        final Band[] targetBands = targetProduct.getBands();
        for (int i = 0; i < targetBands.length; i++) {
            final Unit.UnitType iBandUnit = Unit.getUnitType(targetBands[i]);
            if (iBandUnit == Unit.UnitType.REAL && i+1 < targetBands.length) {
                final Unit.UnitType qBandUnit = Unit.getUnitType(targetBands[i+1]);
                if (qBandUnit == Unit.UnitType.IMAGINARY) {
                    ReaderUtils.createVirtualIntensityBand(
                            targetProduct, targetBands[i], targetBands[i+1], '_' + getPrefix(targetBands[i].getName()));

                    if (hasVirtualPhaseBand) {
                        ReaderUtils.createVirtualPhaseBand(targetProduct,
                                targetBands[i], targetBands[i+1], '_' + getPrefix(targetBands[i].getName()));
                    }
                    i++;
                }
            }
        }


        ProductUtils.copyMetadata(sourceProduct[prodIdx], targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct[prodIdx], targetProduct);
        targetProduct.setStartTime(new ProductData.UTC(targetFirstLineTime/Constants.secondsInDay));
        targetProduct.setEndTime(new ProductData.UTC(targetLastLineTime/Constants.secondsInDay));
        targetProduct.setDescription(sourceProduct[prodIdx].getDescription());

        createTiePointGrids();
    }

    private String getTargetBandNameFromSourceBandName(final String srcBandName) {

        if (!srcBandName.contains(acquisitionMode)) {
            return srcBandName;
        }

        final int firstSeparationIdx = srcBandName.indexOf(acquisitionMode);
        final int secondSeparationIdx = srcBandName.indexOf("_", firstSeparationIdx + 1);
        return srcBandName.substring(0, firstSeparationIdx) + srcBandName.substring(secondSeparationIdx + 1);
    }

    private void createTiePointGrids() {

        final int gridWidth = 20;
        final int gridHeight = 5;

        final int subSamplingX = targetWidth / gridWidth;
        final int subSamplingY = targetHeight / gridHeight;

        final float[] latList = new float[gridWidth * gridHeight];
        final float[] lonList = new float[gridWidth * gridHeight];
        final float[] slrtList = new float[gridWidth * gridHeight];
        final float[] incList = new float[gridWidth * gridHeight];

        int k = 0;
        for (int i = 0; i < gridHeight; i++) {
            final int y = i * subSamplingY;
            final double azTime = targetFirstLineTime + y * targetLineTimeInterval;
            for (int j = 0; j < gridWidth; j++) {
                final int x = j * subSamplingX;
                final double slrTime = targetSlantRangeTimeToFirstPixel + x * targetDeltaSlantRangeTime;
                final int s = getSubSwathIndex(slrTime);
                latList[k] = (float)su[s].getLatitude(azTime, slrTime);
                lonList[k] = (float)su[s].getLongitude(azTime, slrTime);
                slrtList[k] = (float)(su[s].getSlantRangeTime(azTime, slrTime) * 2 * Constants.oneBillion); // 2-way ns
                incList[k] = (float)su[s].getIncidenceAngle(azTime, slrTime);
                k++;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(
                OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, latList);

        final TiePointGrid lonGrid = new TiePointGrid(
                OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, lonList);

        final TiePointGrid slrtGrid = new TiePointGrid(
                OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, slrtList);

        final TiePointGrid incGrid = new TiePointGrid(
                OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, incList);

        latGrid.setUnit(Unit.DEGREES);
        lonGrid.setUnit(Unit.DEGREES);
        slrtGrid.setUnit(Unit.NANOSECONDS);
        incGrid.setUnit(Unit.DEGREES);

        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.addTiePointGrid(slrtGrid);
        targetProduct.addTiePointGrid(incGrid);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        targetProduct.setGeoCoding(tpGeoCoding);
    }

    private int getSubSwathIndex(final double slrTime) {

        double startTime, endTime;
        for (int i = 0; i < numOfSubSwath; i++) {

            if (i == 0) {
                startTime = subSwath[i].slrTimeToFirstValidPixel;
            } else {
                startTime = 0.5 * (subSwath[i].slrTimeToFirstValidPixel + subSwath[i - 1].slrTimeToLastPixel);
            }

            if (i == numOfSubSwath - 1) {
                endTime = subSwath[i].slrTimeToLastPixel;
            } else {
                endTime = 0.5 * (subSwath[i].slrTimeToLastPixel + subSwath[i + 1].slrTimeToFirstValidPixel);
            }

            if (slrTime >= startTime && slrTime < endTime) {
                return i;
            }
        }

        return 0;
    }

    private Band getSourceBandFromTargetBandName(
            final String tgtBandName, final String acquisitionMode, final String swathIndexStr) {

        for (int s = 0; s < numOfSubSwath; s++) {
            final String[] srcBandNames = sourceProduct[s].getBandNames();
            for (String srcBandName:srcBandNames) {
                if (srcBandName.contains(acquisitionMode + swathIndexStr) &&
                        getTargetBandNameFromSourceBandName(srcBandName).equals(tgtBandName)) {
                    return sourceProduct[s].getBand(srcBandName);
                }
            }
        }
        return null;
    }

    private static String getPrefix(final String tgtBandName) {

        final int firstSeparationIdx = tgtBandName.indexOf("_");
        return tgtBandName.substring(firstSeparationIdx+1);
    }

    private boolean containSelectedPolarisations(final String bandName) {
        for (String pol : selectedPolarisations) {
            if (bandName.contains(pol)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        updateAbstractMetadata();
        updateOriginalMetadata();
    }

    private void updateAbstractMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetWidth);
        absTgt.setAttributeUTC(
                AbstractMetadata.first_line_time, new ProductData.UTC(targetFirstLineTime/Constants.secondsInDay));
        absTgt.setAttributeUTC(
                AbstractMetadata.last_line_time, new ProductData.UTC(targetLastLineTime/Constants.secondsInDay));
        absTgt.setAttributeDouble(AbstractMetadata.line_time_interval, targetLineTimeInterval);

        TiePointGrid latGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
        TiePointGrid lonGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_lat, latGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_long, lonGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_lat, latGrid.getPixelFloat(targetWidth, 0));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_long, lonGrid.getPixelFloat(targetWidth, 0));

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_lat, latGrid.getPixelFloat(0, targetHeight));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_long, lonGrid.getPixelFloat(0, targetHeight));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_lat, latGrid.getPixelFloat(targetWidth, targetHeight));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_long, lonGrid.getPixelFloat(targetWidth, targetHeight));

        absTgt.removeAttribute(absTgt.getAttribute("firstValidPixel"));
        absTgt.removeAttribute(absTgt.getAttribute("lastValidPixel"));
        absTgt.removeAttribute(absTgt.getAttribute("slrTimeToFirstValidPixel"));
        absTgt.removeAttribute(absTgt.getAttribute("slrTimeToLastValidPixel"));
        absTgt.removeAttribute(absTgt.getAttribute("firstValidLineTime"));
        absTgt.removeAttribute(absTgt.getAttribute("lastValidLineTime"));
    }

    private void updateOriginalMetadata() {

        if (numOfSubSwath > 1) {
            //updateCalibrationVector();
            //updateNoiseVector(); //todo: to be implemented
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles The current tiles to be computed for each target band.
     * @param pm          A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.framework.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;

            // determine subswaths covered by the tile
            final double tileSlrtToFirstPixel = targetSlantRangeTimeToFirstPixel + tx0 * targetDeltaSlantRangeTime;
            final double tileSlrtToLastPixel = targetSlantRangeTimeToFirstPixel + (tx0 + tw - 1) * targetDeltaSlantRangeTime;
            final double tileFirstLineTime = targetFirstLineTime + ty0 * targetLineTimeInterval;
            final double tileLastLineTime = targetFirstLineTime + (ty0 + th - 1) * targetLineTimeInterval;

            int firstSubSwathIndex = -1;
            int lastSubSwathIndex = -1;
            for (int i = 0; i < numOfSubSwath; i++) {
                if (tileSlrtToFirstPixel >= subSwath[i].slrTimeToFirstValidPixel &&
                        tileSlrtToFirstPixel <= subSwath[i].slrTimeToLastValidPixel) {

                    if (tileFirstLineTime >= subSwath[i].firstValidLineTime &&
                            tileFirstLineTime < subSwath[i].lastValidLineTime ||
                            tileLastLineTime >= subSwath[i].firstValidLineTime &&
                            tileLastLineTime < subSwath[i].lastValidLineTime) {

                        firstSubSwathIndex = i;
                        break;
                    }
                }
            }

            if (firstSubSwathIndex == numOfSubSwath) {
                lastSubSwathIndex = firstSubSwathIndex;
            } else {
                for (int i = 0; i < numOfSubSwath; i++) {
                    if (tileSlrtToLastPixel >= subSwath[i].slrTimeToFirstValidPixel &&
                            tileSlrtToLastPixel <= subSwath[i].slrTimeToLastValidPixel) {

                        if (tileFirstLineTime >= subSwath[i].firstValidLineTime &&
                                tileFirstLineTime < subSwath[i].lastValidLineTime ||
                                tileLastLineTime >= subSwath[i].firstValidLineTime &&
                                tileLastLineTime < subSwath[i].lastValidLineTime) {

                            lastSubSwathIndex = i;
                        }
                    }
                }
            }

            if (firstSubSwathIndex == -1 && lastSubSwathIndex == -1) {
                return;
            }

            if (firstSubSwathIndex != -1 && lastSubSwathIndex == -1) {
                lastSubSwathIndex = firstSubSwathIndex;
            }

            if (firstSubSwathIndex == -1 && lastSubSwathIndex != -1) {
                firstSubSwathIndex = lastSubSwathIndex;
            }

            final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
            final boolean tileInOneSubSwath = (numOfSourceTiles == 1);

            final Rectangle[] sourceRectangle = new Rectangle[numOfSourceTiles];
            int k = 0;
            for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                sourceRectangle[k++] = getSourceRectangle(tx0, ty0, tw, th, i);
            }

            final int txMax = tx0 + tw;
            final int tyMax = ty0 + th;

            final Band[] tgtBands = targetProduct.getBands();
            for (Band tgtBand:tgtBands) {
                if (tgtBand instanceof VirtualBand) {
                    continue;
                }

                final String tgtBandName = tgtBand.getName();
                final int dataType = tgtBand.getDataType();
                final Tile tgtTile = targetTiles.get(tgtBand);
                if (tileInOneSubSwath) {
                    if (dataType == ProductData.TYPE_INT16) {
                        computeTileInOneSwathShort(tx0, ty0, txMax, tyMax, firstSubSwathIndex,
                                sourceRectangle, tgtBandName, tgtTile);
                    } else {
                        computeTileInOneSwathFloat(tx0, ty0, txMax, tyMax, firstSubSwathIndex,
                                sourceRectangle, tgtBandName, tgtTile);
                    }

                } else {
                    if (dataType == ProductData.TYPE_INT16) {
                        computeMultipleSubSwathsShort(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex,
                                sourceRectangle, tgtBandName, tgtTile);
                    } else {
                        computeMultipleSubSwathsFloat(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex,
                                sourceRectangle, tgtBandName, tgtTile);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            //OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void computeTileInOneSwathShort(final int tx0, final int ty0, final int txMax, final int tyMax,
                                            final int firstSubSwathIndex, final Rectangle[] sourceRectangle,
                                            final String tgtBandName, final Tile tgtTile) {

        final int yMin = computeYMin(subSwath[firstSubSwathIndex]);
        final int yMax = computeYMax(subSwath[firstSubSwathIndex]);
        final int xMin = computeXMin(subSwath[firstSubSwathIndex]);
        final int xMax = computeXMax(subSwath[firstSubSwathIndex]);

        final int firstY = Math.max(ty0, yMin);
        final int lastY = Math.min(tyMax, yMax + 1);
        final int firstX = Math.max(tx0, xMin);
        final int lastX = Math.min(txMax, xMax + 1);

        if (firstY >= lastY || firstX >= lastX) {
            return;
        }

        final String swathIndexStr = String.valueOf(getSubSwathIndex(subSwath[firstSubSwathIndex].subSwathName));
        final Band srcBand = getSourceBandFromTargetBandName(tgtBandName, acquisitionMode, swathIndexStr);
        final Tile srcRaster = getSourceTile(srcBand, sourceRectangle[0]);
        final TileIndex srcTileIndex = new TileIndex(srcRaster);
        final TileIndex tgtIndex = new TileIndex(tgtTile);

        final short[] srcArray = (short[]) srcRaster.getDataBuffer().getElems();
        final short[] tgtArray = (short[]) tgtTile.getDataBuffer().getElems();

        for (int y = firstY; y < lastY; y++) {

            final int sy0 = getLineIndexInSourceProduct(y, subSwath[firstSubSwathIndex]);
            final int tgtOffset = tgtIndex.calculateStride(y);
            final Sentinel1Utils.SubSwathInfo firstSubSwath = subSwath[firstSubSwathIndex];
            final int offset = srcTileIndex.calculateStride(sy0);

            final int sx0 = (int) Math.round(((targetSlantRangeTimeToFirstPixel + firstX * targetDeltaSlantRangeTime)
                    - firstSubSwath.slrTimeToFirstValidPixel) / targetDeltaSlantRangeTime);

            System.arraycopy(srcArray, sx0 - offset, tgtArray, firstX - tgtOffset, lastX - firstX);
        }
    }

    private void computeTileInOneSwathFloat(final int tx0, final int ty0, final int txMax, final int tyMax,
                                            final int firstSubSwathIndex, final Rectangle[] sourceRectangle,
                                            final String tgtBandName, final Tile tgtTile) {

        final int yMin = computeYMin(subSwath[firstSubSwathIndex]);
        final int yMax = computeYMax(subSwath[firstSubSwathIndex]);
        final int xMin = computeXMin(subSwath[firstSubSwathIndex]);
        final int xMax = computeXMax(subSwath[firstSubSwathIndex]);

        final int firstY = Math.max(ty0, yMin);
        final int lastY = Math.min(tyMax, yMax + 1);
        final int firstX = Math.max(tx0, xMin);
        final int lastX = Math.min(txMax, xMax + 1);

        if (firstY >= lastY || firstX >= lastX) {
            return;
        }

        final String swathIndexStr = String.valueOf(getSubSwathIndex(subSwath[firstSubSwathIndex].subSwathName));
        final Band srcBand = getSourceBandFromTargetBandName(tgtBandName, acquisitionMode, swathIndexStr);
        final Tile srcRaster = getSourceTile(srcBand, sourceRectangle[0]);
        final TileIndex srcTileIndex = new TileIndex(srcRaster);
        final TileIndex tgtIndex = new TileIndex(tgtTile);

        final float[] srcArray = (float[]) srcRaster.getDataBuffer().getElems();
        final float[] tgtArray = (float[]) tgtTile.getDataBuffer().getElems();

        for (int y = firstY; y < lastY; y++) {

            final int sy0 = getLineIndexInSourceProduct(y, subSwath[firstSubSwathIndex]);
            final int tgtOffset = tgtIndex.calculateStride(y);
            final Sentinel1Utils.SubSwathInfo firstSubSwath = subSwath[firstSubSwathIndex];
            int offset = srcTileIndex.calculateStride(sy0);

            final int sx0 = (int) Math.round(((targetSlantRangeTimeToFirstPixel + firstX * targetDeltaSlantRangeTime)
                    - firstSubSwath.slrTimeToFirstValidPixel) / targetDeltaSlantRangeTime);

            System.arraycopy(srcArray, sx0 - offset, tgtArray, firstX - tgtOffset, lastX - firstX);
        }
    }

    private void computeMultipleSubSwathsShort(final int tx0, final int ty0, final int txMax, final int tyMax,
                                               final int firstSubSwathIndex, final int lastSubSwathIndex,
                                               final Rectangle[] sourceRectangle, final String tgtBandName,
                                               final Tile tgtTile) {

        final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
        final TileIndex tgtIndex = new TileIndex(tgtTile);
        final Tile[] srcTiles = new Tile[numOfSourceTiles];

        final short[][] srcArray = new short[numOfSourceTiles][];
        final short[] tgtArray = (short[]) tgtTile.getDataBuffer().getElems();

        int k = 0;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            final String swathIndexStr = String.valueOf(getSubSwathIndex(subSwath[i].subSwathName));
            final Band srcBand = getSourceBandFromTargetBandName(tgtBandName, acquisitionMode, swathIndexStr);
            final Tile srcRaster = getSourceTile(srcBand, sourceRectangle[k]);
            srcTiles[k] = srcRaster;
            srcArray[k] = (short[]) srcRaster.getDataBuffer().getElems();
            k++;
        }

        for (int y = ty0; y < tyMax; y++) {
            final int tgtOffset = tgtIndex.calculateStride(y);

            for (int x = tx0; x < txMax; x++) {

                int subSwathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex);
                if (subSwathIndex == -1) {
                    continue;
                }

                final int sy = getLineIndexInSourceProduct(y, subSwath[subSwathIndex]);
                final int sx = getSampleIndexInSourceProduct(x, subSwath[subSwathIndex]);

                short val = 0;
                k = subSwathIndex - firstSubSwathIndex;
                int idx = srcTiles[k].getDataBufferIndex(sx, sy);
                if (idx >= 0) {
                    val = srcArray[k][idx];
                }

                tgtArray[x - tgtOffset] = val;
            }
        }
    }

    private void computeMultipleSubSwathsFloat(final int tx0, final int ty0, final int txMax, final int tyMax,
                                               final int firstSubSwathIndex, final int lastSubSwathIndex,
                                               final Rectangle[] sourceRectangle, final String tgtBandName,
                                               final Tile tgtTile) {

        final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
        final TileIndex tgtIndex = new TileIndex(tgtTile);
        final Tile[] srcTiles = new Tile[numOfSourceTiles];

        final float[][] srcArray = new float[numOfSourceTiles][];
        final float[] tgtArray = (float[]) tgtTile.getDataBuffer().getElems();

        int k = 0;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            final String swathIndexStr = String.valueOf(getSubSwathIndex(subSwath[i].subSwathName));
            final Band srcBand = getSourceBandFromTargetBandName(tgtBandName, acquisitionMode, swathIndexStr);
            final Tile srcRaster = getSourceTile(srcBand, sourceRectangle[k]);
            srcTiles[k] = srcRaster;
            srcArray[k] = (float[]) srcRaster.getDataBuffer().getElems();
            k++;
        }

        for (int y = ty0; y < tyMax; y++) {
            final int tgtOffset = tgtIndex.calculateStride(y);

            for (int x = tx0; x < txMax; x++) {

                int subSwathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex);
                if (subSwathIndex == -1) {
                    continue;
                }

                final int sy = getLineIndexInSourceProduct(y, subSwath[subSwathIndex]);
                final int sx = getSampleIndexInSourceProduct(x, subSwath[subSwathIndex]);

                float val = 0;
                k = subSwathIndex - firstSubSwathIndex;
                int idx = srcTiles[k].getDataBufferIndex(sx, sy);
                if (idx >= 0) {
                    val = srcArray[k][idx];
                }

                tgtArray[x - tgtOffset] = val;
            }
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param tx0           X coordinate for the upper left corner pixel in the target tile.
     * @param ty0           Y coordinate for the upper left corner pixel in the target tile.
     * @param tw            The target tile width.
     * @param th            The target tile height.
     * @param subSwathIndex The subswath index.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceRectangle(
            final int tx0, final int ty0, final int tw, final int th, final int subSwathIndex) {

        final Sentinel1Utils.SubSwathInfo sw = subSwath[subSwathIndex];

        final int x0 = getSampleIndexInSourceProduct(tx0, sw);
        final int xMax = getSampleIndexInSourceProduct(tx0 + tw - 1, sw);

        final int y0 = getLineIndexInSourceProduct(ty0, sw);
        final int yMax = getLineIndexInSourceProduct(ty0 + th - 1, sw);

        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;

        return new Rectangle(x0, y0, w, h);
    }

    private int getSampleIndexInSourceProduct(final int tx, final Sentinel1Utils.SubSwathInfo subSwath) {

        final int sx = (int)((((targetSlantRangeTimeToFirstPixel + tx * targetDeltaSlantRangeTime)
                - subSwath.slrTimeToFirstValidPixel) / targetDeltaSlantRangeTime) + 0.5);

        final int numOfValidSamples = subSwath.lastValidPixel - subSwath.firstValidPixel + 1;
        return sx < 0 ? 0 : sx > numOfValidSamples - 1 ? numOfValidSamples - 1 : sx;
    }

    private int getLineIndexInSourceProduct(final int ty, final Sentinel1Utils.SubSwathInfo subSwath) {

        final double targetLineTime = targetFirstLineTime + ty * targetLineTimeInterval;

        final int sy = (int)((targetLineTime - subSwath.firstLineTime) / subSwath.azimuthTimeInterval + 0.5);

        return sy < 0 ? 0 : sy > subSwath.numOfLines - 1 ? subSwath.numOfLines - 1 : sy;
    }

    private int computeYMin(final Sentinel1Utils.SubSwathInfo subSwath) {

        return (int)Math.round((subSwath.firstLineTime - targetFirstLineTime) / targetLineTimeInterval);
    }

    private int computeYMax(final Sentinel1Utils.SubSwathInfo subSwath) {

        return (int)Math.round((subSwath.lastLineTime - targetFirstLineTime) / targetLineTimeInterval);
    }

    private int computeXMin(final Sentinel1Utils.SubSwathInfo subSwath) {

        return (int)Math.round((subSwath.slrTimeToFirstValidPixel - targetSlantRangeTimeToFirstPixel) / targetDeltaSlantRangeTime);
    }

    private int computeXMax(final Sentinel1Utils.SubSwathInfo subSwath) {

        return (int)Math.round((subSwath.slrTimeToLastValidPixel - targetSlantRangeTimeToFirstPixel) / targetDeltaSlantRangeTime);
    }

    private int getSubSwathIndex(
            final int tx, final int ty, final int firstSubSwathIndex, final int lastSubSwathIndex) {

        final double targetSampleSlrTime = targetSlantRangeTimeToFirstPixel + tx * targetDeltaSlantRangeTime;
        final double targetLineTime = targetFirstLineTime + ty * targetLineTimeInterval;

        int cnt = 0;
        int swath0 = -1, swath1 = -1;
        Sentinel1Utils.SubSwathInfo info;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            info = subSwath[i];
            if (targetLineTime >= info.firstValidLineTime &&
                    targetLineTime <= info.lastValidLineTime &&
                    targetSampleSlrTime >= info.slrTimeToFirstValidPixel &&
                    targetSampleSlrTime <= info.slrTimeToLastValidPixel) {

                if (cnt == 0) {
                    swath0 = i;
                } else {
                    swath1 = i;
                    break;
                }
                ++cnt;
            }
        }

        if (swath1 != -1) {
            final double middleTime = (subSwath[swath0].slrTimeToLastValidPixel +
                    subSwath[swath1].slrTimeToFirstValidPixel)/2.0;

            if (targetSampleSlrTime > middleTime) {
                return swath1;
            }
        }
        return swath0;
    }

    private double getSubSwathNoise(final int tx, final double targetLineTime,
                                    final Sentinel1Utils.SubSwathInfo sw, final String pol) {

        final Sentinel1Utils.NoiseVector[] vectorList = sw.noise.get(pol);

        final int sx = getSampleIndexInSourceProduct(tx, sw);
        final int sy = (int) ((targetLineTime - vectorList[0].timeMJD*Constants.secondsInDay) / targetLineTimeInterval);

        int l0 = -1, l1 = -1;
        int vectorIdx0 = -1, vectorIdxInc = 0;
        if (sy < vectorList[0].line) {

            l0 = vectorList[0].line;
            l1 = l0;
            vectorIdx0 = 0;

        } else if (sy >= vectorList[vectorList.length - 1].line) {

            l0 = vectorList[vectorList.length - 1].line;
            l1 = l0;
            vectorIdx0 = vectorList.length - 1;

        } else {
            vectorIdxInc = 1;
            int max = vectorList.length - 1;
            for (int i = 0; i < max; i++) {
                if (sy >= vectorList[i].line && sy < vectorList[i + 1].line) {
                    l0 = vectorList[i].line;
                    l1 = vectorList[i + 1].line;
                    vectorIdx0 = i;
                    break;
                }
            }
        }

        final int[] pixels = vectorList[vectorIdx0].pixels;
        int p0 = -1, p1 = -1;
        int pixelIdx0 = -1, pixelIdxInc = 0;
        if (sx < pixels[0]) {

            p0 = pixels[0];
            p1 = p0;
            pixelIdx0 = 0;

        } else if (sx >= pixels[pixels.length - 1]) {

            p0 = pixels[pixels.length - 1];
            p1 = p0;
            pixelIdx0 = pixels.length - 1;

        } else {

            pixelIdxInc = 1;
            int max = pixels.length - 1;
            for (int i = 0; i < max; i++) {
                if (sx >= pixels[i] && sx < pixels[i + 1]) {
                    p0 = pixels[i];
                    p1 = pixels[i + 1];
                    pixelIdx0 = i;
                    break;
                }
            }
        }

        final float[] noiseLUT0 = vectorList[vectorIdx0].noiseLUT;
        final float[] noiseLUT1 = vectorList[vectorIdx0 + vectorIdxInc].noiseLUT;
        double dx;
        if (p0 == p1) {
            dx = 0;
        } else {
            dx = (sx - p0) / (p1 - p0);
        }

        double dy;
        if (l0 == l1) {
            dy = 0;
        } else {
            dy = (sy - l0) / (l1 - l0);
        }

        return Maths.interpolationBiLinear(noiseLUT0[pixelIdx0], noiseLUT0[pixelIdx0 + pixelIdxInc],
                noiseLUT1[pixelIdx0], noiseLUT1[pixelIdx0 + pixelIdxInc],
                dx, dy);
    }

    private static class SubSwathEffectStartEndPixels {
        public int xMin;
        public int xMax;

        public SubSwathEffectStartEndPixels() {
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(TOPSARMergeOp.class);
        }
    }
}
