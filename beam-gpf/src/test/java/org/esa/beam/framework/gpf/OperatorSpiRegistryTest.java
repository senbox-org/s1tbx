package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.annotations.OperatorAlias;

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
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
        assertSame(spi, GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(DummyOp.Spi.class.getName()));
        assertSame(spi, GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Heino"));
        assertSame(spi, GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Willi"));
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
        assertNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(DummyOp.Spi.class.getName()));
        assertNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Heino"));
        assertNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Willi"));
    }

    public static class DummyOp extends TestOps.Op1 {
        @OperatorAlias("Heino")
        public static class Spi extends AbstractOperatorSpi {
            public Spi() {
                super(DummyOp.class, "Willi");
            }
        }
    }
}
