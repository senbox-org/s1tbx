package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.Selection;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A selection provider that wraps a {@link javax.swing.JList}.
 * Elements contained in {@link Selection}s handled by this provider
 * represent currently selected list objects.
 */
public class ListSelectionProvider extends AbstractSelectionProvider {

    private final ListSelectionListener listSelectionListener;
    private JList list;

    public ListSelectionProvider(final JList list) {
        listSelectionListener = new ListSelectionHandler();
        this.list = list;
        this.list.addListSelectionListener(listSelectionListener);
    }

    public synchronized Selection getSelection() {
        if (list.getSelectedIndex() == -1) {
            return DefaultSelection.EMPTY;
        }
        return new DefaultSelection(list.getSelectedValues());
    }

    public synchronized void setSelection(Selection selection) {
        if (selection.isEmpty()) {
            list.clearSelection();
            return;
        }

        final ListModel listModel = list.getModel();
        final Object[] selectedElements = selection.getElements();
        int[] indices = new int[selectedElements.length];
        int indexCount = 0;
        for (int i = 0; i < listModel.getSize(); i++) {
            final Object element = listModel.getElementAt(i);
            for (Object selectedElement : selectedElements) {
                if (element.equals(selectedElement)) {
                    indices[indexCount++] = i;
                    break;
                }
            }
        }
        if (indexCount == 0) {
            list.clearSelection();
            return;
        }

        if (indexCount < indices.length) {
            int[] t = new int[indexCount];
            System.arraycopy(indices, 0, t, 0, indexCount);
            indices = t;
        }
        list.setValueIsAdjusting(true);
        list.setSelectedIndices(indices);
        list.setValueIsAdjusting(false);
    }

    public JList getList() {
        return list;
    }

    public void setList(JList list) {
        if (list != this.list) {
            this.list.removeListSelectionListener(listSelectionListener);
            this.list = list;
            this.list.addListSelectionListener(listSelectionListener);
            fireSelectionChange(this.list);
        }
    }

    protected void handleListSelectionChange(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            fireSelectionChange(list);
        }
    }

    private class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            handleListSelectionChange(event);
        }
    }
}
