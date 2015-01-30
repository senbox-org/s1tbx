package org.esa.beam.dataio.bigtiff;


import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFIFD;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.beam.dataio.bigtiff.internal.GeoKeyEntry;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.geotiff.EPSGCodes;
import org.esa.beam.util.geotiff.GeoTIFFCodes;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.coverage.grid.io.imageio.geotiff.*;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

class BigGeoTiffProductReader extends AbstractProductReader {

    private static final int FIRST_IMAGE = 0;

    private ImageInputStream inputStream;
    private TIFFImageReader imageReader;
    private Map<Band, Integer> bandMap;
    private boolean isGlobalShifted180;

    protected BigGeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        File inputFile = null;
        Object input = getInput();
        if (input instanceof String) {
            input = new File((String) input);
        }
        if (input instanceof File) {
            inputFile = (File) input;
        }
        inputStream = ImageIO.createImageInputStream(input);

        return readGeoTIFFProduct(inputStream, inputFile);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        if (isGlobalShifted180) {
            // SPECIAL CASE of a global geographic lat/lon with lon from 0..360 instead of -180..180
            readBandRasterDataImplGlobalShifted180(sourceOffsetX, sourceOffsetY,
                    sourceStepX, sourceStepY, destBand, destOffsetX, destOffsetY,
                    destWidth, destHeight, destBuffer, pm);
        } else {
            // the normal case!!
            final int destSize = destWidth * destHeight;
            pm.beginTask("Reading data...", 3);
            try {
                final Raster data = readRect(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destOffsetX, destOffsetY, destWidth, destHeight);
                pm.worked(1);

                Integer bandIdx = bandMap.get(destBand);
                if (bandIdx == null) {
                    bandIdx = 0;
                }
                final DataBuffer dataBuffer = data.getDataBuffer();
                final SampleModel sampleModel = data.getSampleModel();
                final int dataBufferType = dataBuffer.getDataType();

                boolean isInteger = dataBufferType == DataBuffer.TYPE_SHORT
                        || dataBufferType == DataBuffer.TYPE_USHORT
                        || dataBufferType == DataBuffer.TYPE_INT;
                boolean isIntegerTarget = destBuffer.getElems() instanceof int[];
                if (isInteger && isIntegerTarget) {
                    sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIdx, (int[]) destBuffer.getElems(), dataBuffer);
                } else if (dataBufferType == DataBuffer.TYPE_FLOAT && destBuffer.getElems() instanceof float[]) {
                    sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIdx, (float[]) destBuffer.getElems(), dataBuffer);
                } else {
                    final double[] dArray = new double[destSize];
                    sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIdx, dArray, dataBuffer);

                    if (destBuffer.getElems() instanceof double[]) {
                        //noinspection SuspiciousSystemArraycopy
                        System.arraycopy(dArray, 0, destBuffer.getElems(), 0, dArray.length);
                    } else {
                        int i = 0;
                        for (double value : dArray) {
                            destBuffer.setElemDoubleAt(i++, value);
                        }
                    }
                }
                pm.worked(1);
            } finally {
                pm.done();
            }
        }
    }

    private void readBandRasterDataImplGlobalShifted180(int sourceOffsetX, int sourceOffsetY,
                                                        int sourceStepX, int sourceStepY,
                                                        Band destBand,
                                                        int destOffsetX, int destOffsetY,
                                                        int destWidth, int destHeight,
                                                        ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final int destSize = destWidth * destHeight;
        pm.beginTask("Reading data...", 3);
        try {

            final Raster dataLeft = readRect(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                    destOffsetX, destOffsetY, destWidth / 2, destHeight);
            final Raster dataRight = readRect(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                    destOffsetX + destWidth / 2, destOffsetY, destWidth / 2, destHeight);
            pm.worked(1);

            double[] dArrayLeft = new double[destSize / 2];
            double[] dArrayRight = new double[destSize / 2];
            Integer bandIdx = bandMap.get(destBand);
            if (bandIdx == null) {
                bandIdx = 0;
            }
            final DataBuffer dataBufferLeft = dataLeft.getDataBuffer();
            final DataBuffer dataBufferRight = dataRight.getDataBuffer();
            final SampleModel sampleModelLeft = dataLeft.getSampleModel();
            final SampleModel sampleModelRight = dataRight.getSampleModel();
            sampleModelLeft.getSamples(0, 0, dataLeft.getWidth(), dataLeft.getHeight(), bandIdx, dArrayLeft, dataBufferLeft);
            sampleModelRight.getSamples(0, 0, dataRight.getWidth(), dataRight.getHeight(), bandIdx, dArrayRight, dataBufferRight);
            pm.worked(1);

            int dArrayIndex = 0;
            for (int y = 0; y < destHeight; y++) {
                for (int x = 0; x < destWidth / 2; x++) {
                    destBuffer.setElemDoubleAt(dArrayIndex++, dArrayRight[y * destWidth / 2 + x]);
                }
                for (int x = 0; x < destWidth / 2; x++) {
                    destBuffer.setElemDoubleAt(dArrayIndex++, dArrayLeft[y * destWidth / 2 + x]);
                }
            }

            pm.worked(1);

        } finally {
            pm.done();
        }
    }

    private synchronized Raster readRect(int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                                         int destOffsetX, int destOffsetY, int destWidth, int destHeight) throws
            IOException {
        ImageReadParam readParam = imageReader.getDefaultReadParam();
        int subsamplingXOffset = sourceOffsetX % sourceStepX;
        int subsamplingYOffset = sourceOffsetY % sourceStepY;
        readParam.setSourceSubsampling(sourceStepX, sourceStepY, subsamplingXOffset, subsamplingYOffset);
        RenderedImage subsampledImage = imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);

        return subsampledImage.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        inputStream.close();
    }

    Product readGeoTIFFProduct(final ImageInputStream stream, final File inputFile) throws IOException {
        imageReader = BigGeoTiffProductReaderPlugIn.getTiffImageReader(stream);
        if (imageReader == null) {
            throw new IOException("GeoTiff imageReader not found");
        }
        imageReader.setInput(stream);

        Product product = null;

        final TIFFImageMetadata imageMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(FIRST_IMAGE);
        final TIFFIFD rootIFD = imageMetadata.getRootIFD();
        final TiffFileInfo tiffFileInfo = new TiffFileInfo(rootIFD);
        final TIFFField field = tiffFileInfo.getField(Constants.PRIVATE_BEAM_TIFF_TAG_NUMBER);

        if (isAsciiField(field) && isDimapField(field)) {
            product = createProductWithDimapHeader(field);
        }

        if (product == null) {
            product = createProduct(inputFile, tiffFileInfo);
        }

        product.setFileLocation(inputFile);

        if (tiffFileInfo.isGeotiff()) {
            applyGeoCoding(tiffFileInfo, imageMetadata, product);
        }

        TiffToProductMetadataConverter.addTiffTagsToMetadata(imageMetadata, tiffFileInfo, product.getMetadataRoot());

        return product;
    }

    private Product createProduct(File inputFile, TiffFileInfo tiffFileInfo) throws IOException {
        Product product;
        final String productName = getProductName(inputFile, tiffFileInfo);
        final String productType = getReaderPlugIn().getFormatNames()[0];

        final int width = imageReader.getWidth(FIRST_IMAGE);
        final int height = imageReader.getHeight(FIRST_IMAGE);
        product = new Product(productName, productType, width, height, this);
        setPreferredTiling(product);
        addBandsToProduct(tiffFileInfo, product);
        return product;
    }

    private void applyGeoCoding(TiffFileInfo tiffFileInfo, TIFFImageMetadata imageMetadata, Product product) {
        if (tiffFileInfo.containsField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {

            double[] tiePoints = tiffFileInfo.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT).getAsDoubles();

            boolean isGlobal = isGlobal(product, tiffFileInfo);

            // check if we have a global geographic lat/lon with lon from 0..360 instead of -180..180
            final double deltaX = Math.ceil(360. / product.getSceneRasterWidth());
            if (isGlobal && tiePoints.length == 6 && Math.abs(tiePoints[3]) < deltaX) {
                // e.g. tiePoints[3] = -0.5, productWidth=722 --> we have a lon range of 361 which should start
                // at or near -180 but not at zero
                isGlobalShifted180 = true;
                // subtract 180 from the longitudes
                tiePoints[3] -= 180.0;
            }

            if (canCreateTiePointGeoCoding(tiePoints)) {
                applyTiePointGeoCoding(tiffFileInfo, tiePoints, product);
            } else if (canCreateGcpGeoCoding(tiePoints)) {
                applyGcpGeoCoding(tiffFileInfo, tiePoints, product);
            }
        }

        if (product.getGeoCoding() == null) {
            try {
                applyGeoCodingFromGeoTiff(imageMetadata, product);
            } catch (Exception ignored) {
            }
        }
    }

    private static void applyGeoCodingFromGeoTiff(TIFFImageMetadata metadata, Product product) throws Exception {
        final Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final GeoTiffIIOMetadataDecoder metadataDecoder = new GeoTiffIIOMetadataDecoder(metadata);
        final GeoTiffMetadata2CRSAdapter geoTiff2CRSAdapter = new GeoTiffMetadata2CRSAdapter(null);
        // todo reactivate the following line if geotools has fixed the problem. (see BEAM-1510)
        // final MathTransform toModel = GeoTiffMetadata2CRSAdapter.getRasterToModel(metadataDecoder, false);
        final MathTransform toModel = getRasterToModel(metadataDecoder);
        CoordinateReferenceSystem crs;
        try {
            crs = geoTiff2CRSAdapter.createCoordinateSystem(metadataDecoder);
        } catch (UnsupportedOperationException e) {
            if (toModel == null) {
                throw e;
            } else {
                // ENVI falls back to WGS84, if no CRS is given in the GeoTIFF.
                crs = DefaultGeographicCRS.WGS84;
            }
        }
        final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, imageBounds, (AffineTransform) toModel);
        product.setGeoCoding(geoCoding);
    }

    /*
     * Copied from GeoTools GeoTiffMetadata2CRSAdapter because the given tie-point offset is
     * not correctly interpreted in GeoTools. The tie-point should be placed at the pixel center
     * if RasterPixelIsPoint is set as value for GTRasterTypeGeoKey.
     * See links:
     * http://www.remotesensing.org/geotiff/faq.html#PixelIsPoint
     * http://lists.osgeo.org/pipermail/gdal-dev/2007-November/015040.html
     * http://trac.osgeo.org/gdal/wiki/rfc33_gtiff_pixelispoint
     */
    private static MathTransform getRasterToModel(final GeoTiffIIOMetadataDecoder metadata) throws GeoTiffException {
        //
        // Load initials
        //
        final boolean hasTiePoints = metadata.hasTiePoints();
        final boolean hasPixelScales = metadata.hasPixelScales();
        final boolean hasModelTransformation = metadata.hasModelTrasformation();
        int rasterType = getGeoKeyAsInt(GeoTiffConstants.GTRasterTypeGeoKey, metadata);
        // geotiff spec says that PixelIsArea is the default
        if (rasterType == GeoTiffConstants.UNDEFINED) {
            rasterType = GeoTiffConstants.RasterPixelIsArea;
        }
        MathTransform xform;
        if (hasTiePoints && hasPixelScales) {

            //
            // we use tie points and pixel scales to build the grid to world
            //
            // model space
            final TiePoint[] tiePoints = metadata.getModelTiePoints();
            final PixelScale pixScales = metadata.getModelPixelScales();


            // here is the matrix we need to build
            final GeneralMatrix gm = new GeneralMatrix(3);
            final double scaleRaster2ModelLongitude = pixScales.getScaleX();
            final double scaleRaster2ModelLatitude = -pixScales.getScaleY();
            // "raster" space
            final double tiePointColumn = tiePoints[0].getValueAt(0) + (rasterType == GeoTiffConstants.RasterPixelIsPoint ? 0.5 : 0);
            final double tiePointRow = tiePoints[0].getValueAt(1) + (rasterType == GeoTiffConstants.RasterPixelIsPoint ? 0.5 : 0);

            // compute an "offset and scale" matrix
            gm.setElement(0, 0, scaleRaster2ModelLongitude);
            gm.setElement(1, 1, scaleRaster2ModelLatitude);
            gm.setElement(0, 1, 0);
            gm.setElement(1, 0, 0);

            gm.setElement(0, 2, tiePoints[0].getValueAt(3) - (scaleRaster2ModelLongitude * tiePointColumn));
            gm.setElement(1, 2, tiePoints[0].getValueAt(4) - (scaleRaster2ModelLatitude * tiePointRow));

            // make it a LinearTransform
            xform = ProjectiveTransform.create(gm);

        } else if (hasModelTransformation) {
            if (rasterType == GeoTiffConstants.RasterPixelIsPoint) {
                final AffineTransform tempTransform = new AffineTransform(metadata.getModelTransformation());
                tempTransform.concatenate(AffineTransform.getTranslateInstance(0.5, 0.5));
                xform = ProjectiveTransform.create(tempTransform);
            } else {
                assert rasterType == GeoTiffConstants.RasterPixelIsArea;
                xform = ProjectiveTransform.create(metadata.getModelTransformation());
            }
        } else {
            throw new GeoTiffException(metadata, "Unknown Raster to Model configuration.", null);
        }

        return xform;
    }

    private static int getGeoKeyAsInt(final int key, final GeoTiffIIOMetadataDecoder metadata) {

        try {
            return Integer.parseInt(metadata.getGeoKey(key));
        } catch (NumberFormatException ne) {
            BeamLogManager.getSystemLogger().log(Level.FINE, ne.getMessage(), ne);
            return GeoTiffConstants.UNDEFINED;
        }
    }

    private static void applyTiePointGeoCoding(TiffFileInfo info, double[] tiePoints, Product product) {
        final SortedSet<Double> xSet = new TreeSet<>();
        final SortedSet<Double> ySet = new TreeSet<>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        final double xMin = xSet.first();
        final double xMax = xSet.last();
        final double xDiff = (xMax - xMin) / (xSet.size() - 1);
        final double yMin = ySet.first();
        final double yMax = ySet.last();
        final double yDiff = (yMax - yMin) / (ySet.size() - 1);

        final int width = xSet.size();
        final int height = ySet.size();

        int idx = 0;
        final Map<Double, Integer> xIdx = new HashMap<>();
        for (Double val : xSet) {
            xIdx.put(val, idx);
            idx++;
        }
        idx = 0;
        final Map<Double, Integer> yIdx = new HashMap<>();
        for (Double val : ySet) {
            yIdx.put(val, idx);
            idx++;
        }

        final float[] lats = new float[width * height];
        final float[] lons = new float[width * height];

        for (int i = 0; i < tiePoints.length; i += 6) {
            final int idxX = xIdx.get(tiePoints[i + 0]);
            final int idxY = yIdx.get(tiePoints[i + 1]);
            final int arrayIdx = idxY * width + idxX;
            lons[arrayIdx] = (float) tiePoints[i + 3];
            lats[arrayIdx] = (float) tiePoints[i + 4];
        }

        String[] names = findSuitableLatLonNames(product);
        final TiePointGrid latGrid = new TiePointGrid(
                names[0], width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lats);
        final TiePointGrid lonGrid = new TiePointGrid(
                names[1], width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lons);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, datum));
    }

    private static boolean canCreateGcpGeoCoding(final double[] tiePoints) {
        int numTiePoints = tiePoints.length / 6;

        if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            return true;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            return true;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            return true;
        } else {
            return false;
        }
    }

    private static void applyGcpGeoCoding(final TiffFileInfo info,
                                          final double[] tiePoints,
                                          final Product product) {

        int numTiePoints = tiePoints.length / 6;

        final GcpGeoCoding.Method method;
        if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL3;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL2;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL1;
        } else {
            return; // not able to apply GCP geo coding; not enough tie points
        }

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        final ProductNodeGroup<Placemark> gcpGroup = product.getGcpGroup();
        for (int i = 0; i < numTiePoints; i++) {
            final int offset = i * 6;

            final float x = (float) tiePoints[offset + 0];
            final float y = (float) tiePoints[offset + 1];
            final float lon = (float) tiePoints[offset + 3];
            final float lat = (float) tiePoints[offset + 4];

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(lon) || Double.isNaN(lat)) {
                continue;
            }
            final PixelPos pixelPos = new PixelPos(x, y);
            final GeoPos geoPos = new GeoPos(lat, lon);

            final Placemark gcp = Placemark.createPointPlacemark(gcpDescriptor, "gcp_" + i, "GCP_" + i, "",
                    pixelPos, geoPos, product.getGeoCoding());
            gcpGroup.add(gcp);
        }

        final Placemark[] gcps = gcpGroup.toArray(new Placemark[gcpGroup.getNodeCount()]);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        product.setGeoCoding(new GcpGeoCoding(method, gcps, width, height, datum));
    }

    private static Datum getDatum(Map<Integer, GeoKeyEntry> geoKeyEntries) {
        final Datum datum;
        if (geoKeyEntries.containsKey(GeoTIFFCodes.GeographicTypeGeoKey)) {
            final int value = geoKeyEntries.get(GeoTIFFCodes.GeographicTypeGeoKey).getIntValue();
            if (value == EPSGCodes.GCS_WGS_72) {
                datum = Datum.WGS_72;
            } else if (value == EPSGCodes.GCS_WGS_84) {
                datum = Datum.WGS_84;
            } else {
                //@todo if user defined ... make user defined datum
                datum = Datum.WGS_84;
            }
        } else {
            datum = Datum.WGS_84;
        }
        return datum;
    }

    private String getProductName(File inputFile, TiffFileInfo tiffFileInfo) {
        final String productName;
        if (tiffFileInfo.containsField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION)) {
            final TIFFField imageDescriptionField = tiffFileInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
            productName = imageDescriptionField.getAsString(0);
        } else if (inputFile != null) {
            productName = FileUtils.getFilenameWithoutExtension(inputFile);
        } else {
            productName = "geotiff";
        }
        return productName;
    }

    private boolean isGlobal(Product product, TiffFileInfo info) {
        boolean isGlobal = false;
        final TIFFField pixelScaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
        if (pixelScaleField != null) {
            double[] pixelScales = pixelScaleField.getAsDoubles();

            if (isPixelScaleValid(pixelScales)) {
                final double widthInDegree = pixelScales[0] * product.getSceneRasterWidth();
                isGlobal = Math.ceil(widthInDegree) >= 360;
            }
        }

        return isGlobal;
    }

    // package access for testing only tb 2015-01-29
    static boolean isPixelScaleValid(double[] pixelScales) {
        return pixelScales != null &&
                !Double.isNaN(pixelScales[0]) && !Double.isInfinite(pixelScales[0]) &&
                !Double.isNaN(pixelScales[1]) && !Double.isInfinite(pixelScales[1]);
    }

    private static boolean canCreateTiePointGeoCoding(final double[] tiePoints) {
        if ((tiePoints.length / 6) <= 1) {
            return false;
        }
        for (double tiePoint : tiePoints) {
            if (Double.isNaN(tiePoint)) {
                return false;
            }
        }
        final SortedSet<Double> xSet = new TreeSet<>();
        final SortedSet<Double> ySet = new TreeSet<>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        return isEquiDistance(xSet) && isEquiDistance(ySet);
    }

    private static boolean isEquiDistance(SortedSet<Double> set) {
        final double min = set.first();
        final double max = set.last();
        final double diff = (max - min) / (set.size() - 1);
        final double diff100000 = diff / 100000;
        final double maxDiff = diff + diff100000;
        final double minDiff = diff - diff100000;

        final Double[] values = set.toArray(new Double[set.size()]);
        for (int i = 1; i < values.length; i++) {
            final double currentDiff = values[i] - values[i - 1];
            if (currentDiff > maxDiff || currentDiff < minDiff) {
                return false;
            }
        }
        return true;
    }

    private void addBandsToProduct(TiffFileInfo tiffFileInfo, Product product) throws IOException {
        final ImageTypeSpecifier rawImageType = imageReader.getRawImageType(0);
        final int numBands = rawImageType.getNumBands();
        final int productDataType = ImageManager.getProductDataType(rawImageType.getSampleModel().getDataType());
        bandMap = new HashMap<>(numBands);
        for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
            final String bandName = String.format("band_%d", bandIndex + 1);
            final Band band = product.addBand(bandName, productDataType);
            band.setSourceImage(getMultiLevelImageSourceImage(band, bandIndex));
            if (tiffFileInfo.containsField(BaselineTIFFTagSet.TAG_COLOR_MAP) &&
                    rawImageType.getColorModel() instanceof IndexColorModel) {
                final IndexColorModel colorModel = (IndexColorModel) rawImageType.getColorModel();
                band.setImageInfo(createIndexedImageInfo(product, band, colorModel));
            }
            bandMap.put(band, bandIndex);
        }
    }

    private static ImageInfo createIndexedImageInfo(Product product, Band band, IndexColorModel colorModel) {
        final IndexCoding indexCoding = new IndexCoding("color_map");
        final int colorCount = colorModel.getMapSize();
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colorCount];
        for (int j = 0; j < colorCount; j++) {
            final String name = String.format("I%3d", j);
            indexCoding.addIndex(name, j, "");
            points[j] = new ColorPaletteDef.Point(j, new Color(colorModel.getRGB(j)), name);
        }
        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);
        return new ImageInfo(new ColorPaletteDef(points, points.length));
    }

    private void initBandsMap(Product product) throws IOException {
        final Band[] bands = product.getBands();
        bandMap = new HashMap<>(bands.length);
        for (Band band : bands) {
            if (!(band instanceof VirtualBand || band instanceof FilterBand)) {
                final int bandIndex = bandMap.size();
                bandMap.put(band, bandIndex);

                band.setSourceImage(getMultiLevelImageSourceImage(band, bandIndex));
            }
        }
    }

    private void removeGeoCodingAndTiePointGrids(Product product) {
        product.setGeoCoding(null);
        final TiePointGrid[] pointGrids = product.getTiePointGrids();
        for (TiePointGrid pointGrid : pointGrids) {
            product.removeTiePointGrid(pointGrid);
        }
    }

    private Product createProductWithDimapHeader(TIFFField field) throws IOException {
        InputStream is = null;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            is = new ByteArrayInputStream(field.getAsString(0).trim().getBytes());
            final Document document = new DOMBuilder().build(builder.parse(is));
            final Product product = DimapProductHelpers.createProduct(document);
            removeGeoCodingAndTiePointGrids(product);
            setPreferredTiling(product);
            initBandsMap(product);
            return product;
        } catch (ParserConfigurationException | SAXException ignore) {
            // ignore if it can not be read
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return null;
    }

    // package access for testing only tb 2015-01-09
    static boolean isAsciiField(TIFFField field) {
        return field != null && field.getType() == TIFFTag.TIFF_ASCII;
    }

    // package access for testing only tb 2015-01-09
    static boolean isDimapField(TIFFField field) {
        if (field != null) {
            final String value = field.getAsString(0).trim();
            return value.contains("<Dimap_Document");
        }
        return false;
    }

    static String[] findSuitableLatLonNames(Product product) {
        final String[] latNames = {"latitude", "latitude_tpg", "lat", "lat_tpg"};
        final String[] lonNames = {"longitude", "longitude_tpg", "lon", "lon_tpg"};
        String[] names = new String[2];
        for (int i = 0; i < latNames.length; i++) {
            String latName = latNames[i];
            String lonName = lonNames[i];
            if (!product.containsRasterDataNode(latName) && !product.containsRasterDataNode(lonName)) {
                names[0] = latName;
                names[1] = lonName;
                return names;
            }
        }
        String lonName = lonNames[0] + "_";
        String latName = latNames[0] + "_";
        int index = 1;
        while (product.containsRasterDataNode(latName + index) || product.containsRasterDataNode(lonName + index)) {
            index++;
        }
        return new String[]{latName + index, lonName + index};
    }

    private void setPreferredTiling(Product product) throws IOException {
        final Dimension dimension;
        if (isBadTiling(imageReader)) {
            dimension = JAIUtils.computePreferredTileSize(imageReader.getWidth(FIRST_IMAGE),
                    imageReader.getHeight(FIRST_IMAGE), 1);
        } else {
            dimension = new Dimension(imageReader.getTileWidth(FIRST_IMAGE), imageReader.getTileHeight(FIRST_IMAGE));
        }
        product.setPreferredTileSize(dimension);
    }

    private MultiLevelImage getMultiLevelImageSourceImage(final Band band, final int bandIndex) throws IOException {
        MultiLevelModel model = ImageManager.getMultiLevelModel(band);
        Assert.state(model.getLevelCount() == 1 || model.getScale(1) == 2.0);

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(model) {
            @Override
            protected RenderedImage createImage(int level) {
                final ImageReadParam readParam = new ImageReadParam(); //imageReader.getDefaultReadParam();
                readParam.setSourceBands(new int[]{bandIndex});
                readParam.setDestinationBands(new int[]{bandIndex});
//double scale = this.getModel().getScale(level);
//System.out.println("level = " + level + ", scale = " + scale);
                if (level > 0) {
                    int sourceSubsampling = 1 << level;
                    readParam.setSourceSubsampling(sourceSubsampling, sourceSubsampling, 0, 0);
                }
//readParam.setDestination(new BufferedImage());
//ImageTypeSpecifier imageType = imageReader.getRawImageType(FIRST_IMAGE);
//SampleModel destSampleModel = imageType.getSampleModel().createSubsetSampleModel(new int[]{bandIndex});
//ColorModel destColorModel = PlanarImage.createColorModel(destSampleModel);
//ColorModel destColorModel = imageType.getColorModel();
//readParam.setDestinationType(new ImageTypeSpecifier(destColorModel, destSampleModel));
//readParam.setDestinationType(imageType);
                TIFFRenderedImage tiffImage;
                try {
                    tiffImage = (TIFFRenderedImage) imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                RenderedImage bandImage;
                int numBands = tiffImage.getSampleModel().getNumBands();
                if (numBands == 1) {
                    bandImage = tiffImage;
                } else {
//System.out.println(">>>>>>>>>>>>>>>>>>>>>>> GeoTIFF: getBandSourceImage(" + bandIndex + "): " + numBands);
                    bandImage = BandSelectDescriptor.create(tiffImage, new int[]{bandIndex}, null);
                }

//System.out.println(">>>>>>>>>>>>>>>>>>>>>> dataType = " + dataType + ", tiling: " + bandImage.getTileWidth() + ", " + bandImage.getTileHeight());
// If the following line doesn't compile, use the following (because MultiLevelModel.getImageBounds() is new):
// Rectangle expectedImageBounds = getModel().getModelToImageTransform(level).createTransformedShape(getModel().getModelBounds()).getBounds();
                Rectangle expectedImageBounds = getModel().getModelToImageTransform(level).createTransformedShape(getModel().getModelBounds()).getBounds();
                if (bandImage.getWidth() < expectedImageBounds.width
                        || bandImage.getHeight() < expectedImageBounds.height) {
                    final int rightBorder = expectedImageBounds.width - bandImage.getWidth();
                    final int bottomBorder = expectedImageBounds.height - bandImage.getHeight();

//                    System.out.println("right: " + rightBorder + "   bottom: " + bottomBorder);
                    bandImage = BorderDescriptor.create(bandImage,
                            0,
                            rightBorder,
                            0,
                            bottomBorder,
                            BorderExtender.createInstance(BorderExtender.BORDER_COPY),
                            null);
                }
                Dimension expectedTileSize = band.getProduct().getPreferredTileSize();
                if (bandImage.getTileWidth() != expectedTileSize.width
                        || bandImage.getTileHeight() != expectedTileSize.height) {
                    ImageLayout imageLayout = new ImageLayout();
                    SampleModel sampleModel = bandImage.getSampleModel();
//imageLayout.setSampleModel(sampleModel);
                    imageLayout.setTileWidth(expectedTileSize.width);
                    imageLayout.setTileHeight(expectedTileSize.height);
//imageLayout.setTileWidth(bandImage.getWidth());
//imageLayout.setTileHeight(Math.min(64, bandImage.getHeight()));
                    RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                    bandImage = FormatDescriptor.create(bandImage, sampleModel.getDataType(), renderingHints);
                }

                final int sourceDataType = bandImage.getSampleModel().getDataType();
                final int targetBeamDataType = band.getDataType();
                final int targetDataType = ImageManager.getDataBufferType(targetBeamDataType);
                if (sourceDataType != targetDataType) {
                    bandImage = FormatDescriptor.create(bandImage, targetDataType, null);
                }

                return bandImage;
            }
        });
    }

    static boolean isBadTiling(TIFFImageReader imageReader) throws IOException {
        final int imageHeight = imageReader.getHeight(FIRST_IMAGE);
        final int tileHeight = imageReader.getTileHeight(FIRST_IMAGE);
        final int imageWidth = imageReader.getWidth(FIRST_IMAGE);
        final int tileWidth = imageReader.getTileWidth(FIRST_IMAGE);
        // @todo 2 tb/tb check if that is a good decision
        return tileWidth <= 1 || tileHeight <= 1 || imageWidth <= tileWidth || imageHeight <= tileHeight;
    }
}
