/*
 * $Id: ProductMetadataView.java,v 1.1 2006/10/10 14:47:37 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.tool.Tool;

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
        add(BorderLayout.CENTER, new JScrollPane(_metadataTable));
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
    public ProductNode getVisibleProductNode() {
        return getMetadataElement();
    }

    public JPopupMenu createPopupMenu(Component component) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (getCommandUIFactory() != null) {
            getCommandUIFactory().addContextDependentMenuItems("metadata", popupMenu);
        }
        popupMenu.add(expandMenuItem);
        popupMenu.add(collapseMenuItem);
        return popupMenu;
    }

    public JPopupMenu createPopupMenu(MouseEvent event) {
        return null;
    }

    /**
     * Returns the active tool for this view.
     */
    public Tool getTool() {
        return null;
    }

    /**
     * Returns the active tool for this view.
     */
    public void setTool(Tool tool) {
    }

    /**
     * Releases all of the resources used by this view, its subcomponents, and all of its owned children.
     */
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

        public void actionPerformed(ActionEvent e) {
            final ProductMetadataTable metadataTable = getMetadataTable();
            metadataTable.expandAll();
        }
    }
}
