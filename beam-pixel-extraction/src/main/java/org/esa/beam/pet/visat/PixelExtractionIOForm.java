/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class PixelExtractionIOForm {

    static final String BEAM_PET_OP_FILE_LAST_OPEN_DIR = "beam.petOp.file.lastOpenDir";

    private JPanel panel;
    private InputFilesListModel listModel;
    private AppContext appContext;
    private JList inputPathsList;
    private JTextField outputFileTextField;

    public PixelExtractionIOForm(AppContext appContext, PropertyContainer container) {
        this.appContext = appContext;

        panel = new JPanel(createLayout());

        inputPathsList = createInputPathsList(container.getProperty("inputPaths"));
        panel.add(new JLabel("Input paths:"));
        panel.add(new JScrollPane(inputPathsList));
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(createAddInputButton());
        buttonPanel.add(createRemoveInputButton());
        panel.add(buttonPanel);

        panel.add(new JLabel("Output file:"));
        outputFileTextField = new JTextField();
        panel.add(outputFileTextField);
        panel.add(createFileChooserButton(container.getProperty("outputFile")));
    }

    private static TableLayout createLayout() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellWeightY(0, 1, 1.0);
        tableLayout.setCellFill(0, 1, TableLayout.Fill.BOTH);
        return tableLayout;
    }

    private AbstractButton createFileChooserButton(final Property outputFileProperty) {
        AbstractButton button = new JButton("...");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PropertyMap preferences = appContext.getPreferences();
                String lastDir = preferences.getPropertyString(BEAM_PET_OP_FILE_LAST_OPEN_DIR,
                                                               SystemUtils.getUserHomeDir().getPath());

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(lastDir));
                fileChooser.setDialogTitle("Select output file");
                fileChooser.setMultiSelectionEnabled(false);
                int result = fileChooser.showDialog(appContext.getApplicationWindow(), "Select");    /*I18N*/
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File selectedFile = fileChooser.getSelectedFile();
                outputFileTextField.setText(selectedFile.getAbsolutePath());
                try {
                    outputFileProperty.setValue(selectedFile);
                } catch (ValidationException ve) {
                    // not expected to ever come here
                    appContext.handleError("Invalid input path", ve);
                }
            }
        });
        return button;
    }

    private JList createInputPathsList(Property property) {
        listModel = new InputFilesListModel(property);
        JList list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return list;
    }

    private AbstractButton createAddInputButton() {
        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JPopupMenu popup = new JPopupMenu("Add");
                final Rectangle buttonBounds = addButton.getBounds();
                popup.add(new AddDirectoryAction(appContext, listModel, false));
                popup.add(new AddDirectoryAction(appContext, listModel, true));
                popup.add(new AddFileAction(appContext, listModel));
                popup.show(addButton, 1, buttonBounds.height + 1);
            }
        });
        return addButton;
    }

    private AbstractButton createRemoveInputButton() {
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"),
                                                                           false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.removeElement(inputPathsList.getSelectedValues());
            }
        });
        return removeButton;
    }

    public JPanel getPanel() {
        return panel;
    }

}
