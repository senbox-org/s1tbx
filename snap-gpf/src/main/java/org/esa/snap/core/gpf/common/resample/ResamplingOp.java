package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.SceneFactory;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.transform.MathTransform2D;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.opengis.referencing.operation.MathTransform;

import javax.media.jai.ImageLayout;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "Resample",
        version = "2.0",
        authors = "Tonio Fincke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Resampling of a multi-size source product to a single-size target product.")
public class ResamplingOp extends Operator {

    private static final String NAME_EXTENSION = "resampled";

    @SourceProduct(description = "The source product which is to be resampled.", label = "Name")
    Product sourceProduct;

    @TargetProduct(description = "The resampled target product.")
    Product targetProduct;

    @Parameter(alias = "referenceBand", label = "Reference band", description = "The name of the reference band. " +
            "All other bands will be re-sampled to match its size and resolution. Either this or targetResolution" +
            "or targetWidth and targetHeight must be set.", rasterDataNodeType = Band.class)
    String referenceBandName;

    @Parameter(alias = "targetWidth", label = "Target width", description = "The width that all bands of the " +
            "target product shall have. If this is set, targetHeight must be set, too. " +
            "Either this and targetHeight or referenceBand or targetResolution must be set.")
    Integer targetWidth;

    @Parameter(alias = "targetHeight", label = "Target height", description = "The height that all bands of the " +
            "target product shall have. If this is set, targetWidth must be set, too. " +
            "Either this and targetWidth or referenceBand or targetResolution must be set.")
    Integer targetHeight;

    @Parameter(alias = "targetResolution", label = "Target resolution", description = "The resolution that all bands of the " +
            "target product shall have. The same value will be applied to scale image widths and heights. " +
            "Either this or referenceBand or targetwidth and targetHeight must be set.")
    Integer targetResolution;

    @Parameter(alias = "upsampling",
            label = "Upsampling method",
            description = "The method used for interpolation (upsampling to a finer resolution).",
            valueSet = {"NearestNeighbour", "Bilinear"}, //todo add bicubic interpolation
            defaultValue = "NearestNeighbour"
    )
    private String upsamplingMethod;

    @Parameter(alias = "downsampling",
            label = "Downsampling method",
            description = "The method used for aggregation (downsampling to a coarser resolution).",
            valueSet = {"First", "Min", "Max", "Mean", "Median"},
            defaultValue = "First")
    private String downsamplingMethod;

    @Parameter(alias = "flagDownsampling",
            label = "Flag downsampling method",
            description = "The method used for aggregation (downsampling to a coarser resolution) of flags.",
            valueSet = {"First", "FlagAnd", "FlagOr", "FlagMedianAnd", "FlagMedianOr"},
            defaultValue = "First")
    private String flagDownsamplingMethod;

    private InterpolationType interpolationType;
    private AggregationType aggregationType;
    private AggregationType flagAggregationType;

    private int referenceWidth;
    private int referenceHeight;
    private AffineTransform referenceImageToModelTransform;
    private MultiLevelModel referenceMultiLevelModel;

    @Override
    public void initialize() throws OperatorException {
        if (!allNodesHaveIdentitySceneTransform(sourceProduct)) {
            throw new OperatorException("Not all nodes have identity model-to-scene transform.");
        }
        setReferenceValues();
        setResamplingTypes();
        targetProduct = new Product(sourceProduct.getName() + "_" + NAME_EXTENSION, sourceProduct.getProductType(),
                                    referenceWidth, referenceHeight);
        resampleBands();
        resampleTiePointGrids();
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyIndexCodings(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        transferGeoCoding(targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        copyMasks(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
    }

    private void transferGeoCoding(Product targetProduct) {
        final Scene srcScene = SceneFactory.createScene(sourceProduct);
        final Scene destScene = SceneFactory.createScene(targetProduct);
        if (srcScene != null && destScene != null) {
            final GeoCoding sourceGeoCoding = srcScene.getGeoCoding();
            if (sourceGeoCoding == null) {
                destScene.setGeoCoding(null);
            } else if (sourceGeoCoding instanceof AbstractGeoCoding) {
                AbstractGeoCoding abstractGeoCoding = (AbstractGeoCoding) sourceGeoCoding;
                abstractGeoCoding.transferGeoCoding(srcScene, destScene, null);
            }
        }
    }

    //convenience method to increase speed for band maths type masks
    private static void copyMasks(Product sourceProduct, Product targetProduct) {
        final ProductNodeGroup<Mask> sourceMaskGroup = sourceProduct.getMaskGroup();
        for (int i = 0; i < sourceMaskGroup.getNodeCount(); i++) {
            final Mask mask = sourceMaskGroup.get(i);
            final Mask.ImageType imageType = mask.getImageType();
            if (imageType.getName().equals(Mask.BandMathsType.TYPE_NAME)) {
                String expression = Mask.BandMathsType.getExpression(mask);
                final Mask targetMask = Mask.BandMathsType.create(mask.getName(), mask.getDescription(),
                                                                  targetProduct.getSceneRasterWidth(),
                                                                  targetProduct.getSceneRasterHeight(), expression,
                                                                  mask.getImageColor(), mask.getImageTransparency());
                targetProduct.addMask(targetMask);
            } else if (imageType.canTransferMask(mask, targetProduct)) {
                imageType.transferMask(mask, targetProduct);
            }
        }
    }

    public static boolean canBeApplied(Product product) {
        return allNodesHaveIdentitySceneTransform(product);
    }

    static boolean allNodesHaveIdentitySceneTransform(Product product) {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            if (bandGroup.get(i).getModelToSceneTransform() != MathTransform2D.IDENTITY) {
                return false;
            }
        }
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            if (tiePointGridGroup.get(i).getModelToSceneTransform() != MathTransform2D.IDENTITY) {
                return false;
            }
        }
        return true;
    }

    private void resampleTiePointGrids() {
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = sourceProduct.getTiePointGridGroup();
        double scaledReferenceOffsetX = referenceImageToModelTransform.getTranslateX() / referenceImageToModelTransform.getScaleX();
        double scaledReferenceOffsetY = referenceImageToModelTransform.getTranslateY() / referenceImageToModelTransform.getScaleY();
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            final TiePointGrid grid = tiePointGridGroup.get(i);
            AffineTransform transform;
            try {
                transform = new AffineTransform(referenceImageToModelTransform.createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException("Cannot resample: " + e.getMessage());
            }
            final AffineTransform gridTransform = grid.getImageToModelTransform();
            transform.concatenate(gridTransform);
            if (Math.abs(transform.getScaleX() - 1.0) > 1e-8 || Math.abs(transform.getScaleY() - 1.0) > 1e-8 ||
                    scaledReferenceOffsetX != 0 || scaledReferenceOffsetY != 0) {
                double subSamplingX = grid.getSubSamplingX() * transform.getScaleX();
                double subSamplingY = grid.getSubSamplingY() * transform.getScaleY();
                double offsetX = (grid.getOffsetX() * transform.getScaleX()) - scaledReferenceOffsetX;
                double offsetY = (grid.getOffsetY() * transform.getScaleY()) - scaledReferenceOffsetY;
                final TiePointGrid resampledGrid = new TiePointGrid(grid.getName(), grid.getGridWidth(), grid.getGridHeight(),
                        offsetX, offsetY, subSamplingX, subSamplingY, grid.getTiePoints());
                targetProduct.addTiePointGrid(resampledGrid);
                ProductUtils.copyRasterDataNodeProperties(grid, resampledGrid);
            } else {
                ProductUtils.copyTiePointGrid(grid.getName(), sourceProduct, targetProduct);
            }
        }
    }

    private void resampleBands() {
        final ProductNodeGroup<Band> sourceBands = sourceProduct.getBandGroup();
        MultiLevelModel targetMultiLevelModel;
        if (sourceProduct.getSceneGeoCoding() instanceof CrsGeoCoding) {
            targetMultiLevelModel = referenceMultiLevelModel;
        } else {
            targetMultiLevelModel = new DefaultMultiLevelModel(new AffineTransform(), referenceWidth, referenceHeight);
        }
        for (int i = 0; i < sourceBands.getNodeCount(); i++) {
            Band sourceBand = sourceBands.get(i);
            final int dataBufferType = ImageManager.getDataBufferType(sourceBand.getDataType());
            Band targetBand;
            AffineTransform sourceTransform = sourceBand.getImageToModelTransform();
            final boolean isVirtualBand = sourceBand instanceof VirtualBand;
            if ((sourceBand.getRasterWidth() != referenceWidth || sourceBand.getRasterHeight() != referenceHeight) && !isVirtualBand) {
                targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), referenceWidth, referenceHeight);
                MultiLevelImage targetImage = sourceBand.getSourceImage();
                MultiLevelImage sourceImage = sourceBand.getSourceImage();
                if (referenceWidth <= sourceBand.getRasterWidth() && referenceHeight <= sourceBand.getRasterHeight()) {
                    targetImage = createAggregatedImage(sourceImage, dataBufferType, sourceBand.getNoDataValue(),
                                                        sourceBand.isFlagBand(), referenceMultiLevelModel,
                                                        referenceWidth, referenceHeight);
                } else if (referenceWidth >= sourceBand.getRasterWidth() && referenceHeight >= sourceBand.getRasterHeight()) {
                    targetImage = createInterpolatedImage(sourceImage, dataBufferType, sourceBand.getNoDataValue(),
                                                          referenceMultiLevelModel, referenceWidth, referenceHeight);
                } else if (referenceWidth < sourceBand.getRasterWidth()) {
                    AffineTransform intermediateTransform = new AffineTransform(
                            referenceImageToModelTransform.getScaleX(), referenceImageToModelTransform.getShearX(), sourceTransform.getShearY(),
                            sourceTransform.getScaleY(), referenceImageToModelTransform.getTranslateX(), sourceTransform.getTranslateY());
                    final DefaultMultiLevelModel intermediateMultiLevelModel =
                            new DefaultMultiLevelModel(intermediateTransform, referenceWidth, sourceBand.getRasterHeight());
                    targetImage = createAggregatedImage(targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                        sourceBand.isFlagBand(), intermediateMultiLevelModel,
                                                        referenceWidth, sourceBand.getRasterHeight());
                    targetImage = createInterpolatedImage(targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                          referenceMultiLevelModel, referenceWidth, referenceHeight);
                } else if (referenceHeight < sourceBand.getRasterHeight()) {
                    AffineTransform intermediateTransform = new AffineTransform(
                            sourceTransform.getScaleX(), sourceTransform.getShearX(), referenceImageToModelTransform.getShearY(),
                            referenceImageToModelTransform.getScaleY(), sourceTransform.getTranslateX(), referenceImageToModelTransform.getTranslateY());
                    final DefaultMultiLevelModel intermediateMultiLevelModel =
                            new DefaultMultiLevelModel(intermediateTransform, sourceBand.getRasterWidth(), referenceHeight);
                    targetImage = createAggregatedImage(targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                        sourceBand.isFlagBand(), intermediateMultiLevelModel,
                                                        sourceBand.getRasterWidth(), referenceHeight);
                    targetImage = createInterpolatedImage(targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                          referenceMultiLevelModel, referenceWidth, referenceHeight);
                }
                targetBand.setSourceImage(adjustImageToModelTransform(targetImage, targetMultiLevelModel));
                targetProduct.addBand(targetBand);
            } else {
                if (isVirtualBand) {
                    targetBand = ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) sourceBand, sourceBand.getName());
                } else {
                    targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, false);
                    targetBand.setSourceImage(adjustImageToModelTransform(sourceBand.getSourceImage(), targetMultiLevelModel));
                }
            }
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        }
    }

    private RenderedImage adjustImageToModelTransform(final MultiLevelImage image, MultiLevelModel model) {
        MultiLevelModel actualModel = model;
        if (model.getLevelCount() > image.getModel().getLevelCount()) {
            actualModel = new DefaultMultiLevelModel(image.getModel().getLevelCount(), model.getImageToModelTransform(0),
                                                     image.getWidth(), image.getHeight());
        }
        final AbstractMultiLevelSource source = new AbstractMultiLevelSource(actualModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return image.getImage(level);
            }
        };
        return new DefaultMultiLevelImage(source);
    }

    private MultiLevelImage createInterpolatedImage(MultiLevelImage sourceImage, int dataType, double noDataValue,
                                                    MultiLevelModel referenceModel, int targetWidth, int targetHeight) {
        if (interpolationType == null) {
            throw new OperatorException("Invalid upsampling method");
        }
        float[] scalings = new float[2];
        scalings[0] = sourceImage.getWidth() / referenceWidth;
        scalings[1] = sourceImage.getHeight() / referenceHeight;
        final AbstractMultiLevelSource source = new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int targetLevel) {
                final MultiLevelModel targetModel = getModel();
                final double targetScale = targetModel.getScale(targetLevel);
                final MultiLevelModel sourceModel = sourceImage.getModel();
                final int sourceLevel = findBestSourceLevel(targetScale, sourceModel, scalings);
                final RenderedImage sourceLevelImage = sourceImage.getImage(sourceLevel);
                final Dimension tileSize = JAIUtils.computePreferredTileSize(targetWidth, targetHeight, 1);
                final ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), targetLevel);
                final AffineTransform sourceImageToModelTransform = sourceModel.getImageToModelTransform(sourceLevel);
                final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(dataType, null,
                                                                                           referenceWidth,
                                                                                           referenceHeight,
                                                                                           tileSize,
                                                                                           resolutionLevel);
                try {
                    return new InterpolatedOpImage(sourceLevelImage, imageLayout, noDataValue, dataType, interpolationType,
                                                   sourceImageToModelTransform,
                                                   targetModel.getImageToModelTransform(targetLevel));
                } catch (NoninvertibleTransformException e) {
                    throw new OperatorException("Could not downsample band image");
                }
            }
        };
        return new DefaultMultiLevelImage(source);
    }

    private MultiLevelImage createAggregatedImage(MultiLevelImage sourceImage, int dataType, double noDataValue,
                                                  boolean isFlagBand, MultiLevelModel referenceModel,
                                                  int targetWidth, int targetHeight) {
        AggregationType type;
        if (isFlagBand) {
            if (flagAggregationType == null) {
                throw new OperatorException("Invalid flag downsampling method");
            }
            type = flagAggregationType;
        } else {
            if (aggregationType == null) {
                throw new OperatorException("Invalid downsampling method");
            }
            type = aggregationType;
        }
        float[] scalings = new float[2];
        scalings[0] = sourceImage.getWidth() / referenceWidth;
        scalings[1] = sourceImage.getHeight() / referenceHeight;
        final AbstractMultiLevelSource source = new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int targetLevel) {
                final MultiLevelModel targetModel = getModel();
                final double targetScale = targetModel.getScale(targetLevel);
                final MultiLevelModel sourceModel = sourceImage.getModel();
                final int sourceLevel = findBestSourceLevel(targetScale, sourceModel, scalings);
                final RenderedImage sourceLevelImage = sourceImage.getImage(sourceLevel);
                final Dimension tileSize = JAIUtils.computePreferredTileSize(targetWidth, targetHeight, 1);
                final ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), targetLevel);
                final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(dataType, null,
                                                                                           referenceWidth,
                                                                                           referenceHeight,
                                                                                           tileSize,
                                                                                           resolutionLevel);
                try {
                    return new AggregatedOpImage(sourceLevelImage, imageLayout, noDataValue, type, dataType,
                                                 sourceModel.getImageToModelTransform(sourceLevel),
                                                 targetModel.getImageToModelTransform(targetLevel));
                } catch (NoninvertibleTransformException e) {
                    throw new OperatorException("Could not downsample band image");
                }
            }
        };
        return new DefaultMultiLevelImage(source);
    }

    //todo remove code duplication with sourceimagescaler - tf 20160314
    private int findBestSourceLevel(double targetScale, MultiLevelModel sourceModel, float[] scalings) {
            /*
             * Find the source level such that the final scaling factor is the closest to 1.0
             *
             * Example : When scaling a 20m resolution image to 10m resolution,
             * when generating the level 1 image of the scaled image, we prefer using the source image data at level 0,
             * since it will provide a better resolution than upscaling by 2 the source image data at level 1.
             *
             * We can't find the best on both X and Y directions if scaling factors are arbitrary, so we limit the
             * search algorithm by optimizing only for the X direction.
             * This will cover the most frequent use case where scaling factors in both directions are equal.
             */
        float optimizedScaling = 0;
        int optimizedSourceLevel = 0;
        boolean initialized = false;
        for (int sourceLevel = 0; sourceLevel < sourceModel.getLevelCount(); sourceLevel++) {
            final double sourceScale = sourceModel.getScale(sourceLevel);
            final float scaleRatio = (float) (sourceScale / targetScale);
            if (!initialized) {
                optimizedScaling = scalings[0] * scaleRatio;
                optimizedSourceLevel = sourceLevel;
                initialized = true;
            }
            else {
                // We want to be as close to 1.0 as possible
                if (Math.abs(1 - scalings[0] * scaleRatio) < Math.abs(1 - optimizedScaling)) {
                    optimizedScaling = scalings[0] * scaleRatio;
                    optimizedSourceLevel = sourceLevel;
                }
            }
        }
        return optimizedSourceLevel;
    }

    private void setReferenceValues() {
        validateReferenceSettings();
        if (referenceBandName != null) {
            final Band referenceBand = sourceProduct.getBand(referenceBandName);
            referenceWidth = referenceBand.getRasterWidth();
            referenceHeight = referenceBand.getRasterHeight();
            referenceImageToModelTransform = referenceBand.getImageToModelTransform();
            referenceMultiLevelModel = referenceBand.getMultiLevelModel();
        } else if (targetWidth != null && targetHeight != null) {
            referenceWidth = targetWidth;
            referenceHeight = targetHeight;
            double scaleX = (double) sourceProduct.getSceneRasterWidth() / referenceWidth;
            double scaleY = (double) sourceProduct.getSceneRasterHeight() / referenceHeight;
            final MathTransform imageToMapTransform = sourceProduct.getSceneGeoCoding().getImageToMapTransform();
            if (imageToMapTransform instanceof AffineTransform) {
                AffineTransform mapTransform = (AffineTransform) imageToMapTransform;
                referenceImageToModelTransform =
                        new AffineTransform(scaleX * mapTransform.getScaleX(), 0, 0, scaleY * mapTransform.getScaleY(),
                                            mapTransform.getTranslateX(), mapTransform.getTranslateY());
            } else {
                referenceImageToModelTransform = new AffineTransform(scaleX, 0, 0, scaleY, 0, 0);
            }
            referenceMultiLevelModel = new DefaultMultiLevelModel(referenceImageToModelTransform, referenceWidth, referenceHeight);
        } else {
            final MathTransform imageToMapTransform = sourceProduct.getSceneGeoCoding().getImageToMapTransform();
            if (imageToMapTransform instanceof AffineTransform) {
                AffineTransform mapTransform = (AffineTransform) imageToMapTransform;
                referenceWidth = (int) (sourceProduct.getSceneRasterWidth()  * Math.abs(mapTransform.getScaleX()) / targetResolution);
                referenceHeight = (int) (sourceProduct.getSceneRasterHeight()  * Math.abs(mapTransform.getScaleY()) / targetResolution);
                referenceImageToModelTransform = new AffineTransform(targetResolution, 0, 0, -targetResolution,
                                                                     mapTransform.getTranslateX(), mapTransform.getTranslateY());
                referenceMultiLevelModel = new DefaultMultiLevelModel(referenceImageToModelTransform, referenceWidth, referenceHeight);
            } else {
                throw new OperatorException("Use of target resolution parameter is not possible for this source product.");
            }
        }
    }

    private void validateReferenceSettings() {
        if (referenceBandName == null && targetWidth == null && targetHeight == null && targetResolution == null) {
            throw new OperatorException("Either referenceBandName or targetResolution or targetWidth " +
                                                "together with targetHeight must be set.");
        }
        if (referenceBandName != null && (targetWidth != null || targetHeight != null || targetResolution != null)) {
            throw new OperatorException("If referenceBandName is set, targetWidth, targetHeight, " +
                                                "and targetResolution must not be set");
        }
        if (targetResolution != null && (targetWidth != null || targetHeight != null)) {
            throw new OperatorException("If targetResolution is set, targetWidth, targetHeight, " +
                                                "and referenceBandName must not be set");
        }
        if (targetWidth != null && targetHeight == null) {
            throw new OperatorException("If targetWidth is set, targetHeight must be set, too.");
        }
        if (targetWidth == null && targetHeight != null) {
            throw new OperatorException("If targetHeight is set, targetWidth must be set, too.");
        }
        if (targetResolution != null && !(sourceProduct.getSceneGeoCoding() instanceof CrsGeoCoding)) {
            throw new OperatorException("Use of targetResolution is only possible for products with crs geo-coding.");
        }
    }

    private void setResamplingTypes() {
        interpolationType = getInterpolationType(upsamplingMethod);
        aggregationType = getAggregationType(downsamplingMethod);
        flagAggregationType = getAggregationType(flagDownsamplingMethod);
    }

    private InterpolationType getInterpolationType(String interpolationMethod) {
        switch (interpolationMethod) {
            case "NearestNeighbour":
                return InterpolationType.Nearest;
            case "Bilinear":
                return InterpolationType.Bilinear;
        }
        return null;
    }

    private AggregationType getAggregationType(String aggregationMethod) {
        switch (aggregationMethod) {
            case "Mean":
                return AggregationType.Mean;
            case "Median":
                return AggregationType.Median;
            case "Min":
                return AggregationType.Min;
            case "Max":
                return AggregationType.Max;
            case "First":
                return AggregationType.First;
            case "FlagAnd":
                return AggregationType.FlagAnd;
            case "FlagOr":
                return AggregationType.FlagOr;
            case "FlagMedianAnd":
                return AggregationType.FlagMedianAnd;
            case "FlagMedianOr":
                return AggregationType.FlagMedianOr;
        }
        return null;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ResamplingOp.class);
        }
    }

}