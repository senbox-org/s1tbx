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
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.dataio.ProductIOPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PixelExtractionIOForm {

    private static final String BEAM_PET_OP_FILE_LAST_OPEN_DIR = "beam.petOp.file.lastOpenDir";

    private static final String[] PRODUCT_TYPES = new String[]{
            "MER_FR__1P",
            "MER_RR__1P",
            "MER_FRS_1P",
            "MER_FSG_1P",
            "MER_FRG_1P",
            "MER_FR__2P",
            "MER_RR__2P",
            "MER_FRS_2P",
            "ATS_TOA_1P",
            "ATS_NR__2P"
    };

    private JPanel panel;
    private MyListModel listModel;
    private AppContext appContext;
    private JList inputPathsList;

    public PixelExtractionIOForm(AppContext appContext, PropertyContainer container) {
        this.appContext = appContext;

        panel = new JPanel(createLayout());
        final BindingContext bindingContext = new BindingContext(container);

        panel.add(new JLabel("Product Type"));
        panel.add(createProductTypeEditor(bindingContext));
        panel.add(new JLabel(""));

        inputPathsList = createInputPathsList(container.getProperty("inputPaths"));
        panel.add(new JLabel("Input paths"));
        panel.add(new JScrollPane(inputPathsList));
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(createAddInputButton());
        buttonPanel.add(createRemoveInputButton());
        panel.add(buttonPanel);

        panel.add(new JLabel("Square size"));
        panel.add(createSquareSizeEditor(container, bindingContext));
        panel.add(new JLabel(""));
    }

    private static TableLayout createLayout() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellWeightY(1, 1, 1.0);
        tableLayout.setCellFill(1, 1, TableLayout.Fill.BOTH);
        return tableLayout;
    }

    private JList createInputPathsList(Property property) {
        listModel = new MyListModel(property);
        JList inputPathsList = new JList(listModel);
        inputPathsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        inputPathsList.setPreferredSize(new Dimension(200, 150));
        inputPathsList.setMinimumSize(new Dimension(100, 50));
        return inputPathsList;
    }

    private AbstractButton createAddInputButton() {
        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JPopupMenu popup = new JPopupMenu("Add");
                final Rectangle buttonBounds = addButton.getBounds();
                popup.add(new AddDirectoryAction(false));
                popup.add(new AddDirectoryAction(true));
                popup.add(new AddFileAction());
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

    private JComponent createProductTypeEditor(BindingContext binding) {
        final JComboBox productTypesBox = new JComboBox(PRODUCT_TYPES);
        productTypesBox.setEditable(true);
        binding.bind("productType", productTypesBox);
        productTypesBox.setSelectedIndex(0);
        return productTypesBox;
    }

    private JComponent createSquareSizeEditor(PropertyContainer container, BindingContext binding) {
        final Property squareSizeProperty = container.getProperty("squareSize");
        final Number defaultValue = (Number) squareSizeProperty.getDescriptor().getDefaultValue();
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, null, 2));
        binding.bind("squareSize", spinner);
        return spinner;
    }

    public JPanel getPanel() {
        return panel;
    }

    private class AddDirectoryAction extends AbstractAction {

        private boolean recursive;

        private AddDirectoryAction(boolean recursive) {
            this("Add Directory" + (recursive ? " Recursively" : ""));
            this.recursive = recursive;
        }

        protected AddDirectoryAction(String title) {
            super(title);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final FolderChooser folderChooser = new FolderChooser();

            final PropertyMap preferences = appContext.getPreferences();
            String lastDir = preferences.getPropertyString(BEAM_PET_OP_FILE_LAST_OPEN_DIR,
                                                           SystemUtils.getUserHomeDir().getPath());
            if (lastDir != null) {
                folderChooser.setCurrentDirectory(new File(lastDir));
            }

            final int result = folderChooser.showOpenDialog(panel);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File currentDir = folderChooser.getSelectedFolder();

            if (!recursive) {
                try {
                    listModel.addElement(currentDir.getAbsolutePath());
                } catch (ValidationException ve) {
                    // todo check if this is ok; probably it's not because we do not want a message window for each
                    // validation fail
                    appContext.handleError("Invalid input path", ve);
                }
            } else {
                try {
                    listModel.addElement(currentDir.getAbsolutePath());
                    addFiles(currentDir.listFiles());
                } catch (ValidationException ve) {
                    // todo check if this is ok; probably it's not because we do not want a message window for each
                    // validation fail
                    appContext.handleError("Invalid input path", ve);
                }
            }

            preferences.setPropertyString(BEAM_PET_OP_FILE_LAST_OPEN_DIR, currentDir.getAbsolutePath());

        }

        private void addFiles(File[] files) throws ValidationException {
            for (File file : files) {
                if (file.isDirectory()) {
                    listModel.addElement(file.getAbsolutePath());
                    addFiles(file.listFiles());
                }
            }
        }
    }

    private class AddFileAction extends AbstractAction {

        private AddFileAction() {
            super("Add Product(s)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final PropertyMap preferences = appContext.getPreferences();
            String lastDir = preferences.getPropertyString(BEAM_PET_OP_FILE_LAST_OPEN_DIR,
                                                           SystemUtils.getUserHomeDir().getPath());
            String lastFormat = preferences.getPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT,
                                                              VisatApp.ALL_FILES_IDENTIFIER);
            BeamFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setCurrentDirectory(new File(lastDir));
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setDialogTitle("Select Product(s)");
            fileChooser.setMultiSelectionEnabled(true);

            FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
            Iterator allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            while (allReaderPlugIns.hasNext()) {
                final ProductIOPlugIn plugIn = (ProductIOPlugIn) allReaderPlugIns.next();
                BeamFileFilter productFileFilter = plugIn.getProductFileFilter();
                fileChooser.addChoosableFileFilter(productFileFilter);
                if (!VisatApp.ALL_FILES_IDENTIFIER.equals(lastFormat) &&
                    productFileFilter.getFormatName().equals(lastFormat)) {
                    actualFileFilter = productFileFilter;
                }
            }
            fileChooser.setFileFilter(actualFileFilter);

            int result = fileChooser.showDialog(appContext.getApplicationWindow(), "Open Product");    /*I18N*/
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String currentDir = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (currentDir != null) {
                preferences.setPropertyString(BEAM_PET_OP_FILE_LAST_OPEN_DIR, currentDir);
            }

            if (fileChooser.getFileFilter() instanceof BeamFileFilter) {
                String currentFormat = ((BeamFileFilter) fileChooser.getFileFilter()).getFormatName();
                if (currentFormat != null) {
                    preferences.setPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT, currentFormat);
                }
            } else {
                preferences.setPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT,
                                              VisatApp.ALL_FILES_IDENTIFIER);
            }
            final File[] selectedFiles = fileChooser.getSelectedFiles();
//            model.addFiles(selectedFiles);
        }
    }

    private static class MyListModel extends AbstractListModel {

        private List<Object> list = new ArrayList<Object>();
        private Property property;

        MyListModel(Property property) {
            this.property = property;
        }

        @Override
        public Object getElementAt(int index) {
            return list.get(index);
        }

        @Override
        public int getSize() {
            return list.size();
        }

        public void addElement(Object... elements) throws ValidationException {
            for (Object element : elements) {
                if (!list.contains(element)) {
                    list.add(element);
                }
            }
            updateProperty();
            fireIntervalAdded(this, 0, list.size());
        }

        private void updateProperty() throws ValidationException {
            File[] files = new File[list.size()];
            for (int i = 0; i < list.size(); i++) {
                files[i] = new File(String.valueOf(list.get(i)));
            }
            property.setValue(files);
        }

        public void removeElement(Object... elements) {
            for (Object element : elements) {
                list.remove(element);
            }
            try {
                updateProperty();
            } catch (ValidationException ignored) {
            }
            fireIntervalRemoved(this, 0, list.size());
        }
    }
}
