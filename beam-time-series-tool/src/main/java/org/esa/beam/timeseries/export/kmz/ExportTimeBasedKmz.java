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

package org.esa.beam.timeseries.export.kmz;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.kmz.KmlFeature;
import org.esa.beam.util.kmz.KmlFolder;
import org.esa.beam.util.kmz.KmlGroundOverlay;
import org.esa.beam.util.kmz.KmzExporter;
import org.esa.beam.visat.VisatApp;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.BoundingBox;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipOutputStream;

public class ExportTimeBasedKmz extends ExecCommand {

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";
    private final BeamFileFilter kmzFileFilter = new BeamFileFilter("KMZ", "kmz", "KMZ - Google Earth File Format");
    private int level = 2;
    private ProductSceneView view;

    @Override
    public void actionPerformed(CommandEvent event) {
        //todo resolve deprecated calls
        final VisatApp app = VisatApp.getApp();
        view = app.getSelectedProductSceneView();
        final GeoCoding geoCoding = view.getProduct().getGeoCoding();
        boolean isGeographic = false;
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                isGeographic = true;
            }
        } else if (geoCoding instanceof CrsGeoCoding) {
            isGeographic = CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }

        if (isGeographic) {
            final File output = fetchOutputFile(view);
            if (output == null) {
                return;
            }
            final String title = "KMZ Export";
            final ProgressMonitorSwingWorker worker = new KmzSwingWorker(title, output, app);
            worker.executeWithBlocking();
        } else {
            String message = "Product must be in ''Geographic Lat/Lon'' projection.";
            app.showInfoDialog(message, null);
        }
    }

    protected File fetchOutputFile(ProductSceneView sceneView) {
        VisatApp visatApp = VisatApp.getApp();
        final String lastDir = visatApp.getPreferences().getPropertyString(
                IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(kmzFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export time series as time based KMZ"); /* I18N */
        final RasterDataNode refRaster = sceneView.getRaster();
        fileChooser.setCurrentFilename("time_series_" + refRaster.getName());

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int maxLevel = refRaster.getSourceImage().getModel().getLevelCount() - 1;
        maxLevel = maxLevel > 10 ? 10 : maxLevel;

        final JPanel levelPanel = new JPanel(new GridLayout(maxLevel, 1));
        levelPanel.setBorder(BorderFactory.createTitledBorder("Resolution Level"));
        ButtonGroup buttonGroup = new ButtonGroup();
        final RadioButtonActionListener radioButtonListener = new RadioButtonActionListener();
        for (int i = 0; i < maxLevel; i++) {
            String buttonText = Integer.toString(i);
            if (i == 0) {
                buttonText += " (high, very slow)";
            } else if (i == maxLevel - 1) {
                buttonText += " (low, fast)";
            }
            final JRadioButton button = new JRadioButton(buttonText, true);
            buttonGroup.add(button);
            levelPanel.add(button);
            button.addActionListener(radioButtonListener);
        }


        final JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(levelPanel);
        fileChooser.setAccessory(accessory);

        int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        File file = fileChooser.getSelectedFile();

        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            visatApp.getPreferences().setPropertyString(
                    IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                    currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        if (file == null || file.getName().isEmpty()) {
            return null;
        }

        if (!visatApp.promptForOverwrite(file)) {
            return null;
        }

        return file;
    }

    private KmlFeature createKmlFeature() {
        if (view.isRGB()) {
            return null;
        }
        TimeSeriesMapper timeSeriesMapper = TimeSeriesMapper.getInstance();
        AbstractTimeSeries timeSeries = timeSeriesMapper.getTimeSeries(view.getProduct());
        List<Band> bands = timeSeries.getBandsForVariable(
                AbstractTimeSeries.rasterToVariableName(view.getRaster().getName()));

        if (bands.isEmpty()) {
            return null;
        }
        RasterDataNode refRaster = bands.get(0);
        final KmlFolder folder = new KmlFolder(refRaster.getName(), refRaster.getDescription());
        for (RasterDataNode raster : bands) {
            final GeoCoding geoCoding = raster.getGeoCoding();
            final PixelPos upperLeftPP = new PixelPos(0.5f, 0.5f);
            final PixelPos lowerRightPP = new PixelPos(raster.getSceneRasterWidth() - 0.5f,
                                                       raster.getSceneRasterHeight() - 0.5f);
            final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
            final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
            double north = upperLeftGP.getLat();
            double south = lowerRightGP.getLat();
            double east = lowerRightGP.getLon();
            double west = upperLeftGP.getLon();
            if (geoCoding.isCrossingMeridianAt180()) {
                east += 360;
            }

            final BoundingBox referencedEnvelope = new ReferencedEnvelope(west, east, north, south,
                                                                          DefaultGeographicCRS.WGS84);

            TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(raster);
            if (timeCoding != null) {
                final ProductData.UTC startTime = timeCoding.getStartTime();
                final ProductData.UTC endTime = timeCoding.getEndTime();

                final ImageManager imageManager = ImageManager.getInstance();
                final ImageInfo imageInfo = raster.getImageInfo(ProgressMonitor.NULL);
                final RenderedImage levelImage = imageManager.createColoredBandImage(new RasterDataNode[]{raster},
                        imageInfo, level);
                final String name = raster.getName();
                final KmlGroundOverlay groundOverlay = new KmlGroundOverlay(name,
                        levelImage,
                        referencedEnvelope,
                        startTime, endTime);
                groundOverlay.setIconName(name + raster.getProduct().getRefNo());
                folder.addChild(groundOverlay);
            }
        }
        return folder;
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        setEnabled(view != null);
    }

    private class RadioButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final JRadioButton button = (JRadioButton) e.getSource();
            if (button.isSelected()) {
                String buttonText = button.getText();
                final int index = buttonText.indexOf(" (");
                if (index != -1) {
                    buttonText = buttonText.substring(0, index);
                }
                level = Integer.parseInt(buttonText);
            }
        }
    }

    private class KmzSwingWorker extends ProgressMonitorSwingWorker {

        private final String title;
        private final File output;
        private final VisatApp app;
        private static final int ONE_MEGABYTE = 1012 * 1024;

        KmzSwingWorker(String title, File output, VisatApp app) {
            super(ExportTimeBasedKmz.this.view, title);
            this.title = title;
            this.output = output;
            this.app = app;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            KmlFeature kmlFeature = createKmlFeature();
            final FileOutputStream fileOutputStream = new FileOutputStream(output);
            ZipOutputStream zipStream = new ZipOutputStream(
                    new BufferedOutputStream(fileOutputStream, 5 * ONE_MEGABYTE));
            try {
                final KmzExporter exporter = new KmzExporter();
                exporter.export(kmlFeature, zipStream, pm);
            } finally {
                zipStream.close();
            }
            return null;
        }

        @Override
        protected void done() {
            Throwable exception = null;
            try {
                get();
            } catch (InterruptedException e) {
                exception = e;
            } catch (ExecutionException e) {
                exception = e.getCause();
            }
            if (exception != null) {
                String message = String.format("Error occurred while exporting to KMZ.%n%s",
                                               exception.getMessage());
                app.showErrorDialog(title, message);
            }

        }
    }
}
