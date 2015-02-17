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

package org.esa.beam.examples.selection;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.ListSelectionContext;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;

// todo -
// Currently the provided selection context gets lost if the application menu is invoked,
// because the active component changes. It is replaced by the previous selection context.

/**
 * This class is an example implementation and does not belong to the public API.
 * <p/>
 * The implementation demonstrates the usage of {@link SelectionContext} within a
 * {@link org.esa.beam.framework.ui.application.ToolView ToolView}.
 * A selection context is a {@link com.bc.ceres.swing.selection.SelectionSource selection source}
 * with additional capabilities, e.g. insert, delete and select all.
 * It can be seen as the environment in which selections reside and originate,
 * e.g. a GUI table, list, tree, or a drawing of figures.
 * By providing a selection context to the BEAM framework, by implementing {@link #getSelectionContext()},
 * the selection within this tool view is published. This enables other components to listen for selection changes
 * and react appropriate.
 */
public class SelectionEmittingToolView extends AbstractToolView {

    // The selection done in this view is published by the SelectionContext.
    // This makes the selection known to the whole application.
    private SelectionContext selectionContext;

    @Override
    protected JComponent createControl() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableWeightX(1.0);
        final JPanel panel = new JPanel(tableLayout);
        JList selectableItemList = new JList(new String[]{"Red", "Blue", "Green", "White", "Black"});
        selectableItemList.setVisibleRowCount(3);
        selectableItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JLabel("Make your selection: "));
        final JScrollPane scrollPane = new JScrollPane(selectableItemList);
        panel.add(scrollPane);

        // SelectionContextImpl does the handling of selections
        selectionContext = new ListSelectionContext(selectableItemList);
        return panel;
    }

    @Override
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }
}
