package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.help.HelpSys;
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
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StreamTokenizer;

public class ImportTransectDataAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        importTransectData(VisatApp.getApp());
        VisatApp.getApp().updateState();
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        setEnabled(productSceneView != null);
    }

    private void importTransectData(final VisatApp visatApp) {
        final PropertyMap propertyMap = visatApp.getPreferences();
        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setDialogTitle("Import Transect Data");
        fileChooser.setFileFilter(
                new BeamFileFilter("TRANSECT_DATA", new String[]{".txt", ".dat", ".shp"},
                                   "Transect text data files"));/*I18N*/
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
            final int status = visatApp.showQuestionDialog("Import Shape",
                                                           "This will delete the current shape.\n"
                                                           + "Do you really wish to continue?",
                                                           "plugin.imprt.shape.tip"); /*I18N*/
            if (status != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final RasterDataNode raster = productSceneView.getRaster();
        final GeoCoding geoCoding = raster.getProduct().getGeoCoding();

        Shape shape;
        try {
            shape = loadShape(file, geoCoding);
            if (shape == null) {
                visatApp.showErrorDialog("Failed to import shape.\n" +
                                         "All coordinates out of the product's scene bounds."); /*I18N*/
                return;
            }
        } catch (IOException e) {
            visatApp.showErrorDialog("Failed to import shape.\n" +
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
            visatApp.showErrorDialog("The shape was loaded successfully,\n"
                                     + "but is entirely out of the scene bounds."); /*I18N*/
            return;
        }

        final Figure figure = new ShapeFigure(shape, true, null);
        productSceneView.setCurrentShapeFigure(figure);
    }

    private static Shape loadShape(final File file, final GeoCoding geoCoding) throws IOException {
        final FileReader fileReader = new FileReader(file);
        final LineNumberReader reader = new LineNumberReader(fileReader);
        GeneralPath generalPath = null;

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
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();

            boolean headerAvailable = false;
            int column = 0;

            while (true) {
                final int tt = st.nextToken();

                if (tt == StreamTokenizer.TT_EOF
                    || tt == StreamTokenizer.TT_EOL) {
                    final boolean xyAvailable = valid[0] && valid[1];
                    final boolean latLonAvailable = valid[2] && valid[3] && geoCoding != null && geoCoding.canGetPixelPos();
                    if (xyAvailable || latLonAvailable) {
                        float x = values[0];
                        float y = values[1];
                        if (latLonAvailable) {
                            geoPos.lat = values[2];
                            geoPos.lon = values[3];
                            geoCoding.getPixelPos(geoPos, pixelPos);
                            x = pixelPos.x;
                            y = pixelPos.y;
                        }

                        // Do not add positions which are out of bounds, it leads to scrambled shapes
                        if (x != -1 && y != -1) {
                            if (generalPath == null) {
                                generalPath = new GeneralPath();
                                generalPath.moveTo(x, y);
                            } else {
                                generalPath.lineTo(x, y);
                            }
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
        return generalPath;
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
