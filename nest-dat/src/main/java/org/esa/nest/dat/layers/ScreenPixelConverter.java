/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dat.layers;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
    private final Viewport vp;
    private final RasterDataNode raster;

    public ScreenPixelConverter(final Viewport vp, final RasterDataNode raster) {
        this.vp = vp;
        this.raster = raster;
        mli = raster.getGeophysicalImage();
        zoomFactor = vp.getZoomFactor();

        final AffineTransform m2i = mli.getModel().getModelToImageTransform(level);
        i2m = mli.getModel().getImageToModelTransform(level);

        final Shape vbounds = vp.getViewBounds();
        final Shape mbounds = vp.getViewToModelTransform().createTransformedShape(vbounds);
        ibounds = m2i.createTransformedShape(mbounds);
        m2v = vp.getModelToViewTransform();
    }

    public AffineTransform getImageTransform(final AffineTransform transformSave) {
        final AffineTransform transform = new AffineTransform();
        transform.concatenate(transformSave);
        transform.concatenate(vp.getModelToViewTransform());
        transform.concatenate(raster.getSourceImage().getModel().getImageToModelTransform(0));
        return transform;
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

    public static Point[] arrayToPoints(final double[] vpts) {
        int j=0;
        final Point[] pt = new Point[vpts.length];
        for(int i=0; i < vpts.length; i+=2) {
            pt[j++] = new Point((int)vpts[i], (int)vpts[i+1]);
        }
        return  pt;
    }

    public static PixelPos computeLevelZeroPixelPos(final ImageLayer imageLayer,
                                                    final int pixelX, final int pixelY, final int currentLevel) {
        if (currentLevel != 0) {
            AffineTransform i2mTransform = imageLayer.getImageToModelTransform(currentLevel);
            Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX, pixelY), null);
            AffineTransform m2iTransform = imageLayer.getModelToImageTransform();
            Point2D imageP = m2iTransform.transform(modelP, null);

            return new PixelPos(new Float(imageP.getX()), new Float(imageP.getY()));
        } else {
            return new PixelPos(pixelX + 0.5f, pixelY + 0.5f);
        }
    }
}
