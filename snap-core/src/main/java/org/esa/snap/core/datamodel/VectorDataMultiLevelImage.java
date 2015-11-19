/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VectorDataMaskOpImage;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.lang.ref.WeakReference;

/**
 * A {@link MultiLevelImage} computed from vector data. The {@link VectorDataMultiLevelImage}
 * resets itself whenever the referred vector data have changed.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 * @since BEAM 4.7
 */
class VectorDataMultiLevelImage extends DefaultMultiLevelImage implements ProductNodeListener {

    private final WeakReference<VectorDataNode> vectorDataReference;

    /**
     * Creates a new binary mask {@link MultiLevelImage} computed from vector data. The mask image
     * created is reset whenever the referred vector data have changed.
     * <p>
     * A 'node data changed' event is fired from the associated {@link RasterDataNode} whenever
     * the mask image is reset.
     *
     * @param vectorDataNode the vector data referred to.
     * @param associatedNode the {@link RasterDataNode} associated with the image being created.
     * @return the {@code MultiLevelImage} created.
     */
    static VectorDataMultiLevelImage createMaskImage(final VectorDataNode vectorDataNode, final RasterDataNode associatedNode) {
        final MultiLevelModel multiLevelModel = associatedNode.getMultiLevelModel();
        final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return new VectorDataMaskOpImage(vectorDataNode, associatedNode, ResolutionLevel.create(getModel(), level));
            }
        };
        return new VectorDataMultiLevelImage(multiLevelSource, vectorDataNode) {
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
     * @param vectorDataNode   the vector data referred.
     */
    VectorDataMultiLevelImage(MultiLevelSource multiLevelSource, final VectorDataNode vectorDataNode) {
        super(multiLevelSource);

        this.vectorDataReference = new WeakReference<>(vectorDataNode);
        vectorDataNode.getProduct().addProductNodeListener(this);
    }

    @Override
    public Shape getImageShape(int level) {
        VectorDataNode vectorDataNode = vectorDataReference.get();
        if (vectorDataNode != null) {
            ReferencedEnvelope envelope = vectorDataNode.getEnvelope();
            if (!envelope.isEmpty()) {
                Rectangle2D modelBounds = new Rectangle2D.Double(envelope.getMinX(), envelope.getMinY(),
                                                                 envelope.getWidth(), envelope.getHeight());
                AffineTransform m2i = getModel().getModelToImageTransform(level);
                return m2i.createTransformedShape(modelBounds);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        VectorDataNode vectorDataNode = vectorDataReference.get();
        if (vectorDataNode != null) {
            Product product = vectorDataNode.getProduct();
            if (product != null) {
                product.removeProductNodeListener(this);
            }
        }
        vectorDataReference.clear();
        super.dispose();
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
        if (event.getSourceNode() == vectorDataReference.get()) {
            if (event.getPropertyName().equals(VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION)) {
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

    VectorDataNode getVectorDataNode() {
        return vectorDataReference.get();
    }
}
