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
package org.esa.nest.dataio.terrasarx;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.FileImageInputStreamExtImpl;
import org.esa.nest.dataio.XMLProductDirectory;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.Constants;
import org.esa.nest.util.XMLSupport;
import org.jdom.Element;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class represents a product directory.
 *
 */
public class TerraSarXProductDirectory extends XMLProductDirectory {

    private String productName = "TerraSar-X";
    private String productType = "TerraSar-X";
    private String productDescription = "";

    private final float[] latCorners = new float[4];
    private final float[] lonCorners = new float[4];
    private final float[] slantRangeCorners = new float[4];
    private final float[] incidenceCorners = new float[4];

    private final List<File> cosarFileList = new ArrayList<File>(1);
    private final Map<String, ImageInputStream> cosarBandMap = new HashMap<String, ImageInputStream>(1);

    public TerraSarXProductDirectory(final File headerFile, final File imageFolder) {
        super(headerFile, imageFolder);
    }

    @Override
    protected void addAbstractedMetadataHeader(final Product product, final MetadataElement root) throws IOException {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(product);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement level1Elem = origProdRoot.getElementAt(0);
        final MetadataElement generalHeader = level1Elem.getElement("generalHeader");
        final MetadataElement productInfo = level1Elem.getElement("productInfo");
        final MetadataElement productSpecific = level1Elem.getElement("productSpecific");
        final MetadataElement missionInfo = productInfo.getElement("missionInfo");
        final MetadataElement productVariantInfo = productInfo.getElement("productVariantInfo");
        final MetadataElement imageDataInfo = productInfo.getElement("imageDataInfo");
        final MetadataElement sceneInfo = productInfo.getElement("sceneInfo");
        final MetadataElement processing = level1Elem.getElement("processing");
        final MetadataElement instrument = level1Elem.getElement("instrument");
        final MetadataElement platform = level1Elem.getElement("platform");
        final MetadataElement complexImageInfo = productSpecific.getElement("complexImageInfo");
        final MetadataElement geocodedImageInfo = productSpecific.getElement("geocodedImageInfo");

        MetadataAttribute attrib = generalHeader.getAttribute("fileName");
        if(attrib != null)
            productName = attrib.getData().getElemString().replace("_____", "_").replace("__", "_");
        if(productName.endsWith(".xml"))
                productName = productName.substring(0, productName.length()-4);
        
        //mph
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, productName);
        productType = productVariantInfo.getAttributeString("productType", defStr).replace("_____", "_").replace("__", "_");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                generalHeader.getAttributeString("itemName", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "TSX");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(generalHeader, "generationTime", AbstractMetadata.dateFormat));

        MetadataElement elem = generalHeader.getElement("generationSystem");
        if(elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                elem.getAttributeString("generationSystem", defStr));
        }

        if(missionInfo != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, missionInfo.getAttributeInt("orbitCycle", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, missionInfo.getAttributeInt("relOrbit", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, missionInfo.getAttributeInt("absOrbit", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, missionInfo.getAttributeString("orbitDirection", defStr));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, imageDataInfo.getAttributeString("imageDataType", defStr));
        }

        final MetadataElement acquisitionInfo = productInfo.getElement("acquisitionInfo");
        if(acquisitionInfo != null) {
            final String imagingMode = getAcquisitionMode(acquisitionInfo.getAttributeString("imagingMode", defStr));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, imagingMode);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS,
                    acquisitionInfo.getAttributeString("elevationBeamConfiguration", defStr));
            productDescription = productType +' '+ imagingMode;

            if(missionInfo == null) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, acquisitionInfo.getAttributeString("orbitDirection", defStr));   
            }
        }

        final MetadataElement polarisationList = acquisitionInfo.getElement("polarisationList");
        final MetadataAttribute[] polList = polarisationList.getAttributes();
        for(int i=0; i < polList.length; ++i) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.polarTags[i], polList[i].getData().getElemString());
        }

        if(sceneInfo != null) {
            setStartStopTime(product, absRoot, sceneInfo);

            getCornerCoords(sceneInfo, geocodedImageInfo);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height,
                    sceneInfo.getAttributeDouble("sceneAverageHeight", defInt));
        } else if(acquisitionInfo != null) {
            setStartStopTime(product, absRoot, acquisitionInfo);   
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latCorners[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonCorners[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latCorners[1]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonCorners[1]);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latCorners[2]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonCorners[2]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latCorners[3]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonCorners[3]);  

        final MetadataElement imageRaster = imageDataInfo.getElement("imageRaster");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                imageRaster.getAttributeDouble("azimuthLooks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                imageRaster.getAttributeDouble("rangeLooks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                imageRaster.getAttributeInt("numberOfColumns", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                imageRaster.getAttributeInt("numberOfRows", defInt));

        // See Andrea's email dated Sept. 30, 2010
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        if(sampleType.contains("COMPLEX") && complexImageInfo != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    complexImageInfo.getAttributeDouble("projectedSpacingAzimuth", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    complexImageInfo.getElement("projectedSpacingRange").getAttributeDouble("slantRange", defInt));
        } else {
            final MetadataElement rowSpacing = imageDataInfo.getElement("imageRaster").getElement("rowSpacing");
            final MetadataElement colSpacing = imageDataInfo.getElement("imageRaster").getElement("columnSpacing");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    rowSpacing.getAttributeDouble("rowSpacing", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    colSpacing.getAttributeDouble("columnSpacing", defInt));
        }

        if(instrument != null) {
            final MetadataElement settings = instrument.getElement("settings");
            final MetadataElement settingRecord = settings.getElement("settingRecord");
            final MetadataElement PRF = settingRecord.getElement("PRF");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    PRF.getAttributeDouble("PRF", defInt));
            final MetadataElement RSF = settings.getElement("RSF");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    RSF.getAttributeDouble("RSF", defInt) / Constants.oneMillion);
            final MetadataElement radarParameters = instrument.getElement("radarParameters");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                    radarParameters.getAttributeDouble("centerFrequency", defInt) / Constants.oneMillion);
        }

        int srgr = 1;
        if(productVariantInfo.getAttributeString("projection", " ").equalsIgnoreCase("SLANTRANGE"))
            srgr = 0;
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, srgr);
        final String mapProjection = productVariantInfo.getAttributeString("mapProjection", " ").trim();
        if(!mapProjection.isEmpty()) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection, mapProjection);    
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);

        final MetadataElement processingFlags = processing.getElement("processingFlags");
        if(processingFlags != null) {
            setFlag(processingFlags, "rangeSpreadingLossCorrectedFlag", "true", absRoot, AbstractMetadata.range_spread_comp_flag);
            setFlag(processingFlags, "elevationPatternCorrectedFlag", "true", absRoot, AbstractMetadata.ant_elev_corr_flag);
        }
        
        // add Range and Azimuth bandwidth
        final MetadataElement processingParameter = processing.getElement("processingParameter");
        if(processingParameter != null) {
            final double rangeBW = processingParameter.getAttributeDouble("totalProcessedRangeBandwidth"); // Hz
            final double azimuthBW = processingParameter.getAttributeDouble("totalProcessedAzimuthBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                (Math.min(slantRangeCorners[0], slantRangeCorners[2]) / 1000000000.0) * Constants.halfLightSpeed);
        // Note: Here we use the minimum of the slant range times of two corners because the original way cause
        //       problem for stripmap product when the two slant range times are different.

        final MetadataElement calibration = level1Elem.getElement("calibration");
        if(calibration != null) {
            final MetadataElement calibrationConstant = calibration.getElement("calibrationConstant");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor,
                    calibrationConstant.getAttributeDouble("calFactor", defInt));
        }

        if(platform != null) {
            final MetadataElement orbit = platform.getElement("orbit");
            addOrbitStateVectors(absRoot, orbit);
            addSRGRCoefficients(absRoot, productSpecific, productInfo);
        }

        final MetadataElement doppler = processing.getElement("doppler");
        if(doppler != null) {
            final MetadataElement dopplerCentroid = doppler.getElement("dopplerCentroid");
            addDopplerCentroidCoefficients(absRoot, dopplerCentroid);
        }

        // handle ATI products by copying abs metadata to slv metadata
        final String antennaReceiveConfiguration = acquisitionInfo.getAttributeString("antennaReceiveConfiguration", "");
        if(antennaReceiveConfiguration.equals("DRA")) {
            final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(product);

            // copy Abstracted Metadata
            for(File cosFile : cosarFileList) {
                final String fileName = cosFile.getName().toUpperCase();
                if(fileName.contains("_SRA_"))
                    continue;
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 1);
                final MetadataElement targetSlaveMetadata = new MetadataElement(fileName);
                targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
                ProductUtils.copyMetadata(absRoot, targetSlaveMetadata);
            }

            // modify abstracted metadata
            
        }
    }

    private static void setStartStopTime(final Product product,
                                         final MetadataElement absRoot, final MetadataElement elem) {
        final ProductData.UTC startTime = ReaderUtils.getTime(elem.getElement("start"), "timeUTC", AbstractMetadata.dateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(elem.getElement("stop"), "timeUTC", AbstractMetadata.dateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
        product.setStartTime(startTime);
        product.setEndTime(stopTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, stopTime, product.getSceneRasterHeight()));
    }

    private static String getAcquisitionMode(final String mode) {
        if(mode.equalsIgnoreCase("SM"))
            return "Stripmap";
        else if(mode.equalsIgnoreCase("SL") || mode.equalsIgnoreCase("HS"))
            return "Spotlight";
        else if(mode.equalsIgnoreCase("SC"))
            return "ScanSAR";
        return " ";
    }

    private static void setFlag(MetadataElement elem, String attribTag, String trueValue,
                                MetadataElement absRoot, String absTag) {
        int val = 0;
        if(elem.getAttributeString(attribTag, " ").equalsIgnoreCase(trueValue))
            val = 1;
        AbstractMetadata.setAttribute(absRoot, absTag, val);
    }

    private void getCornerCoords(MetadataElement sceneInfo, MetadataElement geocodedImageInfo) {

        int maxRow = 0, maxCol = 0;
        int minRow = Integer.MAX_VALUE, minCol = Integer.MAX_VALUE;
        final List<CornerCoord> coordList = new ArrayList<CornerCoord>();

        final MetadataElement[] children = sceneInfo.getElements();
        for(MetadataElement child : children) {
            if(child.getName().equals("sceneCornerCoord")) {
                final int refRow = child.getAttributeInt("refRow", 0);
                final int refCol = child.getAttributeInt("refColumn", 0);

                coordList.add( new CornerCoord(refRow, refCol,
                                                (float)child.getAttributeDouble("lat", 0),
                                                (float)child.getAttributeDouble("lon", 0),
                                                (float)child.getAttributeDouble("rangeTime", 0) * 1000000000f,
                                                (float)child.getAttributeDouble("incidenceAngle", 0)) );

                if(refRow > maxRow) maxRow = refRow;
                if(refCol > maxCol) maxCol = refCol;
                if(refRow < minRow) minRow = refRow;
                if(refCol < minCol) minCol = refCol;
            }
        }

        int[] indexArray = {0, 1, 2, 3};
        if(minRow == maxRow && minCol == maxCol && geocodedImageInfo != null) {
            final MetadataElement geoParameter = geocodedImageInfo.getElement("geoParameter");
            final MetadataElement sceneCoordsGeographic = geoParameter.getElement("sceneCoordsGeographic");
            final float latUL = (float)sceneCoordsGeographic.getAttributeDouble("upperLeftLatitude", 0);
            final float latUR = (float)sceneCoordsGeographic.getAttributeDouble("upperRightLatitude", 0);
            final float latLL = (float)sceneCoordsGeographic.getAttributeDouble("lowerLeftLatitude", 0);
            final float latLR = (float)sceneCoordsGeographic.getAttributeDouble("lowerRightLatitude", 0);

            final float lonUL = (float)sceneCoordsGeographic.getAttributeDouble("upperLeftLongitude", 0);
            final float lonUR = (float)sceneCoordsGeographic.getAttributeDouble("upperRightLongitude", 0);
            final float lonLL = (float)sceneCoordsGeographic.getAttributeDouble("lowerLeftLongitude", 0);
            final float lonLR = (float)sceneCoordsGeographic.getAttributeDouble("lowerRightLongitude", 0);

            int k = 0;
            final double e = 1e-3;
            for(CornerCoord coord : coordList) {
                if (Math.abs(coord.lat - latUL) < e && Math.abs(coord.lon - lonUL) < e) {
                    indexArray[k] = 0;
                } else if (Math.abs(coord.lat - latUR) < e && Math.abs(coord.lon - lonUR) < e) {
                    indexArray[k] = 1;
                } else if (Math.abs(coord.lat - latLL) < e && Math.abs(coord.lon - lonLL) < e) {
                    indexArray[k] = 2;
                } else if (Math.abs(coord.lat - latLR) < e && Math.abs(coord.lon - lonLR) < e) {
                    indexArray[k] = 3;
                }
                k++;
            }
        }

        int index = 0;
        for(CornerCoord coord : coordList) {
            if(minRow == maxRow && minCol == maxCol) {
                latCorners[indexArray[index]] = coord.lat;
                lonCorners[indexArray[index]] = coord.lon;
                slantRangeCorners[indexArray[index]] = coord.rangeTime;
                incidenceCorners[indexArray[index]] = coord.incidenceAngle;
                ++index;
            } else {
                index = -1;
                if(coord.refRow == minRow) {
                    if(Math.abs(coord.refCol - minCol) < Math.abs(coord.refCol - maxCol)) {            // UL
                        index = 0;
                    } else {     // UR
                        index = 1;
                    }
                } else if(coord.refRow == maxRow) {
                    if(Math.abs(coord.refCol - minCol) < Math.abs(coord.refCol - maxCol)) {            // LL
                        index = 2;
                    } else {     // LR
                        index = 3;
                    }
                }
                if(index >= 0) {
                    latCorners[index] = coord.lat;
                    lonCorners[index] = coord.lon;
                    slantRangeCorners[index] = coord.rangeTime;
                    incidenceCorners[index] = coord.incidenceAngle;
                }
            }
        }
    }

    @Override
    protected void addImageFile(final File file) throws IOException {
        if (file.getName().toUpperCase().endsWith("COS")) {

            cosarFileList.add(file);
            setSLC(true);

            setSceneDimensionsFromXML();
        } else {
            super.addImageFile(file);
        }
    }

    private void setSceneDimensionsFromXML() throws IOException {

        final Element root = getXMLRootElement();
        final Element productInfo = XMLSupport.getElement(root, "productInfo");
        final Element imageDataInfo = XMLSupport.getElement(productInfo, "imageDataInfo");
        final Element imageRaster = XMLSupport.getElement(imageDataInfo, "imageRaster");
        final Element numRows = XMLSupport.getElement(imageRaster, "numberOfRows");
        final Element numColumns = XMLSupport.getElement(imageRaster, "numberOfColumns");

        final int height = Integer.parseInt(XMLSupport.getElementText(numRows).getValue());
        final int width = Integer.parseInt(XMLSupport.getElementText(numColumns).getValue());
        setSceneWidthHeight(width, height);
    }

    @Override
    protected void addGeoCoding(final Product product) {

        final File georefFile = new File(getBaseDir(), "ANNOTATION"+File.separator+"GEOREF.xml");
        if(georefFile.exists()) {
            try {
                readGeoRef(product, georefFile);
                return;
            } catch(Exception e) {
                //
            }
        }

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
    }

    private void readGeoRef(final Product product, final File georefFile) throws IOException {
        final org.jdom.Document xmlDoc = XMLSupport.LoadXML(georefFile.getAbsolutePath());
        final Element root = xmlDoc.getRootElement();
        final Element geoGrid = root.getChild("geolocationGrid");

        final Element numGridPnt = geoGrid.getChild("numberOfGridPoints");
        final Element numAzimuth = numGridPnt.getChild("azimuth");
        final int numAz = Integer.parseInt(numAzimuth.getValue());
        final Element numRange = numGridPnt.getChild("range");
        final int numRg = Integer.parseInt(numRange.getValue());

        final Element gridReferenceTime = geoGrid.getChild("gridReferenceTime");
        final Element tReferenceTimeUTC = gridReferenceTime.getChild("tReferenceTimeUTC");

        final int size = numAz*numRg;
        final float[] latList = new float[size];
        final float[] lonList = new float[size];
        final float[] incList = new float[size];

        final boolean flip = !isSLC();

        int i = 0;
        int r = numRg-1;
        int c = 0;
        final List<Element> grdPntList = geoGrid.getChildren("gridPoint");
        for(Element pnt : grdPntList) {
            int index = i;
            if(flip) {
                index = (numRg * c) + r;
                --r;
                if(r < 0) {
                    r = numRg-1;
                    ++c;
                }
            }

            final Element tElem = pnt.getChild("t");
            final double t = Double.parseDouble(tElem.getValue());

            final Element latElem = pnt.getChild("lat");
            latList[index] = Float.parseFloat(latElem.getValue());
            final Element lonElem = pnt.getChild("lon");
            lonList[index] = Float.parseFloat(lonElem.getValue());

            int row = -1, col = -1;
            final Element rowElem = pnt.getChild("row");
            if(rowElem != null) {
                row = Integer.parseInt(rowElem.getValue());
            }
            final Element colElem = pnt.getChild("col");
            if(colElem != null) {
                col = Integer.parseInt(colElem.getValue());
            }

            final Element incElem = pnt.getChild("inc");
            incList[index] = Float.parseFloat(incElem.getValue());

            ++i;
        }

        final int gridWidth = numRg;
        final int gridHeight = numAz;
        float subSamplingX = (float)product.getSceneRasterWidth() / (gridWidth - 1);
        float subSamplingY = (float)product.getSceneRasterHeight() / (gridHeight - 1);

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, latList);
        latGrid.setUnit(Unit.DEGREES);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, lonList, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(tpGeoCoding);

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, incList);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);
    }

    @Override
    protected void addTiePointGrids(final Product product) {

        final int gridWidth = 4;
        final int gridHeight = 4;
        final float subSamplingX = (float)product.getSceneRasterWidth() / (float)(gridWidth - 1);
        final float subSamplingY = (float)product.getSceneRasterHeight() / (float)(gridHeight - 1);
        if(subSamplingX == 0 || subSamplingY == 0)
            return;

        if(product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final float[] fineAngles = new float[gridWidth*gridHeight];
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);

            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                    subSamplingX, subSamplingY, fineAngles);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

        final float[] fineSlantRange = new float[gridWidth*gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, slantRangeCorners, fineSlantRange);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineSlantRange);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);
    }

    @Override
    protected void addBands(final Product product, final int width, final int height) {
        final Set<String> ImageKeys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : ImageKeys) {
            final ImageIOFile img = bandImageFileMap.get(key);

            for(int i=0; i < img.getNumImages(); ++i) {

                for(int b=0; b < img.getNumBands(); ++b) {
                    final String pol = ReaderUtils.findPolarizationInBandName(img.getName());
                    final Band band = new Band("Amplitude_"+pol, img.getDataType(), width, height);
                    band.setUnit(Unit.AMPLITUDE);
                    product.addBand(band);

                    ReaderUtils.createVirtualIntensityBand(product, band, '_'+pol);

                    bandMap.put(band, new ImageIOFile.BandInfo(img, i, b));
                }
            }
        }

        if(!cosarFileList.isEmpty()) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            final int h = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line, 0);
            final int w = absRoot.getAttributeInt(AbstractMetadata.num_output_lines, 0);

            final boolean polsUnique = arePolarizationsUnique();
            String extraInfo = "";         // if pols not unique add the extra info

            for (final File file : cosarFileList) {

                final String fileName = file.getName().toUpperCase();
                final String pol = ReaderUtils.findPolarizationInBandName(fileName);
                if(!polsUnique) {
                    final int polIndex = fileName.indexOf(pol);
                    extraInfo = fileName.substring(polIndex+2, fileName.indexOf(".", polIndex+3));
                }

                final Band realBand = new Band("i_"+pol+extraInfo, ProductData.TYPE_INT16, w, h);
                realBand.setUnit(Unit.REAL);
                product.addBand(realBand);

                final Band imaginaryBand = new Band("q_"+pol+extraInfo, ProductData.TYPE_INT16, w, h);
                imaginaryBand.setUnit(Unit.IMAGINARY);
                product.addBand(imaginaryBand);

                ReaderUtils.createVirtualIntensityBand(product, realBand, imaginaryBand, '_'+pol+extraInfo);
                ReaderUtils.createVirtualPhaseBand(product, realBand, imaginaryBand, '_'+pol+extraInfo);

                try {
                    cosarBandMap.put(realBand.getName(), FileImageInputStreamExtImpl.createInputStream(file));
                    cosarBandMap.put(imaginaryBand.getName(), FileImageInputStreamExtImpl.createInputStream(file));
                } catch(Exception e) {
                    //
                }
            }
        }
    }

    private boolean arePolarizationsUnique() {
        final List<String> pols = new ArrayList<String>();
        for (final File file : cosarFileList) {
            pols.add(ReaderUtils.findPolarizationInBandName(file.getName()));
        }
        for(int i=0; i < pols.size(); ++i) {
            for(int j=i+1; j < pols.size(); ++j) {
                if(pols.get(i).equals(pols.get(j)))
                   return false;
            }
        }
        return true;
    }

    private static void addOrbitStateVectors(MetadataElement absRoot, MetadataElement orbitInformation) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitInformation.getElements();
        for(int i=1; i < stateVectorElems.length; ++i) {
            // first stateVectorElem is orbitHeader therefore skip it
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i], i);
        }

        // set state vector time
        if(absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, new ProductData.UTC(0)).
                equalElems(new ProductData.UTC(0))) {

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                ReaderUtils.getTime(stateVectorElems[1], "timeUTC", AbstractMetadata.dateFormat));
        }
    }

    private static void addVector(String name, MetadataElement orbitVectorListElem,
                                  MetadataElement srcElem, int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name+num);

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                ReaderUtils.getTime(srcElem, "timeUTC", AbstractMetadata.dateFormat));

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                srcElem.getAttributeDouble("posX", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                srcElem.getAttributeDouble("posY", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                srcElem.getAttributeDouble("posZ", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                srcElem.getAttributeDouble("velX", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                srcElem.getAttributeDouble("velY", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                srcElem.getAttributeDouble("velZ", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    private static void addSRGRCoefficients(
            final MetadataElement absRoot, final MetadataElement productSpecific, final MetadataElement productInfo) {

        // get swath begin time and swath end time
        final MetadataElement sceneInfo = productInfo.getElement("sceneInfo");
        if(sceneInfo == null) {
            return;
        }

        final MetadataElement rangeTime = sceneInfo.getElement("rangeTime");
        if(rangeTime == null) {
            return;
        }

        final double firstPixelTime = rangeTime.getAttributeDouble("firstPixel");
        final double lastPixelTime = rangeTime.getAttributeDouble("lastPixel");

        // get slant range time to ground rang conversion coefficients
        final MetadataElement projectedImageInfo = productSpecific.getElement("projectedImageInfo");
        if(projectedImageInfo == null) {
            return;
        }

        final MetadataElement slantToGroundRangeProjection = projectedImageInfo.getElement("slantToGroundRangeProjection");
        if(slantToGroundRangeProjection == null) {
            return;
        }

        // final double validityRangeMin = slantToGroundRangeProjection.getAttributeDouble("validityRangeMin");
        // final double validityRangeMax = slantToGroundRangeProjection.getAttributeDouble("validityRangeMax");
        final double referencePoint = slantToGroundRangeProjection.getAttributeDouble("referencePoint");
        final int polynomialDegree = slantToGroundRangeProjection.getAttributeInt("polynomialDegree");

        final double[] s2gCoef = new double[polynomialDegree+1];
        int cnt = 0;
        for (MetadataElement elem : slantToGroundRangeProjection.getElements()) {
            s2gCoef[cnt++] = elem.getAttributeDouble("coefficient", 0);
        }

        // compute ground range to slant range conversion coefficients
        final int m = 11; // order of ground to slant polynomial
        double[] sltRgTime = new double[m+1];
        double[] groundRange = new double[m+1];
        for (int i = 0; i <= m; i++) {
            sltRgTime[i] = firstPixelTime + (lastPixelTime - firstPixelTime)*i/m;
            groundRange[i] = org.esa.nest.util.MathUtils.computePolynomialValue(sltRgTime[i] - referencePoint, s2gCoef);
        }

        // final double groundRangeRef = (groundRange[0] + groundRange[m]) / 2;
        final double groundRangeRef = 0.0; // set ground range ref to 0 because when g2sCoef are used in computing
                                           // slant range from ground range, the ground range origin is assumed to be 0
        double[] deltaGroundRange = new double[m+1];
        final double deltaMax = groundRange[m] - groundRangeRef;
        for (int i = 0; i <= m; i++) {
            deltaGroundRange[i] = (groundRange[i] - groundRangeRef) / deltaMax;
        }

        final Matrix G = org.esa.nest.util.MathUtils.createVandermondeMatrix(deltaGroundRange, m);
        final Matrix tau = new Matrix(sltRgTime, m+1);
        final Matrix s = G.solve(tau);
        double[] g2sCoef = s.getColumnPackedCopy();

        double tmp = 1;
        for (int i = 0; i <= m; i++) {
            g2sCoef[i] *= Constants.halfLightSpeed/tmp;
            tmp *= deltaMax;
        }

        // save ground range to slant range conversion coefficients in abstract metadata
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);
        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, new ProductData.UTC(0));
        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

        for (int i = 0; i <= m; i++) {
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i+1));
            srgrListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, g2sCoef[i]);
        }
    }

    private static void addDopplerCentroidCoefficients(
            final MetadataElement absRoot, final MetadataElement dopplerCentroid) {

        final MetadataElement[] dopplerElems = dopplerCentroid.getElements();

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for(MetadataElement dopplerEstimate : dopplerElems) {
            if(dopplerEstimate.getName().equalsIgnoreCase("dopplerEstimate")) {
                final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list+'.'+listCnt);
                dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(dopplerEstimate, "timeUTC", AbstractMetadata.dateFormat);
                dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

                final MetadataElement combinedDoppler = dopplerEstimate.getElement("combinedDoppler");
                final MetadataElement[] coefficients = combinedDoppler.getElements();

                /*final double refTime = elem.getElement("dopplerCentroidReferenceTime").
                       getAttributeDouble("dopplerCentroidReferenceTime", 0)*1e9; // s to ns
               AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                       ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
               AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);
                */

                int cnt = 1;
                for(MetadataElement coefficient : coefficients) {
                    final double coefValue = coefficient.getAttributeDouble("coefficient", 0);
                    final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient+'.'+cnt);
                    dopplerListElem.addElement(coefElem);
                    ++cnt;

                    AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                            ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
                    AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
                }
            }
        }
    }

    ImageInputStream getCosarImageInputStream(final Band band) {
        return cosarBandMap.get(band.getName());
    }

    @Override
    public void close() throws IOException {
        super.close();
        final Set<String> keys = cosarBandMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageInputStream img = cosarBandMap.get(key);
            img.close();
        }
    }

    @Override
    protected String getProductName() {
        return productName;
    }

    @Override
    protected String getProductDescription() {
        return productDescription;
    }

    @Override
    protected String getProductType() {
        return productType;
    }

    private static class CornerCoord {
        final int refRow, refCol;
        final float lat, lon;
        final float rangeTime, incidenceAngle;

        CornerCoord(int row, int col, float lt, float ln, float range, float angle) {
            refRow = row; refCol = col;
            lat = lt; lon = ln;
            rangeTime = range; incidenceAngle = angle;
        }
    }
}