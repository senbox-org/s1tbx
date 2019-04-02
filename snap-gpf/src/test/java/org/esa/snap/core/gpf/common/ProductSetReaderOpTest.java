package org.esa.snap.core.gpf.common;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * test ProductSetReader usage in GPF
 */
public class ProductSetReaderOpTest {

    private static File outputFile;

    @BeforeClass
    public static void setup() {
        outputFile = GlobalTestConfig.getBeamTestDataOutputFile("ProductSetReaderOpTest/writtenProduct.dim");
        outputFile.getParentFile().mkdirs();
    }

    @AfterClass
    public static void teardown() {
        FileUtils.deleteTree(outputFile.getParentFile());
    }

    @Test
    public void testProductSetGraph() throws IOException, GraphException {
        final String graphPath = ProductSetReaderOpTest.class.getResource("/org/esa/snap/core/gpf/common/productset/ProductSetMosaicGraph.xml").getFile();
        final File product1 = new File(ProductSetReaderOpTest.class.getResource("/org/esa/snap/core/gpf/common/productset/subset1.dim").getFile());
        final File product2 = new File(ProductSetReaderOpTest.class.getResource("/org/esa/snap/core/gpf/common/productset/subset2.dim").getFile());
        final File[] srcFiles = new File[] { product1, product2 };

        Graph graph;
        try (Reader fileReader = new FileReader(graphPath)) {
            graph = GraphIO.read(fileReader);
        }

        final GraphProcessor processor = new GraphProcessor();
        setIO(graph, srcFiles, outputFile, "BEAM-DIMAP");

        processor.executeGraph(graph, ProgressMonitor.NULL);
    }

    @Test
    public void testFolderProductSetGraph() throws IOException, GraphException {
        final String graphPath = ProductSetReaderOpTest.class.getResource("/org/esa/snap/core/gpf/common/productset/ProductSetMosaicGraph.xml").getFile();
        final File product1 = new File(ProductSetReaderOpTest.class.getResource("/org/esa/snap/core/gpf/common/productset/subset1.dim").getFile());
        final File[] srcFiles = new File[] { product1.getParentFile() };

        Graph graph;
        try (Reader fileReader = new FileReader(graphPath)) {
            graph = GraphIO.read(fileReader);
        }

        final GraphProcessor processor = new GraphProcessor();
        setIO(graph, srcFiles, outputFile, "BEAM-DIMAP");

        processor.executeGraph(graph, ProgressMonitor.NULL);
    }

    private void setIO(final Graph graph, final File[] srcFiles, final File tgtFile, final String format) {
        final String readOperatorAlias = OperatorSpi.getOperatorAlias(ProductSetReaderOp.class);
        final Node readerNode = findNode(graph, readOperatorAlias);
        if (readerNode != null) {
            final DomElement param = new DefaultDomElement("parameters");
            String value = "";
            for(File srcFile : srcFiles) {
                value += srcFile.getAbsolutePath()+",";
            }
            param.createChild("fileList").setValue(value);
            readerNode.setConfiguration(param);
        }

        final String writeOperatorAlias = OperatorSpi.getOperatorAlias(WriteOp.class);
        final Node writerNode = findNode(graph, writeOperatorAlias);
        if (writerNode != null && tgtFile != null) {
            final DomElement origParam = writerNode.getConfiguration();
            origParam.getChild("file").setValue(tgtFile.getAbsolutePath());
            if (format != null)
                origParam.getChild("formatName").setValue(format);
        }
    }

    private static Node findNode(final Graph graph, final String alias) {
        for (Node n : graph.getNodes()) {
            if (n.getOperatorName().equals(alias))
                return n;
        }
        return null;
    }
}
