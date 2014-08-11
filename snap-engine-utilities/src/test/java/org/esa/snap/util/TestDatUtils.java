/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import junit.framework.TestCase;

import java.io.File;

/**
 * DatUtils Tester.
 *
 * @author lveci
 */
public class TestDatUtils extends TestCase {

    public TestDatUtils(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFindHomeFolder() {
        final File homeFolder = ResourceUtils.findHomeFolder();
        final File file = new File(homeFolder, "config" + File.separator + "settings_win.xml");

        assertTrue(file.exists());
    }
}