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

package org.esa.snap.dataio.rtp;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.image.TiledFileMultiLevelSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

public class RawTiledPyramidsProductReader extends AbstractProductReader {
    public RawTiledPyramidsProductReader(RawTiledPyramidsProductCodecSpi spi) {
        super(spi);
    }

    protected Product readProductNodesImpl() throws IOException {

        final File headerFile = RawTiledPyramidsProductCodecSpi.getHeaderFile(getInput());
        final XStream xStream = RawTiledPyramidsProductCodecSpi.createXStream();
        try (FileReader reader = new FileReader(headerFile)) {
            final ProductDescriptor productDescriptor = new ProductDescriptor();
            xStream.fromXML(reader, productDescriptor);

            final Product product = new Product(productDescriptor.getName(), productDescriptor.getType(), productDescriptor.getWidth(),
                                                productDescriptor.getHeight());
            product.setDescription(productDescriptor.getDescription());

            final BandDescriptor[] bandDescriptors = productDescriptor.getBandDescriptors();
            for (BandDescriptor bandDescriptor : bandDescriptors) {
                final String expression = bandDescriptor.getExpression();
                final Band band;
                if (expression != null && !expression.trim().isEmpty()) {
                    band = new VirtualBand(bandDescriptor.getName(),
                                           ProductData.getType(bandDescriptor.getDataType()),
                                           product.getSceneRasterWidth(),
                                           product.getSceneRasterHeight(), expression);
                } else {
                    band = new Band(bandDescriptor.getName(),
                                    ProductData.getType(bandDescriptor.getDataType()),
                                    product.getSceneRasterWidth(),
                                    product.getSceneRasterHeight());
                    Path imageDir = headerFile.toPath().getParent().resolve(bandDescriptor.getName());
                    band.setSourceImage(new DefaultMultiLevelImage(TiledFileMultiLevelSource.create(imageDir)));
                }
                band.setDescription(bandDescriptor.getDescription());
                band.setScalingFactor(bandDescriptor.getScalingFactor());
                band.setScalingOffset(bandDescriptor.getScalingOffset());
                product.addBand(band);
            }

            return product;
        } catch (XStreamException e) {
            throw new IOException("Failed to read product header.", e);
        }
    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("should not come here!");
    }

}
