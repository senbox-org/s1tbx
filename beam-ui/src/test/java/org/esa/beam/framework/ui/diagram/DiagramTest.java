/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
