package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.merisl3.ISINGrid;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeasuresL3BinFileReader extends SeadasFileReader {

    private ISINGrid grid;
    private RowInfo[] rowInfo;
    private int[] bins;
    private int sceneWidth;
    private int sceneHeight;

    MeasuresL3BinFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws IOException {

        String resolution = "9 km";
        try {
            resolution = getStringAttribute("Bin Resolution");
        } catch (Exception ignored) {

        }
        int rowcnt = 2160;
        if (resolution.contains("4 km")){
            rowcnt = 4320;
        }
        grid = new ISINGrid(rowcnt);
        sceneWidth = grid.getRowCount() * 2;
        sceneHeight = grid.getRowCount();

        String [] nameparts = ncFile.getLocation().split(File.separator);
        String productName = nameparts[nameparts.length-1];
        try {
                productName = getStringAttribute("Product Name");
        } catch (Exception ignored) {

        }
        SeadasProductReader.ProductType productType = productReader.getProductType();
        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight, productReader);
        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);

        List<Variable> l3ProdVars = ncFile.getVariables();
        variableMap = addBands(product, l3ProdVars);

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
        short[] ibuffer;
        Object buffer;

        if (prodtype == DataType.FLOAT) {
            fbuffer = (float[]) destBuffer.getElems();
            Arrays.fill(fbuffer, Float.NaN);
            buffer = fbuffer;
        } else {
            ibuffer = (short[]) destBuffer.getElems();
            Arrays.fill(ibuffer, (short) -999);
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
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                final int rowIndex = (height - 1) - y;
                final RowInfo rowInfo = this.rowInfo[rowIndex];
                if (rowInfo != null) {

                    final int lineOffset = rowInfo.offset;
                    final int lineLength = rowInfo.length;
                    final int[] start = new int[]{lineOffset,0};
//                    final int[] stride = new int[]{sourceStepY,sourceStepX};
                    final int[] stride = new int[]{1,1};
                    final int[] count = new int[]{lineLength,1};
                    final Object bindata;
                    Section section = new Section(start, count, stride);
                    synchronized (ncFile) {
                        bindata = variable.read(section).reduce().copyTo1DJavaArray();
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

    private RowInfo[] createRowInfos() throws
            IOException {
        final ISINGrid grid = this.grid;
        final RowInfo[] binLines = new RowInfo[sceneHeight];
        final Variable idxVariable = ncFile.getRootGroup().findVariable("Indexes");
        final int[] idxValues;
        synchronized (ncFile) {
            idxValues = (int[]) idxVariable.read().getStorage();
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

        if (lineLength > 0 && lastRowIndex > 0) {
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

    public Map<Band, Variable> addBands(Product product, List<Variable> l3ProdVars) {

        final Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();

        for (Variable l3Var : l3ProdVars) {
            String varName = l3Var.getShortName();
            final String dataTypeStr = l3Var.getDataType().name();//
            int dataType = ProductData.TYPE_UNDEFINED;
           if (dataTypeStr.equalsIgnoreCase("float")) {
                    dataType = ProductData.TYPE_FLOAT32;
           } else if  (dataTypeStr.equalsIgnoreCase("int")) {
                    dataType = ProductData.TYPE_INT32;
           }


            if (!varName.contains("Indexes") && (!varName.equalsIgnoreCase("n"))) {
               bandToVariableMap.put(addBand(product, varName, dataType), l3Var);
            }
        }
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
        product.addBand(band);
        return band;
    }
}
