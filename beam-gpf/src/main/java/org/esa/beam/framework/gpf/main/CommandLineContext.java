package org.esa.beam.framework.gpf.main;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;

import java.io.IOException;
import java.util.Map;

interface CommandLineContext {
    Product readProduct(String productFilepath) throws IOException;

    void writeProduct(Product targetProduct, String filePath, String formatName) throws IOException;

    Graph readGraph(String filepath, Map<String, String> parameterMap) throws GraphException, IOException;

    void executeGraph(Graph graph) throws GraphException;

    Map<String, String> readParameterFile(String propertiesFilepath) throws IOException;

    Product createOpProduct(String opName, Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException;

    void print(String m);
}
