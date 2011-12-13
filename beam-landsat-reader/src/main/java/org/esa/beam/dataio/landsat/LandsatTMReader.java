/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.landsat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.UTM;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * The class <code>LandsatTMReader</code> is used to create a BEAM Product
 * and decodes and loads the raster data
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
final class LandsatTMReader extends AbstractProductReader {

    private static final String LANDSAT_TM_PRODUCT = "Landsat 5 TM Product";
    private LandsatTMData landsatTM;
    private Product product;
    private File inputFile;

    public LandsatTMReader(final LandsatTMReaderPlugIn ReaderPlugIn) {
        super(ReaderPlugIn);
    }

    @Override
    protected final Product readProductNodesImpl() throws IOException {
        inputFile = LandsatTMReaderPlugIn.getInputFile(getInput());
        LandsatTMFactory landsatFact = new LandsatTMFactory(inputFile);
        landsatTM = landsatFact.createLandsatTMObject();
        initReader();
        return product;
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                                       int sourceWidth, int sourceHeight,
                                                       int sourceStepX, int sourceStepY,
                                                       Band destband,
                                                       int destOffsetX, int destOffsetY,
                                                       int destWidth, int destHeight,
                                                       ProductData destBuffer,
                                                       ProgressMonitor progressMonitor) throws IOException {
        LandsatBandReader reader = landsatTM.getBandReader(destband);
        if (reader == null) {
            throw new IOException("No band reader for band '" + destband.getName() + "' available!");  /*I18N*/
        } else {
            reader.readBandData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY,
                    destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, progressMonitor);
        }
    }

    /**
     * Creates a new product with all necessary elements (bands, geocoding, RGB-expression, metadata)
     */
    private void initReader() {
        final LandsatHeader landsatHeader = landsatTM.getHeader();
        final int height = landsatHeader.getImageHeight();
        final int width = landsatHeader.getImageWidth();
        final String formatName = landsatTM.getProductName();
        final UTC startTime = landsatHeader.getAcquisitionDate();
        product = new Product(formatName + " " + landsatHeader.getRawDate(), formatName, width, height, this);
        // @todo 3 tb/** make distinction between landsat 5 and 7 files
        product.setDescription(LANDSAT_TM_PRODUCT);
        product.setFileLocation(inputFile);
        product.setStartTime(startTime);
        product.setModified(false);
        addBands(landsatHeader, height, width, formatName);
        setMapGeoCoding();
        setMetadata();
    }

    /**
     * Adds all bands and band properties
     *
     * @param landsatHeader the header class
     * @param height
     * @param width
     * @param formatName
     */
    private void addBands(final LandsatHeader landsatHeader, final int height, final int width,
                          final String formatName) {

        final int dataType = ProductData.TYPE_UINT8;
//        List<Band> reflectanceCollection = new ArrayList<Band>();
        for (int i = 0; i < landsatHeader.getNumberOfBands(); i++) {
            final LandsatTMBand band = landsatTM.getBandAt(i);

            Band radBand = new Band("radiance_" + band.getIndex(), dataType, width, height);
            radBand.setDescription(formatName);
            radBand.setSpectralBandIndex(band.getIndex());
            radBand.setSpectralWavelength(band.getWavelength());
            radBand.setSpectralBandwidth(band.getBandwidth());
            radBand.setSolarFlux(band.getSolarFlux());
            radBand.setNoDataValue(LandsatConstants.NULL_DATA_VALUE);
            radBand.setNoDataValueUsed(true);
            radBand.clearNoDataValue();
            radBand.setScalingFactor(band.getGain());
            radBand.setScalingOffset(band.getBias());
            radBand.setUnit(LandsatConstants.Unit.RADIANCE.toString());
            product.addBand(radBand);

            //TODO refl bands removed (temporarily?)
//            Band reflBand;
//            if (band.isThermal()) {
//                final double k1 = LandsatConstants.Thermal.K1.getConstant();
//                final double k2 = LandsatConstants.Thermal.K2.getConstant();
//                reflBand = new VirtualBand("temp_" + band.getIndex(), dataType, width, height,
//                                           k2 + " / log((" + k1 + "/" + radBand.getName() + " )+1 )");
//                reflBand.setUnit(LandsatConstants.Unit.KELVIN.toString());
//            } else {
//                reflBand = new VirtualBand("refl_" + band.getIndex(), dataType, width, height,
//                                           radBand.getName() + " * PI * " +
//                                           Math.pow(landsatHeader.getEarthSunDistance(),
//                                                    2) + " / (" + band.getSolarFlux() + " * " + Math.cos(
//                                                   (90 - landsatHeader.getGeoData().getSunElevationAngle())) + " ) *" + 1000);
//                reflBand.setUnit(LandsatConstants.Unit.REFLECTANCE.toString());
//            }
//            reflectanceCollection.add(reflBand);
        }
//        for (Iterator<Band> iter = reflectanceCollection.iterator(); iter.hasNext();) {
//            final Band element = iter.next();
//            product.addBand(element);
//        }
    }


    /**
     * Set the metadata for location and radiance
     */
    private void setMetadata() {
        final List metadata = landsatTM.getMetadata();
        for (Iterator iter = metadata.iterator(); iter.hasNext(); ) {
            MetadataElement element = (MetadataElement) iter.next();
            product.getMetadataRoot().addElement(element);
        }
    }

    /**
     * Creates the UTM MapGeocoding
     */
    private void setMapGeoCoding() {
        final LandsatHeader landsatHeader = landsatTM.getHeader();
        final GeometricData geoData = landsatHeader.getGeoData();
        final float pixelSize = landsatHeader.getPixelSize();
        GeoPoint projCenter = geoData.getGeoPointAt(LandsatConstants.Points.CENTER);
        float pcX = projCenter.getPixelX();
        float pcY = projCenter.getPixelY();
        final MapProjection mappro = UTM.createProjection(geoData.getMapZoneNumber() - 1,
                !projCenter.isNorthernHemisphere());
        final int width = landsatHeader.getImageWidth();
        final int height = landsatHeader.getImageHeight();
        MapInfo map = new MapInfo(mappro, pcX, pcY, (float) projCenter.getEasting(),
                (float) projCenter.getNorthing(), pixelSize, pixelSize, Datum.WGS_84);
        map.setSceneWidth(width);
        map.setSceneHeight(height);
        map.setOrientation(geoData.getLookAngle() * -1);
        final MapGeoCoding mapGeocoding = new MapGeoCoding(map);
        product.setGeoCoding(mapGeocoding);
    }

    /**
     * Close all open input streams
     *
     * @throws IOException
     */
    @Override
    public final void close() throws
            IOException {
        super.close();
        landsatTM.close();
        landsatTM = null;
        product = null;
    }
}
