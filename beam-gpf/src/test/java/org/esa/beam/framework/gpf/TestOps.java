package org.esa.beam.framework.gpf;

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import com.bc.ceres.core.ProgressMonitor;

public class TestOps {

    public static final int RASTER_WIDTH = 3;
    public static final int RASTER_HEIGHT = 2;
    static String calls = "";

    public static void registerCall(String str) {
        calls += str;
    }

    public static void clearCalls() {
        calls = "";
    }

    public static String getCalls() {
        return calls;
    }

    public static class Op1 extends AbstractOperator {
        @TargetProduct
        private Product targetProduct;

        public Op1(OperatorSpi spi) {
            super(spi);
        }

        @Override
        public Product initialize(ProgressMonitor pm) {
            targetProduct = new Product("Op1Name", "Op1Type", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand(new Band("A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            return targetProduct;
        }

        @Override
        public void computeBand(Raster targetRaster, ProgressMonitor pm) {
            registerCall("Op1;");
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(Op1.class, "Op1");
            }
        }
    }

    public static class Op2 extends AbstractOperator {

        @Parameter
        public double threshold;

        @SourceProduct(bands= {"A"})
        public Product input;

        @TargetProduct
        public Product output;

        public Op2(OperatorSpi spi) {
            super(spi);
        }

        @Override
        public Product initialize(ProgressMonitor pm) {
            output = new Product("Op2Name", "Op2Type", RASTER_WIDTH, RASTER_HEIGHT);
            output.addBand(new Band("A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("B", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            return output;
        }

        @Override
        public void computeAllBands(Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
            getRaster(input.getBand("A"), rectangle);
            getRaster(output.getBand("A"), rectangle);
            getRaster(output.getBand("B"), rectangle);
            registerCall("Op2;");
        }

        public static class Spi extends AbstractOperatorSpi {
            public Spi() {
                super(Op2.class, "Op2");
            }
        }
    }

    public static class Op3 extends AbstractOperator {

        @Parameter
        public boolean ignoreSign;

        @Parameter
        public String expression;

        @SourceProduct(bands= {"A"})
        public Product input1;

        @SourceProduct(bands= {"A", "B"})
        public Product input2;

        @SourceProducts
        public Product[] inputs;

        @TargetProduct
        public Product output;

        public Op3(OperatorSpi spi) {
            super(spi);
        }

       @Override
        public Product initialize(ProgressMonitor pm) {
            output = new Product("Op3Name", "Op3Type", RASTER_WIDTH, RASTER_HEIGHT);
            output.addBand(new Band("A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("B", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("C", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("D", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            return output;
        }

        @Override
        public void computeAllBands(Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        	getRaster(input1.getBand("A"), rectangle);
        	getRaster(input2.getBand("A"), rectangle);
        	getRaster(input2.getBand("B"), rectangle);
        	
            getRaster(output.getBand("A"), rectangle);
            getRaster(output.getBand("B"), rectangle);
            getRaster(output.getBand("C"), rectangle);
            getRaster(output.getBand("D"), rectangle);
            registerCall("Op3;");
        }

        public static class Spi extends AbstractOperatorSpi {
            public Spi() {
                super(Op3.class, "Op3");
            }
        }
    }
}