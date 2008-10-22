package com.bc.ceres.glayer.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.ConcurrentMultiLevelRenderer;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelRenderer;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * A multi-resolution capable image layer.
 *
 * @author Norman Fomferra
 */
public class ImageLayer extends Layer {

    public static final String PROPERTY_NAME_BORDER_SHOWN = "border.shown";
    public static final String PROPERTY_NAME_BORDER_WIDTH = "border.width";
    public static final String PROPERTY_NAME_BORDER_COLOR = "border.color";

    public static final boolean DEFAULT_BORDER_SHOWN = true;
    public static final double DEFAULT_BORDER_WIDTH = 2.0;
    public static final Color DEFAULT_BORDER_COLOR = new Color(204, 204, 255);

    private MultiLevelSource multiLevelSource;
    private MultiLevelRenderer concurrentRenderer;
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
        this(new DefaultMultiLevelSource(image,
                new DefaultMultiLevelModel(levelCount, imageToModelTransform,
                        DefaultMultiLevelModel.getModelBounds(imageToModelTransform, image))
        ));
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
        fireLayerDataChanged(getModelBounds());
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
        return multiLevelSource.getImage(level);
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

    public int getLevel(Viewport vp) {
        final double i2mScale = Math.sqrt(getImageToModelTransform().getDeterminant());
        final double m2vScale = 1.0 / vp.getZoomFactor();
        final double scale = m2vScale / i2mScale;
        return multiLevelSource.getModel().getLevel(scale);
    }

    @Override
    public Rectangle2D getModelBounds() {
        return multiLevelSource.getModel().getModelBounds();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (multiLevelSource == MultiLevelSource.NULL) {
            return;
        }
        final Viewport vp = rendering.getViewport();
        final int level = getLevel(vp);
        final MultiLevelRenderer renderer = getRenderer(rendering);
        renderer.renderImage(rendering, multiLevelSource, level);

        if (isBorderShown()) {
            renderImageBorder(rendering, level);
        }
    }

    private void renderImageBorder(Rendering rendering, int level) {
        final Graphics2D graphics2D = rendering.getGraphics();
        final Viewport viewport = rendering.getViewport();
        final double borderWidth = Math.min(1.0, getBorderWidth() * viewport.getZoomFactor());
        final Color borderColor = getBorderColor();

        graphics2D.setStroke(new BasicStroke((float) borderWidth));
        graphics2D.setColor(borderColor);

        final Object oldAntialiasing = graphics2D.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        final AffineTransform oldTransform = graphics2D.getTransform();

        try {
            final AffineTransform modelToImageTransform = multiLevelSource.getModel().getModelToImageTransform(level);
            final AffineTransform imageToModelTransform = multiLevelSource.getModel().getImageToModelTransform(level);
            final AffineTransform transform = new AffineTransform();
            transform.concatenate(oldTransform);
            transform.concatenate(viewport.getModelToViewTransform());
            transform.concatenate(imageToModelTransform);

            graphics2D.setTransform(transform);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Rectangle2D modelBounds = multiLevelSource.getModel().getModelBounds();
            final Rectangle2D imageBounds = modelToImageTransform.createTransformedShape(modelBounds).getBounds2D();

            final double x = imageBounds.getX() - borderWidth / 2.0;
            final double y = imageBounds.getY() - borderWidth / 2.0;
            final double w = imageBounds.getWidth() + borderWidth;
            final double h = imageBounds.getHeight() + borderWidth;
            final Rectangle2D border = new Rectangle.Double(x, y, w, h);

            graphics2D.draw(border);
        } finally {
            graphics2D.setTransform(oldTransform);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        }
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

    public boolean isBorderShown() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_BORDER_SHOWN)) {
            return (Boolean) style.getProperty(PROPERTY_NAME_BORDER_SHOWN);
        }
        return DEFAULT_BORDER_SHOWN;
    }

    public double getBorderWidth() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_BORDER_WIDTH)) {
            return (Double) style.getProperty(PROPERTY_NAME_BORDER_WIDTH);
        }
        return DEFAULT_BORDER_WIDTH;
    }

    public Color getBorderColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_BORDER_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_BORDER_COLOR);
        }
        return DEFAULT_BORDER_COLOR;
    }
}