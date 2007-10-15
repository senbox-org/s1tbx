package org.esa.beam.framework.ui.io;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.BeamFileChooser;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SourceProductSelector {

    private Product[] defaultProducts;
    private Pattern typePattern;
    private Product chooserProduct;
    private String labelText;
    private DefaultComboBoxModel productListModel;
    private JLabel productLabel;
    private JButton chooserButton;
    private JComboBox productComboBox;


    public SourceProductSelector(Product[] products, String productLabelText) {
        this(products, productLabelText, ".*");
    }

    public SourceProductSelector(Product[] products, String productLabelText, String productTypePattern) {
        defaultProducts = products;
        labelText = productLabelText.trim();
        if (!labelText.endsWith(":")) {
            labelText = labelText.concat(":");
        }

        typePattern = Pattern.compile(productTypePattern);
        productListModel = new DefaultComboBoxModel();

        for (Product product : products) {
            if (typePattern.matcher(product.getProductType()).matches()) {
                productListModel.addElement(product);
            }
        }
        productListModel.setSelectedItem(null);

        productLabel = new JLabel(labelText);
        chooserButton = new JButton(new ChooserAction());
        productComboBox = new JComboBox(productListModel);
        productComboBox.setPrototypeDisplayValue("[1] 123456789 123456789 12345");
        productComboBox.setRenderer(new ProductListCellRenderer());
    }

    public Product[] getDefaultProducts() {
        return defaultProducts;
    }

    public String getLabelText() {
        return labelText;
    }

    public Pattern getTypePattern() {
        return typePattern;
    }

    public DefaultComboBoxModel getProductListModel() {
        return productListModel;
    }

    public Product getSelectedProduct() {
        return (Product) productListModel.getSelectedItem();
    }
    
    public void selectFirstMatchingProduct() {
        int size = productListModel.getSize();
        if (size > 0) {
            productListModel.setSelectedItem(productListModel.getElementAt(0));
        }
    }

    void setSelectedProduct(Product product) throws Exception {
        if (typePattern.matcher(product.getProductType()).matches()) {
            if (productListModelContains(product)) {
                productListModel.setSelectedItem(product);
            } else {
                if (chooserProduct != null) {
                    productListModel.removeElement(chooserProduct);
                    chooserProduct.dispose();
                }
                productListModel.addElement(product);
                productListModel.setSelectedItem(product);
                chooserProduct = product;
            }
        } else {
            throw new Exception(MessageFormat.format("Product ''{0}'' is not of appropriate type.", product.getName()));
        }
    }

    public void dispose() {
        if (chooserProduct != null && getSelectedProduct() != chooserProduct) {
            chooserProduct.dispose();
        }
    }

    // UI Components

    /////////////////////////////////////

    public JComboBox getComboBox() {
        return productComboBox;
    }

    public JLabel getLabel() {
        return productLabel;
    }

    public JButton getButton() {
        return chooserButton;
    }

    private boolean productListModelContains(Product product) {
        for (int i = 0; i < productListModel.getSize(); i++) {
            if (productListModel.getElementAt(i).equals(product)) {
                return true;
            }
        }
        return false;
    }

    private class ChooserAction extends AbstractAction {

        private String APPROVE_BUTTON_TEXT = "Select";
        private JFileChooser chooser;
        private ErrorHandler errorHandler;

        public ChooserAction() {
            this(new BeamFileChooser(), new ErrorHandler() {
                public void handleError(final JComponent component, final Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(component, t.getMessage(), "Error",
                                                          JOptionPane.ERROR_MESSAGE);
                        }
                    });

                }
            });

            final Iterator<ProductReaderPlugIn> iterator = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

            while (iterator.hasNext()) {
                chooser.addChoosableFileFilter(iterator.next().getProductFileFilter());
            }
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileFilter(chooser.getAcceptAllFileFilter());
        }

        private ChooserAction(JFileChooser fileChooser, ErrorHandler errorHandler) {
            super("...");
            chooser = fileChooser;
            this.errorHandler = errorHandler;
        }

        public void actionPerformed(ActionEvent event) {
            JComponent parent = null;
            if (event.getSource() instanceof JComponent) {
                parent = (JComponent) event.getSource();
            }
            if (chooser.showDialog(parent, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
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
                    errorHandler.handleError(parent, e);
                } catch (Exception e) {
                    if (product != null) {
                        product.dispose();
                    }
                    errorHandler.handleError(parent, e);
                }
            }
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

    private interface ErrorHandler {

        void handleError(JComponent component, Throwable t);
    }

    
}
