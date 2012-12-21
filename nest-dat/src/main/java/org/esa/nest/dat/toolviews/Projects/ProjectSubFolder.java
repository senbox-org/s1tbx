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

import org.jdom.Attribute;
import org.jdom.Element;

import java.io.File;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
* User: lveci
* Date: Jul 2, 2008
* To change this template use File | Settings | File Templates.
*/
public class ProjectSubFolder {

    private String folderName;
    private final File path;
    private final Vector<ProjectFile> fileList = new Vector<ProjectFile>(20);
    private final Vector<ProjectSubFolder> subFolders = new Vector<ProjectSubFolder>(10);
    private boolean removeable = true;
    private boolean physical = false;
    private boolean createdByUser = false;
    private FolderType folderType;

    enum FolderType { ROOT, PRODUCTSET, GRAPH, PRODUCT }

    ProjectSubFolder(File newPath, String name, boolean isPhysical, FolderType type) {
        path = newPath;
        folderName = name;
        physical = isPhysical;
        folderType = type;

        if(physical && !path.exists())
            path.mkdir();
    }

    void setCreatedByUser(boolean flag) {
        createdByUser = flag;
    }

    boolean isCreatedByUser() {
        return createdByUser;
    }

    FolderType getFolderType() {
        return folderType;
    }

    public void setFolderType(FolderType type) {
        folderType = type;
    }

    void setRemoveable(boolean flag) {
        removeable = flag;
    }

    public boolean canBeRemoved() {
        return removeable;
    }

    void setPhysical(boolean flag) {
        physical = flag;
    }

    boolean isPhysical() {
        return physical;
    }

    public String getName() {
        return folderName;
    }

    public File getPath() {
        return path;
    }

    void clear() {
        fileList.clear();
        subFolders.clear();
    }

    boolean addFile(ProjectFile file) {
        if(!containsFile(file.getFile())) {
            file.setFolderType(this.folderType);
            fileList.add(file);
            return true;
        }
        return false;
    }

    ProjectSubFolder addSubFolder(String name) {
        ProjectSubFolder newFolder = findFolder(name);
        if(newFolder != null)
            return newFolder;

        newFolder = new ProjectSubFolder(new File(path, name), name, physical, folderType);
        subFolders.add(newFolder);
        return newFolder;
    }

    ProjectSubFolder addSubFolder(ProjectSubFolder newFolder) {
        if(findFolder(newFolder.getName()) != null)
            return newFolder;

        if(physical) {
            newFolder.setPhysical(physical);
            if(!newFolder.getPath().exists())
                newFolder.getPath().mkdir();
        }
        subFolders.add(newFolder);
        return newFolder;
    }

    void renameTo(String newName) {

        if(physical) {
            final String newPath = path.getParent() + File.separator + folderName;
            final File newFile = new File(newPath);
            if(path.renameTo(newFile))
                folderName = newName;
        } else
            folderName = newName;
    }

    void removeSubFolder(ProjectSubFolder subFolder) {
        subFolders.remove(subFolder);
    }

    void removeFile(File file) {

        for(ProjectFile projFile : fileList) {
            if(projFile.getFile().equals(file)) {
                fileList.remove(projFile);
                return;
            }
        }
    }

    public ProjectSubFolder findFolder(String name) {
        for(int i=0; i < subFolders.size(); ++i) {
            final ProjectSubFolder folder = subFolders.elementAt(i);
            if(folder.getName().equals(name))
                return folder;
        }
        return null;
    }

    public boolean containsFile(File file) {
        for(ProjectFile projFile : fileList) {
            if(projFile.getFile().equals(file)) {
                return true;
            }
        }
        for(int i=0; i < subFolders.size(); ++i) {
            final ProjectSubFolder folder = subFolders.elementAt(i);
            if(folder.containsFile(file))
                return true;
        }
        return false;
    }

    public Vector<ProjectSubFolder> getSubFolders() {
        return subFolders;
    }

    public Vector<ProjectFile> getFileList() {
        return fileList;
    }

    public Element toXML() {
        final Element elem = new Element("subFolder");
        elem.setAttribute("name", folderName);
        if(createdByUser)
            elem.setAttribute("user", "true");

        for(int i=0; i < subFolders.size(); ++i) {
            final ProjectSubFolder sub = subFolders.elementAt(i);
            final Element subElem = sub.toXML();
            elem.addContent(subElem);
        }

        for(int i=0; i < fileList.size(); ++i) {
            final ProjectFile projFile = fileList.elementAt(i);
            final Element fileElem = new Element("product");
            fileElem.setAttribute("path", projFile.getFile().getAbsolutePath());
            fileElem.setAttribute("name", projFile.getDisplayName());
            elem.addContent(fileElem);
        }

        return elem;
    }

    public void fromXML(Element elem, Vector<ProjectSubFolder> folderList, Vector<ProjectFile> prodList) {
        final List children = elem.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element child = (Element) aChild;
                if(child.getName().equals("subFolder")) {
                    final Attribute attrib = child.getAttribute("name");
                    final ProjectSubFolder subFolder = addSubFolder(attrib.getValue());
                    final Attribute attribUser = child.getAttribute("user");
                    if(attribUser != null && attrib.getValue().equals("true"))
                        createdByUser = true;
                    subFolder.fromXML(child, folderList, prodList);
                } else if(child.getName().equals("product")) {
                    final Attribute pathAttrib = child.getAttribute("path");
                    final Attribute nameAttrib = child.getAttribute("name");

                    final File file = new File(pathAttrib.getValue());
                    if (file.exists()) {
                        folderList.add(this);
                        final ProjectFile newFile = new ProjectFile(file, nameAttrib.getValue());
                        boolean added = prodList.add(newFile);
                        if(added) {
                            newFile.setFolderType(this.folderType);
                        }
                    }
                }
            }
        }
    }


}
