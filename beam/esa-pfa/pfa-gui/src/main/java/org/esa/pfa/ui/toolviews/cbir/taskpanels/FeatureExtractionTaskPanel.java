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
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.LabelBarProgressMonitor;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
    Feature extraction Panel
 */
public class FeatureExtractionTaskPanel extends TaskPanel implements ActionListener {

    private final static String instructionsStr = "Extract features from the query images";
    private final CBIRSession session;

    private Map<Patch, LabelBarProgressMonitor> progressMap = new HashMap<>(5);

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

        for(Patch patch : session.getQueryPatches()) {
            listsPanel.add(createProcessingPanel(patch));
        }

        final JButton addButton = new JButton("Process");
        addButton.setActionCommand("processButton");
        addButton.addActionListener(this);
        listsPanel.add(addButton);

        this.add(listsPanel, BorderLayout.SOUTH);
    }

    private JPanel createProcessingPanel(final Patch patch) {
        final JPanel panel = new JPanel();

        final JLabel imgLabel = new JLabel();
        imgLabel.setIcon(new ImageIcon(patch.getImage().getScaledInstance(50, 50, BufferedImage.SCALE_FAST)));
        panel.add(imgLabel);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel.add(progressBar);

        LabelBarProgressMonitor progMon = new LabelBarProgressMonitor(progressBar);
        progMon.addListener(new MyProgressBarListener());
        progressMap.put(patch, progMon);

        return panel;
    }

    private class MyProgressBarListener implements LabelBarProgressMonitor.ProgressBarListener {
        public void notifyStart() {

        }

        public void notifyDone() {

        }
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

                final Set<Patch> keys = progressMap.keySet();
                for(Patch patch : keys) {
                    ProcessThread thread = new ProcessThread(patch, progressMap.get(patch));
                    thread.execute();
                }

            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    public final class ProcessThread extends SwingWorker {

        private final Patch patch;
        private final ProgressMonitor pm;

        public ProcessThread(final Patch patch, final ProgressMonitor pm) {
            this.patch = patch;
            this.pm = pm;
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            pm.beginTask("Processing...", 100);
            try {
                final File tmpInFolder = new File(SystemUtils.getApplicationDataDir(),
                        "tmp"+File.separator+"in"+File.separator+patch.getID());
                final File tmpOutFolder = new File(SystemUtils.getApplicationDataDir(),
                        "tmp"+File.separator+"out"+File.separator+patch.getID());
                final File subsetFile = new File(tmpInFolder, patch.getPatchName()+".dim");
                final WriteOp writeOp = new WriteOp(patch.getPatchProduct(), subsetFile, "BEAM-DIMAP");
                writeOp.setDeleteOutputOnFailure(true);
                writeOp.setWriteEntireTileRows(true);
                writeOp.writeProduct(ProgressMonitor.NULL);

                final File graphFile = session.getApplicationDescriptor().getGraphFile();
                final Graph graph = GraphIO.read(new FileReader(graphFile), null);
                setIO(graph, subsetFile, tmpOutFolder);

                final GraphProcessor processor = new GraphProcessor();
                processor.executeGraph(graph, pm);

                //loadFeatures(tmpOutFolder);

            } catch(Throwable e) {
                System.out.println("processing Exception\n"+e.getMessage());
            } finally {
                pm.done();
            }
            return true;
        }

        private void loadFeatures(final File featureFile) throws Exception {
            Properties featureValues = new Properties();
            try (FileReader reader = new FileReader(featureFile)) {
                featureValues.load(reader);
            }
        }

        private void setIO(final Graph graph, final File srcFile, final File targetFolder) {
            final String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
            final Node readerNode = findNode(graph, readOperatorAlias);
            if(readerNode != null) {
                final DomElement param = new DefaultDomElement("parameters");
                param.createChild("file").setValue(srcFile.getAbsolutePath());
                readerNode.setConfiguration(param);
            }

            final Node writerNode = findNode(graph, "UrbanAreaFeatureWriter");
            if(writerNode != null) {
                final DomElement param = new DefaultDomElement("parameters");
                param.createChild("targetPath").setValue(targetFolder.getAbsolutePath());
                writerNode.setConfiguration(param);
            }
        }

        private Node findNode(final Graph graph, final String alias) {
            for(Node n : graph.getNodes()) {
                if(n.getOperatorName().equals(alias))
                    return n;
            }
            return null;
        }

        @Override
        public void done() {

        }
    }
}