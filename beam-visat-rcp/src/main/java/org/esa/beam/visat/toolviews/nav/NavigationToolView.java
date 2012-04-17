/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glayer.swing.LayerCanvasModel;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static java.lang.Math.*;

/**
 * A window which displays product spectra.
 */
public class NavigationToolView extends AbstractToolView {

    public static final String ID = NavigationToolView.class.getName();
    private static final int MIN_SLIDER_VALUE = -100;
    private static final int MAX_SLIDER_VALUE = +100;

    private LayerCanvasModelChangeHandler layerCanvasModelChangeChangeHandler;
    private ProductNodeListener productNodeChangeHandler;

    private ProductSceneView currentView;

    private NavigationCanvas canvas;
    private AbstractButton zoomInButton;
    private AbstractButton zoomDefaultButton;
    private AbstractButton zoomOutButton;
    private AbstractButton zoomAllButton;
    private AbstractButton syncViewsButton;
    private AbstractButton syncCursorButton;
    private JTextField zoomFactorField;
    private JFormattedTextField rotationAngleField;
    private JSlider zoomSlider;
    private boolean inUpdateMode;
    private DecimalFormat scaleFormat;

    private boolean debug;

    private Color zeroRotationAngleBackground;
    private final Color positiveRotationAngleBackground = new Color(221, 255, 221); //#ddffdd
    private final Color negativeRotationAngleBackground = new Color(255, 221, 221); //#ffdddd
    private JSpinner rotationAngleSpinner;
    private CursorSynchronizer cursorSynchronizer;

    public NavigationToolView() {
    }

    @Override
    public JComponent createControl() {
        layerCanvasModelChangeChangeHandler = new LayerCanvasModelChangeHandler();
        productNodeChangeHandler = createProductNodeListener();
        cursorSynchronizer = new CursorSynchronizer(VisatApp.getApp());

        final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        scaleFormat = new DecimalFormat("#####.##", decimalFormatSymbols);
        scaleFormat.setGroupingUsed(false);
        scaleFormat.setDecimalSeparatorAlwaysShown(false);

        zoomInButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomIn24.gif"), false);
        zoomInButton.setToolTipText("Zoom in."); /*I18N*/
        zoomInButton.setName("zoomInButton");
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoom(getCurrentView().getZoomFactor() * 1.2);
            }
        });

        zoomOutButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomOut24.gif"), false);
        zoomOutButton.setName("zoomOutButton");
        zoomOutButton.setToolTipText("Zoom out."); /*I18N*/
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoom(getCurrentView().getZoomFactor() / 1.2);
            }
        });

        zoomDefaultButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomPixel24.gif"), false);
        zoomDefaultButton.setToolTipText("Actual Pixels (image pixel = view pixel)."); /*I18N*/
        zoomDefaultButton.setName("zoomDefaultButton");
        zoomDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoomToPixelResolution();
            }
        });

        zoomAllButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomAll24.gif"), false);
        zoomAllButton.setName("zoomAllButton");
        zoomAllButton.setToolTipText("Zoom all."); /*I18N*/
        zoomAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoomAll();
            }
        });

        syncViewsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SyncViews24.png"), true);
        syncViewsButton.setToolTipText("Synchronise compatible product views."); /*I18N*/
        syncViewsButton.setName("syncViewsButton");
        syncViewsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                maybeSynchronizeCompatibleProductViews();
            }
        });

        syncCursorButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SyncCursor24.png"), true);
        syncCursorButton.setToolTipText("Synchronise cursor position."); /*I18N*/
        syncCursorButton.setName("syncCursorButton");
        syncCursorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                cursorSynchronizer.setEnabled(syncCursorButton.isSelected());
            }
        });

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"), false);
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");


        final JPanel eastPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        gbc.gridy = 0;
        eastPane.add(zoomInButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomOutButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomDefaultButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomAllButton, gbc);

        gbc.gridy++;
        eastPane.add(syncViewsButton, gbc);

        gbc.gridy++;
        eastPane.add(syncCursorButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        eastPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;

        gbc.gridy++;
        eastPane.add(helpButton, gbc);

        zoomFactorField = new JTextField();
        zoomFactorField.setColumns(8);
        zoomFactorField.setHorizontalAlignment(JTextField.CENTER);
        zoomFactorField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                handleZoomFactorFieldUserInput();
            }
        });
        zoomFactorField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                handleZoomFactorFieldUserInput();
            }
        });

        rotationAngleSpinner = new JSpinner(new SpinnerNumberModel(0.0, -1800.0, 1800.0, 5.0));
        final JSpinner.NumberEditor editor = (JSpinner.NumberEditor) rotationAngleSpinner.getEditor();
        rotationAngleField = editor.getTextField();
        final DecimalFormat rotationFormat;
        rotationFormat = new DecimalFormat("#####.##°", decimalFormatSymbols);
        rotationFormat.setGroupingUsed(false);
        rotationFormat.setDecimalSeparatorAlwaysShown(false);
        rotationAngleField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return new NumberFormatter(rotationFormat);
            }
        });
        rotationAngleField.setColumns(6);
        rotationAngleField.setEditable(true);
        rotationAngleField.setHorizontalAlignment(JTextField.CENTER);
        rotationAngleField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRotationAngleFieldUserInput();
            }
        });
        rotationAngleField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                handleRotationAngleFieldUserInput();
            }
        });
        rotationAngleField.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                handleRotationAngleFieldUserInput();
            }
        });

        zoomSlider = new JSlider(JSlider.HORIZONTAL);
        zoomSlider.setValue(0);
        zoomSlider.setMinimum(MIN_SLIDER_VALUE);
        zoomSlider.setMaximum(MAX_SLIDER_VALUE);
        zoomSlider.setPaintTicks(false);
        zoomSlider.setPaintLabels(false);
        zoomSlider.setSnapToTicks(false);
        zoomSlider.setPaintTrack(true);
        zoomSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!inUpdateMode) {
                    zoom(sliderValueToZoomFactor(zoomSlider.getValue()));
                }
            }
        });

        final JPanel zoomFactorPane = new JPanel(new BorderLayout());
        zoomFactorPane.add(zoomFactorField, BorderLayout.WEST);

        final JPanel rotationAnglePane = new JPanel(new BorderLayout());
        rotationAnglePane.add(rotationAngleSpinner, BorderLayout.EAST);
        rotationAnglePane.add(new JLabel(" "), BorderLayout.CENTER);


        final JPanel sliderPane = new JPanel(new BorderLayout(2, 2));
        sliderPane.add(zoomFactorPane, BorderLayout.WEST);
        sliderPane.add(zoomSlider, BorderLayout.CENTER);
        sliderPane.add(rotationAnglePane, BorderLayout.EAST);

        canvas = createNavigationCanvas();
        canvas.setBackground(new Color(138, 133, 128)); // image background
        canvas.setForeground(new Color(153, 153, 204)); // slider overlay

        final JPanel centerPane = new JPanel(new BorderLayout(4, 4));
        centerPane.add(BorderLayout.CENTER, canvas);
        centerPane.add(BorderLayout.SOUTH, sliderPane);

        final JPanel mainPane = new JPanel(new BorderLayout(8, 8));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(centerPane, BorderLayout.CENTER);
        mainPane.add(eastPane, BorderLayout.EAST);

        mainPane.setPreferredSize(new Dimension(320, 320));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }

        setCurrentView(VisatApp.getApp().getSelectedProductSceneView());

        updateState();

        // Add an internal frame listener to VISAT so that we can update our
        // navigation window with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new NavigationIFL());

        return mainPane;
    }

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    public void setCurrentView(final ProductSceneView newView) {
        if (currentView != newView) {
            final ProductSceneView oldView = currentView;
            if (oldView != null) {
                oldView.getProduct().removeProductNodeListener(productNodeChangeHandler);
                if (oldView.getLayerCanvas() != null) {
                    oldView.getLayerCanvas().getModel().removeChangeListener(layerCanvasModelChangeChangeHandler);
                }
            }
            currentView = newView;
            if (currentView != null) {
                currentView.getProduct().addProductNodeListener(productNodeChangeHandler);
                if (currentView.getLayerCanvas() != null) {
                    currentView.getLayerCanvas().getModel().addChangeListener(layerCanvasModelChangeChangeHandler);
                }
            }
            canvas.handleViewChanged(oldView, newView);
            if (syncViewsButton.isSelected() && oldView != null && newView != null) {
                newView.getLayerCanvas().setInitiallyZoomingAll(false);
                oldView.synchronizeViewport(newView);
            }

            updateState();
        }
    }


    NavigationCanvas createNavigationCanvas() {
        return new NavigationCanvas(this);
    }

    private void handleZoomFactorFieldUserInput() {
        Double zf = getZoomFactorFieldValue();
        if (zf != null) {
            updateScaleField(zf);
            zoom(zf);
        }
    }

    private void handleRotationAngleFieldUserInput() {
        final Double ra = Math.round(getRotationAngleFieldValue() / 5) * 5.0;
        updateRotationField(ra);
        rotate(ra);
    }

    private Double getZoomFactorFieldValue() {
        final String text = zoomFactorField.getText();
        if (text.contains(":")) {
            return parseTextualValue(text);
        } else {
            try {
                double v = Double.parseDouble(text);
                return v > 0 ? v : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private Double getRotationAngleFieldValue() {
        String text = rotationAngleField.getText();
        if (text != null) {
            while (text.endsWith("°")) {
                text = text.substring(0, text.length() - 1);
            }
        }
        try {
            final double v = Double.parseDouble(text);
            final double max = 360 * 100;
            final double negMax = max * -1;
            if (v > max || v < negMax) {
                return 0.0;
            }
            return v;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static Double parseTextualValue(String text) {
        final String[] numbers = text.split(":");
        if (numbers.length == 2) {
            double dividend = 0;
            double divisor = 0;
            try {
                dividend = Double.parseDouble(numbers[0]);
                divisor = Double.parseDouble(numbers[1]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (divisor == 0) {
                return null;
            }
            double factor = dividend / divisor;
            return factor > 0 ? factor : null;
        } else {
            return null;
        }
    }

    public void setModelOffset(final double modelOffsetX, final double modelOffsetY) {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().getViewport().setOffset(modelOffsetX, modelOffsetY);
            maybeSynchronizeCompatibleProductViews();
        }
    }

    private void zoomToPixelResolution() {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            final LayerCanvas layerCanvas = view.getLayerCanvas();
            layerCanvas.getViewport().setZoomFactor(layerCanvas.getDefaultZoomFactor());
            maybeSynchronizeCompatibleProductViews();
        }
    }

    public void zoom(final double zoomFactor) {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().getViewport().setZoomFactor(zoomFactor);
            maybeSynchronizeCompatibleProductViews();
        }
    }

    private void rotate(Double rotationAngle) {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().getViewport().setOrientation(rotationAngle * MathUtils.DTOR);
            maybeSynchronizeCompatibleProductViews();
        }
    }

    public void zoomAll() {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().zoomAll();
            maybeSynchronizeCompatibleProductViews();
        }
    }

    private void maybeSynchronizeCompatibleProductViews() {
        if (syncViewsButton.isSelected()) {
            synchronizeCompatibleProductViews();
        }
    }

    private void synchronizeCompatibleProductViews() {
        final ProductSceneView currentView = getCurrentView();
        if (currentView == null) {
            return;
        }
        final JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (final JInternalFrame internalFrame : internalFrames) {
            if (internalFrame.getContentPane() instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) internalFrame.getContentPane();
                if (view != currentView) {
                    currentView.synchronizeViewport(view);
                }
            }
        }
    }

    /**
     * @param sv a value between MIN_SLIDER_VALUE and MAX_SLIDER_VALUE
     *
     * @return a value between min and max zoom factor of the AdjustableView
     */
    private double sliderValueToZoomFactor(final int sv) {
        AdjustableView adjustableView = getCurrentView().getLayerCanvas();
        double f1 = scaleExp2Min(adjustableView);
        double f2 = scaleExp2Max(adjustableView);
        double s1 = zoomSlider.getMinimum();
        double s2 = zoomSlider.getMaximum();
        double v1 = (sv - s1) / (s2 - s1);
        double v2 = f1 + v1 * (f2 - f1);
        double zf = exp2(v2);

        if (debug) {
            System.out.println("NavigationToolView.sliderValueToZoomFactor:");
            System.out.println("  sv = " + sv);
            System.out.println("  f1 = " + f1);
            System.out.println("  f2 = " + f2);
            System.out.println("  v1 = " + v1);
            System.out.println("  v2 = " + v2);
            System.out.println("  zf = " + zf);
        }

        return zf;
    }

    /**
     * @param zf a value between min and max zoom factor of the AdjustableView
     *
     * @return a value between MIN_SLIDER_VALUE and MAX_SLIDER_VALUE
     */
    private int zoomFactorToSliderValue(final double zf) {
        AdjustableView adjustableView = getCurrentView().getLayerCanvas();
        double f1 = scaleExp2Min(adjustableView);
        double f2 = scaleExp2Max(adjustableView);
        double s1 = zoomSlider.getMinimum();
        double s2 = zoomSlider.getMaximum();
        double v2 = log2(zf);
        double v1 = max(0.0, min(1.0, (v2 - f1) / (f2 - f1)));
        int sv = (int) round((s1 + v1 * (s2 - s1)));

        if (debug) {
            System.out.println("NavigationToolView.zoomFactorToSliderValue:");
            System.out.println("  zf = " + zf);
            System.out.println("  f1 = " + f1);
            System.out.println("  f2 = " + f2);
            System.out.println("  v2 = " + v2);
            System.out.println("  v1 = " + v1);
            System.out.println("  sv = " + sv);
        }

        return sv;
    }

    private void updateState() {
        final boolean canNavigate = getCurrentView() != null;
        zoomInButton.setEnabled(canNavigate);
        zoomDefaultButton.setEnabled(canNavigate);
        zoomOutButton.setEnabled(canNavigate);
        zoomAllButton.setEnabled(canNavigate);
        zoomSlider.setEnabled(canNavigate);
        syncViewsButton.setEnabled(canNavigate);
        syncCursorButton.setEnabled(canNavigate);
        zoomFactorField.setEnabled(canNavigate);
        rotationAngleSpinner.setEnabled(canNavigate);
        updateTitle();
        updateValues();
    }

    private void updateTitle() {
// todo - activate when we can use ToolView.setTitle()
/*
        if (currentView != null) {
            if (currentView.isRGB()) {
                setTitle(getDescriptor().getTitle() + " - " + currentView.getProduct().getProductRefString() + " RGB");
            } else {
                setTitle(getDescriptor().getTitle() + " - " + currentView.getRaster().getDisplayName());
            }
        } else {
            setTitle(getDescriptor().getTitle());
        }
*/
    }

    private void updateValues() {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            boolean oldState = inUpdateMode;
            inUpdateMode = true;

            double zf = view.getZoomFactor();
            updateZoomSlider(zf);
            updateScaleField(zf);

            updateRotationField(view.getOrientation() * MathUtils.RTOD);

            inUpdateMode = oldState;
        }
    }

    private void updateZoomSlider(double zf) {
        int sv = zoomFactorToSliderValue(zf);
        zoomSlider.setValue(sv);
    }

    private void updateScaleField(double zf) {
        String text;
        if (zf > 1.0) {
            text = scaleFormat.format(roundScale(zf)) + " : 1";
        } else if (zf < 1.0) {
            text = "1 : " + scaleFormat.format(roundScale(1.0 / zf));
        } else {
            text = "1 : 1";
        }
        zoomFactorField.setText(text);
    }

    private void updateRotationField(Double ra) {
        while (ra > 180) {
            ra -= 360;
        }
        while (ra < -180) {
            ra += 360;
        }
        rotationAngleField.setValue(ra);
        if (zeroRotationAngleBackground == null) {
            zeroRotationAngleBackground = rotationAngleField.getBackground();
        }
        if (ra > 0) {
            rotationAngleField.setBackground(positiveRotationAngleBackground);
        } else if (ra < 0) {
            rotationAngleField.setBackground(negativeRotationAngleBackground);
        } else {
            rotationAngleField.setBackground(zeroRotationAngleBackground);
        }

    }

    private static double roundScale(double x) {
        double e = floor((log10(x)));
        double f = 10.0 * pow(10.0, e);
        double fx = x * f;
        double rfx = round(fx);
        if (abs((rfx + 0.5) - fx) <= abs(rfx - fx)) {
            rfx += 0.5;
        }
        return rfx / f;
    }

    private static double scaleExp2Min(AdjustableView adjustableView) {
        return floor(log2(adjustableView.getMinZoomFactor()));
    }

    private static double scaleExp2Max(AdjustableView adjustableView) {
        return floor(log2(adjustableView.getMaxZoomFactor()) + 1);
    }

    private static double log2(double x) {
        return log(x) / log(2.0);
    }

    private static double exp2(double x) {
        return pow(2.0, x);
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(Product.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((currentView.isRGB() && sourceNode == currentView.getProduct())
                        || sourceNode == currentView.getRaster()) {
                        updateTitle();
                    }
                }
            }
        };
    }

    private class NavigationIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                PropertyMap preferences = VisatApp.getApp().getPreferences();
                final boolean showWindow = preferences.getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NAVIGATION,
                                                                       true);
                if (showWindow) {
                    ApplicationPage page = VisatApp.getApp().getApplicationPage();
                    ToolView toolView = page.getToolView(NavigationToolView.ID);
                    if (toolView != null) {
                        page.showToolView(NavigationToolView.ID);
                    }
                }
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            if (isControlCreated()) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    final ProductSceneView view = (ProductSceneView) contentPane;
                    setCurrentView(view);
                } else {
                    setCurrentView(null);
                }
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            if (isControlCreated()) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    ProductSceneView view = (ProductSceneView) contentPane;
                    if (getCurrentView() == view) {
                        setCurrentView(null);
                    }
                }
            }
        }

    }

    private class LayerCanvasModelChangeHandler implements LayerCanvasModel.ChangeListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        }

        @Override
        public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
            updateValues();
            maybeSynchronizeCompatibleProductViews();
        }
    }
}
