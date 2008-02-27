package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

abstract class ContinuousForm implements SpecificForm {

    protected final ColorManipulationForm imageForm;
    protected AbstractButton autoStretch95Button;
    protected AbstractButton autoStretch100Button;
    protected AbstractButton zoomInVButton;
    protected AbstractButton zoomOutVButton;
    protected AbstractButton zoomInHButton;
    protected AbstractButton zoomOutHButton;

    protected JPanel contentPanel;
    protected ColourPaletteEditorPanel colorPaletteEditorPanel;

    protected ProductSceneView productSceneView;


    protected ContinuousForm(final ColorManipulationForm imageForm) {
        this.imageForm = imageForm;

        colorPaletteEditorPanel = new ColourPaletteEditorPanel();
        colorPaletteEditorPanel.addPropertyChangeListener(RasterDataNode.PROPERTY_NAME_IMAGE_INFO,
                                                          new PropertyChangeListener() {

                                                              /**
                                                               * This method gets called when a bound property is changed.
                                                               *
                                                               * @param evt A PropertyChangeEvent object describing the event source and the property that has changed.
                                                               */
                                                              public void propertyChange(final PropertyChangeEvent evt) {
                                                                  setApplyEnabled(true);
                                                              }
                                                          });

        autoStretch95Button = createButton("icons/Auto95Percent24.gif");
        autoStretch95Button.setName("AutoStretch95Button");
        autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels"); /*I18N*/
        autoStretch95Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                colorPaletteEditorPanel.compute95Percent();
            }
        });

        autoStretch100Button = createButton("icons/Auto100Percent24.gif");
        autoStretch100Button.setName("AutoStretch100Button");
        autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels"); /*I18N*/
        autoStretch100Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                colorPaletteEditorPanel.compute100Percent();
            }
        });

        zoomInVButton = createButton("icons/ZoomIn24V.gif");
        zoomInVButton.setName("zoomInVButton");
        zoomInVButton.setToolTipText("Stretch histogram vertically"); /*I18N*/
        zoomInVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                colorPaletteEditorPanel.computeZoomInVertical();
            }
        });

        zoomOutVButton = createButton("icons/ZoomOut24V.gif");
        zoomOutVButton.setName("zoomOutVButton");
        zoomOutVButton.setToolTipText("Shrink histogram vertically"); /*I18N*/
        zoomOutVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                colorPaletteEditorPanel.computeZoomOutVertical();
            }
        });

        zoomInHButton = createButton("icons/ZoomIn24H.gif");
        zoomInHButton.setName("zoomInHButton");
        zoomInHButton.setToolTipText("Stretch histogram horizontally"); /*I18N*/
        zoomInHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                colorPaletteEditorPanel.computeZoomInToSliderLimits();
            }
        });

        zoomOutHButton = createButton("icons/ZoomOut24H.gif");
        zoomOutHButton.setName("zoomOutHButton");
        zoomOutHButton.setToolTipText("Shrink histogram horizontally"); /*I18N*/
        zoomOutHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                colorPaletteEditorPanel.computeZoomOutToFullHistogramm();
            }
        });

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(colorPaletteEditorPanel);
    }

    public ImageInfo getCurrentImageInfo() {
        return colorPaletteEditorPanel.getImageInfo();
    }

    // call super!
    public void initProductSceneView(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        this.productSceneView = productSceneView;
    }

    private void setApplyEnabled(boolean b) {
        imageForm.setApplyEnabled(b);
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    protected AbstractButton createToggleButton(String s) {
        return ColorManipulationForm.createToggleButton(s);
    }

    protected AbstractButton createButton(String s) {
        return ColorManipulationForm.createButton(s);
    }

    // super!
    public void releaseProductSceneView() {
        productSceneView = null;
    }

}
