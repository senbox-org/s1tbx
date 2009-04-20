package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glevel.MaskImageMultiLevelSource;

import java.awt.geom.AffineTransform;


/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class BitmaskLayerType extends LayerType {

    public static final String PROPERTY_BITMASKDEF = "bitmask.bitmaskDef";
    public static final String PROPERTY_PRODUCT = "bitmask.product";
    public static final String PROPERTY_IMAGE_TO_MODEL_TRANSFORM = "bitmask.i2mTransform";

    @Override
    public String getName() {
        return "Bitmask Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public ImageLayer createLayer(LayerContext ctx, ValueContainer configuration) {
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_BITMASKDEF);
        final Product product = (Product) configuration.getValue(PROPERTY_PRODUCT);
        final AffineTransform transform = (AffineTransform) configuration.getValue(PROPERTY_IMAGE_TO_MODEL_TRANSFORM);

        final MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(product,
                                                                                   bitmaskDef.getColor(),
                                                                                   bitmaskDef.getExpr(),
                                                                                   false,
                                                                                   transform);

        final ImageLayer layer = new ImageLayer(this, configuration, multiLevelSource);
        layer.setName(bitmaskDef.getName());
        configureLayer(configuration, layer);

        return layer;
    }

    private void configureLayer(ValueContainer configuration, Layer layer) {
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_BITMASKDEF);
        final Product product = (Product) configuration.getValue(PROPERTY_PRODUCT);
        final AffineTransform i2mTransform = (AffineTransform) configuration.getValue(
                PROPERTY_IMAGE_TO_MODEL_TRANSFORM);

        final Style style = layer.getStyle();
        style.setOpacity(bitmaskDef.getAlpha());
        style.setComposite(layer.getStyle().getComposite());
        style.setProperty(PROPERTY_BITMASKDEF, bitmaskDef);
        style.setProperty(PROPERTY_PRODUCT, product);
        style.setProperty(PROPERTY_IMAGE_TO_MODEL_TRANSFORM, i2mTransform);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = new ValueContainer();
        vc.addModel(createDefaultValueModel(PROPERTY_BITMASKDEF, BitmaskDef.class));
        vc.addModel(createDefaultValueModel(PROPERTY_PRODUCT, Product.class));

        return vc;
    }

    public ImageLayer createLayer(BitmaskDef bitmaskDef, Product product, AffineTransform i2m) {
        final ValueContainer configuration = getConfigurationTemplate();

        try {
            configuration.setValue(PROPERTY_BITMASKDEF, bitmaskDef);
            configuration.setValue(PROPERTY_PRODUCT, product);
            configuration.setValue(PROPERTY_IMAGE_TO_MODEL_TRANSFORM, i2m);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }

        return createLayer(null, configuration);
    }
}