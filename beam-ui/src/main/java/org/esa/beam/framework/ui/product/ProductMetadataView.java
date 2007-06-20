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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.tool.Tool;

/**
 * A view component used to display a product's metadata in tabular form.
 */
public class ProductMetadataView extends BasicView implements ProductNodeView {

    private ProductMetadataTable _metadataTable;

    public ProductMetadataView(MetadataElement metadataElement) {
        _metadataTable = new ProductMetadataTable(metadataElement);
        _metadataTable.addMouseListener(new PopupMenuHandler(this));
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, new JScrollPane(_metadataTable));
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
}
