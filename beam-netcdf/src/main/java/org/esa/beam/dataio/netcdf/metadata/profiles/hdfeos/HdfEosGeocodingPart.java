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

package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.jdom.Element;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.List;


public class HdfEosGeocodingPart extends ProfilePartIO {

    private static final double PIXEL_CENTER = 0.5;

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        Element eosElement = (Element) ctx.getProperty(HdfEosUtils.STRUCT_METADATA);
        Element gridStructure = eosElement.getChild("GridStructure");
        Element gridElem = (Element) gridStructure.getChildren().get(0);
        Element projectionElem = gridElem.getChild("Projection");
        Element ulPointElem = gridElem.getChild("UpperLeftPointMtrs");
        Element lrPointElem = gridElem.getChild("LowerRightMtrs");
        if (projectionElem == null || ulPointElem == null || lrPointElem == null) {
            return;
        }

        List<Element> ulList = ulPointElem.getChildren();
        String ulLon = ulList.get(0).getValue();
        String ulLat = ulList.get(1).getValue();
        double upperLeftLon = Double.parseDouble(ulLon);
        double upperLeftLat = Double.parseDouble(ulLat);

        List<Element> lrList = lrPointElem.getChildren();
        String lrLon = lrList.get(0).getValue();
        String lrLat = lrList.get(1).getValue();
        double lowerRightLon = Double.parseDouble(lrLon);
        double lowerRightLat = Double.parseDouble(lrLat);

        String projection = projectionElem.getValue();

        attachGeoCoding(p, upperLeftLon, upperLeftLat, lowerRightLon, lowerRightLat, projection);
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }

    public static void attachGeoCoding(Product p,
                                       double upperLeftLon,
                                       double upperLeftLat,
                                       double lowerRightLon,
                                       double lowerRightLat,
                                       String projection) {
        double pixelSizeX = (lowerRightLon - upperLeftLon) / p.getSceneRasterWidth();
        double pixelSizeY = (upperLeftLat - lowerRightLat) / p.getSceneRasterHeight();

        AffineTransform transform = new AffineTransform();
        transform.translate(upperLeftLon, upperLeftLat);
        transform.scale(pixelSizeX, -pixelSizeY);
        transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);
        Rectangle imageBounds = new Rectangle(p.getSceneRasterWidth(), p.getSceneRasterHeight());

        if (projection.equals("GCTP_GEO")) {
            if ((upperLeftLon >= -180 && upperLeftLon <= 180) && (upperLeftLat >= -90 && upperLeftLat <= 90) &&
                    (lowerRightLon >= -180 && lowerRightLon <= 180) && (lowerRightLat >= -90 && lowerRightLat <= 90)) {
                try {
                    p.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, transform));
                } catch (FactoryException ignore) {
                } catch (TransformException ignore) {
                }
            }
        } else if (projection.equals("GCTP_SNSOID")) {
            DefaultGeographicCRS base = DefaultGeographicCRS.WGS84;
            final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
            ParameterValueGroup parameters;
            try {
                parameters = transformFactory.getDefaultParameters("OGC:Sinusoidal");
            } catch (NoSuchIdentifierException ignore) {
                return;
            }
            DefaultEllipsoid ellipsoid = (DefaultEllipsoid) base.getDatum().getEllipsoid();
            parameters.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
            parameters.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());

            MathTransform mathTransform;
            try {
                mathTransform = transformFactory.createParameterizedTransform(parameters);
            } catch (Exception ignore) {
                return;
            }

            CoordinateReferenceSystem modelCrs = new DefaultProjectedCRS("Sinusoidal", base, mathTransform,
                                                                         DefaultCartesianCS.PROJECTED);
            try {
                CrsGeoCoding geoCoding = new CrsGeoCoding(modelCrs, imageBounds, transform);

                p.setGeoCoding(geoCoding);
            } catch (Exception ignore) {
            }
        }
    }

}
