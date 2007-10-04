package org.esa.beam.framework.gpf;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.internal.GpfOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.util.logging.Logger;


public class AbstractOperatorTest extends TestCase {
    private DummyAbstractOperator operator;

    @Override
    protected void setUp() throws Exception {
        operator = new DummyAbstractOperator();
    }

    public void testInitBehaviour() throws OperatorException {
        OperatorContext context = new DummyOperatorContext();
        operator.initialize(context);

        assertSame(context, operator.getContext());
        assertTrue(operator.isInitDelegatingToInit());
    }

    private static class DummyAbstractOperator extends AbstractOperator {
        private boolean initCalled;

        @Override
        protected Product initialize() throws OperatorException {
            initCalled = true;
            return new Product("x", "x", 1, 1);
        }

        @Override
        public void computeTile(Band band, Tile tile) throws OperatorException {
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

        public String getSourceProductId(Product product) {
            return null;
        }

        public Logger getLogger() {
            return null;
        }

        public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle tileRectangle) throws OperatorException {
            return null;
        }

        public Operator getOperator() {
            return null;
        }

        public OperatorSpi getOperatorSpi() {
            return null;
        }

        public boolean isCancellationRequested() {
            return false;
        }
    }
}
