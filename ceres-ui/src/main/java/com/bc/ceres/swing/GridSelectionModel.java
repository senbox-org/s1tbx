package com.bc.ceres.swing;

import java.util.EventObject;

public interface GridSelectionModel  {

    int getSelectedRowCount();

    boolean isRowSelected(int rowIndex);

    int getMinSelectedRowIndex();

    int getMaxSelectedRowIndex();

    int[] getSelectedRowIndices();

    void setSelectedRowIndices(int... rowIndices);

    void addSelectedRowIndex(int rowIndex);

    void removeSelectedRowIndex(int rowIndex);

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void fireChange(Event event);

    public interface Listener {
        void gridSelectionChanged(Event event);
    }

    public class Event extends EventObject {
        public Event(GridSelectionModel source) {
            super(source);
        }

        @Override
        public GridSelectionModel getSource() {
            return (GridSelectionModel) super.getSource();
        }
    }
}
