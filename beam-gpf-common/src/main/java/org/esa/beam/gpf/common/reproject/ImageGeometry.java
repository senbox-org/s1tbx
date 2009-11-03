/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class ImageGeometry {
    private double referencePixelX;
    private double referencePixelY;
    private double easting;
    private double northing;
    private double orientation;
    private double pixelSizeX;
    private double pixelSizeY;
    
    private AffineTransform i2m;
    private int width;
    private int height;
    private CoordinateReferenceSystem mapCrs;

    private ImageGeometry() {
    }
    
    ImageGeometry(Rectangle bounds, CoordinateReferenceSystem mapCrs, AffineTransform image2map) {
        this.i2m = image2map;
        this.width = bounds.width;
        this.height = bounds.height;
        this.mapCrs = mapCrs;
    }
    
    public AffineTransform getImage2Map() {
        if (i2m != null) {
            return i2m;
        } else {
            AffineTransform i2m = new AffineTransform();
            i2m.translate(easting, northing);
            i2m.scale(pixelSizeX, pixelSizeY);
            i2m.rotate(Math.toRadians(-orientation));
            i2m.translate(-referencePixelX, -referencePixelY);
            return i2m;
        }
    }
    
    public Rectangle getImageRect() {
        return new Rectangle(width, height);
    }
    
    public CoordinateReferenceSystem getMapCrs() {
        return mapCrs;
    }
    
    void changeYAxisDirection() {
        pixelSizeY = -pixelSizeY;
    }
    
    public static ImageGeometry createTargetGeometry(Product sourceProduct, CoordinateReferenceSystem targetCrs,
                                              Double pixelSizeX, Double pixelSizeY, Integer width, Integer height,
                                              Double orientation, Double easting, Double northing,
                                              Double referencePixelX, Double referencePixelY) {
        Rectangle2D mapBoundary = createMapBoundary(sourceProduct, targetCrs);
        ImageGeometry ig = new ImageGeometry();
        ig.mapCrs = targetCrs;
        ig.orientation = orientation == null ? 0.0 : orientation;
        double mapW = mapBoundary.getWidth();
        double mapH = mapBoundary.getHeight();

        if (pixelSizeX == null || pixelSizeY == null) {
            // used float here to preserve same behavior as in old map-projection implementation
            // if double would be used scene size would differ sometimes by one pixel
            float pixelSize = (float) Math.min(mapW / sourceProduct.getSceneRasterWidth(),
                                               mapH / sourceProduct.getSceneRasterHeight());
            if (MathUtils.equalValues(pixelSize, 0.0f)) {
                pixelSize = 1.0f;
            }
            ig.pixelSizeX = (double)pixelSize;
            ig.pixelSizeY = (double)pixelSize;
        } else {
            ig.pixelSizeX = pixelSizeX;
            ig.pixelSizeY = pixelSizeY;
        }
        if (width == null) {
            ig.width = 1 + (int) Math.floor(mapW / ig.pixelSizeX);
        } else {
            ig.width = width;
        }
        if (height == null) {
            ig.height = 1 + (int) Math.floor(mapH / ig.pixelSizeY);
        } else {
            ig.height = height;
        }

        if (referencePixelX == null || referencePixelY == null) {
            ig.referencePixelX = 0.5 * ig.width;
            ig.referencePixelY = 0.5 * ig.height;
        } else {
            ig.referencePixelX = referencePixelX;
            ig.referencePixelY = referencePixelY;
        }
        if(easting == null || northing == null){
            ig.easting = mapBoundary.getX() + ig.referencePixelX * ig.pixelSizeX;
            ig.northing = (mapBoundary.getY() + mapBoundary.getHeight()) - ig.referencePixelY * ig.pixelSizeY;
        } else {
            ig.easting = easting;
            ig.northing = northing;
        }
        return ig;
    }

    public static ImageGeometry createCollocationTargetGeometry(Product collocationProduct) {
        GeoCoding geoCoding = collocationProduct.getGeoCoding();
        AffineTransform i2m = (AffineTransform) geoCoding.getImageToMapTransform();
        CoordinateReferenceSystem mapCRS = geoCoding.getMapCRS();
        ImageGeometry imageGeometry = new ImageGeometry();
        imageGeometry.mapCrs = mapCRS;
        imageGeometry.i2m = (AffineTransform) i2m.clone();
        imageGeometry.width = collocationProduct.getSceneRasterWidth();
        imageGeometry.height = collocationProduct.getSceneRasterHeight();
        return imageGeometry;
    }

    private static Rectangle2D createMapBoundary(Product product, CoordinateReferenceSystem targetCrs) {
        final int sourceW = product.getSceneRasterWidth();
        final int sourceH = product.getSceneRasterHeight();
        final int step = Math.min(sourceW, sourceH) / 10;
        MathTransform mathTransform;
        try {
            mathTransform = CRS.findMathTransform(product.getGeoCoding().getMapCRS(), targetCrs);
        } catch (FactoryException e) {
            throw new OperatorException(e);
        }
        try {
            final Point2D[] point2Ds = createMapBoundary(product, step, mathTransform);
            return new Rectangle2D.Double(point2Ds[0].getX(),
                                          point2Ds[0].getY(),
                                          point2Ds[1].getX() - point2Ds[0].getX(),
                                          point2Ds[1].getY() - point2Ds[0].getY());
        } catch (TransformException e) {
            throw new OperatorException(e);
        }
    }

    private static Point2D[] createMapBoundary(Product product, int step,
                                               MathTransform mathTransform) throws TransformException {
        GeoPos[] geoPoints = ProductUtils.createGeoBoundary(product, null, step);
        float[] geoPointsD = new float[geoPoints.length * 2];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPointsD[i * 2] = geoPoints[i].lon;
            geoPointsD[(i * 2) + 1] = geoPoints[i].lat;
        }
        float[] mapPointsD = new float[geoPoints.length * 2];
        mathTransform.transform(geoPointsD, 0, mapPointsD, 0, geoPoints.length);

        return getMinMax(mapPointsD);
    }

    private static Point2D[] getMinMax(float[] mapPointsD) {
        Point2D.Float min = new Point2D.Float();
        Point2D.Float max = new Point2D.Float();
        min.x = +Float.MAX_VALUE;
        min.y = +Float.MAX_VALUE;
        max.x = -Float.MAX_VALUE;
        max.y = -Float.MAX_VALUE;
        int i = 0;
        while (i < mapPointsD.length) {
            float pointX = mapPointsD[i++];
            float pointY = mapPointsD[i++];
            min.x = Math.min(min.x, pointX);
            min.y = Math.min(min.y, pointY);
            max.x = Math.max(max.x, pointX);
            max.y = Math.max(max.y, pointY);
        }
        return new Point2D[]{min, max};
    }
    
    // This code is simpler as the code currently used, but it does not consider the crossing of the 180° meridian.
    // This results in a map coverage of the whole earth.
    // todo - suggestion to simplify the currently used code are welcome
//    private Rectangle2D createMapBoundary(final Product product, CoordinateReferenceSystem targetCrs) {
//        try {
//            final CoordinateReferenceSystem sourceCrs = product.getGeoCoding().getImageCRS();
//            final int sourceW = product.getSceneRasterWidth();
//            final int sourceH = product.getSceneRasterHeight();
//
//            Rectangle2D rect = XRectangle2D.createFromExtremums(0.5, 0.5, sourceW - 0.5, sourceH - 0.5);
//            int pointsPerSide = Math.min(sourceH, sourceW) / 10;
//            pointsPerSide = Math.max(9, pointsPerSide);
//
//            final ReferencedEnvelope sourceEnvelope = new ReferencedEnvelope(rect, sourceCrs);
//            final ReferencedEnvelope targetEnvelope = sourceEnvelope.transform(targetCrs, true, pointsPerSide);
//            return new Rectangle2D.Double(targetEnvelope.getMinX(), targetEnvelope.getMinY(),
//                                          targetEnvelope.getWidth(), targetEnvelope.getHeight());
//        } catch (Exception e) {
//            throw new OperatorException(e);
//        }
//    }
}
