/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.ocean.toolviews.polarview;

import org.esa.s1tbx.ocean.toolviews.polarview.polarplot.Axis;
import org.esa.s1tbx.ocean.toolviews.polarview.polarplot.ColourScale;
import org.esa.s1tbx.ocean.toolviews.polarview.polarplot.PolarCanvas;
import org.esa.s1tbx.ocean.toolviews.polarview.polarplot.PolarData;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.FileFolderUtils;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;

/**
 * Polar plot panel
 */
public final class PolarView extends JPanel implements ActionListener, PopupMenuListener, MouseListener, MouseMotionListener {

    private Product product;
    private int currentRecord = 0;

    private final String[] unitTypes = new String[]{"Real", "Imaginary", "Amplitude", "Intensity"};

    private ControlPanel controlPanel;
    private PolarPanel polarPanel;
    private Component emptyPanel;

    private SpectraData.SpectraUnit spectraUnit;
    private SpectraData.WaveProductType waveProductType;
    private SpectraData spectraData;

    public static final Color colourTable[] = (new Color[]{
            new Color(255, 255, 255), new Color(0, 0, 255), new Color(0, 255, 255),
            new Color(0, 255, 0), new Color(255, 255, 0), new Color(255, 0, 0)
    });
    private static final double rings[] = {50.0, 100.0, 200.0};
    private static final String ringTextStrings[] = {"200 m", "100 m", "50 m"};

    public PolarView() {

        addMouseListener(this);
        addMouseMotionListener(this);

        this.setLayout(new BorderLayout());

        emptyPanel = createEmptyPanel();
        this.add(emptyPanel, BorderLayout.NORTH);
        polarPanel = new PolarPanel();
        this.add(polarPanel, BorderLayout.CENTER);
        controlPanel = new ControlPanel(this);
        this.add(controlPanel, BorderLayout.SOUTH);

        enablePlot(false);
    }

    private Component createEmptyPanel() {
        return new JLabel("<html>This tool window is used to analyse<br>" +
                                  "<b>Level-2 Ocean Swell</b> data in a polar plot.<br>" +
                                  "Please open and select a Sentinel-1 L2 OCN WV<br>" +
                                  "or an ASAR L2 WV product.", SwingConstants.CENTER);
    }

    private void enablePlot(final boolean flag) {
        emptyPanel.setVisible(!flag);
        polarPanel.setVisible(flag);
        controlPanel.setVisible(flag);
    }

    public void setProduct(final Product prod) {
        if(product == prod) {
            return;
        }
        this.product = prod;
        if (product == null) {
            enablePlot(false);
            return;
        }

        switch (product.getProductType()) {
            case "ASA_WVW_2P":
                waveProductType = SpectraData.WaveProductType.WAVE_SPECTRA;
                spectraUnit = SpectraData.SpectraUnit.AMPLITUDE;
                spectraData = new SpectraDataAsar(product, waveProductType);
                break;
            case "ASA_WVS_1P":
                waveProductType = SpectraData.WaveProductType.CROSS_SPECTRA;
                spectraUnit = SpectraData.SpectraUnit.INTENSITY;
                spectraData = new SpectraDataAsar(product, waveProductType);
                break;
            case "OCN":
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                final String mode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
                if(mode.equals("WV")) {
                    waveProductType = SpectraData.WaveProductType.S1_OCN_WV;
                    spectraUnit = SpectraData.SpectraUnit.AMPLITUDE;
                    spectraData = new SpectraDataSentinel1(product, waveProductType);
                }
                break;
            default:
                waveProductType = null;
                spectraData = null;
                break;
        }

        if (waveProductType != null) {
            currentRecord = 0;      // reset to 0
            createPlot(currentRecord);
        }
        enablePlot(waveProductType != null);
    }

    public void removeProduct(final Product product) {
        if (this.product == product) {
            this.product = null;
            enablePlot(false);
        }
    }

    private void createPlot(final int rec) {
        try {
            final String[] readouts = spectraData.getSpectraMetadata(rec);
            polarPanel.setMetadata(readouts);

            final PolarCanvas polarCanvas = polarPanel.getPolarCanvas();
            polarCanvas.setAxisNames("Azimuth", "Range");

            if (waveProductType == SpectraData.WaveProductType.WAVE_SPECTRA) {
                polarCanvas.setWindDirection(spectraData.getWindDirection());
                polarCanvas.showWindDirection(true);
                polarCanvas.setAxisNames("North", "East");
            }

            final PolarData data = spectraData.getPolarData(currentRecord, spectraUnit);

            final double colourRange[] = {(double) data.getMinValue(), (double) data.getMaxValue()};
            final double radialRange[] = {spectraData.getMinRadius(), spectraData.getMaxRadius()};

            final Axis colourAxis = polarCanvas.getColourAxis();
            final Axis radialAxis = polarCanvas.getRadialAxis();
            colourAxis.setDataRange(colourRange);
            colourAxis.setUnit(unitTypes[spectraUnit.ordinal()]);
            radialAxis.setAutoRange(false);
            radialAxis.setDataRange(radialRange);
            radialAxis.setRange(radialRange[0], radialRange[1], 4);
            radialAxis.setTitle("Wavelength (m)");
            polarCanvas.setRings(rings, ringTextStrings);
            data.setColorScale(ColourScale.newCustomScale(colourRange));
            polarCanvas.setData(data);

            repaint();
            controlPanel.updateControls();
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to read OSW data from product ", e);
        }
    }

    public JPopupMenu createPopupMenu(final MouseEvent event) {
        final JPopupMenu popup = new JPopupMenu();

        final JMenuItem itemNext = createMenuItem("Next");
        popup.add(itemNext);
        itemNext.setEnabled(currentRecord < spectraData.getNumRecords());

        final JMenuItem itemPrev = createMenuItem("Previous");
        popup.add(itemPrev);
        itemPrev.setEnabled(currentRecord > 0);

        final JMenuItem itemColourScale = createMenuItem("Colour Scale");
        popup.add(itemColourScale);

        final JMenu unitMenu = new JMenu("Unit");
        popup.add(unitMenu);

        if (waveProductType == SpectraData.WaveProductType.WAVE_SPECTRA) {
            createCheckedMenuItem(unitTypes[SpectraData.SpectraUnit.AMPLITUDE.ordinal()], unitMenu, spectraUnit == SpectraData.SpectraUnit.AMPLITUDE);
            createCheckedMenuItem(unitTypes[SpectraData.SpectraUnit.INTENSITY.ordinal()], unitMenu, spectraUnit == SpectraData.SpectraUnit.INTENSITY);
        } else {
            createCheckedMenuItem(unitTypes[SpectraData.SpectraUnit.REAL.ordinal()], unitMenu, spectraUnit == SpectraData.SpectraUnit.REAL);
            createCheckedMenuItem(unitTypes[SpectraData.SpectraUnit.IMAGINARY.ordinal()], unitMenu, spectraUnit == SpectraData.SpectraUnit.IMAGINARY);
            createCheckedMenuItem(unitTypes[SpectraData.SpectraUnit.AMPLITUDE.ordinal()], unitMenu, spectraUnit == SpectraData.SpectraUnit.AMPLITUDE);
            createCheckedMenuItem(unitTypes[SpectraData.SpectraUnit.INTENSITY.ordinal()], unitMenu, spectraUnit == SpectraData.SpectraUnit.INTENSITY);
        }

        final JMenuItem itemExportReadout = createMenuItem("Export Readouts");
        popup.add(itemExportReadout);

        popup.setLabel("Justification");
        popup.setBorder(new BevelBorder(BevelBorder.RAISED));
        popup.addPopupMenuListener(this);
        popup.show(this, event.getX(), event.getY());

        return popup;
    }

    private JMenuItem createMenuItem(final String name) {
        final JMenuItem item = new JMenuItem(name);
        item.setHorizontalTextPosition(JMenuItem.RIGHT);
        item.addActionListener(this);
        return item;
    }

    private JCheckBoxMenuItem createCheckedMenuItem(final String name, final JMenu parent, boolean state) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
        item.setHorizontalTextPosition(JMenuItem.RIGHT);
        item.addActionListener(this);
        parent.add(item);
        return item;
    }

    /**
     * Handles menu item pressed events
     *
     * @param event the action event
     */
    public void actionPerformed(final ActionEvent event) {

        if (event.getActionCommand().equals("Next")) {
            showNextPlot();
        } else if (event.getActionCommand().equals("Previous")) {
            showPreviousPlot();
        } else if (event.getActionCommand().equals("Colour Scale")) {
            callColourScaleDlg();
        } else if (event.getActionCommand().equals("Export Readouts")) {
            exportReadouts();
        } else if (event.getActionCommand().equals("Real")) {
            spectraUnit = SpectraData.SpectraUnit.REAL;
            createPlot(currentRecord);
        } else if (event.getActionCommand().equals("Imaginary")) {
            spectraUnit = SpectraData.SpectraUnit.IMAGINARY;
            createPlot(currentRecord);
        } else if (event.getActionCommand().equals("Amplitude")) {
            spectraUnit = SpectraData.SpectraUnit.AMPLITUDE;
            createPlot(currentRecord);
        } else if (event.getActionCommand().equals("Intensity")) {
            spectraUnit = SpectraData.SpectraUnit.INTENSITY;
            createPlot(currentRecord);
        }
    }

    int getCurrentRecord() {
        return currentRecord;
    }

    int getNumRecords() {
        return spectraData != null ? spectraData.getNumRecords() : 0;
    }

    void showNextPlot() {
        createPlot(++currentRecord);
    }

    void showPreviousPlot() {
        createPlot(--currentRecord);
    }

    void showPlot(int record) {
        currentRecord = record;
        createPlot(currentRecord);
    }

    void zoomOut() {

        createPlot(currentRecord);
    }

    void zoomIn() {

        createPlot(currentRecord);
    }

    private void callColourScaleDlg() {
        final PolarCanvas polarCanvas = polarPanel.getPolarCanvas();
        final ColourScaleDialog dlg = new ColourScaleDialog(polarCanvas.getColourAxis());
        dlg.show();
    }

    private void exportReadouts() {
        final File file = FileFolderUtils.GetFilePath("Export Wave Mode Readout", "txt", "txt",
                                                      product.getName() + "_rec" + currentRecord, "Wave mode readout", true);
        try {
            polarPanel.exportReadout(file);
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to export file " + file.toString() + ": " + e.getMessage(), e);
        }
    }

    private void checkPopup(final MouseEvent e) {
        if (e.isPopupTrigger()) {
            createPopupMenu(e);
        }
    }

    public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
    }

    public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
    }

    public void popupMenuCanceled(final PopupMenuEvent e) {
    }

    /**
     * Handle mouse pressed event
     *
     * @param e the mouse event
     */
    public void mousePressed(final MouseEvent e) {
        checkPopup(e);
    }

    /**
     * Handle mouse clicked event
     *
     * @param e the mouse event
     */
    public void mouseClicked(final MouseEvent e) {
        checkPopup(e);

        final Object src = e.getSource();
        final PolarCanvas polarCanvas = polarPanel.getPolarCanvas();
        if (src == polarCanvas) {
            final Axis axis = polarCanvas.selectAxis(e.getPoint());
            if (axis != null && axis == polarCanvas.getColourAxis()) {
                callColourScaleDlg();
            }
        }
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mouseReleased(final MouseEvent e) {
        checkPopup(e);
    }

    public void mouseDragged(final MouseEvent e) {
    }

    /**
     * Handle mouse moved event
     *
     * @param e the mouse event
     */
    public void mouseMoved(final MouseEvent e) {
        updateReadout(e);
    }

    private void updateReadout(final MouseEvent evt) {

        final double rTh[] = polarPanel.getPolarCanvas().getRTheta(evt.getPoint());
        if (rTh != null) {
            final String[] readouts = spectraData.updateReadouts(rTh, currentRecord);
            polarPanel.setReadout(readouts);

        } else {
            polarPanel.setReadout(null);
        }
        repaint();
    }

}
