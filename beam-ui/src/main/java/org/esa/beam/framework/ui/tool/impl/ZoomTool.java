/*
 * Created at 17.07.2004 12:13:58
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.framework.ui.tool.impl;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;

import javax.swing.ImageIcon;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

import com.bc.swing.GraphicsPane;

public class ZoomTool extends AbstractTool {
    private int _viewportX;
    private int _viewportY;
    private Graphics _graphics;
    private final Rectangle _zoomRect = new Rectangle();

    /**
     * Gets a thing that can be drawn while the tool is working.
     *
     * @return always <code>null</code>
     */
    public Drawable getDrawable() {
        return null;
    }

    public void mousePressed(ToolInputEvent e) {
        _graphics = e.getComponent().getGraphics();
        _viewportX = e.getMouseEvent().getX();
        _viewportY = e.getMouseEvent().getY();
        setZoomRect(e);
    }

    public void mouseDragged(ToolInputEvent e) {
        if (_graphics == null) {
            return;
        }
        _graphics.setXORMode(Color.white);
        if (!_zoomRect.isEmpty()) {
            drawZoomRect();
        }
        setZoomRect(e);
        drawZoomRect();
        _graphics.setPaintMode();
    }

    private void setZoomRect(ToolInputEvent e) {
        int x = _viewportX;
        int y = _viewportY;
        int w = e.getMouseEvent().getX() - x;
        int h = e.getMouseEvent().getY() - y;
        if (w < 0) {
            w = -w;
            x -= w;
        }
        if (h < 0) {
            h = -h;
            y -= h;
        }
        _zoomRect.setBounds(x, y, w, h);
    }

    public void mouseReleased(ToolInputEvent e) {
        if (_graphics == null) {
            return;
        }
        GraphicsPane graphicsPane = (GraphicsPane) e.getComponent();
        if (!_zoomRect.isEmpty()) {
            graphicsPane.zoom(new Rectangle2D.Double(graphicsPane.viewToModelX(_zoomRect.x),
                                                     graphicsPane.viewToModelY(_zoomRect.y),
                                                     graphicsPane.viewToModelLength(_zoomRect.width),
                                                     graphicsPane.viewToModelLength(_zoomRect.height)));
        } else {
            boolean zoomOut = e.getMouseEvent().isControlDown() || e.getMouseEvent().getButton() != 1;
            final double viewScaleOld = graphicsPane.getViewModel().getViewScale();
            final double viewScaleNew = zoomOut ? viewScaleOld / 1.6 : viewScaleOld * 1.6;
            graphicsPane.zoom(graphicsPane.viewToModelX(_zoomRect.x),
                              graphicsPane.viewToModelY(_zoomRect.y),
                              viewScaleNew);
        }
        _graphics.dispose();
        _graphics = null;
        _zoomRect.setBounds(0, 0, 0, 0);
    }


    public Cursor getCursor() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "pinCursor";
        ImageIcon icon = UIUtils.loadImageIcon("cursors/ZoomTool.gif");

        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        Point hotSpot = new Point((8 * bestCursorSize.width) / icon.getIconWidth(),
                                  (8 * bestCursorSize.height) / icon.getIconHeight());

        return defaultToolkit.createCustomCursor(icon.getImage(), hotSpot, cursorName);
    }

    private void drawZoomRect() {
        _graphics.drawRect(_zoomRect.x, _zoomRect.y, _zoomRect.width, _zoomRect.height);
    }
}
