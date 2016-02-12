package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.Interpolation;
import java.awt.Dimension;

/**
 * The resampling operator creates a product in which every {@code RasterDataNode} is of the same size and resolution.
 * Every {@code RasterDataNode} will return {@literal SceneTransform.IDENTITY} for calls of
 * {@link RasterDataNode#getSceneToModelTransform()} and {@link RasterDataNode#getModelToSceneTransform()} and the same
 * transformations for calls of {@link RasterDataNode#getImageToModelTransform()}.
 *
 * @author Tonio Fincke
 */
public class ResamplingOp extends Operator {

    private static final String NAME_EXTENSION = "resampled";

    @SourceProduct(alias = "source", description = "The source product which is to be resampled.")
    Product sourceProduct;

    @TargetProduct(description = "The resampled target product.")
    Product targetProduct;

    //todo also allow to set a target size/resolution explicitly
    @Parameter(description = "The reference raster data node. All other bands will be re-sampled to match its size and resolution.")
    RasterDataNode referenceNode;

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
            defaultValue = "Mean")
    private String aggregationMethod;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isMultiSizeProduct()) {
            targetProduct = sourceProduct;
            return;
        }
        Assert.notNull(referenceNode);
        validateInterpolationParameter();
        final int referenceWidth = referenceNode.getRasterWidth();
        final int referenceHeight = referenceNode.getRasterHeight();
        Dimension referenceSize = referenceNode.getRasterSize();
        targetProduct = new Product(sourceProduct.getName() + "_" + NAME_EXTENSION, sourceProduct.getProductType(),
                                    referenceWidth, referenceHeight);
        final ProductNodeGroup<Band> sourceBands = sourceProduct.getBandGroup();
        for (int i = 0; i < sourceBands.getNodeCount(); i++) {
            Band sourceBand = sourceBands.get(i);
            if (!sourceBand.getRasterSize().equals(referenceSize)) {
                //todo consider case when band width is smaller than reference width but band height is larger than reference height or vice versa
                final Band targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), referenceWidth, referenceHeight);
                if (sourceBand.getRasterWidth() < referenceWidth && sourceBand.getRasterHeight() < referenceHeight) {
                    final MultiLevelImage interpolatedImage = createInterpolatedImage(sourceBand, referenceNode);
                    targetBand.setSourceImage(interpolatedImage);
                } else {
                    //aggregation
                }
                targetProduct.addBand(targetBand);
            } else {
                ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
            }
        }
        //todo tie point grids need not to be resampled. Instead, they might need to be adapted to new product sizes
    }

    private MultiLevelImage createInterpolatedImage(Band sourceBand, final RasterDataNode referenceNode) {
        final Interpolation interpolation = getInterpolation(sourceBand);
        return Interpolate.createInterpolatedMultiLevelImage(sourceBand, referenceNode, interpolation);
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

}
