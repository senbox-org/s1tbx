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
package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.imgfilter.model.Filter;


/**
 * Installs commands into VISAT which lets a user attach a {@link org.esa.beam.framework.datamodel.PixelGeoCoding} based on pixels rather than tie points to the current product.
 *
 * @author Norman Fomferra
 */
public class CreateFilteredBandAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        createFilteredBand();
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        event.getCommand().setEnabled(node instanceof Band);
    }

    private void createFilteredBand() {
        final CreateFilteredBandDialog.DialogData dialogData = promptForFilter();
        if (dialogData == null) {
            return;
        }
        final FilterBand filterBand = createFilterBand(dialogData.getFilter(), dialogData.getBandName(), dialogData.getIterationCount());
        VisatApp visatApp = VisatApp.getApp();
        if (visatApp.getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            visatApp.openProductSceneView(filterBand);
        }
    }

    private static FilterBand createFilterBand(Filter filter, String bandName, int iterationCount) {
        RasterDataNode sourceRaster = (RasterDataNode) VisatApp.getApp().getSelectedProductNode();

        FilterBand targetBand;
        Product product = sourceRaster.getProduct();

        if (filter.getOperation() == Filter.Operation.CONVOLVE) {
            targetBand = new ConvolutionFilterBand(bandName, sourceRaster, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        } else {
            GeneralFilterBand.OpType opType = getOpType(filter.getOperation());
            targetBand = new GeneralFilterBand(bandName, sourceRaster, opType, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        }

        targetBand.setDescription(String.format("Filter '%s' (=%s) applied to '%s'", filter.getName(), filter.getOperation(), sourceRaster.getName()));
        if (sourceRaster instanceof Band) {
            ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
        }
        product.addBand(targetBand);
        targetBand.fireProductNodeDataChanged();
        return targetBand;
    }

    private static Kernel getKernel(Filter filter) {
        return new Kernel(filter.getKernelWidth(),
                          filter.getKernelHeight(),
                          filter.getKernelOffsetX(),
                          filter.getKernelOffsetY(),
                          1.0 / filter.getKernelQuotient(),
                          filter.getKernelElements());
    }

    static GeneralFilterBand.OpType getOpType(Filter.Operation operation) {
        if (operation == Filter.Operation.OPEN) {
            return GeneralFilterBand.OpType.OPENING;
        } else if (operation == Filter.Operation.CLOSE) {
            return GeneralFilterBand.OpType.CLOSING;
        } else if (operation == Filter.Operation.ERODE) {
            return GeneralFilterBand.OpType.EROSION;
        } else if (operation == Filter.Operation.DILATE) {
            return GeneralFilterBand.OpType.DILATION;
        } else if (operation == Filter.Operation.MIN) {
            return GeneralFilterBand.OpType.MIN;
        } else if (operation == Filter.Operation.MAX) {
            return GeneralFilterBand.OpType.MAX;
        } else if (operation == Filter.Operation.MEAN) {
            return GeneralFilterBand.OpType.MEAN;
        } else if (operation == Filter.Operation.MEDIAN) {
            return GeneralFilterBand.OpType.MEDIAN;
        } else if (operation == Filter.Operation.STDDEV) {
            return GeneralFilterBand.OpType.STDDEV;
        } else {
            throw new IllegalArgumentException("illegal operation: " + operation);
        }
    }

    private CreateFilteredBandDialog.DialogData promptForFilter() {
        final ProductNode selectedNode = VisatApp.getApp().getSelectedProductNode();
        final Product product = selectedNode.getProduct();
        final CreateFilteredBandDialog dialog = new CreateFilteredBandDialog(product, selectedNode.getName(), getHelpId());
        if (dialog.show() == ModalDialog.ID_OK) {
            return dialog.getDialogData();
        }
        return null;
    }
}
