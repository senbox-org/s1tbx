package org.esa.beam.visat.toolviews.imageinfo;

import javax.swing.AbstractButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ImageInfoEditorSupport {

    final AbstractButton autoStretch95Button;
    final AbstractButton autoStretch100Button;
    final AbstractButton zoomInVButton;
    final AbstractButton zoomOutVButton;
    final AbstractButton zoomInHButton;
    final AbstractButton zoomOutHButton;
    final AbstractButton showExtraInfoButton;

    protected ImageInfoEditorSupport(final ImageInfoEditor2 imageInfoEditor) {

        autoStretch95Button = createButton("icons/Auto95Percent24.gif");
        autoStretch95Button.setName("AutoStretch95Button");
        autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels"); /*I18N*/
        autoStretch95Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageInfoEditor.compute95Percent();
            }
        });

        autoStretch100Button = createButton("icons/Auto100Percent24.gif");
        autoStretch100Button.setName("AutoStretch100Button");
        autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels"); /*I18N*/
        autoStretch100Button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageInfoEditor.compute100Percent();
            }
        });

        zoomInVButton = createButton("icons/ZoomIn24V.gif");
        zoomInVButton.setName("zoomInVButton");
        zoomInVButton.setToolTipText("Stretch histogram vertically"); /*I18N*/
        zoomInVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageInfoEditor.computeZoomInVertical();
            }
        });

        zoomOutVButton = createButton("icons/ZoomOut24V.gif");
        zoomOutVButton.setName("zoomOutVButton");
        zoomOutVButton.setToolTipText("Shrink histogram vertically"); /*I18N*/
        zoomOutVButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageInfoEditor.computeZoomOutVertical();
            }
        });

        zoomInHButton = createButton("icons/ZoomIn24H.gif");
        zoomInHButton.setName("zoomInHButton");
        zoomInHButton.setToolTipText("Stretch histogram horizontally"); /*I18N*/
        zoomInHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageInfoEditor.computeZoomInToSliderLimits();
            }
        });

        zoomOutHButton = createButton("icons/ZoomOut24H.gif");
        zoomOutHButton.setName("zoomOutHButton");
        zoomOutHButton.setToolTipText("Shrink histogram horizontally"); /*I18N*/
        zoomOutHButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                imageInfoEditor.computeZoomOutToFullHistogramm();
            }
        });

        showExtraInfoButton = createToggleButton("icons/Information24.gif");
        showExtraInfoButton.setName("ShowExtraInfoButton");
        showExtraInfoButton.setToolTipText("Show extra information");
        showExtraInfoButton.setSelected(imageInfoEditor.getShowExtraInfo());
        showExtraInfoButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                imageInfoEditor.setShowExtraInfo(showExtraInfoButton.isSelected());
            }
        });
    }

    public static AbstractButton createToggleButton(String s) {
        return ColorManipulationForm.createToggleButton(s);
    }

    public static AbstractButton createButton(String s) {
        return ColorManipulationForm.createButton(s);
    }
}
