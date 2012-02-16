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
import com.bc.ceres.swing.figure.ViewportInteractor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;

import static org.esa.beam.visat.actions.magicstick.MagicStickModel.*;

/**
 * An interactor that lets users create masks using a "magic stick".
 * The mask comprises all pixels in the image that are spectrally close to the pixel that
 * has been selected using the magic stick.
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

    static double[] getSpectrum(Band[] bands, int pixelX, int pixelY) throws IOException {
        final double[] pixel = new double[1];
        final double[] spectrum = new double[bands.length];

        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            band.readPixels(pixelX, pixelY, 1, 1, pixel, com.bc.ceres.core.ProgressMonitor.NULL);
            final double value;
            if (band.isPixelValid(pixelX, pixelY)) {
                value = pixel[0];
            } else {
                value = Double.NaN;
            }
            spectrum[i] = value;
        }
        return spectrum;
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

        final Point2D mp = toModelPoint(event);
        // todo - convert to image point and check against image boundaries! (nf)
        final int pixelX = (int) mp.getX();
        final int pixelY = (int) mp.getY();
        final double[] spectrum;
        try {
            spectrum = getSpectrum(spectralBands, pixelX, pixelY);
        } catch (IOException e1) {
            return;
        }

        getModel().addSpectrum(spectrum);

        updateMagicStickMask(product, spectralBands);
    }

    void updateMagicStickMask() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final Product product = view.getProduct();
            updateMagicStickMask(product, MagicStickModel.getSpectralBands(product));
        }
    }

    private void updateMagicStickMask(Product product, Band[] spectralBands) {
        MagicStickModel.setMagicStickMask(product, getModel().createExpression(spectralBands));
    }

    private JDialog createOptionsWindow() {
        JDialog optionsWindow = new JDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, false);
        UIUtils.centerComponent(optionsWindow, VisatApp.getApp().getMainFrame());
        optionsWindow.getContentPane().add(new MagicStickForm(this).createPanel());
        optionsWindow.pack();
        return optionsWindow;
    }

    public MagicStickModel getModel() {
        return model;
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