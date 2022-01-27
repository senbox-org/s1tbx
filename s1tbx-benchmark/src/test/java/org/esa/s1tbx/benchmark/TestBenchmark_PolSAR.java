/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.benchmark;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.polarimetric.gpf.PolarimetricDecompositionOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.junit.Test;

import java.io.File;

public class TestBenchmark_PolSAR extends BaseBenchmarks {

    @Test
    public void testQP_decomposition_pauli() throws Exception {
        decomposition("Pauli Decomposition", null);
    }

    @Test
    public void testQP_decomposition_pauli_writeOp() throws Exception {
        decompositionWriteOp("Pauli Decomposition", null);
    }

    @Test
    public void testQP_decomposition_pauli_graph() throws Exception {
        decompositionGraph("Pauli Decomposition", null);
    }

    @Test
    public void testQP_decomposition_sinclair() throws Exception {
        decomposition("Sinclair Decomposition", null);
    }

    @Test
    public void testQP_decomposition_FreemanDurden() throws Exception {
        decomposition("Freeman-Durden Decomposition", null);
    }

    @Test
    public void testQP_decomposition_GeneralizedFreemanDurden() throws Exception {
        decomposition("Generalized Freeman-Durden Decomposition", null);
    }

    @Test
    public void testQP_decomposition_Yamaguchi() throws Exception {
        decomposition("Yamaguchi Decomposition", null);
    }

    @Test
    public void testQP_decomposition_vanZyl() throws Exception {
        decomposition("van Zyl Decomposition", null);
    }

    @Test
    public void testQP_decomposition_Cloude() throws Exception {
        decomposition("Cloude Decomposition", null);
    }

    @Test
    public void testQP_decomposition_Touzi() throws Exception {
        decomposition("Touzi Decomposition", "outputTouziParamSet0");
    }

    @Test
    public void testQP_decomposition_HAAlphaQuadPol() throws Exception {
        decomposition("H-A-Alpha Quad Pol Decomposition", "outputHAAlpha");
    }

    private void decomposition(final String name, final String param) throws Exception {
        Benchmark b = new Benchmark(name + " productIO.write") {
            @Override
            protected void execute() throws Exception {
                process(name, param, false, outputFolder);
            }
        };
        b.run();
    }

    private void decompositionWriteOp(final String name, final String param) throws Exception {
        Benchmark b = new Benchmark(name + " GPF.write") {
            @Override
            protected void execute() throws Exception {
                process(name, param, true, outputFolder);
            }
        };
        b.run();
    }

    private void decompositionGraph(final String name, final String param) throws Exception {
        Benchmark b = new Benchmark(name + " GraphProcessor") {
            @Override
            protected void execute() throws Exception {
                processGraph(qpFile, outputFolder, name, param);
            }
        };
        b.run();
    }

    private void process(final String name, final String param, final boolean useWriteOp, final File outputFolder) throws Exception {
        final Product srcProduct = read(qpFile);

        final PolarimetricDecompositionOp op = new PolarimetricDecompositionOp();
        op.setSourceProduct(srcProduct);
        op.setParameter("decomposition", name);
        if(param != null) {
            op.setParameter(param, true);
        }
        Product trgProduct = op.getTargetProduct();

        if(useWriteOp) {
            writeGPF(trgProduct, outputFolder, DIMAP);
        } else {
            write(trgProduct, outputFolder, DIMAP);
        }

        trgProduct.dispose();
        srcProduct.dispose();
    }

    private void processGraph(final File file, final File outputFolder, final String name, final String param) throws Exception {

        final Graph graph = new Graph("graph");

        final Node readNode = new Node("read", "read");
        final DomElement readParameters = new DefaultDomElement("parameters");
        readParameters.createChild("file").setValue(file.getAbsolutePath());
        readNode.setConfiguration(readParameters);
        graph.addNode(readNode);

        final Node decompNode = new Node("Polarimetric-Decomposition", "Polarimetric-Decomposition");
        final DomElement decompParameters = new DefaultDomElement("parameters");
        decompParameters.createChild("decomposition").setValue(name);
        if(param != null) {
            decompParameters.createChild(param).setValue("true");
        }
        decompNode.setConfiguration(decompParameters);
        decompNode.addSource(new NodeSource("source", "read"));
        graph.addNode(decompNode);

        final Node writeNode = new Node("write", "write");
        final DomElement writeParameters = new DefaultDomElement("parameters");
        final File outFile = new File(outputFolder, file.getName());
        writeParameters.createChild("file").setValue(outFile.getAbsolutePath());
        writeNode.setConfiguration(writeParameters);
        writeNode.addSource(new NodeSource("source", "Polarimetric-Decomposition"));
        graph.addNode(writeNode);

        final GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);
    }
}
