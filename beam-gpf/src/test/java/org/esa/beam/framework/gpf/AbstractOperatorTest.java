package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.internal.GpfOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.util.logging.Logger;


public class AbstractOperatorTest extends TestCase {
    private DummyAbstractOperator.Spi operatorSpi;
    private DummyAbstractOperator operator;

    @Override
    protected void setUp() throws Exception {
        operatorSpi = new DummyAbstractOperator.Spi();
        operator = new DummyAbstractOperator(operatorSpi);
    }

    public void testSameSpi() {

        assertSame(operatorSpi, operator.getSpi());
    }

    public void testInitBehaviour() throws OperatorException {
        OperatorContext context = new DummyOperatorContext();
        operator.initialize(context, ProgressMonitor.NULL);

        assertSame(context, operator.getContext());
        assertTrue(operator.isInitDelegatingToInit());
    }

    private static class DummyAbstractOperator extends AbstractOperator {
        private boolean initCalled;

        protected DummyAbstractOperator(OperatorSpi spi) {
            super(spi);
        }

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            initCalled = true;
            return new Product("x", "x", 1, 1);
        }

        @Override
        public void computeBand(Raster tile, ProgressMonitor pm) throws OperatorException {
        }

        public boolean isInitDelegatingToInit() {
            return initCalled;
        }

        private static class Spi extends AbstractOperatorSpi {
            public Spi() {
                super(DummyAbstractOperator.class);
            }
        }
    }

    private static class DummyOperatorContext implements OperatorContext {

        public PlanarImage[] getTargetImages() {
            return new GpfOpImage[0];
        }

        public Product getTargetProduct() {
            return null;
        }

        public Product[] getSourceProducts() {
            return null;
        }

        public Product getSourceProduct(String name) {
            return null;
        }

        public String getIdForSourceProduct(Product product) {
            return null;
        }

        public Logger getLogger() {
            return null;
        }

        public Raster getRaster(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProgressMonitor pm) throws OperatorException {
            return null;
        }

        public Raster getRaster(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProductData dataBuffer,
                                ProgressMonitor pm) throws OperatorException {
            return null;
        }

        public Operator getOperator() {
            return null;
        }
    }
}
