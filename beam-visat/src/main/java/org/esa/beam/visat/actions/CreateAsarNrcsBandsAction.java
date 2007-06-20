package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

public class CreateAsarNrcsBandsAction extends ExecCommand {

    public static final String DIALOG_TITLE = "Create ASAR NRCS Bands";
    public static final String NRCS_BAND_NAME = "nrcs";
    public static final String NRCS_DB_BAND_NAME = "nrcs_db";

    @Override
    public void actionPerformed(CommandEvent event) {
        createNrcsBands();
    }

    @Override
    public void updateState(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && D.create(product) != null);
    }

    private static void createNrcsBands() {
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product == null) {
            return;
        }
        D d = D.create(product);
        if (d == null) {
            return;
        }

        String nrcsExpression = "("
                                + d.procDataBand.getName()
                                + " * "
                                + d.procDataBand.getName()
                                + " / "
                                + d.extCalFactAttr.getData()
                                + ")" +
                                " * " +
                                "sin(rad("
                                + d.incidentAngleGrid.getName()
                                + "))";
        addVirtualBandToProduct(product,
                                NRCS_BAND_NAME, nrcsExpression,
                                "1", "Normalised Radar Cross Section");

        String nrcsDbExpression = "10 * log10(" + NRCS_BAND_NAME + ")";
        addVirtualBandToProduct(product,
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

        Band procDataBand;
        TiePointGrid incidentAngleGrid;
        MetadataAttribute extCalFactAttr;

        private D(Band procDataBand, TiePointGrid incidentAngleGrid, MetadataAttribute extCalFactAttr) {
            this.procDataBand = procDataBand;
            this.incidentAngleGrid = incidentAngleGrid;
            this.extCalFactAttr = extCalFactAttr;
        }

        public static D create(Product product) {
            Band procDataBand = product.getBand("proc_data");
            if (procDataBand == null) {
                procDataBand = product.getBand("proc_data_1");
            }
            if (procDataBand == null) {
                return null;
            }
            TiePointGrid incidentAngleGrid = product.getTiePointGrid("incident_angle");
            if (incidentAngleGrid == null) {
                return null;
            }

            MetadataElement ads = product.getMetadataRoot().getElement("MAIN_PROCESSING_PARAMS_ADS");
            MetadataAttribute extCalFactAttr = null;
            if (ads != null) {
                extCalFactAttr = ads.getAttribute("calibration_factors.1.ext_cal_fact");
                if (extCalFactAttr == null) {
                    ads = ads.getElement("MAIN_PROCESSING_PARAMS_ADS.1");
                    if (ads != null) {
                        extCalFactAttr = ads.getAttribute("calibration_factors.1.ext_cal_fact");
                    }
                }
            }
            if (extCalFactAttr == null) {
                return null;
            }

            return new D(procDataBand, incidentAngleGrid, extCalFactAttr);
        }
    }
}
