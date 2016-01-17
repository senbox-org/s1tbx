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

package org.esa.snap.core.image;

import com.bc.ceres.core.VirtualDir;
import com.sun.media.jai.codec.SeekableStream;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.operator.StreamDescriptor;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;


public class TiledFileOpImage extends SourcelessOpImage {

    private Path imageDir;
    private ImageInputStreamFactory inputStreamFactory;
    private boolean disposed;
    private ImageHeader imageHeader;

    public static TiledFileOpImage create(File imageDir, Properties defaultImageProperties) throws IOException {
        return create(imageDir.toPath(), defaultImageProperties);
    }

    public static TiledFileOpImage create(VirtualDir imageDir, Properties defaultImageProperties) throws IOException {
        return create(Paths.get(imageDir.getBasePath()), defaultImageProperties);
    }

    public static TiledFileOpImage create(Path imageDir, Properties defaultImageProperties) throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(imageDir, defaultImageProperties);
        return new TiledFileOpImage(imageHeader, null, imageDir);
    }

    private TiledFileOpImage(ImageHeader imageHeader, Map configuration, Path imageDir) throws IOException {
        super(imageHeader.getImageLayout(),
              configuration,
              imageHeader.getImageLayout().getSampleModel(null),
              imageHeader.getImageLayout().getMinX(null),
              imageHeader.getImageLayout().getMinY(null),
              imageHeader.getImageLayout().getWidth(null),
              imageHeader.getImageLayout().getHeight(null));
        this.imageDir = imageDir;
        this.imageHeader = imageHeader;
        if (this.imageHeader.getTileFormat().equalsIgnoreCase("raw.zip")) {
            inputStreamFactory = new RawZipImageInputStreamFactory(imageDir);
        } else if (this.imageHeader.getTileFormat().equalsIgnoreCase("raw")) {
            inputStreamFactory = new RawImageInputStreamFactory();
        } else if (this.imageHeader.getTileFormat().equalsIgnoreCase("zip")) {
            inputStreamFactory = new ZipInputStreamFactory();
        }
        if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
    }

    /**
     * Computes a tile.  Since the operation has no sources,
     * there is no need to worry about cobbling.
     * <p> Subclasses should implement the
     * {@code computeRect(PlanarImage[], WritableRaster, Rectangle)}
     * method to perform the actual computation.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    @Override
    public Raster computeTile(int tileX, int tileY) {
        // System.out.println("TiledFileOpImage.computeTile: >> '" + getTileFilename(tileX, tileY) + "'...");
        final Point location = new Point(tileXToX(tileX), tileYToY(tileY));
        final Raster raster;
        try {
            if (imageHeader.getTileFormat().startsWith("raw")) {
                final WritableRaster targetRaster = createWritableRaster(sampleModel, location);
                readRawDataTile(tileX, tileY, targetRaster);
                raster = targetRaster;
            } else {
                raster = readImageTile(tileX, tileY, location);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image tile.", e);
        }

        // System.out.println("TiledFileOpImage.computeTile: << '" + getTileFilename(tileX, tileY) + "'");
        return raster;
    }

    private Raster readImageTile(int tileX, int tileY, Point location) throws IOException {
        SeekableStream inputStream = SeekableStream.wrapInputStream(Files.newInputStream(imageDir.resolve(getTileFilename(tileX, tileY))), true);
        RenderedOp renderedOp = StreamDescriptor.create(inputStream, null, null);
        final Raster data = renderedOp.getData();
        return WritableRaster.createRaster(data.getSampleModel(), data.getDataBuffer(), location);
    }

    private void readRawDataTile(int tileX, int tileY, WritableRaster targetRaster) throws IOException {
        try (ImageInputStream imageInputStream = inputStreamFactory.createImageInputStream(tileX, tileY)) {
            readRawDataTile(imageInputStream, targetRaster);
        }
    }

    /**
     * Reads the data buffer of the given raster from the given image input stream.
     *
     * @param raster The raster.
     * @param stream The image input stream.
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the {@code raster}'s data arrays cannot be retrieved
     * @throws NullPointerException     if {@code raster} or {@code stream} is null
     */
    public static void readRawDataTile(ImageInputStream stream, WritableRaster raster) throws IOException {
        final Object dataObject = getDataObject(raster);
        if (dataObject instanceof byte[]) {
            final byte[] data = (byte[]) dataObject;
            stream.readFully(data, 0, data.length);
        } else if (dataObject instanceof short[]) {
            final short[] data = (short[]) dataObject;
            stream.readFully(data, 0, data.length);
        } else if (dataObject instanceof int[]) {
            final int[] data = (int[]) dataObject;
            stream.readFully(data, 0, data.length);
        } else if (dataObject instanceof float[]) {
            final float[] data = (float[]) dataObject;
            stream.readFully(data, 0, data.length);
        } else if (dataObject instanceof double[]) {
            final double[] data = (double[]) dataObject;
            stream.readFully(data, 0, data.length);
        } else {
            throw new IllegalArgumentException(
                    "raster: Unexpected type returned by raster.getDataBuffer().getData(): " + dataObject);
        }
    }

    /**
     * Writes the data buffer of the given raster to the given image output stream.
     *
     * @param raster The raster.
     * @param stream The image output stream.
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the {@code raster}'s data arrays cannot be retrieved
     * @throws NullPointerException     if {@code raster} or {@code stream} is null
     */
    public static void writeRawDataTile(Raster raster, ImageOutputStream stream) throws IOException {
        final Object dataObject = getDataObject(raster);
        if (dataObject instanceof byte[]) {
            byte[] data = (byte[]) dataObject;
            stream.write(data);
        } else if (dataObject instanceof short[]) {
            short[] data = (short[]) dataObject;
            stream.writeShorts(data, 0, data.length);
        } else if (dataObject instanceof int[]) {
            int[] data = (int[]) dataObject;
            stream.writeInts(data, 0, data.length);
        } else if (dataObject instanceof float[]) {
            float[] data = (float[]) dataObject;
            stream.writeFloats(data, 0, data.length);
        } else if (dataObject instanceof double[]) {
            double[] data = (double[]) dataObject;
            stream.writeDoubles(data, 0, data.length);
        } else {
            throw new IllegalArgumentException(
                    "raster: Unexpected type returned by raster.getDataBuffer().getData(): " + dataObject);
        }
    }

    /**
     * Gets the data object from the data buffer of the given raster.
     * The data object which will always be of a primitive array type.
     *
     * @param raster The raster.
     *
     * @return The data array.
     *
     * @throws IllegalArgumentException if the {@code raster}'s data arrays cannot be retrieved
     * @throws NullPointerException     if {@code raster} is null
     */
    public static Object getDataObject(Raster raster) {
        final DataBuffer dataBuffer = raster.getDataBuffer();
        final Object arrayObject;
        if (dataBuffer instanceof DataBufferByte) {
            arrayObject = ((DataBufferByte) dataBuffer).getData();
        } else if (dataBuffer instanceof DataBufferShort) {
            arrayObject = ((DataBufferShort) dataBuffer).getData();
        } else if (dataBuffer instanceof DataBufferUShort) {
            arrayObject = ((DataBufferUShort) dataBuffer).getData();
        } else if (dataBuffer instanceof DataBufferInt) {
            arrayObject = ((DataBufferInt) dataBuffer).getData();
        } else if (dataBuffer instanceof DataBufferFloat) {
            arrayObject = ((DataBufferFloat) dataBuffer).getData();
        } else if (dataBuffer instanceof DataBufferDouble) {
            arrayObject = ((DataBufferDouble) dataBuffer).getData();
        } else if (dataBuffer instanceof javax.media.jai.DataBufferFloat) {
            arrayObject = ((javax.media.jai.DataBufferFloat) dataBuffer).getData();
        } else if (dataBuffer instanceof javax.media.jai.DataBufferDouble) {
            arrayObject = ((javax.media.jai.DataBufferDouble) dataBuffer).getData();
        } else {
            try {
                final Method method = dataBuffer.getClass().getMethod("getData");
                arrayObject = method.invoke(dataBuffer);
            } catch (Throwable t) {
                throw new IllegalArgumentException("raster: Failed to invoke raster.getDataBuffer().getData().", t);
            }
        }
        if (arrayObject == null) {
            throw new IllegalArgumentException("raster: raster.getDataBuffer().getData() returned null.");
        }
        return arrayObject;
    }

    @Override
    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        inputStreamFactory = null;
        super.dispose();
    }

    private String getTileFilename(int tileX, int tileY) {
        return getTileBasename(tileX, tileY) + "." + imageHeader.getTileFormat();
    }

    private static String getTileBasename(int tileX, int tileY) {
        return tileX + "-" + tileY;
    }

    private interface ImageInputStreamFactory {

        ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException;
    }

    private class RawImageInputStreamFactory implements ImageInputStreamFactory {

        public ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException {
            return new MemoryCacheImageInputStream(Files.newInputStream(imageDir.resolve(getTileFilename(tileX, tileY))));
        }
    }

    static class RawZipImageInputStreamFactory implements ImageInputStreamFactory {

        private final Path imageDir;
        private File tmpDir;

        RawZipImageInputStreamFactory(Path imageDir) throws IOException {
            this.imageDir = imageDir;
            tmpDir = VirtualDir.createUniqueTempDir();
            // System.out.println("TiledFileOpImage: Using temporary directory '" + tmpDir + "'");
        }

        public ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException {
            final String entryName = getTileBasename(tileX, tileY) + ".raw";
            final String entryPathString = createEntryPathString(entryName);
            final URI uri = URI.create(entryPathString);
            Path entryZip = FileUtils.getPathFromURI(uri);
            InputStream stream = Files.newInputStream(entryZip.resolve(entryName));
            return new FileCacheImageInputStream(stream, tmpDir);
        }

        String createEntryPathString(String entryName) {
            return "jar:file:" + imageDir.resolve(entryName + ".zip").toUri().getRawPath() + "!/";
        }
    }

    private class ZipInputStreamFactory implements ImageInputStreamFactory {

        private File tmpDir;

        private ZipInputStreamFactory() throws IOException {
            tmpDir = VirtualDir.createUniqueTempDir();
            // System.out.println("TiledFileOpImage: Using temporary directory '" + tmpDir + "'");
        }

        public ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException {
            final String entryName = getTileBasename(tileX, tileY);
            Stream<Path> list = Files.list(imageDir);
            Optional<Path> first = list.filter(path -> path.getFileName().startsWith(entryName)).findFirst();
            if(first.isPresent()) {
                return new FileCacheImageInputStream(Files.newInputStream(first.get()), tmpDir);
            } else {
                throw new IOException("No tile for coordinates " + tileX + ", " + tileY + ".");
            }
        }
    }
}
