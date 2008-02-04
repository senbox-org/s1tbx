package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.operators.common.PassThroughOp;

import java.awt.image.RenderedImage;
import java.io.IOException;


public class OperatorTest extends TestCase {
    public void testPassThrough() throws OperatorException, IOException {
        Product sourceProduct = createFooProduct();
        final Operator op = new PassThroughOp(sourceProduct);
        assertNotNull(op.getSpi());
        assertFalse(op.context.isPassThrough());
        Product targetProduct = op.getTargetProduct();// force init
        assertSame(sourceProduct, targetProduct);
        assertTrue(op.context.isPassThrough());
    }

    public void testDefaultBehaviour() throws OperatorException, IOException {
        final FooOp op = new FooOp();
        assertNotNull(op.getSpi());
        assertFalse(op.initializeCalled);
        assertFalse(op.computeTileCalled);
        final Product product = op.getTargetProduct();
        assertNotNull(product);
        assertTrue(op.initializeCalled);
        assertFalse(op.computeTileCalled);
        product.getBand("bar").readRasterDataFully(ProgressMonitor.NULL);
        assertTrue(op.initializeCalled);
        assertTrue(op.computeTileCalled);
    }

    private static class FooOp extends Operator {
        private boolean initializeCalled;
        private boolean computeTileCalled;
        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            initializeCalled = true;
            targetProduct = createFooProduct();
        }

        @Override
        public void computeTile(Band band, Tile tile, ProgressMonitor pm) throws OperatorException {
            computeTileCalled = true;
        }
    }

    private static Product createFooProduct() {
        Product product = new Product("foo", "grunt", 1, 1);
        product.addBand("bar", ProductData.TYPE_FLOAT64);
        return product;
    }

    public void testOperatorUpdate() throws IOException, OperatorException {
        Product a = new Product("a", "T", 2, 2);
        a.addBand(new VirtualBand("x", ProductData.TYPE_FLOAT64, 2, 2, "X+Y"));

        Product b = new Product("b", "T", 2, 2);
        b.addBand(new VirtualBand("x", ProductData.TYPE_FLOAT64, 2, 2, "X-Y"));

        MulConstOp opA = new MulConstOp(a, 3.0);
        MulConstOp opB = new MulConstOp(b, 2.0);
        AddOp opC = new AddOp(opA.getTargetProduct(),
                                opB.getTargetProduct());

        Product pA = opA.getTargetProduct();
        Product pB = opB.getTargetProduct();
        Product pC = opC.getTargetProduct();

        RenderedImage iA = pA.getBand("x").getImage();
        RenderedImage iB = pB.getBand("x").getImage();
        RenderedImage iC = pC.getBand("x").getImage();

        assertEquals(3.0*(0+0) + 2.0*(0-0), iC.getData().getSampleDouble(0,0,0), 1e-10);
        assertEquals(3.0*(1+0) + 2.0*(1-0), iC.getData().getSampleDouble(1,0,0), 1e-10);
        assertEquals(3.0*(0+1) + 2.0*(0-1), iC.getData().getSampleDouble(0,1,0), 1e-10);
        assertEquals(3.0*(1+1) + 2.0*(1-1), iC.getData().getSampleDouble(1,1,0), 1e-10);

        opA.setFactor(5.0);
        GPF.updateProduct(pA);

        opB.setFactor(4.0);
        GPF.updateProduct(pB);

        assertNotSame(iA, pA.getBand("x").getImage());
        assertNotSame(iB, pB.getBand("x").getImage());
        assertNotSame(iC, pC.getBand("x").getImage());

        iC = pC.getBand("x").getImage();

        assertEquals(5.0*(0+0) + 4.0*(0-0), iC.getData().getSampleDouble(0,0,0), 1e-10);
        assertEquals(5.0*(1+0) + 4.0*(1-0), iC.getData().getSampleDouble(1,0,0), 1e-10);
        assertEquals(5.0*(0+1) + 4.0*(0-1), iC.getData().getSampleDouble(0,1,0), 1e-10);
        assertEquals(5.0*(1+1) + 4.0*(1-1), iC.getData().getSampleDouble(1,1,0), 1e-10);
    }

    public void testPlainOpUsage() throws IOException, OperatorException {
        Product a = new Product("a", "T", 2, 2);
        a.addBand(new VirtualBand("x", ProductData.TYPE_FLOAT64, 2, 2, "5.2"));

        Product b = new Product("b", "T", 2, 2);
        b.addBand(new VirtualBand("x", ProductData.TYPE_FLOAT64, 2, 2, "4.8"));

        AddOp addOp = new AddOp(a, b);
        MulConstOp mulConstOp = new MulConstOp(addOp.getTargetProduct(), 2.5);
        Product product = mulConstOp.getTargetProduct();

        Band xBand = product.getBand("x");
        ProductData rasterData = xBand.createCompatibleRasterData();
        xBand.readRasterData(0, 0, 2, 2, rasterData, ProgressMonitor.NULL);

        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(0), 1e-10);
        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(1), 1e-10);
        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(2), 1e-10);
        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(3), 1e-10);
    }

    public void testSourceProducts() throws IOException, OperatorException {
        final Operator operator = new Operator() {
            @Override
            public void initialize() throws OperatorException {
            }
        };

        final Product sp1 = new Product("sp1", "t", 1, 1);
        final Product sp2 = new Product("sp2", "t", 1, 1);
        final Product sp3 = new Product("sp3", "t", 1, 1);

        operator.setSourceProduct(sp1);
        assertSame(sp1, operator.getSourceProduct());
        assertSame(sp1, operator.getSourceProduct("sourceProduct"));

        operator.setSourceProduct("sp1", sp1);
        assertSame(sp1, operator.getSourceProduct());
        assertSame(sp1, operator.getSourceProduct("sourceProduct"));
        assertSame(sp1, operator.getSourceProduct("sp1"));

        Product[] products = operator.getSourceProducts();
        assertNotNull(products);
        assertEquals(1, products.length);
        assertSame(sp1, products[0]);

        operator.setSourceProduct("sp2", sp2);
        products = operator.getSourceProducts();
        assertNotNull(products);
        assertEquals(2, products.length);
        assertSame(sp1, products[0]);
        assertSame(sp2, products[1]);

        operator.setSourceProducts(new Product[]{sp3, sp2, sp1});
        assertNull(operator.getSourceProduct("sourceProduct"));
        assertNull(operator.getSourceProduct("sp1"));
        assertNull(operator.getSourceProduct("sp2"));
        products = operator.getSourceProducts();
        assertNotNull(products);
        assertEquals(3, products.length);
        assertSame(sp3, products[0]);
        assertSame(sp2, products[1]);
        assertSame(sp1, products[2]);
        assertSame(sp3, operator.getSourceProduct("sourceProduct1"));
        assertSame(sp2, operator.getSourceProduct("sourceProduct2"));
        assertSame(sp1, operator.getSourceProduct("sourceProduct3"));
        assertEquals("sourceProduct3", operator.getSourceProductId(sp1));
        assertEquals("sourceProduct2", operator.getSourceProductId(sp2));
        assertEquals("sourceProduct1", operator.getSourceProductId(sp3));


        operator.setSourceProducts(new Product[]{sp1, sp2, sp1});
        products = operator.getSourceProducts();
        assertNotNull(products);
        assertEquals(2, products.length);
        assertSame(sp1, products[0]);
        assertSame(sp2, products[1]);
        assertSame(sp1, operator.getSourceProduct("sourceProduct1"));
        assertSame(sp2, operator.getSourceProduct("sourceProduct2"));
        assertSame(sp1, operator.getSourceProduct("sourceProduct3"));
        assertEquals("sourceProduct1", operator.getSourceProductId(sp1));
        assertEquals("sourceProduct2", operator.getSourceProductId(sp2));
        assertNull(operator.getSourceProductId(sp3));
    }

    private static class MulConstOp extends Operator {
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;
        private double factor;

        public MulConstOp(Product sourceProduct, double factor) {
            this.sourceProduct = sourceProduct;
            this.factor = factor;
        }

        public double getFactor() {
            return factor;
        }

        public void setFactor(double factor) {
            this.factor = factor;
        }

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product(sourceProduct.getName() + "_MulConst", sourceProduct.getProductType(), sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            Band[] bands = sourceProduct.getBands();
            for (Band sourceBand : bands) {
                targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            }
        }


        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            Band sourceBand = sourceProduct.getBand(band.getName());
            Tile sourceTile = getSourceTile(sourceBand, targetTile.getRectangle(), pm);
            for (int y = 0; y < targetTile.getHeight(); y++) {
                for (int x = 0; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile.getSampleDouble(x, y) * factor);
                }
            }
        }
    }

    private static class AddOp extends Operator {
        private Product sourceProduct1;
        private Product sourceProduct2;
        @TargetProduct
        private Product targetProduct;


        public AddOp(Product sourceProduct1, Product sourceProduct2) {
            this.sourceProduct1 = sourceProduct1;
            this.sourceProduct2 = sourceProduct2;
        }

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product(sourceProduct1.getName() + "_Add", sourceProduct1.getProductType(), sourceProduct1.getSceneRasterWidth(), sourceProduct1.getSceneRasterHeight());
            Band[] bands = sourceProduct1.getBands();
            for (Band sourceBand : bands) {
                targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            }
        }


        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            Band sourceBand1 = sourceProduct1.getBand(band.getName());
            Band sourceBand2 = sourceProduct2.getBand(band.getName());
            Tile sourceTile1 = getSourceTile(sourceBand1, targetTile.getRectangle(), pm);
            Tile sourceTile2 = getSourceTile(sourceBand2, targetTile.getRectangle(), pm);
            for (int y = 0; y < targetTile.getHeight(); y++) {
                for (int x = 0; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile1.getSampleDouble(x, y) + sourceTile2.getSampleDouble(x, y));
                }
            }
        }
    }
}
