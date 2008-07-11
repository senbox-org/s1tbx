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
 * Cluster class.
 *
 * @author Ralf Quast
 * @version $Revision $ $Date $
 */
public class EMCluster {

    final Distribution distribution;
    final double priorProbability;

    public EMCluster(Distribution distribution, double priorProbability) {
        this.priorProbability = priorProbability;
        this.distribution = distribution;
    }

    public final double getPriorProbability() {
        return priorProbability;
    }

    public final double getLogProbabilityDensity(double[] point) {
        return distribution.logProbabilityDensity(point);
    }

    public final double getProbabilityDensity(double[] point) {
        return distribution.probabilityDensity(point);
    }

    public final double[] getMean() {
        return distribution.getMean();
    }
    
    public Distribution getDistribution() {
        return distribution;
    }
}
