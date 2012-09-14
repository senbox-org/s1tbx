/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.oceantools;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.TestUtils;

import java.io.File;


/**
 * Unit test for OceanTools Graph
 */
public class TestOceanToolsGraph extends TestCase {

    private static String graphFile = "OceanShipAndOilDetectionGraph.xml";
    private static String ASAR_IMM = "input\\ASA_IMM_1P_0739.N1";

    @Override
    protected void setUp() throws Exception {

    }

    @Override
    protected void tearDown() throws Exception {

    }

   public void testProcessGraph() throws GraphException {
        final File inputFile = new File(TestUtils.rootPathExpectedProducts, ASAR_IMM);
        final File outputFile = new File(ResourceUtils.getApplicationUserTempDataDir(), "tmpOut.dim");
        if(!inputFile.exists()) return;
        
     /*    final GraphExecuter graphEx = new GraphExecuter();
        graphEx.loadGraph(new File(ResourceUtils.getGraphFolder("User Graphs"), graphFile), false);

        GraphExecuter.setGraphIO(graphEx,
              "1-Read", inputFile.getAbsoluteFile(),
              "8-Write", outputFile.getAbsoluteFile(),
                DimapProductConstants.DIMAP_FORMAT_NAME);

        graphEx.InitGraph();
        graphEx.executeGraph(ProgressMonitor.NULL);   */
    }
}