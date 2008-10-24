/*
 * Created at 12.07.2004 06:45:55 Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class NavigationCanvas1 extends NavigationCanvas {

    private BufferedImage thumbnailImage;
    private BufferedImageRendering imageRendering;

    /**
     * Currently visible model area in view coordinates
     */
    private Rectangle visibleArea;
    /**
     * Model area in view coordinates
     */
    private Rectangle area;

    private boolean updatingImageDisplay;

    public NavigationCanvas1(NavigationToolView navigationWindow) {
        super(navigationWindow);
        visibleArea = new Rectangle(0, 0, 0, 0);
        area = new Rectangle(0, 0, 0, 0);
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    @Override
    public boolean isUpdatingImageDisplay() {
        return updatingImageDisplay;
    }

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
            final AffineTransform transform = AffineTransform.getTranslateInstance(area.x, area.y);
            ((Graphics2D) g).drawRenderedImage(thumbnailImage, transform);

            g.setColor(new Color(getForeground().getRed(), getForeground().getGreen(),
                                 getForeground().getBlue(), 82));
            g.fillRect(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height);
            g.setColor(getForeground());
            g.draw3DRect(visibleArea.x - 1, visibleArea.y - 1, visibleArea.width + 2, visibleArea.height + 2, true);
            g.draw3DRect(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height, false);
        }
    }

    @Override
    public void handleViewChanged() {
        updateImage();
    }

    @Override
    public void updateImage() {
        final ProductSceneView view = getNavigationWindow().getCurrentView();
        if (view != null) {
            final Insets insets = getInsets();
            int imageWidth = getWidth() - (insets.left + insets.right);
            int imageHeight = getHeight() - (insets.top + insets.bottom);
            final double imageRatio = (double) imageWidth / (double) imageHeight;
            final Rectangle2D tma = view.getLayerCanvas().getMaxVisibleModelBounds();
            final double modelRatio = tma.getWidth() / tma.getHeight();
            if (imageRatio < modelRatio) {
                imageHeight = (int) Math.round(imageWidth / modelRatio);
            } else {
                imageWidth = (int) Math.round(imageHeight * modelRatio);
            }
            boolean isModelYAxisDown = view.getLayerCanvas().getViewport().isModelYAxisDown();
            if (imageWidth > 0 && imageHeight > 0) {
                if (thumbnailImage == null
                        || thumbnailImage.getWidth() != imageWidth
                        || thumbnailImage.getHeight() != imageHeight
                        || imageRendering.getViewport().isModelYAxisDown() != isModelYAxisDown) {
                    thumbnailImage = new BufferedImage(imageWidth, imageHeight,
                                                       BufferedImage.TYPE_3BYTE_BGR);
                    imageRendering = new BufferedImageRendering(thumbnailImage, new DefaultViewport(isModelYAxisDown));
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

    @Override
    public void updateSlider() {
        final ProductSceneView view = getNavigationWindow().getCurrentView();
        if (updatingImageDisplay || view == null) {
            return;
        }
        visibleArea.setRect(getViewportThumbnailBounds(view, area));
        repaint();
    }

    private void updateImageDisplay() {
        final ProductSceneView view = getNavigationWindow().getCurrentView();
        if (view != null && thumbnailImage != null) {
            // todo - ask nf to explain why offset is needed (rq)
            final double x = visibleArea.x - area.x;
            final double y = visibleArea.y - area.y;
            final Point2D point = new Point.Double(x, y);

            imageRendering.getViewport().getViewToModelTransform().transform(point, point);
            updatingImageDisplay = true;
            getNavigationWindow().setModelOffset(point.getX(), point.getY());
            updatingImageDisplay = false;
        }
    }

    private void updateSliderArea() {
        if (thumbnailImage == null) {
            return;
        }
        area.x = 0;
        area.y = 0;
        area.width = thumbnailImage.getWidth();
        area.height = thumbnailImage.getHeight();
        if (thumbnailImage.getWidth() < getWidth()) {
            area.x = (getWidth() - thumbnailImage.getWidth()) / 2;
        }
        if (thumbnailImage.getHeight() < getHeight()) {
            area.y = (getHeight() - thumbnailImage.getHeight()) / 2;
        }
    }

    private void updateImageContent() {
        final ProductSceneView view = getNavigationWindow().getCurrentView();
        if (view == null || thumbnailImage == null) {
            return;
        }
        renderThumbnail(view);
    }

    private void renderThumbnail(ProductSceneView view) {
        final Graphics2D graphics = imageRendering.getGraphics();
        graphics.setBackground(getBackground());
        graphics.clearRect(0, 0, thumbnailImage.getWidth(), thumbnailImage.getHeight());
        configureThumbnailViewport(view.getLayerCanvas(), imageRendering.getViewport());
        view.getRootLayer().render(imageRendering, new Layer.RenderFilter() {
            @Override
            public boolean canRender(Layer layer) {
                return layer instanceof ImageLayer;
            }
        });
    }

    static Rectangle getViewportThumbnailBounds(ProductSceneView view, Rectangle thumbnailArea) {
        Viewport vwViewport = view.getLayerCanvas().getViewport();
        Viewport tnViewport = new DefaultViewport(thumbnailArea, vwViewport.isModelYAxisDown());
        configureThumbnailViewport(view.getLayerCanvas(), tnViewport);

        AffineTransform vwV2M = vwViewport.getViewToModelTransform();
        AffineTransform tnM2V = tnViewport.getModelToViewTransform();

        Rectangle vwViewBounds = vwViewport.getViewBounds();
        Point2D vwViewOffset = vwViewBounds.getLocation();
        Point2D vwModelOffset = vwV2M.transform(vwViewOffset, null);
        Point2D tnViewOffset = tnM2V.transform(vwModelOffset, null);
        double scale = Math.sqrt(Math.abs(vwV2M.getDeterminant())) * Math.sqrt(Math.abs(tnM2V.getDeterminant()));
        return new Rectangle((int) Math.floor(tnViewOffset.getX()),
                             (int) Math.floor(tnViewOffset.getY()),
                             (int) Math.floor(vwViewBounds.width * scale),
                             (int) Math.floor(vwViewBounds.height * scale));
    }

    static void configureThumbnailViewport(AdjustableView view, Viewport thumbnailViewport) {
        thumbnailViewport.zoom(view.getMaxVisibleModelBounds());
        thumbnailViewport.moveViewDelta(thumbnailViewport.getViewBounds().x,
                                        thumbnailViewport.getViewBounds().y);
        thumbnailViewport.setOrientation(view.getViewport().getOrientation());
    }

    private class MouseHandler extends MouseInputAdapter {

        private Point _pickPoint;
        private Point _sliderPoint;

        @Override
        public void mousePressed(MouseEvent e) {
            _pickPoint = e.getPoint();
            if (!visibleArea.contains(_pickPoint)) {
                visibleArea.x = _pickPoint.x - visibleArea.width / 2;
                visibleArea.y = _pickPoint.y - visibleArea.height / 2;
                repaint();
            }
            _sliderPoint = visibleArea.getLocation();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateImageDisplay();
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            visibleArea.x = _sliderPoint.x + (e.getX() - _pickPoint.x);
            visibleArea.y = _sliderPoint.y + (e.getY() - _pickPoint.y);
            updateImageDisplay();
            repaint();
        }
    }
}
