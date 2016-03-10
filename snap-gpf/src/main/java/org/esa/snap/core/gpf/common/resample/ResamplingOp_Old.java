package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.SceneFactory;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.transform.MathTransform2D;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.Interpolation;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;

/**
 * The resampling operator creates a product in which every {@code RasterDataNode} is of the same size and resolution.
 * Every {@code RasterDataNode} will return {@literal SceneTransform.IDENTITY} for calls of
 * {@link RasterDataNode#getSceneToModelTransform()} and {@link RasterDataNode#getModelToSceneTransform()} and the same
 * transformations for calls of {@link RasterDataNode#getImageToModelTransform()}.
 *
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "Resample_Old",
        version = "1.0",
        authors = "Tonio Fincke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Resampling of a multi-size source product to a single-size target product.",
        internal = true)
public class ResamplingOp_Old extends Operator {

    private static final String NAME_EXTENSION = "resampled";

    @SourceProduct(description = "The source product which is to be resampled.", label = "Name")
    Product sourceProduct;

    @TargetProduct(description = "The resampled target product.")
    Product targetProduct;

    //todo also allow to set a target size/resolution explicitly
    @Parameter(alias = "Reference Band", description = "The name of the reference band. " +
            "All other bands will be re-sampled to match its size and resolution.",
            rasterDataNodeType = Band.class,
            notEmpty = true
    )
    String referenceBandName;

    @Parameter(alias = "interpolation",
            label = "Interpolation Method",
            description = "The method used for interpolation (upsampling to a finer resolution).",
            valueSet = {"Nearest", "Bilinear", "Bicubic"},
            defaultValue = "Nearest"
    )
    private String interpolationMethod;

    @Parameter(alias = "aggregation",
            label = "Aggregation Method",
            description = "The method used for aggregation (downsampling to a coarser resolution).",
            valueSet = {"First", "And", "Or", "Mean", "Median"},
            defaultValue = "First")
    private String aggregationMethod;

    @Parameter(alias = "flagAggregation",
            label = "Flag Aggregation Method",
            description = "The method used for aggregation (downsampling to a coarser resolution) of flags.",
            valueSet = {"First", "And", "Or", "Median-And", "Median-Or"},
            defaultValue = "First")
    private String flagAggregationMethod;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isMultiSizeProduct()) {
            targetProduct = sourceProduct;
            return;
        }
        if (!allNodesHaveIdentitySceneTransform(sourceProduct)) {
            throw new OperatorException("Not all nodes have identity model-to-scene transform.");
        }
        if (!allScalingsAreIntDivisible(sourceProduct)) {
            throw new OperatorException("Not all band scalings are int divisible.");
        }
        final Band referenceBand = sourceProduct.getBand(referenceBandName);
        Assert.notNull(referenceBand);
        if (!allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(sourceProduct, referenceBand)) {
            throw new OperatorException("Bands must be either aggregated, interpolated or left as is.");
        }
        validateInterpolationParameter();
        final int referenceWidth = referenceBand.getRasterWidth();
        final int referenceHeight = referenceBand.getRasterHeight();
        targetProduct = new Product(sourceProduct.getName() + "_" + NAME_EXTENSION, sourceProduct.getProductType(),
                                    referenceWidth, referenceHeight);
        resampleBands(referenceBand);
        resampleTiePointGrids(referenceBand);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyIndexCodings(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        transferGeoCoding(referenceBand, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
    }

    private static void transferGeoCoding(Band referenceBand, Product targetProduct) {
        final Scene srcScene = SceneFactory.createScene(referenceBand);
        final Scene destScene = SceneFactory.createScene(targetProduct);
        if (srcScene != null && destScene != null) {
            srcScene.transferGeoCodingTo(destScene, null);
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
        return allNodesHaveIdentitySceneTransform(product) && allScalingsAreIntDivisible(product);
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

    static boolean allScalingsAreIntDivisible(Product product) {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
        AffineTransform referenceModelToImageTransform;
        if (bandGroup.getNodeCount() > 0) {
            try {
                referenceModelToImageTransform = new AffineTransform(bandGroup.get(0).getImageToModelTransform().createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException(e.getMessage());
            }
        } else if (tiePointGridGroup.getNodeCount() > 0) {
            try {
                referenceModelToImageTransform = new AffineTransform(tiePointGridGroup.get(0).getImageToModelTransform().createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException(e.getMessage());
            }
        } else {
            return true;
        }
        double modelToImageScaleX = referenceModelToImageTransform.getScaleX();
        double modelToImageScaleY = referenceModelToImageTransform.getScaleY();
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            if (!isIntDivisible(bandGroup.get(i), modelToImageScaleX, modelToImageScaleY)) {
                return false;
            }
        }
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            if (!isIntDivisible(tiePointGridGroup.get(i), modelToImageScaleX, modelToImageScaleY)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIntDivisible(RasterDataNode node, double modelToImageScaleX, double modelToImageScaleY) {
        final AffineTransform imageToModelTransform = node.getImageToModelTransform();
        final double scaleX = imageToModelTransform.getScaleX() * modelToImageScaleX;
        final double scaleY = imageToModelTransform.getScaleY() * modelToImageScaleY;
        return isIntDivisible(scaleX) && isIntDivisible(scaleY);
    }

    private static boolean isIntDivisible(double value) {
        if (value < 1) {
            value = 1 / value;
        }
        return (value - Math.floor(value)) < 1e-10;
    }

    static boolean allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(Product product, RasterDataNode referenceNode) {
        final int referenceWidth = referenceNode.getRasterWidth();
        final int referenceHeight = referenceNode.getRasterHeight();
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            final int rasterWidth = bandGroup.get(i).getRasterWidth();
            final int rasterHeight = bandGroup.get(i).getRasterHeight();
            if (rasterWidth == referenceWidth && rasterHeight == referenceHeight) {
                continue;
            } else if (rasterWidth <= referenceWidth && rasterHeight <= referenceHeight) {
                continue;
            } else if (rasterWidth >= referenceWidth && rasterHeight >= referenceHeight) {
                continue;
            }
            return false;
        }
        return true;
    }

    private void resampleTiePointGrids(RasterDataNode referenceNode) {
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = sourceProduct.getTiePointGridGroup();
        final AffineTransform referenceTransform = referenceNode.getImageToModelTransform();
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            final TiePointGrid grid = tiePointGridGroup.get(i);
            AffineTransform transform;
            try {
                transform = new AffineTransform(referenceTransform.createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException("Cannot resample: " + e.getMessage());
            }
            final AffineTransform gridTransform = grid.getImageToModelTransform();
            transform.concatenate(gridTransform);
            if (Math.abs(transform.getScaleX() - 1.0) > 1e-8 || Math.abs(transform.getScaleY() - 1.0) > 1e-8 ||
                    referenceTransform.getTranslateX() != 0 || referenceTransform.getTranslateY() != 0) {
                double subSamplingX = grid.getSubSamplingX() * transform.getScaleX();
                double subSamplingY = grid.getSubSamplingY() * transform.getScaleY();
                double offsetX = (grid.getOffsetX() * transform.getScaleX()) - (referenceTransform.getTranslateX() / gridTransform.getScaleX());
                double offsetY = (grid.getOffsetY() * transform.getScaleY()) - (referenceTransform.getTranslateY() / gridTransform.getScaleY());

                final TiePointGrid resampledGrid = new TiePointGrid(grid.getName(), grid.getGridWidth(), grid.getGridHeight(),
                                                                    offsetX, offsetY,
                                                                    subSamplingX, subSamplingY, grid.getTiePoints());
                targetProduct.addTiePointGrid(resampledGrid);
                ProductUtils.copyRasterDataNodeProperties(grid, resampledGrid);
            } else {
                ProductUtils.copyTiePointGrid(grid.getName(), sourceProduct, targetProduct);
            }
        }
    }

    private void resampleBands(RasterDataNode referenceNode) {
        final ProductNodeGroup<Band> sourceBands = sourceProduct.getBandGroup();
        final int referenceWidth = referenceNode.getRasterWidth();
        final int referenceHeight = referenceNode.getRasterHeight();
        Dimension referenceSize = referenceNode.getRasterSize();
        MultiLevelModel multiLevelModel;
        if (referenceNode.getGeoCoding() instanceof CrsGeoCoding) {
            multiLevelModel = referenceNode.getMultiLevelModel();
        } else {
            multiLevelModel = new DefaultMultiLevelModel(new AffineTransform(), referenceWidth, referenceHeight);
        }
        for (int i = 0; i < sourceBands.getNodeCount(); i++) {
            Band sourceBand = sourceBands.get(i);
            Band targetBand;
            if (!sourceBand.getRasterSize().equals(referenceSize)) {
                //todo consider case when band width is smaller than reference width but band height is larger than reference height or vice versa
                targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), referenceWidth, referenceHeight);
                if (sourceBand.getRasterWidth() < referenceWidth && sourceBand.getRasterHeight() < referenceHeight) {
                    final MultiLevelImage interpolatedImage = createInterpolatedImage(sourceBand, referenceNode);
                    final RenderedImage image = adjustImageToModelTransform(interpolatedImage, multiLevelModel);
                    targetBand.setSourceImage(image);
                } else {
                    final MultiLevelImage aggregatedImage = createAggregatedImage(sourceBand, referenceNode);
                    final RenderedImage image = adjustImageToModelTransform(aggregatedImage, multiLevelModel);
                    targetBand.setSourceImage(image);
                }
                targetProduct.addBand(targetBand);
            } else {
                targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, false);
                final RenderedImage image = adjustImageToModelTransform(sourceBand.getSourceImage(), multiLevelModel);
                targetBand.setSourceImage(image);

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

    private MultiLevelImage createInterpolatedImage(Band sourceBand, final RasterDataNode referenceNode) {
        Interpolation interpolation;
        if (sourceBand.isFlagBand() || sourceBand.isIndexBand()) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        } else {
            interpolation = getInterpolation();
        }
        return Resample.createInterpolatedMultiLevelImage(sourceBand, referenceNode, interpolation);
    }

    private MultiLevelImage createAggregatedImage(Band sourceBand, final RasterDataNode referenceNode) {
        final Resample.Type aggregationType = getAggregationType(aggregationMethod);
        final Resample.Type flagAggregationType = getAggregationType(flagAggregationMethod);
        return Resample.createAggregatedMultiLevelImage(sourceBand, referenceNode, aggregationType, flagAggregationType);
    }

    private Resample.Type getAggregationType(String method) {
        if ("And".equalsIgnoreCase(method)) {
            return Resample.Type.MIN;
        } else if ("Or".equalsIgnoreCase(method)) {
            return Resample.Type.MAX;
        } else if ("Median".equalsIgnoreCase(method)) {
            return Resample.Type.MEDIAN;
        } else if ("Mean".equalsIgnoreCase(method)) {
            return Resample.Type.MEAN;
        } else if ("Median-And".equalsIgnoreCase(method)) {
            return Resample.Type.MIN_MEDIAN;
        } else if ("Median-Or".equalsIgnoreCase(method)) {
            return Resample.Type.MAX_MEDIAN;
        } else {
            return Resample.Type.FIRST;
        }
    }

    private Interpolation getInterpolation() {
        int interpolation = getInterpolationType();
        return Interpolation.getInstance(interpolation);
    }

    private int getInterpolationType() {
        final int interpolationType;
        if ("Nearest".equalsIgnoreCase(interpolationMethod)) {
            interpolationType = Interpolation.INTERP_NEAREST;
        } else if ("Bilinear".equalsIgnoreCase(interpolationMethod)) {
            interpolationType = Interpolation.INTERP_BILINEAR;
        } else if ("Bicubic".equalsIgnoreCase(interpolationMethod)) {
            interpolationType = Interpolation.INTERP_BICUBIC;
        } else {
            interpolationType = -1;
        }
        return interpolationType;
    }

    //todo this method has been copied from ReprojectionOp. Find a common place? - tf 20160210
    void validateInterpolationParameter() {
        if (getInterpolationType() == -1) {
            throw new OperatorException("Invalid resampling method: " + interpolationMethod);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ResamplingOp_Old.class);
        }
    }
}
