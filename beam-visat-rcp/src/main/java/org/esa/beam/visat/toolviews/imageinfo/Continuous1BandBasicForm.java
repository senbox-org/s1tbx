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

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

class Continuous1BandBasicForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final JPanel contentPanel;
    private final AbstractButton logDisplayButton;
    private final MoreOptionsForm moreOptionsForm;
    private final ColorPaletteChooser colorPaletteChooser;
    private boolean shouldFireChooserEvent;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm) {
        ColorPalettesManager.loadAvailableColorPalettes(parentForm.getIODir());

        this.parentForm = parentForm;

        final TableLayout layout = new TableLayout();
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(2, 2);
        layout.setCellPadding(0, 0, new Insets(10, 2, 2, 2));
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTH);


        final JRadioButton fromFile = new JRadioButton("load color palette from file");
        final JRadioButton fromCurrent = new JRadioButton("redistribute to current palette min/max");
        final JRadioButton fromData = new JRadioButton("redistribute to data min/max");
        final JRadioButton fromUserDefined = new JRadioButton("set user defined min/max");

        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(fromFile);
        buttonGroup.add(fromCurrent);
        buttonGroup.add(fromData);
        buttonGroup.add(fromUserDefined);

        fromFile.setSelected(true);

        final JPanel editorPanel = new JPanel(layout);
        editorPanel.add(fromFile);
        editorPanel.add(fromCurrent);
        editorPanel.add(fromData);
        editorPanel.add(fromUserDefined);
        final JPanel userMinMaxPanel = new JPanel(new BorderLayout());
        final JPanel filler = new JPanel();
        filler.setMinimumSize(new Dimension(40, 2));
        filler.setPreferredSize(new Dimension(40, 2));
        userMinMaxPanel.add(filler, BorderLayout.WEST);

        final JFormattedTextField minField = getNumberTextField(0.00001);
        final JFormattedTextField maxField = getNumberTextField(1);

        final GridLayout gridLayout = new GridLayout(2, 2);
        gridLayout.setHgap(5);
        final JPanel grid = new JPanel(gridLayout);
        grid.add(new JLabel("min"));
        grid.add(new JLabel("max"));
        grid.add(minField);
        grid.add(maxField);
        userMinMaxPanel.add(grid);

        enableChildComponents(grid, false);

        fromUserDefined.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableChildComponents(grid, fromUserDefined.isSelected());
            }
        });

        editorPanel.add(userMinMaxPanel);
        editorPanel.add(new JLabel(" "));
        editorPanel.add(new JLabel("Predefined Color Palette:"));
        colorPaletteChooser = new ColorPaletteChooser();
        editorPanel.add(colorPaletteChooser);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(editorPanel, BorderLayout.NORTH);
        moreOptionsForm = new MoreOptionsForm(parentForm, true);


        shouldFireChooserEvent = true;

        colorPaletteChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (shouldFireChooserEvent) {
                    final ColorPaletteDef selectedCPD = colorPaletteChooser.getSelectedColorPaletteDefinition();
                    final ImageInfo currentInfo = parentForm.getImageInfo();
                    final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
                    final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
                    deepCopy.setDiscrete(currentCPD.isDiscrete());
                    final double min;
                    final double max;
                    if (fromCurrent.isSelected()) {
                        min = currentCPD.getMinDisplaySample();
                        max = currentCPD.getMaxDisplaySample();
                    } else if (fromFile.isSelected()) {
                        min = selectedCPD.getMinDisplaySample();
                        max = selectedCPD.getMaxDisplaySample();
                    } else if (fromData.isSelected()) {
                        final Stx stx = parentForm.getStx(parentForm.getProductSceneView().getRaster());
                        min = stx.getMinimum();
                        max = stx.getMaximum();
                    } else {
                        min = (double) minField.getValue();
                        max = (double) maxField.getValue();
                    }
                    final boolean autoDistribute = true;
                    currentInfo.setColorPaletteDef(deepCopy, min, max, autoDistribute);
                    parentForm.applyChanges();
                }
            }
        });

        logDisplayButton = ImageInfoEditorSupport.createToggleButton("icons/LogDisplay24.png");
        logDisplayButton.setName("logDisplayButton");
        logDisplayButton.setToolTipText("Switch to logarithmic display"); /*I18N*/
        logDisplayButton.addActionListener(parentForm.wrapWithAutoApplyActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean logScaled = logDisplayButton.isSelected();
                colorPaletteChooser.setLog10Display(logScaled);
                parentForm.getImageInfo().setLogScaled(logScaled);
                parentForm.applyChanges();
            }
        }));
    }

    private JFormattedTextField getNumberTextField(double value) {
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("0.0############"));
        formatter.setValueClass(Double.class); // to ensure that double values are returned
        final JFormattedTextField minField = new JFormattedTextField(formatter);
        minField.setValue(value);
        return minField;
    }

    private void enableChildComponents(Container container, boolean enabled) {
        final Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enabled);
        }
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        final ImageInfo imageInfo = productSceneView.getImageInfo();
        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();

        final boolean logScaled = imageInfo.isLogScaled();
        final boolean discrete = cpd.isDiscrete();

        colorPaletteChooser.setLog10Display(logScaled);
        colorPaletteChooser.setDiscreteDisplay(discrete);
        shouldFireChooserEvent = false;
        colorPaletteChooser.setSelectedColorPaletteDefinition(cpd);
        shouldFireChooserEvent = true;

        moreOptionsForm.setDiscreteColorsMode(discrete);
        logDisplayButton.setSelected(logScaled);
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void resetFormModel(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
            updateFormModel(parentForm.getProductSceneView());
        }
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getProductSceneView().getRasters();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                    logDisplayButton,
        };
    }
}
