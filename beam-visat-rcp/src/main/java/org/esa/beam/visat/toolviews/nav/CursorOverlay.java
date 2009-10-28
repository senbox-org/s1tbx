package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
class CursorOverlay implements LayerCanvas.Overlay {

    private static final int MAX_CROSSHAIR_SIZE = 20;

    private final ProductSceneView sceneView;
    private GeoPos geoPosition;

    CursorOverlay(ProductSceneView sceneView, GeoPos geoPos) {
        this.sceneView = sceneView;
        geoPosition = geoPos;
    }

    public void setGeoPosition(GeoPos geoPosition) {
        this.geoPosition = geoPosition;
    }

    @Override
    public void paintOverlay(LayerCanvas canvas, Graphics2D graphics) {
        if (geoPosition == null || !geoPosition.isValid()) {
            return;
        }

        final GeoCoding geoCoding = sceneView.getRaster().getGeoCoding();
        if (!geoCoding.canGetPixelPos()) {
            return;
        }
        final Product product = sceneView.getRaster().getProduct();
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPosition, null);
        if (!pixelPos.isValid() || !product.containsPixel(pixelPos)) {
            return;
        }

        final Viewport viewport = canvas.getViewport();

        drawCursor(graphics, viewport, pixelPos);
    }

    private void drawCursor(Graphics2D graphics, Viewport viewport, PixelPos pixelPos) {
        AffineTransform i2mTransform = sceneView.getBaseImageLayer().getImageToModelTransform();
        AffineTransform m2vTransform = viewport.getModelToViewTransform();
        AffineTransform i2vTransform = new AffineTransform(m2vTransform);
        i2vTransform.concatenate(i2mTransform);

        Point centerPixel = new Point((int) Math.floor(pixelPos.x), (int) Math.floor(pixelPos.y));
        Rectangle pixelImageRect = new Rectangle(centerPixel, new Dimension(1, 1));

        Rectangle2D pixelViewRect = i2vTransform.createTransformedShape(pixelImageRect).getBounds2D();
        graphics.setColor(Color.WHITE);
        graphics.setXORMode(Color.BLACK);
        graphics.draw(pixelViewRect);

        if (pixelViewRect.getBounds2D().getWidth() < MAX_CROSSHAIR_SIZE) {
            drawCrosshair(graphics, i2vTransform, centerPixel, pixelViewRect);
        }
    }

    private void drawCrosshair(Graphics2D graphics, AffineTransform i2vTransform, Point centerPixel,
                               Rectangle2D pixelViewRect) {
        Rectangle surroundImageRect = new Rectangle(centerPixel.x - 1, centerPixel.y - 1, 3, 3);
        Rectangle2D surroundViewRect = i2vTransform.createTransformedShape(surroundImageRect).getBounds2D();
        double scale = MAX_CROSSHAIR_SIZE / surroundViewRect.getBounds2D().getWidth();
        if (scale > 1) {
            double newWidth = surroundViewRect.getWidth() * scale;
            double newHeight = surroundViewRect.getHeight() * scale;
            double newX = surroundViewRect.getCenterX() - newWidth / 2;
            double newY = surroundViewRect.getCenterY() - newHeight / 2;
            surroundViewRect.setRect(newX, newY, newWidth, newHeight);
        }
        graphics.draw(surroundViewRect);

        Line2D.Double northLine = new Line2D.Double(surroundViewRect.getCenterX(), surroundViewRect.getMinY(),
                                                    surroundViewRect.getCenterX(), pixelViewRect.getMinY());
        Line2D.Double eastLine = new Line2D.Double(surroundViewRect.getMaxX(), surroundViewRect.getCenterY(),
                                                   pixelViewRect.getMaxX(), surroundViewRect.getCenterY());
        Line2D.Double southLine = new Line2D.Double(surroundViewRect.getCenterX(), surroundViewRect.getMaxY(),
                                                    surroundViewRect.getCenterX(), pixelViewRect.getMaxY());
        Line2D.Double westLine = new Line2D.Double(surroundViewRect.getMinX(), surroundViewRect.getCenterY(),
                                                   pixelViewRect.getMinX(), surroundViewRect.getCenterY());
        graphics.draw(northLine);
        graphics.draw(eastLine);
        graphics.draw(southLine);
        graphics.draw(westLine);
    }

}
