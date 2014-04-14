/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.envisat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.cs.DefaultCartesianCS;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * The <code>EnvisatProductReader</code> class is an implementation of the <code>ProductReader</code> interface
 * exclusively for data products having the standard ESA/ENVISAT raw format.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.envisat.EnvisatProductReaderPlugIn
 */
public final class EnvisatProductReader extends AbstractProductReader {

    /**
     * @since BEAM 4.9
     */
    private static final String SYSPROP_ENVISAT_USE_PIXEL_GEO_CODING = "beam.envisat.usePixelGeoCoding";

    /**
     * Represents the product's file.
     */
    private ProductFile productFile;

    /**
     * The width of the raster covering the full scene.
     */
    private int sceneRasterWidth;
    /**
     * The height of the raster covering the full scene.
     */
    private int sceneRasterHeight;

    private Map<Band, BandLineReader> bandlineReaderMap;

    /**
     * Constructs a new ENVISAT product reader.
     *
     * @param readerPlugIn the plug-in which created this reader instance
     */
    public EnvisatProductReader(EnvisatProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public ProductFile getProductFile() {
        return productFile;
    }

    public int getSceneRasterWidth() {
        return sceneRasterWidth;
    }

    public int getSceneRasterHeight() {
        return sceneRasterHeight;
    }


    /**
     * Reads a data product and returns a in-memory representation of it. This method was called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @throws java.lang.IllegalArgumentException
     *                             if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object input = getInput();
        if (input instanceof String || input instanceof File) {
            File file = new File(input.toString());
            try {
                productFile = ProductFile.open(file);
            } catch (IOException e) {
                final InputStream inputStream;
                try {
                    inputStream = EnvisatProductReaderPlugIn.getInflaterInputStream(file);
                } catch (IOException ignored) {
                    throw e;
                }
                productFile = ProductFile.open(file, new FileCacheImageInputStream(inputStream, null));
            }
        } else if (input instanceof ImageInputStream) {
            productFile = ProductFile.open((ImageInputStream) input);
        } else if (input instanceof ProductFile) {
            productFile = (ProductFile) input;
        }

        Debug.assertNotNull(productFile);
        sceneRasterWidth = productFile.getSceneRasterWidth();
        sceneRasterHeight = productFile.getSceneRasterHeight();

        if (getSubsetDef() != null) {
            Dimension s = getSubsetDef().getSceneRasterSize(sceneRasterWidth, sceneRasterHeight);
            sceneRasterWidth = s.width;
            sceneRasterHeight = s.height;
        }

        return createProduct();
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (productFile != null) {
            productFile.close();
            productFile = null;
        }
        super.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final BandLineReader bandLineReader = bandlineReaderMap.get(destBand);
        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = Math.min(destBand.getRasterWidth() - 1, sourceMinX + sourceWidth - 1);
        final int sourceMaxY = sourceMinY + sourceHeight - 1;


        pm.beginTask("Reading band '" + destBand.getName() + "'...", (sourceMaxY - sourceMinY) + 1);
        // For each scan in the data source
        try {

            int destArrayPos = 0;
            for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                bandLineReader.readRasterLine(sourceMinX, sourceMaxX, sourceStepX,
                                              sourceY,
                                              destBuffer, destArrayPos);

                destArrayPos += destWidth;
                pm.worked(sourceStepY);
            }
            pm.worked(1);
        } finally {
            pm.done();
        }

    }

    private Product createProduct() throws IOException {
        Debug.assertNotNull(getProductFile());

        File file = getProductFile().getFile();
        String productName;
        if (file != null) {
            productName = file.getName();
        } else {
            productName = getProductFile().getProductId();
        }
        productName = FileUtils.createValidFilename(productName);

        Product product = new Product(productName,
                                      getProductFile().getProductType(),
                                      getSceneRasterWidth(),
                                      getSceneRasterHeight(),
                                      this);

        product.setFileLocation(getProductFile().getFile());
        product.setDescription(getProductFile().getProductDescription());
        product.setStartTime(getProductFile().getSceneRasterStartTime());
        product.setEndTime(getProductFile().getSceneRasterStopTime());
        product.setAutoGrouping(getProductFile().getAutoGroupingPattern());

        addBandsToProduct(product);
        if (!isMetadataIgnored()) {
            addHeaderAnnotationsToProduct(product);
            addDatasetAnnotationsToProduct(product);
            addTiePointGridsToProduct(product);
            addGeoCodingToProduct(product);
            initPointingFactory(product);
        }
        addDefaultMasksToProduct(product);
        addDefaultMasksDefsToBands(product);
        productFile.addCustomMetadata(product);

        return product;
    }

    private void addBandsToProduct(Product product) {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(product);

        BandLineReader[] bandLineReaders = productFile.getBandLineReaders();
        bandlineReaderMap = new HashMap<>(bandLineReaders.length);
        for (BandLineReader bandLineReader : bandLineReaders) {
            if (bandLineReader.isTiePointBased()) {
                continue;
            }
            if (!(bandLineReader instanceof BandLineReader.Virtual)) {
                if (bandLineReader.getPixelDataReader().getDSD().getDatasetSize() == 0 ||
                    bandLineReader.getPixelDataReader().getDSD().getNumRecords() == 0) {
                    continue;
                }
            }
            String bandName = bandLineReader.getBandName();
            if (isNodeAccepted(bandName)) {
                BandInfo bandInfo = bandLineReader.getBandInfo();
                Band band;

                int width = bandInfo.getWidth();
                int height = bandInfo.getHeight();
                if (getSubsetDef() != null) {
                    Dimension s = getSubsetDef().getSceneRasterSize(width, height);
                    width = s.width;
                    height = s.height;
                }

                if (bandLineReader instanceof BandLineReader.Virtual) {
                    final BandLineReader.Virtual virtual = ((BandLineReader.Virtual) bandLineReader);
                    band = new VirtualBand(bandName, ProductData.TYPE_FLOAT64,//bandInfo.getDataType(),
                                           width, height,
                                           virtual.getExpression());
                } else {
                    band = new Band(bandName,
                                    bandInfo.getDataType() < ProductData.TYPE_FLOAT32 ? bandInfo.getDataType() : bandLineReader.getPixelDataField().getDataType(),
                                    width, height);
                }
                band.setScalingOffset(bandInfo.getScalingOffset());

                productFile.setInvalidPixelExpression(band);

                band.setScalingFactor(bandInfo.getScalingFactor());
                band.setLog10Scaled(bandInfo.getScalingMethod() == BandInfo.SCALE_LOG10);
                band.setSpectralBandIndex(bandInfo.getSpectralBandIndex());
                if (bandInfo.getPhysicalUnit() != null) {
                    band.setUnit(bandInfo.getPhysicalUnit());
                }
                if (bandInfo.getDescription() != null) {
                    band.setDescription(bandInfo.getDescription());
                }
                if (bandInfo.getFlagCoding() != null) {
                    product.getFlagCodingGroup().add(bandInfo.getFlagCoding());
                    band.setSampleCoding(bandInfo.getFlagCoding());
                }
                final String expression = bandInfo.getValidExpression();
                if (expression != null && expression.trim().length() > 0) {
                    band.setValidPixelExpression(expression.trim());
                }
                bandlineReaderMap.put(band, bandLineReader);
                product.addBand(band);
            }

        }
        setSpectralBandInfo(product);
    }

    private void addDefaultMasksToProduct(Product product) {
        List<Band> flagDsList = new Vector<>();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                flagDsList.add(band);
            }
        }
        if (!flagDsList.isEmpty()) {
            for (Band flagDs : flagDsList) {
                String flagDsName = flagDs.getName();
                Mask[] masks = productFile.createDefaultMasks(flagDsName);
                ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
                for (Mask mask : masks) {
                    maskGroup.add(mask);
                }
            }
        }
    }


    private void addDefaultMasksDefsToBands(Product product) {
        for (final Band band : product.getBands()) {
            final String[] maskNames = getDefaultBitmaskNames(band.getName());
            if (maskNames != null) {
                for (final String maskName : maskNames) {
                    final Mask mask = product.getMaskGroup().get(maskName);
                    if (mask != null) {
                        band.getOverlayMaskGroup().add(mask);
                    }
                }
            }
        }
    }

    private String[] getDefaultBitmaskNames(String bandName) {
        return productFile.getDefaultBitmaskNames(bandName);
    }

    private void setSpectralBandInfo(Product product) {
        float[] wavelengths = productFile.getSpectralBandWavelengths();
        float[] bandwidths = productFile.getSpectralBandBandwidths();
        float[] solar_fluxes = productFile.getSpectralBandSolarFluxes();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            int sbi = band.getSpectralBandIndex();
            if (sbi >= 0) {
                if (wavelengths != null) {
                    band.setSpectralWavelength(wavelengths[sbi % wavelengths.length]);
                }
                if (bandwidths != null) {
                    band.setSpectralBandwidth(bandwidths[sbi % bandwidths.length]);
                }
                if (solar_fluxes != null) {
                    band.setSolarFlux(solar_fluxes[sbi % solar_fluxes.length]);
                }
            }
//            Debug.trace(band.toString());
        }
    }

    private void addTiePointGridsToProduct(Product product) throws IOException {
        final BandLineReader[] bandLineReaders = getProductFile().getBandLineReaders();
        BandLineReader samplesReader = null;
        for (BandLineReader bandLineReader : bandLineReaders) {
            if (bandLineReader.isTiePointBased() && bandLineReader.getBandName().equalsIgnoreCase("samples")) {
                samplesReader = bandLineReader;
                break;
            }
        }
        for (BandLineReader bandLineReader : bandLineReaders) {
            if (bandLineReader.isTiePointBased() && bandLineReader != samplesReader) {
                TiePointGrid tiePointGrid = createTiePointGrid(bandLineReader, samplesReader);
                product.addTiePointGrid(tiePointGrid);
            }
        }
    }

    private static void addGeoCodingToProduct(Product product) {
        final String productType = product.getProductType();
        if(productType.contains("IMG")) {
            final boolean crsGeocodingCreated = initCRSGeoCoding(product);
            if(!crsGeocodingCreated)    // if insufficient metadata found to create CRSGeocoding then use tie points
                initTiePointGeoCoding(product);
        } else {
            initTiePointGeoCoding(product);
        }

        final boolean usePixeGeoCoding = Boolean.getBoolean(SYSPROP_ENVISAT_USE_PIXEL_GEO_CODING);
        if (usePixeGeoCoding) {
            Band latBand = product.getBand(EnvisatConstants.LAT_DS_NAME);
            if (latBand == null) {
                latBand = product.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME);
            }
            Band lonBand = product.getBand(EnvisatConstants.LON_DS_NAME);
            if (lonBand == null) {
                lonBand = product.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME);
            }
            if (latBand != null && lonBand != null) {
                String validMask;
                if (EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(product.getProductType()).matches()) {
                    validMask = "NOT l1_flags.INVALID";
                } else {
                    validMask = "l2_flags.LAND or l2_flags.CLOUD or l2_flags.WATER";
                }
                product.setGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, validMask, 6));
            }
        }
    }

    /**
     * Installs an Envisat-specific tie-point geo-coding in the given product.
     */
    public static void initTiePointGeoCoding(final Product product) {
        TiePointGrid latGrid = product.getTiePointGrid(EnvisatConstants.LAT_DS_NAME);
        TiePointGrid lonGrid = product.getTiePointGrid(EnvisatConstants.LON_DS_NAME);
        if (latGrid != null && lonGrid != null) {
            product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));
        }
    }

    /**
     * Creates CRS Geocoding from the metadata
     * @param product the target product
     * @return true if succeeds
     */
    private static boolean initCRSGeoCoding(final Product product) {
        try {
            final MetadataElement root = product.getMetadataRoot();
            final MetadataElement mapElem = root.getElement("MAP_PROJECTION_GADS");
            final MetadataElement sphElem = root.getElement("SPH");
            final String map_descriptor = mapElem.getAttributeString("map_descriptor").trim();
            if(map_descriptor.equalsIgnoreCase("UNIVERSAL_TRANSVERSE_MERCATOR")) {
                final int firstNearLat = sphElem.getAttributeInt("FIRST_NEAR_LAT");
                final boolean south = firstNearLat < 0;
                final int utm_zone = Integer.parseInt(mapElem.getAttributeString("utm_zone").trim());
                // todo this looks suspiciously ASAR specific??
                final double easting = mapElem.getAttributeDouble("ASAR_Map_GADS.sd/position_northings_eastings.tl_easting");
                final double northing = mapElem.getAttributeDouble("ASAR_Map_GADS.sd/position_northings_eastings.tl_northing");
                final double sample_spacing = mapElem.getAttributeDouble("sample_spacing");
                final double line_spacing = mapElem.getAttributeDouble("line_spacing");
                final double utm_scale = mapElem.getAttributeDouble("utm_scale");

                final GeodeticDatum datum = DefaultGeodeticDatum.WGS84;
                final ParameterValueGroup tmParameters = createTransverseMercatorParameters(utm_zone, south, utm_scale,
                                                                                            datum);
                final String projectionName = "UTM Zone " + utm_zone + (south ? ", South" : "");

                final CoordinateReferenceSystem targetCRS = createCrs(projectionName, new TransverseMercator.Provider(),
                                                                      tmParameters, datum);

                final CrsGeoCoding geoCoding = new CrsGeoCoding(targetCRS,
                                                                product.getSceneRasterWidth(),
                                                                product.getSceneRasterHeight(),
                                                                easting, northing,
                                                                sample_spacing, line_spacing);
                product.setGeoCoding(geoCoding);
            } else {
                return false;
            }
            return true;
        } catch(Exception e) {
            // map projection info not found in metadata
            return false;
        }
    }

    private static void setValue(ParameterValueGroup values, ParameterDescriptor<Double> descriptor, double value) {
        values.parameter(descriptor.getName().getCode()).setValue(value);
    }

    private static ParameterValueGroup createTransverseMercatorParameters(int zoneIndex, boolean south, double scale,
                                                                          GeodeticDatum datum) {
        ParameterDescriptorGroup tmParameters = new TransverseMercator.Provider().getParameters();
        ParameterValueGroup tmValues = tmParameters.createValue();

        setValue(tmValues, MapProjection.AbstractProvider.SEMI_MAJOR, datum.getEllipsoid().getSemiMajorAxis());
        setValue(tmValues, MapProjection.AbstractProvider.SEMI_MINOR, datum.getEllipsoid().getSemiMinorAxis());
        setValue(tmValues, MapProjection.AbstractProvider.LATITUDE_OF_ORIGIN, 0.0);
        setValue(tmValues, MapProjection.AbstractProvider.CENTRAL_MERIDIAN, (zoneIndex - 0.5) * 6.0 - 180.0);
        setValue(tmValues, MapProjection.AbstractProvider.SCALE_FACTOR, scale);
        setValue(tmValues, MapProjection.AbstractProvider.FALSE_EASTING, 500000.0);
        setValue(tmValues, MapProjection.AbstractProvider.FALSE_NORTHING, south ? 10000000.0 : 0.0);
        return tmValues;
    }

    private static CoordinateReferenceSystem createCrs(String crsName, OperationMethod method,
                                                  ParameterValueGroup parameters,
                                                  GeodeticDatum datum) throws FactoryException {
        final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
        final CoordinateOperationFactory coFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);
        final HashMap<String, Object> projProperties = new HashMap<>();
        projProperties.put("name", crsName + " / " + datum.getName().getCode());
        final Conversion conversion = coFactory.createDefiningConversion(projProperties,
                                                                         method,
                                                                         parameters);
        final HashMap<String, Object> baseCrsProperties = new HashMap<>();
        baseCrsProperties.put("name", datum.getName().getCode());
        final GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties, datum,
                                                                     DefaultEllipsoidalCS.GEODETIC_2D);
        return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
    }

    /**
     * Installs an Envisat-specific pointing factory in the given product.
     */
    public static void initPointingFactory(final Product product) {
        PointingFactoryRegistry registry = PointingFactoryRegistry.getInstance();
        PointingFactory pointingFactory = registry.getPointingFactory(product.getProductType());
        product.setPointingFactory(pointingFactory);
    }

    private void addHeaderAnnotationsToProduct(Product product) {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(product);
        final MetadataElement metaRoot = product.getMetadataRoot();
        metaRoot.addElement(createMetadataGroup("MPH", productFile.getMPH().getParams()));
        metaRoot.addElement(createMetadataGroup("SPH", productFile.getSPH().getParams()));

        final DSD[] dsds = productFile.getDsds();
        final MetadataElement dsdsGroup = new MetadataElement("DSD");
        for (int i = 0; i < dsds.length; i++) {
            final DSD dsd = dsds[i];
            if (dsd != null) {
                final MetadataElement dsdGroup = new MetadataElement("DSD." + (i + 1));
                dsdGroup.addAttribute(
                        new MetadataAttribute("DATASET_NAME",
                                              ProductData.createInstance(getNonNullString(dsd.getDatasetName())),
                                              true));
                dsdGroup.addAttribute(new MetadataAttribute("DATASET_TYPE",
                                                            ProductData.createInstance(new String(new char[]{dsd.getDatasetType()})),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("FILE_NAME",
                                                            ProductData.createInstance(getNonNullString(dsd.getFileName())),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("OFFSET", ProductData.createInstance(new long[]{dsd.getDatasetOffset()}), true));
                dsdGroup.addAttribute(new MetadataAttribute("SIZE", ProductData.createInstance(new long[]{dsd.getDatasetSize()}), true));
                dsdGroup.addAttribute(new MetadataAttribute("NUM_RECORDS",
                                                            ProductData.createInstance(new int[]{dsd.getNumRecords()}),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("RECORD_SIZE",
                                                            ProductData.createInstance(new int[]{dsd.getRecordSize()}),
                                                            true));
                dsdsGroup.addElement(dsdGroup);
            }
        }
        metaRoot.addElement(dsdsGroup);
    }

    private static String getNonNullString(String s) {
        return s != null ? s : "";
    }

    private void addDatasetAnnotationsToProduct(final Product product) throws IOException {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(product);
        final MetadataElement metaRoot = product.getMetadataRoot();
        final String[] datasetNames = productFile.getValidDatasetNames();
        for (String datasetName : datasetNames) {
            final DSD dsd = productFile.getDSD(datasetName);
            final char dsdType = dsd.getDatasetType();
            if (dsdType == EnvisatConstants.DS_TYPE_ANNOTATION
                    || dsdType == EnvisatConstants.DS_TYPE_GLOBAL_ANNOTATION) {
                final RecordReader recordReader = productFile.getRecordReader(datasetName);
                final int numRecords = recordReader.getNumRecords();
                if (numRecords > 1) {
                    final MetadataElement group = createMetadataTableGroup(datasetName, recordReader);
                    metaRoot.addElement(group);
                } else if (numRecords == 1) {
                    final MetadataElement table = createDatasetTable(datasetName, recordReader);
                    metaRoot.addElement(table);
                }
            }
        }
    }



private TiePointGrid createTiePointGrid(final BandLineReader bandLineReader,
                                            final BandLineReader samplesReader) throws IOException {
        final BandInfo bandInfo = bandLineReader.getBandInfo();
        final String bandName = bandLineReader.getBandName();
        final int gridWidth = bandLineReader.getRasterWidth();
        final int gridHeight = bandLineReader.getRasterHeight();
        final int pixelDataType = bandLineReader.getPixelDataField().getDataType();
        int tiePointIndex = 0;
        final float scalingOffset = bandInfo.getScalingOffset();
        final float scalingFactor = bandInfo.getScalingFactor();
        final float[] tiePoints = new float[gridWidth * gridHeight];
        final boolean storesPixelsInChronologicalOrder = getProductFile().storesPixelsInChronologicalOrder();

        final float offsetX = getProductFile().getTiePointGridOffsetX(gridWidth);
        final float offsetY = getProductFile().getTiePointGridOffsetY(gridWidth);
        final int sceneRasterWidth = getProductFile().getSceneRasterWidth();
        final int sceneRasterHeight = getProductFile().getSceneRasterHeight();
        final float subSamplingX = (float)sceneRasterWidth/(float)(gridWidth - 1);
//        final float subSamplingX = getProductFile().getTiePointSubSamplingX(gridWidth);
        final float subSamplingY = getProductFile().getTiePointSubSamplingY(gridWidth);
        int[] sampleData = null;

        for (int y = 0; y < gridHeight; y++) {
            bandLineReader.readLineRecord(y);

            if(samplesReader != null) {
                samplesReader.readLineRecord(y);
                sampleData = (int[]) samplesReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(sampleData);
                }
            }

            if (pixelDataType == ProductData.TYPE_INT8) {
                final byte[] pixelData = (byte[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }

                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * pixelData[x];
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else if (pixelDataType == ProductData.TYPE_UINT8) {
                final byte[] pixelData = (byte[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }
                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * (pixelData[x] & 0xff);
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else if (pixelDataType == ProductData.TYPE_INT16) {
                final short[] pixelData = (short[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }
                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * pixelData[x];
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else if (pixelDataType == ProductData.TYPE_UINT16) {
                final short[] pixelData = (short[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }
                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * (pixelData[x] & 0xffff);
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else if (pixelDataType == ProductData.TYPE_INT32) {
                final int[] pixelData = (int[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }
                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * pixelData[x];
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else if (pixelDataType == ProductData.TYPE_UINT32) {
                final int[] pixelData = (int[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }
                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * (pixelData[x] & 0xffffffffL);
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else if (pixelDataType == ProductData.TYPE_FLOAT32) {
                final float[] pixelData = (float[]) bandLineReader.getPixelDataField().getElems();
                if (storesPixelsInChronologicalOrder) {
                    ArrayUtils.swapArray(pixelData);
                }
                final float[] pixelDataFloat = new float[gridWidth];
                for (int x = 0; x < gridWidth; x++) {
                    pixelDataFloat[x] = scalingOffset + scalingFactor * pixelData[x];
                }

                for (int x = 0; x < gridWidth; x++) {
                    final float p = x*subSamplingX;
                    tiePoints[tiePointIndex] = interpolateIfNeeded(p, pixelDataFloat, sampleData, x, gridWidth);
                    tiePointIndex++;
                }

            } else {
                throw new IllegalFileFormatException("unhandled tie-point data type"); /*I18N*/
            }
        }

        final TiePointGrid tiePointGrid = createTiePointGrid(bandName,
                                                       gridWidth,
                                                       gridHeight,
                                                       offsetX,
                                                       offsetY,
                                                       subSamplingX,
                                                       subSamplingY,
                                                       tiePoints);
        if (bandInfo.getPhysicalUnit() != null) {
            tiePointGrid.setUnit(bandInfo.getPhysicalUnit());
        }
        if (bandInfo.getDescription() != null) {
            tiePointGrid.setDescription(bandInfo.getDescription());
        }
        return tiePointGrid;
    }

    private static float interpolateIfNeeded(
            final float p, final float[] pixelDataFloat, final int[] sampleData, final int x, final int gridWidth) {

        if(sampleData != null && x > 0 && x < gridWidth - 1) {
            int xx = 0;
            for (int i = 0; i < gridWidth; i++) {
                if (p > sampleData[i] - 1) {
                    xx = i;
                } else {
                    break;
                }
            }

            final int xStart = Math.max(0, xx - 1);
            final int xEnd = Math.min(xx + 2, gridWidth - 1);
            final double[] pos = new double[xEnd - xStart + 1];
            final double[] val = new double[xEnd - xStart + 1];
            for (int i = xStart; i <= xEnd; i++) {
                pos[i - xStart] = sampleData[i] - 1;
                val[i - xStart] = pixelDataFloat[i];
            }
            return (float)lagrangeInterpolatingPolynomial(pos, val, (double)p);
        }
        return pixelDataFloat[x];
    }
    
    private static double lagrangeInterpolatingPolynomial (final double pos[], final double val[], final double desiredPos)  {
        double retVal = 0;
        for (int i = 0; i < pos.length; ++i) {
            double weight = 1;
            for (int j = 0; j < pos.length; ++j) {
                if (j != i) {
                    weight *= (desiredPos - pos[j]) / (pos[i] - pos[j]);
                }
            }
            retVal += weight * val[i];
        }
        return retVal;
    }

    private MetadataElement createDatasetTable(String name, RecordReader recordReader) throws IOException {
        Debug.assertTrue(productFile != null);
        Debug.assertTrue(name != null);
        Debug.assertTrue(recordReader != null);

        final Record record = recordReader.readRecord();
        return createMetadataGroup(name, record);
    }

    private MetadataElement createMetadataTableGroup(String name, RecordReader recordReader) throws IOException {
        Debug.assertTrue(productFile != null);
        Debug.assertTrue(name != null);
        Debug.assertTrue(recordReader != null);

        final MetadataElement metadataTableGroup = new MetadataElement(name);
        final StringBuilder sb = new StringBuilder(16);
        final int numRecords = recordReader.getNumRecords();
        for (int i = 0; i < numRecords; i++) {
            final Record record = recordReader.readRecord(i);
            sb.setLength(0);
            sb.append(name);
            sb.append('.');
            sb.append(i + 1);
            metadataTableGroup.addElement(createMetadataGroup(sb.toString(), record));
        }

        return metadataTableGroup;
    }

    static MetadataElement createMetadataGroup(String name, Record record) {
        Debug.assertNotNullOrEmpty(name);
        Debug.assertNotNull(record);

        final MetadataElement metadataGroup = new MetadataElement(name);
        final int numRecords = record.getNumFields();
        for (int i = 0; i < numRecords; i++) {
            final Field field = record.getFieldAt(i);

            final String description = field.getInfo().getDescription();
            if (description != null) {
                if ("Spare".equalsIgnoreCase(description)) {
                    continue;
                }
            }

            final MetadataAttribute attribute = new MetadataAttribute(field.getName(), field.getData(), true);
            if (field.getInfo().getPhysicalUnit() != null) {
                attribute.setUnit(field.getInfo().getPhysicalUnit());
            }
            //if (description != null) {
            //    attribute.setDescription(field.getInfo().getDescription());
            //}

            metadataGroup.addAttribute(attribute);
        }

        return metadataGroup;
    }

    /**
     * Used by the {@link #createTiePointGrid(String, int, int, float, float, float, float, float[]) createTiePointGrid} method in order to determine
     * the discontinuity mode for angle tie-point grids.
     * <p>The default implementation returns {@link org.esa.beam.framework.datamodel.TiePointGrid#DISCONT_AT_180} for
     * the names "lon", "long" or "longitude" ignoring letter case,
     * {@link org.esa.beam.framework.datamodel.TiePointGrid#DISCONT_NONE} otherwise.
     *
     * @param name the grid name
     *
     * @return the discontinuity mode, always one of {@link org.esa.beam.framework.datamodel.TiePointGrid#DISCONT_NONE}, {@link org.esa.beam.framework.datamodel.TiePointGrid#DISCONT_AT_180} and {@link org.esa.beam.framework.datamodel.TiePointGrid#DISCONT_AT_360}.
     */
    @Override
    protected int getGridDiscontinutity(String name) {
        if (name.equalsIgnoreCase(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME) ||
            name.equalsIgnoreCase(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME)) {
            return TiePointGrid.DISCONT_AT_360;
        } else if (name.equalsIgnoreCase(EnvisatConstants.LON_DS_NAME) ||
                   name.equalsIgnoreCase(EnvisatConstants.AATSR_SUN_AZIMUTH_NADIR_DS_NAME) ||
                   name.equalsIgnoreCase(EnvisatConstants.AATSR_VIEW_AZIMUTH_NADIR_DS_NAME) ||
                   name.equalsIgnoreCase(EnvisatConstants.AATSR_SUN_AZIMUTH_FWARD_DS_NAME) ||
                   name.equalsIgnoreCase(EnvisatConstants.AATSR_VIEW_AZIMUTH_FWARD_DS_NAME)) {
            return TiePointGrid.DISCONT_AT_180;
        } else {
            return TiePointGrid.DISCONT_NONE;
        }
    }
}
