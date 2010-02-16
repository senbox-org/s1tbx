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