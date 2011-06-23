/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.actions.magicstick;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.figure.ViewportInteractor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import static org.esa.beam.visat.actions.magicstick.MagicStickModel.*;

/**
 * An interactor that lets users create masks using a "magic stick".
 * The mask comprises all pixels in the image that are spectrally close to the pixel that
 * has been selected using the magicstick stick.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class MagicStickInteractor extends ViewportInteractor {

    private static final String DIALOG_TITLE = "Magic Stick Options";

    private JDialog optionsWindow;
    private final MyLayerListener layerListener;

    private MagicStickModel model;

    public MagicStickInteractor() {
        layerListener = new MyLayerListener();
        model = new MagicStickModel();
    }

    @Override
    public boolean activate() {
        if (optionsWindow == null) {
            optionsWindow = createOptionsWindow();
        }
        optionsWindow.setVisible(true);

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            view.getRootLayer().addListener(layerListener);
        }

        return super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();

        if (optionsWindow != null) {
            optionsWindow.setVisible(false);
        }

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            view.getRootLayer().removeListener(layerListener);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {

        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view == null) {
            return;
        }
        final Product product = view.getProduct();
        final Band[] spectralBands = getSpectralBands(product);
        if (spectralBands.length == 0) {
            VisatApp.getApp().showErrorDialog("No spectral bands found.");
            return;
        }

        final Point2D mp = this.toModelPoint(event);
        // todo - convert to image point and check against image boundaries! (nf)
        final int pixelX = (int) mp.getX();
        final int pixelY = (int) mp.getY();
        final double[] spectrum;
        try {
            spectrum = getSpectrum(spectralBands, pixelX, pixelY);
        } catch (IOException e1) {
            return;
        }

        model.addSpectrum(spectrum);

        updateMagicStickMask(product, spectralBands);
    }

    private void updateMagicStickMask() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final Product product = view.getProduct();
            updateMagicStickMask(product, MagicStickModel.getSpectralBands(product));
        }
    }

    private void updateMagicStickMask(Product product, Band[] spectralBands) {
        MagicStickModel.setMagicStickMask(product, model.createExpression(spectralBands));
    }

    private JDialog createOptionsWindow() {
        JDialog optionsWindow = new JDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, false);
        UIUtils.centerComponent(optionsWindow, VisatApp.getApp().getMainFrame());
        optionsWindow.getContentPane().add(new MagicStickForm().createPanel());
        optionsWindow.pack();
        return optionsWindow;
    }

    // todo - Move class to top level and also extract MagicStickModel (mode, plusSpectra, minusSpectra, tolerance) (nf)
    class MagicStickForm {
        private JTextField toleranceField;
        private JSlider toleranceSlider;
        boolean adjustingSlider;

        private MagicStickForm() {
        }

        public JPanel createPanel() {

            final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(model));
            bindingContext.addPropertyChangeListener("tolerance", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    adjustSlider();
                    updateMagicStickMask();
                }
            });

            JLabel toleranceLabel = new JLabel("Tolerance:");
            toleranceLabel.setToolTipText("Sets the maximum Euclidian distance tolerated (in units of the spectral bands)");

            toleranceField = new JTextField(10);
            bindingContext.bind("tolerance", toleranceField);
            toleranceField.setText(String.valueOf(model.getTolerance()));

            toleranceSlider = new JSlider(-10, 20);
            toleranceSlider.setSnapToTicks(false);
            toleranceSlider.setPaintTicks(false);
            toleranceSlider.setPaintLabels(false);
            toleranceSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (!adjustingSlider) {
                        bindingContext.getPropertySet().setValue("tolerance", Math.pow(10.0, toleranceSlider.getValue() / 10.0));
                    }
                }
            });

            JRadioButton methodButton1 = new JRadioButton("Distance");
            JRadioButton methodButton2 = new JRadioButton("Average");
            JRadioButton methodButton3 = new JRadioButton("Limits");
            ButtonGroup methodGroup = new ButtonGroup();
            methodGroup.add(methodButton1);
            methodGroup.add(methodButton2);
            methodGroup.add(methodButton3);
            bindingContext.bind("method", methodGroup);
            JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            methodPanel.add(methodButton1);
            methodPanel.add(methodButton2);
            methodPanel.add(methodButton3);
            bindingContext.addPropertyChangeListener("method", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                     updateMagicStickMask();
                }
            });

            JRadioButton operatorButton1 = new JRadioButton("Integral");
            JRadioButton operatorButton2 = new JRadioButton("Identity");
            JRadioButton operatorButton3 = new JRadioButton("Derivative");
            ButtonGroup operatorGroup = new ButtonGroup();
            operatorGroup.add(operatorButton1);
            operatorGroup.add(operatorButton2);
            operatorGroup.add(operatorButton3);
            bindingContext.bind("operator", operatorGroup);
            JPanel operatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            operatorPanel.add(operatorButton1);
            operatorPanel.add(operatorButton2);
            operatorPanel.add(operatorButton3);
            bindingContext.addPropertyChangeListener("operator", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                     updateMagicStickMask();
                }
            });

            final JToggleButton plusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Plus16.gif")));
            plusButton.setToolTipText("Switch to 'plus' mode: Selected spectra will be included in the mask.");
            final JToggleButton minusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Minus16.gif")));
            minusButton.setToolTipText("Switch to 'minus' mode: Selected spectra will be excluded from the mask.");
            plusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    bindingContext.getPropertySet().setValue("mode", plusButton.isSelected() ? Mode.PLUS : Mode.SINGLE);
                }
            });
            minusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    bindingContext.getPropertySet().setValue("mode", plusButton.isSelected() ? Mode.MINUS : Mode.SINGLE);
                }
            });

            bindingContext.addPropertyChangeListener("mode", new PropertyChangeListener() {
                 @Override
                 public void propertyChange(PropertyChangeEvent evt) {
                     plusButton.setSelected(model.getMode() == Mode.PLUS);
                     minusButton.setSelected(model.getMode() == Mode.MINUS);
                 }
             });


            final JButton clearButton = new JButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Remove16.gif")));
            clearButton.setToolTipText("Clears the current mask and removes all spectra collected so far,");
            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.clearSpectra();
                    updateMagicStickMask();
                }
            });

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.add(plusButton);
            toolBar.add(minusButton);
            toolBar.add(clearButton);

            JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
            toolBarPanel.add(toolBar);

            TableLayout tableLayout = new TableLayout(2);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setTableWeightX(1.0);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setCellColspan(1, 0, tableLayout.getColumnCount());
            tableLayout.setCellColspan(2, 0, tableLayout.getColumnCount());
            tableLayout.setCellColspan(3, 0, tableLayout.getColumnCount());
            tableLayout.setCellColspan(4, 0, tableLayout.getColumnCount());

            JPanel panel = new JPanel(tableLayout);
            panel.add(toleranceLabel, new TableLayout.Cell(0, 0));
            panel.add(toleranceField, new TableLayout.Cell(0, 1));
            panel.add(toleranceSlider, new TableLayout.Cell(1, 0));
            panel.add(methodPanel, new TableLayout.Cell(2, 0));
            panel.add(operatorPanel, new TableLayout.Cell(3, 0));
            panel.add(toolBarPanel, new TableLayout.Cell(4, 0));

            adjustSlider();

            return panel;
        }

        private void adjustSlider() {
            adjustingSlider = true;
            toleranceSlider.setValue((int) Math.round(10.0 * Math.log10(model.getTolerance())));
            adjustingSlider = false;
        }

    }

    /**
     * A layer listener that sets the layer for "magic_stick" mask
     * visible, once it is added to the view's layer tree.
     */
    private static class MyLayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            for (Layer childLayer : childLayers) {
                LayerType layerType = childLayer.getLayerType();
                if (layerType instanceof MaskLayerType) {
                    if (childLayer.getName().equals(MAGIC_STICK_MASK_NAME)) {
                        childLayer.setVisible(true);
                    }
                }
            }
        }
    }

}