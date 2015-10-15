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

package org.esa.snap.core.util.math;

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
