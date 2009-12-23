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