/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.esa.snap.core.util.converters.RectangleConverter;

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import static java.lang.Math.*;

/**
 * This operator is used to create either spatial and/or spectral subsets of a data product.
 * Spatial subset may be given by pixel positions (parameter {@code region})
 * or a geographical polygon (parameter {@code geoRegion}). Subsets of band and tie-point grid
 * are given by name lists (parameters {@code bandNames} and  {@code tiePointGridNames}).
 *
 * @author Marco Zuehlke
 * @author Norman Fomferra
 * @author Marco Peters
 * @since BEAM 4.9
 */
@OperatorMetadata(alias = "Subset",
        category = "Raster/Geometric",
        authors = "Marco Zuehlke, Norman Fomferra, Marco Peters",
        version = "1.2",
        copyright = "(c) 2011 by Brockmann Consult",
        description = "Create a spatial and/or spectral subset of a data product.")
public class SubsetOp extends Operator {

    @SourceProduct(alias = "source", description = "The source product to create the subset from.")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] bandNames;

    @Parameter(converter = RectangleConverter.class,
            description = "The subset region in pixel coordinates.\n" +
                          "Use the following format: <x>,<y>,<width>,<height>\n" +
                          "If not given, the entire scene is used. The 'geoRegion' parameter has precedence over this parameter.")
    private Rectangle region = null;

    HashMap<String,Rectangle> finalRegionMap = new HashMap<>();

    @Parameter(description = "The band used to indicate the pixel coordinates.", alias = "referenceBand" ,
            rasterDataNodeType = Band.class, label = "Reference Band")
    private String referenceBand = null;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The subset region in geographical coordinates using WKT-format,\n" +
                          "e.g. POLYGON((<lon1> <lat1>, <lon2> <lat2>, ..., <lon1> <lat1>))\n" +
                          "(make sure to quote the option due to spaces in <geometry>).\n" +
                          "If not given, the entire scene is used.")
    private Geometry geoRegion;
    @Parameter(defaultValue = "1",
            description = "The pixel sub-sampling step in X (horizontal image direction)")
    private int subSamplingX = 1;
    @Parameter(defaultValue = "1",
            description = "The pixel sub-sampling step in Y (vertical image direction)")
    private int subSamplingY = 1;
    @Parameter(defaultValue = "false",
            description = "Forces the operator to extend the subset region to the full swath.")
    private boolean fullSwath = false;

    @Parameter(description = "The comma-separated list of names of tie-point grids to be copied. \n" +
                             "If not given, all bands are copied.")
    private String[] tiePointGridNames;

    @Parameter(defaultValue = "false",
            description = "Whether to copy the metadata of the source product.")
    private boolean copyMetadata = false;

    private transient ProductReader subsetReader;

    public SubsetOp() {
        subSamplingX = 1;
        subSamplingY = 1;
    }

    public String[] getTiePointGridNames() {
        return tiePointGridNames != null ? tiePointGridNames.clone() : null;
    }

    public void setTiePointGridNames(String[] tiePointGridNames) {
        this.tiePointGridNames = tiePointGridNames != null ? tiePointGridNames.clone() : null;
    }

    public String[] getBandNames() {
        return bandNames != null ? bandNames.clone() : null;
    }

    public void setBandNames(String[] bandNames) {
        this.bandNames = bandNames != null ? bandNames.clone() : null;
    }

    public void setCopyMetadata(boolean copyMetadata) {
        this.copyMetadata = copyMetadata;
    }

    public Rectangle getRegion() {
        return region != null ? new Rectangle(region) : null;
    }

    public void setRegion(Rectangle region) {
        this.region = region != null ? new Rectangle(region) : null;
    }

    public void setSubSamplingX(int subSamplingX) {
        this.subSamplingX = subSamplingX;
    }

    public void setSubSamplingY(int subSamplingY) {
        this.subSamplingY = subSamplingY;
    }

    public Geometry getGeoRegion() {
        return geoRegion;
    }

    public void setGeoRegion(Geometry geoRegion) {
        this.geoRegion = geoRegion;
    }


    @Override
    public void initialize() throws OperatorException {
        subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        if (tiePointGridNames != null) {
            subsetDef.addNodeNames(tiePointGridNames);
        } else {
            subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        }

        if (bandNames != null && bandNames.length > 0) {
            subsetDef.addNodeNames(bandNames);
        } else {
            subsetDef.addNodeNames(sourceProduct.getBandNames());
        }
        String[] nodeNames = subsetDef.getNodeNames();
        if (nodeNames != null) {
            final ArrayList<String> referencedNodeNames = new ArrayList<>();
            for (String nodeName : nodeNames) {
                collectReferencedRasters(nodeName, referencedNodeNames);
            }
            subsetDef.addNodeNames(referencedNodeNames.toArray(new String[referencedNodeNames.size()]));
        }

        if (geoRegion != null) {
            if(sourceProduct.isMultiSize()) {
                subsetDef.setRegionMap(computeRegionMap(geoRegion,sourceProduct,null));
            } else {
                region = computePixelRegion(sourceProduct, geoRegion, 0);
            }

            if (region.isEmpty()) {
                targetProduct = new Product("Empty_" + sourceProduct.getName(), "EMPTY", 0, 0);
                String msg = "No intersection with source product boundary " + sourceProduct.getName();
                targetProduct.setDescription(msg);
                getLogger().log(Level.WARNING, msg);
                return;
            }
        }
        if (fullSwath && region != null) {
            region = new Rectangle(0, region.y, sourceProduct.getSceneRasterWidth(), region.height);
        }
        
        if (region != null && !region.isEmpty()) {
            if(referenceBand == null || referenceBand.length() == 0) {
                if (region.width == 0 || region.x + region.width > sourceProduct.getSceneRasterWidth()) {
                    region.width = sourceProduct.getSceneRasterWidth() - region.x;
                }
                if (region.height == 0 || region.y + region.height > sourceProduct.getSceneRasterHeight()) {
                    region.height = sourceProduct.getSceneRasterHeight() - region.y;
                }
                subsetDef.setRegion(region);
            } else {
                subsetDef.setRegionMap(computeRegionMap (region, referenceBand, sourceProduct, null));
            }
        }

        subsetDef.setSubSampling(subSamplingX, subSamplingY);
        subsetDef.setIgnoreMetadata(!copyMetadata);

        try {
            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
            targetProduct.setName("Subset_" + targetProduct.getName());
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private void collectReferencedRasters(String nodeName, ArrayList<String> referencedNodeNames) {
        RasterDataNode rasterDataNode = sourceProduct.getRasterDataNode(nodeName);
        if (rasterDataNode == null) {
            throw new OperatorException(String.format("Source product does not contain a raster named '%s'.", nodeName));
        }
        final String validPixelExpression = rasterDataNode.getValidPixelExpression();
        collectReferencedRastersInExpression(validPixelExpression, referencedNodeNames);
        if (rasterDataNode instanceof VirtualBand) {
            VirtualBand vBand = (VirtualBand) rasterDataNode;
            collectReferencedRastersInExpression(vBand.getExpression(), referencedNodeNames);
        }
    }

    private void collectReferencedRastersInExpression(String expression, ArrayList<String> referencedNodeNames) {
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }
        try {
            final Term term = sourceProduct.parseExpression(expression);
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(term);
            for (RasterDataNode refRaster : refRasters) {
                final String refNodeName = refRaster.getName();
                if (!referencedNodeNames.contains(refNodeName)) {
                    referencedNodeNames.add(refNodeName);
                    collectReferencedRastersInExpression(refNodeName, referencedNodeNames);
                }
            }
        } catch (ParseException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        ProductData destBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            subsetReader.readBandRasterData(band,
                                            rectangle.x,
                                            rectangle.y,
                                            rectangle.width,
                                            rectangle.height,
                                            destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    // todo - nf/mz 20131105 - move this method to a more prominent location (e.g. FeatureUtils)

    /**
     * Non-API (yet).
     */
    public static Rectangle computePixelRegion(Product product, Geometry geoRegion, int numBorderPixels) {
        final Geometry productGeometry = computeProductGeometry(product);
        final Geometry regionIntersection = geoRegion.intersection(productGeometry);
        if (regionIntersection.isEmpty()) {
            return new Rectangle();
        }
        final PixelRegionFinder pixelRegionFinder = new PixelRegionFinder(product.getSceneGeoCoding());
        regionIntersection.apply(pixelRegionFinder);
        final Rectangle pixelRegion = pixelRegionFinder.getPixelRegion();
        pixelRegion.grow(numBorderPixels, numBorderPixels);
        return pixelRegion.intersection(new Rectangle(product.getSceneRasterWidth(),
                                                      product.getSceneRasterHeight()));
    }

    public static Rectangle computePixelRegion(RasterDataNode rasterDataNode, Geometry geoRegion, int numBorderPixels) {
        final Geometry productGeometry = computeProductGeometry(rasterDataNode);
        final Geometry regionIntersection = geoRegion.intersection(productGeometry);
        if (regionIntersection.isEmpty()) {
            return new Rectangle();
        }
        final PixelRegionFinder pixelRegionFinder = new PixelRegionFinder(rasterDataNode.getGeoCoding(),true);
        regionIntersection.apply(pixelRegionFinder);
        final Rectangle pixelRegion = pixelRegionFinder.getPixelRegion();
        pixelRegion.grow(numBorderPixels, numBorderPixels);
        return pixelRegion.intersection(new Rectangle(rasterDataNode.getRasterWidth(),
                                                      rasterDataNode.getRasterHeight()));
    }

    public static Geometry computeGeoRegion(RasterDataNode rasterDataNode, Rectangle pixelRegion) {

        final int step = Math.min(pixelRegion.width, pixelRegion.height) / 8;
        GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(rasterDataNode, pixelRegion, step, false);
        final Polygon[] polygons = new Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }

        if(polygons.length == 1) {
            return polygons[0];
        } else {
            return factory.createMultiPolygon(polygons);
        }
        //final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
        //        polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        //return peuckerSimplifier.getResultGeometry();
    }

    public static Geometry computeGeoRegion(Product product, Rectangle pixelRegion) {

        final int step = Math.min(pixelRegion.width, pixelRegion.height) / 8;
        GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product, pixelRegion, step, false);
        final Polygon[] polygons = new Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }

        if(polygons.length == 1) {
            return polygons[0];
        } else {
            return factory.createMultiPolygon(polygons);
        }
        //final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
        //        polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        //return peuckerSimplifier.getResultGeometry();
    }

    static Geometry computeProductGeometry(Product product) {
        final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product);
        final Polygon[] polygons = new Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }
        final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
                polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        return peuckerSimplifier.getResultGeometry();
    }

    static Geometry computeProductGeometry(RasterDataNode rasterDataNode) {
        final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(rasterDataNode);
        final Polygon[] polygons = new Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }
        final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
                polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        return peuckerSimplifier.getResultGeometry();
    }

    private static Polygon convertAwtPathToJtsPolygon(Path2D path, GeometryFactory factory) {
        final PathIterator pathIterator = path.getPathIterator(null);
        ArrayList<double[]> coordList = new ArrayList<>();
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


    public static HashMap<String,Rectangle> computeRegionMap (Rectangle region, Product product, String[] rasterNames) {
        if(rasterNames == null || rasterNames.length == 0) {
            rasterNames = product.getBandNames();
        }

        HashMap<String,Rectangle> regionMap = new HashMap<>();
        HashMap<String,Geometry> geometryMap = new HashMap<>();
        HashMap<String,Rectangle> finalRegionMap = new HashMap<>();

        Geometry geoRegion = computeGeoRegion(product,region);
        Geometry finalGeometry = null;
        for(String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getBand(rasterName);
            if(rasterDataNode == null) {
                continue;
            }
            Rectangle rect = computePixelRegion(rasterDataNode, geoRegion, 0);
            regionMap.put(rasterDataNode.getName(),rect);
            Geometry geom = computeGeoRegion(rasterDataNode,rect);
            geometryMap.put(rasterDataNode.getName(),geom);
            if(finalGeometry == null) {
                finalGeometry = geom;
            } else if(geom.covers(finalGeometry)) {
                finalGeometry = geom;
            }
        }

        for(String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getBand(rasterName);
            if(rasterDataNode == null) {
                continue;
            }
            Rectangle rect = computePixelRegion(rasterDataNode, finalGeometry, 0);
            finalRegionMap.put(rasterDataNode.getName(),rect);
        }

        return finalRegionMap;
    }

    public static HashMap<String,Rectangle> computeRegionMap (Rectangle region, String referenceBandName, Product product, String[] rasterNames) {
        if(rasterNames == null || rasterNames.length == 0) {
            rasterNames = product.getBandNames();
        }

        HashMap<String,Rectangle> regionMap = new HashMap<>();
        RasterDataNode referenceNode = product.getBand(referenceBandName);

        Geometry geoRegion = computeGeoRegion(referenceNode,region);

        for(String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getBand(rasterName);
            if(rasterDataNode == null) {
                continue;
            }
            if(rasterDataNode.getGeoCoding().equals(referenceNode.getGeoCoding())) {
                regionMap.put(rasterDataNode.getName(),region);
                continue;
            }
            Rectangle rect = computePixelRegion(rasterDataNode, geoRegion, 0);
            regionMap.put(rasterDataNode.getName(),rect);
        }

        return regionMap;
    }

    public static HashMap<String,Rectangle> computeRegionMap (Geometry geoRegion, Product product, String[] rasterNames) {
        if(rasterNames == null || rasterNames.length == 0) {
            rasterNames = product.getBandNames();
        }

        HashMap<String,Rectangle> regionMap = new HashMap<>();

        for(String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getBand(rasterName);
            if(rasterDataNode == null) {
                continue;
            }

            Rectangle rect = computePixelRegion(rasterDataNode, geoRegion, 0);
            regionMap.put(rasterDataNode.getName(),rect);
        }

        return regionMap;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SubsetOp.class);
        }
    }

    private static class PixelRegionFinder implements CoordinateFilter {

        private final GeoCoding geoCoding;
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        boolean round = false;

        private PixelRegionFinder(GeoCoding geoCoding) {
            this(geoCoding, false);
        }

        private PixelRegionFinder(GeoCoding geoCoding, boolean round) {
            this.geoCoding = geoCoding;
            x1 = Integer.MAX_VALUE;
            x2 = Integer.MIN_VALUE;
            y1 = Integer.MAX_VALUE;
            y2 = Integer.MIN_VALUE;
            this.round = round;
        }

        @Override
        public void filter(Coordinate coordinate) {
            final GeoPos geoPos = new GeoPos(coordinate.y, coordinate.x);
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            if (pixelPos.isValid()) {
                if(round) {
                    x1 = min(x1, (int) round(pixelPos.x));
                    x2 = max(x2, (int) round(pixelPos.x));
                    y1 = min(y1, (int) round(pixelPos.y));
                    y2 = max(y2, (int) round(pixelPos.y));
                } else {
                    x1 = min(x1, (int) floor(pixelPos.x));
                    x2 = max(x2, (int) ceil(pixelPos.x));
                    y1 = min(y1, (int) floor(pixelPos.y));
                    y2 = max(y2, (int) ceil(pixelPos.y));
                }
            }
        }

        public Rectangle getPixelRegion() {
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
    }
}
