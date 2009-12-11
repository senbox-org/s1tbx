package org.esa.beam.util;

import com.bc.ceres.binding.PropertySet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class PropertySetSimpleFeatureTest {

    public static final String PROPERTY_NAME_LABEL = "label";
    public static final String PROPERTY_NAME_PIXELPOS = "pixelPos";
    public static final String PROPERTY_NAME_GEOPOS = "geoPos";
    public static final String PROPERTY_NAME_PINSYMBOL = "pinSymbol";

    private PropertySetSimpleFeature simpleFeature;
    private PropertySet propertySet;
    private StringBuilder eventTracer;

    @Before
    public void setup() {
        final PixelPos pixelPos = new PixelPos(17.0f, 11.0f);
        final GeoPos geoPos = new GeoPos(19.0f, 67.0f);
        simpleFeature = new PropertySetSimpleFeature(createSimpleFeature("name", "label", pixelPos, geoPos));
        propertySet = simpleFeature.getPropertySet();
        eventTracer = new StringBuilder();
        propertySet.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (eventTracer.length() > 0) {
                    eventTracer.append(", ");
                }
                eventTracer.append(evt.getPropertyName());
            }
        });
    }

    @Test
    public void initialState() {
        assertNotNull(propertySet);
        assertEquals(3, propertySet.getProperties().length);

        assertEquals("label", propertySet.getValue(PROPERTY_NAME_LABEL));
        assertSame(simpleFeature.getAttribute(PROPERTY_NAME_LABEL), propertySet.getValue(PROPERTY_NAME_LABEL));

        final Point pixelPos = (Point) propertySet.getValue(PROPERTY_NAME_PIXELPOS);
        assertEquals(17.0, pixelPos.getX(), 0.0);
        assertEquals(11.0, pixelPos.getY(), 0.0);
        assertSame(simpleFeature.getAttribute(PROPERTY_NAME_PIXELPOS), pixelPos);

        final Point geoPos = (Point) propertySet.getValue(PROPERTY_NAME_GEOPOS);
        assertEquals(67.0, geoPos.getX(), 0.0);
        assertEquals(19.0, geoPos.getY(), 0.0);
        assertSame(simpleFeature.getAttribute(PROPERTY_NAME_GEOPOS), geoPos);
    }

    @Test
    public void featureAttributesAreChanged() {
        propertySet.setValue(PROPERTY_NAME_LABEL, "otherLabel");
        final Object propertyLabel = propertySet.getValue(PROPERTY_NAME_LABEL);
        assertEquals("otherLabel", propertyLabel);
        assertSame(propertyLabel, simpleFeature.getAttribute(PROPERTY_NAME_LABEL));
    }

    @Test
    public void propertiesAreChanged() {
        simpleFeature.setAttribute(PROPERTY_NAME_LABEL, "otherLabel");
        final Object featureLabel = simpleFeature.getAttribute(PROPERTY_NAME_LABEL);
        assertEquals("otherLabel", featureLabel);
        assertSame(featureLabel, propertySet.getValue(PROPERTY_NAME_LABEL));
    }

    @Test
    public void eventsAreFired() {
        assertEquals("", eventTracer.toString());
        propertySet.setValue(PROPERTY_NAME_LABEL, "otherLabel");
        assertEquals(PROPERTY_NAME_LABEL, eventTracer.toString());

        simpleFeature.setAttribute(PROPERTY_NAME_LABEL, "otherLabel");
        assertEquals(PROPERTY_NAME_LABEL, eventTracer.toString());

        simpleFeature.setAttribute(PROPERTY_NAME_LABEL, "yetAnotherLabel");
        assertEquals(PROPERTY_NAME_LABEL + ", " + PROPERTY_NAME_LABEL, eventTracer.toString());
    }

    private static SimpleFeature createSimpleFeature(String name, String label, PixelPos pixelPos, GeoPos geoPos) {
        final SimpleFeatureType featureType = createSimpleFeatureType("PinType", PROPERTY_NAME_PIXELPOS);
        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        if (label == null) {
            featureBuilder.set(PROPERTY_NAME_LABEL, "");
        } else {
            featureBuilder.set(PROPERTY_NAME_LABEL, label);
        }
        final GeometryFactory geometryFactory = new GeometryFactory();
        if (pixelPos != null) {
            final Coordinate coordinate = new Coordinate(pixelPos.getX(), pixelPos.getY());
            featureBuilder.set(PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(coordinate));
        }
        if (geoPos != null) {
            final Coordinate coordinate = new Coordinate(geoPos.getLon(), geoPos.getLat());
            featureBuilder.set(PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(coordinate));
        }

        return featureBuilder.buildFeature(name);
    }

    private static SimpleFeatureType createSimpleFeatureType(String typeName, String defaultGeometryName) {
        final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        builder.setCRS(crs);
        builder.setName(typeName);
        builder.add(PROPERTY_NAME_LABEL, String.class);
        builder.add(PROPERTY_NAME_PIXELPOS, Point.class, crs);
        builder.add(PROPERTY_NAME_GEOPOS, Point.class, crs);
        builder.setDefaultGeometry(defaultGeometryName);

        return builder.buildFeatureType();
    }
}
