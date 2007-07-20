package org.esa.beam.util.math;

import Jama.Matrix;

public interface SpectralUnmixing {

    /**
     * Gets the abundances of given spectra by performing an unconstrained, linear unmixing.
     *
     * @param specs the spectra, nrow = # of spectral channels, ncol= # of spectra to be unmixed
     * @return the abundances, nrow = # of endmembers, ncol = # of unmixed spectra
     */
    Matrix unmix(Matrix specs);

    /**
     * Gets the spectra for given abundances.
     *
     * @param abundances the abundances, nrow = # of endmembers, ncol = # of unmixed spectra
     * @return the resulting spectra, nrow = # of endmembers, ncol = # of mixed spectra
     */
    Matrix mix(Matrix abundances);
}
