/*
 * $Id: ProductNode_ModifiedPropagationDirectionTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;

public class ProductNode_ModifiedPropagationDirectionTest extends TestCase {

    public void testFireNodeModified_SequenceDirection() {
        final Product node1 = new Product("n", "t", 5, 5);
        final MetadataElement node2 = node1.getMetadataRoot();
        final MetadataElement node3 = new MetadataElement("elem1");
        final MetadataElement node4 = new MetadataElement("elem2");
        final MetadataElement node5 = new MetadataElement("elem3");
        final MetadataAttribute node6 = new MetadataAttribute("attrib", ProductData.createInstance(new int[]{2, 3}),
                                                              false);

        node5.addAttribute(node6);
        node4.addElement(node5);
        node3.addElement(node4);
        node2.addElement(node3);

        node1.setModified(false);

        final ArrayList propertyNameList = new ArrayList();
        final ArrayList propertySourceList = new ArrayList();
        final ArrayList modifiedList = new ArrayList();
        node1.addProductNodeListener(new ProductNodeListenerAdapter() {
            public void nodeChanged(ProductNodeEvent event) {
                propertyNameList.add(event.getPropertyName());
                propertySourceList.add(event.getSource());
                final boolean[] modifiedState = new boolean[]{
                        node1.isModified(),
                        node2.isModified(),
                        node3.isModified(),
                        node4.isModified(),
                        node5.isModified(),
                        node6.isModified(),
                };
                modifiedList.add(modifiedState);
            }
        });

        // If the 'modified' property of any ProductNode was changed to true, it is
        // necessarry that the node propagate his state prior to the nodes owner
        // and in the second step the node fires the node changed event for the
        // 'modified' property. So, if any listener reacts to this event, it is
        // guaranteed that the state of the ProductNode-Tree is stable because
        // the property is fully propagated in the entire tree before any event
        // is fired.

        node6.setModified(true); // setting the leaf to modified

        assertEquals(6, propertyNameList.size());
        assertEquals("modified", propertyNameList.get(0));
        assertEquals("modified", propertyNameList.get(1));
        assertEquals("modified", propertyNameList.get(2));
        assertEquals("modified", propertyNameList.get(3));
        assertEquals("modified", propertyNameList.get(4));
        assertEquals("modified", propertyNameList.get(5));

        assertEquals(6, propertySourceList.size());
        assertSame(node1, propertySourceList.get(0));
        assertSame(node2, propertySourceList.get(1));
        assertSame(node3, propertySourceList.get(2));
        assertSame(node4, propertySourceList.get(3));
        assertSame(node5, propertySourceList.get(4));
        assertSame(node6, propertySourceList.get(5));

        final boolean[] expTrue = new boolean[]{true, true, true, true, true, true};
        assertEquals(6, modifiedList.size());
        assertTrue(Arrays.equals(expTrue, (boolean[]) modifiedList.get(0)));
        assertTrue(Arrays.equals(expTrue, (boolean[]) modifiedList.get(1)));
        assertTrue(Arrays.equals(expTrue, (boolean[]) modifiedList.get(2)));
        assertTrue(Arrays.equals(expTrue, (boolean[]) modifiedList.get(3)));
        assertTrue(Arrays.equals(expTrue, (boolean[]) modifiedList.get(4)));
        assertTrue(Arrays.equals(expTrue, (boolean[]) modifiedList.get(5)));

        propertyNameList.clear();
        propertySourceList.clear();
        modifiedList.clear();

        // the same behavior in the other direction

        node1.setModified(false); // set the modified property of the root node to false

        assertEquals(6, propertyNameList.size());
        assertEquals("modified", propertyNameList.get(0));
        assertEquals("modified", propertyNameList.get(1));
        assertEquals("modified", propertyNameList.get(2));
        assertEquals("modified", propertyNameList.get(3));
        assertEquals("modified", propertyNameList.get(4));
        assertEquals("modified", propertyNameList.get(5));

        assertEquals(6, propertySourceList.size());
        assertSame(node1, propertySourceList.get(0));
        assertSame(node2, propertySourceList.get(1));
        assertSame(node3, propertySourceList.get(2));
        assertSame(node4, propertySourceList.get(3));
        assertSame(node5, propertySourceList.get(4));
        assertSame(node6, propertySourceList.get(5));

        final boolean[] expFalse = new boolean[]{false, false, false, false, false, false};
        assertEquals(6, modifiedList.size());
//        assertTrue(Arrays.equals(expFalse, (boolean[]) modifiedList.get(0)));
//        assertTrue(Arrays.equals(expFalse, (boolean[]) modifiedList.get(1)));
//        assertTrue(Arrays.equals(expFalse, (boolean[]) modifiedList.get(2)));
//        assertTrue(Arrays.equals(expFalse, (boolean[]) modifiedList.get(3)));
//        assertTrue(Arrays.equals(expFalse, (boolean[]) modifiedList.get(4)));
//        assertTrue(Arrays.equals(expFalse, (boolean[]) modifiedList.get(5)));
    }
}
