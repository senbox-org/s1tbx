package com.bc.layer;

import com.bc.ImagingTestCase;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class BackgroundLayerTest extends ImagingTestCase {
    public void testConstructor() {
        BackgroundLayer layer = new BackgroundLayer(Color.BLUE);
        assertEquals(Color.BLUE, layer.getPaint());
        assertNull(layer.getBoundingBox());
    }

    public void testStyleProperties() {
        BackgroundLayer layer = new BackgroundLayer(Color.WHITE);
        assertEquals(Color.WHITE, layer.getPaint());

        TracingPCL tracingPCL = new TracingPCL();
        layer.addPropertyChangeListener(tracingPCL);

        layer.setPaint(Color.YELLOW);
        layer.setPaint(Color.YELLOW);
        assertEquals(Color.YELLOW, layer.getPaint());
        layer.setPaint(Color.BLUE);
        layer.setPaint(Color.BLUE);
        assertEquals(Color.BLUE, layer.getPaint());

        assertEquals("paint;paint;", tracingPCL.trace);
    }
}