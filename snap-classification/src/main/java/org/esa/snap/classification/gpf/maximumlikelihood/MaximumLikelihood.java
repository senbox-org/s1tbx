/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf.maximumlikelihood;

import Jama.Matrix;
import net.sf.javaml.classification.AbstractClassifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;
import org.esa.snap.core.util.SystemUtils;

import java.util.HashMap;
import java.util.Map;

public class MaximumLikelihood extends AbstractClassifier {

    private Map<Object, double[]> meanVector;
    private Map<Object, Jama.Matrix> invCov;
    private Map<Object, Double> determinant;

    private double constantTerm;

    private final boolean biasCorrected = true; // TODO

    public MaximumLikelihood() {

    }

    public void buildClassifier(Dataset data) {

        meanVector = new HashMap<>();
        invCov = new HashMap<>();
        determinant = new HashMap<>();

        // 1/[(2 PI)^(N/2)]
        constantTerm = 1.0 / (Math.pow(2.0 * Math.PI, (double) data.noAttributes() / 2.0));
        //SystemUtils.LOG.info("MaximumLikelihood: constantTerm = " + constantTerm);

        Map<Object, StorelessCovariance> covarianceMap = new HashMap<>();
        Map<Object, Integer> cntMap = new HashMap<>();
        for (Object o : data.classes()) {
            covarianceMap.put(o, new StorelessCovariance(data.noAttributes(), biasCorrected));
            meanVector.put(o, new double[data.noAttributes()]);
            cntMap.put(o, 0);
        }

        final double[] features = new double[data.noAttributes()];
        Object classVal;
        double featureVal;
        for (Instance i : data) {
            classVal = i.classValue();
            for (int j = 0; j < features.length; j++) {
                featureVal = i.value(j);
                features[j] = featureVal;
                meanVector.get(classVal)[j] += featureVal;
            }
            covarianceMap.get(classVal).increment(features);
            cntMap.replace(classVal, cntMap.get(classVal) + 1);
        }

        for (Object o : covarianceMap.keySet()) {
            for (int i = 0; i < data.noAttributes(); i++) {
                meanVector.get(o)[i] /= cntMap.get(o);
            }
            try {
                final RealMatrix m1 = covarianceMap.get(o).getCovarianceMatrix();
                final Jama.Matrix m2 = new Jama.Matrix(m1.getData());
                invCov.put(o, m2.inverse());
                determinant.put(o, Math.abs(m2.det()));
            } catch (Exception e) {
                SystemUtils.LOG.info("MaximumLikelihood.buildClassifier: cannot classify " + o + ' ' + e.getMessage());
            }
        }
    }

    @Override
    public Map<Object, Double> classDistribution(Instance instance) {

        // i = class, N = number of features
        // Natural log of likelihood is expressed as...
        // g(i) = - 0.5*[ transpose(x - m_i) * invCov_i * (x - m_i)] - (N/2)*ln(2*PI) - (0.5 * ln(abs(det_i)))
        // So likelihood is
        // exp(g(i)) = exp{ - 0.5*[ transpose(x - m_i) * invCov_i * (x - m_i)] } * { 1/[(2 PI)^(N/2)] } * { 1/sqrt(det_i) }

        final Map<Object, Double> dis = new HashMap<>();
        double sum = 0.0;
        for (Object o : meanVector.keySet()) {

            if (!invCov.containsKey(o) || !determinant.containsKey(o)) {
                continue;
            }

            double[] tmp = new double[instance.noAttributes()];
            for (int i = 0; i < instance.noAttributes(); i++) {
                tmp[i] = instance.value(i) - meanVector.get(o)[i];
            }

            // x - m_i
            Matrix diffMat = new Matrix(tmp, tmp.length);

            //SystemUtils.LOG.info("diffMat rol x col = " + diffMat.getRowDimension() + " x " + diffMat.getColumnDimension());
            //SystemUtils.LOG.info("invCov rol x col = " + invCov.get(o).getRowDimension() + " x " + invCov.get(o).getColumnDimension());

            // transpose(x - m_i) * invCov
            Matrix tmpMat = diffMat.transpose().times(invCov.get(o));

            //SystemUtils.LOG.info("transpose(x - m_i) * invCov rol  x col = " + tmpMat.getRowDimension() + " x " + tmpMat.getColumnDimension());

            // transpose(x - m_i) * invCov] * (x - m_i)
            Matrix m1 = tmpMat.times(diffMat);

            if (m1.getColumnDimension() != 1 || m1.getRowDimension() != 1) {
                SystemUtils.LOG.info("ERROR: #col = " + m1.getColumnDimension() + " #row = " + m1.getRowDimension());
            }

            // This is the likelihood
            double expg = Math.exp(-0.5 * m1.get(0, 0)) * constantTerm * (1.0 / Math.sqrt(Math.abs(determinant.get(o))));
            sum += expg;
            dis.put(o, expg);
        }

        // Normalize...
        for (Object o : dis.keySet()) {
            final double val = dis.get(o);
            dis.replace(o, val / sum);
            //SystemUtils.LOG.info("DEBUG: o = " + o + ": " + dis.get(o));
        }

        return dis;
    }

}
