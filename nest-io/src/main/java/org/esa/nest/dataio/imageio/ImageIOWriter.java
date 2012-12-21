/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.imageio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.ReaderUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;


public class ImageIOWriter extends AbstractProductWriter {

    private File file;
    private ImageOutputStream _outputStream;
    private ImageWriter writer;
    private String format = "";
    private int[] dataArray = null;
    private int pos = 0;

    /**
     * Construct a new instance of a product writer for the given product writer plug-in.
     *
     * @param writerPlugIn the given product writer plug-in, must not be <code>null</code>
     */
    public ImageIOWriter(final ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Overwrite this method to set the format to write for writers which handle multiple formats.
     */
    public void setFormatName(final String formatName) {
        format = formatName;
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        _outputStream = null;

        file = ReaderUtils.getFileFromInput(getOutput());
        // ensure extension // this should not be done here
        if(!file.getName().toLowerCase().endsWith(format.toLowerCase())) {
            file = new File(file.getAbsolutePath()+'.'+format.toLowerCase());   
        }

        final Iterator<ImageWriter> writerList = ImageIO.getImageWritersBySuffix(format);
        writer = writerList.next();

        _outputStream = ImageIO.createImageOutputStream(file);
        writer.setOutput(_outputStream);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(getSourceProduct());
        AbstractMetadata.saveExternalMetadata(getSourceProduct(), absRoot, file);

        setIncrementalMode(false);
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(final Band sourceBand,
                                    final int sourceOffsetX,
                                    final int sourceOffsetY,
                                    final int sourceWidth,
                                    final int sourceHeight,
                                    final ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        try {
            final ImageWriteParam param = writer.getDefaultWriteParam();
            final boolean canWriteTiles = param.canWriteTiles();

       /*     if(canWriteTiles) {
                param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
                //param.setDestinationOffset(new Point(sourceOffsetX, sourceOffsetY));

                param.setSourceRegion(new Rectangle(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight));
                param.setTiling(sourceWidth, sourceHeight, sourceOffsetX, sourceOffsetY);
                writer.write(null, new IIOImage(sourceBand.getSourceImage(), null, null), param);
            } else {
                if(dataArray == null) {
                    dataArray = new int[sourceBand.getRasterWidth() * sourceBand.getRasterHeight()];
                }
                final int num = sourceBuffer.getNumElems();
                for(int i=0; i < num; ++i) {
                    dataArray[pos++] = (short)sourceBuffer.getElemIntAt(i);
                } */

                if(sourceHeight == sourceBand.getRasterHeight() || sourceOffsetY == sourceBand.getRasterHeight()-1) {
                    
                /*    RenderedImage img = createRenderedImage(dataArray,
                                                            sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
                    //writer.write(img);
                    //writer.write(null, new IIOImage(img, null, null), param);
                    //ImageIO.write(img, format, file);         */

                    writer.write(null, new IIOImage(sourceBand.getSourceImage(), null, null), param);
                }
        //    }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            if(VisatApp.getApp()!=null) {
                VisatApp.getApp().showErrorDialog(e.getMessage()+
                        "\nTry using convertDataType to convert to UInt8 or a data type supported by the image format");    
            }
        }
    }

    private static RenderedImage createRenderedImage(final int[] array, final int w, final int h) {

        // create rendered image with demension being width by height
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_INT, w, h, 1);
        final ColorModel colourModel = PlanarImage.createColorModel(sampleModel);
        final DataBufferInt dataBuffer = new DataBufferInt(array, array.length);
        final WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, dataBuffer, new Point(0,0));

        return new BufferedImage(colourModel, raster, false, new Hashtable());
    }

    /**
     * Deletes the physically representation of the given product from the hard disk.
     */
    public void deleteOutput() {

    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    public void flush() throws IOException {
        if (_outputStream != null) {
            _outputStream.flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        if (_outputStream != null) {
            _outputStream.flush();
            _outputStream.close();
            _outputStream = null;
        }
        if(writer != null) {
            writer.dispose();
        }
    }

    /**
     * Returns wether the given product node is to be written.
     *
     * @param node the product node
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && super.shouldWrite(node);
    }
}