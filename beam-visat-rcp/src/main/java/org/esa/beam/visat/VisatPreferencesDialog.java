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

package org.esa.beam.visat;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.update.ConnectionConfigData;
import com.bc.ceres.swing.update.ConnectionConfigPane;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.param.*;
import org.esa.beam.framework.ui.*;
import org.esa.beam.framework.ui.config.ConfigDialog;
import org.esa.beam.framework.ui.config.DefaultConfigPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.GraticuleLayerType;
import org.esa.beam.glayer.NoDataLayerType;
import org.esa.beam.glayer.WorldMapLayerType;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.actions.ShowModuleManagerAction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.esa.beam.visat.VisatApp.*;

public class VisatPreferencesDialog extends ConfigDialog {

    private static final int _PAGE_INSET_TOP = 15;
    private static final int _LINE_INSET_TOP = 4;

    public VisatPreferencesDialog(VisatApp visatApp, String helpId) {
        super(visatApp.getMainFrame(), helpId);
        setTitleBase(visatApp.getAppName() + " Preferences");  /*I18N*/
        addRootPage(new BehaviorPage());
        addRootPage(new AppearancePage());
        addRootPage(new RepositoryConnectionConfigPage());
        addRootPage(new ProductSettings());
        addRootPage(new GeolocationDisplayPage());
        addRootPage(new DataIO());
        final LayerPropertiesPage layerPropertiesPage = new LayerPropertiesPage();
        layerPropertiesPage.addSubPage(new ImageDisplayPage());
        layerPropertiesPage.addSubPage(new NoDataOverlayPage());
        layerPropertiesPage.addSubPage(new MaskOverlayPage());
        layerPropertiesPage.addSubPage(new GraticuleOverlayPage());
        layerPropertiesPage.addSubPage(new WorldMapLayerPage());
        addRootPage(layerPropertiesPage);
        addRootPage(new RGBImageProfilePage());
        addRootPage(new LoggingPage());
        expandAllPages();
    }

    public static class BehaviorPage extends DefaultConfigPage {

        private Parameter _unsupressParam;
        private SuppressibleOptionPane _suppressibleOptionPane;

        public BehaviorPage() {
            setTitle("UI Behavior"); /*I18N*/
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            _suppressibleOptionPane = getApp().getSuppressibleOptionPane();
            _unsupressParam = new Parameter("unsuppress", Boolean.FALSE);
            _unsupressParam.getProperties().setLabel("Show all suppressed tips and messages again");/*I18N*/

            Parameter param;

            param = new Parameter(PROPERTY_KEY_LOW_MEMORY_LIMIT, 20);
            param.getProperties().setLabel("On low memory, warn if free RAM falls below: "); /*I18N*/
            param.getProperties().setPhysicalUnit("M"); /*I18N*/
            param.getProperties().setMinValue(0);
            param.getProperties().setMaxValue(500);
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, Boolean.TRUE);
            param.getProperties().setLabel("Open image view for new (virtual) bands"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_AUTO_SHOW_NAVIGATION, Boolean.TRUE);
            param.getProperties().setLabel("Show navigation window when image views are opened"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES,
                    PixelInfoView.PROPERTY_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES);
            param.getProperties().setLabel("Show only pixel values of loaded or displayed bands"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_VERSION_CHECK_ENABLED, Boolean.TRUE);
            final String labelText = String.format("Check for new version on %s start", getApp().getAppName());
            param.getProperties().setLabel(labelText); /*I18N*/
            configParams.addParameter(param);
        }

        private JPanel createPageUI() {
            Parameter param;
            GridBagConstraints gbc;

            //////////////////////////////////////////////////////////////////////////////////////
            // Display Settings

            JPanel displaySettingsPane = GridBagUtils.createPanel();
            displaySettingsPane.setBorder(UIUtils.createGroupBorder("Display Settings")); /*I18N*/
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_AUTO_SHOW_NAVIGATION);
            displaySettingsPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_AUTO_SHOW_NEW_BANDS);
            displaySettingsPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES);
            displaySettingsPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            //////////////////////////////////////////////////////////////////////////////////////
            // Memory Management

            JPanel memorySettingsPane = GridBagUtils.createPanel();
            memorySettingsPane.setBorder(UIUtils.createGroupBorder("Memory Management")); /*I18N*/
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam(PROPERTY_KEY_LOW_MEMORY_LIMIT);
            GridBagUtils.addToPanel(memorySettingsPane, param.getEditor().getLabelComponent(), gbc, "weightx=0");
            GridBagUtils.addToPanel(memorySettingsPane, param.getEditor().getEditorComponent(), gbc, "weightx=1");
            GridBagUtils.addToPanel(memorySettingsPane, param.getEditor().getPhysUnitLabelComponent(), gbc,
                    "weightx=0");
            gbc.gridy++;

            //////////////////////////////////////////////////////////////////////////////////////
            // Other Settings

            JPanel otherSettingsPane = GridBagUtils.createPanel();
            otherSettingsPane.setBorder(UIUtils.createGroupBorder("Message Settings")); /*I18N*/
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_VERSION_CHECK_ENABLED);
            otherSettingsPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            otherSettingsPane.add(_unsupressParam.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            //////////////////////////////////////////////////////////////////////////////////////
            // All together

            JPanel pageUI = GridBagUtils.createPanel();
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");

            pageUI.add(displaySettingsPane, gbc);
            gbc.gridy++;
            gbc.insets.top = _LINE_INSET_TOP;

            pageUI.add(memorySettingsPane, gbc);
            gbc.gridy++;
            pageUI.add(otherSettingsPane, gbc);
            gbc.gridy++;

            return createPageUIContentPane(pageUI);
        }

        @Override
        public void onOK() {
            if ((Boolean) _unsupressParam.getValue()) {
                _suppressibleOptionPane.unSuppressDialogs();
            }
        }

        @Override
        public void updatePageUI() {
            boolean supressed = _suppressibleOptionPane.areDialogsSuppressed();
            _unsupressParam.setUIEnabled(supressed);
        }
    }

    public static class AppearancePage extends DefaultConfigPage {

        private static final String PROPERTY_KEY_APP_UI_LAF_NAME = PROPERTY_KEY_APP_UI_LAF + ".name";

        public AppearancePage() {
            setTitle("UI Appearance"); /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            final ParamChangeListener paramChangeListener = new ParamChangeListener() {
                public void parameterValueChanged(ParamChangeEvent event) {
                    updatePageUI();
                }
            };

            Parameter param;

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            param = new Parameter(PROPERTY_KEY_APP_UI_USE_SYSTEM_FONT_SETTINGS, Boolean.TRUE);
            param.getProperties().setLabel("Use system font settings");
            param.addParamChangeListener(paramChangeListener);
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_APP_UI_FONT_NAME, "SansSerif");
            param.getProperties().setValueSet(ge.getAvailableFontFamilyNames());
            param.getProperties().setValueSetBound(true);
            param.getProperties().setLabel("Font name");
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_APP_UI_FONT_SIZE, 9);
            param.getProperties().setValueSet(new String[]{"8", "9", "10", "11", "12", "13", "14", "15", "16"});
            param.getProperties().setValueSetBound(false);
            param.getProperties().setLabel("Font size");
            configParams.addParameter(param);

            String[] lafNames = getAvailableLafNames();
            param = new Parameter(PROPERTY_KEY_APP_UI_LAF_NAME, lafNames[0]);
            param.getProperties().setValueSetBound(true);
            param.getProperties().setValueSet(lafNames);
            param.getProperties().setLabel("Look and feel name");
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
        }

        private JPanel createPageUI() {
            Parameter param;

            // Font
            JPanel fontPane = GridBagUtils.createPanel();
            fontPane.setBorder(UIUtils.createGroupBorder("UI Font")); /*I18N*/
            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;
            gbc.insets.bottom = 10;

            param = getConfigParam(PROPERTY_KEY_APP_UI_USE_SYSTEM_FONT_SETTINGS);
            gbc.weightx = 0;
            gbc.gridwidth = 2;
            fontPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridwidth = 1;
            gbc.gridy++;
            gbc.insets.bottom = 4;

            param = getConfigParam(PROPERTY_KEY_APP_UI_FONT_NAME);
            gbc.weightx = 0;
            fontPane.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            fontPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_APP_UI_FONT_SIZE);
            gbc.weightx = 0;
            fontPane.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            fontPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            // Look and Feel
            JPanel lafPane = GridBagUtils.createPanel();
            lafPane.setBorder(UIUtils.createGroupBorder("UI Look and Feel")); /*I18N*/
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;
            gbc.insets.bottom = 10;

            param = getConfigParam(PROPERTY_KEY_APP_UI_LAF_NAME);
            gbc.weightx = 0;
            lafPane.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            lafPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            //////////////////////////////////////////////////////////////////////////////////////
            // All together
            JPanel pageUI = GridBagUtils.createPanel();
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");

            pageUI.add(fontPane, gbc);
            gbc.gridy++;
            gbc.insets.top = _LINE_INSET_TOP;

            pageUI.add(lafPane, gbc);

            return createPageUIContentPane(pageUI);
        }

        @Override
        public void updatePageUI() {
            Parameter param1 = getConfigParam(PROPERTY_KEY_APP_UI_USE_SYSTEM_FONT_SETTINGS);
            boolean enabled = !(Boolean) param1.getValue();
            getConfigParam(PROPERTY_KEY_APP_UI_FONT_NAME).setUIEnabled(enabled);
            getConfigParam(PROPERTY_KEY_APP_UI_FONT_SIZE).setUIEnabled(enabled);
        }

        @Override
        public PropertyMap getConfigParamValues(PropertyMap propertyMap) {
            propertyMap = super.getConfigParamValues(propertyMap);
            final String lafName = (String) getConfigParams().getParameter(PROPERTY_KEY_APP_UI_LAF_NAME).getValue();
            propertyMap.setPropertyString(PROPERTY_KEY_APP_UI_LAF, getLafClassName(lafName));
            return propertyMap;
        }

        @Override
        public void setConfigParamValues(PropertyMap propertyMap, ParamExceptionHandler errorHandler) {
            String lafClassName = propertyMap.getPropertyString(PROPERTY_KEY_APP_UI_LAF,
                    getDefaultLafClassName());
            getConfigParams().getParameter(PROPERTY_KEY_APP_UI_LAF_NAME).setValue(getLafName(lafClassName),
                    errorHandler);
            super.setConfigParamValues(propertyMap, errorHandler);
        }

        private String[] getAvailableLafNames() {
            UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
            ArrayList<String> lafNames = new ArrayList<String>(installedLookAndFeels.length);
            for (UIManager.LookAndFeelInfo installedLaf : installedLookAndFeels) {
                final String lafName = installedLaf.getName();
                // This should fix a problem occurring in JIDE 3.3.5 with the GTKLookAndFeel (nf, 2012-03-02)
                if (!"com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(lafName)) {
                    lafNames.add(lafName);
                }
            }
            return lafNames.toArray(new String[lafNames.size()]);
        }

        private String getLafClassName(String name) {
            UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
            for (UIManager.LookAndFeelInfo installedLookAndFeel : installedLookAndFeels) {
                if (installedLookAndFeel.getName().equalsIgnoreCase(name)) {
                    return installedLookAndFeel.getClassName();
                }
            }
            return getDefaultLafClassName();
        }

        private String getLafName(String lafClassName) {
            UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
            for (UIManager.LookAndFeelInfo installedLookAndFeel : installedLookAndFeels) {
                if (installedLookAndFeel.getClassName().equals(lafClassName)) {
                    return installedLookAndFeel.getName();
                }
            }
            return getDefaultLafName();
        }

        private String getDefaultLafName() {
            return UIManager.getLookAndFeel().getName();
        }

        private String getDefaultLafClassName() {
            return UIManager.getLookAndFeel().getClass().getName();
        }
    }

    public static class RepositoryConnectionConfigPage extends DefaultConfigPage {

        private ConnectionConfigData connectionConfigData;
        private ConnectionConfigPane connectionConfigPane;

        public RepositoryConnectionConfigPage() {
            setTitle("Module Repository"); /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
        }

        @Override
        protected void initPageUI() {
            connectionConfigData = new ConnectionConfigData();
            connectionConfigPane = new ConnectionConfigPane(connectionConfigData);
            setPageUI(createPageUIContentPane(connectionConfigPane));
        }

        @Override
        public void updatePageUI() {
            connectionConfigPane.updateUiState();
        }

        @Override
        public PropertyMap getConfigParamValues(PropertyMap propertyMap) {
            propertyMap = super.getConfigParamValues(propertyMap);
            if (connectionConfigPane.validateUiState()) {
                connectionConfigPane.transferUiToConfigData();
                ShowModuleManagerAction.transferConnectionData(connectionConfigData, propertyMap);
            }
            return propertyMap;
        }

        @Override
        public void setConfigParamValues(PropertyMap propertyMap, ParamExceptionHandler errorHandler) {
            ShowModuleManagerAction.transferConnectionData(propertyMap, connectionConfigData);
            connectionConfigPane.transferConfigDataToUi();
            super.setConfigParamValues(propertyMap, errorHandler);
        }
    }

    public static class GeolocationDisplayPage extends DefaultConfigPage {

        private Parameter paramOffsetX;
        private Parameter paramOffsetY;
        private JComponent visualizer;
        private Parameter paramShowDecimals;
        private Parameter paramGeolocationAsDecimal;

        public GeolocationDisplayPage() {
            setTitle("Geo-location Display"); /*I18N*/
        }

        @Override
        protected void initPageUI() {
            visualizer = createOffsetVisualizer();
            visualizer.setPreferredSize(new Dimension(60, 60));
            visualizer.setOpaque(true);
            visualizer.setBorder(BorderFactory.createLoweredBevelBorder());

            final TableLayout tableLayout = new TableLayout(3);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);

            final JPanel pageUI = new JPanel(tableLayout);
            pageUI.add(paramOffsetX.getEditor().getLabelComponent());
            tableLayout.setCellWeightX(0, 1, 1.0);
            pageUI.add(paramOffsetX.getEditor().getEditorComponent());

            tableLayout.setCellRowspan(0, 2, 2);
            tableLayout.setCellWeightX(0, 2, 1.0);
            tableLayout.setCellAnchor(0, 2, TableLayout.Anchor.CENTER);
            tableLayout.setCellFill(0, 2, TableLayout.Fill.NONE);
            pageUI.add(visualizer);

            pageUI.add(paramOffsetY.getEditor().getLabelComponent());
            tableLayout.setCellWeightX(1, 1, 1.0);
            pageUI.add(paramOffsetY.getEditor().getEditorComponent());

            tableLayout.setRowPadding(2, new Insets(10, 0, 4, 4));
            pageUI.add(paramShowDecimals.getEditor().getEditorComponent(), new TableLayout.Cell(2, 0, 1, 3));
            tableLayout.setRowPadding(3, new Insets(10, 0, 4, 4));
            pageUI.add(paramGeolocationAsDecimal.getEditor().getEditorComponent(), new TableLayout.Cell(3, 0, 1, 3));

            setPageUI(createPageUIContentPane(pageUI));

        }

        private JComponent createOffsetVisualizer() {
            return new JPanel() {

                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    final int totWidth = getWidth();
                    final int totHeight = getHeight();

                    if (totWidth == 0 || totHeight == 0) {
                        return;
                    }
                    if (!(g instanceof Graphics2D)) {
                        return;
                    }

                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setStroke(new BasicStroke(2));
                    final int borderSize = 10;
                    final int maxPixelWidth = totWidth - 2 * borderSize;
                    final int maxPixelHeight = totHeight - 2 * borderSize;
                    final int pixelSize = Math.min(maxPixelHeight, maxPixelWidth);
                    final Rectangle pixel = new Rectangle((totWidth - pixelSize) / 2, (totHeight - pixelSize) / 2,
                            pixelSize, pixelSize);
                    g2d.setColor(Color.blue);
                    g2d.drawRect(pixel.x, pixel.y, pixel.width, pixel.height);

                    final float offsetX = (Float) paramOffsetX.getValue();
                    final float offsetY = (Float) paramOffsetY.getValue();
                    final int posX = Math.round(pixelSize * offsetX + pixel.x);
                    final int posY = Math.round(pixelSize * offsetY + pixel.y);
                    drawPos(g2d, posX, posY);
                }

                private void drawPos(Graphics2D g2d, final int posX, final int posY) {
                    g2d.setColor(Color.yellow);
                    final int crossLength = 8;
                    g2d.drawLine(posX - crossLength, posY, posX + crossLength, posY);
                    g2d.drawLine(posX, posY - crossLength, posX, posY + crossLength);
                    g2d.setColor(Color.red);

                    final int diameter = 3;
                    g2d.fillOval(posX - diameter / 2, posY - diameter / 2, diameter, diameter);
                }
            };
        }


        @Override
        protected void initConfigParams(ParamGroup configParams) {
            final ParamChangeListener paramChangeListener = new ParamChangeListener() {
                public void parameterValueChanged(ParamChangeEvent event) {
                    visualizer.repaint();
                }
            };

            final ParamProperties propertiesX = new ParamProperties(Float.class);
            propertiesX.setDefaultValue(PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY);
            propertiesX.setMinValue(0.0f);
            propertiesX.setMaxValue(1.0f);
            propertiesX.setLabel("Relative pixel-X offset");
            paramOffsetX = new Parameter(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X, propertiesX);
            paramOffsetX.addParamChangeListener(paramChangeListener);
            configParams.addParameter(paramOffsetX);


            final ParamProperties propertiesY = new ParamProperties(Float.class);
            propertiesY.setDefaultValue(PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY);
            propertiesY.setMinValue(0.0f);
            propertiesY.setMaxValue(1.0f);
            propertiesY.setLabel("Relative pixel-Y offset");
            paramOffsetY = new Parameter(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y, propertiesY);
            paramOffsetY.addParamChangeListener(paramChangeListener);
            configParams.addParameter(paramOffsetY);

            final ParamProperties propShowDecimals = new ParamProperties(Boolean.class);
            propShowDecimals.setDefaultValue(PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS);
            propShowDecimals.setLabel("Show floating-point image coordinates");
            paramShowDecimals = new Parameter(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                    propShowDecimals);
            configParams.addParameter(paramShowDecimals);

            final ParamProperties propGeoLocationDisplay = new ParamProperties(Boolean.class);
            propGeoLocationDisplay.setDefaultValue(PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL);
            propGeoLocationDisplay.setLabel("Show geo-location coordinates in decimal degrees");
            paramGeolocationAsDecimal = new Parameter(PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL,
                    propGeoLocationDisplay);
            configParams.addParameter(paramGeolocationAsDecimal);
        }
    }

    public static class DataIO extends DefaultConfigPage {

        public DataIO() {
            setTitle("Data Input/Output");  /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param;

            param = new Parameter(PROPERTY_KEY_SAVE_PRODUCT_HEADERS, DEFAULT_VALUE_SAVE_PRODUCT_HEADERS);
            param.getProperties().setLabel("Save product header (MPH, SPH, Global_Attributes)"); /*I18N*/
            param.addParamChangeListener(new ParamChangeListener() {
                public void parameterValueChanged(ParamChangeEvent event) {
                    Parameter configParam = getConfigParam(PROPERTY_KEY_SAVE_PRODUCT_ANNOTATIONS);
                    boolean b = (Boolean) event.getParameter().getValue();
                    configParam.setUIEnabled(b);
                    if (!b) {
                        configParam.setValue(false, null);
                    }
                }
            });
            configParams.addParameter(param);


            param = new Parameter(PROPERTY_KEY_SAVE_PRODUCT_HISTORY, DEFAULT_VALUE_SAVE_PRODUCT_HISTORY);
            param.getProperties().setLabel("Save product history (History)"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_SAVE_PRODUCT_ANNOTATIONS, DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS);
            param.getProperties().setLabel("Save product annotation datasets (ADS)"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_SAVE_INCREMENTAL, DEFAULT_VALUE_SAVE_INCREMENTAL);
            param.getProperties().setLabel("Use incremental save (only save modified items)"); /*I18N*/
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            setPageUI(createPageUI());
        }

        private JPanel createPageUI() {
            Parameter param;
            GridBagConstraints gbc;

            JPanel beamDimap = GridBagUtils.createPanel();
            //beamDimap.setBorder(UIUtils.createGroupBorder("Default Format"));  /*I18N*/
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST, weightx=1");

            gbc.gridy = 0;
            param = getConfigParam(PROPERTY_KEY_SAVE_PRODUCT_HEADERS);
            beamDimap.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_SAVE_PRODUCT_HISTORY);
            beamDimap.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_SAVE_PRODUCT_ANNOTATIONS);
            GridBagUtils.addToPanel(beamDimap, param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_SAVE_INCREMENTAL);
            GridBagUtils.addToPanel(beamDimap, param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            JPanel pageUI = GridBagUtils.createPanel();
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST, weightx=1");

            pageUI.add(beamDimap, gbc);

            return createPageUIContentPane(pageUI);
        }
    }

    public static class LayerPropertiesPage extends DefaultConfigPage {

        public LayerPropertiesPage() {
            setTitle("Layer Properties"); /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param;

            param = new Parameter(ProductSceneView.PROPERTY_KEY_GRAPHICS_ANTIALIASING, Boolean.FALSE);
            param.getProperties().setLabel("Use anti-aliasing for rendering text and vector graphics"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(ProductSceneView.PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN, Boolean.TRUE);
            param.getProperties().setLabel("Show a navigation control widget in image views"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(ProductSceneView.PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN, Boolean.FALSE);
            param.getProperties().setLabel("Show scroll bars in image views"); /*I18N*/
            configParams.addParameter(param);

        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
        }

        private JPanel createPageUI() {
            Parameter param;

            GridBagConstraints gbc;
            // UI
            JPanel pageUI = GridBagUtils.createPanel();
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1");
            gbc.gridy = 0;
            gbc.insets.bottom = 8;

            param = getConfigParam(ProductSceneView.PROPERTY_KEY_GRAPHICS_ANTIALIASING);
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            final JLabel note = new JLabel("Note: For best performance turn anti-aliasing off.");
            configureNoteLabel(note);
            pageUI.add(note, gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;
            param = getConfigParam(ProductSceneView.PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN);
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;
            param = getConfigParam(ProductSceneView.PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN);
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            return createPageUIContentPane(pageUI);
        }
    }

    public static class ImageDisplayPage extends DefaultConfigPage {

        public ImageDisplayPage() {
            setTitle("Image Layer"); /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param;

            param = new Parameter(PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY, 512);
            param.getProperties().setLabel("Tile cache capacity"); /*I18N*/
            param.getProperties().setPhysicalUnit("M"); /*I18N*/
            param.getProperties().setMinValue(32);
            param.getProperties().setMaxValue(16384);
            configParams.addParameter(param);

            param = new Parameter("image.background.color", ProductSceneView.DEFAULT_IMAGE_BACKGROUND_COLOR);
            param.getProperties().setLabel("Background colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter("image.border.shown", ImageLayer.DEFAULT_BORDER_SHOWN);
            param.getProperties().setLabel("Show image border"); /*I18N*/
            param.addParamChangeListener(new ParamChangeListener() {

                @Override
                public void parameterValueChanged(ParamChangeEvent event) {
                    updatePageUI();
                }
            });
            configParams.addParameter(param);

            param = new Parameter("image.border.size", ImageLayer.DEFAULT_BORDER_WIDTH);
            param.getProperties().setLabel("Image border size"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter("image.border.color", ImageLayer.DEFAULT_BORDER_COLOR);
            param.getProperties().setLabel("Image border colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter("pixel.border.shown", Boolean.TRUE);
            param.getProperties().setLabel("Show pixel border in magnified views"); /*I18N*/
            param.addParamChangeListener(new ParamChangeListener() {

                @Override
                public void parameterValueChanged(ParamChangeEvent event) {
                    updatePageUI();
                }
            });
            configParams.addParameter(param);

        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
            updatePageUI();
        }

        private JPanel createPageUI() {
            Parameter param;

            JPanel pageUI = GridBagUtils.createPanel();

            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam(PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY);
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            JPanel p = new JPanel(new BorderLayout(4, 4));
            p.add(param.getEditor().getEditorComponent(), BorderLayout.CENTER);
            p.add(param.getEditor().getPhysUnitLabelComponent(), BorderLayout.EAST);
            pageUI.add(p, gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            gbc.gridwidth = 2;
            final JLabel note2 = new JLabel("Note: If you have enough memory select values > 256 M.");
            configureNoteLabel(note2);
            pageUI.add(note2, gbc);
            gbc.gridwidth = 1;
            gbc.gridy++;

            gbc.insets.top = 3 * _LINE_INSET_TOP;

            param = getConfigParam("image.background.color");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = 3 * _LINE_INSET_TOP;

            param = getConfigParam("image.border.shown");
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;
            gbc.gridwidth = 1;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam("image.border.size");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam("image.border.color");
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = 3 * _LINE_INSET_TOP;

            param = getConfigParam("pixel.border.shown");
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;
            gbc.gridwidth = 1;

            return createPageUIContentPane(pageUI);
        }

        @Override
        public void updatePageUI() {
            boolean enabled = (Boolean) getConfigParam("image.border.shown").getValue();
            setConfigParamUIEnabled("image.border.size", enabled);
            setConfigParamUIEnabled("image.border.color", enabled);
        }
    }

    public static class GraticuleOverlayPage extends DefaultConfigPage {

        public GraticuleOverlayPage() {
            setTitle("Graticule Layer"); /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param;

            final ParamChangeListener paramChangeListener = new ParamChangeListener() {
                @Override
                public void parameterValueChanged(ParamChangeEvent event) {
                    updatePageUI();
                }
            };

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_RES_AUTO, Boolean.TRUE);
            param.addParamChangeListener(paramChangeListener);
            param.getProperties().setLabel("Compute latitude and longitude steps"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_RES_PIXELS, 128);
            param.getProperties().setLabel("Average grid size in pixels"); /*I18N*/
            param.getProperties().setMinValue(16);
            param.getProperties().setMaxValue(512);
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_RES_LAT, 1.0);
            param.getProperties().setLabel("Latitude step (dec. degree)"); /*I18N*/
            param.getProperties().setMinValue(0.01);
            param.getProperties().setMaxValue(90.0);
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_RES_LON, 1.0);
            param.getProperties().setLabel("Longitude step (dec. degree)"); /*I18N*/
            param.getProperties().setMinValue(0.01);
            param.getProperties().setMaxValue(180.0);
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_LINE_COLOR, new Color(204, 204, 255));
            param.getProperties().setLabel("Line colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_LINE_WIDTH, 0.5);
            param.getProperties().setLabel("Line width"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_LINE_TRANSPARENCY, 0.0);
            param.getProperties().setLabel("Line transparency"); /*I18N*/
            param.getProperties().setMinValue(0.0);
            param.getProperties().setMaxValue(1.0);
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_TEXT_ENABLED, Boolean.TRUE);
            param.addParamChangeListener(paramChangeListener);
            param.getProperties().setLabel("Show text labels"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_TEXT_FG_COLOR, Color.white);
            param.getProperties().setLabel("Text foreground colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_TEXT_BG_COLOR, Color.black);
            param.getProperties().setLabel("Text background colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(GraticuleLayerType.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, 0.7);
            param.getProperties().setLabel("Text background transparency"); /*I18N*/
            param.getProperties().setMinValue(0.0);
            param.getProperties().setMaxValue(1.0);
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
        }

        private JPanel createPageUI() {
            Parameter param;

            JPanel pageUI = GridBagUtils.createPanel();
            //pageUI.setBorder(UIUtils.createGroupBorder("Graticule Overlay")); /*I18N*/

            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;

            param = getConfigParam("graticule.res.auto");
            gbc.weightx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            final JLabel note1 = new JLabel(
                    "Note: Deselect this option only very carefully. The latitude and longitude");
            configureNoteLabel(note1);
            pageUI.add(note1, gbc);
            gbc.gridy++;

            final JLabel note2 = new JLabel("steps you enter will be used for low and high resolution products.");
            configureNoteLabel(note2);
            pageUI.add(note2, gbc);
            gbc.gridy++;

            gbc.gridwidth = 1;
            gbc.insets.top = 2 * _LINE_INSET_TOP;

            param = getConfigParam("graticule.res.pixels");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam("graticule.res.lat");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam("graticule.res.lon");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = 3 * _LINE_INSET_TOP;

            param = getConfigParam("graticule.line.color");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam("graticule.line.width");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam("graticule.line.transparency");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = 3 * _LINE_INSET_TOP;

            param = getConfigParam("graticule.text.enabled");
            gbc.weightx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridwidth = 1;
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam("graticule.text.fg.color");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam("graticule.text.bg.color");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam("graticule.text.bg.transparency");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            return createPageUIContentPane(pageUI);
        }

        @Override
        public void updatePageUI() {
            final boolean resAuto = (Boolean) getConfigParam("graticule.res.auto").getValue();
            getConfigParam("graticule.res.pixels").setUIEnabled(resAuto);
            getConfigParam("graticule.res.lat").setUIEnabled(!resAuto);
            getConfigParam("graticule.res.lon").setUIEnabled(!resAuto);

            final boolean textEnabled = (Boolean) getConfigParam("graticule.text.enabled").getValue();
            getConfigParam("graticule.text.fg.color").setUIEnabled(textEnabled);
            getConfigParam("graticule.text.bg.color").setUIEnabled(textEnabled);
            getConfigParam("graticule.text.bg.transparency").setUIEnabled(textEnabled);
        }
    }

    public static class MaskOverlayPage extends DefaultConfigPage {

        private static final String PARAMETER_NAME_MASK_COLOR = "mask.color";
        private static final String PARAMETER_NAME_MASK_TRANSPARENCY = "mask.transparency";

        public MaskOverlayPage() {
            setTitle("Mask Layer");
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {

            Parameter param = new Parameter(PARAMETER_NAME_MASK_COLOR, Mask.ImageType.DEFAULT_COLOR.RED);
            param.getProperties().setLabel("Mask colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PARAMETER_NAME_MASK_TRANSPARENCY, Mask.ImageType.DEFAULT_TRANSPARENCY);
            param.getProperties().setLabel("Mask transparency"); /*I18N*/
            param.getProperties().setMinValue(0.0);
            param.getProperties().setMaxValue(0.95);
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
            updatePageUI();
        }

        private JPanel createPageUI() {
            Parameter param;

            JPanel pageUI = GridBagUtils.createPanel();
            //pageUI.setBorder(UIUtils.createGroupBorder("ROI Overlay")); /*I18N*/

            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;

            param = getConfigParam(PARAMETER_NAME_MASK_COLOR);
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam(PARAMETER_NAME_MASK_TRANSPARENCY);
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            return createPageUIContentPane(pageUI);
        }
    }

    private class WorldMapLayerPage extends DefaultConfigPage {

        private static final String PARAMETER_NAME_WORLDMAP_TYPE = "worldmap.type";

        private JComboBox box;
        private List<WorldMapLayerType> worldMapLayerTypes;

        public WorldMapLayerPage() {
            setTitle("World Map Layer");
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            worldMapLayerTypes = new ArrayList<WorldMapLayerType>();
            Set<LayerType> allLayerTypes = LayerTypeRegistry.getLayerTypes();
            for (LayerType layerType : allLayerTypes) {
                if (layerType instanceof WorldMapLayerType) {
                    WorldMapLayerType worldMapLayerType = (WorldMapLayerType) layerType;
                    worldMapLayerTypes.add(worldMapLayerType);
                }
            }
            Parameter param = new Parameter(PARAMETER_NAME_WORLDMAP_TYPE, "BlueMarbleLayerType");
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
            updatePageUI();
        }

        private JPanel createPageUI() {
            JPanel pageUI = GridBagUtils.createPanel();

            box = new JComboBox();
            box.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                              boolean cellHasFocus) {
                    Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected,
                            cellHasFocus);
                    if (value instanceof WorldMapLayerType && rendererComponent instanceof JLabel) {
                        WorldMapLayerType worldMapLayerType = (WorldMapLayerType) value;
                        JLabel label = (JLabel) rendererComponent;
                        label.setText(worldMapLayerType.getLabel());
                    }
                    return rendererComponent;
                }
            });
            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;
            gbc.weightx = 0;

            pageUI.add(new JLabel("World Map Layer: "), gbc);
            gbc.weightx = 1;

            pageUI.add(box, gbc);
            gbc.insets.top = _LINE_INSET_TOP;
            return createPageUIContentPane(pageUI);
        }

        @Override
        public void updatePageUI() {
            DefaultComboBoxModel aModel = new DefaultComboBoxModel(worldMapLayerTypes.toArray());
            final Parameter param = getConfigParam(PARAMETER_NAME_WORLDMAP_TYPE);
            String valueAsText = param.getValueAsText();
            WorldMapLayerType selected = null;
            for (WorldMapLayerType worldMapLayerType : worldMapLayerTypes) {
                if (worldMapLayerType.getName().equals(valueAsText)) {
                    selected = worldMapLayerType;
                    break;
                }
            }
            aModel.setSelectedItem(selected);
            box.setModel(aModel);
        }

        @Override
        public void onOK() {
            WorldMapLayerType selected = (WorldMapLayerType) box.getModel().getSelectedItem();
            final Parameter param = getConfigParam(PARAMETER_NAME_WORLDMAP_TYPE);
            try {
                param.setValue(selected.getName());
            } catch (ParamValidateException ignore) {
            }
        }
    }

    public static class NoDataOverlayPage extends DefaultConfigPage {

        public NoDataOverlayPage() {
            setTitle("No-Data Layer");
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param;

            param = new Parameter("noDataOverlay.color", NoDataLayerType.DEFAULT_COLOR);
            param.getProperties().setLabel("No-data overlay colour"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter("noDataOverlay.transparency", 0.3);
            param.getProperties().setLabel("No-data overlay transparency"); /*I18N*/
            param.getProperties().setMinValue(0.0);
            param.getProperties().setMaxValue(0.95);
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
            updatePageUI();
        }

        private JPanel createPageUI() {
            Parameter param;

            JPanel pageUI = GridBagUtils.createPanel();

            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
            gbc.gridy = 0;

            param = getConfigParam("noDataOverlay.color");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.insets.top = _LINE_INSET_TOP;

            param = getConfigParam("noDataOverlay.transparency");
            gbc.weightx = 0;
            pageUI.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            pageUI.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            return createPageUIContentPane(pageUI);
        }

        @Override
        public void updatePageUI() {
        }
    }

    public static class LoggingPage extends DefaultConfigPage {

        public LoggingPage() {
            setTitle("Logging"); /*I18N*/
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param;

            param = new Parameter(PROPERTY_KEY_APP_LOG_ENABLED, Boolean.FALSE);
            param.addParamChangeListener(new ParamChangeListener() {

                public void parameterValueChanged(ParamChangeEvent event) {
                    updatePageUI();
                }
            });
            param.getProperties().setLabel("Enable logging"); /*I18N*/
            configParams.addParameter(param);

//            param = new Parameter(VisatApp.PROPERTY_KEY_APP_LOG_PATH, new File("logging.txt"));
//            param.getProperties().setFileSelectionMode(ParamProperties.FSM_FILES_ONLY);
//            param.getProperties().setLabel("Logfile path");
//            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_APP_LOG_PREFIX, getApp().getAppName().toLowerCase());
            param.getProperties().setFileSelectionMode(ParamProperties.FSM_FILES_ONLY);
            param.getProperties().setLabel("Log filename prefix");
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_APP_LOG_ECHO, Boolean.FALSE);
            param.getProperties().setLabel("Echo log output (effective only with console)"); /*I18N*/
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_APP_LOG_LEVEL, SystemUtils.LLS_INFO);
            param.getProperties().setLabel("Log level"); /*I18N*/
            param.getProperties().setReadOnly(true);
            configParams.addParameter(param);

            param = new Parameter(PROPERTY_KEY_APP_DEBUG_ENABLED, Boolean.FALSE);
            param.getProperties().setLabel("Log extra debugging information"); /*I18N*/
            param.addParamChangeListener(new ParamChangeListener() {
                public void parameterValueChanged(ParamChangeEvent event) {
                    Parameter configParam = getConfigParam(PROPERTY_KEY_APP_LOG_LEVEL);
                    if (configParam != null) {
                        boolean isLogDebug = (Boolean) event.getParameter().getValue();
                        if (isLogDebug) {
                            configParam.setValue(SystemUtils.LLS_DEBUG, null);
                        } else {
                            configParam.setValue(SystemUtils.LLS_INFO, null);
                        }
                    }
                }
            });
            configParams.addParameter(param);
        }

        @Override
        protected void initPageUI() {
            JPanel panel = createPageUI();

            setPageUI(panel);
            updatePageUI();
        }

        private JPanel createPageUI() {
            Parameter param;

            JPanel logConfigPane = GridBagUtils.createPanel();
            GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST,weightx=1");
            gbc.gridy = 0;


            param = getConfigParam(PROPERTY_KEY_APP_LOG_ENABLED);
            gbc.insets.top = _LINE_INSET_TOP;
            gbc.gridwidth = 2;
            logConfigPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_APP_LOG_PREFIX);
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            logConfigPane.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            logConfigPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_APP_LOG_ECHO);
            gbc.gridwidth = 2;
            logConfigPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            param = getConfigParam(PROPERTY_KEY_APP_DEBUG_ENABLED);
            gbc.gridwidth = 2;
            logConfigPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            GridBagUtils.setAttributes(gbc, "gridwidth=1,weightx=0");
            param = getConfigParam(PROPERTY_KEY_APP_LOG_LEVEL);
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            logConfigPane.add(param.getEditor().getLabelComponent(), gbc);
            gbc.weightx = 1;
            logConfigPane.add(param.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;

            gbc.gridwidth = 2;
            gbc.insets.top = 25;
            final JLabel label = new JLabel(
                    "Note: changes on this page are not effective until restart of " + getApp().getAppName());
            configureNoteLabel(label);
            logConfigPane.add(label, gbc);
            gbc.gridy++;

            return createPageUIContentPane(logConfigPane);
        }

        @Override
        public void updatePageUI() {
            boolean enabled = (Boolean) getConfigParam(PROPERTY_KEY_APP_LOG_ENABLED).getValue();
            setConfigParamUIEnabled(PROPERTY_KEY_APP_LOG_PREFIX, enabled);
            setConfigParamUIEnabled(PROPERTY_KEY_APP_LOG_ECHO, enabled);
            setConfigParamUIEnabled(PROPERTY_KEY_APP_DEBUG_ENABLED, enabled);
            setConfigParamUIEnabled(PROPERTY_KEY_APP_LOG_LEVEL, enabled);
        }
    }

    public static class RGBImageProfilePage extends DefaultConfigPage {

        public RGBImageProfilePage() {
            setTitle("RGB-Image Profiles"); /*I18N*/
        }

        @Override
        public PropertyMap getConfigParamValues(final PropertyMap propertyMap) {
            return propertyMap;
        }

        @Override
        public void setConfigParamValues(final PropertyMap propertyMap, final ParamExceptionHandler errorHandler) {
        }

        @Override
        protected void initConfigParams(final ParamGroup configParams) {
        }

        @Override
        protected void initPageUI() {
            RGBImageProfilePane _profilePane = new RGBImageProfilePane(new PropertyMap());
            setPageUI(_profilePane);
        }
    }

    public static class ProductSettings extends DefaultConfigPage {

        public ProductSettings() {
            setTitle("Product Settings"); /*I18N*/
        }

        @Override
        protected void initPageUI() {
            JPanel pageUI = createPageUI();
            setPageUI(pageUI);
        }

        @Override
        protected void initConfigParams(ParamGroup configParams) {
            Parameter param = new Parameter(PROPERTY_KEY_GEOLOCATION_EPS,
                    new Float(PROPERTY_DEFAULT_GEOLOCATION_EPS));
            param.getProperties().setLabel("If their geo-locations differ less than: ");/*I18N*/
            param.getProperties().setPhysicalUnit("deg"); /*I18N*/
            param.getProperties().setMinValue(0.0f);
            param.getProperties().setMaxValue(360.0f);
            configParams.addParameter(param);
        }

        private JPanel createPageUI() {
            Parameter param;
            GridBagConstraints gbc;

            final JPanel productCompatibility = GridBagUtils.createPanel();
            productCompatibility.setBorder(UIUtils.createGroupBorder("Product Compatibility")); /*I18N*/
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");

            param = getConfigParam(PROPERTY_KEY_GEOLOCATION_EPS);
            gbc.insets.bottom += 8;
            gbc.gridwidth = 3;
            productCompatibility.add(new JLabel("Consider products as spatially compatible:"), gbc); /*I18N*/
            gbc.insets.bottom -= 8;
            gbc.gridy++;
            GridBagUtils.addToPanel(productCompatibility, param.getEditor().getLabelComponent(), gbc, "gridwidth=1");
            GridBagUtils.addToPanel(productCompatibility, param.getEditor().getEditorComponent(), gbc, "weightx=1");
            GridBagUtils.addToPanel(productCompatibility, param.getEditor().getPhysUnitLabelComponent(), gbc,
                    "weightx=0");

            // UI
            JPanel pageUI = GridBagUtils.createPanel();
            gbc = GridBagUtils.createConstraints("fill=HORIZONTAL, anchor=WEST, weightx=1, gridy=1");

            pageUI.add(productCompatibility, gbc);
            gbc.gridy++;

            return createPageUIContentPane(pageUI);
        }
    }

    public static JPanel createPageUIContentPane(JPanel pane) {
        JPanel contentPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=NORTHWEST");
        gbc.insets.top = _PAGE_INSET_TOP;
        gbc.weightx = 1;
        gbc.weighty = 0;
        contentPane.add(pane, gbc);
        GridBagUtils.addVerticalFiller(contentPane, gbc);
        return contentPane;
    }

    public static void configureNoteLabel(final JLabel noteLabel) {
        if (noteLabel.getFont() != null) {
            noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC));
        }
        noteLabel.setForeground(new Color(0, 0, 92));
    }
}
