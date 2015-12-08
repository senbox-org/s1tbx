/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;

@LayerTypeMetadata(name = "RgbImageLayerType", aliasNames = {"org.esa.snap.core.layer.RgbImageLayerType"})
public class RgbImageLayerType extends ImageLayer.Type {

    private static final String PROPERTY_NAME_PRODUCT = "product";
    private static final String PROPERTY_NAME_EXPRESSION_R = "expressionR";
    private static final String PROPERTY_NAME_EXPRESSION_G = "expressionG";
    private static final String PROPERTY_NAME_EXPRESSION_B = "expressionB";

    @Override
    public ImageLayer createLayer(LayerContext ctx, PropertySet configuration) {
        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            final Product product = (Product) configuration.getValue(PROPERTY_NAME_PRODUCT);

            final String[] rgbExpressions = new String[3];
            rgbExpressions[0] = (String) configuration.getValue(PROPERTY_NAME_EXPRESSION_R);
            rgbExpressions[1] = (String) configuration.getValue(PROPERTY_NAME_EXPRESSION_G);
            rgbExpressions[2] = (String) configuration.getValue(PROPERTY_NAME_EXPRESSION_B);
            final RasterDataNode[] rasters = getRgbBands(product, rgbExpressions);

            multiLevelSource = ColoredBandImageMultiLevelSource.create(rasters, rasters[0].getSourceImage().getModel(),
                                                                       ProgressMonitor.NULL);
        }

        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, true);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);
        final ImageLayer layer = new ImageLayer(this, multiLevelSource, configuration);
        layer.setName("RGB Layer");
        return layer;
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet prototype = super.createLayerConfig(ctx);

        final Property productModel = Property.create(PROPERTY_NAME_PRODUCT, Product.class);
        productModel.getDescriptor().setNotNull(true);
        prototype.addProperty(productModel);

        final Property redModel = Property.create(PROPERTY_NAME_EXPRESSION_R, String.class);
        redModel.getDescriptor().setNotNull(true);
        prototype.addProperty(redModel);

        final Property greenModel = Property.create(PROPERTY_NAME_EXPRESSION_G, String.class);
        greenModel.getDescriptor().setNotNull(true);
        prototype.addProperty(greenModel);

        final Property blueModel = Property.create(PROPERTY_NAME_EXPRESSION_B, String.class);
        blueModel.getDescriptor().setNotNull(true);
        prototype.addProperty(blueModel);

        return prototype;
    }

    public Layer createLayer(RasterDataNode[] rasters, ColoredBandImageMultiLevelSource multiLevelSource) {
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
        final PropertySet configuration = createLayerConfig(null);

        final String expressionR = getExpression(rasters[0]);
        final String expressionG = getExpression(rasters[1]);
        final String expressionB = getExpression(rasters[2]);

        configuration.setValue(PROPERTY_NAME_PRODUCT, product);
        configuration.setValue(PROPERTY_NAME_EXPRESSION_R, expressionR);
        configuration.setValue(PROPERTY_NAME_EXPRESSION_G, expressionG);
        configuration.setValue(PROPERTY_NAME_EXPRESSION_B, expressionB);

        if (multiLevelSource == null) {
            multiLevelSource = ColoredBandImageMultiLevelSource.create(rasters, ProgressMonitor.NULL);
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, true);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);
        configuration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_SHOWN, true);
        configuration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_COLOR, ImageLayer.DEFAULT_PIXEL_BORDER_COLOR);
        configuration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_WIDTH, ImageLayer.DEFAULT_PIXEL_BORDER_WIDTH);

        return createLayer(null, configuration);
    }

    // todo - code duplication in Session.java (nf 10.2009)
    private static String getExpression(RasterDataNode raster) {
        final Product product = raster.getProduct();
        if (product != null) {
            if (product.containsBand(raster.getName())) {
                return BandArithmetic.createExternalName(raster.getName());
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
