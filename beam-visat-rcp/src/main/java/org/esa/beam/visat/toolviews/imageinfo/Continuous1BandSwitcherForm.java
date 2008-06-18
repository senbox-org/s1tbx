package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import com.jidesoft.combobox.ColorComboBox;
import com.bc.ceres.binding.ValueContainerFactory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.SwingBindingContext;

class Continuous1BandSwitcherForm implements PaletteEditorForm {

    private final ColorManipulationForm parentForm;
    private PaletteEditorForm currentPaletteEditorForm;
    private JPanel contentPanel;
    private JRadioButton graphicalButton;
    private JRadioButton tabularButton;
    private JCheckBox discretePaletteCheckBox;
    private Continuous1BandTabularForm tabularPaletteEditorForm;
    private Continuous1BandGraphicalForm graphicalPaletteEditorForm;
    private ValueContainer valueContainer;

    protected Continuous1BandSwitcherForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        currentPaletteEditorForm = EmptyPaletteEditorForm.INSTANCE;
        graphicalButton = new JRadioButton("Graphical editor");
        tabularButton = new JRadioButton("Tabular editor");
        final ButtonGroup group = new ButtonGroup();
        group.add(graphicalButton);
        group.add(tabularButton);
        graphicalButton.setSelected(true);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        graphicalButton.addActionListener(switcherActionListener);
        tabularButton.addActionListener(switcherActionListener);
        final JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(graphicalButton);
        northPanel.add(tabularButton);
        final JPanel southPanel = new JPanel(new BorderLayout(4,4));
        discretePaletteCheckBox = new JCheckBox("Discrete palette");
        southPanel.add(discretePaletteCheckBox, BorderLayout.WEST);
        southPanel.add(createNoDataColorChooser(), BorderLayout.CENTER);
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(northPanel, BorderLayout.NORTH);
        contentPanel.add(southPanel, BorderLayout.SOUTH);

        valueContainer = ValueContainerFactory.createMapBackedValueContainer(new HashMap<String, Object>());
        SwingBindingContext context = new SwingBindingContext(valueContainer);
        context.bind(discretePaletteCheckBox, "discretePalette");
        context.bind();
    }

    private JPanel createNoDataColorChooser() {
        final ButtonGroup group = new ButtonGroup();
        final JRadioButton b1 = new JRadioButton("is transparent", false);
        final JRadioButton b2 = new JRadioButton("has colour:", true);
        group.add(b1);
        group.add(b2);
        final ColorComboBox colorComboBox = new ColorComboBox();
        colorComboBox.setColorValueVisible(false);
        colorComboBox.setSelectedColor(Color.YELLOW);
        colorComboBox.setAllowDefaultColor(true);
        b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                 colorComboBox.setEnabled(b2.isSelected());
            }
        });

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        panel.add(new JLabel("No-data"));
        panel.add(b1);
        panel.add(b2);
        panel.add(colorComboBox);
        return panel;
    }

    public void performReset(ProductSceneView productSceneView) {
        currentPaletteEditorForm.performReset(productSceneView);
    }

    public ImageInfo getCurrentImageInfo() {
        return currentPaletteEditorForm.getCurrentImageInfo();
    }

    public void handleFormShown(ProductSceneView productSceneView) {
        currentPaletteEditorForm.handleFormShown(productSceneView);
    }

    private void switchForm(ProductSceneView productSceneView) {
        final PaletteEditorForm oldForm = currentPaletteEditorForm;
        final PaletteEditorForm newForm;
        ImageInfoHolder imageInfoHolder;
        if (tabularButton.isSelected()) {
            if (tabularPaletteEditorForm == null) {
                tabularPaletteEditorForm = new Continuous1BandTabularForm(parentForm);
            }
            imageInfoHolder =  tabularPaletteEditorForm;
            newForm = tabularPaletteEditorForm;
        } else {
            if (graphicalPaletteEditorForm == null) {
                graphicalPaletteEditorForm = new Continuous1BandGraphicalForm(parentForm);
            }
            imageInfoHolder =  graphicalPaletteEditorForm;
            newForm = graphicalPaletteEditorForm;
        }
        if (oldForm != newForm) {
            oldForm.handleFormHidden();

            currentPaletteEditorForm = newForm;
            currentPaletteEditorForm.handleFormShown(productSceneView);

            ImageInfo oldImageInfo = oldForm.getCurrentImageInfo();
            if (oldImageInfo == null) { 
                // oldForm == instanceof EmptyForm
                oldImageInfo = productSceneView.getRaster().getImageInfo();
            }
            imageInfoHolder.setCurrentImageInfo(oldImageInfo);

            contentPanel.remove(oldForm.getContentPanel());
            contentPanel.add(currentPaletteEditorForm.getContentPanel(), BorderLayout.CENTER);

            parentForm.installSpecificFormUI();
        }
    }

    public AbstractButton[] getButtons() {
        return currentPaletteEditorForm.getButtons();
    }

    public void updateState(ProductSceneView productSceneView) {
        switchForm(productSceneView);
        currentPaletteEditorForm.updateState(productSceneView);
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public void performApply(ProductSceneView productSceneView) {
        currentPaletteEditorForm.performApply(productSceneView);
    }

    public String getTitle(ProductSceneView productSceneView) {
        return currentPaletteEditorForm.getTitle(productSceneView);
    }

    public void handleFormHidden() {
        currentPaletteEditorForm.handleFormHidden();
    }

    private class SwitcherActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateState(parentForm.getProductSceneView());
        }
    }
}
