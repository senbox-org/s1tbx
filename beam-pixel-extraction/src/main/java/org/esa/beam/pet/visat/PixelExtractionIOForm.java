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
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
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
import java.io.IOException;

public class PixelExtractionIOForm {

    static final String LAST_OPEN_INPUT_DIR = "beam.petOp.lastOpenInputDir";
    static final String LAST_OPEN_OUTPUT_DIR = "beam.petOp.lastOpenOutputDir";
    static final String LAST_OPEN_FORMAT = "beam.petOp.lastOpenFormat";

    private final AppContext appContext;

    private JPanel panel;
    private InputFilesListModel listModel;
    private JList inputPathsList;
    private JTextField outputDirTextField;
    private PropertyContainer container;

    public PixelExtractionIOForm(final AppContext appContext, PropertyContainer container) {
        this.appContext = appContext;
        this.container = container;

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

        panel.add(new JLabel("Output directory:"));
        outputDirTextField = new JTextField();
        outputDirTextField.setEditable(false);
        String path = getOutputPath(appContext);
        outputDirTextField.setText(path);
        panel.add(outputDirTextField);
        panel.add(createFileChooserButton(container.getProperty("outputDir")));
    }

    private String getOutputPath(AppContext appContext) {
        final Property dirProperty = container.getProperty("outputDir");
        final Object value = dirProperty.getDescriptor().getDefaultValue();
        String lastDir = appContext.getPreferences().getPropertyString(LAST_OPEN_OUTPUT_DIR, value.toString());
        String path;
        try {
            path = new File(lastDir).getCanonicalPath();
        } catch (IOException ignored) {
            path = SystemUtils.getUserHomeDir().getPath();
        }
        try {
            dirProperty.setValue(new File(path));
        } catch (ValidationException ignore) {
        }
        return path;
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
                FolderChooser folderChooser = new FolderChooser();
                folderChooser.setCurrentDirectory(new File(getOutputPath(appContext)));
                folderChooser.setDialogTitle("Select output directory");
                folderChooser.setMultiSelectionEnabled(false);
                int result = folderChooser.showDialog(appContext.getApplicationWindow(), "Select");    /*I18N*/
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File selectedFile = folderChooser.getSelectedFile();
                outputDirTextField.setText(selectedFile.getAbsolutePath());
                try {
                    outputFileProperty.setValue(selectedFile);
                    appContext.getPreferences().setPropertyString(LAST_OPEN_OUTPUT_DIR,
                                                                  selectedFile.getAbsolutePath());

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
        if (appContext.getSelectedProduct() != null) {
            File fileLocation = appContext.getSelectedProduct().getFileLocation();
            if (fileLocation != null) {
                try {
                    listModel.addElement(fileLocation);
                } catch (ValidationException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
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
                popup.add(new AddProductAction(appContext, listModel));
                popup.add(new AddFileAction(appContext, listModel));
                popup.add(new AddDirectoryAction(appContext, listModel, false));
                popup.add(new AddDirectoryAction(appContext, listModel, true));
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

    public void clear() {
        listModel.clear();
        outputDirTextField.setText("");
    }
}
