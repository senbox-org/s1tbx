package org.esa.beam.framework.ui;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

/**
 * This class is an overlay that draws products from a {@link WorldMapPaneDataModel} and lets client decide how to
 * render the selected product.
 *
 * @author Thomas Storm
 * @author Tonio Fincke
 * @author Marco Peters
 */
public abstract class BoundaryOverlay implements LayerCanvas.Overlay {

    private final WorldMapPaneDataModel dataModel;
    private LayerCanvas layerCanvas;

    protected BoundaryOverlay(WorldMapPaneDataModel dataModel) {
        this.dataModel = dataModel;
    }

    @Override
    public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
        layerCanvas = canvas;
        for (final GeoPos[] extraGeoBoundary : dataModel.getAdditionalGeoBoundaries()) {
            drawGeoBoundary(rendering.getGraphics(), extraGeoBoundary, false, null, null);
        }

        final Product selectedProduct = dataModel.getSelectedProduct();
        for (final Product product : dataModel.getProducts()) {
            if (selectedProduct != product) {
                drawProduct(rendering.getGraphics(), product, false);
            }
        }

        handleSelectedProduct(rendering, selectedProduct);

    }

    protected abstract void handleSelectedProduct(Rendering rendering, Product selectedProduct);

    protected void drawProduct(final Graphics2D g2d, final Product product, final boolean isCurrent) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            return;
        }

        GeneralPath[] boundaryPaths = WorldMapPane.getGeoBoundaryPaths(product);
        final String text = String.valueOf(product.getRefNo());
        final PixelPos textCenter = getProductCenter(product);
        drawGeoBoundary(g2d, boundaryPaths, isCurrent, text, textCenter);
    }

    private void drawGeoBoundary(final Graphics2D g2d, final GeneralPath[] boundaryPaths, final boolean isCurrent,
                                 final String text, final PixelPos textCenter) {
        final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
        for (GeneralPath boundaryPath : boundaryPaths) {
            boundaryPath.transform(transform);
            drawPath(isCurrent, g2d, boundaryPath, 0.0f);
        }

        drawText(g2d, text, textCenter, 0.0f);
    }

    private void drawGeoBoundary(final Graphics2D g2d, final GeoPos[] geoBoundary, final boolean isCurrent,
                                 final String text, final PixelPos textCenter) {
        final GeneralPath gp = convertToPixelPath(geoBoundary);
        drawPath(isCurrent, g2d, gp, 0.0f);
        drawText(g2d, text, textCenter, 0.0f);
    }

    private void drawPath(final boolean isCurrent, Graphics2D g2d, final GeneralPath gp, final float offsetX) {
        g2d = prepareGraphics2D(offsetX, g2d);
        if (isCurrent) {
            g2d.setColor(new Color(255, 200, 200, 30));
        } else {
            g2d.setColor(new Color(255, 255, 255, 30));
        }
        g2d.fill(gp);
        if (isCurrent) {
            g2d.setColor(new Color(255, 0, 0));
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.draw(gp);
    }

    private GeneralPath convertToPixelPath(final GeoPos[] geoBoundary) {
        final GeneralPath gp = new GeneralPath();
        for (int i = 0; i < geoBoundary.length; i++) {
            final GeoPos geoPos = geoBoundary[i];
            final AffineTransform m2vTransform = layerCanvas.getViewport().getModelToViewTransform();
            final Point2D viewPos = m2vTransform.transform(new PixelPos.Double(geoPos.lon, geoPos.lat), null);
            if (i == 0) {
                gp.moveTo(viewPos.getX(), viewPos.getY());
            } else {
                gp.lineTo(viewPos.getX(), viewPos.getY());
            }
        }
        gp.closePath();
        return gp;
    }

    private void drawText(Graphics2D g2d, final String text, final PixelPos textCenter, final float offsetX) {
        if (text == null || textCenter == null) {
            return;
        }
        g2d = prepareGraphics2D(offsetX, g2d);
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final Color color = g2d.getColor();
        g2d.setColor(Color.black);

        g2d.drawString(text,
                textCenter.x - fontMetrics.stringWidth(text) / 2.0f,
                textCenter.y + fontMetrics.getAscent() / 2.0f);
        g2d.setColor(color);
    }

    private Graphics2D prepareGraphics2D(final float offsetX, Graphics2D g2d) {
        if (offsetX != 0.0f) {
            g2d = (Graphics2D) g2d.create();
            final AffineTransform transform = g2d.getTransform();
            final AffineTransform offsetTrans = new AffineTransform();
            offsetTrans.setToTranslation(+offsetX, 0);
            transform.concatenate(offsetTrans);
            g2d.setTransform(transform);
        }
        return g2d;
    }

    private PixelPos getProductCenter(final Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        PixelPos centerPos = null;
        if (geoCoding != null) {
            final float pixelX = (float) Math.floor(0.5f * product.getSceneRasterWidth()) + 0.5f;
            final float pixelY = (float) Math.floor(0.5f * product.getSceneRasterHeight()) + 0.5f;
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(pixelX, pixelY), null);
            final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
            final Point2D point2D = transform.transform(new Point2D.Double(geoPos.getLon(), geoPos.getLat()), null);
            centerPos = new PixelPos((float) point2D.getX(), (float) point2D.getY());
        }
        return centerPos;
    }

}
