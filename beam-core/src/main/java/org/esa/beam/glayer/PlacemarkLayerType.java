package org.esa.beam.glayer;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class PlacemarkLayerType extends LayerType {

    public static final String PROPERTY_PRODUCT = "product";
    public static final String PROPERTY_PLACEMARK_DESCRIPTOR = "placemarkDescriptor";
    public static final String PROPERTY_IMAGE_TO_MODEL_TRANSFORM = "imageToModelTransform";

    @Override
    public String getName() {
        return "Placemark";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new PlacemarkLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer valueContainer = new ValueContainer();

        final ValueModel textBgColorModel = LayerType.createDefaultValueModel(
                PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR,
                Color.class, PlacemarkLayer.DEFAULT_TEXT_BG_COLOR);
        valueContainer.addModel(textBgColorModel);

        final ValueModel textFgColorModel = LayerType.createDefaultValueModel(
                PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR,
                Color.class, PlacemarkLayer.DEFAULT_TEXT_FG_COLOR);
        valueContainer.addModel(textFgColorModel);

        final ValueModel textEnabledModel = LayerType.createDefaultValueModel(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED,
                                                                              Boolean.class,
                                                                              PlacemarkLayer.DEFAULT_TEXT_ENABLED);
        valueContainer.addModel(textEnabledModel);

        final ValueModel textFontModel = LayerType.createDefaultValueModel(PlacemarkLayer.PROPERTY_NAME_TEXT_FONT,
                                                                           Font.class,
                                                                           PlacemarkLayer.DEFAULT_TEXT_FONT);
        valueContainer.addModel(textFontModel);

        final ValueModel productModel = createDefaultValueModel(PROPERTY_PRODUCT, Product.class);
        productModel.getDescriptor().setNotNull(true);
        valueContainer.addModel(productModel);

        final ValueModel placemarkModel = createDefaultValueModel(PROPERTY_PLACEMARK_DESCRIPTOR,
                                                                  PlacemarkDescriptor.class);
        placemarkModel.getDescriptor().setConverter(new Converter<Object>() {
            @Override
            public Class<?> getValueType() {
                return PlacemarkDescriptor.class;
            }

            @Override
            public Object parse(String text) throws ConversionException {
                if (GcpDescriptor.INSTANCE.getRoleName().equals(text)) {
                    return GcpDescriptor.INSTANCE;
                } else if (PinDescriptor.INSTANCE.getRoleName().equals(text)) {
                    return PinDescriptor.INSTANCE;
                } else {
                    final String message = String.format("No PlacemarkDescriptor known for role: %s", text);
                    throw new ConversionException(message);
                }
            }

            @Override
            public String format(Object value) {
                final PlacemarkDescriptor placemarkDescriptor = (PlacemarkDescriptor) value;
                return placemarkDescriptor.getRoleName();
            }
        });
        placemarkModel.getDescriptor().setNotNull(true);
        valueContainer.addModel(placemarkModel);

        final ValueModel transformModel = createDefaultValueModel(PROPERTY_IMAGE_TO_MODEL_TRANSFORM,
                                                                  AffineTransform.class);
        placemarkModel.getDescriptor().setNotNull(true);
        valueContainer.addModel(transformModel);

        return valueContainer;
    }
}
