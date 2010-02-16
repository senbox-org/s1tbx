package com.bc.ceres.swing.selection;

public class TracingSelectionChangeListener implements SelectionChangeListener {
    public String trace = "";

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        trace += "sc(" + event.getSelection().getPresentationName() + ");";
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
        trace += "scc;";
    }
}
