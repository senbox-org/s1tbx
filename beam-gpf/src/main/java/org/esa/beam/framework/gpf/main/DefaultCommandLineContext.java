/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.main;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessingObserver;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The default command line context.
 */
class DefaultCommandLineContext implements CommandLineContext {

    @Override
    public Product readProduct(String productFilepath) throws IOException {
        final File input = new File(productFilepath);
        final ProductReader productReader = ProductIO.getProductReaderForInput(input);
        if (productReader == null) {
            throw new OperatorException("No product reader found for '" + productFilepath + "'");
        }
        Product product = productReader.readProductNodes(input, null);
        if (product.getProductReader() == null) {
            product.setProductReader(productReader);
        }
        return product;
    }

    @Override
    public void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
        GPF.writeProduct(targetProduct, new File(filePath), formatName, clearCacheAfterRowWrite, ProgressMonitor.NULL);
    }

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
    public Product createOpProduct(String opName, Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException {
        return GPF.createProduct(opName, parameters, sourceProducts);
    }

    @Override
    public void print(String m) {
        System.out.print(m);
    }

    @Override
    public Logger getLogger() {
        return BeamLogManager.getSystemLogger();
    }

    @Override
    public Reader createReader(String textFilePath) throws FileNotFoundException {
        return new FileReader(textFilePath);
    }

    @Override
    public Writer createWriter(String fileName) throws IOException {
        return new FileWriter(fileName);
    }

    @Override
    public String[] list(String path) throws IOException {
        File directory = new File(path);
        if (directory.exists() && directory.isDirectory()) {
            return directory.list();
        } else {
            return null;
        }
    }

    @Override
    public boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    @Override
    public boolean isFile(String path) {
        return new File(path).isFile();
    }
}
