/*
 * $Id: $
 * 
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.util.MouseEventFilterFactory;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.Vector;

public class LayerDisplay extends LayerCanvas {
    private Tool tool;
    private int pixelX = -1;
    private int pixelY = -1;
    private ComponentAdapter componentAdapter;
    private MouseInputListener mouseInputListener;
    private KeyListener imageDisplayKeyListener;
    private final Vector<PixelPositionListener> pixelPositionListeners;
    private final ProductSceneView45 productSceneView45;

    LayerDisplay(Layer layer, ProductSceneView45 productSceneView45) {
        super(layer);
        this.productSceneView45 = productSceneView45;
        pixelPositionListeners = new Vector<PixelPositionListener>();
        registerListeners();
    }

    @Override
    public void dispose() {
        deregisterListeners();
        super.dispose();
    }

    private void registerListeners() {
        registerComponentListener();
        registerMouseListeners();
        registerKeyListeners();
    }

    private void deregisterListeners() {
        removeComponentListener(componentAdapter);
        removeMouseListener(mouseInputListener);
        removeMouseMotionListener(mouseInputListener);
        removeKeyListener(imageDisplayKeyListener);
        if (pixelPositionListeners != null) {
            pixelPositionListeners.clear();
        }
    }

    /**
     * Adds a new pixel position listener to this image display component. If
     * the component already contains the given listener, the method does
     * nothing.
     *
     * @param listener the pixel position listener to be added
     */
    public final void addPixelPositionListener(PixelPositionListener listener) {
        if (listener == null) {
            return;
        }
        if (pixelPositionListeners.contains(listener)) {
            return;
        }
        pixelPositionListeners.add(listener);
    }

    /**
     * Removes a pixel position listener from this image display component.
     *
     * @param listener the pixel position listener to be removed
     */
    public final void removePixelPositionListener(PixelPositionListener listener) {
        if (listener == null || pixelPositionListeners.isEmpty()) {
            return;
        }
        pixelPositionListeners.remove(listener);
    }

    final synchronized void fireToolEvent(MouseEvent e) {
        if (tool != null) {
            ToolInputEvent toolInputEvent = createToolInputEvent(e);
            tool.handleEvent(toolInputEvent);
        }
    }

    private ToolInputEvent createToolInputEvent(MouseEvent e) {
        return new ToolInputEvent(this, e, pixelX, pixelY, isPixelPosValid(pixelX, pixelY));
    }

    private ToolInputEvent createToolInputEvent(KeyEvent e) {
        return new ToolInputEvent(this, e, pixelX, pixelY, isPixelPosValid(pixelX, pixelY));
    }

    private boolean isPixelPosValid(int currentPixelX, int currentPixelY) {
        return currentPixelX >= 0 && currentPixelX < getImage().getWidth() && currentPixelY >= 0
                && currentPixelY < getImage().getHeight();
    }

    private ImageLayer getBaseImageLayer() {
        return productSceneView45.getBaseImageLayer();
    }

    private RenderedImage getImage() {
        return getBaseImageLayer().getImage();
    }

    /**
     * Fires a 'pixel position changed' event to all registered pixel-pos
     * listeners.
     *
     * @param e             the event
     * @param currentPixelX pixel position X
     * @param currentPixelY pixel position Y
     */
    private final void firePixelPosChanged(MouseEvent e, int currentPixelX, int currentPixelY, int currentLevel) {
        boolean pixelPosValid = isPixelPosValid(currentPixelX, currentPixelY);
        for (PixelPositionListener listener : pixelPositionListeners) {
            listener.pixelPosChanged(getImage(), currentPixelX, currentPixelY, currentLevel, pixelPosValid, e);
        }
    }

    /**
     * Fires a 'pixel position is invalid' event to all registered listeners.
     */
    private final void firePixelPosNotAvailable() {
        for (PixelPositionListener listener : pixelPositionListeners) {
            listener.pixelPosNotAvailable(getImage());
        }
    }

    private void registerComponentListener() {
        componentAdapter = new ComponentAdapter() {

            /**
             * Invoked when the component's size changes.
             */
            @Override
            public void componentResized(ComponentEvent e) {
            }

            /**
             * Invoked when the component has been made invisible.
             */
            @Override
            public void componentHidden(ComponentEvent e) {
                firePixelPosNotAvailable();
            }
        };
        addComponentListener(componentAdapter);
    }

    private void registerMouseListeners() {
        MouseInputListener pixelposUpdater = new PixelPosUpdater();
        mouseInputListener = MouseEventFilterFactory
                .createFilter(pixelposUpdater);
        addMouseListener(mouseInputListener);
        addMouseMotionListener(mouseInputListener);
    }

    private void registerKeyListeners() {

        imageDisplayKeyListener = new KeyListener() {

            /**
             * Invoked when a key has been pressed.
             */
            public void keyPressed(KeyEvent e) {
                if (tool != null) {
                    tool.handleEvent(createToolInputEvent(e));
                }
            }

            /**
             * Invoked when a key has been released.
             */
            public void keyReleased(KeyEvent e) {
                if (tool != null) {
                    tool.handleEvent(createToolInputEvent(e));
                }
            }

            /**
             * Invoked when a key has been typed. This event occurs when a key
             * press is followed by a key dispose.
             */
            public void keyTyped(KeyEvent e) {
                if (tool != null) {
                    tool.handleEvent(createToolInputEvent(e));
                }
            }
        };
        addKeyListener(imageDisplayKeyListener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;

            if (tool != null && tool.isActive()) {
                final Viewport vp = getViewport();
                final AffineTransform transformSave = g2d.getTransform();
                try {
                    final AffineTransform transform = new AffineTransform();
                    transform.concatenate(vp.getModelToViewTransform());
                    g2d.setTransform(transform);
                    drawToolNoTransf(g2d);
                } finally {
                    g2d.setTransform(transformSave);
                }
            }
        }
    }

    private void drawToolNoTransf(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        if (tool.getDrawable() != null) {
            // System.out.println("DRAW_TOOL:" + tool.getClass().toString());
            tool.getDrawable().draw(g2d);
        }
        // reset rendering hints ?????? TODO
    }

    void setTool(Tool tool) {
        // Tool oldTool = this.tool;
        this.tool = tool;
        // is anyone listening ???
        // productSceneView45.firePropertyChange("tool", oldTool, tool);
    }

    public Tool getTool() {
        return tool;
    }

    public String createPixelInfoString(PixelInfoFactory pixelInfoFactory) {
        return pixelInfoFactory.createPixelInfoString(pixelX, pixelY);
    }

    private void setPixelPos(MouseEvent e, boolean showBorder) {
        Point p = e.getPoint();
        ImageLayer baseImageLayer = getBaseImageLayer();
        Viewport viewport = getViewport();
        AffineTransform v2mTransform = viewport.getViewToModelTransform();
        AffineTransform m2iTransform = baseImageLayer.getModelToImageTransform();
        final Point2D modelP = v2mTransform.transform(p, null);
        Point2D imageP = m2iTransform.transform(modelP, null);
        int currentPixelX = (int) Math.floor(imageP.getX());
        int currentPixelY = (int) Math.floor(imageP.getY());
        if (currentPixelX != pixelX || currentPixelY != pixelY) {
            // if (isPixelBorderDisplayEnabled() && (showBorder ||
            // pixelBorderDrawn)) {
            // drawPixelBorder(pixelX, pixelY, showBorder);
            // }
            final int currentLevel = baseImageLayer.getLevel(viewport);
            setPixelPos(e, currentPixelX, currentPixelY, currentLevel);
        }
    }

    private void setPixelPos(MouseEvent e, int currentPixelX, int currentPixelY, int currentLevel) {
        pixelX = currentPixelX;
        pixelY = currentPixelY;
        if (e.getID() != MouseEvent.MOUSE_EXITED) {
            firePixelPosChanged(e, pixelX, pixelY, currentLevel);
        } else {
            firePixelPosNotAvailable();
        }
    }

    private final class PixelPosUpdater implements MouseInputListener {

        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public final void mouseClicked(MouseEvent e) {
            updatePixelPos(e, false);
        }

        /**
         * Invoked when the mouse enters a component.
         */
        public final void mouseEntered(MouseEvent e) {
            updatePixelPos(e, false);
        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        public final void mousePressed(MouseEvent e) {
            updatePixelPos(e, false);
        }

        /**
         * Invoked when a mouse button has been released on a component.
         */
        public final void mouseReleased(MouseEvent e) {
            updatePixelPos(e, false);
        }

        /**
         * Invoked when the mouse exits a component.
         */
        public final void mouseExited(MouseEvent e) {
            updatePixelPos(e, false);
        }

        /**
         * Invoked when a mouse button is pressed on a component and then
         * dragged. Mouse drag events will continue to be delivered to the
         * component where the first originated until the mouse button is
         * released (regardless of whether the mouse position is within the
         * bounds of the component).
         */
        public final void mouseDragged(MouseEvent e) {
            updatePixelPos(e, true);
        }

        /**
         * Invoked when the mouse button has been moved on a component (with no
         * buttons no down).
         */
        public final void mouseMoved(MouseEvent e) {
            updatePixelPos(e, true);
        }

        private void updatePixelPos(MouseEvent e, boolean showBorder) {
            setPixelPos(e, showBorder);
            fireToolEvent(e);
        }
    }

}