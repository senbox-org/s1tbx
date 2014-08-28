/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.esa.snap.gpf.OperatorUtils;

import java.awt.*;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;
import static java.lang.Math.ceil;

@OperatorMetadata(alias = "SpatialSubset",
        category = "Utilities",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Create a spatial subset of the source product.")
public class SubsetOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private
    String[] sourceBandNames;

    @Parameter(label = "X", defaultValue = "0")
    private int regionX = 0;
    @Parameter(label = "Y", defaultValue = "0")
    private int regionY = 0;
    @Parameter(label = "Width", defaultValue = "1000")
    private int width = 1000;
    @Parameter(label = "Height", defaultValue = "1000")
    private int height = 1000;
    @Parameter(defaultValue = "1")
    private int subSamplingX = 1;
    @Parameter(defaultValue = "1")
    private int subSamplingY = 1;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "WKT-format, " +
                    "e.g. POLYGON((<lon1> <lat1>, <lon2> <lat2>, ..., <lon1> <lat1>))\n" +
                    "(make sure to quote the option due to spaces in <geometry>)")
    private Geometry geoRegion;

    private ProductReader subsetReader = null;
    private final Map<Band, Band> bandMap = new HashMap<Band, Band>();

    @Override
    public void initialize() throws OperatorException {
        if (width == 0 || regionX + width > sourceProduct.getSceneRasterWidth()) {
            width = sourceProduct.getSceneRasterWidth() - regionX;
        }
        if (height == 0 || regionY + height > sourceProduct.getSceneRasterHeight()) {
            height = sourceProduct.getSceneRasterHeight() - regionY;
        }

        subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());

        if (sourceBandNames != null && sourceBandNames.length > 0) {
            subsetDef.addNodeNames(sourceBandNames);
        } else {
            subsetDef.addNodeNames(sourceProduct.getBandNames());
        }
        subsetDef.setRegion(regionX, regionY, width, height);

        if (geoRegion != null) {
            final Rectangle region = computePixelRegion(sourceProduct, geoRegion, 0);
            if (region != null) {
                if (region.isEmpty()) {
                    throw new OperatorException("Subset: No intersection with source product boundary " + sourceProduct.getName());
                }
                subsetDef.setRegion(region);
            }
        }

        subsetDef.setSubSampling(subSamplingX, subSamplingY);
        subsetDef.setIgnoreMetadata(false);

        try {
            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);

            // replace virtual bands with real bands
            for (Band b : targetProduct.getBands()) {
                if (b instanceof VirtualBand) {
                    targetProduct.removeBand(b);
                    final Band newBand = targetProduct.addBand(b.getName(), b.getDataType());
                    newBand.setNoDataValue(b.getNoDataValue());
                    newBand.setNoDataValueUsed(b.isNoDataValueUsed());
                    newBand.setDescription(b.getDescription());
                    newBand.setUnit(b.getUnit());
                    bandMap.put(newBand, b);
                }
            }
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    public static Rectangle computePixelRegion(Product product, Geometry geoRegion, int numBorderPixels) {
        final Geometry productGeometry = computeProductGeometry(product);
        final Geometry regionIntersection = geoRegion.intersection(productGeometry);
        if (regionIntersection.isEmpty()) {
            return new Rectangle();
        }
        final PixelRegionFinder pixelRegionFinder = new PixelRegionFinder(product.getGeoCoding());
        regionIntersection.apply(pixelRegionFinder);
        final Rectangle pixelRegion = pixelRegionFinder.getPixelRegion();
        pixelRegion.grow(numBorderPixels, numBorderPixels);
        return pixelRegion.intersection(new Rectangle(product.getSceneRasterWidth(),
                product.getSceneRasterHeight()));
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final ProductData destBuffer = targetTile.getRawSamples();
        final Rectangle rectangle = targetTile.getRectangle();
        try {
            // for virtual bands
            Band tgtBand = bandMap.get(band);
            if (tgtBand == null)
                tgtBand = band;
            subsetReader.readBandRasterData(tgtBand,
                    rectangle.x,
                    rectangle.y,
                    rectangle.width,
                    rectangle.height,
                    destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    static Geometry computeProductGeometry(Product product) {
        final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product);
        final com.vividsolutions.jts.geom.Polygon[] polygons = new com.vividsolutions.jts.geom.Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }
        final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
                polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        return peuckerSimplifier.getResultGeometry();
    }

    private static com.vividsolutions.jts.geom.Polygon convertAwtPathToJtsPolygon(Path2D path, GeometryFactory factory) {
        final PathIterator pathIterator = path.getPathIterator(null);
        ArrayList<double[]> coordList = new ArrayList<double[]>();
        int lastOpenIndex = 0;
        while (!pathIterator.isDone()) {
            final double[] coords = new double[6];
            final int segType = pathIterator.currentSegment(coords);
            if (segType == PathIterator.SEG_CLOSE) {
                // we should only detect a single SEG_CLOSE
                coordList.add(coordList.get(lastOpenIndex));
                lastOpenIndex = coordList.size();
            } else {
                coordList.add(coords);
            }
            pathIterator.next();
        }
        final Coordinate[] coordinates = new Coordinate[coordList.size()];
        for (int i1 = 0; i1 < coordinates.length; i1++) {
            final double[] coord = coordList.get(i1);
            coordinates[i1] = new Coordinate(coord[0], coord[1]);
        }

        return factory.createPolygon(factory.createLinearRing(coordinates), null);
    }

    private static class PixelRegionFinder implements CoordinateFilter {

        private final GeoCoding geoCoding;
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        private PixelRegionFinder(GeoCoding geoCoding) {
            this.geoCoding = geoCoding;
            x1 = Integer.MAX_VALUE;
            x2 = Integer.MIN_VALUE;
            y1 = Integer.MAX_VALUE;
            y2 = Integer.MIN_VALUE;
        }

        @Override
        public void filter(Coordinate coordinate) {
            final GeoPos geoPos = new GeoPos((float) coordinate.y, (float) coordinate.x);
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            if (pixelPos.isValid()) {
                x1 = min(x1, (int) floor(pixelPos.x));
                x2 = max(x2, (int) ceil(pixelPos.x));
                y1 = min(y1, (int) floor(pixelPos.y));
                y2 = max(y2, (int) ceil(pixelPos.y));
            }
        }

        public Rectangle getPixelRegion() {
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SubsetOp.class);
        }
    }
}