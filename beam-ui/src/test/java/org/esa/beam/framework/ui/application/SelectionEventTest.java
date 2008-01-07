package org.esa.beam.framework.ui.application;

import junit.framework.TestCase;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.application.support.DefaultSelection;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class SelectionEventTest extends TestCase {
    public void testComponentSource() {
        final JPanel component = new JPanel();
        final Selection selection = new DefaultSelection("X");
        final SelectionChangeEvent event = new SelectionChangeEvent(component, selection);
        assertSame(component, event.getSource());
        assertSame(component, event.getComponent());
        assertSame(selection, event.getSelection());
    }

    public void testSomeSource() {
        final Object source = new Object();
        final Selection selection = new DefaultSelection("X");
        final SelectionChangeEvent event = new SelectionChangeEvent(source, selection);
        assertSame(source, event.getSource());
        assertNull(event.getComponent());
        assertSame(selection, event.getSelection());
    }
}
