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

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.opendap.datamodel.CatalogNode;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

/**
 * @author Thomas Storm
 */
class CatalogTreeUtils {

    public static void addCellRenderer(final JTree jTree) {
        final ImageIcon dapIcon = UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/DRsProduct16.png", CatalogTree.class);
        final ImageIcon fileIcon = UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/FRsProduct16.png", CatalogTree.class);
        final ImageIcon errorIcon = UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/NoAccess16.png", CatalogTree.class);
        jTree.setToolTipText(null);
        jTree.setCellRenderer(new TreeCellRenderer(dapIcon, fileIcon, errorIcon));
    }

    public static List<InvDataset> getCatalogDatasets(InputStream catalogIS, URI catalogBaseUri) {
        final InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true);
        final InvCatalogImpl catalog = factory.readXML(catalogIS, catalogBaseUri);
        return catalog.getDatasets();
    }

    public static void addTreeSelectionListener(final JTree jTree, final CatalogTree.LeafSelectionListener leafSelectionListener) {

        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {


                final TreePath[] paths = e.getPaths();
                for (TreePath path : paths) {
                    final DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                    final Object userObject = lastPathComponent.getUserObject();
                    if (!(userObject instanceof OpendapLeaf)) {
                        continue;
                    }
                    final OpendapLeaf dapObject = (OpendapLeaf) userObject;
                    leafSelectionListener.leafSelectionChanged(e.isAddedPath(path), dapObject);
                }

                TreePath path = e.getPath();
                final DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                final Object userObject = lastPathComponent.getUserObject();
                if (!(userObject instanceof OpendapLeaf)) {
                    return;
                }
                OpendapLeaf opendapLeaf = (OpendapLeaf) userObject;
                if (opendapLeaf.isDapAccess()) {
                    leafSelectionListener.dapLeafSelected(opendapLeaf);
                } else if (opendapLeaf.isFileAccess()) {
                    leafSelectionListener.fileLeafSelected(opendapLeaf);
                }
            }
        });
    }

    public static boolean isDapNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            return (userObject instanceof OpendapLeaf) && ((OpendapLeaf) userObject).isDapAccess();
        }
        return false;
    }

    public static boolean isFileNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            return (userObject instanceof OpendapLeaf) && ((OpendapLeaf) userObject).isFileAccess();
        }
        return false;
    }

    public static boolean isCatalogReferenceNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            return (userObject instanceof CatalogNode);
        }
        return false;
    }

    public static boolean isHyraxId(String id) {
        return id != null && id.startsWith("/") && id.endsWith("/");
    }

    public static void appendCatalogNode(MutableTreeNode parentNode, DefaultTreeModel treeModel, InvCatalogRef catalogRef) {
        final DefaultMutableTreeNode catalogNode = new DefaultMutableTreeNode(catalogRef.getName());
        final String catalogPath = catalogRef.getURI().toASCIIString();
        final CatalogNode opendapNode = new CatalogNode(catalogPath, catalogRef);
        opendapNode.setCatalogUri(catalogPath);
        catalogNode.add(new DefaultMutableTreeNode(opendapNode));
        treeModel.insertNodeInto(catalogNode, parentNode, parentNode.getChildCount());
    }

    public static DefaultMutableTreeNode createRootNode() {
        return new DefaultMutableTreeNode("root", true);
    }

    public static List<InvDataset> getCatalogDatasets(DefaultMutableTreeNode node) throws IOException {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof CatalogNode)) {
            return Collections.emptyList();
        }
        final CatalogNode catalogNode = (CatalogNode) userObject;
        final URL catalogUrl;
        InputStream inputStream = null;
        try {
            catalogUrl = new URL(catalogNode.getCatalogUri());
            URLConnection urlConnection = catalogUrl.openConnection();
            inputStream = urlConnection.getInputStream();
            return getCatalogDatasets(inputStream, catalogUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                    // ok
                }
            }
        }
    }
}
