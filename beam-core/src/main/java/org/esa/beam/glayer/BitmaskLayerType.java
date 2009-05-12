package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.MaskImageMultiLevelSource;

import java.awt.geom.AffineTransform;


/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class BitmaskLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_BITMASK_DEF = "bitmaskDef";
    public static final String PROPERTY_NAME_PRODUCT = "product";

    @Override
    public String getName() {
        return "Bitmask Layer";
    }

    public static Layer createBitmaskLayer(RasterDataNode raster, final BitmaskDef bitmaskDef,
                                           AffineTransform i2mTransform) {
        final LayerType type = LayerType.getLayerType(BitmaskLayerType.class.getName());
        final ValueContainer configuration = type.getConfigurationTemplate();
        try {
            configuration.setValue(BitmaskLayerType.PROPERTY_NAME_BITMASK_DEF, bitmaskDef);
            configuration.setValue(BitmaskLayerType.PROPERTY_NAME_PRODUCT, raster.getProduct());
            configuration.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM, i2mTransform);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
        final Layer layer = type.createLayer(null, configuration);
        final BitmaskOverlayInfo overlayInfo = raster.getBitmaskOverlayInfo();
        layer.setVisible(overlayInfo != null && overlayInfo.containsBitmaskDef(bitmaskDef));

        return layer;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final MultiLevelSource multiLevelSource = createMultiLevelSource(configuration);
            try {
                configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
            } catch (ValidationException e) {
                throw new IllegalArgumentException(e);
            }
        }
        final ImageLayer layer = new ImageLayer(this, configuration);
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_NAME_BITMASK_DEF);
        layer.setName(bitmaskDef.getName());
        // TODO: Is this correct? (rq-2009-05-11)
        layer.setTransparency(bitmaskDef.getTransparency());

        return layer;
    }

    private MultiLevelSource createMultiLevelSource(ValueContainer configuration) {
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_NAME_BITMASK_DEF);
        final Product product = (Product) configuration.getValue(PROPERTY_NAME_PRODUCT);
        final AffineTransform transform = (AffineTransform) configuration.getValue(
                ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);

        return MaskImageMultiLevelSource.create(product, bitmaskDef.getColor(), bitmaskDef.getExpr(), false, transform);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = super.getConfigurationTemplate();

        vc.addModel(createDefaultValueModel(PROPERTY_NAME_BITMASK_DEF, BitmaskDef.class));
        vc.getModel(PROPERTY_NAME_BITMASK_DEF).getDescriptor().setNotNull(true);

        vc.addModel(createDefaultValueModel(PROPERTY_NAME_PRODUCT, Product.class));
        vc.getModel(PROPERTY_NAME_PRODUCT).getDescriptor().setNotNull(true);

        return vc;
    }

    public Layer createLayer(BitmaskDef bitmaskDef, Product product, AffineTransform i2m) {
        final ValueContainer configuration = getConfigurationTemplate();

        try {
            configuration.setValue(PROPERTY_NAME_BITMASK_DEF, bitmaskDef);
            configuration.setValue(PROPERTY_NAME_PRODUCT, product);
            configuration.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM, i2m);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }

        return createLayer(null, configuration);
    }


}