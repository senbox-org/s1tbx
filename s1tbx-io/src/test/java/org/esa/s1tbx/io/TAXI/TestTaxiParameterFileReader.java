/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.TAXI;

import org.esa.s1tbx.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

/**
 * Created by lveci on 17/12/2014.
 */
public class TestTaxiParameterFileReader {

    public final static File inputParameterFile = new File(TestData.inputSAR+"InSAR"+File.separator+"pp_m20140809_s20140821_s1a-slc-vv_SS1_with_comments.xml");

    @Test
    public void testOpen() throws Exception {
        final File inputFile = inputParameterFile;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }

        final TAXIParameterFileReader reader = new TAXIParameterFileReader(inputFile);
        reader.readParameterFile();

    }
}
