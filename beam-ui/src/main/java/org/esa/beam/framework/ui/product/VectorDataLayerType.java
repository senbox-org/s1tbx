package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.VectorData;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class VectorDataLayerType extends LayerType {
    public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";

    public static final String VECTOR_DATA_LAYER_ID = "org.esa.beam.layers.vectorData";

    @Override
    public String getName() {
        return " Vector Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        VectorData vectorData = (VectorData) configuration.getValue(PROPERTY_NAME_VECTOR_DATA);
        final VectorDataLayer layer = new VectorDataLayer(this, vectorData, configuration);
        layer.setId(VECTOR_DATA_LAYER_ID);

        return layer;
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA, String.class));

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