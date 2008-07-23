package org.esa.beam.glayer;

import com.bc.ceres.glayer.AbstractGraphicalLayer;
import com.bc.ceres.grendering.Viewport;
import com.bc.ceres.grendering.Rendering;
import org.esa.beam.framework.datamodel.*;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class PlacemarkLayer extends AbstractGraphicalLayer {

    private Product product;
    private PlacemarkDescriptor placemarkDescriptor;
    private final AffineTransform imageToModelTransform;

    private boolean textEnabled;
    private Font textFont;
    private Color textFgColor;
    private Color textBgColor;
    private float textBgTransparency;


    public PlacemarkLayer(Product product, PlacemarkDescriptor placemarkDescriptor, AffineTransform imageToModelTransform) {
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        this.imageToModelTransform = new AffineTransform(imageToModelTransform);
        setTextFont(new Font("Helvetica", Font.BOLD, 14));
        setTextEnabled(false);
        setTextFgColor(Color.white);
        setTextBgColor(Color.black);
        setTextBgTransparency(0.7f);
    }

    @Override
    public void dispose() {
        product = null;
        placemarkDescriptor = null;
        super.dispose();
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
            transform.concatenate(viewport.getModelToViewTransform());
            transform.concatenate(imageToModelTransform);
            g2d.setTransform(transform);

            double zoomFactor = viewport.getZoomFactor();
            ProductNodeGroup<Pin> pinGroup = getPlacemarkGroup();
            Pin[] pins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
            for (Pin pin : pins) {
                final PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos != null) {
                    g2d.translate(pixelPos.getX(), pixelPos.getY());
                    g2d.scale(1.0 / zoomFactor, 1.0 / zoomFactor);

                    if (pin.isSelected()) {
                        pin.getSymbol().drawSelected(g2d);
                    } else {
                        pin.getSymbol().draw(g2d);
                    }

                    if (isTextEnabled()) {
                        drawTextLabel(g2d, pin);
                    }

                    g2d.scale(zoomFactor, zoomFactor);
                    g2d.translate(-pixelPos.getX(), -pixelPos.getY());
                }
            }
        } finally {
            g2d.setTransform(oldTransform);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldTextAntialiasing);
        }

    }

    public Rectangle2D getBoundingBox() {
        return new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
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
            BasicStroke selectionStroke = new BasicStroke((float) (alphas.length - i));
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

    public boolean isTextEnabled() {
        return textEnabled;
    }

    public void setTextEnabled(boolean textEnabled) {
        this.textEnabled = textEnabled;
    }

    public Font getTextFont() {
        return textFont;
    }

    public void setTextFont(Font textFont) {
        this.textFont = textFont;
    }

    public Color getTextFgColor() {
        return textFgColor;
    }

    public void setTextFgColor(Color textFgColor) {
        this.textFgColor = textFgColor;
    }

    public Color getTextBgColor() {
        return textBgColor;
    }

    public void setTextBgColor(Color textBgColor) {
        this.textBgColor = textBgColor;
    }

    public float getTextBgTransparency() {
        return textBgTransparency;
    }

    public void setTextBgTransparency(float textBgTransparency) {
        this.textBgTransparency = textBgTransparency;
    }

}
