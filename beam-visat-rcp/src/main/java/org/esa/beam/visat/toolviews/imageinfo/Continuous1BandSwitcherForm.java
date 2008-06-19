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
import com.bc.ceres.binding.swing.BindingContext;

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
    private ColorComboBox noDataColorComboBox;
    private JRadioButton noDataIsTransparentButton;
    private JRadioButton noDataIsColoredButton;

    protected Continuous1BandSwitcherForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        currentPaletteEditorForm = EmptyPaletteEditorForm.INSTANCE;
        graphicalButton = new JRadioButton("Graphical editor");
        tabularButton = new JRadioButton("Tabular editor");
        final ButtonGroup editorGroup = new ButtonGroup();
        editorGroup.add(graphicalButton);
        editorGroup.add(tabularButton);
        graphicalButton.setSelected(true);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        graphicalButton.addActionListener(switcherActionListener);
        tabularButton.addActionListener(switcherActionListener);

        noDataIsTransparentButton = new JRadioButton("is transparent", false);
        noDataIsColoredButton = new JRadioButton("has colour:", true);
        final ButtonGroup noDataButtonGroup = new ButtonGroup();
        noDataButtonGroup.add(noDataIsTransparentButton);
        noDataButtonGroup.add(noDataIsColoredButton);
        noDataColorComboBox = new ColorComboBox();
        noDataColorComboBox.setColorValueVisible(false);
        noDataColorComboBox.setSelectedColor(Color.YELLOW);
        noDataColorComboBox.setAllowDefaultColor(true);
        discretePaletteCheckBox = new JCheckBox("Discrete palette");

        JPanel noDataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        noDataPanel.add(new JLabel("No-data"));
        noDataPanel.add(noDataIsTransparentButton);
        noDataPanel.add(noDataIsColoredButton);
        noDataPanel.add(noDataColorComboBox);

        final JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(graphicalButton);
        northPanel.add(tabularButton);

        final JPanel southPanel = new JPanel(new BorderLayout(4,4));
        southPanel.add(discretePaletteCheckBox, BorderLayout.WEST);
        southPanel.add(noDataPanel, BorderLayout.CENTER);
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(northPanel, BorderLayout.NORTH);
        contentPanel.add(southPanel, BorderLayout.SOUTH);

        HashMap<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("discretePalette", Boolean.FALSE);
        propertyMap.put("noDataIsTransparent", Boolean.FALSE);
        propertyMap.put("noDataIsColored", Boolean.TRUE);
        propertyMap.put("noDataColor", Color.ORANGE);

        valueContainer = ValueContainerFactory.createMapBackedValueContainer(propertyMap);
        BindingContext context = new BindingContext(valueContainer);
        context.bind(discretePaletteCheckBox, "discretePalette");
        context.bind(noDataIsTransparentButton, "noDataIsTransparent");
        context.bind(noDataIsColoredButton, "noDataIsColored");
        context.enable(noDataColorComboBox, "noDataIsColored", true);
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
