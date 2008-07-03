/*
 * Created at 12.07.2004 06:45:55
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.visat.toolviews.nav;

import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.util.Debug;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class NavigationCanvas extends JPanel {

    private NavigationToolView _navigationWindow;
    private BufferedImage _thumbnail;

    /**
     * currently visible model area in canvas coordinates
     */
    private Rectangle _slider;
    /**
     * model area in canvas coordinates
     */
    private Rectangle _sliderArea;
    private boolean _updatingImageDisplay;

    public NavigationCanvas(NavigationToolView navigationWindow) {
        super(null, true);
        _navigationWindow = navigationWindow;
        _slider = new Rectangle(0, 0, 0, 0);
        _sliderArea = new Rectangle(0, 0, 0, 0);
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public boolean isUpdatingImageDisplay() {
        return _updatingImageDisplay;
    }

    public Rectangle getSlider() {
        return _slider;
    }

    public void setSlider(Rectangle slider) {
        Rectangle oldSlider = _slider;
        _slider = new Rectangle(slider);
        firePropertyChange("slider", oldSlider, _slider);
    }

    /**
     * Causes this container to lay out its components.  Most programs should not call this method directly, but should
     * invoke the <code>validate</code> method instead.
     *
     * @see java.awt.LayoutManager#layoutContainer
     * @see #setLayout
     * @see #validate
     * @since JDK1.1
     */
    @Override
    public void doLayout() {
        super.doLayout();
        updateImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (_thumbnail != null) {
            final AffineTransform transform = AffineTransform.getTranslateInstance(_sliderArea.x, _sliderArea.y);
            ((Graphics2D) g).drawRenderedImage(_thumbnail, transform);

            g.setColor(new Color(getForeground().getRed(), getForeground().getGreen(), getForeground().getBlue(), 82));
            g.fillRect(_slider.x, _slider.y, _slider.width, _slider.height);
            g.setColor(getForeground());
            g.draw3DRect(_slider.x - 1, _slider.y - 1, _slider.width + 2, _slider.height + 2, true);
            g.draw3DRect(_slider.x, _slider.y, _slider.width, _slider.height, false);
        }
    }

    private void updateImageDisplay() {
        final ImageDisplay imageDisplay = _navigationWindow.getCurrentImageDisplay();
        if (imageDisplay != null && _thumbnail != null) {
            final Rectangle2D ma = imageDisplay.getViewModel().getModelArea();
            double mpX = ma.getX() + (_slider.x - _sliderArea.x) * ma.getWidth() / _sliderArea.width;
            double mpY = ma.getY() + (_slider.y - _sliderArea.y) * ma.getHeight() / _sliderArea.height;
            _updatingImageDisplay = true;
            _navigationWindow.setModelOffset(mpX, mpY);
            _updatingImageDisplay = false;
        }
    }

    public void updateImage() {
        final ImageDisplay imageDisplay = _navigationWindow.getCurrentImageDisplay();
        if (imageDisplay != null) {
            final Insets insets = getInsets();
            int imageWidth = getWidth() - (insets.left + insets.right);
            int imageHeight = getHeight() - (insets.top + insets.bottom);
            final double imageRatio = (double) imageWidth / (double) imageHeight;
            final Rectangle2D ma = imageDisplay.getViewModel().getModelArea();
            final double modelRatio = ma.getWidth() / ma.getHeight();
            if (imageRatio < modelRatio) {
                imageHeight = (int) Math.round(imageWidth / modelRatio);
            } else {
                imageWidth = (int) Math.round(imageHeight * modelRatio);
            }
            if (imageWidth > 0 && imageHeight > 0) {
                if (_thumbnail == null ||
                        _thumbnail.getWidth() != imageWidth ||
                        _thumbnail.getHeight() != imageHeight) {
                    _thumbnail = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
                }
                updateImageContent();
                updateSliderArea();
                updateSlider();
            } else {
                _thumbnail = null;
            }
        } else {
            _thumbnail = null;
        }
        // needed to update if last ProductSceneView is closed
        updateUI();
    }

    public void updateSlider() {
        final ImageDisplay imageDisplay = _navigationWindow.getCurrentImageDisplay();
        if (_updatingImageDisplay || imageDisplay == null) {
            return;
        }
        final Rectangle2D ma = imageDisplay.getViewModel().getModelArea();
        final double vs = imageDisplay.getViewModel().getViewScale();
        int width = imageDisplay.getWidth();
        int height = imageDisplay.getHeight();
        final Rectangle2D va = new Rectangle2D.Double(imageDisplay.getViewModel().getModelOffsetX(),
                                                      imageDisplay.getViewModel().getModelOffsetY(),
                                                      width / vs, height / vs);

        _slider.x = _sliderArea.x + (int) Math.round(_sliderArea.width * (va.getX() - ma.getX()) / ma.getWidth());
        _slider.y = _sliderArea.y + (int) Math.round(_sliderArea.height * (va.getY() - ma.getY()) / ma.getHeight());
        _slider.width = (int) Math.round(_sliderArea.width * va.getWidth() / ma.getWidth());
        _slider.height = (int) Math.round(_sliderArea.height * va.getHeight() / ma.getHeight());
        repaint();
    }

    private void updateSliderArea() {
        if (_thumbnail == null) {
            return;
        }
        _sliderArea.x = 0;
        _sliderArea.y = 0;
        _sliderArea.width = _thumbnail.getWidth();
        _sliderArea.height = _thumbnail.getHeight();
        if (_thumbnail.getWidth() < getWidth()) {
            _sliderArea.x = (getWidth() - _thumbnail.getWidth()) / 2;
        }
        if (_thumbnail.getHeight() < getHeight()) {
            _sliderArea.y = (getHeight() - _thumbnail.getHeight()) / 2;
        }
    }

    private void updateImageContent() {
        final ImageDisplay imageDisplay = _navigationWindow.getCurrentImageDisplay();
        if (imageDisplay == null || _thumbnail == null) {
            return;
        }
        final Graphics2D graphics = _thumbnail.createGraphics();
        final ImageDisplay painter = new ImageDisplay(imageDisplay.getImage());
        painter.setSize(_thumbnail.getWidth(), _thumbnail.getHeight());
        painter.setOpaque(true);
        painter.setBackground(imageDisplay.getBackground());
        painter.setForeground(imageDisplay.getForeground());
        painter.getViewModel().setViewScaleMax(null);
        painter.getViewModel().setModelArea(imageDisplay.getViewModel().getModelArea());
        painter.zoomAll();
        painter.paintComponent(graphics);
        painter.dispose();
        graphics.dispose();
    }

    /**
     * This method ignores the given parameter.
     * It is an empty implementation to prevent from setting borders on this canvas.
     *
     * @param border is ignored
     */
    @Override
    public void setBorder(Border border) {
        if (border != null) {
            BeamLogManager.getSystemLogger().warning("NavigationCanvas.setBorder() called with " +
                    border.getClass().getCanonicalName());
            BeamLogManager.getSystemLogger().warning("borders not allowed");
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        private Point _pickPoint;
        private Point _sliderPoint;

        @Override
        public void mousePressed(MouseEvent e) {
            _pickPoint = e.getPoint();
            if (!_slider.contains(_pickPoint)) {
                _slider.x = _pickPoint.x - _slider.width / 2;
                _slider.y = _pickPoint.y - _slider.height / 2;
                repaint();
            }
            _sliderPoint = _slider.getLocation();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateImageDisplay();
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            _slider.x = _sliderPoint.x + (e.getX() - _pickPoint.x);
            _slider.y = _sliderPoint.y + (e.getY() - _pickPoint.y);
            updateImageDisplay();
            repaint();
        }
    }
}
