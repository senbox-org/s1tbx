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
package org.esa.s1tbx.io.orbits;

import com.jaunt.JauntException;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by luis on 02/04/2016.
 */
public class TestQCScraper {

    @Test
    public void testDownloadPreciseOrbitFile() throws JauntException {
        final QCScraper qc = new QCScraper(QCScraper.POEORB);

        String[] orbitFiles = qc.getFileURLs(2016, 2);
        assertEquals(60, orbitFiles.length);
    }

    @Test
    public void testDownloadRestituteOrbitFile() throws JauntException {
        final QCScraper qc = new QCScraper(QCScraper.RESORB);

        String[] orbitFiles = qc.getFileURLs(2016, 2);
        assertEquals(640, orbitFiles.length);
    }
}
