package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
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
@OperatorMetadata(alias = "Resample",
        version = "1.0",
        authors = "Tonio Fincke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Resampling of a multi-size source product to a single-size target product.",
        internal = true)
public class ResamplingOp extends Operator {

    private static final String NAME_EXTENSION = "resampled";

    @SourceProduct(alias = "source", description = "The source product which is to be resampled.")
    Product sourceProduct;

    @TargetProduct(description = "The resampled target product.")
    Product targetProduct;

    //todo also allow to set a target size/resolution explicitly
    @Parameter(description = "The name of the reference raster data node. " +
            "All other bands will be re-sampled to match its size and resolution.")
    String referenceNodeName;

    @Parameter(alias = "interpolation",
            label = "Interpolation Method",
            description = "The method used for interpolation (sampling to a finer resolution).",
            valueSet = {"Nearest", "Bilinear", "Bicubic"},
            defaultValue = "Nearest")
    private String interpolationMethod;

    @Parameter(alias = "aggregation",
            label = "Aggregation Method",
            description = "The method used for aggregation (sampling to a coarser resolution).",
            valueSet = {"First", "Min", "Max", "Mean", "Median"},
            defaultValue = "First")
    private String aggregationMethod;

    @Parameter(alias = "flagAggregation",
            label = "Flag aggregation Method",
            description = "The method used for aggregation (sampling to a coarser resolution) of flags.",
            valueSet = {"First", "Min", "Max", "MinMedian", "MaxMedian"},
            defaultValue = "First")
    private String flagAggregationMethod;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isMultiSizeProduct()) {
            targetProduct = sourceProduct;
            return;
        }
        if (!allNodesHaveIdentitySceneTransform(sourceProduct)) {
            throw new OperatorException("Not all nodes have identity model to scene transform.");
        }
        if (!allScalingsAreIntDivisible(sourceProduct)) {
            throw new OperatorException("Not all band scalings are int divisible.");
        }
        final RasterDataNode referenceNode = sourceProduct.getRasterDataNode(referenceNodeName);
        Assert.notNull(referenceNode);
        if (!allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(sourceProduct, referenceNode)) {
            throw new OperatorException("Bands must be either aggregated, interpolated or left as is.");
        }
        validateInterpolationParameter();
        final int referenceWidth = referenceNode.getRasterWidth();
        final int referenceHeight = referenceNode.getRasterHeight();
        targetProduct = new Product(sourceProduct.getName() + "_" + NAME_EXTENSION, sourceProduct.getProductType(),
                                    referenceWidth, referenceHeight);
        resampleBands(referenceNode);
        resampleTiePointGrids(referenceNode);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyIndexCodings(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
    }

    public static boolean canBeApplied(Product product, RasterDataNode node) {
        return allNodesHaveIdentitySceneTransform(product) && allScalingsAreIntDivisible(product)
                && allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(product, node);
    }

    private static boolean allNodesHaveIdentitySceneTransform(Product product) {
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

    private static boolean allScalingsAreIntDivisible(Product product) {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
        AffineTransform referenceModelToImageTransform;
        if (bandGroup.getNodeCount() > 0 ) {
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
            final AffineTransform imageToModelTransform = bandGroup.get(i).getImageToModelTransform();
            final double scaleX = imageToModelTransform.getScaleX() * modelToImageScaleX;
            final double scaleY = imageToModelTransform.getScaleY() * modelToImageScaleY;
            if (!isIntDivisible(scaleX) || !isIntDivisible(scaleY)) {
                return false;
            }
        }
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            final AffineTransform imageToModelTransform = bandGroup.get(i).getImageToModelTransform();
            final double scaleX = imageToModelTransform.getScaleX() * modelToImageScaleX;
            final double scaleY = imageToModelTransform.getScaleY() * modelToImageScaleY;
            if (!isIntDivisible(scaleX) || !isIntDivisible(scaleY)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allBandsMustBeEitherInterpolatedAggregatedOrLeftAsIs(Product product, RasterDataNode referenceNode) {
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

    private static boolean isIntDivisible(double value) {
        if (value < 1) {
            value /= 1;
        }
        return (value - Math.floor(value)) < 1e-10;
    }

    private void resampleTiePointGrids(RasterDataNode referenceNode) {
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = sourceProduct.getTiePointGridGroup();
        AffineTransform transform;
        try {
            transform = new AffineTransform(referenceNode.getImageToModelTransform().createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new OperatorException("Cannot resample: " + e.getMessage());
        }
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            final TiePointGrid grid = tiePointGridGroup.get(i);
            transform.concatenate(grid.getImageToModelTransform());
            if (Math.abs(transform.getScaleX() - 1.0) > 1e-8 || Math.abs(transform.getScaleY() - 1.0) > 1e-8) {
                double subSamplingX = grid.getSubSamplingX() * transform.getScaleX();
                double subSamplingY = grid.getSubSamplingY() * transform.getScaleY();
                double offsetX = grid.getOffsetX() * transform.getScaleX();
                double offsetY = grid.getOffsetY() * transform.getScaleY();
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
        final DefaultMultiLevelModel multiLevelModel = new DefaultMultiLevelModel(new AffineTransform(),
                                                                                  referenceWidth, referenceHeight);
        for (int i = 0; i < sourceBands.getNodeCount(); i++) {
            Band sourceBand = sourceBands.get(i);
            Band targetBand;
            if (!sourceBand.getRasterSize().equals(referenceSize)) {
                //todo consider case when band width is smaller than reference width but band height is larger than reference height or vice versa
                targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), referenceWidth, referenceHeight);
                if (sourceBand.getRasterWidth() < referenceWidth && sourceBand.getRasterHeight() < referenceHeight) {
                    final MultiLevelImage interpolatedImage = createInterpolatedImage(sourceBand, referenceNode);
                    final RenderedImage image = createImageWithIdentityImageToModelTransform(interpolatedImage, multiLevelModel);
                    targetBand.setSourceImage(image);
                } else {
                    final MultiLevelImage aggregatedImage = createAggregatedImage(sourceBand, referenceNode);
                    targetBand.setSourceImage(aggregatedImage);
                }
                targetProduct.addBand(targetBand);
            } else {
                targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, false);
                final RenderedImage image = createImageWithIdentityImageToModelTransform(sourceBand.getSourceImage(), multiLevelModel);
                targetBand.setSourceImage(image);

            }
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        }
    }

    private RenderedImage createImageWithIdentityImageToModelTransform(RenderedImage image, MultiLevelModel multiLevelModel) {
        return new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, multiLevelModel));
    }

    private MultiLevelImage createInterpolatedImage(Band sourceBand, final RasterDataNode referenceNode) {
        Interpolation interpolation;
        if (sourceBand.isFlagBand() || sourceBand.isIndexBand()) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        } else {
            interpolation = getInterpolation(sourceBand);
        }
        return Interpolate.createInterpolatedMultiLevelImage(sourceBand, referenceNode, interpolation);
    }

    private MultiLevelImage createAggregatedImage(Band sourceBand, final RasterDataNode referenceNode) {
        final Aggregate.Type aggregationType = getAggregationType(aggregationMethod);
        final Aggregate.Type flagAggregationType = getAggregationType(flagAggregationMethod);
        return Aggregate.createAggregatedMultiLevelImage(sourceBand, referenceNode, aggregationType, flagAggregationType);
    }

    private Aggregate.Type getAggregationType(String method) {
        if ("Min".equalsIgnoreCase(method)) {
            return Aggregate.Type.MIN;
        } else if ("Max".equalsIgnoreCase(method)) {
            return Aggregate.Type.MAX;
        } else if ("Median".equalsIgnoreCase(method)) {
            return Aggregate.Type.MEDIAN;
        } else if ("MinMedian".equalsIgnoreCase(method)) {
            return Aggregate.Type.MIN_MEDIAN;
        } else if ("MaxMedian".equalsIgnoreCase(method)) {
            return Aggregate.Type.MAX_MEDIAN;
        } else {
            return Aggregate.Type.FIRST;
        }
    }

    //todo this method has been copied from ReprojectionOp. Find a common place? - tf 20160210
    private Interpolation getInterpolation(Band band) {
        int interpolation = getInterpolationType();
        if (!ProductData.isFloatingPointType(band.getDataType())) {
            interpolation = Interpolation.INTERP_NEAREST;
        }
        return Interpolation.getInstance(interpolation);
    }

    //todo this method has been copied from ReprojectionOp. Find a common place? - tf 20160210
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
            super(ResamplingOp.class);
        }
    }

}
