package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.renderer.lite.LabelCache;
import org.geotools.renderer.lite.LabelCacheDefault;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A layer that renders a feature collection using a given style.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class FeatureLayer extends Layer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());

    private MapContext mapContext;
    private CoordinateReferenceSystem crs;

    private StreamingRenderer renderer;
    private LabelCache labelCache;
    private double polyFillOpacity = 1.0;
    private double polyStrokeOpacity = 1.0;
    private double textOpacity = 1.0;
    private Rectangle2D modelBounds;


    public FeatureLayer(final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
                        final Style style) {
        this(LAYER_TYPE, featureCollection, style);
    }

    protected FeatureLayer(Type type,
                           final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
                           final Style style) {
        super(type);

        crs = featureCollection.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
        final ReferencedEnvelope envelope = new ReferencedEnvelope(featureCollection.getBounds(), crs);
        modelBounds = new Rectangle2D.Double(envelope.getMinX(), envelope.getMinY(),
                                             envelope.getWidth(), envelope.getHeight());
        mapContext = new DefaultMapContext(crs);
        mapContext.addLayer(featureCollection, style);
        renderer = new StreamingRenderer();
        workaroundLabelCacheBug();
        style.accept(new RetrievingStyleVisitor());
        renderer.setContext(mapContext);
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        return modelBounds;
    }

    public double getPolyFillOpacity() {
        return polyFillOpacity;
    }

    public double getPolyStrokeOpacity() {
        return polyStrokeOpacity;
    }

    public double getTextOpacity() {
        return textOpacity;
    }

    public void setPolyFillOpacity(double opacity) {
        if (opacity != polyFillOpacity) {
            polyFillOpacity = opacity;
            applyOpacity();
            fireLayerDataChanged(null);
        }
    }

    public void setPolyStrokeOpacity(double opacity) {
        if (opacity != polyStrokeOpacity) {
            polyStrokeOpacity = opacity;
            applyOpacity();
            fireLayerDataChanged(null);
        }
    }

    public void setTextOpacity(double opacity) {
        if (opacity != textOpacity) {
            textOpacity = opacity;
            applyOpacity();
            fireLayerDataChanged(null);
        }
    }

    @Override
    protected void fireLayerPropertyChanged(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(com.bc.ceres.glayer.Style.PROPERTY_NAME_OPACITY)) {
            applyOpacity();
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
        final AffineTransform v2mTransform = rendering.getViewport().getViewToModelTransform();
        Rectangle2D bounds2D = v2mTransform.createTransformedShape(bounds).getBounds2D();
        ReferencedEnvelope mapArea = new ReferencedEnvelope(bounds2D, crs);
        mapContext.setAreaOfInterest(mapArea);

        labelCache.clear();  // workaround for labelCache bug
        renderer.paint(rendering.getGraphics(), bounds, mapArea);
    }

    private void applyOpacity() {
        final MapLayer layer = mapContext.getLayer(0);
        if (layer != null) {
            Style style = layer.getStyle();
            DuplicatingStyleVisitor copyStyle = new ApplyingStyleVisitor();
            style.accept(copyStyle);
            layer.setStyle((Style) copyStyle.getCopy());
        }
    }

    private class ApplyingStyleVisitor extends DuplicatingStyleVisitor {

        private final Expression polyFillExp;
        private final Expression polyStrokeExp;
        private final Expression textExp;
        private final Fill defaultTextFill;

        private ApplyingStyleVisitor() {
            StyleBuilder sb = new StyleBuilder();
            polyFillExp = sb.literalExpression(polyFillOpacity * getStyle().getOpacity());
            polyStrokeExp = sb.literalExpression(polyStrokeOpacity * getStyle().getOpacity());
            textExp = sb.literalExpression(textOpacity * getStyle().getOpacity());
            defaultTextFill = sb.createFill(Color.BLACK, textOpacity * getStyle().getOpacity());
        }

        @Override
        public void visit(PolygonSymbolizer poly) {
            super.visit(poly);
            PolygonSymbolizer polyCopy = (PolygonSymbolizer) pages.peek();
            Fill polyFill = polyCopy.getFill();
            if (polyFill != null) {
                polyFill.setOpacity(polyFillExp);
            }

            Stroke polyStroke = polyCopy.getStroke();
            if (polyStroke != null) {
                polyStroke.setOpacity(polyStrokeExp);
            }
        }


        @Override
        public void visit(TextSymbolizer text) {
            super.visit(text);
            TextSymbolizer textCopy = (TextSymbolizer) pages.peek();
            Fill textFill = textCopy.getFill();
            if (textFill != null) {
                textFill.setOpacity(textExp);
            } else {
                textCopy.setFill(defaultTextFill);
            }
        }
    }

    private class RetrievingStyleVisitor extends DuplicatingStyleVisitor {

        @Override
        public void visit(PolygonSymbolizer poly) {
            super.visit(poly);
            PolygonSymbolizer polyCopy = (PolygonSymbolizer) pages.peek();
            Fill polyFill = polyCopy.getFill();
            if (polyFill != null) {
                Expression opacityExpression = polyFill.getOpacity();
                if (opacityExpression != null) {
                    polyFillOpacity = (opacityExpression.evaluate(opacityExpression, Double.class));
                }
            }

            Stroke polyStroke = polyCopy.getStroke();
            if (polyStroke != null) {
                Expression opacityExpression = polyStroke.getOpacity();
                if (opacityExpression != null) {
                    polyStrokeOpacity = opacityExpression.evaluate(opacityExpression, Double.class);
                }
            }
        }

        @Override
        public void visit(TextSymbolizer text) {
            super.visit(text);
            TextSymbolizer textCopy = (TextSymbolizer) pages.peek();
            Fill textFill = textCopy.getFill();
            if (textFill != null) {
                Expression opacityExpression = textFill.getOpacity();
                if (opacityExpression != null) {
                    textOpacity = opacityExpression.evaluate(opacityExpression, Double.class);
                }
            }
        }
    }

    public static class Type extends LayerType {

        public static final String PROPERTY_SLD_STYLE = "sldStyle";
        public static final String PROPERTY_FEATURE_COLLECTION = "featureCollection";

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
            FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
            fc = (FeatureCollection<SimpleFeatureType, SimpleFeature>) configuration.get(PROPERTY_FEATURE_COLLECTION);
            Style style = (Style) configuration.get(PROPERTY_SLD_STYLE);
            return new FeatureLayer(fc, style);
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
                } catch (IOException ignored) {
                }
                configuration.put(PROPERTY_FEATURE_COLLECTION, featureCollection);
                configuration.put(PROPERTY_SLD_STYLE, mapLayer.getStyle());
            }
            return configuration;
        }
    }
}