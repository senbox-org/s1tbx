package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import com.vividsolutions.jts.geom.Geometry;
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
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A layer that renders a feature collection using a given style.
 * <p/>
 * Unstable API. Use at own risk.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class FeatureLayer extends Layer {

    private MapContext mapContext;
    private CoordinateReferenceSystem crs;

    private StreamingRenderer renderer;
    private LabelCache labelCache;
    private double polyFillOpacity = 1.0;
    private double polyStrokeOpacity = 1.0;
    private double textOpacity = 1.0;
    private Rectangle2D modelBounds;

    public FeatureLayer(LayerType layerType, ValueContainer configuration) {
        super(layerType, configuration);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
        fc = (FeatureCollection<SimpleFeatureType, SimpleFeature>) configuration.getValue(
                FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION);
        if (fc == null) {
            final URL url = (URL) configuration.getValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_URL);
            final CoordinateReferenceSystem targetCrs = (CoordinateReferenceSystem) configuration.getValue(
                    FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CRS);
            final Geometry clipGeometry = (Geometry) configuration.getValue(
                    FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY);
            try {
                fc = ShapefileUtils.createFeatureCollection(url, targetCrs, clipGeometry);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        Style style = (Style) configuration.getValue(FeatureLayerType.PROPERTY_NAME_SLD_STYLE);

        crs = fc.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
        if (crs == null) {
            // todo - check me! Why can this happen??? (nf)
            crs = DefaultGeographicCRS.WGS84;
        }
        final ReferencedEnvelope envelope = new ReferencedEnvelope(fc.getBounds(), crs);
        modelBounds = new Rectangle2D.Double(envelope.getMinX(), envelope.getMinY(),
                                             envelope.getWidth(), envelope.getHeight());
        mapContext = new DefaultMapContext(crs);
        mapContext.addLayer(fc, style);
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
        if ("transparency".equals(event.getPropertyName())) {
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
        final AffineTransform modelToViewTransform = rendering.getViewport().getModelToViewTransform();
        renderer.paint(rendering.getGraphics(), bounds, mapArea, modelToViewTransform);
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
            final double layerOpacity = 1.0 - getTransparency();
            polyFillExp = sb.literalExpression(polyFillOpacity * layerOpacity);
            polyStrokeExp = sb.literalExpression(polyStrokeOpacity * layerOpacity);
            textExp = sb.literalExpression(textOpacity * layerOpacity);
            defaultTextFill = sb.createFill(Color.BLACK, textOpacity * layerOpacity);
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

}