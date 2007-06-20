/*
 * $Id: SstCoefficients.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
package org.esa.beam.processor.sst;

import org.esa.beam.util.Guardian;

/**
 * This class is a container for an arbitrary sized list of sst retrieval coefficients.
 */
public class SstCoefficients {

    private int _start;
    private int _end;
    private float[] _a_Coeffs;
    private float[] _b_Coeffs;
    private float[] _c_Coeffs;
    private float[] _d_Coeffs;

    /**
     * Constructs the object with default values
     */
    public SstCoefficients() {
        this(0, 0);
    }

    /**
     * Constructs the object with start end end values given
     */
    public SstCoefficients(int start, int end) {
        setRange(start, end);
    }

    /**
     * Sets a new coefficient range. Checks for end < start
     */
    public void setRange(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end < start");
        }
        _start = start;
        _end = end;
    }

    /**
     * Retrieves the start point of the range (included)
     */
    public int getStart() {
        return _start;
    }

    /**
     * Retrieves the end point of the range (included)
     */
    public int getEnd() {
        return _end;
    }

    /**
     * Sets the a coefficient set for the calculation of the nadir view sst on daytime
     */
    public void set_A_Coeffs(float[] coeffs) {
        Guardian.assertNotNull("coeffs", coeffs);
        _a_Coeffs = coeffs;
    }

    /**
     * Returns the current a parameter set for the calculation of the nadir view sst on daytime - or null if none is
     * set
     */
    public float[] get_A_Coeffs() {
        return _a_Coeffs;
    }

    /**
     * Sets the b coefficient set for the calculation of the nadir view sst on nighttime
     */
    public void set_B_Coeffs(float[] coeffs) {
        Guardian.assertNotNull("coeffs", coeffs);
        _b_Coeffs = coeffs;
    }

    /**
     * Returns the current b parameter set for the calculation of the nadir view sst on nighttime - or null if none is
     * set
     */
    public float[] get_B_Coeffs() {
        return _b_Coeffs;
    }

    /**
     * Sets the c coefficient set for the calculation of the dual view sst on daytime
     */
    public void set_C_Coeffs(float[] coeffs) {
        Guardian.assertNotNull("coeffs", coeffs);
        _c_Coeffs = coeffs;
    }

    /**
     * Returns the current c parameter set for the calculation of the dual view sst on daytime - or null if none is set
     */
    public float[] get_C_Coeffs() {
        return _c_Coeffs;
    }

    /**
     * Sets the d coefficient set for the calculation of the dual view sst on nighttime
     */
    public void set_D_Coeffs(float[] coeffs) {
        Guardian.assertNotNull("coeffs", coeffs);
        _d_Coeffs = coeffs;
    }

    /**
     * Returns the current d parameter set for the calculation of the dual view sst on nighttime - or null if none is
     * set
     */
    public float[] get_D_Coeffs() {
        return _d_Coeffs;
    }
}
