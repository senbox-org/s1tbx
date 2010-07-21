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
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.SelectableCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.geotiff.GeoTIFF;
import org.esa.beam.util.geotiff.GeoTIFFMetadata;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.media.jai.operator.BandSelectDescriptor;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public abstract class AbstractExportImageAction extends ExecCommand {

    public static final String EXPORT_IMAGE_CMD_ID = "exportImageFile";
    public static final String EXPORT_ROI_IMAGE_CMD_ID = "exportROIImageFile";
    public static final String EXPORT_LEGEND_IMAGE_CMD_ID = "exportLegendImageFile";

    protected static final String[] BMP_FORMAT_DESCRIPTION = {"BMP", "bmp", "BMP - Microsoft Windows Bitmap"};
    protected static final String[] PNG_FORMAT_DESCRIPTION = {"PNG", "png", "PNG - Portable Network Graphics"};
    protected static final String[] JPEG_FORMAT_DESCRIPTION = {
            "JPEG", "jpg,jpeg", "JPEG - Joint Photographic Experts Group"
    };

    // not yet used
//    private static final String[] JPEG2K_FORMAT_DESCRIPTION = {
//            "JPEG2000", "jpg,jpeg", "JPEG 2000 - Joint Photographic Experts Group"
//    };

    protected static final String[] TIFF_FORMAT_DESCRIPTION = {"TIFF", "tif,tiff", "TIFF - Tagged Image File Format"};
    protected static final String[] GEOTIFF_FORMAT_DESCRIPTION = {
            "GeoTIFF", "tif,tiff", "GeoTIFF - TIFF with geo-location"
    };


    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
    };

    private final static String[][] SCENE_IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
            GEOTIFF_FORMAT_DESCRIPTION,
    };
    private static final String[] TRANSPARENCY_IMAGE_FORMATS = new String[]{"TIFF", "PNG"};

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";


    private BeamFileFilter[] imageFileFilters;
    private BeamFileFilter[] sceneImageFileFilters;

    public AbstractExportImageAction() {
        imageFileFilters = new BeamFileFilter[IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            imageFileFilters[i] = createFileFilter(IMAGE_FORMAT_DESCRIPTIONS[i]);
        }
        sceneImageFileFilters = new BeamFileFilter[SCENE_IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < SCENE_IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            sceneImageFileFilters[i] = createFileFilter(SCENE_IMAGE_FORMAT_DESCRIPTIONS[i]);
        }

    }

    protected void exportImage(final VisatApp visatApp,
                               final BeamFileFilter[] filters,
                               final SelectableCommand command) {
        final ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view == null) {
            return;
        }
        final String lastDir = visatApp.getPreferences().getPropertyString(IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                                                                           SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, command.getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        for (int i = 0; i < filters.length; i++) {
            BeamFileFilter filter = filters[i];
            Debug.trace("export image: supported format " + (i + 1) + ": " + filter.getFormatName());
            fileChooser.addChoosableFileFilter(filter); // note: also selects current file filter!
        }
        fileChooser.setAcceptAllFileFilterUsed(false);

        final String imageBaseName = FileUtils.getFilenameWithoutExtension(view.getProduct().getName()).replace('.',
                                                                                                                '_');
        configureFileChooser(fileChooser, view, imageBaseName);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        File file = fileChooser.getSelectedFile();
        fileChooser.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // @todo never comes here, why?
                Debug.trace(evt.toString());
            }
        });
        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            visatApp.getPreferences().setPropertyString(IMAGE_EXPORT_DIR_PREFERENCES_KEY, currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (file == null || file.getName().equals("")) {
            return;
        }
        final boolean entireImageSelected = isEntireImageSelected();

        final BeamFileFilter fileFilter = fileChooser.getBeamFileFilter();
        String imageFormat = fileFilter != null ? fileFilter.getFormatName() : "TIFF";
        if (imageFormat.equals(GEOTIFF_FORMAT_DESCRIPTION[0]) && !entireImageSelected) {
            final int status = visatApp.showQuestionDialog("GeoTIFF is not applicable to image clippings.\n" +
                                                           "Shall TIFF format be used instead?", null);
            if (status == JOptionPane.YES_OPTION) {
                imageFormat = "TIFF";
            } else {
                return;
            }
        }
        if (!visatApp.promptForOverwrite(file)) {
            return;
        }

        final SaveImageSwingWorker worker = new SaveImageSwingWorker(visatApp, "Save Image", imageFormat, view,
                                                                     entireImageSelected, file);
        worker.executeWithBlocking();
    }

    protected abstract RenderedImage createImage(String imageFormat, ProductSceneView view);

    protected abstract boolean isEntireImageSelected();

    protected abstract void configureFileChooser(BeamFileChooser fileChooser, ProductSceneView view,
                                                 String imageBaseName);

    protected BeamFileFilter[] getImageFileFilters() {
        return imageFileFilters;
    }

    protected BeamFileFilter[] getSceneImageFileFilters() {
        return sceneImageFileFilters;
    }

    protected VisatApp getVisatApp() {
        return VisatApp.getApp();
    }

    protected static boolean isTransparencySupportedByFormat(String formatName) {
        final String[] formats = TRANSPARENCY_IMAGE_FORMATS;
        for (final String format : formats) {
            if (format.equalsIgnoreCase(formatName)) {
                return true;
            }
        }
        return false;
    }

    private static BeamFileFilter createFileFilter(String[] description) {
        final String formatName = description[0];
        final String formatExt = description[1];
        final String formatDescr = description[2];
        return new BeamFileFilter(formatName, formatExt, formatDescr);
    }

    private class SaveImageSwingWorker extends ProgressMonitorSwingWorker {

        private final VisatApp visatApp;
        private final String imageFormat;
        private final ProductSceneView view;
        private final boolean entireImageSelected;
        private final File file;

        SaveImageSwingWorker(VisatApp visatApp, String message, String imageFormat, ProductSceneView view,
                             boolean entireImageSelected, File file) {
            super(visatApp.getMainFrame(), message);
            this.visatApp = visatApp;
            this.imageFormat = imageFormat;
            this.view = view;
            this.entireImageSelected = entireImageSelected;
            this.file = file;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            try {
                final String message = "Saving image as " + file.getPath() + "...";
                pm.beginTask(message, 1);
                visatApp.setStatusBarMessage(message);
                visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                RenderedImage image = createImage(imageFormat, view);

                boolean geoTIFFWritten = false;
                if (imageFormat.equals("GeoTIFF") && entireImageSelected) {
                    final GeoTIFFMetadata metadata = ProductUtils.createGeoTIFFMetadata(view.getProduct());
                    if (metadata != null) {
                        GeoTIFF.writeImage(image, file, metadata);
                        geoTIFFWritten = true;
                    }
                }
                if (!geoTIFFWritten) {
                    if ("JPEG".equalsIgnoreCase(imageFormat)) {
                        image = BandSelectDescriptor.create(image, new int[]{0, 1, 2}, null);
                    }
                    final OutputStream stream = new FileOutputStream(file);
                    try {
                        ImageEncoder encoder = ImageCodec.createImageEncoder(imageFormat, stream, null);
                        encoder.encode(image);
                    } finally {
                        stream.close();
                    }
                }
            } catch (OutOfMemoryError e) {
                visatApp.showOutOfMemoryErrorDialog("The image could not be exported.");
            } catch (Throwable e) {
                visatApp.handleUnknownException(e);
            } finally {
                visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
                visatApp.clearStatusBarMessage();
                pm.done();
            }
            return null;
        }
    }
}
