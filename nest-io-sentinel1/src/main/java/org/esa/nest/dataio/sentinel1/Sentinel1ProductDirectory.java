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
package org.esa.nest.dataio.sentinel1;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.dataio.XMLProductDirectory;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.AbstractMetadataIO;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This class represents a product directory.
 *
 */
public class Sentinel1ProductDirectory extends XMLProductDirectory {

    private final transient Map<String, String> imgBandMetadataMap = new HashMap<String, String>(4);
    private Sentinel1OCNReader OCNReader = null;
    private String acqMode = "";

    public Sentinel1ProductDirectory(final File headerFile, final File imageFolder) {
        super(headerFile, imageFolder);
    }

    protected void addImageFile(final File file) throws IOException {
        final String name = file.getName().toLowerCase();
        if (name.endsWith("tiff")) {
            final ImageIOFile img = new ImageIOFile(file);
            bandImageFileMap.put(img.getName(), img);
        } else if(name.endsWith(".nc")) {
            if(OCNReader == null )
                OCNReader = new Sentinel1OCNReader(this);
            OCNReader.addImageFile(file, name);
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

            String tpgPrefix = "";
            String suffix = pol;
            if(isSLC() && isTOPSAR()) {
                suffix = swath +'_'+ pol;
                tpgPrefix = swath;
            }

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
                        AbstractMetadata.addBandToBandMap(bandMetadata, bandName);

                        if(real)
                            lastRealBand = band;
                        else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_'+suffix);
                            ReaderUtils.createVirtualPhaseBand(product, lastRealBand, band, '_'+suffix);
                        }
                        real = !real;

                        // add tiepointgrids and geocoding for band
                        addTiePointGrids(band, imgName, tpgPrefix);
                    }
                } else {
                    for(int b=0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude" +'_'+suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_INT32, width, height);
                        band.setUnit(Unit.AMPLITUDE);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(img, i, b));
                        AbstractMetadata.addBandToBandMap(bandMetadata, bandName);

                        ReaderUtils.createVirtualIntensityBand(product, band, '_'+suffix);

                        // add tiepointgrids and geocoding for band
                        addTiePointGrids(band, imgName, tpgPrefix);
                    }
                }
            }
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final Product product, final MetadataElement root) throws IOException {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(product);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement XFDU = origProdRoot.getElement("XFDU");
        final MetadataElement informationPackageMap = XFDU.getElement("informationPackageMap");
        final MetadataElement contentUnit = informationPackageMap.getElement("contentUnit");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        final String descriptor = contentUnit.getAttributeString("textInfo", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,  descriptor);
        product.setDescription(descriptor);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,  "right");

        final MetadataElement metadataSection = XFDU.getElement("metadataSection");
        final MetadataElement[] metadataObjectList = metadataSection.getElements();

        for(MetadataElement metadataObject : metadataObjectList) {
            final String id = metadataObject.getAttributeString("ID", defStr);
            if(id.endsWith("Annotation") || id.endsWith("Schema")) {
                // continue;
            } else if(id.equals("processing")) {
                final MetadataElement processing = findElement(metadataObject, "processing");
                final MetadataElement facility = processing.getElement("facility");
                final MetadataElement software = facility.getElement("software");
                final String org = facility.getAttributeString("organisation");
                final String name = software.getAttributeString("name");
                final String version = software.getAttributeString("version");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, org+' '+name+' '+version);

                final ProductData.UTC start = getTime(processing, "start");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, start);
            } else if(id.equals("acquisitionPeriod")) {
                final MetadataElement acquisitionPeriod = findElement(metadataObject, "acquisitionPeriod");
                final ProductData.UTC startTime = getTime(acquisitionPeriod, "startTime");
                final ProductData.UTC stopTime = getTime(acquisitionPeriod, "stopTime");
                product.setStartTime(startTime);
                product.setEndTime(stopTime);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

            } else if(id.equals("platform")) {
                final MetadataElement platform = findElement(metadataObject, "platform");
                String missionName = platform.getAttributeString("familyName", "Sentinel-1");
                final String number = platform.getAttributeString("number", defStr);
                if(!missionName.equals("ENVISAT"))
                    missionName += number;
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, missionName);

                final MetadataElement instrument = platform.getElement("instrument");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SWATH, instrument.getAttributeString("swath", defStr));
                acqMode = instrument.getAttributeString("mode", defStr);
                if(acqMode == null || acqMode.equals(defStr)) {
                    final MetadataElement extensionElem = instrument.getElement("extension");
                    if(extensionElem != null)  {
                        final MetadataElement instrumentModeElem = extensionElem.getElement("instrumentMode");
                        if(instrumentModeElem != null)
                            acqMode = instrumentModeElem.getAttributeString("mode", defStr);
                    }
                }
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, acqMode);
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
                MetadataElement generalProductInformation = findElement(metadataObject, "generalProductInformation");
                if(generalProductInformation == null)
                    generalProductInformation = findElement(metadataObject, "standAloneProductInformation");

                String productType = "unknown";
                if(OCNReader != null) {
                    productType = "OCN";
                } else {
                    if(generalProductInformation != null)
                        productType = generalProductInformation.getAttributeString("productType", defStr);
                }
                product.setProductType(productType);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
                if(productType.contains("SLC")) {
                    setSLC(true);
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, "COMPLEX");
                } else {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, "DETECTED");
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 1);
                }
            }
        }

        // get metadata for each band
        addBandAbstractedMetadata(product, absRoot, origProdRoot);
        addCalibrationAbstractedMetadata(origProdRoot);
        addNoiseAbstractedMetadata(origProdRoot);

        determineProductDimensions(product, absRoot);
    }

    private void determineProductDimensions(final Product product, final MetadataElement absRoot) throws IOException {
        int width = 0, height = 0;
        int totalWidth = 0, totalHeight = 0;
        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            final String imgName = img.getName().toLowerCase();
            final String bandMetadataName = imgBandMetadataMap.get(imgName);
            if(bandMetadataName == null)
                throw new IOException("Metadata for measurement dataset "+imgName+" not found");
            final MetadataElement bandMetadata = absRoot.getElement(bandMetadataName);

            width = bandMetadata.getAttributeInt(AbstractMetadata.num_samples_per_line);
            height = bandMetadata.getAttributeInt(AbstractMetadata.num_output_lines);
            totalWidth += width;
            totalHeight += height;
        }
        if(isSLC() && isTOPSAR()) {  // approximate does not account for overlap
            product.setSceneDimensions(totalWidth, totalHeight);
        } else {
            product.setSceneDimensions(width, height);
        }
    }

    private void addBandAbstractedMetadata(final Product product, final MetadataElement absRoot,
                                           final MetadataElement origProdRoot) throws IOException {

        MetadataElement annotationElement = origProdRoot.getElement("annotation");
        if(annotationElement == null) {
            annotationElement = new MetadataElement("annotation");
            origProdRoot.addElement(annotationElement);
        }
        final File annotationFolder = new File(getBaseDir(), "annotation");
        final File[] files = annotationFolder.listFiles();
        if(files == null && OCNReader != null) {
            // add netcdf metadata for OCN products
            OCNReader.addNetCDFMetadata(product, annotationElement);
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

            final ProductData.UTC startTime = getTime(adsHeader, "startTime");
            final ProductData.UTC stopTime = getTime(adsHeader, "stopTime");

            final String bandRootName = AbstractMetadata.BAND_PREFIX+swath +'_'+ pol;
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

            if (swath.contains("1")) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                        imageInformation.getAttributeDouble("slantRangeTime")* Constants.halfLightSpeed);
            }

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

                if(!isTOPSAR() || !isSLC()) {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                            imageInformation.getAttributeInt("numberOfLines"));
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                            imageInformation.getAttributeInt("numberOfSamples"));
                }

                addOrbitStateVectors(absRoot, generalAnnotation.getElement("orbitList"));
                addSRGRCoefficients(absRoot, prodElem.getElement("coordinateConversion"));
                addDopplerCentroidCoefficients(absRoot, prodElem.getElement("dopplerCentroid"));

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

        MetadataElement noiseElement = origProdRoot.getElement("noise");
        if(noiseElement == null) {
            noiseElement = new MetadataElement("noise");
            origProdRoot.addElement(noiseElement);
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
                noiseElement.addElement(nameElem);
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

    private static void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement orbitList) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitList.getElements();
        for(int i=1; i <= stateVectorElems.length; ++i) {
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i-1], i);
        }

        // set state vector time
        if(absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.NO_METADATA_UTC).
                equalElems(AbstractMetadata.NO_METADATA_UTC)) {

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                ReaderUtils.getTime(stateVectorElems[0], "time", AbstractMetadata.dateFormat));
        }
    }

    private static void addVector(final String name, final MetadataElement orbitVectorListElem,
                                  final MetadataElement orbitElem, final int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name+num);

        final MetadataElement positionElem = orbitElem.getElement("position");
        final MetadataElement velocityElem = orbitElem.getElement("velocity");

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                ReaderUtils.getTime(orbitElem, "time", AbstractMetadata.dateFormat));

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                positionElem.getAttributeDouble("x", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                positionElem.getAttributeDouble("y", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                positionElem.getAttributeDouble("z", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                velocityElem.getAttributeDouble("x", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                velocityElem.getAttributeDouble("y", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                velocityElem.getAttributeDouble("z", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    private static void addSRGRCoefficients(final MetadataElement absRoot, final MetadataElement coordinateConversion) {
        if(coordinateConversion == null) return;
        final MetadataElement coordinateConversionList = coordinateConversion.getElement("coordinateConversionList");
        if(coordinateConversionList == null) return;

        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);

        int listCnt = 1;
        for(MetadataElement elem : coordinateConversionList.getElements()) {
            final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list+'.'+listCnt);
            srgrCoefficientsElem.addElement(srgrListElem);
            ++listCnt;

            final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "azimuthTime", AbstractMetadata.dateFormat);
            srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);

            final double grOrigin = elem.getAttributeDouble("gr0", 0);
            AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                    ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
            AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, grOrigin);

            final String coeffStr = elem.getElement("grsrCoefficients").getAttributeString("grsrCoefficients", "");
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

    private static void addDopplerCentroidCoefficients(
            final MetadataElement absRoot, final MetadataElement dopplerCentroid) {
        if(dopplerCentroid == null) return;
        final MetadataElement dcEstimateList = dopplerCentroid.getElement("dcEstimateList");
        if(dcEstimateList == null) return;

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for(MetadataElement elem : dcEstimateList.getElements()) {
            final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list+'.'+listCnt);
            dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
            ++listCnt;

            final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "azimuthTime", AbstractMetadata.dateFormat);
            dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

            final double refTime = elem.getAttributeDouble("t0", 0)*1e9; // s to ns
            AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                    ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
            AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);

            final String coeffStr = elem.getElement("geometryDcPolynomial").getAttributeString("geometryDcPolynomial", "");
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

    @Override
    protected void addGeoCoding(final Product product) {

        TiePointGrid latGrid = product.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
        TiePointGrid lonGrid = product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);
        if (latGrid != null && lonGrid != null) {
            setLatLongMetadata(product, latGrid, lonGrid);
            return;
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        int numOfSubSwath;
        switch (acquisitionMode) {
            case "IW":
                numOfSubSwath = 3;
                break;
            case "EW":
                numOfSubSwath = 5;
                break;
            default:
                return;
        }

        String[] bandNames = product.getBandNames();
        Band firstSWBand = null, lastSWBand = null;
        boolean firstSWBandFound = false, lastSWBandFound = false;
        for (String bandName:bandNames) {
            if (!firstSWBandFound && bandName.contains(acquisitionMode + 1)) {
                firstSWBand = product.getBand(bandName);
                firstSWBandFound = true;
            }

            if (!lastSWBandFound && bandName.contains(acquisitionMode + numOfSubSwath)) {
                lastSWBand = product.getBand(bandName);
                lastSWBandFound = true;
            }
        }
        if(firstSWBand == null && lastSWBand == null)
            return;

        final GeoCoding firstSWBandGeoCoding = firstSWBand.getGeoCoding();
        final int firstSWBandHeight = firstSWBand.getRasterHeight();

        final GeoCoding lastSWBandGeoCoding = lastSWBand.getGeoCoding();
        final int lastSWBandWidth = lastSWBand.getRasterWidth();
        final int lastSWBandHeight = lastSWBand.getRasterHeight();

        final PixelPos ulPix = new PixelPos(0, 0);
        final PixelPos llPix = new PixelPos(0, firstSWBandHeight - 1);
        final GeoPos ulGeo = new GeoPos();
        final GeoPos llGeo = new GeoPos();
        firstSWBandGeoCoding.getGeoPos(ulPix, ulGeo);
        firstSWBandGeoCoding.getGeoPos(llPix, llGeo);

        final PixelPos urPix = new PixelPos(lastSWBandWidth - 1, 0);
        final PixelPos lrPix = new PixelPos(lastSWBandWidth - 1, lastSWBandHeight - 1);
        final GeoPos urGeo = new GeoPos();
        final GeoPos lrGeo = new GeoPos();
        lastSWBandGeoCoding.getGeoPos(urPix, urGeo);
        lastSWBandGeoCoding.getGeoPos(lrPix, lrGeo);

        final float[] latCorners = {ulGeo.getLat(), urGeo.getLat(), llGeo.getLat(), lrGeo.getLat()};
        final float[] lonCorners = {ulGeo.getLon(), urGeo.getLon(), llGeo.getLon(), lrGeo.getLon()};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, ulGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, ulGeo.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, urGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, urGeo.getLon());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, llGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, llGeo.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lrGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lrGeo.getLon());
    }

    @Override
    protected void addTiePointGrids(final Product product) {
        // replaced by call to addTiePointGrids(band)
    }

    private static void addTiePointGrids(final Band band, final String imgXMLName, final String tpgPrefix) {

        final Product product = band.getProduct();
        String pre = "";
        if(!tpgPrefix.isEmpty())
            pre = tpgPrefix+'_';

        final TiePointGrid testTPG = product.getTiePointGrid(pre+OperatorUtils.TPG_LATITUDE);
        if(testTPG != null)
            return;

        final String annotation = FileUtils.exchangeExtension(imgXMLName, ".xml");
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
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
        final int[] x = new int[geoGrid.length];
        final int[] y = new int[geoGrid.length];

        int gridWidth = 0, gridHeight = 0;
        int i=0;
        for(MetadataElement ggPoint : geoGrid) {
            latList[i] = (float)ggPoint.getAttributeDouble("latitude", 0);
            lngList[i] = (float)ggPoint.getAttributeDouble("longitude", 0);
            incidenceAngleList[i] = (float)ggPoint.getAttributeDouble("incidenceAngle", 0);
            elevAngleList[i] = (float)ggPoint.getAttributeDouble("elevationAngle", 0);
            rangeTimeList[i] = (float)(ggPoint.getAttributeDouble("slantRangeTime", 0)*Constants.oneBillion); // s to ns

            x[i] = (int)ggPoint.getAttributeDouble("pixel", 0);
            y[i] = (int)ggPoint.getAttributeDouble("line", 0);
            if(x[i] == 0) {
                if(gridWidth == 0)
                    gridWidth = i;
                ++gridHeight;
            }
            ++i;
        }

        final int newGridWidth = gridWidth;
        final int newGridHeight = gridHeight;
        final float[] newLatList = new float[newGridWidth*newGridHeight];
        final float[] newLonList = new float[newGridWidth*newGridHeight];
        final float[] newIncList = new float[newGridWidth*newGridHeight];
        final float[] newElevList = new float[newGridWidth*newGridHeight];
        final float[] newslrtList = new float[newGridWidth*newGridHeight];
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        final float subSamplingX = (float)sceneRasterWidth / (newGridWidth - 1);
        final float subSamplingY = (float)sceneRasterHeight / (newGridHeight - 1);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, latList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLatList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, lngList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLonList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, incidenceAngleList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newIncList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, elevAngleList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newElevList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, rangeTimeList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newslrtList);

        TiePointGrid latGrid = product.getTiePointGrid(pre+OperatorUtils.TPG_LATITUDE);
        if(latGrid == null) {
            latGrid = new TiePointGrid(pre+OperatorUtils.TPG_LATITUDE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLatList);
            latGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(latGrid);
        }

        TiePointGrid lonGrid = product.getTiePointGrid(pre+OperatorUtils.TPG_LONGITUDE);
        if(lonGrid == null) {
            lonGrid = new TiePointGrid(pre+OperatorUtils.TPG_LONGITUDE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLonList, TiePointGrid.DISCONT_AT_180);
            lonGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(lonGrid);
        }

        if(product.getTiePointGrid(pre+OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final TiePointGrid incidentAngleGrid = new TiePointGrid(pre+OperatorUtils.TPG_INCIDENT_ANGLE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newIncList);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

        if(product.getTiePointGrid(pre+OperatorUtils.TPG_ELEVATION_ANGLE) == null) {
            final TiePointGrid elevAngleGrid = new TiePointGrid(pre+OperatorUtils.TPG_ELEVATION_ANGLE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newElevList);
            elevAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(elevAngleGrid);
        }

        if(product.getTiePointGrid(pre+OperatorUtils.TPG_SLANT_RANGE_TIME) == null) {
            final TiePointGrid slantRangeGrid = new TiePointGrid(pre+OperatorUtils.TPG_SLANT_RANGE_TIME,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newslrtList);
            slantRangeGrid.setUnit(Unit.NANOSECONDS);
            product.addTiePointGrid(slantRangeGrid);
        }

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        band.setGeoCoding(tpGeoCoding);

        //setLatLongMetadata(product, latGrid, lonGrid);
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

    private boolean isTOPSAR() {
        return acqMode.equals("IW") || acqMode.equals("EW");
    }

    @Override
    protected String getProductName() {
        String name = getBaseDir().getName();
        if(name.toUpperCase().endsWith(".SAFE"))
            return name.substring(0, name.length()-5);
        return name;
    }

    protected String getProductType() {
        if(OCNReader != null)
            return "Level-2 OCN";
        return "Level-1";
    }

    private static void getListInEvenlySpacedGrid(
            final int sceneRasterWidth, final int sceneRasterHeight, final int sourceGridWidth,
            final int sourceGridHeight, final int [] x, final int [] y, final float[] sourcePointList,
            final int targetGridWidth, final int targetGridHeight, final float subSamplingX, final float subSamplingY,
            final float[] targetPointList) {

        if (sourcePointList.length != sourceGridWidth*sourceGridHeight) {
            throw new IllegalArgumentException(
                    "Original tie point array size does not match 'sourceGridWidth' x 'sourceGridHeight'");
        }

        if (targetPointList.length != targetGridWidth*targetGridHeight) {
            throw new IllegalArgumentException(
                    "Target tie point array size does not match 'targetGridWidth' x 'targetGridHeight'");
        }

        int k = 0;
        for (int r = 0; r < targetGridHeight; r++) {

            if (r == targetGridHeight - 1) {
                System.out.println();
            }
            float newY = r*subSamplingY;
            if (newY > sceneRasterHeight - 1) {
                newY = sceneRasterHeight - 1;
            }
            float oldY0 = 0, oldY1 = 0;
            int j0 = 0, j1 = 0;
            for (int rr = 1; rr < sourceGridHeight; rr++) {
                j0 = rr - 1;
                j1 = rr;
                oldY0 = y[j0*sourceGridWidth];
                oldY1 = y[j1*sourceGridWidth];
                if (oldY1 > newY) {
                    break;
                }
            }

            final float wj = (newY - oldY0)/(oldY1 - oldY0);

            for (int c = 0; c < targetGridWidth; c++) {

                float newX = c*subSamplingX;
                if (newX > sceneRasterWidth - 1) {
                    newX = sceneRasterWidth - 1;
                }
                float oldX0 = 0, oldX1 = 0;
                int i0 = 0, i1 = 0;
                for (int cc = 1; cc < sourceGridWidth; cc++) {
                    i0 = cc - 1;
                    i1 = cc;
                    oldX0 = x[i0];
                    oldX1 = x[i1];
                    if (oldX1 > newX) {
                        break;
                    }
                }
                final float wi = (newX - oldX0)/(oldX1 - oldX0);

                targetPointList[k++] = MathUtils.interpolate2D(wi, wj,
                        sourcePointList[i0 + j0 * sourceGridWidth],
                        sourcePointList[i1 + j0 * sourceGridWidth],
                        sourcePointList[i0 + j1 * sourceGridWidth],
                        sourcePointList[i1 + j1 * sourceGridWidth]);
            }
        }
    }
	
	public static ProductData.UTC getTime(final MetadataElement elem, final String tag) {

        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, Sentinel1Constants.sentinelDateFormat);
    }

    @Override
    public Product createProduct() throws IOException {

        final Product product = new Product(getProductName(),
                                            getProductType(),
                                            sceneWidth, sceneHeight);

        addMetaData(product);
        addTiePointGrids(product); // empty

        addBands(product, sceneWidth, sceneHeight);
        addGeoCoding(product);

        product.setName(getProductName());
        product.setProductType(getProductType());
        product.setDescription(getProductDescription());

        AbstractMetadata.setAttribute(AbstractMetadata.getAbstractedMetadata(product),
                AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        return product;
    }
}