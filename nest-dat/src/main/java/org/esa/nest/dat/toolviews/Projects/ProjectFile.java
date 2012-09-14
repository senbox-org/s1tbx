/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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

import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.nest.gpf.GPFProcessor;

import java.io.File;

/**
 * Defines a File in a Project
* User: lveci
* Date: Aug 28, 2008
*/
public class ProjectFile {
    private final File file;
    private final String displayName;
    private String tooltipText;
    private ProjectSubFolder.FolderType folderType;

    ProjectFile(File f, String name) {
        file = f;
        displayName = name.trim();
        tooltipText = displayName;
    }

    void setFolderType(ProjectSubFolder.FolderType folder) {
        folderType = folder;
        if(folderType == ProjectSubFolder.FolderType.GRAPH) {
            try {
                Graph graph = GPFProcessor.readGraph(file, null);
                if(graph != null) {
                    XppDom presXML = graph.getApplicationData("Presentation");
                    if(presXML != null) {
                        XppDom descXML = presXML.getChild("Description");
                        if(descXML != null && descXML.getValue() != null) {
                            tooltipText = descXML.getValue();
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ProjectSubFolder.FolderType getFolderType() {
        return folderType;
    }

    public File getFile() {
        return file;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToolTipText() {
        return tooltipText;
    }

    void setToolTipText(String text) {
        tooltipText = text;
    }
}
