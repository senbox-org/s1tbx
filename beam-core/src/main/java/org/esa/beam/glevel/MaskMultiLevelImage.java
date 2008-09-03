package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.support.AbstractMultiLevelImage;
import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.LevelImage;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class MaskMultiLevelImage {

    public static LevelImage create(Product product, Color color, String expression, boolean inverseMask, AffineTransform imageToModelTransform) {
        Assert.notNull(product);
        Assert.notNull(color);
        Assert.notNull(expression);
        final int rasterWidth = product.getSceneRasterWidth();
        final int rasterHeight = product.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        LRImageFactory lrImageFactory = new Factory(product, color, expression, inverseMask);
        DeferredMultiLevelImage deferredMultiLevelImage = new DeferredMultiLevelImage(
                imageToModelTransform, levelCount, lrImageFactory);
        Rectangle2D modelBounds = AbstractMultiLevelImage.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        deferredMultiLevelImage.setModelBounds(modelBounds);
        return deferredMultiLevelImage;
    }

    private static class Factory implements LRImageFactory {
        private final Product product;
        private final Color color;
        private final String expression;
        private final boolean inverseMask;
        
        public Factory(Product product, Color color, String expression, boolean inverseMask) {
            this.product = product;
            this.color = color;
            this.expression = expression;
            this.inverseMask = inverseMask;
        }

        @Override
        public RenderedImage createLRImage(int level) {
            return ImageManager.getInstance().createColoredMaskImage(product, expression, color, inverseMask, level);
        }
    }
}