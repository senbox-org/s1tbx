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
package org.esa.s1tbx.utilities.rcp.actions;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.rcp.actions.AbstractSnapAction;
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

import javax.swing.Action;
import java.awt.event.ActionEvent;

@ActionID(category = "Raster", id = "org.esa.s1tbx.utilities.rcp.actions.LinearTodBAction")
@ActionRegistration(displayName = "#CTL_LinearTodBAction_Text")
@ActionReferences({
        @ActionReference(
                path = "Menu/Raster/Data Conversion", position = 300
        ),
        @ActionReference(
                path = "Context/Product/RasterDataNode",
                position = 42
        )
})
@NbBundle.Messages({
        "CTL_LinearTodBAction_Text=Linear to/from dB",
        "CTL_LinearTodBAction_Description=Creates a dB or linear virtual band from a linear or dB band"
})
/**
 * LinearTodB action.
 */
public class LinearTodBAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private static final String dBStr = "_" + Unit.DB;
    private final Lookup lkp;

    public LinearTodBAction() {
        this(Utilities.actionsGlobalContext());
    }

    public LinearTodBAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();

        putValue(NAME, Bundle.CTL_LinearTodBAction_Text());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_LinearTodBAction_Description());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new LinearTodBAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }


    @Override
    public void actionPerformed(ActionEvent event) {

        final ProductNode productNode = lkp.lookup(ProductNode.class);
        if (productNode != null && productNode instanceof Band) {
            final Band band = (Band) productNode;
            final Product product = band.getProduct();
            final String unit = band.getUnit();

            if (!unit.contains(Unit.DB)) {

                if (Dialogs.requestDecision("Convert to dB", "Would you like to convert band "
                                                             + band.getName() + " into dB in a new virtual band?", true, null) == Dialogs.Answer.YES) {
                    convert(product, band, true);
                }
            } else {

                if (Dialogs.requestDecision("Convert to linear", "Would you like to convert band "
                                                                 + band.getName() + " into linear in a new virtual band?", true, null) == Dialogs.Answer.YES) {
                    convert(product, band, false);
                }
            }
        }
    }

    public void setEnableState() {
        final ProductNode productNode = lkp.lookup(ProductNode.class);
        if (productNode != null && productNode instanceof Band) {
            final Band band = (Band) productNode;
            final String unit = band.getUnit();
            if (unit != null && !unit.contains(Unit.PHASE)) {
                setEnabled(true);
                return;
            }
        }
        setEnabled(false);
    }

    public static void convert(final Product product, final Band band, final boolean todB) {
        String bandName = band.getName();
        String unit = band.getUnit();

        String expression;
        String newBandName;

        if (todB) {
            expression = bandName + "==0 ? 0 : 10 * log10(abs(" + bandName + "))";
            bandName += dBStr;
            unit += dBStr;
        } else {
            expression = "pow(10," + bandName + "/10.0)";
            if (bandName.contains(dBStr))
                bandName = bandName.substring(0, bandName.indexOf(dBStr));
            if (unit.contains(dBStr))
                unit = unit.substring(0, unit.indexOf(dBStr));
        }

        newBandName = bandName;
        int i = 2;
        while (product.getBand(newBandName) != null) {
            newBandName = bandName + i;
            ++i;
        }

        final VirtualBand virtBand = new VirtualBand(newBandName,
                                                     ProductData.TYPE_FLOAT32,
                                                     band.getRasterWidth(),
                                                     band.getRasterHeight(),
                                                     expression);
        virtBand.setUnit(unit);
        virtBand.setDescription(band.getDescription());
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
    }

}
