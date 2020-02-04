/*
 * Copyright (C) 2020 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.saocom;

import Jama.Matrix;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.Maths;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a product directory.
 */
public class SaocomProductDirectory extends XMLProductDirectory {

    private final File headerFile;
    private String productName = null;
    private String productType = null;
    private String productDescription = "";
    private int width, height;

    private final double[] latCorners = new double[4];
    private final double[] lonCorners = new double[4];
    private final double[] slantRangeCorners = new double[4];
    private final double[] incidenceCorners = new double[4];

    private static final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Map<String, ImageInputStream> imageBandMap = new HashMap<>();
    private final List<Product> bandProducts = new ArrayList<>();

    public SaocomProductDirectory(final File inputFile) {
        super(inputFile);
        headerFile = inputFile;
    }

    @Override
    public void close() throws IOException {
        super.close();
        for (Product bandProduct : bandProducts) {
            if (bandProduct != null) {
                bandProduct.dispose();
            }
        }
        bandProducts.clear();
    }

    @Override
    protected String getHeaderFileName() {
        if (ZipUtils.isZip(headerFile)) {
            return ""; //todo
        } else {
            return headerFile.getName();
        }
    }

    @Override
    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        final Element rootElement = xmlDoc.getRootElement();
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);
        AbstractMetadataIO.AddXMLMetadata(rootElement, origProdRoot);

        productName = headerFile.getName().replace(".xemt", "");
        addMetadataFiles(getRootFolder() + productName + "/Data", origProdRoot);

        addAbstractedMetadataHeader(root);

        return root;
    }

    private void addMetadataFiles(final String internalPath, final MetadataElement destElem) throws IOException {
        if (!productDir.exists(internalPath))
            return;
        String[] metaFiles = productDir.list(internalPath);
        for (String file : metaFiles) {
            if (file.endsWith(".xml")) {
                try {
                    if (productType == null) {
                        productType = file.substring(0, file.indexOf("-")).toUpperCase();
                    }

                    final File metaFile = getFile(internalPath + "/" + file);
                    final Document xmlDoc = XMLSupport.LoadXML(metaFile.getAbsolutePath());
                    final Element metaFileElement = xmlDoc.getRootElement();

                    AbstractMetadataIO.AddXMLMetadata(metaFileElement, destElem);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Unable to read metadata " + file);
                }
            }
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement xemtElem = origProdRoot.getElement("xemt");
        final MetadataElement product = xemtElem.getElement("product");
        final MetadataElement features = product.getElement("features");

        final MetadataElement imageAttributes = features.getElement("imageAttributes");
        final MetadataElement bands = imageAttributes.getElement("bands");
        final MetadataElement[] bandElems = bands.getElements();

        width = bandElems[0].getAttributeInt("nCols", defInt);
        height = bandElems[0].getAttributeInt("nRows", defInt);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, width);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, height);

        final MetadataElement acquisition = features.getElement("acquisition");
        final MetadataElement parameters = acquisition.getElement("parameters");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, getAcquisitionMode(parameters.getAttributeString("acqMode")));

        final MetadataElement scene = features.getElement("scene");
        final MetadataElement timeFrame = scene.getElement("timeFrame");
        final MetadataElement timePeriod = timeFrame.getElement("timePeriod");
        setStartStopTime(absRoot, timePeriod, height);

        final MetadataElement production = features.getElement("production");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, production.getAttributeString("facilityID"));

        final MetadataElement StateVectorData = features.getElement("StateVectorData");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, StateVectorData.getAttributeString("OrbitDirection"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latCorners[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonCorners[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latCorners[1]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonCorners[1]);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latCorners[2]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonCorners[2]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latCorners[3]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonCorners[3]);


    }

    private void setStartStopTime(final MetadataElement absRoot, final MetadataElement elem, final int height) {
        final ProductData.UTC startTime = ReaderUtils.getTime(elem, "startTime", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(elem, "endTime", standardDateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, stopTime, height));
    }

    private static String getAcquisitionMode(final String mode) {
        if (mode.equalsIgnoreCase("SM"))
            return "Stripmap";
        else if (mode.equalsIgnoreCase("TW"))
            return "TOPSARWide";
        else if (mode.equalsIgnoreCase("TN"))
            return "TOPSARNarrow";
        else if (mode.equalsIgnoreCase("SL"))
            return "Spotlight";
        else if (mode.equalsIgnoreCase("SC"))
            return "ScanSAR";
        return " ";
    }

    @Override
    protected String getRelativePathToImageFolder() {
        return getRootFolder() + productName + "/Data/";
    }

    @Override
    protected void addImageFile(final String imgPath, final MetadataElement newRoot) {
        if (!imgPath.toLowerCase().endsWith(".xml")) {
            try {
                final Dimension bandDimensions = new Dimension(width, height);
                final InputStream inStream = getInputStream(imgPath);
                if (inStream.available() > 0) {
                    final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);

                    final ImageIOFile img = new ImageIOFile(imgPath, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 1, ProductData.TYPE_INT32, productInputFile);
                    bandImageFileMap.put(img.getName(), img);

                    ProductReader reader = geoTiffPlugIn.createReaderInstance();
                    bandProducts.add(reader.readProductNodes(productDir.getFile(imgPath), null));
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(imgPath + " not found");
            }
        }
    }

    @Override
    protected void addBands(final Product product) {
        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            int numImages = img.getNumImages();

            final String pol = SARReader.findPolarizationInBandName(img.getName());
            String suffix = pol;
            if (isSLC()) {
                numImages *= 2; // real + imaginary
            }

            String bandName;
            boolean real = true;
            Band lastRealBand = null;
            for (int i = 0; i < numImages; ++i) {

                if (isSLC()) {
                    String unit;

                    for (int b = 0; b < img.getNumBands(); ++b) {
                        if (real) {
                            bandName = "i" + '_' + suffix;
                            unit = Unit.REAL;
                        } else {
                            bandName = "q" + '_' + suffix;
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(unit);
                        band.setNoDataValueUsed(true);
                        //band.setNoDataValue(NoDataValue);

                        product.addBand(band);
                        final ImageIOFile.BandInfo bandInfo = new ImageIOFile.BandInfo(band, img, 0, b);
                        bandMap.put(band, bandInfo);

                        if (real) {
                            lastRealBand = band;
                        } else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_' + suffix);
                            bandInfo.setRealBand(lastRealBand);
                            bandMap.get(lastRealBand).setImaginaryBand(band);
                        }
                        real = !real;
                    }
                } else {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude" + '_' + suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(Unit.AMPLITUDE);
                        band.setNoDataValueUsed(true);
                        //band.setNoDataValue(NoDataValue);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        SARReader.createVirtualIntensityBand(product, band, '_' + suffix);
                    }
                }
            }
        }

        if(!bandProducts.isEmpty()) {
            ProductUtils.copyGeoCoding(bandProducts.get(0), product);
        }
    }

    @Override
    protected void addGeoCoding(final Product product) {

        if(product.getSceneGeoCoding() != null) {
            return;
        }

        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);

        if (isMapProjected() || sampleType.contains("COMPLEX")) {

            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

        } else {

            final boolean isAscending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
            final double[] flippedLatCorners = new double[4];
            final double[] flippedLonCorners = new double[4];
            if (isAscending) { // flip up and down
                flippedLatCorners[0] = latCorners[2];
                flippedLatCorners[1] = latCorners[3];
                flippedLatCorners[2] = latCorners[0];
                flippedLatCorners[3] = latCorners[1];

                flippedLonCorners[0] = lonCorners[2];
                flippedLonCorners[1] = lonCorners[3];
                flippedLonCorners[2] = lonCorners[0];
                flippedLonCorners[3] = lonCorners[1];

            } else { // flip left and right

                flippedLatCorners[0] = latCorners[1];
                flippedLatCorners[1] = latCorners[0];
                flippedLatCorners[2] = latCorners[3];
                flippedLatCorners[3] = latCorners[2];

                flippedLonCorners[0] = lonCorners[1];
                flippedLonCorners[1] = lonCorners[0];
                flippedLonCorners[2] = lonCorners[3];
                flippedLonCorners[3] = lonCorners[2];
            }

            ReaderUtils.addGeoCoding(product, flippedLatCorners, flippedLonCorners);
        }
    }

    @Override
    protected void addTiePointGrids(final Product product) {

        final int gridWidth = 4;
        final int gridHeight = 4;
        final double subSamplingX = (double) product.getSceneRasterWidth() / (gridWidth - 1);
        final double subSamplingY = (double) product.getSceneRasterHeight() / (gridHeight - 1);
        if (subSamplingX == 0 || subSamplingY == 0)
            return;

        final float[] flippedSlantRangeCorners = new float[4];
        final float[] flippedIncidenceCorners = new float[4];
        getFlippedCorners(product, flippedSlantRangeCorners, flippedIncidenceCorners);

        if (product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final float[] fineAngles = new float[gridWidth * gridHeight];
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, flippedIncidenceCorners, fineAngles);

            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                    subSamplingX, subSamplingY, fineAngles);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

        final float[] fineSlantRange = new float[gridWidth * gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, flippedSlantRangeCorners, fineSlantRange);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineSlantRange);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);
    }

    private void getFlippedCorners(Product product,
                                   final float[] flippedSlantRangeCorners, final float[] flippedIncidenceCorners) {

        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);

        if (isMapProjected() || sampleType.contains("COMPLEX")) {

            flippedSlantRangeCorners[0] = (float) slantRangeCorners[0];
            flippedSlantRangeCorners[1] = (float) slantRangeCorners[1];
            flippedSlantRangeCorners[2] = (float) slantRangeCorners[2];
            flippedSlantRangeCorners[3] = (float) slantRangeCorners[3];

            flippedIncidenceCorners[0] = (float) incidenceCorners[0];
            flippedIncidenceCorners[1] = (float) incidenceCorners[1];
            flippedIncidenceCorners[2] = (float) incidenceCorners[2];
            flippedIncidenceCorners[3] = (float) incidenceCorners[3];

        } else {

            final boolean isAscending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
            if (isAscending) { // flip up and down
                flippedSlantRangeCorners[0] = (float) slantRangeCorners[2];
                flippedSlantRangeCorners[1] = (float) slantRangeCorners[3];
                flippedSlantRangeCorners[2] = (float) slantRangeCorners[0];
                flippedSlantRangeCorners[3] = (float) slantRangeCorners[1];

                flippedIncidenceCorners[0] = (float) incidenceCorners[2];
                flippedIncidenceCorners[1] = (float) incidenceCorners[3];
                flippedIncidenceCorners[2] = (float) incidenceCorners[0];
                flippedIncidenceCorners[3] = (float) incidenceCorners[1];

            } else { // flip left and right

                flippedSlantRangeCorners[0] = (float) slantRangeCorners[1];
                flippedSlantRangeCorners[1] = (float) slantRangeCorners[0];
                flippedSlantRangeCorners[2] = (float) slantRangeCorners[3];
                flippedSlantRangeCorners[3] = (float) slantRangeCorners[2];

                flippedIncidenceCorners[0] = (float) incidenceCorners[1];
                flippedIncidenceCorners[1] = (float) incidenceCorners[0];
                flippedIncidenceCorners[2] = (float) incidenceCorners[3];
                flippedIncidenceCorners[3] = (float) incidenceCorners[2];
            }
        }
    }

    private void addOrbitStateVectors(MetadataElement absRoot, MetadataElement orbitInformation) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitInformation.getElements();
        for (int i = 1; i < stateVectorElems.length; ++i) {
            // first stateVectorElem is orbitHeader therefore skip it
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i], i);
        }

        // set state vector time
        if (absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.NO_METADATA_UTC).
                equalElems(AbstractMetadata.NO_METADATA_UTC)) {

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                    ReaderUtils.getTime(stateVectorElems[1], "timeUTC", standardDateFormat));
        }
    }

    private void addVector(String name, MetadataElement orbitVectorListElem,
                           MetadataElement srcElem, int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                ReaderUtils.getTime(srcElem, "timeUTC", standardDateFormat));

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
        if (sceneInfo == null) {
            return;
        }

        final MetadataElement rangeTime = sceneInfo.getElement("rangeTime");
        if (rangeTime == null) {
            return;
        }

        final double firstPixelTime = rangeTime.getAttributeDouble("firstPixel");
        final double lastPixelTime = rangeTime.getAttributeDouble("lastPixel");

        // get slant range time to ground rang conversion coefficients
        final MetadataElement projectedImageInfo = productSpecific.getElement("projectedImageInfo");
        if (projectedImageInfo == null) {
            return;
        }

        final MetadataElement slantToGroundRangeProjection = projectedImageInfo.getElement("slantToGroundRangeProjection");
        if (slantToGroundRangeProjection == null) {
            return;
        }

        // final double validityRangeMin = slantToGroundRangeProjection.getAttributeDouble("validityRangeMin");
        // final double validityRangeMax = slantToGroundRangeProjection.getAttributeDouble("validityRangeMax");
        final double referencePoint = slantToGroundRangeProjection.getAttributeDouble("referencePoint");
        final int polynomialDegree = slantToGroundRangeProjection.getAttributeInt("polynomialDegree");

        final double[] s2gCoef = new double[polynomialDegree + 1];
        int cnt = 0;
        for (MetadataElement elem : slantToGroundRangeProjection.getElements()) {
            s2gCoef[cnt++] = elem.getAttributeDouble("coefficient", 0);
        }

        // compute ground range to slant range conversion coefficients
        final int m = 11; // order of ground to slant polynomial
        double[] sltRgTime = new double[m + 1];
        double[] groundRange = new double[m + 1];
        for (int i = 0; i <= m; i++) {
            sltRgTime[i] = firstPixelTime + (lastPixelTime - firstPixelTime) * i / m;
            groundRange[i] = Maths.computePolynomialValue(sltRgTime[i] - referencePoint, s2gCoef);
        }

        // final double groundRangeRef = (groundRange[0] + groundRange[m]) / 2;
        final double groundRangeRef = 0.0; // set ground range ref to 0 because when g2sCoef are used in computing
        // slant range from ground range, the ground range origin is assumed to be 0
        final double[] deltaGroundRange = new double[m + 1];
        final double deltaMax = groundRange[m] - groundRangeRef;
        for (int i = 0; i <= m; i++) {
            deltaGroundRange[i] = (groundRange[i] - groundRangeRef) / deltaMax;
        }

        final Matrix G = Maths.createVandermondeMatrix(deltaGroundRange, m);
        final Matrix tau = new Matrix(sltRgTime, m + 1);
        final Matrix s = G.solve(tau);
        final double[] g2sCoef = s.getColumnPackedCopy();

        double tmp = 1;
        for (int i = 0; i <= m; i++) {
            g2sCoef[i] *= Constants.halfLightSpeed / tmp;
            tmp *= deltaMax;
        }

        // save ground range to slant range conversion coefficients in abstract metadata
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);
        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

        for (int i = 0; i <= m; i++) {
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            srgrListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, g2sCoef[i]);
        }
    }

    private void addDopplerCentroidCoefficients(
            final MetadataElement absRoot, final MetadataElement dopplerCentroid) {

        final MetadataElement[] dopplerElems = dopplerCentroid.getElements();

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for (MetadataElement dopplerEstimate : dopplerElems) {
            if (dopplerEstimate.getName().equalsIgnoreCase("dopplerEstimate")) {
                final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + '.' + listCnt);
                dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(dopplerEstimate, "timeUTC", standardDateFormat);
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
                for (MetadataElement coefficient : coefficients) {
                    final double coefValue = coefficient.getAttributeDouble("coefficient", 0);
                    final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
                    dopplerListElem.addElement(coefElem);
                    ++cnt;

                    AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                            ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
                    AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
                }
            }
        }
    }

    protected String getMission() {
        return "SAOCOM";
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
        final double lat, lon;
        final double rangeTime, incidenceAngle;

        CornerCoord(int row, int col, double lt, double ln, double range, double angle) {
            refRow = row;
            refCol = col;
            lat = lt;
            lon = ln;
            rangeTime = range;
            incidenceAngle = angle;
        }
    }
}
