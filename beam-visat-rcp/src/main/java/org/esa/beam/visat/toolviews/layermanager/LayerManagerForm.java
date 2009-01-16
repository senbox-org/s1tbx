package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerStyleListener;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import com.jidesoft.tree.TreeUtils;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.util.WeakHashMap;

class LayerManagerForm {

    private final Layer rootLayer;
    private final CheckBoxTree layerTree;
    private final JSlider transparencySlider;
    private final JPanel control;
    private final WeakHashMap<LayerSelectionListener, Object> layerSelectionListenerMap;

    private boolean adjusting;

    public LayerManagerForm(final Layer rootLayer) {
        this.rootLayer = rootLayer;
        layerTree = createCheckBoxTree(rootLayer);
        initVisibilitySelection(rootLayer);

        layerSelectionListenerMap = new WeakHashMap<LayerSelectionListener, Object>(3);

        transparencySlider = new JSlider(0, 100, 0);

        final JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);

        transparencySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                TreePath path = layerTree.getSelectionPath();
                if (path != null) {
                    Layer layer = getLayer(path);
                    adjusting = true;
                    layer.getStyle().setOpacity(1.0 - transparencySlider.getValue() / 100.0f);
                    adjusting = false;
                }

            }
        });

        rootLayer.addListener(new LayerStyleListener() {
            @Override
            public void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent event) {
                if (!adjusting) {
                    final TreePath selectionPath = layerTree.getSelectionPath();
                    if (selectionPath != null) {
                        final Layer selectedLayer = getLayer(selectionPath);
                        if (selectedLayer == layer) {
                            updateLayerStyleUI(layer);
                        }
                    }
                }
            }
        });

        rootLayer.addListener(new AbstractLayerListener() {
            @Override
            public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
                if (event.getPropertyName().equals("visible")) {
                    final Boolean oldValue = (Boolean) event.getOldValue();
                    final Boolean newValue = (Boolean) event.getNewValue();

                    if (!oldValue.equals(newValue) && !adjusting) {
                        final DefaultMutableTreeNode treeNode =
                                (DefaultMutableTreeNode) TreeUtils.findTreeNode(layerTree, layer);

                        doVisibilitySelection(treeNode, newValue);
                    }
                }
            }
        });

        control = new JPanel(new BorderLayout(4, 4));
        control.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        control.add(sliderPanel, BorderLayout.SOUTH);
    }

    public Layer getRootLayer() {
        return rootLayer;
    }

    public JComponent getControl() {
        return control;
    }

    Layer getSelectedLayer() {
        TreePath selectionPath = layerTree.getSelectionPath();
        if (selectionPath != null) {
            return getLayer(selectionPath);
        }
        return null;
    }

    void setSelectedLayer(Layer layer) {
        Layer selectedLayer = getSelectedLayer();
        if (selectedLayer == layer) {
            return;
        }
        if (layer != null) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) TreeUtils.findTreeNode(layerTree, layer);
            if (treeNode != null) {
                layerTree.setSelectionPath(new TreePath(treeNode.getPath()));
            } else {
                layerTree.clearSelection();
            }
        } else {
            layerTree.clearSelection();
        }
    }

    void addLayerSelectionListener(LayerSelectionListener listener) {
        layerSelectionListenerMap.put(listener, "<null>");
    }

    void removeLayerSelectionListener(LayerSelectionListener listener) {
        layerSelectionListenerMap.remove(listener);
    }

    private Layer getLayer(TreePath path) {
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        return (Layer) treeNode.getUserObject();
    }

    private void fireLayerSelectionChanged(Layer selectedLayer) {
        for (LayerSelectionListener layerSelectionListener : layerSelectionListenerMap.keySet()) {
            layerSelectionListener.layerSelectionChanged(selectedLayer);
        }
    }

    private void initVisibilitySelection(final Layer layer) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) TreeUtils.findTreeNode(layerTree, layer);
        doVisibilitySelection(treeNode, layer.isVisible());
        for (Layer childLayer : layer.getChildren()) {
            initVisibilitySelection(childLayer);
        }
    }

    private void doVisibilitySelection(DefaultMutableTreeNode treeNode, boolean selected) {
        CheckBoxTreeSelectionModel checkBoxTreeSelectionModel = layerTree.getCheckBoxTreeSelectionModel();
        TreePath treeNodePath = new TreePath(treeNode.getPath());
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();

        if (selected) {
            checkBoxTreeSelectionModel.addSelectionPath(treeNodePath);
            if (parentNode != null) {
                doVisibilitySelection(parentNode, true);
            }
        } else {
            checkBoxTreeSelectionModel.removeSelectionPath(treeNodePath);
            if (parentNode != null) {
                boolean noChildNodeSelected = true;

                for (int i = 0; i < parentNode.getChildCount(); i++) {
                    final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                    if (checkBoxTreeSelectionModel.isPathSelected(new TreePath(childNode.getPath()))) {
                        noChildNodeSelected = false;
                        break;
                    }
                }
                if (noChildNodeSelected) {
                    doVisibilitySelection(parentNode, false);
                }
            }
        }
    }


    private void updateLayerStyleUI(Layer layer) {
        final double transparency = 1 - layer.getStyle().getOpacity();
        final int n = (int) Math.round(100.0 * transparency);
        transparencySlider.setValue(n);
    }

    private CheckBoxTree createCheckBoxTree(Layer rootLayer) {
        final LayerTreeModel layerTreeModel = new LayerTreeModel(rootLayer);

        final CheckBoxTree checkBoxTree = new CheckBoxTree(layerTreeModel);
        checkBoxTree.setRootVisible(false);
        checkBoxTree.setShowsRootHandles(true);
        checkBoxTree.setDigIn(false);
        checkBoxTree.setDragEnabled(false);
        checkBoxTree.setDropMode(DropMode.ON_OR_INSERT);

        // removed because the current {@link NodeTransferHandler} is too immature (rq)
//        final NodeMoveTransferHandler transferHandler = new NodeMoveTransferHandler();
//        checkBoxTree.setTransferHandler(transferHandler);

        checkBoxTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                Layer selectedLayer = getLayer(event.getPath());
                updateLayerStyleUI(selectedLayer);
                fireLayerSelectionChanged(selectedLayer);
            }
        });

        final CheckBoxTreeSelectionModel checkBoxSelectionModel = checkBoxTree.getCheckBoxTreeSelectionModel();
        checkBoxSelectionModel.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                if (!adjusting) {
                    Layer layer = getLayer(event.getPath());
                    layer.setVisible(checkBoxSelectionModel.isPathSelected(event.getPath()));
                }
            }
        });

        layerTreeModel.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                if (e.getChildren().length > 0) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getChildren()[0];
                    checkBoxTree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
                }
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
            }
        });

        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) checkBoxTree.getActualCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        return checkBoxTree;
    }

}

