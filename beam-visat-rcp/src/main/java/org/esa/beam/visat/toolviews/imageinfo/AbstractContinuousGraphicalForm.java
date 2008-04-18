package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

abstract class AbstractContinuousGraphicalForm implements PaletteEditorForm {

    protected final ColorManipulationForm parentForm;
    protected AbstractButton autoStretch95Button;
    protected AbstractButton autoStretch100Button;
    protected AbstractButton zoomInVButton;
    protected AbstractButton zoomOutVButton;
    protected AbstractButton zoomInHButton;
    protected AbstractButton zoomOutHButton;
    protected JPanel contentPanel;
    protected GraphicalPaletteEditor paletteEditor;

    protected AbstractContinuousGraphicalForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        paletteEditor = new GraphicalPaletteEditor();
        paletteEditor.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                parentForm.setApplyEnabled(true);
            }
        });

        autoStretch95Button = createButton("icons/Auto95Percent24.gif");
        autoStretch95Button.setName("AutoStretch95Button");
        autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels"); /*I18N*/
        autoStretch95Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                paletteEditor.compute95Percent();
            }
        });

        autoStretch100Button = createButton("icons/Auto100Percent24.gif");
        autoStretch100Button.setName("AutoStretch100Button");
        autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels"); /*I18N*/
        autoStretch100Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                paletteEditor.compute100Percent();
            }
        });

        zoomInVButton = createButton("icons/ZoomIn24V.gif");
        zoomInVButton.setName("zoomInVButton");
        zoomInVButton.setToolTipText("Stretch histogram vertically"); /*I18N*/
        zoomInVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                paletteEditor.computeZoomInVertical();
            }
        });

        zoomOutVButton = createButton("icons/ZoomOut24V.gif");
        zoomOutVButton.setName("zoomOutVButton");
        zoomOutVButton.setToolTipText("Shrink histogram vertically"); /*I18N*/
        zoomOutVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                paletteEditor.computeZoomOutVertical();
            }
        });

        zoomInHButton = createButton("icons/ZoomIn24H.gif");
        zoomInHButton.setName("zoomInHButton");
        zoomInHButton.setToolTipText("Stretch histogram horizontally"); /*I18N*/
        zoomInHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                paletteEditor.computeZoomInToSliderLimits();
            }
        });

        zoomOutHButton = createButton("icons/ZoomOut24H.gif");
        zoomOutHButton.setName("zoomOutHButton");
        zoomOutHButton.setToolTipText("Shrink histogram horizontally"); /*I18N*/
        zoomOutHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                paletteEditor.computeZoomOutToFullHistogramm();
            }
        });

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(paletteEditor);
    }

    public ImageInfo getCurrentImageInfo() {
        return paletteEditor.getImageInfo();
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

    public void resetDefaultValues(RasterDataNode raster) {
        ImageInfo imageInfoRaster = raster.getImageInfo();
        raster.setImageInfo(null);
        final ImageInfo newValue = ensureValidImageInfo(raster);
        raster.setImageInfo(imageInfoRaster);
        paletteEditor.setImageInfo(newValue);
    }

    public ImageInfo ensureValidImageInfo(RasterDataNode raster) {
        try {
            return raster.ensureValidImageInfo();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                                          "Failed to create image information for '" +
                                                  raster.getName() + "':\n" +e.getMessage(),
                                          "I/O Error",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


}
