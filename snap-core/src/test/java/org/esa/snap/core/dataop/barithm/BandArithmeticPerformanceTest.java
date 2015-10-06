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
package org.esa.snap.core.dataop.barithm;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.DefaultNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;

import java.io.IOException;

public class BandArithmeticPerformanceTest extends TestCase {

    private static final int MAX_NUM_TEST_LOOPS____ = 10000000;
    private static final int MIN_NUM_OPS_PER_SECOND = 10000000;

    public BandArithmeticPerformanceTest(String s) {
        super(s);
    }

    public void testThatPerformanceIsSufficient() throws ParseException, IOException {
        final Band flags = new Band("flags", ProductData.TYPE_INT8, 1, 1);
        final Band reflec_4 = new Band("reflec_4", ProductData.TYPE_FLOAT32, 1, 1);
        final Band reflec_5 = new Band("reflec_5", ProductData.TYPE_FLOAT32, 1, 1);
        final SingleFlagSymbol s1 = new SingleFlagSymbol("flags.WATER", flags, 0x01);
        final SingleFlagSymbol s2 = new SingleFlagSymbol("flags.LAND", flags, 0x02);
        final SingleFlagSymbol s3 = new SingleFlagSymbol("flags.CLOUD", flags, 0x04);
        final RasterDataSymbol r1 = new RasterDataSymbol(reflec_4.getName(), reflec_4, RasterDataSymbol.Source.GEOPHYSICAL);
        final RasterDataSymbol r2 = new RasterDataSymbol(reflec_5.getName(), reflec_5, RasterDataSymbol.Source.GEOPHYSICAL);
        final int[] intDataElems = new int[]{-1};
        s1.setData(intDataElems);
        s2.setData(intDataElems);
        s3.setData(intDataElems);
        final float[] floatDataElems = new float[]{1f};
        r1.setData(floatDataElems);
        r2.setData(floatDataElems);
        final DefaultNamespace namespace = new DefaultNamespace();
        namespace.registerSymbol(s1);
        namespace.registerSymbol(s2);
        namespace.registerSymbol(s3);
        namespace.registerSymbol(r1);
        namespace.registerSymbol(r2);
        final String code = "(flags.WATER OR flags.LAND) AND NOT flags.CLOUD ? sq(reflec_5 - 0.2*reflec_4) / sq(reflec_5 + 0.4*reflec_4) : NaN";
        final Term term = new ParserImpl(namespace, true).parse(code);

        final RasterDataEvalEnv evalEnv = new RasterDataEvalEnv(0, 0, 1, 1);

        long t2 = System.nanoTime();
        for (int i = 0; i < MAX_NUM_TEST_LOOPS____; i++) {
            term.evalI(evalEnv);
        }
        long t3 = System.nanoTime();
        long dt = t3 - t2;
        long numOps = Math.round(MAX_NUM_TEST_LOOPS____ * (1.0E9 / dt));

        System.out.println("BandArithmeticPerformanceTest: #ops/s = " + numOps);
        assertTrue(String.format("Low evaluation performance detected (%d ops/s for term \"%s\"): Term implementation change?", numOps, code),
                   numOps > MIN_NUM_OPS_PER_SECOND);
    }

}
