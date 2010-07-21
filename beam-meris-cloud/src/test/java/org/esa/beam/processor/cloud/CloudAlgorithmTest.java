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
package org.esa.beam.processor.cloud;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision$ $Date$
 */
public class CloudAlgorithmTest extends TestCase {

    private CloudAlgorithm testAlgorithm;

    /*
      * @see junit.framework.TestCase#setUp()
      */
    @Override
    protected void setUp() throws Exception {
        URL auxdataDirUrl = getClass().getResource("/auxdata/nn_config_test.txt");
        File auxdataDir = new File(auxdataDirUrl.toURI()).getParentFile();
        testAlgorithm = new CloudAlgorithm(auxdataDir, "nn_config_test.txt");
    }

    /*
      * Test method for 'org.esa.beam.processor.cloud.CloudAlgorithm.computeCloud(double[])'
      */
    public void testComputeCloud() {
        final double[] in = new double[]{0.0778002, 0.0695650, 0.0591455, 0.0545394,
                0.0460968, 0.0415193, 0.0420742, 0.0421471,
                0.0421236, 0.293535, 1012.98, 762.190,
                0.622985, 0.996135, -0.0447822};

        double out = testAlgorithm.computeCloud(in);
        assertEquals("cloud NN result", 0.004993, out, 0.00001);
    }

    /*
      * Test method for 'org.esa.beam.processor.cloud.CloudAlgorithm.nn2Probability(double)'
      */
    public void testNn2Probability() {
        double probability = testAlgorithm.nn2Probability(0.004993);
        assertEquals("probability", 0.01313, probability, 0.00001);
    }

}
