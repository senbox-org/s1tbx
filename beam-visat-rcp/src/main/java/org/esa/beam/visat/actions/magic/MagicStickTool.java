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

package org.esa.beam.visat.actions.magic;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.framework.ui.tool.ToolAdapter;
import org.esa.beam.framework.ui.tool.ToolEvent;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MagicStickTool extends AbstractTool {

    private double tolerance = 0.1;
    private static final String DIALOG_TITLE = "Magic Stick";
    private JDialog dialog;
    private ToolForm toolForm;

    public MagicStickTool() {
        addToolListener(new ToolAdapter() {
            @Override
            public void toolActivated(ToolEvent toolEvent) {
                if (dialog == null) {
                    initDialog();
                }
                dialog.setVisible(true);
            }

            @Override
            public void toolDeactivated(ToolEvent toolEvent) {
                if (dialog != null) {
                    dialog.setVisible(false);
                }
            }
        });
    }


    @Override
    public void mouseClicked(ToolInputEvent e) {
        if (!e.isPixelPosValid()) {
            return;
        }

        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        final Product product = view.getProduct();
        final Band[] bands = product.getBands();
        ArrayList<Band> selectedBandList = new ArrayList<Band>(bands.length);
        for (Band band : bands) {
            if (band.getSpectralBandwidth() > 0) {
                selectedBandList.add(band);
            }
        }
        if (selectedBandList.size() == 0) {
            VisatApp.getApp().showErrorDialog("No spectral bands found.");
            return;
        }


        final Band[] selectedBands = selectedBandList.toArray(new Band[selectedBandList.size()]);
        toolForm.getUnitLabel().setText(selectedBands[0].getUnit());

        final Point point = e.getPixelPos();
        final int pixelX = point.x;
        final int pixelY = point.y;
        final double[] spectrum;
        try {
            spectrum = getSpectrum(selectedBands, pixelX, pixelY);
        } catch (IOException e1) {
            return;
        }
        final ProgressMonitorSwingWorker<BufferedImage, Object> worker = new ProgressMonitorSwingWorker<BufferedImage, Object>(view, "Computing ROI") {
            @Override
            protected BufferedImage doInBackground(ProgressMonitor pm) throws Exception {
                return createRoiImage(spectrum, product, selectedBands, tolerance, pm);
            }

            @Override
            protected void done() {
                try {
                    final BufferedImage image = get();
                    // the following methods were removed in BEAM 4.7 (rq-20091102)
                    // view.setROIImage(image);
                    // view.setROIOverlayEnabled(true);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ExecutionException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                super.done();
            }
        };
        worker.execute();
    }

    private static BufferedImage createRoiImage(double[] spectrum, Product product, Band[] bands, double tolerance, ProgressMonitor pm) {
        final int rasterWidth = product.getSceneRasterWidth();
        final int rasterHeight = product.getSceneRasterHeight();
        final double[] scanLine = new double[rasterWidth];
        final double[] sqrSums = new double[rasterWidth];
        final Color color = Color.RED;
        // Create the result image
        final IndexColorModel cm = new IndexColorModel(8, 2,
                                                       new byte[]{0, (byte) color.getRed()},
                                                       new byte[]{0, (byte) color.getGreen()},
                                                       new byte[]{0, (byte) color.getBlue()},
                                                       0);
        final BufferedImage bi = new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_BYTE_INDEXED, cm);
        final byte[] imageData = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();

        pm.beginTask("Computing ROI pixels", rasterHeight);
        double dv, v;
        for (int y = 0; y < rasterHeight; y++) {
            for (int x = 0; x < rasterWidth; x++) {
                sqrSums[x] = 0;
            }
            for (int b = 0; b < bands.length; b++) {
                Band band = bands[b];
                if (!band.isFlagBand()) {
                    final double v0 = spectrum[b];
                    try {
                        band.readPixels(0, y, rasterWidth, 1, scanLine);
                        for (int x = 0; x < rasterWidth; x++) {
                            dv = v0 - scanLine[x];
                            sqrSums[x] += dv * dv;
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            for (int x = 0; x < rasterWidth; x++) {
                v = Math.sqrt(sqrSums[x] / (double) bands.length);
                imageData[y * rasterWidth + x] = (byte) (v < tolerance ? 1 : 0);
            }

            if (pm.isCanceled()) {
                break;
            }
            pm.worked(1);
        }
        pm.done();
        return bi;
    }

    private static double[] getSpectrum(Band[] bands, int pixelX, int pixelY) throws IOException {
        final double[] pixel = new double[1];
        final double[] spectrum = new double[bands.length];
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            spectrum[i] = 0.0f;
            if (!band.isFlagBand()) {
                band.readPixels(pixelX, pixelY, 1, 1, pixel, ProgressMonitor.NULL);
                if (band.isPixelValid(pixelX, pixelY)) {
                    spectrum[i] = pixel[0];
                }
            }
        }
        return spectrum;
    }
    private void initDialog() {
        toolForm = new ToolForm();
        dialog = new JDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, false);
        UIUtils.centerComponent(dialog, VisatApp.getApp().getMainFrame());
        dialog.getContentPane().add(toolForm.createPanel());
        toolForm.getToleranceField().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateTolerance();
                }
            });
       toolForm.getToleranceField().addFocusListener(new FocusListener() {
           public void focusGained(FocusEvent e) {
           }

           public void focusLost(FocusEvent e) {
               updateTolerance();
           }
       });
        dialog.pack();
    }

    private void updateTolerance() {
        try {
            this.tolerance = Double.parseDouble(toolForm.getToleranceField().getText());
        } catch (NumberFormatException e) {
            toolForm.getToleranceField().setText("0.1");
            updateTolerance();
        }
    }

    private class ToolForm  {
        private JTextField toleranceField;
        private JLabel unitLabel;

        private ToolForm() {
            toleranceField = new JTextField(10);
            toleranceField.setText(String.valueOf(tolerance));
            unitLabel = new JLabel("         ");
        }

        public JPanel createPanel() {

            final TableLayout tableLayout = new TableLayout(3);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setColumnWeightX(0, 0.1);
            tableLayout.setColumnWeightX(1, 0.1);
            tableLayout.setColumnWeightX(2, 0.8);
            tableLayout.setTablePadding(4,4);
            JPanel panel = new JPanel(tableLayout);
            panel.add(new JLabel("Tolerance:"));
            panel.add(toleranceField);
            panel.add(unitLabel);

            return panel;
        }

        public JTextField getToleranceField() {
            return toleranceField;
        }

        public JLabel getUnitLabel() {
            return unitLabel;
        }
    }

}