package com.bc.ceres.swing;

import com.bc.ceres.core.Assert;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public class ListControlBar extends JToolBar {

    public static final String ID_ADD = "add";
    public static final String ID_REMOVE = "remove";
    public static final String ID_MOVE_UP = "moveUp";
    public static final String ID_MOVE_DOWN = "moveDown";
    //public static final String ID_EDIT = "edit";
    //public static final String ID_COPY = "copy";

    private final Map<String, Action> actionMap;

    private ListModelAdapter listModelAdapter;

    public ListControlBar(int orientation, String... actionIds) {
        super(orientation);

        setFloatable(false);

        if (actionIds == null || actionIds.length == 0) {
            actionIds = new String[]{ID_ADD, ID_REMOVE, ID_MOVE_UP, ID_MOVE_DOWN};
        }

        actionMap = new HashMap<>();
        for (String actionId : actionIds) {
            Action action;
            action = createAction(actionId);
            actionMap.put(action.getValue(Action.ACTION_COMMAND_KEY).toString(), action);
            add(action);
        }

        updateState();
    }

    public ListModelAdapter getListModelAdapter() {
        return listModelAdapter;
    }

    public void setListModelAdapter(ListModelAdapter listModelAdapter) {
        Assert.notNull(listModelAdapter, "listModelAdapter");
        ListModelAdapter oldListModelAdapter = this.listModelAdapter;
        this.listModelAdapter = listModelAdapter;
        firePropertyChange("listModelAdapter", oldListModelAdapter, this.listModelAdapter);
        updateState();
    }

    public static ListControlBar create(int orientation, JList list, ListController listController, String... actionIds) {
        ListControlBar listControlBar = new ListControlBar(orientation, actionIds);
        listControlBar.setListModelAdapter(new JListModelAdapter(listControlBar, list, listController));
        return listControlBar;
    }

    public static ListControlBar create(int orientation, JTable table, ListController listController, String... actionIds) {
        ListControlBar listControlBar = new ListControlBar(orientation, actionIds);
        listControlBar.setListModelAdapter(new JTableModelAdapter(listControlBar, table, listController));
        return listControlBar;
    }

    public static ListControlBar create(int orientation, Grid grid, ListController listController, String... actionIds) {
        ListControlBar listControlBar = new ListControlBar(orientation, actionIds);
        listControlBar.setListModelAdapter(new GridAdapter(listControlBar, grid, listController));
        return listControlBar;
    }

    public Action getAction(String actionId) {
        return actionMap.get(actionId);
    }

    public AbstractButton getActionComponent(String actionId) {
        Action action = getAction(actionId);
        return action != null ? getActionComponent(action) : null;
    }

    public AbstractButton getActionComponent(Action action) {
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof AbstractButton) {
                AbstractButton abstractButton = (AbstractButton) component;
                if (abstractButton.getAction() == action) {
                    return abstractButton;
                }
            }
        }
        return null;
    }

    protected Action createAction(String actionId) {
        switch (actionId) {
            case ID_ADD:
                return new AddAction();
            case ID_REMOVE:
                return new RemoveAction();
            case ID_MOVE_UP:
                return new MoveUpAction();
            case ID_MOVE_DOWN:
                return new MoveDownAction();
        }
        throw new IllegalArgumentException("actionId");
    }

    public void addRow() {
        checkValidState();
        if (listModelAdapter.addRow(listModelAdapter.getMaxSelectedRowIndex())) {
            updateState();
        }
    }

    public void removeSelectedRows() {
        checkValidState();
        if (listModelAdapter.removeRows(listModelAdapter.getSelectedRowIndices())) {
            updateState();
        }
    }

    public void moveSelectedRowUp() {
        checkValidState();
        if (listModelAdapter.moveRowUp(listModelAdapter.getMinSelectedRowIndex())) {
            updateState();
        }
    }

    public void moveSelectedRowDown() {
        checkValidState();
        if (listModelAdapter.moveRowDown(listModelAdapter.getMaxSelectedRowIndex())) {
            updateState();
        }
    }

    public void updateState() {
        Action addAction = getAction(ID_ADD);
        Action removeAction = getAction(ID_REMOVE);
        Action moveUpAction = getAction(ID_MOVE_UP);
        Action moveDownAction = getAction(ID_MOVE_DOWN);
        if (listModelAdapter != null) {
            int[] selectedRowIndices = listModelAdapter.getSelectedRowIndices();
            int selectedRowCount = selectedRowIndices.length;
            if (addAction != null) {
                addAction.setEnabled(true);
            }
            if (removeAction != null) {
                removeAction.setEnabled(selectedRowCount > 0);
            }
            if (moveUpAction != null) {
                int minSelectionIndex = listModelAdapter.getMinSelectedRowIndex();
                moveUpAction.setEnabled(selectedRowCount == 1 && minSelectionIndex >= 0 && minSelectionIndex > 0);
            }
            if (moveDownAction != null) {
                int maxSelectionIndex = listModelAdapter.getMaxSelectedRowIndex();
                moveDownAction.setEnabled(selectedRowCount == 1 && maxSelectionIndex >= 0 && maxSelectionIndex < listModelAdapter.getRowCount() - 1);
            }
            listModelAdapter.updateState(this);
        } else {
            if (addAction != null) {
                addAction.setEnabled(false);
            }
            if (removeAction != null) {
                removeAction.setEnabled(false);
            }
            if (moveUpAction != null) {
                moveUpAction.setEnabled(false);
            }
            if (moveDownAction != null) {
                moveDownAction.setEnabled(false);
            }
        }
    }

    private void checkValidState() {
        Assert.state(listModelAdapter != null, "listModelAdapter != null");
    }

    private class AddAction extends AbstractAction {

        private AddAction() {
            putValue(Action.ACTION_COMMAND_KEY, ID_ADD);
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("icons/Plus16.gif")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addRow();
        }
    }

    private class RemoveAction extends AbstractAction {

        private RemoveAction() {
            putValue(Action.ACTION_COMMAND_KEY, ID_REMOVE);
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("icons/Minus16.gif")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            removeSelectedRows();
        }

    }

    private class MoveUpAction extends AbstractAction {

        private MoveUpAction() {
            putValue(Action.ACTION_COMMAND_KEY, ID_MOVE_UP);
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("icons/MoveUp16.gif")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            moveSelectedRowUp();
        }
    }

    private class MoveDownAction extends AbstractAction {

        private MoveDownAction() {
            putValue(Action.ACTION_COMMAND_KEY, ID_MOVE_DOWN);
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("icons/MoveDown16.gif")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            moveSelectedRowDown();
        }
    }

    public interface ListController {
        boolean addRow(int index);

        boolean removeRows(int[] indices);

        boolean moveRowUp(int index);

        boolean moveRowDown(int index);

        void updateState(ListControlBar listControlBar);
    }

    public static abstract class AbstractListController implements ListController {

        @Override
        public void updateState(ListControlBar listControlBar) {
        }
    }

    public interface ListModelAdapter extends ListController {
        int getRowCount();

        int getMinSelectedRowIndex();

        int getMaxSelectedRowIndex();

        int[] getSelectedRowIndices();
    }

    private static abstract class AbstractListModelAdapter implements ListModelAdapter {
        private final ListController controller;

        protected AbstractListModelAdapter(ListController controller) {
            this.controller = controller;
        }

        @Override
        public boolean addRow(int index) {
            return controller.addRow(index);
        }

        @Override
        public boolean removeRows(int[] indices) {
            return controller.removeRows(indices);
        }

        @Override
        public boolean moveRowUp(int index) {
            return controller.moveRowUp(index);
        }

        @Override
        public boolean moveRowDown(int index) {
            return controller.moveRowDown(index);
        }

        @Override
        public void updateState(ListControlBar listControlBar) {
            controller.updateState(listControlBar);
        }
    }

    private static class JListModelAdapter extends AbstractListModelAdapter implements ListDataListener, ListSelectionListener {
        private final ListControlBar listControlBar;
        private final JList list;

        public JListModelAdapter(ListControlBar listControlBar, JList list, ListController listController) {
            super(listController);
            this.listControlBar = listControlBar;
            this.list = list;
            this.list.addPropertyChangeListener("model", new ModelChangeListener());
            this.list.addListSelectionListener(this);
        }

        @Override
        public int getRowCount() {
            return list.getModel().getSize();
        }

        @Override
        public int getMinSelectedRowIndex() {
            return list.getMinSelectionIndex();
        }

        @Override
        public int getMaxSelectedRowIndex() {
            return list.getMaxSelectionIndex();
        }

        @Override
        public int[] getSelectedRowIndices() {
            return list.getSelectedIndices();
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            listControlBar.updateState();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            listControlBar.updateState();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            listControlBar.updateState();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            listControlBar.updateState();
        }

        private void installModelListener(ListModel oldModel, ListModel newModel) {
            if (oldModel != null) {
                oldModel.removeListDataListener(this);
            }
            if (newModel != null) {
                newModel.addListDataListener(this);
            }
        }

        private class ModelChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ListModel oldModel = (ListModel) evt.getOldValue();
                ListModel newModel = (ListModel) evt.getNewValue();
                installModelListener(oldModel, newModel);
            }
        }
    }

    private static class JTableModelAdapter extends AbstractListModelAdapter implements TableModelListener, ListSelectionListener {
        private final ListControlBar listControlBar;
        private final JTable table;

        public JTableModelAdapter(ListControlBar listControlBar, JTable table, ListController listController) {
            super(listController);
            this.listControlBar = listControlBar;
            this.table = table;
            installModelListener(null, table.getModel());
            installSelectionModelListener(null, table.getSelectionModel());
            this.table.addPropertyChangeListener("model", new ModelChangeListener());
            this.table.addPropertyChangeListener("selectionModel", new SelectionModelChangeListener());
        }

        @Override
        public int getRowCount() {
            return table.getModel().getRowCount();
        }

        @Override
        public int getMinSelectedRowIndex() {
            return table.getSelectionModel().getMinSelectionIndex();
        }

        @Override
        public int getMaxSelectedRowIndex() {
            return table.getSelectionModel().getMaxSelectionIndex();
        }

        @Override
        public int[] getSelectedRowIndices() {
            return table.getSelectedRows();
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            listControlBar.updateState();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            listControlBar.updateState();
        }

        private void installModelListener(TableModel oldModel, TableModel newModel) {
            if (oldModel != null) {
                oldModel.removeTableModelListener(this);
            }
            if (newModel != null) {
                newModel.addTableModelListener(this);
            }
        }

        private void installSelectionModelListener(ListSelectionModel oldModel, ListSelectionModel newModel) {
            if (oldModel != null) {
                oldModel.removeListSelectionListener(this);
            }
            if (newModel != null) {
                newModel.addListSelectionListener(this);
            }
        }

        private class ModelChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                TableModel oldModel = (TableModel) evt.getOldValue();
                TableModel newModel = (TableModel) evt.getNewValue();
                installModelListener(oldModel, newModel);
            }
        }

        private class SelectionModelChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ListSelectionModel oldModel = (ListSelectionModel) evt.getOldValue();
                ListSelectionModel newModel = (ListSelectionModel) evt.getNewValue();
                installSelectionModelListener(oldModel, newModel);
            }
        }
    }

    private static class GridAdapter extends AbstractListModelAdapter implements GridSelectionModel.Listener {
        private final ListControlBar listControlBar;
        private final Grid grid;

        public GridAdapter(ListControlBar listControlBar, Grid grid, ListController listController) {
            super(listController);
            this.listControlBar = listControlBar;
            this.grid = grid;

            installSelectionModelListener(null, grid.getSelectionModel());
            this.grid.addPropertyChangeListener("selectionModel", new SelectionModelChangeListener());

            updateState(listControlBar);
            this.grid.addPropertyChangeListener("showSelectionColumn", new ShowSelectionColumnChangeListener());
        }

        @Override
        public int getRowCount() {
            return grid.getDataRowCount();
        }

        @Override
        public int getMinSelectedRowIndex() {
            return grid.getSelectionModel().getMinSelectedRowIndex();
        }

        @Override
        public int getMaxSelectedRowIndex() {
            return grid.getSelectionModel().getMinSelectedRowIndex();
        }

        @Override
        public int[] getSelectedRowIndices() {
            return grid.getSelectionModel().getSelectedRowIndices();
        }

        @Override
        public void gridSelectionChanged(GridSelectionModel.Event event) {
            listControlBar.updateState();
        }

        @Override
        public void updateState(ListControlBar listControlBar) {
            super.updateState(listControlBar);
            hideActionIfSelectionRowInvisible(ListControlBar.ID_REMOVE);
            hideActionIfSelectionRowInvisible(ListControlBar.ID_MOVE_UP);
            hideActionIfSelectionRowInvisible(ListControlBar.ID_MOVE_DOWN);
        }

        private void hideActionIfSelectionRowInvisible(String actionId) {
            Action action = listControlBar.getAction(actionId);
            AbstractButton actionComponent = listControlBar.getActionComponent(action);
            actionComponent.setVisible(grid.getShowSelectionColumn());
        }

        private void installSelectionModelListener(GridSelectionModel oldModel, GridSelectionModel newModel) {
            if (oldModel != null) {
                oldModel.removeListener(this);
            }
            if (newModel != null) {
                newModel.addListener(this);
            }
        }

        private class SelectionModelChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                GridSelectionModel oldModel = (GridSelectionModel) evt.getOldValue();
                GridSelectionModel newModel = (GridSelectionModel) evt.getNewValue();
                installSelectionModelListener(oldModel, newModel);
            }
        }

        private class ShowSelectionColumnChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateState(listControlBar);
            }
        }
    }
}
