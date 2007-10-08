package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;

import java.io.IOException;


public class PlainOpUsageTest extends TestCase {
    public void testU() throws IOException, OperatorException {
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


    private static class AddConstOp extends AbstractOperator {
        private Product sourceProduct;
        private Product targetProduct;
        private double constant;

        public AddConstOp(Product sourceProduct, double constant) {
            this.sourceProduct = sourceProduct;
            this.constant = constant;
        }

        public Product initialize() throws OperatorException {
            targetProduct = new Product(sourceProduct.getName() + "_AddConst", sourceProduct.getProductType(), sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            Band[] bands = sourceProduct.getBands();
            for (Band sourceBand : bands) {
                targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            }
            return targetProduct;
        }


        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
            Band sourceBand = sourceProduct.getBand(band.getName());
            Tile sourceTile = getSourceTile(sourceBand, targetTile.getRectangle());
            for (int y = 0; y < targetTile.getHeight(); y++) {
                for (int x = 0; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile.getSampleDouble(x, y) + constant);
                }
            }
        }
    }

    private static class MulConstOp extends AbstractOperator {
        private Product sourceProduct;
        private Product targetProduct;
        private double constant;

        public MulConstOp(Product sourceProduct, double constant) {
            this.sourceProduct = sourceProduct;
            this.constant = constant;
        }

        @Override
        public Product initialize() throws OperatorException {
            targetProduct = new Product(sourceProduct.getName() + "_MulConst", sourceProduct.getProductType(), sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            Band[] bands = sourceProduct.getBands();
            for (Band sourceBand : bands) {
                targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            }
            return targetProduct;
        }


        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
            Band sourceBand = sourceProduct.getBand(band.getName());
            Tile sourceTile = getSourceTile(sourceBand, targetTile.getRectangle());
            for (int y = 0; y < targetTile.getHeight(); y++) {
                for (int x = 0; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile.getSampleDouble(x, y) * constant);
                }
            }
        }
    }

    private static class AddOp extends AbstractOperator {
        private Product sourceProduct1;
        private Product sourceProduct2;
        private Product targetProduct;


        public AddOp(Product sourceProduct1, Product sourceProduct2) {
            this.sourceProduct1 = sourceProduct1;
            this.sourceProduct2 = sourceProduct2;
        }

        @Override
        public Product initialize() throws OperatorException {
            targetProduct = new Product(sourceProduct1.getName() + "_Add", sourceProduct1.getProductType(), sourceProduct1.getSceneRasterWidth(), sourceProduct1.getSceneRasterHeight());
            Band[] bands = sourceProduct1.getBands();
            for (Band sourceBand : bands) {
                targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            }
            return targetProduct;
        }


        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
            Band sourceBand1 = sourceProduct1.getBand(band.getName());
            Band sourceBand2 = sourceProduct2.getBand(band.getName());
            Tile sourceTile1 = getSourceTile(sourceBand1, targetTile.getRectangle());
            Tile sourceTile2 = getSourceTile(sourceBand2, targetTile.getRectangle());
            for (int y = 0; y < targetTile.getHeight(); y++) {
                for (int x = 0; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile1.getSampleDouble(x, y) + sourceTile2.getSampleDouble(x, y));
                }
            }
        }
    }

    private static class MulOp extends AbstractOperator {
        private Product sourceProduct1;
        private Product sourceProduct2;
        private Product targetProduct;


        public MulOp(Product sourceProduct1, Product sourceProduct2) {
            this.sourceProduct1 = sourceProduct1;
            this.sourceProduct2 = sourceProduct2;
        }

        @Override
        public Product initialize() throws OperatorException {
            targetProduct = new Product(sourceProduct1.getName() + "_Mul", sourceProduct1.getProductType(), sourceProduct1.getSceneRasterWidth(), sourceProduct1.getSceneRasterHeight());
            Band[] bands = sourceProduct1.getBands();
            for (Band sourceBand : bands) {
                targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            }
            return targetProduct;
        }


        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
            Band sourceBand1 = sourceProduct1.getBand(band.getName());
            Band sourceBand2 = sourceProduct2.getBand(band.getName());
            Tile sourceTile1 = getSourceTile(sourceBand1, targetTile.getRectangle());
            Tile sourceTile2 = getSourceTile(sourceBand2, targetTile.getRectangle());
            for (int y = 0; y < targetTile.getHeight(); y++) {
                for (int x = 0; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile1.getSampleDouble(x, y) * sourceTile2.getSampleDouble(x, y));
                }
            }
        }
    }
}
