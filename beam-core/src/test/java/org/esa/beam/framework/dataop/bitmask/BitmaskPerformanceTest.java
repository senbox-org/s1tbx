/*
 * $Id: BitmaskPerformanceTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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

package org.esa.beam.framework.dataop.bitmask;

import junit.framework.TestCase;

public class BitmaskPerformanceTest extends TestCase {

    public BitmaskPerformanceTest(String s) {
        super(s);
    }

    public void testThatPerformanceIsSufficient() throws BitmaskExpressionParseException {
        final String[] flagNames = {"CLOUD", "LAND", "WATER"};
        final int[] flagMasks = {0x01, 0x02, 0x04};
        final int[] samples = {-1};
        final DefaultFlagDataset flagDataset = DefaultFlagDataset.create("flags", flagNames, flagMasks, samples);
        final BitmaskTermEvalContext context = new DefaultBitmaskTermEvalContext(flagDataset);

        String code = "(flags.WATER OR flags.LAND) AND NOT flags.CLOUD";
        BitmaskTerm bt = BitmaskExpressionParser.parse(code);
        final int n = 10000000;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
        }
        long t2 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            bt.evaluate(context, 0);
        }
        long t3 = System.currentTimeMillis();
        long dt = (t3 - t2) - (t2 - t1);
        long numOps = Math.round(n * (1000.0 / dt));

        System.out.println("BitmaskPerformanceTest: " + numOps + " ops per second for term '" + code + "'");
        assertTrue("Low evaluation performance detected: BitmaskTerm implementation change?",
                   numOps > 2500000);
    }
}
