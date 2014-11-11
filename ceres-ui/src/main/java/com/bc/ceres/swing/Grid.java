package com.bc.ceres.swing;

import com.bc.ceres.core.Assert;

import javax.swing.AbstractButton;
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

/**
 * A panel that contains other components arranged in form of a table.
 * It's layout manager is a {@link com.bc.ceres.swing.TableLayout}.
 *
 * @author Norman
 * @since Ceres 0.14
 */
public class Grid extends JPanel implements GridSelectionModel.Listener {

    //private final List<RowSelector> rowSelectors;
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
        //this.rowSelectors = new ArrayList<>();
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

    public int getColumnCount() {
        return getLayout().getColumnCount();
    }

    public int getDataColumnCount() {
        return getColumnCount() - 1;
    }

    public int getRowCount() {
        return componentRows.size();
    }

    public int getDataRowCount() {
        return getRowCount() - 1;
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

    public int findRowIndex(JComponent component) {
        return findRowIndex(component, 0);
    }

    public int findDataRowIndex(JComponent component) {
        int rowIndex = findRowIndex(component, 1);
        return rowIndex >= 1 ? rowIndex - 1 : -1;
    }

    public void setHeaderRow(JComponent... components) {
        checkColumnCount(components);
        List<JComponent> headerRow = new ArrayList<>(components.length + 1);
        AbstractButton headerRowSelector = createHeaderRowSelector();
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

    public JComponent[] getDataRow(int dataRowIndex) {
        Assert.argument(dataRowIndex >= 0 && dataRowIndex < getDataRowCount(), "dataRowIndex");
        int rowIndex = dataRowIndex + 1;
        List<JComponent> componentRow = componentRows.get(rowIndex);
        JComponent[] dataRow = new JComponent[getDataColumnCount()];
        for (int i = 0; i < dataRow.length; i++) {
            dataRow[i] = componentRow.get(i + 1);
        }
        return dataRow;
    }


    public void addDataRow(JComponent... components) {
        checkColumnCount(components);

        removeFiller();

        List<JComponent> dataRow = new ArrayList<>(components.length + 1);
        AbstractButton dataRowSelector = createDataRowSelector();
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

    public void removeDataRow(int dataRowIndex) {
        Assert.argument(dataRowIndex >= 0, "dataRowIndex");
        int rowIndex = dataRowIndex + 1;
        removeFiller();
        boolean rowSelected = isDataRowSelected(rowIndex);
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
            selectionModel.removeSelectedRowIndex(dataRowIndex);
        }
    }

    public void removeDataRows(int... dataRowIndexes) {
        if (dataRowIndexes.length == 0) {
            return;
        }

        removeFiller();

        int offset = 0;
        int selectedCount = 0;
        for (int dataRowIndex : dataRowIndexes) {
            Assert.argument(dataRowIndex >= 0, "rowIndexes");
            int rowIndex = dataRowIndex + 1 - offset;
            Assert.state(rowIndex >= 1, "rowIndex");
            selectedCount += isDataRowSelected(dataRowIndex) ? 1 : 0;
            List<JComponent> componentRow = componentRows.get(rowIndex);
            removeComponentRowIntern(componentRow);
            componentRows.remove(rowIndex);
            offset++;
        }

        int rowIndex0 = dataRowIndexes[0] + 1;
        // Re-add remaining components, so that they are inserted at correct row positions
        for (int rowIndex = rowIndex0; rowIndex < componentRows.size(); rowIndex++) {
            addComponentRowIntern(componentRows.get(rowIndex), rowIndex);
        }

        addFiller();
        fireComponentsChanged();
        if (selectedCount > 0) {
            for (int dataRowIndex : dataRowIndexes) {
                selectionModel.removeSelectedRowIndex(dataRowIndex);
            }
        }
    }

    public void moveDataRowUp(int dataRowIndex) {
        Assert.argument(dataRowIndex >= 1, "dataRowIndex");
        int rowIndex = dataRowIndex + 1;
        boolean selected = selectionModel.isRowSelected(dataRowIndex);
        List<JComponent> componentRow = componentRows.remove(rowIndex);
        componentRows.add(rowIndex - 1, componentRow);
        for (int i = rowIndex - 1; i < componentRows.size(); i++) {
            List<JComponent> componentRowToUpdate = componentRows.get(i);
            removeComponentRowIntern(componentRowToUpdate);
            addComponentRowIntern(componentRowToUpdate, i);
        }
        fireComponentsChanged();
        if (selected) {
            selectionModel.removeSelectedRowIndex(dataRowIndex);
            selectionModel.addSelectedRowIndex(dataRowIndex - 1);
        }
    }

    public void moveDataRowDown(int dataRowIndex) {
        Assert.argument(dataRowIndex < getDataRowCount() - 1, "dataRowIndex");
        int rowIndex = dataRowIndex + 1;
        boolean selected = selectionModel.isRowSelected(dataRowIndex);
        List<JComponent> componentRow = componentRows.remove(rowIndex);
        componentRows.add(rowIndex + 1, componentRow);
        for (int i = rowIndex; i < componentRows.size(); i++) {
            List<JComponent> componentRowToUpdate = componentRows.get(i);
            removeComponentRowIntern(componentRowToUpdate);
            addComponentRowIntern(componentRowToUpdate, i);
        }
        fireComponentsChanged();
        if (selected) {
            selectionModel.removeSelectedRowIndex(dataRowIndex);
            selectionModel.addSelectedRowIndex(dataRowIndex + 1);
        }
    }

    public boolean isDataRowSelected(int dataRowIndex) {
        return selectionModel.isRowSelected(dataRowIndex);
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

    public void setSelectedDataRowIndexes(int... dataRowIndexes) {
        selectionModel.setSelectedRowIndices(dataRowIndexes);
    }

    @Override
    public void gridSelectionChanged(GridSelectionModel.Event event) {
        int[] selectedRowIndices = selectionModel.getSelectedRowIndices();
        System.out.println("selectedRowIndices = " + Arrays.toString(selectedRowIndices));
        adjustHeaderRowSelector();
        adjustDataRowSelectors();
    }

    protected void adjustHeaderRowSelector(AbstractButton headerRowSelector, int selectedDataRowCount) {
        int dataRowCount = getDataRowCount();
        headerRowSelector.setSelected(dataRowCount > 0 && selectedDataRowCount == dataRowCount);
        headerRowSelector.setEnabled(dataRowCount > 0);
    }

    protected Border createHeaderCellBorder() {
        return new HeaderBorder();
    }

    protected AbstractButton createHeaderRowSelector() {
        return new JCheckBox();
    }

    protected AbstractButton createDataRowSelector() {
        return new JCheckBox();
    }

    private void onHeaderRowSelectorAction() {
        AbstractButton button = (AbstractButton) componentRows.get(0).get(0);
        setAllDataRowsSelected(button.isSelected());
    }

    private void onDataRowSelectorAction() {
        adjustSelectionModel();
    }

    private void setAllDataRowsSelected(boolean selected) {
        if (selected) {
            int dataRowCount = getDataRowCount();
            int[] dataRowIndices = new int[dataRowCount];
            for (int i = 0; i < dataRowIndices.length; i++) {
                dataRowIndices[i] = i;
            }
            selectionModel.setSelectedRowIndices(dataRowIndices);
        } else {
            selectionModel.setSelectedRowIndices();
        }
    }

    private void adjustHeaderRowSelector() {
        JComponent component = componentRows.get(0).get(0);
        if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            adjustHeaderRowSelector(button, getSelectedDataRowCount());
        }
    }


    private void adjustDataRowSelectors() {
        for (int rowIndex = 1; rowIndex < componentRows.size(); rowIndex++) {
            Component component = componentRows.get(rowIndex).get(0);
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                int dataRowIndex = rowIndex - 1;
                button.setSelected(isDataRowSelected(dataRowIndex));
            }
        }
    }

    private void adjustSelectionModel() {
        ArrayList<Integer> dataRowIndexList = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < componentRows.size(); rowIndex++) {
            Component component = componentRows.get(rowIndex).get(0);
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                if (button.isSelected()) {
                    dataRowIndexList.add(rowIndex - 1);
                }
            }
        }
        int[] dataRowIndices = new int[dataRowIndexList.size()];
        for (int i = 0; i < dataRowIndices.length; i++) {
            dataRowIndices[i] = dataRowIndexList.get(i);
        }
        selectionModel.setSelectedRowIndices(dataRowIndices);
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

    private void addFiller() {
        lastFillerRow = getRowCount();
        getLayout().setCellWeightY(lastFillerRow, 0, 1.0);
        getLayout().setCellFill(lastFillerRow, 0, TableLayout.Fill.VERTICAL);
        add(filler, TableLayout.cell(lastFillerRow, 0));
    }

    private void removeFiller() {
        if (lastFillerRow >= 0) {
            getLayout().setCellWeightY(lastFillerRow, 0, null);
            getLayout().setCellFill(lastFillerRow, 0, null);
        }
        remove(filler);
    }

    private int findRowIndex(JComponent component, int rowOffset) {
        for (int rowIndex = rowOffset; rowIndex < componentRows.size(); rowIndex++) {
            List<JComponent> componentRow = componentRows.get(rowIndex);
            for (JComponent jComponent : componentRow) {
                if (jComponent != null && jComponent == component) {
                    return rowIndex;
                }
            }
        }
        return -1;
    }

    private static class HeaderBorder implements Border {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(c.getForeground().brighter());
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

    /*
    public interface RowSelector {
        boolean isSelected();

        void setSelected(boolean selected);

        JComponent getEditorComponent();
    }

    public static class DefaultRowSelector implements RowSelector {
        private AbstractButton button;

        public DefaultRowSelector() {
            this(new JCheckBox());
        }

        public DefaultRowSelector(AbstractButton button) {
            this.button = button;
        }

        @Override
        public boolean isSelected() {
            return button.isSelected();
        }

        @Override
        public void setSelected(boolean selected) {
            button.setSelected(selected);
        }

        @Override
        public AbstractButton getEditorComponent() {
            return button;
        }
    }
    */

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
        public void setSelectedRowIndices(int... rowIndices) {
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
