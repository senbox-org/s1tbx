/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.glayer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import org.esa.beam.framework.datamodel.Graticule;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class GraticuleLayer extends Layer {

    private Product _product;
    private RasterDataNode _raster;
    private ProductNodeHandler _productNodeHandler;

    private Graticule _graticule;
    private boolean _resAuto;
    private int _numPixels;
    private float _lineResLat;
    private float _lineResLon;

    // TODO: IMAGING 4.5: Layer.getStyle(), SVG property names!
    private Color _lineColor;
    private float _lineWidth;
    private float _lineTransparency;
    private boolean _textEnabled;
    private Font _textFont;
    private Color _textFgColor;
    private Color _textBgColor;
    private float _textBgTransparency;

    public GraticuleLayer(Product product, RasterDataNode raster) {
        Guardian.assertNotNull("product", product);

        _productNodeHandler = new ProductNodeHandler();

        _product = product;
        _product.addProductNodeListener(_productNodeHandler);

        _raster = raster;

        _resAuto = true;
        _numPixels = 128;
        _lineResLat = 1.0f;
        _lineResLon = 1.0f;

        _lineColor = new Color(204, 204, 255);
        _lineWidth = 0.5f;
        _lineTransparency = 0.0f;

        _textFont = new Font("SansSerif", Font.ITALIC, 12);
        _textEnabled = true;
        _textFgColor = Color.white;
        _textBgColor = Color.black;
        _textBgTransparency = 0.7f;

    }

    public void setStyleProperties(PropertyMap propertyMap) {
        boolean oldResAuto = _resAuto;
        float oldNumPixels = _numPixels;
        float oldLineResLat = _lineResLat;
        float oldLineResLon = _lineResLon;

        _resAuto = propertyMap.getPropertyBool("graticule.res.auto", _resAuto);
        _numPixels = propertyMap.getPropertyInt("graticule.res.pixels", _numPixels);
        _lineResLat = (float) propertyMap.getPropertyDouble("graticule.res.lat", _lineResLat);
        _lineResLon = (float) propertyMap.getPropertyDouble("graticule.res.lon", _lineResLon);

        _lineWidth = (float) propertyMap.getPropertyDouble("graticule.line.width", _lineWidth);
        _lineColor = propertyMap.getPropertyColor("graticule.line.color", _lineColor);
        _lineTransparency = (float) propertyMap.getPropertyDouble("graticule.line.transparency", _lineTransparency);

        _textEnabled = propertyMap.getPropertyBool("graticule.text.enabled", _textEnabled);
        _textFgColor = propertyMap.getPropertyColor("graticule.text.fg.color", _textFgColor);
        _textBgColor = propertyMap.getPropertyColor("graticule.text.bg.color", _textBgColor);
        _textBgTransparency = (float) propertyMap.getPropertyDouble("graticule.text.bg.transparency",
                                                                    _textBgTransparency);

        if (oldResAuto != _resAuto ||
                oldNumPixels != _numPixels ||
                oldLineResLat != _lineResLat ||
                oldLineResLon != _lineResLon) {
            // Force recreation
            _graticule = null;
        }
        fireLayerDataChanged(getBounds());
    }

    @Override
    public void renderLayer(Rendering rendering) {
        if (_graticule == null) {
            _graticule = Graticule.create(_raster,
                                          _resAuto,
                                          _numPixels,
                                          _lineResLat,
                                          _lineResLon);
        }
        if (_graticule != null) {
            final Graphics2D g2d = rendering.getGraphics();
            final Viewport vp = rendering.getViewport();
            final AffineTransform transformSave = g2d.getTransform();
            try {
                final AffineTransform transform = new AffineTransform();
                transform.concatenate(transformSave);
                transform.concatenate(vp.getModelToViewTransform());
//                transform.concatenate(shapeToModelTransform);
                g2d.setTransform(transform);
                final GeneralPath[] linePaths = _graticule.getLinePaths();
                if (linePaths != null) {
                    drawLinePaths(g2d, linePaths);
                }
                if (_textEnabled) {
                    final Graticule.TextGlyph[] textGlyphs = _graticule.getTextGlyphs();
                    if (textGlyphs != null) {
                        drawTextLabels(g2d, textGlyphs);
                    }
                }
            } finally {
                g2d.setTransform(transformSave);
            }
        }
    }

    private void drawLinePaths(Graphics2D g2d, final GeneralPath[] linePaths) {
        Composite oldComposite = null;
        if (_lineTransparency > 0.0f) {
            oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F - _lineTransparency));
        }
        g2d.setPaint(_lineColor);
        g2d.setStroke(new BasicStroke(_lineWidth));
        for (GeneralPath linePath : linePaths) {
            g2d.draw(linePath);
        }
        if (oldComposite != null) {
            g2d.setComposite(oldComposite);
        }
    }

    private void drawTextLabels(Graphics2D g2d, final Graticule.TextGlyph[] textGlyphs) {
        final float tx = 3;
        final float ty = -3;

        if (_textBgTransparency < 1.0f) {
            Composite oldComposite = null;
            if (_textBgTransparency > 0.0f) {
                oldComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F - _textBgTransparency));
            }

            g2d.setPaint(_textBgColor);
            g2d.setStroke(new BasicStroke(0));
            Rectangle2D labelBounds;
            for (Graticule.TextGlyph glyph : textGlyphs) {
                g2d.translate(glyph.getX(), glyph.getY());
                g2d.rotate(glyph.getAngle());

                labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);
                labelBounds.setRect(labelBounds.getX() + tx - 1,
                                    labelBounds.getY() + ty - 1,
                                    labelBounds.getWidth() + 4,
                                    labelBounds.getHeight());
                g2d.fill(labelBounds);

                g2d.rotate(-glyph.getAngle());
                g2d.translate(-glyph.getX(), -glyph.getY());
            }

            if (oldComposite != null) {
                g2d.setComposite(oldComposite);
            }
        }

        g2d.setFont(_textFont);
        g2d.setPaint(_textFgColor);
        for (Graticule.TextGlyph glyph : textGlyphs) {
            g2d.translate(glyph.getX(), glyph.getY());
            g2d.rotate(glyph.getAngle());

            g2d.drawString(glyph.getText(), tx, ty);

            g2d.rotate(-glyph.getAngle());
            g2d.translate(-glyph.getX(), -glyph.getY());
        }
    }

    @Override
    public void disposeLayer() {
        if (_product != null) {
            _product.removeProductNodeListener(_productNodeHandler);
            _product = null;
            _graticule = null;
        }
    }

    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        /**
         * Overwrite this method if you want to be notified when a node changed.
         *
         * @param event the product node which the listener to be notified
         */
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() == _product && Product.PROPERTY_NAME_GEOCODING.equals(event.getPropertyName())) {
                // Force recreation
                _graticule = null;
                fireLayerDataChanged(getBounds());
            }
        }
    }

}
