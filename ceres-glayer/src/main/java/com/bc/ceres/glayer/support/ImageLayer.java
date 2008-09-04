package com.bc.ceres.glayer.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glevel.ImageLayerModel;
import com.bc.ceres.glevel.ImageLayerModelRenderer;
import com.bc.ceres.glevel.support.*;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

import javax.media.jai.Interpolation;

/**
 * A multi-resolution capable image layer.
 *
 * @author Norman Fomferra
 */
public class ImageLayer extends Layer {

    private ImageLayerModel imageLayerModel;
    private ConcurrentImageLayerModelRenderer concurrentRenderer;
    private boolean debug;

    /**
     * Constructs a single-resolution-level image layer.
     *
     * @param image the image
     */
    public ImageLayer(RenderedImage image) {
        this(image, new AffineTransform());
    }

    /**
     * Constructs a single-resolution-level image layer.
     *
     * @param image                 the image
     * @param imageToModelTransform the transformation from image to model CS
     */
    public ImageLayer(RenderedImage image, AffineTransform imageToModelTransform) {
        this(image, imageToModelTransform, 1);
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param image                 the image
     * @param imageToModelTransform the transformation from image to model CS
     * @param levelCount            the number of resolution levels
     */
    public ImageLayer(RenderedImage image, AffineTransform imageToModelTransform, int levelCount) {
        this(new DefaultImageLayerModel(new DefaultLevelImageSource(image, levelCount, Interpolation.getInstance(Interpolation.INTERP_BICUBIC)),
                                        imageToModelTransform,
                                        DefaultImageLayerModel.getModelBounds(imageToModelTransform, image)));
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param imageLayerModel the multi-resolution-level image
     */
    public ImageLayer(ImageLayerModel imageLayerModel) {
        Assert.notNull(imageLayerModel);
        this.imageLayerModel = imageLayerModel;
    }

    @Override
    public void regenerate() {
        clearCaches();
        fireLayerDataChanged(getBounds());
    }

    public RenderedImage getImage() {
        return getImage(0);
    }

    public ImageLayerModel getLayerImage() {
        return imageLayerModel;
    }

    public void setLayerImage(ImageLayerModel imageLayerModel) {
        Assert.notNull(imageLayerModel);
        if (imageLayerModel != this.imageLayerModel) {
            final Rectangle2D region = this.imageLayerModel.getModelBounds().createUnion(imageLayerModel.getModelBounds());
            clearCaches();
            this.imageLayerModel = imageLayerModel;
            concurrentRenderer = null;
            fireLayerDataChanged(region);
        }
    }

    public AffineTransform getImageToModelTransform() {
        return getImageToModelTransform(0);
    }

    public AffineTransform getModelToImageTransform() {
        return getModelToImageTransform(0);
    }

    public RenderedImage getImage(int level) {
        return imageLayerModel.getLevelImageSource().getLevelImage(level);
    }

    public AffineTransform getImageToModelTransform(int level) {
        return imageLayerModel.getImageToModelTransform(level);
    }

    public AffineTransform getModelToImageTransform(int level) {
        return imageLayerModel.getModelToImageTransform(level);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public Rectangle2D getBounds() {
        return imageLayerModel.getModelBounds();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (imageLayerModel == ImageLayerModel.NULL) {
            return;
        }
        final Viewport vp = rendering.getViewport();
        final double i2mScale = DefaultViewport.getScale(getImageToModelTransform());
        final double m2vScale = 1.0 / vp.getZoomFactor();
        final double scale = m2vScale / i2mScale;
        final int currentLevel = imageLayerModel.getLevelImageSource().computeLevel(scale);
        final ImageLayerModelRenderer renderer = getRenderer(rendering);
        renderer.renderImage(rendering, imageLayerModel, currentLevel);
    }

    @Override
    public synchronized void dispose() {
        resetRenderer();
        if (imageLayerModel != null) {
            imageLayerModel.getLevelImageSource().reset();
            imageLayerModel = null;
        }
        super.dispose();
    }

    private synchronized ImageLayerModelRenderer getRenderer(Rendering rendering) {
        if (rendering instanceof InteractiveRendering) {
            if (concurrentRenderer == null) {
                concurrentRenderer = new ConcurrentImageLayerModelRenderer();
            }
            return concurrentRenderer;
        } else {
            return new DefaultImageLayerModelRenderer();
        }
    }

    private void resetRenderer() {
        if (concurrentRenderer != null) {
            concurrentRenderer.reset();
        }
    }

    private void clearCaches() {
        resetRenderer();
        imageLayerModel.getLevelImageSource().reset();
    }

}