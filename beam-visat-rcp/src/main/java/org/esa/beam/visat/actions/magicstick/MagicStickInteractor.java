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

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.figure.ViewportInteractor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.visat.VisatApp;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;

import static org.esa.beam.visat.actions.magicstick.MagicStickUtils.*;

/**
 * An interactor that lets users create masks using a "magic stick".
 * The mask comprises all pixels in the image that are spectrally close to the pixel that
 * has been selected using the magicstick stick.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class MagicStickInteractor extends ViewportInteractor {

    private static final String DIALOG_TITLE = "Magic Stick";
    private JDialog parameterWindow;
    private final MyLayerListener layerListener;

    private Mode mode;
    private ArrayList<double[]> plusSpectra;
    private ArrayList<double[]> minusSpectra;
    private double tolerance;

    public MagicStickInteractor() {
        layerListener = new MyLayerListener();

        mode = Mode.SINGLE;
        plusSpectra = new ArrayList<double[]>();
        minusSpectra = new ArrayList<double[]>();
        tolerance = 0.1;
    }

    @Override
    public boolean activate() {
        if (parameterWindow == null) {
            parameterWindow = createParameterWindow();
        }
        parameterWindow.setVisible(true);

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            view.getRootLayer().addListener(layerListener);
        }

        return super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();

        if (parameterWindow != null) {
            parameterWindow.setVisible(false);
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

        if (mode == Mode.SINGLE) {
            plusSpectra.clear();
            minusSpectra.clear();
            plusSpectra.add(spectrum);
        } else if (mode == Mode.PLUS) {
            plusSpectra.add(spectrum);
        } else if (mode == Mode.MINUS) {
            minusSpectra.add(spectrum);
        }

        final String expression = createExpression(spectralBands, plusSpectra, minusSpectra, tolerance);

        setMagicStickMask(product, expression);
    }

    private JDialog createParameterWindow() {
        JDialog parameterWindow = new JDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, false);
        UIUtils.centerComponent(parameterWindow, VisatApp.getApp().getMainFrame());
        parameterWindow.getContentPane().add(new MagicStickForm().createPanel());
        parameterWindow.pack();
        return parameterWindow;
    }


    class MagicStickForm {
        private JTextField toleranceField;

        private MagicStickForm() {
        }

        public JPanel createPanel() {
            JLabel toleranceLabel = new JLabel("Tolerance:");
            toleranceLabel.setToolTipText("Sets the maximum Euclidian distance tolerated (in units of the spectral bands)");

            toleranceField = new JTextField(10);
            toleranceField.setText(String.valueOf(tolerance));
            toleranceField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateTolerance();
                }
            });
            toleranceField.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                }

                public void focusLost(FocusEvent e) {
                    updateTolerance();
                }
            });

            final JToggleButton plusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Plus16.gif")));
            final JToggleButton minusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Minus16.gif")));
            plusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (plusButton.isSelected()) {
                        minusButton.setSelected(false);
                        mode = Mode.PLUS;
                    } else {
                        mode = Mode.SINGLE;
                    }
                    System.out.println("mode = " + mode);
                }
            });
            minusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (minusButton.isSelected()) {
                        plusButton.setSelected(false);
                        mode = Mode.MINUS;
                    } else {
                        mode = Mode.SINGLE;
                    }
                    System.out.println("mode = " + mode);
                }
            });
            final JButton clearButton = new JButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Remove16.gif")));
            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
                    if (view == null) {
                        return;
                    }
                    final Product product = view.getProduct();
                    setMagicStickMask(product, "0");
                    plusSpectra.clear();
                    minusSpectra.clear();
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
            buttonPanel.add(plusButton);
            buttonPanel.add(minusButton);
            buttonPanel.add(clearButton);

            final TableLayout tableLayout = new TableLayout(2);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setTableWeightX(1.0);
            tableLayout.setCellColspan(1, 0, 3);
            tableLayout.setTablePadding(4, 4);
            JPanel panel = new JPanel(tableLayout);
            panel.add(toleranceLabel);
            panel.add(toleranceField);
            panel.add(buttonPanel);
            return panel;
        }

        private void updateTolerance() {
            try {
                tolerance = Double.parseDouble(toleranceField.getText());
            } catch (NumberFormatException e) {
                toleranceField.setText(String.valueOf(tolerance));
            }
        }

    }

    /**
     * A layer listener that sets the layer for "magic_stick" mask
     * visible, once it is added to the view's layer tree.
     */
    private static class MyLayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            for (int i = 0; i < childLayers.length; i++) {
                Layer childLayer = childLayers[i];
                LayerType layerType = childLayer.getLayerType();
                if (layerType instanceof MaskLayerType) {
                    if (childLayer.getName().equals(MAGIC_STICK_MASK_NAME)) {
                        childLayer.setVisible(true);
                    }
                }
            }
        }
    }

    private enum Mode {
        SINGLE,
        PLUS,
        MINUS,
    }
}