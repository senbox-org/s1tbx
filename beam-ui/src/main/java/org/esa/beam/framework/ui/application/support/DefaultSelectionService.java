package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.ui.application.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultSelectionService implements SelectionService {
    private final Map<String, List<SelectionListener>> listenersMap;
    private final Map<PageComponent, SelectionProvider> providerMap;

    public DefaultSelectionService() {
        listenersMap = new HashMap<String, List<SelectionListener>>(16);
        providerMap = new HashMap<PageComponent, SelectionProvider>(16);
    }

    public synchronized Selection getSelection() {
        // todo - must return selection for active pageComponentId
        throw new IllegalStateException("Not implemented!");
    }

    public synchronized Selection getSelection(String pageComponentId) {
        final PageComponent pageComponent = getPageComponent(pageComponentId);
        if (pageComponent != null) {
            final SelectionProvider selectionProvider = providerMap.get(pageComponent);
            if (selectionProvider != null) {
                return selectionProvider.getSelection();
            }
        }
        return null;
    }

    public synchronized void addSelectionListener(SelectionListener listener) {
        addSelectionListener("", listener);
    }

    public synchronized void addSelectionListener(String pageComponentId, SelectionListener listener) {
        Assert.notNull(pageComponentId, "pageComponentId");
        Assert.notNull(listener, "listener");
        List<SelectionListener> listeners = listenersMap.get(pageComponentId);
        if (listeners == null) {
            listeners = new ArrayList<SelectionListener>(4);
            listenersMap.put(pageComponentId, listeners);
        }
        listeners.add(listener);
    }

    public synchronized void removeSelectionListener(SelectionListener listener) {
        removeSelectionListener("", listener);
    }

    public synchronized void removeSelectionListener(String pageComponentId, SelectionListener listener) {
        Assert.notNull(pageComponentId, "pageComponentId");
        Assert.notNull(listener, "listener");
        List<SelectionListener> listeners = listenersMap.get(pageComponentId);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public synchronized void fireSelectionChange(PageComponent pageComponent, Selection selection) {
        Assert.notNull(pageComponent, "pageComponent");
        Assert.notNull(selection, "selection");
        List<SelectionListener> listeners = listenersMap.get(pageComponent.getId());
        if (listeners != null) {
            for (SelectionListener selectionListener : listeners) {
                selectionListener.selectionChanged(pageComponent, selection);
            }
        }
    }

    public SelectionProvider getSelectionProvider(PageComponent pageComponent) {
        return providerMap.get(pageComponent);
    }

    public void setSelectionProvider(final PageComponent pageComponent, SelectionProvider selectionProvider) {
        // todo - test & implement re-setting a provider
        providerMap.put(pageComponent, selectionProvider);
        selectionProvider.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                fireSelectionChange(pageComponent, event.getSelection());
            }
        });
    }

    private PageComponent getPageComponent(String pageComponentId) {
        for (PageComponent pageComponent : providerMap.keySet()) {
            if (pageComponentId.equals(pageComponent.getId())) {
                return pageComponent;
            }
        }
        return null;
    }
}
