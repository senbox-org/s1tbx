package org.esa.beam.glevel;

import com.bc.ceres.glevel.support.AbstractMultiLevelImage;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public class MaskMultiLevelImage extends AbstractMultiLevelImage {

    private final Product product;
    private final Color color;
    private final String expression;
    private final boolean inverseMask;
    private final Rectangle2D boundingBox;

    public MaskMultiLevelImage(Product product, Color color, String expression, boolean inverseMask, AffineTransform affineTransform) {
        super(affineTransform, ImageManager.computeMaxLevelCount(product.getSceneRasterWidth(), product.getSceneRasterHeight()));
        this.product = product;
        this.color = color;
        this.expression = expression;
        this.inverseMask = inverseMask;
        this.boundingBox = getImageToModelTransform(0).createTransformedShape(new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight())).getBounds2D();
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        return ImageManager.getInstance().createColoredMaskImage(product, expression, color, inverseMask, level);
    }

    @Override
    public Rectangle2D getBounds(int level) {
        return boundingBox;
    }
}