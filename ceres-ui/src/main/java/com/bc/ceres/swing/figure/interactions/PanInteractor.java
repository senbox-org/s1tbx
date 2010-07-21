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

import java.awt.Cursor;
import java.awt.event.MouseEvent;

public class PanInteractor extends ViewportInteractor {

    private int viewportX;
    private int viewportY;

    @Override
    public void mousePressed(MouseEvent event) {
        viewportX = event.getX();
        viewportY = event.getY();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        Viewport viewport = getViewport(event);
        int viewportX = event.getX();
        int viewportY = event.getY();
        final double dx = viewportX - this.viewportX;
        final double dy = viewportY - this.viewportY;
        viewport.moveViewDelta(dx, dy);
        this.viewportX = viewportX;
        this.viewportY = viewportY;
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
}