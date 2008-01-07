package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;

import javax.swing.JTree;
import javax.swing.JList;

import org.esa.beam.framework.ui.application.SelectionProvider;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;

public class DefaultSelectionProviderTest extends TestCase {
    public void testList() {
        final JList list = new JList(new Object[] {"Sauerkraut", "Zwiebeln", "Äpfel", "Wacholderbeeren"});
        list.setName("list");
        final MySelectionChangeListener listener = new MySelectionChangeListener();
        final SelectionProvider selectionProvider = new ListSelectionProvider(list);

        assertEquals(DefaultSelection.EMPTY, selectionProvider.getSelection());
        selectionProvider.addSelectionChangeListener(listener);

        DefaultSelection selection = new DefaultSelection(new Object[]{"Zwiebeln"});
        selectionProvider.setSelection(selection);
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("(list,1)", listener.callSeq);

        selection = new DefaultSelection(new Object[]{"Zwiebeln", "Äpfel"});
        selectionProvider.setSelection(selection);
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("(list,1)(list,2)", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionProvider.setSelection(selection);
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("(list,1)(list,2)(list,0)", listener.callSeq);
    }

    private static class MySelectionChangeListener implements SelectionChangeListener {
        String callSeq = "";
        public void selectionChanged(SelectionChangeEvent event) {
            callSeq += "(" + event.getComponent().getName()+ ","+event.getSelection().getElements().length+ ")";
        }
    }
}
