/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.ui;

import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.opendap.utils.OpendapUtils;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.io.IOException;

/**
* @author Thomas Storm
*/
class TreeCellRenderer extends DefaultTreeCellRenderer {

    private final ImageIcon dapIcon;
    private final ImageIcon fileIcon;
    private final ImageIcon errorIcon;

    public TreeCellRenderer(ImageIcon dapIcon, ImageIcon fileIcon, ImageIcon errorIcon) {
        this.dapIcon = dapIcon;
        this.fileIcon = fileIcon;
        this.errorIcon = errorIcon;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (CatalogTreeUtils.isDapNode(value)) {
            setLeafIcon(dapIcon);
            setToolTip(node, tree);
        } else if (CatalogTreeUtils.isFileNode(value)) {
            setLeafIcon(fileIcon);
            setToolTip(node, tree);
        } else try {
            if (leaf && isOpendapLeaf(node) && CatalogTreeUtils.getCatalogDatasets(node).isEmpty()) {
                setLeafIcon(errorIcon);
                tree.setToolTipText(null);
            } else {
                setLeafIcon(getClosedIcon());
            }
        } catch (IOException e) {
            setLeafIcon(errorIcon);
            tree.setToolTipText(null);
        }
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        return this;
    }

    private boolean isOpendapLeaf(DefaultMutableTreeNode node) {
        final Object userObject = node.getUserObject();
        return userObject instanceof OpendapLeaf;
    }

    private void setToolTip(DefaultMutableTreeNode value, JTree tree) {
        int fileSize = ((OpendapLeaf) value.getUserObject()).getFileSize();
        tree.setToolTipText(OpendapUtils.format(fileSize / 1024.0) + " MB");
    }
}
