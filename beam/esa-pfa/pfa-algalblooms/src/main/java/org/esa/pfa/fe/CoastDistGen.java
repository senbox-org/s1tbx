package org.esa.pfa.fe;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.util.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.System.currentTimeMillis;

/**
 * <p>Usage: {@code CoastDistGen <shapefilePath> <productPath> <width> <height> <lonMin> <lonMax> <latMin> <meridionalDist>}
 * where {@code shapefilePath} is the path to a directory containing at least one ESRI shapefile,
 * {@code shapefilePath} specifies the output product file,
 * {@code width} and {@code height} in pixels,
 * all other numerical parameters in degrees.
 * </p>
 * <p>Example1: {@code CoastDistGen  src/test/resources/shapefiles/continents ./coast_dist_2048.dim  2048 1024 -15 30 47.5 5.0}  </p>
 * <p>Example2: {@code CoastDistGen  src/test/resources/shapefiles/protected_areas ./ballastwater_protected_areas_2048.dim 2048 1024 -15 30 47.5 20.0} </p>
 * <p>
 * Changes from 1.3 to 1.4:<br/>
 * <ol>
 * <li>Pixels that lie outside the range reachable by the 'meridionalDist' are now set top the absolute maximum distance computed from all pixels</li>
 * <li>Corrected computation of 'periodic' flag important for full Earth products and 180-deg meridian crossing</li>
 * <li>Corrected computation of the 'longituidal distance' for a given 'meridional distance' (old returned NaN at extreme latitudes)</li>
 * </ol>
 * </p>
 */
public class CoastDistGen {

    private static final String PROCESSOR_NAME = CoastDistGen.class.getName();
    private static final String PROCESSOR_VERSION = "1.4";

    private final static double MEAN_EARTH_RADIUS = 6370.997;

    private final double meridionalDist;
    private final int width;
    private final int height;
    private final double easting;
    private final double northing;
    private final double pixelSize;
    private final float[] distData;
    private final byte[] maskData;
    private final double[] longituidalDists;
    private final boolean periodic;
    private final int numThreads;
    private final int blockSize;


    public static void main(String[] args) {
        if (args.length != 8) {
            System.err.println("Usage: <shapefilesPath> <productPath> <width> <height> <lonMin> <lonMax> <latMin> <meridionalDist>");
            System.exit(1);
        }

        String shapefilesPath = args[0];
        String productPath = args[1];
        int width = Integer.parseInt(args[2]);
        int height = Integer.parseInt(args[3]);
        double lonMin = Double.parseDouble(args[4]);
        double lonMax = Double.parseDouble(args[5]);
        double latMin = Double.parseDouble(args[6]);
        double meridionalDist = Double.parseDouble(args[7]);

        double scale = (lonMax - lonMin) / width;
        double latMax = latMin + scale * height;
        try {
            new CoastDistGen(shapefilesPath, productPath,
                             width, height,
                             lonMin, latMax, scale,
                             meridionalDist);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CoastDistGen(String shapefilesPath, String productPath,
                        int width, int height,
                        double easting, double northing, double pixelSize,
                        double meridionalDist) throws Exception {

        System.out.println("Rendering land mask from shapefiles in " + shapefilesPath + "...");
        final long t0 = currentTimeMillis();

        File[] shapefiles = new File(shapefilesPath).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".shp") || name.endsWith(".SHP");
            }
        });
        if (shapefiles == null || shapefiles.length == 0) {
            System.err.println(MessageFormat.format("Error: No shapefiles found in {0}.", shapefilesPath));
            System.exit(1);
        }
        BufferedImage landMaskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = landMaskImage.createGraphics();

        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        MapContext context = new DefaultMapContext(crs);

        for (File shapefile : shapefiles) {
            System.out.println(MessageFormat.format("Loading shapefile {0}...", shapefile));
            URL url = shapefile.toURI().toURL();
            context.addLayer(getFeatureSource(url), createPolygonStyle());
        }

        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setContext(context);
        renderer.paint(graphics,
                       new Rectangle(0, 0, width, height),
                       new ReferencedEnvelope(easting,
                                              easting + width * pixelSize,
                                              northing - height * pixelSize,
                                              northing, crs));

        final long t1 = currentTimeMillis();
        System.out.println("Rendering done in " + (t1 - t0) / 1000.0 + " s");

        File pngFile = FileUtils.exchangeExtension(new File(productPath), ".png");
        System.out.println("Writing " + pngFile + "...");
        ImageIO.write(landMaskImage, "PNG", pngFile);

        Product product = new Product("ns_bs_coast_distance", "ns_bs_coast_distance", width, height);

        AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(pixelSize, -pixelSize);
        product.setGeoCoding(new CrsGeoCoding(crs, new Rectangle(0, 0, width, height), transform));

        Band landMaskBand = product.addBand("land_mask", ProductData.TYPE_INT8);
        landMaskBand.setSourceImage(landMaskImage);
        showBand(landMaskBand);

        Band coastDistBand = product.addBand("coast_dist", ProductData.TYPE_FLOAT32);
        coastDistBand.setUnit("km");
        coastDistBand.setDescription("Distance to next coast pixel (where land_mask != 0)");
        coastDistBand.setNoDataValue(0.0);
        coastDistBand.setNoDataValueUsed(true);

        product.addBand(new VirtualBand("coast_dist_nm", ProductData.TYPE_FLOAT32, width, height, "coast_dist / 1.852"));
        product.getBand("coast_dist_nm").setUnit("NM");
        product.getBand("coast_dist_nm").setDescription(coastDistBand.getDescription());
        product.getBand("coast_dist_nm").setNoDataValue(0.0);
        product.getBand("coast_dist_nm").setNoDataValueUsed(true);

        product.addBand(new VirtualBand("coast_dist_nm_gt_50", ProductData.TYPE_INT8, width, height, "coast_dist_nm >= 50"));
        product.getBand("coast_dist_nm_gt_50").setValidPixelExpression("coast_dist > 0");

        product.addBand(new VirtualBand("coast_dist_nm_gt_200", ProductData.TYPE_INT8, width, height, "coast_dist_nm >= 200"));
        product.getBand("coast_dist_nm_gt_200").setValidPixelExpression("coast_dist > 0");
        coastDistBand.ensureRasterData();

        this.width = width;
        this.height = height;
        this.easting = toRadians(easting);
        this.northing = toRadians(northing);
        this.pixelSize = toRadians(pixelSize);
        this.periodic = abs((this.pixelSize * width) - (2.0 * PI)) < 1e-10;
        this.meridionalDist = toRadians(meridionalDist);
        this.longituidalDists = new double[height];
        for (int y = 0; y < height; y++) {
            double latitude = this.northing - this.pixelSize * (y + 0.5);
            longituidalDists[y] = getLongituidalDistance(this.meridionalDist, latitude);
        }
        this.maskData = ((DataBufferByte) landMaskImage.getRaster().getDataBuffer()).getData();
        this.distData = (float[]) coastDistBand.getRasterData().getElems();
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.blockSize = height < numThreads ? 1 : (height / numThreads);
        dumpParameters();

        long time = computeCoastDistances(product);

        // Add some history metadata
        //
        product.setDescription("Provides distance of water pixels to closest coast line.");
        MetadataElement history = new MetadataElement("History");
        history.setAttributeString("Processor_Name", PROCESSOR_NAME);
        history.setAttributeString("Processor_Version", PROCESSOR_VERSION);
        history.setAttributeString("Processing_Date", new SimpleDateFormat().format(new Date()));
        history.setAttributeDouble("Processing_Time", time / 1000.0);
        history.getAttribute("Processing_Time").setUnit("s");
        history.getAttribute("Processing_Time").setDescription("Total time spend for processing.");
        MetadataElement parameters = new MetadataElement("Processing_Parameters");
        history.addElement(parameters);
        parameters.setAttributeString("shapefilesPath", shapefilesPath);
        parameters.setAttributeString("productPath", productPath);
        parameters.setAttributeInt("width", width);
        parameters.setAttributeInt("height", height);
        parameters.setAttributeDouble("lonMin", easting);
        parameters.setAttributeDouble("lonMax", easting + pixelSize * width);
        parameters.setAttributeDouble("latMin", northing - pixelSize * height);
        parameters.setAttributeDouble("latMax", northing);
        parameters.setAttributeDouble("meridionalDist", meridionalDist);
        product.getMetadataRoot().addElement(history);
        MetadataElement inputFiles = new MetadataElement("Input_Files");
        history.addElement(inputFiles);
        for (int i = 0; i < shapefiles.length; i++) {
            inputFiles.setAttributeString("shapefiles." + i, shapefiles[i].getName());
        }


        System.out.println("Writing " + productPath + "...");
        ProductIO.writeProduct(product, new File(productPath), "BEAM-DIMAP", false);
        showBand(coastDistBand);
    }

    private long computeCoastDistances(Product product) {
        try {
            System.out.println("Computing coast distances...");
            final ExecutorService executorService = Executors.newCachedThreadPool();
            final long t0 = currentTimeMillis();
            int y1, y2 = -1;
            for (int i = 0; i < numThreads; i++) {
                y1 = y2 + 1;
                y2 = y1 + blockSize - 1;
                if (y2 >= height) {
                    y2 = height - y1 - 1;
                }
                executorService.submit(new BlockTask(y1, y2));
            }
            executorService.shutdown();

            final boolean b = executorService.awaitTermination(10, TimeUnit.DAYS);
            final long t1 = currentTimeMillis();
            long time = t1 - t0;
            System.out.println("Coast distances computation done in " + (time / 1000.0) + " s");

            if (!b) {
                System.err.println("Error: Coast distances computation timed out.");
                System.exit(1);
            }

            System.out.println("Filling in maximum value...");
            float fillValue = fillInUnreachedPixels(this.distData);
            product.addMask("unreached",
                            "coast_dist >= " + fillValue,
                            "Pixels unreached by the maximum coast distance of " + Math.round(meridionalDist * MEAN_EARTH_RADIUS) + " km",
                            Color.ORANGE, 0.0);
            System.out.println("Done.");

            return time;
        } catch (InterruptedException e) {
            System.err.println("Error: Coast distances computation interrupted.");
            System.exit(1);
            return -1;
        }
    }

    private static float fillInUnreachedPixels(float[] distData) {
        float max = 0;
        for (float v : distData) {
            max = Math.max(max, v);
        }
        float fillValue = Math.round(max + 1.5);
        for (int i = 0; i < distData.length; i++) {
            float v = distData[i];
            if (v < 0) {
                distData[i] = fillValue;
            }
        }
        return fillValue;
    }


    private void dumpParameters() {
        System.out.println("Parameters:");
        System.out.println("  width = " + width);
        System.out.println("  height = " + height);
        System.out.println("  easting = " + toDegrees(easting) + " ... " + toDegrees(easting + pixelSize * width));
        System.out.println("  northing = " + toDegrees(northing - pixelSize * height) + " ... " + toDegrees(northing));
        System.out.println("  pixelSize = " + toDegrees(pixelSize));
        System.out.println("  periodic = " + periodic);
        System.out.println("  meridionalDist = " + toDegrees(meridionalDist));
        for (int y = 0; y < height; y++) {
            System.out.printf("  meridionalDist[%d] = %s (latitude = %s)%n",
                              y, toDegrees(longituidalDists[y]), toDegrees(northing - pixelSize * y));
        }
        System.out.println("  numThreads = " + numThreads);
        System.out.println("  blockSize = " + blockSize);
    }

    class BlockTask implements Callable<Void> {
        final int y1;
        final int y2;
        final String name;

        BlockTask(int y1, int y2) {
            this.name = String.format("BlockTask %s-%s", y1, y2);
            this.y1 = y1;
            this.y2 = y2;
        }

        public Void call() throws Exception {
            System.out.println(name + " started");

            for (int y = y1; y <= y2; y++) {
                final long t0 = currentTimeMillis();
                for (int x = 0; x < width; x++) {
                    final double distRad = computeDistance(x, y,
                                                           width, height,
                                                           easting, northing, pixelSize,
                                                           periodic,
                                                           meridionalDist, longituidalDists,
                                                           maskData);

                    distData[y * width + x] = (float) (MEAN_EARTH_RADIUS * distRad);
                }
                final long t1 = currentTimeMillis();
                int i = y - y1 + 1;
                int n = y2 - y1 + 1;
                float r;
                r = Math.round((10f * 100f * i) / n) / 10f;
                System.out.println(String.format("%s: computed line %s of %s in %s ms (%s)", name, i, n, t1 - t0, r));
            }

            System.out.println(MessageFormat.format("{0} finished", name));
            return null;
        }
    }

    private static double computeDistance(int x, int y,
                                          int width, int height,
                                          double easting, double northing, double pixelSize,
                                          boolean periodic,
                                          double meridionalDist, double[] longituidalDists,
                                          byte[] maskData) {

        if (maskData[y * width + x] != 0) {
            return 0;
        }

        final double lam1 = easting + pixelSize * (x + 0.5);
        final double phi1 = northing - pixelSize * (y + 0.5);
        final int ndy = getNumDeltaPixels(meridionalDist, pixelSize, height);

        double minDist = Double.MAX_VALUE;
        for (int kernelY = y - ndy; kernelY <= y + ndy; kernelY++) {
            if (kernelY >= 0 && kernelY < height) {
                final double phi2 = northing - pixelSize * (kernelY + 0.5);
                final double longituidalDist = longituidalDists[kernelY];
                final int ndx = getNumDeltaPixels(longituidalDist, pixelSize, width);
                for (int kernelX = x - ndx; kernelX <= x + ndx; kernelX++) {
                    final int kernelXLim = periodic ? getPeriodicX(kernelX, width) : getClampedX(kernelX, width);
                    if (maskData[kernelY * width + kernelXLim] != 0) {
                        final double lam2 = easting + pixelSize * (kernelXLim + 0.5);
                        final double dist = getSphericDistance(lam1, phi1, lam2, phi2);
                        if (dist < minDist) {
                            minDist = dist;
                        }
                    }
                }
            }
        }

        return minDist == Double.MAX_VALUE ? -1.0 : minDist;
    }

    static int getNumDeltaPixels(double distance, double pixelSize, int numPixelsMax) {
        int numDeltaPixels = (int) (distance / pixelSize + 0.5);
        if (numDeltaPixels > numPixelsMax) {
            numDeltaPixels = numPixelsMax;
        }
        return numDeltaPixels;
    }

    static int getPeriodicX(int x, int width) {
        if (x < 0) {
            return width + x;
        } else if (x >= width) {
            return x - width;
        } else {
            return x;
        }
    }

    static int getClampedX(int x, int width) {
        if (x < 0) {
            return 0;
        } else if (x >= width) {
            return width - 1;
        } else {
            return x;
        }
    }

    /*
     * Wie lang ist eine meridionale bzw. equatoriale Strecke auf einem bestimmten Breitenkreis?
     */

    static double getLongituidalDistance(double meridionalDistance, double latitude) {
        if (meridionalDistance < 0 || meridionalDistance > Math.PI / 2) {
            throw new IllegalArgumentException("meridionalDistance < 0 || meridionalDistance > Math.PI / 2");
        }
        if (latitude < -Math.PI / 2 || latitude > Math.PI / 2) {
            throw new IllegalArgumentException("latitude < -Math.PI / 2 || latitude > Math.PI / 2");
        }

        if (meridionalDistance == 0.0) {
            return 0.0;
        }
        double cosLat = cos(latitude);
        if (cosLat > 0.0) {
            double longituidalDistance = meridionalDistance / cosLat;
            if (longituidalDistance < 2 * Math.PI) {
                return longituidalDistance;
            }
        }
        return 2 * Math.PI;
    }


    /*
     * Wie lang ist die Orthodrome zwischen zwei Punkten?
     */

    static double getSphericDistance(final double lam1, final double phi1,
                                     final double lam2, final double phi2) {
        return acos(sin(phi1) * sin(phi2) + cos(phi1) * cos(phi2) * cos(lam1 - lam2));
    }


    public static void showBand(Band band) {
        String title = band.getProduct().getName() + " - " + band.getName();
        System.out.println("Showing " + title + "...");

        LayerCanvas layerCanvas = new LayerCanvas(new ImageLayer(BandImageMultiLevelSource.create(band, ProgressMonitor.NULL)));
        layerCanvas.setPreferredSize(new Dimension(band.getSceneRasterWidth(), band.getSceneRasterHeight()));
        layerCanvas.setInitiallyZoomingAll(true);

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.add(layerCanvas);
        frame.pack();
        frame.setVisible(true);
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(URL url) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ShapefileDataStoreFactory.URLP.key, url);
        map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(map);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = shapefileStore.getFeatureSource(typeName);
        return featureSource;
    }

    private static Style createPolygonStyle() {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

        PolygonSymbolizer symbolizer = styleFactory.createPolygonSymbolizer();
        org.geotools.styling.Stroke stroke = styleFactory.createStroke(
                filterFactory.literal("#FFFFFF"),
                filterFactory.literal(0.0)
        );
        symbolizer.setStroke(stroke);
        Fill fill = styleFactory.createFill(
                filterFactory.literal("#FFFFFF"),
                filterFactory.literal(1.0)
        );
        symbolizer.setFill(fill);

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);

        return style;
    }

}
