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

import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(category = "Raster", id = "org.esa.s1tbx.dat.AmplitudeToIntensityAction")
@ActionRegistration(displayName = "#CTL_AmplitudeToIntensityAction_Text")
@ActionReference(path = "Menu/Raster/Data Conversion", position = 200)
@NbBundle.Messages({
        "CTL_AmplitudeToIntensityAction_Text=Amplitude to/from Intensity",
        "CTL_AmplitudeToIntensityAction_Description=Creates a virtual band from an Amplitude or Intensity band"
})
/**
 * AmplitudeToIntensity action.
 */
public class AmplitudeToIntensityAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public AmplitudeToIntensityAction() {
        this(Utilities.actionsGlobalContext());
    }

    public AmplitudeToIntensityAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();

        putValue(NAME, Bundle.CTL_AmplitudeToIntensityAction_Text());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_AmplitudeToIntensityAction_Description());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AmplitudeToIntensityAction(actionContext);
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
            String bandName = band.getName();
            final String unit = band.getUnit();

            if (unit != null && unit.contains(Unit.DB)) {
                SnapDialogs.showWarning("Please convert band " + bandName + " from dB to linear first");
                return;
            }

            if (unit != null && unit.contains(Unit.AMPLITUDE)) {

                bandName = replaceName(bandName, "Amplitude", "Intensity");
                if (product.getBand(bandName) != null) {
                    SnapDialogs.showWarning(product.getName() + " already contains an "
                                                    + bandName + " band");
                    return;
                }

                if (SnapDialogs.requestDecision("Convert to Intensity", "Would you like to convert band "
                        + band.getName() + " into Intensity in a new virtual band?", true, null) == SnapDialogs.Answer.YES) {
                    convert(product, band, false);
                }
            } else if (unit != null && unit.contains(Unit.INTENSITY)) {

                bandName = replaceName(bandName, "Intensity", "Amplitude");
                if (product.getBand(bandName) != null) {
                    SnapDialogs.showWarning(product.getName() + " already contains an "
                                                    + bandName + " band");
                    return;
                }
                if (SnapDialogs.requestDecision("Convert to Amplitude", "Would you like to convert band "
                        + band.getName() + " into Amplitude in a new virtual band?", true, null) == SnapDialogs.Answer.YES) {
                    convert(product, band, true);
                }
            }
        }
    }

    public void setEnableState() {
        final ProductNode productNode = lkp.lookup(ProductNode.class);
        if (productNode != null && productNode instanceof Band) {
            final Band band = (Band) productNode;
            final String unit = band.getUnit();
            if (unit != null && (unit.contains(Unit.AMPLITUDE) || unit.contains(Unit.INTENSITY))) {
                setEnabled(true);
                return;
            }
        }
        setEnabled(false);
    }

    private static String replaceName(String bandName, final String fromName, final String toName) {
        if (bandName.contains(fromName)) {
            bandName = bandName.replace(fromName, toName);
        } else if (bandName.contains("Sigma0")) {
            bandName = bandName.replace("Sigma0", toName);
        } else if (bandName.contains("Gamma0")) {
            bandName = bandName.replace("Gamma0", toName);
        } else if (bandName.contains("Beta0")) {
            bandName = bandName.replace("Beta0", toName);
        } else {
            bandName = toName + '_' + bandName;
        }
        return bandName;
    }

    public static void convert(final Product product, final Band band, final boolean toAmplitude) {
        String bandName = band.getName();
        String unit;

        String expression;
        if (toAmplitude) {
            expression = "sqrt(" + bandName + ')';
            bandName = replaceName(bandName, "Intensity", "Amplitude");
            unit = Unit.AMPLITUDE;
        } else {
            expression = bandName + " * " + bandName;
            bandName = replaceName(bandName, "Amplitude", "Intensity");
            unit = Unit.INTENSITY;
        }

        final VirtualBand virtBand = new VirtualBand(bandName,
                                                     ProductData.TYPE_FLOAT32,
                                                     band.getSceneRasterWidth(),
                                                     band.getSceneRasterHeight(),
                                                     expression);
        virtBand.setUnit(unit);
        virtBand.setDescription(band.getDescription());
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
    }

}
