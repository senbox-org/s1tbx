/*
 * Created at 17.07.2004 12:14:14
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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;


public class PannerTool extends AbstractTool {
    private int _viewportX;
    private int _viewportY;

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
        _viewportX = e.getMouseEvent().getX();
        _viewportY = e.getMouseEvent().getY();
    }

    @Override
    public void mouseDragged(ToolInputEvent e) {
        Component component = e.getComponent();
        // this should be always the case
        if (component instanceof AdjustableView) {
            AdjustableView view = (AdjustableView) component;
            Viewport viewport = view.getViewport();
            int viewportX = e.getMouseEvent().getX();
            int viewportY = e.getMouseEvent().getY();
            final double dx = viewportX - _viewportX;
            final double dy = viewportY - _viewportY;
            viewport.moveViewDelta(dx, dy);
            _viewportX = viewportX;
            _viewportY = viewportY;
        }
    }

    @Override
    public Cursor getCursor() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "pinCursor";
        ImageIcon icon = UIUtils.loadImageIcon("cursors/PannerTool.gif");

        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        Point hotSpot = new Point((12 * bestCursorSize.width) / icon.getIconWidth(),
                                  (12 * bestCursorSize.height) / icon.getIconHeight());

        return defaultToolkit.createCustomCursor(icon.getImage(), hotSpot, cursorName);
    }

}
