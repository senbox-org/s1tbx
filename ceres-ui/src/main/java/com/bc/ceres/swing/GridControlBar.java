package com.bc.ceres.swing;

import com.bc.ceres.core.Assert;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import java.awt.event.ActionEvent;
import java.util.List;

public class GridControlBar extends JToolBar implements Grid.SelectionListener {
    private final AddAction addAction;
    private final RemoveAction removeAction;
    private final MoveUpAction moveUpAction;
    private final MoveDownAction moveDownAction;
    private Grid grid;
    private Controller controller;

    public GridControlBar(int orientation, Grid grid, Controller controller) {
        super(orientation);

        Assert.notNull(grid, "grid");
        Assert.notNull(controller, "controller");

        this.grid = grid;
        this.controller = controller;
        this.addAction = new AddAction();
        this.removeAction = new RemoveAction();
        this.moveUpAction = new MoveUpAction();
        this.moveDownAction = new MoveDownAction();

        setFloatable(false);
        add(addAction);
        add(removeAction);
        add(moveUpAction);
        add(moveDownAction);

        grid.addSelectionListener(this);

        updateState();
    }

    public Grid getGrid() {
        return grid;
    }

    @Override
    public void selectionStateChanged(Grid grid) {
        //System.out.println("selectionStateChanged: grid = " + grid);
        updateState();
    }

    public void addNewRow() {
        JComponent[] dataRow = controller.newDataRow(this);
        if (dataRow != null) {
            grid.addDataRow(dataRow);
        }
        updateState();
    }

    public void removeSelectedRows() {
        List<Integer> rowIndexes = grid.getSelectedDataRowIndexes();
        if (controller.removeDataRows(this, rowIndexes)) {
            grid.removeDataRows(rowIndexes);
            updateState();
        }
    }

    public void moveSelectedRowUp() {
        int rowIndex = grid.getSelectedDataRowIndex();
        if (controller.moveDataRowUp(this, rowIndex)) {
            grid.moveDataRowUp(rowIndex);
            updateState();
        }
    }

    public void moveSelectedRowDown() {
        int rowIndex = grid.getSelectedDataRowIndex();
        if (controller.moveDataRowDown(this, rowIndex)) {
            grid.moveDataRowDown(rowIndex);
            updateState();
        }
    }

    public void updateState() {
        addAction.setEnabled(true);
        int rowCount = grid.getSelectedDataRowCount();
        removeAction.setEnabled(rowCount > 0);
        moveUpAction.setEnabled(rowCount == 1 && grid.getSelectedDataRowIndex() >= 2);
        moveDownAction.setEnabled(rowCount == 1 && grid.getSelectedDataRowIndex() < grid.getRowCount() - 1);
    }

    private class AddAction extends AbstractAction {

        private AddAction() {
            putValue(Action.ACTION_COMMAND_KEY, "add");
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/com/bc/ceres/swing/update/icons/list-add.png")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addNewRow();
        }
    }

    private class RemoveAction extends AbstractAction {

        private RemoveAction() {
            putValue(Action.ACTION_COMMAND_KEY, "remove");
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/com/bc/ceres/swing/update/icons/list-remove.png")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            removeSelectedRows();
        }

    }

    private class MoveUpAction extends AbstractAction {

        private MoveUpAction() {
            putValue(Action.ACTION_COMMAND_KEY, "up");
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/com/bc/ceres/swing/progress/icons/PanelUp12.png")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            moveSelectedRowUp();
        }
    }

    private class MoveDownAction extends AbstractAction {

        private MoveDownAction() {
            putValue(Action.ACTION_COMMAND_KEY, "down");
            putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/com/bc/ceres/swing/progress/icons/PanelDown12.png")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            moveSelectedRowDown();
        }
    }

    public interface Controller {
        JComponent[] newDataRow(GridControlBar gridControlBar);

        boolean removeDataRows(GridControlBar gridControlBar, List<Integer> selectedIndexes);

        boolean moveDataRowUp(GridControlBar gridControlBar, int selectedIndex);

        boolean moveDataRowDown(GridControlBar gridControlBar, int selectedIndex);
    }
}
