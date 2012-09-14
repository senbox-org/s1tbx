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

import com.jidesoft.icons.IconsFactory;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.Enumeration;
import java.util.Observer;
import java.util.Vector;

/**
 * The tool window which displays the current project
 */
public class ProjectsToolView extends AbstractToolView implements Observer {

    public static final String ID = ProjectsToolView.class.getName();

    private ProjectTree projectTree;
    private DefaultMutableTreeNode rootNode;
    private final Project project = Project.instance();

    public ProjectsToolView() {

    }

    @Override
    public JComponent createControl() {
        Project.instance().addObserver(this);

        final JScrollPane prjScrollPane = new JideScrollPane(createTree());
        prjScrollPane.setPreferredSize(new Dimension(320, 480));
        prjScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        prjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        prjScrollPane.setBorder(null);
        prjScrollPane.setViewportBorder(null);

        final JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.addPane(prjScrollPane);

        return splitPane;
    }

    private ProjectTree createTree() {
        rootNode = new DefaultMutableTreeNode("");
        projectTree = new ProjectTree(false);//rootNode);
        projectTree.populateTree(rootNode);
        projectTree.setRootVisible(false);
        projectTree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) projectTree.getCellRenderer();
        renderer.setLeafIcon(IconsFactory.getImageIcon(BasicApp.class, "/org/esa/beam/resources/images/icons/RsBandAsSwath16.gif"));
        renderer.setClosedIcon(IconsFactory.getImageIcon(BasicApp.class, "/org/esa/beam/resources/images/icons/RsGroupClosed16.gif"));
        renderer.setOpenIcon(IconsFactory.getImageIcon(BasicApp.class, "/org/esa/beam/resources/images/icons/RsGroupOpen16.gif"));
        return projectTree;
    }

    private static void PopulateNode(Vector<ProjectSubFolder> subFolders, DefaultMutableTreeNode treeNode) {

        for (Enumeration e = subFolders.elements(); e.hasMoreElements();)
        {
            final ProjectSubFolder folder = (ProjectSubFolder)e.nextElement();

            final DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder);
            treeNode.add(folderNode);

            final Vector<ProjectFile> fileList = folder.getFileList();
            for (Enumeration file = fileList.elements(); file.hasMoreElements();)
            {
                final DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.nextElement());
                folderNode.add(fileNode);
            }

            final Vector<ProjectSubFolder> moreFolders = folder.getSubFolders();
            if(!moreFolders.isEmpty())
                PopulateNode(moreFolders, folderNode);
        }
    }

    /**
     Implements the functionality of Observer participant of Observer Design Pattern to define a one-to-many
     dependency between a Subject object and any number of Observer objects so that when the
     Subject object changes state, all its Observer objects are notified and updated automatically.

     Defines an updating interface for objects that should be notified of changes in a subject.
     * @param subject The Observerable subject
     * @param data optional data
     */
    public void update(java.util.Observable subject, java.lang.Object data) {

        rootNode.removeAllChildren();

        final ProjectSubFolder projectFolders = project.getProjectSubFolders();
        if(projectFolders == null) {
            projectTree.setRootVisible(false);
            projectTree.populateTree(rootNode);
        } else {
            rootNode.setUserObject(project.getProjectSubFolders());
            projectTree.setRootVisible(true);

            final Vector<ProjectSubFolder> subFolders = project.getProjectSubFolders().getSubFolders();
            PopulateNode(subFolders, rootNode);
            projectTree.populateTree(rootNode);
        }
    }
}