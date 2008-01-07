package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;
import org.esa.beam.framework.ui.application.*;

import javax.swing.JComponent;
import javax.swing.JList;


public class DefaultSelectionServiceTest extends TestCase {
    private DefaultSelectionService selectionService;

    @Override
    protected void setUp() throws Exception {
        selectionService = new DefaultSelectionService();
    }

    public void testConstr() {
        final SelectionListenerMock partListener = new SelectionListenerMock();
        final SelectionListenerMock appListener = new SelectionListenerMock();

        selectionService.addSelectionListener("PC", partListener);
        selectionService.addSelectionListener(appListener);

        PageComponent component = createPageComponent("PC");

        selectionService.fireSelectionChange(component, new DefaultSelection("U"));
        selectionService.fireSelectionChange(component, new DefaultSelection("V"));
        selectionService.fireSelectionChange(component, new DefaultSelection("W"));

        assertEquals("(PC,U)(PC,V)(PC,W)", partListener.callStack);
        assertEquals("", appListener.callStack);

        selectionService.removeSelectionListener("PC", partListener);

        selectionService.fireSelectionChange(component, new DefaultSelection("A"));
        selectionService.fireSelectionChange(component, new DefaultSelection("B"));
        selectionService.fireSelectionChange(component, new DefaultSelection("C"));

        assertEquals("(PC,U)(PC,V)(PC,W)", partListener.callStack);
        assertEquals("", appListener.callStack);
    }

    public void testSelectionProviderSupport() {
        final AbstractToolView pageComponent = createPageComponent("x");
        final SelectionProvider selectionProvider = new TestSelectionProvider();

        selectionService.setSelectionProvider(pageComponent, selectionProvider);

        assertSame(selectionProvider, selectionService.getSelectionProvider(pageComponent));

        final MySelectionListener serviceListener = new MySelectionListener();
        final MySelectionListener serviceListenerX = new MySelectionListener();
        final MySelectionListener serviceListenerY = new MySelectionListener();
        selectionService.addSelectionListener(serviceListener);
        selectionService.addSelectionListener("x", serviceListenerX);
        selectionService.addSelectionListener("y", serviceListenerY);

        // todo - assertEquals(DefaultSelection.EMPTY, selectionService.getSelection());
        assertEquals(DefaultSelection.EMPTY, selectionService.getSelection("x"));
        assertEquals(null, selectionService.getSelection("y"));

        final DefaultSelection selection = new DefaultSelection(new Object[]{12, 13, 14});
        selectionProvider.setSelection(selection);

        // todo - assertEquals(DefaultSelection.EMPTY, selectionService.getSelection());
        assertSame(selection, selectionService.getSelection("x"));
        assertEquals(null, selectionService.getSelection("y"));

        assertEquals(0, serviceListener.calls);
        assertEquals(1, serviceListenerX.calls);
        assertEquals(0, serviceListenerY.calls);
    }

    public void testComponentSelectionProvider() {
        SelectionProvider selectionProvider = new TestSelectionProvider();

        assertEquals(DefaultSelection.EMPTY, selectionProvider.getSelection());

        final MySelectionChangeListener providerListener = new MySelectionChangeListener();
        selectionProvider.addSelectionChangeListener(providerListener);

        selectionProvider.setSelection(new DefaultSelection(new Object[]{6, 7, 8}));
        assertEquals(1, providerListener.calls);

        final DefaultSelection selection = new DefaultSelection(new Object[]{1, 2, 3});
        selectionProvider.setSelection(selection);
        assertSame(selection, selectionProvider.getSelection());
        assertEquals(2, providerListener.calls);
    }

    private AbstractToolView createPageComponent(final String id) {
        return new AbstractToolView() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            protected JComponent createControl() {
                return new JList();
            }
        };
    }

    private static class TestSelectionProvider extends AbstractSelectionProvider {

        private Selection selection;

        public TestSelectionProvider() {
            selection = DefaultSelection.EMPTY;
        }

        public synchronized Selection getSelection() {
            return selection;
        }

        public synchronized void setSelection(Selection selection) {
            this.selection = selection;
            fireSelectionChange(this, selection);
        }
    }

    private static class MySelectionChangeListener implements SelectionChangeListener {

        int calls;

        public void selectionChanged(SelectionChangeEvent event) {
            calls++;
        }
    }

    private static class MySelectionListener implements SelectionListener {

        int calls;

        public void selectionChanged(PageComponent pageComponent, Selection selection) {
            calls++;
        }
    }

    private static class SelectionListenerMock implements SelectionListener {
        String callStack = "";

        public void selectionChanged(PageComponent pageComponent, Selection selection) {
            callStack += "(" + pageComponent.getId() + "," + selection.getFirstElement() + ")";
        }
    }
}
