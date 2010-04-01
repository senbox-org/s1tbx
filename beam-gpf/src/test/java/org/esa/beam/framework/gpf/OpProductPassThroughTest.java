package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.RenderedImage;
import java.util.Arrays;

/**
 * <i>Warning: Tests for experimental code added by nf (25.02.2010)</i><br/>
 *
 * @since BEAM 4.8
 */
public class OpProductPassThroughTest extends TestCase {
    public void testUpdateOnGpfBandOp() throws Exception {
        testOp(new GpfBandOp());
    }

    public void testUpdateOnJaiBandOp() throws Exception {
        testOp(new JaiBandOp());
    }

    private void testOp(Op op) {
        Product sourceProduct = new Product("test", "test", 10, 10);

        op.setSourceProduct(sourceProduct);
        op.bandName = "A";
        op.sampleValue = 0.5f;

        Product targetProduct = op.getTargetProduct();
        assertSame(targetProduct, sourceProduct);
        assertNotNull(targetProduct.getBand("A"));

        assertEquals(0.5f, targetProduct.getBand("A").getSourceImage().getData().getSampleFloat(0, 0, 0));

        op.sampleValue = -0.6f;
        op.update();

        Product targetProduct2 = op.getTargetProduct();
        assertSame(targetProduct2, targetProduct);
        assertNotNull(targetProduct2.getBand("A"));

        assertEquals(-0.6f, targetProduct2.getBand("A").getSourceImage().getData().getSampleFloat(0, 0, 0));
    }

    public static class Op extends Operator {
        @SourceProduct
        Product sourceProduct;
        @TargetProduct
        Product targetProduct;
        @Parameter
        String bandName;
        @Parameter
        float sampleValue;

        Band band;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = sourceProduct;
            if (band == null) {
                band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            }
        }
    }

    public static class JaiBandOp extends Op {

        @Override
        public void initialize() throws OperatorException {
            super.initialize();
            band.setSourceImage(ConstantDescriptor.create((float) targetProduct.getSceneRasterWidth(),
                                                          (float) targetProduct.getSceneRasterHeight(),
                                                          new Float[]{sampleValue}, null));
        }

    }

    public static class GpfBandOp extends Op {

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            if (targetBand == band) {
                Arrays.fill(targetTile.getDataBufferFloat(), sampleValue);
            }
        }
    }
}
