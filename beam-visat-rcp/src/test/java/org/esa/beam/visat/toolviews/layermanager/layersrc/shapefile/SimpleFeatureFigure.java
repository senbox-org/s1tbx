package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.vividsolutions.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;


public interface SimpleFeatureFigure {

    SimpleFeature getSimpleFeature();

    Geometry getGeometry();
}