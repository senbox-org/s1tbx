/*
 * $Id: NavigationToolView.java,v 1.2 2007/04/23 13:49:34 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.nav;

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
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

/**
 * A window which displays product spectra.
 */
public class NavigationToolView extends AbstractToolView {

    public static final String ID = NavigationToolView.class.getName();
    private static final int MAX_SLIDER_VALUE = 100;
    private static final double ZOOM_FACTOR = 1.2;
    private static final double VIEW_SCALE_MAX = 32.0;

    private ProductSceneView currentView;
    private NavigationCanvas canvas;
    private AbstractButton zoomInButton;
    private AbstractButton zoomZeroButton;
    private AbstractButton zoomOutButton;
    private AbstractButton zoomAllButton;
    private AbstractButton syncViewsButton;
    private JTextField percentField;
    private JSlider zoomSlider;
    private ViewportHandler viewportHandler;
    private ImageDisplayResizeHandler imageDisplayRH;
    private DecimalFormat percentFormat;
    private ProductSceneView.ImageUpdateListener imageUpdateHandler;
    private ProductNodeListener productNodeListener;
    private ProductSceneView.LayerContentListener layerContentListener;

    public NavigationToolView() {
        viewportHandler = new ViewportHandler();
        imageDisplayRH = new ImageDisplayResizeHandler();
        final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        percentFormat = new DecimalFormat("#####.##", decimalFormatSymbols);
        percentFormat.setGroupingUsed(false);
        percentFormat.setDecimalSeparatorAlwaysShown(false);
        imageUpdateHandler = new ProductSceneView.ImageUpdateListener() {
            @Override
            public void handleImageUpdated(final ProductSceneView view) {
                if (isVisible()) {
                    canvas.updateImage();
                }
            }
        };
        layerContentListener = new ProductSceneView.LayerContentListener() {
            @Override
            public void layerContentChanged(RasterDataNode raster) {
                if (isVisible()) {
                    canvas.updateImage();
                }
            }
        };
        productNodeListener = createProductNodeListener();

        // Add an internal frame listener to VISAT so that we can update our
        // navigation window with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new NavigationIFL());
    }

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    public void setCurrentView(final ProductSceneView newView) {
        final ProductSceneView oldView = currentView;
        if (oldView != newView) {
            if (oldView != null) {
                oldView.removeImageUpdateListener(imageUpdateHandler);
                currentView.getProduct().removeProductNodeListener(productNodeListener);
                if (oldView.getImageDisplayComponent() != null) {
                    oldView.removeLayerContentListener(layerContentListener);
                    oldView.removeLayerViewportListener(viewportHandler);
                    oldView.getImageDisplayComponent().removeComponentListener(imageDisplayRH);
                }
            }
            currentView = newView;
            if (currentView != null) {
                currentView.addImageUpdateListener(imageUpdateHandler);
                currentView.getProduct().addProductNodeListener(productNodeListener);
                if (currentView.getImageDisplayComponent() != null) {
                    currentView.addLayerViewportListener(viewportHandler);
                    currentView.addLayerContentListener(layerContentListener);
                    currentView.getImageDisplayComponent().addComponentListener(imageDisplayRH);
                }
            }
            canvas.updateImage();
            updateState();
            updateValues();
            // nf: the following repaint() call is for some reason important.
            // If I remove it, this windows goes into background when VISAT frame is selected! Why?
            //repaint();
        }
    }


    @Override
    public JComponent createControl() {
        zoomInButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomIn24.gif"), false);
        zoomInButton.setToolTipText("Zoom in."); /*I18N*/
        zoomInButton.setName("zoomInButton");
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // TODO IMAGING 4.5
                zoom(getCurrentView().getZoomFactor() * ZOOM_FACTOR);
            }
        });

        zoomZeroButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomZero24.gif"), false);
        zoomZeroButton.setToolTipText("Actual Pixels."); /*I18N*/
        zoomZeroButton.setName("zoomZeroButton");
        zoomZeroButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoom(1.0);
            }
        });

        zoomOutButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomOut24.gif"), false);
        zoomOutButton.setName("zoomOutButton");
        zoomOutButton.setToolTipText("Zoom out."); /*I18N*/
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // TODO IMAGING 4.5
                zoom(getCurrentView().getZoomFactor() / ZOOM_FACTOR);
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

        syncViewsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Chain24.gif"), true);
        syncViewsButton.setToolTipText("Synchronize compatible product views."); /*I18N*/
        syncViewsButton.setName("syncViewsButton");
        syncViewsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                maybeSynchronizeCompatibleProductViews();
            }
        });

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
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
        eastPane.add(zoomZeroButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomOutButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomAllButton, gbc);

        gbc.gridy++;
        eastPane.add(syncViewsButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        eastPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;

        gbc.gridy++;
        eastPane.add(helpButton, gbc);

        percentField = new JTextField();
        percentField.setColumns(5);
        percentField.setHorizontalAlignment(JTextField.RIGHT);
        percentField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                applyPercentValue();
            }
        });
        percentField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                applyPercentValue();
            }
        });

        zoomSlider = new JSlider(JSlider.HORIZONTAL);
        zoomSlider.setValue(0);
        zoomSlider.setMinimum(-100);
        zoomSlider.setMaximum(+100);
        zoomSlider.setPaintTicks(false);
        zoomSlider.setPaintLabels(false);
        zoomSlider.setSnapToTicks(false);
        zoomSlider.setPaintTrack(true);
        zoomSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                zoom(sliderValueToViewScale(zoomSlider.getValue()));
            }
        });

        final JPanel percentPane = new JPanel(new BorderLayout());
        percentPane.add(percentField, BorderLayout.WEST);
        percentPane.add(new JLabel("%"), BorderLayout.EAST);

        final JPanel sliderPane = new JPanel(new BorderLayout(2, 2));
        sliderPane.add(percentPane, BorderLayout.WEST);
        sliderPane.add(zoomSlider, BorderLayout.CENTER);

        canvas = new NavigationCanvas(this);
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

        updateState();
        updateValues();

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }


        setCurrentView(VisatApp.getApp().getSelectedProductSceneView());

        return mainPane;
    }

    private void applyPercentValue() {
        final ProductSceneView view = getCurrentView();
        final double viewScaleOld = view.getZoomFactor();
        double viewScale = getPercentFieldValue();
        viewScale = roundAndCropViewScale(viewScale);
        setPercentFieldValue(100.0 * viewScale);
        if (viewScaleOld != viewScale) {
            zoom(viewScale);
        }
    }

    private double getPercentFieldValue() {
        double viewScale;
        final String text = percentField.getText();
        try {
            final Number number = percentFormat.parse(text);
            viewScale = number.doubleValue() / 100.0;
        } catch (ParseException ignore) {
            viewScale = getCurrentView().getZoomFactor();
        }
        return viewScale;
    }

    private void setPercentFieldValue(final double viewScale) {
        percentField.setText(percentFormat.format(viewScale));
    }

    public void setModelOffset(final double modelOffsetX, final double modelOffsetY) {
        final ProductSceneView view = getCurrentView();
        if (view == null) {
            return;
        }
        // TODO IMAGING 4.5
        view.move(modelOffsetX, modelOffsetY);
        maybeSynchronizeCompatibleProductViews();
    }

    public void zoom(final double viewScale) {
        final ProductSceneView view = getCurrentView();
        if (view == null) {
            return;
        }
        view.zoom(viewScale);
        maybeSynchronizeCompatibleProductViews();
    }

    public void zoomAll() {
        final ProductSceneView view = getCurrentView();
        if (view == null) {
            return;
        }
        view.zoomAll();
        maybeSynchronizeCompatibleProductViews();
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
     * @param sliderValue a value between -MAX_SLIDER_VALUE and +MAX_SLIDER_VALUE
     * @return the corresponding value between  1/VIEW_SCALE_MAX and VIEW_SCALE_MAX
     */
    private static double sliderValueToViewScale(final int sliderValue) {
        if (sliderValue == -MAX_SLIDER_VALUE) {
            return 1.0 / VIEW_SCALE_MAX;
        }
        if (sliderValue == MAX_SLIDER_VALUE) {
            return VIEW_SCALE_MAX;
        }
        final double v = (double) sliderValue / (double) MAX_SLIDER_VALUE;
        double viewScale = Math.exp(v * Math.log(VIEW_SCALE_MAX));
        viewScale = roundAndCropViewScale(viewScale);
        return viewScale;
    }

    /**
     * @param viewScale a value between  1/VIEW_SCALE_MAX and VIEW_SCALE_MAX
     * @return the corresponding value between -MAX_SLIDER_VALUE and +MAX_SLIDER_VALUE
     */
    private static int viewScaleToSliderValue(final double viewScale) {
        final double v = Math.log(viewScale) / Math.log(VIEW_SCALE_MAX) * MAX_SLIDER_VALUE;
        int sliderValue = (int) Math.round(v);
        sliderValue = cropSliderValue(sliderValue);
        return sliderValue;
    }

    private static int cropSliderValue(int sliderValue) {
        if (sliderValue < -MAX_SLIDER_VALUE) {
            sliderValue = -MAX_SLIDER_VALUE;
        }
        if (sliderValue > MAX_SLIDER_VALUE) {
            sliderValue = MAX_SLIDER_VALUE;
        }
        return sliderValue;
    }

    private static double roundAndCropViewScale(double viewScale) {
        viewScale *= 1000.0;
        double v = Math.floor(viewScale);
        if (viewScale - v >= 0.5) {
            v += 0.5;
        }
        viewScale = v / 1000.0;
        if (viewScale < 1.0 / VIEW_SCALE_MAX) {
            viewScale = 1.0 / VIEW_SCALE_MAX;
        }
        if (viewScale > VIEW_SCALE_MAX) {
            viewScale = VIEW_SCALE_MAX;
        }
        return viewScale;
    }

    private void updateState() {
        updateTitle();
        final boolean canNavigate = getCurrentView() != null;
        zoomInButton.setEnabled(canNavigate);
        zoomZeroButton.setEnabled(canNavigate);
        zoomOutButton.setEnabled(canNavigate);
        zoomAllButton.setEnabled(canNavigate);
        zoomSlider.setEnabled(canNavigate);
        syncViewsButton.setEnabled(canNavigate);
        percentField.setEnabled(canNavigate);
    }

    private void updateTitle() {
        if (currentView != null) {
            if (currentView.isRGB()) {
                setTitle(getDescriptor().getTitle() + " - " + currentView.getProduct().getProductRefString() + " RGB");     /*I18N*/
            } else {
                setTitle(getDescriptor().getTitle() + " - " + currentView.getRaster().getDisplayName());
            }
        } else {
            setTitle(getDescriptor().getTitle());
        }
    }

    private void updateValues() {
        if (canvas.isUpdatingImageDisplay()) {
            return;
        }
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            canvas.updateSlider();
            // TODO IMAGING 4.5
            final int sliderValue = viewScaleToSliderValue(view.getZoomFactor());
            zoomSlider.setValue(sliderValue);
            // TODO IMAGING 4.5
            final double viewScalePercent = 100.0 * roundAndCropViewScale(view.getZoomFactor());
            setPercentFieldValue(viewScalePercent);
        }
    }

    private class ViewportHandler implements ProductSceneView.LayerViewportListener {
        @Override
        public void layerViewportChanged(boolean orientationChanged) {
            updateValues();
            if (orientationChanged) {
                canvas.updateImage();
            }
        }
    }

    private class ImageDisplayResizeHandler extends ComponentAdapter {

        @Override
        public void componentResized(final ComponentEvent e) {
            canvas.updateSlider();
        }
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

        /**
         * Invoked when an internal frame has been opened.
         */
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            // May be called without having the actual window control created
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                PropertyMap preferences = VisatApp.getApp().getPreferences();
                final boolean showWindow = preferences.getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NAVIGATION, true);
                if (showWindow) {
                    ApplicationPage page = VisatApp.getApp().getPage();
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
        public void internalFrameDeactivated(InternalFrameEvent e) {
            if (isControlCreated()) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    setCurrentView(null);
                }
            }
        }
    }
}
