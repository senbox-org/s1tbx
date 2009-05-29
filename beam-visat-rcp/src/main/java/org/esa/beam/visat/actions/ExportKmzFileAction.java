package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageLegend;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportKmzFileAction extends ExecCommand {

    final BeamFileFilter kmzFileFilter;

    public static final String EXPORT_KML_CMD_ID = "exportKmzFile";

    private static final String[] KMZ_FORMAT_DESCRIPTION = {"KMZ", "kmz",
            "KMZ - Google Earth File Format"};

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";

    public ExportKmzFileAction() {
        final String formatName = KMZ_FORMAT_DESCRIPTION[0];
        final String formatExt = KMZ_FORMAT_DESCRIPTION[1];
        final String formatDescr = KMZ_FORMAT_DESCRIPTION[2];
        kmzFileFilter = new BeamFileFilter(formatName, formatExt, formatDescr);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        final GeoCoding geoCoding = view.getProduct().getGeoCoding();
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                exportImage(view);
            }
        } else {
            String message = MessageFormat.format("Product must be in ''{0}'' projection.",
                                                  IdentityTransformDescriptor.NAME);
            VisatApp.getApp().showInfoDialog(message, null);
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        setEnabled(view != null);
    }

    private void exportImage(ProductSceneView sceneView) {
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

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export KMZ"); /* I18N */
        fileChooser.setCurrentFilename(sceneView.getRaster().getName());

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
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
            visatApp.getPreferences().setPropertyString(
                    IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                    currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (file == null || file.getName().equals("")) {
            return;
        }

        if (!visatApp.promptForOverwrite(file)) {
            return;
        }

        final SaveKMLSwingWorker worker = new SaveKMLSwingWorker(visatApp, "Save KMZ", sceneView, file);
        worker.executeWithBlocking();
    }

    private static RenderedImage createImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = initImageLegend(raster);
        return imageLegend.createImage();
    }

    private static String formatKML(ProductSceneView view, String imageName) {
        final RasterDataNode raster = view.getRaster();
        final Product product = raster.getProduct();
        final GeoCoding geoCoding = raster.getGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0.5f, 0.5f);
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth() - 0.5f,
                                                   product.getSceneRasterHeight() - 0.5f);
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
        float eastLon = lowerRightGP.getLon();
        if (geoCoding.isCrossingMeridianAt180()) {
            eastLon += 360;
        }

        String pinKml = "";
        ProductNodeGroup<Pin> pinGroup = product.getPinGroup();
        Pin[] pins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
        for (Pin pin : pins) {
            GeoPos geoPos = pin.getGeoPos();
            if (geoPos != null && product.containsPixel(pin.getPixelPos())) {
                pinKml += String.format(
                        "<Placemark>\n"
                                + "  <name>%s</name>\n"
                                + "  <Point>\n"
                                + "    <coordinates>%f,%f,0</coordinates>\n"
                                + "  </Point>\n"
                                + "</Placemark>\n",
                        pin.getLabel(),
                        geoPos.lon,
                        geoPos.lat);
            }
        }

        String name;
        String description;
        String legendKml = "";
        if (view.isRGB()) {
            name = "RGB";
            JInternalFrame parent = (JInternalFrame) view.getParent().getParent().getParent();
            description = parent.getTitle() + "\n" + product.getName();
        } else {
            name = raster.getName();
            description = raster.getDescription() + "\n" + product.getName();
            legendKml = "  <ScreenOverlay>\n"
                    + "    <name>Legend</name>\n"
                    + "    <Icon>\n"
                    + "      <href>legend.png</href>\n"
                    + "    </Icon>\n"
                    + "    <overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                    + "    <screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                    + "  </ScreenOverlay>\n";
        }

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

    private static ImageLegend initImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = new ImageLegend(raster.getImageInfo(), raster);

        imageLegend.setHeaderText(getLegendHeaderText(raster));
        imageLegend.setOrientation(ImageLegend.VERTICAL);
        imageLegend.setBackgroundTransparency(0.0f);
        imageLegend.setBackgroundTransparencyEnabled(true);
        imageLegend.setAntialiasing(true);

        return imageLegend;
    }

    private static String getLegendHeaderText(RasterDataNode raster) {
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        return "(" + unit + ")";
    }

    private class SaveKMLSwingWorker extends ProgressMonitorSwingWorker {

        private final VisatApp visatApp;
        private final ProductSceneView view;
        private final File file;

        SaveKMLSwingWorker(VisatApp visatApp, String message, ProductSceneView view, File file) {
            super(visatApp.getMainFrame(), message);
            this.visatApp = visatApp;
            this.view = view;
            this.file = file;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            try {
                final String message = "Saving image as " + file.getPath() + "...";
                pm.beginTask(message, view.isRGB() ? 4 : 3);
                visatApp.setStatusBarMessage(message);
                visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                final Rectangle bounds = view.getLayerCanvas().getViewport().getViewBounds();
                // TODO: let user specifiy dimension (rq-20090529)
                final Dimension dimension = new Dimension(MathUtils.floorInt(bounds.getWidth()),
                                                          MathUtils.floorInt(bounds.getHeight()));
                RenderedImage image = ExportImageAction.createImage(view, true, dimension, true);
                pm.worked(1);
                String imageType = "PNG";
                String imageName = "overlay.png";
                ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(file));
                try {
                    outStream.putNextEntry(new ZipEntry("overlay.kml"));
                    final String kmlContent = formatKML(view, imageName);
                    outStream.write(kmlContent.getBytes());
                    pm.worked(1);

                    outStream.putNextEntry(new ZipEntry(imageName));
                    ImageEncoder encoder = ImageCodec.createImageEncoder(imageType, outStream, null);
                    encoder.encode(image);
                    pm.worked(1);

                    if (!view.isRGB()) {
                        outStream.putNextEntry(new ZipEntry("legend.png"));
                        encoder = ImageCodec.createImageEncoder("PNG", outStream, null);
                        encoder.encode(createImageLegend(view.getRaster()));
                        pm.worked(1);
                    }
                } finally {
                    outStream.close();
                }
            } catch (OutOfMemoryError e) {
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
    }

}
