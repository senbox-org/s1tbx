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

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PlainFeatureFactory {
    public static final String DEFAULT_TYPE_NAME = "Geometry";

    public static final String ATTRIB_NAME_GEOMETRY = "geometry";
    public static final String ATTRIB_NAME_STYLE_CSS = "style_css";


    public static SimpleFeatureType createDefaultFeatureType() {
        return createDefaultFeatureType(DefaultGeographicCRS.WGS84);
    }

    public static SimpleFeatureType createDefaultFeatureType(CoordinateReferenceSystem crs) {
        return createPlainFeatureType(DEFAULT_TYPE_NAME, Geometry.class, crs);
    }

    public static SimpleFeatureType createPlainFeatureType(String typeName,
                                                           Class<? extends Geometry> geometryType,
                                                           CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        if (crs != null) {
            sftb.setCRS(crs);
        }
        sftb.setName(typeName);
        sftb.add(ATTRIB_NAME_GEOMETRY, geometryType);
        sftb.add(ATTRIB_NAME_STYLE_CSS, String.class);
        sftb.setDefaultGeometry(ATTRIB_NAME_GEOMETRY);
        return sftb.buildFeatureType();
    }

    public static SimpleFeature createPlainFeature(SimpleFeatureType type, String id, Geometry geometry, String styleCSS) {
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(type);
        sfb.set(ATTRIB_NAME_GEOMETRY, geometry);
        sfb.set(ATTRIB_NAME_STYLE_CSS, styleCSS != null ? styleCSS : " ");
        return sfb.buildFeature(id);
    }

}