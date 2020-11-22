package org.esa.s1tbx.benchmark;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.junit.Test;

import java.io.File;

public class TestBenchmark_Calibrate extends BaseBenchmarks {


    @Test
    public void testGRD_calibrate() throws Exception {
        calibrate("calibrateGRD");
    }

    @Test
    public void testGRD_calibrateWriteOp() throws Exception {
        calibrateWriteOp("calibrateGRD");
    }

    @Test
    public void testGRD_calibrateGraph() throws Exception {
        calibrateGraph("calibrateGRD");
    }

    private void calibrate(final String name) throws Exception {
        Benchmark b = new Benchmark(name + " productIO.write") {
            @Override
            protected void execute() throws Exception {
                process(grdFile, false);
            }
        };
        b.run();
    }

    private void calibrateWriteOp(final String name) throws Exception {
        Benchmark b = new Benchmark(name + " GPF.write") {
            @Override
            protected void execute() throws Exception {
                process(grdFile, true);
            }
        };
        b.run();
    }

    private void calibrateGraph(final String name) throws Exception {
        Benchmark b = new Benchmark(name + " GraphProcessor") {
            @Override
            protected void execute() throws Exception {
                processGraph(grdFile);
            }
        };
        b.run();
    }

    private void process(final File file, final boolean useWriteOp) throws Exception {
        final Product srcProduct = read(file);

        CalibrationOp op = new CalibrationOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();

        if(useWriteOp) {
            writeGPF(trgProduct, DIMAP);
        } else {
            write(trgProduct, DIMAP);
        }
    }

    private void processGraph(final File file) throws Exception {

        final Graph graph = new Graph("graph");

        final Node readNode = new Node("read", "read");
        final DomElement readParameters = new DefaultDomElement("parameters");
        readParameters.createChild("file").setValue(file.getAbsolutePath());
        readNode.setConfiguration(readParameters);
        graph.addNode(readNode);

        final Node decompNode = new Node("Calibration", "Calibration");
        decompNode.addSource(new NodeSource("source", "read"));
        graph.addNode(decompNode);

        final Node writeNode = new Node("write", "write");
        final DomElement writeParameters = new DefaultDomElement("parameters");
        final File outFile = new File(outputFolder, file.getName());
        writeParameters.createChild("file").setValue(outFile.getAbsolutePath());
        writeNode.setConfiguration(writeParameters);
        writeNode.addSource(new NodeSource("source", "Calibration"));
        graph.addNode(writeNode);

        final GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);
    }
}
