package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.BasicStroke;
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
        final double[] ipts = new double[4];
        final double[] mpts = new double[4];
        final double[] vpts = new double[4];
        for (int y = y1; y <= y2; y += res) {
            for (int x = x1; x <= x2; x += res) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    final float u = windu.getPixelFloat(x, y);
                    final float v = windv.getPixelFloat(x, y);
                    final double length = Math.sqrt(u * u + v * v);

                    ipts[0] = x;
                    ipts[1] = y;
                    ipts[2] = x + res * u / length;
                    ipts[3] = y - res * v / length;
                    imageToModelTransform.transform(ipts, 0, mpts, 0, 2);
                    modelToViewTransform.transform(mpts, 0, vpts, 0, 2);
                    final int grey = Math.min(255, (int) Math.round(256 * length / maxLength));

                    graphics.setColor(palette[grey]);
                    graphics.draw(new Line2D.Double(vpts[0], vpts[1], vpts[2], vpts[3]));
                }
            }
        }
    }
}
