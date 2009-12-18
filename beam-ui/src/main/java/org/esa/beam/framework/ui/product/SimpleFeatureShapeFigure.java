package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.figure.AbstractShapeFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Lineal;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.Puntal;
import org.esa.beam.util.AwtGeomToJtsGeomConverter;
import org.esa.beam.util.Debug;
import org.geotools.geometry.jts.LiteShape2;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;

import java.awt.Shape;


public class SimpleFeatureShapeFigure extends AbstractShapeFigure implements SimpleFeatureFigure {

    private final SimpleFeature simpleFeature;
    private Shape geometryShape;
    private Class<?> geometryType;

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, FigureStyle style) {

        final Object geometry = simpleFeature.getDefaultGeometry();
        if (!(geometry instanceof Geometry)) {
            throw new IllegalArgumentException("simpleFeature: geometry type must be a " + Geometry.class);
        }

        this.simpleFeature = simpleFeature;
        this.geometryType = geometry.getClass();
        this.geometryShape = null;
        setRank(getRank((Geometry) simpleFeature.getDefaultGeometry()));
        setNormalStyle(style);
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Geometry getGeometry() {
        return (Geometry) simpleFeature.getDefaultGeometry();
    }

    @Override
    public void setGeometry(Geometry geometry) {
        if (!geometryType.isAssignableFrom(geometry.getClass())) {
            Debug.trace("WARNING: Assigning a geometry of type " + geometry.getClass() + ", should actually be a " + geometryType);
        }
        simpleFeature.setDefaultGeometry(geometry);
        forceRegeneration();
        fireFigureChanged();
    }

    @Override
    public Shape getShape() {
        try {
            if (geometryShape == null) {
                geometryShape = new LiteShape2(getGeometry(), null, null, true);
            }
            return geometryShape;
        } catch (Exception e) {
            throw new IllegalArgumentException("simpleFeature", e);
        }
    }

    @Override
    public void forceRegeneration() {
        geometryShape = null;
    }

    @Override
    public void setShape(Shape shape) {
        AwtGeomToJtsGeomConverter converter = new AwtGeomToJtsGeomConverter();
        Geometry geometry;
        // May need to handle more cases here in the future!  (nf)
        if (Polygon.class.isAssignableFrom(geometryType)) {
            geometry = converter.createPolygon(shape);
        } else if (MultiPolygon.class.isAssignableFrom(geometryType)) {
            geometry = converter.createMultiPolygon(shape);
        } else {
            geometry = converter.createMultiLineString(shape);
        }
        setGeometry(geometry);
    }

    static Rank getRank(Geometry geometry) {
        if (geometry instanceof Puntal) {
            return Rank.POINT;
        } else if (geometry instanceof Lineal) {
            return Rank.LINE;
        } else if (geometry instanceof Polygonal) {
            return Rank.AREA;
        } else {
            return Rank.NOT_SPECIFIED;
        }
    }
}
