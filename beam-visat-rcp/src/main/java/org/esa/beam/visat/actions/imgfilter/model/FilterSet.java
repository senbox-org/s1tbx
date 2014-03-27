package org.esa.beam.visat.actions.imgfilter.model;

import com.thoughtworks.xstream.XStream;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Norman
 */
public class FilterSet implements Filter.Listener {

    String name;
    boolean editable;
    ArrayList<Filter> filters;
    transient List<Listener> listeners;

    public FilterSet() {
        this("", true);
    }

    public FilterSet(String name, boolean editable) {
        this.name = name;
        this.editable = editable;
        filters = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public int getFilterModelCount() {
        return filters.size();
    }

    public Filter getFilterModel(int index) {
        return filters.get(index);
    }

    public List<Filter> getFilterModels() {
        return filters;
    }

    public int getFilterModelIndex(Filter filter) {
        return filters.indexOf(filter);
    }

    public void addFilterModels(String tag, Filter... filters) {
        for (Filter filter : filters) {
            filter.getTags().add(tag);
            addFilterModel(filter);
        }
    }

    public void addFilterModel(Filter filter) {
        filters.add(filter);
        filter.addListener(this);
        notifyFilterModelAdded(filter);
    }

    public void removeFilterModel(Filter filter) {
        if (filters.remove(filter)) {
            filter.removeListener(this);
            notifyFilterModelRemoved(filter);
        }
    }

    public static XStream createXStream() {
        final XStream xStream = Filter.createXStream();
        xStream.alias("filterSet", FilterSet.class);
        return xStream;
    }

    @SuppressWarnings("UnusedDeclaration")
    private Object readResolve() {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        if (filters == null) {
            filters = new ArrayList<>();
        }
        for (Filter filter : filters) {
            filter.removeListener(this);
            filter.addListener(this);
        }
        return this;
    }

    @Override
    public void filterModelChanged(Filter filter) {
        notifyFilterModelChanged(filter);
    }

    void notifyFilterModelChanged(Filter filter) {
        for (Listener listener : listeners) {
            listener.filterModelChanged(this, filter);
        }
    }

    void notifyFilterModelAdded(Filter filter) {
        for (Listener listener : listeners) {
            listener.filterModelAdded(this, filter);
        }
    }

    void notifyFilterModelRemoved(Filter filter) {
        for (Listener listener : listeners) {
            listener.filterModelRemoved(this, filter);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void filterModelAdded(FilterSet filterSet, Filter filter);

        void filterModelRemoved(FilterSet filterSet, Filter filter);

        void filterModelChanged(FilterSet filterSet, Filter filter);
    }
}
