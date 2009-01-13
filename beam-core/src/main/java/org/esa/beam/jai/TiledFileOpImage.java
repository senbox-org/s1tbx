package org.esa.beam.jai;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class TiledFileOpImage extends SourcelessOpImage {
    private File imageDir;
    private ImageInputStreamFactory inputStreamFactory;
    private boolean disposed;
    private ImageHeader imageHeader;

    public static TiledFileOpImage create(File imageDir, Properties defaultImageProperties) throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(imageDir, defaultImageProperties);
        return new TiledFileOpImage(imageHeader, null, imageDir);
    }


    private TiledFileOpImage(ImageHeader imageHeader, Map configuration, File imageDir) {
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
            inputStreamFactory = new RawZipImageInputStreamFactory();
        } else if (this.imageHeader.getTileFormat().equalsIgnoreCase("raw")) {
            inputStreamFactory = new RawImageInputStreamFactory();
        }
        if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
    }

    /**
     * Computes a tile.  Since the operation has no sources,
     * there is no need to worry about cobbling.
     * <p/>
     * <p> Subclasses should implement the
     * <code>computeRect(PlanarImage[], WritableRaster, Rectangle)</code>
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
        if (imageHeader.getTileFormat().startsWith("raw")) {
            final WritableRaster targetRaster = createWritableRaster(sampleModel, location);
            try {
                readRawDataTile(tileX, tileY, targetRaster);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image tile.", e);
            }
            raster = targetRaster;
        } else {
            raster = readImageTile(tileX, tileY, location);
        }

        // System.out.println("TiledFileOpImage.computeTile: << '" + getTileFilename(tileX, tileY) + "'");
        return raster;
    }

    private Raster readImageTile(int tileX, int tileY, Point location) {
        File imageFile = new File(imageDir, getTileFilename(tileX, tileY));
        final RenderedOp renderedOp = FileLoadDescriptor.create(imageFile.getPath(), null, true, null);
        final Raster data = renderedOp.getData();
        return WritableRaster.createRaster(data.getSampleModel(), data.getDataBuffer(), location);
    }

    private void readRawDataTile(int tileX, int tileY, WritableRaster targetRaster) throws IOException {
        final ImageInputStream imageInputStream = inputStreamFactory.createImageInputStream(tileX, tileY);
        try {
            readRawDataTile(imageInputStream, targetRaster);
        } finally {
            imageInputStream.close();
        }
    }

    private void readRawDataTile(ImageInputStream imageInputStream, WritableRaster targetRaster) throws IOException {
        final DataBuffer dataBuffer = targetRaster.getDataBuffer();
        final int size = dataBuffer.getSize();
        if (sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
            imageInputStream.readFully(((DataBufferByte) dataBuffer).getData(), 0, size);
        } else if (sampleModel.getDataType() == DataBuffer.TYPE_SHORT) {
            imageInputStream.readFully(((DataBufferShort) dataBuffer).getData(), 0, size);
        } else if (sampleModel.getDataType() == DataBuffer.TYPE_USHORT) {
            imageInputStream.readFully(((DataBufferShort) dataBuffer).getData(), 0, size);
        } else if (sampleModel.getDataType() == DataBuffer.TYPE_INT) {
            imageInputStream.readFully(((DataBufferInt) dataBuffer).getData(), 0, size);
        } else if (sampleModel.getDataType() == DataBuffer.TYPE_FLOAT) {
            imageInputStream.readFully(((DataBufferFloat) dataBuffer).getData(), 0, size);
        } else if (sampleModel.getDataType() == DataBuffer.TYPE_DOUBLE) {
            imageInputStream.readFully(((DataBufferDouble) dataBuffer).getData(), 0, size);
        }
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

    private String getTileBasename(int tileX, int tileY) {
        return tileX + "-" + tileY;
    }

    private interface ImageInputStreamFactory {
        ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException;
    }

    private class RawImageInputStreamFactory implements ImageInputStreamFactory {
        public ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException {
            return new FileImageInputStream(new File(imageDir, getTileFilename(tileX, tileY)));
        }
    }

    private class RawZipImageInputStreamFactory implements ImageInputStreamFactory {
        private File tmpDir;

        private RawZipImageInputStreamFactory() {
            tmpDir = new File(System.getProperty("java.io.tmpdir", ".temp"));
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }
            // System.out.println("TiledFileOpImage: Using temporary directory '" + tmpDir + "'");
        }

        public ImageInputStream createImageInputStream(int tileX, int tileY) throws IOException {
            final String entryName = getTileBasename(tileX, tileY) + ".raw";
            final File file = new File(imageDir, entryName + ".zip");
            final ZipFile zipFile = new ZipFile(file);
            final ZipEntry zipEntry = zipFile.getEntry(entryName);
            final InputStream inputStream = zipFile.getInputStream(zipEntry);
            return new FileCacheImageInputStream(inputStream, tmpDir);
        }
    }
}