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

package org.esa.snap.core.dataop.maptransf.geotools;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.dataop.maptransf.AffineTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.AlbersEqualAreaConicDescriptor;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.LambertConformalConicDescriptor;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.dataop.maptransf.StereographicDescriptor;
import org.esa.snap.core.dataop.maptransf.TransverseMercatorDescriptor;
import org.esa.snap.core.dataop.maptransf.UTMProjection;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import java.awt.geom.AffineTransform;

/**
 * @deprecated since BEAM 4.7, use {@link CrsGeoCoding} instead.
 */
@Deprecated
public class CoordinateReferenceSystems {

    private static final GeographicCRS ITRF97 = new DefaultGeographicCRS(GeodeticDatums.ITRF97,
                                                                         DefaultEllipsoidalCS.GEODETIC_2D);
    private static final GeographicCRS WGS72 = new DefaultGeographicCRS(GeodeticDatums.WGS72,
                                                                        DefaultEllipsoidalCS.GEODETIC_2D);
    private static final GeographicCRS WGS84 = DefaultGeographicCRS.WGS84;

    public static CoordinateReferenceSystem getCRS(MapProjection projection, Datum datum) {
        CoordinateReferenceSystem result = WGS84;

        try {
            final MapTransform mapTransform = projection.getMapTransform();
            if (mapTransform.getDescriptor() instanceof IdentityTransformDescriptor) {
                // 1. Identity map projection
                if (Datum.ITRF_97.equals(datum)) {
                    result = ITRF97;
                } else if (Datum.WGS_72.equals(datum)) {
                    result = WGS72;
                }
            } else if (projection instanceof UTMProjection && !Datum.ITRF_97.equals(datum)) {
                // 2. UTM map projections
                final UTMProjection utmProjection = (UTMProjection) projection;
                final int zone = utmProjection.getZone();

                if (zone >= 1 && zone <= 60) {
                    final CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
                    if (utmProjection.isNorth()) {
                        if (Datum.WGS_72.equals(datum)) {
                            final int WGS72_UTM_zone_N_BASE = 32200;
                            result = factory.createProjectedCRS("EPSG:" + (WGS72_UTM_zone_N_BASE + zone));
                        } else if (Datum.WGS_84.equals(datum)) {
                            final int WGS84_UTM_zone_N_BASE = 32600;
                            result = factory.createProjectedCRS("EPSG:" + (WGS84_UTM_zone_N_BASE + zone));
                        }
                    } else {
                        if (Datum.WGS_72.equals(datum)) {
                            final int WGS72_UTM_zone_S_BASE = 32300;
                            result = factory.createProjectedCRS("EPSG:" + (WGS72_UTM_zone_S_BASE + zone));
                        } else if (Datum.WGS_84.equals(datum)) {
                            final int WGS84_UTM_zone_S_BASE = 32700;
                            result = factory.createProjectedCRS("EPSG:" + (WGS84_UTM_zone_S_BASE + zone));
                        }
                    }
                }
            } else if (Datum.ITRF_97.equals(datum)) {
                // 3. Other map projections
                final String crsName = "ITRF 97 / " + mapTransform.getDescriptor().getName();
                final MathTransform mathTransform = getMathTransform(mapTransform);
                if (mathTransform != null) {
                    result = new DefaultProjectedCRS(crsName, ITRF97, mathTransform, DefaultCartesianCS.PROJECTED);
                }
            } else if (Datum.WGS_72.equals(datum)) {
                final String crsName = "WGS 72 / " + mapTransform.getDescriptor().getName();
                final MathTransform mathTransform = getMathTransform(mapTransform);
                if (mathTransform != null) {
                    result = new DefaultProjectedCRS(crsName, WGS72, mathTransform, DefaultCartesianCS.PROJECTED);
                }
            } else if (Datum.WGS_84.equals(datum)) {
                final String crsName = "WGS 84 / " + mapTransform.getDescriptor().getName();
                final MathTransform mathTransform = getMathTransform(mapTransform);
                if (mathTransform != null) {
                    result = new DefaultProjectedCRS(crsName, WGS84, mathTransform, DefaultCartesianCS.PROJECTED);
                }
            }
        } catch (FactoryException e) {
            // ignore
        }

        return result;
    }

    private static MathTransform getMathTransform(MapTransform mapTransform) throws FactoryException {
        if (mapTransform.getDescriptor() instanceof AffineTransformDescriptor) {
            return new AffineTransform2D(new AffineTransform(mapTransform.getParameterValues()));
        }
        if (mapTransform instanceof AlbersEqualAreaConicDescriptor.AEAC) {
            return createAlbersConicEqualAreaMathTransform((AlbersEqualAreaConicDescriptor.AEAC) mapTransform);
        }
        if (mapTransform instanceof LambertConformalConicDescriptor.LCCT) {
            return createLambertConformalConicMathTransform((LambertConformalConicDescriptor.LCCT) mapTransform);
        }
        if (mapTransform instanceof StereographicDescriptor.ST) {
            return createStereographicMathTransform((StereographicDescriptor.ST) mapTransform);
        }
        if (mapTransform instanceof TransverseMercatorDescriptor.TMT) {
            return createTransverseMercatorMathTransform((TransverseMercatorDescriptor.TMT) mapTransform);
        }

        return null;
    }

    private static MathTransform createAlbersConicEqualAreaMathTransform(AlbersEqualAreaConicDescriptor.AEAC t)
            throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters("EPSG:9822");

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("standard_parallel_1").setValue(t.getStandardParallel1());
        parameters.parameter("standard_parallel_2").setValue(t.getStandardParallel2());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

    private static MathTransform createLambertConformalConicMathTransform(LambertConformalConicDescriptor.LCCT t)
            throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters("ESRI:Lambert_Conformal_Conic");

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("standard_parallel_1").setValue(t.getStandardParallel1());
        parameters.parameter("standard_parallel_2").setValue(t.getStandardParallel2());
        parameters.parameter("scale_factor").setValue(t.getScaleFactor());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

    private static MathTransform createStereographicMathTransform(StereographicDescriptor.ST t)
            throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters;

        if (t.isPolar()) {
            parameters = transformFactory.getDefaultParameters("EPSG:9810");
        } else {
            parameters = transformFactory.getDefaultParameters("EPSG:9809");
        }

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("scale_factor").setValue(t.getScaleFactor());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

    private static MathTransform createTransverseMercatorMathTransform(TransverseMercatorDescriptor.TMT t)
            throws FactoryException {

        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters("EPSG:9807");

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("scale_factor").setValue(t.getScaleFactor());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

}
