package org.esa.s1tbx.io.gamma;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.dataio.AbstractProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.io.FileUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reader for stamps insar products
 */
public class GammaReader extends AbstractProductReader {

    private Header header;
    private boolean isComplex = false;
    private boolean isCoregistered = false;
    private ImageInputStream inStream;

    public GammaReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected Product readProductNodesImpl() throws IOException {
        final File inputParFile = ReaderUtils.getFileFromInput(getInput());
        final BufferedReader headerReader = new BufferedReader(new FileReader(inputParFile));

        try {
            header = new Header(headerReader);
            isComplex = isComplex(inputParFile);
            isCoregistered = isCoregistered(inputParFile);

            String productType = isComplex ? "SLC" : header.getDataType();
            final Product product = new Product(header.getName(), productType,
                                                header.getNumSamples(), header.getNumLines());
            product.setProductReader(this);
            product.setFileLocation(inputParFile);
            product.setDescription(header.getDescription());
            product.getMetadataRoot().addElement(header.getAsMetadata());

            final int dataType = getDataType();
            if (isComplex) {
                final Band tgtBandI = new Band("i_band", dataType, header.getNumSamples(), header.getNumLines());
                tgtBandI.setUnit("real");
                product.addBand(tgtBandI);

                final Band tgtBandQ = new Band("q_band", dataType, header.getNumSamples(), header.getNumLines());
                tgtBandQ.setUnit("imaginary");
                product.addBand(tgtBandQ);

                ReaderUtils.createVirtualIntensityBand(product, tgtBandI, tgtBandQ, "_band");
            } else {
                String bandName = getImageFile(inputParFile).getName();
                final Band tgtBandI = new Band(bandName, dataType, header.getNumSamples(), header.getNumLines());
                product.addBand(tgtBandI);
            }

            inStream = new FileImageInputStream(getImageFile(inputParFile));
            inStream.setByteOrder(header.getJavaByteOrder());

            addGeoCoding(product);

            addMetaData(product);

            return product;
        } finally {
            headerReader.close();
        }
    }

    private boolean isComplex(final File file) {
        String name = file.getName().toLowerCase();
        name = FileUtils.getFilenameWithoutExtension(name);
        String ext = FileUtils.getExtension(name);
        return ext != null && ext.endsWith("slc");
    }

    private boolean isCoregistered(final File file) {
        String name = file.getName().toLowerCase();
        name = FileUtils.getFilenameWithoutExtension(name);
        String ext = FileUtils.getExtension(name);
        return ext != null && ext.endsWith("rslc");
    }

    private int getDataType() throws IOException {
        final String imageFormat = header.getDataType();
        if ("SCOMPLEX".equals(imageFormat)) {
            return ProductData.TYPE_INT16;
        } else if ("FCOMPLEX".endsWith(imageFormat)) {
            return ProductData.TYPE_FLOAT32;
        }
        return ProductData.TYPE_FLOAT32;
    }

    private File getImageFile(final File parFile) {
        return new File(parFile.getParentFile(), FileUtils.getFilenameWithoutExtension(parFile));
    }

    private void addGeoCoding(final Product product) {

    }

    private void addMetaData(final Product product) {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, header.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, header.getSensorType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, header.getDescription());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, isComplex ? "SLC" : header.getDataType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.sample_type, isComplex ? "COMPLEX" : "DETECTED");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, header.getNumSamples());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, header.getNumLines());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, header.getStartTime());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, header.getEndTime());
        product.setStartTime(header.getStartTime());
        product.setEndTime(header.getEndTime());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, header.getLineTimeInterval());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, header.getRadarFrequency());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, header.getPRF());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, header.getRangeLooks());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, header.getAzimuthLooks());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, isCoregistered ? 1 : 0);

    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (isComplex) {

            final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
            Product product = destBand.getProduct();
            final int elemSize = destBuffer.getElemSize();

            final int headerOffset = header.getHeaderOffset();
            final int bandIndex = product.getBandIndex(destBand.getName());

            // band interleaved by pixel
            int numBands = 2;
            final long lineSizeInBytes = header.getNumSamples() * numBands * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth * numBands);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (inStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        inStream.seek(lineStartPos + elemSize * sourceOffsetX * numBands);
                        lineData.readFrom(0, sourceWidth * numBands, inStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x * numBands + bandIndex));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }

        } else {
            final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
            Product product = destBand.getProduct();
            final int elemSize = destBuffer.getElemSize();

            final int headerOffset = header.getHeaderOffset();
            final int bandIndex = product.getBandIndex(destBand.getName());

            final long lineSizeInBytes = header.getNumSamples() * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (inStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        inStream.seek(lineStartPos + elemSize * sourceOffsetX);
                        lineData.readFrom(0, sourceWidth, inStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x + bandIndex));
                    }
                    pm.worked(1);
                }
            }catch (Exception e) {

            } finally {
                pm.done();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (inStream != null) {
            inStream.close();
        }
        super.close();
    }
}
