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

import org.esa.beam.cluster.KMeansCluster;

import static java.lang.Math.exp;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class KMeansClusterSet {

    private final KMeansCluster[] clusters;

    public KMeansClusterSet(KMeansCluster[] clusters) {
        this.clusters = clusters;
    }

    public int getMembership(double[] point) {
        double minDistance = Double.MAX_VALUE;
        int closestCluster = 0;
        for (int k = 0; k < getClusterCount(); ++k) {
            final double distance = squaredDistance(getMean(k), point);
            if (distance < minDistance) {
                closestCluster = k;
                minDistance = distance;
            }
        }
        return closestCluster;
    }

     private double squaredDistance(double[] x, double[] y) {
        double d = 0.0;
        for (int l = 0; l < x.length; ++l) {
            d += (y[l] - x[l]) * (y[l] - x[l]);
        }
        return d;
    }

    public final int getClusterCount() {
        return clusters.length;
    }

    public double[] getMean(int i) {
        return clusters[i].getMean();
    }
}
