package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.TracingLayerListener;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;

public class BackgroundLayerTest  {
    @Test
    public void testConstructor() {
        BackgroundLayer layer = new BackgroundLayer(Color.BLUE);
        assertEquals(Color.BLUE, layer.getColor());
        assertNull(layer.getModelBounds());
    }

    @Test
    public void testStyleProperties() {
        BackgroundLayer layer = new BackgroundLayer(Color.WHITE);
        assertEquals(Color.WHITE, layer.getColor());

        TracingLayerListener ll = new TracingLayerListener();
        layer.addListener(ll);

        layer.setColor(Color.YELLOW);
        layer.setColor(Color.YELLOW);
        assertEquals(Color.YELLOW, layer.getColor());
        layer.setColor(Color.BLUE);
        layer.setColor(Color.BLUE);
        assertEquals(Color.BLUE, layer.getColor());

        assertEquals("color;color;", ll.trace);
    }
}