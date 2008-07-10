/*
 * $Id: ProductImportAction.java,v 1.9 2007/04/18 13:01:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.dataio.dimap.DimapProductConstants;
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
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;

/**
 * This action imports a product of the associated format.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ProductImportAction extends ExecCommand {

    private int memWarnLimit;
    private ProductReaderPlugIn readerPlugIn;
    private ProductImportAction.ProductFileChooser fileChooser;
    private String formatName;
    private String lastDirKey;
    private boolean useAllFileFilter;

    @Override
    public void actionPerformed(final CommandEvent event) {
        importProduct();
    }

    @Override
    public void updateState(CommandEvent event) {
        setEnabled(readerPlugIn != null);
    }

    @Override
    public void updateComponentTreeUI() {
        if (fileChooser != null) {
            SwingUtilities.updateComponentTreeUI(fileChooser);
        }
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        String configString = getConfigString(config, "formatName");
        if (configString != null) {
            setFormatName(configString);
        }


        configureReaderPlugin(config);

        Boolean configBoolean = getConfigBoolean(config, "useAllFileFilter");
        if (configBoolean != null) {
            useAllFileFilter = configBoolean;
        }

        // if properties are not set, set the defaults

        String text = getText();
        if (text == null) {
            setText("Import " + getFormatName() + " Product...");
        }

        String parent = getParent();
        if (parent == null) {
            setParent("import");
        }

        String placeAfter = getPlaceAfter();
        if (placeAfter == null) {
            setPlaceAfter("importTransectData");
        }
    }

    private void configureReaderPlugin(ConfigurationElement config) throws CoreException {
        String resString = getConfigString(config, "readerPlugin");
        if (resString != null) {
            Class<?> aClass;
            try {
                aClass = config.getDeclaringExtension().getDeclaringModule().loadClass(resString);
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("[{0}]: Not able to load class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  resString);
                throw new CoreException(msg, e);
            }
            Class<ProductReaderPlugIn> readerPluginClass;
            if (ProductReaderPlugIn.class.isAssignableFrom(aClass)) {
                readerPluginClass = (Class<ProductReaderPlugIn>) aClass;
            } else {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] must be derived from [{2}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  resString,
                                                  ProductReaderPlugIn.class.getName());
                throw new CoreException(msg);
            }

            try {
                readerPlugIn = readerPluginClass.newInstance();
            } catch (Exception e) {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] could not be instantiated",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  readerPluginClass.getName());
                throw new CoreException(msg);

            }
        } else {
            // if readerPlugin not specified, try to find it by formatName
            Iterator iter = ProductIOPlugInManager.getInstance().getReaderPlugIns(getFormatName());
            if (iter.hasNext()) {
                readerPlugIn = (ProductReaderPlugIn) iter.next();
            } else {
                readerPlugIn = null;
                BeamLogManager.getSystemLogger().severe(
                        "no reader plug-in installed for products of type '" + getFormatName() + "'");
            }
        }
    }

    private String getFormatName() {
        return formatName;
    }

    private void setFormatName(String formatName) {
        this.formatName = formatName;
        lastDirKey = "user." + formatName.toLowerCase() + ".import.dir";
    }

    protected void importProduct() {

        VisatApp visatApp = VisatApp.getApp();
        memWarnLimit = visatApp.getPreferences().getPropertyInt(VisatApp.PROPERTY_KEY_BIG_PRODUCT_SIZE, 100);

        if (readerPlugIn == null) {
            // Should not come here...
            return;
        }

        File selectedFile = promptForFile();
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


    protected File promptForFile() {

        File currentDir = null;
        VisatApp visatApp = VisatApp.getApp();
        String currentDirPath = visatApp.getPreferences().getPropertyString(lastDirKey,
                                                                            SystemUtils.getUserHomeDir().getPath());
        if (currentDirPath != null) {
            currentDir = new File(currentDirPath);
        }
        if (fileChooser == null) {
            fileChooser = new ProductFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(useAllFileFilter);
            BeamFileFilter fileFilter = readerPlugIn.getProductFileFilter();
            if (fileFilter != null) {
                fileChooser.setFileFilter(fileFilter);
            }
            HelpSys.enableHelpKey(fileChooser, getHelpId());
        }
        fileChooser.setCurrentDirectory(currentDir);

        File file = null;
        boolean canceled = false;
        while (file == null && !canceled) {
            int result = fileChooser.showOpenDialog(visatApp.getMainFrame());
            file = fileChooser.getSelectedFile();
            if (file != null && file.getParent() != null) {
                visatApp.getPreferences().setPropertyString(lastDirKey, file.getParent());
            }
            if (result == JFileChooser.APPROVE_OPTION) {
                if (file != null && !file.getName().trim().equals("")) {
                    if (!file.exists()) {
                        visatApp.showErrorDialog("File not found:\n" + file.getPath());
                        file = null;
                    } else {

                        double fileSize = file.length() / (1024.0 * 1024.0);
                        if (fileSize == 0.0) {
                            visatApp.showErrorDialog("File is empty:\n" + file.getPath());
                            file = null;
                        } else if (isFileOfFormat(file, DimapProductConstants.DIMAP_FORMAT_NAME)
                                   && !getFormatName().equals(DimapProductConstants.DIMAP_FORMAT_NAME)) {
                            visatApp.showInfoDialog(
                                    "The selected file\n"
                                    + "'" + file.getPath() + "'\n"
                                    + "appears to be a BEAM-DIMAP product.\n\n"
                                    + "Please use 'Open' in the file menu to open such product types.\n"
                                    , null);
                            file = null;
                        } else if (fileChooser.getSubsetProduct() == null
                                   && memWarnLimit >= 0.0
                                   && fileSize > memWarnLimit) {
                            int status = visatApp.showQuestionDialog("\"Big\" Product",
                                                                     "The product size exceeds " + memWarnLimit + " M.\n"
                                                                     + "You can reduce the amount of memory by\n"
                                                                     + "specifying a subset for this product.\n\n"
                                                                     + "Do you wish to open the subset dialog now?\n",
                                                                     true, null);
                            if (status == JOptionPane.YES_OPTION) {
                                fileChooser.openProductSubsetDialog();
                                if (fileChooser.getSubsetProduct() == null) {
                                    canceled = true;
                                }
                            } else if (status == JOptionPane.CANCEL_OPTION) {
                                canceled = true;
                            }
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
        VisatApp visatApp = VisatApp.getApp();
        try {
            visatApp.setStatusBarMessage("Reading from '" + file + "'..."); /*I18N*/
            visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            if (readerPlugIn.getDecodeQualification(file) != DecodeQualification.UNABLE) {
                final ProductReader reader = readerPlugIn.createReaderInstance();
                product = reader.readProductNodes(file, null);
            } else {
                visatApp.showWarningDialog("Cannot decode the selected product file\n" +
                                           "'" + file.getPath() + "'");
            }
            visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
            visatApp.clearStatusBarMessage();
        } catch (Exception e) {
            visatApp.handleUnknownException(e);
        }

        return product;
    }

    protected boolean isFileOfFormat(File file, String format) {
        ProductIOPlugInManager manager = ProductIOPlugInManager.getInstance();
        Iterator it = manager.getReaderPlugIns(format);
        if (it.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) it.next();
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

        protected void createUI() {

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
                ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(mainFrame, product, memWarnLimit);
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
