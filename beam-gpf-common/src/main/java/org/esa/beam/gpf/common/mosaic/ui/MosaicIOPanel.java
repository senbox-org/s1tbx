package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.framework.ui.io.FileArrayEditor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicIOPanel extends JPanel {

    private static final String INPUT_PRODUCT_DIR_KEY = "gpf.mosaic.input.product.dir";

    private final AppContext appContext;
    private final PropertyContainer properties;
    private final TargetProductSelector targetProductSelector;
    private final SourceProductSelector updateProductSelector;
    private FileArrayEditor sourceProductEditor;
    private JButton addFileButton;
    private JButton removeFileButton;

    MosaicIOPanel(AppContext appContext, PropertyContainer container, TargetProductSelector selector) {
        this.appContext = appContext;
        properties = container;
        targetProductSelector = selector;
        updateProductSelector = new SourceProductSelector(appContext);
        init();
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
        updateProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                properties.setValue(MosaicFormModel.PROPERTY_UPDATE_PRODUCT, event.getSelection().getFirstElement());
            }
        });
    }

    private JPanel createSourceProductsPanel() {
        final FileArrayEditor.EditorParent context = new FileArrayEditorContext(appContext);
        sourceProductEditor = new FileArrayEditor(context, "Source products");
        final FileArrayEditor.FileArrayEditorListener listener = new FileArrayEditor.FileArrayEditorListener() {
            @Override
            public void updatedList(final File[] files) {
                properties.setValue(MosaicFormModel.PROPERTY_SOURCE_PRODUCT_FILES, files);
            }
        };
        sourceProductEditor.setListener(listener);


        addFileButton = sourceProductEditor.createAddFileButton();
        removeFileButton = sourceProductEditor.createRemoveFileButton();
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

        final JComponent fileArrayComponent = sourceProductEditor.createFileArrayComponent();
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
        final BindingContext context = new BindingContext(properties);
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
                    cards.show(subPanel, updateProductKey);
                } else {
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
            applicationContext.getPreferences().setPropertyString(INPUT_PRODUCT_DIR_KEY, currentDirectory.getAbsolutePath());
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

}
