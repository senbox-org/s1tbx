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

package org.esa.beam.jai;

import com.vividsolutions.jts.geom.*;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform2D;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

public class VectorDataMaskOpImage extends SingleBandedOpImage {

    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 255;
    private final VectorDataNode vectorDataNode;
    private final AffineTransform m2iTransform;

    public VectorDataMaskOpImage(VectorDataNode vectorDataNode, ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE,
              vectorDataNode.getProduct().getSceneRasterWidth(),
              vectorDataNode.getProduct().getSceneRasterHeight(),
              vectorDataNode.getProduct().getPreferredTileSize(),
              null,
              level);
        this.vectorDataNode = vectorDataNode;
        GeoCoding geoCoding = vectorDataNode.getProduct().getGeoCoding();
        AffineTransform transform = ImageManager.getImageToModelTransform(geoCoding);
        try {
            transform.invert();
            m2iTransform = transform;
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Could not invert model-to-image transformation.", e);
        }
    }

    public VectorDataNode getVectorData() {
        return vectorDataNode;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        final BufferedImage image = new BufferedImage(colorModel,
                                                      RasterFactory.createWritableRaster(tile.getSampleModel(),
                                                                                         tile.getDataBuffer(),
                                                                                         new Point(0, 0)), false, null);
        final Graphics2D graphics2D = image.createGraphics();
        graphics2D.translate(-(tile.getMinX() + 0.5), -(tile.getMinY() + 0.5));
        graphics2D.setColor(Color.WHITE);

        FeatureCollection<SimpleFeatureType, SimpleFeature> features = vectorDataNode.getFeatureCollection();
        FeatureIterator<SimpleFeature> featureIterator = features.features();
        try {
            AffineTransform transform = AffineTransform.getScaleInstance(1.0 / getScale(), 1.0 / getScale());
            transform.concatenate(m2iTransform);
            AffineTransform2D transform2D = new AffineTransform2D(transform);

            while (featureIterator.hasNext()) {
                SimpleFeature feature = featureIterator.next();
                Object value = feature.getDefaultGeometry();
                if (value instanceof Geometry) {
                    try {
                        renderGeometry((Geometry) value, graphics2D, transform2D);
                    } catch (Exception ignored) {
                        // ignore
                    }
                }
            }
        } finally {
            featureIterator.close();
        }

        graphics2D.dispose();

        final byte[] data = ((DataBufferByte) tile.getDataBuffer()).getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = (data[i] != 0) ? TRUE : FALSE;
        }
    }

    private static void renderGeometry(Geometry geom, Graphics2D graphics, MathTransform2D transform) throws Exception {
        if (geom instanceof Puntal) {
            Coordinate c = geom.getCoordinate();
            Point2D.Double pt = new Point2D.Double(c.x, c.y);
            transform.transform(pt, pt);
            graphics.drawLine((int) pt.x, (int) pt.y, (int) pt.x, (int) pt.y);
        } else if (geom instanceof Lineal) {
            LiteShape2 shape = new LiteShape2(geom, transform, null, false, true);
            graphics.draw(shape);
        } else if (geom instanceof Polygonal) {
            LiteShape2 shape = new LiteShape2(geom, transform, null, false, true);
            graphics.fill(shape);
        } else if (geom instanceof GeometryCollection) {
            GeometryCollection collection = (GeometryCollection) geom;
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                renderGeometry(collection.getGeometryN(i), graphics, transform);
            }
        }
    }
}
