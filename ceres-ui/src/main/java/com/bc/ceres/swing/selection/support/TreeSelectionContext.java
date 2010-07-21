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

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.AbstractSelectionContext;
import com.bc.ceres.swing.selection.Selection;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * A selection provider that wraps a {@link JTree}.
 * Elements contained in {@link Selection}s handled by this provider
 * represent currently selected tree node paths as {@code Object[]} values.
 */
public class TreeSelectionContext extends AbstractSelectionContext {

    private final TreeSelectionListener treeSelectionListener;
    private JTree tree;

    public TreeSelectionContext(final JTree tree) {
        treeSelectionListener = new TreeSelectionHandler();
        this.tree = tree;
        this.tree.addTreeSelectionListener(treeSelectionListener);
    }

    @Override
    public Selection getSelection() {
        final TreePath[] treePaths = tree.getSelectionPaths();
        final Selection selection;
        if (treePaths != null && treePaths.length > 0) {
            selection = new DefaultSelection<TreePath>(treePaths);
        } else {
            selection = DefaultSelection.EMPTY;
        }
        return selection;
    }

    @Override
    public void setSelection(Selection selection) {
        Object[] objects = selection.getSelectedValues();
        if (objects instanceof TreePath[] && objects.length > 0) {
            tree.setSelectionPaths((TreePath[]) objects);
        } else if (objects == null || objects.length == 0) {
            tree.clearSelection();
        }
    }

    public JTree getTree() {
        return tree;
    }

    public void setTree(JTree tree) {
        if (tree != this.tree) {
            this.tree.removeTreeSelectionListener(treeSelectionListener);
            this.tree = tree;
            this.tree.addTreeSelectionListener(treeSelectionListener);
            fireSelectionChange(getSelection());
        }
    }

    protected void handleTreeSelectionChanged(TreeSelectionEvent event) {
        fireSelectionChange(getSelection());
    }

    private class TreeSelectionHandler implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            handleTreeSelectionChanged(e);
        }
    }
}