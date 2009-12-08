package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class VectorDataLayerType extends LayerType {

    public static final String FIGURE_LAYER_ID = "org.esa.beam.layers.vectorData";

    @Override
    public String getName() {
        return "Figure Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
//        final List<Figure> figureList = (List<Figure>) configuration.getValue(FigureLayer.PROPERTY_NAME_FIGURE_LIST);
//        final AffineTransform transform = (AffineTransform) configuration.getValue(FigureLayer.PROPERTY_NAME_TRANSFORM);
//        final FigureLayer layer = new VectorDataLayer(this, figureList, transform, configuration);
//        layer.setId(FIGURE_LAYER_ID);

        return null;
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = new PropertyContainer();
        /*
        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTLINED, Boolean.class, VectorDataLayer.DEFAULT_SHAPE_OUTLINED, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, Color.class, VectorDataLayer.DEFAULT_SHAPE_OUTL_COLOR, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, Double.class, VectorDataLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, Double.class, VectorDataLayer.DEFAULT_SHAPE_OUTL_WIDTH, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_FILLED, Boolean.class, VectorDataLayer.DEFAULT_SHAPE_FILLED, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, Color.class, VectorDataLayer.DEFAULT_SHAPE_FILL_COLOR, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, Double.class, VectorDataLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY, true));

        vc.addProperty(Property.create(VectorDataLayer.PROPERTY_NAME_TRANSFORM, AffineTransform.class, new AffineTransform(), true));
        final Property figureListModel = Property.create(VectorDataLayer.PROPERTY_NAME_FIGURE_LIST, ArrayList.class, new ArrayList(), true);
        figureListModel.getDescriptor().setDomConverter(new FigureListDomConverter());
        vc.addProperty(figureListModel);
        */
        return vc;
    }
    /*
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
    */
}