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
package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PopupMenuHandler;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

/**
 * A view component used to display a product's metadata in tabular form.
 */
public class ProductMetadataView extends BasicView implements ProductNodeView {

    private ProductMetadataTable _metadataTable;
    private JMenuItem expandMenuItem;
    private JMenuItem collapseMenuItem;

    public ProductMetadataView(MetadataElement metadataElement) {
        _metadataTable = new ProductMetadataTable(metadataElement);
        _metadataTable.addMouseListener(new PopupMenuHandler(this));
        setLayout(new BorderLayout());
        add(new JScrollPane(_metadataTable), BorderLayout.CENTER);
        expandMenuItem = new JMenuItem(new ExpandAllAction());
        collapseMenuItem = new JMenuItem(new CollapseAllAction());
    }

    public Product getProduct() {
        return getMetadataElement().getProduct();
    }

    public MetadataElement getMetadataElement() {
        return _metadataTable.getMetadataElement();
    }

    public ProductMetadataTable getMetadataTable() {
        return _metadataTable;
    }

    /**
     * Returns the currently visible product node.
     */
    @Override
    public ProductNode getVisibleProductNode() {
        return getMetadataElement();
    }

    @Override
    public JPopupMenu createPopupMenu(Component component) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (getCommandUIFactory() != null) {
            getCommandUIFactory().addContextDependentMenuItems("metadata", popupMenu);
        }
        popupMenu.add(expandMenuItem);
        popupMenu.add(collapseMenuItem);
        return popupMenu;
    }

    @Override
    public JPopupMenu createPopupMenu(MouseEvent event) {
        return null;
    }

    /**
     * Releases all of the resources used by this view, its subcomponents, and all of its owned children.
     */
    @Override
    public void dispose() {
        _metadataTable = null;
        super.dispose();
    }

    private class CollapseAllAction extends AbstractAction {

        public CollapseAllAction() {
            super("Collapse All");
            putValue(MNEMONIC_KEY, (int) 'C');
            putValue(SHORT_DESCRIPTION, "Collapses all tree nodes.");
        }


        @Override
        public boolean isEnabled() {
            final ProductMetadataTable metadataTable = getMetadataTable();
            return metadataTable.isExpandAllAllowed();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final ProductMetadataTable metadataTable = getMetadataTable();
            metadataTable.collapseAll();
        }
    }

    private class ExpandAllAction extends AbstractAction {

        public ExpandAllAction() {
            super("Expand All");
            putValue(MNEMONIC_KEY, (int) 'E');
            putValue(SHORT_DESCRIPTION, "Expands all tree nodes.");
        }


        @Override
        public boolean isEnabled() {
            final ProductMetadataTable metadataTable = getMetadataTable();
            return metadataTable.isExpandAllAllowed();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final ProductMetadataTable metadataTable = getMetadataTable();
            metadataTable.expandAll();
        }
    }
}
