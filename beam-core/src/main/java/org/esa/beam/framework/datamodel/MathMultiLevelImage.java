package org.esa.beam.framework.datamodel;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link MultiLevelImage} computed from raster data arithmetics. A {@link MathMultiLevelImage}
 * resets itsself whenever any referred raster data have changed.
 */
class MathMultiLevelImage extends DefaultMultiLevelImage implements ProductNodeListener {

    private final Product product;
    private final List<ProductNode> nodeList = new ArrayList<ProductNode>();

    /**
     * Creates a new mask {@link MultiLevelImage} computed from raster data arithmetics. The mask
     * image created is reset whenever any referred raster data have changed.
     * <p/>
     * A 'node data changed' event is fired from the associated {@link RasterDataNode} whenever
     * the mask image is reset.
     *
     * @param expression     the raster data arithmetic expression.
     * @param associatedNode the {@link RasterDataNode} associated with the image being created.
     *
     * @return the {@code MultiLevelImage} created.
     */
    static MultiLevelImage createMask(final String expression, final RasterDataNode associatedNode) {
        final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(associatedNode);
        final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.createMask(expression,
                                                     associatedNode.getProduct(),
                                                     ResolutionLevel.create(getModel(), level));
            }
        };
        return new MathMultiLevelImage(expression, associatedNode.getProduct(), multiLevelSource) {
            @Override
            public void reset() {
                super.reset();
                associatedNode.fireProductNodeDataChanged();
            }
        };
    }

    /**
     * Creates a new {@link MultiLevelImage} computed from raster data arithmetics. The image
     * created is reset whenever any referred raster data have changed.
     * <p/>
     * A 'node data changed' event is fired from the associated {@link RasterDataNode} whenever
     * the image created is reset.
     *
     * @param expression     the raster data arithmetic expression.
     * @param associatedNode the {@link RasterDataNode} associated with the image being created.
     *
     * @return the {@code MultiLevelImage} created.
     */
    static MultiLevelImage create(final String expression, final RasterDataNode associatedNode) {
        final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(associatedNode);
        final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.create(expression,
                                                 associatedNode.getDataType(),
                                                 associatedNode.isNoDataValueUsed() ? associatedNode.getGeophysicalNoDataValue() : null,
                                                 associatedNode.getProduct(),
                                                 ResolutionLevel.create(getModel(), level));
            }
        };
        return new MathMultiLevelImage(expression, associatedNode.getProduct(), multiLevelSource) {
            @Override
            public void reset() {
                super.reset();
                associatedNode.fireProductNodeDataChanged();
            }
        };
    }

    /**
     * Creates a new {@link MultiLevelImage} computed from raster data arithmetics. The created
     * image resets itsself whenever any referred raster data have changed.
     *
     * @param expression       the raster data arithmetic expression.
     * @param product          the parent of the raster data referred in {@code expression}.
     * @param multiLevelSource the multi-level image source
     */
    MathMultiLevelImage(String expression, Product product, MultiLevelSource multiLevelSource) {
        super(multiLevelSource);
        this.product = product;
        try {
            final RasterDataNode[] rasters = BandArithmetic.getRefRasters(expression, product);
            if (rasters.length > 0) {
                Collections.addAll(nodeList, rasters);
                product.addProductNodeListener(this);
            }
        } catch (ParseException e) {
            // ignore, we do not need to listen to raster data nodes
        }
    }

    @Override
    public void dispose() {
        product.removeProductNodeListener(this);
        nodeList.clear();
        super.dispose();
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        if (nodeList.contains(event.getSourceNode())) {
            reset();
        }
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
        nodeList.remove(event.getSourceNode());
    }

    List<ProductNode> getNodeList() {
        return Collections.unmodifiableList(nodeList);
    }

}
