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
package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductPlacemarkView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;

/**
 * This action opens a placemark view.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class ShowPlacemarkViewAction extends ExecCommand {
    public static String ID = "showPlacemarkView";

    @Override
    public void actionPerformed(final CommandEvent event) {
        final VisatApp visatApp = VisatApp.getApp();
        ProductNode selectedProductNode = visatApp.getSelectedProductNode();
        if (selectedProductNode instanceof VectorDataNode) {
            VectorDataNode vectorDataNode = (VectorDataNode) selectedProductNode;
            openView(vectorDataNode);
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProductNode() instanceof VectorDataNode);
    }

    public ProductPlacemarkView openView(final VectorDataNode vectorDataNode) {
        ProductPlacemarkView placemarkView = new ProductPlacemarkView(vectorDataNode);
        openInternalFrame(placemarkView);
        return placemarkView;
    }

    // todo - code duplication in ShowMetadataViewAction (nf, 2012.01)
    public JInternalFrame openInternalFrame(ProductPlacemarkView placemarkView) {
        final VisatApp visatApp = VisatApp.getApp();

        visatApp.setStatusBarMessage("Creating placemark view...");
        visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        JInternalFrame metadataFrame = null;
        try {
            placemarkView.setCommandUIFactory(visatApp.getCommandUIFactory());
            final Icon icon = UIUtils.loadImageIcon("icons/RsMetaData16.gif");
            final ProductNode productNode = placemarkView.getVisibleProductNode();
            metadataFrame = visatApp.createInternalFrame(productNode.getDisplayName(),
                    icon,
                    placemarkView, null, false);
            final JInternalFrame internalFrame = metadataFrame;
            productNode.getProduct().addProductNodeListener(new ProductNodeListenerAdapter() {
                @Override
                public void nodeChanged(final ProductNodeEvent event) {
                    if (event.getSourceNode() == productNode &&
                            event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                        internalFrame.setTitle(productNode.getDisplayName());
                    }
                }
            });
            updateState();
        } catch (Exception e) {
            visatApp.handleUnknownException(e);
        }

        visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
        visatApp.clearStatusBarMessage();

        return metadataFrame;
    }
}
