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

package org.esa.beam.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * UI component for names association.
 *
 * @author Sabine Embacher
 * @author Thomas Storm
 */
public class NamesAssociationDialog extends ModalDialog {

    private final AssociationModel associationModel;
    private final NameProvider nameProvider;

    private JTable aliasNames;
    private JScrollPane aliasNameScrollPane;
    private JList centerNames;
    private JList rightNames;
    private AbstractButton removeButton;
    private boolean shown = false;

    public static void show(AssociationModel associationModel, NameProvider nameProvider, String helpId) {
        new NamesAssociationDialog(associationModel, nameProvider, helpId).show();
    }

    private NamesAssociationDialog(AssociationModel associationModel, NameProvider nameProvider, String helpId) {
        super(VisatApp.getApp().getMainFrame(), nameProvider.windowTitle, ModalDialog.ID_OK, helpId);
        this.associationModel = associationModel;
        this.nameProvider = nameProvider;
        init();
    }

    @Override
    public int show() {
        setButtonID(0);
        final JDialog dialog = getJDialog();
        if (!shown) {
            dialog.pack();
            center();
        }
        dialog.setMinimumSize(dialog.getSize());
        dialog.setVisible(true);
        shown = true;
        return getButtonID();
    }

    public static interface AssociationModel {

        List<String> getRightListNames(String alias);

        List<String> getCenterListNames(String alias);

        void addFromCenterList(String alias, String name);

        void addFromRightList(String alias, String name);

        void removeAlias(String alias);

        void addAlias(String alias);

        void removeFromRightList(String alias, String name);

        void removeFromCenterList(String alias, String name);

        Set<String> getAliasNames();

        void replaceAlias(String beforeName, String changedName);
    }

    public static abstract class NameProvider {
        final String windowTitle;
        final String aliasHeaderName;
        final String centerHeaderName;
        final String rightHeaderName;

        public NameProvider(String windowTitle, String aliasHeaderName, String centerHeaderName, String rightHeaderName) {
            this.aliasHeaderName = aliasHeaderName;
            this.centerHeaderName = centerHeaderName;
            this.rightHeaderName = rightHeaderName;
            this.windowTitle = windowTitle;
        }

        public abstract String[] getCenterNames();

        public abstract String[] getRightNames();
    }

    private void init() {
        final TableLayout layout = createLayout();
        final JPanel mainPanel = new JPanel(layout);
        mainPanel.add(new JLabel(nameProvider.aliasHeaderName));
        mainPanel.add(new JLabel(""));
        mainPanel.add(new JLabel(nameProvider.centerHeaderName));
        mainPanel.add(new JLabel(nameProvider.rightHeaderName));
        mainPanel.add(createAliasList());
        mainPanel.add(createButtonsPanel());
        mainPanel.add(createCenterList());
        mainPanel.add(createRightList());
        setContent(mainPanel);
    }

    private JPanel createButtonsPanel() {
        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        final AbstractButton addButton = ToolButtonFactory.createButton(new AddAliasAction(), false);
        removeButton = ToolButtonFactory.createButton(new RemoveAliasAction(), false);
        removeButton.setEnabled(false);
        buttonsPanel.add(addButton, BorderLayout.NORTH);
        buttonsPanel.add(removeButton, BorderLayout.SOUTH);
        return buttonsPanel;
    }

    private TableLayout createLayout() {
        final TableLayout layout = new TableLayout(4);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(new Insets(4, 4, 4, 4));
        layout.setColumnFill(0, TableLayout.Fill.BOTH);
        layout.setColumnFill(1, TableLayout.Fill.VERTICAL);
        layout.setColumnFill(2, TableLayout.Fill.BOTH);
        layout.setColumnFill(3, TableLayout.Fill.BOTH);
        layout.setRowFill(0, TableLayout.Fill.NONE);
        layout.setRowAnchor(0, TableLayout.Anchor.SOUTHWEST);
        layout.setRowWeightY(1, 100);
        layout.setColumnWeightX(0, 100);
        layout.setColumnWeightX(2, 100);
        layout.setColumnWeightX(3, 100);
        layout.setCellFill(0, 0, TableLayout.Fill.NONE);
        layout.setCellFill(0, 1, TableLayout.Fill.NONE);
        layout.setCellFill(0, 2, TableLayout.Fill.NONE);
        layout.setCellFill(0, 3, TableLayout.Fill.NONE);
        return layout;
    }

    private JComponent createAliasList() {
        aliasNames = new JTable();
        final AbstractTableModel tableModel = new AbstractTableModel() {

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }

            @Override
            public int getRowCount() {
                return associationModel.getAliasNames().size();
            }

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                final String beforeName = getAliasNameAt(rowIndex);
                final String changedName = aValue.toString();
                associationModel.replaceAlias(beforeName, changedName);
                aliasNameScrollPane.repaint();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return getAliasNameAt(rowIndex);
            }
        };
        aliasNames.setModel(tableModel);
        aliasNames.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        aliasNames.setColumnSelectionAllowed(true);
        aliasNames.setRowSelectionAllowed(true);
        aliasNames.setTableHeader(null);
        aliasNames.getSelectionModel().addListSelectionListener(new AliasNamesSelectionListener());
        aliasNameScrollPane = new JScrollPane(aliasNames);
        aliasNameScrollPane.setPreferredSize(new Dimension(160, 200));
        aliasNameScrollPane.setMinimumSize(new Dimension(160, 200));
        return aliasNameScrollPane;
    }

    private String getAliasNameAt(int rowIndex) {
        final Set<String> names = associationModel.getAliasNames();
        return names.toArray(new String[names.size()])[rowIndex];
    }

    private JComponent createCenterList() {
        centerNames = new JList(new AbstractListModel() {
            @Override
            public int getSize() {
                return nameProvider.getCenterNames().length;
            }

            @Override
            public Object getElementAt(int index) {
                return nameProvider.getCenterNames()[index];
            }
        });
        centerNames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        centerNames.addListSelectionListener(new CenterListSelectionListener(centerNames));
        centerNames.setEnabled(false);
        final JScrollPane scrollPane = new JScrollPane(centerNames);
        scrollPane.setPreferredSize(new Dimension(160, 200));
        return scrollPane;
    }

    private JComponent createRightList() {
        rightNames = new JList(new AbstractListModel() {

            @Override
            public int getSize() {
                return nameProvider.getRightNames().length;
            }

            @Override
            public Object getElementAt(int index) {
                return nameProvider.getRightNames()[index];
            }
        });
        rightNames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rightNames.addListSelectionListener(new RightListSelectionListener(rightNames));
        rightNames.setEnabled(false);
        final JScrollPane scrollPane = new JScrollPane(rightNames);
        scrollPane.setPreferredSize(new Dimension(160, 200));
        return scrollPane;
    }

    private class AddAliasAction extends AbstractAction {

        private AddAliasAction() {
            super("Add alias", UIUtils.loadImageIcon("icons/Plus16.gif"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            associationModel.addAlias("...");
            removeButton.setEnabled(true);
            aliasNameScrollPane.repaint();
            int rowIndex = 0;
            for (String aliasName : associationModel.getAliasNames()) {
                if (aliasName.equals("...")) {
                    break;
                }
                rowIndex++;
            }
            DefaultCellEditor editor = (DefaultCellEditor) aliasNames.getCellEditor(rowIndex, 0);
            aliasNames.editCellAt(rowIndex, 0);
            final JTextField textField = (JTextField) editor.getComponent();
            textField.requestFocus();
            textField.selectAll();
        }

    }

    private class RemoveAliasAction extends AbstractAction {

        private RemoveAliasAction() {
            super("Remove alias", UIUtils.loadImageIcon("icons/Minus16.gif"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            final ListSelectionModel selectionModel = aliasNames.getSelectionModel();
            final int minSelectionIndex = selectionModel.getMinSelectionIndex();
            final int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
            selectionModel.clearSelection();
            if (minSelectionIndex != -1) {
                for (int i = maxSelectionIndex; i >= minSelectionIndex; i--) {
                    associationModel.removeAlias(getAliasNameAt(i));
                }
            }
            removeButton.setEnabled(associationModel.getAliasNames().size() > 0);
            aliasNameScrollPane.repaint();
        }

    }

    private abstract class VariableNamesSelectionListener implements ListSelectionListener {

        private final JList variableNames;

        private VariableNamesSelectionListener(JList variableNames) {
            this.variableNames = variableNames;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
        // todo - don't allow the the user to select center/right names which are already selected for another alias
            final ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
            for (int index : variableNames.getSelectedIndices()) {
                selectedIndices.add(index);
            }
            final int selectedAliasRow = aliasNames.getSelectedRow();
            if (selectedAliasRow != -1) {
                final String currentAlias = aliasNames.getModel().getValueAt(selectedAliasRow, 0).toString();
                for (int i = 0; i < variableNames.getModel().getSize(); i++) {
                    final String variableName = variableNames.getModel().getElementAt(i).toString();
                    if (selectedIndices.contains(i)) {
                        addVariableName(currentAlias, variableName);
                    } else {
                        removeVariableName(currentAlias, variableName);
                    }
                }
            }
        }

        abstract void addVariableName(String currentAlias, String name);
        abstract void removeVariableName(String currentAlias, String name);
    }

    private class CenterListSelectionListener extends VariableNamesSelectionListener {

        private CenterListSelectionListener(JList variableNames) {
            super(variableNames);
        }

        @Override
        void addVariableName(String currentAlias, String variableName) {
            associationModel.addFromCenterList(currentAlias, variableName);
        }

        @Override
        void removeVariableName(String currentAlias, String variableName) {
            associationModel.removeFromCenterList(currentAlias, variableName);
        }
    }

    private class RightListSelectionListener extends VariableNamesSelectionListener {

        private RightListSelectionListener(JList variableNames) {
            super(variableNames);
        }

        @Override
        void addVariableName(String currentAlias, String variableName) {
            associationModel.addFromRightList(currentAlias, variableName);
        }

        @Override
        void removeVariableName(String currentAlias, String variableName) {
            associationModel.removeFromRightList(currentAlias, variableName);
        }
    }

    private class AliasNamesSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            final boolean isSomeAliasSelected = aliasNames.getSelectionModel().getMinSelectionIndex() != -1;
            centerNames.setEnabled(isSomeAliasSelected);
            rightNames.setEnabled(isSomeAliasSelected);
            removeButton.setEnabled(isSomeAliasSelected);

            if(isSomeAliasSelected) {
                final int[] selectedCenterIndices = getSelectedCenterIndices();
                final int[] selectedRightIndices = getSelectedRightIndices();
                centerNames.setSelectedIndices(selectedCenterIndices);
                rightNames.setSelectedIndices(selectedRightIndices);
            } else {
                centerNames.clearSelection();
                rightNames.clearSelection();
            }
        }

        private int[] getSelectedCenterIndices() {
            final String currentAlias = getCurrentAlias();
            final List<String> selectedCenterNames = associationModel.getCenterListNames(currentAlias);
            return getSelectedIndices(selectedCenterNames, centerNames);
        }

        private int[] getSelectedRightIndices() {
            final String currentAlias = getCurrentAlias();
            final List<String> selectedRightNames = associationModel.getRightListNames(currentAlias);
            return getSelectedIndices(selectedRightNames, rightNames);
        }

        private String getCurrentAlias() {
            final int minSelectionIndex = aliasNames.getSelectionModel().getMinSelectionIndex();
            return aliasNames.getModel().getValueAt(minSelectionIndex, 0).toString();
        }

        private int[] getSelectedIndices(List<String> selectedVariableNames, JList variableNames) {
            final List<Integer> selectedIndices = new ArrayList<Integer>(selectedVariableNames.size());
            final ListModel variableNamesModel = variableNames.getModel();
            for(int i = 0; i < variableNamesModel.getSize(); i++) {
                final String name = variableNamesModel.getElementAt(i).toString();
                if (selectedVariableNames.contains(name)) {
                    selectedIndices.add(i);
                }
            }
            final int[] selectedIndicesArray = new int[selectedIndices.size()];
            int i = 0;
            for (Integer index : selectedIndices) {
                selectedIndicesArray[i] = index;
                i++;
            }
            return selectedIndicesArray;
        }
    }
}
