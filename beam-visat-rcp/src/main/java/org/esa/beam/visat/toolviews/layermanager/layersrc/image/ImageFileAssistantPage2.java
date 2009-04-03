package org.esa.beam.visat.toolviews.layermanager.layersrc.image;


import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.jidesoft.swing.JideSplitButton;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.TransformException;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.concurrent.ExecutionException;

class ImageFileAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private JTextField[] numberFields;
    private Envelope2D layerEnvelope;
    private Envelope imageEnvelope;

    ImageFileAssistantPage2() {
        super("Edit Affine Transformation");
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 2);

        double[] flatmatrix = new double[6];
        numberFields = new JTextField[flatmatrix.length];
        AffineTransform transform = (AffineTransform) getContext().getPropertyValue(
                ImageFileLayerSource.PROPERTY_WORLD_TRANSFORM);
        transform.getMatrix(flatmatrix);

// see http://support.esri.com/index.cfm?fa=knowledgebase.techarticles.articleShow&d=17489
        String[] labels = new String[]{
                "X-dimension of a pixel in map units: ",
                "Rotation parameter for row: ",
                "Rotation parameter for column: ",
                "Negative of Y-dimension of a pixel in map units: ",
                "X-coordinate of center of upper left pixel: ",
                "Y-coordinate of centre of upper left pixel: "
        };
        numberFields[0] = addRow(panel, labels[0], gbc, null);
        numberFields[0].setText(String.valueOf(flatmatrix[0]));
        numberFields[1] = addRow(panel, labels[1], gbc, null);
        numberFields[1].setText(String.valueOf(flatmatrix[1]));
        numberFields[2] = addRow(panel, labels[2], gbc, null);
        numberFields[2].setText(String.valueOf(flatmatrix[2]));
        numberFields[3] = addRow(panel, labels[3], gbc, null);
        numberFields[3].setText(String.valueOf(flatmatrix[3]));

        final JideSplitButton horizButton = createAlignButton(new AlignLeftAction(),
                                                              new AlignCenterAction(),
                                                              new AlignRightAction());
        numberFields[4] = addRow(panel, labels[4], gbc, horizButton);
        numberFields[4].setText(String.valueOf(flatmatrix[4]));

        final JideSplitButton vertButton = createAlignButton(new AlignUpAction(),
                                                             new AlignMiddleAction(),
                                                             new AlignDownAction());
        numberFields[5] = addRow(panel, labels[5], gbc, vertButton);
        numberFields[5].setText(String.valueOf(flatmatrix[5]));

        return panel;
    }
    
    @Override
    public boolean validatePage() {
        try {
            return createTransform().getDeterminant() != 0.0;
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public boolean performFinish() {
        AffineTransform transform = createTransform();
        final LayerSourcePageContext context = getContext();
        context.setPropertyValue(ImageFileLayerSource.PROPERTY_WORLD_TRANSFORM, transform);
        return ImageFileLayerSource.insertImageLayer(context);
    }

    private JideSplitButton createAlignButton(Action... actions) {
        Assert.argument(actions.length >= 1, "actions.length >= 1");
        final JideSplitButton splitButton = new JideSplitButton();
        splitButton.setHideActionText(true);
        splitButton.setAlwaysDropdown(true);
        splitButton.setAction(actions[0]);
        for (Action action : actions) {
            final JMenuItem menuItem = new JMenuItem(action);
            splitButton.add(menuItem);
            addActionListener(menuItem, splitButton);
        }
        return splitButton;
    }


    private void addActionListener(final JMenuItem leftMenuItem, final JideSplitButton horizButton) {
        leftMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                horizButton.setAction(leftMenuItem.getAction());
            }
        });
    }

    private JTextField addRow(JPanel panel, String label, GridBagConstraints gbc, AbstractButton button) {
        gbc.gridy++;

        gbc.weightx = 0.2;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.weightx = 0.8;
        gbc.gridx = 1;
        final JTextField fileField = new JTextField(12);
        fileField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(fileField, gbc);
        fileField.getDocument().addDocumentListener(new MyDocumentListener());
        if (button != null) {
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            panel.add(button, gbc);
        }
        return fileField;
    }

    private AffineTransform createTransform() {
        double[] flatmatrix = new double[numberFields.length];
        for (int i = 0; i < flatmatrix.length; i++) {
            flatmatrix[i] = Double.parseDouble(getText(numberFields[i]));
        }
        return new AffineTransform(flatmatrix);
    }

    private synchronized Envelope getImageEnvelope() {
        if (imageEnvelope == null) {
            ProductSceneView view1 = getContext().getAppContext().getSelectedProductSceneView();
            try {
                RenderedImage image = (RenderedImage) getContext().getPropertyValue(
                        ImageFileLayerSource.PROPERTY_IMAGE);
                GeoCoding geoCoding = view1.getRaster().getGeoCoding();
                final Rectangle2D.Double imageBounds = new Rectangle2D.Double(0, 0, image.getWidth(),
                                                                              image.getHeight());
                imageEnvelope = CRS.transform(new Envelope2D(geoCoding.getGridCRS(), imageBounds),
                                              geoCoding.getModelCRS());
            } catch (TransformException e) {
                throw new IllegalStateException("Not able to transform image.", e.getCause());
            }
        }
        return imageEnvelope;
    }

    private synchronized Envelope2D getLayerEnvelope() {
        if (layerEnvelope == null) {
            final ProductSceneView view = getContext().getAppContext().getSelectedProductSceneView();
            final GeoCoding geoCoding = view.getRaster().getGeoCoding();
            final Layer layer = getContext().getLayerContext().getRootLayer();
            layerEnvelope = new Envelope2D(geoCoding.getModelCRS(), layer.getModelBounds());
        }
        return layerEnvelope;
    }

    private String getText(JTextComponent textComponent) {
        String s = textComponent.getText();
        return s != null ? s.trim() : "";
    }

    private class MyDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            getContext().updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            getContext().updateState();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            getContext().updateState();
        }
    }


    private class AlignUpAction extends AbstractAction {


        private AlignUpAction() {
            putValue(NAME, "Up");
            putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
            putValue(SMALL_ICON, UIUtils.loadImageIcon("icons/AlignUp24.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<String, String> worker = new AlignmentSwingWorker(numberFields[5]) {
                @Override
                protected String doInBackground() throws Exception {
                    Envelope layerEnv = getLayerEnvelope();
                    final CoordinateSystem layerCS = layerEnv.getCoordinateReferenceSystem().getCoordinateSystem();
                    final AxisDirection layerYDirection = layerCS.getAxis(1).getDirection();
                    double value;
                    if (layerYDirection.compareTo(AxisDirection.DISPLAY_DOWN) == 0) {
                        value = layerEnv.getMinimum(1);
                    } else {
                        value = layerEnv.getMaximum(1);
                    }
                    return String.valueOf(value);
                }
            };
            worker.execute();
        }
    }

    private class AlignMiddleAction extends AbstractAction {

        private AlignMiddleAction() {
            putValue(NAME, "Middle");
            putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
            putValue(SMALL_ICON, UIUtils.loadImageIcon("icons/AlignMiddle24.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<String, String> worker = new AlignmentSwingWorker(numberFields[5]) {
                @Override
                protected String doInBackground() throws Exception {
                    Envelope layerEnv = getLayerEnvelope();
                    Envelope imageEnv = getImageEnvelope();
                    final CoordinateSystem layerCS = layerEnv.getCoordinateReferenceSystem().getCoordinateSystem();
                    final AxisDirection layerYDirection = layerCS.getAxis(1).getDirection();
                    double value;
                    if (layerYDirection.compareTo(AxisDirection.DISPLAY_DOWN) == 0) {
                        value = layerEnv.getMedian(1) - imageEnv.getSpan(1) / 2;
                    } else {
                        value = layerEnv.getMedian(1) + imageEnv.getSpan(1) / 2;
                    }
                    return String.valueOf(value);
                }
            };
            worker.execute();
        }
    }

    private class AlignDownAction extends AbstractAction {

        private AlignDownAction() {
            putValue(NAME, "Down");
            putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
            putValue(SMALL_ICON, UIUtils.loadImageIcon("icons/AlignDown24.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<String, String> worker = new AlignmentSwingWorker(numberFields[5]) {
                @Override
                protected String doInBackground() throws Exception {
                    Envelope layerEnv = getLayerEnvelope();
                    Envelope imageEnv = getImageEnvelope();
                    final CoordinateSystem layerCS = layerEnv.getCoordinateReferenceSystem().getCoordinateSystem();
                    final AxisDirection layerYDirection = layerCS.getAxis(1).getDirection();
                    double value;
                    if (layerYDirection.compareTo(AxisDirection.DISPLAY_DOWN) == 0) {
                        value = layerEnv.getMaximum(1) - imageEnv.getSpan(1);
                    } else {
                        value = layerEnv.getMinimum(1) + imageEnv.getSpan(1);
                    }
                    return String.valueOf(String.valueOf(value));
                }
            };
            worker.execute();
        }
    }

    private class AlignLeftAction extends AbstractAction {

        private AlignLeftAction() {
            putValue(NAME, "Left");
            putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
            putValue(SMALL_ICON, UIUtils.loadImageIcon("icons/AlignLeft24.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<String, String> worker = new AlignmentSwingWorker(numberFields[4]) {
                @Override
                protected String doInBackground() throws Exception {
                    Envelope layerEnv = getLayerEnvelope();
                    return String.valueOf(layerEnv.getMinimum(0));
                }
            };
            worker.execute();
        }
    }

    private class AlignCenterAction extends AbstractAction {

        private AlignCenterAction() {
            putValue(NAME, "Center");
            putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
            putValue(SMALL_ICON, UIUtils.loadImageIcon("icons/AlignCenter24.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<String, String> worker = new AlignmentSwingWorker(numberFields[4]) {
                @Override
                protected String doInBackground() throws Exception {
                    Envelope layerEnv = getLayerEnvelope();
                    Envelope imageEnv = getImageEnvelope();
                    return String.valueOf(String.valueOf(layerEnv.getMedian(0) - imageEnv.getSpan(0) / 2));
                }
            };
            worker.execute();
        }
    }

    private class AlignRightAction extends AbstractAction {

        private AlignRightAction() {
            putValue(NAME, "Right");
            putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
            putValue(SMALL_ICON, UIUtils.loadImageIcon("icons/AlignRight24.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<String, String> worker = new AlignmentSwingWorker(numberFields[4]) {
                @Override
                protected String doInBackground() throws Exception {
                    Envelope layerEnv = getLayerEnvelope();
                    Envelope imageEnv = getImageEnvelope();
                    return String.valueOf(layerEnv.getMaximum(0) - imageEnv.getSpan(0));
                }
            };
            worker.execute();
        }

    }

    private abstract class AlignmentSwingWorker extends SwingWorker<String, String> {

        private final JTextField numberField;

        private AlignmentSwingWorker(JTextField textField) {
            numberField = textField;
        }

        @Override
        protected void done() {
            try {
                numberField.setText(get());
            } catch (InterruptedException e1) {
                showError(e1);
            } catch (ExecutionException e1) {
                showError(e1);
            }
        }

        private void showError(Exception e1) {
            Throwable cause = e1.getCause() == null ? e1 : e1.getCause();
            String message = String.format("Could not compute transformation parameter.\n%s", cause.getMessage());
            getContext().showErrorDialog(message);
        }
    }
}