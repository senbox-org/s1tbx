package org.esa.nest.dat.layers;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

/**
 * Converts screen coordinates to pixel coordinates
 */
public class ScreenPixelConverter {

    private final double[] ipts = new double[2];
    private final double[] mpts = new double[2];
    private final static int level = 0;

    private final AffineTransform m2v;
    private final AffineTransform i2m;
    private final Shape ibounds;
    private final MultiLevelImage mli;
    private final double zoomFactor;

    public ScreenPixelConverter(final Viewport vp, final RasterDataNode raster) {
        
        mli = raster.getGeophysicalImage();
        zoomFactor = vp.getZoomFactor();

        final AffineTransform m2i = mli.getModel().getModelToImageTransform(level);
        i2m = mli.getModel().getImageToModelTransform(level);

        final Shape vbounds = vp.getViewBounds();
        final Shape mbounds = vp.getViewToModelTransform().createTransformedShape(vbounds);
        ibounds = m2i.createTransformedShape(mbounds);
        m2v = vp.getModelToViewTransform();
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public boolean withInBounds() {
        final RenderedImage ri = mli.getImage(level);
        final Rectangle irect = ibounds.getBounds().intersection(new Rectangle(0, 0, ri.getWidth(), ri.getHeight()));
        return !irect.isEmpty();
    }

    public void pixelToScreen(final Point pnt, final double[] vpts) {
        ipts[0] = pnt.x;
        ipts[1] = pnt.y;
        i2m.transform(ipts, 0, mpts, 0, 1);
        m2v.transform(mpts, 0, vpts, 0, 1);
    }

    public void pixelToScreen(final double x, final double y, final double[] vpts) {
        ipts[0] = x;
        ipts[1] = y;
        i2m.transform(ipts, 0, mpts, 0, 1);
        m2v.transform(mpts, 0, vpts, 0, 1);
    }

    public void pixelToScreen(final double[] inpts, final double[] vpts) {
        final double[] tmppts = new double[inpts.length];
        i2m.transform(inpts, 0, tmppts, 0, inpts.length/2);
        m2v.transform(tmppts, 0, vpts, 0, inpts.length/2);
    }
}
