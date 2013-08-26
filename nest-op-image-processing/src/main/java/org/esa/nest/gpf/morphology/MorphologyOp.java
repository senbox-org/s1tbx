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
package org.esa.nest.gpf.morphology;

import com.bc.ceres.core.ProgressMonitor;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.gpf.OperatorUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Morphology Operators: Dilate, Erode, Open, Close
 */
@OperatorMetadata(alias = "MorphologyOperator",
        category = "Image Processing",
        authors = "Emanuela Boros",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Dilate, Erode, Open, Close")
public class MorphologyOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;
    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
    rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;
    @Parameter(valueSet = {DILATE_OPERATOR, ERODE_OPERATOR, OPEN_OPERATOR,
        CLOSE_OPERATOR}, defaultValue = DILATE_OPERATOR,
    label = "Operator")
    private String operator;
    @Parameter(description = "Iterations", interval = "[1, 100]", defaultValue = "1", label = "Iterations")
    private int nIterations = 1;
    static final String DILATE_OPERATOR = "Dilate";
    static final String ERODE_OPERATOR = "Erode";
    static final String OPEN_OPERATOR = "Open";
    static final String CLOSE_OPERATOR = "Close";
    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();
    private int sourceImageWidth;
    private int sourceImageHeight;
    private static boolean processed = false;
    private int halfSizeX;
    private int halfSizeY;
    private int filterSizeX = 3;
    private int filterSizeY = 3;
    private static ImagePlus fullImagePlus;

    /**
     * Default constructor. The graph processing framework requires that an
     * operator has a default constructor.
     */
    public MorphologyOp() {
    }

    /**
     * Set the morphology operator. This function is used by unit test only.
     *
     * @param s The filter name.
     */
    public void SetOperator(String s) {

        if (s.equals(DILATE_OPERATOR)
                || s.equals(ERODE_OPERATOR)
                || s.equals(OPEN_OPERATOR)
                || s.equals(CLOSE_OPERATOR)) {
            operator = s;
        } else {
            throw new OperatorException(s + " is an invalid filter name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type
     * {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct}
     * annotation or by calling {@link #setTargetProduct} method.</p> <p>The
     * framework calls this method after it has created this operator. Any
     * client code that must be performed before computation of tile data should
     * be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            halfSizeX = filterSizeX / 2;
            halfSizeY = filterSizeY / 2;

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     *
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);
    }

    /**
     * Called by the framework in order to compute a tile for the given target
     * band. <p>The default implementation throws a runtime exception with the
     * message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param pm A progress monitor which should be used to determine
     * computation cancellation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm)
            throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;

            final Rectangle sourceTileRectangle = getSourceTileRectangle(x0, y0, w, h);
            Tile sourceRaster;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster = getSourceTile(sourceBand, sourceTileRectangle);
            if (sourceRaster == null) {
                throw new OperatorException("Cannot get source tile");
            }

            computeOperator(sourceBand, sourceRaster, targetTile, x0, y0, w, h);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0 X coordinate of the upper left corner point of the target tile
     * rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile
     * rectangle.
     * @param w The width of the target tile rectangle.
     * @param h The height of the target tile rectangle.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfSizeX) {
            sx0 -= halfSizeX;
            sw += halfSizeX;
        }

        if (y0 >= halfSizeY) {
            sy0 -= halfSizeY;
            sh += halfSizeY;
        }

        if (x0 + w + halfSizeX <= sourceImageWidth) {
            sw += halfSizeX;
        }

        if (y0 + h + halfSizeY <= sourceImageHeight) {
            sh += halfSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     *
     * Apply an Operator Operator
     *
     * @param sourceBand The source band.
     * @param sourceRaster The source tile for the band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param x0 X coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     */
    private synchronized void computeOperator(final Band sourceBand, final Tile sourceRaster,
            final Tile targetTile, final int x0, final int y0, final int w, final int h) {

        if (!processed) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);
            processed = true;
        }
        final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

        ByteProcessor fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);

        final Rectangle srcTileRectangle = sourceRaster.getRectangle();

        fullByteProcessor.setRoi(srcTileRectangle);

        ImageProcessor roiImageProcessor = fullByteProcessor.crop();

        for (int i = 0; i < nIterations; i++) {
            if (operator.equals(DILATE_OPERATOR)) {
                roiImageProcessor.dilate();
            } else if (operator.equals(ERODE_OPERATOR)) {
                roiImageProcessor.erode();
            } else if (operator.equals(CLOSE_OPERATOR)) {
                roiImageProcessor.dilate();
                roiImageProcessor.erode();
            } else if (operator.equals(OPEN_OPERATOR)) {
                roiImageProcessor.erode();
                roiImageProcessor.dilate();
            }
        }

        final ProductData trgData = targetTile.getDataBuffer();
        final ProductData ijData = ProductData.createInstance((byte[]) roiImageProcessor.getPixels());

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            for (int x = x0; x < maxX; ++x) {

                trgData.setElemDoubleAt(targetTile.getDataBufferIndex(x, y),
                        ijData.getElemDoubleAt(sourceRaster.getDataBufferIndex(x, y)));
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing
     * framework via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}. This
     * class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MorphologyOp.class);
            setOperatorUI(MorphologyOpUI.class);
        }
    }
}
