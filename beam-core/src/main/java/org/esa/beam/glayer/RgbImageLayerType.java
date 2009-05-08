package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.geom.AffineTransform;

public class RgbImageLayerType extends ImageLayer.Type {

    private static final String PROPERTY_NAME_PRODUCT = "product";
    private static final String PROPERTY_NAME_EXPRESSION_R = "expressionR";
    private static final String PROPERTY_NAME_EXPRESSION_G = "expressionG";
    private static final String PROPERTY_NAME_EXPRESSION_B = "expressionB";


    @Override
    public String getName() {
        return "RGB Layer";
    }

    @Override
    protected ImageLayer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final Product product = (Product) configuration.getValue(PROPERTY_NAME_PRODUCT);

            final String[] rgbExpressions = new String[3];
            rgbExpressions[0] = (String) configuration.getValue(PROPERTY_NAME_EXPRESSION_R);
            rgbExpressions[1] = (String) configuration.getValue(PROPERTY_NAME_EXPRESSION_G);
            rgbExpressions[2] = (String) configuration.getValue(PROPERTY_NAME_EXPRESSION_B);
            final RasterDataNode[] rasters = getRgbBands(product, rgbExpressions);

            final AffineTransform i2mTransform = (AffineTransform) configuration.getValue(
                    ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
            final MultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(rasters, i2mTransform,
                                                                                       ProgressMonitor.NULL);
            try {
                configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
                configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, true);
                configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
                configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);
            } catch (ValidationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return new ImageLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = super.getConfigurationTemplate();

        final ValueModel productModel = createDefaultValueModel(PROPERTY_NAME_PRODUCT, Product.class);
        productModel.getDescriptor().setNotNull(true);
        template.addModel(productModel);

        final ValueModel redModel = createDefaultValueModel(PROPERTY_NAME_EXPRESSION_R, String.class);
        redModel.getDescriptor().setNotNull(true);
        template.addModel(redModel);

        final ValueModel greenModel = createDefaultValueModel(PROPERTY_NAME_EXPRESSION_G, String.class);
        greenModel.getDescriptor().setNotNull(true);
        template.addModel(greenModel);

        final ValueModel blueModel = createDefaultValueModel(PROPERTY_NAME_EXPRESSION_B, String.class);
        blueModel.getDescriptor().setNotNull(true);
        template.addModel(blueModel);

        return template;
    }

    public Layer createLayer(RasterDataNode[] rasters, BandImageMultiLevelSource multiLevelSource) {
        if (rasters.length != 3) {
            throw new IllegalArgumentException("rasters.length != 3");
        }
        final Product product = rasters[0].getProduct();
        if (product == null) {
            throw new IllegalArgumentException("rasters[0].getProduct() == null");
        }
        if (product != rasters[1].getProduct()) {
            throw new IllegalArgumentException("rasters[0].getProduct() != rasters[1].getProduct()");
        }
        if (product != rasters[2].getProduct()) {
            throw new IllegalArgumentException("rasters[0].getProduct() != rasters[2].getProduct()");
        }
        final ValueContainer configuration = getConfigurationTemplate();

        try {
            final String expressionR = getExpression(rasters[0]);
            final String expressionG = getExpression(rasters[1]);
            final String expressionB = getExpression(rasters[2]);

            configuration.setValue(PROPERTY_NAME_PRODUCT, product);
            configuration.setValue(PROPERTY_NAME_EXPRESSION_R, expressionR);
            configuration.setValue(PROPERTY_NAME_EXPRESSION_G, expressionG);
            configuration.setValue(PROPERTY_NAME_EXPRESSION_B, expressionB);

            if (multiLevelSource == null) {
                multiLevelSource = BandImageMultiLevelSource.create(rasters, ProgressMonitor.NULL);
            }
            configuration.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM,
                                   multiLevelSource.getModel().getImageToModelTransform(0));
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
            configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, true);
            configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
            configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }

        return createLayer(null, configuration);
    }

    private static String getExpression(RasterDataNode raster) {
        final ProductNode owner = raster.getOwner();

        if (owner instanceof Product) {
            final Product product = (Product) owner;
            if (product.containsBand(raster.getName())) {
                return raster.getName();
            } else {
                if (raster instanceof VirtualBand) {
                    return ((VirtualBand) raster).getExpression();
                }
            }
        }

        return null;
    }

    private static RasterDataNode[] getRgbBands(final Product product, final String[] rgbExpressions) {
        final RasterDataNode[] rgbBands = new RasterDataNode[3];

        for (int i = 0; i < rgbBands.length; i++) {
            rgbBands[i] = getRgbBand(product, RGBImageProfile.RGB_BAND_NAMES[i], rgbExpressions[i]);
        }

        return rgbBands;
    }

    private static Band getRgbBand(Product product, String bandName, String expression) {
        Band band = null;
        if (expression != null && !expression.isEmpty()) {
            band = product.getBand(expression);
        }
        if (band == null) {
            if (expression == null || expression.isEmpty()) {
                expression = "0.0";
            }
            band = new RgbBand(product, bandName, expression);
        }

        return band;
    }

    private static class RgbBand extends VirtualBand {

        private RgbBand(Product product, final String bandName, final String expression) {
            super(bandName, ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                  expression);
            setOwner(product);
        }
    }
}
