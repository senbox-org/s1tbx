package org.esa.beam.visat.actions;

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
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportKmzFileAction extends ExecCommand {

    final BeamFileFilter[] kmlFilefilter;

    public static final String EXPORT_KML_CMD_ID = "exportKmzFile";

    private static final String[] KML_FORMAT_DESCRIPTION = {"KMZ", "kmz",
            "KMZ - Google Earth File Format"};

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";

    public ExportKmzFileAction() {
        kmlFilefilter = new BeamFileFilter[1];
        kmlFilefilter[0] = createFileFilter(KML_FORMAT_DESCRIPTION);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        exportImage(VisatApp.getApp(), kmlFilefilter);
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;
        if (view != null) {
            final GeoCoding geoCoding = view.getProduct().getGeoCoding();
            if (geoCoding instanceof MapGeoCoding) {
                MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
                String typeID = mapGeoCoding.getMapInfo()
                        .getMapProjection().getMapTransform()
                        .getDescriptor().getTypeID();
                if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                    enabled = true;
                }
            }
        }
        setEnabled(enabled);
    }

    private void exportImage(final VisatApp visatApp, final BeamFileFilter[] filters) {

        final ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view == null) {
            return;
        }

        final String lastDir = visatApp.getPreferences().getPropertyString(
                IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        for (int i = 0; i < filters.length; i++) {
            BeamFileFilter filter = filters[i];
            Debug.trace("export image: supported format " + (i + 1) + ": " + filter.getFormatName());
            fileChooser.addChoosableFileFilter(filter); // note: also selects current file filter!
        }
        fileChooser.setAcceptAllFileFilterUsed(false);

        final JCheckBox buttonTransparancy = new JCheckBox("Mark No-Data as transparent",
                                                           true); /* I18N */

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export KMZ"); /* I18N */
        fileChooser.setCurrentFilename(view.getRaster().getName());
        final JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Options")); /* I18N */
        panel.add(buttonTransparancy);
        final JPanel accessory = new JPanel(new BorderLayout());
        accessory.add(panel, BorderLayout.NORTH);
        fileChooser.setAccessory(accessory);

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
        boolean isNoDataTranparent = buttonTransparancy.isSelected();

        if (!visatApp.promptForOverwrite(file)) {
            return;
        }
        visatApp.setStatusBarMessage("Saving image as " + file.getPath() + "..."); /*I18N*/
        visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));


        try {
            RenderedImage image = createRasterImage(view, isNoDataTranparent);

            String imageType = "JPEG";
            String imageName = "image.jpg";
            if (isNoDataTranparent) {
                imageType = "PNG";
                imageName = "image.png";
            }
            ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(file));
            try {
                outStream.putNextEntry(new ZipEntry("kml.kml"));
                final String kmlContent = formatKML(view, imageName);
                outStream.write(kmlContent.getBytes());

                outStream.putNextEntry(new ZipEntry(imageName));
                ImageEncoder encoder = ImageCodec.createImageEncoder(imageType, outStream, null);
                encoder.encode(image);

                if (!view.isRGB()) {
                    outStream.putNextEntry(new ZipEntry("legend.png"));
                    encoder = ImageCodec.createImageEncoder("PNG", outStream, null);
                    encoder.encode(createImageLegend(view.getRaster()));
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
        }
    }

    private static RenderedImage createImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = initImageLegend(raster);
        return imageLegend.createImage();
    }

    private static RenderedImage createRasterImage(final ProductSceneView view, boolean isNoDataTranparent) {
        view.setPinOverlayEnabled(false);
        final ImageDisplay imageDisplay = view.getImageDisplay();
        final BufferedImage bi;
        final double modelOffsetXOld = imageDisplay.getViewModel().getModelOffsetX();
        final double modelOffsetYOld = imageDisplay.getViewModel().getModelOffsetY();
        final double viewScaleOld = imageDisplay.getViewModel().getViewScale();
        imageDisplay.getViewModel().setModelOffset(0, 0, 1.0);

        if (isNoDataTranparent) {
            bi = new BufferedImage(imageDisplay.getImageWidth(),
                                   imageDisplay.getImageHeight(),
                                   BufferedImage.TYPE_4BYTE_ABGR);
            imageDisplay.paintComponent(bi.createGraphics());

            WritableRaster alphaRaster = bi.getAlphaRaster();
            RasterDataNode raster = view.getRaster();
            for (int y = 0; y < raster.getRasterHeight(); y++) {
                for (int x = 0; x < raster.getRasterWidth(); x++) {
                    if (!raster.isPixelValid(x, y)) {
                        int[] t = new int[1];
                        t[0] = 0;
                        alphaRaster.setPixel(x, y, t);
                    }
                }
            }
        } else {
            bi = new BufferedImage(imageDisplay.getImageWidth(),
                                   imageDisplay.getImageHeight(),
                                   BufferedImage.TYPE_3BYTE_BGR);
            imageDisplay.paintComponent(bi.createGraphics());
        }
        imageDisplay.getViewModel().setModelOffset(modelOffsetXOld,
                                                   modelOffsetYOld, viewScaleOld);
        view.setPinOverlayEnabled(true);
        return bi;
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
        ImageLegend imageLegend = new ImageLegend(raster.getImageInfo());

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
        return "[" + unit + "]";
    }

    private static BeamFileFilter createFileFilter(String[] description) {
        final String formatName = description[0];
        final String formatExt = description[1];
        final String formatDescr = description[2];
        return new BeamFileFilter(formatName, formatExt, formatDescr);
    }
}
