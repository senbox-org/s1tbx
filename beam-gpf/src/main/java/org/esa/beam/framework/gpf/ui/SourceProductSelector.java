package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.util.io.BeamFileChooser;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SourceProductSelector {

    private Pattern typePattern;
    private Product extraProduct;
    private File currentDirectory;
    private DefaultComboBoxModel productListModel;
    private JLabel productNameLabel;
    private JButton productFileChooserButton;
    private JComboBox productNameComboBox;

    public SourceProductSelector(Product[] selectableProducts, String labelText) {
        this(selectableProducts, labelText, ".*");
    }

    public SourceProductSelector(Product[] selectableProducts, String labelText, String typePattern) {
        this.typePattern = Pattern.compile(typePattern);
        productListModel = new DefaultComboBoxModel();

        for (Product product : selectableProducts) {
            if (this.typePattern.matcher(product.getProductType()).matches()) {
                productListModel.addElement(product);
            }
        }
        productListModel.setSelectedItem(null);

        productNameLabel = new JLabel(labelText);
        productFileChooserButton = new JButton(new ProductFileChooserAction());
        final Dimension size = new Dimension(26, 16);
        productFileChooserButton.setPreferredSize(size);
        productFileChooserButton.setMinimumSize(size);

        productNameComboBox = new JComboBox(productListModel);
        productNameComboBox.setPrototypeDisplayValue("[1] 123456789 123456789 12345");
        productNameComboBox.setRenderer(new ProductListCellRenderer());
        productNameComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final Product product = (Product) productNameComboBox.getSelectedItem();
                if (product != null) {
                    if (product.getFileLocation() != null) {
                        productNameComboBox.setToolTipText(product.getFileLocation().getPath());
                    } else {
                        productNameComboBox.setToolTipText(product.getDisplayName());
                    }
                } else {
                    productNameComboBox.setToolTipText("Selects a source product.");
                }
            }
        });
    }

    public Pattern getTypePattern() {
        return typePattern;
    }

    public int getProductCount() {
        return productListModel.getSize();
    }

    public void setSelectedIndex(int index) {
        productListModel.setSelectedItem(productListModel.getElementAt(index));
    }

    public Product getSelectedProduct() {
        return (Product) productListModel.getSelectedItem();
    }

    public void setCurrentDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            currentDirectory = directory;
        }
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    void setSelectedProduct(Product product) throws Exception {
        if (typePattern.matcher(product.getProductType()).matches()) {
            if (productListModelContains(product)) {
                productListModel.setSelectedItem(product);
            } else {
                if (extraProduct != null) {
                    productListModel.removeElement(extraProduct);
                    extraProduct.dispose();
                }
                productListModel.addElement(product);
                productListModel.setSelectedItem(product);
                extraProduct = product;
            }
        } else {
            throw new Exception(MessageFormat.format("Product ''{0}'' is not of appropriate type.", product.getName()));
        }
    }

    public void dispose() {
        if (extraProduct != null && getSelectedProduct() != extraProduct) {
            extraProduct.dispose();
        }
    }

    // UI Components

    /////////////////////////////////////

    public JComboBox getProductNameComboBox() {
        return productNameComboBox;
    }

    public JLabel getProductNameLabel() {
        return productNameLabel;
    }

    public JButton getProductFileChooserButton() {
        return productFileChooserButton;
    }

    private boolean productListModelContains(Product product) {
        for (int i = 0; i < productListModel.getSize(); i++) {
            if (productListModel.getElementAt(i).equals(product)) {
                return true;
            }
        }
        return false;
    }

    public JPanel createDefaultPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setTablePadding(3, 3);

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Source"));

        panel.add(getProductNameLabel());
        panel.add(getProductNameComboBox());
        panel.add(getProductFileChooserButton());

        return panel;
    }

    private class ProductFileChooserAction extends AbstractAction {

        private String APPROVE_BUTTON_TEXT = "Select";
        private JFileChooser chooser;

        public ProductFileChooserAction() {
            super("...");
            chooser = new BeamFileChooser();
            chooser.setDialogTitle("Select Source Product");
            final Iterator<ProductReaderPlugIn> iterator = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            while (iterator.hasNext()) {
                chooser.addChoosableFileFilter(iterator.next().getProductFileFilter());
            }
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileFilter(chooser.getAcceptAllFileFilter());
        }

        public void actionPerformed(ActionEvent event) {
            final Window window = SwingUtilities.getWindowAncestor((JComponent) event.getSource());

            chooser.setCurrentDirectory(currentDirectory);

            if (chooser.showDialog(window, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                final File file = chooser.getSelectedFile();

                Product product = null;
                try {
                    product = ProductIO.readProduct(file, null);
                    if (product == null) {
                        throw new IOException(
                                MessageFormat.format("File ''{0}'' is not of appropriate type.", file.getPath()));
                    }
                    setSelectedProduct(product);
                } catch (IOException e) {
                    handleError(window, e);
                } catch (Exception e) {
                    if (product != null) {
                        product.dispose();
                    }
                    handleError(window, e);
                }
                currentDirectory = chooser.getCurrentDirectory();
            }
        }

        private void handleError(final Component component, final Throwable t) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(component, t.getMessage(), "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private static class ProductListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            final Component cellRendererComponent =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (cellRendererComponent instanceof JLabel && value instanceof Product) {
                final JLabel label = (JLabel) cellRendererComponent;
                final Product product = (Product) value;
                label.setText(product.getDisplayName());
            }

            return cellRendererComponent;
        }
    }

}
