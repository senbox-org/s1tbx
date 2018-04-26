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
package org.esa.s1tbx.calibration.rcp;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Sigma0toBeta0Action action.
 */
@ActionID(category = "Raster", id = "Sigma0toBeta0Action")
@ActionRegistration(displayName = "#CTL_Sigma0toBeta0ActionName")
@ActionReference(path = "Menu/Radar/Radiometric", position = 600)
@NbBundle.Messages({
        "CTL_Sigma0toBeta0ActionName=Convert Sigma0 to Beta0",
        "CTL_Sigma0toBeta0ActionDescription=Creates a Beta0 virtual band from a Sigma0 band"
})
public class Sigma0toBeta0Action extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public Sigma0toBeta0Action() {
        this(Utilities.actionsGlobalContext());
    }

    public Sigma0toBeta0Action(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();

        putValue(NAME, Bundle.CTL_Sigma0toBeta0ActionName());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_Sigma0toBeta0ActionDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new Sigma0toBeta0Action(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final ProductNode productNode = lkp.lookup(ProductNode.class);
        if (productNode != null) {
            CalibrationOp.createBetaVirtualBand(productNode.getProduct(), false);
        }
    }

    public void setEnableState() {
        final ProductNode productNode = lkp.lookup(ProductNode.class);
        if (productNode != null && productNode instanceof Band) {
            final Band band = (Band) productNode;
            final String unit = band.getUnit();
            if (unit != null && unit.contains(Unit.INTENSITY) && band.getName().toLowerCase().contains("sigma")) {
                setEnabled(true);
                return;
            }
        }
        setEnabled(false);
    }
}
