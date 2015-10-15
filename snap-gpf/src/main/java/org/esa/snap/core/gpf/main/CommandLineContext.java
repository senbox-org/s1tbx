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

package org.esa.snap.core.gpf.main;

import com.bc.ceres.metadata.SimpleFileSystem;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphProcessingObserver;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

interface CommandLineContext extends SimpleFileSystem {
    Product readProduct(String productFilepath) throws IOException;

    void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException;

    Graph readGraph(String filePath, Map<String, String> templateVariables) throws GraphException, IOException;

    void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException;

    void print(String m);

    Logger getLogger();

    boolean fileExists(String fileName);

}
