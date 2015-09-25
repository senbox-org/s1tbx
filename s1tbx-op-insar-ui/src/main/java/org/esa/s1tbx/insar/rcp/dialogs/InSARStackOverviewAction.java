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
package org.esa.s1tbx.insar.rcp.dialogs;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(category = "Raster", id = "InSARStackOverviewAction")
@ActionRegistration(displayName = "#CTL_InSARStackOverviewActionName")
@ActionReference(path = "Menu/Radar/Interferometric", position = 600)
@NbBundle.Messages({
        "CTL_InSARStackOverviewActionName=InSAR Stack Overview",
        "CTL_InSARStackOverviewActionDescription=Show InSAR Stack Baselines"
})
public class InSARStackOverviewAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public InSARStackOverviewAction() {
        this(Utilities.actionsGlobalContext());
    }

    public InSARStackOverviewAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<Product> lkpContext = lkp.lookupResult(Product.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();

        putValue(NAME, Bundle.CTL_InSARStackOverviewActionName());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_InSARStackOverviewActionDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new InSARStackOverviewAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final Product product = lkp.lookup(Product.class);
        if (product != null) {
            final InSARStackOverviewDialog dialog = new InSARStackOverviewDialog();
            dialog.show();
        }
    }

    public void setEnableState() {
        final Product product = lkp.lookup(Product.class);
        if (product != null) {
            setEnabled(true);
            return;
        }
        setEnabled(false);
    }
}
