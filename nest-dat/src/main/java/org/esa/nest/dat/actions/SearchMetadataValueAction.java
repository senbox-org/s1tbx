/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.actions;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.PromptDialog;

/**
 * This action to searches the Metadata
 *
 * @author lveci
 * @version $Revision: 1.2 $ $Date: 2011-04-08 18:23:59 $
 */
public class SearchMetadataValueAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {

        final PromptDialog dlg = new PromptDialog("Search Metadata", "Value", "", false);
        dlg.show();
        if(dlg.IsOK()) {
            final String value = dlg.getValue().toUpperCase();
            final MetadataElement resultElem = new MetadataElement("Search result ("+dlg.getValue()+')');

            final Product product = VisatApp.getApp().getSelectedProduct();
            final boolean isModified = product.isModified();
            final MetadataElement root = product.getMetadataRoot();
            resultElem.setOwner(product);

            searchMetadataValue(resultElem, root, value);              
            product.setModified(isModified);

            if(resultElem.getNumElements() > 0 || resultElem.getNumAttributes() > 0) {
                VisatApp.getApp().createProductMetadataView(resultElem);
            } else {
                // no attributes found
                VisatApp.getApp().showErrorDialog("Search Metadata", dlg.getValue() + " not found in the Metadata");
            }
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        final int n = VisatApp.getApp().getProductManager().getProductCount();
        setEnabled(n > 0);
    }

    private static void searchMetadataValue(final MetadataElement resultElem, final MetadataElement elem, final String value) {

        final MetadataElement[] elemList = elem.getElements();
        for(MetadataElement e : elemList) {
            searchMetadataValue(resultElem, e, value);
        }
        final MetadataAttribute[] attribList = elem.getAttributes();
        for(MetadataAttribute attrib : attribList) {
            if(attrib.getData().getElemString().toUpperCase().contains(value)) {
                final MetadataAttribute newAttrib = attrib.createDeepClone();
                newAttrib.setDescription(SearchMetadataAction.getAttributePath(attrib));
                resultElem.addAttribute(newAttrib);
            }
        }
    }
}