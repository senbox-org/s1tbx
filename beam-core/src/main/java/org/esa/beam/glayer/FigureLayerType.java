package org.esa.beam.glayer;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.draw.Figure;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated since BEAM 4.7, replaced by VectorDataLayerType
 */
@Deprecated
public class FigureLayerType extends LayerType {

    public static final String FIGURE_LAYER_ID = "org.esa.beam.layers.figure";

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final List<Figure> figureList = (List<Figure>) configuration.getValue(FigureLayer.PROPERTY_NAME_FIGURE_LIST);
        final AffineTransform transform = (AffineTransform) configuration.getValue(FigureLayer.PROPERTY_NAME_TRANSFORM);
        final FigureLayer layer = new FigureLayer(this, figureList, transform, configuration);
        layer.setId(FIGURE_LAYER_ID);

        return layer;
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet layerConfig = new PropertyContainer();

        layerConfig.addProperty(Property.create(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, Boolean.class,
                                                FigureLayer.DEFAULT_SHAPE_OUTLINED, true));

        layerConfig.addProperty(Property.create(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, Color.class,
                                                FigureLayer.DEFAULT_SHAPE_OUTL_COLOR, true));

        layerConfig.addProperty(Property.create(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, Double.class,
                                                FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY, true));

        layerConfig.addProperty(Property.create(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, Double.class,
                                                FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH, true));

        layerConfig.addProperty(
                Property.create(FigureLayer.PROPERTY_NAME_SHAPE_FILLED, Boolean.class, FigureLayer.DEFAULT_SHAPE_FILLED,
                                true));

        layerConfig.addProperty(Property.create(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, Color.class,
                                                FigureLayer.DEFAULT_SHAPE_FILL_COLOR, true));

        layerConfig.addProperty(Property.create(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, Double.class,
                                                FigureLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY, true));

        layerConfig.addProperty(
                Property.create(FigureLayer.PROPERTY_NAME_TRANSFORM, AffineTransform.class, new AffineTransform(),
                                true));

        final Property figureListModel = Property.create(FigureLayer.PROPERTY_NAME_FIGURE_LIST, ArrayList.class,
                                                         new ArrayList(), true);
        figureListModel.getDescriptor().setDomConverter(new FigureListDomConverter());
        layerConfig.addProperty(figureListModel);
        return layerConfig;
    }

    private static class FigureListDomConverter implements DomConverter {

        @Override
        public Class<?> getValueType() {
            return ArrayList.class;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                       ValidationException {
            final DomElement[] listElements = parentElement.getChildren("figure");
            final ArrayList figureList = new ArrayList();
            final DomConverter figureDomConverter = new AbstractFigureDomConverter();
            for (DomElement figureElement : listElements) {
                figureList.add(figureDomConverter.convertDomToValue(figureElement, null));
            }
            return figureList;
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            ArrayList figureList = (ArrayList) value;
            final DomConverter figureDomConverter = new AbstractFigureDomConverter();
            for (Object figure : figureList) {
                DomElement figureElement = parentElement.createChild("figure");
                figureDomConverter.convertValueToDom(figure, figureElement);
            }
        }
    }
}
