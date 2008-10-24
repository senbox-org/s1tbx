package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Composite;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerStyleListener;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import com.jidesoft.tree.TreeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

public class LayerManager {

    private final Layer rootLayer;
    private final CheckBoxTree layerTree;
    private final JSlider transparencySlider;
    private final JComboBox alphaCompositeBox;
    private final JPanel control;

    private boolean adjusting;

    public LayerManager(final Layer rootLayer) {
        this.rootLayer = rootLayer;
        layerTree = createCheckBoxTree(rootLayer);
        initSelection(rootLayer);

        transparencySlider = new JSlider(0, 100, 0);
        alphaCompositeBox = new JComboBox(Composite.values());

        final JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);
        // todo - may need this? check meaningful alpha composites
        // sliderPanel.add(alphaCompositeBox, BorderLayout.EAST);

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

        alphaCompositeBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = layerTree.getSelectionPath();
                if (path != null) {
                    Layer layer = getLayer(path);
                    adjusting = true;
                    final Composite composite = (com.bc.ceres.glayer.Composite) alphaCompositeBox.getSelectedItem();
                    layer.getStyle().setComposite(composite);
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

                        doSelection(treeNode, newValue);
                    }
                }
            }
        });

        control = new JPanel(new BorderLayout(4, 4));
        control.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        control.add(sliderPanel, BorderLayout.SOUTH);
    }

    private void doSelection(DefaultMutableTreeNode treeNode, boolean selected) {
        final CheckBoxTreeSelectionModel checkBoxTreeSelectionModel = layerTree.getCheckBoxTreeSelectionModel();
        final TreePath treeNodePath = new TreePath(treeNode.getPath());
        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();

        if (selected) {
            checkBoxTreeSelectionModel.addSelectionPath(treeNodePath);
            if (parentNode != null) {
                doSelection(parentNode, true);
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
                    doSelection(parentNode, false);
                }
            }
        }
    }

    private Layer getLayer(TreePath path) {
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        return (Layer) treeNode.getUserObject();
    }

    private void updateLayerStyleUI(Layer layer) {
        final double transparency = 1 - layer.getStyle().getOpacity();
        final int n = (int) Math.round(100.0 * transparency);
        transparencySlider.setValue(n);

        alphaCompositeBox.setSelectedItem(layer.getStyle().getComposite());
    }

    public Layer getRootLayer() {
        return rootLayer;
    }

    public JComponent getControl() {
        return control;
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
                Layer layer = getLayer(event.getPath());
                updateLayerStyleUI(layer);
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

    private void initSelection(final Layer layer) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) TreeUtils.findTreeNode(layerTree, layer);
        doSelection(treeNode, layer.isVisible());

        for (final Layer childLayer : layer.getChildren()) {
            initSelection(childLayer);
        }
    }
}

