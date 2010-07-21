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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.text.MessageFormat;

public class CreateElevationBandAction extends ExecCommand {

    public static final String DIALOG_TITLE = "Create Elevation Band";
    public static final String DEFAULT_BAND_NAME = "elevation";

    @Override
    public void actionPerformed(CommandEvent event) {
        createAltitudeBand();
    }

    @Override
    public void updateState(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && product.getGeoCoding() != null);
    }

    private void createAltitudeBand() {
        final Product product = VisatApp.getApp().getSelectedProduct();
        final DialogData dialogData = promptForDem(product);
        if (dialogData == null) {
            return;
        }

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(dialogData.demName);
        if (demDescriptor == null) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, "The DEM '" + dialogData.demName + "' is not supported.");
            return;
        }
        if (demDescriptor.isInstallingDem()) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE,
                                              "The DEM '" + dialogData.demName + "' is currently being installed.");
            return;
        }
        if (!demDescriptor.isDemInstalled()) {
            demDescriptor.installDemFiles(VisatApp.getApp().getMainFrame());
            return;
        }


        createAndOpenElevationBand(product, demDescriptor, dialogData.bandName);
    }

    private void createAndOpenElevationBand(final Product product,
                                            final ElevationModelDescriptor demDescriptor,
                                            final String bandName) {


        final SwingWorker swingWorker = new SwingWorker<Band, Object>() {
            @Override
            protected Band doInBackground() throws Exception {
                ProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(), DIALOG_TITLE,
                                                               Dialog.ModalityType.APPLICATION_MODAL);
                try {
                    return createElevationBand(product, demDescriptor, bandName, pm);
                } finally {
                }
            }

            @Override
            public void done() {
                if (VisatApp.getApp().getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true))
                {
                    final Band band;
                    try {
                        band = get();
                        VisatApp.getApp().openProductSceneView(band);
                    } catch (Exception e) {
                        VisatApp.getApp().showErrorDialog(DIALOG_TITLE,
                                                          "An internal Error occured:\n" + e.getMessage());
                        Debug.trace(e);
                    }
                }
            }
        };

        swingWorker.execute();
    }

    private static Band createElevationBand(final Product product,
                                            final ElevationModelDescriptor demDescriptor,
                                            final String bandName,
                                            ProgressMonitor pm) {

        final ElevationModel dem = demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        final float noDataValue = dem.getDescriptor().getNoDataValue();
        final Band band = product.addBand(bandName, ProductData.TYPE_INT16);
        band.setSynthetic(true);
        band.setNoDataValue(noDataValue);
        band.setUnit("meters");
        band.setDescription(demDescriptor.getName());
        final int width = band.getSceneRasterWidth();
        final int height = band.getSceneRasterHeight();
        final short[] data = new short[width * height];
        band.setRasterData(ProductData.createInstance(data));
        pm.beginTask("Computing elevations for band '" + bandName + "'...", height);
        try {
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos = new PixelPos();
            float elevation;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixelPos.setLocation(x + 0.5f, y + 0.5f);
                    product.getGeoCoding().getGeoPos(pixelPos, geoPos);
                    try {
                        elevation = dem.getElevation(geoPos);
                    } catch (Exception e) {
                        elevation = noDataValue;
                    }
                    data[y * width + x] = (short) Math.round(elevation);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        return band;
    }

    private DialogData promptForDem(final Product product) {

        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        final String[] demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        final JList demList = new JList(demValueSet);
        demList.setSelectedIndex(0);
        demList.setVisibleRowCount(4);

        JPanel p1 = new JPanel(new BorderLayout(4, 4));
        p1.add(new JLabel("Elevation Model:"), BorderLayout.NORTH);     /*I18N*/
        p1.add(new JScrollPane(demList), BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout(4, 4));
        p2.add(new JLabel("Band name:"), BorderLayout.WEST);     /*I18N*/
        final JTextField nameField = new JTextField(DEFAULT_BAND_NAME);
        p2.add(nameField, BorderLayout.CENTER);

        JPanel p3 = new JPanel(new BorderLayout(4, 4));
        p3.add(p1, BorderLayout.NORTH);
        p3.add(p2, BorderLayout.SOUTH);

        final ModalDialog dialog = new CreateElevationBandDialog(nameField, product, demList);
        dialog.setContent(p3);

        if (dialog.show() == ModalDialog.ID_OK) {
            final DialogData dialogData = new DialogData();
            dialogData.bandName = nameField.getText().trim();
            dialogData.demName = demList.getSelectedValue().toString();
            return dialogData;
        }

        return null;
    }


    private static class DialogData {

        String bandName;
        String demName;
    }

    class CreateElevationBandDialog extends ModalDialog {
        private final JTextField nameField;
        private final Product product;
        private final JList demList;

        public CreateElevationBandDialog(JTextField nameField, Product product, JList demList) {
            super(VisatApp.getApp().getMainFrame(), CreateElevationBandAction.DIALOG_TITLE, ModalDialog.ID_OK_CANCEL_HELP, CreateElevationBandAction.this.getHelpId());
            this.nameField = nameField;
            this.product = product;
            this.demList = demList;
        }

        @Override
            protected boolean verifyUserInput() {
            String message = null;
            final String bandName = nameField.getText().trim();
            if (bandName.equals("")) {
                message = "Please enter a name for the new elevation band.";
            } else if (!ProductNode.isValidNodeName(bandName)) {
                message = MessageFormat.format("The band name ''{0}'' appears not to be valid.\n" +
                                               "Please choose another one.",
                                               bandName);
            } else if (product.containsBand(bandName)) {
                message = MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                               "Please choose another one.",
                                               bandName);
            } else if (demList.getSelectedValue() == null) {
                message = "Please select a DEM.";
            }
            if (message != null) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, message);
                return false;
            }
            return true;
        }
    }
}
