package org.esa.snap.classification.gpf.minimumdistance;

import net.sf.javaml.classification.AbstractMeanClassifier;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.EuclideanDistance;

/**
 * Minimum Distance
 */
public class MinDistClassifier extends AbstractMeanClassifier {
        private static final long serialVersionUID = 3044426429892220857L;
        private EuclideanDistance dist;

        public MinDistClassifier(EuclideanDistance dist) {
            this.dist = dist;
        }

        public Object classify(Instance instance) {
            double min = Double.MAX_VALUE;
            Object pred = null;

            for (Object o : this.mean.keySet()) {
                double d = this.dist.calculateDistance((Instance) this.mean.get(o), instance);
                if (d < min) {
                    min = d;
                    pred = o;
                }
            }

            return pred;
        }
}
