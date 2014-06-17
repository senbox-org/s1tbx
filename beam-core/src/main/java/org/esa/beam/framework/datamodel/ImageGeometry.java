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
package org.esa.beam.framework.datamodel;

import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.resources.geometry.XRectangle2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    public ImageGeometry(Rectangle bounds, CoordinateReferenceSystem mapCrs, AffineTransform image2map) {
        this.i2m = image2map;
        this.width = bounds.width;
        this.height = bounds.height;
        this.mapCrs = mapCrs;
    }

    public AffineTransform getImage2MapTransform() {
        if (i2m != null) {
            return i2m;
        } else {
            return createImageToMapTransform(referencePixelX, referencePixelY, easting, northing, pixelSizeX, pixelSizeY, orientation);
        }
    }

    public Rectangle getImageRect() {
        return new Rectangle(width, height);
    }

    public CoordinateReferenceSystem getMapCrs() {
        return mapCrs;
    }

    public void changeYAxisDirection() {
        pixelSizeY = -pixelSizeY;
    }

    public double getReferencePixelX() {
        return referencePixelX;
    }

    public double getReferencePixelY() {
        return referencePixelY;
    }

    public double getEasting() {
        return easting;
    }

    public double getNorthing() {
        return northing;
    }

    public double getOrientation() {
        return orientation;
    }

    public double getPixelSizeX() {
        return pixelSizeX;
    }

    public double getPixelSizeY() {
        return pixelSizeY;
    }

    public static Point2D calculateEastingNorthing(Product sourceProduct, CoordinateReferenceSystem targetCrs,
                                                   double referencePixelX, double referencePixelY,
                                                   double pixelSizeX, double pixelSizeY) {
        Rectangle2D mapBoundary = createMapBoundary(sourceProduct, targetCrs);
        double easting = mapBoundary.getX() + referencePixelX * pixelSizeX;
        double northing = (mapBoundary.getY() + mapBoundary.getHeight()) - referencePixelY * pixelSizeY;
        return new Point2D.Double(easting, northing);
    }

    public static Rectangle calculateProductSize(Product sourceProduct, CoordinateReferenceSystem targetCrs,
                                                 double pixelSizeX, double pixelSizeY) {
        Rectangle2D mapBoundary = createMapBoundary(sourceProduct, targetCrs);
        double mapW = mapBoundary.getWidth();
        double mapH = mapBoundary.getHeight();
        int width = 1 + (int) Math.floor(mapW / pixelSizeX);
        int height = 1 + (int) Math.floor(mapH / pixelSizeY);
        return new Rectangle(width, height);
    }

    public static ImageGeometry createTargetGeometry(Product sourceProduct, CoordinateReferenceSystem targetCrs,
                                                     Double pixelSizeX, Double pixelSizeY, Integer width,
                                                     Integer height,
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
            ig.pixelSizeX = (double) pixelSize;
            ig.pixelSizeY = (double) pixelSize;
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
        if (easting == null || northing == null) {
            ig.easting = mapBoundary.getX() + ig.referencePixelX * ig.pixelSizeX;
            ig.northing = (mapBoundary.getY() + mapBoundary.getHeight()) - ig.referencePixelY * ig.pixelSizeY;
        } else {
            ig.easting = easting;
            ig.northing = northing;
        }
        return ig;
    }

    public static ImageGeometry createCollocationTargetGeometry(Product targetProduct, Product collocationProduct) {
        GeoCoding geoCoding = collocationProduct.getGeoCoding();
        final AffineTransform modelTransform = ImageManager.getImageToModelTransform(geoCoding);
        final double pixelSizeX = modelTransform.getScaleX();
        final double pixelSizeY = modelTransform.getScaleY();
        final int width = collocationProduct.getSceneRasterWidth();
        final int height = collocationProduct.getSceneRasterHeight();
        final double easting = modelTransform.getTranslateX();
        final double northing = modelTransform.getTranslateY();
        final double referencePixelY = 0.0;
        final double referencePixelX = 0.0;
        return ImageGeometry.createTargetGeometry(targetProduct,
                                                  ImageManager.getModelCrs(geoCoding),
                                                  pixelSizeX, pixelSizeY,
                                                  width, height,
                                                  null,
                                                  easting, northing,
                                                  referencePixelX, referencePixelY);

    }


    private static Rectangle2D createMapBoundary(final Product product, CoordinateReferenceSystem targetCrs) {
        try {
            final CoordinateReferenceSystem sourceCrs = product.getGeoCoding().getImageCRS();
            final int sourceW = product.getSceneRasterWidth();
            final int sourceH = product.getSceneRasterHeight();
            final Rectangle2D rect = XRectangle2D.createFromExtremums(0.5, 0.5, sourceW - 0.5, sourceH - 0.5);
            int pointsPerSide = Math.max(sourceH, sourceW) / 10;
            pointsPerSide = Math.max(9, pointsPerSide);
            final ReferencedEnvelope sourceEnvelope = new ReferencedEnvelope(rect, sourceCrs);
            final ReferencedEnvelope targetEnvelope = sourceEnvelope.transform(targetCrs, true, pointsPerSide);
            double minX = targetEnvelope.getMinX();
            double width = targetEnvelope.getWidth();
            if (product.getGeoCoding().isCrossingMeridianAt180()) {
                minX = -180.0;
                width = 360;
            }
            return new Rectangle2D.Double(minX, targetEnvelope.getMinY(), width, targetEnvelope.getHeight());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static AffineTransform createImageToMapTransform(double referencePixelX,
                                                     double referencePixelY,
                                                     double easting,
                                                     double northing,
                                                     double pixelSizeX,
                                                     double pixelSizeY,
                                                     double orientation) {
        AffineTransform i2m = new AffineTransform();
        i2m.translate(easting, northing);
        i2m.scale(pixelSizeX, pixelSizeY);
        i2m.rotate(Math.toRadians(-orientation));
        i2m.translate(-referencePixelX, -referencePixelY);
        return i2m;
    }
}
