package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandGraphicalForm extends AbstractContinuousGraphicalForm {

    private AbstractButton evenDistButton;

    public Continuous1BandGraphicalForm(final ColorManipulationForm imageForm) {
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

    public void performReset(ProductSceneView productSceneView) {
        resetDefaultValues(productSceneView.getRaster());
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        setCurrentImageInfo(productSceneView.getRaster().getImageInfo());
    }

    private void setApplyEnabled(boolean b) {
        parentForm.setApplyEnabled(b);
    }

    private void distributeSlidersEvenly() {
        paletteEditor.distributeSlidersEvenly();
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

    public void updateState(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        paletteEditor.setUnit(productSceneView.getRaster().getUnit());
        paletteEditor.setRGBColor(null);
        parentForm.revalidate();
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        if (imageInfo != null) {
            paletteEditor.setImageInfo(imageInfo.createDeepCopy());
        } else {
            paletteEditor.setImageInfo(null);
        }
        setApplyEnabled(false);
    }

    public void performApply(ProductSceneView productSceneView) {
        productSceneView.getRaster().setImageInfo(paletteEditor.getImageInfo());
    }

    public void handleFormHidden() {
    }

    public String getTitle(ProductSceneView productSceneView) {
        return productSceneView.getRaster().getDisplayName();
    }
}