package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.jai.SingleBandedSampleModel;

import javax.media.jai.TiledImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.HashMap;

public class JaiOpTest extends TestCase {
    public void testNoSourceGiven() {
        final JaiOp op = new JaiOp();

        try {
            op.getTargetProduct();
            fail("OperatorException expected");
        } catch (OperatorException e) {
            // ok
        }
    }

    public void testNoJaiOpGiven() {

        final Product sourceProduct = createSourceProduct();

        final JaiOp op = new JaiOp();
        op.setSourceProduct(sourceProduct);

        try {
            op.getTargetProduct();
            fail("OperatorException expected");
        } catch (OperatorException e) {
            // ok
        }
    }

    // uses JAI "scale" to create a higher resolution version of a product
    public void testGeometricOperation() {

        final Product sourceProduct = createSourceProduct();
        final Band sourceBand = sourceProduct.getBand("b1");
        setSourceImage(sourceBand);

        final JaiOp op = new JaiOp();

        op.setOperationName("scale");
        op.setSourceProduct(sourceProduct);

        final HashMap<String, Object> operationParameters = new HashMap<String, Object>(3);
        operationParameters.put("xScale", 2.0f);
        operationParameters.put("yScale", 2.0f);
        op.setOperationParameters(operationParameters);

        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(2, targetProduct.getNumBands());
        assertEquals(8, targetProduct.getSceneRasterWidth());
        assertEquals(8, targetProduct.getSceneRasterHeight());

        final Band targetBand = targetProduct.getBand("b1");

        assertNotNull(targetBand);
        assertEquals(targetBand.getDataType(), sourceBand.getDataType());
        assertEquals(8, targetBand.getSceneRasterWidth());
        assertEquals(8, targetBand.getSceneRasterHeight());

        final RenderedImage targetImage = targetBand.getImage();
        assertNotNull(targetImage);
        assertEquals(8, targetImage.getWidth());
        assertEquals(8, targetImage.getHeight());

        final Tile tile = op.getSourceTile(targetBand, new Rectangle(0, 0, 8, 8), ProgressMonitor.NULL);
        assertEquals(123, tile.getSampleInt(0, 0));
        assertEquals(123, tile.getSampleInt(1, 1));
        assertEquals(234, tile.getSampleInt(2, 2));
        assertEquals(234, tile.getSampleInt(3, 3));
        assertEquals(345, tile.getSampleInt(4, 4));
        assertEquals(345, tile.getSampleInt(5, 5));
        assertEquals(456, tile.getSampleInt(6, 6));
        assertEquals(456, tile.getSampleInt(7, 7));
    }

    // uses JAI "rescale" to apply a linear transformation to sample value
    public void testSampleOperation() {

        final Product sourceProduct = createSourceProduct();
        final Band sourceBand = sourceProduct.getBand("b1");
        setSourceImage(sourceBand);

        final JaiOp op = new JaiOp();
        op.setOperationName("rescale");
        op.setSourceProduct(sourceProduct);

        final HashMap<String, Object> operationParameters = new HashMap<String, Object>(3);
        operationParameters.put("constants", new double[]{2.0});
        operationParameters.put("offsets", new double[]{1.0});
        op.setOperationParameters(operationParameters);

        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(2, targetProduct.getNumBands());
        assertEquals(4, targetProduct.getSceneRasterWidth());
        assertEquals(4, targetProduct.getSceneRasterHeight());

        final Band targetBand = targetProduct.getBand("b1");

        assertNotNull(targetBand);
        assertEquals(targetBand.getDataType(), sourceBand.getDataType());
        assertEquals(4, targetBand.getSceneRasterWidth());
        assertEquals(4, targetBand.getSceneRasterHeight());

        final RenderedImage targetImage = targetBand.getImage();
        assertNotNull(targetImage);
        assertEquals(4, targetImage.getWidth());
        assertEquals(4, targetImage.getHeight());

        final Tile tile = op.getSourceTile(targetBand, new Rectangle(0, 0, 4, 4), ProgressMonitor.NULL);
        assertEquals(1 + 2 * 123, tile.getSampleInt(0, 0));
        assertEquals(1 + 2 * 234, tile.getSampleInt(1, 1));
        assertEquals(1 + 2 * 345, tile.getSampleInt(2, 2));
        assertEquals(1 + 2 * 456, tile.getSampleInt(3, 3));
    }

    private void setSourceImage(Band sourceBand) {
        final TiledImage sourceImage = new TiledImage(0, 0,
                                                      sourceBand.getSceneRasterWidth(),
                                                      sourceBand.getSceneRasterHeight(),
                                                      0, 0,
                                                      new SingleBandedSampleModel(DataBuffer.TYPE_INT, sourceBand.getSceneRasterWidth(), sourceBand.getSceneRasterHeight()), null);
        sourceImage.setSample(0, 0, 0, 123);
        sourceImage.setSample(1, 1, 0, 234);
        sourceImage.setSample(2, 2, 0, 345);
        sourceImage.setSample(3, 3, 0, 456);
        sourceBand.setImage(sourceImage);
    }

    private Product createSourceProduct() {
        final Product sourceProduct = new Product("sp", "spt", 4, 4);
        sourceProduct.addBand("b1", ProductData.TYPE_INT32);
        sourceProduct.addTiePointGrid(new TiePointGrid("tpg1", 3,3, 0,0,2,2,new float[] {
                0.1f,0.2f,0.3f,
                0.2f,0.3f,0.4f,
                0.3f,0.4f,0.5f,
        }));
        return sourceProduct;
    }
}
