/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FilterBand;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import java.beans.PropertyVetoException;

/**
 * Converts a virtual band into a "real" band.
 *
 * @author marcoz
 */
public class ConvertComputedBandIntoBandAction extends ExecCommand {

    @Override
    public void updateState(final CommandEvent event) {
        ProductNode selectedProductNode = VisatApp.getApp().getSelectedProductNode();
        setEnabled(isComputedBand(selectedProductNode));
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();
        ProductNode selectedProductNode = visatApp.getSelectedProductNode();
        if (!isComputedBand(selectedProductNode)) {
            return;
        }

        Band computedBand = (Band) selectedProductNode;
        String bandName = computedBand.getName();
        int width = computedBand.getSceneRasterWidth();
        int height = computedBand.getSceneRasterHeight();

        Band realBand;
        if (selectedProductNode instanceof VirtualBand) {
            VirtualBand virtualBand = (VirtualBand) selectedProductNode;
            String expression = virtualBand.getExpression();
            realBand = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
            realBand.setDescription(createDescription(virtualBand.getDescription(), expression));
            realBand.setSourceImage(virtualBand.getSourceImage());
        } else if (selectedProductNode instanceof FilterBand) {
            FilterBand filterBand = (FilterBand) selectedProductNode;
            realBand = new Band(bandName, filterBand.getDataType(), width, height);
            realBand.setDescription(filterBand.getDescription());
            realBand.setValidPixelExpression(filterBand.getValidPixelExpression());
            realBand.setSourceImage(filterBand.getSourceImage());
        } else {
            throw new IllegalStateException();
        }

        realBand.setUnit(computedBand.getUnit());
        realBand.setSpectralWavelength(computedBand.getSpectralWavelength());
        realBand.setGeophysicalNoDataValue(computedBand.getGeophysicalNoDataValue());
        realBand.setNoDataValueUsed(computedBand.isNoDataValueUsed());
        if (computedBand.isStxSet()) {
            realBand.setStx(computedBand.getStx());
        }

        ImageInfo imageInfo = computedBand.getImageInfo();
        if (imageInfo != null) {
            realBand.setImageInfo(imageInfo.clone());
        }

        Product product = computedBand.getProduct();
        final JInternalFrame[] internalFrames = visatApp.findInternalFrames(computedBand);
        boolean productSceneViewOpen = false;
        for (final JInternalFrame internalFrame : internalFrames) {
            try {
                productSceneViewOpen = true;
                internalFrame.setClosed(true);
            } catch (PropertyVetoException e) {
                Debug.trace(e);
            }
        }
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        int bandIndex = bandGroup.indexOf(computedBand);
        bandGroup.remove(computedBand);
        bandGroup.add(bandIndex, realBand);

        realBand.setModified(true);

        if (productSceneViewOpen) {
            visatApp.openProductSceneView(realBand);
        }
    }

    private String createDescription(String oldDescription, String expression) {
        String newDescription = oldDescription == null ? "" : oldDescription.trim();
        String formerExpressionDescription = "(expression was '" + expression + "')";
        newDescription = newDescription.isEmpty() ? formerExpressionDescription : newDescription + " " + formerExpressionDescription;
        return newDescription;
    }

    private boolean isComputedBand(ProductNode selectedProductNode) {
        return selectedProductNode instanceof VirtualBand || selectedProductNode instanceof FilterBand;
    }

}
