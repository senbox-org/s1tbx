/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import junit.framework.TestCase;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.util.ResourceUtils;

import java.io.File;


/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Jul 2, 2008
 * To change this template use File | Settings | File Templates.
 */
public class TestProject extends TestCase {

    private Project project = Project.instance();
    private final static File projectFolder = new File(ResourceUtils.findHomeFolder().getAbsolutePath()
            + File.separator + "testProject");
    private final static File projectFile = new File(projectFolder.getAbsolutePath()
            + File.separator + "TestProjectFile.xml");

    @Override
    public void setUp() throws Exception {
        if(!projectFolder.exists())
            projectFolder.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        SystemUtils.deleteFileTree(projectFolder);
        project = null;
    }

    public void testInitProject() {
        project.initProject(projectFile);

        File[] files = projectFolder.listFiles();
        assertEquals(files.length, 4);

        /* order not the same on different OS
        assertEquals(files[0].getName(), "Graphs");
        assertEquals(files[1].getName(), "Imported Products");
        assertEquals(files[2].getName(), "Processed Products");
        assertEquals(files[3].getName(), "ProductSets");  */

    }



}