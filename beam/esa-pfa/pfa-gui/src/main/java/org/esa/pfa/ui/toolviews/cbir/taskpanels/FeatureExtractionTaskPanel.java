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
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.db.DatasetDescriptor;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
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
import java.io.FileFilter;
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
        try {
            final Patch[] processedPatches = session.getQueryPatches();

            session.clearQueryPatches();
            for(Patch patch : processedPatches) {
                if(patch.getFeatures().length > 0) {
                    session.addQueryPatch(patch);
                }
            }
            if(session.getQueryPatches().length == 0) {
                throw new Exception("No features found in the query images");
            }

            session.setQueryImages();

            return true;
        } catch(Exception e)  {
            showErrorMsg(e.getMessage());
        }
        return false;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel listsPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
        listsPanel.setLayout(layout);

        for(Patch patch : session.getQueryPatches()) {
            listsPanel.add(createProcessingPanel(patch));
        }
        this.add(new JScrollPane(listsPanel), BorderLayout.CENTER);

        final JButton addButton = new JButton("Process Query Images");
        addButton.setActionCommand("processButton");
        addButton.addActionListener(this);
        this.add(addButton, BorderLayout.SOUTH);
    }

    private JPanel createProcessingPanel(final Patch patch) {
        final JPanel panel = new JPanel();

        final JLabel imgLabel = new JLabel();
        imgLabel.setIcon(new ImageIcon(patch.getImage().getScaledInstance(100, 100, BufferedImage.SCALE_FAST)));
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

                loadFeatures(patch, tmpOutFolder);

                //clean up
                writeOp.dispose();
                //FileUtils.deleteDirectory(tmpInFolder);
                //FileUtils.deleteDirectory(tmpOutFolder);

            } catch(Throwable e) {
                System.out.println("processing Exception\n"+e.getMessage());
            } finally {
                pm.done();
            }
            return true;
        }

        private void loadFeatures(final Patch patch, final File datasetDir) throws Exception {
            final File[] fexDirs = datasetDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() && file.getName().endsWith(".fex");
                }
            });
            final File[] patchDirs = fexDirs[0].listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() && file.getName().startsWith("x");
                }
            });

            final File featureFile = new File(patchDirs[0], "features.txt");
            if(featureFile.exists()) {
                final Properties featureValues = new Properties();
                try (FileReader reader = new FileReader(featureFile)) {
                    featureValues.load(reader);
                }

                final DatasetDescriptor dsDescriptor = session.getDsDescriptor();
                for(FeatureType feaType : dsDescriptor.getFeatureTypes()) {
                    if(feaType.hasAttributes()) {
                        for(AttributeType attrib : feaType.getAttributeTypes()) {
                            final String name = feaType.getName()+'.'+attrib.getName();
                            final String value = featureValues.getProperty(name);
                            if(value != null) {
                                FeatureType newFeaType = new FeatureType(name, attrib.getDescription(), attrib.getValueType());
                                patch.addFeature(createFeature(newFeaType, value));
                            }
                        }
                    } else {
                        final String value = featureValues.getProperty(feaType.getName());
                        if(value != null) {
                            patch.addFeature(createFeature(feaType, value));
                        }
                    }
                }
            }
        }

        private Feature createFeature(FeatureType feaType, final String value) {
            final Class<?> valueType = feaType.getValueType();

            if(Double.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Double.parseDouble(value));
            } else if(Float.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Float.parseFloat(value));
            } else if(Integer.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Integer.parseInt(value));
            } else if(Boolean.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Boolean.parseBoolean(value));
            } else if(Character.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, value);
            } else if(String.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, value);
            }
            return null;
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