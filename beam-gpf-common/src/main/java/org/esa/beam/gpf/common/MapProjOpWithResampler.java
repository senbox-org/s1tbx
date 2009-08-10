package org.esa.beam.gpf.common;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;

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
import org.esa.beam.jai.LevelImageSupport;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.factory.Hints;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@OperatorMetadata(alias = "MapProjOpWithResampler",
                  internal = false)
public class MapProjOpWithResampler extends Operator {


    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private String projectionName = "Geographic Lat/Lon";
    private CoordinateReferenceSystem targetCRS;

    @Override
    public void initialize() throws OperatorException {
        Rectangle sourceRect = new Rectangle(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        BeamGridGeometry sourceGridGeometry = new BeamGridGeometry(sourceProduct.getGeoCoding().getImageToModelTransform(), 
                                                                   sourceRect, 
                                                                   sourceProduct.getGeoCoding().getImageCRS());
        try {
            if (targetCRS == null) {
                targetCRS = createTargetCRS();
            }
            
            /*
             * 2. Compute the target grid geometry
             */
            final BeamGridGeometry targetGridGeometry = createTargetGridGeometry(sourceProduct, sourceRect.width, sourceRect.height, targetCRS);
            Rectangle targetGridRect = targetGridGeometry.getBounds();

            /*
             * 3. Create the target product
             */
            targetProduct = new Product("projected_" + sourceProduct.getName(),
                                        "projection of: " + sourceProduct.getDescription(),
                                        targetGridRect.width,
                                        targetGridRect.height);
            /*
             * 4. Define some target properties
             */
            // TODO: also query operatorContext rendering hints for tile size
            final Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
            targetProduct.setPreferredTileSize(tileSize);
            addMetadataToProduct(targetProduct);
            addFlagCodingsToProduct(targetProduct);
            addIndexCodingsToProduct(targetProduct);
            targetProduct.setGeoCoding(new GeotoolsGeoCoding(targetCRS, targetGridGeometry));

            /*
             * 5. Create target bands
             */
            for (Band sourceBand : sourceProduct.getBands()) {
                Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);

//                ImageLayout imageLayout = createImageLayout(targetBand, tileSize);
//                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
//
//                RenderedImage sourceImage = sourceBand.getSourceImage();
//                double backgroundValue = 0;
//                if (sourceBand.getFlagCoding() != null) {
//                    backgroundValue = 128;
//                }
//                RenderedImage targetImage = Reproject.reproject(sourceImage, sourceGridGeometry, targetGridGeometry, backgroundValue, getInterpolation(), hints);
                
                targetBand.setSourceImage(createSourceImage(sourceBand, targetBand));
//                targetBand.setSourceImage(targetImage);
//                targetBand.setStx(sourceBand.getStx());
//                targetBand.setImageInfo(sourceBand.getImageInfo());

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

    private MultiLevelImage createSourceImage(final Band sourceBand, final Band targetBand) {
        final MultiLevelModel model = ImageManager.getInstance().getMultiLevelModel(targetBand);

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(model) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                int sourceLevel = targetLevel;
                MultiLevelModel sourceMLModel = sourceBand.getSourceImage().getModel();
                double targetScale = getModel().getScale(targetLevel);
                int targetWidth = (int)Math.floor(targetBand.getSceneRasterWidth() / targetScale);
                int targetHeight = (int)Math.floor(targetBand.getSceneRasterHeight() / targetScale);
                int sourceLevelCount = sourceMLModel.getLevelCount();
                if (sourceLevelCount-1 < targetLevel) {
                    sourceLevel = sourceLevelCount - 1;
                }
                PlanarImage leveledSourceImage = ImageManager.getInstance().getSourceImage(sourceBand, sourceLevel);
                
                int leveledSourceWidth = leveledSourceImage.getWidth();
                int leveledSourceHeight = leveledSourceImage.getHeight();
                
                Rectangle sourceRect = new Rectangle(leveledSourceWidth, leveledSourceHeight);
                BeamGridGeometry sourceGridGeometry = new BeamGridGeometry(sourceMLModel.getImageToModelTransform(sourceLevel), 
                                                                           sourceRect, 
                                                                           sourceProduct.getGeoCoding().getImageCRS());
                
                final BeamGridGeometry targetGridGeometry = createTargetGridGeometry(sourceProduct, leveledSourceWidth, leveledSourceHeight, targetWidth, targetHeight, targetCRS);
                
                Rectangle targetRect = targetGridGeometry.getBounds();
                ImageLayout imageLayout = createImageLayout(targetBand, targetRect.width, targetRect.height, new Dimension(128, 128));
                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                
                System.out.println("level "+targetLevel);
                System.out.println("targetGG "+targetGridGeometry.getBounds());
                
                try {
                    double backgroundValue = 0;
                  if (sourceBand.getFlagCoding() != null) {
                      backgroundValue = 128;
                  }
                    RenderedImage image = Reproject.reproject(leveledSourceImage, sourceGridGeometry, targetGridGeometry, backgroundValue, getInterpolation(), hints);
                    System.out.println("width "+image.getWidth());
                    System.out.println("height "+image.getHeight());
                    return image;
                } catch (FactoryException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (TransformException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
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
            super(MapProjOpWithResampler.class);
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

    private static BeamGridGeometry createTargetGridGeometry(Product product,
                                                       int sourceWidth,
                                                       int sourceHeight,
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
            mapBoundary = createMapBoundary(product, step, mathTransform);
        } catch (TransformException e) {
            throw new OperatorException(e);
        }
        final Point2D pMin = mapBoundary[0];
        final Point2D pMax = mapBoundary[1];
        double mapW = pMax.getX() - pMin.getX();
        double mapH = pMax.getY() - pMin.getY();

        float pixelSize = (float) Math.min(mapW / sourceWidth, mapH / sourceHeight);
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

        return new BeamGridGeometry(transform, targetGrid, targetCRS);
    }
    
    private static BeamGridGeometry createTargetGridGeometry(Product product,
                                                             int sourceWidth,
                                                             int sourceHeight,
                                                             int targetWidth,
                                                             int targetHeight,
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
                  mapBoundary = createMapBoundary(product, step, mathTransform);
              } catch (TransformException e) {
                  throw new OperatorException(e);
              }
              final Point2D pMin = mapBoundary[0];
              final Point2D pMax = mapBoundary[1];
              double mapW = pMax.getX() - pMin.getX();
              double mapH = pMax.getY() - pMin.getY();

              float pixelSize = (float) Math.min(mapW / sourceWidth, mapH / sourceHeight);
              if (MathUtils.equalValues(pixelSize, 0.0f)) {
                  pixelSize = 1.0f;
              }
//              final int targetW = 1 + (int) Math.floor(mapW / pixelSize);
//              final int targetH = 1 + (int) Math.floor(mapH / pixelSize);
              Rectangle targetGrid = new Rectangle(targetWidth, targetHeight);

              final float pixelX = 0.5f * targetWidth;
              final float pixelY = 0.5f * targetHeight;

              final float easting = (float) pMin.getX() + pixelX * pixelSize;
              final float northing = (float) pMax.getY() - pixelY * pixelSize;
              final double orientation = 0.0;

              AffineTransform transform = new AffineTransform();
              transform.translate(easting, northing);
              transform.scale(pixelSize, -pixelSize);
              transform.rotate(Math.toRadians(-orientation));
              transform.translate(-pixelX, -pixelY);

              return new BeamGridGeometry(transform, targetGrid, targetCRS);
    }    

    private static Point2D[] createMapBoundary(Product product, int step,
                                               MathTransform mathTransform) throws TransformException {
        GeoPos[] geoPoints = ProductUtils.createGeoBoundary(product, null, step);
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
    
    private static ImageLayout createImageLayout(RasterDataNode node, int w, int h, final Dimension tileSize) {
        final int bufferType = ImageManager.getDataBufferType(node.getDataType());

        return createSingleBandedImageLayout(bufferType, w, h, tileSize.width, tileSize.height);
    }

    private static ImageLayout createSingleBandedImageLayout(int dataType,
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
}
