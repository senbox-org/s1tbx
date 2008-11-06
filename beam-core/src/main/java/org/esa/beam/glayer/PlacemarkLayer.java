package org.esa.beam.glayer;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.*;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class PlacemarkLayer extends Layer {

    public static final String PROPERTY_NAME_TEXT_FONT = "text.font";
    public static final String PROPERTY_NAME_TEXT_ENABLED = "text.enabled";
    public static final String PROPERTY_NAME_TEXT_FG_COLOR = "text.fg.color";
    public static final String PROPERTY_NAME_TEXT_BG_COLOR = "text.bg.color";

    private static final boolean DEFAULT_TEXT_ENABLED = false;
    private static final Font DEFAULT_TEXT_FONT = new Font("Helvetica", Font.BOLD, 14);
    private static final Color DEFAULT_TEXT_FG_COLOR = Color.WHITE;
    private static final Color DEFAULT_TEXT_BG_COLOR = Color.BLACK;

    private Product product;
    private PlacemarkDescriptor placemarkDescriptor;
    private final AffineTransform imageToModelTransform;

    public PlacemarkLayer(Product product, PlacemarkDescriptor placemarkDescriptor, AffineTransform imageToModelTransform) {
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        this.imageToModelTransform = new AffineTransform(imageToModelTransform);
    }

    @Override
    public void disposeLayer() {
        product = null;
        placemarkDescriptor = null;
    }

    protected ProductNodeGroup<Pin> getPlacemarkGroup() {
        return placemarkDescriptor.getPlacemarkGroup(getProduct());
    }

    public Product getProduct() {
        return product;
    }

    public AffineTransform getImageToModelTransform() {
        return new AffineTransform(imageToModelTransform);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        Graphics2D g2d = rendering.getGraphics();
        Viewport viewport = rendering.getViewport();
        AffineTransform oldTransform = g2d.getTransform();
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldTextAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final AffineTransform transform = new AffineTransform();
            transform.concatenate(oldTransform);
            transform.concatenate(viewport.getModelToViewTransform());
            transform.concatenate(imageToModelTransform);
            g2d.setTransform(transform);

            ProductNodeGroup<Pin> pinGroup = getPlacemarkGroup();
            Pin[] pins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
            for (final Pin pin : pins) {
                final PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos != null) {
                    g2d.translate(pixelPos.getX(), pixelPos.getY());
                    final double scale = Math.sqrt(Math.abs(g2d.getTransform().getDeterminant()));
                    g2d.scale(1 / scale, 1 / scale);
                    g2d.rotate(viewport.getOrientation());

                    if (pin.isSelected()) {
                        pin.getSymbol().drawSelected(g2d);
                    } else {
                        pin.getSymbol().draw(g2d);
                    }

                    if (isTextEnabled()) {
                        drawTextLabel(g2d, pin);
                    }

                    g2d.rotate(-viewport.getOrientation());
                    g2d.scale(scale, scale);
                    g2d.translate(-pixelPos.getX(), -pixelPos.getY());
                }
            }
        } finally {
            g2d.setTransform(oldTransform);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldTextAntialiasing);
        }
    }

    private void drawTextLabel(Graphics2D g2d, Pin pin) {

        final String label = pin.getLabel();

        g2d.setFont(getTextFont());

        GlyphVector glyphVector = getTextFont().createGlyphVector(g2d.getFontRenderContext(), label);
        Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
        float tx = (float) (logicalBounds.getX() - 0.5 * logicalBounds.getWidth());
        float ty = (float) (1.0 + logicalBounds.getHeight());
        Shape outline = glyphVector.getOutline(tx, ty);

        int[] alphas = new int[]{64, 128, 192, 255};
        for (int i = 0; i < alphas.length; i++) {
            BasicStroke selectionStroke = new BasicStroke((alphas.length - i));
            Color selectionColor = new Color(getTextBgColor().getRed(),
                    getTextBgColor().getGreen(),
                    getTextBgColor().getGreen(),
                    alphas[i]);
            g2d.setStroke(selectionStroke);
            g2d.setPaint(selectionColor);
            g2d.draw(outline);
        }

        g2d.setPaint(getTextFgColor());
        g2d.fill(outline);
    }

    private boolean isTextEnabled() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_ENABLED)) {
            return (Boolean) style.getProperty(PROPERTY_NAME_TEXT_ENABLED);
        }

        return DEFAULT_TEXT_ENABLED;
    }

    public void setTextEnabled(boolean textEnabled) {
        getStyle().setProperty(PROPERTY_NAME_TEXT_ENABLED, textEnabled);
    }

    public Font getTextFont() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_FONT)) {
            return (Font) style.getProperty(PROPERTY_NAME_TEXT_FONT);
        }

        return DEFAULT_TEXT_FONT;
    }

    public void setTextFont(Font font) {
        getStyle().setProperty(PROPERTY_NAME_TEXT_FONT, font);
    }

    public Color getTextFgColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_FG_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_TEXT_FG_COLOR);
        }

        return DEFAULT_TEXT_FG_COLOR;
    }

    public void setTextFgColor(Color color) {
        getStyle().setProperty(PROPERTY_NAME_TEXT_FG_COLOR, color);
    }

    public Color getTextBgColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_BG_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_TEXT_BG_COLOR);
        }

        return DEFAULT_TEXT_BG_COLOR;
    }

    public void setTextBgColor(Color color) {
        getStyle().setProperty(PROPERTY_NAME_TEXT_BG_COLOR, color);
    }
}
