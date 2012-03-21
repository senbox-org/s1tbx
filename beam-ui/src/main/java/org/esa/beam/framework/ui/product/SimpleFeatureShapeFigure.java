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

import java.awt.Shape;

public class SimpleFeatureShapeFigure extends AbstractShapeFigure implements SimpleFeatureFigure {

    private final SimpleFeature simpleFeature;
    private Shape geometryShape;
    private final Class<?> geometryType;

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, FigureStyle style) {
        this(simpleFeature, style, style);
    }

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(getRank(simpleFeature), normalStyle, selectedStyle);
        this.simpleFeature = simpleFeature;
        this.geometryType = simpleFeature.getDefaultGeometry().getClass();
        this.geometryShape = null;
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

    static Rank getRank(SimpleFeature simpleFeature) {
        final Object geometry = simpleFeature.getDefaultGeometry();
        if (!(geometry instanceof Geometry)) {
            throw new IllegalArgumentException("simpleFeature: geometry type must be a " + Geometry.class);
        }
        return getRank((Geometry) geometry);
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
