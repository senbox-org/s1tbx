package org.esa.beam.framework.ui.application;

import junit.framework.TestCase;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.application.support.DefaultDocViewPane;
import org.esa.beam.framework.ui.application.support.DefaultSelection;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class SelectionEventTest extends TestCase {
    public void testComponentSource() {

        final JPanel component = new JPanel();
        final Selection selection = Selection.NULL;
        final SelectionEvent event = new SelectionEvent(component, selection);
        assertSame(component, event.getComponent());
        assertNull(event.getPageComponent());
        assertSame(selection, event.getSelection());
    }

    public void testPageComponentPaneSource() {
        final PageComponent pageComponent = new AbstractToolView() {
            @Override
            protected JComponent createControl() {
                return new JPanel(); 
            }
        };
        final Selection selection = Selection.NULL;
        final SelectionEvent event = new SelectionEvent(pageComponent, selection);
        assertNull(event.getComponent());
        assertSame(pageComponent, event.getPageComponent());
        assertSame(selection, event.getSelection());
    }
}
