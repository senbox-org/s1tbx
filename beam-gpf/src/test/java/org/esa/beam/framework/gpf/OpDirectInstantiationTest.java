package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.io.IOException;


public class OpDirectInstantiationTest extends TestCase {

    public void testDirectInstantiation() throws IOException, OperatorException {
        Product a = new Product("a", "T", 2, 2);
        a.addBand(new VirtualBand("x", ProductData.TYPE_FLOAT64, 2, 2, "5.2"));

        Product b = new Product("b", "T", 2, 2);
        b.addBand(new VirtualBand("x", ProductData.TYPE_FLOAT64, 2, 2, "4.8"));

        // Directly instantiate AddOp
        Operator op1 = new AddOp(a, b);
        // Initializes op1
        Product p1 = op1.getTargetProduct();
        // Directly instantiate MulConstOp
        Operator op2 = new MulConstOp(p1, 2.5);
        // Initializes op2
        Product product = op2.getTargetProduct();

        Band xBand = product.getBand("x");
        ProductData rasterData = xBand.createCompatibleRasterData();
        xBand.readRasterData(0, 0, 2, 2, rasterData, ProgressMonitor.NULL);

        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(0), 1e-10);
        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(1), 1e-10);
        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(2), 1e-10);
        assertEquals(2.5 * (5.2 + 4.8), rasterData.getElemDoubleAt(3), 1e-10);
    }

    private static class MulConstOp extends Operator {
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;
        @Parameter(defaultValue = "100")
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