package org.esa.beam.gpf.common.reproject;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
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
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.factory.Hints;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;

/**
 * @author Marco Zuehlke
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
@OperatorMetadata(alias = "Reproject",
                  internal = false)
public class ReprojectionOp extends Operator {


    @SourceProduct (alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "EPSG Code", description="An EPSG code for the projected Coordinate Reference System.")
    private String epsgCode;
    @Parameter(label = "WKT File", description="A file which contains the projected Coordinate Reference System in WKT format.")
    private File wktFile;
    @Parameter(label = "Collocation product", description="A product to collocate with.")
    private Product collocationProduct;
    @Parameter(label = "Transformation Name", description="The name of the transformation.")
    private String transformationName;
//    @Parameter(label = "Transformation Parameter:", description = "The parameters of the transformation.", itemAlias = "parameter")
    private TransformationParameter[] transformationParameters = new TransformationParameter[0];

    @Parameter(label = "Interpolation Method", description = "The interpolation method.", 
               valueSet= {"Nearest", "Bilinear","Bicubic","Bicubic_2"},
               defaultValue = "Nearest")
    private String interpolationName;
    
    
    private CoordinateReferenceSystem targetCRS;
    private Interpolation interpolation;
    private Point2D[] mapBoundary;
    
    
    public static ReprojectionOp create(Product sourceProduct, CoordinateReferenceSystem targetCrs, String interpolationName) {
        ReprojectionOp reprojectionOp = new ReprojectionOp();
        reprojectionOp.setSourceProduct(sourceProduct);
        reprojectionOp.targetCRS = targetCrs;
        reprojectionOp.interpolationName = interpolationName;
        return reprojectionOp;
    }

    @Override
    public void initialize() throws OperatorException {
        Rectangle sourceRect = new Rectangle(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        try {
            if (targetCRS != null) {
                targetCRS = createTargetCRS();
            }
            interpolation = createInterpolation();
            computeMapBoundary();
            
            /*
             * 2. Compute the target grid geometry
             */
            final BeamGridGeometry targetGridGeometry = createTargetGridGeometryFromSourceSize(sourceRect.width, sourceRect.height);
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
            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            copyIndexCoding();
            targetProduct.setGeoCoding(new GeotoolsGeoCoding(targetGridGeometry));

            /*
             * 5. Create target bands
             */            
            boolean reprojectionFlagRequired = false;
            for (Band sourceBand : sourceProduct.getBands()) {
                int geophysicalDataType = sourceBand.getGeophysicalDataType();
                Band targetBand;
                MultiLevelImage sourceImage;
                if (ProductData.isFloatingPointType(geophysicalDataType)) {
                    targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getGeophysicalDataType());
                    targetBand.setDescription(sourceBand.getDescription());
                    targetBand.setUnit(sourceBand.getUnit());
                    if (ProductData.TYPE_FLOAT32 == geophysicalDataType) {
                        targetBand.setNoDataValue(Float.NaN);
                    } else {
                        targetBand.setNoDataValue(Double.NaN);
                    }
                    targetBand.setNoDataValueUsed(true);
                    ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
                    sourceImage = sourceBand.getGeophysicalImage();
                    String exp = sourceBand.getValidMaskExpression();
                    if (exp != null) {
                        exp = "(" + exp + ")>0?" + sourceBand.getName() + ":NaN";
                        sourceImage = createVirtualSourceImage(exp, geophysicalDataType, targetBand.getNoDataValue(), sourceProduct);
                    }
                } else {
                    targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
                    ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                    String validPixelExpression = targetBand.getValidPixelExpression();
                    if (validPixelExpression == null) {
                        validPixelExpression = "reproject_flag.valid";
                    } else {
                        validPixelExpression = "reproject_flag.valid && " + validPixelExpression;
                    }
                    targetBand.setValidPixelExpression(validPixelExpression);
                    sourceImage = sourceBand.getSourceImage();
                    reprojectionFlagRequired = true;
                }

//                ImageLayout imageLayout = createImageLayout(targetBand, tileSize);
//                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);

//                targetBand.setSourceImage(Reproject.reproject(sourceImage, sourceGridGeometry, targetGridGeometry, targetBand.getNoDataValue(), createInterpolation(), hints));
                
                targetBand.setSourceImage(createProjectedImage(sourceImage, targetBand, targetCRS));

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
            if (reprojectionFlagRequired && !sourceProduct.containsBand("reproject_flag")) {
                /*
                 * 6. Create reprojection valid flag band
                 */
                String reprojFlagBandName = "reproject_flag";
                Band reprojectValidBand = targetProduct.addBand(reprojFlagBandName, ProductData.TYPE_INT8);
                final FlagCoding flagCoding = new FlagCoding(reprojFlagBandName);
                flagCoding.setDescription("Reprojection Flag Coding");

                MetadataAttribute reprojAttr = new MetadataAttribute("valid", ProductData.TYPE_UINT8);
                reprojAttr.getData().setElemInt(1);
                reprojAttr.setDescription("valid data from reprojection");
                flagCoding.addAttribute(reprojAttr);
                targetProduct.getFlagCodingGroup().add(flagCoding);
                reprojectValidBand.setSampleCoding(flagCoding);
//                Hints bhints = new Hints(JAI.KEY_IMAGE_LAYOUT, createImageLayout(reprojectValidBand, tileSize));
//                RenderedImage productImage = VirtualBandOpImage.create("1", reprojectValidBand.getDataType(),
//                                                        0, 
//                                                        sourceProduct, ResolutionLevel.MAXRES);
//                RenderedImage producttargetImage = Reproject.reproject(productImage, sourceGridGeometry, targetGridGeometry, 0, createInterpolation(), bhints);
//                reprojectValidBand.setSourceImage(producttargetImage);
                reprojectValidBand.setSourceImage(createProjectedImage(null, reprojectValidBand, targetCRS));
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
    
    private static MultiLevelImage createVirtualSourceImage(final String expression, final int geophysicalDataType,
                                                            final Number noDataValue, final Product sourceProduct) {
        
        final MultiLevelModel model = ImageManager.getInstance().getMultiLevelModel(sourceProduct.getBandAt(0));
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(model) {

            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.create(expression, geophysicalDataType,
                                                        noDataValue, 
                                                        sourceProduct,
                                                        ResolutionLevel.create(getModel(), level));
            }
        });
    }
    
    private MultiLevelImage createProjectedImage(final RenderedImage sourceImage, final Band targetBand, final CoordinateReferenceSystem targetCRS) {
        final MultiLevelModel model = ImageManager.getInstance().getMultiLevelModel(targetBand);

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(model) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                MultiLevelModel sourceMLModel = sourceProduct.getBandAt(0).getSourceImage().getModel();
                int sourceLevel = targetLevel;
                double targetScale = getModel().getScale(targetLevel);
                int targetWidth = (int)Math.floor(targetBand.getSceneRasterWidth() / targetScale);
                int targetHeight = (int)Math.floor(targetBand.getSceneRasterHeight() / targetScale);
                int sourceLevelCount = sourceMLModel.getLevelCount();
                if (sourceLevelCount-1 < targetLevel) {
                    sourceLevel = sourceLevelCount - 1;
                }
                
                int leveledSourceWidth = (int)Math.floor(sourceProduct.getSceneRasterWidth() / sourceMLModel.getScale(sourceLevel));
                int leveledSourceHeight = (int)Math.floor(sourceProduct.getSceneRasterHeight() / sourceMLModel.getScale(sourceLevel));
                Rectangle sourceRect = new Rectangle(leveledSourceWidth, leveledSourceHeight);
                
                RenderedImage leveledSourceImage;
                if (sourceImage instanceof MultiLevelImage) {
                    MultiLevelImage multiLevelSourceImage = (MultiLevelImage) sourceImage;
                    leveledSourceImage = multiLevelSourceImage.getImage(sourceLevel);
                    
                } else {
                    leveledSourceImage = ConstantDescriptor.create((float)leveledSourceWidth, (float)leveledSourceHeight, new Integer[] {1}, null);
                }
                BeamGridGeometry sourceGridGeometry = new BeamGridGeometry(sourceMLModel.getImageToModelTransform(sourceLevel), 
                                                                           sourceRect, 
                                                                           sourceProduct.getGeoCoding().getImageCRS());
                BeamGridGeometry targetGridGeometry = createTargetGridGeometryFromTargetSize(targetWidth, targetHeight);
                
                Rectangle targetRect = targetGridGeometry.getBounds();
                ImageLayout imageLayout = createImageLayout(targetBand, targetRect.width, targetRect.height, targetProduct.getPreferredTileSize());
                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                
                Interpolation usedInterpolation = interpolation;
                int dataType = targetBand.getDataType();
                if (ProductData.isFloatingPointType(dataType)) {
                    usedInterpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                }
                
                try {
                    return Reproject.reproject(leveledSourceImage, sourceGridGeometry, targetGridGeometry, targetBand.getNoDataValue(), usedInterpolation, hints);
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
    
    private void copyIndexCoding() {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = sourceProduct.getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            ProductUtils.copyIndexCoding(sourceIndexCoding, targetProduct);
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
            super(ReprojectionOp.class);
        }
    }

    private CoordinateReferenceSystem createTargetCRS() throws OperatorException {
        CoordinateReferenceSystem crs;
        try {
            if (epsgCode != null && !epsgCode.isEmpty()) {
                // to force longitude==xAxis and latitude==yAxis
                boolean longitudeFirst = true; 
                crs = CRS.decode(epsgCode, longitudeFirst);
            } else if (wktFile != null) {
                String wkt = FileUtils.readText(wktFile);
                crs = CRS.parseWKT(wkt);
            } else if (collocationProduct != null && collocationProduct.getGeoCoding() != null) {
                crs = collocationProduct.getGeoCoding().getModelCRS();
            } else {
                final DefaultMathTransformFactory mtf = new DefaultMathTransformFactory();
                ParameterValueGroup p = mtf.getDefaultParameters(transformationName);
                if (p == null) {
                    throw new OperatorException("Unsupported transformation: " + transformationName);
                }
                for (TransformationParameter transformationParameter : transformationParameters) {
                    ParameterValue<?> parameter = p.parameter(transformationParameter.name);
                    if (parameter == null) {
                        throw new OperatorException("Unknown transformation parameter: " + transformationParameter.name);
                    }
                    parameter.setValue(transformationParameter.value);
                }
                final MathTransform transformation = mtf.createParameterizedTransform(p);
                crs = new DefaultProjectedCRS("User CRS (" + transformationName + ")",
                                                    DefaultGeographicCRS.WGS84, transformation, 
                                                    DefaultCartesianCS.PROJECTED);
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
        if (crs == null) {
            throw new OperatorException("Unable to create CRS");
        }
        return crs;
    }

    private Interpolation createInterpolation() {
        final int interpolationType;
        if (interpolationName.equalsIgnoreCase("Bilinear")) {
            interpolationType = Interpolation.INTERP_BILINEAR;
        } else if (interpolationName.equalsIgnoreCase("Bicubic")) {
            interpolationType = Interpolation.INTERP_BICUBIC;
        } else if (interpolationName.equalsIgnoreCase("Bicubic_2")) {
            interpolationType = Interpolation.INTERP_BICUBIC_2;
        } else {
            interpolationType = Interpolation.INTERP_NEAREST;
        }
        return Interpolation.getInstance(interpolationType);
    }
    
    private BeamGridGeometry createTargetGridGeometryFromSourceSize(int sourceWidth, int sourceHeight) {
        // TODO: create grid geometry from parameters
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
    
    private BeamGridGeometry createTargetGridGeometryFromTargetSize(int targetWidth, int targetHeight) {
              // TODO: create grid geometry from parameters
              final Point2D pMin = mapBoundary[0];
              final Point2D pMax = mapBoundary[1];
              double mapW = pMax.getX() - pMin.getX();
              double mapH = pMax.getY() - pMin.getY();

              float pixelSize = (float) Math.min(mapW / targetWidth, mapH / targetHeight);
              if (MathUtils.equalValues(pixelSize, 0.0f)) {
                  pixelSize = 1.0f;
              }
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
    
    private void computeMapBoundary() {
        final int sourceW = sourceProduct.getSceneRasterWidth();
        final int sourceH = sourceProduct.getSceneRasterHeight();
        final int step = Math.min(sourceW, sourceH) / 2;
        MathTransform mathTransform;
        try {
            mathTransform = CRS.findMathTransform(sourceProduct.getGeoCoding().getBaseCRS(), targetCRS);
        } catch (FactoryException e) {
            throw new OperatorException(e);
        }
        try {
            mapBoundary = createMapBoundary(sourceProduct, step, mathTransform);
        } catch (TransformException e) {
            throw new OperatorException(e);
        }
    }

    private static Point2D[] createMapBoundary(Product product, int step,
                                               MathTransform mathTransform) throws TransformException {
        GeoPos[] geoPoints = ProductUtils.createGeoBoundary(product, null, step);
        ProductUtils.normalizeGeoPolygon(geoPoints);
        float[] geoPointsD = new float[geoPoints.length * 2];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPointsD[i * 2] = geoPoints[i].lon;
            geoPointsD[(i * 2) + 1] = geoPoints[i].lat;
        }
        float[] mapPointsD = new float[geoPoints.length * 2];
        mathTransform.transform(geoPointsD, 0, mapPointsD, 0, geoPoints.length);

        return getMinMax(mapPointsD);
    }

    private static Point2D[] getMinMax(float[] mapPointsD) {
        Point2D.Float min = new Point2D.Float();
        Point2D.Float max = new Point2D.Float();
        min.x = +Float.MAX_VALUE;
        min.y = +Float.MAX_VALUE;
        max.x = -Float.MAX_VALUE;
        max.y = -Float.MAX_VALUE;
        for (int i = 0; i < mapPointsD.length;) {
            float pointX = mapPointsD[i++];
            float pointY = mapPointsD[i++];
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
