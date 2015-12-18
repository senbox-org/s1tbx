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
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.image.ImageHeader;
import org.esa.snap.core.image.TiledFileOpImage;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

public class RawTiledPyramidsProductWriter extends AbstractProductWriter {

    private File headerFile;
    private HashSet<Band> writtenBands;
    private final ProductNodeHandler productNodeHandler;

    public RawTiledPyramidsProductWriter(RawTiledPyramidsProductCodecSpi spi) {
        super(spi);
        productNodeHandler = new ProductNodeHandler();
    }

    protected void writeProductNodesImpl() throws IOException {
        headerFile = RawTiledPyramidsProductCodecSpi.getHeaderFile(getOutput());
        writtenBands = new HashSet<Band>(31);

        final Product product = getSourceProduct();
        product.addProductNodeListener(productNodeHandler);

        final ProductDescriptor productDescriptor = createProductDescriptor(product);
        final XStream xStream = RawTiledPyramidsProductCodecSpi.createXStream();
        final File productDir = headerFile.getParentFile();
        if (productDir != null && !productDir.exists() && !productDir.mkdirs()) {
            throw new IOException("Failed to create product folder.");
        }
        final FileWriter writer = new FileWriter(headerFile);
        try {
            xStream.toXML(productDescriptor, writer);
        } catch (XStreamException e) {
            throw new IOException("Failed to write product header.", e);
        } finally {
            writer.close();
        }
    }

    private ProductDescriptor createProductDescriptor(Product product) {
        return new ProductDescriptor(product.getName(),
                                     product.getProductType(),
                                     product.getSceneRasterWidth(),
                                     product.getSceneRasterHeight(),
                                     createBandDescriptors(),
                                     product.getDescription());
    }

    private BandDescriptor[] createBandDescriptors() {
        final Product product = getSourceProduct();
        final Band[] bands = product.getBands();
        ArrayList<BandDescriptor> bandList = new ArrayList<BandDescriptor>(bands.length);
        for (Band band : bands) {
            if (band.getClass() == Band.class) {
                bandList.add(new BandDescriptor(band.getName(),
                                                ProductData.getTypeString(band.getDataType()),
                                                band.getScalingOffset(),
                                                band.getScalingFactor(),
                                                null,
                                                band.getDescription()
                ));
            } else if (band.getClass() == VirtualBand.class) {
                bandList.add(new BandDescriptor(band.getName(),
                                                ProductData.getTypeString(band.getDataType()),
                                                band.getScalingOffset(),
                                                band.getScalingFactor(),
                                                ((VirtualBand) band).getExpression(),
                                                band.getDescription()
                ));
            }
        }
        return bandList.toArray(new BandDescriptor[bandList.size()]);
    }

    public synchronized void writeBandRasterData(Band band, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                                 ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        if (writtenBands.contains(band)) {
            return;
        }
        if (band.getClass() != Band.class) {
            return;
        }

        final MultiLevelImage image = band.getSourceImage();
        final File productDir = headerFile.getParentFile();
        final File bandDir = new File(productDir, band.getName());
        if (!ensureDir(bandDir)) {
            return;
        }

        final int levelCount = image.getModel().getLevelCount();
        final Properties imageProperties = new Properties();
        imageProperties.setProperty("numLevels", image.getModel().getLevelCount() + "");
        imageProperties.setProperty("width", image.getWidth() + "");
        imageProperties.setProperty("height", image.getHeight() + "");
        final double[] flatmatrix = new double[6];
        image.getModel().getImageToModelTransform(0).getMatrix(flatmatrix);
        imageProperties.setProperty("i2mTransform", flatmatrix[0]
                                                    + "," + flatmatrix[1]
                                                    + "," + flatmatrix[2]
                                                    + "," + flatmatrix[3]
                                                    + "," + flatmatrix[4]
                                                    + "," + flatmatrix[5]);
        try (FileWriter writer = new FileWriter(new File(bandDir, "image.properties"))) {
            imageProperties.store(writer, "File created by " + getClass());
        }


        pm.beginTask("Writing tile data", levelCount);
        for (int level = 0; level < levelCount; level++) {
            final RenderedImage levelImage = image.getImage(level);
            final File levelDir = new File(bandDir, "" + level);
            writeLevelImage(levelImage, levelDir, SubProgressMonitor.create(pm, 1));
        }

        writtenBands.add(band);
    }

    private synchronized void writeLevelImage(RenderedImage levelImage, File levelDir, ProgressMonitor pm) throws IOException {
        if (!ensureDir(levelDir)) {
            return;
        }
        try {
            pm.beginTask("Writing image", levelImage.getNumXTiles() * levelImage.getNumYTiles());
            final ImageHeader imageHeader = new ImageHeader(levelImage, "raw");
            try (FileWriter fileWriter = new FileWriter(new File(levelDir, "image.properties"))) {
                imageHeader.store(fileWriter, null);
            }
            for (int y = 0; y < levelImage.getNumYTiles(); y++) {
                for (int x = 0; x < levelImage.getNumXTiles(); x++) {
                    writeTile(levelImage, x, y, levelDir);
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private synchronized void writeTile(RenderedImage levelImage, int tileX, int tileY, File levelDir) throws IOException {
        final Raster raster = levelImage.getTile(tileX, tileY);

        final File tileFile = new File(levelDir, tileX + "-" + tileY + ".raw");
        final FileImageOutputStream stream = new FileImageOutputStream(tileFile);
        try {
            TiledFileOpImage.writeRawDataTile(raster, stream);
            stream.close();
        } catch (Throwable t) {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
            tileFile.delete();
            throw new IOException("Failed to write tile to " + tileFile, t);
        }
    }


    private synchronized boolean ensureDir(File dir) {
        return dir.isDirectory() || dir.mkdir();
    }

    public synchronized void flush() throws IOException {
    }

    public synchronized void close() throws IOException {
        flush();
        if (writtenBands != null) {
            writtenBands.clear();
            writtenBands = null;
        }
        Product product = getSourceProduct();
        if (product != null) {
            product.removeProductNodeListener(productNodeHandler);
        }
    }

    public synchronized void deleteOutput() throws IOException {
        close();
        headerFile.delete();
        final File dir = headerFile.getParentFile();
        if (dir == null) {
            throw new IOException("Could not retrieve the parent directory of '" + headerFile.getAbsolutePath() + "'.");
        }
        final Product product = getSourceProduct();
        final String[] bandNames = product.getBandNames();
        for (String bandName : bandNames) {
            final File imageDir = new File(dir, bandName);
            if (imageDir.isDirectory()) {
                FileUtils.deleteTree(imageDir);
            }
        }
        dir.delete();
    }

    private class ProductNodeHandler implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            if (event.getSourceNode().getClass() == Band.class) {
                synchronized (RawTiledPyramidsProductWriter.this) {
                    if (writtenBands != null) {
                        writtenBands.remove(event.getSourceNode());
                    }
                }
            }
        }

        public void nodeAdded(ProductNodeEvent event) {
        }

        public void nodeRemoved(ProductNodeEvent event) {
        }
    }
}
