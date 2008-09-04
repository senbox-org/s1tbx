package com.bc.ceres.glayer.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.*;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * A multi-resolution capable image layer.
 *
 * @author Norman Fomferra
 */
public class ImageLayer extends Layer {

    private MultiLevelSource multiLevelSource;
    private ConcurrentMultiLevelRenderer concurrentRenderer;
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
        this(new DefaultMultiLevelSource(new DefaultMultiLevelModel(levelCount,
                                                                    imageToModelTransform,
                                                                    DefaultMultiLevelModel.getModelBounds(imageToModelTransform, image)) ,
                                                                    image));
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param multiLevelSource the multi-resolution-level image
     */
    public ImageLayer(MultiLevelSource multiLevelSource) {
        Assert.notNull(multiLevelSource);
        this.multiLevelSource = multiLevelSource;
    }

    @Override
    public void regenerate() {
        clearCaches();
        fireLayerDataChanged(getBounds());
    }

    public RenderedImage getImage() {
        return getImage(0);
    }

    public MultiLevelSource getMultiLevelSource() {
        return multiLevelSource;
    }

    public void setMultiLevelSource(MultiLevelSource multiLevelSource) {
        Assert.notNull(multiLevelSource);
        if (multiLevelSource != this.multiLevelSource) {
            final Rectangle2D oldBounds = this.multiLevelSource.getModel().getModelBounds();
            final Rectangle2D newBounds = multiLevelSource.getModel().getModelBounds();
            final Rectangle2D region = oldBounds.createUnion(newBounds);
            clearCaches();
            this.multiLevelSource = multiLevelSource;
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
        return multiLevelSource.getLevelImage(level);
    }

    public AffineTransform getImageToModelTransform(int level) {
        return multiLevelSource.getModel().getImageToModelTransform(level);
    }

    public AffineTransform getModelToImageTransform(int level) {
        return multiLevelSource.getModel().getModelToImageTransform(level);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public Rectangle2D getBounds() {
        return multiLevelSource.getModel().getModelBounds();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (multiLevelSource == MultiLevelSource.NULL) {
            return;
        }
        final Viewport vp = rendering.getViewport();
        final double i2mScale = DefaultViewport.getScale(getImageToModelTransform());
        final double m2vScale = 1.0 / vp.getZoomFactor();
        final double scale = m2vScale / i2mScale;
        final int currentLevel = multiLevelSource.getModel().getLevel(scale);
        final MultiLevelRenderer renderer = getRenderer(rendering);
        renderer.renderImage(rendering, multiLevelSource, currentLevel);
    }

    @Override
    public synchronized void dispose() {
        resetRenderer();
        if (multiLevelSource != null) {
            multiLevelSource.reset();
            multiLevelSource = null;
        }
        super.dispose();
    }

    private synchronized MultiLevelRenderer getRenderer(Rendering rendering) {
        if (rendering instanceof InteractiveRendering) {
            if (concurrentRenderer == null) {
                concurrentRenderer = new ConcurrentMultiLevelRenderer();
            }
            return concurrentRenderer;
        } else {
            return new DefaultMultiLevelRenderer();
        }
    }

    private void resetRenderer() {
        if (concurrentRenderer != null) {
            concurrentRenderer.reset();
        }
    }

    private void clearCaches() {
        resetRenderer();
        multiLevelSource.reset();
    }

}