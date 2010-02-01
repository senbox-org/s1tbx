package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

public class ProductNodeGroupTest extends TestCase {

    public void testAddRemoveAreObservable() {
        Product p = new Product("p", "t", 10, 10);

        assertEquals(false, p.isModified());

        ProductNodeGroup<Pin> pinGroup = p.getPinGroup();
        PNL listener = new PNL();
        p.addProductNodeListener(listener);

        assertNotNull(pinGroup);
        assertSame(p, pinGroup.getOwner());
        assertSame(p, pinGroup.getProduct());

        assertEquals(0, pinGroup.getNodeCount());

        Pin pin1 = new Pin("p1", "l1", "", new PixelPos(0, 0), null, PlacemarkSymbol.createDefaultPinSymbol(), null);
        Pin pin2 = new Pin("p2", "l2", "", new PixelPos(0, 0), null, PlacemarkSymbol.createDefaultPinSymbol(), null);
        Pin pin3 = new Pin("p3", "l3", "", new PixelPos(0, 0), null, PlacemarkSymbol.createDefaultPinSymbol(), null);
        pinGroup.add(pin1);
        pinGroup.add(pin2);
        pinGroup.add(pin3);
        assertEquals(true, p.isModified());
        assertEquals(3, pinGroup.getNodeCount());
        assertEquals("+p1;+p2;+p3;", listener.trace);
        listener.trace = "";

        p.setModified(false);
        assertEquals(false, p.isModified());
        assertEquals("", listener.trace);

        assertSame(pin1, pinGroup.get(0));
        assertSame(pin2, pinGroup.get(1));
        assertSame(pin3, pinGroup.get(2));

        pinGroup.remove(pin1);
        pinGroup.remove(pin2);
        pinGroup.remove(pin3);
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
        final ProductNodeGroup<Pin> pinGroup = p.getPinGroup();

        final Pin pin = new Pin("p1", "l1", "", new PixelPos(0, 0), null, PlacemarkSymbol.createDefaultPinSymbol(), null);
        pinGroup.add(pin);

        final PNL listener = new PNL();
        p.addProductNodeListener(listener);

        pin.setLabel("new label");
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
                || event.getSource() instanceof Pin) {
                trace += "c:" + event.getSourceNode().getName() + "." + event.getPropertyName() + ";";
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Pin) {
                trace += "dc:" + event.getSourceNode().getName() + ";";
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Pin) {
                trace += "+" + event.getSourceNode().getName() + ";";
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSource() instanceof Band
                || event.getSource() instanceof Pin) {
                trace += "-" + event.getSourceNode().getName() + ";";
            }
        }
    }
}
