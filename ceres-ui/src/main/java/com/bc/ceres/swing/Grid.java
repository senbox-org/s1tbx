package com.bc.ceres.swing;

import com.bc.ceres.core.Assert;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Grid extends JPanel implements GridSelectionModel.Listener {

    private final List<List<JComponent>> componentRows;
    private final JPanel filler;
    private GridSelectionModel selectionModel;
    private boolean showSelectionColumn;

    public Grid(int columnCount, boolean showSelectionColumn) {
        this(new TableLayout(columnCount), showSelectionColumn);
    }

    public Grid(TableLayout tableLayout, boolean showSelectionColumn) {
        super(tableLayout);
        this.showSelectionColumn = showSelectionColumn;
        this.componentRows = new ArrayList<>();
        this.componentRows.add(new ArrayList<>(Arrays.asList(new JComponent[tableLayout.getColumnCount()])));
        this.selectionModel = new DefaultGridSelectionModel();
        this.selectionModel.addListener(this);
        filler = new JPanel();
        addFiller();
    }

    @Override
    public TableLayout getLayout() {
        return (TableLayout) super.getLayout();
    }

    @Override
    public void setLayout(LayoutManager mgr) {
        TableLayout oldLayout = getLayout();
        if (oldLayout == mgr) {
            return;
        }
        if (!(mgr instanceof TableLayout)) {
            throw new IllegalArgumentException();
        }
        TableLayout tableLayout = (TableLayout) mgr;
        if (oldLayout != null) {
            if (oldLayout.getColumnCount() != tableLayout.getColumnCount() && getRowCount() > 0) {
                throw new IllegalArgumentException();
            }
        }
        super.setLayout(tableLayout);
    }

    public GridSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(GridSelectionModel selectionModel) {
        GridSelectionModel oldSelectionModel = this.selectionModel;
        if (oldSelectionModel != selectionModel) {
            if (oldSelectionModel != null) {
                oldSelectionModel.removeListener(this);
            }
            this.selectionModel = selectionModel;
            if (this.selectionModel != null) {
                this.selectionModel.addListener(this);
            }
            firePropertyChange("selectionModel", oldSelectionModel, this.selectionModel);
        }
    }

    public boolean getShowSelectionColumn() {
        return showSelectionColumn;
    }

    public void setShowSelectionColumn(boolean showSelectionColumn) {
        boolean oldShowSelectionColumn = this.showSelectionColumn;
        if (oldShowSelectionColumn != showSelectionColumn) {
            this.showSelectionColumn = showSelectionColumn;
            for (List<JComponent> componentRow : componentRows) {
                JComponent component = componentRow.get(0);
                if (component != null) {
                    component.setVisible(this.showSelectionColumn);
                }
            }
            fireComponentsChanged();
            firePropertyChange("showSelectionColumn", oldShowSelectionColumn, showSelectionColumn);
        }
    }

    @Override
    public void gridSelectionChanged(GridSelectionModel.Event event) {
        adjustHeaderRowSelector();
        adjustDataRowSelectors();
    }


    public int getColumnCount() {
        return getLayout().getColumnCount();
    }

    public int getRowCount() {
        return componentRows.size();
    }

    public int getDataRowCount() {
        return componentRows.size() - 1;
    }

    public JComponent getComponent(int rowIndex, int colIndex) {
        return componentRows.get(rowIndex).get(colIndex);
    }

    public JComponent setComponent(int rowIndex, int colIndex, JComponent component) {
        List<JComponent> componentRow = componentRows.get(rowIndex);
        JComponent oldComponent = componentRow.get(colIndex);
        if (oldComponent != null) {
            remove(oldComponent);
        }
        if (component != null) {
            add(component, TableLayout.cell(rowIndex, colIndex));
        }
        componentRow.set(colIndex, component);
        fireComponentsChanged();
        return oldComponent;
    }

    public void setHeaderRow(JComponent... components) {
        checkColumnCount(components);
        List<JComponent> headerRow = new ArrayList<>(components.length + 1);
            JCheckBox headerRowSelector = createHeaderRowSelector();
            if (headerRowSelector != null) {
                headerRowSelector.setVisible(showSelectionColumn);
                headerRowSelector.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onHeaderRowSelectorAction();
                    }
                });
            }
            headerRow.add(headerRowSelector);
        Collections.addAll(headerRow, components);
        addComponentRowIntern(headerRow, 0);
        componentRows.set(0, headerRow);
        fireComponentsChanged();
        adjustHeaderRowSelector();
    }

    public void addDataRow(JComponent... components) {
        checkColumnCount(components);

        removeFiller();

        List<JComponent> dataRow = new ArrayList<>(components.length + 1);
            JCheckBox dataRowSelector = createDataRowSelector();
            if (dataRowSelector != null) {
                dataRowSelector.setVisible(showSelectionColumn);
                dataRowSelector.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onDataRowSelectorAction();
                    }
                });
            }
            dataRow.add(dataRowSelector);
        Collections.addAll(dataRow, components);

        addComponentRowIntern(dataRow, componentRows.size());
        componentRows.add(dataRow);
        addFiller();
        fireComponentsChanged();
        adjustHeaderRowSelector();
    }

    public void removeDataRow(int rowIndex) {
        Assert.argument(rowIndex >= 1, "rowIndex");
        removeFiller();
        boolean rowSelected = isRowSelected(rowIndex);
        List<JComponent> componentRow = componentRows.get(rowIndex);
        removeComponentRowIntern(componentRow);
        componentRows.remove(rowIndex);
        // Re-add remaining components, so that they are inserted at correct row positions
        for (int i = rowIndex; i < componentRows.size(); i++) {
            addComponentRowIntern(componentRows.get(i), i);
        }
        addFiller();
        fireComponentsChanged();
        if (rowSelected) {
            fireSelectionChange();
        }
    }

    public void removeDataRows(int... rowIndexes) {
        if (rowIndexes.length == 0) {
            return;
        }

        removeFiller();

        int offset = 0;
        int selectedCount = 0;
        for (int i : rowIndexes) {
            Assert.argument(i >= 1, "rowIndexes");
            int rowIndex = i - offset;
            Assert.state(rowIndex >= 1, "rowIndex");
            selectedCount += isRowSelected(rowIndex) ? 1 : 0;
            List<JComponent> componentRow = componentRows.get(rowIndex);
            removeComponentRowIntern(componentRow);
            componentRows.remove(rowIndex);
            offset++;
        }

        int rowIndex = rowIndexes[0];
        // Re-add remaining components, so that they are inserted at correct row positions
        for (int i = rowIndex; i < componentRows.size(); i++) {
            addComponentRowIntern(componentRows.get(i), i);
        }

        addFiller();
        fireComponentsChanged();
        if (selectedCount > 0) {
            for (int index : rowIndexes) {
                selectionModel.removeSelectedRowIndex(index);
            }
        }
    }

    public void moveDataRowUp(int rowIndex) {
        Assert.argument(rowIndex >= 2, "rowIndex");
        boolean selected = selectionModel.isRowSelected(rowIndex);
        List<JComponent> componentRow = componentRows.remove(rowIndex);
        componentRows.add(rowIndex - 1, componentRow);
        for (int i = rowIndex - 1; i < componentRows.size(); i++) {
            List<JComponent> componentRowToUpdate = componentRows.get(i);
            removeComponentRowIntern(componentRowToUpdate);
            addComponentRowIntern(componentRowToUpdate, i);
        }
        fireComponentsChanged();
        if (selected) {
            selectionModel.removeSelectedRowIndex(rowIndex);
            selectionModel.addSelectedRowIndex(rowIndex - 1);
        }

    }

    public void moveDataRowDown(int rowIndex) {
        Assert.argument(rowIndex < componentRows.size() - 1, "rowIndex");
        boolean selected = selectionModel.isRowSelected(rowIndex);
        List<JComponent> componentRow = componentRows.remove(rowIndex);
        componentRows.add(rowIndex + 1, componentRow);
        for (int i = rowIndex; i < componentRows.size(); i++) {
            List<JComponent> componentRowToUpdate = componentRows.get(i);
            removeComponentRowIntern(componentRowToUpdate);
            addComponentRowIntern(componentRowToUpdate, i);
        }
        fireComponentsChanged();
        if (selected) {
            selectionModel.removeSelectedRowIndex(rowIndex);
            selectionModel.addSelectedRowIndex(rowIndex + 1);
        }
    }

    public boolean isRowSelected(int rowIndex) {
        return selectionModel.isRowSelected(rowIndex);
    }

    public int getSelectedDataRowCount() {
        return selectionModel.getSelectedRowCount();
    }

    public int getSelectedDataRowIndex() {
        return selectionModel.getMinSelectedRowIndex();
    }

    public int[] getSelectedDataRowIndexes() {
        return selectionModel.getSelectedRowIndices();
    }

    public void setSelectedDataRowIndexes(int ... selectedRowIndexes) {
        selectionModel.setSelectedRowIndices(selectedRowIndexes);
    }

    protected void fireSelectionChange() {
        adjustHeaderRowSelector();
        selectionModel.fireChange(new GridSelectionModel.Event(selectionModel));
    }

    protected void adjustHeaderRowSelector(JCheckBox headerRowSelector, int selectedDataRowCount) {
        headerRowSelector.setSelected(getDataRowCount() > 0 && selectedDataRowCount == getDataRowCount());
        headerRowSelector.setEnabled(getDataRowCount() > 0);
    }

    protected Border createHeaderCellBorder() {
        return new HeaderBorder();
    }

    protected JCheckBox createHeaderRowSelector() {
        return new JCheckBox();
    }

    protected JCheckBox createDataRowSelector() {
        return new JCheckBox();
    }

    private void onHeaderRowSelectorAction() {
        JCheckBox checkBox = (JCheckBox) componentRows.get(0).get(0);
        setAllDataRowsSelected(checkBox.isSelected());
    }

    private void onDataRowSelectorAction() {
        adjustSelectionModel();
    }

    private void setAllDataRowsSelected(boolean selected) {
        if (selected) {
            int[] rowIndices = new int[componentRows.size() - 1];
            for (int i = 0; i < rowIndices.length; i++) {
                rowIndices[i] = i + 1;
            }
            selectionModel.setSelectedRowIndices(rowIndices);
        }  else {
            selectionModel.setSelectedRowIndices();
        }
    }

    private void adjustHeaderRowSelector() {
        JComponent component = componentRows.get(0).get(0);
        if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) component;
            adjustHeaderRowSelector(checkBox, getSelectedDataRowCount());
        }
    }


    private void adjustDataRowSelectors() {
        for (int rowIndex = 1; rowIndex < componentRows.size(); rowIndex++) {
            Component component = componentRows.get(rowIndex).get(0);
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                checkBox.setSelected(selectionModel.isRowSelected(rowIndex));
            }
        }
    }

    private void adjustSelectionModel() {
        ArrayList<Integer> integers = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < componentRows.size(); rowIndex++) {
            Component component = componentRows.get(rowIndex).get(0);
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.isSelected()) {
                    integers.add(rowIndex);
                }
            }
        }
        int[] rowIndices = new int[integers.size()];
        for (int i = 0; i < rowIndices.length; i++) {
           rowIndices[i] = integers.get(i);
        }
        selectionModel.setSelectedRowIndices(rowIndices);
    }

    private void addComponentRowIntern(List<JComponent> componentRow, int rowIndex) {
        for (int colIndex = 0; colIndex < componentRow.size(); colIndex++) {
            JComponent component = componentRow.get(colIndex);
            if (component != null) {
                remove(component);
                if (rowIndex == 0) {
                    addHeaderBorder(component);
                }
                add(component, TableLayout.cell(rowIndex, colIndex));
                //System.out.println("added at (" + rowIndex + "," + colIndex + "): " + component.getClass().getSimpleName());
            }
        }
    }

    private void removeComponentRowIntern(List<JComponent> componentRow) {
        for (JComponent component : componentRow) {
            if (component != null) {
                remove(component);
            }
        }
    }

    private void fireComponentsChanged() {
        invalidate();
        revalidate();
        validate();
        repaint();
    }

    private void checkColumnCount(JComponent[] components) {
        if (components.length != getColumnCount() - 1) {
            throw new IllegalArgumentException("components");
        }
    }

    private void addHeaderBorder(JComponent component) {
        Border oldBorder = component.getBorder();
        Border newBorder = createHeaderCellBorder();
        if (oldBorder != null) {
            newBorder = BorderFactory.createCompoundBorder(newBorder, oldBorder);
        }
        component.setBorder(newBorder);
    }

    private int lastFillerRow = -1;

    public void addFiller() {
        lastFillerRow = getRowCount();
        getLayout().setCellWeightY(lastFillerRow, 0, 1.0);
        getLayout().setCellFill(lastFillerRow, 0, TableLayout.Fill.VERTICAL);
        add(filler, TableLayout.cell(lastFillerRow, 0));
    }

    public void removeFiller() {
        if (lastFillerRow >= 0) {
            getLayout().setCellWeightY(lastFillerRow, 0, null);
            getLayout().setCellFill(lastFillerRow, 0, null);
        }
        remove(filler);
    }

    private static class HeaderBorder implements Border {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(c.getForeground());
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 1, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class DefaultGridSelectionModel implements GridSelectionModel {

        private final SortedSet<Integer> rowIndices;

        private final List<Listener> listeners;

        public DefaultGridSelectionModel() {
            rowIndices = new TreeSet<>();
            listeners = new ArrayList<>();
        }

        @Override
        public int getSelectedRowCount() {
            return rowIndices.size();
        }

        @Override
        public boolean isRowSelected(int rowIndex) {
            return rowIndices.contains(rowIndex);
        }

        @Override
        public int getMinSelectedRowIndex() {
            return rowIndices.isEmpty() ? -1 : rowIndices.first();
        }

        @Override
        public int getMaxSelectedRowIndex() {
            return rowIndices.isEmpty() ? -1 : rowIndices.last();
        }

        @Override
        public int[] getSelectedRowIndices() {
            Integer[] integers = rowIndices.toArray(new Integer[rowIndices.size()]);
            int[] indices = new int[integers.length];
            for (int i = 0; i < integers.length; i++) {
                indices[i] = integers[i];
            }
            return indices;
        }

        @Override
        public void setSelectedRowIndices(int[] rowIndices) {
            Set<Integer> newRowIndices = new TreeSet<>();
            for (int rowIndex : rowIndices) {
                newRowIndices.add(rowIndex);
            }
            if (!newRowIndices.equals(this.rowIndices)) {
                this.rowIndices.clear();
                this.rowIndices.addAll(newRowIndices);
                fireChange(new Event(this));
            }
        }

        @Override
        public void addSelectedRowIndex(int rowIndex) {
            if (!rowIndices.contains(rowIndex)) {
                rowIndices.add(rowIndex);
                fireChange(new Event(this));
            }
        }

        @Override
        public void removeSelectedRowIndex(int rowIndex) {
            if (rowIndices.contains(rowIndex)) {
                rowIndices.remove(rowIndex);
                fireChange(new Event(this));
            }
        }

        @Override
        public void addListener(Listener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(Listener listener) {
            listeners.remove(listener);
        }

        @Override
        public void fireChange(Event event) {
            for (GridSelectionModel.Listener listener : listeners) {
                listener.gridSelectionChanged(event);
            }
        }
    }
}
