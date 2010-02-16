package org.esa.beam.dataio.dimap.spi;
/*
 * $Id: DimapPersistableRegistryTest.java,v 1.2 2007/03/22 16:56:59 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by marco.
 *
 * @author marco
 * @version $Revision$ $Date$
 */
public class DimapPersistableRegistryTest extends TestCase {

    private final static Class[] EXPECTED_SPIS = new Class[] {
            GeneralFilterBandPersistableSpi.class,
            ConvolutionFilterBandPersistableSpi.class
    };

    public void testDimapPersistableRegistryCreation() {
        assertNotNull(DimapPersistableSpiRegistry.getInstance());
    }

    public void testGetStandardSpis() {
        final DimapPersistableSpiRegistry registry = DimapPersistableSpiRegistry.getInstance();

        final Iterator<DimapPersistableSpi> persistableSpis = registry.getPersistableSpis();
        assertNotNull(persistableSpis);
        assertIteratorContainsInstances(persistableSpis, EXPECTED_SPIS);

    }
    private static void assertIteratorContainsInstances(Iterator<DimapPersistableSpi> iterator, Class[] expectedClasses) {

        final List<DimapPersistableSpi> list = copyToList(iterator);

        for (int i = 0; i < expectedClasses.length; i++) {
            final Class expectedClass = expectedClasses[i];
            boolean found = false;
            for (int j = 0; j < list.size(); j++) {
                final Object instance =  list.get(j);
                if(expectedClass.isInstance(instance)) {
                    found = true;
                }
            }
            if(!found) {
                fail("No instance found of '" + expectedClass.toString() + "' in given iterator");
            }
        }
    }

    private static List<DimapPersistableSpi> copyToList(Iterator<DimapPersistableSpi> iterator) {
        final ArrayList<DimapPersistableSpi> list = new ArrayList<DimapPersistableSpi>();
        while(iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }


}