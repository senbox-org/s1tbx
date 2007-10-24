package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.io.SourceProductSelector;
import org.esa.beam.framework.ui.io.TargetProductSelector;

import javax.swing.*;
import java.awt.Insets;

/**
 * Form for geographic collocation dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationForm extends JPanel {

    private CollocationFormModel model;

    private SourceProductSelector referenceProductSelector;
    private SourceProductSelector subsidiaryProductSelector;

    private JCheckBox renameReferenceComponentsCheckBox;
    private JCheckBox renameSubsidiaryComponentsCheckBox;
    private JTextField referenceComponentPatternField;
    private JTextField subsidiaryComponentPatternField;
    private JComboBox resamplingComboBox;
    private JCheckBox createNewProductCheckBox;

    public CollocationForm(CollocationFormModel model, Product[] selectableProducts) {
        this.model = model;

        referenceProductSelector = createProductSelector(selectableProducts, "Reference product", 0);
        subsidiaryProductSelector = createProductSelector(selectableProducts, "Subsidiary product", 1);
        createNewProductCheckBox = new JCheckBox("");
        renameReferenceComponentsCheckBox = new JCheckBox("Rename reference components:");
        renameSubsidiaryComponentsCheckBox = new JCheckBox("Rename subsidiary components:");
        referenceComponentPatternField = new JTextField();
        subsidiaryComponentPatternField = new JTextField();
        resamplingComboBox = new JComboBox(model.getResamplings());

        initComponents();
        bindComponents();
    }

    public void dispose() {
        referenceProductSelector.dispose();
        subsidiaryProductSelector.dispose();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createInputPanel());
        add(createOutputPanel());
        add(createRenamingPanel());
        add(createResamplingPanel());
    }

    private void bindComponents() {
        final SwingBindingContext sbc = new SwingBindingContext(model.getValueContainer());

        sbc.bind(referenceProductSelector.getComboBox(), "masterProduct");
        sbc.bind(subsidiaryProductSelector.getComboBox(), "slaveProduct");
        sbc.bind(createNewProductCheckBox, "createNewProduct");
        sbc.bind(renameReferenceComponentsCheckBox, "renameMasterComponents");
        sbc.bind(renameSubsidiaryComponentsCheckBox, "renameSlaveComponents");
        sbc.bind(referenceComponentPatternField, "masterComponentPattern");
        sbc.bind(subsidiaryComponentPatternField, "slaveComponentPattern");
        sbc.bind(resamplingComboBox, "resampling");
    }

    private JPanel createInputPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setTablePadding(3, 3);

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Input"));

        panel.add(referenceProductSelector.getLabel());
        panel.add(referenceProductSelector.getComboBox());
        panel.add(referenceProductSelector.getButton());
        panel.add(subsidiaryProductSelector.getLabel());
        panel.add(subsidiaryProductSelector.getComboBox());
        panel.add(subsidiaryProductSelector.getButton());

        return panel;
    }

    private JPanel createOutputPanel() {
        final TableLayout layout = new TableLayout(5);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setColumnWeightX(3, 0.0);
        layout.setColumnWeightX(4, 0.0);
        layout.setTablePadding(3, 3);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 0));
        layout.setCellPadding(0, 1, new Insets(3, 0, 3, 3));
        layout.setCellColspan(0, 1, 4);
        layout.setCellColspan(1, 2, 2);
        layout.setCellColspan(3, 1, 2);
        layout.setCellWeightX(2, 2, 0.0);
        layout.setCellWeightX(2, 3, 1.0);

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Output"));

        final TargetProductSelector selector = new TargetProductSelector(model.getTargetProductSelectorModel());

        panel.add(createNewProductCheckBox);
        panel.add(new JLabel("Create new product"));
        panel.add(new JLabel());
        panel.add(selector.getProductNameLabel());
        panel.add(selector.getProductNameTextField());
        panel.add(new JLabel());
        panel.add(new JLabel());
        panel.add(selector.getSaveToFileCheckBox());
        panel.add(selector.getFormatNameComboBox());
        panel.add(selector.getDirectoryTextField());
        panel.add(selector.getDirectoryChooserButton());
        panel.add(new JLabel());
        panel.add(selector.getOpenInVisatCheckBox());

        return panel;
    }

    private JPanel createRenamingPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setTablePadding(3, 3);

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Component Renaming"));

        panel.add(renameReferenceComponentsCheckBox);
        panel.add(referenceComponentPatternField);

        panel.add(renameSubsidiaryComponentsCheckBox);
        panel.add(subsidiaryComponentPatternField);

        return panel;
    }

    private JPanel createResamplingPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setTablePadding(3, 3);

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Resampling"));
        panel.add(new JLabel("Resampling:"));
        panel.add(resamplingComboBox);
        panel.add(new JLabel());

        return panel;
    }

    private static SourceProductSelector createProductSelector(Product[] selectableProducts, String labelText, int index) {
        final SourceProductSelector selector = new SourceProductSelector(selectableProducts, labelText);
        if (selector.getProductCount() > index) {
            selector.setSelectedIndex(index);
        }

        return selector;
    }

    public static void main(String[] args) throws
                                           IllegalAccessException,
                                           UnsupportedLookAndFeelException,
                                           InstantiationException,
                                           ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        ModalDialog dialog = new CollocationDialog(null, new Product[0]);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }
}
