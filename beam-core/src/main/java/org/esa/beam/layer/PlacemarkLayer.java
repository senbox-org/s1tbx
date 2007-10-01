package org.esa.beam.layer;

import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.PropertyMap;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * A layer for placemarks.
 *
 * @author Marco Peters
 * @since 4.1
 */
public class PlacemarkLayer extends StyledLayer {

    private Product product;
    private PlacemarkDescriptor placemarkDescriptor;

    private boolean textEnabled;
    private Font textFont;
    private Color textFgColor;
    private Color textBgColor;
    private float textBgTransparency;


    public PlacemarkLayer(Product product, PlacemarkDescriptor placemarkDescriptor) {
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        setTextFont(new Font("Helvetica", Font.BOLD, 14));
        setTextEnabled(false);
        setTextFgColor(Color.white);
        setTextBgColor(Color.black);
        setTextBgTransparency(0.7f);
    }

    @Override
    protected void setStylePropertiesImpl(PropertyMap propertyMap) {
        super.setStylePropertiesImpl(propertyMap);
        setTextEnabled(propertyMap.getPropertyBool(getPropertyName("text.enabled"), isTextEnabled()));
        setTextFgColor(propertyMap.getPropertyColor(getPropertyName("text.fg.color"), textFgColor));
        setTextBgColor(propertyMap.getPropertyColor(getPropertyName("text.bg.color"), getTextBgColor()));
        setTextBgTransparency((float) propertyMap.getPropertyDouble(getPropertyName("text.bg.transparency"), textBgTransparency));
    }

    @Override
    public String getPropertyNamePrefix() {
        return placemarkDescriptor.getRoleName();
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

    public void draw(Graphics2D g2d, ViewModel viewModel) {
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldTextAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double viewScale = viewModel.getViewScale();
        ProductNodeGroup<Pin> pinGroup = getPlacemarkGroup();
        Pin[] pins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
        for (Pin pin : pins) {
            final PixelPos pixelPos = pin.getPixelPos();
            if (pixelPos != null) {
                g2d.translate(pixelPos.getX(), pixelPos.getY());
                g2d.scale(1.0 / viewScale, 1.0 / viewScale);

                if (pin.isSelected()) {
                    pin.getSymbol().drawSelected(g2d);
                } else {
                    pin.getSymbol().draw(g2d);
                }

                if (isTextEnabled()) {
                    drawTextLabel(g2d, pin);
                }

                g2d.scale(viewScale, viewScale);
                g2d.translate(-pixelPos.getX(), -pixelPos.getY());
            }
        }
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldTextAntialiasing);
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
