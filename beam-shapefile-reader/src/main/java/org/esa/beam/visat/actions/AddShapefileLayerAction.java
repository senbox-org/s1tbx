package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

public class AddShapefileLayerAction extends ExecCommand {

    private static final String DLG_TITLE = "Import Shape";

    @Override
    public void actionPerformed(final CommandEvent event) {
        importShape(VisatApp.getApp());
        VisatApp.getApp().updateState();
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        setEnabled(productSceneView != null);
    }

    private void importShape(final VisatApp visatApp) {
        final PropertyMap propertyMap = visatApp.getPreferences();
        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setDialogTitle(DLG_TITLE);
        fileChooser.setFileFilter(
                new BeamFileFilter("SHAPE", new String[]{".shp"}, "ESRI Shapefiles"));/*I18N*/
        fileChooser.setCurrentDirectory(getIODir(propertyMap));
        final int result = fileChooser.showOpenDialog(visatApp.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(propertyMap, file.getAbsoluteFile().getParentFile());
                loadShape(visatApp, file);
            }
        }
    }

    private static void loadShape(final VisatApp visatApp, final File file) {
        final ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view == null) {
            return;
        }


        final StringBuilder builder = new StringBuilder("Failed to import shapefile.\n");
        final RasterDataNode raster = view.getRaster();
        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding == null || !geoCoding.canGetPixelPos()) {
            builder.append("Suitable geo-coding required for reading ESRI shapefiles.");
            visatApp.showErrorDialog(DLG_TITLE, builder.toString()); /*I18N*/
            return;
        }

        Layer shapeLayer;
        try {
            shapeLayer = readShape(file, geoCoding);
            if (shapeLayer == null) {
                builder.append("All coordinates out of the product''s scene bounds.");
                visatApp.showErrorDialog(DLG_TITLE, builder.toString());
                return;
            }
        } catch (IOException e) {
            builder.append(MessageFormat.format("An I/O Error occured:\n{0}", e.getMessage()));
            visatApp.showErrorDialog(DLG_TITLE, builder.toString());
            return;
        }

        GeographicCRS baseCRS = DefaultGeographicCRS.WGS84;
        MathTransform baseToGridMathTransform = new GeoCodingMathTransform(geoCoding,
                                                                           GeoCodingMathTransform.Mode.P2G);
        CoordinateReferenceSystem gridCRS = new DefaultDerivedCRS(raster.getName(),
                                                                  baseCRS,
                                                                  baseToGridMathTransform,
                                                                  DefaultCartesianCS.GRID);


        final List<Layer> children = view.getRootLayer().getChildren();
        children.add(children.size() - 1, shapeLayer);
        shapeLayer.setVisible(false);
        shapeLayer.setVisible(true);
    }

    public static Layer readShape(File file, GeoCoding geoCoding) throws IOException {
        return new ShapefileLayer(file);
    }


    private static void setIODir(final PropertyMap propertyMap, final File dir) {
        if (dir != null) {
            propertyMap.setPropertyString("shape.io.dir", dir.getPath());
        }
    }

    private static File getIODir(final PropertyMap propertyMap) {
        final File dir = SystemUtils.getUserHomeDir();
        return new File(propertyMap.getPropertyString("shape.io.dir", dir.getPath()));
    }

    private static class PixelToGeoMathTransform extends AbstractMathTransform {

        final private static int DIMS = 2;
        private final GeoCoding geoCoding;

        public PixelToGeoMathTransform(GeoCoding geoCoding) {
            this.geoCoding = geoCoding;
        }

        @Override
        public int getSourceDimensions() {
            return DIMS;
        }

        @Override
        public int getTargetDimensions() {
            return DIMS;
        }

        @Override
        public void transform(double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            GeoPos geoPos = new GeoPos();
            PixelPos pixelPos = new PixelPos();
            for (int i = 0; i < numPts; i++) {
                final int firstIndex = (DIMS * i);
                final int secondIndex = firstIndex + 1;
                pixelPos.x = (float) srcPts[srcOff + firstIndex];
                pixelPos.y = (float) srcPts[srcOff + secondIndex];

                geoCoding.getGeoPos(pixelPos, geoPos);

                dstPts[dstOff + firstIndex] = geoPos.lon;
                dstPts[dstOff + secondIndex] = geoPos.lat;
            }
        }

        @Override
        public MathTransform inverse() throws NoninvertibleTransformException {
            return super.inverse();

        }
    }
}