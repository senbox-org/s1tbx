/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.dialogs;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.db.CommonReaders;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.esa.snap.ui.AppContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes coregistration with multiple master/slave pair target products
 */
public class VirtualStackCoregistrationDialog extends BatchGraphDialog {

    public VirtualStackCoregistrationDialog(final AppContext theAppContext, final String title, final String helpID,
                                            final boolean closeOnDone) {
        super(theAppContext, title, helpID, closeOnDone);

        openProcessedProducts = true;

        LoadGraph(getDefaultGraphFile());
    }

    private static File getDefaultGraphFile() {
        return new File(defaultGraphPath + File.separator + "internal" + File.separator + "coregistration",
                        "MultiOutputCoregister.xml");
    }

    @Override
    protected void cloneGraphs() throws Exception {
        final GraphExecuter graphEx = graphExecutorList.get(0);
        for (int graphIndex = 1; graphIndex < graphExecutorList.size(); ++graphIndex) {
            final GraphExecuter cloneGraphEx = graphExecutorList.get(graphIndex);
            cloneGraphEx.ClearGraph();
        }
        graphExecutorList.clear();
        graphExecutorList.add(graphEx);

        final File[] fileList = productSetPanel.getFileList();
        for (int graphIndex = 1; graphIndex < fileList.length - 1; ++graphIndex) {

            final GraphExecuter cloneGraphEx = new GraphExecuter();
            LoadGraph(cloneGraphEx, graphFile, false);
            graphExecutorList.add(cloneGraphEx);

            // copy UI parameter to clone
            final GraphNode[] cloneGraphNodes = cloneGraphEx.GetGraphNodes();
            for (GraphNode cloneNode : cloneGraphNodes) {
                final GraphNode node = graphEx.getGraphNodeList().findGraphNode(cloneNode.getID());
                //               if (node != null)
                //                   cloneNode.setOperatorUI(node.GetOperatorUI());
            }
        }
    }

    private File[] getSlaveFileList() {
        final File[] fileList = productSetPanel.getFileList();
        if (fileList == null || fileList.length == 0)
            return null;

        final File masterFile = fileList[0];

        final List<File> slaveList = new ArrayList<>(fileList.length);
        for (File f : fileList) {
            if (f != masterFile) {
                slaveList.add(f);
            }
        }
        return slaveList.toArray(new File[slaveList.size()]);
    }

    @Override
    protected void assignParameters() {
        final File[] fileList = productSetPanel.getFileList();
        if (fileList == null || fileList.length == 0)
            return;

        final File masterFile = fileList[0];
        final File[] slaveList = getSlaveFileList();

        int graphIndex = 0;
        for (File slaveFile : slaveList) {
            String name;
            try {
                final Product slvProduct = CommonReaders.readProduct(slaveFile);
                name = slvProduct.getName();
            } catch (IOException e) {
                name = FileUtils.getFilenameWithoutExtension(slaveFile);
            }
            final File targetFile = new File(productSetPanel.getTargetFolder(), name);
            final String targetFormat = productSetPanel.getTargetFormat();

            setIO(graphExecutorList.get(graphIndex),
                  "Read", masterFile,
                  "Write", targetFile, targetFormat);
            setSlaveIO(graphExecutorList.get(graphIndex),
                       "ProductSet-Reader", masterFile, new File[]{slaveFile});
            ++graphIndex;
        }
    }
}