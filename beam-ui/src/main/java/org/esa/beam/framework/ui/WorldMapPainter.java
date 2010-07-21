/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;


/**
 * Paints the given world map with the given products on top.
 * The selected product is painted highlighted.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public class WorldMapPainter {

    private static final String _FONT_NAME = "Verdana";
    private static final int _FONT_SIZE = 11;

    private BufferedImage _worldImage;
    private final Dimension _currentImageSize;

    private float _scale;
    private Product _selectedProduct;
    private Product[] _products;

    private boolean _borderPassed = false;
    private int _passedSide = 0;
    private float _lastLon;
    private GeoPos[][] _extraGeoBoundaries;
    private boolean drawingLabels;


    public WorldMapPainter(final BufferedImage worldImage) {
        Guardian.assertNotNull("worldImage", worldImage);
        this._worldImage = worldImage;
        _currentImageSize = new Dimension();
        drawingLabels = true;
    }

    public final Product getSelectedProduct() {
        return _selectedProduct;
    }

    public final void setSelectedProduct(final Product product) {
        _selectedProduct = product;
    }

    public void setWorldMapImage(BufferedImage image) {
        Guardian.assertNotNull("image", image);
        if (_worldImage != image) {
            _worldImage = image;
            _currentImageSize.width = MathUtils.ceilInt(_worldImage.getWidth(null) * _scale);
            _currentImageSize.height = MathUtils.ceilInt(_worldImage.getHeight(null) * _scale);
        }
    }

    public final Product[] getProducts() {
        return _products;
    }

    public final void setProducts(final Product[] products) {
        if (_products != products) {
            _products = products;
        }
    }

    public void setPathesToDisplay(final GeoPos[][] geoBoundaries) {
        if (_extraGeoBoundaries != geoBoundaries) {
            _extraGeoBoundaries = geoBoundaries;
        }
    }

    public GeoPos[][] getPathesToDisplay() {
        return _extraGeoBoundaries;
    }

    public final float getScale() {
        return _scale;
    }

    public final void setScale(final float scale) {
        if (_scale != scale) {
            _scale = scale;
            _currentImageSize.width = MathUtils.ceilInt(_worldImage.getWidth(null) * _scale);
            _currentImageSize.height = MathUtils.ceilInt(_worldImage.getHeight(null) * _scale);
        }
    }

    public boolean isDrawingLabels() {
        return drawingLabels;
    }

    public void setDrawingLabels(final boolean drawingLabels) {
        this.drawingLabels = drawingLabels;
    }

    public final Dimension getCurrentImageSize() {
        return _currentImageSize;
    }

    public PixelPos getCurrentProductCenter() {
        final Product currentProduct = getSelectedProduct();
        if (currentProduct == null) {
            return null;
        }
        return getProductCenter(currentProduct);
    }

    public final void paint(final Graphics g) {
        final Graphics2D g2d;
        if (g instanceof Graphics2D) {
            g2d = (Graphics2D) g;
        } else {
            return;
        }
        if (_scale > 1) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        g2d.setStroke(new BasicStroke(1 / _scale));
        g2d.setFont(createLabelFont());
        prepareScaling(g2d);
        g2d.drawImage(_worldImage, 0, 0, null);
        if (_extraGeoBoundaries != null) {
            for (final GeoPos[] extraGeoBoundary : _extraGeoBoundaries) {
                drawGeoBoundary(g2d, extraGeoBoundary, false, null, null);
            }
        }
        if (_products != null) {
            for (final Product product : _products) {
                if (_selectedProduct != product) {
                    drawProduct(g2d, product, false);
                }
            }
        }
        if (_selectedProduct != null) {
            drawProduct(g2d, _selectedProduct, true);
        }
    }

    private void prepareScaling(final Graphics2D g2d) {
        // todo - Ugly hack to prevent stripes in image if it is not scaled  
//        if (_scale == 1.0f) {
//            _scale = 0.999f;
//        }
        final AffineTransform oldTransform = g2d.getTransform();
        final AffineTransform scaleTransform = new AffineTransform();
        scaleTransform.setToScale(_scale, _scale);
        oldTransform.concatenate(scaleTransform);
        g2d.setTransform(oldTransform);
    }

    private Font createLabelFont() {
        return new Font(_FONT_NAME, Font.BOLD, (int) (_FONT_SIZE / _scale));
    }

    private void drawProduct(final Graphics2D g2d, final Product product, final boolean isCurrent) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            return;
        }
        final int step = Math.max(16, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 250);
        final String text = "" + product.getRefNo();
        final PixelPos textCenter = getProductCenter(product);

        GeneralPath[] boundaryPaths = ProductUtils.createGeoBoundaryPaths(product, null, step);
        drawGeoBoundary(g2d, boundaryPaths, isCurrent, text, textCenter);
    }

    private void drawGeoBoundary(final Graphics2D g2d, final GeneralPath[] boundaryPaths, final boolean isCurrent,
                                 final String text, final PixelPos textCenter) {
        final double scale_x = _worldImage.getWidth(null) / 360.0;
        final double scale_y = -_worldImage.getHeight(null) / 180.0;
        final AffineTransform transform = new AffineTransform(scale_x, 0.0, 0.0, scale_y,
                                                              _worldImage.getWidth(null) - scale_x * 180 - 1,
                                                              _worldImage.getHeight(null) + scale_y * 90);
        for (int i = 0; i < boundaryPaths.length; i++) {
            GeneralPath boundaryPath = boundaryPaths[i];
            boundaryPath.transform(transform);
            drawPath(isCurrent, g2d, boundaryPath, 0f);
        }

        if (isDrawingLabels()) {
            drawText(g2d, text, textCenter, 0f);
        }

        _lastLon = 0;
        _borderPassed = false;
        _passedSide = 0;
    }

    private void drawGeoBoundary(final Graphics2D g2d, final GeoPos[] geoBoundary, final boolean isCurrent,
                                 final String text, final PixelPos textCenter) {
        final GeneralPath gp = convertToPixelPath(geoBoundary);
        drawPath(isCurrent, g2d, gp, 0f);
        if (isDrawingLabels()) {
            drawText(g2d, text, textCenter, 0f);
        }

        if (_passedSide != 0) {
            //Debug.trace("cross180");
            drawPath(isCurrent, g2d, gp, _worldImage.getWidth(null));
            if (isDrawingLabels()) {
                drawText(g2d, text, textCenter, _worldImage.getWidth(null));
            }
        }
        _lastLon = 0;
        _borderPassed = false;
        _passedSide = 0;
    }

    private void drawPath(final boolean isCurrent, Graphics2D g2d, final GeneralPath gp, final float offsetX) {
        g2d = prepareGraphics2D(offsetX, g2d);
        if (isCurrent) {
            g2d.setColor(new Color(255, 200, 200, 70));
        } else {
            g2d.setColor(new Color(255, 255, 255, 70));
        }
        g2d.fill(gp);
        if (isCurrent) {
            g2d.setColor(new Color(255, 0, 0));
        } else {
            g2d.setColor(new Color(0, 0, 0));
        }
        g2d.draw(gp);
    }

    private GeneralPath convertToPixelPath(final GeoPos[] geoBoundary) {
        final GeneralPath gp = new GeneralPath();
        for (int i = 0; i < geoBoundary.length; i++) {
            final GeoPos geoPos = geoBoundary[i];
            final PixelPos pos = getPixelPos(geoPos);
            if (i == 0) {
                gp.moveTo(pos.x, pos.y);
            } else {
                gp.lineTo(pos.x, pos.y);
            }
        }
        gp.closePath();
        return gp;
    }

    private void drawText(Graphics2D g2d, final String text, final PixelPos textCenter, final float offsetX) {
        if (text == null || textCenter == null) {
            return;
        }
        g2d = prepareGraphics2D(offsetX, g2d);
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final Color color = g2d.getColor();
        g2d.setColor(Color.black);

        g2d.drawString(text,
                       textCenter.x - fontMetrics.stringWidth(text) / 2f,
                       textCenter.y + fontMetrics.getAscent() / 2f);
        g2d.setColor(color);
    }

    private PixelPos getProductCenter(final Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        PixelPos centerPos = null;
        if (geoCoding != null) {
            centerPos = getPixelPos(geoCoding,
                                    new PixelPos(0.5f * product.getSceneRasterWidth() + 0.5f,
                                                 0.5f * product.getSceneRasterHeight() + 0.5f));
        }
        return centerPos;
    }

    private PixelPos getPixelPos(final GeoCoding geoCoding, final PixelPos productPixelPos) {
        final GeoPos geoPos = geoCoding.getGeoPos(productPixelPos, null);
        return getPixelPos(geoPos);
    }

    /**
     * Retrives the pixel pisition adjusted to the wolrd map image.
     *
     * @param geoPos
     *
     * @return the pixel position
     */
    private PixelPos getPixelPos(final GeoPos geoPos) {
        if (_lastLon > 150 && geoPos.lon < -150) {
            _borderPassed = !_borderPassed;
            if (_passedSide == 0) {
                _passedSide = 1;
            }
        }
        if (_lastLon < -150 && geoPos.lon > 150) {
            _borderPassed = !_borderPassed;
            if (_passedSide == 0) {
                _passedSide = -1;
            }
        }
        _lastLon = geoPos.lon;
        if (_borderPassed) {
            if (_passedSide == 1) {
                geoPos.lon += 360f;
            } else {
                geoPos.lon -= 360f;
            }
        }
        final PixelPos pixelPos = new PixelPos(_worldImage.getWidth(null) / 360f * (geoPos.getLon() + 180f),
                                               _worldImage.getHeight(null) - (_worldImage.getHeight(
                                                       null) / 180f * (geoPos.getLat() + 90f)));
        return pixelPos;
    }

    private Graphics2D prepareGraphics2D(final float offsetX, Graphics2D g2d) {
        if (offsetX != 0f) {
            g2d = (Graphics2D) g2d.create();
            final AffineTransform transform = g2d.getTransform();
            final AffineTransform offsetTrans = new AffineTransform();
            if (_passedSide == 1) {
                offsetTrans.setToTranslation(-offsetX, 0);
            } else {
                offsetTrans.setToTranslation(+offsetX, 0);
            }
            transform.concatenate(offsetTrans);
            g2d.setTransform(transform);
        }
        return g2d;
    }
}
