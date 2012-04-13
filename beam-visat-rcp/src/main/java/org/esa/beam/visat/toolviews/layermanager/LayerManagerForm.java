/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.TreeCellExtender;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.layer.LayerSourceAssistantPane;
import org.esa.beam.framework.ui.layer.LayerSourceDescriptor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.visat.toolviews.layermanager.layersrc.SelectLayerSourceAssistantPage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;
import java.util.List;

class LayerManagerForm extends AbstractLayerForm {

    private final ProductSceneView view;
    private CheckBoxTree layerTree;
    private JSlider transparencySlider;
    private JPanel control;
    private boolean adjusting;
    private LayerTreeModel layerTreeModel;
    private JLabel transparencyLabel;
    private RemoveLayerAction removeLayerAction;
    private MoveLayerUpAction moveLayerUpAction;
    private MoveLayerDownAction moveLayerDownAction;
    private MoveLayerLeftAction moveLayerLeftAction;
    private MoveLayerRightAction moveLayerRightAction;
    private OpenLayerEditorAction openLayerEditorAction;
    private ZoomToLayerAction zoomToLayerAction;
    private LayerManagerForm.TransparencyChangeListener transparencyChangeListener;

    LayerManagerForm(AppContext appContext, String helpId) {
        super(appContext);
        this.view = appContext.getSelectedProductSceneView();
        transparencyChangeListener = new TransparencyChangeListener();
        initUI(helpId);
    }

    private void initUI(String helpId) {
        layerTreeModel = new LayerTreeModel(view.getRootLayer());
        layerTree = createCheckBoxTree(layerTreeModel);
        layerTree.setCellRenderer(new MyTreeCellRenderer());
        TreeCellExtender.equip(layerTree);

        Hashtable<Integer, JLabel> transparencySliderLabelTable = new Hashtable<Integer, JLabel>();
        transparencySliderLabelTable.put(0, createSliderLabel("0%"));
        transparencySliderLabelTable.put(127, createSliderLabel("50%"));
        transparencySliderLabelTable.put(255, createSliderLabel("100%"));
        transparencySlider = new JSlider(0, 255, 0);
        transparencySlider.setLabelTable(transparencySliderLabelTable);
        transparencySlider.setPaintLabels(true);
        transparencySlider.addChangeListener(new TransparencySliderListener());

        transparencyLabel = new JLabel("Transparency:");

        final JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(transparencyLabel, BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);

        getRootLayer().addListener(new RootLayerListener());

        AbstractButton addButton = createToolButton("icons/Plus24.gif");
        addButton.addActionListener(new AddLayerActionListener());
        removeLayerAction = new RemoveLayerAction(getAppContext());
        AbstractButton removeButton = ToolButtonFactory.createButton(removeLayerAction, false);

        openLayerEditorAction = new OpenLayerEditorAction();
        AbstractButton openButton = ToolButtonFactory.createButton(openLayerEditorAction, false);

        zoomToLayerAction = new ZoomToLayerAction(getAppContext());
        AbstractButton zoomButton = ToolButtonFactory.createButton(zoomToLayerAction, false);

        moveLayerUpAction = new MoveLayerUpAction(getAppContext());
        AbstractButton upButton = ToolButtonFactory.createButton(moveLayerUpAction, false);

        moveLayerDownAction = new MoveLayerDownAction(getAppContext());
        AbstractButton downButton = ToolButtonFactory.createButton(moveLayerDownAction, false);

        moveLayerLeftAction = new MoveLayerLeftAction(getAppContext());
        AbstractButton leftButton = ToolButtonFactory.createButton(moveLayerLeftAction, false);

        moveLayerRightAction = new MoveLayerRightAction(getAppContext());
        AbstractButton rightButton = ToolButtonFactory.createButton(moveLayerRightAction, false);

        AbstractButton helpButton = createToolButton("icons/Help22.png");
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");

        final JPanel actionBar = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        actionBar.add(addButton, gbc);
        gbc.gridy++;
        actionBar.add(removeButton, gbc);
        gbc.gridy++;
        actionBar.add(openButton, gbc);
        gbc.gridy++;
        actionBar.add(zoomButton, gbc);
        gbc.gridy++;
        actionBar.add(upButton, gbc);
        gbc.gridy++;
        actionBar.add(downButton, gbc);
        gbc.gridy++;
        actionBar.add(leftButton, gbc);
        gbc.gridy++;
        actionBar.add(rightButton, gbc);
        gbc.gridy++;
        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        actionBar.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.EAST;
        actionBar.add(helpButton, gbc);


        JPanel layerPanel = new JPanel(new BorderLayout(4, 4));
        layerPanel.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        layerPanel.add(sliderPanel, BorderLayout.SOUTH);

        control = new JPanel(new BorderLayout(4, 4));
        control.add(layerPanel, BorderLayout.CENTER);
        control.add(actionBar, BorderLayout.EAST);

        initLayerTreeVisibility(view.getRootLayer());
        updateFormControl();

        // todo - code duplication in all tool views with help support!!! (nf 200905)
        HelpSys.enableHelpOnButton(helpButton, helpId);
        HelpSys.enableHelpKey(control, helpId);

    }

    private static JLabel createSliderLabel(String text) {
        JLabel label = new JLabel(text);
        Font oldFont = label.getFont();
        Font newFont = oldFont.deriveFont(oldFont.getSize2D() * 0.85f);
        label.setFont(newFont);
        return label;
    }

    public Layer getRootLayer() {
        return view.getRootLayer();
    }

    @Override
    public JComponent getFormControl() {
        return control;
    }

    @Override
    public void updateFormControl() {
        Layer selectedLayer = getSelectedLayer();
        updateLayerStyleUI(selectedLayer);
        updateLayerTreeSelection(selectedLayer);
        boolean isLayerSelected = selectedLayer != null;
        removeLayerAction.setEnabled(isLayerSelected && !isLayerProtected(selectedLayer));
        openLayerEditorAction.setEnabled(isLayerSelected);
        moveLayerUpAction.setEnabled(isLayerSelected && moveLayerUpAction.canMove(selectedLayer));
        moveLayerDownAction.setEnabled(isLayerSelected && moveLayerDownAction.canMove(selectedLayer));
        moveLayerLeftAction.setEnabled(isLayerSelected && moveLayerLeftAction.canMove(selectedLayer));
        moveLayerRightAction.setEnabled(isLayerSelected && moveLayerRightAction.canMove(selectedLayer));
        zoomToLayerAction.setEnabled(isLayerSelected);
    }

    public static boolean isLayerProtected(Layer layer) {
        return isLayerProtectedImpl(layer) || isChildLayerProtected(layer);
    }

    private Layer getSelectedLayer() {
        return view.getSelectedLayer();
    }

    private static boolean isLayerProtectedImpl(Layer layer) {
        return layer.getId().equals(ProductSceneView.BASE_IMAGE_LAYER_ID);
    }

    private static boolean isChildLayerProtected(Layer selectedLayer) {
        Layer[] children = selectedLayer.getChildren().toArray(new Layer[selectedLayer.getChildren().size()]);
        for (Layer childLayer : children) {
            if (isLayerProtectedImpl(childLayer) ||
                    isChildLayerProtected(childLayer)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isLayerNameEditable(Layer layer) {
        if (layer instanceof VectorDataLayer || 
                layer.getConfiguration().isPropertyDefined(MaskLayerType.PROPERTY_NAME_MASK)) {
            return false;
        }
        return true;   
    }

    private static Layer getLayer(TreePath path) {
        if (path == null) {
            return null;
        }
        return (Layer) path.getLastPathComponent();
    }

    private void initLayerTreeVisibility(final Layer layer) {
        updateLayerTreeVisibility(layer);
        for (Layer childLayer : layer.getChildren()) {
            initLayerTreeVisibility(childLayer);
        }
    }

    private void updateLayerTreeVisibility(Layer layer) {
        CheckBoxTreeSelectionModel checkBoxTreeSelectionModel = layerTree.getCheckBoxTreeSelectionModel();
        Layer[] layerPath = LayerUtils.getLayerPath(layerTreeModel.getRootLayer(), layer);
        if (layerPath.length > 0) {
            if (layer.isVisible()) {
                checkBoxTreeSelectionModel.addSelectionPath(new TreePath(layerPath));
            } else {
                checkBoxTreeSelectionModel.removeSelectionPath(new TreePath(layerPath));
            }
            final List<Layer> children = layer.getChildren();
            if (!children.isEmpty()) {
                for (Layer child : children) {
                    updateLayerTreeVisibility(child);
                }
            }
        }
    }

    private void updateLayerTreeSelection(Layer selectedLayer) {
        if (selectedLayer != null) {
            Layer[] layerPath = LayerUtils.getLayerPath(layerTreeModel.getRootLayer(), selectedLayer);
            if (layerPath.length > 0) {
                layerTree.setSelectionPath(new TreePath(layerPath));
            } else {
                layerTree.clearSelection();
            }
        } else {
            layerTree.clearSelection();
        }
    }

    private void updateLayerStyleUI(Layer layer) {
        transparencyLabel.setEnabled(layer != null);
        transparencySlider.setEnabled(layer != null);
        if (layer != null) {
            final double transparency = layer.getTransparency();
            final int n = (int) Math.round(255.0 * transparency);
            transparencySlider.setValue(n);
        }
    }

    private CheckBoxTree createCheckBoxTree(LayerTreeModel treeModel) {

        final CheckBoxTree checkBoxTree = new CheckBoxTree(treeModel) {
            @Override
            public boolean isPathEditable(TreePath path) {
                Layer layer = getLayer(path);
                if (layer != null) {
                    return isLayerNameEditable(layer);
                }
                return false;
            }  
        };
        checkBoxTree.setRootVisible(false);
        checkBoxTree.setShowsRootHandles(true);
        checkBoxTree.setDigIn(false);

        checkBoxTree.setEditable(true);
        checkBoxTree.setDragEnabled(true);
        checkBoxTree.setDropMode(DropMode.ON_OR_INSERT);
        checkBoxTree.setTransferHandler(new LayerTreeTransferHandler(view, checkBoxTree));

        checkBoxTree.getSelectionModel().addTreeSelectionListener(new LayerSelectionListener());

        final CheckBoxTreeSelectionModel checkBoxSelectionModel = checkBoxTree.getCheckBoxTreeSelectionModel();
        checkBoxSelectionModel.addTreeSelectionListener(new CheckBoxTreeSelectionListener());

        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) checkBoxTree.getActualCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        return checkBoxTree;
    }

    private void installTransparencyChangeListener(Layer selectedLayer) {
        selectedLayer.addListener(transparencyChangeListener);
    }

    private void removeTransparencyChangeListener(Layer selectedLayer) {
        selectedLayer.removeListener(transparencyChangeListener);
    }

    public static AbstractButton createToolButton(final String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), false);
    }


    private class RootLayerListener extends AbstractLayerListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            if ("visible".equals(event.getPropertyName())) {
                updateLayerTreeVisibility(layer);
            }
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            for (Layer layer : childLayers) {
                updateLayerTreeVisibility(layer);
                updateLayerTreeSelection(layer);
            }
        }
    }

    private class TransparencySliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {

            TreePath path = layerTree.getSelectionPath();
            if (path != null) {
                Layer layer = getLayer(path);
                adjusting = true;
                layer.setTransparency(transparencySlider.getValue() / 255.0);
                adjusting = false;
            }

        }
    }

    private class AddLayerActionListener implements ActionListener {

        private Rectangle screenBounds;

        @Override
        public void actionPerformed(ActionEvent e) {
            LayerSourceAssistantPane pane = new LayerSourceAssistantPane(SwingUtilities.getWindowAncestor(control),
                                                                         "Add Layer",
                                                                         getAppContext());
            LayerSourceDescriptor[] layerSourceDescriptors = BeamUiActivator.getInstance().getLayerSources();
            pane.show(new SelectLayerSourceAssistantPage(layerSourceDescriptors), screenBounds);
            screenBounds = pane.getWindow().getBounds();
        }
    }

    private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf,
                                                      int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree,
                                                                       value, sel,
                                                                       expanded, leaf, row,
                                                                       hasFocus);
            Layer layer = (Layer) value;
            if (ProductSceneView.BASE_IMAGE_LAYER_ID.equals(layer.getId())) {
                label.setText(String.format("<html><b>%s</b></html>", layer.getName()));
            }
            return label;

        }
    }

    private class TransparencyChangeListener extends AbstractLayerListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            if("transparency".equals(event.getPropertyName())) {
                updateLayerStyleUI(layer);
            }

        }
    }

    private class LayerSelectionListener implements TreeSelectionListener {

        private Layer selectedLayer;

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            if (selectedLayer != null) {
                removeTransparencyChangeListener(selectedLayer);
            }
            selectedLayer = getLayer(event.getNewLeadSelectionPath());
            if (selectedLayer != null) {
                installTransparencyChangeListener(selectedLayer);
            }
            getAppContext().getSelectedProductSceneView().setSelectedLayer(selectedLayer);
        }
    }

    private class CheckBoxTreeSelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            if (!adjusting) {
                TreePath path = event.getPath();
                Layer layer = getLayer(path);
                if (layer.getParent() != null) {
                    boolean pathSelected = ((TreeSelectionModel) event.getSource()).isPathSelected(path);
                    layer.setVisible(pathSelected);
                }
            }
        }
    }
}

