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
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * This class is an example implementation and does not belong to the public API.
 * <p/>
 * The implementation demonstrates the usage of {@link SelectionChangeListener} within a
 * {@link org.esa.beam.framework.ui.application.ToolView ToolView}.
 * A selection change listener enables the tool view to listen for selection change events, as the name already says.
 * By investigating the selection event the events can be filtered for those events the tool view is interested in.
 */
public class SelectionObservingToolView extends AbstractToolView {

    private static final String MSG_NO_SELECTION = "NO SELECTION";
    private static final String MSG_NO_SELECTION_CONTEXT = "NO SELECTION CONTEXT";

    private final MySelectionChangeListener selectionListener;
    private JLabel contextLabel;
    private JTextArea selectionTextArea;


    public SelectionObservingToolView() {
        selectionListener = new MySelectionChangeListener();
    }

    @Override
    protected JComponent createControl() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setRowWeightY(1, 1.0);
        final JPanel panel = new JPanel(tableLayout);
        contextLabel = new JLabel(MSG_NO_SELECTION_CONTEXT);
        final JLabel label1 = new JLabel("Current Selection Context:");
        label1.setLabelFor(contextLabel);
        panel.add(label1);
        panel.add(contextLabel);
        selectionTextArea = new JTextArea(MSG_NO_SELECTION, 3, 40);
        selectionTextArea.setLineWrap(true);
        final JLabel label2 = new JLabel("Current Selection:");
        label2.setLabelFor(selectionTextArea);
        panel.add(label2);
        panel.add(new JScrollPane(selectionTextArea));
        return panel;
    }

    @Override
    public void componentShown() {
        final SelectionManager selectionManager = getContext().getPage().getSelectionManager();
        // initialise the user interface with the current selection state
        initUi(selectionManager);
        // add the selection listener to the selection manager in order to get informed about selection changes
        selectionManager.addSelectionChangeListener(selectionListener);
    }

    private void initUi(SelectionManager selectionManager) {
        updateCurrentSelection(selectionManager.getSelection());
        updateSelectionContext(selectionManager.getSelectionContext());
    }

    @Override
    public void componentHidden() {
        // if the tool view gets invisible, we are not any more interested in selection-change notifications,
        // therefor we can remove the selection listener (it is added if the tool view gets visible again)
        getContext().getPage().getSelectionManager().removeSelectionChangeListener(selectionListener);
    }

    private void updateCurrentSelection(Selection selection) {
        if (selection.isEmpty()) {
            selectionTextArea.setText(MSG_NO_SELECTION);
        } else {
            //
            final Object selectedValue = selection.getSelectedValue();

            // Looking for a special selected value in order to treat it differently
            if (selectedValue instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) selectedValue;
                if (featureFigure.getSimpleFeature().getType().getTypeName().equals("org.esa.beam.Pin")) {
                    selectionTextArea.setText("A placemark is selected!");
                    return;
                }
            }
            selectionTextArea.setText(selection.getPresentationName());
        }
    }

    private void updateSelectionContext(SelectionContext context) {
        if (context == null) {
            contextLabel.setText(MSG_NO_SELECTION_CONTEXT);
        } else {
            contextLabel.setText(context.getClass().getSimpleName());
        }
    }

    private class MySelectionChangeListener implements SelectionChangeListener {


        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            // The selection has changed, go and update the user interface
            updateCurrentSelection(event.getSelection());
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
            // The selection context has changed, go and update the user interface
            updateSelectionContext(event.getSelectionContext());
        }

    }
}
