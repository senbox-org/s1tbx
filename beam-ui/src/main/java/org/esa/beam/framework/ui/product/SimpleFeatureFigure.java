package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.figure.Figure;
import com.vividsolutions.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;


public interface SimpleFeatureFigure extends Figure {

    SimpleFeature getSimpleFeature();

    Geometry getGeometry();
}