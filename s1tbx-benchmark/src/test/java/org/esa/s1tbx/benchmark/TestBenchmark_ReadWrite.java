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
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class TestBenchmark_ReadWrite extends BaseBenchmarks {

    @Test
    public void testGRD_read_write() throws Exception {
        Benchmark b = new Benchmark("GRD_read_write") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = read(grdFile);
                write(srcProduct, outputFolder, DIMAP);
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    public void testGRD_read_writeGPF() throws Exception {
        Benchmark b = new Benchmark("GRD_read_writeGPF") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = read(grdFile);
                writeGPF(srcProduct, outputFolder, DIMAP);
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    @Ignore
    public void testGRD_read_writeGraph() throws Exception {
        Benchmark b = new Benchmark("GRD_read_write Graph") {
            @Override
            protected void execute() throws Exception {
                processReadWriteGraph(grdFile, outputFolder);
            }
        };
        b.run();
    }

    @Test
    public void testQP_read_write() throws Exception {
        Benchmark b = new Benchmark("QP Read_ProductIO.Write") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = read(qpFile);
                write(srcProduct, outputFolder, DIMAP);
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    public void testQP_read_writeGPF() throws Exception {
        Benchmark b = new Benchmark("QP Read_WriteGPF") {
            @Override
            protected void execute() throws Exception {
                final Product srcProduct = read(qpFile);
                writeGPF(srcProduct, outputFolder, DIMAP);
                srcProduct.dispose();
            }
        };
        b.run();
    }

    @Test
    public void testQP_read_writeGraph() throws Exception {
        Benchmark b = new Benchmark("QP_read_write Graph") {
            @Override
            protected void execute() throws Exception {
                processReadWriteGraph(qpFile, outputFolder);
            }
        };
        b.run();
    }

    public static void processReadWriteGraph(final File file, final File outputFolder) throws Exception {

        final Graph graph = new Graph("graph");

        final Node readNode = new Node("read", "read");
        final DomElement readParameters = new DefaultDomElement("parameters");
        readParameters.createChild("file").setValue(file.getAbsolutePath());
        readNode.setConfiguration(readParameters);
        graph.addNode(readNode);

        final Node writeNode = new Node("write", "write");
        final DomElement writeParameters = new DefaultDomElement("parameters");
        final File outFile = new File(outputFolder, file.getName());
        writeParameters.createChild("file").setValue(outFile.getAbsolutePath());
        writeNode.setConfiguration(writeParameters);
        writeNode.addSource(new NodeSource("source", "read"));
        graph.addNode(writeNode);

        final GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);
    }
}
