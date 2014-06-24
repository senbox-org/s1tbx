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
import org.esa.beam.util.math.Range;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
    private final JFormattedTextField minField;
    private final JFormattedTextField maxField;
    private final DiscreteCheckBox discreteCheckBox;


    private enum RangeKey {FromPaletteSource, FromData, FromMinMaxFields, FromCurrentPalette;}
    private boolean shouldFireChooserEvent;
    private boolean hidden = false;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm) {
        ColorPalettesManager.loadAvailableColorPalettes(parentForm.getIODir());

        this.parentForm = parentForm;

        final TableLayout layout = new TableLayout();
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(2, 2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTH);
        layout.setCellPadding(0, 0, new Insets(8, 2, 2, 2));
        layout.setCellPadding(2, 0, new Insets(13, 2, 5, 2));

        final JPanel editorPanel = new JPanel(layout);
        editorPanel.add(new JLabel("Colour ramp:"));
        colorPaletteChooser = new ColorPaletteChooser();
        editorPanel.add(colorPaletteChooser);
        editorPanel.add(new JLabel("Display range"));

        minField = getNumberTextField(0.00001);
        maxField = getNumberTextField(1);

        final JPanel minPanel = new JPanel(new BorderLayout(5, 2));
        minPanel.add(new JLabel("Min:"), BorderLayout.WEST);
        minPanel.add(minField, BorderLayout.SOUTH);
        final JPanel maxPanel = new JPanel(new BorderLayout(5, 2));
        maxPanel.add(new JLabel("Max:"), BorderLayout.EAST);
        maxPanel.add(maxField, BorderLayout.SOUTH);

        final JPanel minMaxPanel = new JPanel(new BorderLayout(5, 5));
        minMaxPanel.add(minPanel, BorderLayout.WEST);
        minMaxPanel.add(maxPanel, BorderLayout.EAST);
        editorPanel.add(minMaxPanel);

        final JButton fromFile = new JButton("Range from File");
        final JButton fromData = new JButton("Range from Data");

        final JPanel buttonPanel = new JPanel(new BorderLayout(5, 10));
        buttonPanel.add(fromFile, BorderLayout.WEST);
        buttonPanel.add(fromData, BorderLayout.EAST);
        editorPanel.add(new JLabel(" "));
        editorPanel.add(buttonPanel);

        shouldFireChooserEvent = true;

        colorPaletteChooser.addActionListener(createListener(RangeKey.FromCurrentPalette));
        minField.addActionListener(createListener(RangeKey.FromMinMaxFields));
        maxField.addActionListener(createListener(RangeKey.FromMinMaxFields));
        fromFile.addActionListener(createListener(RangeKey.FromPaletteSource));
        fromData.addActionListener(createListener(RangeKey.FromData));

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(editorPanel, BorderLayout.NORTH);
        moreOptionsForm = new MoreOptionsForm(parentForm, parentForm.getFormModel().canUseHistogramMatching());
        discreteCheckBox = new DiscreteCheckBox(parentForm);
        moreOptionsForm.addRow(discreteCheckBox);
        parentForm.getFormModel().modifyMoreOptionsForm(moreOptionsForm);

        logDisplayButton = LogDisplay.createButton();
        logDisplayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean shouldLog10Display = logDisplayButton.isSelected();
                final ImageInfo imageInfo = parentForm.getFormModel().getModifiedImageInfo();
                if (shouldLog10Display) {
                    final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();
                    if (LogDisplay.checkApplicability(cpd)) {
                        colorPaletteChooser.setLog10Display(shouldLog10Display);
                        imageInfo.setLogScaled(shouldLog10Display);
                        parentForm.applyChanges();
                    } else {
                        LogDisplay.showNotApplicableInfo(parentForm.getContentPanel());
                        logDisplayButton.setSelected(false);
                    }
                } else {
                    colorPaletteChooser.setLog10Display(shouldLog10Display);
                    imageInfo.setLogScaled(shouldLog10Display);
                    parentForm.applyChanges();
                }
            }
        });
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(FormModel formModel) {
        hidden = false;
        updateFormModel(formModel);
    }

    @Override
    public void handleFormHidden(FormModel formModel) {
        hidden = true;
    }

    @Override
    public void updateFormModel(FormModel formModel) {
        if (!hidden) {
            ColorPalettesManager.loadAvailableColorPalettes(parentForm.getIODir());
            colorPaletteChooser.reloadPalettes();
        }

        final ImageInfo imageInfo = formModel.getOriginalImageInfo();
        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();

        final boolean logScaled = imageInfo.isLogScaled();
        final boolean discrete = cpd.isDiscrete();

        colorPaletteChooser.setLog10Display(logScaled);
        colorPaletteChooser.setDiscreteDisplay(discrete);
        shouldFireChooserEvent = false;
        colorPaletteChooser.setSelectedColorPaletteDefinition(cpd);
        shouldFireChooserEvent = true;

        discreteCheckBox.setDiscreteColorsMode(discrete);
        logDisplayButton.setSelected(logScaled);
        parentForm.revalidateToolViewPaneControl();
        minField.setValue(cpd.getMinDisplaySample());
        maxField.setValue(cpd.getMaxDisplaySample());
    }

    @Override
    public void resetFormModel(FormModel formModel) {
        updateFormModel(formModel);
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
            updateFormModel(parentForm.getFormModel());
        }
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getFormModel().getRasters();
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

    private ActionListener createListener(final RangeKey key) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyChanges(key);
            }
        };
    }

    private JFormattedTextField getNumberTextField(double value) {
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("0.0############"));
        formatter.setValueClass(Double.class); // to ensure that double values are returned
        final JFormattedTextField numberField = new JFormattedTextField(formatter);
        numberField.setValue(value);
        final Dimension preferredSize = numberField.getPreferredSize();
        preferredSize.width = 70;
        numberField.setPreferredSize(preferredSize);
        return numberField;
    }

    private void applyChanges(RangeKey key) {
        if (shouldFireChooserEvent) {
            final ColorPaletteDef selectedCPD = colorPaletteChooser.getSelectedColorPaletteDefinition();
            final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
            final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
            final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
            deepCopy.setDiscrete(currentCPD.isDiscrete());

            final double min;
            final double max;
            final ColorPaletteDef cpd;
            switch (key) {
            case FromPaletteSource:
                final Range rangeFromFile = colorPaletteChooser.getRangeFromFile();
                min = rangeFromFile.getMin();
                max = rangeFromFile.getMax();
                cpd = currentCPD;
                break;
            case FromData:
                final Stx stx = parentForm.getStx(parentForm.getFormModel().getRaster());
                min = stx.getMinimum();
                max = stx.getMaximum();
                cpd = currentCPD;
                break;
            case FromMinMaxFields:
                min = (double) minField.getValue();
                max = (double) maxField.getValue();
                cpd = currentCPD;
                break;
            default:
                min = currentCPD.getMinDisplaySample();
                max = currentCPD.getMaxDisplaySample();
                cpd = deepCopy;
            }
            final boolean autoDistribute = true;
            currentInfo.setColorPaletteDef(cpd, min, max, autoDistribute);
            parentForm.applyChanges();
        }
    }
}
