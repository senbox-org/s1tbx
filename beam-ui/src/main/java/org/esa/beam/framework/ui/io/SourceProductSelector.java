package org.esa.beam.framework.ui.io;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.TableLayout;
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
    private Action chooserAction;


    public SourceProductSelector(Product[] defaultProducts, String labelText) {
        this(defaultProducts, labelText, ".*");
    }

    public SourceProductSelector(Product[] defaultProducts, String labelText, String productTypePattern) {
        this.defaultProducts = defaultProducts;
        this.labelText = labelText;
        this.typePattern = Pattern.compile(productTypePattern);
        productListModel = new DefaultComboBoxModel();

        for (Product product : defaultProducts) {
            if (this.typePattern.matcher(product.getProductType()).matches()) {
                productListModel.addElement(product);
            }
        }

        chooserAction = new ChooserAction();
    }

    public Product[] getDefaultProducts() {
        return defaultProducts;
    }

    public String getLabelText() {
        return labelText;
    }

    public DefaultComboBoxModel getProductListModel() {
        return productListModel;
    }

    public Product getSelectedProduct() {
        return (Product) productListModel.getSelectedItem();
    }

    private void setSelectedProduct(Product product) throws Exception {
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

    public Pattern getTypePattern() {
        return typePattern;
    }

    public void dispose() {
        if (chooserProduct != null && getSelectedProduct() != chooserProduct) {
            chooserProduct.dispose();
        }
    }

    // UI Components

    /////////////////////////////////////

    public JComboBox createComboBox() {
        final JComboBox comboBox = new JComboBox(productListModel);
        comboBox.setPrototypeDisplayValue("[1] 123456789 123456789 12345");
        comboBox.setRenderer(new ProductListCellRenderer());

        return comboBox;
    }

    public JLabel createLabel() {
        return new JLabel(labelText);
    }

    public JButton createButton() {
        return new JButton(chooserAction);
    }

    public JPanel createDefaultPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setTablePadding(2, 2);

        final JPanel panel = new JPanel(layout);
        panel.add(createLabel());
        panel.add(createComboBox());
        panel.add(createButton());

        return panel;
    }

    private boolean productListModelContains(Product product) {
        for(int i = 0; i < productListModel.getSize(); i++) {
            if(productListModel.getElementAt(i).equals(product)) {
                return true;
            }
        }
        return false;
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

    private class ChooserAction extends AbstractAction {

        private String APPROVE_BUTTON_TEXT = "Select";

        public ChooserAction() {
            super("...");
        }

        public void actionPerformed(ActionEvent event) {
            final BeamFileChooser fileChooser = new BeamFileChooser();
            final Iterator<ProductReaderPlugIn> iterator = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

            while (iterator.hasNext()) {
                fileChooser.addChoosableFileFilter(iterator.next().getProductFileFilter());
            }
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());

            Component parent = null;
            if (event.getSource() instanceof Component) {
                parent = (Component) event.getSource();
            }

            if (fileChooser.showDialog(parent, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();

                Product product = null;
                try {
                    product = ProductIO.readProduct(file, null);
                    setSelectedProduct(product);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(parent,
                                                  MessageFormat.format("An error occurred while reading file ''{0}''.",
                                                                       file.getPath()),
                                                  "I/O Error",
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    if (product != null) {
                        product.dispose();
                    }
                    JOptionPane.showMessageDialog(parent, e.getMessage(), "Illegal Product Type",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }

        }
    }
}
