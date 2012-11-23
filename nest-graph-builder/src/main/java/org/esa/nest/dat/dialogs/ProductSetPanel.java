/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileChooserFactory;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.db.ProductEntry;
import org.esa.nest.util.DialogUtils;
import org.esa.nest.util.ProductFunctions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * NEST IO Panel to handle source and target selection
 * User: lveci
 * Date: Feb 5, 2009
 */
public class ProductSetPanel extends JPanel {

    private final FileTable productSetTable;
    private final TargetFolderSelector targetProductSelector;
    private final AppContext appContext;
    private String targetProductNameSuffix = "";
    private JPanel buttonPanel = null;

    public ProductSetPanel(final AppContext theAppContext, final String title) {
        this(theAppContext, title, null, false, false);
    }

    public ProductSetPanel(final AppContext theAppContext, final String title, final FileTableModel fileModel) {
        this(theAppContext, title, fileModel, false, false);
    }

    public ProductSetPanel(final AppContext theAppContext) {
        this(theAppContext, null, null, true, true);
    }

    public ProductSetPanel(final AppContext theAppContext, final String title, final FileTableModel fileModel,
                           final boolean incTrgProduct, final boolean incButtonPanel) {
        super(new BorderLayout());
        this.appContext = theAppContext;

        if(title != null)
            setBorder(BorderFactory.createTitledBorder(title));

        productSetTable = new FileTable(fileModel);
        final JPanel productSetContent = createComponent(productSetTable, false);
        if(incButtonPanel) {
            buttonPanel = createButtonPanel(productSetTable);
            productSetContent.add(buttonPanel, BorderLayout.EAST);
        }
        this.add(productSetContent, BorderLayout.CENTER);

        if(incTrgProduct) {
            targetProductSelector = new TargetFolderSelector();
            final String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            final String saveDir = theAppContext.getPreferences().getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, homeDirPath);
            targetProductSelector.getModel().setProductDir(new File(saveDir));
            targetProductSelector.getOpenInAppCheckBox().setText("Open in " + theAppContext.getApplicationName());
            targetProductSelector.getOpenInAppCheckBox().setVisible(false);

            this.add(targetProductSelector.createPanel(), BorderLayout.SOUTH);
        } else {
            targetProductSelector = null;
        }
    }

    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public static JPanel createComponent(final FileTable table, final boolean incButtonPanel) {

        final JPanel fileListPanel = new JPanel(new BorderLayout(4, 4));

        final JScrollPane scrollPane = new JScrollPane(table);
        fileListPanel.add(scrollPane, BorderLayout.CENTER);

        if(incButtonPanel) {
            final JPanel buttonPanel = createButtonPanel(table);
            fileListPanel.add(buttonPanel, BorderLayout.EAST);
        }
        return fileListPanel;
    }

    public static JPanel createButtonPanel(final FileTable table) {
        final FileTableModel tableModel = table.getModel();

        final JPanel panel = new JPanel(new GridLayout(10, 1));
        final JLabel countLabel = new JLabel();

        final JButton addButton = DialogUtils.CreateButton("addButton", "Add", null, panel);
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final File[] files = GetFilePath(addButton, "Add Product");
                if(files != null) {
                    for(File file : files) {
                        if (ProductFunctions.isValidProduct(file)) {
                            tableModel.addFile(file);
                            countLabel.setText(tableModel.getRowCount()+" Products");
                        }
                    }
                }
            }
        });

        final JButton addAllOpenButton = DialogUtils.CreateButton("addAllOpenButton", "Add Opened", null, panel);
        addAllOpenButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final Product[] products = VisatApp.getApp().getProductManager().getProducts();
                for(Product prod : products) {
                    final File file = prod.getFileLocation();
                    if(file != null && file.exists()) {
                        tableModel.addFile(file);
                    }
                }
                countLabel.setText(tableModel.getRowCount()+" Products");
            }
        });

        final JButton removeButton = DialogUtils.CreateButton("removeButton", "Remove", null, panel);
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final int[] selRows = table.getSelectedRows();
                final java.util.List<File> filesToRemove = new ArrayList<File>(selRows.length);
                for(int row : selRows) {
                    filesToRemove.add(tableModel.getFileAt(row));
                }
                for(File file : filesToRemove) {
                    int index = tableModel.getIndexOf(file);
                    tableModel.removeFile(index);
                }
                countLabel.setText(tableModel.getRowCount()+" Products");
            }

        });

        final JButton moveUpButton = DialogUtils.CreateButton("moveUpButton", "Move Up", null, panel);
        moveUpButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final int[] selRows = table.getSelectedRows();
                final java.util.List<File> filesToMove = new ArrayList<File>(selRows.length);
                for(int row : selRows) {
                    filesToMove.add(tableModel.getFileAt(row));
                }
                for(File file : filesToMove) {
                    int index = tableModel.getIndexOf(file);
                    if(index > 0) {
                        tableModel.move(index, index-1);
                    }
                }
            }

        });

        final JButton moveDownButton = DialogUtils.CreateButton("moveDownButton", "Move Down", null, panel);
        moveDownButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final int[] selRows = table.getSelectedRows();
                final java.util.List<File> filesToMove = new ArrayList<File>(selRows.length);
                for(int row : selRows) {
                    filesToMove.add(tableModel.getFileAt(row));
                }
                for(File file : filesToMove) {
                    int index = tableModel.getIndexOf(file);
                    if(index < tableModel.getRowCount()) {
                        tableModel.move(index, index+1);
                    }
                }
            }

        });

        final JButton clearButton = DialogUtils.CreateButton("clearButton", "Clear", null, panel);
        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                tableModel.clear();
                countLabel.setText("");
            }
        });

        panel.add(addButton);
        panel.add(addAllOpenButton);
        panel.add(moveUpButton);
        panel.add(moveDownButton);
        panel.add(removeButton);
        panel.add(clearButton);
        panel.add(countLabel);

        return panel;
    }

    private static File[] GetFilePath(Component component, String title) {

        File[] files = null;
        final File openDir = new File(VisatApp.getApp().getPreferences().
                getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, "."));
        final JFileChooser chooser = FileChooserFactory.getInstance().createFileChooser(openDir);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle(title);
        if (chooser.showDialog(component, "ok") == JFileChooser.APPROVE_OPTION) {
            files = chooser.getSelectedFiles();

            VisatApp.getApp().getPreferences().
                setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, chooser.getCurrentDirectory().getAbsolutePath());
        }
        return files;
    }

    public void setTargetProductName(final String name) {
        if(targetProductSelector != null) {
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            targetProductSelectorModel.setProductName(name + getTargetProductNameSuffix());
        }
    }

    public void onApply() {
        if(targetProductSelector != null) {
            final String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
            appContext.getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, productDir);
        }
    }

    String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(final String suffix) {
        targetProductNameSuffix = suffix;
    }

    public File getTargetFolder() {
        if(targetProductSelector != null) {
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            return targetProductSelectorModel.getProductDir();
        }
        return null;
    }

    public String getTargetFormat() {
        if(targetProductSelector != null) {
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            return targetProductSelectorModel.getFormatName();
        }
        return null;
    }

    public void setTargetFolder(final File path) {
        if(targetProductSelector != null) {
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            targetProductSelectorModel.setProductDir(path);
        }
    }

    public File[] getFileList() {
        return productSetTable.getFileList();
    }

    public File[] getSelectedFiles() {
        return productSetTable.getModel().getFilesAt(productSetTable.getSelectedRows());
    }

    public Object getValueAt(final int r, final int c) {
        return productSetTable.getModel().getValueAt(r, c);
    }

    public void setProductFileList(final File[] productFileList) {
        productSetTable.setFiles(productFileList);
    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        productSetTable.setProductEntries(productEntryList);
    }
}