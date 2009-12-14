package org.esa.beam.visat.actions;

import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.SimpleFeatureFigureFactory;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.AwtGeomToJtsGeomConverter;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.ShapefileUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;

import javax.swing.JFileChooser;

public class ImportShapeAction extends ExecCommand {
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
        final BeamFileFilter plainTextFilter = new BeamFileFilter("CSV",
                                                                  new String[]{".txt", ".dat", ".csv"},
                                                                  "Plain text");
        final BeamFileFilter shapefileFilter = new BeamFileFilter("SHAPEFILE",
                                                                  new String[]{".shp"},
                                                                  "ESRI shapefiles");
        fileChooser.addChoosableFileFilter(plainTextFilter);
        fileChooser.addChoosableFileFilter(shapefileFilter);
        fileChooser.setFileFilter(shapefileFilter);/*I18N*/
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
        final ProductSceneView productSceneView = visatApp.getSelectedProductSceneView();
        if (productSceneView == null) {
            return;
        }

        final RasterDataNode raster = productSceneView.getRaster();
        final GeoCoding geoCoding = raster.getProduct().getGeoCoding();
        if (isShapefile(file)
                && (geoCoding == null || !geoCoding.canGetPixelPos())) {
            visatApp.showErrorDialog(DLG_TITLE,
                                     "Failed to import shape.\n" +
                                             "Current geo-coding cannot convert from geographic to pixel coordinates."); /*I18N*/
            return;
        }

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureColl;
        try {
            featureColl = readShape(file, raster);
        } catch (Exception e) {
            visatApp.showErrorDialog(DLG_TITLE,
                                     "Failed to import shape.\n" +
                                             "An I/O Error occured:\n" + e.getMessage()); /*I18N*/
            return;
        }

        final Rectangle2D rasterBounds = new Rectangle(0, 0, raster.getSceneRasterWidth(),
                                                       raster.getSceneRasterHeight());
        ReferencedEnvelope rasterRefEnvelope = new ReferencedEnvelope(rasterBounds, geoCoding.getImageCRS());
        ReferencedEnvelope shapeRefEnvelope = featureColl.getBounds();
        
        if (!rasterRefEnvelope.contains((BoundingBox)shapeRefEnvelope) &&
                !shapeRefEnvelope.contains((BoundingBox)rasterRefEnvelope) && 
                !shapeRefEnvelope.intersects((BoundingBox)rasterBounds)) {
            visatApp.showErrorDialog(DLG_TITLE, "The shape was loaded successfully,\n"
                    + "but no part is located within the scene boundaries."); /* I18N */
            return;
        }
        
        String localPart = featureColl.getSchema().getName().getLocalPart();
        Product product = productSceneView.getProduct();
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        String name = localPart;
        int index = 1;
        while (vectorDataGroup.contains(name)) {
            name = localPart + "_" + index;
            index++;
        }
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureColl);
        vectorDataGroup.add(vectorDataNode);
    }

    private static boolean isShapefile(File file) {
        return file.getName().toLowerCase().endsWith(".shp");
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> readShape(File file, RasterDataNode raster) throws IOException {
        if (isShapefile(file)) {
            return readShapeFromShapefile(file, raster);
        } else {
            return readShapeFromTextFile(file, raster.getGeoCoding());
        }
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> readShapeFromShapefile(File file, final RasterDataNode raster) throws IOException {
        return ShapefileUtils.loadShapefile(file, raster);
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> readShapeFromTextFile(final File file, final GeoCoding geoCoding) throws IOException {
        final FileReader fileReader = new FileReader(file);
        final LineNumberReader reader = new LineNumberReader(fileReader);
        final ArrayList<PixelPos> pixelPositions = new ArrayList<PixelPos>(256);

        try {

            final StreamTokenizer st = new StreamTokenizer(reader);
            st.resetSyntax();
            st.eolIsSignificant(true);
            st.lowerCaseMode(true);
            st.commentChar('#');
            st.whitespaceChars(' ', ' ');
            st.whitespaceChars('\t', '\t');
            st.wordChars(33, 255);

            final float[] values = new float[]{0.0F, 0.0F, 0.0F, 0.0F}; // values for {x, y, lat, lon}
            final boolean[] valid = new boolean[]{false, false, false, false}; // {x, y, lat, lon} columns valid?
            final int[] indices = new int[]{0, 1, 2, 3}; // indexes of {x, y, lat, lon} columns

            boolean headerAvailable = false;
            int column = 0;

            while (true) {
                final int tt = st.nextToken();

                if (tt == StreamTokenizer.TT_EOF
                        || tt == StreamTokenizer.TT_EOL) {
                    final boolean xyAvailable = valid[0] && valid[1];
                    final boolean latLonAvailable = valid[2] && valid[3] && geoCoding != null && geoCoding.canGetPixelPos();
                    if (xyAvailable || latLonAvailable) {
                        PixelPos pixelPos;
                        if (latLonAvailable) {
                            pixelPos = geoCoding.getPixelPos(new GeoPos(values[2], values[3]), null);
                        } else {
                            pixelPos = new PixelPos(values[0], values[1]);
                        }

                        // Do not add positions which are out of bounds, it leads to scrambled shapes
                        if (pixelPos.x != -1 && pixelPos.y != -1) {
                            pixelPositions.add(pixelPos);
                        }
                    }

                    for (int i = 0; i < 4; i++) {
                        values[i] = 0.0F;
                        valid[i] = false;
                    }

                    if (tt == StreamTokenizer.TT_EOF) {
                        column = 0;
                        break;
                    } else if (tt == StreamTokenizer.TT_EOL) {
                        column = 0;
                    }
                } else if (tt == StreamTokenizer.TT_WORD) {
                    final String token = st.sval;
                    int headerText = -1;
                    if ("x".equalsIgnoreCase(token)
                            || "pixel-x".equalsIgnoreCase(token)
                            || "pixel_x".equalsIgnoreCase(token)) {
                        indices[0] = column;
                        headerText = 0;
                    } else if ("y".equalsIgnoreCase(token)
                            || "pixel-y".equalsIgnoreCase(token)
                            || "pixel_y".equalsIgnoreCase(token)) {
                        indices[1] = column;
                        headerText = 1;
                    } else if ("lat".equalsIgnoreCase(token)
                            || "latitude".equalsIgnoreCase(token)) {
                        indices[2] = column;
                        headerText = 2;
                    } else if ("lon".equalsIgnoreCase(token)
                            || "long".equalsIgnoreCase(token)
                            || "longitude".equalsIgnoreCase(token)) {
                        indices[3] = column;
                        headerText = 3;
                    } else {
                        for (int i = 0; i < 4; i++) {
                            if (column == indices[i]) {
                                try {
                                    values[i] = Float.parseFloat(token);
                                    valid[i] = true;
                                    break;
                                } catch (NumberFormatException ignore) {
                                }
                            }
                        }
                    }
                    if (!headerAvailable && headerText >= 0) {
                        for (int i = 0; i < indices.length; i++) {
                            if (headerText != i) {
                                indices[i] = -1;
                            }
                        }
                        headerAvailable = true;
                    }
                    column++;
                } else {
                    Debug.assertTrue(false);
                }
            }
        } finally {
            reader.close();
            fileReader.close();
        }

        final Path2D.Float path = new Path2D.Float();
        if (pixelPositions.size() > 0) {
            PixelPos pixelPos0 = pixelPositions.get(0);
            path.moveTo(pixelPos0.x, pixelPos0.y);
            PixelPos pixelPos1 = null;
            for (int i = 1; i < pixelPositions.size(); i++) {
                pixelPos1 = pixelPositions.get(i);
                path.lineTo(pixelPos1.x, pixelPos1.y);
            }
            if (pixelPos1 != null && pixelPos1.distanceSq(pixelPos0) < 1e-5) {
                path.closePath();
            }
        }
        String name = FileUtils.getFilenameWithoutExtension(file);
        CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(geoCoding);
        SimpleFeatureType simpleFeatureType = SimpleFeatureFigureFactory.createSimpleFeatureType(name, Geometry.class, modelCrs);
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(name, simpleFeatureType);
        
        SimpleFeatureFigureFactory simpleFeatureFigureFactory = new SimpleFeatureFigureFactory(featureCollection);
        DefaultFigureStyle defaultStyle = SimpleFeatureFigureFactory.createDefaultStyle();
        AwtGeomToJtsGeomConverter awtGeomToJtsGeomConverter = new AwtGeomToJtsGeomConverter();
        Polygon polygon = awtGeomToJtsGeomConverter.createPolygon(path);
        SimpleFeature simpleFeature = simpleFeatureFigureFactory.createSimpleFeature(polygon, defaultStyle);
        featureCollection.add(simpleFeature);
        
        return featureCollection;
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
}
