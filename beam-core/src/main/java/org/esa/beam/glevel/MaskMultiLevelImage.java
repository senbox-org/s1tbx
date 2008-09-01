package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.support.DeferredMultiLevelImage;
import com.bc.ceres.glevel.LRImageFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;


public class MaskMultiLevelImage extends DeferredMultiLevelImage implements LRImageFactory {

    private final Product product;
    private final Color color;
    private final String expression;
    private final boolean inverseMask;

    public MaskMultiLevelImage(Product product, Color color, String expression, boolean inverseMask, AffineTransform imageToModelTransform) {
        super(imageToModelTransform, ImageManager.computeMaxLevelCount(product.getSceneRasterWidth(), product.getSceneRasterHeight()));
        Assert.notNull(color);
        Assert.notNull(expression);
        setLRImageFactory(this);
        setModelBounds(getModelBounds(imageToModelTransform, product.getSceneRasterWidth(), product.getSceneRasterHeight()));
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