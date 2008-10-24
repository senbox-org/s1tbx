/*
 * Created at 17.07.2004 12:13:58
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.framework.ui.tool.impl;

import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

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
    @Override
    public Drawable getDrawable() {
        return null;
    }

    @Override
    public void mousePressed(ToolInputEvent e) {
        _graphics = e.getComponent().getGraphics();
        _viewportX = e.getMouseEvent().getX();
        _viewportY = e.getMouseEvent().getY();
        setZoomRect(e);
    }

    @Override
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

    @Override
    public void mouseReleased(ToolInputEvent e) {
        if (_graphics == null) {
            return;
        }
        Component component = e.getComponent();
        // this should be always the case
        if (component instanceof AdjustableView) {
            AdjustableView view = (AdjustableView) component;
            Viewport viewport = view.getViewport();
            if (!_zoomRect.isEmpty()) {
                AffineTransform v2m = viewport.getViewToModelTransform();
                Shape transformedShape = v2m.createTransformedShape(_zoomRect);
                Rectangle2D bounds2D = transformedShape.getBounds2D();
                viewport.zoom(bounds2D);
            } else {
                boolean zoomOut = e.getMouseEvent().isControlDown() || e.getMouseEvent().getButton() != 1;
                final double viewScaleOld = viewport.getZoomFactor();
                final double viewScaleNew = zoomOut ? viewScaleOld / 1.6 : viewScaleOld * 1.6;
                viewport.setZoomFactor(viewScaleNew);
            }
        }
        _graphics.dispose();
        _graphics = null;
        _zoomRect.setBounds(0, 0, 0, 0);
    }


    @Override
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
