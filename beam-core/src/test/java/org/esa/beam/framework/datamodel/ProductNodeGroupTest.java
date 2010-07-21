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

package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

public class ProductNodeGroupTest extends TestCase {

    public void testAddRemoveAreObservable() {
        Product p = new Product("p", "t", 10, 10);

        assertEquals(false, p.isModified());

        ProductNodeGroup<Placemark> pinGroup = p.getPinGroup();
        PNL listener = new PNL();
        p.addProductNodeListener(listener);

        assertNotNull(pinGroup);
        assertSame(p, pinGroup.getOwner());
        assertSame(p, pinGroup.getProduct());

        assertEquals(0, pinGroup.getNodeCount());

        Placemark placemark1 = new Placemark("p1", "l1", "", new PixelPos(0, 0), null, PinDescriptor.INSTANCE, null);
        Placemark placemark2 = new Placemark("p2", "l2", "", new PixelPos(0, 0), null, PinDescriptor.INSTANCE, null);
        Placemark placemark3 = new Placemark("p3", "l3", "", new PixelPos(0, 0), null, PinDescriptor.INSTANCE, null);
        pinGroup.add(placemark1);
        pinGroup.add(placemark2);
        pinGroup.add(placemark3);
        assertEquals(true, p.isModified());
        assertEquals(3, pinGroup.getNodeCount());
        assertEquals("+p1;+p2;+p3;", listener.trace);
        listener.trace = "";

        p.setModified(false);
        assertEquals(false, p.isModified());
        assertEquals("", listener.trace);

        assertSame(placemark1, pinGroup.get(0));
        assertSame(placemark2, pinGroup.get(1));
        assertSame(placemark3, pinGroup.get(2));

        pinGroup.remove(placemark1);
        pinGroup.remove(placemark2);
        pinGroup.remove(placemark3);
        assertEquals(0, pinGroup.getNodeCount());
        assertEquals("-p1;-p2;-p3;", listener.trace);
        listener.trace = "";

        p.addBand("b1", ProductData.TYPE_FLOAT32);
        p.addBand("b2", ProductData.TYPE_INT8);
        p.addBand("b3", ProductData.TYPE_FLOAT32);
        assertEquals("+b1;+b2;+b3;", listener.trace);
        listener.trace = "";

        p.setModified(false);
        assertEquals(false, p.isModified());
        assertEquals("", listener.trace);

        p.getBand("b2").setUnit("m/s");
        assertEquals("c:b2.unit;", listener.trace);
        listener.trace = "";

        assertEquals(true, p.isModified());

        p.removeBand(p.getBand("b1"));
        p.removeBand(p.getBand("b2"));
        p.removeBand(p.getBand("b3"));

        assertEquals("-b1;-b2;-b3;", listener.trace);
        listener.trace = "";
    }

    public void testNodeChangeIsObservable() {
        final Product p = new Product("p", "t", 10, 10);
        final ProductNodeGroup<Placemark> pinGroup = p.getPinGroup();

        final Placemark placemark = new Placemark("p1", "l1", "", new PixelPos(0, 0), null, PinDescriptor.INSTANCE, null);
        pinGroup.add(placemark);

        final PNL listener = new PNL();
        p.addProductNodeListener(listener);

        placemark.setLabel("new label");
        assertEquals(true, p.isModified());
        assertEquals("c:p1.label;", listener.trace);
    }

    public void testOwnership() {
        MetadataElement root = new MetadataElement("root");
        MetadataElement child = new MetadataElement("child");

        ProductNodeGroup<MetadataElement> referencingGroup = new ProductNodeGroup<MetadataElement>(null, "metadataElements", false);
        child.setOwner(root);
        assertSame(root, child.getOwner());
        referencingGroup.add(child);
        assertEquals(true, referencingGroup.contains(child));
        assertSame(root, child.getOwner());
        referencingGroup.remove(child);
        assertEquals(false, referencingGroup.contains(child));
        assertSame(root, child.getOwner());

        ProductNodeGroup<MetadataElement> owningGroup = new ProductNodeGroup<MetadataElement>(null, "metadataElements", true);
        child.setOwner(root);
        assertSame(root, child.getOwner());
        owningGroup.add(child);
        assertEquals(true, owningGroup.contains(child));
        assertSame(owningGroup, child.getOwner());
        owningGroup.remove(child);
        assertEquals(false, owningGroup.contains(child));
        assertSame(null, child.getOwner());
    }

    private static class PNL implements ProductNodeListener {

        String trace = "";

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Placemark) {
                trace += "c:" + event.getSourceNode().getName() + "." + event.getPropertyName() + ";";
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Placemark) {
                trace += "dc:" + event.getSourceNode().getName() + ";";
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Placemark) {
                trace += "+" + event.getSourceNode().getName() + ";";
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Placemark) {
                trace += "-" + event.getSourceNode().getName() + ";";
            }
        }
    }
}
