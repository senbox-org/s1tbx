package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.renderer.lite.LabelCache;
import org.geotools.renderer.lite.LabelCacheDefault;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// todo - compute bounds
/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class FeatureLayer extends Layer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());
    private static final org.geotools.styling.StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    private static final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    private MapContext mapContext;
    private CoordinateReferenceSystem crs;

    private StreamingRenderer renderer;
    private LabelCache labelCache;

    FeatureLayer(final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
                 final Style style) {
        this(LAYER_TYPE, featureCollection, style);
    }

    protected FeatureLayer(Type type,
                           final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
                           final Style style) {
        super(type);

        crs = featureCollection.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();

        mapContext = new DefaultMapContext(crs);
        mapContext.addLayer(featureCollection, style);
        renderer = new StreamingRenderer();
        workaroundLabelCacheBug();
        renderer.setContext(mapContext);
    }

    public Style getSLDStyle() {
        return mapContext.getLayer(0).getStyle();
    }

    public void setSLDStyle(Style style) {
        Assert.argument(style != null, "style != null");
        MapLayer mapLayer = mapContext.getLayer(0);
        mapLayer.setStyle(style);
        super.fireLayerDataChanged(null);
    }

    @Override
    protected void fireLayerPropertyChanged(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(com.bc.ceres.glayer.Style.PROPERTY_NAME_OPACITY)) {
            applyOpacity(getStyle().getOpacity());
        }
        super.fireLayerPropertyChanged(event);
    }

    private void workaroundLabelCacheBug() {
        Map hints = renderer.getRendererHints();
        if (hints == null) {
            hints = new HashMap();
        }
        if (hints.containsKey(StreamingRenderer.LABEL_CACHE_KEY)) {
            labelCache = (LabelCache) hints.get(StreamingRenderer.LABEL_CACHE_KEY);
        } else {
            labelCache = new LabelCacheDefault();
            hints.put(StreamingRenderer.LABEL_CACHE_KEY, labelCache);
        }
        renderer.setRendererHints(hints);
    }

    @Override
    protected void renderLayer(final Rendering rendering) {
        Rectangle bounds = rendering.getViewport().getViewBounds();
        final AffineTransform m2vTransform = rendering.getViewport().getViewToModelTransform();
        Rectangle2D bounds2D = m2vTransform.createTransformedShape(bounds).getBounds2D();
        ReferencedEnvelope mapArea = new ReferencedEnvelope(bounds2D, crs);
        mapContext.setAreaOfInterest(mapArea);

        labelCache.clear();  // workaround for labelCache bug
        renderer.paint(rendering.getGraphics(), bounds, mapArea);
    }

    private void applyOpacity(final double opacity) {
        StyleBuilder sb = new StyleBuilder();
        final Expression opa = sb.literalExpression(opacity);
        final MapLayer layer = mapContext.getLayer(0);
        if (layer != null) {
            Style style = layer.getStyle();
            DuplicatingStyleVisitor copyStyle = new ShapefileOpacityStyleVisitor(opa);
            style.accept(copyStyle);
            layer.setStyle((Style) copyStyle.getCopy());
        }
    }

    public static Style createStyle(File file, FeatureType schema) {
        File sld = toSLDFile(file);
        if (sld.exists()) {
            return createFromSLD(sld);
        }
        Class type = schema.getGeometryDescriptor().getType().getBinding();
        if (type.isAssignableFrom(Polygon.class)
            || type.isAssignableFrom(MultiPolygon.class)) {
            return createPolygonStyle();
        } else if (type.isAssignableFrom(LineString.class)
                   || type.isAssignableFrom(MultiLineString.class)) {
            return createLineStyle();
        } else {
            return createPointStyle();
        }
    }

    /*
     * Figure out the URL for the "sld" file
     */
    private static File toSLDFile(File file) {
        String filename = file.getAbsolutePath();
        if (filename.endsWith(".shp") || filename.endsWith(".dbf")
            || filename.endsWith(".shx")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".sld";
        } else if (filename.endsWith(".SHP") || filename.endsWith(".DBF")
                   || filename.endsWith(".SHX")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".SLD";
        }
        return new File(filename);
    }

    private static Style createFromSLD(File sld) {
        try {
            SLDParser stylereader = new SLDParser(styleFactory, sld.toURI().toURL());
            Style[] style = stylereader.readXML();
            return style[0];
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
        return null;
    }

    private static Style createPointStyle() {
        PointSymbolizer symbolizer = styleFactory.createPointSymbolizer();
        symbolizer.getGraphic().setSize(filterFactory.literal(1));

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    private static Style createLineStyle() {
        LineSymbolizer symbolizer = styleFactory.createLineSymbolizer();
        SLD.setLineColour(symbolizer, Color.BLUE);
        symbolizer.getStroke().setWidth(filterFactory.literal(1));
        symbolizer.getStroke().setColor(filterFactory.literal(Color.BLUE));

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    private static Style createPolygonStyle() {
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

    private static class ShapefileOpacityStyleVisitor extends DuplicatingStyleVisitor {

        private final Expression opa;

        ShapefileOpacityStyleVisitor(Expression opa) {
            this.opa = opa;
        }

        @Override
        public void visit(Fill fill) {
            super.visit(fill);
            Fill fillCopy = (Fill) pages.pop();
            fillCopy.setOpacity(opa);
            pages.push(fillCopy);
        }

        @Override
        public void visit(Stroke stroke) {
            super.visit(stroke);
            Stroke strokeCopy = (Stroke) pages.pop();
            strokeCopy.setOpacity(opa);
            pages.push(strokeCopy);

        }
    }

    public static class Type extends LayerType {

        @Override
        public String getName() {
            return "Feature Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = (FeatureCollection<SimpleFeatureType, SimpleFeature>) configuration.get(
                    "featureCollection");
            Style style = (Style) configuration.get("style");
            return new FeatureLayer(featureCollection, style);
        }

        @Override
        public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
            final HashMap<String, Object> configuration = new HashMap<String, Object>();
            if (layer instanceof FeatureLayer) {
                FeatureLayer featureLayer = (FeatureLayer) layer;
                MapLayer mapLayer = featureLayer.mapContext.getLayer(0);
                FeatureSource<? extends FeatureType, ? extends Feature> featureSource = mapLayer.getFeatureSource();
                FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection = null;
                try {
                    featureCollection = featureSource.getFeatures();
                } catch (IOException e) {
                }
                configuration.put("featureCollection", featureCollection);
                configuration.put("style", mapLayer.getStyle());
            }
            return configuration;
        }
    }
}