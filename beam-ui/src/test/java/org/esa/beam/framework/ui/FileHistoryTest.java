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
package org.esa.beam.framework.ui;

import junit.framework.TestCase;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.GlobalTestTools;
import org.esa.beam.util.PropertyMap;

import java.io.File;
import java.io.IOException;

/**
 * <code>FileHistory</code> is a fixed-size array for the pathes of files opened/saved by a user. If a new file is added
 * and the file history is full, the list of registered files is shifted so that the oldest file path is beeing
 * skipped..
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class FileHistoryTest extends TestCase {

    private File _a;
    private File _b;
    private File _c;
    private File _d;
    private File _e;

    public FileHistoryTest(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _a = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "A.dim");
        _b = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "B.dim");
        _c = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "C.dim");
        _d = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "D.dim");
        _e = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "E.dim");
        try {
            _a.getParentFile().mkdirs();
            _a.createNewFile();
            _b.createNewFile();
            //_c.createNewFile(); should not be created
            _d.createNewFile();
            _e.createNewFile();
        } catch (IOException e) {
        }
    }

    @Override
    public void tearDown() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    public void testFileHistory() {
        assertTrue(_a.getAbsolutePath() + " does not exist", _a.exists());
        assertTrue(_b.getAbsolutePath() + " does not exist", _b.exists());
        assertTrue(_c.getAbsolutePath() + " does exist", !_c.exists()); // should not exist
        assertTrue(_d.getAbsolutePath() + " deos not exist", _d.exists());
        assertTrue(_e.getAbsolutePath() + " deos not exist", _e.exists());
        final String propertyKey = "recent.files.";
        final PropertyMap properties = new PropertyMap();
        properties.setPropertyInt(propertyKey + ".length", 3);
        properties.setPropertyString(propertyKey + ".0", _a.getAbsolutePath());
        properties.setPropertyString(propertyKey + ".1", _b.getAbsolutePath());
        properties.setPropertyString(propertyKey + ".2", _c.getAbsolutePath());
        properties.setPropertyString(propertyKey + ".3", _d.getAbsolutePath());
        properties.setPropertyString(propertyKey + ".4", _e.getAbsolutePath());

        //create and init new FileHistory
        final FileHistory history = new FileHistory(9, propertyKey);

        assertEquals(9, history.getMaxNumEntries());
        assertEquals(0, history.getNumEntries());
        assertNull(history.getEntries());

        //init by Properties
        history.initBy(properties);

        assertEquals(3, history.getMaxNumEntries());
        assertEquals(2, history.getNumEntries());
        String[] files = history.getEntries();
        assertEquals(2, files.length);
        assertEquals(_a.getAbsolutePath(), files[0]);
        assertEquals(_b.getAbsolutePath(), files[1]);

        //Add new File to existin two files
        history.push(_d.getAbsolutePath());

        assertEquals(3, history.getNumEntries());
        files = history.getEntries();
        assertEquals(3, files.length);
        assertEquals(_d.getAbsolutePath(), files[0]);
        assertEquals(_a.getAbsolutePath(), files[1]);
        assertEquals(_b.getAbsolutePath(), files[2]);

        //Add new File to existing three files
        history.push(_e.getAbsolutePath());

        assertEquals(3, history.getNumEntries());
        files = history.getEntries();
        assertEquals(3, files.length);
        assertEquals(_e.getAbsolutePath(), files[0]);
        assertEquals(_d.getAbsolutePath(), files[1]);
        assertEquals(_a.getAbsolutePath(), files[2]);

        //decreace num max files
        history.setMaxNumEntries(2);

        assertEquals(2, history.getNumEntries());
        files = history.getEntries();
        assertEquals(2, files.length);
        assertEquals(_e.getAbsolutePath(), files[0]);
        assertEquals(_d.getAbsolutePath(), files[1]);

        //copy values to properties
        history.copyInto(properties);

        assertEquals(2, properties.getPropertyInt(propertyKey + ".length"));
        assertEquals(_e.getAbsolutePath(), properties.getPropertyString(propertyKey + ".0"));
        assertEquals(_d.getAbsolutePath(), properties.getPropertyString(propertyKey + ".1"));
        assertNull(properties.getPropertyString(propertyKey + ".2", null));
        assertNull(properties.getPropertyString(propertyKey + ".3", null));
        assertNull(properties.getPropertyString(propertyKey + ".4", null));
    }
}