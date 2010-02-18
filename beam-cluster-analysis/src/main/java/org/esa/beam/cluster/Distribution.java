/* Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.cluster;

/**
 * Distribution interface.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface Distribution {
    /**
     * Returns the probability density for a given vector.
     *
     * @param y the vector.
     *
     * @return the probability density for the vector y.
     */
    double probabilityDensity(double[] y);

    /**
     * Returns the logarithm of the probability density for a given vector.
     *
     * @param y the vector.
     *
     * @return the logarithm of the probability density for the vector y.
     */
    double logProbabilityDensity(double[] y);
}
