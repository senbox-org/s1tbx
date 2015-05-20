/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.utilities.rcp;

import org.esa.s1tbx.utilities.gpf.ReplaceMetadataOp;
import org.esa.snap.dat.dialogs.StringSelectorDialog;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.util.ResourceUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@ActionID(
        category = "Processing",
        id = "org.esa.s1tbx.dat.ReplaceMetadataAction"
)
@ActionRegistration(displayName = "#CTL_ReplaceMetadataAction_Text")
@ActionReference(
        path = "Menu/Tools/Data Conversion",
        position = 400
)
@NbBundle.Messages({"CTL_ReplaceMetadataAction_Text=Replace Metadata"})
/**
 * This action replaces the Metadata with that of another product
 *
 */
public class ReplaceMetadataAction extends AbstractSnapAction {

    @Override
    public void actionPerformed(final ActionEvent event) {

        final Product destProduct = SnapApp.getDefault().getSelectedProduct();
        final String[] compatibleProductNames = getCompatibleProducts(destProduct);
        if (compatibleProductNames.length == 0) {
            SnapDialogs.showError("There are not any compatible products currently opened\nDimensions must be the same");
            return;
        }

        final StringSelectorDialog dlg = new StringSelectorDialog("Replace Metadata with", compatibleProductNames);
        dlg.show();
        if (dlg.IsOK()) {
            try {
                final MetadataElement origAbsRoot = AbstractMetadata.getAbstractedMetadata(destProduct);
                final int isPolsar = origAbsRoot.getAttributeInt(AbstractMetadata.polsarData, 0);
                final int isCalibrated = origAbsRoot.getAttributeInt(AbstractMetadata.abs_calibration_flag, 0);

                final String srcProductName = dlg.getSelectedItem();
                final Product[] products = SnapApp.getDefault().getProductManager().getProducts();

                Product srcProduct = null;
                for (Product prod : products) {
                    if (prod.getDisplayName().equals(srcProductName)) {
                        srcProduct = prod;
                        break;
                    }
                }

                final MetadataElement srcAbsRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);
                final File tmpMetadataFile = new File(ResourceUtils.getApplicationUserTempDataDir(),
                        srcProduct.getName() + "_metadata.xml");
                AbstractMetadataIO.Save(srcProduct, srcAbsRoot, tmpMetadataFile);

                clearProductMetadata(destProduct);
                SnapApp.getDefault().getProductManager().removeProduct(destProduct);

                final MetadataElement destAbsRoot = AbstractMetadata.getAbstractedMetadata(destProduct);
                AbstractMetadataIO.Load(destProduct, destAbsRoot, tmpMetadataFile);
                SnapApp.getDefault().getProductManager().addProduct(destProduct);

                ReplaceMetadataOp.resetPolarizations(AbstractMetadata.getAbstractedMetadata(destProduct),
                        isPolsar, isCalibrated);

                tmpMetadataFile.delete();
            } catch (Exception e) {
                SnapDialogs.showError("Unable to save or load metadata\n" + e.getMessage());
            }
        }
    }

    public void updateState(final CommandEvent event) {
        final Product product = SnapApp.getDefault().getSelectedProduct();
        setEnabled(product != null);
    }

    private static String[] getCompatibleProducts(final Product destProduct) {
        final List<String> prodList = new ArrayList<>();

        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
        for (Product p : products) {
            if (p != destProduct &&
                    p.getSceneRasterWidth() == destProduct.getSceneRasterWidth() &&
                    p.getSceneRasterHeight() == destProduct.getSceneRasterHeight()) {
                prodList.add(p.getDisplayName());
            }
        }
        return prodList.toArray(new String[prodList.size()]);
    }

    private static void clearProductMetadata(final Product product) {
        final String[] tpgNames = product.getTiePointGridNames();
        for (String tpg : tpgNames) {
            product.removeTiePointGrid(product.getTiePointGrid(tpg));
        }

        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement[] elems = root.getElements();
        for (MetadataElement e : elems) {
            root.removeElement(e);
        }
        AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());
    }

}
