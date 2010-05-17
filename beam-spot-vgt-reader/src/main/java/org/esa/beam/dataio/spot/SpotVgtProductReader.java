/*
 * Copyright (C) 2010  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.esa.beam.dataio.spot.SpotVgtProductReaderPlugIn.getBandName;
import static org.esa.beam.dataio.spot.SpotVgtProductReaderPlugIn.getFileInput;

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
        PhysVolDescriptor physVolDescriptor = new PhysVolDescriptor(virtualDir.getReader(SpotVgtConstants.PHYS_VOL_FILENAME));
        LogVolDescriptor logVolDescriptor = new LogVolDescriptor(virtualDir.getReader(physVolDescriptor.getLogVolDescriptorFileName()));

        fileVars = new HashMap<Band, FileVar>(33);

        String[] logVolFileNames = virtualDir.list(physVolDescriptor.getLogVolDirName());
        Product product = null;
        for (String logVolFileName : logVolFileNames) {

            if (logVolFileName.endsWith(".hdf") || logVolFileName.endsWith(".HDF")) {

                File hdfFile = virtualDir.getFile(physVolDescriptor.getLogVolDirName() + "/" + logVolFileName);
                NetcdfFile netcdfFile = NetcdfFile.open(hdfFile.getPath());

                HashMap<String, Variable> variables = new HashMap<String, Variable>();
                for (Variable variable : netcdfFile.getVariables()) {
                    variables.put(variable.getName(), variable);
                }

                Variable pixelDataVar = variables.get("PIXEL_DATA");
                if (pixelDataVar == null) {
                    pixelDataVar = variables.get("PIXEL DATA");
                }
                if (pixelDataVar == null) {
                    pixelDataVar = variables.get("ANGLES_VALUES");
                }
                if (pixelDataVar == null) {
                    pixelDataVar = variables.get("ANGLES VALUES");
                }
                if (pixelDataVar != null && pixelDataVar.getRank() == 2 && pixelDataVar.getDataType().isNumeric()) {
                    DataType netCdfDataType = pixelDataVar.getDataType();
                    int bandDataType = ProductData.TYPE_UNDEFINED;
                    if (Byte.TYPE == netCdfDataType.getPrimitiveClassType()) {
                        bandDataType = ProductData.TYPE_INT8;
                    } else if (Short.TYPE == netCdfDataType.getPrimitiveClassType()) {
                        bandDataType = ProductData.TYPE_INT16;
                    } else if (Integer.TYPE == netCdfDataType.getPrimitiveClassType()) {
                        bandDataType = ProductData.TYPE_INT32;
                    } else if (Float.TYPE == netCdfDataType.getPrimitiveClassType()) {
                        bandDataType = ProductData.TYPE_FLOAT32;
                    } else if (Double.TYPE == netCdfDataType.getPrimitiveClassType()) {
                        bandDataType = ProductData.TYPE_FLOAT64;
                    }
                    if (bandDataType != ProductData.TYPE_UNDEFINED) {
                        if (product == null) {
                            product = new Product(logVolDescriptor.getProductId(),
                                                  physVolDescriptor.getFormatReference(),
                                                  pixelDataVar.getDimension(1).getLength(),
                                                  pixelDataVar.getDimension(0).getLength(), this);
                            product.setFileLocation(new File(virtualDir.getBasePath()));
                        }
                        Band band = product.addBand(getBandName(logVolFileName), bandDataType);
                        BandInfo bandInfo = getBandInfo(band.getName());
                        if (bandInfo != null) {
                            band.setScalingFactor(bandInfo.coefA);
                            band.setScalingOffset(bandInfo.offsetB);
                            band.setUnit(bandInfo.unit);
                        }
                        fileVars.put(band, new FileVar(netcdfFile, pixelDataVar));
                    }
                }
            }
        }

        addMetadata(product, physVolDescriptor, logVolDescriptor);
        addFlagsAndMasks(product);
        addSpectralInfo(product);
        addGeoCoding(product, logVolDescriptor);
        addTimeCoding(product, logVolDescriptor);

        return product;
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
                Array array = variable.read(new int[]{targetOffsetY, targetOffsetX},
                                            new int[]{targetHeight, targetWidth});
                System.arraycopy(array.getStorage(),
                                 0,
                                 targetBuffer.getElems(),
                                 0, targetWidth * targetHeight);
            } catch (InvalidRangeException e) {
                // ?
            }
        }
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
            flagCoding.addFlag("B0_OK", 0x80, "Radiometric quality for band B0 is good.");
            flagCoding.addFlag("B2_OK", 0x40, "Radiometric quality for band B2 is good.");
            flagCoding.addFlag("B3_OK", 0x20, "Radiometric quality for band B3 is good.");
            flagCoding.addFlag("MIR_OK", 0x10, "Radiometric quality for band MIR is good.");
            flagCoding.addFlag("LAND", 0x08, "Land code 1 or water code 0.");
            flagCoding.addFlag("ICE_SNOW", 0x04, "Ice/snow code 1, code 0 if there is no ice/snow");
            flagCoding.addFlag("CLOUD_2", 0x02, "");
            flagCoding.addFlag("CLOUD_1", 0x01, "");
            product.getFlagCodingGroup().add(flagCoding);
            smBand.setSampleCoding(flagCoding);

            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (band != smBand) {
                    band.setValidPixelExpression("SM.LAND");
                }
            }

            product.getMaskGroup().add(Mask.BandMathsType.create("B0_BAD", "Radiometric quality for band B0 is bad.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.B0_OK",
                                                                 Color.RED, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("B2_BAD", "Radiometric quality for band B2 is bad.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.B2_OK",
                                                                 Color.RED, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("B3_BAD", "Radiometric quality for band B3 is bad.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.B3_OK",
                                                                 Color.RED, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("MIR_BAD", "Radiometric quality for band MIR is bad.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.MIR_OK",
                                                                 Color.RED, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("LAND", "Land mask.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "SM.LAND",
                                                                 Color.GREEN, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("WATER", "Water mask.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.LAND",
                                                                 Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("ICE_SNOW", "Ice/snow mask.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "SM.ICE_SNOW",
                                                                 Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLEAR", "Clear sky.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.CLOUD_1 && !SM.CLOUD_2",
                                                                 Color.ORANGE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD_SHADOW", "Cloud shadow.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "SM.CLOUD_1 && !SM.CLOUD_2",
                                                                 Color.CYAN, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD_UNCERTAIN", "Cloud uncertain.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "!SM.CLOUD_1 && SM.CLOUD_2",
                                                                 Color.ORANGE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD", "Cloud certain.",
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), "SM.CLOUD_1 && SM.CLOUD_2",
                                                                 Color.YELLOW, 0.5));
        }
    }

    private void addSpectralInfo(Product product) {
        addSpectralInfo(product, "B0", 0, 430, 470);
        addSpectralInfo(product, "B2", 1, 610, 680);
        addSpectralInfo(product, "B3", 2, 780, 890);
        addSpectralInfo(product, "MIR", 3, 1580, 1750);
    }

    private void addSpectralInfo(Product product, String name, int index, float min, float max) {
        if (product.getBand(name) != null) {
            product.getBand(name).setSpectralBandIndex(index);
            product.getBand(name).setSpectralWavelength(min + 0.5f * (max - min));
            product.getBand(name).setSpectralBandwidth(max - min);
            product.getBand(name).setDescription(MessageFormat.format("{0} spectral band", name));
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
        final String unit = bandInfos.getProperty(name + ".UNIT");
        if (coefA != null || offsetB != null || unit != null) {
            try {
                return new BandInfo(coefA != null ? Double.parseDouble(coefA) : 1.0,
                                    offsetB != null ? Double.parseDouble(offsetB) : 0.0,
                                    unit);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static class BandInfo {
        private BandInfo(double coefA, double offsetB, String unit) {
            this.coefA = coefA;
            this.offsetB = offsetB;
            this.unit = unit;
        }

        final double coefA;
        final double offsetB;
        final String unit;
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
