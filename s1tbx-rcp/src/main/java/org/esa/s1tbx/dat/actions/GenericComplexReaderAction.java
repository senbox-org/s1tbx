package org.esa.s1tbx.dat.actions;

import org.esa.s1tbx.dataio.generic.GenericComplexReaderPlugIn;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNodeList;
import org.esa.snap.framework.help.HelpSys;
import org.esa.snap.framework.ui.GridBagUtils;
import org.esa.snap.framework.ui.NewProductDialog;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.framework.ui.product.ProductSubsetDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.Debug;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.BeamFileChooser;
import org.esa.snap.util.io.BeamFileFilter;
import org.esa.snap.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;

/**
 * User: pmar@ppolabs.com
 * Date: 2/23/12
 * Time: 2:42 PM
 */
public class GenericComplexReaderAction extends ExecCommand {

    private ProductReaderPlugIn readerPlugIn = null;
    private ProductFileChooser fileChooser = null;
    private final String formatName = "Generic Complex Binary";
    private String lastDirKey = null;
    private final boolean useAllFileFilter = true;
    private final boolean useFilesAndFolders = false;

    @Override
    public void actionPerformed(final CommandEvent event) {

        readerPlugIn = new GenericComplexReaderPlugIn();

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
                SnapDialogs.showError("The product is already open.\n" + "A product can only be opened once.");
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
        final String currentDirPath = SnapApp.getDefault().getPreferences().get(lastDirKey,
                                                                                SystemUtils.getUserHomeDir().getPath());
        if (currentDirPath != null) {
            currentDir = new File(currentDirPath);
        }
        if (fileChooser == null) {
            fileChooser = new ProductFileChooser();
            if (useFilesAndFolders)
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setAcceptAllFileFilterUsed(useAllFileFilter);
            if (!useFilesAndFolders) {
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
            final int result = fileChooser.showOpenDialog(SnapApp.getDefault().getMainFrame());
            file = fileChooser.getSelectedFile();
            if (file != null && file.getParent() != null) {
                SnapApp.getDefault().getPreferences().put(lastDirKey, file.getParent());
            }
            if (result == JFileChooser.APPROVE_OPTION) {
                if (file != null && file.getName().trim().length() != 0) {
                    if (useFilesAndFolders && file.isDirectory()) {
                        return file;
                    }
                    if (!file.exists()) {
                        SnapDialogs.showError("File not found:\n" + file.getPath());
                        file = null;
                    } else {

                        final double fileSize = file.length() / (1024.0 * 1024.0);
                        if (fileSize == 0.0) {
                            SnapDialogs.showError("File is empty:\n" + file.getPath());
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
            SnapApp.getDefault().getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            final ProductReader reader = readerPlugIn.createReaderInstance();
            product = reader.readProductNodes(file, null);

            SnapApp.getDefault().getMainFrame().setCursor(Cursor.getDefaultCursor());
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
                ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(SnapApp.getDefault().getMainFrame(), product);
                if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                    ProductNodeList<Product> products = new ProductNodeList<Product>();
                    products.add(product);
                    NewProductDialog newProductDialog = new NewProductDialog(SnapApp.getDefault().getMainFrame(), products, 0,
                            true);
                    newProductDialog.setSubsetDef(productSubsetDialog.getProductSubsetDef());
                    if (newProductDialog.show() == NewProductDialog.ID_OK) {
                        _subsetProduct = newProductDialog.getResultProduct();
                        approve = _subsetProduct != null;
                        if (!approve && newProductDialog.getException() != null) {
                            SnapDialogs.showError("The product subset could not be created:\n" +
                                                          newProductDialog.getException().getMessage());
                        }
                    }
                }
            }
            return approve;
        }
    }


}
