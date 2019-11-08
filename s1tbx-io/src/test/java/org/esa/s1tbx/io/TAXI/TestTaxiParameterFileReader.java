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

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.s1tbx.io.terrasarx.TerraSarXProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Created by lveci on 17/12/2014.
 */
public class TestTaxiParameterFileReader extends ReaderTest {

    public final static File inputParameterFile = new File(TestData.inputSAR+"TAXI"+File.separator+"pp_m20140809_s20140821_s1a-slc-vv_SS1_with_comments.xml");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputParameterFile + "not found", inputParameterFile.exists());
    }

    public TestTaxiParameterFileReader() {
        super(new TerraSarXProductReaderPlugIn());
    }

    @Test
    public void testOpen() throws Exception {
        final TAXIParameterFileReader reader = new TAXIParameterFileReader(inputParameterFile);
        reader.readParameterFile();
    }
}
