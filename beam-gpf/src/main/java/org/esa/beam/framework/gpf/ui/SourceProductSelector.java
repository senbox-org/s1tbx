package org.esa.beam.framework.gpf.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SourceProductSelector {

    private AppContext appContext;
    private Product extraProduct;
    private File currentDirectory;
    private DefaultComboBoxModel productListModel;
    private JLabel productNameLabel;
    private JButton productFileChooserButton;
    private JComboBox productNameComboBox;

    public SourceProductSelector(AppContext appContext) {
        this(appContext, "Name:");
    }

    public SourceProductSelector(AppContext appContext, String labelText) {
        this.appContext = appContext;
        productListModel = new DefaultComboBoxModel();

        productNameLabel = new JLabel(labelText);
        productFileChooserButton = new JButton(new ProductFileChooserAction());
        final Dimension size = new Dimension(26, 16);
        productFileChooserButton.setPreferredSize(size);
        productFileChooserButton.setMinimumSize(size);

        productNameComboBox = new JComboBox(productListModel);
        productNameComboBox.setPrototypeDisplayValue("[1] 123456789 123456789 12345");
        productNameComboBox.setRenderer(new ProductListCellRenderer());
        productNameComboBox.addPopupMenuListener(new ProductPopupMenuListener());
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

    public void initProducts() {
        productListModel.removeAllElements();
        for (Product product : appContext.getProducts()) {
            productListModel.addElement(product);
        }
        productListModel.setSelectedItem(appContext.getSelectedProduct());
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

    public void setSelectedProduct(Product product) {
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
    }

    public void releaseProducts() {
        if (extraProduct != null && getSelectedProduct() != extraProduct) {
            extraProduct.dispose();
        }
        extraProduct = null;
        productListModel.removeAllElements();
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
        final JPanel subPanel = new JPanel(new BorderLayout(3, 3));
        subPanel.add(getProductNameComboBox(), BorderLayout.CENTER);
        subPanel.add(getProductFileChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowFill(0, TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(1, TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);
        JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Source Product"));
        panel.add(getProductNameLabel());
        panel.add(subPanel);
        panel.add(tableLayout.createVerticalSpacer());
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

            String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            String openDir = appContext.getPreferences().getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, homeDirPath);
            currentDirectory = new File(openDir);
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
                appContext.getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, currentDirectory.getAbsolutePath());
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
    
    /**
     * To let the popup menu be wider than the closed combobox.
     * Adapted an idea from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6257236
     */
    private static class ProductPopupMenuListener  implements PopupMenuListener {
        
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            JComboBox box = (JComboBox) e.getSource();
            Object comp = box.getUI().getAccessibleChild(box, 0);
            if (!(comp instanceof JPopupMenu))
                return;
            JComponent scrollPane = (JComponent) ((JPopupMenu) comp)
                    .getComponent(0);
            Dimension size = new Dimension();
            size.width = scrollPane.getPreferredSize().width;
            final int boxItemCount = box.getModel().getSize();
            for (int i = 0; i < boxItemCount; i++) {
                Product product = (Product) box.getModel().getElementAt(i);
                final JLabel label = new JLabel();
                label.setText(product.getDisplayName());
                size.width = Math.max(label.getPreferredSize().width, size.width);
            }
            size.height = scrollPane.getPreferredSize().height;
            scrollPane.setPreferredSize(size);
            scrollPane.setMaximumSize(size);
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }
    }

}
