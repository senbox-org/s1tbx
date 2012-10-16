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
package org.esa.nest.dataio.sentinel1;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.dataio.XMLProductDirectory;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.dataio.netcdf.*;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.AbstractMetadataIO;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.gpf.Sentinel1Utils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Element;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class represents a product directory.
 *
 */
public class Sentinel1ProductDirectory extends XMLProductDirectory {

    private final transient Map<String, String> imgBandMetadataMap = new HashMap<String, String>(4);
    private final Map<String, NetcdfFile> bandNCFileMap = new HashMap<String, NetcdfFile>(1);

    public Sentinel1ProductDirectory(final File headerFile, final File imageFolder) {
        super(headerFile, imageFolder);
    }

    protected void addImageFile(final File file) throws IOException {
        final String name = file.getName().toLowerCase();
        if ((name.endsWith("tif") || name.endsWith("tiff")) && !name.contains("browse")) {
            final ImageIOFile img = new ImageIOFile(file, ImageIOFile.getTiffIIOReader(file));
            bandImageFileMap.put(img.getName(), img);

            setSceneWidthHeight(img.getSceneWidth(), img.getSceneHeight());
        } else if(name.endsWith(".nc")) {
            final NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            readNetCDF(netcdfFile);
            bandNCFileMap.put(name, netcdfFile);
        }
    }

    private void readNetCDF(final NetcdfFile netcdfFile) {
        final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
        if (!variableListMap.isEmpty()) {
            final NcRasterDim rasterDim = NetCDFUtils.getBestRasterDim(variableListMap);

            setSceneWidthHeight(rasterDim.getDimX().getLength(), rasterDim.getDimY().getLength());
        }
    }

    @Override
    protected void addBands(final Product product, final int productWidth, final int productHeight) {

        String bandName;
        boolean real = true;
        Band lastRealBand = null;
        String unit;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            final String imgName = img.getName().toLowerCase();
            final MetadataElement bandMetadata = absRoot.getElement(imgBandMetadataMap.get(imgName));
            final String swath = bandMetadata.getAttributeString(AbstractMetadata.swath);
            final String pol = bandMetadata.getAttributeString(AbstractMetadata.polarization);
            final int width = bandMetadata.getAttributeInt(AbstractMetadata.num_samples_per_line);
            final int height = bandMetadata.getAttributeInt(AbstractMetadata.num_output_lines);

            final String suffix = swath +'_'+ pol;

            int numImages = img.getNumImages();
            if(isSLC()) {
                numImages *= 2; // real + imaginary
            }
            for(int i=0; i < numImages; ++i) {

                if(isSLC()) {
                    for(int b=0; b < img.getNumBands(); ++b) {
                        if(real) {
                            bandName = "i" + '_'+suffix;
                            unit = Unit.REAL;
                        } else {
                            bandName = "q" + '_'+suffix;
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, ProductData.TYPE_INT16, width, height);
                        band.setUnit(unit);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(img, i, b));

                        if(real)
                            lastRealBand = band;
                        else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_'+suffix);
                            ReaderUtils.createVirtualPhaseBand(product, lastRealBand, band, '_'+suffix);
                        }
                        real = !real;

                        // add geocoding for band
                        addGeoCoding(band, imgName, suffix);
                    }
                } else {
                    for(int b=0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude" +'_'+suffix;
                        final Band band = new Band(bandName, img.getDataType(), width, height);
                        band.setUnit(Unit.AMPLITUDE);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(img, i, b));

                        ReaderUtils.createVirtualIntensityBand(product, band, '_'+suffix);

                        // add geocoding for band
                        addGeoCoding(band, imgName, suffix);
                    }
                }
            }
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final Product product, final MetadataElement root) throws IOException {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(root);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement XFDU = origProdRoot.getElement("XFDU");
        final MetadataElement informationPackageMap = XFDU.getElement("informationPackageMap");
        final MetadataElement contentUnit = informationPackageMap.getElement("contentUnit");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        final String descriptor = contentUnit.getAttributeString("textInfo", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,  descriptor);
        product.setDescription(descriptor);

        final MetadataElement metadataSection = XFDU.getElement("metadataSection");
        final MetadataElement[] metadataObjectList = metadataSection.getElements();

        for(MetadataElement metadataObject : metadataObjectList) {
            final String id = metadataObject.getAttributeString("ID", defStr);
            if(id.endsWith("Annotation")) {
                // continue;
            } else if(id.equals("processing")) {
                final MetadataElement processing = findElement(metadataObject, "processing");
                final MetadataElement facility = processing.getElement("facility");
                final MetadataElement software = facility.getElement("software");
                final String org = facility.getAttributeString("organisation");
                final String name = software.getAttributeString("name");
                final String version = software.getAttributeString("version");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, org+' '+name+' '+version);
            } else if(id.equals("acquisitionPeriod")) {
                final MetadataElement acquisitionPeriod = findElement(metadataObject, "acquisitionPeriod");
                final ProductData.UTC startTime = Sentinel1Utils.getTime(acquisitionPeriod, "startTime");
                final ProductData.UTC stopTime = Sentinel1Utils.getTime(acquisitionPeriod, "stopTime");
                product.setStartTime(startTime);
                product.setEndTime(stopTime);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

            } else if(id.equals("platform")) {
                final MetadataElement platform = findElement(metadataObject, "platform");
                final String missionName = platform.getAttributeString("familyName", "Sentinel-1");
                final String number = platform.getAttributeString("number", defStr);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, missionName+number);

                final MetadataElement instrument = platform.getElement("instrument");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SWATH, instrument.getAttributeString("swath", defStr));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, instrument.getAttributeString("mode", defStr));
            } else if(id.equals("measurementOrbitReference")) {
                final MetadataElement orbitReference = findElement(metadataObject, "orbitReference");
                final MetadataElement orbitNumber = findElementContaining(orbitReference, "OrbitNumber", "type", "start");
                final MetadataElement relativeOrbitNumber = findElementContaining(orbitReference, "relativeOrbitNumber", "type", "start");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, orbitNumber.getAttributeInt("orbitNumber", defInt));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, relativeOrbitNumber.getAttributeInt("relativeOrbitNumber", defInt));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, orbitReference.getAttributeInt("cycleNumber", defInt));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, orbitReference.getAttributeString("pass", defStr));
            } else if(id.equals("measurementFrameSet")) {

            } else if(id.equals("generalProductInformation")) {
                final MetadataElement generalProductInformation = findElement(metadataObject, "generalProductInformation");
                final String productType = generalProductInformation.getAttributeString("productType", defStr);
                product.setProductType(productType);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
                if(productType.contains("SLC"))
                    setSLC(true);
            }
        }

        // get metadata for each band
        addBandAbstractedMetadata(absRoot, origProdRoot);
        addCalibrationAbstractedMetadata(origProdRoot);
        addNoiseAbstractedMetadata(origProdRoot);
    }

    private void addBandAbstractedMetadata(final MetadataElement absRoot,
                                           final MetadataElement origProdRoot) throws IOException {

        MetadataElement annotationElement = origProdRoot.getElement("annotation");
        if(annotationElement == null) {
            annotationElement = new MetadataElement("annotation");
            origProdRoot.addElement(annotationElement);
        }
        final File annotationFolder = new File(getBaseDir(), "annotation");
        final File[] files = annotationFolder.listFiles();
        if(files == null) {
            // add netcdf metadata for OCN products
            if(!bandNCFileMap.isEmpty()) {
                addNetCDFMetadata(annotationElement);
            }
            return;
        }

        // collect range and azimuth spacing
        double rangeSpacingTotal = 0;
        double azimuthSpacingTotal = 0;
        boolean commonMetadataRetrieved = false;

        int numBands = 0;
        for(File metadataFile : files) {
            if(!metadataFile.isFile())
                continue;

            org.jdom.Document xmlDoc = XMLSupport.LoadXML(metadataFile.getAbsolutePath());
            final Element rootElement = xmlDoc.getRootElement();
            final MetadataElement nameElem = new MetadataElement(metadataFile.getName());
            annotationElement.addElement(nameElem);
            AbstractMetadataIO.AddXMLMetadata(rootElement, nameElem);

            final MetadataElement prodElem = nameElem.getElement("product");
            final MetadataElement adsHeader = prodElem.getElement("adsHeader");

            final String swath = adsHeader.getAttributeString("swath");
            final String pol = adsHeader.getAttributeString("polarisation");

            final ProductData.UTC startTime = Sentinel1Utils.getTime(adsHeader, "startTime");
            final ProductData.UTC stopTime = Sentinel1Utils.getTime(adsHeader, "stopTime");

            final String bandRootName = swath +'_'+ pol;
            final MetadataElement bandAbsRoot = AbstractMetadata.addBandAbstractedMetadata(absRoot, bandRootName);
            final String imgName = FileUtils.exchangeExtension(metadataFile.getName(), ".tiff");
            imgBandMetadataMap.put(imgName, bandRootName);

            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.SWATH, swath);
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.polarization, pol);
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.annotation, metadataFile.getName());
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.first_line_time, startTime);
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.last_line_time, stopTime);

            final MetadataElement imageAnnotation = prodElem.getElement("imageAnnotation");
            final MetadataElement imageInformation = imageAnnotation.getElement("imageInformation");

            rangeSpacingTotal += imageInformation.getAttributeDouble("rangePixelSpacing");
            azimuthSpacingTotal += imageInformation.getAttributeDouble("azimuthPixelSpacing");

            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.line_time_interval,
                    imageInformation.getAttributeDouble("azimuthTimeInterval"));
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.num_samples_per_line,
                    imageInformation.getAttributeInt("numberOfSamples"));
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.num_output_lines,
                    imageInformation.getAttributeInt("numberOfLines"));
            AbstractMetadata.setAttribute(bandAbsRoot, AbstractMetadata.sample_type,
                    imageInformation.getAttributeString("pixelValue").toUpperCase());

            if(!commonMetadataRetrieved) {
                // these should be the same for all swaths
                // set to absRoot

                final MetadataElement generalAnnotation = prodElem.getElement("generalAnnotation");
                final MetadataElement productInformation = generalAnnotation.getElement("productInformation");

                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                        productInformation.getAttributeDouble("radarFrequency"));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                        imageInformation.getAttributeDouble("azimuthTimeInterval"));

                final MetadataElement processingInformation = imageAnnotation.getElement("processingInformation");
                final MetadataElement swathProcParamsList = processingInformation.getElement("swathProcParamsList");
                final MetadataElement swathProcParams = swathProcParamsList.getElement("swathProcParams");
                final MetadataElement rangeProcessing = swathProcParams.getElement("rangeProcessing");
                final MetadataElement azimuthProcessing = swathProcParams.getElement("azimuthProcessing");

                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                        rangeProcessing.getAttributeDouble("numberOfLooks"));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                        azimuthProcessing.getAttributeDouble("numberOfLooks"));

                commonMetadataRetrieved = true;
            }

            ++numBands;
        }

        // set average to absRoot
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                rangeSpacingTotal / (double)numBands);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                azimuthSpacingTotal / (double)numBands);
    }

    private void addNetCDFMetadata(final MetadataElement annotationElement) {
        Set<String> files = bandNCFileMap.keySet();
        for(String file : files) {
            final NetcdfFile netcdfFile = bandNCFileMap.get(file);
            MetadataElement bandElem = NetCDFUtils.addAttributes(annotationElement, file,
                    netcdfFile.getGlobalAttributes());

            final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
            if (!variableListMap.isEmpty()) {
                // removeQuickLooks(variableListMap);

                final NcRasterDim rasterDim = NetCDFUtils.getBestRasterDim(variableListMap);
                final Variable[] rasterVariables = NetCDFUtils.getRasterVariables(variableListMap, rasterDim);
                final Variable[] tiePointGridVariables = NetCDFUtils.getTiePointGridVariables(variableListMap, rasterVariables);
                NcVariableMap variableMap = new NcVariableMap(rasterVariables);

                for (final Variable variable : variableMap.getAll()) {
                    NetCDFUtils.addAttributes(bandElem, variable.getName(), variable.getAttributes());
                }
            }
        }
    }

    private void addCalibrationAbstractedMetadata(final MetadataElement origProdRoot) throws IOException {

        MetadataElement calibrationElement = origProdRoot.getElement("calibration");
        if(calibrationElement == null) {
            calibrationElement = new MetadataElement("calibration");
            origProdRoot.addElement(calibrationElement);
        }
        final File calFolder = new File(getBaseDir(), "annotation"+File.separator+"calibration");
        final File[] files = calFolder.listFiles();
        if(files == null) return;

        for(File metadataFile : files) {
            if(metadataFile.getName().startsWith("calibration")) {

                org.jdom.Document xmlDoc = XMLSupport.LoadXML(metadataFile.getAbsolutePath());
                final Element rootElement = xmlDoc.getRootElement();
                final String name = metadataFile.getName().replace("calibration-","");
                final MetadataElement nameElem = new MetadataElement(name);
                calibrationElement.addElement(nameElem);
                AbstractMetadataIO.AddXMLMetadata(rootElement, nameElem);
            }
        }
    }

    private void addNoiseAbstractedMetadata(final MetadataElement origProdRoot) throws IOException {

        MetadataElement calibrationElement = origProdRoot.getElement("noise");
        if(calibrationElement == null) {
            calibrationElement = new MetadataElement("noise");
            origProdRoot.addElement(calibrationElement);
        }
        final File calFolder = new File(getBaseDir(), "annotation"+File.separator+"calibration");
        final File[] files = calFolder.listFiles();
        if(files == null) return;

        for(File metadataFile : files) {
            if(metadataFile.getName().startsWith("noise")) {

                org.jdom.Document xmlDoc = XMLSupport.LoadXML(metadataFile.getAbsolutePath());
                final Element rootElement = xmlDoc.getRootElement();
                final String name = metadataFile.getName().replace("noise-","");
                final MetadataElement nameElem = new MetadataElement(name);
                calibrationElement.addElement(nameElem);
                AbstractMetadataIO.AddXMLMetadata(rootElement, nameElem);
            }
        }
    }

    private static MetadataElement findElement(final MetadataElement elem, final String name) {
        final MetadataElement metadataWrap = elem.getElement("metadataWrap");
        final MetadataElement xmlData = metadataWrap.getElement("xmlData");
        return xmlData.getElement(name);
    }

    private static MetadataElement findElementContaining(final MetadataElement parent, final String elemName,
                                                  final String attribName, final String attValue) {
        final MetadataElement[] elems = parent.getElements();
        for(MetadataElement elem : elems) {
            if(elem.getName().equalsIgnoreCase(elemName) && elem.containsAttribute(attribName)) {
                String value = elem.getAttributeString(attribName);
                if(value != null && value.equalsIgnoreCase(attValue))
                    return elem;
            }
        }
        return null;
    }
    /*
    private ProductData.UTC getTime(final MetadataElement elem, final String tag) {
        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, dateFormat);
    }
    */
    /*
    private static void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement orbitInformation) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitInformation.getElements();
        for(int i=1; i <= stateVectorElems.length; ++i) {
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i-1], i);
        }

        // set state vector time
        if(absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, new ProductData.UTC(0)).
                equalElems(new ProductData.UTC(0))) {

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                ReaderUtils.getTime(stateVectorElems[0], "timeStamp", AbstractMetadata.dateFormat));
        }
    }

    private static void addVector(String name, MetadataElement orbitVectorListElem,
                                  MetadataElement srcElem, int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name+num);

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                ReaderUtils.getTime(srcElem, "timeStamp", AbstractMetadata.dateFormat));

        final MetadataElement xpos = srcElem.getElement("xPosition");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                xpos.getAttributeDouble("xPosition", 0));
        final MetadataElement ypos = srcElem.getElement("yPosition");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                ypos.getAttributeDouble("yPosition", 0));
        final MetadataElement zpos = srcElem.getElement("zPosition");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                zpos.getAttributeDouble("zPosition", 0));
        final MetadataElement xvel = srcElem.getElement("xVelocity");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                xvel.getAttributeDouble("xVelocity", 0));
        final MetadataElement yvel = srcElem.getElement("yVelocity");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                yvel.getAttributeDouble("yVelocity", 0));
        final MetadataElement zvel = srcElem.getElement("zVelocity");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                zvel.getAttributeDouble("zVelocity", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    private static void addSRGRCoefficients(final MetadataElement absRoot, final MetadataElement imageGenerationParameters) {
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);

        int listCnt = 1;
        for(MetadataElement elem : imageGenerationParameters.getElements()) {
            if(elem.getName().equalsIgnoreCase("slantRangeToGroundRange")) {
                final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list+'.'+listCnt);
                srgrCoefficientsElem.addElement(srgrListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "zeroDopplerAzimuthTime", AbstractMetadata.dateFormat);
                srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);

                final double grOrigin = elem.getElement("groundRangeOrigin").getAttributeDouble("groundRangeOrigin", 0);
                AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                        ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
                AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, grOrigin);

                final String coeffStr = elem.getAttributeString("groundToSlantRangeCoefficients", "");
                if(!coeffStr.isEmpty()) {
                    final StringTokenizer st = new StringTokenizer(coeffStr);
                    int cnt = 1;
                    while(st.hasMoreTokens()) {
                        final double coefValue = Double.parseDouble(st.nextToken());

                        final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient+'.'+cnt);
                        srgrListElem.addElement(coefElem);
                        ++cnt;
                        AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                                ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
                        AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, coefValue);
                    }
                }
            }
        }
    }

    private static void addDopplerCentroidCoefficients(
            final MetadataElement absRoot, final MetadataElement imageGenerationParameters) {

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for(MetadataElement elem : imageGenerationParameters.getElements()) {
            if(elem.getName().equalsIgnoreCase("dopplerCentroid")) {
                final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list+'.'+listCnt);
                dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "timeOfDopplerCentroidEstimate", AbstractMetadata.dateFormat);
                dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

                final double refTime = elem.getElement("dopplerCentroidReferenceTime").
                        getAttributeDouble("dopplerCentroidReferenceTime", 0)*1e9; // s to ns
                AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                        ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
                AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);

                final String coeffStr = elem.getAttributeString("dopplerCentroidCoefficients", "");
                if(!coeffStr.isEmpty()) {
                    final StringTokenizer st = new StringTokenizer(coeffStr);
                    int cnt = 1;
                    while(st.hasMoreTokens()) {
                        final double coefValue = Double.parseDouble(st.nextToken());

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
    }            */

    @Override
    protected void addGeoCoding(final Product product) {
        // replaced by call to addGeoCoding(band)
    }

    protected static void addGeoCoding(final Band band, final String imgXMLName, final String suffix) {

        final Product product = band.getProduct();
        final String annotation = FileUtils.exchangeExtension(imgXMLName, ".xml");
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product.getMetadataRoot());
        final MetadataElement annotationElem = origProdRoot.getElement("annotation");
        final MetadataElement imgElem = annotationElem.getElement(annotation);
        final MetadataElement productElem = imgElem.getElement("product");
        final MetadataElement geolocationGrid = productElem.getElement("geolocationGrid");
        final MetadataElement geolocationGridPointList = geolocationGrid.getElement("geolocationGridPointList");

        final MetadataElement[] geoGrid = geolocationGridPointList.getElements();

        final float[] latList = new float[geoGrid.length];
        final float[] lngList = new float[geoGrid.length];
        final float[] incidenceAngleList = new float[geoGrid.length];
        final float[] elevAngleList = new float[geoGrid.length];
        final float[] rangeTimeList = new float[geoGrid.length];

        int gridWidth = 0, gridHeight = 0;
        int i=0;
        for(MetadataElement ggPoint : geoGrid) {
            latList[i] = (float)ggPoint.getAttributeDouble("latitude", 0);
            lngList[i] = (float)ggPoint.getAttributeDouble("longitude", 0);
            incidenceAngleList[i] = (float)ggPoint.getAttributeDouble("incidenceAngle", 0);
            elevAngleList[i] = (float)ggPoint.getAttributeDouble("elevationAngle", 0);
            rangeTimeList[i] = (float)ggPoint.getAttributeDouble("slantRangeTime", 0);

            final double pix = ggPoint.getAttributeDouble("pixel", 0);
            if(pix == 0) {
                if(gridWidth == 0)
                    gridWidth = i;
                ++gridHeight;
            }
            ++i;
        }

        final float subSamplingX = (float)product.getSceneRasterWidth() / (gridWidth - 1);
        final float subSamplingY = (float)product.getSceneRasterHeight() / (gridHeight - 1);
        final String pre = suffix+'_';

        TiePointGrid latGrid = product.getTiePointGrid(pre+OperatorUtils.TPG_LATITUDE);
        if(latGrid == null) {
            latGrid = new TiePointGrid(pre+OperatorUtils.TPG_LATITUDE,
                gridWidth, gridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, latList);
            latGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(latGrid);
        }

        TiePointGrid lonGrid = product.getTiePointGrid(pre+OperatorUtils.TPG_LONGITUDE);
        if(lonGrid == null) {
            lonGrid = new TiePointGrid(pre+OperatorUtils.TPG_LONGITUDE,
                gridWidth, gridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, lngList, TiePointGrid.DISCONT_AT_180);
            lonGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(lonGrid);
        }

        if(product.getTiePointGrid(pre+OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final TiePointGrid incidentAngleGrid = new TiePointGrid(pre+OperatorUtils.TPG_INCIDENT_ANGLE,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, incidenceAngleList);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

        if(product.getTiePointGrid(pre+OperatorUtils.TPG_ELEVATION_ANGLE) == null) {
            final TiePointGrid elevAngleGrid = new TiePointGrid(pre+OperatorUtils.TPG_ELEVATION_ANGLE,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, elevAngleList);
            elevAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(elevAngleGrid);
        }

        if(product.getTiePointGrid(pre+OperatorUtils.TPG_SLANT_RANGE_TIME) == null) {
            final TiePointGrid slantRangeGrid = new TiePointGrid(pre+OperatorUtils.TPG_SLANT_RANGE_TIME,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, rangeTimeList);
            slantRangeGrid.setUnit(Unit.NANOSECONDS);
            product.addTiePointGrid(slantRangeGrid);
        }

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        band.setGeoCoding(tpGeoCoding);

        setLatLongMetadata(product, latGrid, lonGrid);
    }

    private static void setLatLongMetadata(Product product, TiePointGrid latGrid, TiePointGrid lonGrid) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latGrid.getPixelFloat(w, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonGrid.getPixelFloat(w, 0));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latGrid.getPixelFloat(0, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonGrid.getPixelFloat(0, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latGrid.getPixelFloat(w, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonGrid.getPixelFloat(w, h));
    }

    @Override
    protected void addTiePointGrids(final Product product) {

    }

    @Override
    protected String getProductName() {
        String name = getBaseDir().getName();
        if(name.toUpperCase().endsWith(".SAFE"))
            return name.substring(0, name.length()-5);
        return name;
    }
}