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

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Graticule;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Guardian;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class GraticuleLayer extends Layer {

    private static final LayerType LAYER_TYPE = LayerType.getLayerType(Type.class.getName());

    public static final String PROPERTY_NAME_PRODUCT = "graticule.product";
    public static final String PROPERTY_NAME_RASTER = "graticule.raster";
    public static final String PROPERTY_NAME_TRANSFORM = "graticule.i2mTransform";
    public static final String PROPERTY_NAME_RES_AUTO = "graticule.res.auto";
    public static final String PROPERTY_NAME_RES_PIXELS = "graticule.res.pixels";
    public static final String PROPERTY_NAME_RES_LAT = "graticule.res.lat";
    public static final String PROPERTY_NAME_RES_LON = "graticule.res.lon";
    public static final String PROPERTY_NAME_LINE_COLOR = "graticule.line.color";
    public static final String PROPERTY_NAME_LINE_TRANSPARENCY = "graticule.line.transparency";
    public static final String PROPERTY_NAME_LINE_WIDTH = "graticule.line.width";
    public static final String PROPERTY_NAME_TEXT_ENABLED = "graticule.text.enabled";
    public static final String PROPERTY_NAME_TEXT_FONT = "graticule.text.font";
    public static final String PROPERTY_NAME_TEXT_FG_COLOR = "graticule.text.fg.color";
    public static final String PROPERTY_NAME_TEXT_BG_COLOR = "graticule.text.bg.color";
    public static final String PROPERTY_NAME_TEXT_BG_TRANSPARENCY = "graticule.text.bg.transparency";

    public static final boolean DEFAULT_RES_AUTO = true;
    public static final int DEFAULT_RES_PIXELS = 128;
    public static final double DEFAULT_RES_LAT = 1.0;
    public static final double DEFAULT_RES_LON = 1.0;
    public static final Color DEFAULT_LINE_COLOR = new Color(204, 204, 255);
    public static final double DEFAULT_LINE_TRANSPARENCY = 0.0;
    public static final double DEFAULT_LINE_WIDTH = 0.5;
    public static final boolean DEFAULT_TEXT_ENABLED = true;
    public static final Font DEFAULT_TEXT_FONT = new Font("SansSerif", Font.ITALIC, 12);
    public static final Color DEFAULT_TEXT_FG_COLOR = Color.WHITE;
    public static final Color DEFAULT_TEXT_BG_COLOR = Color.BLACK;
    public static final double DEFAULT_TEXT_BG_TRANSPARENCY = 0.7;

    private Product product;
    private RasterDataNode raster;
    private final AffineTransform i2mTransform;

    private ProductNodeHandler productNodeHandler;
    private Graticule graticule;

    public GraticuleLayer(Product product, RasterDataNode raster, AffineTransform i2mTransform) {
        super(LAYER_TYPE, "Graticule");
        Guardian.assertNotNull("product", product);
        this.i2mTransform = i2mTransform;

        productNodeHandler = new ProductNodeHandler();

        this.product = product;
        this.product.addProductNodeListener(productNodeHandler);
        this.raster = raster;

        getStyle().setOpacity(0.5);
    }

    Product getProduct() {
        return product;
    }

    RasterDataNode getRaster() {
        return raster;
    }

    AffineTransform getI2mTransform() {
        return i2mTransform;
    }

    @Override
    public void renderLayer(Rendering rendering) {
        if (graticule == null) {
            graticule = Graticule.create(raster,
                                         getResAuto(),
                                         getResPixels(),
                                         (float) getResLat(),
                                         (float) getResLon());
        }
        if (graticule != null) {
            final Graphics2D g2d = rendering.getGraphics();
            final Viewport vp = rendering.getViewport();
            final AffineTransform transformSave = g2d.getTransform();
            try {
                final AffineTransform transform = new AffineTransform();
                transform.concatenate(transformSave);
                transform.concatenate(vp.getModelToViewTransform());
                transform.concatenate(i2mTransform);
                g2d.setTransform(transform);
                final GeneralPath[] linePaths = graticule.getLinePaths();
                if (linePaths != null) {
                    drawLinePaths(g2d, linePaths);
                }
                if (isTextEnabled()) {
                    final Graticule.TextGlyph[] textGlyphs = graticule.getTextGlyphs();
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
        if (getLineTransparency() > 0.0) {
            oldComposite = g2d.getComposite();
            g2d.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1.0 - getLineTransparency())));
        }
        g2d.setPaint(getLineColor());
        g2d.setStroke(new BasicStroke((float) getLineWidth()));
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

        if (getTextBgTransparency() < 1.0) {
            Composite oldComposite = null;
            if (getTextBgTransparency() > 0.0) {
                oldComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                            (float) (1.0 - getTextBgTransparency())));
            }

            g2d.setPaint(getTextBgColor());
            g2d.setStroke(new BasicStroke(0));
            for (Graticule.TextGlyph glyph : textGlyphs) {
                g2d.translate(glyph.getX(), glyph.getY());
                g2d.rotate(glyph.getAngle());

                Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);
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

        g2d.setFont(getTextFont());
        g2d.setPaint(getTextFgColor());
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
        if (product != null) {
            product.removeProductNodeListener(productNodeHandler);
            product = null;
            graticule = null;
        }
    }

    private boolean getResAuto() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_RES_AUTO)) {
            return (Boolean) getStyle().getProperty(PROPERTY_NAME_RES_AUTO);
        }

        return DEFAULT_RES_AUTO;
    }

    private double getResLon() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_RES_LON)) {
            return (Double) getStyle().getProperty(PROPERTY_NAME_RES_LON);
        }

        return DEFAULT_RES_LON;
    }

    private double getResLat() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_RES_LAT)) {
            return (Double) getStyle().getProperty(PROPERTY_NAME_RES_LAT);
        }

        return DEFAULT_RES_LAT;
    }

    private int getResPixels() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_RES_PIXELS)) {
            return (Integer) getStyle().getProperty(PROPERTY_NAME_RES_PIXELS);
        }

        return DEFAULT_RES_PIXELS;
    }

    public boolean isTextEnabled() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_ENABLED)) {
            return (Boolean) getStyle().getProperty(PROPERTY_NAME_TEXT_ENABLED);
        }

        return DEFAULT_TEXT_ENABLED;
    }

    private Color getLineColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_LINE_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_LINE_COLOR);
        }

        return DEFAULT_LINE_COLOR;
    }

    private double getLineTransparency() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_LINE_TRANSPARENCY)) {
            return (Double) style.getProperty(PROPERTY_NAME_LINE_TRANSPARENCY);
        }

        return DEFAULT_LINE_TRANSPARENCY;
    }

    private double getLineWidth() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_LINE_WIDTH)) {
            return (Double) style.getProperty(PROPERTY_NAME_LINE_WIDTH);
        }

        return DEFAULT_LINE_WIDTH;
    }

    private Font getTextFont() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_FONT)) {
            return (Font) style.getProperty(PROPERTY_NAME_TEXT_FONT);
        }

        return DEFAULT_TEXT_FONT;
    }

    private Color getTextFgColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_FG_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_TEXT_FG_COLOR);
        }

        return DEFAULT_TEXT_FG_COLOR;
    }

    private Color getTextBgColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_BG_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_TEXT_BG_COLOR);
        }

        return DEFAULT_TEXT_BG_COLOR;
    }

    private double getTextBgTransparency() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_TEXT_BG_TRANSPARENCY)) {
            return (Double) style.getProperty(PROPERTY_NAME_TEXT_BG_TRANSPARENCY);
        }

        return DEFAULT_TEXT_BG_TRANSPARENCY;
    }

    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        /**
         * Overwrite this method if you want to be notified when a node changed.
         *
         * @param event the product node which the listener to be notified
         */
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() == product && Product.PROPERTY_NAME_GEOCODING.equals(event.getPropertyName())) {
                // Force recreation
                graticule = null;
                fireLayerDataChanged(getModelBounds());
            }
        }
    }

    public static class Type extends LayerType {

        @Override
        public String getName() {
            return "Graticule Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
            Product product = (Product) configuration.get(PROPERTY_NAME_PRODUCT);
            RasterDataNode raster = (RasterDataNode) configuration.get(PROPERTY_NAME_RASTER);
            AffineTransform i2mTransform = (AffineTransform) configuration.get(PROPERTY_NAME_TRANSFORM);
            return new GraticuleLayer(product, raster, i2mTransform);
        }

        @Override
        public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
            Assert.argument(layer instanceof GraticuleLayer, "layer instanceof GraticuleLayer");
            HashMap<String, Object> config = new HashMap<String, Object>();
            GraticuleLayer graticuleLayer = (GraticuleLayer) layer;
            config.put(PROPERTY_NAME_PRODUCT, graticuleLayer.getProduct());
            config.put(PROPERTY_NAME_RASTER, graticuleLayer.getRaster());
            config.put(PROPERTY_NAME_TRANSFORM, graticuleLayer.getI2mTransform());
            return config;
        }
    }
}
