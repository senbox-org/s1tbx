package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import org.esa.beam.framework.draw.Figure;

import java.awt.geom.AffineTransform;
import java.util.List;

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
    public Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        List<Figure> figureList = (List<Figure>) configuration.getValue(FigureLayer.PROPERTY_NAME_FIGURE_LIST);
        AffineTransform shapeToModelTransform = (AffineTransform) configuration.getValue(
                FigureLayer.PROPERTY_NAME_TRANSFORM);
        final FigureLayer layer = new FigureLayer(this, shapeToModelTransform, figureList);
        layer.setId(FIGURE_LAYER_ID);
        layer.setVisible(true);
        configureLayer(configuration, layer);
        return layer;
    }

    private void configureLayer(ValueContainer configuration, Layer layer) {
        final Style style = layer.getStyle();
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILLED,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_FILLED));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                          configuration.getValue(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY));

        layer.setStyle(style);
    }

    @Override
    public ValueContainer getConfigurationCopy(LayerContext ctx, Layer layer) {
        FigureLayer figureLayer = (FigureLayer) layer;
        final ValueContainer vc = new ValueContainer();

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_FIGURE_LIST,
                                            figureLayer.getFigureList()
        ));
        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_TRANSFORM,
                                            figureLayer.getShapeToModelTransform()
        ));

        return vc;
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = new ValueContainer();

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_FIGURE_LIST, List.class));
        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_TRANSFORM, AffineTransform.class));

        return vc;
    }
}
