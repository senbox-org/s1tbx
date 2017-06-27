/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.gpf.common.reproject;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageGeometry;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.dem.Orthorectifier;
import org.esa.snap.core.dataop.dem.Orthorectifier2;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.geotools.factory.Hints;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

/**
 * <p>
 * The reprojection operator is used to geo-reference data products.
 * Beside plain reprojection it is able to use a Digital Elevation Model (DEM) to orthorectify a data product and
 * to collocate one product with another.
 * <p>
 * The following XML sample shows how to integrate the <code>Reproject</code> operator in a processing graph (an
 * Lambert_Azimuthal_Equal_Area projection using the WGS-84 datum):
 * <pre>
 *    &lt;node id="reprojectNode"&gt;
 *        &lt;operator&gt;Reproject&lt;/operator&gt;
 *        &lt;sources&gt;
 *            &lt;sourceProducts&gt;readNode&lt;/sourceProducts&gt;
 *        &lt;/sources&gt;
 *        &lt;parameters&gt;
 *            &lt;wktFile/&gt;
 *            &lt;crs&gt;
 *              PROJCS["Lambert_Azimuthal_Equal_Area / World Geodetic System 1984",
 *                GEOGCS["World Geodetic System 1984",
 *                   DATUM["World Geodetic System 1984",
 *                      SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]],
 *                   AUTHORITY["EPSG","6326"]],
 *                   PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]],
 *                   UNIT["degree", 0.017453292519943295],
 *                   AXIS["Geodetic longitude", EAST],
 *                   AXIS["Geodetic latitude", NORTH]],
 *                PROJECTION["Lambert_Azimuthal_Equal_Area"],
 *                PARAMETER["latitude_of_center", 0.0],
 *                PARAMETER["longitude_of_center", 0.0],
 *                PARAMETER["false_easting", 0.0],
 *                PARAMETER["false_northing", 0.0],
 *                UNIT["m", 1.0],
 *                AXIS["Easting", EAST],
 *                AXIS["Northing", NORTH]]
 *            &lt;/crs&gt;
 *            &lt;resampling&gt;Nearest&lt;/resampling&gt;
 *            &lt;referencePixelX&gt;0.5&lt;/referencePixelX&gt;
 *            &lt;referencePixelY&gt;0.5&lt;/referencePixelY&gt;
 *            &lt;easting&gt;9.5&lt;/easting&gt;
 *            &lt;northing&gt;56.84&lt;/northing&gt;
 *            &lt;orientation&gt;0.0&lt;/orientation&gt;
 *            &lt;pixelSizeX&gt;0.012&lt;/pixelSizeX&gt;
 *            &lt;pixelSizeY&gt;0.012&lt;/pixelSizeY&gt;
 *            &lt;width&gt;135010246&lt;/width&gt;
 *            &lt;height&gt;116629771&lt;/height&gt;
 *            &lt;orthorectify&gt;false&lt;/orthorectify&gt;
 *            &lt;elevationModelName/&gt;
 *            &lt;noDataValue&gt;NaN&lt;/noDataValue&gt;
 *            &lt;includeTiePointGrids&gt;true&lt;/includeTiePointGrids&gt;
 *            &lt;addDeltaBands&gt;false&lt;/addDeltaBands&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 * @author Marco Zuehlke
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
@OperatorMetadata(alias = "Reproject",
        category = "Raster/Geometric",
        version = "1.0",
        authors = "Marco Zühlke, Marco Peters, Ralf Quast, Norman Fomferra",
        copyright = "(c) 2009 by Brockmann Consult",
        description = "Reprojection of a source product to a target Coordinate Reference System.",
        internal = false)
@SuppressWarnings({"UnusedDeclaration"})
public class ReprojectionOp extends Operator {


    @SourceProduct(alias = "source", description = "The product which will be reprojected.")
    private Product sourceProduct;
    @SourceProduct(alias = "collocateWith", optional = true, label = "Collocation product",
            description = "The source product will be collocated with this product.")
    private Product collocationProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "A file which contains the target Coordinate Reference System in WKT format.")
    private File wktFile;

    @Parameter(description = "A text specifying the target Coordinate Reference System, either in WKT or as an " +
            "authority code. For appropriate EPSG authority codes see (www.epsg-registry.org). " +
            "AUTO authority can be used with code 42001 (UTM), and 42002 (Transverse Mercator) " +
            "where the scene center is used as reference. Examples: EPSG:4326, AUTO:42001")
    private String crs;

    @Parameter(alias = "resampling",
            label = "Resampling Method",
            description = "The method used for resampling of floating-point raster data.",
            valueSet = {"Nearest", "Bilinear", "Bicubic"},
            defaultValue = "Nearest")
    private String resamplingName;

    @Parameter(description = "The X-position of the reference pixel.")
    private Double referencePixelX;
    @Parameter(description = "The Y-position of the reference pixel.")
    private Double referencePixelY;
    @Parameter(description = "The easting of the reference pixel.")
    private Double easting;
    @Parameter(description = "The northing of the reference pixel.")
    private Double northing;
    @Parameter(description = "The orientation of the output product (in degree).",
            defaultValue = "0", interval = "[-360,360]")
    private Double orientation;
    @Parameter(description = "The pixel size in X direction given in CRS units.")
    private Double pixelSizeX;
    @Parameter(description = "The pixel size in Y direction given in CRS units.")
    private Double pixelSizeY;
    @Parameter(description = "The width of the target product.")
    private Integer width;
    @Parameter(description = "The height of the target product.")
    private Integer height;
    @Parameter(description = "The tile size in X direction.")
    private Integer tileSizeX;
    @Parameter(description = "The tile size in Y direction.")
    private Integer tileSizeY;

    @Parameter(description = "Whether the source product should be orthorectified. (Not applicable to all products)",
            defaultValue = "false")
    private boolean orthorectify;

    @Parameter(description = "The name of the elevation model for the orthorectification. " +
            "If not given tie-point data is used.")
    private String elevationModelName;

    @Parameter(description = "The value used to indicate no-data.")
    private Double noDataValue;

    @Parameter(description = "Whether tie-point grids should be included in the output product.",
            defaultValue = "true")
    private boolean includeTiePointGrids;

    @Parameter(description = "Whether to add delta longitude and latitude bands.",
            defaultValue = "false")
    private boolean addDeltaBands;

    private ReprojectionSettingsProvider reprojectionSettingsProvider;

    private ElevationModel elevationModel;

    @Override
    public void initialize() throws OperatorException {
        ensureSingleRasterSize(sourceProduct);
        validateCrsParameters();
        validateResamplingParameter();
        validateReferencingParameters();
        validateTargetGridParameters();
        validateSARProduct();

        /*
        * 1. Compute the target CRS
        */
        final GeoPos centerGeoPos =
                getCenterGeoPos(sourceProduct.getSceneGeoCoding(), sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        CoordinateReferenceSystem targetCrs = createTargetCRS(centerGeoPos);
        /*
        * 2. Compute the target geometry
        */
        ImageGeometry targetImageGeometry = createImageGeometry(targetCrs);
//        determineDefaultSourceModel();
        /*
        * 3. Create the target product
        */
        Rectangle targetRect = targetImageGeometry.getImageRect();
        targetProduct = new Product("projected_" + sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    targetRect.width,
                                    targetRect.height);
        targetProduct.setDescription(sourceProduct.getDescription());
        Dimension tileSize;
        if (tileSizeX != null && tileSizeY != null) {
            tileSize = new Dimension(tileSizeX, tileSizeY);
        } else {
            tileSize = ImageManager.getPreferredTileSize(targetProduct);
            Dimension sourceProductPreferredTileSize = sourceProduct.getPreferredTileSize();
            if (sourceProductPreferredTileSize != null) {
                if (sourceProductPreferredTileSize.width == sourceProduct.getSceneRasterWidth()) {
                    tileSize.width = targetProduct.getSceneRasterWidth();
                    tileSize.height = Math.min(sourceProductPreferredTileSize.height,
                                               targetProduct.getSceneRasterHeight());
                }
            }
        }
        targetProduct.setPreferredTileSize(tileSize);
        /*
        * 4. Define some target properties
        */
        if (orthorectify) {
            elevationModel = createElevationModel();
        }
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        copyIndexCoding();
        try {
            targetProduct.setSceneGeoCoding(new CrsGeoCoding(targetImageGeometry.getMapCrs(),
                                                             targetRect,
                                                             targetImageGeometry.getImage2MapTransform()));
        } catch (Exception e) {
            throw new OperatorException(e);
        }

        ProductData.UTC meanTime = getSourceMeanTime();
        targetProduct.setStartTime(meanTime);
        targetProduct.setEndTime(meanTime);

        reprojectionSettingsProvider = new ReprojectionSettingsProvider(targetImageGeometry);

        reprojectRasterDataNodes(sourceProduct.getBands());
        if (includeTiePointGrids) {
            reprojectRasterDataNodes(sourceProduct.getTiePointGrids());
        }
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

        if (addDeltaBands) {
            addDeltaBands();
        }
    }

    @Override
    public void dispose() {
        if (elevationModel != null) {
            elevationModel.dispose();
        }
        super.dispose();
    }

    private ElevationModel createElevationModel() throws OperatorException {
        if (elevationModelName != null) {
            final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(
                    elevationModelName);
            return demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        }
        return null; // force use of elevation from tie-points
    }

    private GeoCoding getSourceGeoCoding(final RasterDataNode sourceBand) {
        if (orthorectify && sourceBand.canBeOrthorectified()) {
            return createOrthorectifier(sourceBand);
        } else {
            return sourceBand.getGeoCoding();
        }
    }

    private Orthorectifier createOrthorectifier(final RasterDataNode sourceBand) {
        return new Orthorectifier2(sourceBand.getRasterWidth(),
                                   sourceBand.getRasterHeight(),
                                   sourceBand.getPointing(),
                                   elevationModel, 25);
    }


    private void reprojectRasterDataNodes(RasterDataNode[] rasterDataNodes) {
        for (RasterDataNode raster : rasterDataNodes) {
            reprojectSourceRaster(raster);
        }
    }

    private void reprojectSourceRaster(RasterDataNode sourceRaster) {
        final ReprojectionSettings reprojectionSettings = reprojectionSettingsProvider.getReprojectionSettings(sourceRaster);
        final int targetDataType;
        MultiLevelImage sourceImage;
        if (sourceRaster.isScalingApplied()) {
            targetDataType = sourceRaster.getGeophysicalDataType();
            sourceImage = sourceRaster.getGeophysicalImage();
        } else {
            targetDataType = sourceRaster.getDataType();
            sourceImage = sourceRaster.getSourceImage();
        }
        final Number targetNoDataValue = getTargetNoDataValue(sourceRaster, targetDataType);
        final Rectangle imageRect = reprojectionSettings.getImageGeometry().getImageRect();
        final Band targetBand = new Band(sourceRaster.getName(), targetDataType, (int) imageRect.getWidth(), (int) imageRect.getHeight());
        targetProduct.addBand(targetBand);
        targetBand.setLog10Scaled(sourceRaster.isLog10Scaled());
        targetBand.setNoDataValue(targetNoDataValue.doubleValue());
        targetBand.setNoDataValueUsed(targetBand.getRasterWidth() == targetProduct.getSceneRasterWidth() &&
                                              targetBand.getRasterHeight() == targetProduct.getSceneRasterHeight());
        targetBand.setDescription(sourceRaster.getDescription());
        targetBand.setUnit(sourceRaster.getUnit());
        GeoCoding bandGeoCoding = reprojectionSettings.getGeoCoding();
        if (bandGeoCoding != null) {
            targetBand.setGeoCoding(bandGeoCoding);
        }

        final GeoCoding sourceGeoCoding = getSourceGeoCoding(sourceRaster);
        final String exp = sourceRaster.getValidMaskExpression();
        if (exp != null) {
            sourceImage = createNoDataReplacedImage(sourceRaster, targetNoDataValue);
        }

        final Interpolation resampling = getResampling(targetBand);
        MultiLevelModel targetModel = reprojectionSettings.getTargetModel();
        if (targetModel == null) {
            targetModel = targetBand.getMultiLevelModel();
            reprojectionSettings.setTargetModel(targetModel);
        }
        Reproject reprojection = reprojectionSettings.getReprojection();
        if (reprojection == null) {
            reprojection = new Reproject(targetModel.getLevelCount());
            reprojectionSettings.setReprojection(reprojection);
        }
        MultiLevelImage projectedImage = createProjectedImage(sourceGeoCoding, sourceImage, reprojectionSettings.getSourceModel(),
                                                              targetBand, resampling, targetModel, reprojection);
        if (mustReplaceNaN(sourceRaster, targetDataType, targetNoDataValue.doubleValue())) {
            projectedImage = createNaNReplacedImage(projectedImage, targetModel, targetNoDataValue.doubleValue());
        }
        if (targetBand.isLog10Scaled()) {
            projectedImage = createLog10ScaledImage(projectedImage);
        }
        targetBand.setSourceImage(projectedImage);

        /*
        * Flag and index codings
        */
        if (sourceRaster instanceof Band) {
            final Band sourceBand = (Band) sourceRaster;
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            final FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
            final IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
            if (sourceFlagCoding != null) {
                final String flagCodingName = sourceFlagCoding.getName();
                final FlagCoding destFlagCoding = targetProduct.getFlagCodingGroup().get(flagCodingName);
                targetBand.setSampleCoding(destFlagCoding);
            } else if (sourceIndexCoding != null) {
                final String indexCodingName = sourceIndexCoding.getName();
                final IndexCoding destIndexCoding = targetProduct.getIndexCodingGroup().get(indexCodingName);
                targetBand.setSampleCoding(destIndexCoding);
            }
        }
    }

    private MultiLevelImage createLog10ScaledImage(final MultiLevelImage projectedImage) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(projectedImage.getModel()) {
            @Override
            public RenderedImage createImage(int level) {
                return new Log10OpImage(projectedImage.getImage(level));
            }
        });
    }

    private boolean mustReplaceNaN(RasterDataNode sourceRaster, int targetDataType, double targetNoDataValue) {
        final boolean isFloat = ProductData.isFloatingPointType(targetDataType);
        final boolean isNoDataGiven = sourceRaster.isNoDataValueUsed() || noDataValue != null;
        final boolean isNoDataNaN = Double.isNaN(targetNoDataValue);
        return isFloat && isNoDataGiven && !isNoDataNaN;
    }

    private Number getTargetNoDataValue(RasterDataNode sourceRaster, int targetDataType) {
        double targetNoDataValue = Double.NaN;
        if (noDataValue != null) {
            targetNoDataValue = noDataValue;
        } else if (sourceRaster.isNoDataValueUsed()) {
            targetNoDataValue = sourceRaster.getNoDataValue();
        }
        Number targetNoDataNumber;
        switch (targetDataType) {
            case ProductData.TYPE_INT8:
                targetNoDataNumber = (byte) targetNoDataValue;
                break;
            case ProductData.TYPE_INT16:
            case ProductData.TYPE_UINT8:
                targetNoDataNumber = (short) targetNoDataValue;
                break;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
            case ProductData.TYPE_UINT16:
                targetNoDataNumber = (int) targetNoDataValue;
                break;
            case ProductData.TYPE_FLOAT32:
                targetNoDataNumber = (float) targetNoDataValue;
                break;
            default:
                targetNoDataNumber = targetNoDataValue;
                break;
        }
        return targetNoDataNumber;
    }

    private MultiLevelImage createNaNReplacedImage(final MultiLevelImage projectedImage, MultiLevelModel targetModel, final double value) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(targetModel) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                return new ReplaceNaNOpImage(projectedImage.getImage(targetLevel), value);
            }
        });
    }

    private MultiLevelImage createNoDataReplacedImage(final RasterDataNode rasterDataNode, final Number noData) {
        return ImageManager.createMaskedGeophysicalImage(rasterDataNode, noData);
    }

    private MultiLevelImage createProjectedImage(final GeoCoding sourceGeoCoding, final MultiLevelImage sourceImage,
                                                 MultiLevelModel sourceModel, final Band targetBand, final Interpolation resampling,
                                                 MultiLevelModel targetModel, Reproject reprojection) {
        final CoordinateReferenceSystem sourceModelCrs = Product.findModelCRS(sourceGeoCoding);
        final CoordinateReferenceSystem targetModelCrs = Product.findModelCRS(targetBand.getGeoCoding());
        final AffineTransform sourceImageToMapTransform = Product.findImageToModelTransform(sourceGeoCoding);
        final AffineTransform targetImageToMapTransform = Product.findImageToModelTransform(targetBand.getGeoCoding());

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(targetModel) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                final double targetScale = targetModel.getScale(targetLevel);
                final int sourceLevel = sourceImage.getModel().getLevel(targetScale);
                RenderedImage leveledSourceImage = sourceImage.getImage(sourceLevel);

                final Rectangle sourceBounds = new Rectangle(leveledSourceImage.getMinX(),
                                                             leveledSourceImage.getMinY(),
                                                             leveledSourceImage.getWidth(),
                                                             leveledSourceImage.getHeight());

                // the following transformation maps the source level image to level zero and then to the model,
                // which either is a map or an image CRS
                final AffineTransform i2mSource = sourceModel.getImageToModelTransform(sourceLevel);
                i2mSource.concatenate(sourceModel.getModelToImageTransform(0));
                i2mSource.concatenate(sourceImageToMapTransform);

                ImageGeometry sourceGeometry = new ImageGeometry(sourceBounds,
                                                                 sourceModelCrs,
                                                                 i2mSource);

                ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(
                        ImageManager.getDataBufferType(targetBand.getDataType()),
                        targetBand.getRasterWidth(),
                        targetBand.getRasterHeight(),
                        targetProduct.getPreferredTileSize(),
                        ResolutionLevel.create(getModel(), targetLevel));
                Rectangle targetBounds = new Rectangle(imageLayout.getMinX(null), imageLayout.getMinY(null),
                                                       imageLayout.getWidth(null), imageLayout.getHeight(null));

                // the following transformation maps the target level image to level zero and then to the model,
                // which always is a map
                final AffineTransform i2mTarget = getModel().getImageToModelTransform(targetLevel);
                i2mTarget.concatenate(getModel().getModelToImageTransform(0));
                i2mTarget.concatenate(targetImageToMapTransform);

                ImageGeometry targetGeometry = new ImageGeometry(targetBounds,
                                                                 targetModelCrs,
                                                                 i2mTarget);
                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                hints.put(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

                Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
                try {
                    return reprojection.reproject(leveledSourceImage, sourceGeometry, targetGeometry,
                                                  targetBand.getNoDataValue(), resampling, hints, targetLevel,
                                                  tileSize);
                } catch (FactoryException | TransformException e) {
                    Debug.trace(e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private int getSourceLevel(MultiLevelModel sourceModel, int targetLevel) {
        int maxSourceLevel = sourceModel.getLevelCount() - 1;
        return maxSourceLevel < targetLevel ? maxSourceLevel : targetLevel;
    }

    private ProductData.UTC getSourceMeanTime() {
        ProductData.UTC startTime = sourceProduct.getStartTime();
        ProductData.UTC endTime = sourceProduct.getEndTime();
        ProductData.UTC meanTime;
        if (startTime != null && endTime != null) {
            meanTime = new ProductData.UTC(0.5 * (startTime.getMJD() + endTime.getMJD()));
        } else if (startTime != null) {
            meanTime = startTime;
        } else if (endTime != null) {
            meanTime = endTime;
        } else {
            meanTime = null;
        }
        return meanTime;
    }

    private void copyIndexCoding() {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = sourceProduct.getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            ProductUtils.copyIndexCoding(sourceIndexCoding, targetProduct);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ReprojectionOp.class);
        }
    }

    private GeoPos getCenterGeoPos(GeoCoding geoCoding, int width, int height) {
        final PixelPos centerPixelPos = new PixelPos(0.5 * width + 0.5,
                                                     0.5 * height + 0.5);
        return geoCoding.getGeoPos(centerPixelPos, null);
    }

    private CoordinateReferenceSystem createTargetCRS(GeoPos centerGeoPos) throws OperatorException {
        try {
            if (wktFile != null) {
                return CRS.parseWKT(FileUtils.readText(wktFile));
            }
            if (crs != null) {
                try {
                    return CRS.parseWKT(crs);
                } catch (FactoryException ignored) {
                    return createCRSFromCode(crs, centerGeoPos);
                }
            }
            if (collocationProduct != null && collocationProduct.getSceneGeoCoding() != null) {
                return collocationProduct.getSceneGeoCoding().getMapCRS();
            }
        } catch (FactoryException | IOException e) {
            throw new OperatorException(String.format("Target CRS could not be created: %s", e.getMessage()), e);
        }

        throw new OperatorException("Target CRS could not be created.");
    }

    static CoordinateReferenceSystem createCRSFromCode(String crsCode, GeoPos centerGeoPos) throws FactoryException {
        // prefix with EPSG, if there are only numbers
        if (crsCode.matches("[0-9]*")) {
            crsCode = "EPSG:" + crsCode;
        }
        // append center coordinates for AUTO code
        if (crsCode.matches("AUTO:[0-9]*")) {
            crsCode = String.format("%s,%s,%s", crsCode, centerGeoPos.lon, centerGeoPos.lat);
        }
        // force longitude==x-axis and latitude==y-axis
        return CRS.decode(crsCode, true);
    }

    protected void validateCrsParameters() {
        final String msgPattern = "Invalid target CRS specification.\nSpecify {0} one of the " +
                "''wktFile'', ''crs'' or ''collocationProduct'' parameters.";

        if (wktFile == null && crs == null && collocationProduct == null) {
            throw new OperatorException(MessageFormat.format(msgPattern, "at least"));
        }

        boolean crsDefined = false;
        final String exceptionMsg = MessageFormat.format(msgPattern, "only");
        if (wktFile != null) {
            crsDefined = true;
        }
        if (crs != null) {
            if (crsDefined) {
                throw new OperatorException(exceptionMsg);
            }
            crsDefined = true;
        }
        if (collocationProduct != null) {
            if (crsDefined) {
                throw new OperatorException(exceptionMsg);
            }
        }
    }

    private Interpolation getResampling(Band band) {
        int resampleType = getResampleType();
        if (!ProductData.isFloatingPointType(band.getDataType())) {
            resampleType = Interpolation.INTERP_NEAREST;
        }
        return Interpolation.getInstance(resampleType);
    }

    private int getResampleType() {
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
        if (getResampleType() == -1) {
            throw new OperatorException("Invalid resampling method: " + resamplingName);
        }
    }

    void validateReferencingParameters() {
        if (!((referencePixelX == null && referencePixelY == null && easting == null && northing == null)
                || (referencePixelX != null && referencePixelY != null && easting != null && northing != null))) {
            throw new OperatorException("Invalid referencing parameters: \n" +
                                                "'referencePixelX', 'referencePixelY', 'easting' and 'northing' have to be specified either all or not at all.");
        }
    }

    void validateTargetGridParameters() {
        if ((pixelSizeX != null && pixelSizeY == null) ||
                (pixelSizeX == null && pixelSizeY != null)) {
            throw new OperatorException("'pixelSizeX' and 'pixelSizeY' must be specified both or not at all.");
        }
    }

    /**
     * For SAR products check that geocoding has been performed
     */
    void validateSARProduct() {
        final MetadataElement root = sourceProduct.getMetadataRoot();
        if(root != null) {
            final MetadataElement absRoot = root.getElement("Abstracted_Metadata");
            if(absRoot != null) {
                boolean isRadar = absRoot.getAttributeDouble("radar_frequency", 99999) != 99999;
                if(isRadar && !(sourceProduct.getSceneGeoCoding() instanceof CrsGeoCoding)) {
                    throw new OperatorException("SAR products should be terrain corrected or ellipsoid corrected");
                }
            }
        }
    }

    private ImageGeometry createImageGeometry(CoordinateReferenceSystem targetCrs) {
        ImageGeometry imageGeometry;
        if (collocationProduct != null) {
            //todo adapt this to multi resolution products
            imageGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
        } else {
            imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                                                               pixelSizeX, pixelSizeY,
                                                               width, height, orientation,
                                                               easting, northing,
                                                               referencePixelX, referencePixelY);
            final AxisDirection targetAxisDirection = targetCrs.getCoordinateSystem().getAxis(1).getDirection();
            if (!AxisDirection.DISPLAY_DOWN.equals(targetAxisDirection)) {
                imageGeometry.changeYAxisDirection();
            }
        }
        return imageGeometry;
    }

    private void addDeltaBands() {

        final Band deltaLonBand = targetProduct.addBand("delta_lon_angular", "longitude - LON");
        deltaLonBand.setUnit("deg");
        deltaLonBand.setDescription("Delta between old longitude and new longitude in degree");
        deltaLonBand.setNoDataValueUsed(true);
        deltaLonBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLonBand.setImageInfo(createDeltaBandImageInfo(-0.015, +0.015));

        final Band deltaLatBand = targetProduct.addBand("delta_lat_angular", "latitude - LAT");
        deltaLatBand.setUnit("deg");
        deltaLatBand.setDescription("Delta between old latitude and new latitude in degree");
        deltaLatBand.setNoDataValueUsed(true);
        deltaLatBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLatBand.setImageInfo(createDeltaBandImageInfo(-0.01, +0.01));

        final Band deltaLonMetBand = targetProduct.addBand("delta_lon_metric",
                                                           "cos(rad(LAT)) * 6378137 * rad(longitude - LON)");
        deltaLonMetBand.setUnit("m");
        deltaLonMetBand.setDescription("Delta between old longitude and new longitude in meters");
        deltaLonMetBand.setNoDataValueUsed(true);
        deltaLonMetBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLonMetBand.setImageInfo(createDeltaBandImageInfo(-1500.0, +1500.0));

        final Band deltaLatMetBand = targetProduct.addBand("delta_lat_metric", "6378137 * rad(latitude - LAT)");
        deltaLatMetBand.setUnit("m");
        deltaLatMetBand.setDescription("Delta between old latitude and new latitude in meters");
        deltaLatMetBand.setNoDataValueUsed(true);
        deltaLatMetBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLatMetBand.setImageInfo(createDeltaBandImageInfo(-1000.0, +1000.0));
    }

    private ImageInfo createDeltaBandImageInfo(double p1, double p2) {
        return new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(p1, new Color(255, 0, 0)),
                new ColorPaletteDef.Point((p1 + p2) / 2, new Color(255, 255, 255)),
                new ColorPaletteDef.Point(p2, new Color(0, 0, 127)),
        }));
    }

    class ReprojectionSettingsProvider {

        ReprojectionSettingsProvider provider;

        ReprojectionSettingsProvider() {
        }

        ReprojectionSettingsProvider(ImageGeometry targetImageGeometry) {
            if (ProductUtils.areRastersEqualInSize(sourceProduct.getBands())) {
                provider = new DefaultReprojectionSettingsProvider(targetImageGeometry);
            } else {
                provider = new MultiResolutionReprojectionSettingsProvider();
            }
        }

        protected ReprojectionSettings getReprojectionSettings(RasterDataNode rasterDataNode) {
            return provider.getReprojectionSettings(rasterDataNode);
        }
    }

    private class DefaultReprojectionSettingsProvider extends ReprojectionSettingsProvider {

        ReprojectionSettings defaultReprojectionSettings;

        DefaultReprojectionSettingsProvider(ImageGeometry imageGeometry) {
            Band firstBand = sourceProduct.getBandGroup().get(0);
            MultiLevelModel sourceModel = firstBand.getMultiLevelModel();
            MultiLevelModel targetModel = targetProduct.createMultiLevelModel();
            Reproject reprojection = new Reproject(targetModel.getLevelCount());
            defaultReprojectionSettings = new ReprojectionSettings(null, sourceModel, imageGeometry);
            defaultReprojectionSettings.setTargetModel(targetModel);
            defaultReprojectionSettings.setReprojection(reprojection);
        }


        @Override
        protected ReprojectionSettings getReprojectionSettings(RasterDataNode rasterDataNode) {
            return defaultReprojectionSettings;
        }
    }

    private class MultiResolutionReprojectionSettingsProvider extends ReprojectionSettingsProvider {

        HashMap<String, ReprojectionSettings> reprojectionSettingsMap;

        MultiResolutionReprojectionSettingsProvider() {
            reprojectionSettingsMap = new HashMap<>();
            final ProductNodeGroup<Band> sourceBands = sourceProduct.getBandGroup();
            for (int i = 0; i < sourceBands.getNodeCount(); i++) {
                addReprojectionSettingsIfNecessary(sourceBands.get(i));
            }
            if (includeTiePointGrids) {
                final ProductNodeGroup<TiePointGrid> tiePointGridGroup = sourceProduct.getTiePointGridGroup();
                for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
                    addReprojectionSettingsIfNecessary(tiePointGridGroup.get(i));
                }
            }
        }

        @Override
        protected ReprojectionSettings getReprojectionSettings(RasterDataNode rasterDataNode) {
            return reprojectionSettingsMap.get(getKey(rasterDataNode));
        }

        private void addReprojectionSettingsIfNecessary(RasterDataNode rasterDataNode) {
            String key = getKey(rasterDataNode);
            if (!reprojectionSettingsMap.containsKey(key)) {
                GeoPos centerGeoPos =
                        getCenterGeoPos(rasterDataNode.getGeoCoding(),
                                        rasterDataNode.getRasterWidth(),
                                        rasterDataNode.getRasterHeight());
                CoordinateReferenceSystem targetCrs = createTargetCRS(centerGeoPos);
                ImageGeometry targetImageGeometry = ImageGeometry.createTargetGeometry(rasterDataNode, targetCrs,
                                                                                       pixelSizeX, pixelSizeY,
                                                                                       width, height,
                                                                                       orientation, easting,
                                                                                       northing, referencePixelX,
                                                                                       referencePixelY);
                AxisDirection targetAxisDirection = targetCrs.getCoordinateSystem().getAxis(1).getDirection();
                if (!AxisDirection.DISPLAY_DOWN.equals(targetAxisDirection)) {
                    targetImageGeometry.changeYAxisDirection();
                }
                Rectangle targetRect = targetImageGeometry.getImageRect();
                try {
                    CrsGeoCoding geoCoding = new CrsGeoCoding(targetImageGeometry.getMapCrs(),
                                                              targetRect,
                                                              targetImageGeometry.getImage2MapTransform());
                    MultiLevelModel sourceModel = rasterDataNode.getMultiLevelModel();
                    reprojectionSettingsMap.put(key, new ReprojectionSettings(geoCoding, sourceModel, targetImageGeometry));
                } catch (FactoryException | TransformException e) {
                    throw new OperatorException(e);
                }
            }
        }

        private String getKey(RasterDataNode rasterDataNode) {
            return rasterDataNode.getGeoCoding().toString() + " " + rasterDataNode.getRasterWidth() + " "
                    + rasterDataNode.getRasterHeight();
        }

    }

    private class ReprojectionSettings {

        private GeoCoding geoCoding;
        private MultiLevelModel sourceModel;
        private ImageGeometry imageGeometry;

        public void setTargetModel(MultiLevelModel targetModel) {
            this.targetModel = targetModel;
        }

        public void setReprojection(Reproject reprojection) {
            this.reprojection = reprojection;
        }

        private MultiLevelModel targetModel;
        private Reproject reprojection;

        ReprojectionSettings(GeoCoding geoCoding, MultiLevelModel sourceModel, ImageGeometry imageGeometry) {
            this.geoCoding = geoCoding;
            this.sourceModel = sourceModel;
            this.imageGeometry = imageGeometry;
        }

        public GeoCoding getGeoCoding() {
            return geoCoding;
        }

        public MultiLevelModel getSourceModel() {
            return sourceModel;
        }

        public ImageGeometry getImageGeometry() {
            return imageGeometry;
        }

        public MultiLevelModel getTargetModel() {
            return targetModel;
        }

        public Reproject getReprojection() {
            return reprojection;
        }

    }

}
