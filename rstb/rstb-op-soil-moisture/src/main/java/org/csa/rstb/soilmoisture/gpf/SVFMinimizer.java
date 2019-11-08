/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

/**
 * Minimizes a single variable function for a set of fixed parameter values using Brent optimizer.
 */
public class SVFMinimizer {

    private final static double ABSOLUTE_ACCURACY = 1.0e-11;
    private final static double RELATIVE_ACCURACY = 1.0e-9;

    private final static MaxEval maxEval = new MaxEval(1000);
    private final static MaxIter maxIter = new MaxIter(100);

    private double invalidValue = -999.0;
    private SearchInterval searchInterval;
    private SingleVarFunc svFunc;

    SVFMinimizer(final double invalidVal, final double lowerVal, final double upperVal, final SingleVarFunc func) {

        invalidValue = invalidVal;
        searchInterval = new SearchInterval(lowerVal, upperVal);
        svFunc = func;
    }

    double minimize(final double[] fixed) {

        final SVF svf = new SVF();
        svf.setSingleVarFunc(svFunc);
        svf.setFixedValues(fixed);

        final UnivariateObjectiveFunction uof = new UnivariateObjectiveFunction(svf);

        final BrentOptimizer brentOptimizer = new BrentOptimizer(RELATIVE_ACCURACY, ABSOLUTE_ACCURACY);

        double minVal = invalidValue;

        try {

            UnivariatePointValuePair ptValPair = brentOptimizer.optimize(maxEval, maxIter, GoalType.MINIMIZE, searchInterval, uof);
            minVal = ptValPair.getPoint();

        } catch (TooManyIterationsException e) {

            //System.out.println("SVFMinimizer: BrentOptimizer exceeded max iterations - " + e.getMessage());
            minVal = invalidValue;

        } catch (TooManyEvaluationsException e) {

            //System.out.println("SVFMinimizer: BrentOptimizer function evaluation exception - " + e.getMessage());
            minVal = invalidValue;

        } catch (Throwable e) {

            //System.out.println("SVFMinimizer: Unknown exception from BrentOptimizer - " + e.getMessage());
            minVal = invalidValue;
        }

        return minVal;
    }

    private static class SVF implements UnivariateFunction {

        private double[] fixedValues;
        private SingleVarFunc singleVarFunc;

        void setFixedValues(double[] fixedVal) {
            fixedValues = fixedVal;
        }

        void setSingleVarFunc(SingleVarFunc func) {
            singleVarFunc = func;
        }

        public double value(double x) {
            return singleVarFunc.compute(x, fixedValues);
        }
    }
}



