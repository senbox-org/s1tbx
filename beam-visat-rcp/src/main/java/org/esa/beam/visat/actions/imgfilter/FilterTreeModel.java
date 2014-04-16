package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The internal model for a {@code JTree} displaying a {@link FilterSet}.
 *
 * @author Norman
 */
class FilterTreeModel implements TreeModel, FilterSet.Listener {

    private final Root root;
    private final ArrayList<TreeModelListener> listeners;
    private final FilterSet filterSet;

    public FilterTreeModel(FilterSet filterSet) {
        this.filterSet = filterSet;
        this.filterSet.addListener(this);
        root = new Root();
        listeners = new ArrayList<>();
        createTreeNodes();
    }

    public void addFilterModel(Filter filter, TreePath selectionPath) {
        if (selectionPath != null) {
            Object[] path = selectionPath.getPath();
            if (path.length >= 2) {
                Group group = (Group) path[1];
                filter.getTags().add(group.name);
            }
        }
        filterSet.addFilter(filter);
    }

    public void removeFilterModel(Filter filter) {
        filterSet.removeFilter(filter);
    }

    @Override
    public void filterAdded(FilterSet filterSet, Filter filter) {
        insertTreeNodes(filter);
    }

    @Override
    public void filterRemoved(FilterSet filterSet, Filter filter) {
        removeTreeNodes(filter);
    }

    @Override
    public void filterChanged(FilterSet filterSet, Filter filter, String propertyName) {
        if ("tags".equals(propertyName)) {
            createTreeNodes();
            fireTreeStructureChanged();
        } else {
            fireTreeNodesChanged(filter);
        }
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == root) {
            return root.groups.get(index);
        } else if (parent instanceof Group) {
            return ((Group) parent).filters.get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == root) {
            return root.groups.size();
        } else if (parent instanceof Group) {
            return ((Group) parent).filters.size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == root) {
            return root.groups.indexOf(child);
        } else if (parent instanceof Group) {
            return ((Group) parent).filters.indexOf(child);
        }
        return -1;
    }


    @Override
    public boolean isLeaf(Object node) {
        return node instanceof Filter;
    }

    @Override
    public void valueForPathChanged(TreePath treePath, Object newValue) {
        /*
        System.out.print("valueForPathChanged: ");
        for (Object node : treePath.getPath()) {
            System.out.print("/" + node);
        }
        System.out.print(" = " + newValue);
        */
        //notifyTreeNodeChanged(treePath);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    private synchronized void insertTreeNodes(Filter filter) {

        HashSet<String> remainingTags = new HashSet<>(filter.getTags());

        if (filter.getTags().isEmpty()
            || filter.getTags().contains(root.any.name )) {
            Group group = root.any;
            if (!root.groups.contains(group)) {
                _addGroup(group);
            }
            _addFilter(group, filter);
            remainingTags.remove(group.name);
            if (remainingTags.isEmpty()) {
                return;
            }
        }

        for (Group group : root.groups) {
            if (filter.getTags().contains(group.name)
                || filter.getTags().isEmpty() && group == root.any) {
                _addFilter(group, filter);
                remainingTags.remove(group.name);
            }
        }

        for (String remainingTag : remainingTags) {
            Group group = new Group(remainingTag);
            _addGroup(group);
            _addFilter(group, filter);
        }
    }

    private void _addGroup(Group group) {
        root.groups.add(group);
        fireTreeNodeInserted(root.groups.size() - 1, group);
    }

    private void _addFilter(Group group, Filter filter) {
        group.filters.add(filter);
        fireTreeNodeInserted(group, group.filters.size() - 1, filter);
    }

    private synchronized void removeTreeNodes(Filter filter) {
        int groupIndex = 0;
        while (groupIndex < root.groups.size()) {
            Group group = root.groups.get(groupIndex);
            int filterIndex = group.filters.indexOf(filter);
            if (filterIndex >= 0) {
                group.filters.remove(filterIndex);
                if (group.filters.isEmpty() && group != root.any) {
                    root.groups.remove(groupIndex);
                    fireTreeNodeRemoved(groupIndex, group);
                } else {
                    fireTreeNodeRemoved(group, filterIndex, filter);
                    groupIndex++;
                }
            } else {
                groupIndex++;
            }
        }
    }

    private void fireTreeNodeInserted(int groupIndex, Group group) {
        fireTreeNodesInserted(new TreeModelEvent(this, getPath(root), new int[]{groupIndex}, new Object[]{group}));
    }

    private void fireTreeNodeInserted(Group group, int filterIndex, Filter filter) {
        fireTreeNodesInserted(new TreeModelEvent(this, getPath(root, group), new int[]{filterIndex}, new Object[]{filter}));
    }

    private void fireTreeNodesInserted(TreeModelEvent treeModelEvent) {
        for (TreeModelListener listener : listeners) {
            listener.treeNodesInserted(treeModelEvent);
        }
    }

    private void fireTreeNodeRemoved(int groupIndex, Group group) {
        fireTreeNodesRemoved(new TreeModelEvent(this, getPath(root), new int[]{groupIndex}, new Object[]{group}));
    }

    private void fireTreeNodeRemoved(Group group, int filterIndex, Filter filter) {
        fireTreeNodesRemoved(new TreeModelEvent(this, getPath(root, group), new int[]{filterIndex}, new Object[]{filter}));
    }

    private void fireTreeNodesRemoved(TreeModelEvent treeModelEvent) {
        for (TreeModelListener listener : listeners) {
            listener.treeNodesRemoved(treeModelEvent);
        }
    }

    private void fireTreeNodesChanged(Filter filterModel) {
        for (Group group : root.groups) {
            for (Filter filter : group.filters) {
                if (filter == filterModel) {
                    fireTreeNodeChanged(group, filterModel);
                }
            }
        }
    }

    private void fireTreeNodeChanged(Group group, Filter filter) {
        fireTreeNodesChanged(new TreeModelEvent(this, getPath(root, group, filter)));
    }

    private void fireTreeNodesChanged(TreeModelEvent treeModelEvent) {
        for (TreeModelListener listener : listeners) {
            listener.treeNodesChanged(treeModelEvent);
        }
    }

    public void fireTreeStructureChanged() {
        TreeModelEvent treeModelEvent = new TreeModelEvent(this, getPath(root));
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(treeModelEvent);
        }
    }

    static Object[] getPath(Object ... path) {
        return path;
    }

    private void createTreeNodes() {
        root.any.filters.clear();
        root.groups.clear();
        List<Filter> filters = filterSet.getFilters();
        for (Filter filter : filters) {
            insertTreeNodes(filter);
        }
    }

    public static class Root {
        final Group any = new Group("Any");
        final List<Group> groups;

        private Root() {
            this.groups = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "Root";
        }
    }

    public static class Group implements Comparable<Group> {

        final String name;
        final String nameLC;
        final List<Filter> filters;

        private Group(String name) {
            this.name = name;
            this.nameLC = name.toLowerCase();
            filters = new ArrayList<>();
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Group group = (Group) o;
            return name.equalsIgnoreCase(group.name);
        }

        @Override
        public int hashCode() {
            return nameLC.hashCode();
        }

        @Override
        public int compareTo(Group group) {
            return name.compareToIgnoreCase(group.name);
        }
    }


}
