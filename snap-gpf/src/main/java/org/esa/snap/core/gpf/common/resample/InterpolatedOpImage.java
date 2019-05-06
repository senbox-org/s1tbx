package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

/**
 * @author Tonio Fincke
 */
class InterpolatedOpImage extends GeometricOpImage {

    private final double scaleX;
    private final double scaleY;
    private final float offsetX;
    private final float offsetY;
    private final double noDataValue;
    private final int dataBufferType;
    private Upsampling upsampling;
    private RasterDataNode rasterDataNode;

    InterpolatedOpImage(RasterDataNode rasterDataNode,  RenderedImage sourceImage, ImageLayout layout, double noDataValue, int dataBufferType,
                               Upsampling upsampling, AffineTransform sourceImageToModelTransform,
                               AffineTransform referenceImageToModelTransform) throws NoninvertibleTransformException {
        super(vectorize(sourceImage), layout, null, true, createBorderExtender(noDataValue), null,
              createBackground(noDataValue));
        this.rasterDataNode = rasterDataNode;
        this.noDataValue = noDataValue;
        final AffineTransform transform = new AffineTransform(referenceImageToModelTransform);
        transform.concatenate(sourceImageToModelTransform.createInverse());
        scaleX = transform.getScaleX();
        scaleY = transform.getScaleY();
        offsetX = (float) (referenceImageToModelTransform.getTranslateX() / sourceImageToModelTransform.getScaleX()) -
                (float) (sourceImageToModelTransform.getTranslateX() / sourceImageToModelTransform.getScaleX());
        offsetY = (float) (referenceImageToModelTransform.getTranslateY() / sourceImageToModelTransform.getScaleY()) -
                (float) (sourceImageToModelTransform.getTranslateY() / sourceImageToModelTransform.getScaleY());
        this.upsampling = upsampling;
        this.dataBufferType = dataBufferType;
    }


    private static BorderExtender createBorderExtender(double value) {
        return new BorderExtenderConstant(new double[]{value});
    }

    private static double[] createBackground(double value) {
        return new double[]{value};
    }

    /**
     * The sources are cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        Raster source = sources[0];
        final Rectangle srcRect = mapDestRect(destRect, 0);
        RasterAccessor srcAccessor = new RasterAccessor(source, srcRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());
        //final Interpolator interpolator = InterpolatorFactory.createInterpolator(interpolationType, dataBufferType);
        Interpolator interpolator = upsampling.createUpsampler(rasterDataNode, dataBufferType);
        interpolator.init(rasterDataNode, srcAccessor, dstAccessor, noDataValue);
        interpolator.interpolate(destRect, srcRect, scaleX, scaleY, offsetX, offsetY);
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }

    }

    @Override
    protected Rectangle forwardMapRect(Rectangle rectangle, int i) {
        //calculates the dest rectangle for a source rectangle
        final int x = (int) (rectangle.getX() * (1 / scaleX) - offsetX);
        final int y = (int) (rectangle.getY() * (1 / scaleY) - offsetY);
        final int width = (int) Math.ceil((rectangle.getWidth() - 1) * (1 / scaleX));
        final int height = (int) Math.ceil((rectangle.getHeight() - 1) * (1 / scaleY));
        return new Rectangle(x, y, width, height);
    }

    @Override
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {
        if(destRect == null) {
            throw new IllegalArgumentException("destRect must not be null");
        } else if(sourceIndex >= 0 && sourceIndex < this.getNumSources()) {
            return backwardMapRect(destRect, sourceIndex);
        } else {
            throw new IllegalArgumentException("Invalid source index");
        }
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle rectangle, int i) {
        //calculates the source rectangle for a dest rectangle
        final int x = (int) (offsetX + rectangle.getX() * scaleX);
        final int y = (int) (offsetY + rectangle.getY() * scaleY);
        final int width = (int) Math.ceil(rectangle.getWidth() * scaleX) + 1;
        final int height = (int) Math.ceil(rectangle.getHeight() * scaleY) + 1;
        return new Rectangle(x, y, width, height);
    }
}
