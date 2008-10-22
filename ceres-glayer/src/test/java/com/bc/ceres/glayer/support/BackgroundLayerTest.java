package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.TracingLayerListener;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;

public class BackgroundLayerTest  {
    @Test
    public void testConstructor() {
        BackgroundLayer layer = new BackgroundLayer(Color.BLUE);
        assertEquals(Color.BLUE, layer.getPaint());
        assertNull(layer.getModelBounds());
    }

    @Test
    public void testStyleProperties() {
        BackgroundLayer layer = new BackgroundLayer(Color.WHITE);
        assertEquals(Color.WHITE, layer.getPaint());

        TracingLayerListener ll = new TracingLayerListener();
        layer.addListener(ll);

        layer.setPaint(Color.YELLOW);
        layer.setPaint(Color.YELLOW);
        assertEquals(Color.YELLOW, layer.getPaint());
        layer.setPaint(Color.BLUE);
        layer.setPaint(Color.BLUE);
        assertEquals(Color.BLUE, layer.getPaint());

        assertEquals("paint;paint;", ll.trace);
    }
}