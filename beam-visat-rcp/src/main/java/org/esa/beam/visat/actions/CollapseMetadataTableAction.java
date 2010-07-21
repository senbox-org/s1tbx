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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductMetadataTable;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.visat.VisatApp;

public class CollapseMetadataTableAction extends ExecCommand {

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        collapseMetadataTable();
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    @Override
    public void updateState(CommandEvent event) {
        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        boolean expandAllowed = false;
        if (view instanceof ProductMetadataView) {
            final ProductMetadataTable metadataTable = ((ProductMetadataView) view).getMetadataTable();
            expandAllowed = metadataTable.isExpandAllAllowed();
        }
        setEnabled(expandAllowed);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "Export Metadata" command
    /////////////////////////////////////////////////////////////////////////

    private static void collapseMetadataTable() {

        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        if (!(view instanceof ProductMetadataView)) {
            return;
        }

        final ProductMetadataView productMetadataView = (ProductMetadataView) view;
        final ProductMetadataTable metadataTable = productMetadataView.getMetadataTable();
        metadataTable.collapseAll();
    }

}