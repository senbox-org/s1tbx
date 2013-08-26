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
import java.util.*;
import java.util.List;

/**
 * De-Burst a Sentinel-1 TOPSAR product
 */
@OperatorMetadata(alias = "DeburstTOPSAR",
        category = "SAR Tools\\SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Debursts a Sentinel-1 TOPSAR product")
public final class Sentinel1DeburstTOPSAROp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of polarisations", label="Polarisations")
    private String[] selectedPolarisations;

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

    private SubSwathInfo[] subSwath = null;

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

            if(selectedPolarisations == null || selectedPolarisations.length == 0) {
                selectedPolarisations = getProductPolarizations(absRoot, acquisitionMode);
            }

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

    public static String[] getProductPolarizations(final MetadataElement absRoot, final String acquisitionMode) {

        final MetadataElement[] elems = absRoot.getElements();
        final String subSwathName = acquisitionMode + '1';
        final List<String> polarizations = new ArrayList<String>(4);
        for(MetadataElement elem : elems) {
            if(elem.getName().contains(subSwathName)) {
                polarizations.add(elem.getAttributeString("polarization"));
            }
        }
        return polarizations.toArray(new String[polarizations.size()]);
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

    private static void setParameters(final MetadataElement subSwathMetadata, final SubSwathInfo subSwath) {

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
        subSwath.azimuthTimeInterval = imageInformation.getAttributeDouble("azimuthTimeInterval")/Constants.secondsInDay; // s to day
        subSwath.rangePixelSpacing = imageInformation.getAttributeDouble("rangePixelSpacing");
        subSwath.slrTimeToFirstPixel = imageInformation.getAttributeDouble("slantRangeTime")/2.0; // 2-way to 1-way
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

        // get geolocation grid points
        final MetadataElement geolocationGrid = product.getElement("geolocationGrid");
        final MetadataElement geolocationGridPointList = geolocationGrid.getElement("geolocationGridPointList");
        final int numOfGeoLocationGridPoints = geolocationGridPointList.getAttributeInt("count");
        final MetadataElement[] geolocationGridPointListElem = geolocationGridPointList.getElements();
        int numOfGeoPointsPerLine = 0;
        int line = 0;
        for (MetadataElement listElem : geolocationGridPointListElem) {
            if (numOfGeoPointsPerLine == 0) {
                line = listElem.getAttributeInt("line");
                numOfGeoPointsPerLine++;
            } else if (line == listElem.getAttributeInt("line")) {
                numOfGeoPointsPerLine++;
            } else {
                break;
            }
        }

        final int numOfGeoLines = numOfGeoLocationGridPoints/numOfGeoPointsPerLine;
        subSwath.numOfGeoLines = numOfGeoLines;
        subSwath.numOfGeoPointsPerLine = numOfGeoPointsPerLine;
        subSwath.azimuthTime = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.slantRangeTime = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.latitude = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.longitude = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.incidenceAngle = new double[numOfGeoLines][numOfGeoPointsPerLine];
        k = 0;
        for (MetadataElement listElem : geolocationGridPointListElem) {
            final int i = k / numOfGeoPointsPerLine;
            final int j = k - i*numOfGeoPointsPerLine;
            subSwath.azimuthTime[i][j] = Sentinel1Utils.getTime(listElem, "azimuthTime").getMJD();
            subSwath.slantRangeTime[i][j] = listElem.getAttributeDouble("slantRangeTime")/2.0;
            subSwath.latitude[i][j] = listElem.getAttributeDouble("latitude");
            subSwath.longitude[i][j] = listElem.getAttributeDouble("longitude");
            subSwath.incidenceAngle[i][j] = listElem.getAttributeDouble("incidenceAngle");
            k++;
        }
    }

    private void getSubSwathNoiseVectors() {

        for (int i = 0; i < numOfSubSwath; i++) {
            for (String pol:selectedPolarisations) {
                if (pol != null) {
                    final Band srcBand = getSourceBand(subSwath[i].subSwathName, pol);
                    final Sentinel1Utils.NoiseVector[] noiseVectors = Sentinel1Utils.getNoiseVector(srcBand);
                    subSwath[i].noise.put(pol, noiseVectors);
                }
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
        targetLineTimeInterval = subSwath[0].azimuthTimeInterval; // days
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

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), targetWidth, targetHeight);

        final Band[] sourceBands = sourceProduct.getBands();
        for (String pol:selectedPolarisations) {
            final Band srcBand = sourceBands[0];
            final Band trgI = targetProduct.addBand("i_" + pol, srcBand.getDataType());
            trgI.setUnit(Unit.REAL);
            trgI.setNoDataValueUsed(true);
            trgI.setNoDataValue(srcBand.getNoDataValue());

            final Band trgQ = targetProduct.addBand("q_" + pol, srcBand.getDataType());
            trgQ.setUnit(Unit.IMAGINARY);
            trgQ.setNoDataValueUsed(true);
            trgQ.setNoDataValue(srcBand.getNoDataValue());

            ReaderUtils.createVirtualIntensityBand(targetProduct, trgI, trgQ, '_' + pol);
            ReaderUtils.createVirtualPhaseBand(targetProduct, trgI, trgQ, '_' + pol);
        }

        copyMetaData(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        targetProduct.setStartTime(new ProductData.UTC(targetFirstLineTime));
        targetProduct.setEndTime(new ProductData.UTC(targetLastLineTime));
        targetProduct.setDescription(sourceProduct.getDescription());

        createTiePointGrids();

        targetProduct.setPreferredTileSize(500,50);
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

        final int gridWidth = 20;
        final int gridHeight = 5;

        final int subSamplingX = targetWidth/gridWidth;
        final int subSamplingY = targetHeight/gridHeight;

        final float[] latList = new float[gridWidth*gridHeight];
        final float[] lonList = new float[gridWidth*gridHeight];
        final float[] slrtList = new float[gridWidth*gridHeight];
        final float[] incList = new float[gridWidth*gridHeight];

        final Index index = new Index();

        int k = 0;
        for (int i = 0; i < gridHeight; i++) {
            final int y = i*subSamplingY;
            final double azTime = targetFirstLineTime + y*targetLineTimeInterval;
            for (int j = 0; j < gridWidth; j++) {
                final int x = j*subSamplingX;
                final double slrTime = targetSlantRangeTimeToFirstPixel + x*targetDeltaSlantRangeTime;
                final int subSwathIndex = getSubSwathIndex(slrTime);
                computeIndex(azTime, slrTime, subSwathIndex, index);
                latList[k] = getLatitudeValue(index, subSwathIndex);
                lonList[k] = getLongitudeValue(index, subSwathIndex);
                slrtList[k] = (float)(getSlantRangeTimeValue(index, subSwathIndex)*2*Constants.oneBillion); // 2-way ns
                incList[k] = getIncidenceAngleValue(index, subSwathIndex);
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
    }

    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetWidth);
        absTgt.setAttributeUTC(AbstractMetadata.first_line_time, new ProductData.UTC(targetFirstLineTime));
        absTgt.setAttributeUTC(AbstractMetadata.last_line_time, new ProductData.UTC(targetLastLineTime));
        absTgt.setAttributeDouble(AbstractMetadata.line_time_interval, targetLineTimeInterval * Constants.secondsInDay); // days to s

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
    }

    private int getSubSwathIndex(final double slrTime) {
        double startTime, endTime;
        for (int i = 0; i < numOfSubSwath; i++) {

            if (i == 0) {
                startTime = subSwath[i].slrTimeToFirstPixel;
            } else {
                startTime = 0.5*(subSwath[i].slrTimeToFirstPixel + subSwath[i-1].slrTimeToLastPixel);
            }

            if (i == numOfSubSwath - 1) {
                endTime = subSwath[i].slrTimeToLastPixel;
            } else {
                endTime = 0.5*(subSwath[i].slrTimeToLastPixel + subSwath[i+1].slrTimeToFirstPixel);
            }

            if (slrTime >= startTime && slrTime < endTime) {
                return i + 1; // subswath index start from 0
            }
        }

        return 0;
    }

    private void computeIndex(final double azTime, final double slrTime, final int subSwathIndex, Index index) {

        int j0 = -1, j1 = -1;
        double muX = 0;
        for (int j = 0; j < subSwath[subSwathIndex - 1].numOfGeoPointsPerLine - 1; j++) {
            if (subSwath[subSwathIndex-1].slantRangeTime[0][j] <= slrTime &&
                subSwath[subSwathIndex-1].slantRangeTime[0][j+1] > slrTime) {
                j0 = j;
                j1 = j + 1;
                muX = (slrTime - subSwath[subSwathIndex-1].slantRangeTime[0][j]) /
                      (subSwath[subSwathIndex-1].slantRangeTime[0][j+1] - subSwath[subSwathIndex-1].slantRangeTime[0][j]);
            }
        }

        if (j0 == -1 || j1 == -1) {
            throw new OperatorException("Invalid subswath index");
        }

        int i0 = -1, i1 = -1;
        double muY = 0;
        for (int i = 0; i < subSwath[subSwathIndex-1].numOfGeoLines - 1; i++) {
            final double i0AzTime = (1 - muX)*subSwath[subSwathIndex-1].azimuthTime[i][j0] +
                                          muX*subSwath[subSwathIndex-1].azimuthTime[i][j1];

            final double i1AzTime = (1 - muX)*subSwath[subSwathIndex-1].azimuthTime[i+1][j0] +
                                          muX*subSwath[subSwathIndex-1].azimuthTime[i+1][j1];

            if (i == 0 && azTime < i0AzTime ||
                i == subSwath[subSwathIndex-1].numOfGeoLines - 2 && azTime >= i1AzTime ||
                i0AzTime <= azTime && i1AzTime > azTime) {

                i0 = i;
                i1 = i + 1;
                muY = (azTime - i0AzTime) / (i1AzTime - i0AzTime);
                break;
            }
        }

        index.i0 = i0;
        index.i1 = i1;
        index.j0 = j0;
        index.j1 = j1;
        index.muX = muX;
        index.muY = muY;
    }

    private float getLatitudeValue(final Index index, final int subSwathIndex) {
        final double lat00 = subSwath[subSwathIndex - 1].latitude[index.i0][index.j0];
        final double lat01 = subSwath[subSwathIndex - 1].latitude[index.i0][index.j1];
        final double lat10 = subSwath[subSwathIndex - 1].latitude[index.i1][index.j0];
        final double lat11 = subSwath[subSwathIndex - 1].latitude[index.i1][index.j1];

        return (float)((1 - index.muY)*((1 - index.muX)*lat00 + index.muX*lat01) +
                             index.muY*((1 - index.muX)*lat10 + index.muX*lat11));
    }

    private float getLongitudeValue(final Index index, final int subSwathIndex) {
        final double lon00 = subSwath[subSwathIndex - 1].longitude[index.i0][index.j0];
        final double lon01 = subSwath[subSwathIndex - 1].longitude[index.i0][index.j1];
        final double lon10 = subSwath[subSwathIndex - 1].longitude[index.i1][index.j0];
        final double lon11 = subSwath[subSwathIndex - 1].longitude[index.i1][index.j1];

        return (float)((1 - index.muY)*((1 - index.muX)*lon00 + index.muX*lon01) +
                             index.muY*((1 - index.muX)*lon10 + index.muX*lon11));
    }

    private float getSlantRangeTimeValue(final Index index, final int subSwathIndex) {
        final double slrt00 = subSwath[subSwathIndex - 1].slantRangeTime[index.i0][index.j0];
        final double slrt01 = subSwath[subSwathIndex - 1].slantRangeTime[index.i0][index.j1];
        final double slrt10 = subSwath[subSwathIndex - 1].slantRangeTime[index.i1][index.j0];
        final double slrt11 = subSwath[subSwathIndex - 1].slantRangeTime[index.i1][index.j1];

        return (float)((1 - index.muY)*((1 - index.muX)*slrt00 + index.muX*slrt01) +
                             index.muY*((1 - index.muX)*slrt10 + index.muX*slrt11));
    }

    private float getIncidenceAngleValue(final Index index, final int subSwathIndex) {
        final double inc00 = subSwath[subSwathIndex - 1].incidenceAngle[index.i0][index.j0];
        final double inc01 = subSwath[subSwathIndex - 1].incidenceAngle[index.i0][index.j1];
        final double inc10 = subSwath[subSwathIndex - 1].incidenceAngle[index.i1][index.j0];
        final double inc11 = subSwath[subSwathIndex - 1].incidenceAngle[index.i1][index.j1];

        return (float)((1 - index.muY)*((1 - index.muX)*inc00 + index.muX*inc01) +
                             index.muY*((1 - index.muX)*inc10 + index.muX*inc11));
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
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

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

            for (String pol:selectedPolarisations) {
                if (pol != null) {
                    final Tile targetTileI = targetTiles.get(targetProduct.getBand("i_" + pol));
                    final Tile targetTileQ = targetTiles.get(targetProduct.getBand("q_" + pol));

                    if (tileInOneSubSwath) {
                        computeTileInOneSwath(tx0, ty0, lastX, th, firstSubSwathIndex, pol,
                                sourceRectangle, bandNameI, bandNameQ, targetTileI, targetTileQ, burstInfo);

                    } else {
                        computeMultipleSubSwaths(tx0, ty0, lastX, th, firstSubSwathIndex, lastSubSwathIndex, pol,
                                sourceRectangle, bandNameI, bandNameQ, targetTileI, targetTileQ, burstInfo);

                    }
                }
            }
        } catch(Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void computeTileInOneSwath(final int tx0, final int ty0, final int lastX, final int th,
                                       final int firstSubSwathIndex, final String pol,
                                       final Rectangle[] sourceRectangle,
                                       final String bandNameI, final String bandNameQ,
                                       final Tile targetTileI, final Tile targetTileQ,
                                       final BurstInfo burstInfo) {

        final int yMin = computeYMin(subSwath[firstSubSwathIndex - 1]);
        final int yMax = computeYMax(subSwath[firstSubSwathIndex-1]);
        final int firstY = Math.max(ty0, yMin);
        final int lastY = Math.min(ty0 + th, yMax + 1);

        if(firstY >= lastY)
            return;

        final Band srcBandI = sourceProduct.getBand(bandNameI + firstSubSwathIndex + '_' + pol);
        final Band srcBandQ = sourceProduct.getBand(bandNameQ + firstSubSwathIndex + '_' + pol);
        final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[0]);
        final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[0]);
        final TileIndex srcTileIndex = new TileIndex(sourceRasterI);
        final TileIndex tgtIndex = new TileIndex(targetTileI);

        final short[] srcArrayI = (short[])sourceRasterI.getDataBuffer().getElems();
        final short[] srcArrayQ = (short[])sourceRasterQ.getDataBuffer().getElems();
        final short[] tgtArrayI = (short[])targetTileI.getDataBuffer().getElems();
        final short[] tgtArrayQ = (short[])targetTileQ.getDataBuffer().getElems();

        for (int y = firstY; y < lastY; y++) {

            if(!getLineIndicesInSourceProduct(y, subSwath[firstSubSwathIndex-1], burstInfo)) {
                continue;
            }

            final int tgtOffset = tgtIndex.calculateStride(y);
            final SubSwathInfo firstSubSwath = subSwath[firstSubSwathIndex-1];
            int offset;
            if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                offset = srcTileIndex.calculateStride(burstInfo.sy1);
            } else {
                offset = srcTileIndex.calculateStride(burstInfo.sy0);
            }

            final int sx = (int)Math.round(( (targetSlantRangeTimeToFirstPixel + tx0*targetDeltaSlantRangeTime)
                    - firstSubSwath.slrTimeToFirstPixel)/targetDeltaSlantRangeTime);

            System.arraycopy(srcArrayI, sx-offset, tgtArrayI, tx0-tgtOffset, lastX-tx0);
            System.arraycopy(srcArrayQ, sx-offset, tgtArrayQ, tx0-tgtOffset, lastX-tx0);
        }
    }

    private void computeMultipleSubSwaths(final int tx0, final int ty0, final int lastX, final int th,
                                          final int firstSubSwathIndex, final int lastSubSwathIndex, final String pol,
                                          final Rectangle[] sourceRectangle,
                                          final String bandNameI, final String bandNameQ,
                                          final Tile targetTileI, final Tile targetTileQ,
                                          final BurstInfo burstInfo) {
        final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
        final TileIndex tgtIndex = new TileIndex(targetTileI);
        final Tile[] srcTiles = new Tile[numOfSourceTiles];

        final short[][] srcArrayI = new short[numOfSourceTiles][];
        final short[][] srcArrayQ = new short[numOfSourceTiles][];
        final short[] tgtArrayI = (short[])targetTileI.getDataBuffer().getElems();
        final short[] tgtArrayQ = (short[])targetTileQ.getDataBuffer().getElems();

        int k = 0;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            final Band srcBandI = sourceProduct.getBand(bandNameI + i + '_' + pol);
            final Band srcBandQ = sourceProduct.getBand(bandNameQ + i + '_' + pol);
            final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[k]);
            final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[k]);
            srcTiles[k] = sourceRasterI;

            srcArrayI[k] = (short[])sourceRasterI.getDataBuffer().getElems();
            srcArrayQ[k] = (short[])sourceRasterQ.getDataBuffer().getElems();
            k++;
        }

        int sy;
        for (int y = ty0; y < ty0 + th; y++) {
            final int tgtOffset = tgtIndex.calculateStride(y);

            for (int x = tx0; x < lastX; x++) {

                int subswathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex, pol, burstInfo);
                if (subswathIndex == -1) {
                    continue;
                }
                if(!getLineIndicesInSourceProduct(y, subSwath[subswathIndex-1], burstInfo)) {
                    continue;
                }

                short iVal = 0, qVal = 0;
                k = subswathIndex - firstSubSwathIndex;

                int sx = getSampleIndexInSourceProduct(x, subSwath[subswathIndex-1]);
                if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                    sy = burstInfo.sy1;
                } else {
                    sy = burstInfo.sy0;
                }
                int idx = srcTiles[k].getDataBufferIndex(sx, sy);

                if(idx >= 0) {
                    iVal = srcArrayI[k][idx];
                    qVal = srcArrayQ[k][idx];
                }

                double intensity = iVal*iVal+qVal*qVal;
                //if(burstInfo.swath1 != -1 && iVal == 0 && qVal == 0) {
                if(burstInfo.swath1 != -1 && intensity < 300) {
                    // edge of swaths found therefore use other swath
                    if(subswathIndex == burstInfo.swath0)
                        subswathIndex = burstInfo.swath1;
                    else
                        subswathIndex = burstInfo.swath0;

                    getLineIndicesInSourceProduct(y, subSwath[subswathIndex-1], burstInfo);

                    k = subswathIndex - firstSubSwathIndex;

                    sx = getSampleIndexInSourceProduct(x, subSwath[subswathIndex-1]);
                    if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                        sy = burstInfo.sy1;
                    } else {
                        sy = burstInfo.sy0;
                    }
                    idx = srcTiles[k].getDataBufferIndex(sx, sy);

                    if(idx >=0 && !(srcArrayI[k][idx] == 0 && srcArrayQ[k][idx] == 0)) {
                        iVal = srcArrayI[k][idx];
                        qVal = srcArrayQ[k][idx];
                    }
                }
                tgtArrayI[x-tgtOffset] = iVal;
                tgtArrayQ[x-tgtOffset] = qVal;
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
        final int sx = (int)Math.round(( (targetSlantRangeTimeToFirstPixel + tx*targetDeltaSlantRangeTime)
                - subSwath.slrTimeToFirstPixel)/targetDeltaSlantRangeTime);
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
                    break;
                }
                ++k;
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
        SubSwathInfo info;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            int i_1 = i - 1;
            info = subSwath[i_1];
            if (targetLineTime >= info.firstLineTime &&
                targetLineTime <= info.lastLineTime &&
                targetSampleSlrTime >= info.slrTimeToFirstPixel &&
                targetSampleSlrTime <= info.slrTimeToLastPixel) {

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
            final double noise0 = getSubSwathNoise(tx, targetLineTime, subSwath[burstInfo.swath0 - 1], pol);
            final double noise1 = getSubSwathNoise(tx, targetLineTime, subSwath[burstInfo.swath1 - 1], pol);
            if (noise0 > noise1) {
                return burstInfo.swath1;
            }
        }
        return burstInfo.swath0;
    }

    private double getSubSwathNoise(final int tx, final double targetLineTime,
                                    final SubSwathInfo sw, final String pol) {

        final Sentinel1Utils.NoiseVector[] vectorList = sw.noise.get(pol);

        final int sx = getSampleIndexInSourceProduct(tx, sw);
        final int sy = (int)((targetLineTime - vectorList[0].timeMJD) / targetLineTimeInterval);

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
                if (sy >= vectorList[i].line && sy < vectorList[i+1].line) {
                    l0 = vectorList[i].line;
                    l1 = vectorList[i+1].line;
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
                if (sx >= pixels[i] && sx < pixels[i+1]) {
                    p0 = pixels[i];
                    p1 = pixels[i+1];
                    pixelIdx0 = i;
                    break;
                }
            }
        }

        final float[] noiseLUT0 = vectorList[vectorIdx0].noiseLUT;
        final float[] noiseLUT1 = vectorList[vectorIdx0+vectorIdxInc].noiseLUT;
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

        return MathUtils.interpolationBiLinear(noiseLUT0[pixelIdx0], noiseLUT0[pixelIdx0+pixelIdxInc],
                                               noiseLUT1[pixelIdx0], noiseLUT1[pixelIdx0+pixelIdxInc],
                                               dx, dy);
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

        // GeoLocationGridPoint
        public int numOfGeoLines;
        public int numOfGeoPointsPerLine;
        public double[][] azimuthTime;
        public double[][] slantRangeTime;
        public double[][] latitude;
        public double[][] longitude;
        public double[][] incidenceAngle;
    }

    private static class Index {
        public int i0;
        public int i1;
        public int j0;
        public int j1;
        public double muX;
        public double muY;

        public Index(){
        }
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
            super.setOperatorUI(Sentinel1DeburstTOPSAROpUI.class);
        }
    }
}