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
 * @version $Revision: 1.9 $ $Date: 2011-04-08 18:23:59 $
 */
public class SearchMetadataAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {

        final PromptDialog dlg = new PromptDialog("Search Metadata", "Item Name", "", false);
        dlg.show();
        if(dlg.IsOK()) {
            final String tag = dlg.getValue().toUpperCase();
            final MetadataElement resultElem = new MetadataElement("Search result ("+dlg.getValue()+')');

            final Product product = VisatApp.getApp().getSelectedProduct();
            final boolean isModified = product.isModified();
            final MetadataElement root = product.getMetadataRoot();
            resultElem.setOwner(product);

            searchMetadata(resultElem, root, tag);
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

    private static void searchMetadata(final MetadataElement resultElem, final MetadataElement elem, final String tag) {

        final MetadataElement[] elemList = elem.getElements();
        for(MetadataElement e : elemList) {
            searchMetadata(resultElem, e, tag);
        }
        final MetadataAttribute[] attribList = elem.getAttributes();
        for(MetadataAttribute attrib : attribList) {
            if(attrib.getName().toUpperCase().contains(tag)) {
                final MetadataAttribute newAttrib = attrib.createDeepClone();
                newAttrib.setDescription(getAttributePath(attrib));
                resultElem.addAttribute(newAttrib);
            }
        }
    }

    static String getAttributePath(final MetadataAttribute attrib) {
        MetadataElement parentElem = attrib.getParentElement();
        String path = parentElem.getName();
        while(parentElem != null && !parentElem.getName().equals("metadata")) {
            parentElem = parentElem.getParentElement();
            if(parentElem != null)
                path = parentElem.getName() + "/" + path;
        }
        return path;
    }
}