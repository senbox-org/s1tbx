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

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandBasicForm implements ColorManipulationChildForm {

    public static final Scaling POW10_SCALING = new Pow10Scaling();

    private final ColorManipulationForm parentForm;
    private final JPanel contentPanel;
    private final AbstractButton logDisplayButton;
    private final MoreOptionsForm moreOptionsForm;
    private final ColorPaletteChooser colorPaletteChooser;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        contentPanel = new JPanel(new BorderLayout(2, 2));
        colorPaletteChooser = new ColorPaletteChooser();
        contentPanel.add(colorPaletteChooser, BorderLayout.NORTH);
        moreOptionsForm = new MoreOptionsForm(parentForm, true);

        colorPaletteChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ColorPaletteDef selectedCPD = colorPaletteChooser.getSelectedColorPaletteDefinition();
                final ImageInfo currentInfo = parentForm.getImageInfo();
                final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
                final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
                deepCopy.setDiscrete(currentCPD.isDiscrete());
                final double min = currentCPD.getMinDisplaySample();
                final double max = currentCPD.getMaxDisplaySample();
                final boolean autoDistribute = true;
                currentInfo.setColorPaletteDef(deepCopy, min, max, autoDistribute);
                parentForm.applyChanges();
            }
        });

        logDisplayButton = ImageInfoEditorSupport.createToggleButton("icons/LogDisplay24.png");
        logDisplayButton.setName("logDisplayButton");
        logDisplayButton.setToolTipText("Switch to logarithmic display"); /*I18N*/
        logDisplayButton.addActionListener(parentForm.wrapWithAutoApplyActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                colorPaletteChooser.setLog10Display(logDisplayButton.isSelected());
                parentForm.getImageInfo().setLogScaled(logDisplayButton.isSelected());
                parentForm.applyChanges();
            }
        }));
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
        colorPaletteChooser.setSelectedColorPaletteDefinition(cpd);

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

    public void renderDiscrete(boolean discrete) {
        colorPaletteChooser.setDiscreteDisplay(discrete);
    }

    private static class Log10Scaling implements Scaling {

        @Override
        public final double scale(double value) {
            return value > 1.0E-9 ? Math.log10(value) : -9.0;
        }

        @Override
        public final double scaleInverse(double value) {
            return value < -9.0 ? 1.0E-9 : Math.pow(10.0, value);
        }
    }

    private static class Pow10Scaling implements Scaling {

        private final Scaling log10Scaling = new Log10Scaling();

        @Override
        public double scale(double value) {
            return log10Scaling.scaleInverse(value);
        }

        @Override
        public double scaleInverse(double value) {
            return log10Scaling.scale(value);
        }
    }
}
