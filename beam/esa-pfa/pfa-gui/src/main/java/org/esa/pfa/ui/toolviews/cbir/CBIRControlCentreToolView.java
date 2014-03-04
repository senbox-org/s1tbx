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


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.PFAApplicationRegistry;
import org.esa.pfa.search.CBIRSession;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;


public class CBIRControlCentreToolView extends AbstractToolView implements CBIRSession.CBIRSessionListener {

    private final static Dimension preferredDimension = new Dimension(550, 300);
    private final static Font titleFont = new Font("Ariel", Font.BOLD, 14);
    private final static String title = "Content Based Image Retrieval";
    private final static String instructionsStr = "Select a feature extraction application";
    private final static String PROPERTY_KEY_DB_PATH = "app.file.cbir.dbPath";

    private JComboBox<String> applicationCombo;
    private JList<String> classifierList;
    private JButton newBtn, deleteBtn;
    private JButton queryBtn, trainBtn, applyBtn;
    private JTextField numTrainingImages;
    private JTextField numRetrievedImages;
    private JButton updateBtn;
    private JLabel iterationsLabel = new JLabel();

    private File dbFolder;
    private JTextField dbFolderTextField;

    private CBIRSession session = null;

    public CBIRControlCentreToolView() {
        CBIRSession.Instance().addListener(this);
    }

    public JComponent createControl() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        final PFAApplicationDescriptor[] apps = PFAApplicationRegistry.getInstance().getAllDescriptors();
        applicationCombo = new JComboBox<>();
        for (PFAApplicationDescriptor app : apps) {
            applicationCombo.addItem(app.getName());
        }
        applicationCombo.setEditable(false);
        applicationCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                session = null;
                updateControls();
            }
        });

        dbFolder = new File(VisatApp.getApp().getPreferences().getPropertyString(PROPERTY_KEY_DB_PATH, ""));
        dbFolderTextField = new JTextField();
        if (dbFolder.exists()) {
            dbFolderTextField.setText(dbFolder.getAbsolutePath());
        }

        contentPane.add(new Label("Application:"), gbc);
        gbc.gridy++;
        gbc.gridwidth = 3;
        contentPane.add(applicationCombo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        contentPane.add(new JLabel("Local database:"), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        contentPane.add(dbFolderTextField, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;

        final JButton fileChooserButton = new JButton(new FolderChooserAction("..."));
        contentPane.add(fileChooserButton, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy += 2;
        contentPane.add(new JLabel("Saved Classifiers:"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy++;

        final String[] savedClassifierNames = CBIRSession.getSavedClassifierNames(dbFolder.getAbsolutePath());
        final DefaultListModel modelList = new DefaultListModel<String>();
        for (String name : savedClassifierNames) {
            modelList.addElement(name);
        }

        classifierList = new JList<String>(modelList);
        classifierList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        classifierList.setLayoutOrientation(JList.VERTICAL);
        classifierList.setPrototypeCellValue("123456789012345678901234567890");
        classifierList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    if (e.getValueIsAdjusting() == false) {
                        createNewSession();
                        updateControls();
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        contentPane.add(new JScrollPane(classifierList), gbc);

        final JPanel optionsPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcOpt = GridBagUtils.createDefaultConstraints();
        gbcOpt.fill = GridBagConstraints.HORIZONTAL;
        gbcOpt.anchor = GridBagConstraints.NORTHWEST;
        gbcOpt.gridx = 0;
        gbcOpt.gridy = 0;

        optionsPane.add(new JLabel("# of training images:"), gbcOpt);
        gbcOpt.gridx = 1;
        gbcOpt.weightx = 0.8;
        numTrainingImages = new JTextField();
        numTrainingImages.setColumns(3);
        numTrainingImages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (session != null) {
                        session.setNumTrainingImages(Integer.parseInt(numTrainingImages.getText()));
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        optionsPane.add(numTrainingImages, gbcOpt);

        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        gbcOpt.weightx = 1;
        optionsPane.add(new JLabel("# of retrieved images:"), gbcOpt);
        gbcOpt.gridx = 1;
        gbcOpt.weightx = 1;
        numRetrievedImages = new JTextField();
        numRetrievedImages.setColumns(3);
        numRetrievedImages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (session != null) {
                        session.setNumRetrievedImages(Integer.parseInt(numRetrievedImages.getText()));
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        optionsPane.add(numRetrievedImages, gbcOpt);
        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        gbcOpt.weightx = 1;
        optionsPane.add(new JLabel("# of iterations:"), gbcOpt);
        gbcOpt.gridx = 1;
        gbcOpt.weightx = 1;
        optionsPane.add(iterationsLabel, gbcOpt);

        updateBtn = new JButton(new AbstractAction("Update") {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (session != null) {
                        session.setNumTrainingImages(Integer.parseInt(numTrainingImages.getText()));
                        session.setNumRetrievedImages(Integer.parseInt(numRetrievedImages.getText()));
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        gbcOpt.gridy++;
        gbcOpt.gridx = 1;
        optionsPane.add(updateBtn, gbcOpt);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(optionsPane, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(createClassifierButtonPanel(), gbc);

        final JPanel mainPane0 = new JPanel(new BorderLayout());
        mainPane0.add(contentPane, BorderLayout.CENTER);
        mainPane0.add(createSideButtonPanel(), BorderLayout.EAST);

        final JPanel mainPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mainPane.add(mainPane0);

        updateControls();

        return mainPane;
    }

    @Override
    public void componentShown() {

        final Window win = getPaneWindow();
        if (win != null) {
            win.setPreferredSize(preferredDimension);
            win.setMaximumSize(preferredDimension);
            win.setSize(preferredDimension);
        }
    }

    public static JLabel createTitleLabel(final String title) {
        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleFont);
        return titleLabel;
    }

    public static JPanel createTextPanel(final String title, final String text) {
        final JPanel textPanel = new JPanel(new BorderLayout(2, 2));
        if (title != null)
            textPanel.setBorder(BorderFactory.createTitledBorder(title));
        final JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPanel.add(textPane, BorderLayout.CENTER);
        return textPanel;
    }

    public static JPanel createInstructionsPanel(final String title, final String text) {
        final JPanel instructPanel = new JPanel(new BorderLayout(2, 2));
        instructPanel.add(createTitleLabel(title), BorderLayout.NORTH);
        instructPanel.add(createTextPanel(null, text), BorderLayout.CENTER);
        return instructPanel;
    }

    private JPanel createClassifierButtonPanel() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        newBtn = new JButton(new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) {
                try {
                    final PromptDialog dlg = new PromptDialog("New Classifier", "Name", "");
                    dlg.show();

                    final String value = dlg.getValue();
                    if (!value.isEmpty()) {
                        final DefaultListModel listModel = (DefaultListModel) classifierList.getModel();
                        listModel.addElement(value);
                        classifierList.setSelectedIndex(listModel.indexOf(value));
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        deleteBtn = new JButton(new AbstractAction("Delete") {
            public void actionPerformed(ActionEvent e) {
                try {
                    final boolean ret = session.deleteClassifier();
                    if (ret) {
                        final DefaultListModel listModel = (DefaultListModel) classifierList.getModel();
                        listModel.remove(classifierList.getSelectedIndex());
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });

        panel.add(newBtn);
        panel.add(deleteBtn);

        return panel;
    }

    private JPanel createSideButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(-1, 1, 2, 2));

        queryBtn = new JButton(new AbstractAction("Query") {
            public void actionPerformed(ActionEvent e) {
                try {
                    getContext().getPage().showToolView(CBIRQueryToolView.ID);
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        trainBtn = new JButton(new AbstractAction("Label") {
            public void actionPerformed(ActionEvent e) {
                try {
                    ProgressMonitorSwingWorker<Boolean, Void> worker = new ProgressMonitorSwingWorker<Boolean, Void>(getControl(), "Getting images to label") {
                        @Override
                        protected Boolean doInBackground(final ProgressMonitor pm) throws Exception {
                            pm.beginTask("Getting images...", 100);
                            try {
                                session.populateArchivePatches(SubProgressMonitor.create(pm, 50));
                                session.getImagesToLabel(SubProgressMonitor.create(pm, 50));
                                if (!pm.isCanceled()) {
                                    return Boolean.TRUE;
                                }
                            } finally {
                                pm.done();
                            }
                            return Boolean.FALSE;
                        }
                    };
                    worker.executeWithBlocking();
                    if (worker.get()) {
                        getContext().getPage().showToolView(CBIRLabelingToolView.ID);
                    }
                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });
        applyBtn = new JButton(new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent e) {
                try {
                    ProgressMonitorSwingWorker<Boolean, Void> worker = new ProgressMonitorSwingWorker<Boolean, Void>(getControl(), "Retrieving") {
                        @Override
                        protected Boolean doInBackground(final ProgressMonitor pm) throws Exception {
                            pm.beginTask("Retrieving images...", 100);
                            try {
                                session.populateArchivePatches(SubProgressMonitor.create(pm, 50));  // not needed to train model but needed for next iteration
                                session.trainModel(SubProgressMonitor.create(pm, 50));
                                if (!pm.isCanceled()) {
                                    return Boolean.TRUE;
                                }
                            } finally {
                                pm.done();
                            }
                            return Boolean.FALSE;
                        }
                    };
                    worker.executeWithBlocking();
                    if (worker.get()) {
                        getContext().getPage().showToolView(CBIRRetrievedImagesToolView.ID);
                    }

                } catch (Throwable t) {
                    VisatApp.getApp().handleUnknownException(t);
                }
            }
        });

        panel.add(queryBtn);
        panel.add(trainBtn);
        panel.add(applyBtn);


        final JPanel panel2 = new JPanel(new BorderLayout(2, 2));
        panel2.add(panel, BorderLayout.NORTH);
        panel2.add(new JLabel(new ImageIcon(getClass().getResource("/images/pfa-logo-small.png"))), BorderLayout.SOUTH);
        return panel2;
    }

    private void createNewSession() throws Exception {
        final String name = classifierList.getSelectedValue();
        if (name != null && session == null || (session.getName() == null || !session.getName().equals(name))) {

            ProgressMonitorSwingWorker<Boolean, Void> worker = new ProgressMonitorSwingWorker<Boolean, Void>(getControl(), "Loading") {
                @Override
                protected Boolean doInBackground(final ProgressMonitor pm) throws Exception {
                    pm.beginTask("Creating session...", 100);
                    try {
                        createNewSession(name, pm);
                        if (!pm.isCanceled()) {
                            return Boolean.TRUE;
                        }
                    } finally {
                        pm.done();
                    }
                    return Boolean.FALSE;
                }
            };
            worker.executeWithBlocking();
        }
    }

    private void updateControls() {
        newBtn.setEnabled(dbFolder.exists());

        final String name = classifierList.getSelectedValue();
        final boolean activeSession = name != null && session != null;
        deleteBtn.setEnabled(activeSession);

        applicationCombo.setEnabled(activeSession);
        numTrainingImages.setEnabled(activeSession);
        numRetrievedImages.setEnabled(activeSession);
        updateBtn.setEnabled(activeSession);

        queryBtn.setEnabled(activeSession);
        trainBtn.setEnabled(activeSession);
        applyBtn.setEnabled(activeSession);

        if (session != null && session.isInit()) {
            final int numIterations = session.getNumIterations();
            numTrainingImages.setText(String.valueOf(session.getNumTrainingImages()));
            numRetrievedImages.setText(String.valueOf(session.getNumRetrievedImages()));
            iterationsLabel.setText(String.valueOf(numIterations));

            trainBtn.setEnabled(numIterations > 0);
            applyBtn.setEnabled(numIterations > 0);
        }
    }

    private void createNewSession(final String classifierName, final ProgressMonitor pm) throws Exception {
        final String application = (String) applicationCombo.getSelectedItem();
        final PFAApplicationDescriptor applicationDescriptor = PFAApplicationRegistry.getInstance().getDescriptor(application);

        final String dbPath = dbFolderTextField.getText();
        session = CBIRSession.Instance();

        session.initSession(classifierName, applicationDescriptor, dbPath, pm);
    }

    private class FolderChooserAction extends AbstractAction {

        private String APPROVE_BUTTON_TEXT = "Select";
        private JFileChooser chooser;

        private FolderChooserAction(final String text) {
            super(text);
            chooser = new FolderChooser();
            chooser.setDialogTitle("Find database folder");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final Window window = SwingUtilities.getWindowAncestor((JComponent) event.getSource());
            if (dbFolder.exists()) {
                chooser.setCurrentDirectory(dbFolder.getParentFile());
            }
            if (chooser.showDialog(window, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                dbFolder = chooser.getSelectedFile();
                dbFolderTextField.setText(dbFolder.getAbsolutePath());
                VisatApp.getApp().getPreferences().setPropertyString(PROPERTY_KEY_DB_PATH, dbFolder.getAbsolutePath());
                final String[] savedClassifierNames = CBIRSession.getSavedClassifierNames(dbFolder.getAbsolutePath());

                DefaultListModel<String> modelList = (DefaultListModel<String>) classifierList.getModel();
                modelList.clear();
                for (String name : savedClassifierNames) {
                    modelList.addElement(name);
                }
                updateControls();
            }
        }
    }

    private static class PromptDialog extends ModalDialog {

        private JTextArea textArea;

        public PromptDialog(String title, String labelStr, String defaultValue) {
            super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK, null);

            final JPanel content = new JPanel();
            final JLabel label = new JLabel(labelStr);
            textArea = new JTextArea(defaultValue);
            textArea.setColumns(50);

            content.add(label);
            content.add(textArea);

            setContent(content);
        }

        public String getValue() {
            return textArea.getText();
        }

        protected void onOK() {
            hide();
        }
    }

    public void notifyNewSession() {
    }

    public void notifyNewTrainingImages() {
    }

    public void notifyModelTrained() {
        updateControls();
    }

}