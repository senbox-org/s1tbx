package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Composite;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerStyleListener;
import com.jidesoft.icons.IconsFactory;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import com.jidesoft.tree.TreeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;

class LayerManager {
    private JSlider transparencySlider;
    private JComboBox alphaCompositeBox;
    private boolean adjusting;
    private JPanel control;
    private CheckBoxTree layerTree;
    private final Layer rootLayer;

    public LayerManager(Layer rootLayer) {
        this.rootLayer = rootLayer;
        layerTree = createCheckBoxTree(rootLayer);
        initSelection(rootLayer);

        transparencySlider = new JSlider(0, 100, 0);
        alphaCompositeBox = new JComboBox(Composite.values());

        JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);
        // todo - may need this? check meaningful alpha composites
        // sliderPanel.add(alphaCompositeBox, BorderLayout.EAST);

        transparencySlider.addChangeListener(new ChangeListener() {
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

        rootLayer.addListener(new AbstractLayerListener() {

        });

        rootLayer.addListener(new LayerStyleListener() {
            public void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent event) {
                if (!adjusting) {
                    TreePath path = layerTree.getSelectionPath();
                    if (path != null) {
                        Layer selectedLayer = getLayer(path);
                        if (selectedLayer == layer) {
                            updateLayerStyleUI(layer);
                        }
                    }
                }
            }
        });

        rootLayer.addListener(new AbstractLayerListener() {
            public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
                if (event.getPropertyName().equals("visible")) {
                    final Boolean oldValue = (Boolean) event.getOldValue();
                    final Boolean newValue = (Boolean) event.getNewValue();
                    if (!adjusting && !oldValue.equals(newValue)) {
                        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) TreeUtils.findTreeNode(layerTree, layer);
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
        final CheckBoxTreeSelectionModel treeSelectionModel = layerTree.getCheckBoxTreeSelectionModel();
        final TreePath path = new TreePath(treeNode.getPath());
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) treeNode.getParent();
        if (selected) {
            treeSelectionModel.addSelectionPath(path);
            if (parent != null) {
                doSelection(parent, selected);
            }
        } else {
            treeSelectionModel.removeSelectionPath(path);
            if (parent != null) {
                boolean oneSelected = false;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
                    if (treeSelectionModel.isPathSelected(new TreePath(child.getPath()))) {
                        oneSelected = true;
                        break;
                    }
                }
                if (!oneSelected) {
                    doSelection(parent, selected);
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

    public static void showLayerManager(final JFrame frame, String title, Layer collectionLayer, Point point) {
        final LayerManager layerManager = new LayerManager(collectionLayer);
        final JDialog lm = new JDialog(frame, title, false);
        lm.getContentPane().add(layerManager.getControl(), BorderLayout.CENTER);
        lm.pack();
        lm.setLocation(point);
        lm.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        lm.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                lm.dispose();
                if (frame != null) {
                    frame.dispose();
                }
            }
        });
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                lm.setVisible(true);
            }
        });
    }


    private CheckBoxTree createCheckBoxTree(Layer rootLayer) {
        LayerTreeModel layerTreeModel = new LayerTreeModel(rootLayer);
        final CheckBoxTree tree = new CheckBoxTree(layerTreeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setDigIn(false);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        final NodeMoveTransferHandler transferHandler = new NodeMoveTransferHandler();
        tree.setTransferHandler(transferHandler);
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                Layer layer = getLayer(event.getPath());
                updateLayerStyleUI(layer);
            }
        });

        final CheckBoxTreeSelectionModel selectionModel = tree.getCheckBoxTreeSelectionModel();
        selectionModel.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                if (!adjusting) {
                    Layer layer = getLayer(event.getPath());
                    layer.setVisible(selectionModel.isPathSelected(event.getPath()));
                }
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getActualCellRenderer();
        renderer.setLeafIcon(IconsFactory.getImageIcon(getClass(), "/org/esa/beam/resources/images/icons/RsBandAsSwath16.gif"));
        renderer.setClosedIcon(IconsFactory.getImageIcon(getClass(), "/org/esa/beam/resources/images/icons/RsGroupClosed16.gif"));
        renderer.setOpenIcon(IconsFactory.getImageIcon(getClass(), "/org/esa/beam/resources/images/icons/RsGroupOpen16.gif"));
        return tree;
    }

    private void initSelection(Layer layer) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) TreeUtils.findTreeNode(layerTree, layer);
        doSelection(treeNode, layer.isVisible());

        final List<Layer> childLayers = layer.getChildLayerList();
        for (Layer childLayer : childLayers) {
            initSelection(childLayer);
        }


    }

}

