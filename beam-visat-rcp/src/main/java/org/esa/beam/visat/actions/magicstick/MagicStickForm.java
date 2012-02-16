package org.esa.beam.visat.actions.magicstick;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
class MagicStickForm {
    private MagicStickInteractor interactor;
    private JTextField toleranceField;
    private JSlider toleranceSlider;

    boolean adjustingSlider;
    private JCheckBox normalizeCheckBox;

    MagicStickForm(MagicStickInteractor interactor) {
        this.interactor = interactor;
    }

    public JPanel createPanel() {

        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(interactor.getModel()));
        bindingContext.addPropertyChangeListener("tolerance", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                adjustSlider();
                interactor.updateMagicStickMask();
            }
        });

        JLabel toleranceLabel = new JLabel("Tolerance:");
        toleranceLabel.setToolTipText("Sets the maximum Euclidian distance tolerated (in units of the spectral bands)");

        toleranceField = new JTextField(10);
        bindingContext.bind("tolerance", toleranceField);
        toleranceField.setText(String.valueOf(interactor.getModel().getTolerance()));

        toleranceSlider = new JSlider(-10, 20);
        toleranceSlider.setSnapToTicks(false);
        toleranceSlider.setPaintTicks(false);
        toleranceSlider.setPaintLabels(false);
        toleranceSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!adjustingSlider) {
                    bindingContext.getPropertySet().setValue("tolerance", Math.pow(10.0, toleranceSlider.getValue() / 10.0));
                }
            }
        });

        normalizeCheckBox = new JCheckBox("Normalize spectra");
        normalizeCheckBox.setToolTipText("Normalizes spectra by dividing them by their first values");
        bindingContext.bind("normalize", normalizeCheckBox);
        bindingContext.addPropertyChangeListener("normalize", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.updateMagicStickMask();
            }
        });

        JRadioButton methodButton1 = new JRadioButton("Distance");
        JRadioButton methodButton2 = new JRadioButton("Average");
        JRadioButton methodButton3 = new JRadioButton("Limits");
        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(methodButton1);
        methodGroup.add(methodButton2);
        methodGroup.add(methodButton3);
        bindingContext.bind("method", methodGroup);
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        methodPanel.add(methodButton1);
        methodPanel.add(methodButton2);
        methodPanel.add(methodButton3);
        bindingContext.addPropertyChangeListener("method", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.updateMagicStickMask();
            }
        });

        JRadioButton operatorButton1 = new JRadioButton("Integral");
        JRadioButton operatorButton2 = new JRadioButton("Identity");
        JRadioButton operatorButton3 = new JRadioButton("Derivative");
        ButtonGroup operatorGroup = new ButtonGroup();
        operatorGroup.add(operatorButton1);
        operatorGroup.add(operatorButton2);
        operatorGroup.add(operatorButton3);
        bindingContext.bind("operator", operatorGroup);
        JPanel operatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        operatorPanel.add(operatorButton1);
        operatorPanel.add(operatorButton2);
        operatorPanel.add(operatorButton3);
        bindingContext.addPropertyChangeListener("operator", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.updateMagicStickMask();
            }
        });

        final JToggleButton plusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Plus16.gif")));
        plusButton.setToolTipText("Switch to 'plus' mode: Selected spectra will be included in the mask.");
        final JToggleButton minusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Minus16.gif")));
        minusButton.setToolTipText("Switch to 'minus' mode: Selected spectra will be excluded from the mask.");
        plusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingContext.getPropertySet().setValue("mode", plusButton.isSelected() ? MagicStickModel.Mode.PLUS : MagicStickModel.Mode.SINGLE);
            }
        });
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingContext.getPropertySet().setValue("mode", plusButton.isSelected() ? MagicStickModel.Mode.MINUS : MagicStickModel.Mode.SINGLE);
            }
        });

        bindingContext.addPropertyChangeListener("mode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                plusButton.setSelected(interactor.getModel().getMode() == MagicStickModel.Mode.PLUS);
                minusButton.setSelected(interactor.getModel().getMode() == MagicStickModel.Mode.MINUS);
            }
        });

        final JButton clearButton = new JButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Remove16.gif")));
        clearButton.setToolTipText("Clears the current mask and removes all spectra collected so far,");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interactor.getModel().clearSpectra();
                interactor.updateMagicStickMask();
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(plusButton);
        toolBar.add(minusButton);
        toolBar.add(clearButton);

        JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        toolBarPanel.add(toolBar);

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellColspan(1, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(2, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(3, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(4, 0, tableLayout.getColumnCount());

        JPanel panel = new JPanel(tableLayout);
        panel.add(toleranceLabel, new TableLayout.Cell(0, 0));
        panel.add(toleranceField, new TableLayout.Cell(0, 1));
        panel.add(toleranceSlider, new TableLayout.Cell(1, 0));
        panel.add(methodPanel, new TableLayout.Cell(2, 0));
        panel.add(operatorPanel, new TableLayout.Cell(3, 0));
        panel.add(normalizeCheckBox, new TableLayout.Cell(4, 0));
        panel.add(toolBarPanel, new TableLayout.Cell(5, 0));

        adjustSlider();

        return panel;
    }

    private void adjustSlider() {
        adjustingSlider = true;
        toleranceSlider.setValue((int) Math.round(10.0 * Math.log10(interactor.getModel().getTolerance())));
        adjustingSlider = false;
    }

}
