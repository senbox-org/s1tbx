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

package org.esa.beam.timeseries.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * UI component for selecting variables, working on a {@link VariableSelectionPaneModel}.
 *
 * @author Marco Peters
 */
public class VariableSelectionPane extends JPanel {

    private VariableSelectionPaneModel model;
    private CheckBoxList variableList;
    private CheckBoxSelectionListener checkBoxSelectionListener;

    public VariableSelectionPane() {
        this(new DefaultVariableSelectionPaneModel());
    }

    public VariableSelectionPane(VariableSelectionPaneModel variableSelectionPaneModel) {
        model = variableSelectionPaneModel;
        createPane();
    }

    public void setModel(VariableSelectionPaneModel model) {
        this.model = model;
        updatePane();
    }

    public VariableSelectionPaneModel getModel() {
        return model;
    }

    private void createPane() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setTableWeightX(1.0);
        setLayout(tableLayout);
        variableList = new CheckBoxList(model);
        variableList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                                                                           cellHasFocus);
                if (value instanceof Variable) {
                    Variable variable = (Variable) value;
                    label.setText(variable.getName());
                }
                return label;

            }
        });
        variableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        checkBoxSelectionListener = new CheckBoxSelectionListener(variableList);
        variableList.getCheckBoxListSelectionModel().addListSelectionListener(checkBoxSelectionListener);
        add(new JScrollPane(variableList));
    }

    private void updatePane() {
        variableList.getCheckBoxListSelectionModel().removeListSelectionListener(checkBoxSelectionListener);
        variableList.setModel(model);
        variableList.setCheckBoxListSelectedIndices(getSelectedIndices(model));
        final CheckBoxListSelectionModel selectionModel = variableList.getCheckBoxListSelectionModel();
        selectionModel.addListSelectionListener(checkBoxSelectionListener);
    }


    private int[] getSelectedIndices(VariableSelectionPaneModel variableModel) {
        final List<Integer> indexList = new ArrayList<Integer>();
        for (int i = 0; i < variableModel.getSize(); i++) {
            if (variableModel.getElementAt(i).isSelected()) {
                indexList.add(i);
            }
        }
        final int[] indices = new int[indexList.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indexList.get(i);
        }
        return indices;
    }

    private class CheckBoxSelectionListener implements ListSelectionListener {

        private final CheckBoxList variableList;

        private CheckBoxSelectionListener(CheckBoxList variableList) {
            this.variableList = variableList;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                final CheckBoxListSelectionModel selectionModel = variableList.getCheckBoxListSelectionModel();
                for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
                    model.setSelectedVariableAt(i, selectionModel.isSelectedIndex(i));
                }
            }
        }
    }
}
