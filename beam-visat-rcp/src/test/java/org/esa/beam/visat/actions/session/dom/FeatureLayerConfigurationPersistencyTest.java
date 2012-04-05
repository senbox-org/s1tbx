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

package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayer;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayerType;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;

import java.io.IOException;
import java.net.URL;

public class FeatureLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public FeatureLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(FeatureLayerType.class));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {

        final PropertySet configuration = layerType.createLayerConfig(null);

        final URL shapefileUrl = getClass().getResource("bundeslaender.shp");
        configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_URL, shapefileUrl);
        configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CRS, DefaultGeographicCRS.WGS84);
        final Coordinate[] coordinates = {
                new Coordinate(-10, 50),
                new Coordinate(+10, 50),
                new Coordinate(+10, 30),
                new Coordinate(-10, 30),
                new Coordinate(-10, 50)
        };
        final GeometryFactory geometryFactory = new GeometryFactory();
        final LinearRing ring = geometryFactory.createLinearRing(coordinates);
        final Polygon clipGeometry = geometryFactory.createPolygon(ring, new LinearRing[0]);
        configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY, clipGeometry);
        configuration.setValue(FeatureLayerType.PROPERTY_NAME_SLD_STYLE, createStyle());
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
        try {
            fc = FeatureUtils.createFeatureCollection(
                    shapefileUrl, DefaultGeographicCRS.WGS84, clipGeometry);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return new FeatureLayer(layerType, fc, configuration);
    }

    @SuppressWarnings({"deprecation"})
    private static Style createStyle() {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
        PolygonSymbolizer symbolizer = styleFactory.createPolygonSymbolizer();
        Fill fill = styleFactory.createFill(
                filterFactory.literal("#FFAA00"),
                filterFactory.literal(0.5)
        );
        symbolizer.setFill(fill);
        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

}
