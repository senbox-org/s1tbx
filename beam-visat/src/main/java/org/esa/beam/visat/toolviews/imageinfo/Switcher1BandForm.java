package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Switcher1BandForm implements SpecificForm {

    private SpecificForm currentForm;
    private JPanel contentPanel;
    private JRadioButton continuousButton;
    private JRadioButton discreteButton;
    private SpecificForm discrete1BandForm;
    private SpecificForm continuous1BandForm;
    private final ImageInterpretationForm imageForm;
    private ProductSceneView productSceneView;

    protected Switcher1BandForm(final ImageInterpretationForm imageForm) {
        this.imageForm = imageForm;
        currentForm = EmptyForm.INSTANCE;

        continuousButton = new JRadioButton("Continuous palette");
        discreteButton = new JRadioButton("Discrete palette");
        final ButtonGroup group = new ButtonGroup();
        group.add(continuousButton);
        group.add(discreteButton);
        final SwitcherActionListener switcherActionListener = new SwitcherActionListener();
        continuousButton.addActionListener(switcherActionListener);
        discreteButton.addActionListener(switcherActionListener);
        final JPanel switcherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        switcherPanel.add(continuousButton);
        switcherPanel.add(discreteButton);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(switcherPanel, BorderLayout.NORTH);
    }

    public void reset() {
        currentForm.reset();
    }

    public ImageInfo getCurrentImageInfo() {
        return currentForm.getCurrentImageInfo();
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        switchForm(imageInfo);
        currentForm.setCurrentImageInfo(imageInfo);
    }

    public void initProductSceneView(ProductSceneView productSceneView) {
        this.productSceneView = productSceneView;
        final ImageInfo imageInfo = productSceneView.getRaster().getImageInfo();
        setCurrentImageInfo(imageInfo);
        updateState();
    }

    private void switchForm(ImageInfo imageInfo) {
        final SpecificForm oldForm = currentForm;
        final SpecificForm newForm;
        if (imageInfo.getColorPaletteDef().isDiscrete()) {
            if (discrete1BandForm == null) {
                discrete1BandForm = new DiscreteForm();
            }
            newForm = discrete1BandForm;
        } else {
            if (continuous1BandForm == null) {
                continuous1BandForm = new Continuous1BandForm(imageForm);
            }
            newForm = continuous1BandForm;
        }
        if (oldForm != newForm) {
            oldForm.releaseProductSceneView();

            currentForm = newForm;
            currentForm.initProductSceneView(productSceneView);

            contentPanel.remove(oldForm.getContentPanel());
            contentPanel.add(currentForm.getContentPanel(), BorderLayout.CENTER);

            imageForm.installSpecificFormUI();
        }
    }

    public AbstractButton[] getButtons() {
        return currentForm.getButtons();
    }

    public void updateState() {
        continuousButton.setSelected(currentForm == continuous1BandForm);
        discreteButton.setSelected(currentForm == discrete1BandForm);
        currentForm.updateState();
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public void apply() {
        currentForm.apply();
    }

    public String getTitle() {
        return currentForm.getTitle();
    }

    public void releaseProductSceneView() {
        currentForm.releaseProductSceneView();
    }

    private class SwitcherActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final ImageInfo imageInfoOld = getCurrentImageInfo();
            final ColorPaletteDef colorPaletteDef = new ColorPaletteDef(imageInfoOld.getColorPaletteDef().getPoints(), discreteButton.isSelected());
            final ImageInfo imageInfoNew = new ImageInfo(imageInfoOld.getMinSample(),
                                                         imageInfoOld.getMaxSample(),
                                                         imageInfoOld.getHistogramBins(),
                                                         colorPaletteDef);
            System.out.println("imageInfoNew = " + imageInfoNew);
            // setCurrentImageInfo(imageInfoNew);
        }
    }
}
