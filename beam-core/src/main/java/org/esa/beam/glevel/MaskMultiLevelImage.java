package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.support.AbstractLayerImage;
import com.bc.ceres.glevel.support.DeferredLayerImage;
import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.LayerImage;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class MaskMultiLevelImage {

    public static LayerImage create(Product product, Color color, String expression, boolean inverseMask, AffineTransform imageToModelTransform) {
        Assert.notNull(product);
        Assert.notNull(color);
        Assert.notNull(expression);
        final int rasterWidth = product.getSceneRasterWidth();
        final int rasterHeight = product.getSceneRasterHeight();
        final int levelCount = ImageManager.computeMaxLevelCount(rasterWidth, rasterHeight);
        LevelImageFactory levelImageFactory = new Factory(product, color, expression, inverseMask);
        DeferredLayerImage deferredLayerImage = new DeferredLayerImage(
                imageToModelTransform, levelCount, levelImageFactory);
        Rectangle2D modelBounds = AbstractLayerImage.getModelBounds(imageToModelTransform, rasterWidth, rasterHeight);
        deferredLayerImage.setModelBounds(modelBounds);
        return deferredLayerImage;
    }

    private static class Factory implements LevelImageFactory {
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