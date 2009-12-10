package org.esa.beam.framework.datamodel;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VectorDataMaskOpImage;

import java.awt.image.RenderedImage;
import java.lang.ref.WeakReference;

/**
 * A {@link MultiLevelImage} computed from vector data. The {@link VectorDataMultiLevelImage}
 * resets itsself whenever the referred vector data have changed.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 * @since BEAM 4.7
 */
class VectorDataMultiLevelImage extends DefaultMultiLevelImage implements ProductNodeListener {

    private final WeakReference<VectorData> vectorDataReference;

    /**
     * Creates a new mask {@link MultiLevelImage} computed from vector data. The mask image
     * created is reset whenever the referred vector data have changed.
     * <p/>
     * A 'node data changed' event is fired from the associated {@link RasterDataNode} whenever
     * the mask image is reset.
     *
     * @param vectorData     the vector data referred.
     * @param associatedNode the {@link RasterDataNode} associated with the image being created.
     *
     * @return the {@code MultiLevelImage} created.
     */
    static MultiLevelImage createMask(final VectorData vectorData, final RasterDataNode associatedNode) {
        final MultiLevelSource multiLevelSource = createMaskMultiLevelSource(vectorData);
        return new VectorDataMultiLevelImage(multiLevelSource, vectorData) {
            @Override
            public void reset() {
                super.reset();
                associatedNode.fireProductNodeDataChanged();
            }
        };
    }

    /**
     * Creates a new {@link MultiLevelImage} computed from vector data. The created
     * image resets itsself whenever the referred vector data have changed.
     *
     * @param multiLevelSource the multi-level image source
     * @param vectorData       the vector data referred.
     */
    VectorDataMultiLevelImage(MultiLevelSource multiLevelSource, final VectorData vectorData) {
        super(multiLevelSource);

        this.vectorDataReference = new WeakReference<VectorData>(vectorData);
        this.vectorDataReference.get().getProduct().addProductNodeListener(this);
    }

    @Override
    public void dispose() {
        vectorDataReference.get().getProduct().removeProductNodeListener(this);
        vectorDataReference.clear();
        super.dispose();
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
        if (event.getSourceNode() == vectorDataReference.get()) {
            if (event.getPropertyName().equals("featureCollection")) {
                reset();
            }
        }
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }

    // use for testing only
    VectorData getVectorData() {
        return vectorDataReference.get();
    }

    // use for testing only
    static MultiLevelSource createMaskMultiLevelSource(final VectorData vectorData) {
        final MultiLevelModel multiLevelModel = ImageManager.createMultiLevelModel(vectorData.getProduct());
        return new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return new VectorDataMaskOpImage(vectorData, ResolutionLevel.create(getModel(), level));
            }
        };
    }
}
