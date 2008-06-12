package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

/**
 * A registry for operator SPI instances.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since 4.1
 */
public class OperatorSpiRegistryImplTest extends TestCase {

    public void testAlias() {
        DummyOp.Spi spi = new DummyOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
        assertSame(spi, GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(DummyOp.Spi.class.getName()));
        assertSame(spi, GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Heino"));
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
        assertNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(DummyOp.Spi.class.getName()));
        assertNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Heino"));
    }

    @OperatorMetadata(alias = "Heino")
    public static class DummyOp extends TestOps.Op1 {
        public static class Spi extends OperatorSpi {
            public Spi() {
                super(DummyOp.class);
            }
        }
    }
}
