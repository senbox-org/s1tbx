/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.gpf.internal;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.ReadOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Replace ProductSet operator with ReadOps
 */
public class ProductSetHandler {

    private final Graph graph;

    public final static String PRODUCT_SET_READER_NAME = "ProductSet-Reader";
    public final static String SEPARATOR = ",";
    public final static String SEPARATOR_ESC = "\\u002C"; // Unicode escape repr. of ','

    public ProductSetHandler(final Graph graph) {
        this.graph = graph;
    }

    public void replaceProductSetsWithReaders() throws GraphException {
        final ProductSetHandler.ProductSetData[] productSetDataList = findProductSetStacks("");
        if(productSetDataList.length != 0) {
            replaceAllProductSets(productSetDataList);
        }
    }

    private ProductSetData[] findProductSetStacks(final String fileListPath) throws GraphException {

        final List<ProductSetData> productSetDataList = new ArrayList<>();
        final Node[] nodes = graph.getNodes();
        for(Node n : nodes) {
            if(n.getOperatorName().equalsIgnoreCase(PRODUCT_SET_READER_NAME)) {
                final ProductSetData psData = new ProductSetData(n);

                boolean usingFileListPath = false;
                if(fileListPath != null) {
                    final File inputFolder = new File(fileListPath);
                    if(inputFolder.isDirectory() && inputFolder.exists()) {
                        usingFileListPath = true;
                        //ProductFunctions.scanForValidProducts(inputFolder, psData.fileList);
                    }
                }

                if(!usingFileListPath) {
                    final DomElement config = n.getConfiguration();
                    final DomElement[] params = config.getChildren();
                    for(DomElement p : params) {
                        if(p.getName().equals("fileList")) {
                            if(p.getValue() == null)
                                throw new GraphException(PRODUCT_SET_READER_NAME+" fileList is empty");

                            final StringTokenizer st = new StringTokenizer(p.getValue(), SEPARATOR);
                            final int length = st.countTokens();
                            for (int i = 0; i < length; i++) {
                                final String str = st.nextToken().replace(SEPARATOR_ESC, SEPARATOR).trim();
                                psData.fileList.add(str);
                            }
                            break;
                        }
                    }
                }
                if(psData.fileList.size() == 0)
                    throw new GraphException("no input products found in "+fileListPath);

                resolveFolders(psData);

                productSetDataList.add(psData);
            }
        }

        return productSetDataList.toArray(new ProductSetData[productSetDataList.size()]);
    }

    private void resolveFolders(final ProductSetData psData) {
        final List<String> toAdd = new ArrayList<>();
        final List<String> toRemove = new ArrayList<>();
        final ValidProductFileFilter dirFilter = new ValidProductFileFilter();
        for(String path : psData.fileList) {
            final File file = new File(path);
            if(file.exists() && file.isDirectory()) {
                toRemove.add(path);

                final File[] files = file.listFiles(dirFilter);
                if(files != null) {
                    for (File f : files) {
                        // test with readers
                        if(ProductIO.getProductReaderForInput(f) != null) {
                            toAdd.add(f.getAbsolutePath());
                        }
                    }
                }
            }
        }
        for(String path : toRemove) {
            psData.fileList.remove(path);
        }
        psData.fileList.addAll(toAdd);
    }

    private void replaceAllProductSets(final ProductSetData[] productSetDataList) {
        int cnt = 0;
        for(ProductSetData psData : productSetDataList) {

            final Node psNode = graph.getNode(psData.getNodeID());
            for(String filePath : psData.fileList) {

                ReplaceProductSetWithReaders(graph, psNode, cnt, "inserted--"+psNode.getId()+"--"+cnt, filePath);
                ++cnt;
            }
            if(!psData.fileList.isEmpty()) {
                for(Node n : graph.getNodes()) {
                    disconnectNodeSource(n, psNode.getId());
                }
                graph.removeNode(psNode.getId());
            }
        }
    }

    private static void ReplaceProductSetWithReaders(final Graph graph, final Node psNode, final int newNodeCnt,
                                                     final String id, String value) {

        final Node newNode = new Node(id, OperatorSpi.getOperatorAlias(ReadOp.class));
        final XppDomElement config = new XppDomElement("parameters");
        final XppDomElement fileParam = new XppDomElement("file");
        fileParam.setValue(value);
        config.addChild(fileParam);
        newNode.setConfiguration(config);

        graph.addNode(newNode);
        switchConnections(graph, newNode, newNodeCnt, psNode);
    }

    private static void switchConnections(final Graph graph, final Node newNode, final int newNodeCnt, final Node oldNode) {
        for(Node n : graph.getNodes()) {
            if(isNodeSource(n, oldNode)) {
                final NodeSource ns = new NodeSource("sourceProduct." + newNodeCnt, newNode.getId());
                n.addSource(ns);
            }
        }
    }

    private static void disconnectNodeSource(final Node node, final String id) {
        for (NodeSource ns : node.getSources()) {
            if (ns.getSourceNodeId().equals(id)) {
                node.removeSource(ns);
            }
        }
    }

    private static boolean isNodeSource(final Node node, final Node source) {

        final NodeSource[] sources = node.getSources();
        for (NodeSource ns : sources) {
            if (ns.getSourceNodeId().equals(source.getId())) {
                return true;
            }
        }
        return false;
    }


    private static class ProductSetData {
        private final String nodeID;
        private final List<String> fileList = new ArrayList<>();

        ProductSetData(final Node n) {
            this.nodeID = n.getId();
        }

        public String getNodeID() {
            return nodeID;
        }
    }

    private static class ValidProductFileFilter implements java.io.FileFilter {

        public ValidProductFileFilter() {
        }

        public boolean accept(final File file) {
            return !file.isDirectory();
        }
    }
}
