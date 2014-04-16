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

    public int getFilterCount() {
        return filters.size();
    }

    public Filter getFilter(int index) {
        return filters.get(index);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public boolean containsFilter(Filter filter) {
        return filters.contains(filter);
    }

    public int getFilterIndex(Filter filter) {
        return filters.indexOf(filter);
    }

    public void addFilter(String tag, Filter... filters) {
        for (Filter filter : filters) {
            filter.getTags().add(tag);
            addFilter(filter);
        }
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
        filter.addListener(this);
        fireFilterAdded(filter);
    }

    public void removeFilter(Filter filter) {
        if (filters.remove(filter)) {
            filter.removeListener(this);
            fireFilterModelRemoved(filter);
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
    public void filterChanged(Filter filter, String propertyName) {
        fireFilterChanged(filter, propertyName);
    }

    void fireFilterChanged(Filter filter, String propertyName) {
        for (Listener listener : listeners) {
            listener.filterChanged(this, filter, propertyName);
        }
    }

    void fireFilterAdded(Filter filter) {
        for (Listener listener : listeners) {
            listener.filterAdded(this, filter);
        }
    }

    void fireFilterModelRemoved(Filter filter) {
        for (Listener listener : listeners) {
            listener.filterRemoved(this, filter);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void filterAdded(FilterSet filterSet, Filter filter);

        void filterRemoved(FilterSet filterSet, Filter filter);

        void filterChanged(FilterSet filterSet, Filter filter, String propertyName);
    }
}
