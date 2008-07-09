/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class KMeansClusterSet {

    private final double[][] means;

    public KMeansClusterSet(KMeansCluster[] clusters) {
        this.means = new double[clusters.length][0];
        for (int c = 0; c < clusters.length; c++) {
            means[c] = clusters[c].getMean().clone();
        }
    }

    public int getMembership(double[] point) {
        return KMeansClusterer.getClosestCluster(means, point);
    }
    
    public double[][] getMeans() {
        return means;
    }
}
