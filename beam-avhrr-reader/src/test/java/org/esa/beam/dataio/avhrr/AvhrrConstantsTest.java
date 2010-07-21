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

package org.esa.beam.dataio.avhrr;

import junit.framework.TestCase;
import org.esa.beam.util.BeamConstants;

public class AvhrrConstantsTest extends TestCase {

    public void testThatConstantsReferToSixDifferntAvhrrChannels() {
        assertEquals(6, AvhrrConstants.CH_STRINGS.length);

        assertEquals("1", AvhrrConstants.CH_STRINGS[0]);
        assertEquals("2", AvhrrConstants.CH_STRINGS[1]);
        assertEquals("3a", AvhrrConstants.CH_STRINGS[2]);
        assertEquals("3b", AvhrrConstants.CH_STRINGS[3]);
        assertEquals("4", AvhrrConstants.CH_STRINGS[4]);
        assertEquals("5", AvhrrConstants.CH_STRINGS[5]);

        assertEquals(6, AvhrrConstants.CH_DATASET_INDEXES.length);

        assertEquals(0, AvhrrConstants.CH_DATASET_INDEXES[0]);
        assertEquals(1, AvhrrConstants.CH_DATASET_INDEXES[1]);
        assertEquals(2, AvhrrConstants.CH_DATASET_INDEXES[2]);
        assertEquals(2, AvhrrConstants.CH_DATASET_INDEXES[3]);
        assertEquals(3, AvhrrConstants.CH_DATASET_INDEXES[4]);
        assertEquals(4, AvhrrConstants.CH_DATASET_INDEXES[5]);

        assertEquals(0, AvhrrConstants.CH_1);
        assertEquals(1, AvhrrConstants.CH_2);
        assertEquals(2, AvhrrConstants.CH_3A);
        assertEquals(3, AvhrrConstants.CH_3B);
        assertEquals(4, AvhrrConstants.CH_4);
        assertEquals(5, AvhrrConstants.CH_5);
    }

    // Important for tie-point geocoding creation
    public void testThatNamesAreSetOk() {
        assertEquals(BeamConstants.LAT_DS_NAME, AvhrrConstants.LAT_DS_NAME);
        assertEquals(BeamConstants.LON_DS_NAME, AvhrrConstants.LON_DS_NAME);
    }
}
