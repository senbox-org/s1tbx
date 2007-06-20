/*
 * $Id: PinLayer.java,v 1.1 2006/10/10 14:47:37 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.ui.product;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.PropertyMap;

import com.bc.layer.AbstractLayer;

public class PinLayer extends AbstractLayer {

    private Product _product;

    private boolean _textEnabled;
    private Font _textFont;
    private Color _textFgColor;
    private Color _textBgColor;
    private float _textBgTransparency;

    public PinLayer(Product product) {
        _product = product;
        _textFont = new Font("SansSerif", Font.PLAIN, 12);
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

    public void draw(Graphics2D g2d) {
        for (int i = 0; i < _product.getNumPins(); i++) {
            final Pin pin = _product.getPinAt(i);
            final PixelPos pixelPos = pin.getPixelPos();
            if (pixelPos != null) {
                g2d.translate(pixelPos.getX(), pixelPos.getY());
                if (pin.isSelected()) {
                    pin.getSymbol().drawSelected(g2d);
                } else {
                    pin.getSymbol().draw(g2d);
                }

                if (_textEnabled) {
                    drawTextLabel(g2d, pin);
                }

                g2d.translate(-pixelPos.getX(), -pixelPos.getY());
            }
        }
    }

    private void drawTextLabel(Graphics2D g2d, Pin pin) {

        final String label = pin.getLabel();

        final Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(label, g2d);

        final double width = labelBounds.getWidth() * 1.1;
        final double height = labelBounds.getHeight();
        final Rectangle shapeBounds = pin.getSymbol().getShape().getBounds();
        final double x = shapeBounds.getX() + 0.5 * (shapeBounds.getWidth() - width);
        final double y = shapeBounds.getY() + 4;

        final Rectangle2D r = new Rectangle2D.Double(x, y, width, height);
        final float tx =  0.5f * (float)(shapeBounds.getWidth() - labelBounds.getWidth());
        final float ty =  0.5f * (float)(r .getY() + r.getHeight() + labelBounds.getHeight() - 2);

        if (_textBgTransparency < 1.0f) {
            Composite oldComposite = null;
            if (_textBgTransparency > 0.0f) {
                oldComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - _textBgTransparency));
            }

            g2d.setPaint(_textBgColor);
            g2d.setStroke(new BasicStroke(0));
            g2d.fill(r);

            if (oldComposite != null) {
                g2d.setComposite(oldComposite);
            }
        }

        g2d.setFont(_textFont);
        g2d.setPaint(_textFgColor);
        g2d.drawString(label, tx, ty);
    }
}
