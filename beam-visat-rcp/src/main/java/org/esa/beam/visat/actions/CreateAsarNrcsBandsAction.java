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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

public class CreateAsarNrcsBandsAction extends ExecCommand {

    public static final String DIALOG_TITLE = "Create ASAR NRCS Bands";
    public static final String NRCS_BAND_NAME = "nrcs";
    public static final String NRCS_DB_BAND_NAME = "nrcs_db";

    private static final String ATTRIBUTE_EXT_CAL_FACT = "ASAR_Main_ADSR.sd/calibration_factors.1.ext_cal_fact";

    @Override
    public void actionPerformed(CommandEvent event) {
        createNrcsBands();
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        setEnabled(node instanceof Band);
    }

    private static void createNrcsBands() {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();

        if (!(node instanceof Band)) {
            return;
        }
        Band sourceBand = (Band) node;
        D d = D.create(sourceBand);
        if (d == null) {
            return;
        }

        String nrcsExpression = "("
                                + d.sourceBand.getName()
                                + " * "
                                + d.sourceBand.getName()
                                + " / "
                                + d.extCalFactAttr.getData()
                                + ")" +
                                " * " +
                                "sin(rad("
                                + d.incidentAngleGrid.getName()
                                + "))";
        addVirtualBandToProduct(sourceBand.getProduct(),
                                NRCS_BAND_NAME, nrcsExpression,
                                "1", "Normalised Radar Cross Section");

        String nrcsDbExpression = "10 * log10(" + NRCS_BAND_NAME + ")";
        addVirtualBandToProduct(sourceBand.getProduct(),
                                NRCS_DB_BAND_NAME, nrcsDbExpression,
                                "dB", "Normalised Radar Cross Section in dB");
    }

    private static void addVirtualBandToProduct(Product product,
                                                String nrcsBandName, String nrcsExpression,
                                                String nrcsUnit, String nrcsDescription) {

        Band nrcsBand = product.getBand(nrcsBandName);
        if (nrcsBand instanceof VirtualBand) {
            VirtualBand vNrcsBand = (VirtualBand) nrcsBand;
            vNrcsBand.setExpression(nrcsExpression);
            vNrcsBand.setUnit(nrcsUnit);
            vNrcsBand.setDescription(nrcsDescription);
        } else {
            VirtualBand vNrcsBand = new VirtualBand(nrcsBandName,
                                                    ProductData.TYPE_FLOAT32,
                                                    product.getSceneRasterWidth(),
                                                    product.getSceneRasterHeight(),
                                                    nrcsExpression);
            vNrcsBand.setUnit(nrcsUnit);
            vNrcsBand.setDescription(nrcsDescription);
            product.addBand(vNrcsBand);
        }
    }


    private static class D {

        Band sourceBand;
        TiePointGrid incidentAngleGrid;
        MetadataAttribute extCalFactAttr;

        private D(Band sourceBand, TiePointGrid incidentAngleGrid, MetadataAttribute extCalFactAttr) {
            this.sourceBand = sourceBand;
            this.incidentAngleGrid = incidentAngleGrid;
            this.extCalFactAttr = extCalFactAttr;
        }

        public static D create(Band sourceBand) {
            TiePointGrid incidentAngleGrid = sourceBand.getProduct().getTiePointGrid("incident_angle");
            if (incidentAngleGrid == null) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, "Cant find tie-point grid 'incident_angle'.");
                return null;
            }

            MetadataElement ads = sourceBand.getProduct().getMetadataRoot().getElement("MAIN_PROCESSING_PARAMS_ADS");
            MetadataAttribute extCalFactAttr = null;
            if (ads != null) {
                extCalFactAttr = ads.getAttribute(ATTRIBUTE_EXT_CAL_FACT);
                if (extCalFactAttr == null) {
                    ads = ads.getElement("MAIN_PROCESSING_PARAMS_ADS.1");
                    if (ads != null) {
                        extCalFactAttr = ads.getAttribute(ATTRIBUTE_EXT_CAL_FACT);
                    }
                }
            }
            if (extCalFactAttr == null) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, "Cant find metadata 'MAIN_PROCESSING_PARAMS_ADS/" + ATTRIBUTE_EXT_CAL_FACT + "'.");
                return null;
            }

            return new D(sourceBand, incidentAngleGrid, extCalFactAttr);
        }
    }
}
