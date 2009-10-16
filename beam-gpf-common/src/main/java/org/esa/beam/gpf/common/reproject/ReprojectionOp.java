/*
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.gpf.common.reproject;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.dem.Orthorectifier;
import org.esa.beam.framework.dataop.dem.Orthorectifier2;
import org.esa.beam.framework.dataop.resamp.Resampling;
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
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.text.MessageFormat;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;

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

    // todo (mp) - description needs to be enhanced
    @Parameter(description = "An EPSG or AUTO code defining the target Coordinate Reference System. " +
            "To find appropriate EPSG codes see (www.epsg-registry.com). " +
            "AUTO can be used with code 42001 (UTM), and 42002 (Transverse Mercator) where the scene center " +
            "is used as reference. Examples: EPSG:4326, AUTO:42001",
               pattern = "(?:[a-zA-Z]+:)?[0-9]+")
    private String crsCode;

    @Parameter(description = "A file which contains the target Coordinate Reference System in WKT format.")
    private File wktFile;

    @Parameter(description = "Text in WKT format describing the target Coordinate Reference System.")
    private String wkt;

    @Parameter(alias = "resampling",
               label = "Resampling Method",
               description = "The method used for resampling of floating-point raster data.",
               valueSet = {"Nearest", "Bilinear", "Bicubic"},
               defaultValue = "Nearest")
    private String resamplingName;

    @Parameter(description = "Wether tie-point grids should be included in the output product.",
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
               defaultValue = "0", interval = "[0,360]")
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

    @Parameter(description = "Wether the source product should be orthorectified. (Currently only applicable for MERIS and AATSR)",
               defaultValue = "false")
    private boolean orthorectify;
    @Parameter(description = "The name of the elevation model for the orthorectification. If not given tie-point data is used.")
    private String elevationModelName;
    
    @Parameter(description = "The value used to indicate no-data.")
    private Double noDataValue;
    @Parameter(description = "Generates an out-of-source region mask to indicate which pixel don't belong to the source product.",
               defaultValue = "false")
    private boolean generateOutOfSourceRegion;
    
    private ElevationModel elevationModel;


    @Override
    public void initialize() throws OperatorException {

        validateCrsParameters();
        validateResamplingParameter();
        validateReferencingParameters();
        validateTargetGridParameters();

        /*
        * 1. Compute the target CRS
        */
        CoordinateReferenceSystem targetCrs = createTargetCRS();
        /*
        * 2. Compute the target grid geometry
        */
        GridGeometry targetGridGeometry = createTargetGridGeometry(targetCrs);

        /*
        * 3. Create the target product
        */
        Rectangle targetGridRect = targetGridGeometry.getBounds2D().getBounds();
        targetProduct = new Product("projected_" + sourceProduct.getName(),
                                    "projection of: " + sourceProduct.getDescription(),
                                    targetGridRect.width,
                                    targetGridRect.height);
        /*
        * 4. Define some target properties
        */
        if (orthorectify) {
            elevationModel = createElevationModel();
        }
        // todo: also query operatorContext rendering hints for tile size
        final Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
        targetProduct.setPreferredTileSize(tileSize);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        copyIndexCoding();
        try {
            targetProduct.setGeoCoding(new CrsGeoCoding(targetGridGeometry.getModelCRS(),
                                                        targetGridGeometry.getBounds2D(),
                                                        targetGridGeometry.getGridToModel()));
        } catch (Exception e) {
            throw new OperatorException(e);
        }

        /*
        * 5. Create target bands
        */
        final MultiLevelModel srcModel = ImageManager.getInstance().getMultiLevelModel(sourceProduct.getBandAt(0));
        reprojectRasterDataNodes(sourceProduct.getBands(), srcModel);
        if (includeTiePointGrids) {
            reprojectRasterDataNodes(sourceProduct.getTiePointGrids(), srcModel);
        }
        if (generateOutOfSourceRegion && !sourceProduct.containsBand("reproject_flag")) {
            /*
            * 6. Create reprojection valid flag band
            */
            // todo consider products with more than one GeoCoding (mz) 
            // Avnir2 has no int bands ==> so this is not applicable
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
            GeoCoding sourceGeoCoding = getSourceGeoCoding(sourceProduct.getBandAt(0));
            reprojectValidBand.setSourceImage(createProjectedImage(sourceGeoCoding, createConstSourceImage(srcModel),
                                                                   srcModel, reprojectValidBand));
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
    
    @Override
    public void dispose() {
        if (elevationModel != null) {
            elevationModel.dispose();
        }
    }
    
    private ElevationModel createElevationModel() throws OperatorException {
        if (elevationModelName != null) {
            final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(
                    elevationModelName);
            if (!demDescriptor.isDemInstalled()) {
                throw new OperatorException("DEM not installed: " + elevationModelName);
            }
            return demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        }
        return null; // force use of elevation from tie-points
    }
    
    private GeoCoding getSourceGeoCoding(final RasterDataNode sourceBand) {
        if (orthorectify && sourceBand.canBeOrthorectified()) {
            return createOrthorectifier(sourceBand, elevationModel);
        } else {
            return sourceBand.getGeoCoding();
        }
    }    
    
    private Orthorectifier createOrthorectifier(final RasterDataNode sourceBand, ElevationModel elevationModel) {
        return new Orthorectifier2(sourceBand.getSceneRasterWidth(),
                                   sourceBand.getSceneRasterHeight(),
                                   sourceBand.getPointing(),
                                   elevationModel, 25);
    }
    

    private void reprojectRasterDataNodes(RasterDataNode[] rasterDataNodes, MultiLevelModel srcModel) {
        for (RasterDataNode raster : rasterDataNodes) {
            reprojectSourceRaster(raster, srcModel);
        }
    }

    private void reprojectSourceRaster(RasterDataNode sourceRaster, MultiLevelModel srcModel) {
        int geophysicalDataType = sourceRaster.getGeophysicalDataType();
        Band targetBand;
        MultiLevelImage projectedImage; 
        GeoCoding sourceGeoCoding = getSourceGeoCoding(sourceRaster);
        if (ProductData.isFloatingPointType(geophysicalDataType)) {
            targetBand = targetProduct.addBand(sourceRaster.getName(),
                                               sourceRaster.getGeophysicalDataType());
            targetBand.setDescription(sourceRaster.getDescription());
            targetBand.setUnit(sourceRaster.getUnit());
            double targetNoDataValue = getTargetNoDataValue(sourceRaster);
            targetBand.setNoDataValue(targetNoDataValue);
            targetBand.setNoDataValueUsed(true);
            MultiLevelImage sourceImage = sourceRaster.getGeophysicalImage();
            String exp = sourceRaster.getValidMaskExpression();
            if (exp != null) {
                exp = String.format("(%s) ? %s : NaN", exp, BandArithmetic.createExternalName(sourceRaster.getName()));
                sourceImage = createVirtualSourceImage(exp, geophysicalDataType, targetBand.getNoDataValue(),
                                                       sourceProduct, srcModel);
            }
            projectedImage = createProjectedImage(sourceGeoCoding, sourceImage, srcModel, targetBand);
            if (sourceRaster.isNoDataValueUsed() || noDataValue != null) {
                projectedImage = createNaNReplacedImage(srcModel, projectedImage, targetNoDataValue);
            }
        } else {
            targetBand = targetProduct.addBand(sourceRaster.getName(), sourceRaster.getDataType());
            ProductUtils.copyRasterDataNodeProperties(sourceRaster, targetBand);
            if (generateOutOfSourceRegion) {
                String validPixelExpression = targetBand.getValidPixelExpression();
                if (validPixelExpression == null) {
                    validPixelExpression = "reproject_flag.valid";
                } else {
                    validPixelExpression = String.format("reproject_flag.valid && %s", validPixelExpression);
                }
                targetBand.setValidPixelExpression(validPixelExpression);
            }
            projectedImage = createProjectedImage(sourceGeoCoding, sourceRaster.getSourceImage(), srcModel, targetBand);
        }
        targetBand.setSourceImage(projectedImage);

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
    }

    private double getTargetNoDataValue(RasterDataNode sourceRaster) {
        double targetNoDataValue = Double.NaN;
        if (noDataValue != null) {
            targetNoDataValue = noDataValue;
        } else if (sourceRaster.isNoDataValueUsed()) {
            targetNoDataValue = sourceRaster.getNoDataValue();
        }
        return targetNoDataValue;
    }

    private MultiLevelImage createNaNReplacedImage(final MultiLevelModel srcModel, final MultiLevelImage srcImage, final double value) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {

            @Override
            public RenderedImage createImage(int level) {
                return new ReplaceNaNOpImage(srcImage.getImage(level), value);
            }
        });
    }    
    
    
    private MultiLevelImage createConstSourceImage(final MultiLevelModel srcModel) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {

            @Override
            public RenderedImage createImage(int level) {
                Rectangle2D bounds2D = createLevelBounds(getModel(), level);
                Rectangle intBounds = bounds2D.getBounds();
                return ConstantDescriptor.create((float) intBounds.width, (float) intBounds.height, new Integer[]{1}, null);
            }
        });
    }

    private static MultiLevelImage createVirtualSourceImage(final String expression, final int geophysicalDataType,
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

    private MultiLevelImage createProjectedImage(final GeoCoding sourceGeoCoding, final MultiLevelImage sourceImage, final MultiLevelModel srcModel, final Band targetBand) {
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
                GridGeometry sourceGridGeometry = new GridGeometry(sourceRect,
                                                                   sourceGeoCoding.getModelCRS(),
                                                                   srcModel.getImageToModelTransform(sourceLevel));
                GridGeometry targetGridGeometry = new GridGeometry(targetRect,
                                                                   targetProduct.getGeoCoding().getModelCRS(),
                                                                   getModel().getImageToModelTransform(targetLevel));

                Interpolation usedResampling = getResampling();
                int dataType = targetBand.getDataType();
                if (!ProductData.isFloatingPointType(dataType)) {
                    usedResampling = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                }

                Rectangle targetRectInt = targetGridGeometry.getBounds2D().getBounds();
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
            if (crsCode != null && !crsCode.isEmpty()) {
                if (crsCode.matches("[0-9]*")) {  // if only numbers, then prefix with EPSG
                    crsCode = "EPSG:" + crsCode;
                }
                if (crsCode.matches("AUTO:[0-9]*")) {  // if AUTO code, then appen center Lon/Lat
                    PixelPos pixelPos = new PixelPos(sourceProduct.getSceneRasterWidth() / 2,
                                                     sourceProduct.getSceneRasterHeight() / 2);
                    GeoPos geoPos = sourceProduct.getGeoCoding().getGeoPos(pixelPos, null);
                    crsCode = String.format("%s,%s,%s", crsCode, geoPos.lon, geoPos.lat);
                }
                // to force longitude==xAxis and latitude==yAxis
                crs = CRS.decode(crsCode, true);
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
        final String msgPattern = "Invalid target CRS specification.\nSpecify {0} one of " +
                "''crsCode'', ''wktFile'', ''wkt'' and ''collocationProduct'' parameter.";

        if (crsCode == null && wktFile == null && wkt == null && collocationProduct == null) {
            throw new OperatorException(MessageFormat.format(msgPattern, "at least"));
        }

        boolean isCrsDefined = false;
        final String exceptionMsg = MessageFormat.format(msgPattern, "only");
        if (crsCode != null) {
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

    private Interpolation getResampling() {
        return Interpolation.getInstance(getResampleType(resamplingName));
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
        if (getResampleType(resamplingName) == -1) {
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

    private GridGeometry createTargetGridGeometry(CoordinateReferenceSystem targetCrs) {
        if (collocationProduct != null) {
            return createCollocationTargetGrid();
        } else {
            return createTargetGrid(targetCrs);
        }
    }

    private GridGeometry createTargetGrid(CoordinateReferenceSystem targetCrs) {
        Rectangle2D mapBoundary = createMapBoundary(targetCrs);
        double mapW = mapBoundary.getWidth();
        double mapH = mapBoundary.getHeight();

        if (pixelSizeX == null && pixelSizeY == null) {
            double pixelSize = Math.min(mapW / sourceProduct.getSceneRasterWidth(),
                                        mapH / sourceProduct.getSceneRasterHeight());
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
        return new GridGeometry(targetGrid, targetCrs, transform);
    }

    private GridGeometry createCollocationTargetGrid() {
        GeoCoding geoCoding = collocationProduct.getGeoCoding();
        AffineTransform i2m = (AffineTransform) geoCoding.getImageToModelTransform().clone();
        Rectangle bounds = new Rectangle(collocationProduct.getSceneRasterWidth(),
                                         collocationProduct.getSceneRasterHeight());
        CoordinateReferenceSystem modelCRS = geoCoding.getModelCRS();
        return new GridGeometry(bounds, modelCRS, i2m);
    }

    private Rectangle2D createMapBoundary(CoordinateReferenceSystem targetCrs) {
        final int sourceW = sourceProduct.getSceneRasterWidth();
        final int sourceH = sourceProduct.getSceneRasterHeight();
        final int step = Math.min(sourceW, sourceH) / 2;
        MathTransform mathTransform;
        try {
            mathTransform = CRS.findMathTransform(sourceProduct.getGeoCoding().getBaseCRS(), targetCrs);
        } catch (FactoryException e) {
            throw new OperatorException(e);
        }
        try {
            final Point2D[] point2Ds = createMapBoundary(sourceProduct, step, mathTransform);
            return new Rectangle2D.Double(point2Ds[0].getX(),
                                          point2Ds[0].getY(),
                                          point2Ds[1].getX() - point2Ds[0].getX(),
                                          point2Ds[1].getY() - point2Ds[0].getY());
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
        int i = 0;
        while (i < mapPointsD.length) {
            float pointX = mapPointsD[i++];
            float pointY = mapPointsD[i++];
            min.x = Math.min(min.x, pointX);
            min.y = Math.min(min.y, pointY);
            max.x = Math.max(max.x, pointX);
            max.y = Math.max(max.y, pointY);
        }
        return new Point2D[]{min, max};
    }

    private static ImageLayout createImageLayout(int productDataType, int width, int height, final Dimension tileSize) {
        int bufferType = ImageManager.getDataBufferType(productDataType);
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(bufferType, tileSize.width, tileSize.height);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return new ImageLayout(0, 0, width, height, 0, 0, tileSize.width, tileSize.height, sampleModel, colorModel);
    }

    // This code is simpler as the code currently used, but it does not consider the crossing of the 180° meridian.
    // This results in a map coverage of the whole earth.
    // todo - suggestion to simplify the currently used code are welcome
//    private Rectangle2D createMapBoundary(final Product product, CoordinateReferenceSystem targetCrs) {
//        try {
//            final CoordinateReferenceSystem sourceCrs = product.getGeoCoding().getImageCRS();
//            final int sourceW = product.getSceneRasterWidth();
//            final int sourceH = product.getSceneRasterHeight();
//
//            Rectangle2D rect = XRectangle2D.createFromExtremums(0.5, 0.5, sourceW - 0.5, sourceH - 0.5);
//            int pointsPerSide = Math.min(sourceH, sourceW) / 10;
//            pointsPerSide = Math.max(9, pointsPerSide);
//
//            final ReferencedEnvelope sourceEnvelope = new ReferencedEnvelope(rect, sourceCrs);
//            final ReferencedEnvelope targetEnvelope = sourceEnvelope.transform(targetCrs, true, pointsPerSide);
//            return new Rectangle2D.Double(targetEnvelope.getMinX(), targetEnvelope.getMinY(),
//                                          targetEnvelope.getWidth(), targetEnvelope.getHeight());
//        } catch (Exception e) {
//            throw new OperatorException(e);
//        }
//    }


}
