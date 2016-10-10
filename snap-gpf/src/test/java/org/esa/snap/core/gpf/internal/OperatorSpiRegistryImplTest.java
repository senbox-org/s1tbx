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

package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.DefaultServiceRegistry;
import com.bc.ceres.core.ServiceRegistry;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.TestOps;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class OperatorSpiRegistryImplTest {

    private OperatorSpiRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new OperatorSpiRegistryImpl(new DefaultServiceRegistry<>(OperatorSpi.class));
    }

    @Test
    public void testAddRemoveWithAndWithoutAlias() {
        DummyOp.Spi spi = new DummyOp.Spi();

        assertTrue(registry.addOperatorSpi(spi));
        assertFalse(registry.addOperatorSpi(spi));
        assertSame(spi, registry.getOperatorSpi(DummyOp.Spi.class.getName()));
        assertSame(spi, registry.getOperatorSpi("Heino"));

        Set<OperatorSpi> operatorSpis = registry.getOperatorSpis();
        assertEquals(1, operatorSpis.size());
        assertTrue(operatorSpis.contains(spi));

        assertTrue(registry.removeOperatorSpi(spi));
        assertFalse(registry.removeOperatorSpi(spi));
        assertNull(registry.getOperatorSpi(DummyOp.Spi.class.getName()));
        assertNull(registry.getOperatorSpi("Heino"));
    }

    @Test
    public void testMultipleSpiInstanceOfSameClass() {

        DummyOp.Spi heino1 = new DummyOp.Spi();
        DummyOp.Spi heino2 = new DummyOp.Spi();
        DummyOp.Spi heino3 = new DummyOp.Spi();

        assertTrue(registry.addOperatorSpi("Heino1", heino1));
        assertTrue(registry.addOperatorSpi("Heino2", heino2));
        assertTrue(registry.addOperatorSpi("Heino3", heino3));

        assertFalse(registry.addOperatorSpi("Heino1", heino1));
        assertFalse(registry.addOperatorSpi("Heino2", heino2));
        assertFalse(registry.addOperatorSpi("Heino3", heino3));

        assertSame(heino1, registry.getOperatorSpi("Heino1"));
        assertSame(heino2, registry.getOperatorSpi("Heino2"));
        assertSame(heino3, registry.getOperatorSpi("Heino3"));

        // We can't use the ServiceRegistry for keeping the services in this case, because all three
        // have the same class name (= key)
        ServiceRegistry<OperatorSpi> serviceRegistry = registry.getServiceRegistry();
        Set<OperatorSpi> services = serviceRegistry.getServices();
        assertFalse(services.contains(heino1));
        assertFalse(services.contains(heino2));
        assertFalse(services.contains(heino3));

        Set<OperatorSpi> operatorSpis = registry.getOperatorSpis();
        assertEquals(3, operatorSpis.size());
        assertTrue(operatorSpis.contains(heino1));
        assertTrue(operatorSpis.contains(heino2));
        assertTrue(operatorSpis.contains(heino3));
    }


    @Test
    public void testInSensentiveOperator() {
        DummyOp.Spi heino1 = new DummyOp.Spi();
        DummyOp.Spi heino2 = new DummyOp.Spi();
        DummyOp.Spi heino3 = new DummyOp.Spi();

        assertTrue(registry.addOperatorSpi("heino1", heino1));
        assertTrue(registry.addOperatorSpi("HEino2", heino2));
        assertTrue(registry.addOperatorSpi("HeINo3", heino3));

        assertSame(heino1, registry.getOperatorSpi("HEINO1"));
        assertSame(heino2, registry.getOperatorSpi("HeinO2"));
        assertSame(heino3, registry.getOperatorSpi("HEinO3"));
    }

    @Test
    public void testRemovalWhenSpiRegisteredWithAlias() throws InterruptedException {

        DummyOp.Spi heino1 = new DummyOp.Spi();

        assertTrue(registry.addOperatorSpi("Heino1", heino1));

        assertTrue(registry.removeOperatorSpi(heino1));
        assertTrue(registry.getAliases().isEmpty());

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
