/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandSwitcherForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private JPanel contentPanel;
    private JRadioButton graphicalButton;
    private JRadioButton tabularButton;
    private JCheckBox discreteColorsCheckBox;
    private ColorManipulationChildForm childForm;
    private Continuous1BandTabularForm tabularPaletteEditorForm;
    private Continuous1BandGraphicalForm graphicalPaletteEditorForm;

    protected Continuous1BandSwitcherForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        childForm = EmptyImageInfoForm.INSTANCE;
        graphicalButton = new JRadioButton("Sliders");
        tabularButton = new JRadioButton("Table");
        final ButtonGroup editorGroup = new ButtonGroup();
        editorGroup.add(graphicalButton);
        editorGroup.add(tabularButton);
        graphicalButton.setSelected(true);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        graphicalButton.addActionListener(switcherActionListener);
        tabularButton.addActionListener(switcherActionListener);
        discreteColorsCheckBox = new JCheckBox("Discrete colors");
        discreteColorsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDiscreteColorsMode();
            }
        });

        final JPanel editorSwitcherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        editorSwitcherPanel.add(new JLabel("Editor:"));
        editorSwitcherPanel.add(graphicalButton);
        editorSwitcherPanel.add(tabularButton);

        final JPanel northPanel = new JPanel(new BorderLayout(2, 2));
        northPanel.add(editorSwitcherPanel, BorderLayout.WEST);
        northPanel.add(discreteColorsCheckBox, BorderLayout.EAST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(northPanel, BorderLayout.NORTH);
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        switchForm(productSceneView);
        ImageInfo imageInfo = parentForm.getImageInfo();
        if (imageInfo != null) {
            discreteColorsCheckBox.setSelected(imageInfo.getColorPaletteDef().isDiscrete());
        }
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        childForm.handleFormHidden(productSceneView);
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        childForm.updateFormModel(productSceneView);
    }

    @Override
    public void resetFormModel(ProductSceneView productSceneView) {
        childForm.resetFormModel(productSceneView);
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        childForm.handleRasterPropertyChange(event, raster);
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return childForm.getMoreOptionsForm();
    }

    @Override
    public RasterDataNode[] getRasters() {
        return childForm.getRasters();
    }

    private void setDiscreteColorsMode() {
        parentForm.getImageInfo().getColorPaletteDef().setDiscrete(discreteColorsCheckBox.isSelected());
        if (childForm == graphicalPaletteEditorForm) {
            graphicalPaletteEditorForm.getImageInfoEditor().getModel().fireStateChanged();
        }
        parentForm.setApplyEnabled(true);
    }

    private void switchForm(ProductSceneView productSceneView) {
        final ColorManipulationChildForm oldForm = childForm;
        final ColorManipulationChildForm newForm;
        if (tabularButton.isSelected()) {
            if (tabularPaletteEditorForm == null) {
                tabularPaletteEditorForm = new Continuous1BandTabularForm(parentForm);
            }
            newForm = tabularPaletteEditorForm;
        } else {
            if (graphicalPaletteEditorForm == null) {
                graphicalPaletteEditorForm = new Continuous1BandGraphicalForm(parentForm);
            }
            newForm = graphicalPaletteEditorForm;
        }
        if (oldForm != newForm) {
            oldForm.handleFormHidden(productSceneView);

            childForm = newForm;
            childForm.handleFormShown(productSceneView);

            contentPanel.remove(oldForm.getContentPanel());
            contentPanel.add(childForm.getContentPanel(), BorderLayout.CENTER);

            parentForm.installToolButtons();
            parentForm.installMoreOptions();
            parentForm.revalidateToolViewPaneControl();
        } else {
            childForm.updateFormModel(productSceneView);
        }
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return childForm.getToolButtons();
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    private class SwitcherActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switchForm(parentForm.getProductSceneView());
        }
    }
}
