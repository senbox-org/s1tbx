/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.snap.util.FileIOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Processes a graph
 */
public class GPFProcessor {
    private final Graph graph;
    private final GraphProcessor processor = new GraphProcessor();

    public GPFProcessor(final File graphFile) throws GraphException, IOException {
        this(graphFile, null);
    }

    public GPFProcessor(final File graphFile, final Map<String, String> parameterMap) throws GraphException, IOException {
        graph = readGraph(new FileReader(graphFile), parameterMap);
    }

    public static Graph readGraph(final Reader fileReader, final Map<String, String> parameterMap)
            throws GraphException, IOException {
        Graph graph = null;
        try {
            graph = GraphIO.read(fileReader, parameterMap);
        } finally {
            fileReader.close();
        }
        return graph;
    }

    public void setIO(final File srcFile, final File tgtFile, final String format) {
        final String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
        final Node readerNode = findNode(graph, readOperatorAlias);
        if (readerNode != null) {
            final DomElement param = new DefaultDomElement("parameters");
            param.createChild("file").setValue(srcFile.getAbsolutePath());
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

    public void executeGraph(final ProgressMonitor pm) throws GraphException {
        processor.executeGraph(graph, pm);
    }

    private static Node findNode(final Graph graph, final String alias) {
        for (Node n : graph.getNodes()) {
            if (n.getOperatorName().equals(alias))
                return n;
        }
        return null;
    }
}
