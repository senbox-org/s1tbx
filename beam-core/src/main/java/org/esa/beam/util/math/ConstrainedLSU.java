package org.esa.beam.util.math;

import Jama.Matrix;

/**
 * A pixel spectrum is assumed to be a linear superposition of
 * the spectra of the end-members, which therefore are the basis of this class.
 * The constraint is sum(abundances)=1 for each pixel.
 *
 * @author Helmut Schiller, GKSS
 * @since 4.1
 */
public class ConstrainedLSU extends UnconstrainedLSU {

    private final Matrix ATAinv;
    private final Matrix Z;

    /**
     * Constructs a new <code>LinearSpectralUnmixing</code> for the given matrix.
     *
     * @param endMembers the matrix of end-members, nrow = # of spectral channels, ncol = # of endmembers
     */
    public ConstrainedLSU(Matrix endMembers) {
        super(endMembers);
        this.ATAinv = (((endMembers.transpose()).times(endMembers))).inverse();
        this.Z = new Matrix(1, endMembers.getColumnDimension(), 1.);
    }

    /**
     * Gets the abundances of given spectra by performing a constrained, linear unmixing.
     * The constraint is sum(abundances)=1 for each pixel.
     *
     * @param specs nrow = # of spectral channels, ncol= # of spectra to be unmixed
     * @return the abundances, nrow = # of endmembers, ncol = # of unmixed spectra
     */
    @Override
    public Matrix unmix(Matrix specs) {
        if (specs.getRowDimension() != getEndMembers().getRowDimension()) {
            throw new IllegalArgumentException("specs.getRowDimension() != endmembers.getRowDimension()");
        }
        Matrix res, hr;
        Matrix au = super.unmix(specs);
        hr = this.ATAinv.times(Z.transpose()).times(
                Z.times(ATAinv).times(Z.transpose()).inverse()).times(
                Z.times(au).minus(
                        (new Matrix(1, specs.getColumnDimension(), 1.))));
        res = au.minus(hr);
        return res;
    }
}
