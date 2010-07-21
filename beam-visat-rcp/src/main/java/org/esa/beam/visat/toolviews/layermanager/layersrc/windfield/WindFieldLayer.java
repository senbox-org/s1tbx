/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
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
import java.awt.image.RenderedImage;

/**
 * Experimental wind field layer. Given two band names for u,v, it could
 * be generalized to any vector field layer.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class WindFieldLayer extends Layer {
    private RasterDataNode windu;
    private RasterDataNode windv;
    private final Color[] palette;
    private double maxLength = 10.0; // m/s
    private int res = 16;
    private float lineThickness = 2.0f;

    public WindFieldLayer(PropertyContainer configuration) {
        this(LayerTypeRegistry.getLayerType(WindFieldLayerType.class.getName()),
             (RasterDataNode) configuration.getValue("windu"),
             (RasterDataNode) configuration.getValue("windv"),
             configuration);
    }

    public WindFieldLayer(LayerType layerType, RasterDataNode windu, RasterDataNode windv,
                          PropertySet configuration) {
        super(layerType, configuration);
        setName("Wind Speed");
        this.windu = windu;
        this.windv = windv;
        palette = new Color[256];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = new Color(i, i, i);
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final MultiLevelImage winduMLI = windu.getGeophysicalImage();
        final MultiLevelImage windvMLI = windv.getGeophysicalImage();
        final Viewport vp = rendering.getViewport();
        final int level = ImageLayer.getLevel(winduMLI.getModel(), vp);

        final AffineTransform m2i = winduMLI.getModel().getModelToImageTransform(level);
        final AffineTransform i2m = winduMLI.getModel().getImageToModelTransform(level);

        final Shape vbounds = vp.getViewBounds();
        final Shape mbounds = vp.getViewToModelTransform().createTransformedShape(vbounds);
        final Shape ibounds = m2i.createTransformedShape(mbounds);

        final RenderedImage winduRI = winduMLI.getImage(level);
        final RenderedImage windvRI = windvMLI.getImage(level);

        final int width = winduRI.getWidth();
        final int height = winduRI.getHeight();
        final Rectangle irect = ibounds.getBounds().intersection(new Rectangle(0, 0, width, height));
        if (irect.isEmpty()) {
            return;
        }

        final AffineTransform m2v = vp.getModelToViewTransform();

        final Graphics2D graphics = rendering.getGraphics();
        graphics.setStroke(new BasicStroke(lineThickness));

        final MultiLevelImage winduValidMLI = windu.getValidMaskImage();
        final MultiLevelImage windvValidMLI = windv.getValidMaskImage();

        final RenderedImage winduValidRI = winduValidMLI != null ? winduValidMLI.getImage(level) : null;
        final RenderedImage windvValidRI = windvValidMLI != null ? windvValidMLI.getImage(level) : null;

        final int x1 = res * (irect.x / res);
        final int x2 = x1 + res * (1 + irect.width / res);
        final int y1 = res * (irect.y / res);
        final int y2 = y1 + res * (1 + irect.height / res);
        final double[] ipts = new double[8];
        final double[] mpts = new double[8];
        final double[] vpts = new double[8];

        final Rectangle pixelRect = new Rectangle(0, 0, 1, 1);
        for (int y = y1; y <= y2; y += res) {
            for (int x = x1; x <= x2; x += res) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    pixelRect.x = x;
                    pixelRect.y = y;
                    final boolean winduValid = winduValidRI == null || winduValidRI.getData(pixelRect).getSample(x, y, 0) != 0;
                    if (!winduValid) {
                        continue;
                    }
                    final boolean windvValid = windvValidRI == null || windvValidRI.getData(pixelRect).getSample(x, y, 0) != 0;
                    if (!windvValid) {
                        continue;
                    }

                    final double u = winduRI.getData(pixelRect).getSampleDouble(x, y, 0);
                    final double v = windvRI.getData(pixelRect).getSampleDouble(x, y, 0);
                    final double length = Math.sqrt(u * u + v * v);
                    final double ndx = length > 0 ? +u / length : 0;
                    final double ndy = length > 0 ? -v / length : 0;
                    final double ondx = -ndy;
                    final double ondy = ndx;

                    final double s0 = (length / maxLength) * res;
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
                    i2m.transform(ipts, 0, mpts, 0, 4);
                    m2v.transform(mpts, 0, vpts, 0, 4);

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
