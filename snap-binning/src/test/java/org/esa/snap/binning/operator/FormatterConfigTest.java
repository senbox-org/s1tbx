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

package org.esa.snap.binning.operator;

import com.bc.ceres.binding.BindingException;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class FormatterConfigTest {

    private FormatterConfig config;

    @Before
    public void initOutputterConfig() throws IOException, BindingException {
        config = loadConfig("FormatterConfigTest.xml");
    }

    @Test
    public void testOutputterConfig() {
        assertEquals("RGB", config.getOutputType());
        assertEquals("PNG", config.getOutputFormat());
        assertEquals("level-3-rgb.png", config.getOutputFile());
    }

    @Test
    public void testXmlGeneration() throws BindingException {
        final String xml = config.toXml();
        //System.out.println("xml = \n" + xml);
        final FormatterConfig configCopy = FormatterConfig.fromXml(xml);

        assertEquals(config.getOutputFile(), configCopy.getOutputFile());
        assertEquals(config.getOutputFormat(), configCopy.getOutputFormat());
        assertEquals(config.getOutputType(), configCopy.getOutputType());
        assertArrayEquals(config.getBandConfigurations(), configCopy.getBandConfigurations());
    }

    private FormatterConfig loadConfig(String configPath) throws IOException, BindingException {
        final InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(configPath));
        try {
            return FormatterConfig.fromXml(FileUtils.readText(reader));
        } finally {
            reader.close();
        }
    }
}
