/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.jai.JAIUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.esa.beam.dataio.spot.SpotVgtProductReaderPlugIn.*;

/**
 * Reader for SPOT VGT products.
 *
 * @author Norman Fomferra
 * @version 1.0
 */
public class SpotVgtProductReader extends AbstractProductReader {

    private static final String BAND_INFO_PROPERTIES = "band-info.properties";
    private HashMap<Band, FileVar> fileVars;
    private VirtualDir virtualDir;
    private Properties bandInfos;
    private static final String[] PIXEL_DATA_VAR_NAMES = new String[]{
            "PIXEL_DATA",
            "PIXEL DATA",
            "ANGLES_VALUES",
            "ANGLES VALUES",
            "MEASURE_VALUE",
            "MEASURE VALUE",
    };

    /**
     * Constructor.
     *
     * @param productReaderPlugIn the product reader plug-in used to create this reader instance.
     */
    SpotVgtProductReader(final SpotVgtProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
        initBandInfos();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = getFileInput(getInput());
        virtualDir = VirtualDir.create(inputFile);
        return createProduct();
    }

    private Product createProduct() throws IOException {
        PhysVolDescriptor physVolDescriptor = new PhysVolDescriptor(
                virtualDir.getReader(SpotVgtConstants.PHYS_VOL_FILENAME));
        LogVolDescriptor logVolDescriptor = new LogVolDescriptor(
                virtualDir.getReader(physVolDescriptor.getLogVolDescriptorFileName()));

        Rectangle imageBounds = logVolDescriptor.getImageBounds();

        fileVars = new HashMap<Band, FileVar>(33);

        int targetWidth = imageBounds.width;
        int targetHeight = imageBounds.height;
        Product product = new Product(logVolDescriptor.getProductId(),
                                      physVolDescriptor.getFormatReference(),
                                      targetWidth,
                                      targetHeight, this);
        Dimension tileSize = JAIUtils.computePreferredTileSize(targetWidth, targetHeight, 1);
        product.setPreferredTileSize(tileSize);
        product.setFileLocation(new File(virtualDir.getBasePath()));
        addGeoCoding(product, logVolDescriptor);
        addTimeCoding(product, logVolDescriptor);
        addMetadata(product, physVolDescriptor, logVolDescriptor);

        String[] logVolFileNames = virtualDir.list(physVolDescriptor.getLogVolDirName());
        for (String logVolFileName : logVolFileNames) {

            if (logVolFileName.endsWith(".hdf") || logVolFileName.endsWith(".HDF")) {

                File hdfFile = virtualDir.getFile(physVolDescriptor.getLogVolDirName() + "/" + logVolFileName);
                NetcdfFile netcdfFile = NetcdfFile.open(hdfFile.getPath());

                Variable variable = findPixelDataVariable(netcdfFile);
                if (isPotentialPixelDataVariable(variable)) {
                    DataType netCdfDataType = variable.getDataType();
                    int bandDataType = convertNetcdfTypeToProductDataType(netCdfDataType, variable.isUnsigned());
                    if (bandDataType != ProductData.TYPE_UNDEFINED) {
                        String bandName = getBandName(logVolFileName);
                        BandInfo bandInfo = getBandInfo(bandName);

                        // Check if we know about this variable (bandInfo != null)
                        //
                        if (bandInfo != null) {
                            // SPOT VGT P Products contain sub-sampled variables.
                            // Need to check whether source raster resolution is at target raster resolution.
                            //
                            int sourceWidth = variable.getDimension(1).getLength();
                            int sourceHeight = variable.getDimension(0).getLength();
                            int sampling = bandInfo.pSampling;
                            if (sampling == 1 || sourceWidth == targetWidth || sourceHeight == targetHeight) {
                                // Source raster resolution is at target raster resolution.
                                addBand(product, bandDataType, bandInfo, netcdfFile, variable);
                            } else if (sampling > 1 || sourceWidth <= targetWidth || sourceHeight <= targetHeight) {
                                // Source raster resolution is a sub-sampling.
                                try {
                                    ProductData data = readData(variable, bandDataType, sourceWidth, sourceHeight);
                                    RenderedOp dstImg = createScaledImage(targetWidth, targetHeight, sourceWidth,
                                                                          sourceHeight, sampling, data, tileSize);
                                    Band band = addBand(product, bandDataType, bandInfo, netcdfFile, variable);
                                    band.setSourceImage(dstImg);
                                } catch (IOException e) {
                                    // band not added
                                } catch (InvalidRangeException e) {
                                    // band not added
                                }
                            } else {
                                // band not added
                            }
                        }
                    }
                }
            }
        }

        addFlagsAndMasks(product);
        addSpectralInfo(product);

        return product;
    }

    private int convertNetcdfTypeToProductDataType(DataType netCdfDataType, boolean unsigned) {
        if (netCdfDataType == DataType.BYTE) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (netCdfDataType == DataType.SHORT) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (netCdfDataType == DataType.INT) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (netCdfDataType == DataType.FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (netCdfDataType == DataType.DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        }
        return ProductData.TYPE_UNDEFINED;
    }

    private boolean isPotentialPixelDataVariable(Variable variable) {
        return variable != null && variable.getRank() == 2 && variable.getDataType().isNumeric();
    }

    private static ProductData readData(Variable variable, int bandDataType, int rasterWidth, int rasterHeight) throws
                                                                                                                IOException,
                                                                                                                InvalidRangeException {
        ProductData data = ProductData.createInstance(bandDataType, rasterWidth * rasterHeight);
        read(variable, 0, 0, rasterWidth, rasterHeight, data);
        return data;
    }

    private static RenderedOp createScaledImage(int targetWidth, int targetHeight, int sourceWidth, int sourceHeight,
                                                int sourceSampling, ProductData data, Dimension tileSize) {
        int tempW = sourceWidth * sourceSampling + 1;
        int tempH = sourceHeight * sourceSampling + 1;
        float xScale = (float) tempW / (float) sourceWidth;
        float yScale = (float) tempH / (float) sourceHeight;
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                           BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        RenderedImage srcImg = ImageUtils.createRenderedImage(sourceWidth, sourceHeight, data);
        ImageLayout imageLayout = new ImageLayout(0, 0, targetWidth, targetHeight,
                0, 0, tileSize.width, tileSize.height, null, null);
        renderingHints.put(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale,
                                                    -0.5f * sourceSampling + 1,
                                                    -0.5f * sourceSampling + 1,
                                                    Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                                                    renderingHints);

        return CropDescriptor.create(tempImg, 0f, 0f, (float) targetWidth, (float) targetHeight, null);
    }

    private Band addBand(Product product, int bandDataType, BandInfo bandInfo, NetcdfFile netcdfFile,
                         Variable variable) {
        Band band = product.addBand(bandInfo.name, bandDataType);
        band.setScalingFactor(bandInfo.coefA);
        band.setScalingOffset(bandInfo.offsetB);
        band.setUnit(bandInfo.unit);
        band.setDescription(bandInfo.description);
        fileVars.put(band, new FileVar(netcdfFile, variable));
        return band;
    }

    private Variable findPixelDataVariable(NetcdfFile netcdfFile) {
        for (String name : PIXEL_DATA_VAR_NAMES) {
            Variable pixelDataVar = netcdfFile.getRootGroup().findVariable(name);
            if (pixelDataVar != null) {
                return pixelDataVar;
            }
        }
        //System.out.println("No variable found in file file " + netcdfFile);
        return null;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer,
                                          ProgressMonitor pm) throws IOException {
        Assert.state(sourceOffsetX == targetOffsetX, "sourceOffsetX != targetOffsetX");
        Assert.state(sourceOffsetY == targetOffsetY, "sourceOffsetY != targetOffsetY");
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        FileVar fileVar = fileVars.get(targetBand);
        if (fileVar == null) {
            return;
        }
        final Variable variable = fileVar.var;
        synchronized (variable) {
            try {
                read(variable, targetOffsetX, targetOffsetY, targetWidth, targetHeight, targetBuffer);
            } catch (InvalidRangeException e) {
                // ?
            }
        }
    }

    private static void read(Variable variable,
                             int targetOffsetX, int targetOffsetY,
                             int targetWidth, int targetHeight,
                             ProductData targetBuffer) throws IOException, InvalidRangeException {
        Array array = variable.read(new int[]{targetOffsetY, targetOffsetX},
                                    new int[]{targetHeight, targetWidth});
        System.arraycopy(array.getStorage(),
                         0,
                         targetBuffer.getElems(),
                         0, targetWidth * targetHeight);
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<Band, FileVar> entry : fileVars.entrySet()) {
            NetcdfFile netcdfFile = entry.getValue().file;
            try {
                netcdfFile.close();
            } catch (IOException e) {
                // ok
            }
        }
        fileVars.clear();
        virtualDir.close();
        super.close();
    }

    private void addGeoCoding(Product product, LogVolDescriptor logVolDescriptor) {
        GeoCoding geoCoding = logVolDescriptor.getGeoCoding();
        if (geoCoding != null) {
            product.setGeoCoding(geoCoding);
        }
    }

    private void addMetadata(Product product, PhysVolDescriptor physVolDescriptor, LogVolDescriptor logVolDescriptor) {
        product.getMetadataRoot().addElement(createMetadataElement("PHYS_VOL",
                                                                   "Physical volume descriptor",
                                                                   physVolDescriptor.getPropertySet().getProperties()));
        product.getMetadataRoot().addElement(createMetadataElement("LOG_VOL",
                                                                   "Logical volume descriptor",
                                                                   logVolDescriptor.getPropertySet().getProperties()));
    }

    private void addFlagsAndMasks(Product product) {
        Band smBand = product.getBand("SM");
        if (smBand != null) {
            FlagCoding flagCoding = new FlagCoding("SM");
            flagCoding.addFlag("B0_GOOD", 0x80, "Radiometric quality for band B0 is good.");
            flagCoding.addFlag("B2_GOOD", 0x40, "Radiometric quality for band B2 is good.");
            flagCoding.addFlag("B3_GOOD", 0x20, "Radiometric quality for band B3 is good.");
            flagCoding.addFlag("MIR_GOOD", 0x10, "Radiometric quality for band MIR is good.");
            flagCoding.addFlag("LAND", 0x08, "Land code 1 or water code 0.");
            flagCoding.addFlag("ICE_SNOW", 0x04, "Ice/snow code 1, code 0 if there is no ice/snow");
            flagCoding.addFlag("CLOUD_2", 0x02, "");
            flagCoding.addFlag("CLOUD_1", 0x01, "");
            product.getFlagCodingGroup().add(flagCoding);
            smBand.setSampleCoding(flagCoding);

            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (band != smBand) {
                    if (isSpectralBand(band)) {  // P + S1 + S10 products
                        band.setValidPixelExpression("SM." + band.getName() + "_GOOD");
                    } else if (!isSubsampledBand(band)) { // S1 + S10 products
                        band.setValidPixelExpression("SM.LAND");
                    }
                }
            }

            product.addMask("B0_BAD", "!SM.B0_GOOD", "Radiometric quality for band B0 is bad.",
                            Color.RED, 0.2);
            product.addMask("B2_BAD", "!SM.B2_GOOD", "Radiometric quality for band B2 is bad.",
                            Color.RED, 0.2);
            product.addMask("B3_BAD", "!SM.B3_GOOD", "Radiometric quality for band B3 is bad.",
                            Color.RED, 0.2);
            product.addMask("MIR_BAD", "!SM.MIR_GOOD", "Radiometric quality for band MIR is bad.",
                            Color.RED, 0.2);
            product.addMask("LAND", "SM.LAND", "Land mask.",
                            Color.GREEN, 0.5);
            product.addMask("WATER", "!SM.LAND", "Water mask.",
                            Color.BLUE, 0.5);
            product.addMask("ICE_SNOW", "SM.ICE_SNOW", "Ice/snow mask.",
                            Color.MAGENTA, 0.5);
            product.addMask("CLEAR", "!SM.CLOUD_1 && !SM.CLOUD_2", "Clear sky.",
                            Color.ORANGE, 0.5);
            product.addMask("CLOUD_SHADOW", "SM.CLOUD_1 && !SM.CLOUD_2", "Cloud shadow.",
                            Color.CYAN, 0.5);
            product.addMask("CLOUD_UNCERTAIN", "!SM.CLOUD_1 && SM.CLOUD_2", "Cloud uncertain.",
                            Color.ORANGE, 0.5);
            product.addMask("CLOUD", "SM.CLOUD_1 && SM.CLOUD_2", "Cloud certain.",
                            Color.YELLOW, 0.5);
        }
    }

    private boolean isSubsampledBand(Band band) {
        return band.isSourceImageSet();
    }

    private boolean isSpectralBand(Band band) {
        return band.getName().equals("B0") || band.getName().equals("B2") || band.getName().equals(
                "B3") || band.getName().equals("MIR");
    }

    private void addSpectralInfo(Product product) {
        addSpectralInfo(product, "B0", 0, 430, 470, 1.963400e+03f);
        addSpectralInfo(product, "B2", 1, 610, 680, 1.570300e+03f);
        addSpectralInfo(product, "B3", 2, 780, 890, 1.045600e+03f);
        addSpectralInfo(product, "MIR", 3, 1580, 1750, 2.347000e+02f);
    }

    private void addSpectralInfo(Product product, String name, int index, float min, float max, float solFlux) {
        Band spectralBand = product.getBand(name);
        if (spectralBand != null) {
            spectralBand.setSpectralBandIndex(index);
            spectralBand.setSpectralWavelength(min + 0.5f * (max - min));
            spectralBand.setSpectralBandwidth(max - min);
            spectralBand.setSolarFlux(solFlux);
            spectralBand.setDescription(MessageFormat.format("{0} spectral band", name));
        }
    }

    private MetadataElement createMetadataElement(String name, String description, Property[] properties) {
        MetadataElement element = new MetadataElement(name);
        element.setDescription(description);
        for (Property property : properties) {
            element.addAttribute(new MetadataAttribute(property.getName(),
                                                       ProductData.createInstance(property.getValueAsText()), true));
        }
        return element;
    }

    private void addTimeCoding(Product product, LogVolDescriptor logVolDescriptor) {
        Date startDate = logVolDescriptor.getStartDate();
        if (startDate != null) {
            product.setStartTime(ProductData.UTC.create(startDate, 0));
        }
        Date endDate = logVolDescriptor.getEndDate();
        if (endDate != null) {
            product.setEndTime(ProductData.UTC.create(endDate, 0));
        }
    }

    private void initBandInfos() {
        bandInfos = new Properties();
        try {
            InputStream stream = getClass().getResourceAsStream(BAND_INFO_PROPERTIES);
            bandInfos.load(stream);
            stream.close();
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format("Failed to load resource {0}: {1}",
                                                                 BAND_INFO_PROPERTIES, e.getMessage()), e);
        }

    }

    BandInfo getBandInfo(String name) {
        final String coefA = bandInfos.getProperty(name + ".COEF_A");
        final String offsetB = bandInfos.getProperty(name + ".OFFSET_B");
        final String sampling = bandInfos.getProperty(name + ".SAMPLING");
        final String unit = bandInfos.getProperty(name + ".UNIT");
        final String description = bandInfos.getProperty(name + ".DESCRIPTION");
        if (coefA != null || offsetB != null || unit != null || description != null || sampling != null) {
            try {
                return new BandInfo(name,
                                    coefA != null ? Double.parseDouble(coefA) : 1.0,
                                    offsetB != null ? Double.parseDouble(offsetB) : 0.0,
                                    sampling != null ? Integer.parseInt(sampling) : 1,
                                    unit,
                                    description);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static class BandInfo {

        private BandInfo(String name, double coefA, double offsetB, int pSampling, String unit, String description) {
            this.name = name;
            this.coefA = coefA;
            this.offsetB = offsetB;
            this.pSampling = pSampling;
            this.unit = unit;
            this.description = description;
        }

        private final String name;
        private final double coefA;
        private final double offsetB;
        private final int pSampling;
        private final String unit;
        private final String description;
    }

    private static class FileVar {

        final NetcdfFile file;
        final Variable var;

        private FileVar(NetcdfFile file, Variable var) {
            this.file = file;
            this.var = var;
        }
    }

}
