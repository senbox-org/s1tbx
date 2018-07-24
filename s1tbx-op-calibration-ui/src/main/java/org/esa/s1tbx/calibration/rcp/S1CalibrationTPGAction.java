/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.calibration.rcp;

import org.esa.s1tbx.calibration.gpf.Sentinel1RemoveThermalNoiseOp;
import org.esa.s1tbx.calibration.gpf.calibrators.Sentinel1Calibrator;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * S1CalibrationTPGAction action.
 */
@ActionID(category = "Raster", id = "S1CalibrationTPGAction")
@ActionRegistration(displayName = "#CTL_S1CalibrationTPGActionName")
@ActionReference(path = "Menu/Radar/Radiometric", position = 800)
@NbBundle.Messages({
        "CTL_S1CalibrationTPGActionName=Create Calibration LUT TPG",
        "CTL_S1CalibrationTPGActionDescription=Creates a tie point grid from calibration and noise vectors"
})
public class S1CalibrationTPGAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    private final static String CALIBRATION_VECTOR_TPG = "CalibrationVector";
    private final static String NOISE_VECTOR_TPG = "NoiseVector";

    public S1CalibrationTPGAction() {
        this(Utilities.actionsGlobalContext());
    }

    public S1CalibrationTPGAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<Product> lkpContext = lkp.lookupResult(Product.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();

        putValue(NAME, Bundle.CTL_S1CalibrationTPGActionName());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_S1CalibrationTPGActionDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new S1CalibrationTPGAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    public void setEnableState() {
        final Product product = lkp.lookup(Product.class);
        boolean enable = false;
        if (product != null) {
            final InputProductValidator validator = new InputProductValidator(product);
            enable = validator.isSentinel1Product();
        }
        setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final Product product = lkp.lookup(Product.class);
        if (product != null) {
            final S1CalibrationTPGDialog dlg = new S1CalibrationTPGDialog();
            dlg.show();

            if (dlg.IsOK()) {
                try {
                    Sentinel1Utils su = new Sentinel1Utils(product);
                    int numSubswaths = su.getNumOfSubSwath();

                    List<String> selectedPolList = new ArrayList<>();
                    for (String pol : OperatorUtils.getPolarisations(product)) {
                        if (pol != null) {
                            selectedPolList.add(pol.toUpperCase());
                        }
                    }

                    boolean doSigma0 = dlg.doSigma0();
                    boolean doGamma = dlg.doGamma();
                    boolean doBeta0 = dlg.doBeta0();
                    boolean doDN = dlg.doDN();
                    boolean doNoise = dlg.doNoise();

                    if (doSigma0 || doGamma || doBeta0 || doDN) {
                        Sentinel1Calibrator.CalibrationInfo[] calInfo =
                                Sentinel1Calibrator.getCalibrationVectors(product, selectedPolList,
                                        doSigma0, doBeta0, doGamma, doDN);

                        addCalibrationTiePointGrids(product, calInfo, numSubswaths,
                                doSigma0, doGamma, doBeta0, doDN);
                    }

                    if (doNoise) {
                        MetadataElement origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(product);

                        Sentinel1RemoveThermalNoiseOp.ThermalNoiseInfo[] noiseList =
                                Sentinel1RemoveThermalNoiseOp.getThermalNoiseVectors(
                                        origMetadataRoot, selectedPolList, numSubswaths);

                        addNoiseTiePointGrids(product, noiseList, numSubswaths);
                    }

                } catch (Exception e) {
                    SnapApp.getDefault().handleError("Unable to add TPG", e);
                }
            }
        }
    }

    private void addCalibrationTiePointGrids(final Product product, final Sentinel1Calibrator.CalibrationInfo[] calInfoList,
                                             final int numSubswaths,
                                             boolean doSigma0, boolean doGamma, boolean doBeta0, boolean doDN) {

        for (Sentinel1Calibrator.CalibrationInfo calInfo : calInfoList) {
            final Band referenceBand = findBand(product, numSubswaths, calInfo.subSwath, calInfo.polarization);
            final int gridWidth = calInfo.calibrationVectorList[0].pixels.length;
            final int gridHeight = calInfo.calibrationVectorList.length;
            final int gridSize = gridWidth * gridHeight;
            final String suffix = '_' + calInfo.subSwath + '_' + calInfo.polarization;

            if (doSigma0) {
                final String name = CALIBRATION_VECTOR_TPG + '_' + "Sigma0LUT" + suffix;
                final float[] data = new float[gridSize];

                int i = 0;
                for (Sentinel1Utils.CalibrationVector calVec : calInfo.calibrationVectorList) {
                    for (float value : calVec.sigmaNought) {
                        data[i++] = value;
                    }
                }
                addTPG(product, gridWidth, gridHeight, referenceBand, name, data);
            }
            if (doGamma) {
                final String name = CALIBRATION_VECTOR_TPG + '_' + "GammaLUT" + suffix;
                final float[] data = new float[gridSize];

                int i = 0;
                for (Sentinel1Utils.CalibrationVector calVec : calInfo.calibrationVectorList) {
                    for (float value : calVec.gamma) {
                        data[i++] = value;
                    }
                }
                addTPG(product, gridWidth, gridHeight, referenceBand, name, data);
            }
            if (doBeta0) {
                final String name = CALIBRATION_VECTOR_TPG + '_' + "Beta0LUT" + suffix;
                final float[] data = new float[gridSize];

                int i = 0;
                for (Sentinel1Utils.CalibrationVector calVec : calInfo.calibrationVectorList) {
                    for (float value : calVec.betaNought) {
                        data[i++] = value;
                    }
                }
                addTPG(product, gridWidth, gridHeight, referenceBand, name, data);
            }
            if (doDN) {
                final String name = CALIBRATION_VECTOR_TPG + '_' + "DNLUT" + suffix;
                final float[] data = new float[gridSize];

                int i = 0;
                for (Sentinel1Utils.CalibrationVector calVec : calInfo.calibrationVectorList) {
                    for (float value : calVec.dn) {
                        data[i++] = value;
                    }
                }
                addTPG(product, gridWidth, gridHeight, referenceBand, name, data);
            }
        }
    }

    private void addTPG(final Product product, final int gridWidth, final int gridHeight,
                        final Band referenceBand,
                        final String name, final float[] data) {

        final int sceneRasterWidth = referenceBand.getRasterWidth();
        final int sceneRasterHeight = referenceBand.getRasterHeight();
        final double subSamplingX = (double) sceneRasterWidth / (gridWidth - 1);
        final double subSamplingY = (double) sceneRasterHeight / (gridHeight - 1);

        if (product.getTiePointGrid(name) == null) {
            final TiePointGrid tpg = new TiePointGrid(name,
                    gridWidth, gridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, data);
            tpg.setUnit(Unit.INTENSITY);
            product.addTiePointGrid(tpg);
        }
    }

    private void addNoiseTiePointGrids(final Product product,
                                       final Sentinel1RemoveThermalNoiseOp.ThermalNoiseInfo[] noiseList,
                                       final int numSubswaths) {

        for (Sentinel1RemoveThermalNoiseOp.ThermalNoiseInfo noiseInfo : noiseList) {
            final Band referenceBand = findBand(product, numSubswaths, noiseInfo.subSwath, noiseInfo.polarization);
            final int gridWidth = noiseInfo.noiseVectorList[0].pixels.length + 1;
            final int gridHeight = noiseInfo.noiseVectorList.length;
            final int gridSize = gridWidth * gridHeight;
            final String suffix = '_' + noiseInfo.subSwath + '_' + noiseInfo.polarization;

            final String name = NOISE_VECTOR_TPG + suffix;
            final float[] data = new float[gridSize];

            int i = 0;
            for (Sentinel1Utils.NoiseVector noiseVec : noiseInfo.noiseVectorList) {
                for (float value : noiseVec.noiseLUT) {
                    data[i++] = value;
                }
            }
            addTPG(product, gridWidth, gridHeight, referenceBand, name, data);
        }
    }

    private static Band findBand(final Product product, final int numSubswaths,
                                 final String subSwath, final String polarization) {
        Band matchingBand = product.getBandAt(0);
        for (Band band : product.getBands()) {
            String name = band.getName().toUpperCase();
            if (name.contains(polarization)) {
                if (numSubswaths == 1) {
                    return band;
                } else if (name.contains(subSwath)) {
                    return band;
                }
            }
        }
        return matchingBand;
    }
}
