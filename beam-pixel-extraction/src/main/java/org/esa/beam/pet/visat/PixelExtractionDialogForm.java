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
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.io.FileArrayEditor;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PixelExtractionDialogForm {

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

    private static final String INPUT_PRODUCT_DIR_KEY = "gpf.petop.input.product.dir";

    private JPanel panel;

    public PixelExtractionDialogForm(AppContext appContext, PropertyContainer container) {

        final TableLayout tableLayout = createLayout();

        panel = new JPanel();
        panel.setLayout(tableLayout);
        final BindingContext bindingContext = new BindingContext(container);

        panel.add(new JLabel("Product Type"));
        panel.add(createProductTypeEditor(bindingContext));
        panel.add(tableLayout.createHorizontalSpacer());

        final FileArrayEditor editor = createSourceProductEditor(appContext, bindingContext, container);
        panel.add(new JLabel("Source Products"), new TableLayout.Cell(1, 0, 2, 1));
        panel.add(editor.createFileArrayComponent(), new TableLayout.Cell(1, 1, 2, 1));
        panel.add(editor.createAddFileButton());
        panel.add(editor.createRemoveFileButton(), new TableLayout.Cell(2, 2));

        panel.add(new JLabel("Products path"));
        panel.add(createInputFolderChooser(bindingContext));
        panel.add(createInputButton(bindingContext));

        panel.add(new JLabel("Square size"));
        panel.add(createSquareSizeEditor(container, bindingContext));
        panel.add(tableLayout.createHorizontalSpacer());
    }

    private JTextField createInputFolderChooser(BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        bindingContext.bind("inputPath", textField);
        return textField;
    }

    private JButton createInputButton(BindingContext bindingContext) {
        final Binding binding = bindingContext.getBinding("inputPath");
        final JButton etcButton = new JButton("...");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new FolderChooser();
                int i = fileChooser.showDialog(panel, "Select");
                if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                    binding.setPropertyValue(fileChooser.getSelectedFile());
                }
            }
        });
        return etcButton;
    }

    private static TableLayout createLayout() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setTableWeightY(0.0);
        return tableLayout;
    }

    private FileArrayEditor createSourceProductEditor(final AppContext appContext, final BindingContext binding,
                                                      final PropertyContainer container) {
        final FileArrayEditor.EditorParent context = new FileArrayEditorContext(appContext);
        final FileArrayEditor editor = new FileArrayEditor(context, "Source products");
        final FileArrayEditor.FileArrayEditorListener listener = new MyFileArrayEditorListener(container, appContext);
        editor.setListener(listener);
        return editor;
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

    private static class FileArrayEditorContext implements FileArrayEditor.EditorParent {

        private final AppContext applicationContext;

        private FileArrayEditorContext(AppContext applicationContext) {

            this.applicationContext = applicationContext;
        }

        @Override
        public File getUserInputDir() {
            final String path = applicationContext.getPreferences().getPropertyString(INPUT_PRODUCT_DIR_KEY);
            final File inputProductDir;
            if (path != null) {
                inputProductDir = new File(path);
            } else {
                inputProductDir = null;
            }
            return inputProductDir;
        }

        @Override
        public void setUserInputDir(File newDir) {
            applicationContext.getPreferences().setPropertyString(INPUT_PRODUCT_DIR_KEY,
                                                                  newDir.getAbsolutePath());
        }

    }

    private static class MyFileArrayEditorListener implements FileArrayEditor.FileArrayEditorListener {

        private final PropertyContainer container;
        private final AppContext appContext;
        private HashMap<File, Product> map;

        private MyFileArrayEditorListener(PropertyContainer container, AppContext appContext) {
            this.container = container;
            this.appContext = appContext;
            map = new HashMap<File, Product>(31);
        }

        @Override
        public void updatedList(final File[] files) {
            final SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    mapSourceProducts( files );
                    final Collection<Product> products = map.values();
                    container.getProperty("sourceProducts").setValue(products.toArray(new Product[products.size()]));
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                    } catch (Exception e) {
                        final String msg = String.format("Cannot display source products.\n%s", e.getMessage());
                        appContext.handleError(msg, e);
                    }
                }
            };
            worker.execute();
        }

        void mapSourceProducts(File[] files) throws IOException {
            final List<File> fileList = Arrays.asList(files);
            final Iterator<Map.Entry<File, Product>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<File, Product> entry = iterator.next();
                if (!fileList.contains(entry.getKey())) {
                    final Product product = entry.getValue();
                    iterator.remove();
                    product.dispose();
                }
            }
            for (final File file : files) {
                Product product = map.get(file);
                if (product == null) {
                    product = ProductIO.readProduct(file);
                    map.put(file, product);
                }
//                final int refNo = i + 1;
//                if (product.getRefNo() != refNo) {
//                    product.resetRefNo();
//                    product.setRefNo(refNo);
//                }
            }
        }

    }
}
