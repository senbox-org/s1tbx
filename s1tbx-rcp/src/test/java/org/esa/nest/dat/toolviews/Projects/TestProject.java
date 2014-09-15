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
package org.esa.nest.dat.toolviews.Projects;

import org.apache.commons.io.FileUtils;
import org.esa.snap.util.ResourceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Jul 2, 2008
 * To change this template use File | Settings | File Templates.
 */
public class TestProject {

    private Project project = Project.instance();
    private final static File projectFolder = new File(ResourceUtils.findHomeFolder().getAbsolutePath()
            + File.separator + "testProject");
    private final static File projectFile = new File(projectFolder.getAbsolutePath()
            + File.separator + "TestProjectFile.xml");

    @Before
    public void setUp() throws Exception {
        if (!projectFolder.exists())
            projectFolder.mkdir();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(projectFolder);
        project = null;
    }

    @Test
    public void testInitProject() {
        project.initProject(projectFile);

        final File[] files = projectFolder.listFiles();
        assertEquals(files.length, 4);

        boolean foundGraphs=false, foundImport=false, foundProcessed=false, foundProductSets=false;
        // order not the same on different OS
        for(File file : files) {
            if(file.getName().equals("Graphs"))
                foundGraphs = true;
            if(file.getName().equals("Imported Products"))
                foundImport = true;
            if(file.getName().equals("Processed Products"))
                foundProcessed = true;
            if(file.getName().equals("ProductSets"))
                foundProductSets = true;
        }
        assertTrue(foundGraphs && foundImport && foundProcessed && foundProductSets);
    }


}