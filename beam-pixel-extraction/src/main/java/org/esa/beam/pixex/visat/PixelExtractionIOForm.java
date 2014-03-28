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

package org.esa.beam.pixex.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.product.SourceProductList;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

class PixelExtractionIOForm {

    static final String LAST_OPEN_INPUT_DIR = "beam.petOp.lastOpenInputDir";
    static final String LAST_OPEN_OUTPUT_DIR = "beam.petOp.lastOpenOutputDir";
    static final String LAST_OPEN_FORMAT = "beam.petOp.lastOpenFormat";

    private final AppContext appContext;

    private final JPanel panel;
    private final JTextField outputDirTextField;
    private final PropertyContainer container;
    private final BindingContext context;
    private final SourceProductList sourceProductList;

    PixelExtractionIOForm(final AppContext appContext, PropertyContainer container, ChangeListener changeListener) {
        this.appContext = appContext;
        this.container = container;
        context = new BindingContext(container);

        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setColumnPadding(0, new Insets(3, 4, 4, 4));
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellWeightY(0, 1, 1.0);
        tableLayout.setCellFill(0, 1, TableLayout.Fill.BOTH);
        tableLayout.setCellColspan(3, 1, 2);
        panel = new JPanel(tableLayout);

        sourceProductList = new SourceProductList(appContext, container.getProperty("sourceProductPaths"),
                                                  LAST_OPEN_INPUT_DIR, LAST_OPEN_FORMAT, changeListener);
        JPanel[] components = sourceProductList.createComponents();
        panel.add(new JLabel("Source Paths:"));
        panel.add(components[0]);
        panel.add(components[1]);

        panel.add(new JLabel("Time extraction:"));
        panel.add(new TimeExtractionPane(container));
        panel.add(new JLabel(""));

        JLabel outputDirLabel = new JLabel("Output directory:");
        panel.add(outputDirLabel);
        outputDirTextField = new JTextField();
        outputDirTextField.setEditable(false);
        outputDirTextField.setPreferredSize(new Dimension(80, outputDirTextField.getPreferredSize().height));
        String path = getDefaultOutputPath(appContext);
        setOutputDirPath(path);
        panel.add(outputDirTextField);
        AbstractButton outputDirChooserButton = createOutputDirChooserButton(container.getProperty("outputDir"));
        panel.add(outputDirChooserButton);

        JLabel filePrefixLabel = new JLabel("File prefix:");
        JTextField filePrefixField = createFilePrefixField(container.getProperty("outputFilePrefix"));
        panel.add(filePrefixLabel);
        panel.add(filePrefixField);
    }

    JPanel getPanel() {
        return panel;
    }

    void clear() {
        sourceProductList.clear();
        setOutputDirPath("");
    }

    void addProduct(Product selectedProduct) {
        sourceProductList.addProduct(selectedProduct);
    }

    Product[] getSourceProducts() {
        return sourceProductList.getSourceProducts();
    }

    private String getDefaultOutputPath(AppContext appContext) {
        final Property dirProperty = container.getProperty("outputDir");
        String lastDir = appContext.getPreferences().getPropertyString(LAST_OPEN_OUTPUT_DIR, ".");
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

    private void setOutputDirPath(String path) {
        outputDirTextField.setText(path);
        outputDirTextField.setToolTipText(path);
    }

    private AbstractButton createOutputDirChooserButton(final Property outputFileProperty) {
        AbstractButton button = new JButton("...");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FolderChooser folderChooser = new FolderChooser();
                folderChooser.setCurrentDirectory(new File(getDefaultOutputPath(appContext)));
                folderChooser.setDialogTitle("Select output directory");
                folderChooser.setMultiSelectionEnabled(false);
                int result = folderChooser.showDialog(appContext.getApplicationWindow(), "Select");    /*I18N*/
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File selectedFile = folderChooser.getSelectedFile();
                setOutputDirPath(selectedFile.getAbsolutePath());
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

    private JTextField createFilePrefixField(Property property) {
        return createTextFieldBinding(property);
    }

    private JTextField createTextFieldBinding(Property property) {
        final JTextField textField = new JTextField();
        context.bind(property.getName(), textField);
        return textField;
    }

    private JCheckBox createCheckBoxBinding(Property property) {
        final JCheckBox checkBox = new JCheckBox(property.getDescriptor().getDisplayName());
        context.bind(property.getName(), checkBox);
        return checkBox;
    }

    private class TimeExtractionPane extends JPanel {
        public TimeExtractionPane(PropertyContainer container) {
            super(new BorderLayout(0, 5));

            final JCheckBox extractTime = createCheckBoxBinding(container.getProperty("extractTimeFromFilename"));

            final Property datePattern = container.getProperty("dateInterpretationPattern");
            final String dateDN = datePattern.getDescriptor().getDisplayName();
            final JPanel datePanel = new JPanel(new BorderLayout(0, 2));
            final JLabel dateLabel = new JLabel(dateDN + ":");
            final JTextField datePatternField = createTextFieldBinding(datePattern);
            dateLabel.setEnabled(false);
            datePatternField.setEnabled(false);
            datePanel.add(dateLabel, BorderLayout.NORTH);
            datePanel.add(datePatternField, BorderLayout.CENTER);

            final Property filenamePattern = container.getProperty("filenameInterpretationPattern");
            final String filenameDN = filenamePattern.getDescriptor().getDisplayName();
            final JPanel filenamePanel = new JPanel(new BorderLayout(0, 2));
            final JLabel filenameLabel = new JLabel(filenameDN + ":");
            final JTextField filenamePatternField = createTextFieldBinding(filenamePattern);
            filenameLabel.setEnabled(false);
            filenamePatternField.setEnabled(false);
            filenamePanel.add(filenameLabel, BorderLayout.NORTH);
            filenamePanel.add(filenamePatternField, BorderLayout.CENTER);

            extractTime.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    final boolean selected = extractTime.isSelected();
                    dateLabel.setEnabled(selected);
                    datePatternField.setEnabled(selected);
                    filenameLabel.setEnabled(selected);
                    filenamePatternField.setEnabled(selected);
                }
            });

            add(extractTime, BorderLayout.NORTH);
            add(datePanel, BorderLayout.CENTER);
            add(filenamePanel, BorderLayout.SOUTH);
        }
    }
}
