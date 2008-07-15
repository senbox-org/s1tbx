package org.esa.beam.visat.actions;

import org.esa.beam.dataio.shapefile.Shapefile;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;

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
        fileChooser.setFileFilter(
                new BeamFileFilter("SHAPE", new String[]{".txt", ".dat", ".csv", ".shp"},
                                   "Shape files"));/*I18N*/
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

        if (productSceneView.getCurrentShapeFigure() != null) {
            final int status = visatApp.showQuestionDialog(DLG_TITLE,
                                                           "This will delete the current shape.\n"
                                                                   + "Do you really wish to continue?",
                                                           "plugin.imprt.shape.tip"); /*I18N*/
            if (status != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final RasterDataNode raster = productSceneView.getRaster();
        final GeoCoding geoCoding = raster.getProduct().getGeoCoding();
        if (file.getName().endsWith(".shp") && (geoCoding == null || !geoCoding.canGetPixelPos())) {
            visatApp.showErrorDialog(DLG_TITLE,
                                     "Failed to import shape.\n" +
                                             "Suitable geo-coding required for reading ESRI shapefiles."); /*I18N*/
            return;
        }

        Shape shape = null;
        try {
            shape = readShape(file, geoCoding);
            if (shape == null) {
                visatApp.showErrorDialog(DLG_TITLE,
                                         "Failed to import shape.\n" +
                                                 "All coordinates out of the product's scene bounds."); /*I18N*/
                return;
            }
        } catch (IOException e) {
            visatApp.showErrorDialog(DLG_TITLE,
                                     "Failed to import shape.\n" +
                                             "An I/O Error occured:\n" + e.getMessage()); /*I18N*/
            return;
        }

        final Rectangle2D rasterBounds = new Rectangle(0, 0,
                                                       raster.getSceneRasterWidth(),
                                                       raster.getSceneRasterHeight());
        final Rectangle2D shapeBounds = shape.getBounds2D();
        if (!rasterBounds.contains(shapeBounds)
                && !shapeBounds.contains(rasterBounds)
                && !shape.intersects(rasterBounds)) {
            visatApp.showErrorDialog(DLG_TITLE,
                                     "The shape was loaded successfully,\n"
                                             + "but is entirely out of the scene bounds."); /*I18N*/
            return;
        }

        final Figure figure = new ShapeFigure(shape, true, null);
        productSceneView.setCurrentShapeFigure(figure);
    }

    public static Shape readShape(File file, GeoCoding geoCoding) throws IOException {
        Shape shape;
        if (file.getName().endsWith(".shp")) {
            shape = readShapeFromShapefile(file, geoCoding);
        } else {
            shape = readShapeFromTextFile(file, geoCoding);
        }
        return shape;
    }

    public static Shape readShapeFromShapefile(File file, final GeoCoding geoCoding) throws IOException {
        final Shapefile.Transform transform = new Shapefile.Transform() {
            public Shapefile.Point transformPoint(Shapefile.Point pt) {
                final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos((float) pt.y, (float) pt.x), null);
                return new Shapefile.Point(pixelPos.x, pixelPos.y);
            }
        };
        final Shapefile shapefile = Shapefile.getShapefile(file);
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        while (true) {
            final Shapefile.Record record = shapefile.readRecord();
            if (record == null) {
                break;
            }
            final Shapefile.Geometry geometry = record.getGeometry();
            final Shape shape = geometry.toShape(transform);
            if (shape != null) {
                shapes.add(shape);
            }
        }
        if (shapes.size() == 0) {
            return null;
        } else if (shapes.size() == 1) {
            return shapes.get(0);
        } else {
            final Area area = new Area();
            for (Shape shape : shapes) {
                area.add(new Area(shape));
            }
            return area;
        }
    }

    public static Shape readShapeFromTextFile(final File file, final GeoCoding geoCoding) throws IOException {
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
        return path;
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
