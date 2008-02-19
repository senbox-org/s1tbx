package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.operators.common.PassThroughOp;

import java.io.IOException;
import java.io.File;


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


    public void testParameterDefaultValueInitialisation() {
        final ParameterDefaultValueOp op = new ParameterDefaultValueOp();
        testParameterValues(op, 12345, 0.123450);
        assertEquals(false, op.initialized);
        op.getTargetProduct(); // force initialisation through framework
        assertEquals(true, op.initialized);
        testParameterValues(op, 12345 + 1, 0.123450);
    }

    public void testDerivedParameterDefaultValueInitialisation() {
        final DerivedParameterDefaultValueOp op = new DerivedParameterDefaultValueOp();
        testParameterValues(op, 12345, 0.123450);
        assertEquals(new File("/usr/marco"), op.pf);
        assertEquals(false, op.initialized);
        op.getTargetProduct(); // force initialisation through framework
        assertEquals(true, op.initialized);
        testParameterValues(op, 12345 + 1, 0.123450);
        assertEquals(new File("/usr/marco"), op.pf);
    }

    private void testParameterValues(ParameterDefaultValueOp op, int pi, double pd) {
        assertEquals((byte)123, op.pb);
        assertEquals('a', op.pc);
        assertEquals((short)321, op.ph);
        assertEquals(pi, op.pi);
        assertEquals(1234512345L, op.pl);
        assertEquals(123.45F, op.pf, 1e-5);
        assertEquals(pd, op.pd, 1e-10);
        assertEquals("x", op.ps);

        assertNotNull(op.pab);
        assertEquals(3, op.pab.length);
        assertEquals((byte)123, op.pab[0]);
        assertNotNull(op.pac);
        assertEquals(3, op.pac.length);
        assertEquals('a', op.pac[0]);
        assertEquals('b', op.pac[1]);
        assertEquals('c', op.pac[2]);
        assertNotNull(op.pah);
        assertEquals(3, op.pah.length);
        assertEquals((short)321, op.pah[0]);
        assertNotNull(op.pai);
        assertEquals(3, op.pai.length);
        assertEquals(12345, op.pai[0]);
        assertNotNull(op.pal);
        assertEquals(3, op.pal.length);
        assertEquals(1234512345L, op.pal[0]);
        assertNotNull(op.paf);
        assertEquals(3, op.paf.length);
        assertEquals(123.45F, op.paf[0], 1e-5);
        assertNotNull(op.pad);
        assertEquals(3, op.pad.length);
        assertEquals(0.123450, op.pad[0], 1e-10);
        assertNotNull(op.pas);
        assertEquals(3, op.pas.length);
        assertEquals("x", op.pas[0]);
        assertEquals("y", op.pas[1]);
        assertEquals("z", op.pas[2]);
    }

    private static class MulConstOp extends Operator {
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;
        @Parameter  (defaultValue = "100")
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

    public static class ParameterDefaultValueOp extends Operator {

        @Parameter (defaultValue = "123")
        byte pb;
        @Parameter (defaultValue = "a")
        char pc;
        @Parameter (defaultValue = "321")
        short ph;
        @Parameter (defaultValue = "12345")
        int pi;
        @Parameter (defaultValue = "1234512345")
        long pl;
        @Parameter (defaultValue = "123.45")
        float pf;
        @Parameter (defaultValue = "0.12345")
        double pd;
        @Parameter (defaultValue = "x")
        String ps;

        @Parameter (defaultValue = "123,122,121")
        byte[] pab;
        @Parameter (defaultValue = "a,b,c")
        char[] pac;
        @Parameter (defaultValue = "321,331,341")
        short[] pah;
        @Parameter (defaultValue = "12345,32345,42345")
        int[] pai;
        @Parameter (defaultValue = "1234512345,2234512345,3234512345")
        long[] pal;
        @Parameter (defaultValue = "123.45,133.45,143.45")
        float[] paf;
        @Parameter (defaultValue = "0.12345,-0.12345,1.12345")
        double[] pad;
        @Parameter (defaultValue = "x,y,z")
        String[] pas;

        boolean initialized = false;

        @Override
        public void initialize() throws OperatorException {
            initialized = true;
            pi++;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

    public static class DerivedParameterDefaultValueOp extends ParameterDefaultValueOp {
        @Parameter(defaultValue = "/usr/marco")
        File pf;
    }
}
