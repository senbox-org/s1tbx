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

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.util.Debug;
import org.geotools.factory.Hints;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.AbstractCoordinateOperationFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.CRSUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * A geo-coding that is based on a well-known coordinate reference system (CRS) and affine transformation used to
 * locate a product's scene image within the CRS.
 *
 * @since BEAM 4.7
 */
public class CrsGeoCoding extends AbstractGeoCoding {

    private final Rectangle imageBounds;
    private final AffineTransform imageToMap;
    private final MathTransform imageToGeo;
    private final MathTransform geoToImage;
    private final Datum datum;
    private final boolean crossingMeridianAt180;

    /**
     * Constructs a new instance of this class from a map CRS, image dimension, and image-to-map
     * transformation parameters.
     * <p/>
     * The reference pixel is set to the BEAM default value, namely the center of the upper left image pixel
     * (i.e. {@code referencePixelX = referencePixelY = 0.5}).
     *
     * @param mapCRS      the map CRS.
     * @param imageWidth  the width of the image.
     * @param imageHeight the height of the image.
     * @param easting     the easting of the reference pixel position.
     * @param northing    the northing of the reference pixel position.
     * @param pixelSizeX  the size of a pixel along the x-axis in map units.
     * @param pixelSizeY  the size of a pixel along the y-axis in map units (negative, if positive image Y-axis points up).
     *
     * @throws FactoryException   when an error occurred.
     * @throws TransformException when an error occurred.
     */
    public CrsGeoCoding(CoordinateReferenceSystem mapCRS,
                        int imageWidth,
                        int imageHeight,
                        double easting,
                        double northing,
                        double pixelSizeX,
                        double pixelSizeY) throws FactoryException, TransformException {
        this(mapCRS, imageWidth, imageHeight, easting, northing, pixelSizeX, pixelSizeY, 0.5, 0.5);
    }

    /**
     * Constructs a new instance of this class from a map CRS, image dimension, and image-to-map
     * transformation parameters.
     *
     * @param mapCRS          the map CRS.
     * @param imageWidth      the width of the image.
     * @param imageHeight     the height of the image.
     * @param easting         the easting of the reference pixel position.
     * @param northing        the northing of the reference pixel position.
     * @param pixelSizeX      the size of a pixel along the x-axis in map units.
     * @param pixelSizeY      the size of a pixel along the y-axis in map units (negative, if positive image Y-axis points up).
     * @param referencePixelX the x-position of the reference pixel.
     * @param referencePixelY the y-position of the reference pixel.
     *
     * @throws FactoryException   when an error occurred.
     * @throws TransformException when an error occurred.
     */
    public CrsGeoCoding(CoordinateReferenceSystem mapCRS,
                        int imageWidth,
                        int imageHeight,
                        double easting,
                        double northing,
                        double pixelSizeX,
                        double pixelSizeY,
                        double referencePixelX,
                        double referencePixelY) throws FactoryException, TransformException {
        this(mapCRS,
             new Rectangle(imageWidth, imageHeight),
             createImageToMapTransform(easting, northing, pixelSizeX, pixelSizeY, referencePixelX, referencePixelY));
    }

    public CrsGeoCoding(final CoordinateReferenceSystem mapCRS,
                        final Rectangle imageBounds,
                        final AffineTransform imageToMap) throws FactoryException, TransformException {
        this.imageBounds = imageBounds;
        this.imageToMap = imageToMap;
        setMapCRS(mapCRS);
        org.opengis.referencing.datum.Ellipsoid gtEllipsoid = CRS.getEllipsoid(mapCRS);
        String ellipsoidName = gtEllipsoid.getName().getCode();
        Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName,
                                            gtEllipsoid.getSemiMinorAxis(),
                                            gtEllipsoid.getSemiMajorAxis());
        org.opengis.referencing.datum.Datum gtDatum = CRSUtilities.getDatum(mapCRS);
        String datumName = gtDatum.getName().getCode();
        this.datum = new Datum(datumName, ellipsoid, 0, 0, 0);

        MathTransform i2m = new AffineTransform2D(imageToMap);

        if (mapCRS instanceof DerivedCRS) {
            DerivedCRS derivedCRS = (DerivedCRS) mapCRS;
            CoordinateReferenceSystem baseCRS = derivedCRS.getBaseCRS();
            setGeoCRS(baseCRS);
        } else if (gtDatum instanceof GeodeticDatum) {
            setGeoCRS(new DefaultGeographicCRS((GeodeticDatum) gtDatum, DefaultEllipsoidalCS.GEODETIC_2D));
        } else {
            // Fallback
            setGeoCRS(DefaultGeographicCRS.WGS84);
        }

        setImageCRS(createImageCRS(mapCRS, i2m.inverse()));

        MathTransform map2Geo = CRS.findMathTransform(mapCRS, getGeoCRS(), true);
        Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

        final CoordinateOperationFactory factory = ReferencingFactoryFinder.getCoordinateOperationFactory(hints);
        final MathTransformFactory mtFactory;
        if (factory instanceof AbstractCoordinateOperationFactory) {
            mtFactory = ((AbstractCoordinateOperationFactory) factory).getMathTransformFactory();
        } else {
            mtFactory = ReferencingFactoryFinder.getMathTransformFactory(hints);
        }
        imageToGeo = mtFactory.createConcatenatedTransform(i2m, map2Geo);
        geoToImage = imageToGeo.inverse();
        crossingMeridianAt180 = detect180MeridianCrossing();
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return new AffineTransform2D(imageToMap);
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        final AffineTransform destTransform = new AffineTransform(imageToMap);
        Rectangle destBounds = new Rectangle(destScene.getRasterWidth(), destScene.getRasterHeight());

        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            double scaleX = subsetDef.getSubSamplingX();
            double scaleY = subsetDef.getSubSamplingY();
            if (region != null) {
                destTransform.translate(region.getX(), region.getY());
                destBounds.setRect(0, 0, region.getWidth() / scaleX, region.getHeight() / scaleY);
            }
            destTransform.scale(scaleX, scaleY);
        }

        try {
            destScene.setGeoCoding(new CrsGeoCoding(getMapCRS(), destBounds, destTransform));
        } catch (Exception e) {
            Debug.trace(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

    @Override
    public Datum getDatum() {
        return datum;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        try {
            DirectPosition directPixelPos = new DirectPosition2D(pixelPos);
            DirectPosition directGeoPos = imageToGeo.transform(directPixelPos, null);
            geoPos.setLocation((float) directGeoPos.getOrdinate(1), (float) directGeoPos.getOrdinate(0));
        } catch (Exception ignored) {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        try {
            DirectPosition directGeoPos = new DirectPosition2D(geoPos.getLon(), geoPos.getLat());
            DirectPosition directPixelPos = geoToImage.transform(directGeoPos, null);
            pixelPos.setLocation((float) directPixelPos.getOrdinate(0), (float) directPixelPos.getOrdinate(1));
        } catch (Exception ignored) {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    public final void getPixels(final int x1, final int y1, final int w, final int h,
                                final float[] latPixels, final float[] lonPixels) {
        final DirectPosition2D directPixPos = new DirectPosition2D();
        final DirectPosition directGeoPos = new GeneralDirectPosition(0,0);
        final int x2 = x1 + w;
        final int y2 = y1 + h;
        int pos = 0;
        for (int y = y1; y < y2; ++y) {
            final double yp = y + 0.5;
            for (int x = x1; x < x2; ++x) {
                try {
                    directPixPos.setLocation(x + 0.5, yp);
                    imageToGeo.transform(directPixPos, directGeoPos);
                    latPixels[pos] = (float) directGeoPos.getOrdinate(1);
                    lonPixels[pos] = (float) directGeoPos.getOrdinate(0);
                } catch (Exception ignored) {
                    latPixels[pos] = Float.NaN;
                    lonPixels[pos] = Float.NaN;
                }
                ++pos;
            }
        }
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return crossingMeridianAt180;
    }

    @Override
    public String toString() {
        final String s = super.toString();
        return s + "\n\n" +
               "Map CRS:\n" + getMapCRS().toString() + "\n" +
               "Image To Map:\n" + imageToMap.toString();

    }

    private boolean detect180MeridianCrossing() throws TransformException, FactoryException {
        final Rectangle bounds = this.imageBounds;

        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(bounds.getMinX() + 0.5,
                                                                       bounds.getMaxX() - 0.5,
                                                                       bounds.getMinY() + 0.5,
                                                                       bounds.getMaxY() - 0.5,
                                                                       getImageCRS());
        referencedEnvelope = referencedEnvelope.transform(getGeoCRS(), true);
        final DirectPosition uc = referencedEnvelope.getUpperCorner();
        final DirectPosition lc = referencedEnvelope.getLowerCorner();
        return uc.getOrdinate(0) > 180 || lc.getOrdinate(0) < -180;
    }

    private static AffineTransform createImageToMapTransform(double easting,
                                                             double northing,
                                                             double pixelSizeX,
                                                             double pixelSizeY,
                                                             double referencePixelX,
                                                             double referencePixelY) {
        final AffineTransform at = new AffineTransform();
        at.translate(easting, northing);
        at.scale(pixelSizeX, -pixelSizeY);
        at.translate(-referencePixelX, -referencePixelY);
        return at;
    }
}
