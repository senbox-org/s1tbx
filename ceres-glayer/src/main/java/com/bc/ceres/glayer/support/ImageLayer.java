package com.bc.ceres.glayer.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glevel.LevelImage;
import com.bc.ceres.glevel.LevelImageRenderer;
import com.bc.ceres.glevel.support.*;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.logging.Level;

/**
 * A multi-resolution capable image layer.
 *
 * @author Norman Fomferra
 */
public class ImageLayer extends Layer {

    private LevelImage levelImage;
    private ConcurrentLevelImageRenderer concurrentRenderer;
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
        this(levelCount > 1 ? new DefaultMultiLevelImage(image, imageToModelTransform, levelCount) : new SingleLevelImage(image, imageToModelTransform));
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param levelImage the multi-resolution-level image
     */
    public ImageLayer(LevelImage levelImage) {
        Assert.notNull(levelImage);
        this.levelImage = levelImage;
    }

    @Override
    public void regenerate() {
        clearCaches();
        fireLayerDataChanged(getBounds());
    }

    public int getLevelCount() {
        return levelImage.getLevelCount();
    }

    public RenderedImage getImage() {
        return getImage(0);
    }

    public LevelImage getLevelImage() {
        return levelImage;
    }

    public void setLevelImage(LevelImage levelImage) {
        Assert.notNull(levelImage);
        if (levelImage != this.levelImage) {
            final Rectangle2D region = this.levelImage.getBounds(0).createUnion(levelImage.getBounds(0));
            clearCaches();
            this.levelImage = levelImage;
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
        return levelImage.getPlanarImage(level);
    }

    public AffineTransform getImageToModelTransform(int level) {
        return levelImage.getImageToModelTransform(level);
    }

    public AffineTransform getModelToImageTransform(int level) {
        return levelImage.getModelToImageTransform(level);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Rectangle2D getBounds() {
        return levelImage.getBounds(0);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (levelImage == LevelImage.NULL) {
            return;
        }
        final Viewport vp = rendering.getViewport();
        final double i2mScale = DefaultViewport.getScale(getImageToModelTransform());
        final double m2vScale = 1.0 / vp.getZoomFactor();
        final double scale = m2vScale / i2mScale;
        final int currentLevel = levelImage.computeLevel(scale);
        final LevelImageRenderer renderer = getRenderer(rendering);
        renderer.renderImage(rendering, levelImage, currentLevel);
    }

    public void dispose() {
        resetRenderer();
        if (levelImage != null) {
            levelImage.reset();
            levelImage = null;
        }
        super.dispose();
    }

    private LevelImageRenderer getRenderer(Rendering rendering) {
        if (rendering instanceof InteractiveRendering) {
            if (concurrentRenderer == null) {
                concurrentRenderer = new ConcurrentLevelImageRenderer();
            }
            return concurrentRenderer;
        } else {
            return new DefaultLevelImageRenderer();
        }
    }

    private void resetRenderer() {
        if (concurrentRenderer != null) {
            concurrentRenderer.reset();
        }
    }

    private void clearCaches() {
        resetRenderer();
        levelImage.reset();
    }

}