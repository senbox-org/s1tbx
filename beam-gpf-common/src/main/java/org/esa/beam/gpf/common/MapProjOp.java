package org.esa.beam.gpf.common;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.factory.Hints;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import org.geotools.util.NumberRange;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@OperatorMetadata(alias = "Mapproj",
                  internal = false)
public class MapProjOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private String projectionName = "Geographic Lat/Lon";

    @Override
    public void initialize() throws OperatorException {
        try {
            /*
             * 1. Create the target CRS
             */
            final CoordinateReferenceSystem targetCRS = createTargetCRS();

            /*
             * 2. Compute the target grid geometry
             */
            final GridGeometry2D gridGeometry = createGridGeometry(sourceProduct, targetCRS);
            Rectangle gridRect = gridGeometry.getGridRange2D();

            /*
             * 3. Create the target product
             */
            targetProduct = new Product("projected_" + sourceProduct.getName(),
                                        "projection of: " + sourceProduct.getDescription(),
                                        gridRect.width,
                                        gridRect.height);

            /*
             * 4. Define some target properties
             */
            // TODO: also query operatorContext rendering hints for tile size
            final Dimension tileSize = new Dimension(128, 128);
            targetProduct.setPreferredTileSize(tileSize);
            addMetadataToProduct(targetProduct);
            addFlagCodingsToProduct(targetProduct);
            addIndexCodingsToProduct(targetProduct);

            final Envelope2D sourceEnvelope = new Envelope2D(sourceProduct.getGeoCoding().getImageCRS(),
                                                             0, 0,
                                                             sourceProduct.getSceneRasterWidth(),
                                                             sourceProduct.getSceneRasterHeight());
            /*
             * 5. Create target bands
             */
            for (Band sourceBand : sourceProduct.getBands()) {
                Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);

                /*
                 * 7. Create coverage from source band
                 */
                GridCoverage2D sourceCoverage = createSourceCoverage(sourceEnvelope, sourceBand);
                // only the tile size of the image layout is actually taken into account
                // by the resample operation.   Use Operations.DEFAULT if tile size does
                // not matter
                final Operations operations = new Operations(
                        new Hints(JAI.KEY_IMAGE_LAYOUT, createImageLayout(targetBand, tileSize)));

                /*
                 * 8. Create coverage for target band and set target image
                 */
                GridCoverage2D targetCoverage = (GridCoverage2D) operations.resample(sourceCoverage,
                                                                                     targetCRS,
                                                                                     gridGeometry,
                                                                                     getInterpolation());
                RenderedImage targetImage = targetCoverage.getRenderedImage();
                targetBand.setSourceImage(targetImage);

                /*
                 * Flag and index codings
                 */
                FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
                IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
                if (sourceFlagCoding != null) {
                    String flagCodingName = sourceFlagCoding.getName();
                    FlagCoding destFlagCoding = targetProduct.getFlagCodingGroup().get(flagCodingName);
                    targetBand.setSampleCoding(destFlagCoding);
                } else if (sourceIndexCoding != null) {
                    String indexCodingName = sourceIndexCoding.getName();
                    IndexCoding destIndexCoding = targetProduct.getIndexCodingGroup().get(indexCodingName);
                    targetBand.setSampleCoding(destIndexCoding);
                }
            }

            /*
             * Bitmask definitions and placemarks
             */
            ProductUtils.copyBitmaskDefsAndOverlays(sourceProduct, targetProduct);
            copyPlacemarks(sourceProduct.getPinGroup(), targetProduct.getPinGroup(),
                           PlacemarkSymbol.createDefaultPinSymbol());
            copyPlacemarks(sourceProduct.getGcpGroup(), targetProduct.getGcpGroup(),
                           PlacemarkSymbol.createDefaultGcpSymbol());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new OperatorException(t.getMessage(), t);
        }
    }

    protected void addFlagCodingsToProduct(Product product) {
        final ProductNodeGroup<FlagCoding> flagCodingGroup = sourceProduct.getFlagCodingGroup();
        for (int i = 0; i < flagCodingGroup.getNodeCount(); i++) {
            FlagCoding sourceFlagCoding = flagCodingGroup.get(i);
            FlagCoding destFlagCoding = new FlagCoding(sourceFlagCoding.getName());
            destFlagCoding.setDescription(sourceFlagCoding.getDescription());
            cloneFlags(sourceFlagCoding, destFlagCoding);
            product.getFlagCodingGroup().add(destFlagCoding);
        }
    }

    protected void addIndexCodingsToProduct(Product product) {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = sourceProduct.getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            IndexCoding destIndexCoding = new IndexCoding(sourceIndexCoding.getName());
            destIndexCoding.setDescription(sourceIndexCoding.getDescription());
            cloneIndexes(sourceIndexCoding, destIndexCoding);
            product.getIndexCodingGroup().add(destIndexCoding);
        }
    }

    protected void addMetadataToProduct(Product product) {
        cloneMetadataElementsAndAttributes(sourceProduct.getMetadataRoot(), product.getMetadataRoot(), 0);
    }

    protected void cloneFlags(FlagCoding sourceFlagCoding, FlagCoding destFlagCoding) {
        cloneMetadataElementsAndAttributes(sourceFlagCoding, destFlagCoding, 1);
    }

    protected void cloneIndexes(IndexCoding sourceFlagCoding, IndexCoding destFlagCoding) {
        cloneMetadataElementsAndAttributes(sourceFlagCoding, destFlagCoding, 1);
    }

    protected void cloneMetadataElementsAndAttributes(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        cloneMetadataElements(sourceRoot, destRoot, level);
        cloneMetadataAttributes(sourceRoot, destRoot);
    }

    protected void cloneMetadataElements(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        for (int i = 0; i < sourceRoot.getNumElements(); i++) {
            MetadataElement sourceElement = sourceRoot.getElementAt(i);
            if (level > 0) {
                MetadataElement element = new MetadataElement(sourceElement.getName());
                element.setDescription(sourceElement.getDescription());
                destRoot.addElement(element);
                cloneMetadataElementsAndAttributes(sourceElement, element, level + 1);
            }
        }
    }

    protected void cloneMetadataAttributes(MetadataElement sourceRoot, MetadataElement destRoot) {
        for (int i = 0; i < sourceRoot.getNumAttributes(); i++) {
            MetadataAttribute sourceAttribute = sourceRoot.getAttributeAt(i);
            destRoot.addAttribute(sourceAttribute.createDeepClone());
        }
    }

    private static void copyPlacemarks(ProductNodeGroup<Pin> sourcePlacemarkGroup,
                                       ProductNodeGroup<Pin> targetPlacemarkGroup, PlacemarkSymbol symbol) {
        final Pin[] placemarks = sourcePlacemarkGroup.toArray(new Pin[0]);
        for (Pin placemark : placemarks) {
            final Pin pin1 = new Pin(placemark.getName(), placemark.getLabel(),
                                     placemark.getDescription(), null, placemark.getGeoPos(),
                                     symbol);
            targetPlacemarkGroup.add(pin1);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MapProjOp.class);
        }
    }

    private static CoordinateReferenceSystem createTargetCRS() throws FactoryException {
        // TODO: create targetCRS from parameters
        final DefaultMathTransformFactory mtf = new DefaultMathTransformFactory();
//        Set<OperationMethod> methods = mtf.getAvailableMethods(Projection.class);
//        for (OperationMethod method : methods) {
//            System.out.println("method.getName() = " + method.getName());
//        }
        final ParameterValueGroup p = mtf.getDefaultParameters("Transverse_Mercator");
        p.parameter("semi_major").setValue(6378137.000);
        p.parameter("semi_minor").setValue(6356752.314);
        final MathTransform mt = mtf.createParameterizedTransform(p);

        return new DefaultProjectedCRS("tm", DefaultGeographicCRS.WGS84, mt, DefaultCartesianCS.PROJECTED);
    }

    private static Interpolation getInterpolation() {
        // TODO: create interpolation from parameters
        return new InterpolationNearest();
    }

    private static GridGeometry2D createGridGeometry(Product product,
                                                     CoordinateReferenceSystem targetCRS) {
        // TODO: create grid geometry from parameters
        final int sourceW = product.getSceneRasterWidth();
        final int sourceH = product.getSceneRasterHeight();
        final int step = Math.min(sourceW, sourceH) / 2;
        MathTransform mathTransform;
        try {
            mathTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, targetCRS);
        } catch (FactoryException e) {
            throw new OperatorException(e);
        }
        Point2D[] mapBoundary;
        try {
            mapBoundary = createMapBoundary(product, null, step, mathTransform);
        } catch (TransformException e) {
            throw new OperatorException(e);
        }
        Point2D[] minMax = ProductUtils.getMinMax(mapBoundary);
        final Point2D pMin = minMax[0];
        final Point2D pMax = minMax[1];
        double mapW = pMax.getX() - pMin.getX();
        double mapH = pMax.getY() - pMin.getY();

        float pixelSize = (float) Math.min(mapW / sourceW, mapH / sourceH);
        if (MathUtils.equalValues(pixelSize, 0.0f)) {
            pixelSize = 1.0f;
        }
        final int targetW = 1 + (int) Math.floor(mapW / pixelSize);
        final int targetH = 1 + (int) Math.floor(mapH / pixelSize);
        Rectangle targetGrid = new Rectangle(targetW, targetH);

        final float pixelX = 0.5f * targetW;
        final float pixelY = 0.5f * targetH;

        final float easting = (float) pMin.getX() + pixelX * pixelSize;
        final float northing = (float) pMax.getY() - pixelY * pixelSize;
        final double orientation = 0.0;

        AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(pixelSize, -pixelSize);
        transform.rotate(Math.toRadians(-orientation));
        transform.translate(-pixelX, -pixelY);

        final MathTransform gridToCrs = new AffineTransform2D(transform);
        return new GridGeometry2D(new GeneralGridEnvelope(targetGrid, 2), gridToCrs, targetCRS);
    }

    private static Point2D[] createMapBoundary(Product product, Rectangle rect, int step,
                                               MathTransform mathTransform) throws TransformException {
        GeoPos[] geoPoints = ProductUtils.createGeoBoundary(product, rect, step);
        ProductUtils.normalizeGeoPolygon(geoPoints);
        double[] geoPointsD = new double[geoPoints.length * 2];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPointsD[i * 2] = geoPoints[i].lon;
            geoPointsD[(i * 2) + 1] = geoPoints[i].lat;
        }
        double[] mapPointsD = new double[geoPoints.length * 2];
        mathTransform.transform(geoPointsD, 0, mapPointsD, 0, geoPoints.length);

        return getMinMax(mapPointsD);
    }

    private static Point2D[] getMinMax(double[] mapPointsD) {
        Point2D.Double min = new Point2D.Double();
        Point2D.Double max = new Point2D.Double();
        min.x = +Double.MAX_VALUE;
        min.y = +Double.MAX_VALUE;
        max.x = -Double.MAX_VALUE;
        max.y = -Double.MAX_VALUE;
        for (int i = 0; i < mapPointsD.length;) {
            double pointX = mapPointsD[i++];
            double pointY = mapPointsD[i++];
            min.x = Math.min(min.x, pointX);
            min.y = Math.min(min.y, pointY);
            max.x = Math.max(max.x, pointX);
            max.y = Math.max(max.y, pointY);
        }
        return new Point2D[]{min, max};
    }

    private static ImageLayout createImageLayout(RasterDataNode node, final Dimension tileSize) {
        final int w = node.getSceneRasterWidth();
        final int h = node.getSceneRasterHeight();
        final int bufferType = ImageManager.getDataBufferType(node.getDataType());

        return createSingleBandedImageLayout(bufferType, w, h, tileSize.width, tileSize.height);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataType,
                                                            int width,
                                                            int height,
                                                            int tileWidth,
                                                            int tileHeight) {
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, 1, 1);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return createImageLayout(width, height, tileWidth, tileHeight, sampleModel, colorModel);
    }

    private static ImageLayout createImageLayout(int width,
                                                 int height,
                                                 int tileWidth,
                                                 int tileHeight,
                                                 SampleModel sampleModel,
                                                 ColorModel colorModel) {
        return new ImageLayout(0, 0,
                               width,
                               height,
                               0, 0,
                               tileWidth,
                               tileHeight,
                               sampleModel,
                               colorModel);
    }

    private static GridCoverage2D createSourceCoverage(Envelope2D envelope, Band band) {
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        final RenderedImage sourceImage = band.getSourceImage();

        // TODO: create no-data gridSampleDimension from parameters
        if (band.getFlagCoding() == null) {
            return factory.create(band.getName(), sourceImage, envelope);
        }

        final Category category = createNoDataCategory(128.0);
        final GridSampleDimension sampleDimension = createGridSampleDimension(category);
        final GridSampleDimension[] sampleDimensions = new GridSampleDimension[]{sampleDimension};

        return factory.create(band.getName(), sourceImage, envelope, sampleDimensions, null, null);
    }

    private static GridSampleDimension createGridSampleDimension(Category category) {
        return new GridSampleDimension(category.getName(), new Category[]{category}, null);
    }

    private static Category createNoDataCategory(final double value) {
        final CharSequence name = Vocabulary.formatInternational(VocabularyKeys.NODATA);
        final Color transparent = new Color(0, 0, 0, 0);
        final NumberRange<Double> range = NumberRange.create(value, value);

        return new Category(name, transparent, range);
    }
}
