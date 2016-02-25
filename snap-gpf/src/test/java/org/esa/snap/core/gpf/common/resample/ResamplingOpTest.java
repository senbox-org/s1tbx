package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.transform.MathTransform2D;
import org.junit.Test;

import java.awt.geom.AffineTransform;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Tonio Fincke
 */
public class ResamplingOpTest {

    @Test
    public void testAllNodesHaveIdentitySceneTransform() {
        final Product product = new Product("name", "tapce", 2, 2);
        product.addBand("band_1", "X + Y");
        final Band band2 = product.addBand("band_2", "X + 1 + Y");

        assertTrue(ResamplingOp.allNodesHaveIdentitySceneTransform(product));

        band2.setModelToSceneTransform(MathTransform2D.NULL);

        assertFalse(ResamplingOp.allNodesHaveIdentitySceneTransform(product));
    }

    @Test
    public void testAllScalingsAreIntDivisible() {
        final Product product = new Product("name", "tapce", 2, 2);
        product.addBand("band_1", "X + Y");
        product.addBand("band_2", "X + 1 + Y");

        assertTrue(ResamplingOp.allScalingsAreIntDivisible(product));

        final VirtualBandOpImage image = VirtualBandOpImage.builder("X + Y", product).create();
        final AffineTransform nonIntScalableTransform = new AffineTransform(1.5, 0, 0, 1.5, 0, 0);
        final DefaultMultiLevelModel nonIntScalbleModel = new DefaultMultiLevelModel(nonIntScalableTransform, 3, 3);
        final DefaultMultiLevelSource nonInScalableSource = new DefaultMultiLevelSource(image, nonIntScalbleModel);
        final DefaultMultiLevelImage nonIntScalableImage = new DefaultMultiLevelImage(nonInScalableSource);
        final Band band_3 = new Band("band_3", ProductData.TYPE_FLOAT32, 3, 3);
        band_3.setSourceImage(nonIntScalableImage);
        product.addBand(band_3);

        assertFalse(ResamplingOp.allScalingsAreIntDivisible(product));
    }

    @Test
    public void testAllBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs() {
        final Product product = new Product("name", "tapce", 2, 2);
        final Band band_1 = product.addBand("band_1", "X + Y");
        product.addBand("band_2", "X + 1 + Y");

        assertTrue(ResamplingOp.allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(product, band_1));

        product.addBand(new Band("band_3", ProductData.TYPE_INT8, 1, 3));

        assertFalse(ResamplingOp.allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(product, band_1));
    }

}