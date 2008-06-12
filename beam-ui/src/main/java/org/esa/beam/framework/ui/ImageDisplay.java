/*
 * $Id: ImageDisplay.java,v 1.6 2007/04/23 13:40:49 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui;

import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import com.bc.swing.GraphicsPane;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.layer.RenderedImageLayer;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.MouseEventFilterFactory;
import org.esa.beam.util.SystemUtils;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Vector;

/**
 * An output widget for a <code>PlanarImage</code>. <code>ImageDisplay</code> subclasses javax.swing.JComponent, and can
 * be used in any context that calls for a <code>JComponent</code>.  It monitors component resize and update events and
 * automatically requests tiles from its source on demand.
 * <p/>
 * <p> Due to the limitations of BufferedImage, only TYPE_BYTE of band 1, 2, 3, 4, and TYPE_USHORT of band 1, 2, 3
 * images can be displayed using this widget.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see PixelPositionListener
 */
public class ImageDisplay extends GraphicsPane {

    /**
     * The current tool.
     */
    private Tool _tool;
    /**
     * A pixel info sink.
     */
    private PixelInfoFactory _pixelInfoFactory;

    private int _imageWidth;
    private int _imageHeight;

    private int _pixelX = -1;
    private int _pixelY = -1;

    // Used only when painting in tool-interaction mode
    private Graphics2D dblbufG2D;
    private BufferedImage dblbufImg;

    private Vector<PixelPositionListener> pixelPositionListeners;
    private boolean toolRepaintRequested;

    private boolean pixelBorderShown; // can it be shown?
    private boolean pixelBorderDrawn; // has it been drawn?
    private double pixelBorderViewScale;

    // todo - move to ImageLayer
    private boolean imageBorderShown;
    private Color imageBorderColor;
    private float imageBorderSize;

    private Object antialiasing;
    private Object interpolation;
    private ComponentAdapter componentAdapter;
    private MouseInputListener mouseInputListener;
    private KeyListener imageDisplayKeyListener;

//    private StopWatch _stopWatch = new StopWatch();

    /**
     * Constructs an ImageDisplay to display a given RenderedImage.
     *
     * @param image the image image
     */
    public ImageDisplay(RenderedImage image) {
        getLayerModel().addLayer(new RenderedImageLayer(image));
        setImage(image);
        postInit();
    }

    /**
     * Constructs an ImageDisplay of fixed size with no source image.
     *
     * @param imgWidth  the image width
     * @param imgHeight the image height
     */
    public ImageDisplay(int imgWidth, int imgHeight) {
        getLayerModel().addLayer(new RenderedImageLayer(null));
        setImageSize(imgWidth, imgHeight);
        postInit();
    }

    /**
     * Constructs an ImageDisplay for an existing layer model.
     *
     * @param layerModel the layer model, must not be empty and the first layer must be an instance of {@link RenderedImageLayer}.
     */
    public ImageDisplay(LayerModel layerModel) {
        if (layerModel.getLayerCount() == 0) {
            throw new IllegalArgumentException("layerModel is empty");
        }
        if (layerModel.getLayer(0) instanceof RenderedImageLayer) {
            throw new IllegalArgumentException("first layer in layerModel must be a RenderedImageLayer");
        }
        setLayerModel(layerModel);
        if (getImage() != null) {
            setImageSize(getImage().getWidth(), getImage().getHeight());
        }
        postInit();
    }

    /**
     * Last call in all constructors. Override to perform alternate initialization, but don't
     * forget to call <code>super.postInit()</code> first.
     */
    protected void postInit() {
        pixelBorderViewScale = 2.0;
        antialiasing = RenderingHints.VALUE_ANTIALIAS_OFF;
        interpolation = null;
        setForeground(Color.white);
        setBackground(Color.black);
        registerListeners();
    }

    /**
     * Gets the current tool.
     *
     * @return the current tool or <code>null</code> if no tool is active.
     */
    public Tool getTool() {
        return _tool;
    }

    /**
     * Sets the new current tool.
     *
     * @param tool the new tool or <code>null</code> if no tool shall be active.
     */
    public void setTool(Tool tool) {
        if (_tool == tool) {
            return;
        }
        Tool oldTool = _tool;
        _tool = tool;
        firePropertyChange("tool", oldTool, _tool);
    }

    public double getPixelBorderViewScale() {
        return pixelBorderViewScale;
    }

    public void setPixelBorderViewScale(double pixelBorderViewScale) {
        this.pixelBorderViewScale = pixelBorderViewScale;
    }

    public boolean isImageBorderShown() {
        return imageBorderShown;
    }

    public void setImageBorderShown(boolean imageBorderShown) {
        this.imageBorderShown = imageBorderShown;
    }

    public Color getImageBorderColor() {
        return imageBorderColor;
    }

    public void setImageBorderColor(Color imageBorderColor) {
        this.imageBorderColor = imageBorderColor;
    }

    public float getImageBorderSize() {
        return imageBorderSize;
    }

    public void setImageBorderSize(float imageBorderSize) {
        this.imageBorderSize = imageBorderSize;
    }

    public boolean isPixelBorderShown() {
        return pixelBorderShown;
    }

    public void setPixelBorderShown(boolean pixelBorderShown) {
        this.pixelBorderShown = pixelBorderShown;
    }

    public Object getAntialiasing() {
        return antialiasing;
    }

    public void setAntialiasing(Object antialiasing) {
        this.antialiasing = antialiasing;
    }

    public Object getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(Object interpolation) {
        this.interpolation = interpolation;
    }

    public PixelInfoFactory getPixelInfoFactory() {
        return _pixelInfoFactory;
    }

    public void setPixelInfoFactory(PixelInfoFactory pixelInfoFactory) {
        PixelInfoFactory oldFactory = _pixelInfoFactory;
        if (oldFactory == pixelInfoFactory) {
            return;
        }
        _pixelInfoFactory = pixelInfoFactory;
        firePropertyChange("pixelInfoFactory", oldFactory, _pixelInfoFactory);
    }

    /**
     * Gets the source image.
     *
     * @return the source image, can be <code>null</code>
     */
    public RenderedImage getImage() {
        return getImageLayer().getImage();
    }

    /**
     * Sets the source image to a new <code>RenderedImage</code> and updates the internal state of this image display
     * component due to to the new image properties.
     *
     * @param image the new source image
     */
    public void setImage(RenderedImage image) {
        Guardian.assertNotNull("image", image);

        setImageSize(image.getWidth(), image.getHeight());

        RenderedImage oldImage = getImage();
        if (oldImage == image) {
            return;
        }

        getImageLayer().setImage(image);

        repaint();
        firePropertyChange("image", oldImage, getImage());
    }

    /**
     * Sets the size of the image to be displayed.
     *
     * @param imgWidth  the image width
     * @param imgHeight the image height
     */
    public void setImageSize(int imgWidth, int imgHeight) {
        if (getImage() != null
                && (imgWidth != getImage().getWidth()
                || imgHeight != getImage().getHeight())) {
            throw new IllegalArgumentException("invalid image size");
        }
        _imageWidth = imgWidth;
        _imageHeight = imgHeight;
        final Rectangle modelArea = new Rectangle(0, 0, _imageWidth, _imageHeight);
        if (getViewModel().getModelArea().isEmpty()) {
            getViewModel().setModelArea(modelArea);
        }
        if (!isPreferredSizeSet()) {
            setPreferredSize(modelArea.getSize());
        }
    }

    /**
     * Returns the source image's width.
     */
    public int getImageWidth() {
        return _imageWidth;
    }

    /**
     * Returns the source image's height.
     */
    public int getImageHeight() {
        return _imageHeight;
    }

    /**
     * Moves and resizes this component. The new location of the top-left corner is specified by <code>x</code> and
     * <code>y</code>, and the new size is specified by <code>width</code> and <code>height</code>.
     *
     * @param x      The new <i>x</i>-coordinate of this component.
     * @param y      The new <i>y</i>-coordinate of this component.
     * @param width  The new <code>width</code> of this component.
     * @param height The new <code>height</code> of this component.
     * @see java.awt.Component#getBounds
     * @see java.awt.Component#setLocation(int,int)
     * @see java.awt.Component#setLocation(java.awt.Point)
     * @see java.awt.Component#setSize(int,int)
     * @see java.awt.Component#setSize(java.awt.Dimension)
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        if (dblbufImg != null
                && (dblbufImg.getWidth() != width
                || dblbufImg.getHeight() != height)) {
            dblbufImg = null;
            dblbufG2D = null;
        }
        super.setBounds(x, y, width, height);
    }

    /**
     * Sets the internal source references to <code>null</code>. This should help the garbage collector to quickly
     * release image resources.
     * <p/>
     * <p> The results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public synchronized void dispose() {
        super.dispose();
        dblbufImg = null;
        if (dblbufG2D != null) {
            dblbufG2D.dispose();
            dblbufG2D = null;
        }
        _tool = null;
        _pixelInfoFactory = null;
        deregisterListeners();
        if (pixelPositionListeners != null) {
            pixelPositionListeners.clear();
            pixelPositionListeners = null;
        }
    }


    public void copyPixelInfoStringToClipboard() {
        copyPixelInfoStringToClipboard(_pixelX, _pixelY);
    }


    public void copyPixelInfoStringToClipboard(int pixelX, int pixelY) {
        if (getPixelInfoFactory() != null) {
            String text = getPixelInfoFactory().createPixelInfoString(pixelX, pixelY);
            SystemUtils.copyToClipboard(text);
        }
    }

    /**
     * Paint the image onto a Graphics object.  The painting is performed tile-by-tile, and includes a grey region
     * covering the unused portion of image tiles as well as the general background.  At this point the image must be
     * byte data.
     */
    @Override
    public synchronized void paintComponent(Graphics g) {

//        if (Debug.isEnabled()) {
//            _stopWatch.start();
//        }

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2d;
        if (g instanceof Graphics2D) {
            g2d = (Graphics2D) g;
        } else {
            return;
        }

        if (toolRepaintRequested) {
            if (dblbufImg == null
                    || dblbufImg.getWidth() != width
                    || dblbufImg.getHeight() != height) {
                dblbufImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                dblbufG2D = dblbufImg.createGraphics();
                draw(dblbufG2D);
            }
            g2d.drawImage(dblbufImg, null, 0, 0);
            drawTool(g2d);
        } else {
            dblbufImg = null;
            dblbufG2D = null;
            draw(g2d);
            drawTool(g2d);
        }

//        if (Debug.isEnabled()) {
//            _stopWatch.stop();
//            Debug.trace("ImageDisplay.paintComponent: paint took " + _stopWatch);
//        }

        toolRepaintRequested = false;
    }

    protected void draw(Graphics2D g2d) {
        Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldInterpolation = g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();

        drawBackground(g2d);

        transformGraphics(g2d, true);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        if (interpolation != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        }
        drawImage(g2d);

        if (antialiasing != null) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
        }
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        drawLayers(g2d);

        transformGraphics(g2d, false);

        if (oldAntialias != null) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
        }
        if (oldInterpolation != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }

    @Override
    public void drawLayers(Graphics2D g2d) {
        final LayerModel layerModel = getLayerModel();
        final int layerCount = layerModel.getLayerCount();
        for (int i = 1; i < layerCount; i++) {
            final Layer layer = layerModel.getLayer(i);
            if (layer.isVisible()) {
                layer.draw(g2d, getViewModel());
            }
        }
    }

    private void drawBackground(Graphics2D g2d) {
        if (isOpaque()) {
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    protected void drawImage(Graphics2D g2d) {

        if (getImage() == null) {
            drawMissingImageText(g2d);
            return;
        }

        getImageLayer().draw(g2d, getViewModel());
        drawImageBorder(g2d);
    }

    private void drawMissingImageText(Graphics2D g2d) {
        FontMetrics fm = g2d.getFontMetrics();
        int textX = 10;
        int textY = 10 + fm.getHeight();
        g2d.setColor(getForeground());
        g2d.drawString("No image.", textX, textY);
    }

    protected void drawImageBorder(Graphics2D g2d) {
        if (imageBorderShown) {
            g2d.setStroke(new BasicStroke(imageBorderSize));
            g2d.setColor(imageBorderColor);
            g2d.drawRect(-1, -1, getImage().getWidth() + 2, getImage().getHeight() + 2);
        }
    }

    protected void drawTool(Graphics2D g2d) {
        if (_tool != null && _tool.isActive()) {
            transformGraphics(g2d, true);
            drawToolNoTransf(g2d);
            transformGraphics(g2d, false);
        }
    }

    private void drawPixelBorder(int pixelX, int pixelY, boolean showBorder) {
        final Graphics g = getGraphics();
        g.setXORMode(Color.white);
        if (pixelBorderDrawn) {
            drawPixelBorder(g, _pixelX, _pixelY);
            pixelBorderDrawn = false;
        }
        if (showBorder) {
            drawPixelBorder(g, pixelX, pixelY);
            pixelBorderDrawn = true;
        }
        g.setPaintMode();
        g.dispose();
    }

    private void drawPixelBorder(final Graphics g, final int pixelX, final int pixelY) {
        g.drawRect((int) Math.round(modelToViewX(pixelX)) - 1,
                   (int) Math.round(modelToViewY(pixelY)) - 1,
                   (int) Math.round(modelToViewLength(1.0)) + 2,
                   (int) Math.round(modelToViewLength(1.0)) + 2);
    }

    private boolean isPixelBorderDisplayEnabled() {
        return pixelBorderShown &&
                (getTool() == null || getTool().getDrawable() == null) &&
                getViewModel().getViewScale() >= pixelBorderViewScale;
    }

    public final void drawToolNoTransf(Graphics2D g2d) {
        if (_tool != null) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (_tool.getDrawable() != null) {
                _tool.getDrawable().draw(g2d);
            }
        }
    }

    /**
     * Draws the tool in an interactive mode.
     */
    public final void repaintTool() {
        if (_tool != null) {
            toolRepaintRequested = true;
            repaint(100);
        }
    }

    /**
     * Displays a status message. If <code>null</code> is passed to this method, the status message is reset or
     * cleared.
     *
     * @param message the message to be displayed
     */
    public final void setStatusMessage(String message) {
        Debug.traceMethodNotImplemented(getClass(), "setStatusMessage");
    }

    private void setPixelPos(MouseEvent e, boolean showBorder) {
        Point p = e.getPoint();
        int pixelX = (int) Math.floor(viewToModelX(p.x));
        int pixelY = (int) Math.floor(viewToModelY(p.y));
        if (pixelX != _pixelX || pixelY != _pixelY) {
            if (isPixelBorderDisplayEnabled() && (showBorder || pixelBorderDrawn)) {
                drawPixelBorder(pixelX, pixelY, showBorder);
            }
            setPixelPos(e, pixelX, pixelY);
        }
    }


    final void setPixelPos(MouseEvent e, int pixelX, int pixelY) {
        _pixelX = pixelX;
        _pixelY = pixelY;
        if (e.getID() != MouseEvent.MOUSE_EXITED) {
            firePixelPosChanged(e, _pixelX, _pixelY);
        } else {
            firePixelPosNotAvailable();
        }
    }

    /**
     * Adds a new pixel position listener to this image display component. If the component already contains the given
     * listener, the method does nothing.
     *
     * @param listener the pixel position listener to be added
     */
    public final void addPixelPositionListener(PixelPositionListener listener) {
        if (listener == null) {
            return;
        }
        if (pixelPositionListeners == null) {
            pixelPositionListeners = new Vector<PixelPositionListener>();
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
        if (listener == null || pixelPositionListeners == null) {
            return;
        }
        pixelPositionListeners.remove(listener);
    }

    final synchronized void fireToolEvent(MouseEvent e) {
//        Debug.trace("fireToolEvent "  + e);
        if (_tool != null) {
            ToolInputEvent toolInputEvent = createToolInputEvent(e);
            _tool.handleEvent(toolInputEvent);
        }
    }

    private ToolInputEvent createToolInputEvent(MouseEvent e) {
        return new ToolInputEvent(this, e, _pixelX, _pixelY, isPixelPosValid(_pixelX, _pixelY));
    }

    private ToolInputEvent createToolInputEvent(KeyEvent e) {
        return new ToolInputEvent(this, e, _pixelX, _pixelY, isPixelPosValid(_pixelX, _pixelY));
    }

    public final boolean isPixelPosValid(int pixelX, int pixelY) {
        return pixelX >= 0 && pixelX < getImage().getWidth()
                && pixelY >= 0 && pixelY < getImage().getHeight();
    }

    /**
     * Fires a 'pixel position changed' event to all registered pixel-pos listeners.
     */
    protected final void firePixelPosChanged(MouseEvent e, int pixelX, int pixelY) {
        if (pixelPositionListeners != null) {
            PixelPositionListener[] listeners = this.pixelPositionListeners.toArray(new PixelPositionListener[0]);
            boolean pixelPosValid = isPixelPosValid(pixelX, pixelY);
            for (PixelPositionListener listener : listeners) {
                listener.pixelPosChanged(getImage(), pixelX, pixelY, pixelPosValid, e);
            }
        }
    }

    /**
     * Fires a 'pixel position is invalid' event to all registered listeners.
     */
    protected final void firePixelPosNotAvailable() {
        if (pixelPositionListeners != null) {
            PixelPositionListener[] listeners = this.pixelPositionListeners.toArray(new PixelPositionListener[0]);
            for (PixelPositionListener listener : listeners) {
                listener.pixelPosNotAvailable(getImage());
            }
        }
    }

    private RenderedImageLayer getImageLayer() {
        return ((RenderedImageLayer) getLayerModel().getLayer(0));
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
        mouseInputListener = MouseEventFilterFactory.createFilter(pixelposUpdater);
        addMouseListener(mouseInputListener);
        addMouseMotionListener(mouseInputListener);
    }

    private void registerKeyListeners() {

        imageDisplayKeyListener = new KeyListener() {

            /**
             * Invoked when a key has been pressed.
             */
            public void keyPressed(KeyEvent e) {
                if (_tool != null) {
                    _tool.handleEvent(createToolInputEvent(e));
                }
            }

            /**
             * Invoked when a key has been released.
             */
            public void keyReleased(KeyEvent e) {
                if (_tool != null) {
                    _tool.handleEvent(createToolInputEvent(e));
                }
            }

            /**
             * Invoked when a key has been typed. This event occurs when a key press is followed by a key dispose.
             */
            public void keyTyped(KeyEvent e) {
                if (_tool != null) {
                    _tool.handleEvent(createToolInputEvent(e));
                }
            }
        };
        addKeyListener(imageDisplayKeyListener);
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
         * Invoked when a mouse button is pressed on a component and then dragged.  Mouse drag events will continue
         * to be delivered to the component where the first originated until the mouse button is released
         * (regardless of whether the mouse position is within the bounds of the component).
         */
        public final void mouseDragged(MouseEvent e) {
            updatePixelPos(e, true);
        }

        /**
         * Invoked when the mouse button has been moved on a component (with no buttons no down).
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
