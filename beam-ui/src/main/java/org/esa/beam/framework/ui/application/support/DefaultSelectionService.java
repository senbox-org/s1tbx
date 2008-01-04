package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.ui.application.*;

import javax.swing.JComponent;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
* User: Norman
* Date: 03.01.2008
* Time: 15:11:54
* To change this template use File | Settings | File Templates.
*/
public class DefaultSelectionService implements SelectionService {
    private final Map<String, List<SelectionListener>> listenersMap;
    private final Map<String, Selection> selectionMap;
    private final Map<String, SelectionProvider> providerMap;

    public DefaultSelectionService() {
        listenersMap = new HashMap<String, List<SelectionListener>>(16);
        selectionMap = new HashMap<String, Selection>(16);
        providerMap = new HashMap<String, SelectionProvider>(16);
    }

    public synchronized Selection getSelection() {
        return Selection.NULL;
    }

    public synchronized Selection getSelection(String pageComponentId) {
        final SelectionProvider selectionProvider = providerMap.get(pageComponentId);
        if (selectionProvider == null) {
            return Selection.NULL;
        }
        final Selection selection = selectionProvider.getSelection();
        return selection != null ? selection : Selection.NULL;
    }

    public synchronized void addSelectionListener(SelectionListener listener) {
        addSelectionListener("", listener);
    }

    public synchronized void addSelectionListener(String pageComponentId, SelectionListener listener) {
        Assert.notNull(pageComponentId, "pageComponentId");
        Assert.notNull(listener, "listener");
        // todo - check valid partId
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
        // todo - check valid partId
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

    public SelectionProvider getSelectionProvider(String pageComponentId) {
        return providerMap.get(pageComponentId);
    }

    public void setSelectionProvider(SelectionProvider selectionProvider) {
        providerMap.put(selectionProvider.getPageComponent().getId(), selectionProvider);
        selectionProvider.addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(PageComponent pageComponent, Selection selection) {
                 fireSelectionChange(pageComponent, selection);
            }
        });
    }
}
