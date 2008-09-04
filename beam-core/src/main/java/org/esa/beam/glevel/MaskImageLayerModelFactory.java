package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;


public class MaskImageLayerModelFactory {

    public static ImageLayerModel create(Product product, Color color, String expression, boolean inverseMask, AffineTransform imageToModelTransform) {
        Assert.notNull(product);
        Assert.notNull(color);
        Assert.notNull(expression);
        final int rasterWidth = product.getSceneRasterWidth();
        final int rasterHeight = product.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        final LIS levelImageSource = new LIS(product, color, expression, inverseMask, levelCount);
        Rectangle2D modelBounds = DefaultImageLayerModel.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        return new DefaultImageLayerModel(levelImageSource, imageToModelTransform, modelBounds);
    }

    private static class LIS extends AbstractMultiLevelSource {
        private final Product product;
        private final Color color;
        private final String expression;
        private final boolean inverseMask;

        public LIS(Product product, Color color, String expression, boolean inverseMask, int levelCount) {
            super(levelCount);
            this.product = product;
            this.color = color;
            this.expression = expression;
            this.inverseMask = inverseMask;
        }

        @Override
        public RenderedImage createLevelImage(int level) {
            return ImageManager.getInstance().createColoredMaskImage(product, expression, color, inverseMask, level);
        }
    }
}