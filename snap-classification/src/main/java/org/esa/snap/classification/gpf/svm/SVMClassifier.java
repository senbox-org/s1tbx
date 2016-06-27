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
package org.esa.snap.classification.gpf.svm;

import org.esa.snap.classification.gpf.BaseClassifier;
import org.esa.snap.classification.gpf.ClassifierDescriptor;
import org.esa.snap.classification.gpf.SupervisedClassifier;
import libsvm.LibSVM;
import libsvm.svm;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.engine_utilities.gpf.ThreadManager;

import java.util.Iterator;
import java.util.SortedSet;

/**
 * Support Vector Machine
 */
public class SVMClassifier extends BaseClassifier implements SupervisedClassifier {

    private static final double[] c = {0.03125, 0.125, 0.5, 2.0, 8.0, 32.0, 128.0, 512.0, 2048.0, 8192.0, 32768.0};
    private static final double[] gamma = {0.000030517578125, 0.0001220703125, 0.00048828125, 0.001953125, 0.0078125,
            0.03125, 0.125, 0.5, 2.0, 8.0};

    public SVMClassifier(final ClassifierParams params) {
        super(params);
    }

    public Classifier createMLClassifier(final FeatureInfo[] featureInfos) {
        LibSVM libSVM = new LibSVM();

        svm_parameter param = libSVM.getParameters();
        param.svm_type = svm_parameter.NU_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 300;

        return libSVM;
    }

    public Classifier retrieveMLClassifier(final ClassifierDescriptor classifierDescriptor) {
        return (LibSVM) classifierDescriptor.getObject();
    }

    protected void buildClassifier(final Classifier classifier, final Dataset trainDataset) {
        final LibSVM libSVM = (LibSVM) classifier;

        try {
            //findOptimalModelParameters(libSVM, trainDataset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        libSVM.buildClassifier(trainDataset);
    }

    private static class NewValues {
        public double accuracyMax = 0.0;
        public int cIdx = 0, gammaIdx = 0;
    }

    /**
     * Find optimal RBF model parameters (C, gamma) using grid search.
     */
    private static void findOptimalModelParameters(final LibSVM libSVM, final Dataset trainDataset) throws Exception {

        final StatusProgressMonitor pm = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);

        final svm_parameter param = libSVM.getParameters();
        final svm_problem problem = transformDataset(trainDataset);
        final ThreadManager threadManager = new ThreadManager();
        final NewValues newValues = new NewValues();

        pm.beginTask("Determining optimal parameters...", c.length * gamma.length);
        try {
            for (int i = 0; i < c.length; i++) {
                if (pm.isCanceled()) {
                    return;
                }

                for (int j = 0; j < gamma.length; j++) {
                    final svm_parameter newParam = (svm_parameter)param.clone();
                    newParam.C = c[i];
                    newParam.gamma = gamma[j];
                    final int ii = i;
                    final int jj = j;

                    final Thread worker = new Thread() {

                        @Override
                        public void run() {
                            final double accuracy = performCrossValidation(newParam, problem);
                            synchronized (this) {
                                if (accuracy > newValues.accuracyMax) {
                                    newValues.accuracyMax = accuracy;
                                    newValues.cIdx = ii;
                                    newValues.gammaIdx = jj;
                                }
                            }
                        }

                    };

                    threadManager.add(worker);
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }

        threadManager.finish();

        param.C = c[newValues.cIdx];
        param.gamma = gamma[newValues.gammaIdx];
    }

    private static double performCrossValidation(final svm_parameter param, final svm_problem problem) {

            final double[] target = new double[problem.l];
        svm.svm_cross_validation(problem, param, 3, target);

        int countErr = 0, i = 0;
        for(double y : problem.y) {
            if (y != target[i]) {
                countErr++;
            }
            ++i;
        }
        return (1.0 - (double) countErr / (double) problem.l) * 100.0;
    }

    private static svm_problem transformDataset(Dataset data) {
        svm_problem p = new svm_problem();
        p.l = data.size();
        p.y = new double[data.size()];
        p.x = new svm_node[data.size()][];
        int tmpIndex = 0;

        for (int j = 0; j < data.size(); ++j) {
            Instance tmp = data.instance(j);
            p.y[tmpIndex] = (double) data.classIndex(tmp.classValue());
            p.x[tmpIndex] = new svm_node[tmp.keySet().size()];
            int i = 0;
            SortedSet<Integer> tmpSet = tmp.keySet();

            for (Iterator<Integer> i$ = tmpSet.iterator(); i$.hasNext(); ++i) {
                int index = (Integer) i$.next();
                p.x[tmpIndex][i] = new svm_node();
                p.x[tmpIndex][i].index = index;
                p.x[tmpIndex][i].value = tmp.value(index);
            }

            ++tmpIndex;
        }

        return p;
    }
}
