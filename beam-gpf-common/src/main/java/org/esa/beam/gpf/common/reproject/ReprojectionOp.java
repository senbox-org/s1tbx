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
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
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
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

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
@SuppressWarnings({"UnusedDeclaration"})
public class ReprojectionOp extends Operator {


    @SourceProduct(alias = "source", description = "The product which will be reprojected.")
    private Product sourceProduct;
    @SourceProduct(alias = "collocate", optional = true, label = "Collocation product",
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
    
    @Parameter(description = "Wether TiePoint grids should be included in the output product.",
               defaultValue = "true")
    private boolean includeTiePointGrids;
    
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
    private BeamGridGeometry targetGridGeometry;
    private Interpolation resampling;


    public static ReprojectionOp create(Map<String, Object> parameters,
                         Map<String, Product> sourceProducts,
                         RenderingHints renderingHints, 
                         CoordinateReferenceSystem targetCrs,
                         BeamGridGeometry targetGridGeometry) {
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        String operatorName = ReprojectionOp.Spi.getOperatorAlias(ReprojectionOp.class);
        OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new OperatorException("No SPI found for operator '" + operatorName + "'");
        }
        ReprojectionOp operator = (ReprojectionOp) operatorSpi.createOperator(parameters, sourceProducts, renderingHints);
        operator.targetCrs = targetCrs;
        operator.targetGridGeometry = targetGridGeometry;
        return operator;
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

    void setResamplingName(String resamplingName) {
        this.resamplingName = resamplingName;
    }

    void setReferencePixelX(double referencePixelX) {
        this.referencePixelX = referencePixelX;
    }

    void setReferencePixelY(double referencePixelY) {
        this.referencePixelY = referencePixelY;
    }

    void setEasting(double easting) {
        this.easting = easting;
    }

    void setNorthing(double northing) {
        this.northing = northing;
    }

    void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    void setWidth(int width) {
        this.width = width;
    }

    void setHeight(int height) {
        this.height = height;
    }

    void setPixelSizeX(double pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }

    void setPixelSizeY(double pixelSizeY) {
        this.pixelSizeY = pixelSizeY;
    }
    
    public void setIncludeTiePointGrids(boolean includeTiePointGrids) {
        this.includeTiePointGrids = includeTiePointGrids;
    }

    @Override
    public void initialize() throws OperatorException {

        validateCrsParameters();
        validateResamplingParameter();
        validateReferencingParameters();
        validateTargetGridParameters();
        
        if (targetCrs == null) {
            targetCrs = createTargetCRS();
        }
        resampling = Interpolation.getInstance(getResampleType(resamplingName));
        /*
        * 2. Compute the target grid geometry
        */
        if (targetGridGeometry == null) {
            targetGridGeometry = createTargetGridGeometry();
        }
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
            if (handleSourceRaster(sourceBand, srcModel)) {
                reprojectionFlagRequired = true;
            }
        }
        if (includeTiePointGrids) {
            for (TiePointGrid tiePointGrid: sourceProduct.getTiePointGrids()) {
                if (handleSourceRaster(tiePointGrid, srcModel)) {
                    reprojectionFlagRequired = true;
                }
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
    
    private boolean handleSourceRaster(RasterDataNode sourceRaster, MultiLevelModel srcModel) {
        int geophysicalDataType = sourceRaster.getGeophysicalDataType();
        Band targetBand;
        boolean reprojectionFlagRequired = false;
        MultiLevelImage sourceImage;
        if (ProductData.isFloatingPointType(geophysicalDataType)) {
            targetBand = targetProduct.addBand(sourceRaster.getName(), sourceRaster.getGeophysicalDataType());
            targetBand.setDescription(sourceRaster.getDescription());
            targetBand.setUnit(sourceRaster.getUnit());
            if (ProductData.TYPE_FLOAT32 == geophysicalDataType) {
                targetBand.setNoDataValue(Float.NaN);
            } else {
                targetBand.setNoDataValue(Double.NaN);
            }
            targetBand.setNoDataValueUsed(true);
            sourceImage = sourceRaster.getGeophysicalImage();
            String exp = sourceRaster.getValidMaskExpression();
            if (exp != null) {
                exp = String.format("(%s)>0?%s:NaN", exp, sourceRaster.getName());
                sourceImage = createVirtualSourceImage(exp, geophysicalDataType, targetBand.getNoDataValue(),
                                                       sourceProduct, srcModel);
            }
        } else {
            targetBand = targetProduct.addBand(sourceRaster.getName(), sourceRaster.getDataType());
            ProductUtils.copyRasterDataNodeProperties(sourceRaster, targetBand);
            String validPixelExpression = targetBand.getValidPixelExpression();
            if (validPixelExpression == null) {
                validPixelExpression = "reproject_flag.valid";
            } else {
                validPixelExpression = String.format("reproject_flag.valid && %s", validPixelExpression);
            }
            targetBand.setValidPixelExpression(validPixelExpression);
            sourceImage = sourceRaster.getSourceImage();
            reprojectionFlagRequired = true;
        }
        targetBand.setSourceImage(createProjectedImage(sourceImage, targetBand, srcModel));

        /*
        * Flag and index codings
        */
        if (sourceRaster instanceof Band) {
            Band sourceBand = (Band) sourceRaster;
            if (ProductData.isFloatingPointType(geophysicalDataType)) {
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
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
        return reprojectionFlagRequired;
    }

    private MultiLevelImage createConstSourceImage(final MultiLevelModel srcModel) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {

            @Override
            public RenderedImage createImage(int level) {
                Rectangle2D bounds = createLevelBounds(getModel(), level);
                Rectangle intBounds = BeamGridGeometry.getIntRectangle(bounds);
                return ConstantDescriptor.create((float) intBounds.width, (float) intBounds.height, new Integer[]{1}, null);
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
                Rectangle2D sourceRect = createLevelBounds(srcModel, sourceLevel);
                Rectangle2D targetRect = createLevelBounds(targetModel, targetLevel);

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

                Rectangle targetRectInt = targetGridGeometry.getBounds();
                ImageLayout imageLayout = createImageLayout(dataType, 
                                                            targetRectInt.width, 
                                                            targetRectInt.height, 
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

    private Rectangle2D createLevelBounds(MultiLevelModel model, int level) {
        final AffineTransform m2i = model.getModelToImageTransform(level);
        return m2i.createTransformedShape(model.getModelBounds()).getBounds2D();
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
                if (!epsgCode.toUpperCase().startsWith("EPSG:")) {
                    epsgCode = "EPSG:" + epsgCode;
                }
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

    protected void validateCrsParameters() {
        if(targetCrs != null) {
            return;     // no need to validate
        }
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

    private int getResampleType(String resamplingName) {
        final int resamplingType;
        if ("Nearest".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_NEAREST;
        } else if ("Bilinear".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BILINEAR;
        } else if ("Bicubic".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BICUBIC;
        } else {
            resamplingType = -1;
        }
        return resamplingType;
    }

    void validateResamplingParameter() {
        if(getResampleType(resamplingName) == -1) {
            throw new OperatorException("Invalid resampling method: " + resamplingName);
        }
    }

    void validateReferencingParameters() {
        if (!((referencePixelX == null && referencePixelY == null && easting == null && northing == null) 
                || (referencePixelX != null && referencePixelY != null && easting != null && northing != null))) {
            throw new OperatorException("Invalid referencing parameters: \n" +
            		"'referencePixelX, referencePixelY, easting and northing' have to be specified either all or non.");
        }
    }

    void validateTargetGridParameters() {
        if ((pixelSizeX != null && pixelSizeY == null) ||
                (pixelSizeX == null && pixelSizeY != null)) {
            throw new OperatorException("'pixelSizeX' and 'pixelSizeY' must be specifies both or not at all.");
        }
    }
    
    private BeamGridGeometry createTargetGridGeometry() {
        Rectangle2D mapBoundary = createMapBoundary(sourceProduct); 
        double mapW = mapBoundary.getWidth();
        double mapH = mapBoundary.getHeight();

        if (pixelSizeX == null && pixelSizeY == null) {
            double pixelSize =  Math.min(mapW / sourceProduct.getSceneRasterWidth(), mapH / sourceProduct.getSceneRasterHeight());
            if (MathUtils.equalValues(pixelSize, 0.0f)) {
                pixelSize = 1.0f;
            }
            pixelSizeX = pixelSize;
            pixelSizeY = pixelSize;
        }

        if (width == null) {
            width = (int) Math.floor(mapW / pixelSizeX);
        }
        if (height == null) {
            height = (int) Math.floor(mapH / pixelSizeY);
        }

        if (easting == null) {
            referencePixelX = 0.5 * width;
            referencePixelY = 0.5 * height;
            easting = mapBoundary.getX() + referencePixelX * pixelSizeX;
            northing = (mapBoundary.getY() + mapBoundary.getHeight()) - referencePixelY * pixelSizeY;
        }

        final AxisDirection targetAxisDirection = targetCrs.getCoordinateSystem().getAxis(1).getDirection();
        // When collocating the Y-Axis is DISPLAY_DOWN, then pixelSizeY must not negated
        if (!AxisDirection.DISPLAY_DOWN.equals(targetAxisDirection)) {
            pixelSizeY = -pixelSizeY;
        }

        AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(pixelSizeX, pixelSizeY);
        transform.rotate(Math.toRadians(-orientation));
        transform.translate(-referencePixelX, -referencePixelY);

        Rectangle targetGrid = new Rectangle(width, height);
        return new BeamGridGeometry(transform, targetGrid, targetCrs);
    }

    private Rectangle2D createMapBoundary(final Product product) {
        try {
            final CoordinateReferenceSystem sourceCrs = product.getGeoCoding().getImageCRS();
            final int sourceW = product.getSceneRasterWidth();
            final int sourceH = product.getSceneRasterHeight();

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
