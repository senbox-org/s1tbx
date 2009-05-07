package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;

/**
 * Experimental wind field layer. Given two band names for u,v, it could
 * be generalized to any vector field layer.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class WindFieldLayer extends Layer {
    private AffineTransform imageToModelTransform;
    private AffineTransform modelToImageTransform;
    private RasterDataNode windu;
    private RasterDataNode windv;
    private final Color[] palette;
    private double maxLength = 10.0; // m/s
    private int res = 16;
    private float lineThickness = 2.0f;

    public WindFieldLayer(ValueContainer configuration) {
        super(LayerType.getLayerType(WindFieldLayerType.class.getName()), configuration);
        windu = (RasterDataNode) configuration.getValue("windu");
        windv = (RasterDataNode) configuration.getValue("windv");
        imageToModelTransform = windu.getGeoCoding() != null ? windu.getGeoCoding().getImageToModelTransform() : new AffineTransform();
        try {
            modelToImageTransform = imageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException(e);
        }
        palette = new Color[256];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = new Color(i, i, i);
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Viewport vp = rendering.getViewport();
        final Shape vbounds = vp.getViewBounds();
        final Shape mbounds = vp.getViewToModelTransform().createTransformedShape(vbounds);
        final Shape ibounds = modelToImageTransform.createTransformedShape(mbounds);
        final int width = windu.getSceneRasterWidth();
        final int height = windu.getSceneRasterHeight();
        final Rectangle rectangle = ibounds.getBounds().intersection(new Rectangle(0, 0, width, height));
        if (rectangle.isEmpty()) {
            return;
        }

        final Graphics2D graphics = rendering.getGraphics();
        final AffineTransform modelToViewTransform = vp.getModelToViewTransform();

        graphics.setStroke(new BasicStroke(lineThickness));

        final int x1 = res * (rectangle.x / res);
        final int x2 = x1 + res * (1 + rectangle.width / res);
        final int y1 = res * (rectangle.y / res);
        final int y2 = y1 + res * (1 + rectangle.height / res);
        final double[] ipts = new double[8];
        final double[] mpts = new double[8];
        final double[] vpts = new double[8];
        for (int y = y1; y <= y2; y += res) {
            for (int x = x1; x <= x2; x += res) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    final double u = windu.getPixelDouble(x, y);
                    final double v = windv.getPixelDouble(x, y);
                    final double length = Math.sqrt(u * u + v * v);
                    final double ndx = length > 0 ? +u / length : 0;
                    final double ndy = length > 0 ? -v / length : 0;
                    final double ondx = -ndy;
                    final double ondy = ndx;

                    final double s0 = (length  / maxLength) * res;
                    final double s1 = s0 - 0.2 * res;
                    final double s2 = 0.1 * res;

                    ipts[0] = x;
                    ipts[1] = y;
                    ipts[2] = x + s0 * ndx;
                    ipts[3] = y + s0 * ndy;
                    ipts[4] = x + s1 * ndx + s2 * ondx;
                    ipts[5] = y + s1 * ndy + s2 * ondy;
                    ipts[6] = x + s1 * ndx - s2 * ondx;
                    ipts[7] = y + s1 * ndy - s2 * ondy;
                    imageToModelTransform.transform(ipts, 0, mpts, 0, 4);
                    modelToViewTransform.transform(mpts, 0, vpts, 0, 4);
                    final int grey = Math.min(255, (int) Math.round(256 * length / maxLength));

                    graphics.setColor(palette[grey]);
                    graphics.draw(new Line2D.Double(vpts[0], vpts[1], vpts[2], vpts[3]));
                    graphics.draw(new Line2D.Double(vpts[4], vpts[5], vpts[2], vpts[3]));
                    graphics.draw(new Line2D.Double(vpts[6], vpts[7], vpts[2], vpts[3]));
                }
            }
        }
    }
}
