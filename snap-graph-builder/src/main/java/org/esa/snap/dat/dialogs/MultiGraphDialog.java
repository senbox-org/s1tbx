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
package org.esa.snap.dat.dialogs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.snap.dat.graphbuilder.GraphExecuter;
import org.esa.snap.dat.graphbuilder.ProgressBarProgressMonitor;
import org.esa.snap.util.MemUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Provides the dialog for excuting multiple graph from one user interface
 */
public abstract class MultiGraphDialog extends ModelessDialog {

    protected final AppContext appContext;
    protected final IOPanel ioPanel;
    protected final List<GraphExecuter> graphExecuterList = new ArrayList<GraphExecuter>(3);

    private final JPanel mainPanel;
    protected final JTabbedPane tabbedPane;
    private final JLabel statusLabel;
    private final JPanel progressPanel;
    private final JProgressBar progressBar;
    private ProgressBarProgressMonitor progBarMonitor = null;

    private boolean isProcessing = false;

    protected static final String TMP_FILENAME = "tmp_intermediate";

    public MultiGraphDialog(final AppContext theAppContext, final String title, final String helpID,
                            final boolean useSourceSelector) {
        super(theAppContext.getApplicationWindow(), title, ID_APPLY_CLOSE_HELP, helpID);
        appContext = theAppContext;

        mainPanel = new JPanel(new BorderLayout(4, 4));

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                ValidateAllNodes();
            }
        });
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        ioPanel = new IOPanel(appContext, tabbedPane, useSourceSelector);

        // status
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        // progress Bar
        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout(2, 2));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                CancelProcessing();
            }
        });
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);
        progressPanel.setVisible(false);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        getButton(ID_APPLY).setText("Run");

        super.getJDialog().setMinimumSize(new Dimension(500, 300));
    }

    @Override
    public int show() {
        ioPanel.initProducts();
        setContent(mainPanel);
        initGraphs();
        return super.show();
    }

    @Override
    public void hide() {
        ioPanel.releaseProducts();
        super.hide();
    }

    @Override
    protected void onApply() {

        if (isProcessing) return;

        ioPanel.onApply();

        try {
            DoProcessing();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    @Override
    protected void onClose() {
        CancelProcessing();

        super.onClose();
    }

    void initGraphs() {
        try {
            deleteGraphs();
            createGraphs();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     *
     * @throws GraphException on assignParameters
     */
    private void DoProcessing() {

        if (ValidateAllNodes()) {

            MemUtils.freeAllMemory();

            progressBar.setValue(0);
            progBarMonitor = new ProgressBarProgressMonitor(progressBar, null, progressPanel);

            final SwingWorker processThread = new ProcessThread(progBarMonitor);
            processThread.execute();

        } else {
            showErrorDialog(statusLabel.getText());
        }
    }

    private void CancelProcessing() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
    }

    private void deleteGraphs() {
        for (GraphExecuter gex : graphExecuterList) {
            gex.ClearGraph();
        }
        graphExecuterList.clear();
    }

    /**
     * Loads a new graph from a file
     *
     * @param executer the GraphExcecuter
     * @param file     the graph file to load
     */
    public void LoadGraph(final GraphExecuter executer, final File file) {
        try {
            executer.loadGraph(file, true);

        } catch (Exception e) {
            showErrorDialog(e.getMessage());
        }
    }

    protected abstract void createGraphs() throws GraphException;

    protected abstract void assignParameters() throws GraphException;

    protected abstract void cleanUpTempFiles();

    private boolean ValidateAllNodes() {
        if (isProcessing) return false;
        if (ioPanel == null || graphExecuterList.isEmpty())
            return false;

        boolean result;
        statusLabel.setText("");
        try {
            // check the all files have been saved
            final Product srcProduct = ioPanel.getSelectedSourceProduct();
            if (srcProduct != null && (srcProduct.isModified() || srcProduct.getFileLocation() == null)) {
                throw new OperatorException("The source product has been modified. Please save it before using it in " + getTitle());
            }
            assignParameters();
            // first graph must pass
            result = graphExecuterList.get(0).InitGraph();

        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            result = false;
        }
        return result;
    }

    private void openTargetProducts(final List<File> fileList) {
        if (!fileList.isEmpty()) {
            for (File file : fileList) {
                try {

                    final Product product = ProductIO.readProduct(file);
                    if (product != null) {
                        appContext.getProductManager().addProduct(product);
                    }
                } catch (Exception e) {
                    showErrorDialog(e.getMessage());
                }
            }
        }
    }

    protected IOPanel getIOPanel() {
        return ioPanel;
    }

    public void setTargetProductNameSuffix(final String suffix) {
        ioPanel.setTargetProductNameSuffix(suffix);
    }

    /**
     * For running graphs in unit tests
     *
     * @throws Exception when failing validation
     */
    public void testRunGraph() throws Exception {
        ioPanel.initProducts();
        initGraphs();

        if (ValidateAllNodes()) {

            for (GraphExecuter graphEx : graphExecuterList) {
                final String desc = graphEx.getGraphDescription();
                if (desc != null && !desc.isEmpty())
                    System.out.println("Processing " + graphEx.getGraphDescription());

                graphEx.InitGraph();

                graphEx.executeGraph(ProgressMonitor.NULL);
                graphEx.disposeGraphContext();
            }

            cleanUpTempFiles();
        } else {
            throw new OperatorException(statusLabel.getText());
        }
    }

    /////

    private class ProcessThread extends SwingWorker<Boolean, Object> {

        private final ProgressMonitor pm;
        private Date executeStartTime = null;
        private boolean errorOccured = false;

        public ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            pm.beginTask("Processing Graph...", 100 * graphExecuterList.size());
            try {
                executeStartTime = Calendar.getInstance().getTime();
                isProcessing = true;

                for (GraphExecuter graphEx : graphExecuterList) {
                    final String desc = graphEx.getGraphDescription();
                    if (desc != null && !desc.isEmpty())
                        statusLabel.setText("Processing " + graphEx.getGraphDescription());

                    graphEx.InitGraph();

                    graphEx.executeGraph(new SubProgressMonitor(pm, 100));
                    graphEx.disposeGraphContext();
                }

            } catch (Exception e) {
                System.out.print(e.getMessage());
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.toString());
                errorOccured = true;
            } finally {
                isProcessing = false;
                pm.done();
            }
            return true;
        }

        @Override
        public void done() {
            if (!errorOccured) {
                final Date now = Calendar.getInstance().getTime();
                final long diff = (now.getTime() - executeStartTime.getTime()) / 1000;
                if (diff > 120) {
                    final float minutes = diff / 60f;
                    statusLabel.setText("Processing completed in " + minutes + " minutes");
                } else {
                    statusLabel.setText("Processing completed in " + diff + " seconds");
                }

                MemUtils.freeAllMemory();

                if (ioPanel.isOpenInAppSelected()) {
                    final GraphExecuter graphEx = graphExecuterList.get(graphExecuterList.size() - 1);
                    openTargetProducts(graphEx.getProductsToOpenInDAT());
                }
            }
            cleanUpTempFiles();
        }

    }

}