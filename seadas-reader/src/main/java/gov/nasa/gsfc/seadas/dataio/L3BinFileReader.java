package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.merisl3.ISINGrid;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class L3BinFileReader extends SeadasFileReader {

    private ISINGrid grid;
    private RowInfo[] rowInfo;
    private int[] bins;
    private int sceneWidth;
    private int sceneHeight;

    L3BinFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws IOException {

        sceneHeight = ncFile.getRootGroup().findGroup("Level-3_Binned_Data").findVariable("BinIndex").getShape(0);
        sceneWidth = sceneHeight * 2;

        grid = new ISINGrid(sceneHeight);

        String productName = getStringAttribute("Product_Name");

        Product product = new Product(productName, "NASA-OBPG-L3", sceneWidth, sceneHeight, productReader);
        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);

        final Variable idxVariable = ncFile.getRootGroup().findGroup("Level-3_Binned_Data").findVariable("BinList");
        List<Variable> l3ProdVars = ncFile.getVariables();
        variableMap = addBands(product, idxVariable, l3ProdVars);
//        try {
//            addBandsBinMap(product);
//        } catch (InvalidRangeException e) {
//            e.printStackTrace();
//        }
        if (product.getNumBands() == 0) {
            throw new ProductIOException("No bands found.");
        }

        initGeoCoding(product);


        return product;
    }

    @Override
    public synchronized void readBandData(Band destBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                             int sourceHeight, int sourceStepX, int sourceStepY, ProductData destBuffer,
                             ProgressMonitor pm) throws IOException, InvalidRangeException {

        final Variable variable = variableMap.get(destBand);
        DataType prodtype = variable.getDataType();
        float[] fbuffer;
        short[] sbuffer;
        int [] ibuffer;
        byte [] bbuffer;
        Object buffer;

        if (prodtype == DataType.FLOAT) {
            fbuffer = (float[]) destBuffer.getElems();
            Arrays.fill(fbuffer, Float.NaN);
            buffer = fbuffer;
        } else if (prodtype == DataType.SHORT) {
            sbuffer = (short[]) destBuffer.getElems();
            Arrays.fill(sbuffer, (short) -999);
            buffer = sbuffer;
        } else if (prodtype == DataType.BYTE) {
            bbuffer = (byte[]) destBuffer.getElems();
            Arrays.fill(bbuffer, (byte) 255);
            buffer = bbuffer;
        } else {
            ibuffer = (int[]) destBuffer.getElems();
            Arrays.fill(ibuffer, -999);
            buffer = ibuffer;
        }

        if (rowInfo == null) {
            rowInfo = createRowInfos();
        }

        final int height = sceneHeight;
        final int width = sceneWidth;
        final ISINGrid grid = this.grid;


        // loop over lines
        try {
            int[] lineOffsets = new int[1];
            int[] lineLengths = new int[1];
            int[] stride = new int[1];
            stride[0] = 1;


//            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y+=sourceStepY) {
                    if (pm.isCanceled()) {
                    break;
                }
                final int rowIndex = (height - 1) - y;
                final RowInfo rowInfo = this.rowInfo[rowIndex];
                if (rowInfo != null) {

                    final int lineOffset = rowInfo.offset;
                    final int lineLength = rowInfo.length;


                    lineOffsets[0] = lineOffset;
                    lineLengths[0] = lineLength;
                    final Object bindata;

                    synchronized (ncFile) {
                        bindata = variable.read().section(lineOffsets, lineLengths, stride).copyTo1DJavaArray();
                    }
                    int lineIndex0 = 0;
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final double lon = x * 360.0 / width;
                        final int binIndex = grid.getBinIndex(rowIndex, lon);
                        int lineIndex = -1;
                        for (int i = lineIndex0; i < lineLength; i++) {
                            int binidx = bins[lineOffset + i];
                            if (binidx >= binIndex) {
                                if (binidx == binIndex) {
                                    lineIndex = i;
                                }
                                lineIndex0 = i;
                                break;
                            }
                        }

                        if (lineIndex >= 0) {
                            final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);

                            System.arraycopy(bindata, lineIndex, buffer, rasterIndex, 1);
                        }
                    }
                    pm.worked(1);
                }
            }

        } finally {
            pm.done();
        }
    }

    // Don't do this...it hurts.  Too much of a memory hog...
    private void addBandsBinMap (Product product)throws IOException, InvalidRangeException {
        String[] bandList = product.getBandNames();
        if (rowInfo == null) {
            rowInfo = createRowInfos();
        }

        final int height = sceneHeight;
        final int width = sceneWidth;
        final ISINGrid grid = this.grid;


        // loop over lines
        try {
            int[] lineOffsets = new int[1];
            int[] lineLengths = new int[1];
            int[] stride = new int[1];
            stride[0] = 1;


//            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
            for (String name : bandList) {
                if (name.endsWith("mean") || name.endsWith("stdev"))
                    continue;
                Band band = product.getBand(name);
                ProductData buffer;
                final Variable variable = variableMap.get(band);
                DataType prodtype = variable.getDataType();
                float[] fbuffer = new float[width*height];
                short[] sbuffer = new short[width*height];
                int [] ibuffer= new int[width*height];
                byte [] bbuffer= new byte[width*height];

                if (prodtype == DataType.FLOAT) {
                    Arrays.fill(fbuffer, Float.NaN);
                    buffer = ProductData.createInstance(fbuffer);
                } else if (prodtype == DataType.SHORT) {
                    Arrays.fill(sbuffer, (short) -999);
                    buffer = ProductData.createInstance(sbuffer);
                } else if (prodtype == DataType.BYTE) {
                    Arrays.fill(bbuffer, (byte) 255);
                    buffer = ProductData.createInstance(bbuffer);
                } else {
                    Arrays.fill(ibuffer, -999);
                    buffer = ProductData.createInstance(ibuffer);
                }

                for (int y = 0; y < height; y++) {

                    final int rowIndex = (height - 1) - y;
                    final RowInfo rowInfo = this.rowInfo[rowIndex];
                    if (rowInfo != null) {
                        final Array bindata;

                        final int lineOffset = rowInfo.offset;
                        final int lineLength = rowInfo.length;


                        lineOffsets[0] = lineOffset;
                        lineLengths[0] = lineLength;

                        synchronized (ncFile) {
                            bindata = variable.read().section(lineOffsets, lineLengths, stride);//.copyTo1DJavaArray();
                        }
                        int lineIndex0 = 0;
                        for (int x = 0; x < width; x++) {
                            final double lon = x * 360.0 / width;
                            final int binIndex = grid.getBinIndex(rowIndex, lon);
                            int lineIndex = -1;
                            for (int i = lineIndex0; i < lineLength; i++) {
                                int binidx = bins[lineOffset + i];
                                if (binidx >= binIndex) {
                                    if (binidx == binIndex) {
                                        lineIndex = i;
                                    }
                                    lineIndex0 = i;
                                    break;
                                }
                            }

                            if (lineIndex >= 0) {
                                final int rasterIndex = width * y + x;
                                final Array elem;
                                elem = Array.factory(bindata.copyTo1DJavaArray());
                                for (int i=0; i<elem.getSize(); i++){
                                    if (prodtype == DataType.FLOAT) {

                                        buffer.setElemFloatAt(rasterIndex, elem.getFloat(i));
                                    } else {
                                        buffer.setElemIntAt(rasterIndex, elem.getInt(i));
                                    }
    //                                System.arraycopy(bindata, lineIndex, buffer, rasterIndex, 1);
                                }
                            }
                        }
                    }
                }
                band.setDataElems(buffer);
            }
        } catch (IOException e){
            throw new IOException("Could not map product " + product.getName());
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // private helpers
    /////////////////////////////////////////////////////////////////////////

    private void initGeoCoding(Product product) throws IOException {
        float pixelX = 0.0f;
        float pixelY = 0.0f;
        float easting = -180f;
        float northing = +90f;
        float pixelSizeX = 360.0f / sceneWidth;
        float pixelSizeY = 180.0f / sceneHeight;
        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                    sceneWidth, sceneHeight,
                    easting, northing,
                    pixelSizeX, pixelSizeY,
                    pixelX, pixelY));
        } catch (FactoryException e) {
            throw new IOException(e);
        } catch (TransformException e) {
            throw new IOException(e);
        }
    }

    private RowInfo[] createRowInfos() throws IOException {
        final ISINGrid grid = this.grid;
        final RowInfo[] binLines = new RowInfo[sceneHeight];
        final Variable idxVariable = ncFile.getRootGroup().findGroup("Level-3_Binned_Data").findVariable("BinList");
        final Structure idxStructure = (Structure) idxVariable;
        final Variable idx = idxStructure.findVariable("bin_num");
        final int[] idxValues;
        synchronized (ncFile) {
            idxValues = (int[]) idx.read().getStorage();
        }
        if (bins == null) {
            bins = idxValues;//(int[]) idxVariable.read().copyTo1DJavaArray();
        }
        final Point gridPoint = new Point();
        int lastBinIndex = -1;
        int lastRowIndex = -1;
        int lineOffset = 0;
        int lineLength = 0;
        for (int i = 0; i < idxValues.length; i++) {

            final int binIndex = idxValues[i];
            if (binIndex < lastBinIndex) {
                throw new IOException(
                        "Unrecognized level-3 format. Bins numbers expected to appear in ascending order.");
            }
            lastBinIndex = binIndex;

            grid.getGridPoint(binIndex, gridPoint);
            final int rowIndex = gridPoint.y;

            if (rowIndex != lastRowIndex) {
                if (lineLength > 0) {
                    binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
                }
                lineOffset = i;
                lineLength = 0;
            }

            lineLength++;
            lastRowIndex = rowIndex;
        }

        if (lineLength > 0) {
            binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
        }

        return binLines;
    }

    private static final class RowInfo {

        // offset of row within file
        final int offset;
        // number of bins per row
        final int length;

        public RowInfo(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }

    public Map<Band, Variable> addBands(Product product, Variable idxVariable, List<Variable> l3ProdVars) {

        final Structure binListStruc = (Structure) idxVariable;

        final Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();


//        bandToVariableMap.put(addBand(product, "bin_num", ProductData.TYPE_UINT32), binListStruc.select("bin_num").findVariable("bin_num"));
        bandToVariableMap.put(addBand(product, "weights", ProductData.TYPE_FLOAT32), binListStruc.select("weights").findVariable("weights"));
        bandToVariableMap.put(addBand(product, "nobs", ProductData.TYPE_UINT16), binListStruc.select("nobs").findVariable("nobs"));
        bandToVariableMap.put(addBand(product, "nscenes", ProductData.TYPE_UINT16), binListStruc.select("nscenes").findVariable("nscenes"));
//        ncFile.getRootGroup().findGroup("Level-3 Binned Data").findVariable("BinList");
        if (ncFile.getRootGroup().findGroup("Level-3_Binned_Data").findVariable("qual_l3") != null){
            bandToVariableMap.put(addBand(product, "qual_l3", ProductData.TYPE_UINT8), ncFile.getRootGroup().findGroup("Level-3_Binned_Data").findVariable("qual_l3"));
        }
        String groupnames = "";
        for (Variable l3Var : l3ProdVars) {
            String varName = l3Var.getShortName();
            final int dataType = ProductData.TYPE_FLOAT32;


            if (!varName.contains("Bin") && (!varName.startsWith("qual")) &&
                    (!varName.equalsIgnoreCase("SEAGrid")) &&
                    (!varName.equalsIgnoreCase("Input_Files"))) {
                final Structure binStruc = (Structure) l3Var;
                if (groupnames.length() == 0) {
                    groupnames = varName;
                } else {
                    groupnames = groupnames + ":" + varName;
                }

                List<String> vnames = binStruc.getVariableNames();
                for (String bandvar : vnames) {
                    bandToVariableMap.put(addBand(product, bandvar, dataType), binStruc.select(bandvar).findVariable(bandvar));
                }
                // Add virtual band for product mean
                StringBuilder prodname = new StringBuilder(varName);
                prodname.append("_mean");

                String calcmean = ComputeBinMeans(varName);
                Band varmean = new VirtualBand(prodname.toString(), ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), calcmean);
                varmean.setNoDataValue(Double.NaN);
                varmean.setNoDataValueUsed(true);
                product.addBand(varmean);

                // Add virtual band for product stdev
                int underscore = prodname.indexOf("_mean");
                prodname.delete(underscore, underscore + 5);
                prodname.append("_stdev");

                String calcstdev = ComputeBinVariances(varName);

                Band varstdev = new VirtualBand(prodname.toString(), ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), calcstdev);
                varstdev.setNoDataValue(Double.NaN);
                varstdev.setNoDataValueUsed(true);

                product.addBand(varstdev);
            }
        }
        product.setAutoGrouping(groupnames);
        return bandToVariableMap;
    }

    private Band addBand(Product product, String varName, int productType) {
        Band band = new Band(varName, productType, product.getSceneRasterWidth(),
                product.getSceneRasterHeight());
        band.setScalingOffset(0.0);
        band.setScalingFactor(1.0);
        band.setLog10Scaled(false);
        if (productType == ProductData.TYPE_FLOAT32) {
            band.setNoDataValue(Double.NaN);
        } else {
            band.setNoDataValue(-999);
        }
        band.setNoDataValueUsed(true);
        product.addBand(band);
        return band;
    }


    private String ComputeBinMeans(String prodname) {
        StringBuilder bin_mean = new StringBuilder(prodname);
        bin_mean.append("_");
        bin_mean.append("sum / weights");
        return bin_mean.toString();
    }

    private String ComputeBinVariances(String prodname) {
        StringBuilder bin_stdev = new StringBuilder("weights * weights <= nscenes ? 0.0 : sqrt((((");
        bin_stdev.append(prodname);
        bin_stdev.append("_sum_sq/weights) - (");
        bin_stdev.append(prodname);
        bin_stdev.append("_sum /weights)*(");
        bin_stdev.append(prodname);
        bin_stdev.append("_sum /weights))");
        bin_stdev.append("* weights * weights) / (weights * weights - nscenes))");
        return bin_stdev.toString();
    }
}
