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

package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.FeatureUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.styling.*;
import org.geotools.styling.Stroke;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
class ShapefileLoader extends ProgressMonitorSwingWorker<Layer, Object> {

    private static final org.geotools.styling.StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    private static final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    private final LayerSourcePageContext context;

    ShapefileLoader(LayerSourcePageContext context) {
        super(context.getWindow(), "Loading Shapefile");
        this.context = context;
    }

    protected LayerSourcePageContext getContext() {
        return context;
    }

    @Override
    protected Layer doInBackground(ProgressMonitor pm) throws Exception {

        try {
            pm.beginTask("Reading shapes", ProgressMonitor.UNKNOWN);
            final ProductSceneView sceneView = context.getAppContext().getSelectedProductSceneView();
            final Geometry clipGeometry = FeatureUtils.createGeoBoundaryPolygon(sceneView.getProduct());

            File file = new File((String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FILE_PATH));
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = getFeatureCollection(file);
            CoordinateReferenceSystem featureCrs = (CoordinateReferenceSystem) context.getPropertyValue(
                    ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION_CRS);


            Style[] styles = getStyles(file, featureCollection);
            Style selectedStyle = getSelectedStyle(styles);

            final LayerType type = LayerTypeRegistry.getLayerType(FeatureLayerType.class.getName());
            final PropertySet configuration = type.createLayerConfig(sceneView);
            configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_URL, file.toURI().toURL());
            configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION, featureCollection);
            configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CRS, featureCrs);
            configuration.setValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY, clipGeometry);
            configuration.setValue(FeatureLayerType.PROPERTY_NAME_SLD_STYLE, selectedStyle);
            Layer featureLayer = type.createLayer(sceneView, configuration);
            featureLayer.setName(file.getName());
            featureLayer.setVisible(true);
            return featureLayer;
        } finally {
            pm.done();
        }
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection(File file) throws IOException {
        Object featureCollectionValue = context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        if (featureCollectionValue == null) {
            featureCollection = FeatureUtils.getFeatureSource(file.toURI().toURL()).getFeatures();
        } else {
            featureCollection = (FeatureCollection<SimpleFeatureType, SimpleFeature>) featureCollectionValue;
        }
        return featureCollection;
    }

    private Style getSelectedStyle(Style[] styles) {
        Style selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_SELECTED_STYLE);
        if (selectedStyle == null) {
            selectedStyle = styles[0];
            context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_SELECTED_STYLE, styles[0]);
        }
        return selectedStyle;
    }

    private Style[] getStyles(File file, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        Style[] styles = (Style[]) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_STYLES);
        if (styles == null || styles.length == 0) {
            styles = createStyle(file, featureCollection.getSchema());
            context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_STYLES, styles);
        }
        return styles;
    }

    private static Style[] createStyle(File shapeFile, FeatureType schema) {
        final Style[] styles = SLDUtils.loadSLD(shapeFile);
        if (styles != null && styles.length > 0) {
            return styles;
        }
        Class<?> type = schema.getGeometryDescriptor().getType().getBinding();
        if (type.isAssignableFrom(Polygon.class)
                || type.isAssignableFrom(MultiPolygon.class)) {
            return new Style[]{createPolygonStyle()};
        } else if (type.isAssignableFrom(LineString.class)
                || type.isAssignableFrom(MultiLineString.class)) {
            return new Style[]{createLineStyle()};
        } else {
            return new Style[]{createPointStyle()};
        }
    }

    private static Style createPointStyle() {
        PointSymbolizer symbolizer = styleFactory.createPointSymbolizer();
        symbolizer.getGraphic().setSize(filterFactory.literal(1));

        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(symbolizer);
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    private static Style createLineStyle() {
        LineSymbolizer symbolizer = styleFactory.createLineSymbolizer();
        SLD.setLineColour(symbolizer, Color.BLUE);
        symbolizer.getStroke().setWidth(filterFactory.literal(1));
        symbolizer.getStroke().setColor(filterFactory.literal(Color.BLUE));

        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(symbolizer);
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    private static Style createPolygonStyle() {
        PolygonSymbolizer symbolizer = styleFactory.createPolygonSymbolizer();
        Fill fill = styleFactory.createFill(
                filterFactory.literal("#FFAA00"),
                filterFactory.literal(0.5)
        );
        final Stroke stroke = styleFactory.createStroke(filterFactory.literal(Color.BLACK),
                                                        filterFactory.literal(1));
        symbolizer.setFill(fill);
        symbolizer.setStroke(stroke);
        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(symbolizer);
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }
}
