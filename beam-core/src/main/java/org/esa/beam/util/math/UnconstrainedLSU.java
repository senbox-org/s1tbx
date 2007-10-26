/*
 * $Id: LinEqSysSolver.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.math;

import Jama.Matrix;

/**
 * A pixel spectrum is assumed to be a linear superposition of
 * the spectra of the end-members, which therefore are the basic of this class.
 *
 * @author Helmut Schiller, GKSS
 * @since 4.1
 */
public class UnconstrainedLSU implements SpectralUnmixing {

    private final Matrix endMembers;
    private final Matrix Apinv;

    /**
     * Constructs a new <code>LinearSpectralUnmixing</code> for the given matrix.
     *
     * @param endMembers The matrix of end-members, nrow = # of spectral channels, ncol = # of endmembers.
     */
    public UnconstrainedLSU(Matrix endMembers) {
        this.endMembers = endMembers;
        this.Apinv = endMembers.inverse();
    }

    /**
     * @return The matrix of end-members, nrow = # of spectral channels, ncol = # of endmembers.
     */
    public Matrix getEndMembers() {
        return endMembers;
    }

    /**
     * Gets the abundances of given spectra by performing an unconstrained, linear unmixing.
     *
     * @param specs the spectra, nrow = # of spectral channels, ncol= # of spectra to be unmixed
     * @return the abundances, nrow = # of endmembers, ncol = # of unmixed spectra
     */
    public Matrix unmix(Matrix specs) {
        if (specs.getRowDimension() != endMembers.getRowDimension()) {
            throw new IllegalArgumentException("specs.getRowDimension() != endmembers.getRowDimension()");
        }
        return this.Apinv.times(specs);
    }

    /**
     * Gets the spectra for given abundances.
     *
     * @param abundances the abundances, nrow = # of endmembers, ncol = # of unmixed spectra
     * @return the resulting spectra, nrow = # of endmembers, ncol = # of mixed spectra
     */
    public Matrix mix(Matrix abundances) {
        if (abundances.getRowDimension() != endMembers.getColumnDimension()) {
            throw new IllegalArgumentException("specs.getRowDimension() != endmembers.getRowDimension()");
        }
        return endMembers.times(abundances);
    }
}
