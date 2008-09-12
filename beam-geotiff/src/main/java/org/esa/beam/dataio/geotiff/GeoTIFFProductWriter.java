package org.esa.beam.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.geotiff.GeoTIFFMetadata;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.media.jai.operator.BandMergeDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class GeoTIFFProductWriter extends AbstractProductWriter {

    private static final int LEVEL_ZERO = 0;

    /**
     * Constructs a <code>GeoTIFFProductWriter</code>. Since no output destination is set, the <code>setOutput</code>
     * method must be called before data can be written.
     *
     * @param writerPlugIn the plug-in which created this writer, must not be <code>null</code>
     *
     * @throws IllegalArgumentException
     * @see #writeGeoTIFFProduct
     */
    public GeoTIFFProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        final File outputFile = Utils.getFile(getOutput());
        final FileOutputStream outputStream = new FileOutputStream(outputFile);

        try {
            writeGeoTIFFProduct(outputStream, getSourceProduct());
        } finally {
            outputStream.close();
        }

    }

    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                    int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }

    public void deleteOutput() throws IOException {
    }

    static void writeGeoTIFFProduct(OutputStream outputStream, Product product) throws IOException {
        final ImageContainer imageContainer = createImageContainer(product);
        ImageEncoder enc = ImageCodec.createImageEncoder("TIFF", outputStream, imageContainer.getEncodeParam());
        enc.encode(imageContainer.getImage());
    }

    static ImageContainer createImageContainer(Product product) {
        final Band[] bands = product.getBands();
        final int bufferType = getLeastCommonDataBufferType(bands);
        final RenderedImage[] renderedImages = ImageManager.getInstance().getBandImages(bands, LEVEL_ZERO);
        for (int i = 0; i < renderedImages.length; i++) {
            renderedImages[i] = FormatDescriptor.create(renderedImages[i], bufferType, null);
        }
        RenderedImage masterImage = renderedImages[0];
        for (int i = 1; i < renderedImages.length; i++) {
            RenderedImage renderedImage = renderedImages[i];
            masterImage = BandMergeDescriptor.create(masterImage, renderedImage, null);
        }

        final List<TIFFField> tiffFieldList = createGeoTiffFields(product);
        tiffFieldList.add(new TIFFField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
                                        TIFFField.TIFF_ASCII,
                                        1,
                                        new String[]{product.getName()}));
        TIFFEncodeParam masterParam = new TIFFEncodeParam();


        final BeamMetadata.Metadata metadata = BeamMetadata.createMetadata(product);
        final Document dom = metadata.getDocument();
        final StringWriter writer = new StringWriter();
        try {
            new XMLOutputter(Format.getCompactFormat()).output(dom, writer);
        } catch (IOException ignore) {
        }

        tiffFieldList.add(new TIFFField(
                BeamMetadata.PRIVATE_TIFF_TAG_NUMBER,
                TIFFField.TIFF_ASCII,
                1,
                new String[]{writer.toString()}
        ));

        masterParam.setExtraFields(tiffFieldList.toArray(new TIFFField[tiffFieldList.size()]));

        return new ImageContainer(masterImage, masterParam);
    }

    private static List<TIFFField> createGeoTiffFields(final Product product) {
        final GeoTIFFMetadata geoTIFFMetadata = ProductUtils.createGeoTIFFMetadata(
                product.getGeoCoding(), product.getSceneRasterWidth(), product.getSceneRasterHeight());
        if(geoTIFFMetadata != null) {
            return Utils.createGeoTIFFFields(geoTIFFMetadata);
        }

        return new ArrayList<TIFFField>(0);
    }

    /**
     * Returns the least common {@link DataBuffer} type of
     * all the bands in the given array of bands.
     * @param bands the given array of bands to analyze.
     * @return the least common {@link DataBuffer} type of all given bands
     */
    static int getLeastCommonDataBufferType(final Band[] bands) {
        int maxSignedIntType = -1;
        int maxUnsignedIntType = -1;
        int maxFloatType = -1;
        for (Band band : bands) {
            int dt = band.getDataType();
            if (ProductData.isIntType(dt)) {
                if (ProductData.isUIntType(dt)) {
                    maxUnsignedIntType = Math.max(maxUnsignedIntType, dt);
                } else {
                    maxSignedIntType = Math.max(maxSignedIntType, dt);
                }
            }
            if (ProductData.isFloatingPointType(dt)) {
                maxFloatType = Math.max(maxFloatType, dt);
            }
        }

        if (maxFloatType == ProductData.TYPE_FLOAT64) {
            return DataBuffer.TYPE_DOUBLE;
        }

        if (maxFloatType != -1) {
            if (maxSignedIntType > ProductData.TYPE_INT16 || maxUnsignedIntType > ProductData.TYPE_UINT16) {
                return DataBuffer.TYPE_DOUBLE;
            } else {
                return DataBuffer.TYPE_FLOAT;
            }
        }

        if (maxUnsignedIntType != -1) {
            if (maxSignedIntType == -1) {
                return ImageManager.getDataBufferType(maxUnsignedIntType);
            }
            if (ProductData.getElemSize(maxUnsignedIntType) >= ProductData.getElemSize(maxSignedIntType)) {
                int returnType = maxUnsignedIntType - 10 + 1;
                if (returnType > 12) {
                    return DataBuffer.TYPE_DOUBLE;
                } else {
                    return ImageManager.getDataBufferType(returnType);
                }
            }
        }

        if (maxSignedIntType != -1) {
            return ImageManager.getDataBufferType(maxSignedIntType);
        }

        return DataBuffer.TYPE_UNDEFINED;
    }

    static class ImageContainer {

        private RenderedImage image;
        private ImageEncodeParam encodeParam;

        ImageContainer(RenderedImage image, ImageEncodeParam encodeParam) {
            this.image = image;
            this.encodeParam = encodeParam;
        }

        public RenderedImage getImage() {
            return image;
        }

        public ImageEncodeParam getEncodeParam() {
            return encodeParam;
        }
    }
}
