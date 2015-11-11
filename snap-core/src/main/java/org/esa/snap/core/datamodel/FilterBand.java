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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.image.FillConstantOpImage;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.Guardian;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderCopy;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * Represents a band that generates its data by using another band as input and performs some kind of operation on this input.
 * <p><i>Note that this class is not yet public API and may change in future releases.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class FilterBand extends Band {

    private RasterDataNode source;

    protected FilterBand(String name, int dataType, int width, int height, RasterDataNode source) {
        super(name, dataType, width, height);
        Guardian.assertNotNull("source", source);
        this.source = source;
        setOwner(source.getProduct());
        setSynthetic(true);
        setNoDataValue(Double.NaN);
        setNoDataValueUsed(true);
    }

    public RasterDataNode getSource() {
        return source;
    }

    @Override
    public void dispose() {
        source = null;
        super.dispose();
    }

    @Override
    protected RenderedImage createSourceImage() {
        final MultiLevelModel model = createMultiLevelModel();
        final AbstractMultiLevelSource multiLevelSource = new AbstractMultiLevelSource(model) {
            @Override
            protected RenderedImage createImage(int level) {
                ImageManager imageManager = ImageManager.getInstance();

                RenderingHints rh = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(
                        BorderExtenderCopy.BORDER_COPY));

                PlanarImage geophysicalImage = imageManager.getGeophysicalImage(getSource(), level);

                int dataBufferType = getDataType() == ProductData.TYPE_FLOAT64 ? DataBuffer.TYPE_DOUBLE : DataBuffer.TYPE_FLOAT;
                geophysicalImage = FormatDescriptor.create(geophysicalImage, dataBufferType, null);

                PlanarImage validMaskImage = imageManager.getValidMaskImage(getSource(), level);
                if (validMaskImage != null) {
                    geophysicalImage = new FillConstantOpImage(geophysicalImage, validMaskImage, Float.NaN);
                }

                return createSourceLevelImage(geophysicalImage, level, rh);
            }
        };
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    /**
     * @param sourceImage The geophysical source image. No-data is masked as NaN.
     * @param level       The image level.
     * @param rh          Rendering hints. JAI.KEY_BORDER_EXTENDER is set to BorderExtenderCopy.BORDER_COPY.
     * @return The resulting filtered level image.
     * @since BEAM 5
     */
    protected abstract RenderedImage createSourceLevelImage(RenderedImage sourceImage, int level, RenderingHints rh);

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY,
                                int width, int height,
                                ProductData rasterData, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for filtered band");
    }

}
