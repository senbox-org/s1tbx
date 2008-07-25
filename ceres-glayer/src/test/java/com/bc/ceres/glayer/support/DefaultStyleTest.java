package com.bc.ceres.glayer.support;

import junit.framework.TestCase;

import com.bc.ceres.glayer.TracingPropertyChangeListener;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class DefaultStyleTest extends TestCase {
    public void testDefaultStyle() {

        final TracingPropertyChangeListener pcl0 = new TracingPropertyChangeListener();
        final TracingPropertyChangeListener pcl1 = new TracingPropertyChangeListener();

        final DefaultStyle style = new DefaultStyle();
        assertSame(DefaultStyle.getInstance(), style.getDefaultStyle());

        DefaultStyle.getInstance().addPropertyChangeListener(pcl0);
        style.addPropertyChangeListener(pcl1);

        ////////////////////////////
        assertEquals("", pcl0.trace);
        assertEquals("", pcl1.trace);

        assertEquals(1.0, DefaultStyle.getInstance().getOpacity(), 1.0e-10);
        assertEquals(1.0, style.getOpacity(), 1.0e-10);

        ////////////////////////////
        DefaultStyle.getInstance().setOpacity(0.3);
        assertEquals("opacity;", pcl0.trace);
        assertEquals("opacity;", pcl1.trace); // delegated from pcl0

        assertEquals(0.3, DefaultStyle.getInstance().getOpacity(), 1.0e-10);
        assertEquals(0.3, style.getOpacity(), 1.0e-10);

        ////////////////////////////
        style.setOpacity(0.5);
        assertEquals("opacity;", pcl0.trace); // not affected
        assertEquals("opacity;opacity;", pcl1.trace);

        assertEquals(0.3, DefaultStyle.getInstance().getOpacity(), 1.0e-10);
        assertEquals(0.5, style.getOpacity(), 1.0e-10);

        ////////////////////////////
        DefaultStyle.getInstance().setOpacity(0.4);
        assertEquals("opacity;opacity;", pcl0.trace);
        assertEquals("opacity;opacity;", pcl1.trace); // not delegated from pcl0, because is set 

        assertEquals(0.4, DefaultStyle.getInstance().getOpacity(), 1.0e-10);
        assertEquals(0.5, style.getOpacity(), 1.0e-10);
    }

}
