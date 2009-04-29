package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
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

    public static final String PROPERTY_BITMASKDEF = "bitmask.bitmaskDef";
    public static final String PROPERTY_PRODUCT = "bitmask.product";
    public static final String PROPERTY_IMAGE_TO_MODEL_TRANSFORM = "bitmask.i2mTransform";

    @Override
    public String getName() {
        return "Bitmask Layer";
    }

    public static Layer createBitmaskLayer(RasterDataNode raster, final BitmaskDef bitmaskDef,
                                           AffineTransform i2mTransform) {
        final LayerType type = LayerType.getLayerType(BitmaskLayerType.class.getName());
        final ValueContainer configuration = type.getConfigurationTemplate();
        try {
            configuration.setValue(BitmaskLayerType.PROPERTY_BITMASKDEF, bitmaskDef);
            configuration.setValue(BitmaskLayerType.PROPERTY_PRODUCT, raster.getProduct());
            configuration.setValue(BitmaskLayerType.PROPERTY_IMAGE_TO_MODEL_TRANSFORM, i2mTransform);
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
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_BITMASKDEF);
        layer.setName(bitmaskDef.getName());
        configureLayer(configuration, layer);

        return layer;
    }

    private MultiLevelSource createMultiLevelSource(ValueContainer configuration) {
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_BITMASKDEF);
        final Product product = (Product) configuration.getValue(PROPERTY_PRODUCT);
        final AffineTransform transform = (AffineTransform) configuration.getValue(PROPERTY_IMAGE_TO_MODEL_TRANSFORM);

        return MaskImageMultiLevelSource.create(product, bitmaskDef.getColor(), bitmaskDef.getExpr(), false, transform);
    }

    private void configureLayer(ValueContainer configuration, Layer layer) {
        final BitmaskDef bitmaskDef = (BitmaskDef) configuration.getValue(PROPERTY_BITMASKDEF);

        final Style style = layer.getStyle();
        style.setOpacity(bitmaskDef.getAlpha());
        style.setComposite(layer.getStyle().getComposite());
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN,
                          configuration.getValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN));
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_COLOR,
                          configuration.getValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR));
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_WIDTH,
                          configuration.getValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH));
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = super.getConfigurationTemplate();

        vc.addModel(createDefaultValueModel(PROPERTY_BITMASKDEF, BitmaskDef.class));
        vc.getModel(PROPERTY_BITMASKDEF).getDescriptor().setNotNull(true);

        vc.addModel(createDefaultValueModel(PROPERTY_PRODUCT, Product.class));
        vc.getModel(PROPERTY_PRODUCT).getDescriptor().setNotNull(true);

        vc.addModel(createDefaultValueModel(PROPERTY_IMAGE_TO_MODEL_TRANSFORM,
                                            new AffineTransform(),
                                            new AffineTransform()));

        return vc;
    }

    public Layer createLayer(BitmaskDef bitmaskDef, Product product, AffineTransform i2m) {
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