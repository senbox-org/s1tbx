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
package org.esa.snap.engine_utilities.gpf;

import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.eo.Constants;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.text.DateFormat;
import java.util.*;

/**
 * Helper methods for working with Operators
 */
public final class OperatorUtils {

    public static final String TPG_SLANT_RANGE_TIME = "slant_range_time";
    public static final String TPG_INCIDENT_ANGLE = "incident_angle";
    public static final String TPG_ELEVATION_ANGLE = "elevation_angle";
    public static final String TPG_LATITUDE = "latitude";
    public static final String TPG_LONGITUDE = "longitude";

    /**
     * Get incidence angle tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The incidence angle tie point grid.
     */
    public static TiePointGrid getIncidenceAngle(final Product sourceProduct) {

        return sourceProduct.getTiePointGrid(TPG_INCIDENT_ANGLE);
    }

    /**
     * Get slant range time tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The slant range time tie point grid.
     */
    public static TiePointGrid getSlantRangeTime(final Product sourceProduct) {

        return sourceProduct.getTiePointGrid(TPG_SLANT_RANGE_TIME);
    }

    /**
     * Get latitude tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The latitude tie point grid.
     */
    public static TiePointGrid getLatitude(final Product sourceProduct) {

        return sourceProduct.getTiePointGrid(TPG_LATITUDE);
    }

    /**
     * Get longitude tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The longitude tie point grid.
     */
    public static TiePointGrid getLongitude(final Product sourceProduct) {

        return sourceProduct.getTiePointGrid(TPG_LONGITUDE);
    }

    public static String[] getPolarisations(final Product product) {
        final Set<String> polarisationSet = new HashSet<>();
        for(Band band : product.getBands()) {
            String pol = OperatorUtils.getPolarizationFromBandName(band.getName());
            if(pol != null) {
                polarisationSet.add(pol);
            }
        }
        return polarisationSet.toArray(new String[polarisationSet.size()]);
    }

    public static String getBandPolarization(final String bandName, final MetadataElement absRoot) {
        final String pol = getPolarizationFromBandName(bandName);
        if (pol != null) {
            return pol;
        } else if(absRoot != null) {
            final String[] mdsPolar = getProductPolarization(absRoot);
            return mdsPolar[0];
        }
        return "";
    }

    public static String getPolarizationFromBandName(final String bandName) {

        // Account for possibilities like "x_HH_dB" or "x_HH_times_VV_conj"
        // where the last one will return an exception because it appears to contain
        // multiple polarizations
        String pol = "";
        final String bandNameLower = bandName.toLowerCase();
        if (bandNameLower.contains("_hh")) {
            pol += "hh";
        }
        if (bandNameLower.contains("_vv")) {
            pol += "vv";
        }
        if (bandNameLower.contains("_hv")) {
            pol += "hv";
        }
        if (bandNameLower.contains("_vh")) {
            pol += "vh";
        }

        if (pol.length() == 2) {
            return pol;
        } else if (pol.length() > 2) {
            throw new OperatorException("Band name contains multiple polarziations: " + pol);
        }
        return null;
    }

    public static String getSubswathFromBandName(final String bandName) {

        String ss = "";
        final String bandNameU = bandName.toUpperCase();
        if (bandNameU.contains("_IW")) {
            int idx = bandNameU.indexOf("_IW")+1;
            return bandNameU.substring(idx, idx + 3);
        } else if (bandNameU.contains("_EW")) {
            int idx = bandNameU.indexOf("_EW")+1;
            return bandNameU.substring(idx, idx + 3);
        }

        return "";
    }

    public static String getPolarizationType(final MetadataElement absRoot) {
        String pol1 = absRoot.getAttributeString(AbstractMetadata.mds1_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
        String pol2 = absRoot.getAttributeString(AbstractMetadata.mds2_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
        String pol3 = absRoot.getAttributeString(AbstractMetadata.mds3_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
        String pol4 = absRoot.getAttributeString(AbstractMetadata.mds4_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);

        if(hasPol(pol1)) {
            if(hasPol(pol2)) {
                if(hasPol(pol3) && hasPol(pol4)) {
                    return DBQuery.ANY;
                }
                if(pol1.equals("VV")) {
                    return DBQuery.VVVH;
                }
                if(pol2.equals("VV")) {
                    return DBQuery.HHVV;
                }
                if(pol2.equals("HV")) {
                    return DBQuery.HHHV;
                }
            }
            return pol1;
        }
        return null;
    }

    private static boolean hasPol(String pol) {
        return pol != null && !pol.trim().isEmpty() && !pol.equals(AbstractMetadata.NO_METADATA_STRING);
    }

    /**
     * Get product polarizations for each band in the product.
     *
     * @param absRoot the AbstractMetadata
     * @return mdsPolar the string array to hold the polarization names
     */
    public static String[] getProductPolarization(final MetadataElement absRoot) {

        final String[] mdsPolar = new String[4];
        for (int i = 0; i < mdsPolar.length; ++i) {
            final String polarName = absRoot.getAttributeString(AbstractMetadata.polarTags[i], "").toLowerCase();
            mdsPolar[i] = "";
            if (polarName.contains("hh") || polarName.contains("hv") || polarName.contains("vh") || polarName.contains("vv")) {
                mdsPolar[i] = polarName;
            }
        }
        return mdsPolar;
    }

    public static String getPrefixFromBandName(final String bandName) {

        final int idx1 = bandName.indexOf('_');
        if (idx1 != -1) {
            return bandName.substring(0, idx1);
        }
        final int idx2 = bandName.indexOf('-');
        if (idx2 != -1) {
            return bandName.substring(0, idx2);
        }
        final int idx3 = bandName.indexOf('.');
        if (idx3 != -1) {
            return bandName.substring(0, idx3);
        }
        return null;
    }

    public static String getSuffixFromBandName(final String bandName) {

        final int idx1 = bandName.indexOf('_');
        if (idx1 != -1) {
            return bandName.substring(idx1 + 1);
        }
        final int idx2 = bandName.indexOf('-');
        if (idx2 != -1) {
            return bandName.substring(idx2 + 1);
        }
        final int idx3 = bandName.indexOf('.');
        if (idx3 != -1) {
            return bandName.substring(idx3 + 1);
        }
        return null;
    }

    /**
     * Copy master GCPs to target product.
     *
     * @param group           input master GCP group
     * @param targetGCPGroup  output master GCP group
     * @param targetGeoCoding the geocoding of the target product
     */
    public static void copyGCPsToTarget(final ProductNodeGroup<Placemark> group,
                                        final ProductNodeGroup<Placemark> targetGCPGroup,
                                        final GeoCoding targetGeoCoding) {
        targetGCPGroup.removeAll();

        for (int i = 0; i < group.getNodeCount(); ++i) {
            final Placemark sPin = group.get(i);
            final Placemark tPin = Placemark.createPointPlacemark(GcpDescriptor.getInstance(),
                    sPin.getName(),
                    sPin.getLabel(),
                    sPin.getDescription(),
                    sPin.getPixelPos(),
                    sPin.getGeoPos(),
                    targetGeoCoding);

            targetGCPGroup.add(tPin);
        }
    }

    public static Product createDummyTargetProduct(final Product[] sourceProducts) {
        final Product targetProduct = new Product(sourceProducts[0].getName(),
                sourceProducts[0].getProductType(),
                sourceProducts[0].getSceneRasterWidth(),
                sourceProducts[0].getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProducts[0], targetProduct);
        for (Product prod : sourceProducts) {
            for (Band band : prod.getBands()) {
                ProductUtils.copyBand(band.getName(), prod, band.getName(), targetProduct, false);
            }
        }
        return targetProduct;
    }

    public static String getAcquisitionDate(final MetadataElement root) {
        String dateString;
        try {
            final ProductData.UTC date = root.getAttributeUTC(AbstractMetadata.first_line_time);
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("ddMMMyyyy");
            dateString = dateFormat.format(date.getAsDate());
        } catch (Exception e) {
            dateString = "";
        }
        return dateString;
    }

    public static String getAcquisitionTime(final MetadataElement root) {
        String dateString;
        try {
            final ProductData.UTC time = root.getAttributeUTC(AbstractMetadata.first_line_time);
            final DateFormat timeFormat = ProductData.UTC.createDateFormat("yyyyMMdd_HHmmss");
            dateString = timeFormat.format(time.getAsDate());
            dateString = dateString.replace('_', 'T').substring(0, dateString.length() - 2);
        } catch (Exception e) {
            dateString = "";
        }
        return dateString;
    }

    public static void createNewTiePointGridsAndGeoCoding(
            final Product sourceProduct, final Product targetProduct,
            final int gridWidth, final int gridHeight,
            final double subSamplingX, final double subSamplingY,
            final PixelPos[] newTiePointPos) {

        TiePointGrid latGrid = null;
        TiePointGrid lonGrid = null;

        for (TiePointGrid srcTPG : sourceProduct.getTiePointGrids()) {

            final float[] tiePoints = new float[gridWidth * gridHeight];
            for (int k = 0; k < newTiePointPos.length; k++) {
                tiePoints[k] = (float)srcTPG.getPixelDouble(newTiePointPos[k].x, newTiePointPos[k].y);
            }

            int discontinuity = TiePointGrid.DISCONT_NONE;
            if (srcTPG.getName().equals(TPG_LONGITUDE)) {
                discontinuity = TiePointGrid.DISCONT_AT_180;
            }

            final TiePointGrid tgtTPG = new TiePointGrid(srcTPG.getName(),
                    gridWidth,
                    gridHeight,
                    0.0f,
                    0.0f,
                    subSamplingX,
                    subSamplingY,
                    tiePoints,
                    discontinuity);

            TiePointGrid prevTPG = targetProduct.getTiePointGrid(tgtTPG.getName());
            if(prevTPG != null) {
                targetProduct.removeTiePointGrid(prevTPG);
            }
            targetProduct.addTiePointGrid(tgtTPG);

            if (srcTPG.getName().equals(TPG_LATITUDE)) {
                latGrid = tgtTPG;
            } else if (srcTPG.getName().equals(TPG_LONGITUDE)) {
                lonGrid = tgtTPG;
            }
        }

        final TiePointGeoCoding gc = new TiePointGeoCoding(latGrid, lonGrid);

        targetProduct.setSceneGeoCoding(gc);
    }

    /**
     * get the selected bands
     *
     * @param sourceProduct   the input product
     * @param sourceBandNames the select band names
     * @param includeVirtualBands include virtual bands by default
     * @return band list
     * @throws OperatorException if source band not found
     */
    public static Band[] getSourceBands(final Product sourceProduct, String[] sourceBandNames, final boolean includeVirtualBands) throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (!(band instanceof VirtualBand) || includeVirtualBands)
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        final List<Band> sourceBandList = new ArrayList<>(sourceBandNames.length);
        for (final String sourceBandName : sourceBandNames) {
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand != null) {
                sourceBandList.add(sourceBand);
            }
        }
        return sourceBandList.toArray(new Band[sourceBandList.size()]);
    }

    public static Band[] addBands(final Product targetProduct, final String[] targetBandNameList, final String suffix) {
        final List<Band> bandList = new ArrayList<>(targetBandNameList.length);
        for (String targetBandName : targetBandNameList) {

            final Band targetBand = new Band(targetBandName + suffix,
                    ProductData.TYPE_FLOAT32,
                    targetProduct.getSceneRasterWidth(),
                    targetProduct.getSceneRasterHeight());

            if (targetBandName.contains("_real")) {
                targetBand.setUnit(Unit.REAL);
            } else if (targetBandName.contains("_imag")) {
                targetBand.setUnit(Unit.IMAGINARY);
            } else {
                targetBand.setUnit(Unit.INTENSITY);
            }

            bandList.add(targetBand);
            targetProduct.addBand(targetBand);
        }
        return bandList.toArray(new Band[bandList.size()]);
    }

    public static void catchOperatorException(String opName, final Throwable e) throws OperatorException {
        String message = "";
        if (e.getMessage() != null) {
            message += e.getMessage();
        }
        if (e.getCause() != null && e.getCause().getMessage() != null && !e.getCause().getMessage().equals(e.getMessage())) {
            message += " due to " + e.getCause().getMessage();
        } else if (e.getMessage() == null || e.getMessage().isEmpty()) {
            message += e.toString();
            if (e.getCause() != null) {
                message += " due to " + e.getCause().toString();
            }
        }

        System.out.println(message);
        throw new OperatorException(message);
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @param sourceProduct The input source product.
     * @return geoBoundary The object to pass back the max/min lat/lon.
     * @throws OperatorException for no geocoding
     */
    public static ImageGeoBoundary computeImageGeoBoundary(final Product sourceProduct) throws OperatorException {
        final ImageGeoBoundary geoBoundary = new ImageGeoBoundary();
        final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("Product does not contain a geocoding");
        }
        final GeoPos geoPosFirstNear = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
        final GeoPos geoPosFirstFar = geoCoding.getGeoPos(new PixelPos(sourceProduct.getSceneRasterWidth() - 0.5f, 0.5f), null);
        final GeoPos geoPosLastNear = geoCoding.getGeoPos(new PixelPos(0.5f, sourceProduct.getSceneRasterHeight() - 0.5f), null);
        final GeoPos geoPosLastFar = geoCoding.getGeoPos(new PixelPos(sourceProduct.getSceneRasterWidth() - 0.5f,
                sourceProduct.getSceneRasterHeight() - 0.5f), null);

        final double[] lats = {geoPosFirstNear.getLat(), geoPosFirstFar.getLat(), geoPosLastNear.getLat(), geoPosLastFar.getLat()};
        final double[] lons = {geoPosFirstNear.getLon(), geoPosFirstFar.getLon(), geoPosLastNear.getLon(), geoPosLastFar.getLon()};

        geoBoundary.latMin = 90.0;
        geoBoundary.latMax = -90.0;
        for (double lat : lats) {
            if (lat < geoBoundary.latMin) {
                geoBoundary.latMin = lat;
            }
            if (lat > geoBoundary.latMax) {
                geoBoundary.latMax = lat;
            }
        }

        geoBoundary.lonMin = 180.0;
        geoBoundary.lonMax = -180.0;
        for (double lon : lons) {
            if (lon < geoBoundary.lonMin) {
                geoBoundary.lonMin = lon;
            }
            if (lon > geoBoundary.lonMax) {
                geoBoundary.lonMax = lon;
            }
        }

        if (geoBoundary.lonMax - geoBoundary.lonMin >= 180) {
            geoBoundary.lonMin = 360.0;
            geoBoundary.lonMax = 0.0;
            for (double lon : lons) {
                if (lon < 0) {
                    lon += 360;
                }
                if (lon < geoBoundary.lonMin) {
                    geoBoundary.lonMin = lon;
                }
                if (lon > geoBoundary.lonMax) {
                    geoBoundary.lonMax = lon;
                }
            }
        }

        return geoBoundary;
    }

    public static class ImageGeoBoundary {
        public double latMin = 0.0, latMax = 0.0;
        public double lonMin = 0.0, lonMax = 0.0;
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    public static void addSelectedBands(final Product sourceProduct, final String[] sourceBandNames,
                                        final Product targetProduct,
                                        final Map<String, String[]> targetBandNameToSourceBandName,
                                        final boolean outputIntensity, final boolean outputFloat) throws OperatorException {

        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames, false);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final boolean isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;

        String targetBandName;
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            String unit = srcBand.getUnit();
            if (unit == null) {
                unit = Unit.AMPLITUDE;  // assume amplitude
            }

            String targetUnit = "";

            if (unit.contains(Unit.IMAGINARY) && outputIntensity && !isPolsar) {

                throw new OperatorException("Real and imaginary bands should be selected in pairs");

            } else if (unit.contains(Unit.REAL) && outputIntensity && !isPolsar) {

                if (i == sourceBands.length - 1) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !((unit.equals(Unit.REAL) && nextUnit.equals(Unit.IMAGINARY)) ||
                        (unit.equals(Unit.IMAGINARY) && nextUnit.equals(Unit.REAL)))) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String[] srcBandNames = new String[2];
                srcBandNames[0] = srcBand.getName();
                srcBandNames[1] = sourceBands[i + 1].getName();
                targetBandName = "Intensity";
                final String suff = getSuffixFromBandName(srcBandNames[0]);
                if (suff != null) {
                    targetBandName += '_' + suff;
                }
                if (isPolsar) {
                    final String pre = getPrefixFromBandName(srcBandNames[0]);
                    targetBandName = "Intensity_" + pre;
                }
                ++i;
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                    targetUnit = Unit.INTENSITY;
                }

            } else {

                final String[] srcBandNames = {srcBand.getName()};
                targetBandName = srcBand.getName();
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                    targetUnit = unit;
                }
            }

            if (targetProduct.getBand(targetBandName) == null) {
                int dataType = srcBand.getDataType();
                if (outputFloat)
                    dataType = ProductData.TYPE_FLOAT32;
                if (outputIntensity && (dataType == ProductData.TYPE_INT8 || dataType == ProductData.TYPE_INT16))
                    dataType = ProductData.TYPE_INT32;
                if (outputIntensity && (dataType == ProductData.TYPE_UINT8 || dataType == ProductData.TYPE_UINT16))
                    dataType = ProductData.TYPE_UINT32;

                final Band targetBand = new Band(targetBandName,
                        dataType,
                        targetProduct.getSceneRasterWidth(),
                        targetProduct.getSceneRasterHeight());

                targetBand.setUnit(targetUnit);
                targetBand.setDescription(srcBand.getDescription());
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());

                targetProduct.addBand(targetBand);
            }
        }

        if(targetProduct.getNumBands() == 0) {
            throw new OperatorException("Target product has no bands");
        }
    }

    /**
     * Get an array of rectangles for all source tiles of the image
     *
     * @param sourceProduct the input
     * @param tileSize      the rect
     * @param margin        feathered area
     * @return Array of rectangles
     */
    public static Rectangle[] getAllTileRectangles(final Product sourceProduct, final Dimension tileSize,
                                                   final int margin) {

        if(tileSize.getHeight() > sourceProduct.getSceneRasterHeight() || tileSize.getWidth() > sourceProduct.getSceneRasterWidth()) {
            return new Rectangle[] { new Rectangle(0,0, sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight())};
        }

        final int rasterHeight = sourceProduct.getSceneRasterHeight() - margin - margin;
        final int rasterWidth = sourceProduct.getSceneRasterWidth() - margin - margin;

        final Rectangle boundary = new Rectangle(margin, margin, rasterWidth, rasterHeight);

        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);

        final Rectangle[] rectangles = new Rectangle[tileCountX * tileCountY];
        int index = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                final Rectangle tileRectangle = new Rectangle(tileX * tileSize.width + margin,
                        tileY * tileSize.height + margin,
                        tileSize.width,
                        tileSize.height);
                final Rectangle intersection = boundary.intersection(tileRectangle);
                rectangles[index] = intersection;
                index++;
            }
        }
        return rectangles;
    }


    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @param sourceProducts the list of input products
     * @param scnProp        the output scene properties
     */
    public static void computeImageGeoBoundary(final Product[] sourceProducts, final SceneProperties scnProp) {

        scnProp.latMin = 90.0f;
        scnProp.latMax = -90.0f;
        scnProp.lonMin = 180.0f;
        scnProp.lonMax = -180.0f;

        for (final Product srcProd : sourceProducts) {
            final GeoCoding geoCoding = srcProd.getSceneGeoCoding();
            final GeoPos geoPosFirstNear = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos geoPosFirstFar = geoCoding.getGeoPos(new PixelPos(srcProd.getSceneRasterWidth() - 1, 0), null);
            final GeoPos geoPosLastNear = geoCoding.getGeoPos(new PixelPos(0, srcProd.getSceneRasterHeight() - 1), null);
            final GeoPos geoPosLastFar = geoCoding.getGeoPos(new PixelPos(srcProd.getSceneRasterWidth() - 1,
                    srcProd.getSceneRasterHeight() - 1), null);

            final double[] lats = {geoPosFirstNear.getLat(), geoPosFirstFar.getLat(), geoPosLastNear.getLat(), geoPosLastFar.getLat()};
            final double[] lons = {geoPosFirstNear.getLon(), geoPosFirstFar.getLon(), geoPosLastNear.getLon(), geoPosLastFar.getLon()};
            scnProp.srcCornerLatitudeMap.put(srcProd, lats);
            scnProp.srcCornerLongitudeMap.put(srcProd, lons);

            for (double lat : lats) {
                if (lat < scnProp.latMin) {
                    scnProp.latMin = (float)lat;
                }
                if (lat > scnProp.latMax) {
                    scnProp.latMax = (float)lat;
                }
            }

            for (double lon : lons) {
                if (lon < scnProp.lonMin) {
                    scnProp.lonMin = (float)lon;
                }
                if (lon > scnProp.lonMax) {
                    scnProp.lonMax = (float)lon;
                }
            }
        }
    }

    public static void getSceneDimensions(final double minSpacing, final SceneProperties scnProp) {
        double minAbsLat;
        if (scnProp.latMin * scnProp.latMax > 0) {
            minAbsLat = Math.min(Math.abs(scnProp.latMin), Math.abs(scnProp.latMax)) * Constants.DTOR;
        } else {
            minAbsLat = 0.0;
        }
        double delLat = minSpacing / Constants.MeanEarthRadius * Constants.RTOD;
        double delLon = minSpacing / (Constants.MeanEarthRadius * FastMath.cos(minAbsLat)) * Constants.RTOD;
        delLat = Math.min(delLat, delLon);
        delLon = delLat;

        scnProp.sceneWidth = (int) ((scnProp.lonMax - scnProp.lonMin) / delLon) + 1;
        scnProp.sceneHeight = (int) ((scnProp.latMax - scnProp.latMin) / delLat) + 1;
    }

    public static void addGeoCoding(final Product product, final SceneProperties scnProp) {

        final int sceneWidth = product.getSceneRasterWidth();
        final int sceneHeight = product.getSceneRasterHeight();
        final double delLon = (scnProp.lonMax - scnProp.lonMin) / (scnProp.sceneWidth - 1);
        final double delLat = (scnProp.latMax - scnProp.latMin) / (scnProp.sceneHeight - 1);

        final int gridWidth = 11;
        final int gridHeight = 11;
        final int subSamplingX = (sceneWidth - 1) / (gridWidth - 1);
        final int subSamplingY = (sceneHeight - 1) / (gridHeight - 1);
        final float[] latTiePoints = new float[gridWidth*gridHeight];
        final float[] lonTiePoints = new float[gridWidth*gridHeight];

        for (int i = 0; i < gridHeight; ++i) {
            for (int j = 0; j < gridWidth; ++j) {
                latTiePoints[i*gridHeight + j] = (float)(scnProp.latMax - i*subSamplingY*delLat);
                lonTiePoints[i*gridHeight + j] = (float)(scnProp.lonMin + j*subSamplingX*delLon);
                System.out.print(lonTiePoints[i*gridHeight + j]);
                System.out.print(" ");
            }
            System.out.println();
        }

        final TiePointGrid latGrid = new TiePointGrid("latitude", gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, latTiePoints);
        latGrid.setUnit(Unit.DEGREES);

        final TiePointGrid lonGrid = new TiePointGrid("longitude", gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, lonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
    }

    public static class SceneProperties {
        public int sceneWidth, sceneHeight;
        public float latMin, lonMin, latMax, lonMax;

        public final Map<Product, double[]> srcCornerLatitudeMap = new HashMap<>(10);
        public final Map<Product, double[]> srcCornerLongitudeMap = new HashMap<>(10);
    }

}
