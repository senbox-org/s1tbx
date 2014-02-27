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


public class CBIRControlCentreToolView extends AbstractToolView {

    private final static Font titleFont = new Font("Ariel", Font.BOLD, 14);
    private final static String instructionsStr = "Select a feature extraction application";
    private final static String PROPERTY_KEY_DB_PATH = "app.file.cbir.dbPath";

    private JComboBox<String> applicationCombo;
    private JList<String> classifierList;
    private JButton newBtn, deleteBtn;
    private JButton queryBtn, trainBtn;
    private JTextField numTrainingImages;
    private JTextField numRetrievedImages;
    private JLabel iterationsLabel = new JLabel();

    private File dbFolder;
    private JTextField dbFolderTextField;

    private CBIRSession session = null;

    public CBIRControlCentreToolView() {
    }

    public JComponent createControl() {

        final JPanel mainPane = new JPanel(new BorderLayout(5,5));
        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        final PFAApplicationDescriptor[] apps = PFAApplicationRegistry.getInstance().getAllDescriptors();
        applicationCombo = new JComboBox<>();
        for(PFAApplicationDescriptor app : apps) {
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
        if(dbFolder.exists()) {
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
        numTrainingImages = new JTextField();
        numTrainingImages.setColumns(3);
        numTrainingImages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                session = null;
                updateControls();
            }
        });
        optionsPane.add(numTrainingImages, gbcOpt);

        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        optionsPane.add(new JLabel("# of retrieved images:"), gbcOpt);
        gbcOpt.gridx = 1;
        numRetrievedImages = new JTextField();
        numRetrievedImages.setColumns(3);
        numRetrievedImages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                session = null;
                updateControls();
            }
        });
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

        mainPane.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);
        mainPane.add(contentPane, BorderLayout.CENTER);
        mainPane.add(createSideButtonPanel(), BorderLayout.EAST);

        updateControls();

        return mainPane;
    }

    protected JLabel createTitleLabel() {
        final JLabel titleLabel = new JLabel("CBIR");
        titleLabel.setFont(titleFont);
        return titleLabel;
    }

    protected static JPanel createTextPanel(final String title, final String text) {
        final JPanel textPanel = new JPanel(new BorderLayout(2,2));
        if(title != null)
            textPanel.setBorder(BorderFactory.createTitledBorder(title));
        final JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPanel.add(textPane, BorderLayout.CENTER);
        return textPanel;
    }

    protected JPanel createInstructionsPanel(final String title, final String text) {
        final JPanel instructPanel = new JPanel(new BorderLayout(2, 2));
        instructPanel.add(createTitleLabel(), BorderLayout.NORTH);
        instructPanel.add(createTextPanel(title, text), BorderLayout.CENTER);
        return instructPanel;
    }

    private JPanel createClassifierButtonPanel() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        newBtn = new JButton(new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) {
                final PromptDialog dlg = new PromptDialog("New Classifier", "Name", "");
                dlg.show();

                final String value = dlg.getValue();
                if(!value.isEmpty()) {
                    final DefaultListModel listModel = (DefaultListModel)classifierList.getModel();
                    listModel.addElement(value);
                    classifierList.setSelectedIndex(listModel.indexOf(value));
                }
            }
        });
        deleteBtn = new JButton(new AbstractAction("Delete") {
             public void actionPerformed(ActionEvent e) {
                final boolean ret = session.deleteClassifier();
                if(ret) {
                    final DefaultListModel listModel = (DefaultListModel)classifierList.getModel();
                    listModel.remove(classifierList.getSelectedIndex());
                }
             }
         });

        panel.add(newBtn);
        panel.add(deleteBtn);

        return panel;
    }

    private JPanel createSideButtonPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        queryBtn = new JButton(new AbstractAction("Query") {
            public void actionPerformed(ActionEvent e) {
                getContext().getPage().showToolView(CBIRQueryToolView.ID);
            }
        });
        trainBtn = new JButton(new AbstractAction("Training") {
            public void actionPerformed(ActionEvent e) {

            }
        });
      /*  final JButton btn3 = new JButton(new AbstractAction("3") {
            public void actionPerformed(ActionEvent e) {

            }
        });
        final JButton btn4 = new JButton(new AbstractAction("4") {
            public void actionPerformed(ActionEvent e) {

            }
        });    */

        panel.add(queryBtn);
        panel.add(trainBtn);
        //panel.add(btn3);
        //panel.add(btn4);

        return panel;
    }

    private void updateControls() {
        newBtn.setEnabled(dbFolder.exists());

        final String name = classifierList.getSelectedValue();
        if(name != null) {
            if(session == null || !session.getName().equals(name)) {
                createNewSession(name);
            }
        }

        final boolean activeSession = name != null && session != null;
        deleteBtn.setEnabled(activeSession);

        applicationCombo.setEnabled(activeSession);
        numTrainingImages.setEnabled(activeSession);
        numRetrievedImages.setEnabled(activeSession);

        queryBtn.setEnabled(activeSession);
        trainBtn.setEnabled(activeSession);

        if(session != null) {
            numTrainingImages.setText(String.valueOf(session.getNumTrainingImages()));
            numRetrievedImages.setText(String.valueOf(session.getNumRetrievedImages()));
            iterationsLabel.setText(String.valueOf(session.getNumIterations()));
        }
    }

    private void createNewSession(final String classifierName) {
        try {
            System.out.println("Creating new session");

            final String application = (String)applicationCombo.getSelectedItem();
            final PFAApplicationDescriptor applicationDescriptor = PFAApplicationRegistry.getInstance().getDescriptor(application);

            final String dbPath = dbFolderTextField.getText();
            session = CBIRSession.Instance();

            session.initSession(classifierName, applicationDescriptor, dbPath);

        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
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

}