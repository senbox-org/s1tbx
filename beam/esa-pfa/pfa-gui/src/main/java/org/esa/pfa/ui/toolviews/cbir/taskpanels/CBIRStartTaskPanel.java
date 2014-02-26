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

import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.PFAApplicationRegistry;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

/**
    Start Panel
 */
public class CBIRStartTaskPanel extends TaskPanel {

    private final static String instructionsStr = "Select a feature extraction application";
    private final static String PROPERTY_KEY_DB_PATH = "app.file.cbir.dbPath";

    private JComboBox<String> applicationCombo = new JComboBox<>();

    private JList<String> classifierList;
    private JButton newBtn;
    private JTextField numTrainingImages = new JTextField();
    private JTextField numRetrievedImages = new JTextField();
    private JLabel iterationsLabel = new JLabel();

    private File dbFolder;
    private final JTextField dbFolderTextField = new JTextField();

    private CBIRSession session = null;

    public CBIRStartTaskPanel() {
        super("Content Based Image Retrieval");

        final PFAApplicationDescriptor[] apps = PFAApplicationRegistry.getInstance().getAllDescriptors();
        for(PFAApplicationDescriptor app : apps) {
            applicationCombo.addItem(app.getName());
        }

        applicationCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String fea = (String) applicationCombo.getSelectedItem();

            }
        });

        dbFolder = new File(VisatApp.getApp().getPreferences().getPropertyString(PROPERTY_KEY_DB_PATH, ""));
        if(dbFolder.exists()) {
            dbFolderTextField.setText(dbFolder.getAbsolutePath());
        }

        createPanel();

        if (apps.length == 0) {
            Component[] components = getComponents();
            for (Component component : components) {
                component.setEnabled(false);
            }
        }
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean canProceedToNextPanel() {
        return session != null;
    }

    public boolean hasNextPanel() {
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public TaskPanel getNextPanel() {
        return new QueryTaskPanel(session);
    }

    public boolean validateInput() {
        try {
            if(session == null) {
                throw new Exception("Select or create a new classifier");
            }

            String dbPath = dbFolderTextField.getText();
            dbFolder = new File(dbPath);
            if(dbPath.isEmpty() || !dbFolder.exists()) {
                throw new Exception("Database path is invalid");
            }

            final int numTrainingImg = Integer.parseInt(numTrainingImages.getText());
            final int numRetrievedImg = Integer.parseInt(numRetrievedImages.getText());
            session.setNumTrainingImages(numTrainingImg);
            session.setNumRetrievedImages(numRetrievedImg);

            VisatApp.getApp().getPreferences().setPropertyString(PROPERTY_KEY_DB_PATH, dbFolder.getAbsolutePath());

            return true;
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
        return false;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

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
        gbc.gridy+=2;
        contentPane.add(new JLabel("Saved Classifiers:"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy++;

        final String[] savedClassifierNames = CBIRSession.getSavedClassifierNames(dbFolder.getAbsolutePath());
        final DefaultListModel modelList = new DefaultListModel<String>();
        for(String name : savedClassifierNames) {
            modelList.addElement(name);
        }

        classifierList = new JList<String>(modelList);
        classifierList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        classifierList.setLayoutOrientation(JList.VERTICAL);
        classifierList.setPrototypeCellValue("123456789012345678901234567890");
        classifierList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    updateControls();
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
        numTrainingImages.setColumns(3);
        optionsPane.add(numTrainingImages, gbcOpt);

        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        optionsPane.add(new JLabel("# of retrieved images:"), gbcOpt);
        gbcOpt.gridx = 1;
        numRetrievedImages.setColumns(3);
        optionsPane.add(numRetrievedImages, gbcOpt);
        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        optionsPane.add(new JLabel("# of iterations:"), gbcOpt);
        gbcOpt.gridx = 1;
        optionsPane.add(iterationsLabel, gbcOpt);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(optionsPane, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(createClassifierButtonPanel(), gbc);

        this.add(contentPane, BorderLayout.CENTER);
        this.add(createSideButtonPanel(), BorderLayout.EAST);

        updateControls();
    }

    private JPanel createClassifierButtonPanel() {
        final JPanel panel = new JPanel();

        newBtn = new JButton(new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) {
                final PromptDialog dlg = new PromptDialog("New Classifier", "Name", "");
                dlg.show();

                final DefaultListModel listModel = (DefaultListModel)classifierList.getModel();
                listModel.addElement(dlg.getValue());
                classifierList.setSelectedIndex(listModel.indexOf(dlg.getValue()));
            }
        });
     /*   final JButton loadBtn = new JButton(new AbstractAction("Load") {
            public void actionPerformed(ActionEvent e) {

            }
        });
        final JButton saveBtn = new JButton(new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {

            }
        });
        final JButton editBtn = new JButton(new AbstractAction("Edit") {
            public void actionPerformed(ActionEvent e) {

            }
        });   */

        panel.add(newBtn);
        //panel.add(loadBtn);
        //panel.add(saveBtn);
        //panel.add(editBtn);

        return panel;
    }

    private JPanel createSideButtonPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        final JButton btn1 = new JButton(new AbstractAction("1") {
            public void actionPerformed(ActionEvent e) {

            }
        });
        final JButton btn2 = new JButton(new AbstractAction("2") {
            public void actionPerformed(ActionEvent e) {

            }
        });
        final JButton btn3 = new JButton(new AbstractAction("3") {
            public void actionPerformed(ActionEvent e) {

            }
        });
        final JButton btn4 = new JButton(new AbstractAction("4") {
            public void actionPerformed(ActionEvent e) {

            }
        });

        panel.add(btn1);
        panel.add(btn2);
        panel.add(btn3);
        panel.add(btn4);

        return panel;
    }

    private void updateControls() {
        newBtn.setEnabled(dbFolder.exists());

        final String name = classifierList.getSelectedValue();
        if(name != null) {
            createNewSession(name);
        }

        final boolean activeSession = name != null && session != null;
        numTrainingImages.setEnabled(activeSession);
        numRetrievedImages.setEnabled(activeSession);

        if(session != null) {
            numTrainingImages.setText(String.valueOf(session.getNumTrainingImages()));
            numRetrievedImages.setText(String.valueOf(session.getNumRetrievedImages()));
            iterationsLabel.setText(String.valueOf(session.getNumIterations()));
        }

        if(getOwner() != null) {
            getOwner().updateState();
        }
    }

    private void createNewSession(final String classifierName) {
        try {
            final String application = (String)applicationCombo.getSelectedItem();
            final PFAApplicationDescriptor applicationDescriptor = PFAApplicationRegistry.getInstance().getDescriptor(application);

            final String dbPath = dbFolderTextField.getText();
            session = new CBIRSession(classifierName, applicationDescriptor, dbPath);
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
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
            if(dbFolder.exists()) {
                chooser.setCurrentDirectory(dbFolder.getParentFile());
            }
            if (chooser.showDialog(window, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                dbFolder = chooser.getSelectedFile();
                dbFolderTextField.setText(dbFolder.getAbsolutePath());
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

}