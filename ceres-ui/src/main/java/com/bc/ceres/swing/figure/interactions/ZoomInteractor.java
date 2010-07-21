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

package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.figure.ViewportInteractor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class ZoomInteractor extends ViewportInteractor {
    private int viewportX;
    private int viewportY;
    private Graphics graphics;
    private final Rectangle zoomRect = new Rectangle();

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        viewportX = event.getX();
        viewportY = event.getY();
        setZoomRect(event);
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        graphics = event.getComponent().getGraphics();
        graphics.setXORMode(Color.white);
        if (!zoomRect.isEmpty()) {
            drawZoomRect();
        }
        setZoomRect(event);
        drawZoomRect();
        graphics.setPaintMode();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (graphics == null) {
            return;
        }
        Viewport viewport = getViewport(event);
        if (!zoomRect.isEmpty()) {
            AffineTransform v2m = viewport.getViewToModelTransform();
            Shape transformedShape = v2m.createTransformedShape(zoomRect);
            Rectangle2D bounds2D = transformedShape.getBounds2D();
            viewport.zoom(bounds2D);
        } else {
            boolean zoomOut = event.isControlDown() || event.getButton() != 1;
            final double viewScaleOld = viewport.getZoomFactor();
            final double viewScaleNew = zoomOut ? viewScaleOld / 1.6 : viewScaleOld * 1.6;
            viewport.setZoomFactor(viewScaleNew);
        }
        graphics.dispose();
        graphics = null;
        zoomRect.setBounds(0, 0, 0, 0);
    }

    private void setZoomRect(MouseEvent e) {
        int x = viewportX;
        int y = viewportY;
        int w = e.getX() - x;
        int h = e.getY() - y;
        if (w < 0) {
            w = -w;
            x -= w;
        }
        if (h < 0) {
            h = -h;
            y -= h;
        }
        zoomRect.setBounds(x, y, w, h);
    }

    private void drawZoomRect() {
        graphics.drawRect(zoomRect.x, zoomRect.y, zoomRect.width, zoomRect.height);
    }
}
