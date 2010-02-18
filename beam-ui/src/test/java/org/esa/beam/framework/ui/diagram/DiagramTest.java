package org.esa.beam.framework.ui.diagram;

import junit.framework.TestCase;
import org.esa.beam.util.math.Range;

import java.awt.geom.Point2D;


public class DiagramTest extends TestCase {
    public void testTransform() {
        Diagram.RectTransform rectTransform = new Diagram.RectTransform(new Range(0, 10),
                                                                        new Range(-1, +1),
                                                                        new Range(100, 200),
                                                                        new Range(100, 0));
        Point2D a, b;

        a = new Point2D.Double(5, 0);
        b = rectTransform.transformA2B(a, null);
        assertEquals(new Point2D.Double(150.0, 50.0), b);
        assertEquals(a, rectTransform.transformB2A(b, null));

        a = new Point2D.Double(7.5, -0.25);
        b = rectTransform.transformA2B(a, null);
        assertEquals(new Point2D.Double(175.0, 62.5), b);
        assertEquals(a, rectTransform.transformB2A(b, null));
    }



}
