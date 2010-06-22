/*
 * $Id: EnvisatProductReader.java,v 1.3 2007/03/19 15:52:30 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.envisat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.PointingFactory;
import org.esa.beam.framework.datamodel.PointingFactoryRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
public class EnvisatProductReader extends AbstractProductReader {

    private static final String ENVISAT_AMORGOS_USE_PIXEL_GEO_CODING = "beam.envisat.amorgos.usePixelGeoCoding";

    /**
     * Represents the product's file.
     */
    private ProductFile _productFile;

    /**
     * The width of the raster covering the full scene.
     */
    private int _sceneRasterWidth;
    /**
     * The height of the raster covering the full scene.
     */
    private int _sceneRasterHeight;

    /**
     * Constructs a new ENVISAT product reader.
     *
     * @param readerPlugIn the plug-in which created this reader instance
     */
    public EnvisatProductReader(EnvisatProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public ProductFile getProductFile() {
        return _productFile;
    }

    public BandLineReader getBandLineReader(final Band band) throws IOException {
        BandLineReader bandLineReader = getProductFile().getBandLineReader(band);
        if (bandLineReader == null) {
            throw new ProductIOException("nothing known about a band named '" + band.getName() + "'"); /*I18N*/
        }
        return bandLineReader;
    }

    public int getSceneRasterWidth() {
        return _sceneRasterWidth;
    }

    public int getSceneRasterHeight() {
        return _sceneRasterHeight;
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
                _productFile = ProductFile.open(file);
            } catch (IOException e) {
                final InputStream inputStream;
                try {
                    inputStream = EnvisatProductReaderPlugIn.getInflaterInputStream(file);
                } catch (IOException ignored) {
                    throw e;
                }
                _productFile = ProductFile.open(file, new FileCacheImageInputStream(inputStream, null));
            }
        } else if (input instanceof ImageInputStream) {
            _productFile = ProductFile.open((ImageInputStream) input);
        } else if (input instanceof ProductFile) {
            _productFile = (ProductFile) input;
        }

        Debug.assertNotNull(_productFile);
        _sceneRasterWidth = _productFile.getSceneRasterWidth();
        _sceneRasterHeight = _productFile.getSceneRasterHeight();

        if (getSubsetDef() != null) {
            Dimension s = getSubsetDef().getSceneRasterSize(_sceneRasterWidth, _sceneRasterHeight);
            _sceneRasterWidth = s.width;
            _sceneRasterHeight = s.height;
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
        if (_productFile != null) {
            _productFile.close();
            _productFile = null;
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
        final BandLineReader bandLineReader = getBandLineReader(destBand);
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
        Debug.assertTrue(getSceneRasterWidth() > 0);
        Debug.assertTrue(getSceneRasterHeight() > 0);

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
        product.setBandSubGroupPaths(getProductFile().getBandSubGroupPaths());

        addBandsToProduct(product);
        if (!isMetadataIgnored()) {
            addHeaderAnnotationsToProduct(product);
            addDatasetAnnotationsToProduct(product);
            addTiePointGridsToProduct(product);
            addGeoCodingToProduct(product);
            initPointingFactory(product);
        }
        addDefaultBitmaskDefsToProduct(product);
        addDefaultBitmaskDefsToBands(product);

        _productFile.addCustomMetadata(product);

        return product;
    }

    private void addBandsToProduct(Product product) {
        Debug.assertNotNull(_productFile);
        Debug.assertNotNull(product);

        BandLineReader[] bandLineReaders = _productFile.getBandLineReaders();
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

                _productFile.setInvalidPixelExpression(band);

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
                    band.setFlagCoding(bandInfo.getFlagCoding());
                }
                final String expression = bandInfo.getValidExpression();
                if (expression != null && expression.trim().length() > 0) {
                    band.setValidPixelExpression(expression.trim());
                }
                product.addBand(band);
            }

        }
        setSpectralBandInfo(product);
    }

    private void addDefaultBitmaskDefsToProduct(Product product) {
        List<Band> flagDsList = new Vector<Band>();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                flagDsList.add(band);
            }
        }
        if (!flagDsList.isEmpty()) {
            for (Band flagDs : flagDsList) {
                String flagDsName = flagDs.getName();
                BitmaskDef[] bitmaskDefs = _productFile.createDefaultBitmaskDefs(flagDsName);
                for (BitmaskDef bitmaskDef : bitmaskDefs) {
                    product.addBitmaskDef(bitmaskDef);
                }
            }
        }
    }


    private void addDefaultBitmaskDefsToBands(Product product) {
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
        return _productFile.getDefaultBitmaskNames(bandName);
    }

    private void setSpectralBandInfo(Product product) {
        float[] wavelengths = _productFile.getSpectralBandWavelengths();
        float[] bandwidths = _productFile.getSpectralBandBandwidths();
        float[] solar_fluxes = _productFile.getSpectralBandSolarFluxes();
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
        BandLineReader[] bandLineReaders = getProductFile().getBandLineReaders();
        for (BandLineReader bandLineReader : bandLineReaders) {
            if (bandLineReader.isTiePointBased()) {
                TiePointGrid tiePointGrid = createTiePointGrid(bandLineReader);
                product.addTiePointGrid(tiePointGrid);
            }
        }
    }

    private static void addGeoCodingToProduct(Product product) {
        initTiePointGeoCoding(product);

        final boolean usePixeGeoCoding = Boolean.getBoolean(ENVISAT_AMORGOS_USE_PIXEL_GEO_CODING);
        if (usePixeGeoCoding) {
            final String productType = product.getProductType();
            if (productType.equals(EnvisatConstants.MERIS_FRG_L1B_PRODUCT_TYPE_NAME) ||
                productType.equals(EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME)) {
                initPixelGeoCoding(product);
            }
        }
    }

    private static void initPixelGeoCoding(Product product) {
        final PixelGeoCoding pixelGeoCoding = new PixelGeoCoding(
                product.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME),
                product.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME),
                "NOT l1_flags.INVALID", 6);
        product.setGeoCoding(pixelGeoCoding);
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
     * Installs an Envisat-specific pointing factory in the given product.
     */
    public static void initPointingFactory(final Product product) {
        PointingFactoryRegistry registry = PointingFactoryRegistry.getInstance();
        PointingFactory pointingFactory = registry.getPointingFactory(product.getProductType());
        product.setPointingFactory(pointingFactory);
    }

    private void addHeaderAnnotationsToProduct(Product product) {
        Debug.assertNotNull(_productFile);
        Debug.assertNotNull(product);
        product.getMetadataRoot().addElement(createMetadataGroup("MPH", _productFile.getMPH().getParams()));
        product.getMetadataRoot().addElement(createMetadataGroup("SPH", _productFile.getSPH().getParams()));

        final DSD[] dsds = _productFile.getDsds();
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
                                                            ProductData.createInstance(
                                                                    new String(new char[]{dsd.getDatasetType()})),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("FILE_NAME",
                                                            ProductData.createInstance(
                                                                    getNonNullString(dsd.getFileName())),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("OFFSET", ProductData.createInstance(
                        new long[]{dsd.getDatasetOffset()}),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("SIZE", ProductData.createInstance(
                        new long[]{dsd.getDatasetSize()}),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("NUM_RECORDS",
                                                            ProductData.createInstance(new int[]{dsd.getNumRecords()}),
                                                            true));
                dsdGroup.addAttribute(new MetadataAttribute("RECORD_SIZE",
                                                            ProductData.createInstance(new int[]{dsd.getRecordSize()}),
                                                            true));
                dsdsGroup.addElement(dsdGroup);
            }
        }
        product.getMetadataRoot().addElement(dsdsGroup);
    }

    private static String getNonNullString(String s) {
        return s != null ? s : "";
    }

    private void addDatasetAnnotationsToProduct(Product product) throws IOException {
        Debug.assertNotNull(_productFile);
        Debug.assertNotNull(product);
        String[] datasetNames = _productFile.getValidDatasetNames();
        for (String datasetName : datasetNames) {
            DSD dsd = _productFile.getDSD(datasetName);
            if (dsd.getDatasetType() == EnvisatConstants.DS_TYPE_ANNOTATION
                || dsd.getDatasetType() == EnvisatConstants.DS_TYPE_GLOBAL_ANNOTATION) {
                RecordReader recordReader = _productFile.getRecordReader(datasetName);
                if (recordReader.getNumRecords() == 1) {
                    MetadataElement table = createDatasetTable(datasetName, recordReader);
                    product.getMetadataRoot().addElement(table);
                } else if (recordReader.getNumRecords() > 1) {
                    MetadataElement group = createMetadataTableGroup(datasetName, recordReader);
                    product.getMetadataRoot().addElement(group);
                }
            }
        }
    }

    private TiePointGrid createTiePointGrid(BandLineReader bandLineReader) throws IOException {
        BandInfo bandInfo = bandLineReader.getBandInfo();
        String bandName = bandLineReader.getBandName();
        int gridWidth = bandLineReader.getRasterWidth();
        int gridHeight = bandLineReader.getRasterHeight();
        int pixelDataType = bandLineReader.getPixelDataField().getDataType();
        int tiePointIndex = 0;
        float scalingOffset = bandInfo.getScalingOffset();
        float scalingFactor = bandInfo.getScalingFactor();
        float[] tiePoints = new float[gridWidth * gridHeight];
        for (int y = 0; y < gridHeight; y++) {
            bandLineReader.readLineRecord(y);
            if (pixelDataType == ProductData.TYPE_INT8) {
                byte[] pixelData = (byte[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * pixelData[x];
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_UINT8) {
                byte[] pixelData = (byte[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * (pixelData[x] & 0xff);
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_INT16) {
                short[] pixelData = (short[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * pixelData[x];
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_UINT16) {
                short[] pixelData = (short[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * (pixelData[x] & 0xffff);
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_INT32) {
                int[] pixelData = (int[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * pixelData[x];
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_UINT32) {
                int[] pixelData = (int[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * (pixelData[x] & 0xffffffffL);
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_FLOAT32) {
                float[] pixelData = (float[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = scalingOffset + scalingFactor * pixelData[x];
                    tiePointIndex++;
                }
            } else {
                throw new IllegalFileFormatException("unhandled tie-point data type"); /*I18N*/
            }
        }
        float offsetX = getProductFile().getTiePointGridOffsetX(gridWidth);
        float offsetY = getProductFile().getTiePointGridOffsetY(gridWidth);
        float subSamplingX = getProductFile().getTiePointSubSamplingX(gridWidth);
        float subSamplingY = getProductFile().getTiePointSubSamplingY(gridWidth);

        TiePointGrid tiePointGrid = createTiePointGrid(bandName,
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

    private MetadataElement createDatasetTable(String name, RecordReader recordReader) throws IOException {
        Debug.assertTrue(_productFile != null);
        Debug.assertTrue(name != null);
        Debug.assertTrue(recordReader != null);

        Record record = recordReader.readRecord();
        return createMetadataGroup(name, record);
    }

    private MetadataElement createMetadataTableGroup(String name, RecordReader recordReader) throws IOException {
        Debug.assertTrue(_productFile != null);
        Debug.assertTrue(name != null);
        Debug.assertTrue(recordReader != null);

        MetadataElement metadataTableGroup = new MetadataElement(name);
        StringBuffer sb = new StringBuffer(16);
        for (int i = 0; i < recordReader.getNumRecords(); i++) {
            Record record = recordReader.readRecord(i);
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

        MetadataElement metadataGroup = new MetadataElement(name);

        for (int i = 0; i < record.getNumFields(); i++) {
            Field field = record.getFieldAt(i);

            String description = field.getInfo().getDescription();
            if (description != null) {
                if ("Spare".equalsIgnoreCase(description)) {
                    continue;
                }
            }

            MetadataAttribute attribute = new MetadataAttribute(field.getName(), field.getData(), true);
            if (field.getInfo().getPhysicalUnit() != null) {
                attribute.setUnit(field.getInfo().getPhysicalUnit());
            }
            if (description != null) {
                attribute.setDescription(field.getInfo().getDescription());
            }

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
