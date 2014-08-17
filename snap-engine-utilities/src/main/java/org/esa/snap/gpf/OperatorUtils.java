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
package org.esa.snap.gpf;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.util.ExceptionLog;

import java.awt.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param sourceProduct    The source product.
     * @param tiePointGridName The tie point grid name.
     * @return srcTPG The incidence angle tie point grid.
     */
    private static TiePointGrid getTiePointGrid(final Product sourceProduct, final String tiePointGridName) {

        for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
            final TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
            if (srcTPG.getName().equals(tiePointGridName)) {
                return srcTPG;
            }
        }

        return null;
    }

    /**
     * Get incidence angle tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The incidence angle tie point grid.
     */
    public static TiePointGrid getIncidenceAngle(final Product sourceProduct) {

        return getTiePointGrid(sourceProduct, TPG_INCIDENT_ANGLE);
    }

    /**
     * Get slant range time tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The slant range time tie point grid.
     */
    public static TiePointGrid getSlantRangeTime(final Product sourceProduct) {

        return getTiePointGrid(sourceProduct, TPG_SLANT_RANGE_TIME);
    }

    /**
     * Get latitude tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The latitude tie point grid.
     */
    public static TiePointGrid getLatitude(final Product sourceProduct) {

        return getTiePointGrid(sourceProduct, TPG_LATITUDE);
    }

    /**
     * Get longitude tie point grid.
     *
     * @param sourceProduct The source product.
     * @return srcTPG The longitude tie point grid.
     */
    public static TiePointGrid getLongitude(final Product sourceProduct) {

        return getTiePointGrid(sourceProduct, TPG_LONGITUDE);
    }

    public static String getBandPolarization(final String bandName, final MetadataElement absRoot) {
        final String pol = getPolarizationFromBandName(bandName);
        if (pol != null) {
            return pol;
        } else {
            final String[] mdsPolar = getProductPolarization(absRoot);
            return mdsPolar[0];
        }
    }

    public static String getPolarizationFromBandName(final String bandName) {

        // Account for possibilities like "x_HH_dB" or "x_HH_times_VV_conj"
        // where the last one will return an exception because it appears to contain
        // multiple polarizations
        String pol = "";
        final String bandNameLower = bandName.toLowerCase();
        if (bandNameLower.contains("_hh"))
            pol += "hh";
        if (bandNameLower.contains("_vv"))
            pol += "vv";
        if (bandNameLower.contains("_hv"))
            pol += "hv";
        if (bandNameLower.contains("_vh"))
            pol += "vh";

        if (pol.length() == 2)
            return pol;
        else if (pol.length() > 2)
            throw new OperatorException("Band name contains multiple polarziations: " + pol);

        return null;
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

    public static void copyVirtualBand(final Product product, final VirtualBand srcBand, final String name) {

        final VirtualBand virtBand = new VirtualBand(name,
                srcBand.getDataType(),
                srcBand.getSceneRasterWidth(),
                srcBand.getSceneRasterHeight(),
                srcBand.getExpression());
        virtBand.setUnit(srcBand.getUnit());
        virtBand.setDescription(srcBand.getDescription());
        virtBand.setNoDataValue(srcBand.getNoDataValue());
        virtBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
        product.addBand(virtBand);
    }

    public static boolean isDIMAP(final Product prod) {
        return StringUtils.contains(prod.getProductReader().getReaderPlugIn().getFormatNames(),
                DimapProductConstants.DIMAP_FORMAT_NAME);
    }

    public static boolean isMapProjected(final Product product) {
        if (product.getGeoCoding() instanceof MapGeoCoding || product.getGeoCoding() instanceof CrsGeoCoding)
            return true;
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        return absRoot != null && !absRoot.getAttributeString(AbstractMetadata.map_projection, "").trim().isEmpty();
    }

    public static boolean isComplex(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE, "").trim();
            if (sampleType.equalsIgnoreCase("complex"))
                return true;
        }
        return false;
    }

    public static boolean isQuadPol(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            final String pol1 = absRoot.getAttributeString(AbstractMetadata.mds1_tx_rx_polar, "").trim();
            final String pol2 = absRoot.getAttributeString(AbstractMetadata.mds2_tx_rx_polar, "").trim();
            final String pol3 = absRoot.getAttributeString(AbstractMetadata.mds3_tx_rx_polar, "").trim();
            final String pol4 = absRoot.getAttributeString(AbstractMetadata.mds4_tx_rx_polar, "").trim();
            if (!pol1.isEmpty() && !pol2.isEmpty() && !pol3.isEmpty() && !pol4.isEmpty())
                return true;
        }
        return false;
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

    public static void createNewTiePointGridsAndGeoCoding(
            final Product sourceProduct, final Product targetProduct,
            final int gridWidth, final int gridHeight,
            final float subSamplingX, final float subSamplingY,
            final PixelPos[] newTiePointPos) {

        TiePointGrid latGrid = null;
        TiePointGrid lonGrid = null;

        for (TiePointGrid srcTPG : sourceProduct.getTiePointGrids()) {

            final float[] tiePoints = new float[gridWidth * gridHeight];
            for (int k = 0; k < newTiePointPos.length; k++) {
                tiePoints[k] = srcTPG.getPixelFloat(newTiePointPos[k].x, newTiePointPos[k].y);
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

            targetProduct.addTiePointGrid(tgtTPG);

            if (srcTPG.getName().equals(TPG_LATITUDE)) {
                latGrid = tgtTPG;
            } else if (srcTPG.getName().equals(TPG_LONGITUDE)) {
                lonGrid = tgtTPG;
            }
        }

        final TiePointGeoCoding gc = new TiePointGeoCoding(latGrid, lonGrid);

        targetProduct.setGeoCoding(gc);
    }

    /**
     * get the selected bands
     *
     * @param sourceProduct   the input product
     * @param sourceBandNames the select band names
     * @return band list
     * @throws OperatorException if source band not found
     */
    public static Band[] getSourceBands(final Product sourceProduct, String[] sourceBandNames) throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (!(band instanceof VirtualBand))
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        final Band[] sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final String sourceBandName = sourceBandNames[i];
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand == null) {
                throw new OperatorException("Source band not found: " + sourceBandName);
            }
            sourceBands[i] = sourceBand;
        }
        return sourceBands;
    }

    public static Band[] addBands(final Product targetProduct, final String[] targetBandNameList, final String suffix) {
        final List<Band> bandList = new ArrayList<Band>(targetBandNameList.length);
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
        if (opName.contains("$"))
            opName = opName.substring(0, opName.indexOf('$'));
        String message = opName + ": ";
        if (e.getMessage() != null)
            message += e.getMessage();
        else
            message += e.toString();

        if (Boolean.getBoolean("sendErrorOnException")) {
            ExceptionLog.log(message);
        }

        System.out.println(message);
        throw new OperatorException(message);
    }


    // mosaic and createStack scene functions

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @param sourceProducts the list of input products
     * @param scnProp        the output scene properties
     */
    public static void computeImageGeoBoundary(final Product[] sourceProducts, final SceneProperties scnProp) {

        scnProp.latMin = 90.0;
        scnProp.latMax = -90.0;
        scnProp.lonMin = 180.0;
        scnProp.lonMax = -180.0;

        for (final Product srcProd : sourceProducts) {
            final GeoCoding geoCoding = srcProd.getGeoCoding();
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
                    scnProp.latMin = lat;
                }
                if (lat > scnProp.latMax) {
                    scnProp.latMax = lat;
                }
            }

            for (double lon : lons) {
                if (lon < scnProp.lonMin) {
                    scnProp.lonMin = lon;
                }
                if (lon > scnProp.lonMax) {
                    scnProp.lonMax = lon;
                }
            }
        }
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
        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
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

    public static void getSceneDimensions(final double minSpacing, final SceneProperties scnProp) {
        double minAbsLat;
        if (scnProp.latMin * scnProp.latMax > 0) {
            minAbsLat = Math.min(Math.abs(scnProp.latMin), Math.abs(scnProp.latMax)) * org.esa.beam.util.math.MathUtils.DTOR;
        } else {
            minAbsLat = 0.0;
        }
        double delLat = minSpacing / Constants.MeanEarthRadius * org.esa.beam.util.math.MathUtils.RTOD;
        double delLon = minSpacing / (Constants.MeanEarthRadius * Math.cos(minAbsLat)) * org.esa.beam.util.math.MathUtils.RTOD;
        delLat = Math.min(delLat, delLon);
        delLon = delLat;

        scnProp.sceneWidth = (int) ((scnProp.lonMax - scnProp.lonMin) / delLon) + 1;
        scnProp.sceneHeight = (int) ((scnProp.latMax - scnProp.latMin) / delLat) + 1;
    }

    /**
     * Add geocoding to the target product.
     *
     * @param targetProduct the destination product
     * @param scnProp       the scene properties
     */
    public static void addGeoCoding(final Product targetProduct, final SceneProperties scnProp) {

        final float[] latTiePoints = {(float) scnProp.latMax, (float) scnProp.latMax,
                (float) scnProp.latMin, (float) scnProp.latMin};
        final float[] lonTiePoints = {(float) scnProp.lonMin, (float) scnProp.lonMax,
                (float) scnProp.lonMin, (float) scnProp.lonMax};

        final int gridWidth = 10;
        final int gridHeight = 10;

        final float[] fineLatTiePoints = new float[gridWidth * gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, latTiePoints, fineLatTiePoints);

        final float subSamplingX = (float) targetProduct.getSceneRasterWidth() / (gridWidth - 1);
        final float subSamplingY = (float) targetProduct.getSceneRasterHeight() / (gridHeight - 1);

        final TiePointGrid latGrid = new TiePointGrid(TPG_LATITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, fineLatTiePoints);
        latGrid.setUnit(Unit.DEGREES);

        final float[] fineLonTiePoints = new float[gridWidth * gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, lonTiePoints, fineLonTiePoints);

        final TiePointGrid lonGrid = new TiePointGrid(TPG_LONGITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, fineLonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.setGeoCoding(tpGeoCoding);
    }

    public static class ImageGeoBoundary {
        public double latMin = 0.0, latMax = 0.0;
        public double lonMin = 0.0, lonMax = 0.0;
    }

    public static class SceneProperties {
        public int sceneWidth, sceneHeight;
        public double latMin, lonMin, latMax, lonMax;

        public final Map<Product, double[]> srcCornerLatitudeMap = new HashMap<Product, double[]>(10);
        public final Map<Product, double[]> srcCornerLongitudeMap = new HashMap<Product, double[]>(10);
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

        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames);

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

            if (unit.contains(Unit.PHASE) && outputIntensity) {
                continue;

            } else if (unit.contains(Unit.IMAGINARY) && outputIntensity && !isPolsar) {

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
                final String pol = getBandPolarization(srcBandNames[0], absRoot);
                if (pol != null && !pol.isEmpty() && !isPolsar && !targetBandName.toLowerCase().contains(pol)) {
                    targetBandName += '_' + pol.toUpperCase();
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
                final String pol = getBandPolarization(targetBandName, absRoot);
                if (pol != null && !pol.isEmpty() && !isPolsar && !targetBandName.toLowerCase().contains(pol)) {
                    targetBandName += '_' + pol.toUpperCase();
                }
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
}
