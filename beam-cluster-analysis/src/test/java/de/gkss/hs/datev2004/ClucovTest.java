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

package de.gkss.hs.datev2004;

import junit.framework.TestCase;

import java.util.Random;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class ClucovTest extends TestCase {

    public void testClucov() throws IOException, URISyntaxException {

        DataSet ds = new DataSet(ClucovTest.class.getResource("iris.mdda").toURI().getPath());
        Random rgen = new Random(12345);

        Clucov clucov = new Clucov(ds, rgen);
        clucov.max_iteration = 20;
        clucov.n_planes = 100;
        clucov.mahadistsqCut = 16;
        clucov.t_comb = 0.45;
        clucov.t_div = 0.2;
        clucov.cont_min = 15;
        clucov.max_clusters = 20;
        clucov.few_changes = 0;
        clucov.initialize(6);
        clucov.run();

        assertEquals(3, clucov.clusters.size());

        testCluster(clucov,
                    (short) 1,
                    "(3,1)l",
                    54,
                    new double[]{6.5518519, 2.9500000, 5.4981481, 1.9888888},
                    new double[][]{{0.385089, 0.092407, 0.2988, 0.058354},
                            {0.092407, 0.111389, 0.083426, 0.056296},
                            {0.2988, 0.083426, 0.316108, 0.064239},
                            {0.058354, 0.056296, 0.064239, 0.084691}});

        testCluster(clucov,
                    (short) 2,
                    "(5,4)",
                    50,
                    new double[]{5.006, 3.425999, 1.457999, 0.246000},
                    new double[][]{{0.121764, 0.097244, 0.015652, 0.010124},
                            {0.097244, 0.141124, 0.010292, 0.009204},
                            {0.015652, 0.010292, 0.030036, 0.005332},
                            {0.010124, 0.009204, 0.005332, 0.010884}});

        testCluster(clucov,
                    (short) 3,
                    "(3,1)r",
                    46,
                    new double[]{5.9217391, 2.7804348, 4.1934782, 1.3021739},
                    new double[][]{{0.279093, 0.09673, 0.186881, 0.056692},
                            {0.09673, 0.092009, 0.089003, 0.043303},
                            {0.186881, 0.089003, 0.194088, 0.060666},
                            {0.056692, 0.043303, 0.060666, 0.033256}});


    }

    private void testCluster(Clucov clucov, short k, String history, int npoints, double[]  avg, double[][] cov) {
        assertNotNull(clucov.clusters);
        Clucov.Cluster cluster = clucov.clusters.get(k);
        assertNotNull(cluster);
        assertEquals(k, cluster.group);
        assertEquals(history, cluster.history);
        assertEquals(npoints, cluster.fm.npoints);
        assertEquals(4, cluster.fm.dim);
        assertEquals(4, cluster.fm.avg.length);
        for (int i = 0; i < cluster.fm.avg.length; i++) {
            assertEquals(avg[i], cluster.fm.avg[i], 1.0e-5);
        }
        assertEquals(4, cluster.fm.cov.length);
        for (int i = 0; i < cluster.fm.cov.length; i++) {
            assertEquals(4, cluster.fm.cov[i].length);
            for (int j = 0; j < cluster.fm.cov[i].length; j++) {
                assertEquals(cov[i][j], cluster.fm.cov[i][j], 1.0e-5);
            }
        }
    }

}