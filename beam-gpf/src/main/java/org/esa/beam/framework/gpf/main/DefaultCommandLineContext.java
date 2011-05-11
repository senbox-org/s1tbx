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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The default command line context.
 */
class DefaultCommandLineContext implements CommandLineContext {
    @Override
    public Product readProduct(String productFilepath) throws IOException {
        Product product;
        product = ProductIO.readProduct(productFilepath);
        return product;
    }

    @Override
    public void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
        WriteOp writeOp = new WriteOp(targetProduct, new File(filePath), formatName);
        writeOp.setDeleteOutputOnFailure(true);
        writeOp.setWriteEntireTileRows(true);
        writeOp.setClearCacheAfterRowWrite(clearCacheAfterRowWrite);
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    @Override
    public Graph readGraph(String filepath, Map<String, String> parameterMap) throws GraphException, IOException {
        FileReader fileReader = new FileReader(filepath);
        Graph graph;
        try {
            graph = GraphIO.read(fileReader, parameterMap);
        } finally {
            fileReader.close();
        }
        return graph;
    }

    @Override
    public void executeGraph(Graph graph) throws GraphException {
        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);
    }

    @Override
    public Map<String, String> readParameterFile(String filepath) throws IOException {
        File file = new File(filepath);
        String fileContent = FileUtils.readText(file);
        if (fileContent.contains("<parameters>") && fileContent.contains("</parameters>")) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("gpt.xml.parameters", fileContent);
            return map;
        } else {
            return readProperies(fileContent);
        }
    }

    private Map<String, String> readProperies(String fileContent) throws IOException {
        Properties properties = new Properties();
        Reader reader = new StringReader(fileContent);
        HashMap<String, String> hashMap;
        try {
            properties.load(reader);
            hashMap = new HashMap<String, String>();
            for (Object object : properties.keySet()) {
                String key = object.toString();
                hashMap.put(key, properties.getProperty(key));
            }
        } finally {
            reader.close();
        }
        return hashMap;
    }

    @Override
    public Product createOpProduct(String opName, Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException {
        return GPF.createProduct(opName, parameters, sourceProducts);
    }

    @Override
    public void print(String m) {
        System.out.print(m);
    }
}
