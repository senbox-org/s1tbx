package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.visat.VisatApp;

import java.awt.*;
import java.io.IOException;

import com.bc.view.ViewModel;

public class CreateSubsetFromViewAction extends ExecCommand {

    private int subsetNumber;
    private static final String DIALOG_TITLE = "Create Subset from View";

    @Override
    public void actionPerformed(CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();
        final ProductSceneView psv = visatApp.getSelectedProductSceneView();
        if (psv != null) {
            final Rectangle bounds = computeArea(psv);
            final String propertyName = null;
            if (bounds == null) {
                visatApp.showInfoDialog(DIALOG_TITLE,
                        "The viewing area is completely outside of the product bounds.", /*I18N*/
                        propertyName);
                return;
            }

            final Product sourceProduct = psv.getProduct();
            final String subsetName = "subset_"+subsetNumber+"_of_" + sourceProduct.getName();
            final ProductSubsetDef initSubset = new ProductSubsetDef();
            initSubset.setRegion(bounds);
            initSubset.setNodeNames(sourceProduct.getBandNames());
            initSubset.addNodeNames(sourceProduct.getTiePointGridNames());
            initSubset.setIgnoreMetadata(false);
            final ProductSubsetDialog subsetDialog = new ProductSubsetDialog(visatApp.getMainFrame(),
                    sourceProduct, initSubset);
            if (subsetDialog.show() != ProductSubsetDialog.ID_OK) {
                return;
            }
            final ProductSubsetDef subsetDef = subsetDialog.getProductSubsetDef();
            if (subsetDef == null) {
                visatApp.showInfoDialog(DIALOG_TITLE,
                        "The selected subset is equal to the entire product.\n" + /*I18N*/
                        "So no subset was created.",
                        propertyName);
                return;
            }
            try {
                final Product subset = sourceProduct.createSubset(subsetDef, subsetName,
                        sourceProduct.getDescription());
                visatApp.getProductManager().addProduct(subset);
                subsetNumber++;
            } catch (IOException e) {
                visatApp.showInfoDialog(DIALOG_TITLE,
                        "Unable to create the product subset because\n" + /*I18N*/
                        "an I/O error occures at creating the subset.",
                        propertyName); /*I18N*/
            }
        }
    }

    public void updateState(CommandEvent event) {
        event.getCommand().setEnabled(VisatApp.getApp().getSelectedProductSceneView() != null);
    }
        private Rectangle computeArea(final ProductSceneView psv) {
        final ImageDisplay imageDisplay = psv.getImageDisplay();
        final int imageWidth = imageDisplay.getImageWidth();
        final int imageHeight = imageDisplay.getImageHeight();
        final ViewModel viewModel = imageDisplay.getViewModel();
        int width = (int) Math.round(imageDisplay.getWidth() / viewModel.getViewScale());
        int height = (int) Math.round(imageDisplay.getHeight() / viewModel.getViewScale());
        int x = (int) Math.round(viewModel.getModelOffsetX());
        int y = (int) Math.round(viewModel.getModelOffsetY());
        if (x < 0) {
            width += x;
            x = 0;
        }
        if (y < 0) {
            height += y;
            y = 0;
        }
        Rectangle bounds = null;
        if (x <= imageWidth && y <= imageHeight && width >= 1 && height >= 1) {
            final int xMax = x + width;
            if (xMax > imageWidth) {
                width += imageWidth - xMax;
                x = imageWidth - width;
            }
            final int yMax = y + height;
            if (yMax > imageHeight) {
                height += imageHeight - yMax;
                y = imageHeight - height;
            }
            bounds = new Rectangle(x, y, width, height);
        }
        return bounds;
    }
}
