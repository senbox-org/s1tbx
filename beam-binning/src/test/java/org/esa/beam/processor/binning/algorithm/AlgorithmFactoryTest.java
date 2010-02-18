/*
 * $Id: AlgorithmFactoryTest.java,v 1.1 2006/09/11 10:47:33 norman Exp $
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
package org.esa.beam.processor.binning.algorithm;

import junit.framework.TestCase;

public class AlgorithmFactoryTest extends TestCase {

    private AlgorithmFactory algorithmFactory;

    @Override
    public void setUp() {
        algorithmFactory = new AlgorithmFactory();
    }

    public void testGetAlgorithmWithNull() {
        // must throw IllegalArgumentException when fed with null
        try {
            algorithmFactory.getAlgorithm(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetAlgorithmWithUnkownAlgoName() {
        // must throw IllegalArgumentException when fed with unkown algo
        try {
            algorithmFactory.getAlgorithm("doesNotExist");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetAlgorithm() {
        Algorithm algo;

        algo = algorithmFactory.getAlgorithm("Maximum Likelihood");
        assertNotNull(algo);
        assertEquals(MLEAlgorithm.class, algo.getClass());

        algo = algorithmFactory.getAlgorithm("Arithmetic Mean");
        assertNotNull(algo);
        assertEquals(AMEAlgorithm.class, algo.getClass());

        algo = algorithmFactory.getAlgorithm("Minimum/Maximum");
        assertNotNull(algo);
        assertEquals(MINMAXAlgorithm.class, algo.getClass());
    }
}
