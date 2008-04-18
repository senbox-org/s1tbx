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

import com.bc.view.ViewModel;
import com.bc.view.ViewModelChangeListener;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
    private static final double VIEW_SCALE_MAX = 16.0; // todo - obtain from global option, also refer to GraphicsPane.viewScaleMax
    public static final double MIN_PERCENT_VALUE = 100.0 / VIEW_SCALE_MAX;
    public static final double MAX_PERCENT_VALUE = 100.0 * VIEW_SCALE_MAX;

    private ProductSceneView _currentView;
    private NavigationCanvas _canvas;
    private AbstractButton _zoomInButton;
    private AbstractButton _zoomZeroButton;
    private AbstractButton _zoomOutButton;
    private AbstractButton _zoomAllButton;
    private AbstractButton _syncViewsButton;
    private JTextField _percentField;
    private JSlider _zoomSlider;
    private ViewModelChangeHandler _imageDisplayCH;
    private ImageDisplayResizeHandler _imageDisplayRH;
    private DecimalFormat _percentFormat;
    private ProductSceneView.ImageUpdateListener _imageUpdateHandler;
    private ProductNodeListener _productNodeListener;

    public NavigationToolView() {
        _imageDisplayCH = new ViewModelChangeHandler();
        _imageDisplayRH = new ImageDisplayResizeHandler();
        final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        _percentFormat = new DecimalFormat("#####.##", decimalFormatSymbols);
        _percentFormat.setGroupingUsed(false);
        _percentFormat.setDecimalSeparatorAlwaysShown(false);
        _imageUpdateHandler = new ProductSceneView.ImageUpdateListener() {
            public void handleImageUpdated(final ProductSceneView view) {
                _canvas.updateImage();
            }
        };
        _productNodeListener = createProductNodeListener();
    }

    public ProductSceneView getCurrentView() {
        return _currentView;
    }

    public void setCurrentView(final ProductSceneView newView) {
        final ProductSceneView oldView = _currentView;
        if (oldView != newView) {
            if (oldView != null) {
                oldView.removeImageUpdateListener(_imageUpdateHandler);
                _currentView.getProduct().removeProductNodeListener(_productNodeListener);
                if (oldView.getImageDisplay() != null) {
                    oldView.getImageDisplay().getViewModel().removeViewModelChangeListener(_imageDisplayCH);
                    oldView.getImageDisplay().removeComponentListener(_imageDisplayRH);
                }
            }
            _currentView = newView;
            if (_currentView != null) {
                _currentView.addImageUpdateListener(_imageUpdateHandler);
                _currentView.getProduct().addProductNodeListener(_productNodeListener);
                if (_currentView.getImageDisplay() != null) {
                    _currentView.getImageDisplay().getViewModel().addViewModelChangeListener(_imageDisplayCH);
                    _currentView.getImageDisplay().addComponentListener(_imageDisplayRH);
                }
            }
            _canvas.updateImage();
            updateState();
            updateValues();
            // nf: the following repaint() call is for some reason important.
            // If I remove it, this windows goes into background when VISAT frame is selected! Why?
            //repaint();
        }
    }

    public ImageDisplay getCurrentImageDisplay() {
        return getCurrentView() != null ? getCurrentView().getImageDisplay() : null;
    }


    @Override
    public JComponent createControl() {

        _zoomInButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomIn24.gif"), false);
        _zoomInButton.setToolTipText("Zoom in."); /*I18N*/
        _zoomInButton.setName("zoomInButton");
        _zoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                zoom(getCurrentImageDisplay().getViewModel().getViewScale() * ZOOM_FACTOR);
            }
        });

        _zoomZeroButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomZero24.gif"), false);
        _zoomZeroButton.setToolTipText("Actual Pixels."); /*I18N*/
        _zoomZeroButton.setName("zoomZeroButton");
        _zoomZeroButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                zoom(1.0);
            }
        });

        _zoomOutButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomOut24.gif"), false);
        _zoomOutButton.setName("zoomOutButton");
        _zoomOutButton.setToolTipText("Zoom out."); /*I18N*/
        _zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                zoom(getCurrentImageDisplay().getViewModel().getViewScale() / ZOOM_FACTOR);
            }
        });

        _zoomAllButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomAll24.gif"), false);
        _zoomAllButton.setName("zoomAllButton");
        _zoomAllButton.setToolTipText("Zoom all."); /*I18N*/
        _zoomAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                zoomAll();
            }
        });

        _syncViewsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Chain24.gif"), true);
        _syncViewsButton.setToolTipText("Synchronize compatible product views."); /*I18N*/
        _syncViewsButton.setName("syncViewsButton");
        _syncViewsButton.addActionListener(new ActionListener() {
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
        eastPane.add(_zoomInButton, gbc);

        gbc.gridy++;
        eastPane.add(_zoomZeroButton, gbc);

        gbc.gridy++;
        eastPane.add(_zoomOutButton, gbc);

        gbc.gridy++;
        eastPane.add(_zoomAllButton, gbc);

        gbc.gridy++;
        eastPane.add(_syncViewsButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        eastPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;

        gbc.gridy++;
        eastPane.add(helpButton, gbc);

        _percentField = new JTextField();
        _percentField.setColumns(5);
        _percentField.setHorizontalAlignment(JTextField.RIGHT);
        _percentField.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                applyPercentValue();
            }
        });
        _percentField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                applyPercentValue();
            }
        });

        _zoomSlider = new JSlider(JSlider.HORIZONTAL);
        _zoomSlider.setValue(0);
        _zoomSlider.setMinimum(-100);
        _zoomSlider.setMaximum(+100);
        _zoomSlider.setPaintTicks(false);
        _zoomSlider.setPaintLabels(false);
        _zoomSlider.setSnapToTicks(false);
        _zoomSlider.setPaintTrack(true);
        _zoomSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                zoom(sliderValueToViewScale(_zoomSlider.getValue()));
            }
        });

        final JPanel percentPane = new JPanel(new BorderLayout());
        percentPane.add(_percentField, BorderLayout.WEST);
        percentPane.add(new JLabel("%"), BorderLayout.EAST);

        final JPanel sliderPane = new JPanel(new BorderLayout(2, 2));
        sliderPane.add(percentPane, BorderLayout.WEST);
        sliderPane.add(_zoomSlider, BorderLayout.CENTER);

        _canvas = new NavigationCanvas(this);
        _canvas.setBackground(new Color(138, 133, 128)); // image background
        _canvas.setForeground(new Color(153, 153, 204)); // slider overlay

        final JPanel centerPane = new JPanel(new BorderLayout(4, 4));
        centerPane.add(BorderLayout.CENTER, _canvas);
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

        // Add an internal frame listener to VISAT so that we can update our
        // navigation window with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new NavigationIFL());

        setCurrentView(VisatApp.getApp().getSelectedProductSceneView());

        return mainPane;
    }

    private void applyPercentValue() {
        final ImageDisplay imageDisplay = getCurrentImageDisplay();
        final double viewScaleOld = imageDisplay.getViewModel().getViewScale();
        double viewScale = getPercentFieldValue();
        viewScale = roundAndCropViewScale(viewScale);
        setPercentFieldValue(100.0 * viewScale);
        if (viewScaleOld != viewScale) {
            zoom(viewScale);
        }
    }

    private double getPercentFieldValue() {
        double viewScale;
        final String text = _percentField.getText();
        try {
            final Number number = _percentFormat.parse(text);
            viewScale = number.doubleValue() / 100.0;
        } catch (ParseException ignore) {
            viewScale = getCurrentImageDisplay().getViewModel().getViewScale();
        }
        return viewScale;
    }

    private void setPercentFieldValue(final double viewScale) {
        _percentField.setText(_percentFormat.format(viewScale));
    }

    public void setModelOffset(final double modelOffsetX, final double modelOffsetY) {
        final ImageDisplay imageDisplay = getCurrentImageDisplay();
        if (imageDisplay == null) {
            return;
        }
        imageDisplay.getViewModel().setModelOffset(modelOffsetX, modelOffsetY);
        maybeSynchronizeCompatibleProductViews();
    }

    public void zoom(final double viewScale) {
        final ImageDisplay imageDisplay = getCurrentImageDisplay();
        if (imageDisplay == null) {
            return;
        }
        imageDisplay.zoom(viewScale);
        maybeSynchronizeCompatibleProductViews();
    }

    public void zoomAll() {
        final ImageDisplay imageDisplay = getCurrentImageDisplay();
        if (imageDisplay == null) {
            return;
        }
        imageDisplay.zoomAll();
        maybeSynchronizeCompatibleProductViews();
    }

    private void maybeSynchronizeCompatibleProductViews() {
        if (_syncViewsButton.isSelected()) {
            synchronizeCompatibleProductViews();
        }
    }

    private void synchronizeCompatibleProductViews() {
        final ProductSceneView currentView = getCurrentView();
        if (currentView == null) {
            return;
        }
        final Product currentProduct = currentView.getRaster().getProduct();
        final JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (final JInternalFrame internalFrame : internalFrames) {
            if (internalFrame.getContentPane() instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) internalFrame.getContentPane();
                if (view != currentView) {
                    final Product otherProduct = view.getRaster().getProduct();
                    if (otherProduct == currentProduct ||
                        otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {
                        view.getImageDisplay().getViewModel().setModelOffset(
                                currentView.getImageDisplay().getViewModel().getModelOffsetX(),
                                currentView.getImageDisplay().getViewModel().getModelOffsetY(),
                                currentView.getImageDisplay().getViewModel().getViewScale());
                    }
                }
            }
        }
    }

    /**
     * @param sliderValue a value between -MAX_SLIDER_VALUE and +MAX_SLIDER_VALUE
     *
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
     *
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
        _zoomInButton.setEnabled(canNavigate);
        _zoomZeroButton.setEnabled(canNavigate);
        _zoomOutButton.setEnabled(canNavigate);
        _zoomAllButton.setEnabled(canNavigate);
        _zoomSlider.setEnabled(canNavigate);
        _syncViewsButton.setEnabled(canNavigate);
        _percentField.setEnabled(canNavigate);
    }

    private void updateTitle() {
        if (_currentView != null) {
            if (_currentView.isRGB()) {
                setTitle(getDescriptor().getTitle() + " - " + _currentView.getProduct().getProductRefString() + " RGB");     /*I18N*/
            } else {
                setTitle(getDescriptor().getTitle() + " - " + _currentView.getRaster().getDisplayName());
            }
        } else {
            setTitle(getDescriptor().getTitle());
        }
    }

    private void updateValues() {
        if (_canvas.isUpdatingImageDisplay()) {
            return;
        }
        final ImageDisplay imageDisplay = getCurrentImageDisplay();
        if (imageDisplay != null) {
            _canvas.updateSlider();
            final int sliderValue = viewScaleToSliderValue(imageDisplay.getViewModel().getViewScale());
            _zoomSlider.setValue(sliderValue);
            final double viewScalePercent = 100.0 * roundAndCropViewScale(imageDisplay.getViewModel().getViewScale());
            setPercentFieldValue(viewScalePercent);
        }
    }

    private class ViewModelChangeHandler implements ViewModelChangeListener {

        public void handleViewModelChanged(final ViewModel viewModel) {
            updateValues();
        }
    }

    private class ImageDisplayResizeHandler extends ComponentAdapter {

        @Override
        public void componentResized(final ComponentEvent e) {
            _canvas.updateSlider();
        }
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(Product.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((_currentView.isRGB() && sourceNode == _currentView.getProduct())
                        || sourceNode == _currentView.getRaster()) {
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
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final boolean showNavigationWin = VisatApp.getApp().getPreferences().getPropertyBool(
                        VisatApp.PROPERTY_KEY_AUTO_SHOW_NAVIGATION, true);
                if (showNavigationWin) {
                    ApplicationPage page = VisatApp.getApp().getPage();
                    ToolView toolView = page.getToolView(NavigationToolView.ID);
                    if(toolView != null && !toolView.getDescriptor().isVisible()) {
                        page.showToolView(NavigationToolView.ID);
                    }
                }
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                setCurrentView(view);
            } else {
                setCurrentView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setCurrentView(null);
            }
        }
    }
}
