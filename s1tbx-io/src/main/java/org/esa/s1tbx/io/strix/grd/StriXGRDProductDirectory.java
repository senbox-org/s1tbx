
package org.esa.s1tbx.io.strix.grd;

import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Map;

import static org.esa.s1tbx.io.sentinel1.Sentinel1Directory.sentinelDateFormat;

/**
 * This class represents a product directory.
 */
public class StriXGRDProductDirectory extends XMLProductDirectory {

    private final String productName;
    private String productType;
    private String pol;
    private int width, height;
    private Product bandProduct;

    private static final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public StriXGRDProductDirectory(final File inputFile) {
        super(inputFile);

        productName = inputFile.getName().replace(".xml", "");
    }

    @Override
    public void close() throws IOException  {
        super.close();
        if(bandProduct != null) {
            bandProduct.dispose();
        }
    }

    protected void addAbstractedMetadataHeader(final MetadataElement root) {
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);
        final MetadataElement EarthObservation = origProdRoot.getElement("EarthObservation");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "StriX");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());

        final MetadataElement using = EarthObservation.getElement("using");
        final MetadataElement EarthObservationEquipment = using.getElement("EarthObservationEquipment");
        final MetadataElement sensor = EarthObservationEquipment.getElement("sensor");

        final MetadataElement metaDataProperty = EarthObservation.getElement("metaDataProperty");
        final MetadataElement EarthObservationMetaData = metaDataProperty.getElement("EarthObservationMetaData");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, getMode(sensor.getAttributeString("operationalMode")));

        final MetadataElement acquisitionParameters = EarthObservationEquipment.getElement("acquisitionParameters");
        final MetadataElement acquisition = acquisitionParameters.getElement("Acquisition");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, acquisition.getAttributeString("orbitDirection"));
        pol = acquisition.getAttributeString("polarisationChannels");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, pol);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, acquisition.getAttributeDouble("acquisitionPRF"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, acquisition.getAttributeString("antennaLookDirection").toLowerCase());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, acquisition.getAttributeDouble("minimumIncidenceAngle"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, acquisition.getAttributeDouble("maximumIncidenceAngle"));

        double carrierFrequency = acquisition.containsAttribute("carrierFrequency") ?
                acquisition.getAttributeDouble("carrierFrequency") : acquisition.getAttributeDouble("carieerFrequency");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, carrierFrequency / Constants.oneMillion);

        final MetadataElement resultOf = EarthObservation.getElement("resultOf");
        final MetadataElement EarthObservationResult = resultOf.getElement("EarthObservationResult");
        final MetadataElement product = EarthObservationResult.getElement("product");
        final MetadataElement productInformation = product.getElement("ProductInformation");

        width = productInformation.getAttributeInt("numberOfPixel");
        height = productInformation.getAttributeInt("numberOfLine");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, productInformation.getAttributeInt("size"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, width);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, height);

        if(EarthObservation.containsElement("validTime")) {
            final MetadataElement validTime = EarthObservation.getElement("validTime");
            final MetadataElement timePeriod = validTime.getElement("TimePeriod");

            ProductData.UTC firstLineTime = ReaderUtils.getTime(timePeriod, "beginPosition", standardDateFormat);
            ProductData.UTC lastLineTime = ReaderUtils.getTime(timePeriod, "endPosition", standardDateFormat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, firstLineTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, lastLineTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                    ReaderUtils.getLineTimeInterval(firstLineTime, lastLineTime, height));
        } else {
            final MetadataElement vendorSpecific = EarthObservationMetaData.getElement("vendorSpecific");
            if(vendorSpecific != null) {
                for(MetadataElement specificInformation : vendorSpecific.getElements()) {
                    if(specificInformation.containsAttribute("localAttribute")) {
                        final MetadataAttribute localAttribute = specificInformation.getAttribute("localAttribute");
                        if(localAttribute.getData().getElemString().equals("sceneCenterDateTime")) {
                            final MetadataAttribute localValue = specificInformation.getAttribute("localValue");
                            final String timeStr = ReaderUtils.createValidUTCString(localValue.getData().getElemString(),
                                    new char[]{':','.','-'}, ' ').trim();
                            final ProductData.UTC centerTime =  AbstractMetadata.parseUTC(timeStr, sentinelDateFormat);
                            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, centerTime);
                            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, centerTime);
                            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, 0.0);
                        }
                    }
                }
            }
        }

        final MetadataElement processing = EarthObservationMetaData.getElement("processing");
        final MetadataElement processingInformation = processing.getElement("ProcessingInformation");

        productType = processingInformation.getAttributeString("processingLevel");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, getProductDescription());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getProductType().equals("SLC") ? "COMPLEX" : "DETECTED");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, ReaderUtils.getTime(metaDataProperty, "creationDate", standardDateFormat));
        setSLC(getProductType().equals("SLC"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                processingInformation.getAttributeString("processorName") +" "+ processingInformation.getAttributeString("processorVersion"));

        if(processingInformation.containsElement("sarProcessingParameter")) {
            final MetadataElement sarProcessingParameter = processingInformation.getElement("sarProcessingParameter");
            if(sarProcessingParameter.containsAttribute("numberOfRangeLooks")) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, sarProcessingParameter.getAttributeInt("numberOfRangeLooks"));
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, sarProcessingParameter.getAttributeInt("numberOfAzimuthLooks"));
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, sarProcessingParameter.getAttributeDouble("rangePixelSpacing"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, sarProcessingParameter.getAttributeDouble("azimuthPixelSpacing"));
        }

//        addOrbitStateVectors(absRoot, state.getElement("state_vectors"));
    }

    @Override
    protected String getProductName() {
        return productName;
    }

    @Override
    protected String getProductType() {
        return productType;
    }

    @Override
    protected String getProductDescription() {
        return getProductType().equals("SLC") ? "Single Look Complex" : "Ground Range Detected";
    }

    private String getMode(final String mode) {
        final String theMode = mode.toLowerCase();
        if(theMode.contains("strip")) {
            return "StripMap";
        } else if(theMode.contains("spot")) {
            return "Spotlight";
        } else if(theMode.contains("scan")) {
            return "ScanSAR";
        }
        return mode;
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) {
        final String name = getBandFileNameFromImage(imgPath);
        String prefix = "IMG";
        if(!productName.startsWith("PAR-"+pol)) {
            prefix += "-"+pol;
        }
        final String imageName = productName.replace("PAR", prefix);
        if ((name.endsWith("tif")) && name.startsWith(imageName) && !name.contains("preview")) {
            try {
                if(!productDir.isCompressed()) {
                    final File file = new File(getBaseDir(), imgPath);
                    if (file.exists() && file.length() > 0) {
                        final ProductReader geoTiffReader = geoTiffPlugIn.createReaderInstance();
                        bandProduct = geoTiffReader.readProductNodes(file, null);
                        bandProduct.setName(name);
                    }
                } else {
                    ImageInputStream imgStream = null;
                    final Dimension bandDimensions = new Dimension(width, height);
                    final InputStream inStream = getInputStream(imgPath);
                    if (inStream.available() > 0) {
                        imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);
                    } else {
                        inStream.close();
                    }
                    if(imgStream != null) {
                        final ImageIOFile img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                                1, 1, ProductData.TYPE_INT16, productInputFile);
                        bandImageFileMap.put(img.getName(), img);

                        ProductReader reader = geoTiffPlugIn.createReaderInstance();
                        bandProduct = reader.readProductNodes(productDir.getFile(imgPath), null);
                    }
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(imgPath + " not found");
            }
        }
    }

    @Override
    protected Dimension getProductDimensions(final MetadataElement newRoot) {
        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        int w = bandProduct.getSceneRasterWidth();
        int h = bandProduct.getSceneRasterHeight();
        absRoot.setAttributeInt(AbstractMetadata.num_samples_per_line, w);
        absRoot.setAttributeInt(AbstractMetadata.num_output_lines, h);

        return new Dimension(w, h);
    }

    @Override
    protected void addBands(final Product product) {

        if(bandImageFileMap.isEmpty()) {
            for (Band band : bandProduct.getBands()) {
                final String trgBandName = "Amplitude" + '_' + pol;
                Band trgBand = ProductUtils.copyBand(band.getName(), bandProduct, trgBandName, product, true);
                trgBand.setUnit(Unit.AMPLITUDE);
                trgBand.setNoDataValueUsed(true);
                trgBand.setNoDataValue(0);
                trgBand.setGeoCoding(band.getGeoCoding());

                SARReader.createVirtualIntensityBand(product, band, '_' + pol);
            }

            if (product.getSceneGeoCoding() == null &&
                    product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                product.setSceneGeoCoding(bandProduct.getSceneGeoCoding());
                Dimension tileSize = bandProduct.getPreferredTileSize();
                if (tileSize == null) {
                    tileSize = ImageManager.getPreferredTileSize(bandProduct);
                }
                product.setPreferredTileSize(tileSize);
                final ImageLayout imageLayout = new ImageLayout();
                imageLayout.setTileWidth(tileSize.width);
                imageLayout.setTileHeight(tileSize.height);
            }
        } else {
            for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
                final ImageIOFile img = stringImageIOFileEntry.getValue();
                int numImages = img.getNumImages();

                String suffix = pol;
                String bandName;
                for (int i = 0; i < numImages; ++i) {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude";
                        bandName += '_' + suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_INT16, width, height);
                        band.setUnit(Unit.AMPLITUDE);
                        band.setNoDataValueUsed(true);
                        band.setNoDataValue(0);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        SARReader.createVirtualIntensityBand(product, band, '_' + suffix);
                    }
                }
            }
        }
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement orbitList) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitList.getElements();
        for (int i = 1; i <= stateVectorElems.length; ++i) {
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i - 1], i);
        }

        // set state vector time
        if (absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.NO_METADATA_UTC).
                equalElems(AbstractMetadata.NO_METADATA_UTC)) {

            DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                                          ReaderUtils.getTime(stateVectorElems[0], "time", dateFormat));
        }
    }

    private void addVector(final String name, final MetadataElement orbitVectorListElem,
                           final MetadataElement orbitElem, final int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        final MetadataElement positionElem = orbitElem.getElement("position");
        final MetadataElement velocityElem = orbitElem.getElement("velocity");

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                                        ReaderUtils.getTime(orbitElem, "time", sentinelDateFormat));

        String xPosName = positionElem.containsAttribute("x") ? "x" : "position1";
        String yPosName = positionElem.containsAttribute("y") ? "y" : "position2";
        String zPosName = positionElem.containsAttribute("z") ? "z" : "position3";
        String xVelName = positionElem.containsAttribute("x") ? "x" : "velocity1";
        String yVelName = positionElem.containsAttribute("y") ? "y" : "velocity2";
        String zVelName = positionElem.containsAttribute("z") ? "z" : "velocity3";

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                positionElem.getAttributeDouble(xPosName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                positionElem.getAttributeDouble(yPosName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                positionElem.getAttributeDouble(zPosName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                velocityElem.getAttributeDouble(xVelName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                velocityElem.getAttributeDouble(yVelName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                velocityElem.getAttributeDouble(zVelName, 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    @Override
    protected void addGeoCoding(final Product product) {
        if(bandProduct != null) {
            ProductUtils.copyGeoCoding(bandProduct, product);
        }

        if(product.getSceneGeoCoding() == null) {
            try {
                final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
                final MetadataElement productMetadata = origProdRoot.getElement("ProductMetadata");
                final MetadataElement collect = productMetadata.getElement("collect");
                final MetadataElement image = collect.getElement("image");
                final MetadataElement imageGeometry = image.getElement("image_geometry");

                if(imageGeometry.containsElement("coordinate_system")) {
                    final MetadataElement coordinateSystem = imageGeometry.getElement("coordinate_system");
                    final MetadataElement geotransform = imageGeometry.getElement("geotransform");

                    double easting = geotransform.getAttributeDouble("geotransform1");
                    double northing = geotransform.getAttributeDouble("geotransform4");
                    double pixelSizeX = image.getAttributeDouble("pixel_spacing_column");
                    double pixelSizeY = image.getAttributeDouble("pixel_spacing_row");

                    final CoordinateReferenceSystem crs = CRS.parseWKT(coordinateSystem.getAttributeString("WKT"));
                    CrsGeoCoding crsGeoCoding = new CrsGeoCoding(crs, product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                            easting, northing, pixelSizeX, pixelSizeY, 0, 0);
                    product.setSceneGeoCoding(crsGeoCoding);
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe("unable to create geocoding");
            }
        }
    }

    @Override
    protected void addTiePointGrids(final Product product) {

//        final int gridWidth = 4;
//        final int gridHeight = 4;
//        final double subSamplingX = (double) product.getSceneRasterWidth() / (gridWidth - 1);
//        final double subSamplingY = (double) product.getSceneRasterHeight() / (gridHeight - 1);
//        if (subSamplingX == 0 || subSamplingY == 0)
//            return;
//
//        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
//        final MetadataElement productMetadata = origProdRoot.getElement("ProductMetadata");
//        final MetadataElement collect = productMetadata.getElement("collect");
//        final MetadataElement image = collect.getElement("image");
//        final MetadataElement centerPixel = image.getElement("center_pixel");
//        final double incidenceAngle = centerPixel.getAttributeDouble("incidence_angle");
//
//        final double[] incidenceCorners = new double[] { incidenceAngle,incidenceAngle,incidenceAngle,incidenceAngle};
//
//        if (product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
//            final float[] fineAngles = new float[gridWidth * gridHeight];
//            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);
//
//            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
//                    subSamplingX, subSamplingY, fineAngles);
//            incidentAngleGrid.setUnit(Unit.DEGREES);
//            product.addTiePointGrid(incidentAngleGrid);
//        }
//
////        final float[] fineSlantRange = new float[gridWidth * gridHeight];
////        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, flippedSlantRangeCorners, fineSlantRange);
////
////        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
////                subSamplingX, subSamplingY, fineSlantRange);
////        slantRangeGrid.setUnit(Unit.NANOSECONDS);
////        product.addTiePointGrid(slantRangeGrid);
    }
}
