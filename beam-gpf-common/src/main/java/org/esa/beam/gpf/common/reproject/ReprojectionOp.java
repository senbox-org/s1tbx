package org.esa.beam.gpf.common.reproject;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.resources.geometry.XRectangle2D;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.text.MessageFormat;

/**
 * @author Marco Zuehlke
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
@OperatorMetadata(alias = "Reproject",
                  version = "1.0",
                  authors = "Marco Zühlke, Marco Peters, Ralf Quast",
                  copyright = "(c) 2009 by Brockmann Consult",
                  description = "Reprojection of a source product to a target Coordinate Reference System.",
                  internal = false)
public class ReprojectionOp extends Operator {


    @SourceProduct(alias = "source", description = "The product which will be reprojected.")
    private Product sourceProduct;
    @SourceProduct(alias = "colocate", optional = true, label = "Collocation product",
                   description = "The source product will be collocated with this product.")
    private Product collocationProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "An EPSG code for the target Coordinate Reference System.",
               pattern = "[0-9]*")
    private String epsgCode;

    @Parameter(description = "A file which contains the target Coordinate Reference System in WKT format.")
    private File wktFile;

    @Parameter(description = "Text in WKT format describing the target Coordinate Reference System.")
    private String wkt;

    @Parameter(label = "Resampling Method", description = "The method used for resampling.",
               valueSet = {"Nearest", "Bilinear", "Bicubic"},
               defaultValue = "Nearest")
    private String resamplingName;
    
    // Referencing  todo - parameter object?
    @Parameter(description = "The X-position of the reference pixel.")
    private Double referencePixelX;
    @Parameter(description = "The Y-position of the reference pixel.")
    private Double referencePixelY;
    @Parameter(description = "The easting of the reference pixel.")
    private Double easting;
    @Parameter(description = "The northing of the reference pixel.")
    private Double northing;
    @Parameter(description = "The orientation of the output product (in degree).",
               defaultValue = "0", interval = "[0,360]" )
    private Double orientation;
    

    // target grid  todo - parameter object?
    @Parameter(description = "The pixels per reference unit in X direction.")
    private Double pixelSizeX;
    @Parameter(description = "The pixels per reference unit in Y direction.")
    private Double pixelSizeY;
    @Parameter(description = "The width of the output product.")
    private Integer width;
    @Parameter(description = "The height of the output product.")
    private Integer height;
    
    
    private CoordinateReferenceSystem targetCrs;
    private Interpolation resampling;
    private Rectangle2D mapBoundary;


    public static ReprojectionOp create(Product sourceProduct, CoordinateReferenceSystem targetCrs,
                                        String resamplingName) {
        ReprojectionOp reprojectionOp = new ReprojectionOp();
        reprojectionOp.setSourceProduct(sourceProduct);
        reprojectionOp.targetCrs = targetCrs;
        reprojectionOp.resamplingName = resamplingName;
        return reprojectionOp;
    }

    void setCollocationProduct(Product collocationProduct) {
        this.collocationProduct = collocationProduct;
    }

    void setEpsgCode(String epsgCode) {
        this.epsgCode = epsgCode;
    }

    void setWktFile(File wktFile) {
        this.wktFile = wktFile;
    }

    void setWkt(String wkt) {
        this.wkt = wkt;
    }

    @Override
    public void initialize() throws OperatorException {

        if (targetCrs == null) {
            validateParameters();
            targetCrs = createTargetCRS();
        }
        resampling = createResampling();
        mapBoundary = createMapBoundary();
        /*
        * 2. Compute the target grid geometry
        */
        Rectangle sourceRect = new Rectangle(sourceProduct.getSceneRasterWidth(),
                                             sourceProduct.getSceneRasterHeight());
        final BeamGridGeometry targetGridGeometry = createTargetGridGeometryFromSourceSize(sourceRect.width,
                                                                                           sourceRect.height);
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
        try {
            targetProduct.setGeoCoding(new GeotoolsGeoCoding(targetGridGeometry));
        } catch (Exception e) {
            throw new OperatorException(e);
        }

        /*
        * 5. Create target bands
        */
        final MultiLevelModel srcModel = ImageManager.getInstance().getMultiLevelModel(sourceProduct.getBandAt(0));
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
                    exp = String.format("(%s)>0?%s:NaN", exp, sourceBand.getName());
                    sourceImage = createVirtualSourceImage(exp, geophysicalDataType, targetBand.getNoDataValue(),
                                                           sourceProduct, srcModel);
                }
            } else {
                targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                String validPixelExpression = targetBand.getValidPixelExpression();
                if (validPixelExpression == null) {
                    validPixelExpression = "reproject_flag.valid";
                } else {
                    validPixelExpression = String.format("reproject_flag.valid && %s", validPixelExpression);
                }
                targetBand.setValidPixelExpression(validPixelExpression);
                sourceImage = sourceBand.getSourceImage();
                reprojectionFlagRequired = true;
            }
            targetBand.setSourceImage(createProjectedImage(sourceImage, targetBand, srcModel));

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
            reprojectValidBand.setSourceImage(
                    createProjectedImage(createConstSourceImage(srcModel), reprojectValidBand, srcModel));
        }

        /*
        * Bitmask definitions and placemarks
        */
        ProductUtils.copyBitmaskDefsAndOverlays(sourceProduct, targetProduct);
        copyPlacemarks(sourceProduct.getPinGroup(), targetProduct.getPinGroup(),
                       PlacemarkSymbol.createDefaultPinSymbol());
        copyPlacemarks(sourceProduct.getGcpGroup(), targetProduct.getGcpGroup(),
                       PlacemarkSymbol.createDefaultGcpSymbol());
    }

    private MultiLevelImage createConstSourceImage(final MultiLevelModel srcModel) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {

            @Override
            public RenderedImage createImage(int level) {
                Rectangle bounds = createLevelBounds(getModel(), level);
                return ConstantDescriptor.create((float) bounds.width, (float) bounds.height, new Integer[]{1}, null);
            }
        });
    }

    private MultiLevelImage createVirtualSourceImage(final String expression, final int geophysicalDataType,
                                                     final Number noDataValue, final Product sourceProduct,
                                                     final MultiLevelModel srcModel) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {

            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.create(expression, geophysicalDataType,
                                                 noDataValue,
                                                 sourceProduct,
                                                 ResolutionLevel.create(getModel(), level));
            }
        });
    }

    private MultiLevelImage createProjectedImage(final MultiLevelImage sourceImage, final Band targetBand,
                                                 final MultiLevelModel srcModel) {
        final MultiLevelModel targetModel = ImageManager.getInstance().getMultiLevelModel(targetBand);
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(targetModel) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                int sourceLevel = targetLevel;
                int sourceLevelCount = srcModel.getLevelCount();
                if (sourceLevelCount - 1 < targetLevel) {
                    sourceLevel = sourceLevelCount - 1;
                }
                Rectangle sourceRect = createLevelBounds(srcModel, sourceLevel);
                Rectangle targetRect = createLevelBounds(targetModel, targetLevel);

                BeamGridGeometry sourceGridGeometry = new BeamGridGeometry(
                        srcModel.getImageToModelTransform(sourceLevel),
                        sourceRect,
                        sourceProduct.getGeoCoding().getModelCRS());
                BeamGridGeometry targetGridGeometry = new BeamGridGeometry(
                        getModel().getImageToModelTransform(targetLevel),
                        targetRect,
                        targetProduct.getGeoCoding().getModelCRS());

                Interpolation usedResampling = resampling;
                int dataType = targetBand.getDataType();
                if (!ProductData.isFloatingPointType(dataType)) {
                    usedResampling = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                }

                ImageLayout imageLayout = createImageLayout(dataType, targetRect.width, targetRect.height,
                                                            targetProduct.getPreferredTileSize());
                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                RenderedImage leveledSourceImage = sourceImage.getImage(sourceLevel);

                try {
                    return Reproject.reproject(leveledSourceImage, sourceGridGeometry, targetGridGeometry,
                                               targetBand.getNoDataValue(), usedResampling, hints);
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

    private Rectangle createLevelBounds(MultiLevelModel model, int level) {
        final AffineTransform m2i = model.getModelToImageTransform(level);
        return m2i.createTransformedShape(model.getModelBounds()).getBounds();
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
        CoordinateReferenceSystem crs = null;
        try {
            if (epsgCode != null && !epsgCode.isEmpty()) {
                // to force longitude==xAxis and latitude==yAxis
                crs = CRS.decode(epsgCode, true);
            } else if (wktFile != null) {
                crs = CRS.parseWKT(FileUtils.readText(wktFile));
            } else if (wkt != null) {
                crs = CRS.parseWKT(wkt);
            } else if (collocationProduct != null && collocationProduct.getGeoCoding() != null) {
                crs = collocationProduct.getGeoCoding().getModelCRS();
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
        return crs;
    }

    protected void validateParameters() {
        final String msgPattern = "Invalid target CRS specification.\nSpecify {0} one of " +
                                  "''epsgCode'', ''wktFile'', ''wkt'' and ''collocationProduct'' parameter.";

        if (epsgCode == null && wktFile == null && wkt == null && collocationProduct == null) {
            throw new OperatorException(MessageFormat.format(msgPattern, "at least"));
        }

        boolean isCrsDefined = false;
        final String exceptionMsg = MessageFormat.format(msgPattern, "only");
        if (epsgCode != null) {
            isCrsDefined = true;
        }
        if (wktFile != null) {
            if (isCrsDefined) {
                throw new OperatorException(exceptionMsg);
            }
            isCrsDefined = true;
        }
        if (wkt != null) {
            if (isCrsDefined) {
                throw new OperatorException(exceptionMsg);
            }
            isCrsDefined = true;
        }

        if (collocationProduct != null && isCrsDefined) {
            throw new OperatorException(exceptionMsg);
        }
    }

    private Interpolation createResampling() {
        final int resamplingType;
        if ("Bilinear".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BILINEAR;
        } else if ("Bicubic".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BICUBIC;
        } else if ("Bicubic_2".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BICUBIC_2;
        } else {
            resamplingType = Interpolation.INTERP_NEAREST;
        }
        return Interpolation.getInstance(resamplingType);
    }

    private BeamGridGeometry createTargetGridGeometryFromSourceSize(int sourceWidth, int sourceHeight) {
        // TODO: create grid geometry from parameters
        double mapW = mapBoundary.getWidth();
        double mapH = mapBoundary.getHeight();

        float pixelSize = (float) Math.min(mapW / sourceWidth, mapH / sourceHeight);
        if (MathUtils.equalValues(pixelSize, 0.0f)) {
            pixelSize = 1.0f;
        }
        final int targetW = 1 + (int) Math.floor(mapW / pixelSize);
        final int targetH = 1 + (int) Math.floor(mapH / pixelSize);
        Rectangle targetGrid = new Rectangle(targetW, targetH);

        AffineTransform transform = createI2MTransform(targetGrid, pixelSize);
        return new BeamGridGeometry(transform, targetGrid, targetCrs);
    }

    private BeamGridGeometry createTargetGridGeometryFromTargetSize(int targetWidth, int targetHeight) {
        // TODO: create grid geometry from parameters
        double mapW = mapBoundary.getWidth();
        double mapH = mapBoundary.getHeight();

        float pixelSize = (float) Math.min(mapW / targetWidth, mapH / targetHeight);
        if (MathUtils.equalValues(pixelSize, 0.0f)) {
            pixelSize = 1.0f;
        }
        Rectangle targetGrid = new Rectangle(targetWidth, targetHeight);

        AffineTransform transform = createI2MTransform(targetGrid, pixelSize);
        return new BeamGridGeometry(transform, targetGrid, targetCrs);
    }

    private AffineTransform createI2MTransform(Rectangle imageRect, double pixelSize) {
        final double pixelX = 0.5 * imageRect.getWidth();
        final double pixelY = 0.5 * imageRect.getHeight();
        final double easting = mapBoundary.getX() + pixelX * pixelSize;
        final double northing = (mapBoundary.getY() + mapBoundary.getHeight()) - pixelY * pixelSize;
        final double orientation = 0.0;

        AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(pixelSize, -pixelSize);
        transform.rotate(Math.toRadians(-orientation));
        transform.translate(-pixelX, -pixelY);
        return transform;
    }

    private Rectangle2D createMapBoundary() {
        try {
            final CoordinateReferenceSystem sourceCrs = sourceProduct.getGeoCoding().getImageCRS();
            final int sourceW = sourceProduct.getSceneRasterWidth();
            final int sourceH = sourceProduct.getSceneRasterHeight();

            Rectangle2D rect = XRectangle2D.createFromExtremums(0.5, 0.5, sourceW - 0.5, sourceH - 0.5);
            int pointsPerSide = Math.min(sourceH, sourceW) / 10;
            pointsPerSide = Math.max(9, pointsPerSide);

            final ReferencedEnvelope sourceEnvelope = new ReferencedEnvelope(rect, sourceCrs);
            final ReferencedEnvelope targetEnvelope = sourceEnvelope.transform(targetCrs, true, pointsPerSide);
            return new Rectangle2D.Double(targetEnvelope.getMinX(), targetEnvelope.getMinY(),
                                          targetEnvelope.getWidth(), targetEnvelope.getHeight());

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private static ImageLayout createImageLayout(int productDataType, int width, int height, final Dimension tileSize) {
        int bufferType = ImageManager.getDataBufferType(productDataType);
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(bufferType, tileSize.width, tileSize.height);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return new ImageLayout(0, 0, width, height, 0, 0, tileSize.width, tileSize.height, sampleModel, colorModel);
    }
}
