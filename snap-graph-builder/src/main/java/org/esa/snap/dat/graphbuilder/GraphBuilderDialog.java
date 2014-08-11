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
package org.esa.snap.dat.graphbuilder;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.esa.snap.dat.dialogs.PromptDialog;
import org.esa.snap.gpf.ProductSetReaderOp;
import org.esa.snap.gpf.ProductSetReaderOpUI;
import org.esa.snap.gpf.ui.SourceUI;
import org.esa.snap.gpf.ui.UIValidation;
import org.esa.snap.util.DialogUtils;
import org.esa.snap.util.MemUtils;
import org.esa.snap.util.ResourceUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Provides the User Interface for creating, loading and saving Graphs
 */
public class GraphBuilderDialog extends ModelessDialog implements Observer {

    private static final ImageIcon processIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/cog.png");
    private static final ImageIcon saveIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/save.png");
    private static final ImageIcon loadIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/open.png");
    private static final ImageIcon clearIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/edit-clear.png");
    private static final ImageIcon helpIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/help-browser.png");
    private static final ImageIcon infoIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/info22.png");

    private final AppContext appContext;
    private GraphPanel graphPanel = null;
    private JLabel statusLabel = null;
    private String lastWarningMsg = "";

    private JPanel progressPanel = null;
    private JProgressBar progressBar = null;
    private ProgressBarProgressMonitor progBarMonitor = null;
    private JLabel progressMsgLabel = null;
    private boolean initGraphEnabled = true;

    private final GraphExecuter graphEx;
    private boolean isProcessing = false;
    private boolean allowGraphBuilding = true;
    private final List<ProcessingListener> listenerList = new ArrayList<>(1);

    private final static String LAST_GRAPH_PATH = "graphbuilder.last_graph_path";

    //TabbedPanel
    private JTabbedPane tabbedPanel = null;

    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID) {
        this(theAppContext, title, helpID, true);
    }

    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID, final boolean allowGraphBuilding) {
        super(theAppContext.getApplicationWindow(), title, 0, helpID);

        this.allowGraphBuilding = allowGraphBuilding;
        appContext = theAppContext;
        graphEx = new GraphExecuter();
        graphEx.addObserver(this);

        String lastDir = VisatApp.getApp().getPreferences().getPropertyString(LAST_GRAPH_PATH,
                ResourceUtils.getGraphFolder("").getAbsolutePath());
        if (new File(lastDir).exists()) {
            VisatApp.getApp().getPreferences().setPropertyString(LAST_GRAPH_PATH, lastDir);
        }

        initUI();
    }

    /**
     * Initializes the dialog components
     */
    private void initUI() {
        if (this.allowGraphBuilding) {
            super.getJDialog().setMinimumSize(new Dimension(600, 750));
        } else {
            super.getJDialog().setMinimumSize(new Dimension(600, 500));
        }
        super.getJDialog().setIconImage(ResourceUtils.esaPlanetIcon.getImage());

        final JPanel mainPanel = new JPanel(new BorderLayout(4, 4));

        // north panel
        final JPanel northPanel = new JPanel(new BorderLayout(4, 4));

        if (allowGraphBuilding) {
            graphPanel = new GraphPanel(graphEx);
            graphPanel.setBackground(Color.WHITE);
            graphPanel.setPreferredSize(new Dimension(500, 500));
            final JScrollPane scrollPane = new JScrollPane(graphPanel);
            scrollPane.setPreferredSize(new Dimension(300, 300));
            northPanel.add(scrollPane, BorderLayout.CENTER);

            mainPanel.add(northPanel, BorderLayout.NORTH);
        }

        // mid panel
        final JPanel midPanel = new JPanel(new BorderLayout(4, 4));
        tabbedPanel = new JTabbedPane();
        //tabbedPanel.setTabPlacement(JTabbedPane.LEFT);
        tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPanel.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                ValidateAllNodes();
            }
        });

        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));

        midPanel.add(tabbedPanel, BorderLayout.CENTER);
        midPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(midPanel, BorderLayout.CENTER);

        // south panel
        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel buttonPanel = new JPanel();
        initButtonPanel(buttonPanel);
        southPanel.add(buttonPanel, BorderLayout.CENTER);

        // progress Bar
        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout(2, 2));
        progressMsgLabel = new JLabel();
        progressPanel.add(progressMsgLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                CancelProcessing();
            }
        });
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);

        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        setContent(mainPanel);
    }

    private void initButtonPanel(final JPanel panel) {
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        final JButton processButton = DialogUtils.CreateButton("processButton", "Process", processIcon, panel);
        processButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                DoProcessing();
            }
        });

        final JButton saveButton = DialogUtils.CreateButton("saveButton", "Save", saveIcon, panel);
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                SaveGraph();
            }
        });

        final JButton loadButton = DialogUtils.CreateButton("loadButton", "Load", loadIcon, panel);
        loadButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                LoadGraph();
            }
        });

        final JButton clearButton = DialogUtils.CreateButton("clearButton", "Clear", clearIcon, panel);
        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                ClearGraph();
            }
        });

        final JButton infoButton = DialogUtils.CreateButton("infoButton", "Note", infoIcon, panel);
        infoButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                OnInfo();
            }
        });
        //getClass().getName() + name
        final JButton helpButton = DialogUtils.CreateButton("helpButton", "Help", helpIcon, panel);
        helpButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                OnHelp();
            }
        });

        gbc.weightx = 0;
        if (allowGraphBuilding) {
            panel.add(loadButton, gbc);
            panel.add(saveButton, gbc);
            panel.add(clearButton, gbc);
            panel.add(infoButton, gbc);
        }
        panel.add(helpButton, gbc);
        panel.add(processButton, gbc);
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     */
    public void DoProcessing() {

        if (ValidateAllNodes()) {

            MemUtils.freeAllMemory();

            progressBar.setValue(0);
            progBarMonitor = new ProgressBarProgressMonitor(progressBar, progressMsgLabel, progressPanel);
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

    private boolean InitGraph() {
        boolean result = true;
        try {
            if (initGraphEnabled) {
                result = graphEx.InitGraph();
            }
            if (!result)
                statusLabel.setText("Graph is incomplete");
        } catch (Exception e) {
            if (e.getMessage() != null)
                statusLabel.setText(e.getMessage());
            else
                statusLabel.setText(e.toString());
            result = false;
        }
        return result;
    }

    /**
     * Validates the input and then saves the current graph to a file
     */
    private void SaveGraph() {

        //if(ValidateAllNodes()) {
        try {
            final File file = graphEx.saveGraph();

            this.setTitle("Graph Builder : " + file.getName());
        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
        //} else {
        //    showErrorDialog(statusLabel.getText());
        //}
    }

    /**
     * Loads a new graph from a file
     */
    private void LoadGraph() {
        final BeamFileFilter fileFilter = new BeamFileFilter("XML", "xml", "Graph");
        final File file = VisatApp.getApp().showFileOpenDialog("Load Graph", false, fileFilter, LAST_GRAPH_PATH);
        if (file == null) return;

        LoadGraph(file);

        if (allowGraphBuilding)
            this.setTitle("Graph Builder : " + file.getName());
    }

    /**
     * Loads a new graph from a file
     *
     * @param file the graph file to load
     */
    public void LoadGraph(final File file) {
        try {
            initGraphEnabled = false;
            tabbedPanel.removeAll();
            graphEx.loadGraph(file, true);
            if (allowGraphBuilding) {
                graphPanel.showRightClickHelp(false);
                graphPanel.repaint();
            }
            initGraphEnabled = true;
        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
    }

    public void EnableInitialInstructions(final boolean flag) {
        if (this.allowGraphBuilding) {
            graphPanel.showRightClickHelp(flag);
        }
    }

    /**
     * Removes all tabs and clears the graph
     */
    private void ClearGraph() {

        initGraphEnabled = false;
        tabbedPanel.removeAll();
        graphEx.ClearGraph();
        graphPanel.repaint();
        initGraphEnabled = true;
        statusLabel.setText("");
    }

    /**
     * pass in a file list for a ProductSetReader
     *
     * @param productFileList the product files
     */
    public void setInputFiles(final File[] productFileList) {
        final GraphNode productSetNode = graphEx.getGraphNodeList().findGraphNodeByOperator(
                ProductSetReaderOp.Spi.getOperatorAlias(ProductSetReaderOp.class));
        if (productSetNode != null) {
            ProductSetReaderOpUI ui = (ProductSetReaderOpUI) productSetNode.GetOperatorUI();
            ui.setProductFileList(productFileList);
        }
    }

    /**
     * pass in a file list for a ProductSetReader
     *
     * @param product the product files
     */
    public void setInputFile(final Product product) {
        final GraphNode readerNode = graphEx.getGraphNodeList().findGraphNodeByOperator(
                ReadOp.Spi.getOperatorAlias(ReadOp.class));
        if (readerNode != null) {
            SourceUI ui = (SourceUI) readerNode.GetOperatorUI();
            ui.setSourceProduct(product);

            ValidateAllNodes();
        }
    }

    /**
     * Call Help
     */
    private void OnHelp() {
        HelpSys.showTheme(super.getHelpID());
    }

    /**
     * Call decription dialog
     */
    private void OnInfo() {
        final PromptDialog dlg = new PromptDialog("Graph Description", "Description", graphEx.getGraphDescription(), true);
        dlg.show();
        if (dlg.IsOK()) {
            graphEx.setGraphDescription(dlg.getValue());
        }
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    /**
     * lets all operatorUIs validate their parameters
     * If parameter validation fails then a list of the failures is presented to the user
     *
     * @return true if validation passes
     */
    boolean ValidateAllNodes() {

        if (isProcessing) return false;

        boolean isValid = true;
        final StringBuilder errorMsg = new StringBuilder(100);
        final StringBuilder warningMsg = new StringBuilder(100);
        for (GraphNode n : graphEx.GetGraphNodes()) {
            try {
                final UIValidation validation = n.validateParameterMap();
                if (validation.getState() == UIValidation.State.ERROR) {
                    isValid = false;
                    errorMsg.append(validation.getMsg()).append('\n');
                } else if (validation.getState() == UIValidation.State.WARNING) {
                    warningMsg.append(validation.getMsg()).append('\n');
                }
            } catch (Exception e) {
                isValid = false;
                errorMsg.append(e.getMessage()).append('\n');
            }
        }

        statusLabel.setForeground(new Color(255, 0, 0));
        statusLabel.setText("");
        final String warningStr = warningMsg.toString();
        if (!isValid) {
            statusLabel.setText(errorMsg.toString());
            return false;
        } else if (!warningStr.isEmpty()) {
            if (warningStr.length() > 100 && !warningStr.equals(lastWarningMsg)) {
                VisatApp.getApp().showWarningDialog(warningStr);
                lastWarningMsg = warningStr;
            } else {
                statusLabel.setForeground(new Color(0, 100, 255));
                statusLabel.setText("Warning: " + warningStr);
            }
        }

        return InitGraph();
    }

    public void addListener(final ProcessingListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final ProcessingListener listener) {
        listenerList.remove(listener);
    }

    private void notifyMSG(final ProcessingListener.MSG msg, final String text) {
        for (final ProcessingListener listener : listenerList) {
            listener.notifyMSG(msg, text);
        }
    }

    private void notifyMSG(final ProcessingListener.MSG msg, final File[] fileList) {
        for (final ProcessingListener listener : listenerList) {
            listener.notifyMSG(msg, fileList);
        }
    }

    /**
     * Implements the functionality of Observer participant of Observer Design Pattern to define a one-to-many
     * dependency between a Subject object and any number of Observer objects so that when the
     * Subject object changes state, all its Observer objects are notified and updated automatically.
     * <p>
     * Defines an updating interface for objects that should be notified of changes in a subject.
     *
     * @param subject The Observerable subject
     * @param data    optional data
     */
    public void update(Observable subject, Object data) {

        try {
            final GraphExecuter.GraphEvent event = (GraphExecuter.GraphEvent) data;
            final GraphNode node = (GraphNode) event.getData();
            final String opID = node.getID();
            if (event.getEventType() == GraphExecuter.events.ADD_EVENT) {

                tabbedPanel.addTab(opID, null, CreateOperatorTab(node), opID + " Operator");
            } else if (event.getEventType() == GraphExecuter.events.REMOVE_EVENT) {

                int index = tabbedPanel.indexOfTab(opID);
                tabbedPanel.remove(index);
            } else if (event.getEventType() == GraphExecuter.events.SELECT_EVENT) {

                int index = tabbedPanel.indexOfTab(opID);
                tabbedPanel.setSelectedIndex(index);
            }
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private JComponent CreateOperatorTab(final GraphNode node) {

        return node.GetOperatorUI().CreateOpTab(node.getOperatorName(), node.getParameterMap(), appContext);
    }

    private class ProcessThread extends SwingWorker<GraphExecuter, Object> {

        private final ProgressMonitor pm;
        private Date executeStartTime = null;
        private boolean errorOccured = false;

        public ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected GraphExecuter doInBackground() throws Exception {

            pm.beginTask("Processing Graph...", 10);
            try {
                executeStartTime = Calendar.getInstance().getTime();
                isProcessing = true;
                graphEx.executeGraph(pm);

            } catch (Throwable e) {
                System.out.print(e.getMessage());
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.getCause().toString());
                errorOccured = true;
            } finally {
                isProcessing = false;
                graphEx.disposeGraphContext();
                // free cache
                MemUtils.freeAllMemory();

                pm.done();
            }
            return graphEx;
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
                final List<File> fileList = graphEx.getProductsToOpenInDAT();
                final File[] files = fileList.toArray(new File[fileList.size()]);
                notifyMSG(ProcessingListener.MSG.DONE, files);

                openTargetProducts(files);
            }
        }

    }

    private void openTargetProducts(final File[] fileList) {
        if (fileList.length != 0) {
            for (File file : fileList) {
                try {

                    final Product product = ProductIO.readProduct(file);
                    if (product != null) {
                        appContext.getProductManager().addProduct(product);
                    }
                } catch (IOException e) {
                    showErrorDialog(e.getMessage());
                }
            }
        }
    }

    public static File getInternalGraphFolder() {
        return ResourceUtils.getGraphFolder("internal");
    }

    public static File getStandardGraphFolder() {
        return ResourceUtils.getGraphFolder("Standard Graphs");
    }

    public interface ProcessingListener {

        public enum MSG {DONE, UPDATE}

        public void notifyMSG(final MSG msg, final File[] fileList);

        public void notifyMSG(final MSG msg, final String text);
    }
}
