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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Tests for class {@link EMClusterer}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class EMClustererTest extends TestCase {
    private static final double A = 1.0;
    private static final double B = 2.0;

    public void testFindClusters() {
        final double[][] points = createRandomPoints(new double[]{
                A, B, A, B, A, B, A, B,
                B, A, B, A, B, A, B, A,
                A, B, A, B, A, B, A, B,
                B, A, B, A, B, A, B, A,
                A, B, A, B, A, B, A, B,
                B, A, B, A, B, A, B, A,
                A, B, A, B, A, B, A, B,
                B, A, B, A, B, A, B, A,
        });

        final EMCluster[] clusters = EMClusterer.findClusters(points, 2, 100, 5489);
        assertEquals(2, clusters.length);

        Arrays.sort(clusters, new Comparator<EMCluster>() {
            @Override
            public int compare(EMCluster o1, EMCluster o2) {
                return Double.compare(o1.getMean(0), o2.getMean(0));
            }
        });

        assertEquals(A, clusters[0].getMean(0), 0.1);
        assertEquals(B, clusters[1].getMean(0), 0.1);
    }

    private static double[][] createRandomPoints(double[] doubles) {
        final double[][] points = new double[doubles.length][1];

        final Random random = new Random(5489);
        for (int i = 0; i < doubles.length; ++i) {
            points[i][0] = doubles[i] + 0.1 * random.nextGaussian();
        }

        return points;
    }
}
