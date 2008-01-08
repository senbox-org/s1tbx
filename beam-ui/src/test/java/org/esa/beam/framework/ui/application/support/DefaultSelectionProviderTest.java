package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;

import javax.swing.JList;

import org.esa.beam.framework.ui.application.SelectionProvider;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;

public class DefaultSelectionProviderTest extends TestCase {
    private static final Object[] DATA = new Object[] {"Sauerkraut", "Zwiebeln", "Äpfel", "Wacholderbeeren"};

    public void testList() {
        final JList list = new JList(DATA);
        final ListSelectionProvider selectionProvider = new ListSelectionProvider(list);
        assertSame(list, selectionProvider.getList());
        assertSame(DefaultSelection.EMPTY, selectionProvider.getSelection());
        testSelectionProviderInterface(selectionProvider);

        final JList otherList = new JList(DATA);
        otherList.setSelectedIndices(new int[] {2, 0, 3});
        selectionProvider.setList(otherList);
        assertSame(otherList, selectionProvider.getList());
        assertEquals(new DefaultSelection(new Object[] { "Sauerkraut", "Äpfel","Wacholderbeeren"}), 
                     selectionProvider.getSelection());
        testSelectionProviderInterface(selectionProvider);
    }

    private void testSelectionProviderInterface(SelectionProvider selectionProvider) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionProvider.addSelectionChangeListener(listener);

        DefaultSelection selection = new DefaultSelection(new Object[]{"Zwiebeln"});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection(new Object[]{"Zwiebeln", "Äpfel"});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionProvider.setSelection(selection);
        assertSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection(DATA);
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static class SelectionChangeHandler implements SelectionChangeListener {
        String callSeq = "";
        public void selectionChanged(SelectionChangeEvent event) {
            callSeq += event.getSelection().getElements().length + ";";
        }
    }
}
