/*
 * $Id: PinLayer.java,v 1.1 2006/10/10 14:47:37 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.ui.product;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.PropertyMap;

import com.bc.layer.AbstractLayer;
import com.bc.view.ViewModel;

public class PinLayer extends AbstractLayer {

    private Product _product;

    private boolean _textEnabled;
    private Font _textFont;
    private Color _textFgColor;
    private Color _textBgColor;
    private float _textBgTransparency;

    public PinLayer(Product product) {
        _product = product;
        _textFont = new Font("Helvetica", Font.BOLD, 14);
        _textEnabled = true;
        _textFgColor = Color.white;
        _textBgColor = Color.black;
        _textBgTransparency = 0.7f;
    }

    /**
     * Sets multiple graticule display properties.
     */
    public void setProperties(PropertyMap propertyMap) {

        _textEnabled = propertyMap.getPropertyBool("pin.text.enabled", _textEnabled);
        _textFgColor = propertyMap.getPropertyColor("pin.text.fg.color", _textFgColor);
        _textBgColor = propertyMap.getPropertyColor("pin.text.bg.color", _textBgColor);
        _textBgTransparency = (float) propertyMap.getPropertyDouble("pin.text.bg.transparency", _textBgTransparency);

        fireLayerChanged();
    }

    public void draw(Graphics2D g2d, ViewModel viewModel) {
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldTextAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double viewScale = viewModel.getViewScale();
        Pin[] pins = _product.getPins();
        for (Pin pin : pins) {
            final PixelPos pixelPos = pin.getPixelPos();
            if (pixelPos != null) {
                g2d.translate(pixelPos.getX(), pixelPos.getY());
                g2d.scale(1.0/viewScale, 1.0/viewScale);

                if (pin.isSelected()) {
                    pin.getSymbol().drawSelected(g2d);
                } else {
                    pin.getSymbol().draw(g2d);
                }

                if (_textEnabled) {
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

        g2d.setFont(_textFont);

        GlyphVector glyphVector = _textFont.createGlyphVector(g2d.getFontRenderContext(), label);
        Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
        float tx = (float) (logicalBounds.getX() - 0.5 * logicalBounds.getWidth());
        float ty = (float) (1.0 + logicalBounds.getHeight());
        Shape outline = glyphVector.getOutline(tx, ty);

        int[] alphas = new int[] {64, 128, 192, 255};
        for (int i = 0; i < alphas.length; i++) {
            BasicStroke selectionStroke = new BasicStroke((float)(alphas.length - i));
            Color selectionColor = new Color(_textBgColor.getRed(),
                                             _textBgColor.getGreen(),
                                             _textBgColor.getGreen(),
                                             alphas[i]);
            g2d.setStroke(selectionStroke);
            g2d.setPaint(selectionColor);
            g2d.draw(outline);
        }

        g2d.setPaint(_textFgColor);
        g2d.fill(outline);
    }
}
