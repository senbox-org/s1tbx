/*
 * Created at 17.07.2004 12:14:14
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.framework.ui.tool.impl;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.ImageIcon;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

import com.bc.swing.GraphicsPane;

public class  PannerTool extends AbstractTool {
    private int _viewportX;
    private int _viewportY;
    private double _modelOffsetX;
    private double _modelOffsetY;

    /**
     * Gets a thing that can be drawn while the tool is working.
     *
     * @return always <code>null</code>
     */
    public Drawable getDrawable() {
        return null;
    }

    public void mousePressed(ToolInputEvent e) {
        GraphicsPane graphicsPane = (GraphicsPane) e.getComponent();
        _viewportX = e.getMouseEvent().getX();
        _viewportY = e.getMouseEvent().getY();
        _modelOffsetX = graphicsPane.getViewModel().getModelOffsetX();
        _modelOffsetY = graphicsPane.getViewModel().getModelOffsetY();
    }

    public void mouseDragged(ToolInputEvent e) {
        GraphicsPane graphicsPane = (GraphicsPane) e.getComponent();
        final double viewScale = graphicsPane.getViewModel().getViewScale();
        graphicsPane.getViewModel().setModelOffset(
                _modelOffsetX + (_viewportX -e.getMouseEvent().getX()) / viewScale,
                _modelOffsetY + (_viewportY - e.getMouseEvent().getY()) / viewScale);
    }

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
