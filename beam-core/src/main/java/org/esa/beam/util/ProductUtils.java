/*
 * $Id: ProductUtils.java,v 1.8 2007/03/28 11:37:01 norman Exp $
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
package org.esa.beam.util;

import Jama.Matrix;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataLoop;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.dataop.maptransf.*;
import org.esa.beam.util.geotiff.EPSGCodes;
import org.esa.beam.util.geotiff.GeoTIFFCodes;
import org.esa.beam.util.geotiff.GeoTIFFMetadata;
import org.esa.beam.util.jai.JAIDebug;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
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

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Image Creation Routines
    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an bitmask-overlayed image for the given product and the given array of band indices. If no bitmasks are
     * defined, a non-overlayed image is created.
     * <p/>
     * <p>The given parameter <code>bandIndices</code> specifies the bands to be used for image creation. If it just
     * contains one element, a greyscale/palette based on-band image is created. If it has three elements an RGB image
     * will be created.
     * <p/>
     * <p>The method uses the image display information given by the <code>{@link org.esa.beam.framework.datamodel.ImageInfo
     * ImageInfo}</code> and <code>{@link org.esa.beam.framework.datamodel.BitmaskOverlayInfo BitmaskOverlayInfo}</code>
     * properties of each of the bands.
     *
     * @param product           the source product
     * @param bandIndices       the band indices to be used for image creation
     * @param histogramMatching the optional histogram matching to be performed: can be either
     *                          <code>ImageInfo.HISTOGRAM_MATCHING_EQUALIZE</code> or <code>ImageInfo.HISTOGRAM_MATCHING_NORMALIZE</code>.
     *                          If this parameter is <code>null</code> or any other value, no histogram matching is
     *                          applied.
     * @return a greyscale/palette-based or RGB image
     * @throws IOException if the given raster data is not loaded and reload causes an I/O error
     * @see RasterDataNode#setImageInfo
     * @see RasterDataNode#setBitmaskOverlayInfo
     */
    public static RenderedImage createOverlayedImage(final Product product,
                                                     final int[] bandIndices,
                                                     final String histogramMatching) throws IOException {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("bandIndices", bandIndices);
        if (bandIndices.length != 1 && bandIndices.length != 3) {
            throw new IllegalArgumentException("bandIndices.length is not 1 and not 3");
        }
        final Band[] bands = new Band[bandIndices.length];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = product.getBandAt(bandIndices[i]);
        }
        return createOverlayedImage(bands, histogramMatching);
    }

    /**
     * @deprecated in 4.0, use {@link #createOverlayedImage(RasterDataNode[],String,ProgressMonitor)} instead
     */
    public static RenderedImage createOverlayedImage(final RasterDataNode[] rasterDataNodes,
                                                     final String histogramMatching) throws IOException {
        return createOverlayedImage(rasterDataNodes, histogramMatching, ProgressMonitor.NULL);
    }

    /**
     * Creates an image from the given <code>{@link org.esa.beam.framework.datamodel.RasterDataNode}</code>s. If no
     * bitmasks are defined, a non-overlayed image is created.
     * <p/>
     * <p>Depending on the number of <code>RasterDataNode</code>s given in the array a greyscale/palette-based or RGB
     * image is created.
     * <p/>
     * <p>The method uses the image display information given by the <code>{@link org.esa.beam.framework.datamodel.ImageInfo
     * ImageInfo}</code> and <code>{@link org.esa.beam.framework.datamodel.BitmaskOverlayInfo BitmaskOverlayInfo}</code>
     * properties of each of the bands.
     *
     * @param rasterDataNodes   the array of raster data nodes to be used for image creation, if length is one a
     *                          greyscale/palette-based image is created, if the length is three, a RGB image is
     *                          created
     * @param histogramMatching the optional histogram matching to be performed: can be either
     *                          <code>ImageInfo.HISTOGRAM_MATCHING_EQUALIZE</code> or <code>ImageInfo.HISTOGRAM_MATCHING_NORMALIZE</code>.
     *                          If this parameter is <code>null</code> or any other value, no histogram matching is
     *                          applied.
     * @param pm                a monitor to inform the user about progress
     * @return a greyscale/palette-based or RGB image depending on the number of raster data nodes given
     * @throws IOException if the given raster data is not loaded and reload causes an I/O error
     * @see RasterDataNode#setImageInfo
     * @see RasterDataNode#setBitmaskOverlayInfo
     * @see org.esa.beam.framework.datamodel.ImageInfo
     */
    public static RenderedImage createOverlayedImage(final RasterDataNode[] rasterDataNodes,
                                                     final String histogramMatching,
                                                     final ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("rasterDataNodes", rasterDataNodes);
        if (rasterDataNodes.length != 1 && rasterDataNodes.length != 3) {
            throw new IllegalArgumentException("rasterDataNodes.length is not 1 and not 3");
        }

        final RasterDataNode raster = rasterDataNodes[0];
        final StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        BufferedImage overlayBIm;
        BitmaskDef[] bitmaskDefs = raster.getBitmaskDefs();
        pm.beginTask("Creating image...", rasterDataNodes.length + (bitmaskDefs.length > 0 ? 1 : 0));
        try {
            overlayBIm = createRgbImage(rasterDataNodes, SubProgressMonitor.create(pm, rasterDataNodes.length));
            stopWatch.stopAndTrace("ProductSceneView.createOverlayedImage: base RGB image created");

            final boolean doEqualize = ImageInfo.HISTOGRAM_MATCHING_EQUALIZE.equalsIgnoreCase(histogramMatching);
            final boolean doNormalize = ImageInfo.HISTOGRAM_MATCHING_NORMALIZE.equalsIgnoreCase(histogramMatching);
            if (doEqualize || doNormalize) {
                PlanarImage sourcePIm = PlanarImage.wrapRenderedImage(overlayBIm);
                sourcePIm = JAIUtils.createTileFormatOp(sourcePIm, 512, 512);
                if (doEqualize) {
                    sourcePIm = JAIUtils.createHistogramEqualizedImage(sourcePIm);
                    stopWatch.stopAndTrace("ProductSceneView.createOverlayedImage: histogram-equalized image created");
                } else {
                    sourcePIm = JAIUtils.createHistogramNormalizedImage(sourcePIm);
                    stopWatch.stopAndTrace("ProductSceneView.createOverlayedImage: histogram-normalized image created");
                }
                overlayBIm = sourcePIm.getAsBufferedImage();
            }

            if (bitmaskDefs.length > 0) {
                // @todo 3 nf/nf - check: for RGB, BitmaskOverlayInfo is always taken from the red raster?
                stopWatch.start();
                overlayBIm = overlayBitmasks(raster, overlayBIm, SubProgressMonitor.create(pm, 1));
                stopWatch.stopAndTrace("ProductSceneView.createOverlayedImage: overlays added");
            }

            JAIDebug.trace("overlayBIm", overlayBIm);
        } finally {
            pm.done();
        }

        return overlayBIm;
    }


    /**
     * @deprecated in 4.0, use {@link #createColorIndexedImage(RasterDataNode,ProgressMonitor)} instead
     */
    public static BufferedImage createColorIndexedImage(final RasterDataNode rasterDataNode) throws IOException {
        return createColorIndexedImage(rasterDataNode, ProgressMonitor.NULL);
    }

    /**
     * Creates a greyscale image from the given <code>{@link org.esa.beam.framework.datamodel.RasterDataNode}</code>.
     * <p/>
     * <p>The method uses the given raster data node's image information (an instance of <code>{@link
     * org.esa.beam.framework.datamodel.ImageInfo}</code>) to create the image.
     *
     * @param rasterDataNode the raster data node, must not be <code>null</code>
     * @param pm             a monitor to inform the user about progress
     * @return the color indexed image
     * @throws IOException if the given raster data is not loaded and reload causes an I/O error
     * @see RasterDataNode#setImageInfo
     */
    public static BufferedImage createColorIndexedImage(final RasterDataNode rasterDataNode,
                                                        ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("rasterDataNode", rasterDataNode);

        final int width = rasterDataNode.getSceneRasterWidth();
        final int height = rasterDataNode.getSceneRasterHeight();

        final StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        final ImageInfo imageInfo = rasterDataNode.ensureValidImageInfo();
        stopWatch.stopAndTrace("ProductUtils.createColorIndexedImage, mark 1");

        final double newMin = imageInfo.getMinDisplaySample();
        final double newMax = imageInfo.getMaxDisplaySample();
        final double gamma = (double) imageInfo.getGamma();

        stopWatch.start();
        final byte[] colorIndexes = rasterDataNode.quantizeRasterData(newMin, newMax, gamma, pm);
        stopWatch.stopAndTrace("ProductUtils.createColorIndexedImage, mark 2");

        stopWatch.start();
        final IndexColorModel cm = imageInfo.createColorModel();
        final SampleModel sm = cm.createCompatibleSampleModel(width, height);
        final DataBuffer db = new DataBufferByte(colorIndexes, colorIndexes.length);
        final WritableRaster wr = WritableRaster.createWritableRaster(sm, db, null);
        final BufferedImage bufferedImage = new BufferedImage(cm, wr, false, null);
        stopWatch.stopAndTrace("ProductUtils.createColorIndexedImage, mark 3");

        return bufferedImage;
    }

    /**
     * @deprecated in 4.0, use {@link #createRgbImage(org.esa.beam.framework.datamodel.RasterDataNode[],com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static BufferedImage createRgbImage(final RasterDataNode[] rasterDataNodes) throws IOException {
        return createRgbImage(rasterDataNodes, ProgressMonitor.NULL);
    }

    /**
     * Creates a RGB image from the given array of <code>{@link org.esa.beam.framework.datamodel.RasterDataNode}</code>s.
     * The given array <code>rasterDataNodes</code> contain three raster data nodes to be used for the red, green and
     * blue components of the RGB image to be created.
     *
     * @param rasterDataNodes an array of exactly three raster nodes to be used for the R,G and B component.
     * @param pm              a monitor to inform the user about progress
     * @return the RGB image
     * @throws IOException if the given raster data is not loaded and reload causes an I/O error
     * @see RasterDataNode#setImageInfo
     */
    public static BufferedImage createRgbImage(final RasterDataNode[] rasterDataNodes, ProgressMonitor pm) throws
            IOException {
        Guardian.assertNotNull("rasterDataNodes", rasterDataNodes);
        if (rasterDataNodes.length != 1 && rasterDataNodes.length != 3) {
            throw new IllegalArgumentException("rasterDataNodes.length is not 1 and not 3");
        }

        final boolean singleBand = rasterDataNodes.length == 1;
        final StopWatch stopWatch = new StopWatch();
        final int width = rasterDataNodes[0].getSceneRasterWidth();
        final int height = rasterDataNodes[0].getSceneRasterHeight();

        final int numPixels = width * height;
        final byte[] rgbSamples = new byte[3 * numPixels];

        final String[] progressMessages = new String[]{
                /*I18N*/
                "Computing red channel",
                "Computing green channel",
                "Computing blue channel"
        };

        BufferedImage bufferedImage = null;
        int progressMax;
        if (singleBand) {
            progressMax = 2;
        } else {
            progressMax = 3;
        }
        for (final RasterDataNode node : rasterDataNodes) {
            if (node.getImageInfo() == null) {
                progressMax += 2;
            }
        }
        final String singleMessage = "Computing image";
        final String rgbMessage = "Computing RGB image";
        pm.beginTask(singleBand ? singleMessage : rgbMessage, progressMax);
        try {
            for (int i = 0; i < rasterDataNodes.length; i++) {
                pm.setSubTaskName(singleBand ? singleMessage : progressMessages[i]);
                checkCanceled(pm);
                final RasterDataNode raster = rasterDataNodes[i];

                final ImageInfo imageInfo;
                if (raster.getImageInfo() == null) {
// todo - Proformance boost. But better change SubProgressMonitor implementation.
//                    final Range range = raster.computeRasterDataRange(null, SubProgressMonitor.create(pm, 1));
                    final Range range = raster.computeRasterDataRange(null, ProgressMonitor.NULL);
                    pm.worked(1);

                    checkCanceled(pm);
// todo - Proformance boost. But better change SubProgressMonitor implementation.
//                    final Histogram histogram = raster.computeRasterDataHistogram(null, 512, range,
//                                                                                  SubProgressMonitor.create(pm, 1));
                    final Histogram histogram = raster.computeRasterDataHistogram(null, 512, range,
                                                                                  ProgressMonitor.NULL);
                    pm.worked(1);

                    imageInfo = raster.createDefaultImageInfo(null, histogram, true);
                    raster.setImageInfo(imageInfo);
                } else {
                    imageInfo = raster.getImageInfo();
                }

                final double newMin = imageInfo.getMinDisplaySample();
                final double newMax = imageInfo.getMaxDisplaySample();
                final double gamma = (double) imageInfo.getGamma();

                checkCanceled(pm);
// todo - Proformance boost. But better change SubProgressMonitor implementation.
//                raster.quantizeRasterData(newMin, newMax, gamma, rgbSamples, singleBand ? 0 : 2 - i,
//                                          SubProgressMonitor.create(pm, 1));
                raster.quantizeRasterData(newMin, newMax, gamma, rgbSamples, singleBand ? 0 : 2 - i,
                                          ProgressMonitor.NULL);
                pm.worked(1);
            }

            checkCanceled(pm);
            if (singleBand) {
                final RasterDataNode raster = rasterDataNodes[0];
                final Color[] palette = raster.getImageInfo().getColorPalette();
                Guardian.assertEquals("palette.length must be 256", palette.length, 256);
                final byte[] r = new byte[256];
                final byte[] g = new byte[256];
                final byte[] b = new byte[256];
                for (int i = 0; i < 256; i++) {
                    r[i] = (byte) palette[i].getRed();
                    g[i] = (byte) palette[i].getGreen();
                    b[i] = (byte) palette[i].getBlue();
                }
                int colorIndex;
                for (int i = 0; i < rgbSamples.length; i += 3) {
                    colorIndex = rgbSamples[i] & 0xff;
                    // BufferedImage.TYPE_3BYTE_BGR order
                    rgbSamples[i] = b[colorIndex];
                    rgbSamples[i + 1] = g[colorIndex];
                    rgbSamples[i + 2] = r[colorIndex];
                }
                pm.worked(1);
            }

            // Create a BufferedImage of type TYPE_3BYTE_BGR (the fastest type)
            //
            final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            final ColorModel cm = new ComponentColorModel(cs,
                                                          false, // hasAlpha,
                                                          false, //isAlphaPremultiplied,
                                                          Transparency.OPAQUE, //  transparency,
                                                          DataBuffer.TYPE_BYTE); //transferType
            final DataBuffer db = new DataBufferByte(rgbSamples, rgbSamples.length);
            final WritableRaster wr = Raster.createInterleavedRaster(db, width, height, 3 * width, 3, RGB_BAND_OFFSETS,
                                                                     null);
            bufferedImage = new BufferedImage(cm, wr, false, null);
        } finally {
            pm.done();
        }

        stopWatch.stopAndTrace("ProductUtils.createRgbImage");
        return bufferedImage;
    }

    private static void checkCanceled(ProgressMonitor pm) throws IOException {
        if (pm.isCanceled()) {
            throw createUserTerminationException();
        }
    }

    private static IOException createUserTerminationException() {
        return new IOException("Process terminated by user."); /*I18N*/
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
     * java.awt.Rectangle,int,org.esa.beam.framework.dataop.maptransf.MapTransform) createMapEnvelope(product, rect,
     * step, mapTransform)} where <code>step</code> is the half of the minimum of the product scene raster width and
     * height.
     *
     * @param product
     * @param rect
     * @param mapTransform
     * @return the boundary in map coordinates for the given product
     */
    public static Point2D[] createMapEnvelope(final Product product,
                                              final Rectangle rect,
                                              final MapTransform mapTransform) {
        final int step = Math.min(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / 2;
        return createMapEnvelope(product, rect, step, mapTransform);
    }

    /**
     * Creates the boundary in map coordinates for the given product, source rectangle (in product pixel coordinates)
     * and the given map transfromation. The method delegates to {@link #createMapEnvelope(org.esa.beam.framework.datamodel.Product,
     * java.awt.Rectangle,int,org.esa.beam.framework.dataop.maptransf.MapTransform) createMapEnvelope(product, rect,
     * step, mapTransform)} where <code>step</code> is the half of the minimum of the product scene raster width and
     * height.
     *
     * @param product
     * @param rect
     * @param mapTransform
     * @return the boundary in map coordinates for the given product
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
     * @see #createPixelBoundary
     */
    public static GeoPos[] createGeoBoundary(Product product, int step) {
        final Rectangle rect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        return createGeoBoundary(product, rect, step);
    }

    /**
     * Creates the geographical boundary of the given region within the given product and returns it as a list of
     * geographical coordinates.
     * <p> This method delegates to {@link ProductUtils#createGeoBoundary(org.esa.beam.framework.datamodel.Product,java.awt.Rectangle,int,boolean) createGeoBoundary(Product, Rectangle, int, boolean)}
     * and the additional boolean parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param product the input product, must not be null
     * @param region  the region rectangle in product pixel coordinates, can be null for entire product
     * @param step    the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createPixelBoundary
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
     * @see #createPixelBoundary
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
     * @see #createPixelBoundary
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
     *
     * @param product the input product
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary
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
     * This method delegates to {@link ProductUtils#createGeoBoundaryPaths(org.esa.beam.framework.datamodel.Product,java.awt.Rectangle,int,boolean) createGeoBoundaryPaths(Product, Rectangle, int, boolean)}
     * and the aditional parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param product the input product
     * @param region  the region rectangle in product pixel coordinates, can be null for entire product
     * @param step    the step given in pixels
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step) {
        final boolean usePixelCenter = true;
        return createGeoBoundaryPaths(product, region, step, usePixelCenter);
    }

    /**
     * Converts the geographic boundary of the region within the given product into one, two or three shape objects. If
     * the product does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     *
     * @param product        the input product
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter <code>true</code> if the pixel center should be used to create the pathes
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step,
                                                       final boolean usePixelCenter) {
        Guardian.assertNotNull("product", product);
        final GeoCoding gc = product.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }
        final GeoPos[] geoPoints = ProductUtils.createGeoBoundary(product, region, step, usePixelCenter);
        ProductUtils.normalizeGeoPolygon(geoPoints);

        final ArrayList<GeneralPath> pathList = assemblePathList(geoPoints);

        return (GeneralPath[]) pathList.toArray(new GeneralPath[pathList.size()]);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * <code>rect</code> is 100 x 50 pixels and <code>step</code> is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p/>
     * <p>This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p> This method delegates to {@link ProductUtils#createPixelBoundary(org.esa.beam.framework.datamodel.Product,java.awt.Rectangle,int,boolean) createPixelBoundary(Product, Rectangle, int, boolean)}
     * and the additional boolean parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param product the product
     * @param rect    the source rectangle
     * @param step    the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     * @see #createPixelBoundary
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
     * @see #createPixelBoundary
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
     * @see #createPixelBoundary
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
     * <p> This method delegates to {@link ProductUtils#createRectBoundary(java.awt.Rectangle,int,boolean) createRectBoundary(Rectangle, int, boolean)}
     * and the additional boolean parameter <code>usePixelCenter</code> is <code>true</code>.
     *
     * @param rect the source rectangle
     * @param step the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     * @see #createPixelBoundary
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
     * @see #createPixelBoundary
     */
    public static PixelPos[] createRectBoundary(final Rectangle rect, int step, final boolean usePixelCenter) {
        final float insetDistance;
        if (usePixelCenter) {
            insetDistance = 0.5f;
        } else {
            insetDistance = 0.0f;
        }
        final float x1 = rect.x + insetDistance;
        final float y1 = rect.y + insetDistance;
        final float w = rect.width - 2 * insetDistance;
        final float h = rect.height - 2 * insetDistance;
        final float x2 = x1 + w;
        final float y2 = y1 + h;

        if (step <= 0) {
            step = 2 * Math.max(rect.width, rect.height); // don't step!
        }

        final ArrayList<PixelPos> pixelPosList = new ArrayList<PixelPos>(rect.width * rect.height / step + 10);

        float lastX = 0;
        for (float x = x1; x < x2; x += step) {
            pixelPosList.add(new PixelPos(x, y1));
            lastX = x;
        }

        float lastY = 0;
        for (float y = y1; y < y2; y += step) {
            pixelPosList.add(new PixelPos(x2, y));
            lastY = y;
        }

        pixelPosList.add(new PixelPos(x2, y2));

        for (float x = lastX; x > x1; x -= step) {
            pixelPosList.add(new PixelPos(x, y2));
        }

        pixelPosList.add(new PixelPos(x1, y2));

        for (float y = lastY; y > y1; y -= step) {
            pixelPosList.add(new PixelPos(x1, y));
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

        int numCodings = source.getNumFlagCodings();

        for (int n = 0; n < numCodings; n++) {
            FlagCoding sourceFlagCoding = source.getFlagCodingAt(n);
            copyFlagCoding(sourceFlagCoding, target);
        }
    }

    /**
     * Copies the given source flag coding to the target product
     *
     * @param sourceFlagCoding the source flag coding
     * @param target           the target product
     */
    public static void copyFlagCoding(FlagCoding sourceFlagCoding, Product target) {
        FlagCoding flagCoding = new FlagCoding(sourceFlagCoding.getName());
        flagCoding.setDescription(sourceFlagCoding.getDescription());
        target.addFlagCoding(flagCoding);
        copyMetadata(sourceFlagCoding, flagCoding);
    }

    /**
     * Copies bit-mask definitions from the target product into the source product. The method will
     * only copy those bit-mask definitions which are valid in the context of the target product.
     * Bit-mask overlay informations are also copied if valid in the context of the target bands.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     * @see org.esa.beam.framework.datamodel.Product#getBitmaskDefs()
     * @see org.esa.beam.framework.datamodel.RasterDataNode#getBitmaskOverlayInfo()
     */
    public static void copyBitmaskDefsAndOverlays(final Product sourceProduct, final Product targetProduct) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("targetProduct", targetProduct);

        final BitmaskDef[] bitmaskDefs = sourceProduct.getBitmaskDefs();
        for (final BitmaskDef bitmaskDef : bitmaskDefs) {
            if (targetProduct.isCompatibleBandArithmeticExpression(bitmaskDef.getExpr())) {
                targetProduct.addBitmaskDef(bitmaskDef.createCopy());
            }
        }

        for (int i = 0; i < sourceProduct.getNumBands(); i++) {
            final Band sourceBand = sourceProduct.getBandAt(i);
            final Band targetBand = targetProduct.getBand(sourceBand.getName());
            if (targetBand != null && sourceBand.getBitmaskOverlayInfo() != null) {
                final BitmaskDef[] sourceBandBitmaskDefs = sourceBand.getBitmaskOverlayInfo().getBitmaskDefs();
                BitmaskOverlayInfo targetBitmaskOverlayInfo = targetBand.getBitmaskOverlayInfo();
                if (targetBitmaskOverlayInfo == null) {
                    targetBitmaskOverlayInfo = new BitmaskOverlayInfo();
                    targetBand.setBitmaskOverlayInfo(targetBitmaskOverlayInfo);
                }
                for (final BitmaskDef sourceBandBitmaskDef : sourceBandBitmaskDefs) {
                    final BitmaskDef targetBandBitmaskDef = targetProduct.getBitmaskDef(sourceBandBitmaskDef.getName());
                    if (targetBandBitmaskDef != null) {
                        targetBitmaskOverlayInfo.addBitmaskDef(targetBandBitmaskDef);
                    }
                }
            }
        }
    }

    /**
     * Copies the bitmask definitions from the source product to the target.
     * <p>IMPORTANT NOTE: This method should only be used, if it is known that all bitmask definitions
     * in the source product will also be valid in the target product.
     * Furthermore this method does NOT copy the bitmask overlay information from source bands
     * to target bands.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     * @see #copyBitmaskDefsAndOverlays
     */
    public static void copyBitmaskDefs(Product sourceProduct, Product targetProduct) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("target", targetProduct);

        int numBitmaskDefs = sourceProduct.getNumBitmaskDefs();

        for (int i = 0; i < numBitmaskDefs; i++) {
            BitmaskDef bitmaskDef = sourceProduct.getBitmaskDefAt(i);
            targetProduct.addBitmaskDef(bitmaskDef.createCopy());
        }
    }

    /**
     * Copies all bands which contain a flagcoding from the source product to the target product.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     */
    public static void copyFlagBands(Product sourceProduct, Product targetProduct) {
        Guardian.assertNotNull("source", sourceProduct);
        Guardian.assertNotNull("target", targetProduct);
        if (sourceProduct.getNumFlagCodings() > 0) {
            Band sourceBand;
            Band targetBand;
            FlagCoding coding;

            copyFlagCodings(sourceProduct, targetProduct);
            copyBitmaskDefs(sourceProduct, targetProduct);

            // loop over bands and check if they have a flags coding attached
            for (int i = 0; i < sourceProduct.getNumBands(); i++) {
                sourceBand = sourceProduct.getBandAt(i);
                String bandName = sourceBand.getName();
                coding = sourceBand.getFlagCoding();
                if (coding != null) {
                    targetBand = copyBand(bandName, sourceProduct, targetProduct);
                    targetBand.setFlagCoding(targetProduct.getFlagCoding(coding.getName()));
                }
            }
        }
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
     * @param sourceBandName the name of the band to be copied.
     * @param sourceProduct  the source product.
     * @param targetProduct  the target product.
     * @return the copied band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     */
    public static Band copyBand(String sourceBandName, Product sourceProduct, Product targetProduct) {
        return copyBand(sourceBandName, sourceProduct, sourceBandName, targetProduct);
    }

    /**
     * Copies the named band from the source product to the target product.
     *
     * @param sourceBandName the name of the band to be copied.
     * @param sourceProduct  the source product.
     * @param targetBandName the name of the band copied.
     * @param targetProduct  the target product.
     * @return the copied band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     */
    public static Band copyBand(String sourceBandName, Product sourceProduct,
                                String targetBandName, Product targetProduct) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("targetProduct", targetProduct);

        if (sourceBandName == null || sourceBandName.length() == 0) {
            return null;
        }
        final Band sourceBand = sourceProduct.getBand(sourceBandName);
        if (sourceBand == null) {
            return null;
        }
        Band targetBand = new Band(targetBandName,
                                   sourceBand.getDataType(),
                                   sourceBand.getRasterWidth(),
                                   sourceBand.getRasterHeight());
        targetBand.setDescription(sourceBand.getDescription());
        targetBand.setUnit(sourceBand.getUnit());
        targetBand.setScalingFactor(sourceBand.getScalingFactor());
        targetBand.setScalingOffset(sourceBand.getScalingOffset());
        targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
        targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
        targetBand.setNoDataValue(sourceBand.getNoDataValue());
        targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
        copySpectralAttributes(sourceBand, targetBand);
        targetProduct.addBand(targetBand);
        return targetBand;
    }

    /**
     * Copies the spectral attributes from source band to target band. These attributes are:
     * <ul>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSpectralBandIndex() spectral band index},</li>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSpectralWavelength() the central wavelength},</li>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSpectralBandwidth() the spectral bandwidth} and</li>
     * <li>{@link org.esa.beam.framework.datamodel.Band#getSolarFlux() the solar spectral flux}.</li>
     * </ul>
     *
     * @param source the source band
     * @param target the target band
     */
    public static void copySpectralAttributes(Band source, Band target) {
        Guardian.assertNotNull("source", source);
        Guardian.assertNotNull("target", target);

        target.setSpectralBandIndex(source.getSpectralBandIndex());
        target.setSpectralWavelength(source.getSpectralWavelength());
        target.setSpectralBandwidth(source.getSpectralBandwidth());
        target.setSolarFlux(source.getSolarFlux());
    }

    /**
     * Copies the geocoding from the source product to target product.
     * <p/>
     * <p>If the geo-coding in the source product is a {@link TiePointGeoCoding} or a {@link PixelGeoCoding},
     * the method expects existing latitude and longitude tie-point grids respectively existing latitude and longitude
     * bands in the target product. The method will NOT automatically copy them as well.
     * This behaviour may change in the future.</p>
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
     * @deprecated in 4.0, use {@link #createScatterPlotImage(RasterDataNode,float,float,RasterDataNode,float,float,ROI,int,int,Color,BufferedImage,ProgressMonitor)} instead
     */
    public static BufferedImage createScatterPlotImage(final RasterDataNode raster1,
                                                       final float sampleMin1,
                                                       final float sampleMax1,
                                                       final RasterDataNode raster2,
                                                       final float sampleMin2,
                                                       final float sampleMax2,
                                                       final ROI roi,
                                                       final int width,
                                                       final int height,
                                                       final Color background,
                                                       BufferedImage image) throws IOException {
        return createScatterPlotImage(raster1, sampleMin1, sampleMax1,
                                      raster2, sampleMin2, sampleMax2,
                                      roi,
                                      width, height,
                                      background,
                                      image,
                                      ProgressMonitor.NULL);
    }

    /**
     * Creates a scatter plot image from two raster data nodes.
     *
     * @param raster1    the first raster data node
     * @param sampleMin1 the minimum sample value to be considered in the first raster
     * @param sampleMax1 the maximum sample value to be considered in the first raster
     * @param raster2    the second raster data node
     * @param sampleMin2 the minimum sample value to be considered in the second raster
     * @param sampleMax2 the maximum sample value to be considered in the second raster
     * @param roi        an otional ROI to be used for the computation
     * @param width      the width of the output image
     * @param height     the height of the output image
     * @param background the background color of the output image
     * @param image      an image to be used as output image, if <code>null</code> a new image is created
     * @return the scatter plot image
     */
    public static BufferedImage createScatterPlotImage(final RasterDataNode raster1,
                                                       final float sampleMin1,
                                                       final float sampleMax1,
                                                       final RasterDataNode raster2,
                                                       final float sampleMin2,
                                                       final float sampleMax2,
                                                       final ROI roi,
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

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Debug.trace("RasterDataNode: Started computing scatter-plot: " + stopWatch.toString());

        if (image == null
                || image.getWidth() != width
                || image.getHeight() != height
                || !(image.getColorModel() instanceof IndexColorModel)
                || !(image.getRaster().getDataBuffer() instanceof DataBufferByte)) {
            final int palSize = 256;
            final byte[] r = new byte[palSize];
            final byte[] g = new byte[palSize];
            final byte[] b = new byte[palSize];
            r[0] = (byte) background.getRed();
            g[0] = (byte) background.getGreen();
            b[0] = (byte) background.getBlue();
            for (int i = 1; i < palSize; i++) {
                r[i] = (byte) i;
                g[i] = (byte) i;
                b[i] = (byte) i;
            }
            image = new BufferedImage(width,
                                      height,
                                      BufferedImage.TYPE_BYTE_INDEXED,
                                      new IndexColorModel(8, palSize, r, g, b));
        }

        final int rasterW = raster1.getSceneRasterWidth();
        final int rasterH = raster1.getSceneRasterHeight();
        final byte[] pixelValues = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        final float xScale = width / (sampleMax1 - sampleMin1);
        final float yScale = height / (sampleMax2 - sampleMin2);

        float sample1;
        float sample2;

        int pixelX;
        int pixelY;
        int pixelIndex;
        int pixelValue;
        int numPixels = 0;

        float[] line1 = new float[rasterW];
        float[] line2 = new float[rasterW];
        final IndexValidator pixelValidator1 = raster1.createPixelValidator(0, roi);
        final IndexValidator pixelValidator2 = raster2.createPixelValidator(0, roi);
        pm.beginTask("Creating scatter plot image...", rasterH * 3);
        try {
            for (int y = 0; y < rasterH; y++) {
                raster1.readPixels(0, y, rasterW, 1, line1, SubProgressMonitor.create(pm, 1));
                raster2.readPixels(0, y, rasterW, 1, line2, SubProgressMonitor.create(pm, 1));
                for (int x = 0; x < rasterW; x++) {
                    final int index = y * rasterW + x;
                    if (pixelValidator1.validateIndex(index) && pixelValidator2.validateIndex(index)) {
                        sample1 = line1[x];
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            sample2 = line2[x];
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    pixelIndex = pixelX + pixelY * width;
                                    pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                    numPixels++;
                                }
                            }
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        stopWatch.stop();
        Debug.trace(
                "RasterDataNode: Ended scatter-plot drawing: " + stopWatch.toString() + ", numPixels = " + numPixels + ", roi=" + roi);

        return image;
    }

    /**
     * @deprecated in 4.0, use {@link #overlayBitmasks(org.esa.beam.framework.datamodel.RasterDataNode,javax.media.jai.PlanarImage,com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static PlanarImage overlayBitmasks(RasterDataNode raster, PlanarImage overlayPIm) throws IOException {
        return overlayBitmasks(raster, overlayPIm, ProgressMonitor.NULL);
    }

    /**
     * Draws all the activated bitmasks to the given ovelayPIm image.
     * The returned image is a new instance of planar image, not the given image!
     *
     * @param raster     the raster data node which contains all the activated bitmask definitions
     * @param overlayPIm the source image which is used as base image for all the overlays.
     * @param pm         a monitor to inform the user about progress
     * @return a new planar image which contains the source image and all the activated bitmasks.
     * @throws IOException if any reading process fails.
     */
    public static PlanarImage overlayBitmasks(RasterDataNode raster, PlanarImage overlayPIm, ProgressMonitor pm) throws
            IOException {

        final Product product = raster.getProduct();
        if (product == null) {
            throw new IllegalArgumentException("raster data node has not been added to a product");
        }

        final BitmaskDef[] bitmaskDefs = raster.getBitmaskDefs();
        if (bitmaskDefs.length == 0) {
            return overlayPIm;
        }

        final StopWatch stopWatch = new StopWatch();

        final int w = raster.getSceneRasterWidth();
        final int h = raster.getSceneRasterHeight();

        final Parser parser = raster.getProduct().createBandArithmeticParser();

        pm.beginTask("Creating bitmasks ...", bitmaskDefs.length * 2);
        try {
            for (int i = bitmaskDefs.length - 1; i >= 0; i--) {
                BitmaskDef bitmaskDef = bitmaskDefs[i];

                final String expr = bitmaskDef.getExpr();

                final Term term;
                try {
                    term = parser.parse(expr);
                } catch (ParseException e) {
                    final IOException ioException = new IOException("Illegal bitmask expression '" + expr + "'");
                    ioException.initCause(e);
                    throw ioException;
                }
                final RasterDataSymbol[] rasterSymbols = BandArithmetic.getRefRasterDataSymbols(term);
                final RasterDataNode[] rasterNodes = BandArithmetic.getRefRasters(rasterSymbols);

                // Ensures that all the raster data which are needed to create overlays are loaded
                ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
                subPm.beginTask("Reading raster data...", rasterNodes.length);
                try {
                    for (RasterDataNode rasterNode : rasterNodes) {
                        if (!rasterNode.hasRasterData()) {
                            rasterNode.readRasterDataFully(SubProgressMonitor.create(subPm, 1));
                        } else {
                            subPm.worked(1);
                        }
                    }
                } finally {
                    subPm.done();
                }

                final byte[] alphaData = new byte[w * h];
                product.readBitmask(0, 0, w, h, term, alphaData, (byte) (255 * bitmaskDef.getAlpha()), (byte) 0,
                                    SubProgressMonitor.create(pm, 1));

                Debug.trace("ProductSceneView: creating bitmask overlay '" + bitmaskDef.getName() + "'...");
                BufferedImage alphaBIm = ImageUtils.createGreyscaleColorModelImage(w, h, alphaData);
                PlanarImage alphaPIm = PlanarImage.wrapRenderedImage(alphaBIm);

                overlayPIm = JAIUtils.createAlphaOverlay(overlayPIm, alphaPIm, bitmaskDef.getColor());
                Debug.trace("ProductSceneView: bitmask overlay OK");
            }
        } finally {
            pm.done();
        }
        stopWatch.stopAndTrace("overlay Bitmask");
        return overlayPIm;
    }

    /**
     * @deprecated in 4.0, use {@link #overlayBitmasks(org.esa.beam.framework.datamodel.RasterDataNode,java.awt.image.BufferedImage,com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static BufferedImage overlayBitmasks(final RasterDataNode raster, final BufferedImage overlayBIm) throws
            IOException {
        return overlayBitmasks(raster, overlayBIm, ProgressMonitor.NULL);
    }

    /**
     * Draws all the activated bitmasks to the given ovelayBIm image.
     *
     * @param raster     the raster data node which contains all the activated bitmask definitions
     * @param overlayBIm the source image which is used as base image for all the overlays.
     * @param pm         a monitor to inform the user about progress
     * @return the modified given overlaBImm which contains all the activated bitmasks.
     * @throws IOException if any reading process fails.
     */
    public static BufferedImage overlayBitmasks(final RasterDataNode raster, final BufferedImage overlayBIm,
                                                ProgressMonitor pm) throws
            IOException {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        BitmaskDef[] bitmaskDefs = raster.getBitmaskDefs();
        if (bitmaskDefs.length == 0) {
            return overlayBIm;
        }

        final Product product = raster.getProduct();
        if (product == null) {
            throw new IllegalArgumentException("raster data node has not been added to a product");
        }

        final int w = raster.getSceneRasterWidth();
        final int h = raster.getSceneRasterHeight();

        final Parser parser = raster.getProduct().createBandArithmeticParser();

        pm.beginTask("Creating bitmasks ...", bitmaskDefs.length);
        try {
            for (int i = bitmaskDefs.length - 1; i >= 0; i--) {
                final BitmaskDef bitmaskDef = bitmaskDefs[i];

                final String expr = bitmaskDef.getExpr();

                final Term term;
                try {
                    term = parser.parse(expr);
                } catch (ParseException e) {
                    final IOException ioException = new IOException("Illegal bitmask expression '" + expr + "'");
                    ioException.initCause(e);
                    throw ioException;
                }
                final float alpha = bitmaskDef.getAlpha();
                final Color color = bitmaskDef.getColor();
                final float overlayAlphaRed = color.getRed() * alpha;
                final float overlayAlphaGreen = color.getGreen() * alpha;
                final float overlayAlphaBlue = color.getBlue() * alpha;
                final float alphaInv = (1 - alpha);

                // the formula to compute the new destination color
                // newColor = overlayColor * alpha + oldColor * (1 - alpha)

                Debug.trace("ProductSceneView: creating bitmask overlay '" + bitmaskDef.getName() + "'...");
                final RasterDataLoop loop = new RasterDataLoop(0, 0, w, h, new Term[]{term},
                                                               SubProgressMonitor.create(pm, 1));
                loop.forEachPixel(new RasterDataLoop.Body() {
                    public void eval(RasterDataEvalEnv env, int pixelIndex) {
                        if (term.evalB(env)) {
                            final int oldRGB = overlayBIm.getRGB(env.getPixelX(), env.getPixelY());
                            final int oldRed = (oldRGB & 0x00ff0000) >> 16;
                            final int oldGreen = (oldRGB & 0x0000ff00) >> 8;
                            final int oldBlue = oldRGB & 0x000000ff;

                            int rgb = 0;
                            rgb += overlayAlphaRed + oldRed * alphaInv;
                            rgb = rgb << 8;
                            rgb += overlayAlphaGreen + oldGreen * alphaInv;
                            rgb = rgb << 8;
                            rgb += overlayAlphaBlue + oldBlue * alphaInv;

                            overlayBIm.setRGB(env.getPixelX(), env.getPixelY(), rgb);
                        }
                    }
                }, "Drawing bitmasks..."); /*I18N*/

                // needed to stop creating a wrong image with not completly computed masks.
                if (pm.isCanceled()) {
                    throw new IOException("Process terminated by user.");   /*I18N*/
                }

                Debug.trace("ProductSceneView: bitmask overlay OK");
            }
        } finally {
            pm.done();
        }
        stopWatch.stopAndTrace("overlay Bitmask");
        return overlayBIm;
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
     * @see #denormalizeGeoPolygon
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
            if (lonDiff > 180.f) {
                increment -= 360.f;
            } else if (lonDiff < -180.f) {
                increment += 360.f;
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

        if (minLon < -180.f) {
            posNormalized = true;
        }
        if (maxLon > 180.f) {
            negNormalized = true;
        }

        if (negNormalized && !posNormalized) {
            normalized = 1;
        } else if (!negNormalized && posNormalized) {
            normalized = -1;
            for (int i = 0; i < polygon.length; i++) {
                polygon[i].lon += 360;
            }
        } else if (negNormalized && posNormalized) {
            normalized = 2;
        }

        return normalized;
    }

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
     *                  #convertToGeoPath} method
     * @param geoCoding the geocoding used to convert the geographic coordinates into pixel coordinates.
     * @return a <code>GeneralPath</code> given in pixel coordinates.
     * @throws IllegalArgumentException if one of the given parameter is null.
     * @throws IllegalStateException    if the given geoPath is not a geo referenced <code>GeneralPath</code> wich
     *                                  contains only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types.
     * @see #convertToGeoPath
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

    public static GeoTIFFMetadata createGeoTIFFMetadata(final Product product) {
        final GeoCoding geoCoding = product != null ? product.getGeoCoding() : null;
        GeoTIFFMetadata metadata = null;
        if (geoCoding instanceof MapGeoCoding) {
            final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            final MapInfo mapInfo = mapGeoCoding.getMapInfo();
            final MapProjection mapProjection = mapInfo.getMapProjection();
            final MapTransform mapTransform = mapProjection.getMapTransform();
            final Datum datum = mapInfo.getDatum();

            metadata = new GeoTIFFMetadata();
            if (MathUtils.equalValues(mapInfo.getOrientation(), 0.0f)) {
                metadata.setModelPixelScale(mapInfo.getPixelSizeX(), mapInfo.getPixelSizeY());
                metadata.setModelTiePoint(mapInfo.getPixelX(), mapInfo.getPixelY(),
                                          mapInfo.getEasting(), mapInfo.getNorthing());
            } else {
                double theta = Math.toRadians(mapInfo.getOrientation());
                Matrix m1 = new Matrix(new double[][]{
                        {1, 0, -mapInfo.getPixelX()},
                        {0, 1, -mapInfo.getPixelY()},
                        {0, 0, 1},
                });
                Matrix m2 = new Matrix(new double[][]{
                        {Math.cos(theta), Math.sin(theta), 0},
                        {-Math.sin(theta), Math.cos(theta), 0},
                        {0, 0, 1},
                }).times(m1);
                Matrix m3 = new Matrix(new double[][]{
                        {mapInfo.getPixelSizeX(), 0, 0},
                        {0, -mapInfo.getPixelSizeY(), 0},
                        {0, 0, 1},
                }).times(m2);
                Matrix m4 = new Matrix(new double[][]{
                        {1, 0, mapInfo.getEasting()},
                        {0, 1, mapInfo.getNorthing()},
                        {0, 0, 1},
                }).times(m3);
                double[][] m = m4.getArray();
                metadata.setModelTransformation(new double[]{
                        m[0][0], m[0][1], 0.0, m[0][2],
                        m[1][0], m[1][1], 0.0, m[1][2],
                        0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 1.0
                });
            }

            metadata.addGeoShortParam(GeoTIFFCodes.GTRasterTypeGeoKey, GeoTIFFCodes.RasterPixelIsArea);
            metadata.addGeoAscii(GeoTIFFCodes.GTCitationGeoKey, "org.esa.beam.util.geotiff.GeoTIFF");
            if (Datum.WGS_84.equals(datum)) {
                metadata.addGeoShortParam(GeoTIFFCodes.GeographicTypeGeoKey, EPSGCodes.GCS_WGS_84);
            } else {
                metadata.addGeoShortParam(GeoTIFFCodes.GeographicTypeGeoKey, EPSGCodes.GCS_WGS_84);
            }
            metadata.addGeoAscii(GeoTIFFCodes.PCSCitationGeoKey, mapProjection.getName() + " with " + datum.getName());
            final String mapUnit = mapProjection.getMapUnit();
            if ("meter".equalsIgnoreCase(mapUnit)) {
                metadata.addGeoShortParam(GeoTIFFCodes.ProjLinearUnitsGeoKey, EPSGCodes.Linear_Meter);
            } else if ("foot".equalsIgnoreCase(mapUnit)) {
                metadata.addGeoShortParam(GeoTIFFCodes.ProjLinearUnitsGeoKey, EPSGCodes.Linear_Foot);
            } else if ("degree".equalsIgnoreCase(mapUnit)) {
                metadata.addGeoShortParam(GeoTIFFCodes.GeogAngularUnitsGeoKey, EPSGCodes.Angular_Degree);
            } else if ("radian".equalsIgnoreCase(mapUnit)) {
                metadata.addGeoShortParam(GeoTIFFCodes.GeogAngularUnitsGeoKey, EPSGCodes.Angular_Radian);
            }
/*
            metadata.addGeoDoubleParam(GeoTIFFCodes.ProjAzimuthAngleGeoKey, mapInfo.getOrientation());
            metadata.addGeoShortParam(GeoTIFFCodes.GeogAzimuthUnitsGeoKey, EPSGCodes.Angular_Degree);
*/
            final double[] parameterValues = mapTransform.getParameterValues();
            if (TransverseMercatorDescriptor.NAME.equals(mapTransform.getDescriptor().getName())) {
                metadata.addGeoShortParam(GeoTIFFCodes.GTModelTypeGeoKey, GeoTIFFCodes.ModelTypeProjected);
                int utmEPSGCode = findMatchingUtmEpsgCode(parameterValues);
                if (utmEPSGCode != -1) {
                    metadata.addGeoShortParam(GeoTIFFCodes.ProjectedCSTypeGeoKey, utmEPSGCode);
                } else {
                    metadata.addGeoShortParam(GeoTIFFCodes.ProjectedCSTypeGeoKey, GeoTIFFCodes.GTUserDefinedGeoKey);
                    metadata.addGeoShortParam(GeoTIFFCodes.ProjectionGeoKey, GeoTIFFCodes.GTUserDefinedGeoKey);
                    metadata.addGeoShortParam(GeoTIFFCodes.ProjCoordTransGeoKey, GeoTIFFCodes.CT_TransverseMercator);

                    metadata.addGeoDoubleParam(GeoTIFFCodes.GeogSemiMajorAxisGeoKey, parameterValues[0]); // semi_major
                    metadata.addGeoDoubleParam(GeoTIFFCodes.GeogSemiMinorAxisGeoKey, parameterValues[1]);  // semi_minor
                    metadata.addGeoDoubleParam(GeoTIFFCodes.ProjNatOriginLatGeoKey,
                                               parameterValues[2]); // latitude_of_origin (not used)
                    metadata.addGeoDoubleParam(GeoTIFFCodes.ProjNatOriginLongGeoKey,
                                               parameterValues[3]);   // central_meridian
                    metadata.addGeoDoubleParam(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey,
                                               parameterValues[4]);  // scale_factor
                    metadata.addGeoDoubleParam(GeoTIFFCodes.ProjFalseEastingGeoKey,
                                               parameterValues[5]);  // false_easting
                    metadata.addGeoDoubleParam(GeoTIFFCodes.ProjFalseNorthingGeoKey,
                                               parameterValues[6]);  // false_northing
                }
            } else if (LambertConformalConicDescriptor.NAME.equals(mapTransform.getDescriptor().getName())) {
                metadata.addGeoShortParam(GeoTIFFCodes.GTModelTypeGeoKey, GeoTIFFCodes.ModelTypeProjected);
                metadata.addGeoShortParam(GeoTIFFCodes.ProjectedCSTypeGeoKey, GeoTIFFCodes.GTUserDefinedGeoKey);
                metadata.addGeoShortParam(GeoTIFFCodes.ProjectionGeoKey, GeoTIFFCodes.GTUserDefinedGeoKey);
                metadata.addGeoShortParam(GeoTIFFCodes.ProjCoordTransGeoKey, GeoTIFFCodes.CT_LambertConfConic);

                metadata.addGeoDoubleParam(GeoTIFFCodes.GeogSemiMajorAxisGeoKey, parameterValues[0]); // semi_major
                metadata.addGeoDoubleParam(GeoTIFFCodes.GeogSemiMinorAxisGeoKey, parameterValues[1]);  // semi_minor
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjNatOriginLatGeoKey,
                                           parameterValues[2]); // latitude_of_origin
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjNatOriginLongGeoKey,
                                           parameterValues[3]);   // central_meridian
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjStdParallel1GeoKey,
                                           parameterValues[4]);  // latitude_of_intersection_1
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjStdParallel2GeoKey,
                                           parameterValues[5]);  // latitude_of_intersection_2
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey,
                                           parameterValues[6]);  // scale_factor

            } else if (StereographicDescriptor.NAME.equals(mapTransform.getDescriptor().getName())) {
                metadata.addGeoShortParam(GeoTIFFCodes.GTModelTypeGeoKey, GeoTIFFCodes.ModelTypeProjected);
                metadata.addGeoShortParam(GeoTIFFCodes.ProjectedCSTypeGeoKey, GeoTIFFCodes.GTUserDefinedGeoKey);
                metadata.addGeoShortParam(GeoTIFFCodes.ProjectionGeoKey, GeoTIFFCodes.GTUserDefinedGeoKey);
                metadata.addGeoShortParam(GeoTIFFCodes.ProjCoordTransGeoKey, GeoTIFFCodes.CT_PolarStereographic);

                metadata.addGeoDoubleParam(GeoTIFFCodes.GeogSemiMajorAxisGeoKey, parameterValues[0]); // semi_major
                metadata.addGeoDoubleParam(GeoTIFFCodes.GeogSemiMinorAxisGeoKey, parameterValues[1]);  // semi_minor
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjNatOriginLatGeoKey,
                                           parameterValues[2]); // latitude_of_origin
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjNatOriginLongGeoKey,
                                           parameterValues[3]);   // central_meridian
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey,
                                           parameterValues[4]);  // scale_factor
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjFalseEastingGeoKey, parameterValues[5]);  // false_easting
                metadata.addGeoDoubleParam(GeoTIFFCodes.ProjFalseNorthingGeoKey, parameterValues[6]);  // false_northing
            } else if (IdentityTransformDescriptor.NAME.equals(mapTransform.getDescriptor().getName())) {
                metadata.addGeoShortParam(GeoTIFFCodes.GTModelTypeGeoKey, GeoTIFFCodes.ModelTypeGeographic);
            } else {
                metadata = null;
            }
        }

        if (metadata == null && geoCoding != null) {
            metadata = new GeoTIFFMetadata();
            metadata.addGeoShortParam(GeoTIFFCodes.GTModelTypeGeoKey, GeoTIFFCodes.ModelTypeGeographic);
            metadata.addGeoShortParam(GeoTIFFCodes.GTRasterTypeGeoKey, GeoTIFFCodes.RasterPixelIsArea);
            metadata.addGeoShortParam(GeoTIFFCodes.GeographicTypeGeoKey, EPSGCodes.GCS_WGS_84);
            final int w = product.getSceneRasterWidth();
            final int h = product.getSceneRasterHeight();
            final int numTotMax = 32;
            int numHor = (int) Math.sqrt(numTotMax * ((double) w / (double) h));
            if (numHor < 2) {
                numHor = 2;
            }
            int numVer = numTotMax / numHor;
            if (numVer < 2) {
                numVer = 2;
            }
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos = new PixelPos();
            for (int y = 0; y < numVer; y++) {
                for (int x = 0; x < numHor; x++) {
                    pixelPos.x = w * (float) x / (numHor - 1.0f);
                    pixelPos.y = h * (float) y / (numVer - 1.0f);
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    metadata.addModelTiePoint(pixelPos.x, pixelPos.y, geoPos.lon, geoPos.lat);
                }
            }
        }
        return metadata;
    }

    private static int findMatchingUtmEpsgCode(double[] parameterValues) {
        int utmEPSGCode = -1;
        double[] utmParameterValues;
        for (int zoneIndex = 0; zoneIndex < UTM.MAX_UTM_ZONE; zoneIndex++) {
            utmParameterValues = UTM.getProjectionParams(zoneIndex, false);
            if (ArrayUtils.equalArrays(utmParameterValues, parameterValues, 1.0e-6)) {
                utmEPSGCode = EPSGCodes.PCS_WGS84_UTM_zone_1N + zoneIndex;
            }
            utmParameterValues = UTM.getProjectionParams(zoneIndex, true);
            if (ArrayUtils.equalArrays(utmParameterValues, parameterValues, 1.0e-6)) {
                utmEPSGCode = EPSGCodes.PCS_WGS84_UTM_zone_1S + zoneIndex;
            }
        }
        return utmEPSGCode;
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
                final ROIDefinition roi = raster.getROIDefinition();
                if (roi != null) {
                    final String bitmaskExpr = roi.getBitmaskExpr();
                    if (bitmaskExpr != null && !product.isCompatibleBandArithmeticExpression(bitmaskExpr)) {
                        raster.setROIDefinition(null);
                        String pattern = "ROI definition of output {0} ''{1}'' removed because it is not applicable.";   /*I18N*/
                        messages.add(MessageFormat.format(pattern, type, raster.getName()));
                    }
                }
            }
        });
        return (String[]) messages.toArray(new String[messages.size()]);
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
     * {@link org.esa.beam.framework.datamodel.RasterDataNode#isScalingApplied() sourceBand.scalingApplied} is true,
     * this method will always create the related target band with the raw data type {@link ProductData#TYPE_FLOAT32},
     * regardless which raw data type the source band has.
     * In this case, {@link org.esa.beam.framework.datamodel.RasterDataNode#getScalingFactor() targetBand.scalingFactor}
     * will always be 1.0, {@link org.esa.beam.framework.datamodel.RasterDataNode#getScalingOffset() targetBand.scalingOffset}
     * will always be 0.0 and
     * {@link org.esa.beam.framework.datamodel.RasterDataNode#isLog10Scaled() targetBand.log10Scaled} will be taken from the source band.
     * This ensures that source pixel resampling methods operating on floating point
     * data can be stored without loss in accuracy in the target band.
     * <p/>
     * <p>Furthermore, the
     * {@link org.esa.beam.framework.datamodel.RasterDataNode#isNoDataValueSet() targetBands.noDataValueSet}
     * and {@link org.esa.beam.framework.datamodel.RasterDataNode#isNoDataValueUsed() targetBands.noDataValueUsed}
     * properties will always be true for all added target bands. The {@link org.esa.beam.framework.datamodel.RasterDataNode#getGeophysicalNoDataValue() targetBands.geophysicalNoDataValue},
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

        final Map<Band, RasterDataNode> sourceNodes = new HashMap<Band, RasterDataNode>(sourceProduct.getNumBands() + sourceProduct.getNumTiePointGrids() + 10);

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
                ProductUtils.copySpectralAttributes(sourceBand, targetBand);
                FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
                if (sourceFlagCoding != null) {
                    String flagCodingName = sourceFlagCoding.getName();
                    FlagCoding targetFlagCoding = targetProduct.getFlagCoding(flagCodingName);
                    Debug.assertNotNull(
                            targetFlagCoding); // should not happen because flag codings should be already in product
                    targetBand.setFlagCoding(targetFlagCoding);
                } else {
                    targetBand.setFlagCoding(null);
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
                if (Float.isNaN(lon)) {
                    continue;
                }
                if (lon < minLon) {
                    minLon = lon;
                }
                if (lon > maxLon) {
                    maxLon = lon;
                }
                path.lineTo(lon, geoPoints[i].getLat());
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

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API

    /**
     * @deprecated in 3.x, use {@link #createGeoBoundaryPaths}
     */
    public static GeneralPath createGeoBoundaryPath(Product product) {
        final Rectangle rect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final int step = Math.min(rect.width, rect.height) / 8;
        return createGeoBoundaryPath(product, rect, step > 0 ? step : 1);
    }

    /**
     * @deprecated in 3.x, use {@link #createGeoBoundaryPaths}
     */
    public static GeneralPath createGeoBoundaryPath(Product product, Rectangle rect, int step) {
        GeoPos[] geoPoints = createGeoBoundary(product, rect, step);
        GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, geoPoints.length + 8);
        if (geoPoints.length > 1) {
            path.moveTo(geoPoints[0].getLon(), geoPoints[0].getLat());
            for (int i = 1; i < geoPoints.length; i++) {
                path.lineTo(geoPoints[i].getLon(), geoPoints[i].getLat());
            }
            path.closePath();
        }
        return path;
    }
}
