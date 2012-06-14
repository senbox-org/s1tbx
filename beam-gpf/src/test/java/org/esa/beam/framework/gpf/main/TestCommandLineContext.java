package org.esa.beam.framework.gpf.main;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessingObserver;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.framework.gpf.internal.OperatorContext;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;
import org.junit.Ignore;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Norman Fomferra
 */
@Ignore
public abstract class TestCommandLineContext implements CommandLineContext {

    final StringBuffer printBuffer = new StringBuffer();
    final Map<String, String> textFiles = new HashMap<String, String>();
    final List<StringReader> readers = new ArrayList<StringReader>();
    final Map<String, StringWriter> writers = new HashMap<String, StringWriter>();
    final Map<String, Product> products = new HashMap<String, Product>();
    private Product targetProduct;

    @Override
    public Graph readGraph(String filePath, Map<String, String> templateVariables) throws GraphException, IOException {
        Reader fileReader = createReader(filePath);
        Graph graph;
        try {
            graph = GraphIO.read(fileReader, templateVariables);
        } finally {
            fileReader.close();
        }
        return graph;
    }

    @Override
    public void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException {
        GraphProcessor processor = new GraphProcessor();
        if (observer != null) {
            processor.addObserver(observer);
        }
        processor.executeGraph(graph, ProgressMonitor.NULL);
    }

    @Override
    public Product readProduct(String productFilepath) throws IOException {
        if (!products.containsKey(productFilepath)) {
            throw new FileNotFoundException(productFilepath);
        }
        return products.get(productFilepath);
    }

    @Override
    public Reader createReader(String fileName) throws FileNotFoundException {
        if (!textFiles.containsKey(fileName)) {
            throw new FileNotFoundException(fileName);
        }
        StringReader stringReader = new StringReader(textFiles.get(fileName));
        readers.add(stringReader);
        return stringReader;
    }

    @Override
    public Writer createWriter(String fileName) throws IOException {
        StringWriter stringWriter = new StringWriter();
        writers.put(fileName, stringWriter);
        return stringWriter;
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger("TestCommandLineContext");
    }

    @Override
    public void print(String m) {
        printBuffer.append(m);
        printBuffer.append('\n');
    }

    @Override
    public Product createOpProduct(String opName, Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts) throws OperatorException {
        if (targetProduct != null) {
            Assert.fail("createOpProduct() called twice");
        }
        targetProduct = new Product("target", "TEST", 10, 10);
        final OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        final Operator operator = operatorSpiRegistry.getOperatorSpi(opName).createOperator();
        targetProduct.setProductReader(new OperatorProductReader(new OperatorContext(operator)));
        return targetProduct;
    }

    @Override
    public void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
        products.put(filePath, targetProduct);
    }
}
