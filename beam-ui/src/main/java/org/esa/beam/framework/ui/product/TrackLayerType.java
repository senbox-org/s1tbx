package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

/**
 * A special layer type that is used to create layers for {@link VectorDataNode}s that
 * have a special feature type. In this case "org.esa.beam.TrackPoint".
 * <p/>
 * <i>Note: this is experimental code.</i>
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class TrackLayerType extends VectorDataLayerType {

    public static boolean isTrackPointNode(VectorDataNode node) {
        final Object trackPoints = node.getFeatureType().getUserData().get("trackPoints");
        return trackPoints != null && trackPoints.toString().equals("true");
    }

    @Override
    protected VectorDataLayer createLayer(VectorDataNode vectorDataNode, PropertySet configuration) {
        return new TrackLayer(this, vectorDataNode, configuration);
    }

    public static class TrackLayer extends VectorDataLayer {

        public static final Color STROKE_COLOR = Color.ORANGE;
        public static final double STROKE_OPACITY = 0.8;
        public static final double STROKE_WIDTH = 2.0;
        public static final double FILL_OPACITY = 0.5;
        public static final Color FILL_COLOR = Color.WHITE;

        private final Paint strokePaint;

        public TrackLayer(VectorDataLayerType vectorDataLayerType, VectorDataNode vectorDataNode, PropertySet configuration) {
            super(vectorDataLayerType, vectorDataNode, configuration);
            String styleCss = vectorDataNode.getDefaultStyleCss();
            DefaultFigureStyle style = new DefaultFigureStyle(styleCss);
            style.fromCssString(styleCss);
            style.setSymbolName("circle");
            style.setStrokeColor(STROKE_COLOR);
            style.setStrokeWidth(STROKE_WIDTH);
            style.setStrokeOpacity(STROKE_OPACITY);
            style.setFillColor(FILL_COLOR);
            style.setFillOpacity(FILL_OPACITY);
            strokePaint = style.getStrokePaint();
            vectorDataNode.setDefaultStyleCss(style.toCssString());
        }

        @Override
        protected void renderLayer(Rendering rendering) {
            drawTrackPointConnections(rendering);
            super.renderLayer(rendering);
        }

        private void drawTrackPointConnections(Rendering rendering) {

            Graphics2D g = rendering.getGraphics();
            AffineTransform oldTransform = g.getTransform();
            try {
                g.transform(rendering.getViewport().getModelToViewTransform());
                drawTrackPointConnections0(rendering);
            } finally {
                g.setTransform(oldTransform);
            }
        }

        private void drawTrackPointConnections0(Rendering rendering) {
            // todo - get these styles from vector data node.  (nf)
            rendering.getGraphics().setPaint(strokePaint);
            float scalingFactor = (float) rendering.getViewport().getViewToModelTransform().getScaleX();
            float effectiveStrokeWidth = (float) (scalingFactor * STROKE_WIDTH);
            float effectiveDash = Math.max(1.0F, scalingFactor * 5.0F);
            float effectiveMeterLimit = Math.max(1.0F, scalingFactor * 10.0F);
            BasicStroke basicStroke = new BasicStroke(effectiveStrokeWidth,
                                                      BasicStroke.CAP_SQUARE,
                                                      BasicStroke.JOIN_MITER,
                                                      effectiveMeterLimit,
                                                      new float[]{
                                                              effectiveDash,
                                                              effectiveDash},
                                                      0.0f);
            rendering.getGraphics().setStroke(basicStroke);

            // FeatureCollection.toArray() returns the feature in original order
            // todo - must actually sort using some (timestamp) attribute (nf)
            SimpleFeature[] features = getVectorDataNode().getFeatureCollection().toArray(new SimpleFeature[0]);
            double lastX = 0;
            double lastY = 0;
            for (int i = 0; i < features.length; i++) {
                SimpleFeature feature = features[i];
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                com.vividsolutions.jts.geom.Point centroid = geometry.getCentroid();
                if (i > 0) {
                    rendering.getGraphics().draw(new Line2D.Double(lastX, lastY, centroid.getX(), centroid.getY()));
                }
                lastX = centroid.getX();
                lastY = centroid.getY();
            }
        }
    }
}
