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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marcoz on 29.02.16.
 */
public class CrsGrid implements MosaickingGrid {

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

    public CrsGrid(int numRowsGlobal, String crsCode) {
        try {
            this.crs = CRS.decode(crsCode, true);
            this.envelopeCRS = CRS.getEnvelope(this.crs);
            System.out.println("envelopeCRS = " + this.envelopeCRS);
            String units = this.crs.getCoordinateSystem().getAxis(LON_DIM).getUnit().toString();
            if (!units.equalsIgnoreCase("m") && !units.equalsIgnoreCase("meter")) {
                this.pixelSize = 180.0D / (double)numRowsGlobal;
            } else {
                Ellipsoid ellipsoid = CRS.getEllipsoid(this.crs);
                double semiMinorAxis = ellipsoid.getSemiMinorAxis();
                double meterSpanGlobal = semiMinorAxis * Math.PI;
                this.pixelSize = meterSpanGlobal / (double)numRowsGlobal;
            }

            System.out.println("pixelSize = " + this.pixelSize + " [" + units + "]");
            this.numCols = (int)(this.envelopeCRS.getSpan(LON_DIM) / this.pixelSize);
            this.easting = this.envelopeCRS.getMinimum(LON_DIM);
            this.numRows = (int)(this.envelopeCRS.getSpan(LAT_DIM) / this.pixelSize);
            this.northing = this.envelopeCRS.getMaximum(LAT_DIM);
            this.crsGeoCoding = new CrsGeoCoding(this.crs, this.numCols, this.numRows, this.easting, this.northing, this.pixelSize, this.pixelSize);
        } catch (FactoryException | TransformException var9) {
            throw new IllegalArgumentException("Can not create crs for:" + crsCode, var9);
        }

        this.geometryFactory = new GeometryFactory();
    }

    public long getBinIndex(double lat, double lon) {
        PixelPos pixelPos = this.crsGeoCoding.getPixelPos(new GeoPos(lat, lon), null);
        long x = (long)pixelPos.getX();
        long y = (long)pixelPos.getY();
        return y * this.numCols + x;
    }

    public int getRowIndex(long bin) {
        long x = bin % this.numCols;
        int y = (int)((bin - x) / this.numCols);
        return y;
    }

    public long getNumBins() {
        return (long)this.numCols * (long)this.numRows;
    }

    public int getNumRows() {
        return this.numRows;
    }

    public int getNumCols(int row) {
        return this.numCols;
    }

    public long getFirstBinIndex(int row) {
        return (long)row * this.numCols;
    }

    public double getCenterLat(int row) {
        GeoPos geoPos = this.crsGeoCoding.getGeoPos(new PixelPos(0.5D, row + 0.5D), null);
        return geoPos.getLat();
    }

    public double[] getCenterLatLon(long bin) {
        int x = (int)(bin % this.numCols);
        int y = (int)((bin - x) / this.numCols);
        GeoPos geoPos = this.crsGeoCoding.getGeoPos(new PixelPos(x + 0.5D, y + 0.5D), null);
        return new double[]{geoPos.getLat(), geoPos.getLon()};
    }

    public Product reprojectToGrid(Product sourceProduct) {
        Product gridProduct = new Product("ColocationGrid", "ColocationGrid", this.numCols, this.numRows);
        gridProduct.setSceneGeoCoding(this.crsGeoCoding);
        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", false);
        repro.setParameter("tileSizeX", 250);
        repro.setParameter("tileSizeY", 250);
        repro.setSourceProduct("collocateWith", gridProduct);
        repro.setSourceProduct("source", sourceProduct);
        Product targetProduct = repro.getTargetProduct();
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        return targetProduct;
    }

    public Geometry getImageGeometry(Geometry Geometry) {
        Product gridProduct = new Product("ColocationGrid", "ColocationGrid", this.numCols, this.numRows);
        gridProduct.setSceneGeoCoding(this.crsGeoCoding);
        RasterDataNode rdn = gridProduct.addBand("dummy", ProductData.TYPE_UINT8);
        SimpleFeatureType wktFeatureType = PlainFeatureFactory.createDefaultFeatureType(DefaultGeographicCRS.WGS84);
        ListFeatureCollection featureCollection = new ListFeatureCollection(wktFeatureType);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(wktFeatureType);
        SimpleFeature wktFeature = featureBuilder.buildFeature("ID1");
        wktFeature.setDefaultGeometry(Geometry);
        featureCollection.add(wktFeature);
        FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(featureCollection, gridProduct,
                                                                                                                                null, ProgressMonitor.NULL);
        FeatureIterator<SimpleFeature> features = productFeatures.features();
        if (!features.hasNext()) {
            return null;
        } else {
            SimpleFeature simpleFeature = features.next();
            Geometry clippedGeometry = (Geometry)simpleFeature.getDefaultGeometry();

            try {
                AffineTransform i2mTransform = rdn.getImageToModelTransform();
                i2mTransform.invert();
                GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
                transformer.setMathTransform(new AffineTransform2D(i2mTransform));
                return transformer.transform(clippedGeometry);
            } catch (TransformException | NoninvertibleTransformException e) {
                throw new IllegalArgumentException("Could not invert model-to-image transformation.", e);
            }
        }
    }

    public Rectangle getBounds(Geometry pixelGeometry) {
        com.vividsolutions.jts.geom.Envelope envelopeInternal = pixelGeometry.getEnvelopeInternal();
        int minX = (int)Math.floor(envelopeInternal.getMinX());
        int minY = (int)Math.floor(envelopeInternal.getMinY());
        int maxX = (int)Math.ceil(envelopeInternal.getMaxX());
        int maxY = (int)Math.ceil(envelopeInternal.getMaxY());
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public GeoCoding getGeoCoding(Rectangle outputRegion) {
        try {
            return new CrsGeoCoding(this.crs, outputRegion.width, outputRegion.height, this.easting + this.pixelSize * outputRegion.x, this.northing - this.pixelSize * outputRegion.y, this.pixelSize, this.pixelSize);
        } catch (TransformException | FactoryException e) {
            throw new IllegalArgumentException("Can not create geocoding for crs.", e);
        }
    }

    public Rectangle[] getDataSliceRectangles(Geometry sourceProductGeometry, Dimension tileSize) {
        Geometry imageGeometry = this.getImageGeometry(sourceProductGeometry);
        if (imageGeometry == null) {
            return new Rectangle[0];
        } else {
            Rectangle productBoundingBox = this.getBounds(imageGeometry);
            Rectangle gridAlignedBoundingBox = alignToTileGrid(productBoundingBox, tileSize);
            int xStart = gridAlignedBoundingBox.x / tileSize.width;
            int yStart = gridAlignedBoundingBox.y / tileSize.height;
            int width = gridAlignedBoundingBox.width / tileSize.width;
            int height = gridAlignedBoundingBox.height / tileSize.height;
            List<Rectangle> rectangles = new ArrayList((int)((long)this.numCols * (long)this.numRows / (tileSize.width * tileSize.height)));

            for(int y = yStart; y < yStart + height; ++y) {
                for(int x = xStart; x < xStart + width; ++x) {
                    Rectangle tileRect = new Rectangle(x * tileSize.width, y * tileSize.height, tileSize.width, tileSize.height);
                    Geometry tileGeometry = this.getTileGeometry(tileRect);
                    Geometry intersection = imageGeometry.intersection(tileGeometry);
                    if (!intersection.isEmpty() && intersection.getDimension() == 2) {
                        System.out.println("tileRect = " + tileRect);
                        rectangles.add(productBoundingBox.intersection(tileRect));
                    }
                }
            }

            System.out.println("rectangles = " + rectangles.size());
            return rectangles.toArray(new Rectangle[0]);
        }
    }

    static Rectangle alignToTileGrid(Rectangle rectangle, Dimension tileSize) {
        int minX = rectangle.x / tileSize.width * tileSize.width;
        int maxX = (rectangle.x + rectangle.width + tileSize.width - 1) / tileSize.width * tileSize.width;
        int minY = rectangle.y / tileSize.height * tileSize.height;
        int maxY = (rectangle.y + rectangle.height + tileSize.height - 1) / tileSize.height * tileSize.height;
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Geometry getTileGeometry(Rectangle rect) {
        return this.geometryFactory.toGeometry(new com.vividsolutions.jts.geom.Envelope(rect.x, (rect.x + rect.width), rect.y, (rect.y + rect.height)));
    }
}
