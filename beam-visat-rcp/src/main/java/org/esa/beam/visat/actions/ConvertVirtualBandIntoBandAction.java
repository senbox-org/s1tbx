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
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.Stx;
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
public class ConvertVirtualBandIntoBandAction extends ExecCommand {

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProductNode() instanceof VirtualBand);
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();
        ProductNode selectedProductNode = visatApp.getSelectedProductNode();
        if (!(selectedProductNode instanceof VirtualBand)) {
            return;
        }
        VirtualBand virtualBand = (VirtualBand) selectedProductNode;
        String bandName = virtualBand.getName();
        int width = virtualBand.getSceneRasterWidth();
        int height = virtualBand.getSceneRasterHeight();
        String expression = virtualBand.getExpression();
        String validPixelExpression = virtualBand.getValidPixelExpression();
        ImageInfo imageInfo = virtualBand.getImageInfo().clone();
        Stx stx = null;
        if (virtualBand.isStxSet()) {
            stx = virtualBand.getStx();
        }

        Band realBand = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
        String description = createDescription(virtualBand.getDescription(), expression);
        realBand.setDescription(description);
        realBand.setUnit(virtualBand.getUnit());
        realBand.setSpectralWavelength(virtualBand.getSpectralWavelength());
        realBand.setGeophysicalNoDataValue(virtualBand.getGeophysicalNoDataValue());
        realBand.setNoDataValueUsed(virtualBand.isNoDataValueUsed());

        Product product = virtualBand.getProduct();
        final JInternalFrame[] internalFrames = visatApp.findInternalFrames(virtualBand);
        boolean productSceneViewOpen = false;
        for (final JInternalFrame internalFrame : internalFrames) {
            try {
                productSceneViewOpen = true;
                internalFrame.setClosed(true);
            } catch (PropertyVetoException e) {
                Debug.trace(e);
            }
        }
        product.removeBand(virtualBand);
        virtualBand.dispose();
        product.addBand(realBand);

        if (validPixelExpression != null && !validPixelExpression.isEmpty()) {
            expression = "(" + validPixelExpression + ") ? (" + expression + ") : NaN";
        }
        realBand.setSourceImage(VirtualBand.createVirtualSourceImage(realBand, expression));
        realBand.setStx(stx);
        realBand.setImageInfo(imageInfo);
        realBand.setModified(true);

        if (productSceneViewOpen) {
            visatApp.openProductSceneView(realBand);
        }
    }

    private String createDescription(String oldDescription, String expression) {
        String newDescription = oldDescription== null ? "" : oldDescription.trim();
        String formerExpressionDescription = "(expression was '" + expression + "')";
        newDescription = newDescription.isEmpty() ? formerExpressionDescription : newDescription + " " + formerExpressionDescription;
        return newDescription;
    }
}
