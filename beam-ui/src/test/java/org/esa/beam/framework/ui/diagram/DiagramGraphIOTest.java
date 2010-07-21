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

import java.io.StringWriter;
import java.io.IOException;
import java.io.StringReader;

public class DiagramGraphIOTest extends TestCase {

    public void testIOWithEqualXValues() throws IOException {
        double[] expectedXValues = new double[]{0, 1, 2, 3, 4, 5};
        double[] expectedY1Values = new double[]{0, 1, 4, 9, 16, 25};
        double[] expectedY2Values = new double[]{0, 1, 2, 3, 5, 8};
        double[] expectedY3Values = new double[]{0.5, 0.4, 0.3, 0.2, 0.1, 0.0};
        DiagramGraph[] expectedGraphs = new DefaultDiagramGraph[]{
                new DefaultDiagramGraph("x", expectedXValues, "y1", expectedY1Values),
                new DefaultDiagramGraph("x", expectedXValues, "y2", expectedY2Values),
                new DefaultDiagramGraph("x", expectedXValues, "y3", expectedY3Values)
        };

        testIO(expectedGraphs);
    }

    public void testIOWithDifferentXValues() throws IOException {
        double[] expectedX1Values = new double[]{0, 1, 2, 3, 4, 5};
        double[] expectedY1Values = new double[]{0, 1, 2, 3, 5, 8};
        double[] expectedX2Values = new double[]{4, 9, 16, 25}; // length = 4!
        double[] expectedY2Values = new double[]{0.3, 0.2, 0.1, 0.0}; // length = 4!
        DiagramGraph[] expectedGraphs = new DefaultDiagramGraph[]{
                new DefaultDiagramGraph("x1", expectedX1Values, "y1", expectedY1Values),
                new DefaultDiagramGraph("x2", expectedX2Values, "y2", expectedY2Values),
        };
       testIO(expectedGraphs);
    }

    private void testIO(DiagramGraph[] expectedGraphs) throws IOException {
        StringWriter writer1 = new StringWriter();
        DiagramGraphIO.writeGraphs(expectedGraphs, writer1);

        DiagramGraph[] actualGraphs = DiagramGraphIO.readGraphs(new StringReader(writer1.toString()));
        assertEqualGraphs(actualGraphs, expectedGraphs);

        StringWriter writer2 = new StringWriter();
        DiagramGraphIO.writeGraphs(expectedGraphs, writer2);
        assertEquals(writer1.toString(), writer2.toString());
    }

    private void assertEqualGraphs(DiagramGraph[] actualGraphs, DiagramGraph[] expectedGraphs) {
        assertNotNull(actualGraphs.length);
        assertEquals(expectedGraphs.length, actualGraphs.length);
        for (int i = 0; i < expectedGraphs.length; i++) {
            assertEqualGraphs(expectedGraphs[i], actualGraphs[i]);
        }
    }

    private void assertEqualGraphs(DiagramGraph expectedGraph, DiagramGraph actualGraph) {
        assertNotNull(actualGraph);
        assertEquals(expectedGraph.getXName(), actualGraph.getXName());
        assertEquals(expectedGraph.getYName(), actualGraph.getYName());
        assertEquals(expectedGraph.getNumValues(), actualGraph.getNumValues());
        for (int i = 0; i < expectedGraph.getNumValues(); i++) {
            assertEquals(expectedGraph.getXValueAt(i), actualGraph.getXValueAt(i), 1e-10);
            assertEquals(expectedGraph.getYValueAt(i), actualGraph.getYValueAt(i), 1e-10);
        }
    }

}
