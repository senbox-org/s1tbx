package org.esa.beam.util.math;

/**
 * Linear spectral unmixing interface.
 *
 * @author Norman Fomferra.
 * @version $Revision$ $Date$
 */
public interface SpectralUnmixing {

    /**
     * Performs a linear spectral unmixing of a set of spectra.
     *
     * @param spectra the spectra, where
     *                number of rows = number of spectral channels
     *                number of cols = number of spectra
     *
     * @return the abundances, where
     *         number of rows = number of endmembers
     *         number of cols = number of spectra
     */
    double[][] unmix(double[][] spectra);

    /**
     * Calculates the spectra for a set of abundances.
     *
     * @param abundances the abundances, where
     *                   number of rows = number of endmembers
     *                   number of cols = number of spectra
     *
     * @return the resulting spectra, where
     *         number of rows = number of spectral channels
     *         number of cols = number of spectra
     */
    double[][] mix(double[][] abundances);
}
