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

package org.esa.snap.core.image;

import com.bc.ceres.glevel.MultiLevelImage;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Lineal;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.Puntal;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.jai.JAIUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform2D;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

/**
 * A {@link SingleBandedOpImage} which computes its (binary) data from geometries provided
 * by a {@link VectorDataNode} by rasterizing the geometries to a raster data grid.
 */
public class VectorDataMaskOpImage extends SingleBandedOpImage {

    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 255;
    private final VectorDataNode vectorDataNode;
    private final AffineTransform m2iTransform;

    /**
     * Constructs a new VectorDataMaskOpImage.
     *
     * @param vectorDataNode The vector-data node providing the geometries to be rasterized.
     * @param rasterDataNode The raster-data node providing the context for the rasterization.
     * @param level The multi-level resolution level.
     */
    public VectorDataMaskOpImage(VectorDataNode vectorDataNode, RasterDataNode rasterDataNode, ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE,
              rasterDataNode.getRasterWidth(),
              rasterDataNode.getRasterHeight(),
              getPreferredTileSize(rasterDataNode),
              null,
              level);
        this.vectorDataNode = vectorDataNode;
        AffineTransform transform = rasterDataNode.getImageToModelTransform();
        try {
            transform.invert();
            m2iTransform = transform;
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Could not invert model-to-image transformation.", e);
        }
    }

    /**
     * @return The vector-data node associated with this image.
     */
    public VectorDataNode getVectorDataNode() {
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

    private static Dimension getPreferredTileSize(RasterDataNode rasterDataNode) {
        if (rasterDataNode.isSourceImageSet()) {
            MultiLevelImage sourceImage = rasterDataNode.getSourceImage();
            return new Dimension(sourceImage.getTileWidth(),
                                 sourceImage.getTileHeight());
        }
        if (rasterDataNode.getProduct() != null) {
            return ImageManager.getPreferredTileSize(rasterDataNode.getProduct());
        } else {
            return JAIUtils.computePreferredTileSize(rasterDataNode.getRasterWidth(),
                                                     rasterDataNode.getRasterHeight(), 1);
        }
    }
}
