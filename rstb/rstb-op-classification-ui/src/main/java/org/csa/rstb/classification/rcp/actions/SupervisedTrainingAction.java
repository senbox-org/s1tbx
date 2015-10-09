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
package org.csa.rstb.classification.rcp.actions;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.csa.rstb.classification.rcp.dialogs.ProductGeometrySelectorDialog;
import org.csa.rstb.polarimetric.gpf.DualPolOpUtils;
import org.csa.rstb.polarimetric.gpf.PolOpUtils;
import org.esa.s1tbx.io.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.gpf.ProcessTimeMonitor;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@ActionID(
        category = "Raster",
        id = "SupervisedTrainingAction"
)
@ActionRegistration(
        displayName = "#CTL_SupervisedTrainingAction_MenuText",
        popupText = "#CTL_SupervisedTrainingAction_MenuText",
        lazy = true
)
@ActionReference(path = "Menu/Radar/Polarimetric", position = 600)
@NbBundle.Messages({
        "CTL_SupervisedTrainingAction_MenuText=Supervised Classification Training",
        "CTL_SupervisedTrainingAction_ShortDescription=Supervised Classification Training"
})
/**
 * Supervised Training action.
 */
public class SupervisedTrainingAction extends AbstractAction {

    private static final int windowSize = 5;
    private static final int halfWindowSize = windowSize / 2;

    private static int sourceImageWidth;
    private static int sourceImageHeight;
    private static boolean isDualPol = false;

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            final ProductGeometrySelectorDialog dlg = new ProductGeometrySelectorDialog("Select Training Geometries");
            dlg.show();
            if (dlg.IsOK()) {
                final Product sourceProduct = dlg.getProduct();
                sourceImageWidth = sourceProduct.getSceneRasterWidth();
                sourceImageHeight = sourceProduct.getSceneRasterHeight();

                PolBandUtils.MATRIX sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
                if (sourceProductType != PolBandUtils.MATRIX.T3 &&
                        sourceProductType != PolBandUtils.MATRIX.C3 &&
                        sourceProductType != PolBandUtils.MATRIX.FULL) {

                    if (sourceProductType != PolBandUtils.MATRIX.DUAL_HH_HV &&
                            sourceProductType != PolBandUtils.MATRIX.DUAL_VH_VV &&
                            sourceProductType != PolBandUtils.MATRIX.DUAL_HH_VV &&
                            sourceProductType != PolBandUtils.MATRIX.C2) {

                        SnapDialogs.showError("Quad-pol or dual-pol product is expected");
                        return;

                    } else {
                        isDualPol = true;
                    }
                }

                PolBandUtils.PolSourceBand[] srcBandList =
                        PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

                final ProgressMonitorSwingWorker worker = new TrainingSwingWorker(sourceProduct,
                                                                                  dlg.getRoiProduct(),
                                                                                  dlg.getSelectedGeometries(),
                                                                                  dlg.getSaveFile(),
                                                                                  srcBandList[0].srcBands,
                                                                                  sourceProductType);
                worker.executeWithBlocking();
            }
        } catch (Exception e) {
            SnapDialogs.showError(e.getMessage());
        }
    }

    private static class TrainingSwingWorker extends ProgressMonitorSwingWorker {

        private final Product sourceProduct;
        private final Product roiProduct;
        private final String[] geometries;
        private final File file;
        private final Band[] sourceBands;
        private final PolBandUtils.MATRIX sourceProductType;
        private Throwable error;
        private final ProcessTimeMonitor timeMonitor = new ProcessTimeMonitor();

        private TrainingSwingWorker(final Product sourceProduct, final Product roiProduct,
                                    final String[] geometries, final File file,
                                    final Band[] sourceBands, final PolBandUtils.MATRIX sourceProductType) {
            super(SnapApp.getDefault().getMainFrame(), "Training...");
            this.sourceProduct = sourceProduct;
            this.roiProduct = roiProduct;
            this.geometries = geometries;
            this.file = file;
            this.sourceBands = sourceBands;
            this.sourceProductType = sourceProductType;
        }

        @Override
        protected Boolean doInBackground(final ProgressMonitor pm) throws Exception {

            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];
            final double[][] Cr = new double[2][2];
            final double[][] Ci = new double[2][2];
            PrintStream out = null;
            timeMonitor.start();

            final String[] subGeometries = createSubGeometries(roiProduct, geometries);
            try {
                out = new PrintStream(new FileOutputStream(file.getAbsolutePath(), false));
                out.println("number_of_clusters = " + subGeometries.length);
                out.println();
                final String title = "Generating Supervised Training Dataset... ";

                // find task length
                int totalRows = 0;
                for (final String geom : subGeometries) {
                    final VectorDataNode vec = roiProduct.getVectorDataGroup().get(geom);
                    final int minY = Math.min(sourceImageWidth, Math.max(0, (int) vec.getEnvelope().getMinY() - 1));
                    final int maxY = Math.min(sourceImageHeight, (int) vec.getEnvelope().getMaxY() + 1);
                    totalRows += (maxY - minY);
                }

                pm.beginTask(title, totalRows);
                int k = 0;
                for (final String geom : subGeometries) {

                    final Mask band = roiProduct.getMaskGroup().get(geom);
                    final VectorDataNode vec = roiProduct.getVectorDataGroup().get(geom);
                    final int minX = Math.min(sourceImageWidth, Math.max(0, (int) vec.getEnvelope().getMinX() - 1));
                    final int minY = Math.min(sourceImageWidth, Math.max(0, (int) vec.getEnvelope().getMinY() - 1));
                    final int maxX = Math.min(sourceImageWidth, (int) vec.getEnvelope().getMaxX() + 1);
                    final int maxY = Math.min(sourceImageHeight, (int) vec.getEnvelope().getMaxY() + 1);

                    int counter = 0;

                    final int width = maxX - minX;
                    final int height = maxY - minY;
                    if (width <= 0 || height <= 0) {
                        pm.worked(height);
                        continue;
                    }

                    final int[] data = new int[width];

                    if (!isDualPol) { // quad-pol

                        double t11 = 0.0, t12Re = 0.0, t12Im = 0.0, t13Re = 0.0, t13Im = 0.0;
                        double t22 = 0.0, t23Re = 0.0, t23Im = 0.0, t33 = 0.0;

                        for (int y = minY; y < maxY; ++y) {
                            if (pm.isCanceled()) {
                                error = new Exception("Training cancelled by user");
                                return false;
                            }
                            final int pct = (int) (((y - minY) / (float) height) * 100);
                            pm.setTaskName(title + geom + ' ' + pct + '%');

                            band.readPixels(minX, y, width, 1, data);
                            for (int x = minX; x < maxX; ++x) {
                                if (data[x - minX] != 0) {

                                    getMeanCoherencyMatrix(x, y, halfWindowSize, sourceImageWidth,
                                                           sourceImageHeight, sourceProductType, sourceBands, Tr, Ti);

                                    t11 += Tr[0][0];
                                    t12Re += Tr[0][1];
                                    t12Im += Ti[0][1];
                                    t13Re += Tr[0][2];
                                    t13Im += Ti[0][2];
                                    t22 += Tr[1][1];
                                    t23Re += Tr[1][2];
                                    t23Im += Ti[1][2];
                                    t33 += Tr[2][2];

                                    counter++;
                                }
                            }
                            pm.worked(1);
                        }

                        t11 /= counter;
                        t12Re /= counter;
                        t12Im /= counter;
                        t13Re /= counter;
                        t13Im /= counter;
                        t22 /= counter;
                        t23Re /= counter;
                        t23Im /= counter;
                        t33 /= counter;

                        out.println("cluster" + k + " = " + geom);
                        out.println();
                        out.println("cluster" + k + "_T11 = " + t11);
                        out.println("cluster" + k + "_T12_real = " + t12Re);
                        out.println("cluster" + k + "_T12_imag = " + t12Im);
                        out.println("cluster" + k + "_T13_real = " + t13Re);
                        out.println("cluster" + k + "_T13_imag = " + t13Im);
                        out.println("cluster" + k + "_T22 = " + t22);
                        out.println("cluster" + k + "_T23_real = " + t23Re);
                        out.println("cluster" + k + "_T23_imag = " + t23Im);
                        out.println("cluster" + k + "_T33 = " + t33);
                        out.println("pixels = " + counter);
                        out.println();
                        k++;

                    } else { // dual-pol

                        double c11 = 0.0, c12Re = 0.0, c12Im = 0.0, c22 = 0.0;

                        for (int y = minY; y < maxY; ++y) {
                            if (pm.isCanceled()) {
                                error = new Exception("Training cancelled by user");
                                return false;
                            }
                            final int pct = (int) (((y - minY) / (float) height) * 100);
                            pm.setTaskName(title + geom + ' ' + pct + '%');

                            band.readPixels(minX, y, width, 1, data);
                            for (int x = minX; x < maxX; ++x) {
                                if (data[x - minX] != 0) {

                                    getMeanCovarianceMatrixC2(x, y, halfWindowSize, sourceImageWidth,
                                                              sourceImageHeight, sourceProductType, sourceBands, Cr, Ci);

                                    c11 += Cr[0][0];
                                    c12Re += Cr[0][1];
                                    c12Im += Ci[0][1];
                                    c22 += Cr[1][1];

                                    counter++;
                                }
                            }
                            pm.worked(1);
                        }

                        c11 /= counter;
                        c12Re /= counter;
                        c12Im /= counter;
                        c22 /= counter;

                        out.println("cluster" + k + " = " + geom);
                        out.println();
                        out.println("cluster" + k + "_C11 = " + c11);
                        out.println("cluster" + k + "_C12_real = " + c12Re);
                        out.println("cluster" + k + "_C12_imag = " + c12Im);
                        out.println("cluster" + k + "_C22 = " + c22);
                        out.println("pixels = " + counter);
                        out.println();
                        k++;
                    }
                }

                return true;

            } catch (Throwable e) {
                e.printStackTrace();
                error = e;
                return false;
            } finally {
                if (out != null)
                    out.close();
                pm.done();

                removeSubGeometries(roiProduct, subGeometries);
            }
        }

        private static String[] createSubGeometries(final Product product, final String[] geometries) {
            final List<String> subGeometries = new ArrayList<>(geometries.length);
            try {
                for (String geometry : geometries) {
                    final VectorDataNode vec = product.getVectorDataGroup().get(geometry);
                    final FeatureCollection featCollection = vec.getFeatureCollection();
                    int i = 1;
                    FeatureIterator f = featCollection.features();
                    while (f.hasNext()) {
                        final SimpleFeature feature = (SimpleFeature) f.next();

                        final String subGeomName = geometry + '_' + i;
                        final VectorDataNode vectorDataNode = new VectorDataNode(subGeomName, vec.getFeatureType());
                        vectorDataNode.getFeatureCollection().add(feature);
                        product.getVectorDataGroup().add(vectorDataNode);
                        subGeometries.add(subGeomName);
                        ++i;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return subGeometries.toArray(new String[subGeometries.size()]);
        }

        private static void removeSubGeometries(final Product product, final String[] subGeometries) {
            try {
                for (String subGeom : subGeometries) {
                    final VectorDataNode vectorDataNode = product.getVectorDataGroup().get(subGeom);
                    product.getVectorDataGroup().remove(vectorDataNode);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void done() {
            roiProduct.setModified(false);
            try {
                final long duration = timeMonitor.stop();
                final Boolean isOk = (Boolean) this.get();
                if (isOk) {
                    final String durationStr = "Processing completed in " + ProcessTimeMonitor.formatDuration(duration);
                    SnapDialogs.showInformation("Done",
                                                "Supervised Training Dataset Completed\n" + file.getAbsolutePath() + "\n\n" + durationStr, null);
                } else {
                    SnapDialogs.showError("An error occurred\n" + error.getMessage());
                }
            } catch (Exception e) {
                SnapDialogs.showError("An error occurred\n" + e.getMessage());
            }
        }
    }

    private static void getMeanCoherencyMatrix(final int x, final int y, final int halfWindowSize,
                                               final int sourceImageWidth, final int sourceImageHeight,
                                               final PolBandUtils.MATRIX sourceProductType,
                                               final Band[] sourceBands, final double[][] Tr, final double[][] Ti)
            throws Exception {

        final int xSt = Math.max(x - halfWindowSize, 0);
        final int xEd = Math.min(x + halfWindowSize, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSize, 0);
        final int yEd = Math.min(y + halfWindowSize, sourceImageHeight - 1);
        final int w = xEd - xSt + 1;
        final int h = yEd - ySt + 1;
        final int num = w * h;

        final Matrix TrMat = new Matrix(3, 3);
        final Matrix TiMat = new Matrix(3, 3);
        double[][] Sr = new double[2][2];
        double[][] Si = new double[2][2];
        double[][] tempTr = new double[3][3];
        double[][] tempTi = new double[3][3];
        double[][] tempCr = new double[3][3];
        double[][] tempCi = new double[3][3];

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[] i_hh = new double[num];
            final double[] q_hh = new double[num];
            final double[] i_hv = new double[num];
            final double[] q_hv = new double[num];
            final double[] i_vh = new double[num];
            final double[] q_vh = new double[num];
            final double[] i_vv = new double[num];
            final double[] q_vv = new double[num];

            sourceBands[0].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, i_hh);
            sourceBands[1].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, q_hh);
            sourceBands[2].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, i_hv);
            sourceBands[3].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, q_hv);
            sourceBands[4].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, i_vh);
            sourceBands[5].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, q_vh);
            sourceBands[6].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, i_vv);
            sourceBands[7].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, q_vv);

            for (int i = 0; i < num; ++i) {
                Sr[0][0] = i_hh[i];
                Si[0][0] = q_hh[i];
                Sr[0][1] = i_hv[i];
                Si[0][1] = q_hv[i];
                Sr[1][0] = i_vh[i];
                Si[1][0] = q_vh[i];
                Sr[1][1] = i_vv[i];
                Si[1][1] = q_vv[i];

                PolOpUtils.computeCoherencyMatrixT3(Sr, Si, tempTr, tempTi);

                TrMat.plusEquals(new Matrix(tempTr));
                TiMat.plusEquals(new Matrix(tempTi));
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            final double[] c11 = new double[num];
            final double[] c12r = new double[num];
            final double[] c12i = new double[num];
            final double[] c13r = new double[num];
            final double[] c13i = new double[num];
            final double[] c22 = new double[num];
            final double[] c23r = new double[num];
            final double[] c23i = new double[num];
            final double[] c33 = new double[num];

            sourceBands[0].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c11);
            sourceBands[1].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c12r);
            sourceBands[2].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c12i);
            sourceBands[3].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c13r);
            sourceBands[4].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c13i);
            sourceBands[5].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c22);
            sourceBands[6].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c23r);
            sourceBands[7].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c23i);
            sourceBands[8].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c33);

            for (int i = 0; i < num; ++i) {
                tempCr[0][0] = c11[i]; // C11 - real
                tempCi[0][0] = 0.0;    // C11 - imag
                tempCr[0][1] = c12r[i];// C12 - real
                tempCi[0][1] = c12i[i];// C12 - imag
                tempCr[0][2] = c13r[i];// C13 - real
                tempCi[0][2] = c13i[i];// C13 - imag
                tempCr[1][1] = c22[i]; // C22 - real
                tempCi[1][1] = 0.0;    // C22 - imag
                tempCr[1][2] = c23r[i];// C23 - real
                tempCi[1][2] = c23i[i];// C23 - imag
                tempCr[2][2] = c33[i]; // C33 - real
                tempCi[2][2] = 0.0;    // C33 - imag
                tempCr[1][0] = tempCr[0][1];
                tempCi[1][0] = -tempCi[0][1];
                tempCr[2][0] = tempCr[0][2];
                tempCi[2][0] = -tempCi[0][2];
                tempCr[2][1] = tempCr[1][2];
                tempCi[2][1] = -tempCi[1][2];

                PolOpUtils.c3ToT3(tempCr, tempCi, tempTr, tempTi);

                TrMat.plusEquals(new Matrix(tempTr));
                TiMat.plusEquals(new Matrix(tempTi));
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            final double[] t11 = new double[num];
            final double[] t12r = new double[num];
            final double[] t12i = new double[num];
            final double[] t13r = new double[num];
            final double[] t13i = new double[num];
            final double[] t22 = new double[num];
            final double[] t23r = new double[num];
            final double[] t23i = new double[num];
            final double[] t33 = new double[num];

            sourceBands[0].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t11);
            sourceBands[1].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t12r);
            sourceBands[2].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t12i);
            sourceBands[3].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t13r);
            sourceBands[4].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t13i);
            sourceBands[5].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t22);
            sourceBands[6].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t23r);
            sourceBands[7].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t23i);
            sourceBands[8].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, t33);

            for (int i = 0; i < num; ++i) {
                tempTr[0][0] = t11[i]; // T11 - real
                tempTi[0][0] = 0.0;    // T11 - imag
                tempTr[0][1] = t12r[i];// T12 - real
                tempTi[0][1] = t12i[i];// T12 - imag
                tempTr[0][2] = t13r[i];// T13 - real
                tempTi[0][2] = t13i[i];// T13 - imag
                tempTr[1][1] = t22[i]; // T22 - real
                tempTi[1][1] = 0.0;    // T22 - imag
                tempTr[1][2] = t23r[i];// T23 - real
                tempTi[1][2] = t23i[i];// T23 - imag
                tempTr[2][2] = t33[i]; // T33 - real
                tempTi[2][2] = 0.0;    // T33 - imag
                tempTr[1][0] = tempTr[0][1];
                tempTi[1][0] = -tempTi[0][1];
                tempTr[2][0] = tempTr[0][2];
                tempTi[2][0] = -tempTi[0][2];
                tempTr[2][1] = tempTr[1][2];
                tempTi[2][1] = -tempTi[1][2];

                TrMat.plusEquals(new Matrix(tempTr));
                TiMat.plusEquals(new Matrix(tempTi));
            }
        }

        TrMat.timesEquals(1.0 / num);
        TiMat.timesEquals(1.0 / num);
        copyMatrix(TrMat, Tr);
        copyMatrix(TiMat, Ti);
    }

    private static void getMeanCovarianceMatrixC2(final int x, final int y, final int halfWindowSize,
                                                  final int sourceImageWidth, final int sourceImageHeight,
                                                  final PolBandUtils.MATRIX sourceProductType, final Band[] sourceBands,
                                                  final double[][] Cr, final double[][] Ci)
            throws Exception {

        final int xSt = Math.max(x - halfWindowSize, 0);
        final int xEd = Math.min(x + halfWindowSize, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSize, 0);
        final int yEd = Math.min(y + halfWindowSize, sourceImageHeight - 1);
        final int w = xEd - xSt + 1;
        final int h = yEd - ySt + 1;
        final int num = w * h;

        final Matrix CrMat = new Matrix(2, 2);
        final Matrix CiMat = new Matrix(2, 2);
        final double[] tempKr = new double[2];
        final double[] tempKi = new double[2];
        double[][] tempCr = new double[2][2];
        double[][] tempCi = new double[2][2];

        if (sourceProductType == PolBandUtils.MATRIX.DUAL_HH_HV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_VH_VV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_VV) {

            final double[] K0_i = new double[num];
            final double[] K0_q = new double[num];
            final double[] K1_i = new double[num];
            final double[] K1_q = new double[num];

            sourceBands[0].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, K0_i);
            sourceBands[1].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, K0_q);
            sourceBands[2].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, K1_i);
            sourceBands[3].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, K1_q);

            for (int i = 0; i < num; ++i) {
                tempKr[0] = K0_i[i];
                tempKi[0] = K0_q[i];
                tempKr[1] = K1_i[i];
                tempKi[1] = K1_q[i];

                DualPolOpUtils.computeCovarianceMatrixC2(tempKr, tempKi, tempCr, tempCi);

                CrMat.plusEquals(new Matrix(tempCr));
                CiMat.plusEquals(new Matrix(tempCi));
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C2) {

            final double[] c11 = new double[num];
            final double[] c12r = new double[num];
            final double[] c12i = new double[num];
            final double[] c22 = new double[num];

            sourceBands[0].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c11);
            sourceBands[1].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c12r);
            sourceBands[2].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c12i);
            sourceBands[3].getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, c22);

            for (int i = 0; i < num; ++i) {
                tempCr[0][0] = c11[i]; // C11 - real
                tempCi[0][0] = 0.0;    // C11 - imag
                tempCr[0][1] = c12r[i];// C12 - real
                tempCi[0][1] = c12i[i];// C12 - imag
                tempCr[1][1] = c22[i]; // C22 - real
                tempCi[1][1] = 0.0;    // C22 - imag
                tempCr[1][0] = tempCr[0][1];
                tempCi[1][0] = -tempCi[0][1];

                CrMat.plusEquals(new Matrix(tempCr));
                CiMat.plusEquals(new Matrix(tempCi));
            }
        }

        CrMat.timesEquals(1.0 / num);
        CiMat.timesEquals(1.0 / num);
        copyMatrix(CrMat, Cr);
        copyMatrix(CiMat, Ci);
    }

    /**
     * copy matrix with loop unwinding
     *
     * @param mat Matrix input
     * @param T   double[][] output
     */
    public static void copyMatrix(final Matrix mat, final double[][] T) {

        for (int i = 0; i < mat.getRowDimension(); i++) {
            for (int j = 0; j < mat.getColumnDimension(); j++) {
                T[i][j] = mat.get(i, j);
            }
        }
    }

}
