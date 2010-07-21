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

package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

/**
 * A file chooser designed for data products.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class ProductFileChooser extends BeamFileChooser {

    private Product _product;
    private ProductReaderPlugIn _readerPlugIn;
    private ProductSubsetDef _productSubsetDef;
    private JButton _subsetButton;
    private JLabel _sizeLabel;

    /**
     * Constructs an "Import" file chooser with the given title and the given reader plug-in.
     *
     * @param title        the dialog title, must not be <code>null</code>
     * @param readerPlugIn the reader plug-in for a particular product type, must not be <code>null</code>
     */
    public ProductFileChooser(String title, ProductReaderPlugIn readerPlugIn) {
        Guardian.assertNotNullOrEmpty("title", title);
        Guardian.assertNotNull("readerPlugIn", readerPlugIn);
        _readerPlugIn = readerPlugIn;
        setCurrentFilename("noname");
        setDialogTitle(title);
        setFileSelectionMode(JFileChooser.FILES_ONLY);
//        setAcceptAllFileFilterUsed(true);
        addChoosableFileFilter(new BeamFileFilter());
        createUI();
    }

    /**
     * Constructs an "Export" file chooser with the given title and for the given product.
     *
     * @param title   the dialog title, must not be <code>null</code>
     * @param product the product to export, must not be <code>null</code>
     */
    public ProductFileChooser(String title, Product product) {
        Guardian.assertNotNullOrEmpty("title", title);
        Guardian.assertNotNull("product", product);
        setProduct(product);
        setDialogTitle(title);
        setFileSelectionMode(JFileChooser.FILES_ONLY);
//        setAcceptAllFileFilterUsed(true);
        createUI();
    }

    /**
     * Returns the product.
     *
     * @return the product which can be <code>null</code>, if this is an "Import" file chooser.
     */
    public Product getProduct() {
        return _product;
    }

    /**
     * Sets the product.
     *
     * @param product the product which must not be <code>null</code>, if this is an "Export" file chooser.
     */
    public void setProduct(Product product) {
        if (_readerPlugIn == null) {
            Guardian.assertNotNull("product", product);
        }
        _product = product;
        if (_product != null) {
            setCurrentFilename(_product.getName());
        }
    }

    /**
     * Returns the product subset definition.
     *
     * @return the product subset definition, can be <code>null</code>
     */
    public ProductSubsetDef getProductSubsetDef() {
        return _productSubsetDef;
    }

    /**
     * Clears the currently used product subset definition.
     */
    public void clearProductSubsetDef() {
        _productSubsetDef = null;
        updateUIState();
    }

    /**
     * Returns whether or not this file chooser is a product import dialog.
     *
     * @return <code>true</code> if so
     */
    public boolean isImportDialog() {
        return _readerPlugIn != null;
    }

    /**
     * Returns whether or not this file chooser is a product export dialog.
     *
     * @return <code>true</code> if so
     */
    public boolean isExportDialog() {
        return !isImportDialog();
    }

    /**
     * Called if the user clicks the 'Subset' button.
     */
    protected void openProductSubsetDialog() {

        if (isImportDialog()) {
            File file = getSelectedFile();
            if (file != null) {
                _product = readProductNodes(file);
                _productSubsetDef = null;
            }
        }

        if (_product == null) {
            return;
        }

        ProductSubsetDialog dialog = new ProductSubsetDialog(getWindow(),
                                                             _product,
                                                             _productSubsetDef
//                                                             isImportDialog(),
        );
        if (dialog.show() == ProductSubsetDialog.ID_OK) {
            if (dialog.getProductSubsetDef().isEntireProductSelected()) {
                _productSubsetDef = null;
            } else {
                _productSubsetDef = dialog.getProductSubsetDef();
            }
            if (getCurrentFilename() != null && !getCurrentFilename().startsWith("subset_")) {
                setCurrentFilename("subset_" + getCurrentFilename());
            }
        }
        updateUIState();
    }

    /**
     * Called if the user clicks the 'History' button.
     */
    protected void openHistoryDialog() {
    }

    /**
     * Creates the UI of this file chooser. Called by the constructors.
     */
    protected void createUI() {

        _subsetButton = new JButton("Subset...");  /*I18N*/
        _subsetButton.setMnemonic('S'); /*I18N*/
        _subsetButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openProductSubsetDialog();
            }
        });
        _subsetButton.setEnabled(_product != null);

        JButton historyButton = new JButton("History...");
        historyButton.setMnemonic('H');
        historyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openHistoryDialog();
            }
        });
        historyButton.setEnabled(false);

        _sizeLabel = new JLabel(" ");
        _sizeLabel.setHorizontalAlignment(JLabel.RIGHT);

        JPanel accessoryPane = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints(
                "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
        GridBagUtils.addToPanel(accessoryPane, _subsetButton, gbc, "gridy=0");
        //GridBagUtils.addToPanel(accessoryPane, historyButton, gbc, "gridy=1");
        GridBagUtils.addToPanel(accessoryPane, _sizeLabel, gbc, "gridy=1");
        GridBagUtils.addVerticalFiller(accessoryPane, gbc);

        setAccessory(accessoryPane);

        addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (!isVisible()) {
                    return;
                }
                Debug.trace(event);
            }
        });

        ProductFileChooser.this.setPreferredSize(new Dimension(640, 400));

        updateUIState();
    }

    /**
     * Updates the state of this file chooser's assessory.
     */
    protected void updateUIState() {

        // UI already created?
        if (_subsetButton == null) {
            // If not, we've nothing to do
            return;
        }

        if (isImportDialog()) {
            _product = null;
            _productSubsetDef = null;
        }

        File selectedFile = getSelectedFile();
        if (selectedFile != null && !selectedFile.isFile()) {
            selectedFile = null;
        }

        long fileSize = 0L;
        if (isImportDialog()) {
            setDialogType(JFileChooser.OPEN_DIALOG);
            _subsetButton.setEnabled(selectedFile != null);
            if (_productSubsetDef != null) {
                setApproveButtonText("Import Subset"); /*I18N*/
                setApproveButtonMnemonic('I');/*I18N*/
                setApproveButtonToolTipText("Imports the specified subset of the product.");/*I18N*/
            } else {
                setApproveButtonText("Import Product");/*I18N*/
                setApproveButtonMnemonic('I');/*I18N*/
                setApproveButtonToolTipText("Imports the entire product.");/*I18N*/
            }
            if (selectedFile != null) {
                fileSize = selectedFile.length();
            }
        } else {
            setDialogType(JFileChooser.SAVE_DIALOG);
            _subsetButton.setEnabled(_product != null);
            if (_productSubsetDef != null) {
                setApproveButtonText("Export Subset"); /*I18N*/
                setApproveButtonMnemonic('E');/*I18N*/
                setApproveButtonToolTipText("Exports the specified subset of the product.");/*I18N*/
            } else {
                setApproveButtonText("Export Product");/*I18N*/
                setApproveButtonMnemonic('E');/*I18N*/
                setApproveButtonToolTipText("Exports the entire product.");/*I18N*/
            }
            // _product may still be null
            if (_product != null) {
                fileSize = _product.getRawStorageSize(_productSubsetDef);
            }
        }

        if (fileSize > 0L) {
            fileSize = Math.round(fileSize / (1024.0 * 1024.0));
            if (fileSize >= 1) {
                _sizeLabel.setText("File size: ~" + fileSize + " M");
            } else {
                _sizeLabel.setText("File size: <1 M");
            }
        } else {
            _sizeLabel.setText("");
        }
    }

    private Product readProductNodes(final File file) {

        final ProductReader reader = _readerPlugIn.createReaderInstance();
        Product product = null;
        Cursor cursor = UIUtils.setRootFrameWaitCursor(this);
        try {
            product = reader.readProductNodes(file, null);
            UIUtils.setRootFrameCursor(this, cursor);
        } catch (IOException e) {
            UIUtils.setRootFrameCursor(this, cursor);
            JOptionPane.showMessageDialog(getWindow(),
                                          "Product I/O Error:\n"
                                          + "'" + e.getMessage() + "'",
                                          "Product I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
        }

        return product;
    }
}
