/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.utilities.rcp.actions;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.graphbuilder.rcp.dialogs.PromptDialog;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.metadata.MetadataViewTopComponent;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

@ActionID(category = "Metadata", id = "SearchMetadataAction" )
@ActionRegistration(
        displayName = "#CTL_SearchMetadataAction_MenuText",
        popupText = "#CTL_SearchMetadataAction_MenuText"
)
@ActionReferences({
        @ActionReference(path = "Context/Product/MetadataElement", position = 110),
        @ActionReference(path = "Menu/Tools/Metadata", position = 60)
})
@NbBundle.Messages({
        "CTL_SearchMetadataAction_MenuText=Search Metadata",
        "CTL_SearchMetadataAction_ShortDescription=Search Metadata"
})
/**
 * This action searches the Metadata by name
 *
 */
public class SearchMetadataAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;
    private Product product;

    public SearchMetadataAction() {
        this(Utilities.actionsGlobalContext());
    }

    public SearchMetadataAction(Lookup lkp) {
        super(Bundle.CTL_SearchMetadataAction_MenuText());
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_SearchMetadataAction_ShortDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new SearchMetadataAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    private void setEnableState() {
        ProductNode productNode = lkp.lookup(ProductNode.class);
        boolean state = false;
        if (productNode != null) {
            product = productNode.getProduct();
            state = product.getMetadataRoot() != null;
        }
        setEnabled(state);
    }

    @Override
    public void actionPerformed(final ActionEvent event) {

        final PromptDialog dlg = new PromptDialog("Search Metadata", "Item Name", "", false);
        dlg.show();
        if (dlg.IsOK()) {
            final String tag = dlg.getValue().toUpperCase();
            final MetadataElement resultElem = new MetadataElement("Search result (" + dlg.getValue() + ')');

            final boolean isModified = product.isModified();
            final MetadataElement root = product.getMetadataRoot();
            resultElem.setOwner(product);

            searchMetadata(resultElem, root, tag);
            product.setModified(isModified);

            if (resultElem.getNumElements() > 0 || resultElem.getNumAttributes() > 0) {
                openMetadataWindow(resultElem);
            } else {
                // no attributes found
                Dialogs.showError("Search Metadata", dlg.getValue() + " not found in the Metadata");
            }
        }
    }

    static MetadataViewTopComponent openMetadataWindow(final MetadataElement element) {
        final MetadataViewTopComponent metadataViewTopComponent = new MetadataViewTopComponent(element);
        DocumentWindowManager.getDefault().openWindow(metadataViewTopComponent);
        metadataViewTopComponent.requestSelected();
        return metadataViewTopComponent;
    }

    private static void searchMetadata(final MetadataElement resultElem, final MetadataElement elem, final String tag) {

        final MetadataElement[] elemList = elem.getElements();
        for (MetadataElement e : elemList) {
            searchMetadata(resultElem, e, tag);
        }
        final MetadataAttribute[] attribList = elem.getAttributes();
        for (MetadataAttribute attrib : attribList) {
            if (attrib.getName().toUpperCase().contains(tag)) {
                final MetadataAttribute newAttrib = attrib.createDeepClone();
                newAttrib.setDescription(getAttributePath(attrib));
                resultElem.addAttribute(newAttrib);
            }
        }
    }

    static String getAttributePath(final MetadataAttribute attrib) {
        MetadataElement parentElem = attrib.getParentElement();
        String path = parentElem.getName();
        while (parentElem != null && !parentElem.getName().equals("metadata")) {
            parentElem = parentElem.getParentElement();
            if (parentElem != null)
                path = parentElem.getName() + "/" + path;
        }
        return path;
    }
}
