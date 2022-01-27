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
import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestBenchmark_SpeckleFilters extends BaseBenchmarks {

    @Test
    public void testGRD_specklefilter_Boxcar() throws Exception {
        specklefilter("Boxcar");
    }

    @Test
    public void testGRD_specklefilter_BoxcarWriteOp() throws Exception {
        specklefilterWriteOp("Boxcar");
    }

    @Test
    public void testGRD_specklefilter_BoxcarGraph() throws Exception {
        specklefilterGraph("Boxcar");
    }

    @Test
    public void testGRD_specklefilter_Median() throws Exception {
        specklefilter("Median");
    }

    @Test
    public void testGRD_specklefilter_Frost() throws Exception {
        specklefilter("Frost");
    }

    @Test
    public void testGRD_specklefilter_GammaMap() throws Exception {
        specklefilter("Gamma Map");
    }

    @Test
    public void testGRD_specklefilter_Lee() throws Exception {
        specklefilter("Lee");
    }

    @Test
    public void testGRD_specklefilter_RefinedLee() throws Exception {
        specklefilter("Refined Lee");
    }

    @Test
    public void testGRD_specklefilter_LeeSigma() throws Exception {
        specklefilter("Lee Sigma");
    }

    @Test
    public void testGRD_specklefilter_IDAN() throws Exception {
        specklefilter("IDAN");
    }

    private void specklefilter(final String name) throws Exception {
        Benchmark b = new Benchmark(name) {
            @Override
            protected void execute() throws Exception {
                process(name, outputFolder, false);
            }
        };
        b.run();
    }

    private void specklefilterWriteOp(final String name) throws Exception {
        Benchmark b = new Benchmark(name) {
            @Override
            protected void execute() throws Exception {
                process(name, outputFolder, true);
            }
        };
        b.run();
    }

    private void specklefilterGraph(final String name) throws Exception {
        Benchmark b = new Benchmark(name) {
            @Override
            protected void execute() throws Exception {
                processGraph(grdFile, outputFolder, name);
            }
        };
        b.run();
    }

    private void process(final String name, final File outputFolder, final boolean useWriteOp) throws IOException {
        final Product srcProduct = read(grdFile);

        SpeckleFilterOp op = new SpeckleFilterOp();
        op.setSourceProduct(srcProduct);
        op.SetFilter(name);
        Product trgProduct = op.getTargetProduct();

        if(useWriteOp) {
            writeGPF(trgProduct, outputFolder, DIMAP);
        } else {
            write(trgProduct, outputFolder, DIMAP);
        }

        trgProduct.dispose();
        srcProduct.dispose();
    }

    private void processGraph(final File file, final File outputFolder, final String name) throws Exception {

        final Graph graph = new Graph("graph");

        final Node readNode = new Node("read", "read");
        final DomElement readParameters = new DefaultDomElement("parameters");
        readParameters.createChild("file").setValue(file.getAbsolutePath());
        readNode.setConfiguration(readParameters);
        graph.addNode(readNode);

        final Node decompNode = new Node("Speckle-Filter", "Speckle-Filter");
        final DomElement decompParameters = new DefaultDomElement("parameters");
        decompParameters.createChild("filter").setValue(name);

        decompNode.setConfiguration(decompParameters);
        decompNode.addSource(new NodeSource("source", "read"));
        graph.addNode(decompNode);

        final Node writeNode = new Node("write", "write");
        final DomElement writeParameters = new DefaultDomElement("parameters");
        final File outFile = new File(outputFolder, file.getName());
        writeParameters.createChild("file").setValue(outFile.getAbsolutePath());
        writeNode.setConfiguration(writeParameters);
        writeNode.addSource(new NodeSource("source", "Speckle-Filter"));
        graph.addNode(writeNode);

        final GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);
    }
}
