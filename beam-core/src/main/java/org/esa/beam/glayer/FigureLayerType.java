package org.esa.beam.glayer;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import org.esa.beam.framework.draw.Figure;

import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class FigureLayerType extends LayerType {

    public static final String FIGURE_LAYER_ID = "org.esa.beam.layers.figure";

    @Override
    public String getName() {
        return "Figure Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
        List<Figure> figureList = (List<Figure>) configuration.get(FigureLayer.PROPERTY_NAME_FIGURE_LIST);
        AffineTransform shapeToModelTransform = (AffineTransform) configuration.get(
                FigureLayer.PROPERTY_NAME_TRANSFORM);
        final FigureLayer layer = new FigureLayer(this, shapeToModelTransform, figureList);
        layer.setId(FIGURE_LAYER_ID);
        layer.setVisible(true);
        configureLayer(configuration, layer);
        return layer;
    }

    private void configureLayer(Map<String, Object> configuration, Layer layer) {
        final Style style = layer.getStyle();
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILLED,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_FILLED));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                          configuration.get(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY));

        layer.setStyle(style);
    }

    @Override
    public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
        final HashMap<String, Object> configuration = new HashMap<String, Object>();
        if (layer instanceof FigureLayer) {
            FigureLayer figureLayer = (FigureLayer) layer;
            configuration.put(FigureLayer.PROPERTY_NAME_FIGURE_LIST, figureLayer.getFigureList());
            configuration.put(FigureLayer.PROPERTY_NAME_TRANSFORM, figureLayer.getShapeToModelTransform());
        }
        return configuration;
    }
}
