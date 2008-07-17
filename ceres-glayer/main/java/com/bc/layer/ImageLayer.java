package com.bc.layer;

import com.bc.layer.level.DefaultMultiLevelImage;
import com.bc.layer.level.LevelImage;
import com.bc.layer.level.SingleLevelImage;
import com.bc.layer.painter.ConcurrentImagePainter;
import com.bc.layer.painter.DefaultImagePainter;
import com.bc.layer.painter.ImagePainter;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * A multi-resolution capable image layer.
 *
 * @author Norman Fomferra
 */
public class ImageLayer extends AbstractGraphicalLayer {

    private LevelImage levelImage;
    private ImagePainter imagePainter;
    private boolean concurrent;
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
     * @param image                the image
     * @param imageToModelTransform the transformation from image to model CS
     */
    public ImageLayer(RenderedImage image, AffineTransform imageToModelTransform) {
        this(image, imageToModelTransform, 1);
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param image                the image
     * @param imageToModelTransform the transformation from image to model CS
     * @param levelCount           the number of resolution levels
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
        this.levelImage = levelImage;
    }

    public int getLevelCount() {
        return levelImage.getLevelCount();
    }

    public RenderedImage getImage() {
        return getImage(0);
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

    public boolean isConcurrent() {
        return concurrent;
    }

    public void setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
        if (concurrent && imagePainter != null && !(imagePainter instanceof ConcurrentImagePainter)) {
            imagePainter.dispose();
            imagePainter = null;
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Rectangle2D getBoundingBox() {
        return levelImage.getBoundingBox(0);
    }

    @Override
    protected void paintLayer(Graphics2D g, Viewport vp) {
        final double i2mScale = Viewport.getScale(getImageToModelTransform());
        final double m2vScale = vp.getModelScale();
        final double scale = m2vScale / i2mScale;

        final int currentLevel = levelImage.computeLevel(scale);
        if (imagePainter == null) {
            imagePainter = createImagePainter();
        }
        imagePainter.paint(g, vp, levelImage, currentLevel);
    }

    public void dispose() {
        if (imagePainter != null) {
            imagePainter.dispose();
            imagePainter = null;
        }
        levelImage = null;
        super.dispose();
    }

    private ImagePainter createImagePainter() {
        if (concurrent) {
            final ConcurrentImagePainter concurrentImagePainter = new ConcurrentImagePainter();
            concurrentImagePainter.setDebug(debug);
            return concurrentImagePainter;
        }
        return new DefaultImagePainter();
    }

}