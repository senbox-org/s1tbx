package org.esa.snap.core.datamodel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RasterDataNodeTimeCodigTest {

    private TimeCoding timeCoding;
    private RasterDataNode rasterDataNode;

    @Before
    public void setUp() throws Exception {
        timeCoding = mock(TimeCoding.class);
        rasterDataNode = mock(RasterDataNode.class, CALLS_REAL_METHODS);
    }

    @Test
    public void testSettingAndResetingTimeCoding() {
        assertNull(rasterDataNode.getTimeCoding());

        rasterDataNode.setTimeCoding(timeCoding);
        assertNotNull(rasterDataNode.getTimeCoding());

        rasterDataNode.setTimeCoding(null);
        assertNull(rasterDataNode.getTimeCoding());
    }

    @Test
    public void testPropertyChangeEventOnTimeCodingChange() {
        final Product product = new Product("pn", "pt", 20, 30);
        final RasterDataNode rasterDataNode = product.addBand("bn", ProductData.TYPE_INT16);
        final MyProductNodeListener nodeListener = new MyProductNodeListener();
        product.addProductNodeListener(nodeListener);

        rasterDataNode.setTimeCoding(timeCoding);
        rasterDataNode.setTimeCoding(null);

        final ArrayList<ProductNodeEvent> events = nodeListener.events;
        assertEquals(2, events.size());
        final ProductNodeEvent firstEvent = events.get(0);
        final ProductNodeEvent secondEvent = events.get(1);
        assertSame(rasterDataNode, firstEvent.getSourceNode());
        assertSame(rasterDataNode, secondEvent.getSourceNode());
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
