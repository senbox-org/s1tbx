package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.io.SourceProductSelector;
import org.esa.beam.framework.ui.io.TargetProductSelector;
import org.esa.beam.framework.ui.io.TargetProductSelectorModel;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Form for geographic collocation dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationForm extends JPanel {

    private CollocationFormModel model;

    // todo - put into model
    private Product externalProduct;
    private Product[] internalProducts;
    private Product selectedProduct;

    public CollocationForm(CollocationFormModel model) {
        this.model = model;

        initComponents();
        bindComponents();
    }

    private void initComponents() {

        setLayout(new BorderLayout());

        final TableLayout layout1 = new TableLayout(3);
        layout1.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout1.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout1.setColumnWeightX(0, 0.0);
        layout1.setColumnWeightX(1, 1.0);
        layout1.setColumnWeightX(2, 0.0);
        layout1.setTablePadding(3, 3);

        final JPanel inputPanel = new JPanel(layout1);
        // todo - why specify all these parameters? The title shoud be enough (MP - 15.10.2007)
        inputPanel.setBorder(BorderFactory.createTitledBorder(null, "Input",
                                                              TitledBorder.DEFAULT_JUSTIFICATION,
                                                              TitledBorder.DEFAULT_POSITION,
                                                              new Font("Tahoma", 0, 11),
                                                              new Color(0, 70, 213)));

        final SourceProductSelector masterSelector = new SourceProductSelector(new Product[0], "Reference:");
        final SourceProductSelector slaveSelector = new SourceProductSelector(new Product[0], "Collocate:");

        inputPanel.add(masterSelector.getLabel());
        inputPanel.add(masterSelector.getComboBox());
        inputPanel.add(masterSelector.getButton());
        inputPanel.add(slaveSelector.getLabel());
        inputPanel.add(slaveSelector.getComboBox());
        inputPanel.add(slaveSelector.getButton());

        final JPanel outputPanel = new JPanel(new BorderLayout());
        // todo - why specify all these parameters? The title shoud be enough (MP - 15.10.2007)
        outputPanel.setBorder(BorderFactory.createTitledBorder(null, "Output",
                                                               TitledBorder.DEFAULT_JUSTIFICATION,
                                                               TitledBorder.DEFAULT_POSITION,
                                                               new Font("Tahoma", 0, 11),
                                                               new Color(0, 70, 213)));

        outputPanel.add(new TargetProductSelector(new TargetProductSelectorModel(true)).createDefaultPanel());

        /*
        final JPanel radioButtonPanel = new JPanel(layout2);
        final JRadioButton radioButton1 = new JRadioButton();
        radioButton1.setSelected(true);
        radioButtonPanel.add(radioButton1);
        radioButtonPanel.add(new JLabel("Use master product"));

        final JRadioButton radioButton2 = new JRadioButton();
        radioButton2.setSelected(false);
        radioButtonPanel.add(radioButton2);
        radioButtonPanel.add(new JLabel("Create new product"));

        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButton1);
        buttonGroup.add(radioButton2);

        final TableLayout layout3 = new TableLayout(3);
        layout3.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout3.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout3.setColumnWeightX(0, 0.0);
        layout3.setColumnWeightX(1, 1.0);
        layout3.setColumnWeightX(2, 0.0);
        layout3.setTablePadding(2, 2);
        layout3.setCellPadding(0, 0, new Insets(2, 0, 2, 2));
        layout3.setCellPadding(0, 2, new Insets(2, 0, 2, 0));

        final JPanel targetProductPanel = new JPanel(layout3);

        final JTextField textField3 = new JTextField("New product");
        textField3.setColumns(35);
        targetProductPanel.add(new JLabel("Name:"));
        targetProductPanel.add(textField3);
        targetProductPanel.add(new JButton("Define ..."));

        radioButtonPanel.add(new JLabel());
        radioButtonPanel.add(targetProductPanel);
        outputPanel.add(radioButtonPanel, BorderLayout.NORTH);
        */

        final TableLayout layout4 = new TableLayout(3);
        layout4.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout4.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout4.setColumnWeightX(0, 0.0);
        layout4.setColumnWeightX(1, 0.0);
        layout4.setColumnWeightX(2, 1.0);
        layout4.setTablePadding(3, 3);

        final JPanel resamplingPanel = new JPanel(layout4);
        resamplingPanel.setBorder(BorderFactory.createTitledBorder(null, "Resampling",
                                                                   TitledBorder.DEFAULT_JUSTIFICATION,
                                                                   TitledBorder.DEFAULT_POSITION,
                                                                   new Font("Tahoma", 0, 11),
                                                                   new Color(0, 70, 213)));
        resamplingPanel.add(new JLabel("Method:"));
        resamplingPanel.add(new JComboBox(new String[]{"Nearest Neighbor", "Bilinear Convolution",
                "Bicubic Convolution"}));
        resamplingPanel.add(new JLabel());

        add(inputPanel, BorderLayout.NORTH);
        add(outputPanel, BorderLayout.CENTER);
        add(resamplingPanel, BorderLayout.SOUTH);
    }

    private void bindComponents() {
    }

    public static void main(String[] args) throws
                                           IllegalAccessException,
                                           UnsupportedLookAndFeelException,
                                           InstantiationException,
                                           ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        ModalDialog dialog = new CollocationDialog(null, null);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }

}
