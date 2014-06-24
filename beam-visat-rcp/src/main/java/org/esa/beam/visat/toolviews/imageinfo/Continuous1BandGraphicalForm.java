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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.ui.ImageInfoEditorModel;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandGraphicalForm implements ColorManipulationChildForm {

    public static final Scaling POW10_SCALING = new Pow10Scaling();

    private final ColorManipulationForm parentForm;
    private final ImageInfoEditor2 imageInfoEditor;
    private final ImageInfoEditorSupport imageInfoEditorSupport;
    private final JPanel contentPanel;
    private final AbstractButton logDisplayButton;
    private final AbstractButton evenDistButton;
    private final MoreOptionsForm moreOptionsForm;
    private final DiscreteCheckBox discreteCheckBox;

    Continuous1BandGraphicalForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        imageInfoEditor = new ImageInfoEditor2(parentForm);
        imageInfoEditorSupport = new ImageInfoEditorSupport(imageInfoEditor);
        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(imageInfoEditor, BorderLayout.CENTER);
        moreOptionsForm = new MoreOptionsForm(parentForm, parentForm.getFormModel().canUseHistogramMatching());
        discreteCheckBox = new DiscreteCheckBox(parentForm);
        moreOptionsForm.addRow(discreteCheckBox);
        parentForm.getFormModel().modifyMoreOptionsForm(moreOptionsForm);

        logDisplayButton = LogDisplay.createButton();
        logDisplayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean shouldLog10Display = logDisplayButton.isSelected();
                if (shouldLog10Display) {
                    final ImageInfo imageInfo = parentForm.getFormModel().getModifiedImageInfo();
                    final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();
                    if (LogDisplay.checkApplicability(cpd)) {
                        setLogarithmicDisplay(parentForm.getFormModel().getRaster(), shouldLog10Display);
                        parentForm.applyChanges();
                    } else {
                        LogDisplay.showNotApplicableInfo(parentForm.getContentPanel());
                        logDisplayButton.setSelected(false);
                    }
                } else {
                    setLogarithmicDisplay(parentForm.getFormModel().getRaster(), shouldLog10Display);
                    parentForm.applyChanges();
                }
            }
        });

        evenDistButton = ImageInfoEditorSupport.createButton("icons/EvenDistribution24.gif");
        evenDistButton.setName("evenDistButton");
        evenDistButton.setToolTipText("Distribute sliders evenly between first and last slider"); /*I18N*/
        evenDistButton.addActionListener(parentForm.wrapWithAutoApplyActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                distributeSlidersEvenly();
            }
        }));
    }


    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(FormModel formModel) {
        updateFormModel(formModel);
    }

    @Override
    public void handleFormHidden(FormModel formModel) {
        if (imageInfoEditor.getModel() != null) {
            imageInfoEditor.setModel(null);
        }
    }

    @Override
    public void updateFormModel(FormModel formModel) {
        final ImageInfoEditorModel oldModel = imageInfoEditor.getModel();
        final ImageInfo imageInfo = parentForm.getFormModel().getModifiedImageInfo();
        final ImageInfoEditorModel newModel = new ImageInfoEditorModel1B(imageInfo);
        imageInfoEditor.setModel(newModel);

        final RasterDataNode raster = formModel.getRaster();
        setLogarithmicDisplay(raster, newModel.getImageInfo().isLogScaled());
        if (oldModel != null) {
            newModel.setHistogramViewGain(oldModel.getHistogramViewGain());
            newModel.setMinHistogramViewSample(oldModel.getMinHistogramViewSample());
            newModel.setMaxHistogramViewSample(oldModel.getMaxHistogramViewSample());
        }
        if (newModel.getSliderSample(0) < newModel.getMinHistogramViewSample() ||
            newModel.getSliderSample(newModel.getSliderCount() - 1) > newModel.getMaxHistogramViewSample()) {
            imageInfoEditor.computeZoomInToSliderLimits();
        }

        discreteCheckBox.setDiscreteColorsMode(imageInfo.getColorPaletteDef().isDiscrete());
        logDisplayButton.setSelected(newModel.getImageInfo().isLogScaled());
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void resetFormModel(FormModel formModel) {
        updateFormModel(formModel);
        imageInfoEditor.computeZoomOutToFullHistogramm();
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        final ImageInfoEditorModel model = imageInfoEditor.getModel();
        if (model != null) {
            if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
                updateFormModel(parentForm.getFormModel());
            } else {
                setLogarithmicDisplay(raster, model.getImageInfo().isLogScaled());
            }
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

    private void setLogarithmicDisplay(final RasterDataNode raster, final boolean logarithmicDisplay) {
        final ImageInfoEditorModel model = imageInfoEditor.getModel();
        if (logarithmicDisplay) {
            final StxFactory stxFactory = new StxFactory();
            final Stx stx = stxFactory
                        .withHistogramBinCount(raster.getStx().getHistogramBinCount())
                        .withLogHistogram(logarithmicDisplay)
                        .withResolutionLevel(raster.getSourceImage().getModel().getLevelCount() - 1)
                        .create(raster, ProgressMonitor.NULL);
            model.setDisplayProperties(raster.getName(), raster.getUnit(), stx, POW10_SCALING);
        } else {
            model.setDisplayProperties(raster.getName(), raster.getUnit(), raster.getStx(), Scaling.IDENTITY);
        }
        model.getImageInfo().setLogScaled(logarithmicDisplay);
    }

    private void distributeSlidersEvenly() {
        imageInfoEditor.distributeSlidersEvenly();
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                    imageInfoEditorSupport.autoStretch95Button,
                    imageInfoEditorSupport.autoStretch100Button,
                    imageInfoEditorSupport.zoomInVButton,
                    imageInfoEditorSupport.zoomOutVButton,
                    imageInfoEditorSupport.zoomInHButton,
                    imageInfoEditorSupport.zoomOutHButton,
                    logDisplayButton,
                    evenDistButton,
                    imageInfoEditorSupport.showExtraInfoButton,
        };
    }

    static void setDisplayProperties(ImageInfoEditorModel model, RasterDataNode raster) {
        model.setDisplayProperties(raster.getName(), raster.getUnit(), raster.getStx(),
                                   raster.isLog10Scaled() ? POW10_SCALING : Scaling.IDENTITY);
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
