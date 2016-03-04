package org.esa.snap.binning.support;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.binning.MosaickingGrid;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.common.reproject.ReprojectionOp;
import org.esa.snap.core.util.FeatureUtils;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marcoz on 29.02.16.
 */
public abstract class CrsGrid implements MosaickingGrid {

    private static final int TILE_SIZE = 250; // TODO

    private static final int LON_DIM = 0;
    private static final int LAT_DIM = 1;

    private final CoordinateReferenceSystem crs;
    private final int numRows;
    private final int numCols;
    private final double pixelSize;
    private final GeometryFactory geometryFactory;

    private final Envelope envelopeCRS;
    private final CrsGeoCoding crsGeoCoding;
    private final double easting;
    private final double northing;



//    public CrsGrid(int numRowsGlobal) {
//        this(numRowsGlobal, "EPSG:31467"); // DHDN : 3-degree Gauss-Kruger zone 3
//        this(numRowsGlobal, "EPSG:4326"); // WGS84
//        this(numRowsGlobal, "EPSG:3067"); // ETRS89 / ETRS-TM35FIN
//        this(numRowsGlobal, "EPSG:2192");  // France
//    }

    public CrsGrid(int numRowsGlobal, String crsCode) {
        try {
            // force longitude==x-axis and latitude==y-axis
            crs = CRS.decode(crsCode, true);
            envelopeCRS = CRS.getEnvelope(crs);
            System.out.println("envelopeCRS = " + envelopeCRS);

            String units = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            if (units.equalsIgnoreCase("m") || units.equalsIgnoreCase("meter")) {
                Ellipsoid ellipsoid = CRS.getEllipsoid(crs);
                double semiMinorAxis = ellipsoid.getSemiMinorAxis();
                double meterSpanGlobal = semiMinorAxis * Math.PI;
                pixelSize = meterSpanGlobal / numRowsGlobal;
            } else {
                pixelSize = 180.0 / numRowsGlobal;
            }
            System.out.println("pixelSize = " + pixelSize + " [" + units + "]");

            numCols = (int) (envelopeCRS.getSpan(LON_DIM) / pixelSize);
            easting = envelopeCRS.getMinimum(LON_DIM);
            numRows = (int) (envelopeCRS.getSpan(LAT_DIM) / pixelSize);
            northing = envelopeCRS.getMaximum(LAT_DIM);

            crsGeoCoding = new CrsGeoCoding(crs,
                                            numCols, numRows,
                                            easting, northing,
                                            pixelSize, pixelSize);
        } catch (TransformException | FactoryException e) {
            throw new IllegalArgumentException("Can not create crs for:" + crsCode, e);
        }
        this.geometryFactory = new GeometryFactory();
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        PixelPos pixelPos = crsGeoCoding.getPixelPos(new GeoPos(lat, lon), null);
        int x = (int) pixelPos.getX();
        int y = (int) pixelPos.getY();
        return y * numCols + x;
    }

    @Override
    public int getRowIndex(long bin) {
        long x = bin % numCols;
        int y = (int) ((bin - x) / numCols);
        return y;
    }

    @Override
    public long getNumBins() {
        return numCols * numRows;
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int row) {
        return numCols;
    }

    @Override
    public long getFirstBinIndex(int row) {
        return row * numCols;
    }

    @Override
    public double getCenterLat(int row) {
        GeoPos geoPos = crsGeoCoding.getGeoPos(new PixelPos(0.5, row + 0.5), null);
        return geoPos.getLat();
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        int x = (int) (bin % numCols);
        int y = (int) ((bin - x) / numCols);
        GeoPos geoPos = crsGeoCoding.getGeoPos(new PixelPos(x + 0.5, y + 0.5), null);

        return new double[]{geoPos.getLat(), geoPos.getLon()};
    }

    @Override
    public Product reprojectToGrid(Product sourceProduct) {
        Product gridProduct = new Product("ColocationGrid", "ColocationGrid", numCols, numRows);
        gridProduct.setSceneGeoCoding(crsGeoCoding);

        final ReprojectionOp repro = new ReprojectionOp();

        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", false);  // TODO change to true
        repro.setParameter("tileSizeX", TILE_SIZE);
        repro.setParameter("tileSizeY", TILE_SIZE);
        repro.setSourceProduct("collocateWith", gridProduct);
        repro.setSourceProduct("source", sourceProduct);
        Product targetProduct = repro.getTargetProduct();

        // reprojected products lack time information
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        return targetProduct;
    }

    public Geometry getImageGeometry(Geometry Geometry) {
        Product gridProduct = new Product("ColocationGrid", "ColocationGrid", numCols, numRows);
        gridProduct.setSceneGeoCoding(crsGeoCoding);
        RasterDataNode rdn = gridProduct.addBand("dummy", ProductData.TYPE_UINT8);


        SimpleFeatureType wktFeatureType = PlainFeatureFactory.createDefaultFeatureType(DefaultGeographicCRS.WGS84);
        ListFeatureCollection featureCollection = new ListFeatureCollection(wktFeatureType);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(wktFeatureType);
        SimpleFeature wktFeature = featureBuilder.buildFeature("ID1");
        wktFeature.setDefaultGeometry(Geometry);
        featureCollection.add(wktFeature);

        FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(
                featureCollection,
                gridProduct,
                null,
                ProgressMonitor.NULL);
        FeatureIterator<SimpleFeature> features = productFeatures.features();
        if (!features.hasNext()) {
            return null;
        }
        SimpleFeature simpleFeature = features.next();
        Geometry clippedGeometry = (Geometry) simpleFeature.getDefaultGeometry();

        Geometry pixelGeometry;
        try {
            AffineTransform i2mTransform = rdn.getImageToModelTransform();
            i2mTransform.invert();
            AffineTransform m2iTransform = i2mTransform;
            GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(new AffineTransform2D(m2iTransform));
            pixelGeometry = transformer.transform(clippedGeometry);
        } catch (NoninvertibleTransformException | TransformException e) {
            throw new IllegalArgumentException("Could not invert model-to-image transformation.", e);
        }
        return pixelGeometry;
    }

    public Rectangle getBounds(Geometry pixelGeometry) {
        com.vividsolutions.jts.geom.Envelope envelopeInternal = pixelGeometry.getEnvelopeInternal();
        int minX = (int) Math.floor(envelopeInternal.getMinX());
        int minY = (int) Math.floor(envelopeInternal.getMinY());
        int maxX = (int) Math.ceil(envelopeInternal.getMaxX());
        int maxY = (int) Math.ceil(envelopeInternal.getMaxY());

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public GeoCoding getGeoCoding(Rectangle outputRegion) {
        try {
            return new CrsGeoCoding(crs,
                                    outputRegion.width,
                                    outputRegion.height,
                                    easting + pixelSize * outputRegion.x,
                                    northing - pixelSize * outputRegion.y,
                                    pixelSize, pixelSize);
        } catch (FactoryException | TransformException e) {
            throw new IllegalArgumentException("Can not create geocoding for crs.", e);
        }
    }

    @Override
    public Rectangle[] getDataSliceRectangles(Geometry sourceProductGeometry, Dimension tileSize) {
        System.out.println("sourceProductGeometry = " + sourceProductGeometry);

        Geometry imageGeometry = getImageGeometry(sourceProductGeometry);
        if (imageGeometry == null) {
            return new Rectangle[0];
        }
        Rectangle productBoundingBox = getBounds(imageGeometry);

        System.out.println("imageGeometry = " + imageGeometry);
        System.out.println("productBoundingBox = " + productBoundingBox);

        Rectangle gridAlignedBoundingBox = alignToTileGrid(productBoundingBox, tileSize);
        System.out.println("gridAlignedBoundingBox = " + gridAlignedBoundingBox);
        final int xStart = gridAlignedBoundingBox.x / tileSize.width;
        final int yStart = gridAlignedBoundingBox.y / tileSize.height;
        final int width = gridAlignedBoundingBox.width / tileSize.width;
        final int height = gridAlignedBoundingBox.height / tileSize.height;

        List<Rectangle> rectangles = new ArrayList<>((numCols * numRows) / (tileSize.width * tileSize.height));
        for (int y = yStart; y < yStart + height; y++) {
            for (int x = xStart; x < xStart + width; x++) {
                Rectangle tileRect = new Rectangle(x * tileSize.width, y * tileSize.height, tileSize.width, tileSize.height);
                Geometry tileGeometry = getTileGeometry(tileRect);
                System.out.println("tileGeometry = " + tileGeometry);

                Geometry intersection = imageGeometry.intersection(tileGeometry);
                System.out.println("intersection = " + intersection);

                if (!intersection.isEmpty() && intersection.getDimension() == 2) {
                    System.out.println("tileRect = " + tileRect);
                    rectangles.add(productBoundingBox.intersection(tileRect));
                }
            }
        }
        System.out.println("rectangles = " + rectangles.size());
        return rectangles.toArray(new Rectangle[rectangles.size()]);
    }

    static Rectangle alignToTileGrid(Rectangle rectangle, Dimension tileSize) {
        int minX = rectangle.x / tileSize.width * tileSize.width;
        int maxX = (rectangle.x + rectangle.width + tileSize.width - 1) / tileSize.width * tileSize.width;
        int minY = (rectangle.y / tileSize.height) * tileSize.height;
        int maxY = (rectangle.y + rectangle.height + tileSize.height - 1) / tileSize.height * tileSize.height;

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Geometry getTileGeometry(Rectangle rect) {
        return geometryFactory.toGeometry(new com.vividsolutions.jts.geom.Envelope(
                rect.x, rect.x + rect.width, rect.y, rect.y + rect.height
        ));
    }
}
