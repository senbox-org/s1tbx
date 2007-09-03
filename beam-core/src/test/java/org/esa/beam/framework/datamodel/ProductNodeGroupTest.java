package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class ProductNodeGroupTest extends TestCase {
    String s = "";

    public void testAddRemoveAreObservable() {
        Product p = new Product("p", "t", 10, 10);
        p.addProductNodeListener(new ProductNodeListener() {
            public void nodeChanged(ProductNodeEvent event) {
            }

            public void nodeDataChanged(ProductNodeEvent event) {
            }

            public void nodeAdded(ProductNodeEvent event) {
                s += "+"+event.getSourceNode().getName() + ";";
            }

            public void nodeRemoved(ProductNodeEvent event) {
                s += "-"+event.getSourceNode().getName() + ";";
            }
        });
        ProductNodeGroup<Pin> pinGroup = p.getPinGroup();

        assertNotNull(pinGroup);
        assertSame(p, pinGroup.getOwner());
        assertSame(p, pinGroup.getProduct());

        assertEquals(0, pinGroup.getNodeCount());

        Pin pin1 = new Pin("p1", "l1", "", new PixelPos(0, 0), null, PinSymbol.createDefaultPinSymbol());
        Pin pin2 = new Pin("p2", "l2", "", new PixelPos(0, 0), null, PinSymbol.createDefaultPinSymbol());
        Pin pin3 = new Pin("p3", "l3", "", new PixelPos(0, 0), null, PinSymbol.createDefaultPinSymbol());
        pinGroup.add(pin1);
        pinGroup.add(pin2);
        pinGroup.add(pin3);
        assertEquals(3, pinGroup.getNodeCount());
        assertEquals("+p1;+p2;+p3;", s);

        assertSame(pin1, pinGroup.get(0));
        assertSame(pin2, pinGroup.get(1));
        assertSame(pin3, pinGroup.get(2));

        pinGroup.remove(pin1);
        pinGroup.remove(pin2);
        pinGroup.remove(pin3);
        assertEquals(0, pinGroup.getNodeCount());
        assertEquals("+p1;+p2;+p3;-p1;-p2;-p3;", s);

        p.addBand("b1", ProductData.TYPE_FLOAT32);
        assertEquals("+p1;+p2;+p3;-p1;-p2;-p3;+b1;", s);

    }

}
