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

import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandSwitcherForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private JPanel contentPanel;
    private ColorManipulationChildForm childForm;
    private JRadioButton graphicalButton;
    private Continuous1BandGraphicalForm graphicalPaletteEditorForm;
    private JRadioButton tabularButton;
    private Continuous1BandTabularForm tabularPaletteEditorForm;
    private JRadioButton basicButton;
    private Continuous1BandBasicForm basicPaletteEditorForm;

    protected Continuous1BandSwitcherForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        childForm = EmptyImageInfoForm.INSTANCE;
        basicButton = new JRadioButton("Basic");
        graphicalButton = new JRadioButton("Sliders");
        tabularButton = new JRadioButton("Table");
        final ButtonGroup editorGroup = new ButtonGroup();
        editorGroup.add(basicButton);
        editorGroup.add(graphicalButton);
        editorGroup.add(tabularButton);
        graphicalButton.setSelected(true);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        basicButton.addActionListener(switcherActionListener);
        graphicalButton.addActionListener(switcherActionListener);
        tabularButton.addActionListener(switcherActionListener);

        final JPanel editorSwitcherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        editorSwitcherPanel.add(new JLabel("Editor:"));
        editorSwitcherPanel.add(basicButton);
        editorSwitcherPanel.add(graphicalButton);
        editorSwitcherPanel.add(tabularButton);

        final JPanel northPanel = new JPanel(new BorderLayout(2, 2));
        northPanel.add(editorSwitcherPanel, BorderLayout.WEST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(northPanel, BorderLayout.NORTH);
    }

    @Override
    public void handleFormShown(FormModel formModel) {
        switchForm();
    }

    @Override
    public void handleFormHidden(FormModel formModel) {
        childForm.handleFormHidden(formModel);
    }

    @Override
    public void updateFormModel(FormModel formModel) {
        childForm.updateFormModel(formModel);
    }

    @Override
    public void resetFormModel(FormModel formModel) {
        childForm.resetFormModel(formModel);
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

    private void switchForm() {
        final ColorManipulationChildForm oldForm = childForm;
        final ColorManipulationChildForm newForm;
        if (tabularButton.isSelected()) {
            if (tabularPaletteEditorForm == null) {
                tabularPaletteEditorForm = new Continuous1BandTabularForm(parentForm);
            }
            newForm = tabularPaletteEditorForm;
        } else if (basicButton.isSelected()) {
            if (basicPaletteEditorForm == null) {
                basicPaletteEditorForm = new Continuous1BandBasicForm(parentForm);
            }
            newForm = basicPaletteEditorForm;
        } else {
            if (graphicalPaletteEditorForm == null) {
                graphicalPaletteEditorForm = new Continuous1BandGraphicalForm(parentForm);
            }
            newForm = graphicalPaletteEditorForm;
        }
        if (oldForm != newForm) {
            oldForm.handleFormHidden(parentForm.getFormModel());

            childForm = newForm;
            childForm.handleFormShown(parentForm.getFormModel());

            contentPanel.remove(oldForm.getContentPanel());
            contentPanel.add(childForm.getContentPanel(), BorderLayout.CENTER);

            parentForm.installToolButtons();
            parentForm.installMoreOptions();
            parentForm.revalidateToolViewPaneControl();
        } else {
            childForm.updateFormModel(parentForm.getFormModel());
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
            switchForm();
        }
    }
}
