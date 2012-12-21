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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.dataio.geometry.VectorDataNodeIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.SLDUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The import vector operator.
 */
@OperatorMetadata(alias = "Import-Vector",
        category = "Utilities",
        authors = "NEST team", copyright = "(C) 2013 by Array Systems Computing Inc.",
        description = "Imports a shape file into a product")
public class ImportVectorOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(label = "Vector File")
    private File vectorFile = null;

    @Parameter(label = "Separate Shapes", defaultValue = "true")
    private boolean separateShapes = true;

    @Override
    public void initialize() throws OperatorException {
        try {

            targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            for(String bandName : sourceProduct.getBandNames()) {
                ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
            }

            if(vectorFile != null) {
                importGeometry(targetProduct, vectorFile);
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void importGeometry(final Product product, final File file) {

        final GeoCoding geoCoding = product.getGeoCoding();
        if (isShapefile(file) && (geoCoding == null || !geoCoding.canGetPixelPos())) {
            throw new OperatorException("Current geo-coding cannot convert from geographic to pixel coordinates."); /* I18N */
        }

        final VectorDataNodeReader reader;
        if (isShapefile(file)) {
            reader = new VdnShapefileReader();
        } else {
            reader = new VdnTextReader();
        }

        VectorDataNode vectorDataNode;
        try {
            vectorDataNode = reader.readVectorDataNode(file, product, null, ProgressMonitor.NULL);
        } catch (Exception e) {
            throw new OperatorException("Failed to import geometry.\n" + "An I/O Error occurred:\n"
                    + e.getMessage()); /* I18N */
        }

        VectorDataNode[] vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, separateShapes, "importedVector");
        for (VectorDataNode vectorDataNode1 : vectorDataNodes) {
            product.getVectorDataGroup().add(vectorDataNode1);
        }
    }

    private static String findUniqueVectorDataNodeName(final String suggestedName,
                                                       final ProductNodeGroup<VectorDataNode> vectorDataGroup) {
        String name = suggestedName;
        int index = 1;
        while (vectorDataGroup.contains(name)) {
            name = suggestedName + '_' + index;
            index++;
        }
        return name;
    }

    private static boolean isShapefile(final File file) {
        return file.getName().toLowerCase().endsWith(".shp");
    }

    interface VectorDataNodeReader {

        VectorDataNode readVectorDataNode(File file, Product product, String helpId, ProgressMonitor pm) throws IOException;
    }

    static class VdnShapefileReader implements VectorDataNodeReader {

        @Override
        public VectorDataNode readVectorDataNode(File file, Product product, String helpId, ProgressMonitor pm) throws IOException {

            MyFeatureCrsProvider crsProvider = new MyFeatureCrsProvider(helpId);
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = FeatureUtils.loadShapefileForProduct(file,
                    product, crsProvider, pm);
            Style[] styles = SLDUtils.loadSLD(file);
            ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
            String name = findUniqueVectorDataNodeName(featureCollection.getSchema().getName().getLocalPart(),
                    vectorDataGroup);
            if (styles.length > 0) {
                SimpleFeatureType featureType = SLDUtils.createStyledFeatureType(featureCollection.getSchema());

                VectorDataNode vectorDataNode = new VectorDataNode(name, featureType);
                FeatureCollection<SimpleFeatureType, SimpleFeature> styledCollection = vectorDataNode.getFeatureCollection();
                String defaultCSS = vectorDataNode.getDefaultStyleCss();
                SLDUtils.applyStyle(styles[0], defaultCSS, featureCollection, styledCollection);
                return vectorDataNode;
            } else {
                return new VectorDataNode(name, featureCollection);
            }
        }

        private class MyFeatureCrsProvider implements FeatureUtils.FeatureCrsProvider {

            private final String helpId;

            public MyFeatureCrsProvider(String helpId) {
                this.helpId = helpId;
            }

            @Override
            public CoordinateReferenceSystem getFeatureCrs(final Product product) {
                final CoordinateReferenceSystem[] featureCrsBuffer = new CoordinateReferenceSystem[1];
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        featureCrsBuffer[0] = product.getGeoCoding().getMapCRS();
                    }
                };
                if (!SwingUtilities.isEventDispatchThread()) {
                    try {
                        SwingUtilities.invokeAndWait(runnable);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    runnable.run();
                }
                CoordinateReferenceSystem featureCrs = featureCrsBuffer[0];
                return featureCrs != null ? featureCrs : DefaultGeographicCRS.WGS84;
            }

        }
    }

    static class VdnTextReader implements VectorDataNodeReader {

        @Override
        public VectorDataNode readVectorDataNode(File file, Product product, String helpId, ProgressMonitor pm) throws IOException {
            final ArrayList<PixelPos> pixelPositions = new ArrayList<PixelPos>(256);
            final GeoCoding geoCoding = product.getGeoCoding();

            final FileReader fileReader = new FileReader(file);
            final LineNumberReader reader = new LineNumberReader(fileReader);
            try {

                final StreamTokenizer st = createConfiguredTokenizer(reader);

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
                        final boolean latLonAvailable =
                                valid[2] && valid[3] && geoCoding != null && geoCoding.canGetPixelPos();
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

                        Arrays.fill(values, 0.0f);
                        Arrays.fill(valid, false);

                        if (tt == StreamTokenizer.TT_EOF) {
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
            if (!pixelPositions.isEmpty()) {
                geometry = createGeometry(pixelPositions);
            }
            if (geometry == null) {
                return null;
            }
            CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(geoCoding);
            AffineTransform imageToModelTransform = ImageManager.getImageToModelTransform(geoCoding);
            GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(new AffineTransform2D(imageToModelTransform));
            transformer.setCoordinateReferenceSystem(modelCrs);
            try {
                geometry = transformer.transform(geometry);
            } catch (TransformException ignored) {
                return null;
            }

            String name = FileUtils.getFilenameWithoutExtension(file);
            findUniqueVectorDataNodeName(name, product.getVectorDataGroup());
            SimpleFeatureType simpleFeatureType = PlainFeatureFactory.createDefaultFeatureType(modelCrs);
            DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(name, simpleFeatureType);

            VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection);
            String style = vectorDataNode.getDefaultStyleCss();
            SimpleFeature simpleFeature = PlainFeatureFactory.createPlainFeature(simpleFeatureType, name, geometry, style);
            featureCollection.add(simpleFeature);

            return vectorDataNode;
        }

        private static Geometry createGeometry(ArrayList<PixelPos> pixelPositions) {
            GeometryFactory geometryFactory = new GeometryFactory();
            ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
            PixelPos pixelPos0 = pixelPositions.get(0);
            coordinates.add(new Coordinate(pixelPos0.x, pixelPos0.y));
            PixelPos pixelPos1 = null;
            for (int i = 1; i < pixelPositions.size(); i++) {
                pixelPos1 = pixelPositions.get(i);
                coordinates.add(new Coordinate(pixelPos1.x, pixelPos1.y));
            }
            if (pixelPos1 != null && pixelPos1.distanceSq(pixelPos0) < 1.0e-5) {
                coordinates.add(coordinates.get(0));
            }
            if (coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
                return geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
            } else {
                return geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
            }
        }

        private static StreamTokenizer createConfiguredTokenizer(LineNumberReader reader) {
            final StreamTokenizer st = new StreamTokenizer(reader);
            st.resetSyntax();
            st.eolIsSignificant(true);
            st.lowerCaseMode(true);
            st.commentChar('#');
            st.whitespaceChars(' ', ' ');
            st.whitespaceChars('\t', '\t');
            st.wordChars(33, 255);
            return st;
        }
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ImportVectorOp.class);
        }
    }
}