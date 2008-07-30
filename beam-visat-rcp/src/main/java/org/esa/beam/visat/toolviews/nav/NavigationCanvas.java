/*
 * Created at 12.07.2004 06:45:55
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.visat.toolviews.nav;

import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class NavigationCanvas extends JPanel {

    private NavigationToolView navigationWindow;
    private BufferedImage thumbnailImage;

    /**
     * currently visible model area in canvas coordinates
     */
    private Rectangle slider;
    /**
     * model area in canvas coordinates
     */
    private Rectangle sliderArea;
    private boolean updatingImageDisplay;

    public NavigationCanvas(NavigationToolView navigationWindow) {
        super(null, true);
        this.navigationWindow = navigationWindow;
        slider = new Rectangle(0, 0, 0, 0);
        sliderArea = new Rectangle(0, 0, 0, 0);
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public boolean isUpdatingImageDisplay() {
        return updatingImageDisplay;
    }

    public Rectangle getSlider() {
        return slider;
    }

    public void setSlider(Rectangle slider) {
        Rectangle oldSlider = this.slider;
        this.slider = new Rectangle(slider);
        firePropertyChange("slider", oldSlider, this.slider);
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
        if (thumbnailImage != null) {
            final AffineTransform transform = AffineTransform.getTranslateInstance(sliderArea.x, sliderArea.y);
            ((Graphics2D) g).drawRenderedImage(thumbnailImage, transform);

            g.setColor(new Color(getForeground().getRed(), getForeground().getGreen(), getForeground().getBlue(), 82));
            g.fillRect(slider.x, slider.y, slider.width, slider.height);
            g.setColor(getForeground());
            g.draw3DRect(slider.x - 1, slider.y - 1, slider.width + 2, slider.height + 2, true);
            g.draw3DRect(slider.x, slider.y, slider.width, slider.height, false);
        }
    }

    private void updateImageDisplay() {
        // TODO IMAGING 4.5
        final ProductSceneView view = navigationWindow.getCurrentView();
        if (view != null && thumbnailImage != null) {
            final Rectangle2D ma = view.getModelBounds();
            double mpX = ma.getX() + (slider.x - sliderArea.x) * ma.getWidth() / sliderArea.width;
            double mpY = ma.getY() + (slider.y - sliderArea.y) * ma.getHeight() / sliderArea.height;
            updatingImageDisplay = true;
            navigationWindow.setModelOffset(mpX, mpY);
            updatingImageDisplay = false;
        }
    }

    public void updateImage() {
        final ProductSceneView view = navigationWindow.getCurrentView();
        if (view != null) {
            final Insets insets = getInsets();
            int imageWidth = getWidth() - (insets.left + insets.right);
            int imageHeight = getHeight() - (insets.top + insets.bottom);
            final double imageRatio = (double) imageWidth / (double) imageHeight;
            final Rectangle2D ma = view.getModelBounds();
            final double modelRatio = ma.getWidth() / ma.getHeight();
            if (imageRatio < modelRatio) {
                imageHeight = (int) Math.round(imageWidth / modelRatio);
            } else {
                imageWidth = (int) Math.round(imageHeight * modelRatio);
            }
            if (imageWidth > 0 && imageHeight > 0) {
                if (thumbnailImage == null ||
                        thumbnailImage.getWidth() != imageWidth ||
                        thumbnailImage.getHeight() != imageHeight) {
                    thumbnailImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
                }
                updateImageContent();
                updateSliderArea();
                updateSlider();
            } else {
                thumbnailImage = null;
            }
        } else {
            thumbnailImage = null;
        }
        // needed to update if last ProductSceneView is closed
        updateUI();
    }

    public void updateSlider() {
        // TODO IMAGING 4.5
        final ProductSceneView view = navigationWindow.getCurrentView();
        if (updatingImageDisplay || view == null) {
            return;
        }
        final Rectangle2D va = view.getVisibleModelBounds();
        final Rectangle2D ma = view.getModelBounds();

        slider.x = sliderArea.x + (int) Math.round(sliderArea.width * (va.getX() - ma.getX()) / ma.getWidth());
        slider.y = sliderArea.y + (int) Math.round(sliderArea.height * (va.getY() - ma.getY()) / ma.getHeight());
        slider.width = (int) Math.round(sliderArea.width * va.getWidth() / ma.getWidth());
        slider.height = (int) Math.round(sliderArea.height * va.getHeight() / ma.getHeight());
        repaint();
    }

    private void updateSliderArea() {
        if (thumbnailImage == null) {
            return;
        }
        sliderArea.x = 0;
        sliderArea.y = 0;
        sliderArea.width = thumbnailImage.getWidth();
        sliderArea.height = thumbnailImage.getHeight();
        if (thumbnailImage.getWidth() < getWidth()) {
            sliderArea.x = (getWidth() - thumbnailImage.getWidth()) / 2;
        }
        if (thumbnailImage.getHeight() < getHeight()) {
            sliderArea.y = (getHeight() - thumbnailImage.getHeight()) / 2;
        }
    }

    private void updateImageContent() {
        // Will to this totally different later!!! (Use max. level image of LevelImage).
        final ProductSceneView view = navigationWindow.getCurrentView();
        if (view == null || thumbnailImage == null) {
            return;
        }
        view.renderThumbnail(thumbnailImage);
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
            if (!slider.contains(_pickPoint)) {
                slider.x = _pickPoint.x - slider.width / 2;
                slider.y = _pickPoint.y - slider.height / 2;
                repaint();
            }
            _sliderPoint = slider.getLocation();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateImageDisplay();
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            slider.x = _sliderPoint.x + (e.getX() - _pickPoint.x);
            slider.y = _sliderPoint.y + (e.getY() - _pickPoint.y);
            updateImageDisplay();
            repaint();
        }
    }
}
