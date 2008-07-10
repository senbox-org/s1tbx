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

import java.util.Random;

/**
 * Tests for class {@link EMClusterer}.
 *
 * @author Ralf Quast
 * @version $Revision: 2229 $ $Date: 2008-06-16 15:49:49 +0200 (Mo, 16 Jun 2008) $
 */
public class EMClustererTest extends TestCase {

    public void testFindClusters() {
        final double[][] points = createRandomPoints(new double[]{
                4, 4, 4, 1, 1, 1, 1, 1,
                4, 4, 4, 1, 1, 1, 1, 1,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
        });

        final EMClusterSet clusters = EMClusterer.findClusters(points, 4, 10, 5489);
        assertEquals(4, clusters.getClusterCount());

//        assertEquals(3.0, clusters.getMean(0)[0], 0.01);
//        assertEquals(2.0, clusters.getMean(1)[0], 0.01);
//        assertEquals(1.0, clusters.getMean(2)[0], 0.01);
//        assertEquals(4.0, clusters.getMean(3)[0], 0.01);
    }

    private static double[][] createRandomPoints(double[] doubles) {
        final double[][] points = new double[doubles.length][1];

        final Random random = new Random(5489);
        for (int i = 0; i < doubles.length; ++i) {
            points[i][0] = doubles[i] + 0.01 * random.nextGaussian();
        }

        return points;
    }
}
