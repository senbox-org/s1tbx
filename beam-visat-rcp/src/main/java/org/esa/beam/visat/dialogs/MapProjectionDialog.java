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
package org.esa.beam.visat.dialogs;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductProjectionBuilder;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNodeNameValidator;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.framework.dataop.maptransf.UTM;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.DemSelector;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ProjectionParamsDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * @deprecated since BEAM 4.7, replaced by GPF operator 'Reproject'
 */
@Deprecated
public class MapProjectionDialog extends ModalDialog {

    private static final String RESAMPLING_NEAREST_NEIGHBOUR = "Nearest Neighbour";
    private static final String RESAMPLING_BILINEAR_INTERPOLATION = "Bi-linear Interpolation";
    private static final String RESAMPLING_CUBIC_CONVOLUTION = "Cubic Convolution";

    private static final String _defaultNumberText = "####";
    private static final String _defaultLatLonText = "##°";

    private static final String _orthoHelpId = "orthorectification";
    private static final String _projectionHelpId = "mapProjection";

    private static final String _orthoTitel = "Orthorectification";         /*I18N*/
    private static final String _projectionTitel = "Map Projection";        /*I18N*/

    private static int _numNewProjections = 0;

    private final Product _sourceProduct;
    private Product _outputProduct;
    private MapInfo _outputMapInfo;
    private Exception _exception;
    private boolean _orthorectificationMode;

    private Parameter _paramOldProductName;
    private Parameter _paramOldProductDesc;
    private Parameter _paramNewProductName;
    private Parameter _paramNewProductDesc;
    private Parameter _paramIncludeTiePointGrids;
    private Parameter _paramColocationUsed;
    private Parameter _paramColocationProductName;
    private Parameter _paramProjection;
    private Parameter _paramDatum;
    private Parameter _paramResampling;

    private final Window _parent;
    private JLabel _labelWidthInfo;
    private JLabel _labelHeightInfo;
    private JLabel _labelCenterLatInfo;
    private JLabel _labelCenterLonInfo;
    private JButton _buttonProjectParams;
    private DemSelector _demSelector;

    public MapProjectionDialog(final Window parent, final Product sourceProduct, final boolean orthoFlag) {
        this(parent, sourceProduct, orthoFlag, getTitel(orthoFlag), getHelpId(orthoFlag));
    }

    protected MapProjectionDialog(final Window parent, final Product sourceProduct, final boolean orthoFlag,
                                  final String titel, final String helpID) {
        super(parent, titel, ID_OK_CANCEL_HELP, helpID);
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        _sourceProduct = sourceProduct;
        _parent = parent;
        _orthorectificationMode = orthoFlag;
    }

    @Override
    public int show() {
        initParameters();
        createUI();
        updateUIState();
        return super.show();
    }

    public Product getSourceProduct() {
        return _sourceProduct;
    }

    public Product getOutputProduct() {
        return _outputProduct;
    }

    @Override
    protected void onOK() {
        super.onOK();
        _outputMapInfo.setResampling(getResampling());
        final String prodName = _paramNewProductName.getValueAsText();
        final String prodDesc = _paramNewProductDesc.getValueAsText();
        _outputProduct = null;
        final boolean includeGrids = Boolean.parseBoolean(_paramIncludeTiePointGrids.getValueAsText());
        ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(getJDialog(), getJDialog().getTitle()) {
            @Override
            protected Object doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Applying map projection...", 1);
                try {
                    _outputProduct = ProductProjectionBuilder.createProductProjection(getSourceProduct(),
                                                                                      false, includeGrids,
                                                                                      _outputMapInfo,
                                                                                      prodName, prodDesc);
                    pm.worked(1);
                } finally {
                    pm.done();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    _exception = e;
                    e.printStackTrace();
                }
            }
        };
        worker.executeWithBlocking();

    }

    @Override
    protected void onCancel() {
        super.onCancel();
        _outputProduct = null;
    }


    public Exception getException() {
        return _exception;
    }

    @Override
    protected boolean verifyUserInput() {
        String name = _paramNewProductName.getValueAsText();
        final boolean nameValid = (name != null && name.length() > 0);
        if (!nameValid) {
            showErrorDialog("Please enter a valid product name.");              /*I18N*/
            return false;
        }
        final int sceneWidth = _outputMapInfo.getSceneWidth();
        final int sceneHeight = _outputMapInfo.getSceneHeight();
        if (sceneWidth <= 1 || sceneHeight <= 1) {
            showErrorDialog("Invalid product scene size.\n" +
                            "Please check 'Output Parameters' and adjust\n" +
                            "resulting product scene width and height.");               /*I18N*/
            return false;
        }
        if (_outputMapInfo.isOrthorectified() && _demSelector != null && _demSelector.isUsingExternalDem()) {
            final String demName = _outputMapInfo.getElevationModelName();
            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
            if (demDescriptor == null) {
                showErrorDialog("The DEM '" + demName + "' is not supported.");
                close();
                return false;
            }
            if (demDescriptor.isInstallingDem()) {
                showErrorDialog("The DEM '" + demName + "' is currently being installed.");
                close();
                return false;
            }
            if (!demDescriptor.isDemInstalled()) {
                final boolean ok = demDescriptor.installDemFiles(_parent);
                if (ok) {
                    // close dialog becuase DEM will be installed first
                    close();
                }
                return false;
            }
        }


        return true;
    }

    private void initParameters() {
        _numNewProjections++;

        _paramOldProductName = new Parameter("oldProductname", getSourceProduct().getDisplayName());
        _paramOldProductName.getProperties().setLabel("Name");                  /* I18N */
        _paramOldProductName.getProperties().setReadOnly(true);

        _paramOldProductDesc = new Parameter("oldProductDesc", getSourceProduct().getDescription());
        _paramOldProductDesc.getProperties().setLabel("Description");           /* I18N */
        _paramOldProductDesc.getProperties().setReadOnly(true);

        _paramNewProductName = new Parameter("newProductName",
                                             "proj_" + _numNewProjections + "_"
                                             + getSourceProduct().getName());
        _paramNewProductName.getProperties().setLabel("Name");                  /* I18N */
        _paramNewProductName.getProperties().setNullValueAllowed(false);
        _paramNewProductName.getProperties().setValidatorClass(ProductNodeNameValidator.class);

        _paramNewProductDesc = new Parameter("newProductDesc", getSourceProduct().getDescription());
        _paramNewProductDesc.getProperties().setLabel("Description");           /* I18N */
        _paramNewProductDesc.getProperties().setNullValueAllowed(false);

        _paramIncludeTiePointGrids = new Parameter("includeTiePointGrids", true);
        _paramIncludeTiePointGrids.getProperties().setLabel("Include Tie Point Grids");           /* I18N */
        _paramIncludeTiePointGrids.getProperties().setDescription(
                "Includes the tie point grids of the input product as bands.");

        final ParamChangeListener onRecreateMapInfoAndUpdateUIState = new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                _outputMapInfo = null;
                updateUIState();
            }
        };

        _paramColocationUsed = new Parameter("colocationUsed", Boolean.FALSE);
        _paramColocationUsed.getProperties().setLabel("Collocate with product:");       /*I18N*/
        _paramColocationUsed.addParamChangeListener(onRecreateMapInfoAndUpdateUIState);

        final String[] compatibleProductNames = getCompatibleProductNames();
        _paramColocationProductName = new Parameter("colocationProductName", (String) null);
        _paramColocationProductName.getProperties().setLabel(
                "Product which provides the map-projection:");         /*I18N*/
        _paramColocationProductName.getProperties().setValueSetBound(true);
        _paramColocationProductName.getProperties().setValueSet(compatibleProductNames);
        if (compatibleProductNames.length > 0) {
            _paramColocationProductName.setValue(compatibleProductNames[0], null);
            _paramColocationProductName.addParamChangeListener(onRecreateMapInfoAndUpdateUIState);
        }

        String defaultMapProjectionName = IdentityTransformDescriptor.NAME;
        String[] projectionsValueSet = getProjectionsValueSet();
        Arrays.sort(projectionsValueSet);
        _paramProjection = new Parameter("projection", projectionsValueSet[0]);
        _paramProjection.getProperties().setValueSet(projectionsValueSet);
        _paramProjection.getProperties().setLabel("Projection"); /* I18N */
        _paramProjection.getProperties().setValueSetBound(true);
        _paramProjection.setValue(defaultMapProjectionName, null);
        _paramProjection.addParamChangeListener(onRecreateMapInfoAndUpdateUIState);

        // @todo 2 nf/nf - add support for multiple datums
        String defaultDatumName = getSourceProduct().getGeoCoding().getDatum().getName();
        String[] projectionDatumValueSet = new String[]{defaultDatumName};
        Arrays.sort(projectionDatumValueSet);
        _paramDatum = new Parameter("projectionDatum", projectionDatumValueSet[0]);
        _paramDatum.getProperties().setValueSet(projectionDatumValueSet);
        _paramDatum.getProperties().setLabel("Datum"); /* I18N */
        _paramDatum.getProperties().setValueSetBound(true);
        _paramDatum.setValue(defaultDatumName, null);
        _paramDatum.addParamChangeListener(onRecreateMapInfoAndUpdateUIState);

        String[] resamplingValueSet = new String[]{
                RESAMPLING_NEAREST_NEIGHBOUR,
                RESAMPLING_BILINEAR_INTERPOLATION,
                RESAMPLING_CUBIC_CONVOLUTION
        };

        _paramResampling = new Parameter("resampling", resamplingValueSet[0]);
        _paramResampling.getProperties().setValueSet(resamplingValueSet);
        _paramResampling.getProperties().setValueSetBound(true);
        _paramResampling.getProperties().setLabel("Resampling Method:");
        _paramResampling.getProperties().setDescription("The method to be used for pixel resampling");
    }

    private static String[] getProjectionsValueSet() {
        final MapProjection[] projections = MapProjectionRegistry.getProjections();
        String[] projectionsValueSet = new String[projections.length];
        for (int i = 0; i < projectionsValueSet.length; i++) {
            projectionsValueSet[i] = projections[i].getName();
        }
        return projectionsValueSet;
    }

    private void createUI() {
        int line = 0;
        final JPanel dialogPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, createInputOutputPane(), gbc,
                                "fill=HORIZONTAL, anchor=NORTHWEST, weightx=1, weighty=0");

        if (_orthorectificationMode) {
            gbc.gridy = ++line;
            _demSelector = createDemSelector();
            GridBagUtils.addToPanel(dialogPane, _demSelector, gbc,
                                    "insets.top=15, fill=HORIZONTAL, anchor=NORTHWEST, weightx=1, weighty=0");
        }

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, createProjectionPane(), gbc,
                                "insets.top=15, fill=HORIZONTAL, anchor=NORTHWEST, weightx=1, weighty=0");


        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, createInfoPane(), gbc,
                                "insets.top=15, fill=HORIZONTAL, anchor=NORTHWEST, weightx=1, weighty=0");

        gbc.gridy = ++line;
        final JPanel spacer = GridBagUtils.createPanel();
        GridBagUtils.addToPanel(dialogPane, spacer, gbc,
                                "insets.top=15, fill=HORIZONTAL, anchor=NORTHWEST, weightx=1, weighty=1");

        setContent(dialogPane);
    }


    private JPanel createInputOutputPane() {
        int line = 0;
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbc, "fill=HORIZONTAL, weightx=1");
        final JPanel inOutPane = GridBagUtils.createPanel();

        inOutPane.setBorder(BorderFactory.createTitledBorder("Input/Output"));          /*I18N*/

        GridBagUtils.addToPanel(inOutPane, new JLabel("Input Product:"), gbc,
                                "weightx=1, gridwidth=2, insets.top = 3");        /*I18N*/
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(inOutPane, _paramOldProductName.getEditor().getLabelComponent(), gbc,
                                "gridwidth=1, insets.top = 3");
        GridBagUtils.addToPanel(inOutPane, _paramOldProductName.getEditor().getComponent(), gbc,
                                "weightx=999, insets.top = 3");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(inOutPane, _paramOldProductDesc.getEditor().getLabelComponent(), gbc,
                                "weightx=1, insets.top = 3");
        GridBagUtils.addToPanel(inOutPane, _paramOldProductDesc.getEditor().getComponent(), gbc,
                                "weightx=999, insets.top = 3");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(inOutPane, new JLabel("Output Product:"), gbc,
                                "weightx=1, gridwidth=2, insets.top = 6");        /*I18N*/
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(inOutPane, _paramNewProductName.getEditor().getLabelComponent(), gbc,
                                "gridwidth=1, insets.top = 3");
        GridBagUtils.addToPanel(inOutPane, _paramNewProductName.getEditor().getComponent(), gbc,
                                "weightx=999, insets.top = 3");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(inOutPane, _paramNewProductDesc.getEditor().getLabelComponent(), gbc,
                                "weightx=1, insets.top = 3");
        GridBagUtils.addToPanel(inOutPane, _paramNewProductDesc.getEditor().getComponent(), gbc,
                                "weightx=999, insets.top = 3");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(inOutPane, _paramIncludeTiePointGrids.getEditor().getComponent(), gbc,
                                "weightx=999, insets.top = 3");

        final JComponent jComponent = _paramNewProductName.getEditor().getEditorComponent();
        if (jComponent instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) jComponent;
            tc.requestFocus();
        }

        setCaretPosOfParameter(_paramOldProductName, 0);
        setCaretPosOfParameter(_paramOldProductDesc, 0);
        setCaretPosOfParameter(_paramNewProductDesc, 0);

        return inOutPane;
    }

    private DemSelector createDemSelector() {
        return new DemSelector(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState();
            }
        });
    }

    private JPanel createProjectionPane() {
        int line = 0;
        final JPanel panel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        panel.setBorder(BorderFactory.createTitledBorder("Projection"));

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramColocationUsed.getEditor().getComponent(), gbc, "insets.top=3");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramColocationProductName.getEditor().getComponent(), gbc, "insets.top=2");

        GridBagUtils.setAttributes(gbc, "insets.top=3, fill=HORIZONTAL");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramProjection.getEditor().getLabelComponent(), gbc, "weightx=1");
        GridBagUtils.addToPanel(panel, _paramDatum.getEditor().getLabelComponent(), gbc, "weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramProjection.getEditor().getComponent(), gbc, "weightx=0");
        GridBagUtils.addToPanel(panel, _paramDatum.getEditor().getComponent(), gbc, "weightx=1");
        gbc.gridy = ++line;

        JButton buttonOutputParams = new JButton("Output Parameters...");
        buttonOutputParams.setName("outputParamsButton");
        buttonOutputParams.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showOutputParamsDialog();
            }
        });

        _buttonProjectParams = new JButton("Projection Parameters...");
        _buttonProjectParams.setName("projectParamsButton");
        _buttonProjectParams.setEnabled(false);
        _buttonProjectParams.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showProjectionParameterDialog();
            }
        });

        gbc.insets.top = 6;
        gbc.gridx = 0;
        GridBagUtils.addToPanel(panel, _paramResampling.getEditor().getLabelComponent(), gbc,
                                "gridwidth=1, fill=NONE, anchor=SOUTHWEST, weightx=999");
        gbc.gridx = 1;
        GridBagUtils.addToPanel(panel, _buttonProjectParams, gbc, "gridwidth=2, fill=NONE, anchor=EAST, weightx=1");

        gbc.gridy = ++line;
        gbc.insets.top = 3;
        gbc.gridx = 0;
        GridBagUtils.addToPanel(panel, _paramResampling.getEditor().getComponent(), gbc,
                                "gridwidth=1, fill=NONE, anchor=WEST, weightx=0");
        gbc.gridx = 1;
        GridBagUtils.addToPanel(panel, buttonOutputParams, gbc, "gridwidth=2, fill=HORIZONTAL, anchor=EAST, weightx=1");

        return panel;
    }

    private JPanel createInfoPane() {
        _labelWidthInfo = new JLabel(_defaultNumberText);
        _labelHeightInfo = new JLabel(_defaultNumberText);
        _labelCenterLatInfo = new JLabel(_defaultLatLonText);
        _labelCenterLonInfo = new JLabel(_defaultLatLonText);

        final JPanel infoPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        infoPanel.setBorder(UIUtils.createGroupBorder("Output Product Information"));                       /*I18N*/
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(infoPanel, new JLabel("Scene Width:"), gbc);                                /*I18N*/
        GridBagUtils.addToPanel(infoPanel, _labelWidthInfo, gbc);
        GridBagUtils.addToPanel(infoPanel, new JLabel("pixel"), gbc, "weightx=1");                          /*I18N*/
        GridBagUtils.addToPanel(infoPanel, new JLabel("Scene Height:"), gbc,
                                "gridy=1, weightx=0");                    /*I18N*/
        GridBagUtils.addToPanel(infoPanel, _labelHeightInfo, gbc);
        GridBagUtils.addToPanel(infoPanel, new JLabel("pixel"), gbc, "weightx=1");                          /*18N*/

        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Longitude:"), gbc,
                                "gridy=0, weightx=0, insets.left=50");   /*I18N*/
        GridBagUtils.addToPanel(infoPanel, _labelCenterLonInfo, gbc, "weightx=1, insets.left=0");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Latitude:"), gbc,
                                "gridy=1, weightx=0, insets.left=50");    /*I18N*/
        GridBagUtils.addToPanel(infoPanel, _labelCenterLatInfo, gbc, "weightx=1, insets.left=0");
        return infoPanel;
    }

    private void showProjectionParameterDialog() {
        final MapProjection projection = _outputMapInfo.getMapProjection();
        final MapTransformUI transformUI = projection.getMapTransformUI();
        final ProjectionParamsDialog dialog = new ProjectionParamsDialog(_parent, transformUI);
        if (dialog.show() == ID_OK) {
            projection.setMapTransform(transformUI.createTransform());
            final int dialogAnswer = JOptionPane.showConfirmDialog(getParent(),
                                                                   "Projection parameters have been changed.\n\n" +
                                                                   "Adjust the output parameters?", /*I18N*/
                                                                   getTitel(
                                                                           _orthorectificationMode),
                                                                   JOptionPane.YES_NO_OPTION);
            if (dialogAnswer == JOptionPane.YES_OPTION) {
                _outputMapInfo = ProductUtils.createSuitableMapInfo(getSourceProduct(),
                                                                    projection,
                                                                    _outputMapInfo.getOrientation(),
                                                                    _outputMapInfo.getNoDataValue());
            }

            updateUIState();
        }
    }

    private void showOutputParamsDialog() {
        final boolean editable = !(Boolean) _paramColocationUsed.getValue();
        final OutputParamsDialog dialog = new OutputParamsDialog(_parent, _outputMapInfo, getSourceProduct(), editable);
        if (dialog.show() == ID_OK) {
            _outputMapInfo = dialog.getMapInfo();
            updateUIState();
        }
    }

    private void updateUIState() {

        boolean colocationUsed = (Boolean) _paramColocationUsed.getValue();
        boolean canUseColocation = _paramColocationProductName.getProperties().getValueSet().length > 0;
        if (colocationUsed && !canUseColocation) {
            _paramColocationUsed.setValue(Boolean.FALSE, null);
            colocationUsed = false;
        }

        if (colocationUsed) {
            final String refProductName = (String) _paramColocationProductName.getValue();
            final Product refProduct = getSourceProduct().getProductManager().getProduct(refProductName);
            final MapGeoCoding refGeoCoding = (MapGeoCoding) refProduct.getGeoCoding();
            final MapInfo refMapInfo = refGeoCoding.getMapInfo();
            _paramProjection.setValueAsText(refMapInfo.getMapProjection().getName(), null);
            _outputMapInfo = refMapInfo.createDeepClone();
            _outputMapInfo.setSceneSizeFitted(false);
        } else {
            if (_outputMapInfo == null) {
                // todo - if source product has a map geo-coding, init params with map-info from source product first
                final String projectionName = _paramProjection.getValueAsText();
                MapProjection projection;
                if (UTM.AUTO_PROJECTION_NAME.equals(projectionName)) {
                    final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(getSourceProduct());
                    projection = UTM.getSuitableProjection(centerGeoPos);
                } else {
                    projection = MapProjectionRegistry.getProjection(projectionName);
                }
                Assert.state(projection != null, "projection != null");
                if (!projection.isPreDefined()) {
                    projection = (MapProjection) projection.clone();
                }
                // enable/disable projection parameter button depending on the projection needs
                _buttonProjectParams.setEnabled(!projection.isPreDefined() && projection.hasMapTransformUI());

                float orientation = 0.0f;
                double defaultNoDataValue = MapInfo.DEFAULT_NO_DATA_VALUE;
                if (getSourceProduct().getGeoCoding() instanceof MapGeoCoding) {
                    final MapInfo mapInfo = ((MapGeoCoding) getSourceProduct().getGeoCoding()).getMapInfo();
                    orientation = mapInfo.getOrientation();
                    defaultNoDataValue = mapInfo.getNoDataValue();
                }

                _outputMapInfo = ProductUtils.createSuitableMapInfo(getSourceProduct(),
                                                                    projection,
                                                                    orientation,
                                                                    defaultNoDataValue);
            }
        }

        _outputMapInfo.setOrthorectified(_orthorectificationMode);
        if (_orthorectificationMode && _demSelector.isUsingExternalDem()) {
            _outputMapInfo.setElevationModelName(_demSelector.getDemName());
        } else {
            _outputMapInfo.setElevationModelName(null);
        }
        _outputMapInfo.setResampling(getResampling());

        _paramColocationUsed.setUIEnabled(canUseColocation);
        _paramColocationProductName.setUIEnabled(colocationUsed);
        _paramProjection.setUIEnabled(!colocationUsed);
        _paramProjection.setValue(_outputMapInfo.getMapProjection().getName(), null);
        _paramDatum.setUIEnabled(!colocationUsed);

        if (_outputMapInfo != null) {
            final int width = _outputMapInfo.getSceneWidth();
            final int height = _outputMapInfo.getSceneHeight();
            final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(getSourceProduct());
            _labelWidthInfo.setText(String.valueOf(width));
            _labelHeightInfo.setText(String.valueOf(height));
            _labelCenterLonInfo.setText(centerGeoPos.getLonString());
            _labelCenterLatInfo.setText(centerGeoPos.getLatString());
        } else {
            _labelWidthInfo.setText(_defaultNumberText);
            _labelHeightInfo.setText(_defaultNumberText);
            _labelCenterLonInfo.setText(_defaultLatLonText);
            _labelCenterLatInfo.setText(_defaultLatLonText);
        }
    }

    private Resampling getResampling() {
        final String resamplingName = _paramResampling.getValueAsText();
        final Resampling resampling;
        if (resamplingName.equals(RESAMPLING_NEAREST_NEIGHBOUR)) {
            resampling = Resampling.NEAREST_NEIGHBOUR;
        } else if (resamplingName.equals(RESAMPLING_BILINEAR_INTERPOLATION)) {
            resampling = Resampling.BILINEAR_INTERPOLATION;
        } else if (resamplingName.equals(RESAMPLING_CUBIC_CONVOLUTION)) {
            resampling = Resampling.CUBIC_CONVOLUTION;
        } else {
            throw new IllegalStateException("unknown resampling: " + resamplingName);
        }
        return resampling;
    }

    private String[] getCompatibleProductNames() {
        final GeneralPath sourcePath = ProductUtils.createGeoBoundaryPath(getSourceProduct());
        final Rectangle2D sourceBounds = sourcePath.getBounds2D();
        final ArrayList<String> compatibleProducts = new ArrayList<String>(5);
        final ProductManager productManager = getSourceProduct().getProductManager();
        for (int i = 0; i < productManager.getProductCount(); i++) {
            final Product product = productManager.getProduct(i);
            if (product != getSourceProduct()) {
                final GeoCoding otherGeoCoding = product.getGeoCoding();
                if (otherGeoCoding instanceof MapGeoCoding) {
                    final GeneralPath otherPath = ProductUtils.createGeoBoundaryPath(product);
                    final Rectangle2D otherBounds = otherPath.getBounds2D();
                    if (sourcePath.intersects(otherBounds) || otherPath.intersects(sourceBounds)) {
                        compatibleProducts.add(product.getName());
                    }
                }
            }
        }
        return compatibleProducts.toArray(new String[compatibleProducts.size()]);
    }

    private static String getHelpId(final boolean orthoFlag) {
        return orthoFlag ? _orthoHelpId : _projectionHelpId;
    }

    private static String getTitel(final boolean orthoFlag) {
        return orthoFlag ? _orthoTitel : _projectionTitel;
    }

    private static void setCaretPosOfParameter(Parameter param, int caretPos) {
        final JComponent jComponent = param.getEditor().getEditorComponent();
        if (jComponent instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) jComponent;
            tc.setCaretPosition(caretPos);
        }
    }

}


