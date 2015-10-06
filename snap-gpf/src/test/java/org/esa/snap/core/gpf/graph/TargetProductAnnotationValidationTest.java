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

package org.esa.snap.core.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.TargetProduct;

public class TargetProductAnnotationValidationTest extends TestCase {


    private OperatorSpi notInitTargetProductOpSPI;

    @Override
    protected void setUp() throws Exception {
        notInitTargetProductOpSPI = new NotInitOutputOperator.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(notInitTargetProductOpSPI);

    }

    @Override
    protected void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(notInitTargetProductOpSPI);
    }

    public void testTargetProductIsSetByAnnotation() throws GraphException {
        Graph graph = new Graph("graph");

        Node node = new Node("OutputNotSet", notInitTargetProductOpSPI.getOperatorAlias());
        graph.addNode(node);

        GraphContext graphContext = new GraphContext(graph);
        NodeContext nodeContext = graphContext.getNodeContext(node);
        NotInitOutputOperator notInitOutputOperator = (NotInitOutputOperator) nodeContext.getOperator();
        assertNotNull("Output of operator is null", notInitOutputOperator.output);
        assertSame(nodeContext.getTargetProduct(), notInitOutputOperator.output);
    }


    @OperatorMetadata(alias = "NotInitOutputOperator")
    public static class NotInitOutputOperator extends Operator {

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {
            public Spi() {
                super(NotInitOutputOperator.class);
            }
        }
    }


}
