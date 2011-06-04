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

package org.esa.beam.dataio.spot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(TestDirRunner.class)
public class PhysVolDescriptorTest {
    @Test
    public void testIt() throws IOException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");
        FileReader reader = new FileReader(file);
        try {
            PhysVolDescriptor descriptor = new PhysVolDescriptor(reader);
            assertEquals("0001", descriptor.getLogVolDirName());
            assertEquals("0001/0001_LOG.TXT", descriptor.getLogVolDescriptorFileName());
            assertEquals(1, descriptor.getPhysVolNumber());
            assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
            assertEquals("VGT PRODUCT FORMAT V1.5", descriptor.getFormatReference());
        } finally {
            reader.close();
        }
    }
}
