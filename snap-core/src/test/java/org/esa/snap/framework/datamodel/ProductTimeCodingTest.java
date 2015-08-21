package org.esa.snap.framework.datamodel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.*;

import java.util.ArrayList;

/**
 * @author Norman
 * @author Sabine
 */
public class ProductTimeCodingTest {

    private TimeCoding timeCoding;

    @Before
    public void setUp() {
        timeCoding = mock(TimeCoding.class);
    }

    @Test
    public void testTimeCodingIsOptional() throws Exception {
        Product product = new Product("A", "B", 1, 1);
        assertNull(product.getTimeCoding());

        product.setTimeCoding(timeCoding);
        assertNotNull(product.getTimeCoding());

        product.setTimeCoding(null);
        assertNull(product.getTimeCoding());
    }
    @Test
    public void testPropertyChangeEventOnTimeCodingChange() {
        final Product product = new Product("pn", "pt", 20, 30);
        final MyProductNodeListener nodeListener = new MyProductNodeListener();
        product.addProductNodeListener(nodeListener);

        product.setTimeCoding(timeCoding);
        product.setTimeCoding(null);

        final ArrayList<ProductNodeEvent> events = nodeListener.events;
        assertEquals(2, events.size());
        final ProductNodeEvent firstEvent = events.get(0);
        final ProductNodeEvent secondEvent = events.get(1);
        assertSame(product, firstEvent.getSourceNode());
        assertSame(product, secondEvent.getSourceNode());
        assertEquals(RasterDataNode.PROPERTY_NAME_TIMECODING, firstEvent.getPropertyName());
        assertEquals(RasterDataNode.PROPERTY_NAME_TIMECODING, secondEvent.getPropertyName());
        assertNull(firstEvent.getOldValue());
        assertSame(timeCoding, firstEvent.getNewValue());
        assertSame(timeCoding, secondEvent.getOldValue());
        assertNull(secondEvent.getNewValue());
    }

    private static class MyProductNodeListener implements ProductNodeListener {

        ArrayList<ProductNodeEvent> events = new ArrayList<>();

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            events.add(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            events.add(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            events.add(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            events.add(event);
        }
    }

}

