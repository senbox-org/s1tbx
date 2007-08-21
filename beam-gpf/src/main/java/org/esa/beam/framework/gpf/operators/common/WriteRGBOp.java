package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.JAI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>WriteRGBOp</code> takes three bands of its source Product to
 * create an RGB image file. This Operaor will need the JAI libs on the class
 * path.
 * <p/>
 * Configuration Elements:
 * <ul>
 * <li><b>red</b> the band index of the source band for the red values
 * <li><b>green</b> the band index of the source band for the green values
 * <li><b>blue</b> the band index of the source band for the blue values
 * <li><b>filePath</b> the path of the file to write to
 * <li><b>formatName</b> the format of the file
 * </ul>
 *
 * @author Maximilian Aulinger
 */
public class WriteRGBOp extends AbstractOperator {

    @Parameter
    private int red;
    @Parameter
    private int green;
    @Parameter
    private int blue;
    @Parameter
    private String formatName = "bmp";
    @Parameter
    private String filePath = null;

    private RasterDataNode[] rgbChannelNodes;
    private Product targetProduct;
    private Map<Band, Band> bandMap;
    private Map<Band, ProductData> dataMap;
    @TargetProduct
    private Product sourceProduct;

    public WriteRGBOp(Spi spi) {
        super(spi);
    }

    @Override
    public Product initialize(ProgressMonitor pm) throws OperatorException {
        bandMap = new HashMap<Band, Band>(3);
        dataMap = new HashMap<Band, ProductData>(3);
        rgbChannelNodes = new RasterDataNode[3];

        sourceProduct = getContext().getSourceProduct("input");
        final int height = sourceProduct.getSceneRasterHeight();
        final int width = sourceProduct.getSceneRasterWidth();

        targetProduct = new Product("RGB", "RGB", width, height);
        prepareTargetBand(0, sourceProduct.getBandAt(red), "red", width, height);
        prepareTargetBand(1, sourceProduct.getBandAt(green), "green", width, height);
        prepareTargetBand(2, sourceProduct.getBandAt(blue), "blue", width, height);

        return targetProduct;
    }

    private void prepareTargetBand(int rgbIndex, Band sourceBand, String bandName, int width, int height) {
        Band targetBand = new Band(bandName, sourceBand.getDataType(), width, height);
        targetProduct.addBand(targetBand);
        bandMap.put(targetBand, sourceBand);

        ProductData data = targetBand.createCompatibleRasterData();
        dataMap.put(targetBand, data);

        targetBand.setRasterData(data);
        rgbChannelNodes[rgbIndex] = targetBand;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        RasterDataNode targetBand = targetRaster.getRasterDataNode();
        Rectangle rectangle = targetRaster.getRectangle();
        
		Band sourceBand = bandMap.get(targetBand);
        Raster sourceRaster = getRaster(sourceBand, rectangle);
        
        ProductData rgbData = dataMap.get(targetBand);
        System.arraycopy(sourceRaster.getDataBuffer().getElems(), 0, rgbData.getElems(), rectangle.x + rectangle.y * rectangle.width, rectangle.width * rectangle.height);
    }

    @Override
    public void dispose() {
        try {
            writeImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeImage() throws IOException {
        BufferedImage outputImage = ProductUtils.createRgbImage(rgbChannelNodes, ProgressMonitor.NULL);
        ParameterBlock storeParams = new ParameterBlock();
        storeParams.addSource(outputImage);
        storeParams.add(filePath);
        storeParams.add(formatName);
        JAI.create("filestore", storeParams);
    }

    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(WriteRGBOp.class, "RGBWriter");
        }
    }
}
