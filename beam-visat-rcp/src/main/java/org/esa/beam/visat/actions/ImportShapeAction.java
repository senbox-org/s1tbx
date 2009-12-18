package org.esa.beam.visat.actions;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.jai.ImageManager;
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
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.AffineTransform;
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
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null);
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
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product == null) {
            return;
        }

        final GeoCoding geoCoding = product.getGeoCoding();
        if (isShapefile(file) && (geoCoding == null || !geoCoding.canGetPixelPos())) {
            visatApp.showErrorDialog(DLG_TITLE, "Failed to import shape.\n"
                    + "Current geo-coding cannot convert from geographic to pixel coordinates."); /* I18N */
            return;
        }

        VectorDataNode vectorDataNode;
        try {
            vectorDataNode = readShape(file, product);
        } catch (Exception e) {
            visatApp.showErrorDialog(DLG_TITLE, "Failed to import shape.\n" + "An I/O Error occured:\n"
                    + e.getMessage()); /* I18N */
            return;
        }

        if (vectorDataNode.getFeatureCollection().size() == 0) {
            visatApp.showErrorDialog(DLG_TITLE, "The shape was loaded successfully,\n"
                    + "but no part is located within the scene boundaries."); /* I18N */
            return;
        }
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        vectorDataGroup.add(vectorDataNode);
    }
    
    private static String findUniqueVectorName(String sugestedName, ProductNodeGroup<VectorDataNode> vectorDataGroup) {
        String name = sugestedName;
        int index = 1;
        while (vectorDataGroup.contains(name)) {
            name = sugestedName + "_" + index;
            index++;
        }
        return name;
    }

    private static boolean isShapefile(File file) {
        return file.getName().toLowerCase().endsWith(".shp");
    }

    private static VectorDataNode readShape(File file, Product product) throws IOException {
        if (isShapefile(file)) {
            return readShapeFromShapefile(file, product);
        } else {
            return readShapeFromTextFile(file, product);
        }
    }

    private static VectorDataNode readShapeFromShapefile(File file, Product product) throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> loadShapefile = ShapefileUtils.loadShapefile(file, product);
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        String name = findUniqueVectorName(loadShapefile.getSchema().getName().getLocalPart(), vectorDataGroup);
        VectorDataNode vectorDataNode = new VectorDataNode(name, loadShapefile);
        return vectorDataNode;
    }

    private static VectorDataNode readShapeFromTextFile(final File file, Product product) throws IOException {
        final FileReader fileReader = new FileReader(file);
        final LineNumberReader reader = new LineNumberReader(fileReader);
        final ArrayList<PixelPos> pixelPositions = new ArrayList<PixelPos>(256);
        final GeoCoding geoCoding = product.getGeoCoding();

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

        Geometry geometry = null;
        if (pixelPositions.size() > 0) {
            GeometryFactory geometryFactory = new GeometryFactory();
            ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
            PixelPos pixelPos0 = pixelPositions.get(0);
            coordinates.add(new Coordinate(pixelPos0.x, pixelPos0.y));
            PixelPos pixelPos1 = null;
            for (int i = 1; i < pixelPositions.size(); i++) {
                pixelPos1 = pixelPositions.get(i);
                coordinates.add(new Coordinate(pixelPos1.x, pixelPos1.y));
            }
            if (pixelPos1 != null && pixelPos1.distanceSq(pixelPos0) < 1e-5) {
                coordinates.add(coordinates.get(0));
            }
            if (coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
                geometry = geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
            } else {
                geometry = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
            }
        }
        if (geometry == null) {
            return null;
        }
        CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(geoCoding);
        AffineTransform imageToModelTransform = ImageManager.getImageToModelTransform(geoCoding);
        GeometryCoordinateSequenceTransformer transformer;
        transformer = new GeometryCoordinateSequenceTransformer();
        transformer.setMathTransform(new AffineTransform2D(imageToModelTransform));
        transformer.setCoordinateReferenceSystem(modelCrs);
        try {
            geometry = transformer.transform(geometry);
        } catch (TransformException e) {
            return null;
        }
 
        String name = FileUtils.getFilenameWithoutExtension(file);
        findUniqueVectorName(name, product.getVectorDataGroup());
        SimpleFeatureType simpleFeatureType = PlainFeatureFactory.createDefaultFeatureType(modelCrs);
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(name, simpleFeatureType);

        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection);
        String style = vectorDataNode.getDefaultCSS();
        SimpleFeature simpleFeature = PlainFeatureFactory.createPlainFeature(simpleFeatureType, name, geometry, style);
        featureCollection.add(simpleFeature);
        
        return vectorDataNode;
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
