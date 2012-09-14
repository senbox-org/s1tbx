package org.esa.nest.dataio.generic;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.ComplexBinaryDialog;
import org.esa.nest.dataio.FileImageInputStreamExtImpl;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.ReaderUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

public class GenericComplexReader extends GenericReader {

    private int rasterWidth = 0;
    private int rasterHeight = 0;
//    private int numBands = 1;
    private int dataType = ProductData.TYPE_INT16;
    private ByteOrder byteOrder = ByteOrder.nativeOrder();

    private int imageRecordLength = rasterWidth;
    private int _startPosImageRecords = 0;
    private int _imageHeaderLength = 0;
    private ImageInputStream imageInputStream = null;

    private BinaryFileReader binaryReader = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public GenericComplexReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        if (VisatApp.getApp() != null) {
            //if in DAT then open options dialog
            final ComplexBinaryDialog dialog = new ComplexBinaryDialog(VisatApp.getApp().getMainFrame(), "importGenericBinary");
            if (dialog.show() == ModalDialog.ID_OK) {
                rasterWidth = dialog.getRasterWidth();
                rasterHeight = dialog.getRasterHeight();
//                numBands = dialog.getNumBands();
                dataType = dialog.getDataType();
                byteOrder = dialog.getByteOrder();
                _imageHeaderLength = dialog.getHeaderBytes();
                switch (dataType) {
                    case ProductData.TYPE_INT16:
                        imageRecordLength = rasterWidth * ProductData.getElemSize(ProductData.TYPE_INT16) * 2;
                        break;
                    case ProductData.TYPE_INT32:
                        imageRecordLength = rasterWidth * ProductData.getElemSize(ProductData.TYPE_INT32) * 2;
                        break;
                    case ProductData.TYPE_FLOAT32:
                        imageRecordLength = rasterWidth * ProductData.getElemSize(ProductData.TYPE_FLOAT32) * 2;
                        break;
                    default:
                        imageRecordLength = rasterWidth * ProductData.getElemSize(ProductData.TYPE_FLOAT32) * 2;
                        break;
                }
            } else {
                throw new IOException("Import Canceled");
            }
        }


        final File inputFile = ReaderUtils.getFileFromInput(getInput());

        final Product product = new Product(inputFile.getName(), "Complex", rasterWidth, rasterHeight);

        final Band bandI = new Band("i", dataType, rasterWidth, rasterHeight);
        final Band bandQ = new Band("q", dataType, rasterWidth, rasterHeight);

        bandI.setDescription("i");
        bandI.setUnit(Unit.REAL);
        bandQ.setDescription("q");
        bandQ.setUnit(Unit.IMAGINARY);

        product.addBand(bandI);
        product.addBand(bandQ);

        ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, "");
        ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, "");

        addMetaData(product, inputFile);

        product.setProductReader(this);
        product.setModified(false);
        product.setFileLocation(inputFile);

        imageInputStream = FileImageInputStreamExtImpl.createInputStream(inputFile);
        imageInputStream.setByteOrder(byteOrder);
        binaryReader = new BinaryFileReader(imageInputStream);
        binaryReader.setByteOrder(byteOrder);

        return product;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    static DecodeQualification checkProductQualification(File file) {
        return DecodeQualification.SUITABLE;
    }

    private static void addMetaData(final Product product, final File inputFile) throws IOException {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, product.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, product.getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, product.getSceneRasterHeight());
        AbstractMetadata.loadExternalMetadata(product, absRoot, inputFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        try {
            boolean oneOf2 = !destBand.getName().startsWith("q");

            switch (dataType) {
                case ProductData.TYPE_INT16:
                    readBandRasterDataSLCShort(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            _startPosImageRecords + _imageHeaderLength, imageRecordLength,
                            destWidth, destBuffer, oneOf2, binaryReader, pm);
                    break;
                case ProductData.TYPE_INT32:
                    readBandRasterDataIntSLC(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            _startPosImageRecords + _imageHeaderLength, oneOf2, imageInputStream,
                            destBand, destWidth, destBuffer, pm);
                    break;
                case ProductData.TYPE_FLOAT32:
                    readBandRasterDataSLCFloat(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            _startPosImageRecords + _imageHeaderLength, imageRecordLength,
                            destWidth, destBuffer, oneOf2, binaryReader, pm);
                default:
                    break;
            }


        } catch (Exception e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }

    }

}
