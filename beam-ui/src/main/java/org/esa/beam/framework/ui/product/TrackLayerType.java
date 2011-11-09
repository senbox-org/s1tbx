package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.grender.Rendering;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

/**
 * A special layer type that is used to create layers for {@link VectorDataNode}s that
 * have the {@code FeatureType} "TrackPoint".
 * <p/>
 * <i>Note: this is experimental code.</i>
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class TrackLayerType extends VectorDataLayerType {

    @Override
    protected VectorDataLayer createLayer(VectorDataNode vectorDataNode, PropertySet configuration) {
        return new TrackLayer(this, vectorDataNode, configuration);
    }

    public static class TrackLayer extends VectorDataLayer {
        public TrackLayer(VectorDataLayerType vectorDataLayerType, VectorDataNode vectorDataNode, PropertySet configuration) {
            super(vectorDataLayerType, vectorDataNode, configuration);
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
            rendering.getGraphics().setPaint(new Color(255, 128, 128, 128));
            rendering.getGraphics().setStroke(new BasicStroke(0.5f));
            // FeatureCollection.toArray() returns the feature in original order
            // todo - must actually sort using 'timestamp' attribute (nf)
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
