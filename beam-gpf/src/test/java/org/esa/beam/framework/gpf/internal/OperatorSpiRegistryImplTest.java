/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
