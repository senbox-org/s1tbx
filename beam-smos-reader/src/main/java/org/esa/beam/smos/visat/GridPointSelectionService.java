package org.esa.beam.smos.visat;

import java.util.ArrayList;
import java.util.List;

public class GridPointSelectionService {
    private final List<SelectionListener> selectionListeners;
    private int selectedPointId;

    public GridPointSelectionService() {
        this.selectionListeners = new ArrayList<SelectionListener>();
        this.selectedPointId = -1;
    }

    public synchronized void stop() {
        selectionListeners.clear();
        selectedPointId = -1;
    }

    public synchronized int getSelectedGridPointId() {
        return selectedPointId;
    }

    public synchronized void setSelectedGridPointId(int id) {
        int oldId = this.selectedPointId;
        if (oldId != id) {
            this.selectedPointId = id;
            fireSelectionChange(oldId, id);
        }
    }

    public synchronized void addGridPointSelectionListener(SelectionListener selectionListener) {
        selectionListeners.add(selectionListener);
    }

    public synchronized void removeGridPointSelectionListener(SelectionListener selectionListener) {
        selectionListeners.remove(selectionListener);
    }

    private void fireSelectionChange(int oldId, int newId) {
        for (SelectionListener selectionListener : selectionListeners) {
            selectionListener.handleGridPointSelectionChanged(oldId, newId);
        }
    }

    public interface SelectionListener {
        void handleGridPointSelectionChanged(int oldId, int newId);
    }
}