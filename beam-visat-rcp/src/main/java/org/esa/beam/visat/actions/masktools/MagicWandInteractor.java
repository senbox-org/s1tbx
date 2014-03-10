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

package org.esa.beam.visat.actions.masktools;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.swing.figure.ViewportInteractor;
import com.bc.ceres.swing.undo.UndoContext;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;

import static org.esa.beam.visat.actions.masktools.MagicWandModel.MAGIC_WAND_MASK_NAME;

/**
 * An interactor that lets users create masks using a "magic wand".
 * The mask comprises all pixels in the image that are "spectrally" close to the pixel that
 * has been selected using the magic wand.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class MagicWandInteractor extends ViewportInteractor implements MagicWandModel.Listener {

    private static final String DIALOG_TITLE = "Magic Wand Tool";

    private JDialog optionsWindow;
    private final MyLayerListener layerListener;

    private MagicWandModel model;
    private UndoContext undoContext;
    private MagicWandForm form;

    public MagicWandInteractor() {
        layerListener = new MyLayerListener();
        model = new MagicWandModel();
        model.addListener(this);
    }

    @Override
    public void modelChanged(MagicWandModel model, boolean recomputeMask) {
        if (recomputeMask) {
            updateMask();
        }
        if (form != null) {
            form.getBindingContext().adjustComponents();
            form.updateState();
        }
    }

    public Window getOptionsWindow() {
        return optionsWindow;
    }

    static double[] getSpectrum(List<Band> bands, int pixelX, int pixelY) throws IOException {
        final double[] pixel = new double[1];
        final double[] spectrum = new double[bands.size()];

        for (int i = 0; i < bands.size(); i++) {
            final Band band = bands.get(i);
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
        if (!ensureBandNamesSet(view, product)) {
            return;
        }

        List<Band> bands = model.getBands(product);
        if (bands == null) {
            if (!handleInvalidBandFilter(view)) {
                return;
            }
            bands = model.getBands(product);
            if (bands == null) {
                // Should not come here.
                return;
            }
        }

        Point pixelPos = getPixelPos(product, event);
        if (pixelPos == null) {
            return;
        }

        final double[] spectrum;
        try {
            spectrum = getSpectrum(bands, pixelPos.x, pixelPos.y);
        } catch (IOException e1) {
            return;
        }

        MagicWandModel oldModel = getModel().clone();
        getModel().addSpectrum(spectrum);
        MagicWandModel newModel = getModel().clone();

        ensureMaskVisible(view);

        undoContext.postEdit(new MyUndoableEdit(oldModel, newModel));
    }

    private void ensureMaskVisible(ProductSceneView view) {
        Product product = view.getProduct();
        ProductNodeGroup<Mask> overlayMaskGroup = view.getRaster().getOverlayMaskGroup();
        Mask mask = overlayMaskGroup.getByDisplayName(MAGIC_WAND_MASK_NAME);
        if (mask == null) {
            mask = product.getMaskGroup().get(MAGIC_WAND_MASK_NAME);
            if (mask != null) {
                overlayMaskGroup.add(mask);
            }
        }
    }

    private boolean handleInvalidBandFilter(ProductSceneView view) {
        Product product = view.getProduct();
        int resp = VisatApp.getApp().showQuestionDialog(DIALOG_TITLE,
                                                        "The currently selected band filter does not match\n" +
                                                        "the bands of the selected data product.\n\n" +
                                                        "Reset filter and use the ones of the selected product?",
                                                        false,
                                                        "visat.magicWandTool.resetFilter");
        if (resp == JOptionPane.YES_OPTION) {
            model.setBandNames();
            return ensureBandNamesSet(view, product);
        } else {
            return false;
        }
    }

    Point getPixelPos(Product product, MouseEvent event) {
        final Point2D mp = toModelPoint(event);
        final Point2D ip;
        if (product.getGeoCoding() != null) {
            AffineTransform transform = ImageManager.getImageToModelTransform(product.getGeoCoding());
            try {
                ip = transform.inverseTransform(mp, null);
            } catch (NoninvertibleTransformException e) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, "A geographic transformation problem occurred:\n" + e.getMessage());
                return null;
            }
        } else {
            ip = mp;
        }

        final int pixelX = (int) ip.getX();
        final int pixelY = (int) ip.getY();
        if (pixelX < 0
            || pixelY < 0
            || pixelX >= product.getSceneRasterWidth()
            || pixelY >= product.getSceneRasterHeight()) {
            return null;
        }

        return new Point(pixelX, pixelY);
    }

    private boolean ensureBandNamesSet(ProductSceneView view, Product product) {
        if (model.getBandCount() == 0) {
            model.setSpectralBandNames(product);
        }
        if (model.getBandCount() == 0) {
            model.setBandNames(view.getRaster().getName());
        }
        if (model.getBandCount() == 0) {
            // It's actually hard to get here, because we have a selected image view...
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, "No bands selected.");
            return false;
        }
        return true;
    }

    void updateMask() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final Product product = view.getProduct();
            updateMagicWandMask(product);
        }
    }

    private void updateMagicWandMask(Product product) {
        MagicWandModel.setMagicWandMask(product, getModel().createMaskExpression());
    }

    private JDialog createOptionsWindow() {
        form = new MagicWandForm(this);
        JDialog optionsWindow = new JDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, false);
        UIUtils.centerComponent(optionsWindow, VisatApp.getApp().getMainFrame());
        optionsWindow.getContentPane().add(form.createPanel());
        optionsWindow.pack();
        return optionsWindow;
    }

    public MagicWandModel getModel() {
        return model;
    }

    public void setUndoContext(UndoContext undoContext) {
        this.undoContext = undoContext;
    }

    void updateModel(MagicWandModel other) {
        getModel().set(other);
    }


    /**
     * A layer listener that sets the layer for "magic_wand" mask
     * visible, once it is added to the view's layer tree.
     */
    private static class MyLayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            for (Layer childLayer : childLayers) {
                LayerType layerType = childLayer.getLayerType();
                if (layerType instanceof MaskLayerType) {
                    if (childLayer.getName().equals(MAGIC_WAND_MASK_NAME)) {
                        childLayer.setVisible(true);
                    }
                }
            }
        }
    }

    private class MyUndoableEdit extends AbstractUndoableEdit {
        private final MagicWandModel oldModel;
        private final MagicWandModel newModel;

        public MyUndoableEdit(MagicWandModel oldModel, MagicWandModel newModel) {
            this.oldModel = oldModel;
            this.newModel = newModel;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            updateModel(oldModel);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            updateModel(newModel);
        }

        @Override
        public String getPresentationName() {
            return "Modify magic wand mask";
        }
    }

}