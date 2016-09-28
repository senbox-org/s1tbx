/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.orbits.sentinel1;

import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Test Sentinel-1 QC Scrapping of orbit files
 */
@Ignore("Takes too long")
public class TestQCScraper {

    @Test
    public void testDownloadPreciseOrbitFileS1A() {
        final QCScraper qc = new QCScraper(SentinelPODOrbitFile.PRECISE);

        String[] orbitFiles = qc.getFileURLs("S1A", 2016, 2);
        assertEquals(29, orbitFiles.length);
    }

    @Test
    public void testDownloadPreciseOrbitFileS1B() {
        final QCScraper qc = new QCScraper(SentinelPODOrbitFile.PRECISE);

        String[] orbitFiles = qc.getFileURLs("S1B", 2016, 7);
        assertEquals(31, orbitFiles.length);
    }

    @Test
    public void testDownloadRestituteOrbitFileS1A() {
        final QCScraper qc = new QCScraper(SentinelPODOrbitFile.RESTITUTED);

        String[] orbitFiles = qc.getFileURLs("S1A", 2016, 3);
        assertEquals(636, orbitFiles.length);
    }

    @Test
    public void testDownloadRestituteOrbitFileS1B() {
        final QCScraper qc = new QCScraper(SentinelPODOrbitFile.RESTITUTED);

        String[] orbitFiles = qc.getFileURLs("S1B", 2016, 7);
        assertEquals(592, orbitFiles.length);
    }
}
