package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Continuous1BandGraphicalForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final ImageInfoEditor imageInfoEditor;
    private final ImageInfoEditorSupport imageInfoEditorSupport;
    private final JPanel contentPanel;
    private final AbstractButton evenDistButton;
    private final MoreOptionsForm moreOptionsForm;
    private ChangeListener applyEnablerCL;

    public Continuous1BandGraphicalForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        imageInfoEditor = new ImageInfoEditor();
        imageInfoEditorSupport = new ImageInfoEditorSupport(imageInfoEditor);
        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(imageInfoEditor, BorderLayout.CENTER);
        moreOptionsForm = new MoreOptionsForm(parentForm, true);

        evenDistButton = ImageInfoEditorSupport.createButton("icons/EvenDistribution24.gif");
        evenDistButton.setName("evenDistButton");
        evenDistButton.setToolTipText("Distribute sliders evenly between first and last slider"); /*I18N*/
        evenDistButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                distributeSlidersEvenly();
            }
        });

        applyEnablerCL = parentForm.createApplyEnablerChangeListener();
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public ImageInfoEditor getImageInfoEditor() {
        return imageInfoEditor;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        if (imageInfoEditor.getModel() != null) {
            imageInfoEditor.getModel().removeChangeListener(applyEnablerCL);
            imageInfoEditor.setModel(null);
        }
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        ImageInfoEditorModel1B model = new ImageInfoEditorModel1B(parentForm.getImageInfo());
        model.setDisplayProperties(productSceneView.getRaster());
        model.addChangeListener(applyEnablerCL);
        imageInfoEditor.setModel(model);
        parentForm.revalidateToolViewPaneControl();
    }


    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        imageInfoEditor.getModel().setDisplayProperties(raster);
        if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STATISTICS)) {
            imageInfoEditor.compute100Percent();
        }
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getProductSceneView().getRasters();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    private void distributeSlidersEvenly() {
        imageInfoEditor.distributeSlidersEvenly();
    }


    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                imageInfoEditorSupport.autoStretch95Button,
                imageInfoEditorSupport.autoStretch100Button,
                imageInfoEditorSupport.zoomInVButton,
                imageInfoEditorSupport.zoomOutVButton,
                imageInfoEditorSupport.zoomInHButton,
                imageInfoEditorSupport.zoomOutHButton,
                evenDistButton,
        };
    }
}