package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.CommonFactoryFinder;
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
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.Stroke;
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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
class ShapefileLayer extends Layer {

    private static final org.geotools.styling.StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    private static final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    private MapContext mapContext;
    private CoordinateReferenceSystem crs;

    private StreamingRenderer renderer;
    private LabelCache labelCache;

    ShapefileLayer(File file) throws IOException {

        ShapefileDataStore shapefile = new ShapefileDataStore(file.toURI().toURL());
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = shapefile.getFeatureSource();
        FeatureType schema = shapefile.getSchema();

        crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

        Style style = createStyle(file, schema);

        setName(style.getName());
        mapContext = new DefaultMapContext(crs);
        mapContext.addLayer(featureSource, style);
        renderer = new StreamingRenderer();
        workaroundLabelCacheBug();
        renderer.setContext(mapContext);
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
        Rectangle2D d = m2vTransform.createTransformedShape(bounds).getBounds2D();
        ReferencedEnvelope mapArea = new ReferencedEnvelope(d, crs);
        mapContext.setAreaOfInterest(mapArea);

        applyOpacity(getStyle().getOpacity());

        labelCache.clear();

        renderer.paint(rendering.getGraphics(), bounds, mapArea);
    }

    private void applyOpacity(final double opacity) {
        StyleBuilder sb = new StyleBuilder();
        final Expression opa = sb.literalExpression(opacity);
        final MapLayer layer = mapContext.getLayer(0);
        if (layer != null) {

            FeatureTypeStyle[] sty = layer.getStyle().getFeatureTypeStyles();

            List<Rule> rules = sty[0].rules();
            for (Rule r : rules) {
                final Symbolizer[] symbolizers = r.getSymbolizers();
                for (Symbolizer symbolizer : symbolizers) {
                    if (symbolizer instanceof PolygonSymbolizer) {
                        PolygonSymbolizer polySym = (PolygonSymbolizer) symbolizer;
                        final Stroke stroke = polySym.getStroke();
                        if (stroke != null) {
                            stroke.setOpacity(opa);
                        }
                        final Fill fill = polySym.getFill();
                        if (fill != null) {
                            fill.setOpacity(opa);
                        }
                    }
                }
            }
        }
    }

    private static Style createStyle(File file, FeatureType schema) {
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

    /**
     * Figure out the URL for the "sld" file
     */
    public static File toSLDFile(File file) {
        String filename = file.getAbsolutePath();
        if (filename.endsWith(".shp") || filename.endsWith(".dbf")
                || filename.endsWith(".shx")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".sld";
        } else if (filename.endsWith(".SLD") || filename.endsWith(".SLD")
                || filename.endsWith(".SLD")) {
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
            System.exit(0);
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

}
