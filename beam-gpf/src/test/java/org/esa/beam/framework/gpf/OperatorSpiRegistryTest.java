package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.annotations.OperatorAlias;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;

/**
 * A registry for operator SPI instances.
 *
 * @author Marco Zuehlke
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 * @since 4.1
 */
public class OperatorSpiRegistryTest extends TestCase {

    public void testAlias() {
        DummyOp.Spi spi = new DummyOp.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(spi);
        assertSame(spi, OperatorSpiRegistry.getInstance().getOperatorSpi(DummyOp.Spi.class.getName()));
        assertSame(spi, OperatorSpiRegistry.getInstance().getOperatorSpi("Heino"));
        assertSame(spi, OperatorSpiRegistry.getInstance().getOperatorSpi("Willi"));
        OperatorSpiRegistry.getInstance().removeOperatorSpi(spi);
        assertNull(OperatorSpiRegistry.getInstance().getOperatorSpi(DummyOp.Spi.class.getName()));
        assertNull(OperatorSpiRegistry.getInstance().getOperatorSpi("Heino"));
        assertNull(OperatorSpiRegistry.getInstance().getOperatorSpi("Willi"));
    }

    public static class DummyOp extends TestOps.Op1 {
        public DummyOp(OperatorSpi spi) {
            super(spi);
        }
        @OperatorAlias("Heino")
        public static class Spi extends AbstractOperatorSpi {
            public Spi() {
                super(DummyOp.class, "Willi");
            }
        }
    }
}
