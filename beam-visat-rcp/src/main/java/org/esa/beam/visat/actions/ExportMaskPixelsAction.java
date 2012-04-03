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

package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.SelectExportMethodDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExportMaskPixelsAction extends ExecCommand {

    private static final String DLG_TITLE = "Export Mask Pixels";
    private static final String ERR_MSG_BASE = "Mask pixels cannot be exported:\n";

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        exportMaskPixels();
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    @Override
    public void updateState(final CommandEvent event) {
        final boolean enabled = hasSelectedRasterMasks();
        setEnabled(enabled);
    }
    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "Export Mask Pixels" command
    /////////////////////////////////////////////////////////////////////////

    /**
     * Performs the actual "Export Mask Pixels" command.
     */
    private void exportMaskPixels() {

        if (!hasSelectedRasterMasks()) {
            VisatApp.getApp().showErrorDialog(DLG_TITLE,
                                              ERR_MSG_BASE + "There are no masks available in the currently selected product.");  /*I18N*/
            return;
        }

        // Get current VISAT view showing a product's band
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view == null) {
            return;
        }
        // Get the displayed raster data node (band or tie-point grid)
        final RasterDataNode raster = view.getRaster();
        
        String[] maskNames = raster.getProduct().getMaskGroup().getNodeNames();
        String maskName;
        if (maskNames.length == 1) {
            maskName = maskNames[0];
        } else {
            JPanel panel = new JPanel();
            BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.X_AXIS);
            panel.setLayout(boxLayout);
            panel.add(new JLabel("Select Mask: "));
            JComboBox maskCombo = new JComboBox(maskNames);
            panel.add(maskCombo);
            ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getApplicationWindow(), DLG_TITLE, panel, 
                                                      ModalDialog.ID_OK_CANCEL | ModalDialog.ID_HELP, getHelpId());
            if (modalDialog.show() == AbstractDialog.ID_OK) {
                maskName = (String) maskCombo.getSelectedItem();
            } else {
                return;
            }
        }
        Mask mask = raster.getProduct().getMaskGroup().get(maskName);
        
        final RenderedImage maskImage = mask.getSourceImage();
        if (maskImage == null) {
            VisatApp.getApp().showErrorDialog(DLG_TITLE, ERR_MSG_BASE + "No Mask image available.");
            return;
        }
        // Compute total number of Mask pixels
        final long numMaskPixels = getNumMaskPixels(raster, maskImage);

        String numPixelsText;
        if (numMaskPixels == 1) {
            numPixelsText = "One Mask pixel will be exported.\n";
        } else {
            numPixelsText = numMaskPixels + " Mask pixels will be exported.\n";
        }
        // Get export method from user
        final String questionText = "How do you want to export the pixel values?\n";
        final int method = SelectExportMethodDialog.run(VisatApp.getApp().getMainFrame(), getWindowTitle(),
                                                        questionText + numPixelsText, getHelpId());
        final PrintWriter out;
        final StringBuffer clipboardText;
        final int initialBufferSize = 256000;
        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            final StringWriter stringWriter = new StringWriter(initialBufferSize);
            out = new PrintWriter(stringWriter);
            clipboardText = stringWriter.getBuffer();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            final File file = promptForFile(VisatApp.getApp(), createDefaultFileName(raster, maskName));
            if (file == null) {
                return; // Cancel
            }
            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                VisatApp.getApp().showErrorDialog(DLG_TITLE,
                                                  ERR_MSG_BASE + "Failed to create file '" + file + "':\n" + e.getMessage()); /*I18N*/
                return; // Error
            }
            out = new PrintWriter(new BufferedWriter(fileWriter, initialBufferSize));
            clipboardText = null;
        } else {
            return; // Cancel
        }

        final ProgressMonitorSwingWorker<Exception, Object> swingWorker = new ProgressMonitorSwingWorker<Exception, Object>(
                VisatApp.getApp().getMainFrame(), DLG_TITLE) {

            @Override
            protected Exception doInBackground(ProgressMonitor pm) throws Exception {
                Exception returnValue = null;
                try {
                    boolean success = exportMaskPixels(out, raster.getProduct(), maskImage, pm);
                    if (success && clipboardText != null) {
                        SystemUtils.copyToClipboard(clipboardText.toString());
                        clipboardText.setLength(0);
                    }
                } catch (Exception e) {
                    returnValue = e;
                } finally {
                    out.close();
                }
                return returnValue;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread) after the <code>construct</code> method
             * has returned.
             */
            @Override
            public void done() {
                // clear status bar
                VisatApp.getApp().clearStatusBarMessage();
                // show default-cursor
                UIUtils.setRootFrameDefaultCursor(VisatApp.getApp().getMainFrame());
                // On error, show error message
                Exception exception;
                try {
                    exception = get();
                } catch (Exception e) {
                    exception = e;
                }
                if (exception != null) {
                    VisatApp.getApp().showErrorDialog(DLG_TITLE,
                                                      ERR_MSG_BASE + exception.getMessage());
                }
            }

        };

        // show wait-cursor
        UIUtils.setRootFrameWaitCursor(VisatApp.getApp().getMainFrame());
        // show message in status bar
        VisatApp.getApp().setStatusBarMessage("Exporting Mask pixels..."); /*I18N*/

        // Start separate worker thread.
        swingWorker.execute();
    }

    private static String createDefaultFileName(final RasterDataNode raster, String maskName) {
        String productName = FileUtils.getFilenameWithoutExtension(raster.getProduct().getName());
        String rasterName = raster.getName();
        return productName + "_" + rasterName + "_" + maskName + "_Mask.txt";
    }

    private static String getWindowTitle() {
        return VisatApp.getApp().getAppName() + " - " + DLG_TITLE;
    }

    /*
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @param visatApp the VISAT application
     * @return the selected file, <code>null</code> means "Cancel"
     */
    private static File promptForFile(final VisatApp visatApp, String defaultFileName) {
        return visatApp.showFileSaveDialog(DLG_TITLE,
                                           false,
                                           null,
                                           ".txt",
                                           defaultFileName,
                                           "exportMaskPixels.lastDir");
    }

    /*
     * Writes all pixel values of the given product within the given Mask to the specified out.
     *
     * @param out      the data output writer
     * @param product  the product providing the pixel values
     * @param maskImage the mask image for the Mask
     * @return <code>true</code> for success, <code>false</code> if export has been terminated (by user)
     */
    private static boolean exportMaskPixels(final PrintWriter out,
                                           final Product product,
                                           final RenderedImage maskImage, ProgressMonitor pm) throws IOException {

        final Band[] bands = product.getBands();
        final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        final GeoCoding geoCoding = product.getGeoCoding();
        
        final int minTileX = maskImage.getMinTileX();
        final int minTileY = maskImage.getMinTileY();
        
        final int numXTiles = maskImage.getNumXTiles();
        final int numYTiles = maskImage.getNumYTiles();
        
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        final Rectangle imageRect = new Rectangle(0, 0, w, h);
        
        pm.beginTask("Writing pixel data...", numXTiles * numYTiles + 1);
        try {
            writeHeaderLine(out, geoCoding, bands, tiePointGrids);
            pm.worked(1);

            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                    if (pm.isCanceled()) {
                        return false;
                    }
                    final Rectangle tileRectangle = new Rectangle(maskImage.getTileGridXOffset() + tileX * maskImage.getTileWidth(),
                                                                  maskImage.getTileGridYOffset() + tileY * maskImage.getTileHeight(),
                                                                  maskImage.getTileWidth(), maskImage.getTileHeight());

                    final Rectangle r = imageRect.intersection(tileRectangle);
                    if (!r.isEmpty()) {
                        Raster maskTile = maskImage.getTile(tileX, tileY);
                        for (int y = r.y; y < r.y + r.height; y++) {
                            for (int x = r.x; x < r.x + r.width; x++) {
                                if (maskTile.getSample(x, y, 0) != 0) {
                                    writeDataLine(out, geoCoding, bands, tiePointGrids, x, y);
                                }
                            }
                        }
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }

        return true;
    }

    /*
     * Writes the header line of the dataset to be exported.
     *
     * @param out           the data output writer
     * @param geoCoding     the product's geo-coding
     * @param bands         the array of bands to be considered
     * @param tiePointGrids
     */
    private static void writeHeaderLine(final PrintWriter out,
                                        final GeoCoding geoCoding,
                                        final Band[] bands, TiePointGrid[] tiePointGrids) {
        out.print("Pixel-X");
        out.print("\t");
        out.print("Pixel-Y");
        if (geoCoding != null) {
            out.print("\t");
            out.print("Longitude");
            out.print("\t");
            out.print("Latitude");
        }
        for (final Band band : bands) {
            out.print("\t");
            out.print(band.getName());
        }
        for (final TiePointGrid tiePointGrid : tiePointGrids) {
            out.print("\t");
            out.print(tiePointGrid.getName());
        }
        out.print("\n");
    }

    /*
     * Writes a data line of the dataset to be exported for the given pixel position.
     *
     * @param out           the data output writer
     * @param geoCoding     the product's geo-coding
     * @param bands         the array of bands that provide pixel values
     * @param tiePointGrids
     * @param x             the current pixel's X coordinate
     * @param y             the current pixel's Y coordinate
     */
    private static void writeDataLine(final PrintWriter out,
                                      final GeoCoding geoCoding,
                                      final Band[] bands,
                                      TiePointGrid[] tiePointGrids, int x,
                                      int y) throws IOException {
        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);

        out.print(String.valueOf(pixelPos.x));
        out.print("\t");
        out.print(String.valueOf(pixelPos.y));
        if (geoCoding != null) {
            final GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
            out.print("\t");
            out.print(String.valueOf(geoPos.lon));
            out.print("\t");
            out.print(String.valueOf(geoPos.lat));
        }
        final int[] intPixel = new int[1];
        final float[] floatPixel = new float[1];
        for (final Band band : bands) {
            out.print("\t");
            if (band.isPixelValid(x, y)) {
                if (band.isFloatingPointType()) {
                    band.readPixels(x, y, 1, 1, floatPixel, ProgressMonitor.NULL);
                    out.print(floatPixel[0]);
                } else {
                    band.readPixels(x, y, 1, 1, intPixel, ProgressMonitor.NULL);
                    out.print(intPixel[0]);
                }
            } else {
                out.print("NaN");
            }
        }
        for (final TiePointGrid grid : tiePointGrids) {
            grid.readPixels(x, y, 1, 1, floatPixel, ProgressMonitor.NULL);
            out.print("\t");
            out.print(floatPixel[0]);
        }
        out.print("\n");
    }


    /*
     * Computes the total number of pixels within the specified Mask.
     *
     * @param raster   the raster data node
     * @param maskImage the rendered image masking out the Mask
     * @return the total number of pixels in the Mask
     */
    private static long getNumMaskPixels(final RasterDataNode raster, final RenderedImage maskImage) {
        final int minTileX = maskImage.getMinTileX();
        final int minTileY = maskImage.getMinTileY();

        final int numXTiles = maskImage.getNumXTiles();
        final int numYTiles = maskImage.getNumYTiles();

        final int w = raster.getSceneRasterWidth();
        final int h = raster.getSceneRasterHeight();
        final Rectangle imageRect = new Rectangle(0, 0, w, h);

        long numMaskPixels = 0;
        for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
            for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                final Rectangle tileRectangle = new Rectangle(maskImage.getTileGridXOffset() + tileX * maskImage.getTileWidth(),
                                                              maskImage.getTileGridYOffset() + tileY * maskImage.getTileHeight(),
                                                              maskImage.getTileWidth(), maskImage.getTileHeight());

                final Rectangle r = imageRect.intersection(tileRectangle);
                if (!r.isEmpty()) {
                    Raster maskTile = maskImage.getTile(tileX, tileY);
                    for (int y = r.y; y < r.y + r.height; y++) {
                        for (int x = r.x; x < r.x + r.width; x++) {
                            if (maskTile.getSample(x, y, 0) != 0) {
                                numMaskPixels++;
                            }
                        }
                    }
                }
            }
        }
        return numMaskPixels;
    }

    /**
     * Checks whether the command can be performed or not.
     *
     * @return <code>true</code> if so
     */
    private static boolean hasSelectedRasterMasks() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;

        if (view != null) {
            final RasterDataNode raster = view.getRaster();
            if (raster != null) {
                Product product = raster.getProduct();
                int numMasks = product.getMaskGroup().getNodeCount();
                enabled = numMasks > 0;
            }
        }
        return enabled;
    }

}
