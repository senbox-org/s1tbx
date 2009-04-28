package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.FeatureLayerType;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.FilterFactory;

public class FeatureLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public FeatureLayerConfigurationPersistencyTest() {
        super(LayerType.getLayerType(FeatureLayerType.class.getName()));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {

        final ValueContainer configuration = layerType.getConfigurationTemplate();

        configuration.setValue(FeatureLayerType.PROPERTY_FEATURE_COLLECTION_URL,
                               getClass().getResource("bundeslaender.shp"));
        configuration.setValue(FeatureLayerType.PROPERTY_FEATURE_COLLECTION_CRS, DefaultGeographicCRS.WGS84);
        final GeometryFactory geometryFactory = new GeometryFactory();
        final Coordinate[] coordinates = {
                new Coordinate(-10, 50),
                new Coordinate(+10, 50),
                new Coordinate(+10, 30),
                new Coordinate(-10, 30),
                new Coordinate(-10, 50)
        };
        final Polygon polygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(coordinates),
                                                              new LinearRing[0]);
        configuration.setValue(FeatureLayerType.PROPERTY_FEATURE_COLLECTION_CLIP_GEOMETRY, polygon);
        configuration.setValue(FeatureLayerType.PROPERTY_SLD_STYLE, createStyle());
        return layerType.createLayer(null, configuration);
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
