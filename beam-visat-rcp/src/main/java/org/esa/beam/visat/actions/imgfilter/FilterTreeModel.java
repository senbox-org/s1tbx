package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Norman
 */
class FilterTreeModel implements TreeModel, FilterSet.Listener {

    private final Root root;
    private final ArrayList<TreeModelListener> listeners;
    private final FilterSet filterSet;
    private Group anyGroup;

    public FilterTreeModel(FilterSet filterSet) {
        this.filterSet = filterSet;
        this.filterSet.addListener(this);
        root = new Root();
        listeners = new ArrayList<>();

        List<Filter> filters = filterSet.getFilterModels();
        for (Filter filter : filters) {
            insertTreeNodes(filter);
        }
    }

    public void addFilterModel(Filter filter, TreePath selectionPath) {
        if (selectionPath != null) {
            Object[] path = selectionPath.getPath();
            if (path.length >= 2) {
                Group group = (Group) path[1];
                filter.getTags().add(group.name);
            }
        }
        filterSet.addFilterModel(filter);
    }

    public void removeFilterModel(Filter filter) {
        filterSet.removeFilterModel(filter);
    }

    @Override
    public void filterModelAdded(FilterSet filterSet, Filter filter) {
        insertTreeNodes(filter);
    }

    @Override
    public void filterModelRemoved(FilterSet filterSet, Filter filter) {
        removeTreeNodes(filter);
    }

    @Override
    public void filterModelChanged(FilterSet filterSet, Filter filter) {
        notifyTreeNodesChanged(filter);
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

    private void insertTreeNodes(Filter filter) {

        HashSet<String> remainingTags = new HashSet<>(filter.getTags());

        if (filter.getTags().isEmpty()) {
            if (anyGroup == null) {
                anyGroup = new Group("Any");
                root.groups.add(anyGroup);
                notifyTreeNodeInserted(root.groups.size() - 1, anyGroup);
            }
            anyGroup.filters.add(filter);
            notifyTreeNodeInserted(anyGroup, anyGroup.filters.size() - 1, filter);
        }

        for (Group group : root.groups) {
            if (filter.getTags().contains(group.name)) {
                group.filters.add(filter);
                notifyTreeNodeInserted(group, group.filters.size() - 1, filter);
                remainingTags.remove(group.name);
            }
        }

        for (String remainingTag : remainingTags) {
            Group group = new Group(remainingTag);
            root.groups.add(group);
            notifyTreeNodeInserted(root.groups.size() - 1, group);

            group.filters.add(filter);
            notifyTreeNodeInserted(group, group.filters.size() - 1, filter);
        }
    }

    private void removeTreeNodes(Filter filter) {
        int groupIndex = 0;
        while (groupIndex < root.groups.size()) {
            Group group = root.groups.get(groupIndex);
            int filterIndex = group.filters.indexOf(filter);
            if (filterIndex >= 0) {
                group.filters.remove(filterIndex);
                if (group.filters.isEmpty() && group != anyGroup) {
                    root.groups.remove(groupIndex);
                    notifyTreeNodeRemoved(groupIndex, group);
                    //notifyTreeNodeRemoved(group, filterIndex, filterModel);
                    //groupIndex++;
                } else {
                    notifyTreeNodeRemoved(group, filterIndex, filter);
                    groupIndex++;
                }
            } else {
                groupIndex++;
            }
        }
    }

    private void notifyTreeNodeInserted(int groupIndex, Group group) {
        notifyTreeNodesInserted(new TreeModelEvent(this, new Object[]{root}, new int[]{groupIndex}, new Object[]{group}));
    }

    private void notifyTreeNodeInserted(Group group, int filterIndex, Filter filter) {
        notifyTreeNodesInserted(new TreeModelEvent(this, new Object[]{root, group}, new int[]{filterIndex}, new Object[]{filter}));
    }

    private void notifyTreeNodesInserted(TreeModelEvent treeModelEvent) {
        for (TreeModelListener listener : listeners) {
            listener.treeNodesInserted(treeModelEvent);
        }
    }

    private void notifyTreeNodeRemoved(int groupIndex, Group group) {
        notifyTreeNodesRemoved(new TreeModelEvent(this, new Object[]{root}, new int[]{groupIndex}, new Object[]{group}));
    }

    private void notifyTreeNodeRemoved(Group group, int filterIndex, Filter filter) {
        notifyTreeNodesRemoved(new TreeModelEvent(this, new Object[]{root, group}, new int[]{filterIndex}, new Object[]{filter}));
    }

    private void notifyTreeNodesRemoved(TreeModelEvent treeModelEvent) {
        boolean eventDispatchThread = SwingUtilities.isEventDispatchThread();
        System.out.println("eventDispatchThread = " + eventDispatchThread);
        for (TreeModelListener listener : listeners) {
            listener.treeNodesRemoved(treeModelEvent);
        }
    }

    private void notifyTreeNodesChanged(Filter filterModel) {
        for (Group group : root.groups) {
            for (Filter filter : group.filters) {
                if (filter == filterModel) {
                    notifyTreeNodeChanged(group, filterModel);
                }
            }
        }
    }

    private void notifyTreeNodeChanged(Group group, Filter filter) {
        notifyTreeNodesChanged(new TreeModelEvent(this, new Object[]{root.groups, group, filter}));
    }

    private void notifyTreeNodeChanged(TreePath treePath) {
        notifyTreeNodesChanged(new TreeModelEvent(this, treePath));
    }

    private void notifyTreeNodesChanged(TreeModelEvent treeModelEvent) {
        for (TreeModelListener listener : listeners) {
            listener.treeNodesChanged(treeModelEvent);
        }
    }

    public static class Root {
        ArrayList<Group> groups;

        private Root() {
            this.groups = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "Root";
        }
    }

    public static class Group {
        final String name;
        final List<Filter> filters;

        private Group(String name) {
            this.name = name;
            filters = new ArrayList<>();
        }

        @Override
        public String toString() {
            return name;
        }
    }


}
