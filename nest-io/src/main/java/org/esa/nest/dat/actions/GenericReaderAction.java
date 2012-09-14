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
package org.esa.nest.dat.actions;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.NewProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dataio.generic.GenericReaderPlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;

/**

 */
public class GenericReaderAction extends ExecCommand {

    private ProductReaderPlugIn readerPlugIn = null;
    private ProductFileChooser fileChooser = null;
    private final String formatName = "Generic Binary";
    private String lastDirKey = null;
    private final boolean useAllFileFilter = true;
    private final boolean useFilesAndFolders = false;

    @Override
    public void actionPerformed(final CommandEvent event) {

        readerPlugIn = new GenericReaderPlugIn();

        importProduct();
    }

    void importProduct() {

        VisatApp visatApp = VisatApp.getApp();

        if (readerPlugIn == null) {
            // Should not come here...
            return;
        }

        final File selectedFile = promptForFile();
        if (selectedFile == null) {
            return;
        }

        Debug.assertTrue(fileChooser != null);
        // file chooser only returns a product, if a product subset was created.
        Product product = fileChooser.getSubsetProduct();
        if (product == null) {
            // if no product subset was created, check if a product with the same file location
            // was opened.
            product = visatApp.getOpenProduct(selectedFile);
            if (product != null) {
                visatApp.showErrorDialog("The product is already open.\n"
                        + "A product can only be opened once.");
                visatApp.setSelectedProductNode(product);
                return;
            }
        }

        try {
            if (product == null) {
                product = readProductNodes(selectedFile);
            }
            if (product != null) {
                visatApp.addProduct(product);
                product.setModified(false);
            }
        } catch (Exception e) {
            visatApp.handleUnknownException(e);
        }
    }


    File promptForFile() {
        if (lastDirKey == null) {
            lastDirKey = "user." + formatName.toLowerCase().replace(' ', '_') + ".import.dir";
        }

        File currentDir = null;
        final VisatApp visatApp = VisatApp.getApp();
        final String currentDirPath = visatApp.getPreferences().getPropertyString(lastDirKey,
                                                                            SystemUtils.getUserHomeDir().getPath());
        if (currentDirPath != null) {
            currentDir = new File(currentDirPath);
        }
        if (fileChooser == null) {
            fileChooser = new ProductFileChooser();
            if(useFilesAndFolders)
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setAcceptAllFileFilterUsed(useAllFileFilter);
            if(!useFilesAndFolders) {
                final BeamFileFilter fileFilter = readerPlugIn.getProductFileFilter();
                if (fileFilter != null) {
                    fileChooser.setFileFilter(fileFilter);
                }
            }
            HelpSys.enableHelpKey(fileChooser, getHelpId());
        }
        fileChooser.setCurrentDirectory(currentDir);

        File file = null;
        boolean canceled = false;
        while (file == null && !canceled) {
            final int result = fileChooser.showOpenDialog(visatApp.getMainFrame());
            file = fileChooser.getSelectedFile();
            if (file != null && file.getParent() != null) {
                visatApp.getPreferences().setPropertyString(lastDirKey, file.getParent());
            }
            if (result == JFileChooser.APPROVE_OPTION) {
                if (file != null && file.getName().trim().length() != 0) {
                    if(useFilesAndFolders && file.isDirectory()) {
                        return file;
                    }
                    if (!file.exists()) {
                        visatApp.showErrorDialog("File not found:\n" + file.getPath());
                        file = null;
                    } else {

                        final double fileSize = file.length() / (1024.0 * 1024.0);
                        if (fileSize == 0.0) {
                            visatApp.showErrorDialog("File is empty:\n" + file.getPath());
                            file = null;
                        }
                    }
                }
            } else {
                canceled = true;
            }
        }

        return canceled ? null : file;
    }

    private Product readProductNodes(final File file) {

        Product product = null;
        final VisatApp visatApp = VisatApp.getApp();
        try {
            visatApp.setStatusBarMessage("Reading from '" + file + "'..."); /*I18N*/
            visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            final ProductReader reader = readerPlugIn.createReaderInstance();
            product = reader.readProductNodes(file, null);

            visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
            visatApp.clearStatusBarMessage();
        } catch (Exception e) {
            visatApp.handleUnknownException(e);
        }

        return product;
    }

    protected static boolean isFileOfFormat(File file, String format) {
        final ProductIOPlugInManager manager = ProductIOPlugInManager.getInstance();
        final Iterator it = manager.getReaderPlugIns(format);
        if (it.hasNext()) {
            final ProductReaderPlugIn plugIn = (ProductReaderPlugIn) it.next();
            return plugIn.getDecodeQualification(file) != DecodeQualification.UNABLE;
        }
        return false;
    }

    protected class ProductFileChooser extends BeamFileChooser {

    private static final long serialVersionUID = -8122437634943074658L;

    private JButton _subsetButton;
    private Product _subsetProduct;

    private JLabel _sizeLabel;

    public ProductFileChooser() {
        createUI();
    }

    /**
     * File chooser only returns a product, if a product subset was created.
     *
     * @return the product subset or null
     */
    public Product getSubsetProduct() {
        return _subsetProduct;
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) {
        clearCurrentProduct();
        return super.showDialog(parent, approveButtonText);
    }

    void createUI() {

        setDialogType(OPEN_DIALOG);
        setDialogTitle(VisatApp.getApp().getAppName() + " - Import " + formatName + " Product"); /*I18N*/

        _subsetButton = new JButton("Subset...");  /*I18N*/
        _subsetButton.setMnemonic('S'); /*I18N*/
        _subsetButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                openProductSubsetDialog();
            }
        });
        _subsetButton.setEnabled(false);

        JButton _historyButton = new JButton("History...");
        _historyButton.setMnemonic('H');
        _historyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // @todo 2 nf/nf - Implement!
            }
        });
        _historyButton.setEnabled(false);

        _sizeLabel = new JLabel("0 M");
        _sizeLabel.setHorizontalAlignment(JLabel.RIGHT);
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints(
                "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
        GridBagUtils.addToPanel(panel, _subsetButton, gbc, "gridy=0");
        //GridBagUtils.addToPanel(panel, _historyButton, gbc, "gridy=1");
        GridBagUtils.addToPanel(panel, _sizeLabel, gbc, "gridy=1");
        GridBagUtils.addVerticalFiller(panel, gbc);

        setAccessory(panel);

        addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();
                if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    clearCurrentProduct();
                    _subsetButton.setEnabled(true);
                } else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                    clearCurrentProduct();
                    _subsetButton.setEnabled(false);
                }
                updateState();
            }
        });

        ProductFileChooser.this.setPreferredSize(new Dimension(640, 400));
        clearCurrentProduct();
        updateState();
    }

    private void updateState() {
        setApproveButtonText("Import Product");/*I18N*/
        setApproveButtonMnemonic('I');/*I18N*/
        setApproveButtonToolTipText("Imports the entire product.");/*I18N*/
        File file = getSelectedFile();
        if (file != null && file.isFile()) {
            long fileSize = Math.round(file.length() / (1024.0 * 1024.0));
            if (fileSize >= 1) {
                _sizeLabel.setText("File size: " + fileSize + " M");
            } else {
                _sizeLabel.setText("File size: < 1 M");
            }
        } else {
            _sizeLabel.setText("");
        }
    }

    private void clearCurrentProduct() {
        _subsetProduct = null;
    }

    private void openProductSubsetDialog() {

        File file = getSelectedFile();
        if (file == null) {
            // Should not come here...
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Product product = readProductNodes(file);
        setCursor(Cursor.getDefaultCursor());

        boolean approve = openProductSubsetDialog(product);
        if (approve) {
            approveSelection();
        }

        updateState();
    }

    private boolean openProductSubsetDialog(Product product) {
        _subsetProduct = null;
        boolean approve = false;
        if (product != null) {
            VisatApp visatApp = VisatApp.getApp();
            JFrame mainFrame = visatApp.getMainFrame();
            ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(mainFrame, product);
            if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                ProductNodeList<Product> products = new ProductNodeList<Product>();
                products.add(product);
                NewProductDialog newProductDialog = new NewProductDialog(visatApp.getMainFrame(), products, 0,
                                                                         true);
                newProductDialog.setSubsetDef(productSubsetDialog.getProductSubsetDef());
                if (newProductDialog.show() == NewProductDialog.ID_OK) {
                    _subsetProduct = newProductDialog.getResultProduct();
                    approve = _subsetProduct != null;
                    if (!approve && newProductDialog.getException() != null) {
                        visatApp.showErrorDialog("The product subset could not be created:\n" +
                                newProductDialog.getException().getMessage());
                    }
                }
            }
        }
        return approve;
    }
   }

}
