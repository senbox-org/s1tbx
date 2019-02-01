package org.csa.rstb.polarimetric.gpf.specklefilters.covariance;

import org.esa.snap.core.datamodel.ProductData;

public interface Covariance {

    void getCovarianceMatrix(final int index, final ProductData[] dataBuffers);

    Covariance clone();

    void rescaleMatrix(final double gamma);

    void addCovarianceMatrix(final Covariance matrix);

    void addWeightedCovarianceMatrix(final double w, final Covariance matrix);

    double[][] getRealCovarianceMatrix();

    double[][] getImagCovarianceMatrix();

    double[] getDiagonalElements();

    double getDeterminant();
}
