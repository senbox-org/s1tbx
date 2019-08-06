package org.csa.rstb.soilmoisture.gpf;

/**
 * Interface for single variable function with fixed parameters.
 */
public interface SingleVarFunc {

    double compute(double var, double[] fixed);
}
