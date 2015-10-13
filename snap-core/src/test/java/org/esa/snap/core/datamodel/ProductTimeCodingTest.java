package org.esa.snap.core.datamodel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
    public void testSceneTimeCodingIsOptional() throws Exception {
        Product product = new Product("A", "B", 1, 1);
        assertNull(product.getSceneTimeCoding());

        product.setSceneTimeCoding(timeCoding);
        assertNotNull(product.getSceneTimeCoding());

        product.setSceneTimeCoding(null);
        assertNull(product.getSceneTimeCoding());
    }

    @Test
    public void testPropertyChangeEventOnSceneTimeCodingChange() {
        final Product product = new Product("pn", "pt", 20, 30);
        final MyProductNodeListener nodeListener = new MyProductNodeListener();
        product.addProductNodeListener(nodeListener);

        product.setSceneTimeCoding(timeCoding);
        product.setSceneTimeCoding(null);

        final ArrayList<ProductNodeEvent> events = nodeListener.events;
        assertEquals(2, events.size());
        final ProductNodeEvent firstEvent = events.get(0);
        final ProductNodeEvent secondEvent = events.get(1);
        assertSame(product, firstEvent.getSourceNode());
        assertSame(product, secondEvent.getSourceNode());
        assertEquals(Product.PROPERTY_NAME_SCENE_TIME_CODING, firstEvent.getPropertyName());
        assertEquals(Product.PROPERTY_NAME_SCENE_TIME_CODING, secondEvent.getPropertyName());
        assertNull(firstEvent.getOldValue());
        assertSame(timeCoding, firstEvent.getNewValue());
        assertSame(timeCoding, secondEvent.getOldValue());
        assertNull(secondEvent.getNewValue());
    }

    @Test
    public void testPropertyChangeEventOnTimeCodingChange() {
        final Product product = new Product("pn", "pt", 20, 30);
        final Band band = product.addBand("dummy", ProductData.TYPE_INT16);
        final MyProductNodeListener nodeListener = new MyProductNodeListener();
        product.addProductNodeListener(nodeListener);

        band.setTimeCoding(timeCoding);
        band.setTimeCoding(null);

        final ArrayList<ProductNodeEvent> events = nodeListener.events;
        assertEquals(2, events.size());
        final ProductNodeEvent firstEvent = events.get(0);
        final ProductNodeEvent secondEvent = events.get(1);
        assertSame(band, firstEvent.getSourceNode());
        assertSame(band, secondEvent.getSourceNode());
        assertEquals(RasterDataNode.PROPERTY_NAME_TIME_CODING, firstEvent.getPropertyName());
        assertEquals(RasterDataNode.PROPERTY_NAME_TIME_CODING, secondEvent.getPropertyName());
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

