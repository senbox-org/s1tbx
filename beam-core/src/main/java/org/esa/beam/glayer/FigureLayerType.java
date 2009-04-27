package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import org.esa.beam.framework.draw.Figure;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
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
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        final FigureLayer layer = new FigureLayer(this, configuration);
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
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = new ValueContainer();

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                                            FigureLayer.DEFAULT_SHAPE_OUTLINED,
                                            FigureLayer.DEFAULT_SHAPE_OUTLINED));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                                            FigureLayer.DEFAULT_SHAPE_OUTL_COLOR,
                                            FigureLayer.DEFAULT_SHAPE_OUTL_COLOR));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                                            FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY,
                                            FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                                            FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH,
                                            FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_FILLED,
                                            FigureLayer.DEFAULT_SHAPE_FILLED,
                                            FigureLayer.DEFAULT_SHAPE_FILLED));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                                            FigureLayer.DEFAULT_SHAPE_FILL_COLOR,
                                            FigureLayer.DEFAULT_SHAPE_FILL_COLOR));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                                            FigureLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY,
                                            FigureLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY));

        vc.addModel(createDefaultValueModel(FigureLayer.PROPERTY_NAME_TRANSFORM,
                                            new AffineTransform(),
                                            new AffineTransform()));

        final ValueModel figureListModel = createDefaultValueModel(FigureLayer.PROPERTY_NAME_FIGURE_LIST,
                                                                   List.class,
                                                                   new ArrayList<Figure>());
        figureListModel.getDescriptor().setItemAlias("figure");
        vc.addModel(figureListModel);

        return vc;
    }
}
