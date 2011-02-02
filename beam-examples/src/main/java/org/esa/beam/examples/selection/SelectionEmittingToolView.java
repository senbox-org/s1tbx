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
import com.bc.ceres.swing.selection.AbstractSelectionContext;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.DefaultSelection;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
        JList listOfSelectables = new JList(new String[]{"Red", "Blue", "Green", "White", "Black"});
        listOfSelectables.setVisibleRowCount(3);
        listOfSelectables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JLabel("Make your selection: "));
        final JScrollPane scrollPane = new JScrollPane(listOfSelectables);
        panel.add(scrollPane);

        // MySelectionContext does the handling of selections
        selectionContext = new MySelectionContext(listOfSelectables);
        return panel;
    }

    @Override
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    private static class MySelectionContext extends AbstractSelectionContext {


        private JList listOfSelectables;

        private MySelectionContext(JList listOfSelectables) {
            this.listOfSelectables = listOfSelectables;
            // register a listener at the source of selections to get notified if selection changes
            this.listOfSelectables.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    fireSelectionChange(getSelection());
                }
            });
        }

        @Override
        public void setSelection(Selection selection) {
            // Change the current selection; it is requested from an external source
            listOfSelectables.setSelectedValue(selection.getSelectedValue(), true);
        }

        @Override
        public Selection getSelection() {
            // Create a Selection object if there is a selection otherwise use Selection.EMPTY if there is no selected item.
            final Object selectedValue = listOfSelectables.getSelectedValue();
            if (selectedValue == null) {
                return Selection.EMPTY;
            } else {
                return new DefaultSelection<Object>(selectedValue);
            }
        }

    }

}
