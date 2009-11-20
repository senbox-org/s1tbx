package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;

class TracingSelectionChangeListener implements SelectionChangeListener {
    String trace = "";

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        trace += "sc(" + event.getSelection().getPresentationName() + ");";
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
        trace += "scc;";
    }
}
