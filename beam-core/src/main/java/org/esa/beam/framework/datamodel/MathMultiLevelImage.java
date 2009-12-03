package org.esa.beam.framework.datamodel;

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

class MathMultiLevelImage extends DefaultMultiLevelImage implements ProductNodeListener {

    private final Product product;
    private final List<ProductNode> nodeList = new ArrayList<ProductNode>();

    MathMultiLevelImage(String expression, Product product) {
        super(createMultiLevelSource(expression, product));
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

    private static MultiLevelSource createMultiLevelSource(final String expression, final Product product) {
        return new AbstractMultiLevelSource(ImageManager.createMultiLevelModel(product)) {
            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.createMask(expression, product,
                                                     ResolutionLevel.create(getModel(), level));
            }
        };
    }
}
