package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.GPF;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class ResampleUtils {

    public static boolean allGridsAlignAtUpperLeftPixel(Product product) {
        MathTransform mapTransform = product.getSceneGeoCoding().getImageToMapTransform();
        if (!(mapTransform instanceof AffineTransform)) {
            return false;
        }
        AffineTransform affineTransform = (AffineTransform) mapTransform;
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
        return allGridsAlignAtUpperLeftPixelCenter(affineTransform, bandGroup, tiePointGridGroup) ||
                allGridsAlignAtUpperLeftPixelCorner(affineTransform, bandGroup, tiePointGridGroup);
    }

    static boolean allGridsAlignAtUpperLeftPixelCorner(AffineTransform mapTransform,
                                                               ProductNodeGroup<Band> bandGroup,
                                                               ProductNodeGroup<TiePointGrid> tiePointGridGroup) {
        return allGridsAlignAtUpperLeft(mapTransform, bandGroup, tiePointGridGroup, 0.0);
    }

    static boolean allGridsAlignAtUpperLeftPixelCenter(AffineTransform mapTransform,
                                                               ProductNodeGroup<Band> bandGroup,
                                                               ProductNodeGroup<TiePointGrid> tiePointGridGroup) {
        return allGridsAlignAtUpperLeft(mapTransform, bandGroup, tiePointGridGroup, 0.5);
    }

    private static boolean allGridsAlignAtUpperLeft(AffineTransform mapTransform,
                                                    ProductNodeGroup<Band> bandGroup,
                                                    ProductNodeGroup<TiePointGrid> tiePointGridGroup,
                                                    double offset) {
        ArrayList<AffineTransform> transforms = new ArrayList<>();
        transforms.add(mapTransform);
        double centerX = mapTransform.getTranslateX() + offset * mapTransform.getScaleX();
        double centerY = mapTransform.getTranslateY() + offset * mapTransform.getScaleY();
        return allGridsAlignAtUpperLeft(transforms, centerX, centerY, bandGroup, offset) &&
                allGridsAlignAtUpperLeft(transforms, centerX, centerY, tiePointGridGroup, offset);
    }

    //package local for testing
    @SuppressWarnings("WeakerAccess")
    static boolean allGridsAlignAtUpperLeft(ArrayList<AffineTransform> transforms, double centerX, double centerY,
                                            ProductNodeGroup group, double offset) {
        for (int i = 0; i < group.getNodeCount(); i++) {
            RasterDataNode rasterDataNode = (RasterDataNode) group.get(i);
            AffineTransform transform = rasterDataNode.getImageToModelTransform();
            if (!transforms.contains(transform)) {
                if (Math.abs(centerX - (transform.getTranslateX() + offset * transform.getScaleX())) > 1e-8 ||
                        Math.abs(centerY - (transform.getTranslateY() + offset * transform.getScaleY())) > 1e-8) {
                    return false;
                }
                transforms.add(transform);
            }
        }
        return true;
    }

    public static Downsampling getDownsamplingFromAggregatorType(AggregationType type) {
        DownsamplerSpi spi = GPF.getDefaultInstance().getDownsamplerSpiRegistry().getDownsamplerSpi(type.toString());
        if(spi == null) {
            return null;
        }
        return spi.createDownsampling();
    }

    public static Upsampling getUpsamplingFromInterpolationType(InterpolationType type) {
        UpsamplerSpi spi = GPF.getDefaultInstance().getUpsamplerSpiRegistry().getUpsamplerSpi(type.toString());
        if(spi == null) {
            return null;
        }
        return spi.createUpsampling();
    }
}
