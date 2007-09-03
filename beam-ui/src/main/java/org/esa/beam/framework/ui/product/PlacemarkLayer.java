package org.esa.beam.framework.ui.product;

import com.bc.layer.AbstractLayer;
import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.PlacemarkDescriptor;
import org.esa.beam.util.PropertyMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class PlacemarkLayer extends AbstractLayer {

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

    /**
     * Sets multiple graticule display properties.
     */
    public void setProperties(PropertyMap propertyMap) {

        setTextEnabled(propertyMap.getPropertyBool(getPropertyNamePrefix() + ".text.enabled", isTextEnabled()));
        setTextFgColor(propertyMap.getPropertyColor(getPropertyNamePrefix() + ".text.fg.color", textFgColor));
        setTextBgColor(propertyMap.getPropertyColor(getPropertyNamePrefix() + ".text.bg.color", getTextBgColor()));
        setTextBgTransparency((float) propertyMap.getPropertyDouble(getPropertyNamePrefix() + ".text.bg.transparency", textBgTransparency));

        fireLayerChanged();
    }

    private String getPropertyNamePrefix() {
        return placemarkDescriptor.getRoleName();
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

        int[] alphas = new int[] {64, 128, 192, 255};
        for (int i = 0; i < alphas.length; i++) {
            BasicStroke selectionStroke = new BasicStroke((float)(alphas.length - i));
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
