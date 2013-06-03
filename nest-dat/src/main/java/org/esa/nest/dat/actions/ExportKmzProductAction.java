/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.StringSelectorDialog;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;
import org.esa.nest.gpf.GPFProcessor;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportKmzProductAction extends ExecCommand {
    private static final String OVERLAY_KML = "overlay.kml";
    private static final String OVERLAY_TIF = "overlay.tif";
    private static final String IMAGE_TYPE = "TIF";
    private static final String LEGEND_PNG = "legend.png";
    private static final String[] KMZ_FORMAT_DESCRIPTION = {"KMZ", "kmz","KMZ - Google Earth File Format"};
    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";

    private final BeamFileFilter kmzFileFilter;

    public ExportKmzProductAction() {
        final String formatName = KMZ_FORMAT_DESCRIPTION[0];
        final String formatExt = KMZ_FORMAT_DESCRIPTION[1];
        final String formatDescr = KMZ_FORMAT_DESCRIPTION[2];
        kmzFileFilter = new BeamFileFilter(formatName, formatExt, formatDescr);
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        final GeoCoding geoCoding = product.getGeoCoding();
        boolean isGeographic = false;
        if (geoCoding instanceof MapGeoCoding) {
            final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            final MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            final String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                isGeographic = true;
            }
        } else if(geoCoding instanceof CrsGeoCoding){
            isGeographic = CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }

        if(isGeographic) {

            final StringSelectorDialog dlg = new StringSelectorDialog("Select band",
                                                    getValidBandNames(product));
            dlg.show();
            if(dlg.IsOK()) {
                exportImage(product, dlg.getSelectedItem());
            }
        } else {
            final String message = "Product must be Orthorectified and in ''Geographic Lat/Lon'' projection.";
            VisatApp.getApp().showInfoDialog(message, null);
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null);
        //setEnabled(false);      // DEMO_CODE
    }

    private static String[] getValidBandNames(final Product product) {
        final List<String> bandNames = new ArrayList<String>(4);
        for(Band band : product.getBands()) {
            bandNames.add(band.getName());
        }

        return bandNames.toArray(new String[bandNames.size()]);
    }

    private void exportImage(final Product product, final String selectedBandName) {
        final VisatApp visatApp = VisatApp.getApp();
        final String lastDir = visatApp.getPreferences().getPropertyString(
                IMAGE_EXPORT_DIR_PREFERENCES_KEY, SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(kmzFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export KMZ"); /* I18N */
        fileChooser.setCurrentFilename(product.getName());

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        final Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        final int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        final File file = fileChooser.getSelectedFile();

        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            visatApp.getPreferences().setPropertyString(
                    IMAGE_EXPORT_DIR_PREFERENCES_KEY, currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (file == null || file.getName().isEmpty()) {
            return;
        }

        if (!visatApp.promptForOverwrite(file)) {
            return;
        }

        final SaveKMLSwingWorker worker = new SaveKMLSwingWorker(visatApp, "Save KMZ",
                                            product, selectedBandName, file);
        worker.executeWithBlocking();
    }

    private static RenderedImage createImageLegend(final RasterDataNode raster) {
        final ImageLegend imageLegend = initImageLegend(raster);
        return imageLegend.createImage();
    }

    private static String formatKML(final Product product, final String imageName) {
        final GeoCoding geoCoding = product.getGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0.5f, 0.5f);
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth() - 0.5f,
                                                   product.getSceneRasterHeight() - 0.5f);
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
        double eastLon = lowerRightGP.getLon();
        if (geoCoding.isCrossingMeridianAt180()) {
            eastLon += 360;
        }

        String pinKml = "";
        final ProductNodeGroup<Placemark> pinGroup = product.getPinGroup();
        final Placemark[] pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        for (Placemark placemark : pins) {
            final GeoPos geoPos = placemark.getGeoPos();
            if (geoPos != null && product.containsPixel(placemark.getPixelPos())) {
                pinKml += String.format(
                        "<Placemark>\n"
                                + "  <name>%s</name>\n"
                                + "  <Point>\n"
                                + "    <coordinates>%f,%f,0</coordinates>\n"
                                + "  </Point>\n"
                                + "</Placemark>\n",
                        placemark.getLabel(),
                        geoPos.lon,
                        geoPos.lat);
            }
        }

        final String name = product.getName();
        final String description = product.getDescription() + '\n' + product.getName();
        final String legendKml = "  <ScreenOverlay>\n"
                    + "    <name>Legend</name>\n"
                    + "    <Icon>\n"
                    + "      <href>legend.png</href>\n"
                    + "    </Icon>\n"
                    + "    <overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                    + "    <screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                    + "  </ScreenOverlay>\n";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<kml xmlns=\"http://earth.google.com/kml/2.0\">\n"
                + "<Document>\n"
                + "  <name>" + name + "</name>\n"
                + "  <description>" + description + "</description>\n"
                + "  <GroundOverlay>\n"
                + "    <name>Raster data</name>\n"
                + "    <LatLonBox>\n"
                + "      <north>" + upperLeftGP.getLat() + "</north>\n"
                + "      <south>" + lowerRightGP.getLat() + "</south>\n"
                + "      <east>" + eastLon + "</east>\n"
                + "      <west>" + upperLeftGP.getLon() + "</west>\n"
                + "    </LatLonBox>\n"
                + "    <Icon>\n"
                + "      <href>" + imageName + "</href>\n"
                + "    </Icon>\n"
                + "  </GroundOverlay>\n"
                + legendKml
                + pinKml
                + "</Document>\n"
                + "</kml>\n";
    }

    private static ImageLegend initImageLegend(final RasterDataNode raster) {
        final ImageLegend imageLegend = new ImageLegend(raster.getImageInfo(), raster);

        imageLegend.setHeaderText(getLegendHeaderText(raster));
        imageLegend.setOrientation(ImageLegend.VERTICAL);
        imageLegend.setBackgroundTransparency(0.0f);
        imageLegend.setBackgroundTransparencyEnabled(true);
        imageLegend.setAntialiasing(true);

        return imageLegend;
    }

    private static String getLegendHeaderText(final RasterDataNode raster) {
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        return '(' + unit + ')';
    }

    private static class SaveKMLSwingWorker extends ProgressMonitorSwingWorker {

        private final VisatApp visatApp;
        private final Product product;
        private final String selectedBandName;
        private final File file;

        SaveKMLSwingWorker(final VisatApp visatApp, final String message,
                           final Product product, final String selectedBandName, final File file) {
            super(visatApp.getMainFrame(), message);
            this.visatApp = visatApp;
            this.product = product;
            this.selectedBandName = selectedBandName;
            this.file = file;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            try {
                final Product subsetProduct = createSubsetProduct(product, selectedBandName);

                final String message = String.format("Saving image as %s...", file.getPath());
                pm.beginTask(message, 4);
                visatApp.setStatusBarMessage(message);
                visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                final ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(file));
                try {
                    outStream.putNextEntry(new ZipEntry(OVERLAY_KML));
                    final String kmlContent = formatKML(subsetProduct, OVERLAY_TIF);
                    outStream.write(kmlContent.getBytes());
                    pm.worked(1);

                    final File imgFile = new File(file.getParent(), product.getName()+".tif");
                    final File graphFile = new File(GraphBuilderDialog.getInternalGraphFolder(), "DataConvertGraph.xml");

                    final GPFProcessor proc = new GPFProcessor(graphFile);
                    proc.setIO(product.getFileLocation(), imgFile, "GeoTIFF");
                    proc.executeGraph(SubProgressMonitor.create(pm, 1));
                    
                    //ProductIO.writeProduct(subsetProduct, imgFile, "GeoTIFF", true, SubProgressMonitor.create(pm, 1));
                    pm.worked(1);

                    final FileInputStream fin = new FileInputStream(imgFile);
                    try {
                        outStream.putNextEntry(new ZipEntry(OVERLAY_TIF));

                        final int size = 8192;
                        final byte[] buf = new byte[size];
                        int n;
                        while ((n = fin.read(buf, 0, size)) > -1)  {
                            outStream.write(buf, 0, n);
                        }
                    } finally {
                        fin.close();
                    }
                    pm.worked(1);

                    imgFile.delete();
                    pm.worked(1);

                   // final Band selectedBand = product.getBandAt(0);
                   // outStream.putNextEntry(new ZipEntry(LEGEND_PNG));
                   // ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", outStream, null);
                  //  encoder.encode(createImageLegend(selectedBand));
                  //  pm.worked(1);
                } finally {
                    outStream.close();
                }
            } catch (OutOfMemoryError ignored) {
                visatApp.showOutOfMemoryErrorDialog("The image could not be exported."); /*I18N*/
            } catch (Throwable e) {
                visatApp.handleUnknownException(e);
            } finally {
                visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
                visatApp.clearStatusBarMessage();
                pm.done();
            }
            return null;
        }

        private static Product createSubsetProduct(final Product product, final String selectedBandName)
                                                    throws IOException {
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            final Band srcBand = product.getBand(selectedBandName);

            // if not virtual set as single band in subset
            if(!(srcBand instanceof VirtualBand)) {
                productSubsetDef.setNodeNames(new String[] {selectedBandName});
            }

            return product.createSubset(productSubsetDef, null, null);
        }

    }

}
