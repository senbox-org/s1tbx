/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.supervised.svm;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.csa.rstb.gpf.PolOpUtils;
import org.csa.rstb.gpf.decompositions.hAAlpha;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.base.AbstractPolarimetricClassifier;
import org.esa.nest.clustering.fuzzykmeans.Roi;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author emmab
 */
public class SVMClassifier extends AbstractPolarimetricClassifier {

    private static final Logger log = LoggerFactory.getLogger(SVMClassifier.class);

    private static final String H_ALPHA_CLASS = "H_alpha_class";
    private final boolean useLeeHAlphaPlaneDefinition;
    private svm_parameter parameters;		// set by parse_command_line
    private svm_problem problem;		// set by read_problem
    private svm_model model;
    private String input_file_name;		// set by parse_command_line
    private String model_file_name;		// set by parse_command_line
    private String error_msg;
    private int cross_validation;
    private int nr_fold;
    private transient Roi roi;

    public SVMClassifier(final PolBandUtils.MATRIX sourceProductType,
            final int sourceWidth, final int sourceHeight, final int winSize,
            final Map<Band, PolBandUtils.QuadSourceBand> srcbandMap) {
        super(sourceProductType, sourceWidth, sourceHeight, winSize, srcbandMap);

        useLeeHAlphaPlaneDefinition = Boolean.getBoolean(SystemUtils.getApplicationContextId()
                + ".useLeeHAlphaPlaneDefinition");
    }

    @Override
    public int getNumClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTargetBandName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, Operator op) {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(targetTile);
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final PolBandUtils.QuadSourceBand srcBandList = bandMap.get(targetBand);

        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
        final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
        final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];
        for (int i = 0; i < srcBandList.srcBands.length; ++i) {
            sourceTiles[i] = op.getSourceTile(srcBandList.srcBands[i], sourceRectangle);
            dataBuffers[i] = sourceTiles[i].getDataBuffer();
        }
        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];
        final int noDataValue = 0;

        double[] point = new double[sourceTiles.length];
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, srcWidth, srcHeight,
                        sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                final hAAlpha.HAAlpha data = hAAlpha.computeHAAlpha(Tr, Ti);

                try {
                    if (roi.contains(x, y)) {
                        for (int i = 0; i < sourceTiles.length; i++) {
                            point[i] = sourceTiles[i].getSampleDouble(x, y);
                        }
                        if (!Double.isNaN(data.entropy) && !Double.isNaN(data.anisotropy) && !Double.isNaN(data.alpha)) {
//                            targetData.setElemIntAt(trgIndex.getIndex(x),
//                                    getZoneIndex(data.entropy, data.alpha, useLeeHAlphaPlaneDefinition));

                            targetTile.setSample(trgIndex.getIndex(x), y, getPrediction(data.entropy, data.alpha, useLeeHAlphaPlaneDefinition, new DenseVector(point)));
                        }
                    } else {
                        targetTile.setSample(trgIndex.getIndex(x), y, noDataValue);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean getPrediction(final double entropy, final double alpha, final boolean useLeeHAlphaPlaneDefinition, Vector point) {
        return true;
    }

    private void readProblem(List<Vector> points, List labels) throws IOException {
        List<Double> vy = new ArrayList<>();
        List<svm_node[]> vx = new ArrayList<>();
        int max_index = 0;

        for (Vector point : points) {
            for (int i = 0; i < point.size(); i++) {
                vy.add(point.get(i));

                int m = point.size() / 2;
                svm_node[] x = new svm_node[m];
                for (int j = 0; j < m; j++) {
                    x[j] = new svm_node();
                    x[j].index = i;
                    x[j].value = point.get(i);
                }
                if (m > 0) {
                    max_index = Math.max(max_index, x[m - 1].index);
                }
                vx.add(x);
            }

        }

        problem = new svm_problem();
        problem.l = vy.size();
        problem.x = new svm_node[problem.l][];
        for (int i = 0; i < problem.l; i++) {
            problem.x[i] = vx.get(i);
        }
        problem.y = new double[problem.l];
        for (int i = 0; i < problem.l; i++) {
            problem.y[i] = vy.get(i);
        }

        if (parameters.gamma == 0 && max_index > 0) {
            parameters.gamma = 1.0 / max_index;
        }

        if (parameters.kernel_type == svm_parameter.PRECOMPUTED) {
            for (int i = 0; i < problem.l; i++) {
                if (problem.x[i][0].index != 0) {
                    log.info("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int) problem.x[i][0].value <= 0 || (int) problem.x[i][0].value > max_index) {
                    log.info("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }
        }

    }

    @Override
    public Vector classify(Vector instance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void classify_one(Vector instance, svm_model model, int predict_probability) {
        int correct = 0;
        int total = 0;
        double error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        int svm_type = svm.svm_get_svm_type(model);
        int nr_class = svm.svm_get_nr_class(model);
        double[] prob_estimates = null;
        log.info("Scaled Inter-Cluster Density = {}", sumvy);

    }

    @Override
    public double classifyScalar(Vector instance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void crossValidate() {
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[problem.l];

        svm.svm_cross_validation(problem, parameters, nr_fold, target);
        if (parameters.svm_type == svm_parameter.EPSILON_SVR
                || parameters.svm_type == svm_parameter.NU_SVR) {
            for (i = 0; i < problem.l; i++) {
                double y = problem.y[i];
                double v = target[i];
                total_error += (v - y) * (v - y);
                sumv += v;
                sumy += y;
                sumvv += v * v;
                sumyy += y * y;
                sumvy += v * y;
            }
            System.out.print("Cross Validation Mean squared error = " + total_error / problem.l + "\n");
            System.out.print("Cross Validation Squared correlation coefficient = "
                    + ((problem.l * sumvy - sumv * sumy) * (problem.l * sumvy - sumv * sumy))
                    / ((problem.l * sumvv - sumv * sumv) * (problem.l * sumyy - sumy * sumy)) + "\n"
            );
        } else {
            for (i = 0; i < problem.l; i++) {
                if (target[i] == problem.y[i]) {
                    ++total_correct;
                }
            }
            System.out.print("Cross Validation Accuracy = " + 100.0 * total_correct / problem.l + "%\n");
        }
    }
}
