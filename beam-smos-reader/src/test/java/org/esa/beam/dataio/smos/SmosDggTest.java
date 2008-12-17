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
package org.esa.beam.dataio.smos;

import junit.framework.TestCase;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class SmosDggTest extends TestCase {

    public void testGridPointIdToSeqnum() {
        assertEquals(1, SmosDgg.smosGridPointIdToDggridSeqnum(1));
        assertEquals(2, SmosDgg.smosGridPointIdToDggridSeqnum(2));
        assertEquals(3, SmosDgg.smosGridPointIdToDggridSeqnum(3));

        assertEquals(262144, SmosDgg.smosGridPointIdToDggridSeqnum(262144));
        assertEquals(262145, SmosDgg.smosGridPointIdToDggridSeqnum(262145));

        assertEquals(262146, SmosDgg.smosGridPointIdToDggridSeqnum(1000001));
        assertEquals(262147, SmosDgg.smosGridPointIdToDggridSeqnum(1000002));
        assertEquals(524288, SmosDgg.smosGridPointIdToDggridSeqnum(1262143));
        assertEquals(524289, SmosDgg.smosGridPointIdToDggridSeqnum(1262144));

        assertEquals(524290, SmosDgg.smosGridPointIdToDggridSeqnum(2000001));
        assertEquals(524291, SmosDgg.smosGridPointIdToDggridSeqnum(2000002));

        assertEquals(2359296, SmosDgg.smosGridPointIdToDggridSeqnum(8262143));
        assertEquals(2359297, SmosDgg.smosGridPointIdToDggridSeqnum(8262144));

        assertEquals(2359298, SmosDgg.smosGridPointIdToDggridSeqnum(9000001));
        assertEquals(2359299, SmosDgg.smosGridPointIdToDggridSeqnum(9000002));

        assertEquals(2621441, SmosDgg.smosGridPointIdToDggridSeqnum(9262144));
        assertEquals(2621442, SmosDgg.smosGridPointIdToDggridSeqnum(9262145));
    }

    public void testSeqnumToGridPointId() {
        assertEquals(1, seqnumToGridPointId(1));
        assertEquals(2, seqnumToGridPointId(2));
        assertEquals(3, seqnumToGridPointId(3));

        assertEquals(262144, seqnumToGridPointId(262144));
        assertEquals(262145, seqnumToGridPointId(262145));

        assertEquals(1000001, seqnumToGridPointId(262146));
        assertEquals(1000002, seqnumToGridPointId(262147));
        assertEquals(1262143, seqnumToGridPointId(524288));
        assertEquals(1262144, seqnumToGridPointId(524289));

        assertEquals(2000001, seqnumToGridPointId(524290));
        assertEquals(2000002, seqnumToGridPointId(524291));

        assertEquals(8262143, seqnumToGridPointId(2359296));
        assertEquals(8262144, seqnumToGridPointId(2359297));

        assertEquals(9000001, seqnumToGridPointId(2359298));
        assertEquals(9000002, seqnumToGridPointId(2359299));
        assertEquals(9262144, seqnumToGridPointId(2621441));

        assertEquals(9262145, seqnumToGridPointId(2621442));
    }

    private static int seqnumToGridPointId(int seqnum) {
        if (seqnum < 262146) {
            return seqnum;
        }
        if (seqnum < 2621442) {
            return ((seqnum - 2) / 262144) * 1000000 + ((seqnum - 2) % 262144) + 1;
        }
        
        return 9262145 + (seqnum - 2621442);
    }
}
