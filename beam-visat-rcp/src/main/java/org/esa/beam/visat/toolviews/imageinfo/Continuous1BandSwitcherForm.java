package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandSwitcherForm implements ImageInfoEditor {

    private final ColorManipulationForm parentForm;
    private JPanel contentPanel;
    private JRadioButton graphicalButton;
    private JRadioButton tabularButton;
    private JCheckBox gradientPaletteCheckBox;
    private ImageInfoEditor imageInfoEditor;
    private Continuous1BandTabularForm tabularPaletteEditorForm;
    private Continuous1BandGraphicalForm graphicalPaletteEditorForm;

    protected Continuous1BandSwitcherForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        imageInfoEditor = EmptyPaletteEditorForm.INSTANCE;
        graphicalButton = new JRadioButton("Sliders");
        tabularButton = new JRadioButton("Table");
        final ButtonGroup editorGroup = new ButtonGroup();
        editorGroup.add(graphicalButton);
        editorGroup.add(tabularButton);
        graphicalButton.setSelected(true);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        graphicalButton.addActionListener(switcherActionListener);
        tabularButton.addActionListener(switcherActionListener);
        gradientPaletteCheckBox = new JCheckBox("Gradient");
        gradientPaletteCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              setDiscreteMode();
            }
        });

        final JPanel editorSwitcherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        editorSwitcherPanel.add(new JLabel("Editor:"));
        editorSwitcherPanel.add(graphicalButton);
        editorSwitcherPanel.add(tabularButton);

        final JPanel northPanel = new JPanel(new BorderLayout(2,2));
        northPanel.add(editorSwitcherPanel, BorderLayout.WEST);
        northPanel.add(gradientPaletteCheckBox, BorderLayout.EAST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(northPanel, BorderLayout.NORTH);
    }

    public void performReset(ProductSceneView productSceneView) {
        imageInfoEditor.performReset(productSceneView);
    }

    public ImageInfo getCurrentImageInfo() {
        return imageInfoEditor.getCurrentImageInfo();
    }

    public void handleFormShown(ProductSceneView productSceneView) {
        imageInfoEditor.handleFormShown(productSceneView);
    }

    private void setDiscreteMode() {
        imageInfoEditor.getCurrentImageInfo().getColorPaletteDef().setGradient(gradientPaletteCheckBox.isSelected());
        imageInfoEditor.getContentPanel().repaint();
        parentForm.setApplyEnabled(true);
    }

    private void switchForm(ProductSceneView productSceneView) {
        final ImageInfoEditor oldForm = imageInfoEditor;
        final ImageInfoEditor newForm;
        ImageInfoHolder imageInfoHolder;
        if (tabularButton.isSelected()) {
            if (tabularPaletteEditorForm == null) {
                tabularPaletteEditorForm = new Continuous1BandTabularForm(parentForm);
            }
            imageInfoHolder = tabularPaletteEditorForm;
            newForm = tabularPaletteEditorForm;
        } else {
            if (graphicalPaletteEditorForm == null) {
                graphicalPaletteEditorForm = new Continuous1BandGraphicalForm(parentForm);
            }
            imageInfoHolder = graphicalPaletteEditorForm;
            newForm = graphicalPaletteEditorForm;
        }
        if (oldForm != newForm) {
            oldForm.handleFormHidden();

            imageInfoEditor = newForm;
            imageInfoEditor.handleFormShown(productSceneView);

            ImageInfo oldImageInfo = oldForm.getCurrentImageInfo();
            if (oldImageInfo == null) { 
                // oldForm == instanceof EmptyForm
                oldImageInfo = productSceneView.getRaster().getImageInfo();
            }
            imageInfoHolder.setCurrentImageInfo(oldImageInfo);

            contentPanel.remove(oldForm.getContentPanel());
            contentPanel.add(imageInfoEditor.getContentPanel(), BorderLayout.CENTER);

            parentForm.installSpecificFormUI();
        }
    }

    public AbstractButton[] getButtons() {
        return imageInfoEditor.getButtons();
    }

    public void updateState(ProductSceneView productSceneView) {
        switchForm(productSceneView);
        imageInfoEditor.updateState(productSceneView);
        ImageInfo imageInfo = productSceneView.getRaster().getImageInfo();
        if (imageInfo != null) {
            gradientPaletteCheckBox.setSelected(imageInfo.getColorPaletteDef().isGradient());
        }
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public void performApply(ProductSceneView productSceneView) {
        imageInfoEditor.performApply(productSceneView);
    }

    public String getTitle(ProductSceneView productSceneView) {
        return imageInfoEditor.getTitle(productSceneView);
    }

    public void handleFormHidden() {
        imageInfoEditor.handleFormHidden();
    }

    private class SwitcherActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateState(parentForm.getProductSceneView());
        }
    }
}
