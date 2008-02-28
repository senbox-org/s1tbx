package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandSwitcherForm implements PaletteEditorForm {

    private final ColorManipulationForm parentForm;
    private PaletteEditorForm currentPaletteEditorForm;
    private JPanel contentPanel;
    private JRadioButton graphicalButton;
    private JRadioButton tabularButton;
    private JCheckBox gradientCheckBox;
    private PaletteEditorForm tabularPaletteEditorForm;
    private PaletteEditorForm graphicalPaletteEditorForm;

    protected Continuous1BandSwitcherForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        currentPaletteEditorForm = EmptyPaletteEditorForm.INSTANCE;
        graphicalButton = new JRadioButton("graphical");
        tabularButton = new JRadioButton("tabular");
        final ButtonGroup group = new ButtonGroup();
        group.add(graphicalButton);
        group.add(tabularButton);
        graphicalButton.setSelected(true);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        graphicalButton.addActionListener(switcherActionListener);
        tabularButton.addActionListener(switcherActionListener);
        gradientCheckBox = new JCheckBox("Create gradient");
        final JPanel switcherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        switcherPanel.add(new JLabel("Palette editor: "));
        switcherPanel.add(graphicalButton);
        switcherPanel.add(tabularButton);
        final JPanel gradientCurvePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gradientCurvePanel.add(gradientCheckBox);
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(switcherPanel, BorderLayout.NORTH);
        contentPanel.add(gradientCurvePanel, BorderLayout.SOUTH);
    }

    public void performReset(ProductSceneView productSceneView) {
        currentPaletteEditorForm.performReset(productSceneView);
    }

    public ImageInfo getCurrentImageInfo() {
        return currentPaletteEditorForm.getCurrentImageInfo();
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        currentPaletteEditorForm.setCurrentImageInfo(imageInfo);
    }

    public void handleFormShown(ProductSceneView productSceneView) {
        currentPaletteEditorForm.handleFormShown(productSceneView);
    }

    private void switchForm(ProductSceneView productSceneView) {
        final PaletteEditorForm oldForm = currentPaletteEditorForm;
        final PaletteEditorForm newForm;
        if (tabularButton.isSelected()) {
            if (tabularPaletteEditorForm == null) {
                tabularPaletteEditorForm = new Continuous1BandTabularForm();
            }
            newForm = tabularPaletteEditorForm;
        } else {
            if (graphicalPaletteEditorForm == null) {
                graphicalPaletteEditorForm = new Continuous1BandGraphicalForm(parentForm);
            }
            newForm = graphicalPaletteEditorForm;
        }
        if (oldForm != newForm) {
            oldForm.handleFormHidden();

            currentPaletteEditorForm = newForm;
            currentPaletteEditorForm.handleFormShown(productSceneView);
            ImageInfo imageInfo = oldForm.getCurrentImageInfo();
            if (imageInfo == null) {
                imageInfo = productSceneView.getRaster().getImageInfo();
            }
            currentPaletteEditorForm.setCurrentImageInfo(imageInfo);

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
