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
package org.esa.beam.util;


import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.beam.util.geotiff.GeoTIFFMetadata;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.MathUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides many static factory methods to be used in conjunction with data products.
 *
 * @see org.esa.beam.framework.datamodel.Product
 */
public class ProductUtils {

    private static final int[] RGB_BAND_OFFSETS = new int[]{
            2, 1, 0
    };

    private static final int[] RGBA_BAND_OFFSETS = new int[]{
            3, 2, 1, 0,
    };
    private static final String MSG_CREATING_IMAGE = "Creating image";

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Image Creation Routines
    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates image creation information.
     *
     * @param rasters                 The raster data nodes.
     * @param assignMissingImageInfos if {@code true}, it is ensured that to all {@code RasterDataNode}s a valid {@code ImageInfo} will be assigned.
     * @param pm                      The progress monitor.
     * @return image information
     * @throws IOException if an I/O error occurs
     * @since BEAM 4.2
     */
    public static ImageInfo createImageInfo(RasterDataNode[] rasters, boolean assignMissingImageInfos,
                                            ProgressMonitor pm) throws IOException {
        Assert.notNull(rasters, "rasters");
        Assert.argument(rasters.length == 1 || rasters.length == 3, "rasters.length == 1 || rasters.length == 3");
        if (rasters.length == 1) {
            return assignMissingImageInfos ? rasters[0].getImageInfo(pm) : rasters[0].createDefaultImageInfo(null, pm);
        } else {
            try {
                pm.beginTask("Computing image information", 3);
                final RGBChannelDef rgbChannelDef = new RGBChannelDef();
                for (int i = 0; i < rasters.length; i++) {
                    RasterDataNode raster = rasters[i];
                    final ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
                    ImageInfo imageInfo = assignMissingImageInfos ? raster.getImageInfo(
                            subPm) : raster.createDefaultImageInfo(null, subPm);
                    rgbChannelDef.setSourceName(i, raster.getName());
                    rgbChannelDef.setMinDisplaySample(i, imageInfo.getColorPaletteDef().getMinDisplaySample());
                    rgbChannelDef.setMaxDisplaySample(i, imageInfo.getColorPaletteDef().getMaxDisplaySample());
                }
                return new ImageInfo(rgbChannelDef);
            } finally {
                pm.done();
            }
        }
    }

    /**
     * Creates a RGB image from the given array of <code>{@link RasterDataNode}</code>s.
     * The given array <code>rasters</code> containing one or three raster data nodes. If three rasters are given
     * RGB image is created, if only one raster is provided a gray scale image created.
     *
     * @param rasters   an array of one or three raster nodes.
     * @param imageInfo the image info provides the information how to create the image
     * @param pm        a monitor to inform the user about progress
     * @return the created image
     * @throws IOException if the given raster data is not loaded and reload causes an I/O error
     * @see RasterDataNode#setImageInfo(org.esa.beam.framework.datamodel.ImageInfo)
     */
    public static BufferedImage createRgbImage(final RasterDataNode[] rasters,
                                               final ImageInfo imageInfo,
                                               final ProgressMonitor pm) throws IOException {
        Assert.notNull(rasters, "rasters");
        Assert.argument(rasters.length == 1 || rasters.length == 3, "rasters.length == 1 || rasters.length == 3");

        final RasterDataNode raster0 = rasters[0];
        ProductNodeGroup<Mask> maskGroup = raster0.getOverlayMaskGroup();
        pm.beginTask(MSG_CREATING_IMAGE, 3 + 3 + maskGroup.getNodeCount());
        try {
            BufferedImage overlayBIm;
            if (rasters.length == 1) {
                overlayBIm = create1BandRgbImage(raster0, imageInfo, SubProgressMonitor.create(pm, 3));
            } else {
                overlayBIm = create3BandRgbImage(rasters, imageInfo, SubProgressMonitor.create(pm, 3));
            }
            if (maskGroup.getNodeCount() > 0) {
                overlayBIm = overlayMasks(raster0, overlayBIm, SubProgressMonitor.create(pm, maskGroup.getNodeCount()));
            }
            return overlayBIm;
        } finally {
            pm.done();
        }
    }

    private static BufferedImage create1BandRgbImage(final RasterDataNode raster,
                                                     final ImageInfo imageInfo,
                                                     final ProgressMonitor pm) throws IOException {
        Assert.notNull(raster, "raster");
        Assert.notNull(imageInfo, "imageInfo");
        Assert.argument(imageInfo.getColorPaletteDef() != null, "imageInfo.getColorPaletteDef() != null");
        Assert.notNull(pm, "pm");

        final IndexCoding indexCoding = (raster instanceof Band) ? ((Band) raster).getIndexCoding() : null;
        final int width = raster.getSceneRasterWidth();
        final int height = raster.getSceneRasterHeight();
        final int numPixels = width * height;
        final int numColorComponents = imageInfo.getColorComponentCount();
        final byte[] rgbSamples = new byte[numColorComponents * numPixels];
        final double minSample = imageInfo.getColorPaletteDef().getMinDisplaySample();
        final double maxSample = imageInfo.getColorPaletteDef().getMaxDisplaySample();

        pm.beginTask(MSG_CREATING_IMAGE, 100);
        try {
            Color[] palette;
            final IndexValidator indexValidator;

            // Compute indices into palette --> rgbSamples
            if (indexCoding == null) {
                raster.quantizeRasterData(minSample,
                                          maxSample,
                                          1.0,
                                          rgbSamples,
                                          0,
                                          numColorComponents,
                                          ProgressMonitor.NULL);
                indexValidator = new IndexValidator() {
                    @Override
                    public boolean validateIndex(int pixelIndex) {
                        return raster.isPixelValid(pixelIndex);
                    }
                };
                palette = raster.getImageInfo().getColorPaletteDef().createColorPalette(raster);
                pm.worked(50);
                checkCanceled(pm);
            } else {
                final IntMap sampleColorIndexMap = new IntMap((int) minSample - 1, 4098);
                final ColorPaletteDef.Point[] points = imageInfo.getColorPaletteDef().getPoints();
                for (int colorIndex = 0; colorIndex < points.length; colorIndex++) {
                    sampleColorIndexMap.putValue((int) points[colorIndex].getSample(), colorIndex);
                }
                final int noDataIndex = points.length < 255 ? points.length + 1 : 0;
                final ProductData data = raster.getSceneRasterData();
                for (int pixelIndex = 0; pixelIndex < data.getNumElems(); pixelIndex++) {
                    int sample = data.getElemIntAt(pixelIndex);
                    int colorIndex = sampleColorIndexMap.getValue(sample);
                    rgbSamples[pixelIndex * numColorComponents] = colorIndex != IntMap.NULL ? (byte) colorIndex : (byte) noDataIndex;
                }
                palette = raster.getImageInfo().getColors();
                if (noDataIndex > 0) {
                    palette = Arrays.copyOf(palette, palette.length + 1);
                    palette[palette.length - 1] = ImageInfo.NO_COLOR;
                }
                indexValidator = new IndexValidator() {
                    @Override
                    public boolean validateIndex(int pixelIndex) {
                        return raster.isPixelValid(pixelIndex)
                                && (noDataIndex == 0 || (rgbSamples[pixelIndex * numColorComponents] & 0xff) != noDataIndex);
                    }
                };
                pm.worked(50);
                checkCanceled(pm);
            }

            convertPaletteToRgbSamples(palette, imageInfo.getNoDataColor(), numColorComponents, rgbSamples,
                                       indexValidator);
            pm.worked(40);
            checkCanceled(pm);

            BufferedImage image = createBufferedImage(imageInfo, width, height, rgbSamples);
            image = applyHistogramMatching(image, imageInfo.getHistogramMatching());
            pm.worked(10);
            checkCanceled(pm);

            return image;

        } finally {
            pm.done();
        }
    }


    private static void convertPaletteToRgbSamples(Color[] palette, Color noDataColor, int numColorComponents,
                                                   byte[] rgbSamples, IndexValidator indexValidator) {
        final byte[] r = new byte[palette.length];
        final byte[] g = new byte[palette.length];
        final byte[] b = new byte[palette.length];
        final byte[] a = new byte[palette.length];
        for (int i = 0; i < palette.length; i++) {
            r[i] = (byte) palette[i].getRed();
            g[i] = (byte) palette[i].getGreen();
            b[i] = (byte) palette[i].getBlue();
            a[i] = (byte) palette[i].getAlpha();
        }
        int colorIndex;
        int pixelIndex = 0;
        for (int i = 0; i < rgbSamples.length; i += numColorComponents) {
            if (indexValidator.validateIndex(pixelIndex)) {
                colorIndex = rgbSamples[i] & 0xff;
                if (numColorComponents == 4) {
                    rgbSamples[i] = a[colorIndex];
                    rgbSamples[i + 1] = b[colorIndex];
                    rgbSamples[i + 2] = g[colorIndex];
                    rgbSamples[i + 3] = r[colorIndex];
                } else {
                    rgbSamples[i] = b[colorIndex];
                    rgbSamples[i + 1] = g[colorIndex];
                    rgbSamples[i + 2] = r[colorIndex];
                }
            } else {
                if (numColorComponents == 4) {
                    rgbSamples[i] = (byte) noDataColor.getAlpha();
                    rgbSamples[i + 1] = (byte) noDataColor.getBlue();
                    rgbSamples[i + 2] = (byte) noDataColor.getGreen();
                    rgbSamples[i + 3] = (byte) noDataColor.getRed();
                } else {
                    rgbSamples[i] = (byte) noDataColor.getBlue();
                    rgbSamples[i + 1] = (byte) noDataColor.getGreen();
                    rgbSamples[i + 2] = (byte) noDataColor.getRed();
                }
            }
            pixelIndex++;
        }
    }

    private static BufferedImage create3BandRgbImage(final RasterDataNode[] rasters,
                                                     final ImageInfo imageInfo,
                                                     final ProgressMonitor pm) throws IOException {
        Assert.notNull(rasters, "rasters");
        Assert.argument(rasters.length == 3, "rasters.length == 3");
        Assert.notNull(imageInfo, "imageInfo");
        Assert.argument(imageInfo.getRgbChannelDef() != null, "imageInfo.getRgbChannelDef() != null");
        Assert.notNull(pm, "pm");

        Color noDataColor = imageInfo.getNoDataColor();

        final int width = rasters[0].getSceneRasterWidth();
        final int height = rasters[0].getSceneRasterHeight();
        final int numColorComponents = imageInfo.getColorComponentCount();
        final int numPixels = width * height;
        final byte[] rgbSamples = new byte[numColorComponents * numPixels];

        final String[] taskMessages = new String[]{
                "Computing red channel",
                "Computing green channel",
                "Computing blue channel"
        };

        pm.beginTask(MSG_CREATING_IMAGE, 100);
        try {
            // Compute indices into palette --> rgbSamples
            for (int i = 0; i < rasters.length; i++) {
                final RasterDataNode raster = rasters[i];
                pm.setSubTaskName(taskMessages[i]);
                raster.quantizeRasterData(imageInfo.getRgbChannelDef().getMinDisplaySample(i),
                                          imageInfo.getRgbChannelDef().getMaxDisplaySample(i),
                                          imageInfo.getRgbChannelDef().getGamma(i),
                                          rgbSamples,
                                          numColorComponents - 1 - i,
                                          numColorComponents,
                                          ProgressMonitor.NULL);
                pm.worked(30);
                checkCanceled(pm);
            }

            final boolean validMaskUsed = rasters[0].isValidMaskUsed()
                    || rasters[1].isValidMaskUsed()
                    || rasters[2].isValidMaskUsed();
            boolean pixelValid;
            int pixelIndex = 0;
            for (int i = 0; i < rgbSamples.length; i += numColorComponents) {
                pixelValid = !validMaskUsed
                        || rasters[0].isPixelValid(pixelIndex)
                        && rasters[1].isPixelValid(pixelIndex)
                        && rasters[2].isPixelValid(pixelIndex);
                if (pixelValid) {
                    if (numColorComponents == 4) {
                        rgbSamples[i] = (byte) 255;
                    }
                } else {
                    if (numColorComponents == 4) {
                        rgbSamples[i] = (byte) noDataColor.getAlpha();
                        rgbSamples[i + 1] = (byte) noDataColor.getBlue();
                        rgbSamples[i + 2] = (byte) noDataColor.getGreen();
                        rgbSamples[i + 3] = (byte) noDataColor.getRed();
                    } else {
                        rgbSamples[i] = (byte) noDataColor.getBlue();
                        rgbSamples[i + 1] = (byte) noDataColor.getGreen();
                        rgbSamples[i + 2] = (byte) noDataColor.getRed();
                    }
                }
                pixelIndex++;
            }
            pm.worked(5);
            checkCanceled(pm);


            BufferedImage image = createBufferedImage(imageInfo, width, height, rgbSamples);
            image = applyHistogramMatching(image, imageInfo.getHistogramMatching());
            pm.worked(5);
            checkCanceled(pm);

            return image;

        } finally {
            pm.done();
        }
    }

    private static BufferedImage createBufferedImage(ImageInfo imageInfo, int width, int height, byte[] rgbSamples) {
        final ComponentColorModel cm = imageInfo.createComponentColorModel();
        final DataBuffer db = new DataBufferByte(rgbSamples, rgbSamples.length);
        final int colorComponentCount = imageInfo.getColorComponentCount();
        final WritableRaster wr = Raster.createInterleavedRaster(db, width, height,
                                                                 colorComponentCount * width,
                                                                 colorComponentCount,
                                                                 colorComponentCount == 4 ?
                                                                         RGBA_BAND_OFFSETS : RGB_BAND_OFFSETS,
                                                                 null);
        return new BufferedImage(cm, wr, false, null);
    }


    /**
     * Creates a greyscale image from the given <code>{@link RasterDataNode}</code>.
     * <p/>
     * <p>The method uses the given raster data node's image information (an instance of <code>{@link
     * ImageInfo}</code>) to create the image.
     *
     * @param rasterDataNode the raster data node, must not be <code>null</code>
     * @param pm             a monitor to inform the user about progress
     * @return the color indexed image
     * @throws IOException if the given raster data is not loaded and reload causes an I/O error
     * @see org.esa.beam.framework.datamodel.RasterDataNode#getImageInfo()
     */
    public static BufferedImage createColorIndexedImage(final RasterDataNode rasterDataNode,
                                                        ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("rasterDataNode", rasterDataNode);
        final int width = rasterDataNode.getSceneRasterWidth();
        final int height = rasterDataNode.getSceneRasterHeight();
        final ImageInfo imageInfo = rasterDataNode.getImageInfo(ProgressMonitor.NULL);
        final double newMin = imageInfo.getColorPaletteDef().getMinDisplaySample();
        final double newMax = imageInfo.getColorPaletteDef().getMaxDisplaySample();
        final byte[] colorIndexes = rasterDataNode.quantizeRasterData(newMin, newMax, 1.0, pm);
        final IndexColorModel cm = imageInfo.createIndexColorModel(rasterDataNode);
        final SampleModel sm = cm.createCompatibleSampleModel(width, height);
        final DataBuffer db = new DataBufferByte(colorIndexes, colorIndexes.length);
        final WritableRaster wr = WritableRaster.createWritableRaster(sm, db, null);
        return new BufferedImage(cm, wr, false, null);
    }

    private static BufferedImage applyHistogramMatching(BufferedImage overlayBIm,
                                                        ImageInfo.HistogramMatching histogramMatching) {
        final boolean doEqualize = ImageInfo.HistogramMatching.Equalize == histogramMatching;
        final boolean doNormalize = ImageInfo.HistogramMatching.Normalize == histogramMatching;
        if (doEqualize || doNormalize) {
            PlanarImage sourcePIm = PlanarImage.wrapRenderedImage(overlayBIm);
            sourcePIm = JAIUtils.createTileFormatOp(sourcePIm, 512, 512);
            if (doEqualize) {
                sourcePIm = JAIUtils.createHistogramEqualizedImage(sourcePIm);
            } else {
                sourcePIm = JAIUtils.createHistogramNormalizedImage(sourcePIm);
            }
            overlayBIm = sourcePIm.getAsBufferedImage();
        }
        return overlayBIm;
    }

    private static void checkCanceled(ProgressMonitor pm) throws IOException {
        if (pm.isCanceled()) {
            throw new IOException("Process terminated by user.");
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
// Helpers used for map projections

    /**
     * Retuns a suitable <code>MapInfo</code> instance for the given (geo-coded) product which includes the entire or a
     * subset of the product's scene region for the given map projection. The position of the reference pixel will be
     * the upper left pixel's center (0.5, 0.5).
     *
     * @param product       the product, must not be <code>null</code>
     * @param rect          the rectangle in pixel coordinates of the product, if <code>null</code> the entire region is
     *                      considered
     * @param mapProjection the map projection, must not be <code>null</code>
     * @return the map information instance
     */
    public static MapInfo createSuitableMapInfo(final Product product,
                                                final Rectangle rect,
                                                final MapProjection mapProjection) {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("mapProjection", mapProjection);
        final GeoCoding gc = product.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }
        final int sourceW = product.getSceneRasterWidth();
        final int sourceH = product.getSceneRasterHeight();
        final MapTransform mapTransform = mapProjection.getMapTransform();
        final Point2D[] envelope = createMapEnvelope(product, rect, mapTransform);
        final double mapW = Math.abs(envelope[1].getX() - envelope[0].getX());
        final double mapH = Math.abs(envelope[1].getY() - envelope[0].getY());
        float pixelSize = (float) Math.min(mapW / sourceW, mapH / sourceH);
        if (MathUtils.equalValues(pixelSize, 0.0f)) {
            pixelSize = 1.0f;
        }
        final int targetW = 1 + (int) Math.floor(mapW / pixelSize);
        final int targetH = 1 + (int) Math.floor(mapH / pixelSize);
        final float easting = (float) envelope[0].getX();
        final float northing = (float) envelope[1].getY();
        final MapInfo mapInfo = new MapInfo(mapProjection,
                                            0.5F,
                                            0.5F,
                                            easting,
                                            northing,
                                            pixelSize,
                                            pixelSize,
                                            gc.getDatum());
        mapInfo.setSceneSizeFitted(true);
        mapInfo.setSceneWidth(targetW);
        mapInfo.setSceneHeight(targetH);
        mapInfo.setNoDataValue(MapInfo.DEFAULT_NO_DATA_VALUE);
        return mapInfo;
    }

    /**
     * Retuns a suitable <code>MapInfo</code> instance for the given (geo-coded) product which includes the entire or a
     * subset of the product's scene region for the given map projection. The position of the reference pixel will be the scene center.
     *
     * @param product       the product, must not be <code>null</code>
     * @param mapProjection the map projection, must not be <code>null</code>
     * @param orientation   the orientation angle
     * @param noDataValue   the no-data value to be used
     * @return the map information instance
     */
    public static MapInfo createSuitableMapInfo(final Product product,
                                                final MapProjection mapProjection,
                                                final double orientation,
                                                final double noDataValue) {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("mapProjection", mapProjection);
        final GeoCoding gc = product.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }
        final int sourceW = product.getSceneRasterWidth();
        final int sourceH = product.getSceneRasterHeight();
        final MapTransform mapTransform = mapProjection.getMapTransform();
        final Point2D[] envelope = createMapEnvelope(product, new Rectangle(sourceW, sourceH), mapTransform);
        final Point2D pMin = envelope[0];
        final Point2D pMax = envelope[1];
        double mapW = pMax.getX() - pMin.getX();
        double mapH = pMax.getY() - pMin.getY();

        float pixelSize = (float) Math.min(mapW / sourceW, mapH / sourceH);
        if (MathUtils.equalValues(pixelSize, 0.0f)) {
            pixelSize = 1.0f;
        }
        final int targetW = 1 + (int) Math.floor(mapW / pixelSize);
        final int targetH = 1 + (int) Math.floor(mapH / pixelSize);

        final float pixelX = 0.5f * targetW;
        final float pixelY = 0.5f * targetH;

        final float easting = (float) pMin.getX() + pixelX * pixelSize;
        final float northing = (float) pMax.getY() - pixelY * pixelSize;

        final MapInfo mapInfo = new MapInfo(mapProjection,
                                            pixelX,
                                            pixelY,
                                            easting,
                                            northing,
                                            pixelSize,
                                            pixelSize,
                                            gc.getDatum());
        mapInfo.setOrientation((float) orientation);
        mapInfo.setSceneSizeFitted(true);
        mapInfo.setSceneWidth(targetW);
        mapInfo.setSceneHeight(targetH);
        mapInfo.setNoDataValue(noDataValue);
        return mapInfo;
    }

    public static Dimension getOutputRasterSize(final Product product,
                                                final Rectangle rect,
                                                final MapTransform mapTransform,
                                                final double pixelSizeX,
                                                final double pixelSizeY) {
        final Point2D[] envelope = createMapEnvelope(product, rect, mapTransform);
        final double mapW = envelope[1].getX() - envelope[0].getX();
        final double mapH = envelope[1].getY() - envelope[0].getY();
        return new Dimension(1 + (int) Math.floor(mapW / pixelSizeX),
                             1 + (int) Math.floor(mapH / pixelSizeY));
    }

    /**
     * Creates the boundary in map coordinates for the given product, source rectangle (in product pixel coordinates)
     * and the given map transfromation. The method delegates to {@link #createMapEnvelope(org.esa.beam.framework.datamodel.Product,
     * java.awt.Rectangle, int, org.esa.beam.framework.dataop.maptransf.MapTransform) createMapEnvelope(product, rect,
     * step, mapTransform)} where <code>step</code> is the half of the minimum of the product scene raster width and
     * height.
     *
     * @param product      The product.
     * @param rect         The rectangle in pixel coordinates.
     * @param mapTransform The map transformation.
     * @return The boundary in map coordinates for the given product.
     */
    public static Point2D[] createMapEnvelope(final Product product,
                                              final Rectangle rect,
                                              final MapTransform mapTransform) {
        final int step = Math.min(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / 2;
        return createMapEnvelope(product, rect, step, mapTransform);
    }

    /**
     * Creates the boundary in map coordinates for the given product, source rectangle (in product
     * pixel coordinates) and the given map transfromation. The method delegates to
     * {@link #createMapBoundary(Product, Rectangle, int, MapTransform) createMapBoundary(product, rect,
     * step, mapTransform)} where <code>step</code> is the half of the minimum of the product scene
     * raster width and height.
     *
     * @param product      The product.
     * @param rect         The rectangle in pixel coordinates.
     * @param step         The step size in pixels.
     * @param mapTransform The map transformation.
     * @return The boundary in map coordinates for the given product.
     */
    public static Point2D[] createMapEnvelope(Product product,
                                              Rectangle rect,
                                              int step,
                                              MapTransform mapTransform) {
        Point2D[] boundary = createMapBoundary(product, rect, step, mapTransform);
        return getMinMax(boundary);
    }

    public static Point2D[] getMinMax(Point2D[] boundary) {
        Point2D.Float min = new Point2D.Float();
        Point2D.Float max = new Point2D.Float();
        min.x = +Float.MAX_VALUE;
        min.y = +Float.MAX_VALUE;
        max.x = -Float.MAX_VALUE;
        max.y = -Float.MAX_VALUE;
        for (Point2D point : boundary) {
            min.x = Math.min(min.x, (float) point.getX());
            min.y = Math.min(min.y, (float) point.getY());
            max.x = Math.max(max.x, (float) point.getX());
            max.y = Math.max(max.y, (float) point.getY());
        }
        return new Point2D[]{min, max};
    }

    public static Point2D[] createMapBoundary(Product product, Rectangle rect, int step, MapTransform mapTransform) {
        GeoPos[] geoPoints = createGeoBoundary(product, rect, step);
        normalizeGeoPolygon(geoPoints);
        Point2D[] mapPoints = new Point2D[geoPoints.length];
        for (int i = 0; i < geoPoints.length; i++) {
            mapPoints[i] = mapTransform.forward(geoPoints[i], new Point2D.Float());
        }
        return mapPoints;
    }

    /**
     * Creates the geographical boundary of the given product and returns it as a list of geographical coordinates.
     *
     * @param product the input product, must not be null
     * @param step    the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(Product product, int step) {
        final Rectangle rect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        return createGeoBoundary(product, rect, step);
    }

    /**
     * Creates the geographical boundary of the given region within the given product and returns it as a list of
     * geographical coordinates.
     * <p> This method delegates to {@link #createGeoBoundary(org.esa.beam.framework.datamodel.Product, java.awt.Rectangle, int, boolean) createGeoBoundary(Product, Rectangle, int, boolean)}
     * and the additional boolean parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param product the input product, must not be null
     * @param region  the region rectangle in product pixel coordinates, can be null for entire product
     * @param step    the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createPixelBoundary(org.esa.beam.framework.datamodel.RasterDataNode, java.awt.Rectangle, int)
     */
    public static GeoPos[] createGeoBoundary(Product product, Rectangle region, int step) {
        final boolean usePixelCenter = true;
        return createGeoBoundary(product, region, step, usePixelCenter);
    }

    /**
     * Creates the geographical boundary of the given region within the given product and returns it as a list of
     * geographical coordinates.
     *
     * @param product        the input product, must not be null
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter <code>true</code> if the pixel center should be used to create the boundary
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createPixelBoundary(org.esa.beam.framework.datamodel.Product, java.awt.Rectangle, int, boolean)
     */
    public static GeoPos[] createGeoBoundary(Product product, Rectangle region, int step,
                                             final boolean usePixelCenter) {
        Guardian.assertNotNull("product", product);
        final GeoCoding gc = product.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }
        PixelPos[] points = createPixelBoundary(product, region, step, usePixelCenter);
        GeoPos[] geoPoints = new GeoPos[points.length];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPoints[i] = gc.getGeoPos(points[i], null);
        }
        return geoPoints;
    }

    /**
     * Creates the geographical boundary of the given region within the given raster and returns it as a list of
     * geographical coordinates.
     *
     * @param raster the input raster, must not be null
     * @param region the region rectangle in raster pixel coordinates, can be null for entire raster
     * @param step   the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if raster is null or if the raster has no {@link GeoCoding} is null
     * @see #createPixelBoundary(org.esa.beam.framework.datamodel.RasterDataNode, java.awt.Rectangle, int)
     */
    public static GeoPos[] createGeoBoundary(RasterDataNode raster, Rectangle region, int step) {
        Guardian.assertNotNull("raster", raster);
        final GeoCoding gc = raster.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }
        PixelPos[] points = createPixelBoundary(raster, region, step);
        GeoPos[] geoPoints = new GeoPos[points.length];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPoints[i] = gc.getGeoPos(points[i], null);
        }
        return geoPoints;
    }

    /**
     * Converts the geographic boundary entire product into one, two or three shape objects. If the product does not
     * intersect the 180 degree meridian, a single general path is returned. Otherwise two or three shapes are created
     * and returned in the order from west to east.
     * <p/>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product the input product
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(org.esa.beam.framework.datamodel.Product, int)
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product) {
        final Rectangle rect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final int step = Math.min(rect.width, rect.height) / 8;
        return createGeoBoundaryPaths(product, rect, step > 0 ? step : 1);
    }

    /**
     * Converts the geographic boundary of the region within the given product into one, two or three shape objects. If
     * the product does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     * <p/>
     * This method delegates to {@link #createGeoBoundaryPaths(org.esa.beam.framework.datamodel.Product, java.awt.Rectangle, int, boolean) createGeoBoundaryPaths(Product, Rectangle, int, boolean)}
     * and the additional parameter <code>usePixelCenter</code> is <code>true</code>.
     * <p/>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product the input product
     * @param region  the region rectangle in product pixel coordinates, can be null for entire product
     * @param step    the step given in pixels
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(org.esa.beam.framework.datamodel.Product, java.awt.Rectangle, int)
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step) {
        final boolean usePixelCenter = true;
        return createGeoBoundaryPaths(product, region, step, usePixelCenter);
    }

    /**
     * Converts the geographic boundary of the region within the given product into one, two or three shape objects. If
     * the product does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     * <p/>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product        the input product
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter <code>true</code> if the pixel center should be used to create the pathes
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(org.esa.beam.framework.datamodel.Product, java.awt.Rectangle, int, boolean)
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step,
                                                       final boolean usePixelCenter) {
        Guardian.assertNotNull("product", product);
        final GeoCoding gc = product.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }
        final GeoPos[] geoPoints = createGeoBoundary(product, region, step, usePixelCenter);
        normalizeGeoPolygon(geoPoints);

        final ArrayList<GeneralPath> pathList = assemblePathList(geoPoints);

        return pathList.toArray(new GeneralPath[pathList.size()]);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * <code>rect</code> is 100 x 50 pixels and <code>step</code> is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p/>
     * <p>This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p> This method delegates to {@link #createPixelBoundary(org.esa.beam.framework.datamodel.Product, java.awt.Rectangle, int, boolean) createPixelBoundary(Product, Rectangle, int, boolean)}
     * and the additional boolean parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param product the product
     * @param rect    the source rectangle
     * @param step    the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(Product product, Rectangle rect, int step) {
        final boolean usePixelCenter = true;
        return createPixelBoundary(product, rect, step, usePixelCenter);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * <code>rect</code> is 100 x 50 pixels and <code>step</code> is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p/>
     * <p>This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     *
     * @param product        the product
     * @param rect           the source rectangle
     * @param step           the mean distance from one pixel position to the other in the returned array
     * @param usePixelCenter <code>true</code> if the pixel center should be used to create the boundary
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(Product product, Rectangle rect, int step,
                                                 final boolean usePixelCenter) {
        if (rect == null) {
            rect = new Rectangle(0,
                                 0,
                                 product.getSceneRasterWidth(),
                                 product.getSceneRasterHeight());
        }
        return createRectBoundary(rect, step, usePixelCenter);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * <code>rect</code> is 100 x 50 pixels and <code>step</code> is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p/>
     * <p>This method is used for an intermediate step when determining a raster boundary expressed in geographical
     * co-ordinates.
     *
     * @param raster the raster
     * @param rect   the source rectangle
     * @param step   the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(RasterDataNode raster, Rectangle rect, int step) {
        if (rect == null) {
            rect = new Rectangle(0,
                                 0,
                                 raster.getSceneRasterWidth(),
                                 raster.getSceneRasterHeight());
        }
        return createRectBoundary(rect, step);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * <code>rect</code> is 100 x 50 pixels and <code>step</code> is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p> This method delegates to {@link #createRectBoundary(java.awt.Rectangle, int, boolean) createRectBoundary(Rectangle, int, boolean)}
     * and the additional boolean parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param rect the source rectangle
     * @param step the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createRectBoundary(Rectangle rect, int step) {
        final boolean usePixelCenter = true;
        return createRectBoundary(rect, step, usePixelCenter);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * <code>rect</code> is 100 x 50 pixels and <code>step</code> is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p/>
     * This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p/>
     *
     * @param rect           the source rectangle
     * @param step           the mean distance from one pixel position to the other in the returned array
     * @param usePixelCenter <code>true</code> if the pixel center should be used
     * @return the rectangular boundary
     */
    public static PixelPos[] createRectBoundary(final Rectangle rect, int step, final boolean usePixelCenter) {
        final float insetDistance = usePixelCenter ? 0.5f : 0.0f;
        final int x1 = rect.x;
        final int y1 = rect.y;
        final int w = usePixelCenter ? rect.width - 1 : rect.width;
        final int h = usePixelCenter ? rect.height - 1 : rect.height;
        final int x2 = x1 + w;
        final int y2 = y1 + h;

        if (step <= 0) {
            step = 2 * Math.max(rect.width, rect.height); // don't step!
        }

        final ArrayList<PixelPos> pixelPosList = new ArrayList<PixelPos>(2 * (rect.width + rect.height) / step + 10);

        int lastX = 0;
        for (int x = x1; x < x2; x += step) {
            pixelPosList.add(new PixelPos(x + insetDistance, y1 + insetDistance));
            lastX = x;
        }

        int lastY = 0;
        for (int y = y1; y < y2; y += step) {
            pixelPosList.add(new PixelPos(x2 + insetDistance, y + insetDistance));
            lastY = y;
        }

        pixelPosList.add(new PixelPos(x2 + insetDistance, y2 + insetDistance));

        for (int x = lastX; x > x1; x -= step) {
            pixelPosList.add(new PixelPos(x + insetDistance, y2 + insetDistance));
        }

        pixelPosList.add(new PixelPos(x1 + insetDistance, y2 + insetDistance));

        for (int y = lastY; y > y1; y -= step) {
            pixelPosList.add(new PixelPos(x1 + insetDistance, y + insetDistance));
        }

        return pixelPosList.toArray(new PixelPos[pixelPosList.size()]);
    }

    /**
     * Copies the flag codings from the source product to the target.
     *
     * @param source the source product
     * @param target the target product
     */
    public static void copyFlagCodings(Product source, Product target) {
        Guardian.assertNotNull("source", source);
        Guardian.assertNotNull("target", target);

        int numCodings = source.getFlagCodingGroup().getNodeCount();
        for (int n = 0; n < numCodings; n++) {
            FlagCoding sourceFlagCoding = source.getFlagCodingGroup().get(n);
            copyFlagCoding(sourceFlagCoding, target);
        }
    }

    /**
     * Copies the given source flag coding to the target product.
     * If it exists already, the method simply returns the existing instance.
     *
     * @param sourceFlagCoding the source flag coding
     * @param target           the target product
     * @return The flag coding.
     */
    public static FlagCoding copyFlagCoding(FlagCoding sourceFlagCoding, Product target) {
        FlagCoding flagCoding = target.getFlagCodingGroup().get(sourceFlagCoding.getName());
        if (flagCoding == null) {
            flagCoding = new FlagCoding(sourceFlagCoding.getName());
            flagCoding.setDescription(sourceFlagCoding.getDescription());
            target.getFlagCodingGroup().add(flagCoding);
            copyMetadata(sourceFlagCoding, flagCoding);
        }
        return flagCoding;
    }

    /**
     * Copies the given source index coding to the target product
     * If it exists already, the method simply returns the existing instance.
     *
     * @param sourceIndexCoding the source index coding
     * @param target            the target product
     * @return The index coding.
     */
    public static IndexCoding copyIndexCoding(IndexCoding sourceIndexCoding, Product target) {
        IndexCoding indexCoding = target.getIndexCodingGroup().get(sourceIndexCoding.getName());
        if (indexCoding == null) {
            indexCoding = new IndexCoding(sourceIndexCoding.getName());
            indexCoding.setDescription(sourceIndexCoding.getDescription());
            target.getIndexCodingGroup().add(indexCoding);
            copyMetadata(sourceIndexCoding, indexCoding);
        }
        return indexCoding;
    }

    /**
     * Copies the {@link Mask}s from the source product to the target product.
     * <p/>
     * IMPORTANT NOTE: This method should only be used, if it is known that all masks
     * in the source product will also be valid in the target product. This method does
     * <em>not</em> copy overlay masks from the source bands to the target bands. Also
     * note that a source mask is not copied to the target product, when there already
     * is a mask in the target product with the same name as the source mask.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     */
    public static void copyMasks(Product sourceProduct, Product targetProduct) {
        final ProductNodeGroup<Mask> sourceMaskGroup = sourceProduct.getMaskGroup();
        for (int i = 0; i < sourceMaskGroup.getNodeCount(); i++) {
            final Mask mask = sourceMaskGroup.get(i);
            if (!targetProduct.getMaskGroup().contains(mask.getName())
                    && mask.getImageType().canTransferMask(mask, targetProduct)) {
                mask.getImageType().transferMask(mask, targetProduct);
            }
        }
    }

    /**
     * Copies the overlay {@link Mask}s from the source product's raster data nodes to
     * the target product's raster data nodes.
     * <p/>
     * IMPORTANT NOTE: This method should only be used, if it is known that all masks
     * in the source product will also be valid in the target product. This method does
     * <em>not</em> copy overlay masks, which are not contained in the target product's
     * mask group.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     */
    public static void copyOverlayMasks(Product sourceProduct, Product targetProduct) {
        for (RasterDataNode sourceNode : sourceProduct.getTiePointGrids()) {
            copyOverlayMasks(sourceNode, targetProduct);
        }
        for (RasterDataNode sourceNode : sourceProduct.getBands()) {
            copyOverlayMasks(sourceNode, targetProduct);
        }
    }

    /**
     * Copies the ROI {@link Mask}s from the source product's raster data nodes to
     * the target product's raster data nodes.
     * <p/>
     * IMPORTANT NOTE: This method should only be used, if it is known that all masks
     * in the source product will also be valid in the target product. This method does
     * <em>not</em> copy ROI masks, which are not contained in the target product's
     * mask group.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     * @deprecated since BEAM 4.10 (no replacement)
     */
    @Deprecated
    public static void copyRoiMasks(Product sourceProduct, Product targetProduct) {
        for (RasterDataNode sourceNode : sourceProduct.getTiePointGrids()) {
            copyRoiMasks(sourceNode, targetProduct);
        }
        for (RasterDataNode sourceNode : sourceProduct.getBands()) {
            copyRoiMasks(sourceNode, targetProduct);
        }
    }

    private static void copyOverlayMasks(final RasterDataNode sourceNode, final Product targetProduct) {
        String[] maskNames = sourceNode.getOverlayMaskGroup().getNodeNames();
        RasterDataNode targetNode = targetProduct.getRasterDataNode(sourceNode.getName());
        if (targetNode != null) {
            ProductNodeGroup<Mask> overlayMaskGroup = targetNode.getOverlayMaskGroup();
            ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
            addMasksToGroup(maskNames, maskGroup, overlayMaskGroup);
        }
    }

    /*
     * @deprecated since BEAM 4.10 (no replacement)
     */
    @Deprecated
    private static void copyRoiMasks(final RasterDataNode sourceNode, final Product targetProduct) {
        String[] maskNames = sourceNode.getRoiMaskGroup().getNodeNames();
        RasterDataNode targetNode = targetProduct.getRasterDataNode(sourceNode.getName());
        if (targetNode != null) {
            ProductNodeGroup<Mask> roiMaskGroup = targetNode.getRoiMaskGroup();
            ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
            addMasksToGroup(maskNames, maskGroup, roiMaskGroup);
        }
    }


    private static void addMasksToGroup(String[] maskNames, ProductNodeGroup<Mask> maskGroup,
                                        ProductNodeGroup<Mask> specialMaskGroup) {
        for (String maskName : maskNames) {
            final Mask mask = maskGroup.get(maskName);
            if (mask != null) {
                specialMaskGroup.add(mask);
            }
        }
    }

    /**
     * Copies all bands which contain a flagcoding from the source product to the target product.
     *
     * @param sourceProduct   the source product
     * @param targetProduct   the target product
     * @param copySourceImage whether the source image of the source band should be copied.
     * @since BEAM 4.10
     */
    public static void copyFlagBands(Product sourceProduct, Product targetProduct, boolean copySourceImage) {
        Guardian.assertNotNull("source", sourceProduct);
        Guardian.assertNotNull("target", targetProduct);
        if (sourceProduct.getFlagCodingGroup().getNodeCount() > 0) {

            // loop over bands and check if they have a flags coding attached
            for (int i = 0; i < sourceProduct.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBandAt(i);
                String bandName = sourceBand.getName();
                if (sourceBand.isFlagBand() && targetProduct.getBand(bandName) == null) {
                    copyBand(bandName, sourceProduct, targetProduct, copySourceImage);
                }
            }

            // first the bands have to be copied and then the masks
            // other wise the referenced bands, e.g. flag band, is not contained in the target product
            // and the mask is not copied
            copyMasks(sourceProduct, targetProduct);
            copyOverlayMasks(sourceProduct, targetProduct);
        }
    }

    /**
     * Copies all bands which contain a flagcoding from the source product to the target product.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     * @deprecated since BEAM 4.10, use {@link #copyFlagBands(Product, Product, boolean)} instead.
     */
    @Deprecated
    public static void copyFlagBands(Product sourceProduct, Product targetProduct) {
        copyFlagBands(sourceProduct, targetProduct, false);
    }

    /**
     * Copies the named tie-point grid from the source product to the target product.
     *
     * @param gridName      the name of the tie-point grid to be copied.
     * @param sourceProduct the source product
     * @param targetProduct the target product
     * @return the copied tie-point grid, or <code>null</code> if the sourceProduct does not contain a tie-point grid with the given name.
     */
    public static TiePointGrid copyTiePointGrid(String gridName, Product sourceProduct, Product targetProduct) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("targetProduct", targetProduct);

        if (gridName == null || gridName.length() == 0) {
            return null;
        }
        final TiePointGrid sourceGrid = sourceProduct.getTiePointGrid(gridName);
        if (sourceGrid == null) {
            return null;
        }
        final TiePointGrid targetGrid = sourceGrid.cloneTiePointGrid();
        targetProduct.addTiePointGrid(targetGrid);
        return targetGrid;
    }

    /**
     * Copies the named band from the source product to the target product.
     *
     * @param sourceBandName  the name of the band to be copied.
     * @param sourceProduct   the source product.
     * @param targetProduct   the target product.
     * @param copySourceImage whether the source image of the source band should be copied.
     * @return the copy of the band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     * @since BEAM 4.10
     */
    public static Band copyBand(String sourceBandName, Product sourceProduct, Product targetProduct, boolean copySourceImage) {
        return copyBand(sourceBandName, sourceProduct, sourceBandName, targetProduct, copySourceImage);
    }

    /**
     * Copies the named band from the source product to the target product.
     *
     * @param sourceBandName  the name of the band to be copied.
     * @param sourceProduct   the source product.
     * @param targetBandName  the name of the band copied.
     * @param targetProduct   the target product.
     * @param copySourceImage whether the source image of the source band should be copied.
     * @return the copy of the band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     * @since BEAM 4.10
     */
    public static Band copyBand(String sourceBandName, Product sourceProduct,
                                String targetBandName, Product targetProduct, boolean copySourceImage) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("targetProduct", targetProduct);

        if (sourceBandName == null || sourceBandName.length() == 0) {
            return null;
        }
        final Band sourceBand = sourceProduct.getBand(sourceBandName);
        if (sourceBand == null) {
            return null;
        }
        Band targetBand = targetProduct.addBand(targetBandName, sourceBand.getDataType());
        copyRasterDataNodeProperties(sourceBand, targetBand);
        if (copySourceImage) {
            targetBand.setSourceImage(sourceBand.getSourceImage());
        }
        return targetBand;
    }

    /**
     * Copies all properties from source band to the target band.
     *
     * @param sourceRaster the source band
     * @param targetRaster the target band
     * @see #copySpectralBandProperties(org.esa.beam.framework.datamodel.Band, org.esa.beam.framework.datamodel.Band)
     */
    public static void copyRasterDataNodeProperties(RasterDataNode sourceRaster, RasterDataNode targetRaster) {
        targetRaster.setDescription(sourceRaster.getDescription());
        targetRaster.setUnit(sourceRaster.getUnit());
        targetRaster.setScalingFactor(sourceRaster.getScalingFactor());
        targetRaster.setScalingOffset(sourceRaster.getScalingOffset());
        targetRaster.setLog10Scaled(sourceRaster.isLog10Scaled());
        targetRaster.setNoDataValueUsed(sourceRaster.isNoDataValueUsed());
        targetRaster.setNoDataValue(sourceRaster.getNoDataValue());
        targetRaster.setValidPixelExpression(sourceRaster.getValidPixelExpression());
        if (sourceRaster instanceof Band && targetRaster instanceof Band) {
            Band sourceBand = (Band) sourceRaster;
            Band targetBand = (Band) targetRaster;
            copySpectralBandProperties(sourceBand, targetBand);
            Product targetProduct = targetBand.getProduct();
            if (targetProduct == null) {
                return;
            }
            if (sourceBand.getFlagCoding() != null) {
                FlagCoding srcFlagCoding = sourceBand.getFlagCoding();
                copyFlagCoding(srcFlagCoding, targetProduct);
                targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(srcFlagCoding.getName()));
            }
            if (sourceBand.getIndexCoding() != null) {
                IndexCoding srcIndexCoding = sourceBand.getIndexCoding();
                copyIndexCoding(srcIndexCoding, targetProduct);
                targetBand.setSampleCoding(targetProduct.getIndexCodingGroup().get(srcIndexCoding.getName()));
            }
        }
    }

    /**
     * Copies the named band from the source product to the target product.
     *
     * @param sourceBandName the name of the band to be copied.
     * @param sourceProduct  the source product.
     * @param targetProduct  the target product.
     * @return the copy of the band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     * @deprecated since BEAM 4.10, use {@link #copyBand(String, Product, Product, boolean)} instead.
     */
    @Deprecated
    public static Band copyBand(String sourceBandName, Product sourceProduct, Product targetProduct) {
        return copyBand(sourceBandName, sourceProduct, sourceBandName, targetProduct, false);
    }

    /**
     * Copies the named band from the source product to the target product.
     *
     * @param sourceBandName the name of the band to be copied.
     * @param sourceProduct  the source product.
     * @param targetBandName the name of the band copied.
     * @param targetProduct  the target product.
     * @return the copy of the band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     * @deprecated since BEAM 4.10, use {@link #copyBand(String, Product, String, Product, boolean)} instead.
     */
    @Deprecated
    public static Band copyBand(String sourceBandName, Product sourceProduct,
                                String targetBandName, Product targetProduct) {
        return copyBand(sourceBandName, sourceProduct, targetBandName, targetProduct, false);
    }

    /**
     * Copies the spectral properties from source band to target band. These properties are:
     * <ul>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSpectralBandIndex() spectral band index},</li>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSpectralWavelength() the central wavelength},</li>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSpectralBandwidth() the spectral bandwidth} and</li>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSolarFlux() the solar spectral flux}.</li>
     * </ul>
     *
     * @param sourceBand the source band
     * @param targetBand the target band
     * @see #copyRasterDataNodeProperties(org.esa.beam.framework.datamodel.RasterDataNode, org.esa.beam.framework.datamodel.RasterDataNode)
     */
    public static void copySpectralBandProperties(Band sourceBand, Band targetBand) {
        Guardian.assertNotNull("source", sourceBand);
        Guardian.assertNotNull("target", targetBand);

        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
        targetBand.setSolarFlux(sourceBand.getSolarFlux());
    }


    /**
     * Copies the geocoding from the source product to target product.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     * @throws IllegalArgumentException if one of the params is <code>null</code>.
     */
    public static void copyGeoCoding(final Product sourceProduct, final Product targetProduct) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("targetProduct", targetProduct);
        sourceProduct.transferGeoCodingTo(targetProduct, null);
    }

    /**
     * Copies all tie point grids from one product to another.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     */
    public static void copyTiePointGrids(Product sourceProduct, Product targetProduct) {
        for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
            TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
            targetProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
        }
    }

    public static void copyVectorData(Product sourceProduct, Product targetProduct) {

        ProductNodeGroup<VectorDataNode> vectorDataGroup = sourceProduct.getVectorDataGroup();

        // Note that since BEAM 4.10, we always have 2 permanent VDNs: pins and ground_control_points
        boolean allPermanentAndEmpty = true;
        for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
            VectorDataNode sourceVDN = vectorDataGroup.get(i);
            if (!sourceVDN.isPermanent() || !sourceVDN.getFeatureCollection().isEmpty()) {
                allPermanentAndEmpty = false;
                break;
            }
        }
        if (allPermanentAndEmpty) {
            return;
        }

        if (sourceProduct.isCompatibleProduct(targetProduct, 1.0e-3f)) {
            for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
                VectorDataNode sourceVDN = vectorDataGroup.get(i);
                String name = sourceVDN.getName();

                FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = sourceVDN.getFeatureCollection();
                featureCollection = new DefaultFeatureCollection(featureCollection);
                if (!targetProduct.getVectorDataGroup().contains(name)) {
                    targetProduct.getVectorDataGroup().add(new VectorDataNode(name, featureCollection.getSchema()));
                }
                VectorDataNode targetVDN = targetProduct.getVectorDataGroup().get(name);
                targetVDN.getFeatureCollection().addAll(featureCollection);
                targetVDN.setDefaultStyleCss(sourceVDN.getDefaultStyleCss());
                targetVDN.setDescription(sourceVDN.getDescription());
            }
        } else {
            if (sourceProduct.getGeoCoding() == null || targetProduct.getGeoCoding() == null) {
                return;
            }
            Geometry clipGeometry;
            try {
                Geometry sourceGeometryWGS84 = FeatureUtils.createGeoBoundaryPolygon(sourceProduct);
                Geometry targetGeometryWGS84 = FeatureUtils.createGeoBoundaryPolygon(targetProduct);
                if (!sourceGeometryWGS84.intersects(targetGeometryWGS84)) {
                    return;
                }
                clipGeometry = sourceGeometryWGS84.intersection(targetGeometryWGS84);
            } catch (Exception e) {
                return;
            }

            CoordinateReferenceSystem srcModelCrs = ImageManager.getModelCrs(sourceProduct.getGeoCoding());
            CoordinateReferenceSystem targetModelCrs = ImageManager.getModelCrs(targetProduct.getGeoCoding());

            for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
                VectorDataNode sourceVDN = vectorDataGroup.get(i);
                String name = sourceVDN.getName();
                FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = sourceVDN.getFeatureCollection();
                featureCollection = FeatureUtils.clipCollection(featureCollection,
                                                                srcModelCrs,
                                                                clipGeometry,
                                                                DefaultGeographicCRS.WGS84,
                                                                null,
                                                                targetModelCrs,
                                                                ProgressMonitor.NULL);
                if (!targetProduct.getVectorDataGroup().contains(name)) {
                    targetProduct.getVectorDataGroup().add(new VectorDataNode(name, featureCollection.getSchema()));
                }
                VectorDataNode targetVDN = targetProduct.getVectorDataGroup().get(name);
                targetVDN.getPlacemarkGroup();
                targetVDN.getFeatureCollection().addAll(featureCollection);
                targetVDN.setDefaultStyleCss(sourceVDN.getDefaultStyleCss());
                targetVDN.setDescription(sourceVDN.getDescription());
            }
        }
    }

    /**
     * Returns whether or not a product can return a pixel position from a given geographical position.
     *
     * @param product the product to be checked
     * @return <code>true</code> if the given product can return a pixel position
     */
    public static boolean canGetPixelPos(Product product) {
        return product != null
                && product.getGeoCoding() != null
                && product.getGeoCoding().canGetPixelPos();
    }

    /**
     * Returns whether or not a raster can return a pixel position from a given geographical position.
     *
     * @param raster the raster to be checked
     * @return <code>true</code> if the given raster can return a pixel position
     */
    public static boolean canGetPixelPos(final RasterDataNode raster) {
        return raster != null
                && raster.getGeoCoding() != null
                && raster.getGeoCoding().canGetPixelPos();
    }

    /**
     * Creates a density plot image from two raster data nodes.
     *
     * @param raster1    the first raster data node
     * @param sampleMin1 the minimum sample value to be considered in the first raster
     * @param sampleMax1 the maximum sample value to be considered in the first raster
     * @param raster2    the second raster data node
     * @param sampleMin2 the minimum sample value to be considered in the second raster
     * @param sampleMax2 the maximum sample value to be considered in the second raster
     * @param roiMask    an optional mask to be used as a ROI for the computation
     * @param width      the width of the output image
     * @param height     the height of the output image
     * @param background the background color of the output image
     * @param image      an image to be used as output image, if <code>null</code> a new image is created
     * @param pm         the progress monitor
     * @return the density plot image
     * @throws java.io.IOException when an error occurred.
     */
    public static BufferedImage createDensityPlotImage(final RasterDataNode raster1,
                                                       final float sampleMin1,
                                                       final float sampleMax1,
                                                       final RasterDataNode raster2,
                                                       final float sampleMin2,
                                                       final float sampleMax2,
                                                       final Mask roiMask,
                                                       final int width,
                                                       final int height,
                                                       final Color background,
                                                       BufferedImage image,
                                                       final ProgressMonitor pm) throws IOException {

        Guardian.assertNotNull("raster1", raster1);
        Guardian.assertNotNull("raster2", raster2);
        Guardian.assertNotNull("background", background);
        if (raster1.getSceneRasterWidth() != raster2.getSceneRasterWidth()
                || raster1.getSceneRasterHeight() != raster2.getSceneRasterHeight()) {
            throw new IllegalArgumentException("'raster1' has not the same size as 'raster2'");
        }

        image = getCompatibleBufferedImageForDensityPlot(image, width, height, background);
        final byte[] pixelValues = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        DensityPlot.accumulate(raster1, sampleMin1, sampleMax1, raster2, sampleMin2, sampleMax2, roiMask, width,
                               height, pixelValues, pm);
        return image;
    }

    private static BufferedImage getCompatibleBufferedImageForDensityPlot(BufferedImage image, int width, int height,
                                                                          Color background) {
        if (image == null
                || image.getWidth() != width
                || image.getHeight() != height
                || !(image.getColorModel() instanceof IndexColorModel)
                || !(image.getRaster().getDataBuffer() instanceof DataBufferByte)) {
            final int palSize = 256;
            final byte[] r = new byte[palSize];
            final byte[] g = new byte[palSize];
            final byte[] b = new byte[palSize];
            final byte[] a = new byte[palSize];
            r[0] = (byte) background.getRed();
            g[0] = (byte) background.getGreen();
            b[0] = (byte) background.getBlue();
            a[0] = (byte) background.getAlpha();
            for (int i = 1; i < 128; i++) {
                r[i] = (byte) (2 * i);
                g[i] = (byte) 0;
                b[i] = (byte) 0;
                a[i] = (byte) 255;
            }
            for (int i = 128; i < 256; i++) {
                r[i] = (byte) 255;
                g[i] = (byte) (2 * (i - 128));
                b[i] = (byte) 0; // (2 * (i-128));
                a[i] = (byte) 255;
            }
            image = new BufferedImage(width,
                                      height,
                                      BufferedImage.TYPE_BYTE_INDEXED,
                                      new IndexColorModel(8, palSize, r, g, b, a));
        }
        return image;
    }

    /**
     * Draws all the masks contained overlay mask group of the given raster to the ovelayBIm image.
     *
     * @param raster     the raster data node which contains all the activated bitmask definitions
     * @param overlayBIm the source image which is used as base image for all the overlays.
     * @param pm         a monitor to inform the user about progress
     * @return the modified given overlayBImm which contains all the activated masks.
     * @see RasterDataNode#getOverlayMaskGroup()
     */

    public static BufferedImage overlayMasks(final RasterDataNode raster, final BufferedImage overlayBIm,
                                             ProgressMonitor pm) {
        ProductNodeGroup<Mask> maskGroup = raster.getOverlayMaskGroup();
        if (maskGroup.getNodeCount() == 0) {
            return overlayBIm;
        }

        final BufferedImageRendering imageRendering = new BufferedImageRendering(overlayBIm);

        pm.beginTask("Creating masks ...", maskGroup.getNodeCount());
        try {
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (Mask mask : masks) {
                pm.setSubTaskName(String.format("Applying mask '%s'", mask.getName()));
                final GeoCoding geoCoding = raster.getGeoCoding();
                final Layer layer = MaskLayerType.createLayer(raster, mask);
                layer.render(imageRendering);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return imageRendering.getImage();
    }

    public static GeoPos getCenterGeoPos(final Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding != null) {
            final PixelPos centerPixelPos = new PixelPos(0.5f * product.getSceneRasterWidth() + 0.5f,
                                                         0.5f * product.getSceneRasterHeight() + 0.5f);
            return geoCoding.getGeoPos(centerPixelPos, null);
        }
        return null;
    }

    /**
     * Normalizes the given geographical polygon so that maximum longitude differences between two points are 180
     * degrees. The method operates only on the longitude values of the given polygon.
     *
     * @param polygon a geographical, closed polygon
     * @return 0 if normalizing has not been applied , -1 if negative normalizing has been applied, 1 if positive
     *         normalizing has been applied, 2 if positive and negative normalising has been applied
     * @see #denormalizeGeoPolygon(GeoPos[])
     */
    public static int normalizeGeoPolygon(GeoPos[] polygon) {
        final float[] originalLon = new float[polygon.length];
        for (int i = 0; i < originalLon.length; i++) {
            originalLon[i] = polygon[i].lon;
        }

        float lonDiff;
        float increment = 0.f;
        float minLon = Float.MAX_VALUE;
        float maxLon = -Float.MAX_VALUE;
        for (int i = 1; i < polygon.length; i++) {
            final GeoPos geoPos = polygon[i];
            lonDiff = originalLon[i] - originalLon[i - 1];
            if (lonDiff > 180.0F) {
                increment -= 360.0F;
            } else if (lonDiff < -180.0F) {
                increment += 360.0F;
            }

            geoPos.lon += increment;
            if (geoPos.lon < minLon) {
                minLon = geoPos.lon;
            }
            if (geoPos.lon > maxLon) {
                maxLon = geoPos.lon;
            }
        }

        int normalized = 0;
        boolean negNormalized = false;
        boolean posNormalized = false;

        if (minLon < -180.0F) {
            posNormalized = true;
        }
        if (maxLon > 180.0F) {
            negNormalized = true;
        }

        if (negNormalized && !posNormalized) {
            normalized = 1;
        } else if (!negNormalized && posNormalized) {
            normalized = -1;
            for (GeoPos aPolygon : polygon) {
                aPolygon.lon += 360.0F;
            }
        } else if (negNormalized && posNormalized) {
            normalized = 2;
        }

        return normalized;
    }

    @Deprecated
    public static int normalizeGeoPolygon_old(GeoPos[] polygon) {
        boolean negNormalized = false;
        boolean posNormalized = false;
        float lonDiff;
        final int numValues = polygon.length;
        for (int i = 0; i < numValues - 1; i++) {
            GeoPos p1 = polygon[i];
            GeoPos p2 = polygon[(i + 1) % numValues];
            lonDiff = p2.lon - p1.lon;

            if (lonDiff >= 180.0f) {
                p2.lon -= 360.0f;
                negNormalized = true;
            } else if (lonDiff <= -180.0f) {
                p2.lon += 360.0f;
                posNormalized = true;
            }
        }

        int normalized = 0;
        if (negNormalized && !posNormalized) {
            for (GeoPos aPolygon : polygon) {
                aPolygon.lon += 360.0f;
            }
            normalized = -1;
        } else if (!negNormalized && posNormalized) {
            normalized = 1;
        } else if (negNormalized && posNormalized) {
            normalized = 2;
// todo - check if we should throw an IllegalArgumentException here
// Debug.trace("ProductUtils.normalizeGeoPolygon: negNormalized && posNormalized == true");
        }
        return normalized;
    }

    /**
     * Denormalizes the longitude values which have been normalized using the
     * {@link #normalizeGeoPolygon(org.esa.beam.framework.datamodel.GeoPos[])} method. The
     * method operates only on the longitude values of the given polygon.
     *
     * @param polygon a geographical, closed polygon
     */
    public static void denormalizeGeoPolygon(final GeoPos[] polygon) {
        for (GeoPos geoPos : polygon) {
            denormalizeGeoPos(geoPos);
        }
    }

    public static void denormalizeGeoPos(GeoPos geoPos) {
        int factor;
        if (geoPos.lon >= 0.f) {
            factor = (int) ((geoPos.lon + 180.f) / 360.f);
        } else {
            factor = (int) ((geoPos.lon - 180.f) / 360.f);
        }
        geoPos.lon -= factor * 360.f;
    }

    @Deprecated
    public static void denormalizeGeoPos_old(GeoPos geoPos) {
        if (geoPos.lon > 180.0f) {
            geoPos.lon -= 360.0f;
        } else if (geoPos.lon < -180.0f) {
            geoPos.lon += 360.0f;
        }
    }

    public static int getRotationDirection(GeoPos[] polygon) {
        return getAngleSum(polygon) > 0 ? 1 : -1;
    }

    public static double getAngleSum(GeoPos[] polygon) {
        final int length = polygon.length;
        double angleSum = 0;
        for (int i = 0; i < length; i++) {
            GeoPos p1 = polygon[i];
            GeoPos p2 = polygon[(i + 1) % length];
            GeoPos p3 = polygon[(i + 2) % length];
            double ax = p2.lon - p1.lon;
            double ay = p2.lat - p1.lat;
            double bx = p3.lon - p2.lon;
            double by = p3.lat - p2.lat;
            double a = Math.sqrt(ax * ax + ay * ay);
            double b = Math.sqrt(bx * bx + by * by);
            double cosAB = (ax * bx + ay * by) / (a * b);  // Skalarproduct geteilt durch Betragsprodukt
            double sinAB = (ax * by - ay * bx) / (a * b);  // Vektorproduct geteilt durch Betragsprodukt
            angleSum += Math.atan2(sinAB, cosAB);
        }
        return angleSum;
    }

/**
 * Converts a shape given in geographic coordionates
 * into a shape in pixel coordinates using the supplied geo coding.
 * The given shape
 *
 * @param geoPath   a <code>GeneralPath</code> given in geographic lon/lat coordinates,
 *                  as returned by the {@link #convertToGeoPath} method
 * @param geoCoding the geocoding used to convert the geographic coordinates into pixel coordinates.
 * @return a <code>GeneralPath</code> given in pixel coordinates.
 * @throws IllegalArgumentException if one of the given parameter is null.
 * @throws IllegalStateException    if the given geoPath is not a geo referenced <code>GeneralPath</code>
 *                                  wich contains only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types.
 * @see #convertToGeoPath
 */

    /**
     * Converts a <code>GeneralPath</code> given in geographic lon/lat coordinates into a <code>GeneralPath</code> in
     * pixel coordinates using the supplied geo coding.
     *
     * @param geoPath   a <code>GeneralPath</code> given in geographic lon/lat coordinates, as returned by the {@link
     *                  #convertToGeoPath(Shape, GeoCoding)} method
     * @param geoCoding the geocoding used to convert the geographic coordinates into pixel coordinates.
     * @return a <code>GeneralPath</code> given in pixel coordinates.
     * @throws IllegalArgumentException if one of the given parameter is null.
     * @throws IllegalStateException    if the given geoPath is not a geo referenced <code>GeneralPath</code> wich
     *                                  contains only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types.
     * @see #convertToGeoPath(Shape, GeoCoding)
     */
    public static GeneralPath convertToPixelPath(GeneralPath geoPath, GeoCoding geoCoding) {
        Guardian.assertNotNull("geoPath", geoPath);
        Guardian.assertNotNull("geoCoding", geoCoding);

        final PathIterator pathIterator = geoPath.getPathIterator(null);
        final float[] floats = new float[6];
        final PixelPos pixelPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();

        final GeneralPath pixelPath = new GeneralPath();
        while (!pathIterator.isDone()) {
            final int segmentType = pathIterator.currentSegment(floats);
            geoPos.setLocation(floats[1], floats[0]);
            if (segmentType == PathIterator.SEG_CLOSE) {
                pixelPath.closePath();
            } else if (segmentType == PathIterator.SEG_LINETO) {
                geoCoding.getPixelPos(geoPos, pixelPos);
                pixelPath.lineTo(pixelPos.x, pixelPos.y);
            } else if (segmentType == PathIterator.SEG_MOVETO) {
                geoCoding.getPixelPos(geoPos, pixelPos);
                pixelPath.moveTo(pixelPos.x, pixelPos.y);
            } else {
                throw new IllegalStateException("Unexpected path iterator segment: " + segmentType);
            }
            pathIterator.next();
        }
        return pixelPath;
    }

    /**
     * Converts a <code>Shape</code> given in pixel X/Y coordinates into a <code>GeneralPath</code> in geografic
     * coordinates using the supplied geo coding.
     *
     * @param shape     a <code>Shape</code> given in pixel X/Y coordinates
     * @param geoCoding the geo coding used to convert the pixel coordinates into geografic coordinates.
     * @return a <code>GeneralPath</code> given in geografic coordinates
     * @throws IllegalArgumentException if one of the given parameter is <code>null</code> or the given geo coding can
     *                                  not get geografic coordinates.
     * @throws IllegalStateException    if this method was used with a java runtime version in which it is not guaranted
     *                                  that a <code>PathIterator</code> returned by {@link Shape#getPathIterator(java.awt.geom.AffineTransform,
     *                                  double)} returnes only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types.
     * @see GeoCoding#canGetGeoPos()
     */
    public static GeneralPath convertToGeoPath(Shape shape, GeoCoding geoCoding) {
        Guardian.assertNotNull("shape", shape);
        Guardian.assertNotNull("geoCoding", geoCoding);
        if (!geoCoding.canGetGeoPos()) {
            throw new IllegalArgumentException("invalid 'geoCoding'"); /*I18N*/
        }

        final PathIterator pathIterator = shape.getPathIterator(null, 0.1);
        final float[] floats = new float[6];
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        final PixelPos lastPixelPos = new PixelPos();
        final GeneralPath geoPath = new GeneralPath();
        while (!pathIterator.isDone()) {
            final int segmentType = pathIterator.currentSegment(floats);
            pixelPos.x = floats[0];
            pixelPos.y = floats[1];
            if (segmentType == PathIterator.SEG_CLOSE) {
                geoPath.closePath();
            } else if (segmentType == PathIterator.SEG_LINETO) {
                final double maxDist = 1.5;
                final double distance = lastPixelPos.distance(pixelPos);
                if (distance > maxDist) {
                    final float startX = lastPixelPos.x;
                    final float startY = lastPixelPos.y;
                    final float endX = pixelPos.x;
                    final float endY = pixelPos.y;
                    final int numParts = (int) (distance / maxDist) + 1;
                    final float addX = (endX - startX) / numParts;
                    final float addY = (endY - startY) / numParts;
                    pixelPos.setLocation(startX + addX, startY + addY);
                    for (int i = 1; i < numParts; i++, pixelPos.x += addX, pixelPos.y += addY) {
                        geoCoding.getGeoPos(pixelPos, geoPos);
                        geoPath.lineTo(geoPos.lon, geoPos.lat);
                    }
                    pixelPos.setLocation(endX, endY);
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    geoPath.lineTo(geoPos.lon, geoPos.lat);
                    lastPixelPos.setLocation(pixelPos);
                } else {
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    geoPath.lineTo(geoPos.lon, geoPos.lat);
                    lastPixelPos.setLocation(pixelPos);
                }
            } else if (segmentType == PathIterator.SEG_MOVETO) {
                geoCoding.getGeoPos(pixelPos, geoPos);
                geoPath.moveTo(geoPos.lon, geoPos.lat);
                lastPixelPos.setLocation(pixelPos);
            } else {
                throw new IllegalStateException("Unexpected path iterator segment: " + segmentType);
            }
            pathIterator.next();
        }
        return geoPath;
    }

    /**
     * Copies all metadata elements and attributes of the source product to the target product.
     * The copied elements and attributes are deeply cloned.
     *
     * @param source the source product.
     * @param target the target product.
     * @throws NullPointerException if the source or the target product is {@code null}.
     */
    public static void copyMetadata(Product source, Product target) {
        Assert.notNull(source, "source");
        Assert.notNull(target, "target");
        copyMetadata(source.getMetadataRoot(), target.getMetadataRoot());
    }

    /**
     * Copies all metadata elements and attributes of the source element to the target element.
     * The copied elements and attributes are deeply cloned.
     *
     * @param source the source element.
     * @param target the target element.
     * @throws NullPointerException if the source or the target element is {@code null}.
     */
    public static void copyMetadata(MetadataElement source, MetadataElement target) {
        Assert.notNull(source, "source");
        Assert.notNull(target, "target");
        for (final MetadataElement element : source.getElements()) {
            target.addElement(element.createDeepClone());
        }
        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
    }

    /**
     * Copies the source product's preferred tile size (if any) to the target product.
     *
     * @param sourceProduct The source product.
     * @param targetProduct The target product.
     */
    public static void copyPreferredTileSize(Product sourceProduct, Product targetProduct) {
        final Dimension preferredTileSize = sourceProduct.getPreferredTileSize();
        if (preferredTileSize != null) {
            final Rectangle targetRect = new Rectangle(targetProduct.getSceneRasterWidth(),
                                                       targetProduct.getSceneRasterHeight());
            final Rectangle tileRect = new Rectangle(preferredTileSize).intersection(targetRect);
            targetProduct.setPreferredTileSize(tileRect.width, tileRect.height);
        }
    }


    public static GeoTIFFMetadata createGeoTIFFMetadata(final Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        return createGeoTIFFMetadata(geoCoding, w, h);
    }

    public static GeoTIFFMetadata createGeoTIFFMetadata(GeoCoding geoCoding, int width, int height) {
        return GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(geoCoding, width, height);
    }

    public static GeneralPath areaToPath(Area negativeArea, double deltaX) {
        final GeneralPath pixelPath = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        final float[] floats = new float[6];
// move to correct rectangle
        final AffineTransform transform = AffineTransform.getTranslateInstance(deltaX, 0.0);
        final PathIterator iterator = negativeArea.getPathIterator(transform);

        while (!iterator.isDone()) {
            final int segmentType = iterator.currentSegment(floats);
            if (segmentType == PathIterator.SEG_LINETO) {
                pixelPath.lineTo(floats[0], floats[1]);
            } else if (segmentType == PathIterator.SEG_MOVETO) {
                pixelPath.moveTo(floats[0], floats[1]);
            } else if (segmentType == PathIterator.SEG_CLOSE) {
                pixelPath.closePath();
            } else {
                throw new IllegalStateException("unhandled segment type in path iterator: " + segmentType);
            }
            iterator.next();
        }
        return pixelPath;
    }

    /**
     * Adds a given elem to the history of the given product. If the products metadata root
     * does not contain a history entry a new one will be created.
     *
     * @param product the product to add the history element.
     * @param elem    the element to add to the products history. If <code>null</code> nothing will be added.
     */
    public static void addElementToHistory(Product product, MetadataElement elem) {
        Guardian.assertNotNull("product", product);
        if (elem != null) {
            final String historyName = Product.HISTORY_ROOT_NAME;
            final MetadataElement metadataRoot = product.getMetadataRoot();
            if (!metadataRoot.containsElement(historyName)) {
                metadataRoot.addElement(new MetadataElement(historyName));
            }
            final MetadataElement historyElem;
            historyElem = metadataRoot.getElement(historyName);
            if (historyElem.containsElement(elem.getName())) {
                final MetadataElement previousElem = historyElem.getElement(elem.getName());
                historyElem.removeElement(previousElem);
                elem.addElement(previousElem);
            }
            historyElem.addElement(elem);
        }
    }

    /**
     * Validates all the expressions contained in the given (output) product. If an expression is not applicable to the given
     * product, the related element is removed.
     *
     * @param product the (output) product to be cleaned up
     * @return an array of messages which changes are done to the given product.
     */
    public static String[] removeInvalidExpressions(final Product product) {
        final ArrayList<String> messages = new ArrayList<String>(10);

        product.acceptVisitor(new ProductVisitorAdapter() {
            @Override
            public void visit(BitmaskDef bitmaskDef) {
                if (!product.isCompatibleBandArithmeticExpression(bitmaskDef.getExpr())) {
                    String pattern = "Bitmask definition ''{0}'' removed from output product because it is not applicable."; /*I18N*/
                    messages.add(MessageFormat.format(pattern, bitmaskDef.getName()));
                }
            }

            @Override
            public void visit(TiePointGrid grid) {
                final String type = "tie point grid";
                checkRaster(grid, type);
            }

            @Override
            public void visit(Band band) {
                final String type = "band";
                checkRaster(band, type);
            }

            @Override
            public void visit(VirtualBand virtualBand) {
                if (!product.isCompatibleBandArithmeticExpression(virtualBand.getExpression())) {
                    product.removeBand(virtualBand);
                    String pattern = "Virtual band ''{0}'' removed from output product because it is not applicable.";  /*I18N*/
                    messages.add(MessageFormat.format(pattern, virtualBand.getName()));
                } else {
                    checkRaster(virtualBand, "virtual band");
                }
            }

            private void checkRaster(RasterDataNode raster, String type) {
                final String validExpr = raster.getValidPixelExpression();
                if (validExpr != null && !product.isCompatibleBandArithmeticExpression(validExpr)) {
                    raster.setValidPixelExpression(null);
                    String pattern = "Valid pixel expression ''{0}'' removed from output {1} ''{2}'' " +
                            "because it is not applicable.";   /*I18N*/
                    messages.add(MessageFormat.format(pattern, validExpr, type, raster.getName()));
                }
            }
        });
        return messages.toArray(new String[messages.size()]);
    }

    /**
     * Finds the name of a band in the given product which is suitable to product a good quicklook.
     * The method prefers bands with longer wavelengths, in order to produce good results for night-time scenes.
     *
     * @param product the product to be searched
     * @return the name of a suitable band or null if the given product does not contain any bands
     */
    public static String findSuitableQuicklookBandName(final Product product) {
        // Step 0: Try if pre-defined quicklook band exists
        //
        String bandName = product.getQuicklookBandName();
        if (bandName != null && product.containsBand(bandName)) {
            return bandName;
        }

        final Band[] bands = product.getBands();
        if (bands.length == 0) {
            return null;
        }

        // Step 1: Find the band with a max. wavelength > 1000 nm
        //
        double wavelengthMax = 0.0;
        for (Band band : bands) {
            final float wavelength = band.getSpectralWavelength();
            if (wavelength > 1000 && wavelength > wavelengthMax) {
                wavelengthMax = wavelength;
                bandName = band.getName();
            }
        }
        if (bandName != null) {
            return bandName;
        }

// Step 2: Find a band in the range 860 to 890 nm preferring the one at 860 nm
// (for MERIS, this method will always find channel 13)
//
        final double WL1 = 860;
        final double WL2 = 890;
        double minValue = Double.MAX_VALUE;
        for (Band band : bands) {
            final double wavelength = band.getSpectralWavelength();
            if (wavelength > WL1 && wavelength < WL2) {
                final double value = wavelength - WL1;
                if (value < minValue) {
                    minValue = value;
                    bandName = band.getName();
                }
            }
        }

        if (bandName != null) {
            return bandName;
        }

        // Method 3: Find the band with absolute max. wavelength
        //
        wavelengthMax = 0.0;
        for (Band band : bands) {
            final float wavelength = band.getSpectralWavelength();
            if (wavelength > wavelengthMax) {
                wavelengthMax = wavelength;
                bandName = band.getName();
            }
        }

        if (bandName != null) {
            return bandName;
        }

        return bands[0].getName();
    }

    public static PixelPos[] computeSourcePixelCoordinates(final GeoCoding sourceGeoCoding,
                                                           final int sourceWidth,
                                                           final int sourceHeight,
                                                           final GeoCoding destGeoCoding,
                                                           final Rectangle destArea) {
        Guardian.assertNotNull("sourceGeoCoding", sourceGeoCoding);
        Guardian.assertEquals("sourceGeoCoding.canGetPixelPos()", sourceGeoCoding.canGetPixelPos(), true);
        Guardian.assertNotNull("destGeoCoding", destGeoCoding);
        Guardian.assertEquals("destGeoCoding.canGetGeoPos()", destGeoCoding.canGetGeoPos(), true);
        Guardian.assertNotNull("destArea", destArea);

        final int minX = destArea.x;
        final int minY = destArea.y;
        final int maxX = minX + destArea.width - 1;
        final int maxY = minY + destArea.height - 1;

        final PixelPos[] pixelCoords = new PixelPos[destArea.width * destArea.height];
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();

        int coordIndex = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                pixelPos.x = x + 0.5f;
                pixelPos.y = y + 0.5f;
                destGeoCoding.getGeoPos(pixelPos, geoPos);
                sourceGeoCoding.getPixelPos(geoPos, pixelPos);
                if (pixelPos.x >= 0.0f && pixelPos.x < sourceWidth
                        && pixelPos.y >= 0.0f && pixelPos.y < sourceHeight) {
                    pixelCoords[coordIndex] = new PixelPos(pixelPos.x, pixelPos.y);
                } else {
                    pixelCoords[coordIndex] = null;
                }
                coordIndex++;
            }
        }
        return pixelCoords;
    }

    /**
     * Computes the minimum and maximum y value of the given {@link PixelPos} array.
     *
     * @param pixelPositions the {@link PixelPos} array
     * @return an int array which containes the minimum and maximum y value of the given {@link PixelPos} array in the
     *         order:<br> &nbsp;&nbsp;&nbsp;&nbsp;[0] - the minimum value<br>&nbsp;&nbsp;&nbsp;&nbsp;[1] - the maximum
     *         value<br><br>or <code>null</code> if no minimum or maximum can be retrieved because there given array is
     *         empty.
     * @throws IllegalArgumentException if the given pixelPositions are <code>null</code>.
     */
    public static float[] computeMinMaxY(PixelPos[] pixelPositions) {
        Guardian.assertNotNull("pixelPositions", pixelPositions);

        float min = Integer.MAX_VALUE; // min initial
        float max = Integer.MIN_VALUE; // max initial
        for (PixelPos pixelPos : pixelPositions) {
            if (pixelPos != null) {
                if (pixelPos.y < min) {
                    min = pixelPos.y;
                }
                if (pixelPos.y > max) {
                    max = pixelPos.y;
                }
            }
        }
        if (min > max) {
            return null;
        }
        return new float[]{min, max};
    }

    /**
     * Copies only the bands from source to target.
     *
     * @see #copyBandsForGeomTransform(org.esa.beam.framework.datamodel.Product, org.esa.beam.framework.datamodel.Product, boolean, double, java.util.Map)
     */
    public static void copyBandsForGeomTransform(final Product sourceProduct,
                                                 final Product targetProduct,
                                                 final double defaultNoDataValue,
                                                 final Map<Band, RasterDataNode> addedRasterDataNodes) {
        copyBandsForGeomTransform(sourceProduct,
                                  targetProduct,
                                  false,
                                  defaultNoDataValue,
                                  addedRasterDataNodes);
    }

    /**
     * Adds raster data nodes of a source product as bands to the given target product. This method is especially usefull if the target
     * product is a geometric transformation (e.g. map-projection) of the source product.
     * <p>If
     * {@link RasterDataNode#isScalingApplied() sourceBand.scalingApplied} is true,
     * this method will always create the related target band with the raw data type {@link ProductData#TYPE_FLOAT32},
     * regardless which raw data type the source band has.
     * In this case, {@link RasterDataNode#getScalingFactor() targetBand.scalingFactor}
     * will always be 1.0, {@link RasterDataNode#getScalingOffset() targetBand.scalingOffset}
     * will always be 0.0 and
     * {@link RasterDataNode#isLog10Scaled() targetBand.log10Scaled} will be taken from the source band.
     * This ensures that source pixel resampling methods operating on floating point
     * data can be stored without loss in accuracy in the target band.
     * <p/>
     * <p>Furthermore, the
     * {@link RasterDataNode#isNoDataValueSet() targetBands.noDataValueSet}
     * and {@link RasterDataNode#isNoDataValueUsed() targetBands.noDataValueUsed}
     * properties will always be true for all added target bands. The {@link RasterDataNode#getGeophysicalNoDataValue() targetBands.geophysicalNoDataValue},
     * will be either the one from the source band, if any, or otherwise the one passed into this method.
     *
     * @param sourceProduct        the source product as the source for the band specifications. Must be not
     *                             <code>null</code>.
     * @param targetProduct        the destination product to receive the bands created. Must be not <code>null</code>.
     * @param includeTiePointGrids if {@code true}, tie-point grids of source product will be included as bands in target product
     * @param defaultNoDataValue   the default, geophysical no-data value to be used if no no-data value is used by the source band.
     * @param targetToSourceMap    a mapping from a target band to a source raster data node, can be {@code null}
     */
    public static void copyBandsForGeomTransform(final Product sourceProduct,
                                                 final Product targetProduct,
                                                 final boolean includeTiePointGrids,
                                                 final double defaultNoDataValue,
                                                 final Map<Band, RasterDataNode> targetToSourceMap) {
        Debug.assertNotNull(sourceProduct);
        Debug.assertNotNull(targetProduct);

        final Map<Band, RasterDataNode> sourceNodes = new HashMap<Band, RasterDataNode>(
                sourceProduct.getNumBands() + sourceProduct.getNumTiePointGrids() + 10);

        final Band[] sourceBands = sourceProduct.getBands();
        for (Band sourceBand : sourceBands) {
            if (sourceBand.getGeoCoding() != null) {
                final Band targetBand;
                if (sourceBand instanceof VirtualBand) {
                    targetBand = new VirtualBand(sourceBand.getName(),
                                                 sourceBand.getDataType(),
                                                 targetProduct.getSceneRasterWidth(),
                                                 targetProduct.getSceneRasterHeight(),
                                                 ((VirtualBand) sourceBand).getExpression());
                } else if (sourceBand.isScalingApplied()) {
                    targetBand = new Band(sourceBand.getName(),
                                          ProductData.TYPE_FLOAT32,
                                          targetProduct.getSceneRasterWidth(),
                                          targetProduct.getSceneRasterHeight());
                    targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
                } else {
                    targetBand = new Band(sourceBand.getName(),
                                          sourceBand.getDataType(),
                                          targetProduct.getSceneRasterWidth(),
                                          targetProduct.getSceneRasterHeight());
                }
                targetBand.setUnit(sourceBand.getUnit());
                if (sourceBand.getDescription() != null) {
                    targetBand.setDescription(sourceBand.getDescription());
                }
                if (sourceBand.isNoDataValueUsed()) {
                    // prevent from possible accuracy loss GeophysicalNoDataValue <--> NoDataValue
                    if (sourceBand.isScalingApplied()) {
                        targetBand.setGeophysicalNoDataValue(sourceBand.getGeophysicalNoDataValue());
                    } else {
                        targetBand.setNoDataValue(sourceBand.getNoDataValue());
                    }
                } else {
                    targetBand.setGeophysicalNoDataValue(defaultNoDataValue);
                }
                targetBand.setNoDataValueUsed(true);
                copySpectralBandProperties(sourceBand, targetBand);
                FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
                IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
                if (sourceFlagCoding != null) {
                    String flagCodingName = sourceFlagCoding.getName();
                    FlagCoding destFlagCoding = targetProduct.getFlagCodingGroup().get(flagCodingName);
                    Debug.assertNotNull(
                            destFlagCoding); // should not happen because flag codings should be already in product
                    targetBand.setSampleCoding(destFlagCoding);
                } else if (sourceIndexCoding != null) {
                    String indexCodingName = sourceIndexCoding.getName();
                    IndexCoding destIndexCoding = targetProduct.getIndexCodingGroup().get(indexCodingName);
                    Debug.assertNotNull(
                            destIndexCoding); // should not happen because index codings should be already in product
                    targetBand.setSampleCoding(destIndexCoding);
                } else {
                    targetBand.setSampleCoding(null);
                }

                ImageInfo sourceImageInfo = sourceBand.getImageInfo();
                if (sourceImageInfo != null) {
                    targetBand.setImageInfo(sourceImageInfo.createDeepCopy());
                }
                targetProduct.addBand(targetBand);
                sourceNodes.put(targetBand, sourceBand);
            }
        }

        if (includeTiePointGrids) {
            for (final TiePointGrid sourceGrid : sourceProduct.getTiePointGrids()) {
                if (sourceGrid.getGeoCoding() != null) {
                    Band targetBand = new Band(sourceGrid.getName(),
                                               sourceGrid.getGeophysicalDataType(),
                                               targetProduct.getSceneRasterWidth(),
                                               targetProduct.getSceneRasterHeight());
                    targetBand.setUnit(sourceGrid.getUnit());
                    if (sourceGrid.getDescription() != null) {
                        targetBand.setDescription(sourceGrid.getDescription());
                    }
                    if (sourceGrid.isNoDataValueUsed()) {
                        targetBand.setNoDataValue(sourceGrid.getNoDataValue());
                    } else {
                        targetBand.setNoDataValue(defaultNoDataValue);
                    }
                    targetBand.setNoDataValueUsed(true);
                    targetProduct.addBand(targetBand);
                    sourceNodes.put(targetBand, sourceGrid);
                }
            }
        }

        for (Band targetBand : targetProduct.getBands()) {
            final RasterDataNode sourceNode = sourceNodes.get(targetBand);
            if (sourceNode != null) {
                // Set the valid pixel expression to <null> if the given expression
                // is not applicable. This is e.g. the case if the flags dataset(s) are not projected as well
                //
                final String sourceExpression = sourceNode.getValidPixelExpression();
                if (sourceExpression != null && !targetProduct.isCompatibleBandArithmeticExpression(sourceExpression)) {
                    targetBand.setValidPixelExpression(sourceExpression);
                }
            }
        }

        if (targetToSourceMap != null) {
            targetToSourceMap.putAll(sourceNodes);
        }
    }

    static ArrayList<GeneralPath> assemblePathList(GeoPos[] geoPoints) {
        final GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, geoPoints.length + 8);
        final ArrayList<GeneralPath> pathList = new ArrayList<GeneralPath>(16);

        if (geoPoints.length > 1) {
            float lon = geoPoints[0].getLon();
            float minLon = lon;
            float maxLon = lon;

            path.moveTo(lon, geoPoints[0].getLat());
            for (int i = 1; i < geoPoints.length; i++) {
                lon = geoPoints[i].getLon();
                final float lat = geoPoints[i].getLat();
                if (Float.isNaN(lon) || Float.isNaN(lat)) {
                    continue;
                }
                if (lon < minLon) {
                    minLon = lon;
                }
                if (lon > maxLon) {
                    maxLon = lon;
                }
                path.lineTo(lon, lat);
            }
            path.closePath();

            int runIndexMin = (int) Math.floor((minLon + 180) / 360);
            int runIndexMax = (int) Math.floor((maxLon + 180) / 360);

            final Area pathArea = new Area(path);
            for (int k = runIndexMin; k <= runIndexMax; k++) {
                final Area currentArea = new Area(new Rectangle2D.Float(k * 360.0f - 180.0f, -90.0f, 360.0f, 180.0f));
                currentArea.intersect(pathArea);
                if (!currentArea.isEmpty()) {
                    pathList.add(areaToPath(currentArea, -k * 360.0));
                }
            }
        }
        return pathList;
    }

    public static ProductData.UTC getScanLineTime(Product product, double y) {
        final ProductData.UTC utcStartTime = product.getStartTime();
        final ProductData.UTC utcEndTime = product.getEndTime();

        if (utcStartTime == null && utcEndTime == null) {
            return null;
        }
        if (utcStartTime == null) {
            return utcEndTime;
        }
        if (utcEndTime == null) {
            return utcStartTime;
        }
        final double start = utcStartTime.getMJD();
        final double stop = utcEndTime.getMJD();

        final double timePerLine = (stop - start) / (product.getSceneRasterHeight() - 1);
        final double currentLine = timePerLine * y + start;
        return new ProductData.UTC(currentLine);
    }

    public static double getGeophysicalSampleDouble(Band band, int pixelX, int pixelY, int level) {
        final PlanarImage image = ImageManager.getInstance().getSourceImage(band, level);
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        final Raster data = image.getTile(tileX, tileY);
        if (data == null) {
            return Double.NaN;
        }

        final double sample;
        if (band.getDataType() == ProductData.TYPE_INT8) {
            sample = (byte) data.getSample(pixelX, pixelY, 0);
        } else if (band.getDataType() == ProductData.TYPE_UINT32) {
            sample = data.getSample(pixelX, pixelY, 0) & 0xFFFFFFFFL;
        } else {
            sample = data.getSampleDouble(pixelX, pixelY, 0);
        }
        if (band.isScalingApplied()) {
            return band.scale(sample);
        }
        return sample;
    }

    public static long getGeophysicalSampleLong(Band band, int pixelX, int pixelY, int level) {
        final PlanarImage image = ImageManager.getInstance().getSourceImage(band, level);
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        final Raster data = image.getTile(tileX, tileY);

        final long sample;
        if (band.getDataType() == ProductData.TYPE_INT8) {
            sample = (byte) data.getSample(pixelX, pixelY, 0);
        } else if (band.getDataType() == ProductData.TYPE_UINT32) {
            sample = data.getSample(pixelX, pixelY, 0) & 0xFFFFFFFFL;
        } else {
            sample = data.getSample(pixelX, pixelY, 0);
        }
        if (band.isScalingApplied()) {
            return (long) band.scale(sample);
        }
        return sample;
    }
}
