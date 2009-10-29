package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;

import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.LineFigure;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.FigureLayerType;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FigureLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public FigureLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(FigureLayerType.class));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertyContainer configuration = layerType.createLayerConfig(null);
        final ArrayList<Figure> figureList = new ArrayList<Figure>();
        figureList.add(createFigure());
        configuration.setValue(FigureLayer.PROPERTY_NAME_FIGURE_LIST, figureList);
        return layerType.createLayer(null, configuration);
    }

    private LineFigure createFigure() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("Color", Color.RED);
        attributes.put("Color", Color.RED);
        attributes.put("Composite", AlphaComposite.Clear);
        attributes.put("Stroke", new BasicStroke(0.7f));
        return new LineFigure(new Rectangle(0, 0, 10, 10), attributes);
    }

}
