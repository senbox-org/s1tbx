/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.dat.actions;

import Jama.Matrix;
import org.csa.rstb.dat.dialogs.ProductGeometrySelectorDialog;
import org.csa.rstb.gpf.PolOpUtils;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.util.ProcessTimeMonitor;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Supervised Training action.
 *
 */
public class SupervisedTrainingAction extends AbstractVisatAction {

    private static final int windowSize = 5;
    private static final int halfWindowSize = windowSize/2;

    private static final double[][] Sr = new double[2][2];
    private static final double[][] Si = new double[2][2];
    private static final double[][] tempCr = new double[3][3];
    private static final double[][] tempCi = new double[3][3];
    private static final double[][] tempTr = new double[3][3];
    private static final double[][] tempTi = new double[3][3];


    private static int sourceImageWidth;
    private static int sourceImageHeight;

    @Override
    public void actionPerformed(CommandEvent event) {
        try {
            final ProductGeometrySelectorDialog dlg = new ProductGeometrySelectorDialog("Select Training Geometries");
            dlg.show();
            if(dlg.IsOK()) {
                final Product quadPolProduct = dlg.getQuadPolProduct();
                sourceImageWidth = quadPolProduct.getSceneRasterWidth();
                sourceImageHeight = quadPolProduct.getSceneRasterHeight();

                PolBandUtils.MATRIX sourceProductType = PolBandUtils.getSourceProductType(quadPolProduct);

                PolBandUtils.QuadSourceBand[] srcBandList =
                        PolBandUtils.getSourceBands(quadPolProduct, sourceProductType);

                final ProgressMonitorSwingWorker worker = new TrainingSwingWorker(quadPolProduct,
                                                                    dlg.getRoiProduct(),
                                                                    dlg.getSelectedGeometries(),
                                                                    dlg.getSaveFile(),
                                                                    srcBandList[0].srcBands,
                                                                    sourceProductType);
                worker.executeWithBlocking();
            }
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
        }
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    @Override
    public void updateState(CommandEvent event) {
        setEnabled(VisatApp.getApp().getProductManager().getProductCount() > 0);
    }

    private static class TrainingSwingWorker extends ProgressMonitorSwingWorker {

        private final Product quadPolProduct;
        private final Product roiProduct;
        private final String[] geometries;
        private final File file;
        private final Band[] sourceBands;
        private final PolBandUtils.MATRIX sourceProductType;
        private Throwable error;
        private final ProcessTimeMonitor timeMonitor = new ProcessTimeMonitor();

        private TrainingSwingWorker(final Product quadPolProduct, final Product roiProduct,
                                    final String[] geometries, final File file,
                                    final Band[] sourceBands, final PolBandUtils.MATRIX sourceProductType) {
            super(VisatApp.getApp().getMainFrame(), "Training...");
            this.quadPolProduct = quadPolProduct;
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
                for(final String geom : subGeometries) {
                    final VectorDataNode vec = roiProduct.getVectorDataGroup().get(geom);
                    final int minY = Math.min(sourceImageWidth, Math.max(0, (int)vec.getEnvelope().getMinY()-1));
                    final int maxY = Math.min(sourceImageHeight, (int)vec.getEnvelope().getMaxY()+1);
                    totalRows += (maxY - minY);
                }

                pm.beginTask(title, totalRows);
                int k = 0;
                for(final String geom : subGeometries) {

                    final Mask band = roiProduct.getMaskGroup().get(geom);
                    final VectorDataNode vec = roiProduct.getVectorDataGroup().get(geom);
                    final int minX = Math.min(sourceImageWidth, Math.max(0, (int)vec.getEnvelope().getMinX()-1));
                    final int minY = Math.min(sourceImageWidth, Math.max(0, (int)vec.getEnvelope().getMinY()-1));
                    final int maxX = Math.min(sourceImageWidth, (int)vec.getEnvelope().getMaxX()+1);
                    final int maxY = Math.min(sourceImageHeight, (int)vec.getEnvelope().getMaxY()+1);

                    double t11 = 0.0, t12Re = 0.0, t12Im = 0.0, t13Re = 0.0, t13Im = 0.0;
                    double t22 = 0.0, t23Re = 0.0, t23Im = 0.0, t33 = 0.0;
                    int counter = 0;

                    final int width = maxX - minX;
                    final int height = maxY - minY;
                    if(width <= 0 || height <= 0) {
                        pm.worked(height);
                        continue;
                    }

                    final int[] data = new int[width];

                    for(int y = minY; y < maxY; ++y) {

                        if(pm.isCanceled()) {
                            error = new Exception("Training cancelled by user");
                            return false;
                        }
                        final int pct = (int)(((y-minY)/(float)height)*100);
                        pm.setTaskName(title+geom+' '+pct+'%');

                        band.readPixels(minX, y, width, 1, data);
                        for(int x = minX; x < maxX; ++x) {
                            if(data[x-minX] != 0) {
                                if (sourceProductType == PolBandUtils.MATRIX.FULL) {
                                    getMeanCoherencyMatrixFromFullPol(x, y, sourceBands, Tr, Ti);
                                } else if (sourceProductType == PolBandUtils.MATRIX.C3) {
                                    getMeanCoherencyMatrixFromC3(x, y, sourceBands, Tr, Ti);
                                } else if (sourceProductType == PolBandUtils.MATRIX.T3) {
                                    getMeanCoherencyMatrixFromT3(x, y, sourceBands, Tr, Ti);
                                }

                                t11   += Tr[0][0];
                                t12Re += Tr[0][1];
                                t12Im += Ti[0][1];
                                t13Re += Tr[0][2];
                                t13Im += Ti[0][2];
                                t22   += Tr[1][1];
                                t23Re += Tr[1][2];
                                t23Im += Ti[1][2];
                                t33   += Tr[2][2];

                                counter++;
                            }
                        }
                        pm.worked(1);
                    }
                    
                    t11   /= counter;
                    t12Re /= counter;
                    t12Im /= counter;
                    t13Re /= counter;
                    t13Im /= counter;
                    t22   /= counter;
                    t23Re /= counter;
                    t23Im /= counter;
                    t33   /= counter;

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
                }
                return true;
            } catch(Throwable e) {
                e.printStackTrace();
                error = e;
                return false;
            } finally {
                if(out != null)
                    out.close();
                pm.done();

                removeSubGeometries(roiProduct, subGeometries);
            }
        }

        private static String[] createSubGeometries(final Product product, final String[] geometries) {
            final List<String> subGeometries = new ArrayList<String>(geometries.length);
            try {
                for(String geometry : geometries) {
                    final VectorDataNode vec = product.getVectorDataGroup().get(geometry);
                    final FeatureCollection featCollection = vec.getFeatureCollection();
                    int i=1;
                    Iterator f = featCollection.iterator();
                    while(f.hasNext()) {
                        final SimpleFeature feature = (SimpleFeature)f.next();

                        final String subGeomName = geometry+'_'+i;
                        final VectorDataNode vectorDataNode = new VectorDataNode(subGeomName, vec.getFeatureType());
                        vectorDataNode.getFeatureCollection().add(feature);
                        product.getVectorDataGroup().add(vectorDataNode);
                        subGeometries.add(subGeomName);
                        ++i;
                    }
                }
            } catch(Throwable e) {
                e.printStackTrace();
            }
            return subGeometries.toArray(new String[subGeometries.size()]);
        }

        private static void removeSubGeometries(final Product product, final String[] subGeometries) {
            try {
                for(String subGeom : subGeometries) {
                    final VectorDataNode vectorDataNode = product.getVectorDataGroup().get(subGeom);
                    product.getVectorDataGroup().remove(vectorDataNode);
                }
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void done() {
            roiProduct.setModified(false);
            try {
                final long duration = timeMonitor.stop();
                final Boolean isOk = (Boolean)this.get();
                if(isOk) {
                    final String durationStr = "Processing completed in " + ProcessTimeMonitor.formatDuration(duration);
                    VisatApp.getApp().showMessageDialog("Done",
                                                 "Supervised Training Dataset Completed\n"+
                                                  file.getAbsolutePath() +"\n\n"+
                                                  durationStr,
                                                  JOptionPane.INFORMATION_MESSAGE, null);
                } else {
                    VisatApp.getApp().showErrorDialog("An error occurred\n"+ error.getMessage());
                }
            } catch(Exception e) {
                VisatApp.getApp().showErrorDialog("An error occurred\n"+ e.getMessage());
            }
        }
    }

    private static void getMeanCoherencyMatrixFromFullPol(final int x, final int y, final Band[] sourceBands,
                                               final double[][] Tr, final double[][] Ti) throws Exception {

        final int xSt = Math.max(x - halfWindowSize, 0);
        final int xEd = Math.min(xSt + windowSize - 1, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSize, 0);
        final int yEd = Math.min(ySt + windowSize - 1, sourceImageHeight - 1);
        final int w = xEd - xSt + 1;
        final int h = yEd - ySt + 1;
        final int num = w*h;

        final double[] i_hh = new double[num];
        final double[] q_hh = new double[num];
        final double[] i_hv = new double[num];
        final double[] q_hv = new double[num];
        final double[] i_vh = new double[num];
        final double[] q_vh = new double[num];
        final double[] i_vv = new double[num];
        final double[] q_vv = new double[num];

        sourceBands[0].readPixels(xSt, ySt, w, h, i_hh);
        sourceBands[1].readPixels(xSt, ySt, w, h, q_hh);
        sourceBands[2].readPixels(xSt, ySt, w, h, i_hv);
        sourceBands[3].readPixels(xSt, ySt, w, h, q_hv);
        sourceBands[4].readPixels(xSt, ySt, w, h, i_vh);
        sourceBands[5].readPixels(xSt, ySt, w, h, q_vh);
        sourceBands[6].readPixels(xSt, ySt, w, h, i_vv);
        sourceBands[7].readPixels(xSt, ySt, w, h, q_vv);

        final Matrix TrMat = new Matrix(3,3);
        final Matrix TiMat = new Matrix(3,3);
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

        TrMat.timesEquals(1.0/num);
        TiMat.timesEquals(1.0/num);
        copyMatrix(TrMat, Tr);
        copyMatrix(TiMat, Ti);
    }

    private static void getMeanCoherencyMatrixFromC3(final int x, final int y, final Band[] sourceBands,
                                               final double[][] Tr, final double[][] Ti) throws Exception {

        final int xSt = Math.max(x - halfWindowSize, 0);
        final int xEd = Math.min(xSt + windowSize - 1, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSize, 0);
        final int yEd = Math.min(ySt + windowSize - 1, sourceImageHeight - 1);
        final int w = xEd - xSt + 1;
        final int h = yEd - ySt + 1;
        final int num = w*h;

        final double[] c11 = new double[num];
        final double[] c12r = new double[num];
        final double[] c12i = new double[num];
        final double[] c13r = new double[num];
        final double[] c13i = new double[num];
        final double[] c22 = new double[num];
        final double[] c23r = new double[num];
        final double[] c23i = new double[num];
        final double[] c33 = new double[num];

        sourceBands[0].readPixels(xSt, ySt, w, h, c11);
        sourceBands[1].readPixels(xSt, ySt, w, h, c12r);
        sourceBands[2].readPixels(xSt, ySt, w, h, c12i);
        sourceBands[3].readPixels(xSt, ySt, w, h, c13r);
        sourceBands[4].readPixels(xSt, ySt, w, h, c13i);
        sourceBands[5].readPixels(xSt, ySt, w, h, c22);
        sourceBands[6].readPixels(xSt, ySt, w, h, c23r);
        sourceBands[7].readPixels(xSt, ySt, w, h, c23i);
        sourceBands[8].readPixels(xSt, ySt, w, h, c33);

        final Matrix TrMat = new Matrix(3,3);
        final Matrix TiMat = new Matrix(3,3);
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

        TrMat.timesEquals(1.0/num);
        TiMat.timesEquals(1.0/num);
        copyMatrix(TrMat, Tr);
        copyMatrix(TiMat, Ti);
    }

    private static void getMeanCoherencyMatrixFromT3(final int x, final int y, final Band[] sourceBands,
                                               final double[][] Tr, final double[][] Ti) throws Exception {
        
        final int xSt = Math.max(x - halfWindowSize, 0);
        final int xEd = Math.min(xSt + windowSize - 1, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSize, 0);
        final int yEd = Math.min(ySt + windowSize - 1, sourceImageHeight - 1);
        final int w = xEd - xSt + 1;
        final int h = yEd - ySt + 1;
        final int num = w*h;

        final double[] t11 = new double[num];
        final double[] t12r = new double[num];
        final double[] t12i = new double[num];
        final double[] t13r = new double[num];
        final double[] t13i = new double[num];
        final double[] t22 = new double[num];
        final double[] t23r = new double[num];
        final double[] t23i = new double[num];
        final double[] t33 = new double[num];

        sourceBands[0].readPixels(xSt, ySt, w, h, t11);
        sourceBands[1].readPixels(xSt, ySt, w, h, t12r);
        sourceBands[2].readPixels(xSt, ySt, w, h, t12i);
        sourceBands[3].readPixels(xSt, ySt, w, h, t13r);
        sourceBands[4].readPixels(xSt, ySt, w, h, t13i);
        sourceBands[5].readPixels(xSt, ySt, w, h, t22);
        sourceBands[6].readPixels(xSt, ySt, w, h, t23r);
        sourceBands[7].readPixels(xSt, ySt, w, h, t23i);
        sourceBands[8].readPixels(xSt, ySt, w, h, t33);

        final Matrix TrMat = new Matrix(3,3);
        final Matrix TiMat = new Matrix(3,3);
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

        TrMat.timesEquals(1.0/num);
        TiMat.timesEquals(1.0/num);
        copyMatrix(TrMat, Tr);
        copyMatrix(TiMat, Ti);
    }

    /**
     * copy 3 x 3 matrix with loop unwinding
     * @param mat Matrix input
     * @param T double[][] output
     */
    private static void copyMatrix(final Matrix mat, final double[][] T) {
        T[0][0] = mat.get(0,0);
        T[0][1] = mat.get(0,1);
        T[0][2] = mat.get(0,2);
        T[1][0] = mat.get(1,0);
        T[1][1] = mat.get(1,1);
        T[1][2] = mat.get(1,2);
        T[2][0] = mat.get(2,0);
        T[2][1] = mat.get(2,1);
        T[2][2] = mat.get(2,2);
    }
}