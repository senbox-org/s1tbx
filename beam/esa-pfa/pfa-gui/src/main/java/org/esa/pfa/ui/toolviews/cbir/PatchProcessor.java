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
package org.esa.pfa.ui.toolviews.cbir;

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
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.taskpanels.LabelingTaskPanel;

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
 * Feature extraction
 */
public class PatchProcessor {

    private final CBIRSession session;

    public PatchProcessor(final CBIRSession session) {
        this.session = session;
    }

    public boolean validateInput() {
        try {
      /*      final Patch[] processedPatches = session.getQueryPatches();

            session.clearQueryPatches();
            for (Patch patch : processedPatches) {
                if (patch.getFeatures().length > 0) {
                    session.addQueryPatch(patch);
                }
            }
            if (session.getQueryPatches().length == 0) {
                throw new Exception("No features found in the query images");
            }

            session.setQueryImages();
                                     */
            return true;
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
        return false;
    }

    public void process(final Patch patch) {
        try {
            FeaturePanel panel = new FeaturePanel(patch);
            final ProcessThread thread = new ProcessThread(panel);
            thread.execute();
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
    }

    private final class ProcessThread extends SwingWorker {

        private final FeaturePanel patchData;
        private File tmpOutFolder;

        public ProcessThread(final FeaturePanel patchData) {
            this.patchData = patchData;
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            patchData.pm.beginTask("Processing...", 100);
            try {
                final Patch patch = patchData.patch;
                final File tmpInFolder = new File(SystemUtils.getApplicationDataDir(),
                        "tmp"+File.separator+"in"+File.separator+patch.getPatchProduct().getName()+".fex");
                tmpOutFolder = new File(SystemUtils.getApplicationDataDir(),
                        "tmp"+File.separator+"out"+File.separator+patch.getPatchProduct().getName()+".fex");
                final File subsetFile = new File(tmpInFolder, patch.getPatchName() + ".dim");
                final WriteOp writeOp = new WriteOp(patch.getPatchProduct(), subsetFile, "BEAM-DIMAP");
                writeOp.setDeleteOutputOnFailure(true);
                writeOp.setWriteEntireTileRows(true);
                writeOp.writeProduct(ProgressMonitor.NULL);

                final File graphFile = session.getApplicationDescriptor().getGraphFile();
                final Graph graph = GraphIO.read(new FileReader(graphFile), null);
                setIO(graph, subsetFile, tmpOutFolder);

                final GraphProcessor processor = new GraphProcessor();
                processor.executeGraph(graph, patchData.pm);

                loadFeatures(patchData.patch, tmpOutFolder);

            } catch (Throwable e) {
                final String msg = "processing Exception\n" + e.getMessage();
                System.out.println(msg);
                VisatApp.getApp().showErrorDialog(e.toString());
            } finally {
                patchData.pm.done();
            }
            return true;
        }

        private void loadFeatures(final Patch patch, final File datasetDir) {
            try {
                final File[] fexDirs = datasetDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() && file.getName().endsWith(".fex");
                    }
                });
                if (fexDirs.length == 0)
                    return;

                final File[] patchDirs = fexDirs[0].listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() && file.getName().startsWith("x");
                    }
                });
                if (patchDirs.length == 0)
                    return;

                patch.setPathOnServer(patchDirs[0].getAbsolutePath());

                final File featureFile = new File(patchDirs[0], "features.txt");
                if (featureFile.exists()) {
                    final Properties featureValues = new Properties();
                    try (FileReader reader = new FileReader(featureFile)) {
                        featureValues.load(reader);
                    }

                    patch.clearFeatures();

                    for (FeatureType featureType : session.getEffectiveFeatureTypes()) {
                        final String featureValue = featureValues.getProperty(featureType.getName());
                        if (featureValue != null) {
                            patch.addFeature(createFeature(featureType, featureValue));
                        }
                    }
                }
            } catch (Exception e) {
                final String msg = "Error reading features " + patch.getPatchName() + "\n" + e.getMessage();
                VisatApp.getApp().showErrorDialog(msg);
            }
        }

        private Feature createFeature(FeatureType feaType, String value) {
            final Class<?> valueType = feaType.getValueType();
            if(value.equals("NaN")) {
                value = "0";
            }

            if (Double.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Double.parseDouble(value));
            } else if (Float.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Float.parseFloat(value));
            } else if (Integer.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Integer.parseInt(value));
            } else if (Boolean.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, Boolean.parseBoolean(value));
            } else if (Character.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, value);
            } else if (String.class.isAssignableFrom(valueType)) {
                return new Feature(feaType, value);
            }
            return null;
        }

        private void setIO(final Graph graph, final File srcFile, final File targetFolder) {
            final String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
            final Node readerNode = findNode(graph, readOperatorAlias);
            if (readerNode != null) {
                final DomElement param = new DefaultDomElement("parameters");
                param.createChild("file").setValue(srcFile.getAbsolutePath());
                readerNode.setConfiguration(param);
            }

            Node[] nodes = graph.getNodes();
            if (nodes.length > 0) {
                Node lastNode = nodes[nodes.length - 1];
                DomElement configuration = lastNode.getConfiguration();
                configuration.getChild("targetPath").setValue(targetFolder.getAbsolutePath());
            }

        }

        private Node findNode(final Graph graph, final String alias) {
            for (Node n : graph.getNodes()) {
                if (n.getOperatorName().equals(alias))
                    return n;
            }
            return null;
        }

        @Override
        public void done() {

            //clean up
            //FileUtils.deleteDirectory(tmpInFolder);
            //FileUtils.deleteDirectory(tmpOutFolder);
        }
    }

    private static class FeaturePanel extends JPanel implements LabelBarProgressMonitor.ProgressBarListener {
        private final Patch patch;
        private final JProgressBar progressBar;
        private final LabelBarProgressMonitor pm;
        private final JTextArea textPane;
        private final JScrollPane textScroll;

        FeaturePanel(final Patch patch) {
            this.patch = patch;

            progressBar = new JProgressBar();
            progressBar.setStringPainted(true);
            add(progressBar);

            textPane = new JTextArea();
            textScroll = new JScrollPane(textPane);
            textScroll.setVisible(false);
            add(textScroll);

            pm = new LabelBarProgressMonitor(progressBar);
            pm.addListener(this);
        }

        public void notifyStart() {
            progressBar.setVisible(true);
            textScroll.setVisible(false);
        }

        public void notifyDone() {
            progressBar.setVisible(false);
            textScroll.setVisible(true);
            textPane.setText(patch.writeFeatures());
        }
    }

}