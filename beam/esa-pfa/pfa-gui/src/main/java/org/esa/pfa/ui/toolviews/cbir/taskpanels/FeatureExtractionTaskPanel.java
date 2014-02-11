/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.ui.toolviews.cbir.taskpanels;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.*;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;

/**
    Feature extraction Panel
 */
public class FeatureExtractionTaskPanel extends TaskPanel implements ActionListener {

    private final static String instructionsStr = "Extract features from the query images";
    private final CBIRSession session;

    public FeatureExtractionTaskPanel(final CBIRSession session) {
        super("Feature Extraction");
        this.session = session;

        createPanel();

        repaint();
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public TaskPanel getNextPanel() {
        return new LabelingTaskPanel(session);
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel listsPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
        listsPanel.setLayout(layout);

        final JButton addButton = new JButton("Process");
        addButton.setActionCommand("processButton");
        addButton.addActionListener(this);
        listsPanel.add(addButton);

        this.add(listsPanel, BorderLayout.SOUTH);
    }

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        try {
            final String command = event.getActionCommand();
            if (command.equals("processButton")) {
                File subsetFile = new File("c:\\Temp\\in\\in.dim");
                WriteOp writeOp = new WriteOp(session.getQueryProducts()[0], subsetFile, "BEAM-DIMAP");
                writeOp.setDeleteOutputOnFailure(true);
                writeOp.setWriteEntireTileRows(true);
                writeOp.writeProduct(ProgressMonitor.NULL);

                File graphFile = new File("c:\\Temp\\UrbanDetectionFeatureWriter.xml");
                Graph graph = GraphIO.read(new FileReader(graphFile), null);

                GraphProcessor processor = new GraphProcessor();
                setIO(graph, subsetFile);
                GraphContext graphContext = new GraphContext(graph);
                Product chainOut = processor.executeGraph(graphContext, com.bc.ceres.core.ProgressMonitor.NULL)[0];

                processor.executeGraph(graphContext, ProgressMonitor.NULL);

            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    public void setIO(final Graph graph, final File srcFile) {
        final String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
        final Node readerNode = findNode(graph, readOperatorAlias);
        if(readerNode != null) {
            final DomElement param = new DefaultDomElement("parameters");
            param.createChild("file").setValue(srcFile.getAbsolutePath());
            readerNode.setConfiguration(param);
        }
    }

    private static Node findNode(final Graph graph, final String alias) {
        for(Node n : graph.getNodes()) {
            if(n.getOperatorName().equals(alias))
                return n;
        }
        return null;
    }
}