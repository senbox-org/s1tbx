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
package org.esa.snap.cluster;

import static java.lang.Math.*;

/**
 * Strategy for calculating posterior cluster probabilities.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class ProbabilityCalculator {

    private final IndexFilter NO_FILTERING = new IndexFilter() {
        @Override
        public boolean accept(int index) {
            return true;
        }
    };

    private final Distribution[] distributions;
    private final double[] priors;

    public ProbabilityCalculator(final Distribution[] distributions, final double[] priors) {
        this.distributions = distributions;
        this.priors = priors;
    }

    public void calculate(final double[] point, final double[] posteriors) {
        calculate(point, posteriors, NO_FILTERING);
    }

    public void calculate(final double[] point, final double[] posteriors, final IndexFilter filter) {
        double sum = 0.0;

        for (int k = 0; k < distributions.length; ++k) {
            if (filter.accept(k)) {
                posteriors[k] = priors[k] * distributions[k].probabilityDensity(point);
                sum += posteriors[k];
            } else {
                posteriors[k] = 0.0;
            }
        }
        if (sum > 0.0) {
            // calculate posterior probabilities
            for (int k = 0; k < distributions.length; ++k) {
                if (filter.accept(k)) {
                    posteriors[k] /= sum;
                }
            }
        } else {
            // numerical underflow - compute posteriors using the logarithmic probability density
            final double[] sums = new double[distributions.length];

            for (int k = 0; k < distributions.length; ++k) {
                if (filter.accept(k)) {
                    posteriors[k] = distributions[k].logProbabilityDensity(point);
                }
            }
            for (int k = 0; k < distributions.length; ++k) {
                if (filter.accept(k)) {
                    for (int l = 0; l < distributions.length; ++l) {
                        if (filter.accept(l) && l != k) {
                            sums[k] += (priors[l] / priors[k]) * exp(posteriors[l] - posteriors[k]);
                        }
                    }
                }
            }
            // calculate posterior probabilities
            for (int k = 0; k < distributions.length; ++k) {
                if (filter.accept(k)) {
                    posteriors[k] = 1.0 / (1.0 + sums[k]);
                }
            }
        }
    }
}
