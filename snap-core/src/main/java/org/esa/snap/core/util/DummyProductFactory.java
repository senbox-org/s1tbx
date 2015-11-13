package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility class for creating various Product dummy instances for testing.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class DummyProductFactory {

    /**
     * Occurrence of something.
     */
    public enum Occurrence {
        /**
         * Single / Unique
         */
        S,
        /**
         * Multiple / various
         */
        M,
    }

    /**
     * Raster Size
     */
    public enum Size {
        /**
         * Small
         */
        S,
        /**
         * Medium
         */
        M,
        /**
         * Large
         */
        L,
    }

    /**
     * Geo Coding
     */
    public enum GC {
        /**
         * No geo-coding
         */
        NON,
        /**
         * Map/CRS geo-coding
         */
        MAP,
        /**
         * Tie-point grid geo-coding
         */
        TPG,
        /**
         * Per-pixel geo-coding
         */
        PIX,
    }

    /**
     * Geo Position
     */
    public enum GP {
        /**
         * Null-meridian crossing
         */
        NMER,
        /**
         * Anti-meridian crossing (dateline)
         */
        AMER,
        /**
         * North pole
         */
        NPOL,
        /**
         * South pole
         */
        SPOL,
    }

    /**
     * Type of test product to be created.
     */
    public static class Type {
        public final Size size;
        public final Occurrence sizeOcc;
        public final GC gc;
        public final Occurrence gcOcc;
        public final GP gp;

        public Type(Size size, Occurrence sizeOcc, GC gc, Occurrence gcOcc, GP gp) {
            this.size = size;
            this.sizeOcc = sizeOcc;
            this.gc = gc;
            this.gcOcc = gcOcc;
            this.gp = gp;
        }
    }

    /**
     * Creates a test product of the given type.
     *
     * @param type The test product type.
     */
    public static Product createProduct(Type type) {

        String name = String.format("Test-SZ_%s_%s-GC_%s_%s_%s", type.size, type.sizeOcc, type.gc, type.gcOcc, type.gp);
        String description = String.format("%s %s-size test product at %s with %s %s geo-codings",
                                           type.size == Size.S ? "Small"
                                                   : type.size == Size.M ? "Medium"
                                                   : "Large",
                                           type.sizeOcc == Occurrence.M ? "multi"
                                                   : "single",
                                           type.gp == GP.NMER ? "null-meridian"
                                                   : type.gp == GP.AMER ? "anti-meridian"
                                                   : type.gp == GP.NPOL ? "north pole"
                                                   : "south pole",
                                           type.gcOcc == Occurrence.M ? "different"
                                                   : "unique",
                                           type.gc == GC.NON ? "no"
                                                   : type.gc == GC.MAP ? "map"
                                                   : type.gc == GC.TPG ? "tie-point"
                                                   : "per-pixel");

        int sceneRasterWidth = 1800;
        int sceneRasterHeight = 3600;
        int gridWidth = sceneRasterWidth / 10;
        int gridHeight = sceneRasterHeight / 10;
        int tileSize = 512;
        if (type.size == Size.L) {
            sceneRasterWidth *= 15;
            sceneRasterHeight *= 15;
            gridWidth *= 15;
            gridHeight *= 15;
            tileSize = 2048;
        } else if (type.size == Size.S) {
            sceneRasterWidth /= 15;
            sceneRasterHeight /= 15;
            gridWidth /= 15;
            gridHeight /= 15;
            tileSize = 256;
        }

        int nx = sceneRasterWidth / gridWidth;
        if (nx * gridWidth <= sceneRasterWidth) {
            gridWidth = sceneRasterWidth / nx + 1;
        }
        int ny = sceneRasterHeight / gridHeight;
        if (ny * gridHeight <= sceneRasterHeight) {
            gridHeight = sceneRasterHeight / ny + 1;
        }

        Product product = new Product(name, name, sceneRasterWidth, sceneRasterHeight);
        product.setDescription(description);
        product.setPreferredTileSize(tileSize, tileSize);
        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));

        addRandomTiePointGrids(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight);

        String bandAExpr = "sin(4 * PI * sqrt( sq(0.5*X/%s - 1) + sq(0.5*Y/%s - 1) ))";
        String bandBExpr = "sin(4 * PI * sqrt( 2.0 * abs(0.5*X/%s * 0.5*Y/%s) ))";
        String bandCExpr = "cos( X/180 ) * cos( Y/180 )";
        String bandDExpr = "max(cos( X/180 ), cos( Y/180 ))";

        String maskAExpr = "band_a%s > 0.5";
        String maskBExpr = "band_b%s < 0.0";
        String maskCExpr = "band_c%1$s > -0.1 && band_c%1$s < 0.1";

        if (type.sizeOcc == Occurrence.M) {
            addMultiSizeBands(product, sceneRasterWidth, sceneRasterHeight, "band_a", bandAExpr, 500, "mask_a", maskAExpr, Color.ORANGE);
            addMultiSizeBands(product, sceneRasterWidth, sceneRasterHeight, "band_b", bandBExpr, 600, "mask_b", maskBExpr, Color.GREEN);
            addMultiSizeBands(product, sceneRasterWidth, sceneRasterHeight, "band_c", bandCExpr, 700, "mask_c", maskCExpr, Color.BLUE);
            setMultiSizeGeoCodings(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, bandDExpr, type);
        } else {
            addSingleSizeBand(product, sceneRasterWidth, sceneRasterHeight, "band_a", bandAExpr, 500, "mask_a", maskAExpr, Color.ORANGE);
            addSingleSizeBand(product, sceneRasterWidth, sceneRasterHeight, "band_b", bandBExpr, 600, "mask_b", maskBExpr, Color.GREEN);
            addSingleSizeBand(product, sceneRasterWidth, sceneRasterHeight, "band_c", bandCExpr, 700, "mask_c", maskCExpr, Color.BLUE);
            setSingleSizeGeoCoding(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, bandDExpr, type);
        }

        product.setModified(false);
        return product;
    }

    private static void setMultiSizeGeoCodings(Product product, int sceneRasterWidth, int sceneRasterHeight, int gridWidth, int gridHeight, String bandDExpr, Type type) {
        if (type.gc == GC.NON) {
            product.setSceneGeoCoding(null);
        } else if (type.gc == GC.MAP) {
            Map<Dimension, GeoCoding[]> dimensionSet = new HashMap<>();
            for (RasterDataNode rasterDataNode : product.getRasterDataNodes()) {
                dimensionSet.put(rasterDataNode.getRasterSize(), new GeoCoding[1]);
            }
            for (Dimension dimension : dimensionSet.keySet()) {
                dimensionSet.get(dimension)[0] = createCrsGeoCoding(dimension.width, dimension.height, type.gp);
            }
            for (RasterDataNode rasterDataNode : product.getRasterDataNodes()) {
                rasterDataNode.setGeoCoding(dimensionSet.get(rasterDataNode.getRasterSize())[0]);
            }
            product.setSceneGeoCoding(dimensionSet.get(product.getSceneRasterSize())[0]);
            if (type.gcOcc == Occurrence.M) {
                product.addBand("band_d", bandDExpr).setGeoCoding(createTiePointGeoCoding(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, type.gp));
            }
        } else if (type.gc == GC.TPG) {
            product.setSceneGeoCoding(createTiePointGeoCoding(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, type.gp));
            if (type.gcOcc == Occurrence.M) {
                product.addBand("band_d", bandDExpr).setGeoCoding(createCrsGeoCoding(sceneRasterWidth, sceneRasterHeight, type.gp));
            }
        } else if (type.gc == GC.PIX) {
            product.setSceneGeoCoding(createPixelGeoCoding(product, sceneRasterWidth, sceneRasterHeight, type.gp));
            if (type.gcOcc == Occurrence.M) {
                product.addBand("band_d", bandDExpr).setGeoCoding(createCrsGeoCoding(sceneRasterWidth, sceneRasterHeight, type.gp));
            }
        }
    }

    private static void setSingleSizeGeoCoding(Product product, int sceneRasterWidth, int sceneRasterHeight, int gridWidth, int gridHeight, String bandDExpr, Type type) {
        if (type.gc == GC.NON) {
            product.setSceneGeoCoding(null);
        } else if (type.gc == GC.MAP) {
            product.setSceneGeoCoding(createCrsGeoCoding(sceneRasterWidth, sceneRasterHeight, type.gp));
            if (type.gcOcc == Occurrence.M) {
                product.addBand("band_d", bandDExpr).setGeoCoding(createTiePointGeoCoding(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, type.gp));
            }
        } else if (type.gc == GC.TPG) {
            product.setSceneGeoCoding(createTiePointGeoCoding(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, type.gp));
            if (type.gcOcc == Occurrence.M) {
                product.addBand("band_d", bandDExpr).setGeoCoding(createCrsGeoCoding(sceneRasterWidth, sceneRasterHeight, type.gp));
            }
        } else if (type.gc == GC.PIX) {
            product.setSceneGeoCoding(createPixelGeoCoding(product, sceneRasterWidth, sceneRasterHeight, type.gp));
            if (type.gcOcc == Occurrence.M) {
                product.addBand("band_d", bandDExpr).setGeoCoding(createCrsGeoCoding(sceneRasterWidth, sceneRasterHeight, type.gp));
            }
        }
    }

    private static PixelGeoCoding createPixelGeoCoding(Product product, int sceneRasterWidth, int sceneRasterHeight, GP gp) {
        addLatLonBands(product, sceneRasterWidth, sceneRasterHeight, gp);
        return new PixelGeoCoding(product.getBand("latitude"),
                                  product.getBand("longitude"), null, 10);
    }

    private static TiePointGeoCoding createTiePointGeoCoding(Product product, int sceneRasterWidth, int sceneRasterHeight, int gridWidth, int gridHeight, GP gp) {
        addLatLonTiePointGrids(product, sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, gp);
        return new TiePointGeoCoding(product.getTiePointGrid("latitude"),
                                     product.getTiePointGrid("longitude"));
    }

    private static CrsGeoCoding createCrsGeoCoding(int imageWidth, int imageHeight, GP gp) {
        try {
            double extend = 10.0;
            double pixelSize = extend / imageWidth;
            // todo - (nf 20151113) use gp parameter to setup easting/northing correctly
            double easting = 0.0;
            double northing = 0.0;
            return new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                    imageWidth, imageHeight,
                                    easting, northing,
                                    pixelSize, pixelSize,
                                    0.0, 0.0);
        } catch (FactoryException | TransformException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addSingleSizeBand(Product product,
                                          int sceneRasterWidth,
                                          int sceneRasterHeight,
                                          String bandName,
                                          String bandExpr,
                                          float bandWavelength,
                                          String maskName,
                                          String maskExpr,
                                          Color maskColor) {
        Band band = product.addBand(bandName, String.format(bandExpr, sceneRasterWidth, sceneRasterHeight));
        band.setSpectralWavelength(bandWavelength);
        product.addMask(maskName, String.format(maskExpr, ""), String.format(maskExpr, ""), maskColor, 0.5);
    }

    private static void addMultiSizeBands(Product product,
                                          int width, int height,
                                          String bandName,
                                          String bandExpr,
                                          float bandWavelength,
                                          String maskName,
                                          String maskExpr,
                                          Color maskColor) {
        addMultiSizeBand(product, 1, width, height, bandName, bandExpr, bandWavelength, maskName, maskExpr, maskColor);
        addMultiSizeBand(product, 2, width, height, bandName, bandExpr, bandWavelength + 10, maskName, maskExpr, maskColor.brighter());
        addMultiSizeBand(product, 6, width, height, bandName, bandExpr, bandWavelength + 20, maskName, maskExpr, maskColor.darker());
    }

    private static void addMultiSizeBand(Product product,
                                         int divisor, int width, int height,
                                         String bandName,
                                         String bandExpr,
                                         float bandWavelength,
                                         String maskName,
                                         String maskExpr,
                                         Color maskColor) {
        VirtualBand band = new VirtualBand(bandName + "_" + divisor,
                                           ProductData.TYPE_FLOAT32,
                                           width / divisor,
                                           height / divisor,
                                           String.format(bandExpr,
                                                         width / divisor,
                                                         height / divisor));
        //band.setImageToModelTransform(AffineTransform.getScaleInstance(divisor, divisor));
        band.setSpectralWavelength(bandWavelength);
        product.addBand(band);
        maskName = maskName + "_" + divisor;
        maskExpr = String.format(maskExpr, "_" + divisor);
        product.addMask(Mask.BandMathsType.create(maskName,
                                                  maskExpr,
                                                  width / divisor,
                                                  height / divisor,
                                                  maskExpr,
                                                  maskColor,
                                                  0.5));
    }

    private static float[] createRandomPoints(int n) {
        Random random = new Random();
        float[] pnts = new float[n];
        for (int i = 0; i < pnts.length; i++) {
            pnts[i] = (float) random.nextGaussian();
        }
        return pnts;
    }

    private static void addRandomTiePointGrids(Product product,
                                               int sceneRasterWidth, int sceneRasterHeight,
                                               int gridWidth, int gridHeight) {
        double subSamplingX = sceneRasterWidth / (gridWidth - 1.0);
        double subSamplingY = sceneRasterHeight / (gridHeight - 1.0);
        product.addTiePointGrid(new TiePointGrid("tpgrid_a", gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, createRandomPoints(gridWidth * gridHeight)));
        product.addTiePointGrid(new TiePointGrid("tpgrid_b", gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, createRandomPoints(gridWidth * gridHeight)));
    }

    private static void addLatLonBands(Product product,
                                       int sceneRasterWidth, int sceneRasterHeight,
                                       GP GP) {

        float[] lonData = new float[sceneRasterWidth * sceneRasterHeight];
        float[] latData = new float[sceneRasterWidth * sceneRasterHeight];

        computeLonLat(GP, 0.2, sceneRasterWidth, sceneRasterHeight, lonData, latData);

        Band longitudeBand = new Band("longitude", ProductData.TYPE_FLOAT32, sceneRasterWidth, sceneRasterHeight);
        longitudeBand.setRasterData(ProductData.createInstance(lonData));
        product.addBand(longitudeBand);

        Band latitudeBand = new Band("latitude", ProductData.TYPE_FLOAT32, sceneRasterWidth, sceneRasterHeight);
        latitudeBand.setRasterData(ProductData.createInstance(latData));
        product.addBand(latitudeBand);
    }

    private static void addLatLonTiePointGrids(Product product,
                                               int sceneRasterWidth, int sceneRasterHeight,
                                               int gridWidth, int gridHeight, GP GP) {

        float[] lonData = new float[gridWidth * gridHeight];
        float[] latData = new float[gridWidth * gridHeight];

        computeLonLat(GP, 0.2, gridWidth, gridHeight, lonData, latData);

        double subSamplingX = sceneRasterWidth / (gridWidth - 1.0);
        double subSamplingY = sceneRasterHeight / (gridHeight - 1.0);

        product.addTiePointGrid(new TiePointGrid("longitude", gridWidth, gridHeight, 0.0, 0.0,
                                                 subSamplingX, subSamplingY, lonData));

        product.addTiePointGrid(new TiePointGrid("latitude", gridWidth, gridHeight, 0.0, 0.0,
                                                 subSamplingX, subSamplingY, latData));
    }

    /**
     * @param gp         Where?
     * @param extend     The extend in unit-sphere coordinates
     * @param gridWidth  Grid width
     * @param gridHeight Grid height
     * @param lonData    Longitude data array of size gridWidth x gridHeight
     * @param latData    Latitude data array of size gridWidth x gridHeight
     */
    private static void computeLonLat(GP gp,
                                      double extend,
                                      int gridWidth, int gridHeight,
                                      float[] lonData, float[] latData) {

        double sizeX = extend;
        double sizeY = (extend * gridHeight) / gridWidth;

        for (int j = 0; j < gridHeight; j++) {
            for (int i = 0; i < gridWidth; i++) {
                double x = 1.0;
                double y = sizeX * (i / (gridWidth - 1.0) - 0.5);
                double z = 0.5 * sizeY - sizeY * (j / (gridHeight - 1.0) - 0.5);
                double[] p0 = new double[]{x, y, z};
                double[] p;
                if (gp == GP.AMER) {
                    p = rotZ(Math.PI, p0);
                } else if (gp == GP.NPOL) {
                    p = rotY(+0.5 * Math.PI, p0);
                } else if (gp == GP.SPOL) {
                    p = rotY(+0.5 * Math.PI, p0);
                } else {
                    p = p0;
                }
                double r = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                x = p[0] / r;
                y = p[1] / r;
                z = p[2] / r;
                double r2 = Math.sqrt(x * x + y * y);
                double lon = 180 * Math.atan2(y, x) / Math.PI;
                double lat = 180 * Math.atan2(z, r2) / Math.PI;
                lonData[i * gridWidth * j] = (float) lon;
                latData[i * gridWidth * j] = (float) lat;
            }
        }
    }

    private static double[] rotX(double a, double[] in) {
        return rotX(a, in, new double[3]);
    }

    private static double[] rotY(double a, double[] in) {
        return rotY(a, in, new double[3]);
    }

    private static double[] rotZ(double a, double[] in) {
        return rotZ(a, in, new double[3]);
    }

    private static double[] rotX(double a, double[] in, double[] out) {
        double x = in[0];
        double y = in[1];
        double z = in[2];
        // y' = y*cos q - z*sin q
        // z' = y*sin q + z*cos q
        // x' = x
        out[0] = x;
        out[1] = y * Math.cos(a) - z * Math.sin(a);
        out[2] = y * Math.sin(a) + z * Math.cos(a);
        return out;
    }

    private static double[] rotY(double a, double[] in, double[] out) {
        double x = in[0];
        double y = in[1];
        double z = in[2];
        // z' = z*cos q - x*sin q
        // x' = z*sin q + x*cos q
        // y' = y
        out[1] = y;
        out[0] = z * Math.sin(a) + x * Math.cos(a);
        out[2] = z * Math.cos(a) - x * Math.sin(a);
        return out;
    }

    private static double[] rotZ(double a, double[] in, double[] out) {
        double x = in[0];
        double y = in[1];
        double z = in[2];
        // x' = x*cos q - y*sin q
        // y' = x*sin q + y*cos q
        // z' = z
        out[0] = x * Math.cos(a) - y * Math.sin(a);
        out[1] = x * Math.sin(a) + y * Math.cos(a);
        out[2] = z;
        return out;
    }
}
