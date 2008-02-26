package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandForm extends ContinuousForm {

    private AbstractButton evenDistButton;

    public Continuous1BandForm(final ImageInterpretationForm imageForm) {
        super(imageForm);
        evenDistButton = createButton("icons/EvenDistribution24.gif");
        evenDistButton.setName("evenDistButton");
        evenDistButton.setToolTipText("Distribute sliders evenly between first and last slider"); /*I18N*/
        evenDistButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                distributeSlidersEvenly();
            }
        });
    }

    public void reset() {
        final RasterDataNode raster = productSceneView.getRaster();
        colorPaletteEditorPanel.resetDefaultValues(raster);
    }

    @Override
    public void initProductSceneView(ProductSceneView productSceneView) {
        super.initProductSceneView(productSceneView);
        setCurrentImageInfo(this.productSceneView.getRaster().getImageInfo());
    }

    private void setApplyEnabled(boolean b) {
        imageForm.setApplyEnabled(b);
    }

    private void distributeSlidersEvenly() {
        colorPaletteEditorPanel.distributeSlidersEvenly();
    }


    public AbstractButton[] getButtons() {
        return new AbstractButton[]{
                autoStretch95Button,
                autoStretch100Button,
                zoomInVButton,
                zoomOutVButton,
                zoomInHButton,
                zoomOutHButton,
                evenDistButton,
        };


    }

    public void updateState() {
        colorPaletteEditorPanel.setUnit(productSceneView.getRaster().getUnit());
        colorPaletteEditorPanel.setRGBColor(null);
        imageForm.revalidate();
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        if (imageInfo != null) {
            colorPaletteEditorPanel.setImageInfo(imageInfo.createDeepCopy());
        } else {
            colorPaletteEditorPanel.setImageInfo(null);
        }
        setApplyEnabled(false);
    }

    public void apply() {
        productSceneView.getRaster().setImageInfo(colorPaletteEditorPanel.getImageInfo());
        VisatApp.getApp().updateImage(productSceneView);
    }

    @Override
    public void releaseProductSceneView() {
        super.releaseProductSceneView();
    }

    public String getTitle() {
        return productSceneView.getRaster().getDisplayName();
    }
}