package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.io.SourceProductSelector;
import org.esa.beam.framework.ui.io.TargetProductSelector;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    //    private JCheckBox createNewProductCheckBox;
    private TargetProductSelector targetProductSelector;

    public CollocationForm(final CollocationFormModel model, Product[] selectableProducts) {
        this.model = model;

        referenceProductSelector = new SourceProductSelector(selectableProducts, "Reference product:");
        subsidiaryProductSelector = new SourceProductSelector(selectableProducts, "Subsidiary product:");
//        createNewProductCheckBox = new JCheckBox("Create new product");
        renameReferenceComponentsCheckBox = new JCheckBox("Rename reference components:");
        renameSubsidiaryComponentsCheckBox = new JCheckBox("Rename subsidiary components:");
        referenceComponentPatternField = new JTextField();
        subsidiaryComponentPatternField = new JTextField();
        resamplingComboBox = new JComboBox(model.getResamplingComboBoxModel());

        subsidiaryProductSelector.getProductNameComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.adaptResamplingComboBoxModel();
            }
        });

        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        };
//        createNewProductCheckBox.addActionListener(listener);
        renameReferenceComponentsCheckBox.addActionListener(listener);
        renameSubsidiaryComponentsCheckBox.addActionListener(listener);

        initComponents();
        bindComponents();

        if (referenceProductSelector.getProductCount() > 0) {
            referenceProductSelector.setSelectedIndex(0);
        }
        if (subsidiaryProductSelector.getProductCount() > 1) {
            subsidiaryProductSelector.setSelectedIndex(1);
        }
    }

    private void updateUIState() {
        referenceComponentPatternField.setEnabled(renameReferenceComponentsCheckBox.isSelected());
        subsidiaryComponentPatternField.setEnabled(renameSubsidiaryComponentsCheckBox.isSelected());
//        targetProductSelector.setEnabled(createNewProductCheckBox.isSelected());
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

        sbc.bind(referenceProductSelector.getProductNameComboBox(), "masterProduct");
        sbc.bind(subsidiaryProductSelector.getProductNameComboBox(), "slaveProduct");
//        sbc.bind(createNewProductCheckBox, "createNewProduct");
        sbc.bind(renameReferenceComponentsCheckBox, "renameMasterComponents");
        sbc.bind(renameSubsidiaryComponentsCheckBox, "renameSlaveComponents");
        sbc.bind(referenceComponentPatternField, "masterComponentPattern");
        sbc.bind(subsidiaryComponentPatternField, "slaveComponentPattern");
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
        panel.setBorder(BorderFactory.createTitledBorder("Source"));

        panel.add(referenceProductSelector.getProductNameLabel());
        panel.add(referenceProductSelector.getProductNameComboBox());
        panel.add(referenceProductSelector.getProductFileChooserButton());
        panel.add(subsidiaryProductSelector.getProductNameLabel());
        panel.add(subsidiaryProductSelector.getProductNameComboBox());
        panel.add(subsidiaryProductSelector.getProductFileChooserButton());

        return panel;
    }

    private JPanel createOutputPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 1.0);
//        layout.setTablePadding(0, 0);
//        layout.setCellPadding(1, 0, new Insets(0, 21, 0, 0));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Output"));

        targetProductSelector = new TargetProductSelector(model.getTargetProductSelectorModel(), "Target product name:");
//        panel.add(createNewProductCheckBox);
        panel.add(targetProductSelector.createDefaultPanel());

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
