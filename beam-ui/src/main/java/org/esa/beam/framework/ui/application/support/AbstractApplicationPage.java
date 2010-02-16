package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.DocView;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentListener;
import org.esa.beam.framework.ui.application.PageComponentPane;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.util.Debug;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract "convenience" implementation of <code>ApplicationPage</code>.
 *
 * @author Marco Peters (inspired by Spring RCP)
 */
public abstract class AbstractApplicationPage extends AbstractControlFactory implements ApplicationPage {

    private final List<PageComponentListener> pageComponentListeners = new ArrayList<PageComponentListener>();

    private final Map<String, PageComponent> pageComponentMap = new HashMap<String, PageComponent>();

    private PageComponent activeComponent;

    private boolean settingActiveComponent;

    private PropertyChangeListener pageComponentUpdater = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            pageComponentChanged(evt);
        }
    };

    protected AbstractApplicationPage() {
    }

    @Override
    public void addPageComponentListener(PageComponentListener listener) {
        pageComponentListeners.add(listener);
    }

    @Override
    public void removePageComponentListener(PageComponentListener listener) {
        pageComponentListeners.remove(listener);
    }

    /**
     * Returns the active <code>PageComponent</code>, or <code>null</code> if none.
     *
     * @return the active <code>PageComponent</code>
     */
    @Override
    public PageComponent getActiveComponent() {
        return activeComponent;
    }

    /**
     * Closes the given <code>PageComponent</code>. This method disposes the
     * <code>PageComponent</code>, triggers all necessary events ("focus lost" and
     * "closed"), and will activate another <code>PageComponent</code> (if there is
     * one).
     * <p/>
     * Does nothing if this <code>ApplicationPage</code> doesn't contain the given
     * <code>PageComponent</code>.
     *
     * @param pageComponent the <code>PageComponent</code>
     */
    @Override
    public void close(PageComponent pageComponent) {
        if (!pageComponentMap.containsValue(pageComponent)) {
            return;
        }

        if (pageComponent == activeComponent) {
            fireFocusLost(pageComponent);
            activeComponent = null;
        }

        pageComponentMap.remove(pageComponent.getId());
        if (pageComponent instanceof ToolView) {
            ToolView toolView = (ToolView) pageComponent;
            doRemoveToolView(toolView);
        } else if (pageComponent instanceof DocView) {
            // todo - DocView docView = (DocView) pageComponent;
            // todo - doRemoveDocView(docView);
        }

        pageComponent.removePropertyChangeListener(pageComponentUpdater);

        fireHidden(pageComponent);
        fireClosed(pageComponent);

        pageComponent.dispose();

        if (activeComponent == null) {
            setActiveComponent();
        }
    }

    /**
     * Closes this <code>ApplicationPage</code>. This method calls
     * {@link #close(PageComponent)} for each open <code>PageComponent</code>.
     *
     * @return <code>true</code> if the operation was successfull, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean close() {
        for (PageComponent component : pageComponentMap.values()) {
            close(component);
        }
        return true;
    }

    @Override
    public PageComponent getPageComponent(String id) {
        return pageComponentMap.get(id);
    }

    @Override
    public ToolView[] getToolViews() {
        ArrayList<ToolView> toolViews = new ArrayList<ToolView>(pageComponentMap.size());
        for (PageComponent component : pageComponentMap.values()) {
            if (component instanceof ToolView) {
                toolViews.add((ToolView) component);
            }
        }
        return toolViews.toArray(new ToolView[toolViews.size()]);
    }

    @Override
    public ToolView getToolView(String id) {
        final PageComponent component = getPageComponent(id);
        if (component instanceof ToolView) {
            return (ToolView) component;
        }
        return null;
    }

    public abstract ToolViewDescriptor getToolViewDescriptor(String id);

    @Override
    public ToolView addToolView(ToolViewDescriptor viewDescriptor) {
        ToolView toolView = getToolView(viewDescriptor.getId());
        if (toolView != null) {
            throw new IllegalStateException("pageComponent != null");
        }
        toolView = createToolView(viewDescriptor);
        pageComponentMap.put(viewDescriptor.getId(),toolView);
        doAddToolView(toolView);
        toolView.addPropertyChangeListener(pageComponentUpdater);
        return toolView;
    }

    @Override
    public ToolView showToolView(String id) {
        return showToolView(getToolViewDescriptor(id));
    }

    @Override
    public ToolView showToolView(ToolViewDescriptor viewDescriptor) {
        ToolView toolView = getToolView(viewDescriptor.getId());
        if (toolView == null) {
            toolView = addToolView(viewDescriptor);
        }
        doShowToolView(toolView);
        fireShown(toolView);
        setActiveComponent(toolView);
        return toolView;
    }


    @Override
    public void hideToolView(ToolView toolView) {
        doHideToolView(toolView);
        fireHidden(toolView);
        setActiveComponent();
    }

    @Override
    public DocView openDocView(Object editorInput) {
        // TODO implement DocViews
        return null;
    }

    @Override
    public boolean closeAllDocViews() {
        // TODO implement DocViews
        return true;
    }

    /**
     * Creates a PageComponent for the given PageComponentDescriptor.
     *
     * @param descriptor the descriptor
     * @return the created PageComponent
     */
    protected ToolView createToolView(ToolViewDescriptor descriptor) {
        ToolView toolView = (ToolView) descriptor.createPageComponent();
        toolView.setContext(new DefaultPageComponentContext(this, createToolViewPane(toolView)));
        return toolView;
    }

    /**
     * Called when the <code>PageComponent</code> changes any of its properties (display
     * name, caption, icon, ...).
     * <p/>
     * This method should be overridden when these changes must be reflected in the UI.
     *
     * @param evt
     */
    protected void pageComponentChanged(PropertyChangeEvent evt) {
        // do nothing by default
    }


    protected void fireOpened(PageComponent component) {
        Debug.trace("AbstractApplicationPage.fireOpened [" + component + "]");
        component.componentOpened();
        for (PageComponentListener listener : pageComponentListeners) {
            listener.componentOpened(component);
        }
    }

    protected void fireClosed(PageComponent component) {
        Debug.trace("AbstractApplicationPage.fireClosed [" + component + "]");
        component.componentClosed();
        for (PageComponentListener listener : pageComponentListeners) {
            listener.componentClosed(component);
        }
    }

    protected void fireShown(PageComponent component) {
        Debug.trace("AbstractApplicationPage.fireShown [" + component + "]");
        for (PageComponentListener listener : pageComponentListeners) {
            listener.componentShown(component);
        }
    }

    protected void fireHidden(PageComponent component) {
        Debug.trace("AbstractApplicationPage.fireHidden [" + component + "]");
        for (PageComponentListener listener : pageComponentListeners) {
            listener.componentHidden(component);
        }
    }

    protected void fireFocusGained(PageComponent component) {
        Debug.trace("AbstractApplicationPage.fireFocusGained [" + component + "]");
        component.componentFocusGained();
        for (PageComponentListener listener : pageComponentListeners) {
            listener.componentFocusGained(component);
        }
    }

    protected void fireFocusLost(PageComponent component) {
        Debug.trace("AbstractApplicationPage.fireFocusLost [" + component + "]");        
        component.componentFocusLost();
        for (PageComponentListener listener : pageComponentListeners) {
            listener.componentFocusLost(component);
        }
    }


    /**
     * Set the active page component by determing it. This is usually achieved by
     * asking the component manager used in a concrete application page for its activated component.
     * For example, if the component manager is a {@link javax.swing.JDesktopPane}, the active component
     * is determined by its active {@link javax.swing.JInternalFrame}.
     */
    protected abstract void setActiveComponent();


    /**
     * Activates the given <code>PageComponent</code>. Does nothing if it is already
     * the active one.
     * <p/>
     * Does nothing if this <code>ApplicationPage</code> doesn't contain the given
     * <code>PageComponent</code>.
     *
     * @param pageComponent the <code>PageComponent</code>
     */
    public void setActiveComponent(PageComponent pageComponent) {
        if (!pageComponentMap.containsValue(pageComponent)) {
            return;
        }

        Debug.trace("setActiveComponent: pageComponent = " + pageComponent);
        if (settingActiveComponent) {
            return;
        }

        // if pageComponent is already active, don't do anything
        if (this.activeComponent == pageComponent) {
            setActiveSelectionContext();
            return;
        }

        settingActiveComponent = true;

        if (this.activeComponent != null) {
            fireFocusLost(this.activeComponent);
        }
        giveFocusTo(pageComponent);
        this.activeComponent = pageComponent;
        fireFocusGained(this.activeComponent);

        setActiveSelectionContext();

        settingActiveComponent = false;
    }

    private void setActiveSelectionContext() {
        if (activeComponent!= null) {
            getSelectionManager().setSelectionContext(activeComponent.getSelectionContext());
        } else {
            getSelectionManager().setSelectionContext(null);
        }
    }


    /**
     * This method must add the given tool view in the UI.
     * <p/>
     * Implementors may choose to add the tool view's control
     * directly, or add the tool view's control.
     *
     * @param toolView the tool view
     */
    protected abstract void doAddToolView(ToolView toolView);

    /**
     * This method must remove the given tool view from the UI.
     *
     * @param toolView the tool view
     */
    protected abstract void doRemoveToolView(ToolView toolView);

    /**
     * This method must show the given tool view in the UI.
     *
     * @param toolView the tool view
     */
    protected abstract void doShowToolView(ToolView toolView);

    /**
     * This method must hide the given tool view in the UI.
     *
     * @param toolView the tool view
     */
    protected abstract void doHideToolView(ToolView toolView);

    /**
     * This method must transfer the focus to the given <code>PageComponent</code>.
     * This could involve making an internal frame visible, selecting a tab in a tabbed
     * pane, ...
     *
     * @param pageComponent the <code>PageComponent</code>
     * @return <code>true</code> if the operation was successful, <code>false</code>
     *         otherwise
     */
    protected abstract boolean giveFocusTo(PageComponent pageComponent);

    protected abstract PageComponentPane createToolViewPane(ToolView toolView);

}
