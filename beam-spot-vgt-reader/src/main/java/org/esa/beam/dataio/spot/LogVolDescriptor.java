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

package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

final class LogVolDescriptor {
    private final PropertySet propertySet;
    private final String productId;

    private static final double PIXEL_CENTER = 0.5;
    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyyMMddHHmmss");

    public LogVolDescriptor(Reader reader) throws IOException {
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(reader);
        this.productId = getValueString("PRODUCT_ID");
    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    public String getValueString(String key) {
        return propertySet.getValue(key);
    }

    Integer getValueInteger(String key) {
        final String value = getValueString(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // ?
            }
        }
        return null;
    }

    Double getValueDouble(String key) {
        final String value = getValueString(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // ?
            }
        }
        return null;
    }

    Date getValueDate(String s) {
        String value = getValueString(s);
        if (value != null) {
            try {
                return DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                // ?
            }
        }
        return null;
    }

    Date getValueDate(String s1, String s2) {
        String value1 = getValueString(s1);
        String value2 = getValueString(s2);
        if (value1 != null && value2 != null) {
            try {
                return DATE_FORMAT.parse(value1 + value2);
            } catch (ParseException e) {
                // ?
            }
        }
        return null;
    }

    public String getProductId() {
        return productId;
    }

    public GeoCoding getGeoCoding() {

        String geodeticSystName = getValueString("GEODETIC_SYST_NAME");
        if (geodeticSystName == null) {
            return null;
        }

        if (geodeticSystName.equals("WGS 1984")) {

            String mapProjUnit = getValueString("MAP_PROJ_UNIT");
            if (mapProjUnit != null && !mapProjUnit.equals("DEGREES")) {
                return null;
            }

            Double meridianOrigin = getValueDouble("MERIDIAN_ORIGIN");
            if (meridianOrigin != null && meridianOrigin != 0.0) {
                return null;
            }

            Rectangle imageBounds = getImageBounds();
            if (imageBounds == null) {
                return null;
            }

            Double pixelSize = getValueDouble("MAP_PROJ_RESOLUTION");
            Double upperLeftLat = getValueDouble("GEO_UPPER_LEFT_LAT");
            Double upperLeftLon = getValueDouble("GEO_UPPER_LEFT_LONG");
            if (pixelSize != null
                    && upperLeftLat != null
                    && upperLeftLon != null) {
                try {
                    AffineTransform transform = new AffineTransform();
                    transform.translate(upperLeftLon, upperLeftLat);
                    transform.scale(pixelSize, -pixelSize);
                    transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);
                    return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, transform);
                } catch (TransformException e) {
                    // ?
                } catch (FactoryException e) {
                    // ?
                }
            }

        } else if (geodeticSystName.equals("UTM/UPS INTERNATIONAL 1909")) {

            String mapProjUnit = getValueString("MAP_PROJ_UNIT");
            if (mapProjUnit != null && !mapProjUnit.equals("METERS")) {
                return null;
            }

            Double meridianOrigin = getValueDouble("MERIDIAN_ORIGIN");
            if (meridianOrigin == null) {
                meridianOrigin = 0.0;
            }

            String spheroidName = getValueString("SPHEROID_NAME");
            if (spheroidName == null) {
                spheroidName = "INTERNATIONAL HAYFORD 1909";   // ?
            }

            Double semiMajAxis = getValueDouble("SPHEROID_SEMI_MAJ_AXIS");
            if (semiMajAxis == null) {
                semiMajAxis = DefaultGeographicCRS.WGS84.getDatum().getEllipsoid().getSemiMajorAxis();
            }

            Double semiMinAxis = getValueDouble("SPHEROID_SEMI_MIN_AXIS");
            if (semiMinAxis == null) {
                semiMinAxis = DefaultGeographicCRS.WGS84.getDatum().getEllipsoid().getSemiMinorAxis();
            }

            Double longOrigin = getValueDouble("PROJ_LONG_ORIGIN");
            if (longOrigin == null) {
                longOrigin = 0.0;
            }

            Double scaleFactor = getValueDouble("PROJ_SCALE_FACTOR");
            if (scaleFactor == null) {
                scaleFactor = 1.0;
            }

            Double xOrigin = getValueDouble("PROJ_X_ORIGIN");
            if (xOrigin == null) {
                xOrigin = 0.0;
            }

            Double yOrigin = getValueDouble("PROJ_Y_ORIGIN");
            if (yOrigin == null) {
                yOrigin = 0.0;
            }

            Rectangle imageBounds = getImageBounds();
            if (imageBounds == null) {
                return null;
            }

            Double pixelSize = getValueDouble("MAP_PROJ_RESOLUTION");
            Double upperLeftLon = getValueDouble("CARTO_UPPER_LEFT_X");
            Double upperLeftLat = getValueDouble("CARTO_UPPER_LEFT_Y");
            String projHemi = getValueString("PROJ_HEMI");
            String meridianName = getValueString("MERIDIAN_NAME");
            if (pixelSize != null
                    && upperLeftLat != null
                    && upperLeftLon != null
                    && projHemi != null
                    && meridianName != null) {
                AffineTransform transform = new AffineTransform();
                transform.translate(upperLeftLon, upperLeftLat);
                transform.scale(pixelSize, -pixelSize);
                transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);

                double inverseFlattening = semiMajAxis / (semiMajAxis - semiMinAxis);

                String projectionName;
                double standardParallel;
                if (projHemi.equalsIgnoreCase("SOUTH")) {
                    projectionName = "Stereographic_South_Pole";
                    standardParallel = -90.0;
                } else {
                    projectionName = "Stereographic_North_Pole";
                    standardParallel = +90.0;
                }

                String upsWktPattern = "PROJCS[\"%s\", \n" +
                        "  GEOGCS[\"%s\", \n" +
                        "    DATUM[\"%s\", \n" +
                        "      SPHEROID[\"%s\", %f, %f]], \n" +
                        "    PRIMEM[\"%s\", %f], \n" +
                        "    UNIT[\"degree\", 0.017453292519943295], \n" +
                        "    AXIS[\"Geodetic longitude\", EAST], \n" +
                        "    AXIS[\"Geodetic latitude\", NORTH]], \n" +
                        "  PROJECTION[\"%s\"], \n" +
                        "  PARAMETER[\"central_meridian\", %f], \n" +
                        "  PARAMETER[\"standard_parallel_1\", %f], \n" +
                        "  PARAMETER[\"scale_factor\", %f], \n" +
                        "  PARAMETER[\"false_easting\", %f], \n" +
                        "  PARAMETER[\"false_northing\", %f], \n" +
                        "  UNIT[\"m\", 1.0], \n" +
                        "  AXIS[\"Easting\", EAST], \n" +
                        "  AXIS[\"Northing\", NORTH] \n" +
                        "]";
                String upsWkt = String.format(upsWktPattern,
                                              geodeticSystName,
                                              spheroidName,
                                              spheroidName,
                                              spheroidName,
                                              semiMajAxis,
                                              inverseFlattening,
                                              meridianName,
                                              meridianOrigin,
                                              projectionName,
                                              longOrigin,
                                              standardParallel,
                                              scaleFactor,
                                              xOrigin,
                                              yOrigin);
                try {
                    final CoordinateReferenceSystem crs = CRS.parseWKT(upsWkt);
                    return new CrsGeoCoding(crs, imageBounds, transform);
                } catch (Throwable t) {
                    Debug.trace("upsWkt = " + upsWkt);
                    Debug.trace(t);
                }
            }
        }
        return null;
    }

    public Date getStartDate() {
        Date date = getValueDate("SYNTHESIS_FIRST_DATE");
        if (date == null) {
            date = getValueDate("SEGM_FIRST_DATE", "SEGM_FIRST_TIME");
        }
        return date;
    }

    public Date getEndDate() {
        Date date = getValueDate("SYNTHESIS_LAST_DATE");
        if (date == null) {
            date = getValueDate("SEGM_LAST_DATE", "SEGM_LAST_TIME");
        }
        return date;
    }

    Rectangle getImageBounds() {
        Integer upperLeftCol = getValueInteger("IMAGE_UPPER_LEFT_COL");
        Integer upperLeftRow = getValueInteger("IMAGE_UPPER_LEFT_ROW");
        Integer lowerRightCol = getValueInteger("IMAGE_LOWER_RIGHT_COL");
        Integer lowerRightRow = getValueInteger("IMAGE_LOWER_RIGHT_ROW");
        if (upperLeftCol != null
                && upperLeftRow != null
                && lowerRightCol != null
                && lowerRightRow != null) {
            return new Rectangle(upperLeftCol - 1, upperLeftRow - 1,
                                 lowerRightCol - upperLeftCol + 1,
                                 lowerRightRow - upperLeftRow + 1);
        }
        return null;
    }

}
