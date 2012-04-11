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

package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
class CursorOverlay implements LayerCanvas.Overlay {

    private static final int MAX_CROSSHAIR_SIZE = 20;

    private final ProductSceneView sceneView;
    private GeoPos geoPosition;
    private BasicStroke cursorStroke;
    private Color cursorColor;

    CursorOverlay(ProductSceneView sceneView, GeoPos geoPos) {
        this.sceneView = sceneView;
        geoPosition = geoPos;
        cursorStroke = new BasicStroke(1F);
        cursorColor = Color.WHITE;
    }

    public void setGeoPosition(GeoPos geoPosition) {
        this.geoPosition = geoPosition;
    }

    @Override
    public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
        if (geoPosition == null || !geoPosition.isValid()) {
            return;
        }

        final GeoCoding geoCoding = sceneView.getRaster().getGeoCoding();
        if (!geoCoding.canGetPixelPos()) {
            return;
        }
        final Product product = sceneView.getRaster().getProduct();
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPosition, null);
        if (!pixelPos.isValid() || !product.containsPixel(pixelPos)) {
            return;
        }

        final Viewport viewport = canvas.getViewport();

        drawCursor(rendering.getGraphics(), viewport, pixelPos);
    }

    private void drawCursor(Graphics2D graphics, Viewport viewport, PixelPos pixelPos) {
        AffineTransform i2mTransform = sceneView.getBaseImageLayer().getImageToModelTransform();
        AffineTransform m2vTransform = viewport.getModelToViewTransform();
        AffineTransform i2vTransform = new AffineTransform(m2vTransform);
        i2vTransform.concatenate(i2mTransform);

        Point centerPixel = new Point((int) Math.floor(pixelPos.x), (int) Math.floor(pixelPos.y));
        Rectangle pixelImageRect = new Rectangle(centerPixel, new Dimension(1, 1));

        Rectangle2D pixelViewRect = i2vTransform.createTransformedShape(pixelImageRect).getBounds2D();
        graphics.setStroke(cursorStroke);
        graphics.setColor(cursorColor);
        graphics.setXORMode(Color.BLACK);
        graphics.draw(pixelViewRect);

        if (pixelViewRect.getBounds2D().getWidth() < MAX_CROSSHAIR_SIZE) {
            drawCrosshair(graphics, i2vTransform, centerPixel, pixelViewRect);
        }
    }

    private void drawCrosshair(Graphics2D graphics, AffineTransform i2vTransform, Point centerPixel,
                               Rectangle2D pixelViewRect) {
        Rectangle surroundImageRect = new Rectangle(centerPixel.x - 1, centerPixel.y - 1, 3, 3);
        Rectangle2D surroundViewRect = i2vTransform.createTransformedShape(surroundImageRect).getBounds2D();
        double scale = MAX_CROSSHAIR_SIZE / surroundViewRect.getBounds2D().getWidth();
        if (scale > 1) {
            double newWidth = surroundViewRect.getWidth() * scale;
            double newHeight = surroundViewRect.getHeight() * scale;
            double newX = surroundViewRect.getCenterX() - newWidth / 2;
            double newY = surroundViewRect.getCenterY() - newHeight / 2;
            surroundViewRect.setRect(newX, newY, newWidth, newHeight);
        }
        graphics.draw(surroundViewRect);

        Line2D.Double northLine = new Line2D.Double(surroundViewRect.getCenterX(), surroundViewRect.getMinY(),
                                                    surroundViewRect.getCenterX(), pixelViewRect.getMinY());
        Line2D.Double eastLine = new Line2D.Double(surroundViewRect.getMaxX(), surroundViewRect.getCenterY(),
                                                   pixelViewRect.getMaxX(), surroundViewRect.getCenterY());
        Line2D.Double southLine = new Line2D.Double(surroundViewRect.getCenterX(), surroundViewRect.getMaxY(),
                                                    surroundViewRect.getCenterX(), pixelViewRect.getMaxY());
        Line2D.Double westLine = new Line2D.Double(surroundViewRect.getMinX(), surroundViewRect.getCenterY(),
                                                   pixelViewRect.getMinX(), surroundViewRect.getCenterY());
        graphics.draw(northLine);
        graphics.draw(eastLine);
        graphics.draw(southLine);
        graphics.draw(westLine);
    }

}
