package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.esa.beam.visat.actions.imgfilter.model.FilterSetStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Norman
 */
public class FilterSetForm extends JPanel {

    private FilterSet filterSet;

    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    private JTree filterTree;
    private FilterSetStore filterSetStore;
    private FilterEditor filterEditor;
    private transient List<Listener> listeners;
    private JButton saveButton;
    private boolean modified;

    public FilterSetForm(FilterSet filterSet, FilterSetStore filterSetStore, FilterEditor filterEditor) {
        super(new BorderLayout(4, 4));
        this.filterSetStore = filterSetStore;
        this.filterEditor = filterEditor;
        setBorder(new EmptyBorder(4, 4, 4, 4));
        listeners = new ArrayList<>();
        this.filterSet = filterSet;
        this.filterSet.addListener(new FilterSet.Listener() {
            @Override
            public void filterModelAdded(FilterSet filterSet, Filter filter) {
                setModified(true);
            }

            @Override
            public void filterModelRemoved(FilterSet filterSet, Filter filter) {
                setModified(true);
            }

            @Override
            public void filterModelChanged(FilterSet filterSet, Filter filter) {
                setModified(true);
            }
        });
        initUI();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        if (this.modified != modified) {
            this.modified = modified;
            updateState();
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        filterSet.addListener(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
        filterSet.removeListener(listener);
    }

    public Filter getSelectedFilterModel() {
        TreePath selectionPath = filterTree.getSelectionPath();
        if (selectionPath != null) {
            return getFilterModel(selectionPath);
        }
        return null;
    }

    private void initUI() {

        addButton = new JButton(UIUtils.loadImageIcon("/com/bc/ceres/swing/actions/icons_22x22/list-add.png"));
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Filter filter = Filter.create(5);
                filter.setEditable(true);
                ((FilterTreeModel) filterTree.getModel()).addFilterModel(filter, filterTree.getSelectionPath());
            }
        });


        removeButton = new JButton(UIUtils.loadImageIcon("/com/bc/ceres/swing/actions/icons_22x22/list-remove.png"));
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((FilterTreeModel) filterTree.getModel()).removeFilterModel((Filter) filterTree.getSelectionPath().getLastPathComponent());
            }
        });

        editButton = new JButton(UIUtils.loadImageIcon("/com/bc/ceres/swing/actions/icons_22x22/document-properties.png"));
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Filter filter = (Filter) filterTree.getSelectionPath().getLastPathComponent();
                filterEditor.setFilter(filter);
                filterEditor.show();

            }
        });

        saveButton = new JButton(UIUtils.loadImageIcon("/com/bc/ceres/swing/actions/icons_22x22/document-save.png"));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    filterSetStore.storeFilterSetModel(filterSet);
                    setModified(false);
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(null, "Failed to save:\n" + ioe.getMessage(), "Save", JOptionPane.ERROR_MESSAGE);
                }

            }
        });

        JToolBar toolBar = new JToolBar(SwingConstants.VERTICAL);
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.add(addButton);
        toolBar.add(removeButton);
        toolBar.add(editButton);
        toolBar.add(saveButton);

        filterTree = new JTree(filterSet != null ? new FilterTreeModel(filterSet) : null);
        filterTree.setRootVisible(false);
        filterTree.setShowsRootHandles(true);
        filterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        filterTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                updateState();
                TreePath selectionPath = filterTree.getSelectionPath();
                Filter filter = getFilterModel(selectionPath);
                filterEditor.setFilter(filter);
                notifyFilterModelSelected(filter);
            }
        });
        filterTree.setCellRenderer(new MyDefaultTreeCellRenderer());
        filterTree.putClientProperty("JTree.lineStyle", "Angled");

        //installTreeDragAndDrop();
        expandAllTreeNodes();

        add(new JScrollPane(filterTree), BorderLayout.CENTER);
        add(toolBar, BorderLayout.EAST);

        updateState();
    }

    private Filter getFilterModel(TreePath selectionPath) {
        if (selectionPath != null) {
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if (lastPathComponent instanceof Filter) {
                return (Filter) lastPathComponent;
            }
        }
        return null;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void installTreeDragAndDrop() {
        filterTree.setDragEnabled(true);
        filterTree.setDropMode(DropMode.INSERT);
        filterTree.setDropTarget(new DropTarget(filterTree, DnDConstants.ACTION_MOVE, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                System.out.println("dragEnter: dtde = " + dtde);
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                System.out.println("dragOver: dtde = " + dtde);
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                System.out.println("dropActionChanged: dtde = " + dtde);
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                System.out.println("dragExit: dte = " + dte);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                System.out.println("drop: dtde = " + dtde);
            }
        }));
    }

    private void updateState() {
        TreePath selectionPath = filterTree.getSelectionPath();
        boolean filterModelSelected = selectionPath != null && selectionPath.getLastPathComponent() instanceof Filter;

        addButton.setEnabled(filterSet.isEditable());
        removeButton.setEnabled(filterModelSelected && filterSet.isEditable());
        editButton.setEnabled(filterModelSelected);
        saveButton.setEnabled(filterSet.isEditable() && isModified());
    }

    private void expandAllTreeNodes() {
        FilterTreeModel model = (FilterTreeModel) filterTree.getModel();
        int childCount = model.getChildCount(model.getRoot());
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(model.getRoot(), i);
            TreePath treePath = new TreePath(new Object[]{model.getRoot(), child});
            filterTree.expandRow(filterTree.getRowForPath(treePath));
        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {

        private Font plainFont;
        private Font boldFont;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            final JLabel c = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (plainFont == null) {
                plainFont = c.getFont().deriveFont(Font.PLAIN);
                boldFont = c.getFont().deriveFont(Font.BOLD);
            }
            c.setFont(leaf ? plainFont : boldFont);
            c.setIcon(null);
            return c;
        }
    }

    void notifyFilterModelSelected(Filter filter) {
        for (Listener listener : listeners) {
            listener.filterModelSelected(filterSet, filter);
        }
    }

    public interface Listener extends FilterSet.Listener {
        void filterModelSelected(FilterSet filterSet, Filter filter);
    }

}
