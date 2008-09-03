package com.bc.ceres.glayer.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glevel.LayerImage;
import com.bc.ceres.glevel.LayerImageRenderer;
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

    private LayerImage layerImage;
    private ConcurrentLayerImageRenderer concurrentRenderer;
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
        this(new DefaultLayerImage(image, imageToModelTransform, levelCount,
                                        Interpolation.getInstance(Interpolation.INTERP_BICUBIC)));
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param layerImage the multi-resolution-level image
     */
    public ImageLayer(LayerImage layerImage) {
        Assert.notNull(layerImage);
        this.layerImage = layerImage;
    }

    @Override
    public void regenerate() {
        clearCaches();
        fireLayerDataChanged(getBounds());
    }

    public RenderedImage getImage() {
        return getImage(0);
    }

    public LayerImage getLayerImage() {
        return layerImage;
    }

    public void setLayerImage(LayerImage layerImage) {
        Assert.notNull(layerImage);
        if (layerImage != this.layerImage) {
            final Rectangle2D region = this.layerImage.getModelBounds().createUnion(layerImage.getModelBounds());
            clearCaches();
            this.layerImage = layerImage;
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
        return layerImage.getLevelImage(level);
    }

    public AffineTransform getImageToModelTransform(int level) {
        return layerImage.getImageToModelTransform(level);
    }

    public AffineTransform getModelToImageTransform(int level) {
        return layerImage.getModelToImageTransform(level);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public Rectangle2D getBounds() {
        return layerImage.getModelBounds();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (layerImage == LayerImage.NULL) {
            return;
        }
        final Viewport vp = rendering.getViewport();
        final double i2mScale = DefaultViewport.getScale(getImageToModelTransform());
        final double m2vScale = 1.0 / vp.getZoomFactor();
        final double scale = m2vScale / i2mScale;
        final int currentLevel = layerImage.computeLevel(scale);
        final LayerImageRenderer renderer = getRenderer(rendering);
        renderer.renderImage(rendering, layerImage, currentLevel);
    }

    @Override
    public void dispose() {
        resetRenderer();
        if (layerImage != null) {
            layerImage.reset();
            layerImage = null;
        }
        super.dispose();
    }

    private LayerImageRenderer getRenderer(Rendering rendering) {
        if (rendering instanceof InteractiveRendering) {
            if (concurrentRenderer == null) {
                concurrentRenderer = new ConcurrentLayerImageRenderer();
            }
            return concurrentRenderer;
        } else {
            return new DefaultLayerImageRenderer();
        }
    }

    private void resetRenderer() {
        if (concurrentRenderer != null) {
            concurrentRenderer.reset();
        }
    }

    private void clearCaches() {
        resetRenderer();
        layerImage.reset();
    }

}