package org.esa.beam.dataio.hico;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envi.EnviProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;

/**
 * Reader plugin for HICO data products.
 * http://hico.coas.oregonstate.edu/datasets/datacharacteristics.shtml
 */
class HicoProductReader extends AbstractProductReader {

    private enum FileType {
        RAD, GEOM, GEOM_PRECISE, RGB, NDVI, FLAG;

        static FileType fromString(String fileType) {
            if (fileType.equalsIgnoreCase("hico")) {
                return FileType.RAD;
            } else if (fileType.equalsIgnoreCase("hico_rad_geom")) {
                return FileType.GEOM;
            } else if (fileType.equalsIgnoreCase("hico_LonLatViewAngles")) {
                return FileType.GEOM_PRECISE;
            } else if (fileType.equalsIgnoreCase("hico_rad_rgb")) {
                return FileType.RGB;
            } else if (fileType.equalsIgnoreCase("hico_rad_ndvi")) {
                return FileType.NDVI;
            } else if (fileType.equalsIgnoreCase("hico_rad_flag")) {
                return FileType.FLAG;
            }
            throw new IllegalArgumentException("Unkown File type: " + fileType);
        }
    }

    private final EnumMap<FileType, Product> hicoProductParts = new EnumMap<FileType, Product>(FileType.class);

    HicoProductReader(HicoProductReaderPlugin hicoProductReaderPlugin) {
        super(hicoProductReaderPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File[] hdrFiles = HicoProductReaderPlugin.findHdrFiles(getInput());
        if (hdrFiles.length > 0) {
            return createProduct(hdrFiles);
        }
        throw new ProductIOException("Failed reading HICO product");
    }

    @Override
    public void close() throws IOException {
        super.close();
        for (Product product : hicoProductParts.values()) {
            product.dispose();
        }
    }

    private Product createProduct(File[] hdrFiles) throws IOException {
        ProductReaderPlugIn enviProductReaderPlugIn = new EnviProductReaderPlugIn();
        int sceneWidth = 0;
        int sceneHeight = 0;
        HicoFilename genericHicoFilename = null;
        for (File hdrFile : hdrFiles) {
            ProductReader enviProductReader = enviProductReaderPlugIn.createReaderInstance();
            Product product = enviProductReader.readProductNodes(hdrFile, null);
            if (sceneHeight == 0) {
                sceneHeight = product.getSceneRasterHeight();
                sceneWidth = product.getSceneRasterWidth();
            }
            HicoFilename hicoFilename = HicoFilename.create(hdrFile.getName());
            if (genericHicoFilename == null) {
                genericHicoFilename = hicoFilename;
            }
            FileType fileType = FileType.fromString(hicoFilename.getFileType());
            hicoProductParts.put(fileType, product);
        }
        Product product = new Product(genericHicoFilename.getProductBase(), genericHicoFilename.getProcessingLevel(),
                                      sceneWidth, sceneHeight);
        product.setDescription("HICO data product");
        handleRadianceProduct(product);
        handleFlagProduct(product);
        handleNdviProduct(product);
        handleGeomProduct(product);
        handleRgbProduct(product);
        return product;
    }

    private void handleRadianceProduct(Product product) {
        Product hicoProductPart = hicoProductParts.get(FileType.RAD);
        if (hicoProductPart != null) {
            String[] bandNames = hicoProductPart.getBandNames();
            for (String bandName : bandNames) {
                String[] bandNameSplit = bandName.split("_");
                String newBandname = "radiance_" + bandNameSplit[1];
                Band band = ProductUtils.copyBand(bandName, hicoProductPart, newBandname, product, true);
                band.setScalingFactor(1.0 / 50.0);
                band.setSpectralBandwidth(5.7f);
            }
            product.setAutoGrouping("radiance");
            product.setFileLocation(hicoProductPart.getFileLocation());
        }
    }

    private void handleGeomProduct(Product product) throws IOException {
        Product hicoProductPart = hicoProductParts.get(FileType.GEOM_PRECISE);
        if (hicoProductPart == null) {
            hicoProductPart = hicoProductParts.get(FileType.GEOM);
        }
        if (hicoProductPart != null) {
            String[] bandNames = hicoProductPart.getBandNames();
            Band latitudeBand = null;
            Band longitudeBand = null;
            for (String bandName : bandNames) {
                if (bandName.startsWith("latitude")) {
                    latitudeBand = hicoProductPart.getBand(bandName);
                } else if (bandName.startsWith("longitude")) {
                    longitudeBand = hicoProductPart.getBand(bandName);
                } else {
                    int i = bandName.indexOf("(");
                    String newBandname = bandName.substring(0, i).trim().replace(" ", "_");
                    ProductUtils.copyBand(bandName, hicoProductPart, newBandname, product, true);
                }
            }
            if (latitudeBand != null && longitudeBand != null) {
                int rasterWidth = latitudeBand.getSceneRasterWidth();
                int rasterHeight = latitudeBand.getSceneRasterHeight();

                // convert bands into tie-points
                // to create a tie-point geo-coding, because it is much faster than a pixel-geo-coding
                float[] latData = latitudeBand.readPixels(0, 0, rasterWidth, rasterHeight, (float[]) null);
                TiePointGrid tpLat = new TiePointGrid("latitude", rasterWidth, rasterHeight, 0.5f, 0.5f, 1f, 1f, latData);
                product.addTiePointGrid(tpLat);

                float[] lonData = longitudeBand.readPixels(0, 0, rasterWidth, rasterHeight, (float[]) null);
                TiePointGrid tpLon = new TiePointGrid("longitude", rasterWidth, rasterHeight, 0.5f, 0.5f, 1f, 1f, lonData);
                product.addTiePointGrid(tpLon);

                product.setGeoCoding(new TiePointGeoCoding(tpLat, tpLon));
            }
        }
    }

    private void handleNdviProduct(Product product) {
        Product hicoProductPart = hicoProductParts.get(FileType.NDVI);
        if (hicoProductPart != null) {
            String[] bandNames = hicoProductPart.getBandNames();
            for (String bandName : bandNames) {
                String newBandname = bandName.replace(" ", "_");
                ProductUtils.copyBand(bandName, hicoProductPart, newBandname, product, true);
            }
        }
    }


    private void handleRgbProduct(Product product) {
        Product hicoProductPart = hicoProductParts.get(FileType.RGB);
        if (hicoProductPart != null && hicoProductPart.getNumBands() == 3) {
            Band red = hicoProductPart.getBandAt(0);
            Band band = ProductUtils.copyBand(red.getName(), hicoProductPart, "red", product, true);
            band.setSpectralWavelength(0.0f);
            Band green = hicoProductPart.getBandAt(1);
            band = ProductUtils.copyBand(green.getName(), hicoProductPart, "green", product, true);
            band.setSpectralWavelength(0.0f);
            Band blue = hicoProductPart.getBandAt(2);
            band = ProductUtils.copyBand(blue.getName(), hicoProductPart, "blue", product, true);
            band.setSpectralWavelength(0.0f);
        }
    }

    private void handleFlagProduct(Product product) {
        Product hicoProductPart = hicoProductParts.get(FileType.FLAG);
        if (hicoProductPart != null) {
            Band flags = hicoProductPart.getBandAt(0);
            Band flagBand = ProductUtils.copyBand(flags.getName(), hicoProductPart, "flags", product, true);

            FlagCoding flagCoding = new FlagCoding("flag_coding");
            flagCoding.addFlag("LAND", (1 << 0), "land (or possibly glint or clouds)(ρNIR > 0.02)");
            flagCoding.addFlag("NAVWARN", (1 << 1), "latitude or longitude out of bounds");
            flagCoding.addFlag("NAVFAIL", (1 << 2), "navigation is rough (currently always set to 1)");
            flagCoding.addFlag("HISATZEN", (1 << 3), "satellite view angle > 60°");
            flagCoding.addFlag("HISOLZEN", (1 << 4), "solar zenith angle at estimated position > 75°");
            flagCoding.addFlag("SATURATE", (1 << 5), "pixel has ≥ 1 saturated bands");
            flagCoding.addFlag("CALFAIL", (1 << 6), "pixel has ≥ bands from a dropped packet");
            flagCoding.addFlag("CLOUD", (1 << 7), "rough cloud mask (ρNIR > 0.05 and ρRED > 0.5) or (0.8 < ρNIR/ρRED < 1.1)");
            product.getFlagCodingGroup().add(flagCoding);
            flagBand.setSampleCoding(flagCoding);

            product.addMask("LAND", "flags.LAND", "land (or possibly glint or clouds)(ρNIR > 0.02)", Color.GREEN, 0.5);
            product.addMask("NAVWARN", "flags.NAVWARN", "latitude or longitude out of bounds", Color.CYAN, 0.5);
            product.addMask("NAVFAIL", "flags.NAVFAIL", "navigation is rough (currently always set to 1)", Color.CYAN.darker(), 0.5);
            product.addMask("HISATZEN", "flags.HISATZEN", "satellite view angle > 60°", Color.MAGENTA, 0.5);
            product.addMask("HISOLZEN", "flags.HISOLZEN", "solar zenith angle at estimated position > 75°", Color.PINK, 0.5);
            product.addMask("SATURATE", "flags.SATURATE", "pixel has ≥ 1 saturated bands", Color.RED, 0.5);
            product.addMask("CALFAIL", "flags.CALFAIL", "pixel has ≥ bands from a dropped packet", Color.BLUE, 0.5);
            product.addMask("CLOUD", "flags.CLOUD", "rough cloud mask (ρNIR > 0.05 and ρRED > 0.5) or (0.8 < ρNIR/ρRED < 1.1)", Color.YELLOW, 0.5);
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("should be read from source images only");
    }

}
