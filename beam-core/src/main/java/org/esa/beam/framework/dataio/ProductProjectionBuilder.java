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
package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.util.CachingObjectArray;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.*;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A special purpose product reader used to build map-projected data products.
 *
 * @deprecated since BEAM 4.7, replaced by GPF operator 'Reproject'
 */
@Deprecated
public class ProductProjectionBuilder extends AbstractProductBuilder {

    private static final int MAX_NUM_PIXELS_PER_BLOCK = 20000;
    private MapInfo mapInfo;
    private boolean includeTiePointGrids;
    private ElevationModel elevationModel;
    private Map<Pointing, Segmentation> segmentationMap;
    private Map<Long, Map<RasterDataNode, SourceBandLineCache>> threadHashMap;
    private final ReentrantLock lock;

    public ProductProjectionBuilder(MapInfo mapInfo) {
        this(mapInfo, false);
    }

    public ProductProjectionBuilder(MapInfo mapInfo, boolean sourceProductOwner) {
        super(sourceProductOwner);
        this.mapInfo = mapInfo;
        includeTiePointGrids = false;
        elevationModel = null;
        segmentationMap = new HashMap<Pointing, Segmentation>(19);
        threadHashMap = new HashMap<Long, Map<RasterDataNode, SourceBandLineCache>>(19);
        lock = new ReentrantLock();
    }

    public static Product createProductProjection(Product sourceProduct, MapInfo mapInfo, String name,
                                                  String desc) throws IOException {
        return createProductProjection(sourceProduct, false, mapInfo, name, desc);
    }

    public static Product createProductProjection(Product sourceProduct,
                                                  boolean sourceProductOwner,
                                                  MapInfo mapInfo,
                                                  String name,
                                                  String desc) throws IOException {
        return createProductProjection(sourceProduct, sourceProductOwner, false, mapInfo, name, desc);
    }

    public static Product createProductProjection(Product sourceProduct,
                                                  boolean sourceProductOwner,
                                                  boolean includeTiePointGrids,
                                                  MapInfo mapInfo,
                                                  String name,
                                                  String desc) throws IOException {
        ProductProjectionBuilder productProjectionBuilder = new ProductProjectionBuilder(mapInfo, sourceProductOwner);
        productProjectionBuilder.setIncludeTiePointGrids(includeTiePointGrids);
        if (mapInfo.isOrthorectified()) {
            final String demName = mapInfo.getElevationModelName();
            if (demName != null) {
                final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(
                        demName);
                if (!demDescriptor.isDemInstalled()) {
                    throw new IOException("DEM not installed: " + demName);
                }
                productProjectionBuilder.setElevationModel(demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION));
            } else {
                productProjectionBuilder.setElevationModel(null); // force use of elevation from tie-points
            }
        }
        return productProjectionBuilder.readProductNodes(sourceProduct, null, name, desc);
    }

    public MapInfo getMapInfo() {
        return mapInfo;
    }

    public void setMapInfo(MapInfo mapInfo) {
        this.mapInfo = mapInfo;
    }

    public ElevationModel getElevationModel() {
        return elevationModel;
    }

    public void setElevationModel(ElevationModel elevationModel) {
        this.elevationModel = elevationModel;
    }

    public boolean getIncludeTiePointGrids() {
        return includeTiePointGrids;
    }

    public void setIncludeTiePointGrids(boolean includeTiePointtGrids) {
        includeTiePointGrids = includeTiePointtGrids;
    }

    /**
     * Sets the subset information. This implemetation is protected to overwrite in the inherided class to ensure that
     * the subset information cannot be set from the <code>readProductNodes</code> method.
     *
     * @param subsetDef the subset definition
     */
    @Override
    protected void setSubsetDef(ProductSubsetDef subsetDef) {
        // ensures that the subset information in this class is null
        super.setSubsetDef(null);
    }

    /**
     * Reads a data product and returns a in-memory representation of it. This method was called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @throws IllegalArgumentException if <code>input</code> type is not one of the supported input sources.
     * @throws IOException              if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        if (getInput() instanceof Product) {
            sourceProduct = (Product) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());
        }
        if (mapInfo == null) {
            throw new IllegalStateException("no map info set");
        }

        sceneRasterWidth = mapInfo.getSceneWidth();
        sceneRasterHeight = mapInfo.getSceneHeight();

        return createProduct(new MapGeoCoding(mapInfo));
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
        if (elevationModel != null) {
            elevationModel.dispose();
            elevationModel = null;
        }
        segmentationMap.clear();
        segmentationMap = null;
        final Collection<Map<RasterDataNode, SourceBandLineCache>> sourceLineCacheMaps = threadHashMap.values();
        for (Map<RasterDataNode, SourceBandLineCache> lineCacheMap : sourceLineCacheMaps) {
            lineCacheMap.clear();
        }
        threadHashMap.clear();
        threadHashMap = null;
        super.close();
    }

    /**
     * Reads raster data from the data source specified by the given destination band into the given in-memory buffer
     * and region.
     * <p/>
     * <p>For a complete description, please refer to the {@link ProductReader#readBandRasterData(org.esa.beam.framework.datamodel.Band, int, int, int, int, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor)}  interface definition}
     * of this method.
     * <p/>
     * <p>The <code>AbstractProductReader</code> implements this method using the <i>Template Method</i> pattern. The
     * template method in this case is the abstract method to which the call is delegated after an optional spatial
     * subset given by {@link #getSubsetDef()} has been applied to the input parameters.
     *
     * @param destBand    the destination band which identifies the data source from which to readBandRasterDataImpl the
     *                    sample values
     * @param destOffsetX the X-offset in the band's raster co-ordinates
     * @param destOffsetY the Y-offset in the band's raster co-ordinates
     * @param destWidth   the width of region to be readBandRasterDataImpl given in the band's raster co-ordinates
     * @param destHeight  the height of region to be readBandRasterDataImpl given in the band's raster co-ordinates
     * @param destBuffer  the destination buffer which receives the sample values to be readBandRasterDataImpl
     * @param pm          a monitor to inform the user about progress
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements destination buffer not equals <code>destWidth *
     *                                  destHeight</code> or the destination region is out of the band's raster
     * @see #readBandRasterDataImpl
     * @see #getSubsetDef()
     * @see ProductReader#readBandRasterData(org.esa.beam.framework.datamodel.Band, int, int, int, int, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor)
     * @see org.esa.beam.framework.datamodel.Band#getRasterWidth()
     * @see org.esa.beam.framework.datamodel.Band#getRasterHeight()
     */
    @Override
    public void readBandRasterData(final Band destBand,
                                   final int destOffsetX,
                                   final int destOffsetY,
                                   final int destWidth,
                                   final int destHeight,
                                   final ProductData destBuffer,
                                   ProgressMonitor pm) throws IOException {

        Guardian.assertNotNull("destBand", destBand);
        Guardian.assertNotNull("destBuffer", destBuffer);
        if (destBuffer.getNumElems() != destWidth * destHeight) {
            throw new IllegalArgumentException("destBuffer.getNumElems() != destWidth * destHeight");
        }
        pm.beginTask("Performing map-transformation...", 1);
        try {
            readBandRasterDataImpl(destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer,
                                   SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    private void readBandRasterDataImpl(final Band destBand,
                                        final int destOffsetX,
                                        final int destOffsetY,
                                        final int destWidth,
                                        final int destHeight,
                                        final ProductData destBuffer,
                                        final ProgressMonitor pm) throws IOException {
        final RasterDataNode sourceBand = bandMap.get(destBand);
        Debug.assertNotNull(sourceBand);
        Debug.assertTrue(getSubsetDef() == null);

        final int sourceWidth = sourceBand.getSceneRasterWidth();
        final int sourceHeight = sourceBand.getSceneRasterHeight();

        final GeoCoding sourceGeoCoding = getSourceGeoCoding(sourceBand);

        final boolean canInterpolateValues = ProductData.isFloatingPointType(
                destBand.getDataType()) || destBand.isScalingApplied();

        final Resampling sourceResampling;
        if (mapInfo.getResampling() != null && canInterpolateValues) {
            sourceResampling = mapInfo.getResampling();
        } else {
            sourceResampling = Resampling.NEAREST_NEIGHBOUR;
        }

        try {
            lock.lock();
            final Resampling.Index sourceResamplingIndex = sourceResampling.createIndex();
            final SourceBandLineCache sourceBandLineCache = getSourceBandLineCache(sourceBand);
            final SourceRaster sourceRaster = new SourceRaster(sourceBandLineCache);
            final PixelPos[] sourceLineCoords = new PixelPos[destWidth];

            final GeoCoding destGeoCoding = destBand.getProduct().getGeoCoding();
            final Segmentation destSegmentation = getDestSegmentation(sourceBand.getPointing(),
                                                                      destOffsetX,
                                                                      destOffsetY,
                                                                      destWidth,
                                                                      destHeight);

            // Note: we now get the RAW no-data value, because we write data into RAW ProductData buffers
            final double destNoDataValue = destBand.getNoDataValue();

            final int numBlocks = destSegmentation.getNumBlocks();
            for (int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
                if (pm.isCanceled()) {
                    break;
                }

                destSegmentation.initSourcePixelCoords(blockIndex,
                                                       sourceGeoCoding,
                                                       sourceWidth,
                                                       sourceHeight,
                                                       destGeoCoding);

                final int blockOffsetY = destSegmentation.getBlockOffsetY(blockIndex);
                final int numLinesPerBlock = destSegmentation.getNumLines(blockIndex);

                pm.beginTask("Reading raster data...", numLinesPerBlock);
                try {
                    for (int destLineIndex = 0; destLineIndex < numLinesPerBlock; destLineIndex++) {
                        if (pm.isCanceled()) {
                            break;
                        }
                        destSegmentation.getLinePixelCoords(destLineIndex, sourceLineCoords);
                        final float[] minMaxY = ProductUtils.computeMinMaxY(sourceLineCoords);
                        if (minMaxY != null) { // any lines found?
                            final int minY = MathUtils.floorAndCrop(minMaxY[0] - 2, 0, sourceHeight - 1);
                            final int maxY = MathUtils.floorAndCrop(minMaxY[1] + 2, 0, sourceHeight - 1);
                            sourceBandLineCache.setCachedRange(minY, maxY);
                            for (int destX = 0; destX < destWidth; destX++) {
                                final int destBufferIndex = destWidth * (blockOffsetY + destLineIndex) + destX;
                                final PixelPos sourcePixelPos = sourceLineCoords[destX];
                                double destSample = destNoDataValue;
                                if (sourcePixelPos != null) {
                                    sourceResampling.computeIndex(sourcePixelPos.x,
                                                                  sourcePixelPos.y,
                                                                  sourceWidth,
                                                                  sourceHeight,
                                                                  sourceResamplingIndex);
                                    try {
                                        // resample in geophysical units
                                        final float sourceSample = sourceResampling.resample(sourceRaster,
                                                                                             sourceResamplingIndex);
                                        if (!Float.isNaN(sourceSample)) {
                                            // convert to RAW data units
                                            destSample = destBand.scaleInverse(sourceSample);
                                        }
                                    } catch (Exception e) {
                                        throw convertToIOException(e);
                                    }
                                }
                                destBuffer.setElemDoubleAt(destBufferIndex, destSample);
                            }
                        } else { // no lines found
                            for (int x = 0; x < destWidth; x++) {
                                final int destBufferIndex = destWidth * (blockOffsetY + destLineIndex) + x;
                                destBuffer.setElemDoubleAt(destBufferIndex, destNoDataValue);
                            }
                        }
                        pm.worked(1);
                    }
                } finally {
                    pm.done();
                }
            }
        } finally {
            lock.unlock();
        }

    }

    public void getSourceLinePixelCoords(final Band destBand,
                                         final int destOffsetX,
                                         final int destOffsetY,
                                         final PixelPos[] sourceLineCoords) {
        final RasterDataNode sourceBand = bandMap.get(destBand);
        Debug.assertNotNull(sourceBand);
        Debug.assertTrue(getSubsetDef() == null);

        final Segmentation destSegmentation = getDestSegmentation(sourceBand.getPointing(),
                                                                  destOffsetX,
                                                                  destOffsetY,
                                                                  sourceLineCoords.length,
                                                                  1);

        destSegmentation.initSourcePixelCoords(0,
                                               getSourceGeoCoding(sourceBand),
                                               sourceBand.getSceneRasterWidth(),
                                               sourceBand.getSceneRasterHeight(),
                                               destBand.getProduct().getGeoCoding());

        destSegmentation.getLinePixelCoords(0, sourceLineCoords);
    }

    private GeoCoding getSourceGeoCoding(final RasterDataNode sourceBand) {
        final GeoCoding sourceGeoCoding;
        if (getMapInfo().isOrthorectified() && sourceBand.canBeOrthorectified()) {
            sourceGeoCoding = createOrthorectifier(sourceBand);
        } else {
            sourceGeoCoding = sourceBand.getGeoCoding();
        }
        return sourceGeoCoding;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("invalid call");
    }

    private Product createProduct(final MapGeoCoding targetGC) {
        Debug.assertNotNull(getSourceProduct());
        Debug.assertTrue(getSceneRasterWidth() > 0);
        Debug.assertTrue(getSceneRasterHeight() > 0);
        final String newProductName;
        if (this.newProductName == null || this.newProductName.length() == 0) {
            newProductName = getSourceProduct().getName();
        } else {
            newProductName = this.newProductName;
        }
        final Product product = new Product(newProductName, getSourceProduct().getProductType(),
                                            getSceneRasterWidth(),
                                            getSceneRasterHeight(),
                                            this);
        if (newProductDesc == null || newProductDesc.length() == 0) {
            product.setDescription(getSourceProduct().getDescription());
        } else {
            product.setDescription(newProductDesc);
        }
        if (!isMetadataIgnored()) {
            addMetadataToProduct(product);
            addFlagCodingsToProduct(product);
            addIndexCodingsToProduct(product);
        }
        addGeoCodingToProduct(targetGC, product);
        addBandsToProduct(product);
        ProductUtils.copyMasks(getSourceProduct(), product);
        ProductUtils.copyOverlayMasks(getSourceProduct(), product);
        if (getSourceProduct().getPinGroup().getNodeCount() > 0) {
            copyPlacemarks(getSourceProduct().getPinGroup(), product.getPinGroup(),
                           PinDescriptor.getInstance());
        }
        if (getSourceProduct().getGcpGroup().getNodeCount() > 0) {
            copyPlacemarks(getSourceProduct().getGcpGroup(), product.getGcpGroup(),
                           GcpDescriptor.getInstance());
        }
        product.setPreferredTileSize(64, 64);
        return product;
    }

    @Deprecated
    private static void copyPlacemarks(ProductNodeGroup<Placemark> sourcePlacemarkGroup,
                                       ProductNodeGroup<Placemark> targetPlacemarkGroup, PlacemarkDescriptor descriptor) {
        final Placemark[] placemarks = sourcePlacemarkGroup.toArray(new Placemark[0]);
        for (Placemark placemark : placemarks) {
            final Placemark placemark1 = Placemark.createPointPlacemark(descriptor, placemark.getName(), placemark.getLabel(),
                                                                        placemark.getDescription(), null, placemark.getGeoPos(),
                                                                        targetPlacemarkGroup.getProduct().getGeoCoding());
            targetPlacemarkGroup.add(placemark1);
        }
    }

    private void addBandsToProduct(Product targetProduct) {
        ProductUtils.copyBandsForGeomTransform(getSourceProduct(), targetProduct, includeTiePointGrids,
                                               mapInfo.getNoDataValue(),
                                               bandMap);
    }

    private static void addGeoCodingToProduct(final MapGeoCoding targetGC, Product product) {
        product.setGeoCoding(targetGC);
    }


    private Orthorectifier createOrthorectifier(final RasterDataNode sourceBand) {
        return new Orthorectifier2(sourceBand.getSceneRasterWidth(),
                                   sourceBand.getSceneRasterHeight(),
                                   sourceBand.getPointing(),
                                   elevationModel, 25);
    }


    private SourceBandLineCache getSourceBandLineCache(final RasterDataNode sourceBand) {
        final long threadId = Thread.currentThread().getId();
        Map<RasterDataNode, SourceBandLineCache> sourceLineCacheMap;
        if (threadHashMap.containsKey(threadId)) {
            sourceLineCacheMap = threadHashMap.get(threadId);
        } else {
            sourceLineCacheMap = new ConcurrentHashMap<RasterDataNode, SourceBandLineCache>(19);
            threadHashMap.put(threadId, sourceLineCacheMap);
        }
        SourceBandLineCache sourceLineCache = sourceLineCacheMap.get(sourceBand);
        if (sourceLineCache == null) {
            sourceLineCache = new SourceBandLineCache(sourceBand);
            sourceLineCacheMap.put(sourceBand, sourceLineCache);
        }
        return sourceLineCache;
    }

    private Segmentation getDestSegmentation(final Pointing sourcePointing,
                                             final int destOffsetX,
                                             final int destOffsetY,
                                             final int destWidth,
                                             final int destHeight) {
        Segmentation segmentation = segmentationMap.get(sourcePointing);
        if (segmentation != null) {
            if (!segmentation.coversSameRegion(destOffsetX, destOffsetY, destWidth, destHeight)) {
                segmentation = null;
            }
        }
        if (segmentation == null) {
            segmentation = new Segmentation(MAX_NUM_PIXELS_PER_BLOCK / destWidth, destOffsetX, destOffsetY, destWidth,
                                            destHeight);
            segmentationMap.put(sourcePointing, segmentation);
        }
        return segmentation;
    }

    private static IOException convertToIOException(Exception e) {
        IOException ioe;
        if (e instanceof IOException) {
            ioe = (IOException) e;
        } else {
            ioe = new IOException(e.getClass().getName() + ": " + e.getMessage());
            ioe.initCause(e);
            ioe.setStackTrace(e.getStackTrace());
        }
        return ioe;
    }

    static class Segmentation {

        private final int destOffsetX;
        private final int destOffsetY;
        private final int destWidth;
        private final int destHeight;
        private final int numBlocks;
        private final int numLinesMax;
        private PixelPos[] pixelCoordsOfBlock0;
        private PixelPos[] pixelCoords;

        Segmentation(final int maxNumLinesPerBlock,
                     final int destOffsetX,
                     final int destOffsetY,
                     final int destWidth,
                     final int destHeight) {
            this.destOffsetX = destOffsetX;
            this.destOffsetY = destOffsetY;
            this.destWidth = destWidth;
            this.destHeight = destHeight;
            numLinesMax = maxNumLinesPerBlock <= 0 ? 1 : maxNumLinesPerBlock;
            if (destHeight <= numLinesMax) {
                numBlocks = 1;
            } else {
                numBlocks = destHeight / numLinesMax + 1;
            }
            pixelCoordsOfBlock0 = null;
            pixelCoords = null;
        }

        public int getNumBlocks() {
            return numBlocks;
        }

        public int getDestOffsetX() {
            return destOffsetX;
        }

        public int getDestOffsetY() {
            return destOffsetY;
        }

        public int getDestWidth() {
            return destWidth;
        }

        public int getDestHeight() {
            return destHeight;
        }

        public int getBlockOffsetY(final int blockIndex) {
            return blockIndex * numLinesMax;
        }

        public int getLineStartY(final int blockIndex) {
            return destOffsetY + getBlockOffsetY(blockIndex);
        }

        public int getNumLinesMax() {
            return numLinesMax;
        }

        public int getNumLines(final int blockIndex) {
            final int restHeight = destHeight - getBlockOffsetY(blockIndex);
            return restHeight > numLinesMax ? numLinesMax : restHeight;
        }

        public void getLinePixelCoords(int lineY, final PixelPos[] linePixelCoords) {
            System.arraycopy(pixelCoords, lineY * destWidth, linePixelCoords, 0, destWidth);
        }

        public boolean coversSameRegion(int destOffsetX,
                                        int destOffsetY,
                                        int destWidth,
                                        int destHeight) {
            return getDestOffsetX() == destOffsetX &&
                    getDestOffsetY() == destOffsetY &&
                    getDestWidth() == destWidth &&
                    getDestHeight() == destHeight;
        }

        public void initSourcePixelCoords(int blockIndex,
                                          GeoCoding sourceGeoCoding,
                                          int sourceWidth,
                                          int sourceHeight,
                                          GeoCoding destGeoCoding) {

            if (blockIndex == 0) {
                if (pixelCoordsOfBlock0 == null) {
                    pixelCoordsOfBlock0 = computeSourcePixelCoords(blockIndex, sourceGeoCoding, sourceWidth,
                                                                   sourceHeight,
                                                                   destGeoCoding);
                }
                pixelCoords = pixelCoordsOfBlock0;
            } else {
                pixelCoords = computeSourcePixelCoords(blockIndex, sourceGeoCoding, sourceWidth, sourceHeight,
                                                       destGeoCoding);
            }
        }

        private PixelPos[] computeSourcePixelCoords(int iBlock,
                                                    GeoCoding sourceGeoCoding,
                                                    int sourceWidth,
                                                    int sourceHeight,
                                                    GeoCoding destGeoCoding) {
            return ProductUtils.computeSourcePixelCoordinates(sourceGeoCoding,
                                                              sourceWidth,
                                                              sourceHeight,
                                                              destGeoCoding,
                                                              new Rectangle(destOffsetX,
                                                                            getLineStartY(iBlock),
                                                                            destWidth,
                                                                            getNumLines(iBlock)));
        }
    }

    private static class SourceRaster implements Resampling.Raster {

        private final SourceBandLineCache _lineCache;
        private final int _width;
        private final int _height;

        private SourceRaster(SourceBandLineCache lineCache) {
            _lineCache = lineCache;
            RasterDataNode sourceBand = lineCache.getSourceBand();
            _width = sourceBand.getSceneRasterWidth();
            _height = sourceBand.getSceneRasterHeight();
        }

        public int getWidth() {
            return _width;
        }

        public int getHeight() {
            return _height;
        }

        public float getSample(int x, int y) throws Exception {
            if (!_lineCache.getSourceBand().isPixelValid(x, y)) {
                return Float.NaN;
            }
            float[] lineSamples = (float[]) _lineCache.getObject(y);
            return lineSamples[x];
        }
    }

    private static class SourceBandLineCache extends CachingObjectArray {

        private SourceBandLineCache(RasterDataNode sourceBand) {
            super(new SourceBandLineReader(sourceBand));
        }

        public RasterDataNode getSourceBand() {
            return ((SourceBandLineReader) getObjectFactory()).getSourceBand();
        }
    }

    private static class SourceBandLineReader implements CachingObjectArray.ObjectFactory {

        private final RasterDataNode _sourceBand;

        private SourceBandLineReader(RasterDataNode sourceBand) {
            _sourceBand = sourceBand;
        }

        public RasterDataNode getSourceBand() {
            return _sourceBand;
        }

        public Object createObject(int line) throws IOException {
            final int lineWidth = _sourceBand.getSceneRasterWidth();
            final float[] floats = new float[lineWidth];
            _sourceBand.readPixels(0, line, lineWidth, 1, floats, ProgressMonitor.NULL);
            return floats;
        }
    }
}
