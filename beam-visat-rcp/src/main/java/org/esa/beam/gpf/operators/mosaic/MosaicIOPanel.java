package org.esa.beam.gpf.operators.mosaic;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.io.FileArrayEditor;
import org.esa.beam.gpf.operators.standard.MosaicOp;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicIOPanel extends JPanel {

    private static final String INPUT_PRODUCT_DIR_KEY = "gpf.mosaic.input.product.dir";

    private final AppContext appContext;
    private final MosaicFormModel mosaicModel;
    private final PropertyContainer propertyContainer;
    private final TargetProductSelector targetProductSelector;
    private final SourceProductSelector updateProductSelector;
    private FileArrayEditor sourceFileEditor;

    MosaicIOPanel(AppContext appContext, MosaicFormModel mosaicModel, TargetProductSelector selector) {
        this.appContext = appContext;
        this.mosaicModel = mosaicModel;
        propertyContainer = mosaicModel.getPropertyContainer();
        sourceFileEditor = new ProductArrayEditor(new FileArrayEditorContext(appContext));
        targetProductSelector = selector;
        updateProductSelector = new SourceProductSelector(appContext);
        updateProductSelector.setProductFilter(new UpdateProductFilter());
        init();
        propertyContainer.addPropertyChangeListener(MosaicFormModel.PROPERTY_UPDATE_MODE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (Boolean.TRUE.equals(evt.getNewValue())) {
                    propertyContainer.setValue(MosaicFormModel.PROPERTY_UPDATE_PRODUCT,
                                               updateProductSelector.getSelectedProduct());
                } else {
                    updateProductSelector.setSelectedProduct(null);
                }
            }
        });
        propertyContainer.addPropertyChangeListener(MosaicFormModel.PROPERTY_UPDATE_PRODUCT,
                                                    new TargetProductSelectorUpdater());

    }

    private void init() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTablePadding(3, 3);
        setLayout(tableLayout);
        tableLayout.setRowWeightY(0, 1.0);
        add(createSourceProductsPanel());
        add(createTargetProductPanel());
        updateProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product product = (Product) event.getSelection().getSelectedValue();
                try {
                    if (product != null) {
                        final Map<String, Object> map = MosaicOp.getOperatorParameters(product);
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            if (propertyContainer.getProperty(entry.getKey()) != null) {
                                propertyContainer.setValue(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    propertyContainer.setValue(MosaicFormModel.PROPERTY_UPDATE_PRODUCT, product);
                } catch (OperatorException e) {
                    appContext.handleError("Selected product cannot be used for update mode.", e);
                }
            }
        });
    }

    private JPanel createSourceProductsPanel() {
        final FileArrayEditor.FileArrayEditorListener listener = new FileArrayEditor.FileArrayEditorListener() {
            @Override
            public void updatedList(final File[] files) {
                final SwingWorker worker = new SwingWorker() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        mosaicModel.setSourceProducts(files);
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
        };
        sourceFileEditor.setListener(listener);


        JButton addFileButton = sourceFileEditor.createAddFileButton();
        JButton removeFileButton = sourceFileEditor.createRemoveFileButton();
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);

        final JPanel sourceProductPanel = new JPanel(tableLayout);
        sourceProductPanel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addFileButton);
        buttonPanel.add(removeFileButton);
        tableLayout.setRowPadding(0, new Insets(1, 4, 1, 4));
        sourceProductPanel.add(buttonPanel);

        final JComponent fileArrayComponent = sourceFileEditor.createFileArrayComponent();
        tableLayout.setRowWeightY(1, 1.0);
        sourceProductPanel.add(fileArrayComponent);


        return sourceProductPanel;
    }

    private JPanel createTargetProductPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setTablePadding(3, 3);

        final JPanel targetProductPanel = new JPanel(tableLayout);
        targetProductPanel.setBorder(BorderFactory.createTitledBorder("Target Product"));
        final JCheckBox updateTargetCheckBox = new JCheckBox("Update target product", false);
        final BindingContext context = new BindingContext(propertyContainer);
        context.bind(MosaicFormModel.PROPERTY_UPDATE_MODE, updateTargetCheckBox);
        targetProductPanel.add(updateTargetCheckBox);

        final CardLayout cards = new CardLayout(0, 3);
        final JPanel subPanel = new JPanel(cards);
        final JPanel newProductSelectorPanel = createTargetProductSelectorPanel(targetProductSelector);
        final JPanel updateProductSelectorPanel = createUpdateProductSelectorPanel(updateProductSelector);
        final String newProductKey = "NEW_PRODUCT";
        final String updateProductKey = "UPDATE_PRODUCT";
        subPanel.add(newProductSelectorPanel, newProductKey);
        subPanel.add(updateProductSelectorPanel, updateProductKey);
        updateTargetCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updateTargetCheckBox.isSelected()) {
                    prepareHideTargetProductSelector();
                    prepareShowUpdateProductSelector();
                    cards.show(subPanel, updateProductKey);
                } else {
                    prepareShowTargetProductSelector();
                    prepareHideUpdateProductSelector();
                    cards.show(subPanel, newProductKey);
                }
            }
        });
        cards.show(subPanel, newProductKey);
        targetProductPanel.add(subPanel);
        targetProductPanel.add(targetProductSelector.getOpenInAppCheckBox());
        return targetProductPanel;
    }

    private JPanel createUpdateProductSelectorPanel(final SourceProductSelector selector) {
        final JPanel subPanel = new JPanel(new BorderLayout(3, 3));
        subPanel.add(selector.getProductNameComboBox(), BorderLayout.CENTER);
        subPanel.add(selector.getProductFileChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowFill(0, TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(1, TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);
        JPanel panel = new JPanel(tableLayout);
        panel.add(selector.getProductNameLabel());
        panel.add(subPanel);
        panel.add(tableLayout.createVerticalSpacer());
        return panel;
    }

    private static JPanel createTargetProductSelectorPanel(final TargetProductSelector selector) {
        final JPanel subPanel1 = new JPanel(new BorderLayout(3, 3));
        subPanel1.add(selector.getProductNameLabel(), BorderLayout.NORTH);
        subPanel1.add(selector.getProductNameTextField(), BorderLayout.CENTER);

        final JPanel subPanel2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        subPanel2.add(selector.getSaveToFileCheckBox());
        subPanel2.add(selector.getFormatNameComboBox());

        final JPanel subPanel3 = new JPanel(new BorderLayout(3, 3));
        subPanel3.add(selector.getProductDirLabel(), BorderLayout.NORTH);
        subPanel3.add(selector.getProductDirTextField(), BorderLayout.CENTER);
        subPanel3.add(selector.getProductDirChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);

        tableLayout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(1, 0, new Insets(3, 0, 3, 3));
        tableLayout.setCellPadding(2, 0, new Insets(0, 21, 3, 3));

        final JPanel panel = new JPanel(tableLayout);
        panel.add(subPanel1);
        panel.add(subPanel2);
        panel.add(subPanel3);

        return panel;
    }

    void prepareShow() {
        prepareShowTargetProductSelector();
        if (mosaicModel.isUpdateMode()) {
            prepareShowUpdateProductSelector();
        }
    }

    private void prepareShowUpdateProductSelector() {
        updateProductSelector.initProducts();
    }

    private void prepareShowTargetProductSelector() {
        try {
            final List<File> files = sourceFileEditor.getFiles();
            mosaicModel.setSourceProducts(files.toArray(new File[files.size()]));
        } catch (IOException ignore) {
        }
    }

    void prepareHide() {
        prepareHideUpdateProductSelector();
        prepareHideTargetProductSelector();
    }

    private void prepareHideUpdateProductSelector() {
        updateProductSelector.releaseProducts();
    }

    private void prepareHideTargetProductSelector() {
        try {
            mosaicModel.setSourceProducts(new File[0]);
        } catch (IOException ignore) {
        }
    }

    private static class FileArrayEditorContext implements FileArrayEditor.EditorParent {

        private final AppContext applicationContext;

        private FileArrayEditorContext(AppContext applicationContext) {

            this.applicationContext = applicationContext;
        }

        @Override
        public File getUserInputDir() {
            return getInputProductDir();
        }

        @Override
        public void setUserInputDir(File newDir) {
            setInputProductDir(newDir);
        }

        private void setInputProductDir(final File currentDirectory) {
            applicationContext.getPreferences().setPropertyString(INPUT_PRODUCT_DIR_KEY,
                                                                  currentDirectory.getAbsolutePath());
        }

        private File getInputProductDir() {
            final String path = applicationContext.getPreferences().getPropertyString(INPUT_PRODUCT_DIR_KEY);
            final File inputProductDir;
            if (path != null) {
                inputProductDir = new File(path);
            } else {
                inputProductDir = null;
            }
            return inputProductDir;
        }

    }

    private static class ProductArrayEditor extends FileArrayEditor {

        private ProductArrayEditor(EditorParent context) {
            super(context, "Source products");
        }

        @Override
        protected JFileChooser createFileChooserDialog() {
            BeamFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setDialogTitle("Mosaic - Open Source Product(s)"); /*I18N*/
            fileChooser.setMultiSelectionEnabled(true);

            Iterator allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            while (allReaderPlugIns.hasNext()) {
                final ProductIOPlugIn plugIn = (ProductIOPlugIn) allReaderPlugIns.next();
                BeamFileFilter productFileFilter = plugIn.getProductFileFilter();
                fileChooser.addChoosableFileFilter(productFileFilter);
            }
            FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
            fileChooser.setFileFilter(actualFileFilter);

            return fileChooser;
        }
    }

    private class TargetProductSelectorUpdater implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final Product product = (Product) evt.getNewValue();
            final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
            if (product != null) {
                final String formatName = product.getProductReader().getReaderPlugIn().getFormatNames()[0];
                final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
                final Iterator<ProductWriterPlugIn> writerIterator = ioPlugInManager.getWriterPlugIns(formatName);
                if (writerIterator.hasNext()) {
                    selectorModel.setFormatName(formatName);
                } else {
                    final String errMsg = "Cannot write to update product.";
                    final String iseMsg = String.format("No product writer found for format '%s'", formatName);
                    appContext.handleError(errMsg, new IllegalStateException(iseMsg));
                }
                final File fileLocation = product.getFileLocation();
                final String fileName = FileUtils.getFilenameWithoutExtension(fileLocation);
                final File fileDir = fileLocation.getParentFile();
                selectorModel.setProductName(fileName);
                selectorModel.setProductDir(fileDir);
            } else {
                selectorModel.setFormatName(ProductIO.DEFAULT_FORMAT_NAME);
                selectorModel.setProductName("mosaic");
                String homeDirPath = SystemUtils.getUserHomeDir().getPath();
                final PropertyMap prefs = appContext.getPreferences();
                String saveDir = prefs.getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, homeDirPath);
                selectorModel.setProductDir(new File(saveDir));
            }
        }
    }
    
    public class UpdateProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            ProductReader productReader = product.getProductReader();
            final String formatName = productReader.getReaderPlugIn().getFormatNames()[0];
            final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
            final Iterator<ProductWriterPlugIn> writerIterator = ioPlugInManager.getWriterPlugIns(formatName);
            if (writerIterator.hasNext()) {
                try {
                    final Map<String, Object> map = MosaicOp.getOperatorParameters(product);
                } catch (OperatorException e) {
                    return false;
                }
                return true;
            }
            return false;
        }

    }


}
