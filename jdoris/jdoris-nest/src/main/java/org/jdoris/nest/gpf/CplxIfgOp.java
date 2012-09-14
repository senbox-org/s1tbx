package org.jdoris.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


@OperatorMetadata(alias = "CplxIfg",
        category = "InSAR\\Products",
        description = "Compute interferograms from stack of coregistered images", internal = true)
public class CplxIfgOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

//    @Parameter(description = "Num. of points for 'reference phase' polynomial estimation",
//            valueSet = {"301", "401", "501", "601", "701", "801", "901", "1001"},
//            defaultValue = "501",
//            label="Number of SRP estimation points")
//    private int srpNumberPoints = 501;
//
//    @Parameter(description = "The order of 'reference phase' polynomial", valueSet = {"3", "4", "5", "6", "7", "8"},
//            defaultValue = "5",
//            label="SRP Polynomial Order")
//    private int srpPolynomialOrder = 2;
//
//    private String[] stackBandNames = null;
    private Band masterBand0 = null;
    private Band masterBand1 = null;

    private final Map<Band, Band> sourceRasterMap = new HashMap<Band, Band>(10);
    private final Map<Band, Band> complexSrcMapI = new HashMap<Band, Band>(10);
    private final Map<Band, Band> complexSrcMapQ = new HashMap<Band, Band>(10);
    //private final Map<Band, Band> complexIfgMap = new HashMap<Band, Band>(10);

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CplxIfgOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {

            checkUserInput();

            masterBand0 = sourceProduct.getBandAt(0);
            masterBand1 = sourceProduct.getBandAt(1);

            createTargetProduct();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void checkUserInput() throws OperatorException {
        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final int isCoregStack = masterMeta.getAttributeInt(AbstractMetadata.coregistered_stack);
        if(isCoregStack != 1) {
            throw new OperatorException("Input should be a coregistered SLC stack");
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        // TODO: this doesnt work for multichannel data
        final int numSrcBands = sourceProduct.getNumBands();

        int cnt = 1;
        int inc = 2;
        //int cnt_master = 0;
        String iBandName = "null";
        String qBandName = "null";

        for (int i = 0; i < numSrcBands; i += inc) {

            final Band srcBandI = sourceProduct.getBandAt(i);
            final Band srcBandQ = sourceProduct.getBandAt(i + 1);

            // TODO: beautify names of ifg bands
            if (srcBandI.getUnit().equals(Unit.REAL) && srcBandQ.getUnit().equals(Unit.IMAGINARY)) {

                if (srcBandI == masterBand0) {
                    iBandName = srcBandI.getName();
                } else {
                    iBandName = "i_ifg" + cnt + "_" +
                            masterBand0.getName() + "_" +
                            srcBandI.getName();
                }
                Band targetBandI;
                if (srcBandI == masterBand0) {
                    targetBandI = ProductUtils.copyBand(srcBandI.getName(), sourceProduct, targetProduct);
                    targetBandI.setSourceImage(srcBandI.getSourceImage());
                } else {
                    targetBandI = targetProduct.addBand(iBandName, ProductData.TYPE_FLOAT32);
                    ProductUtils.copyRasterDataNodeProperties(srcBandI, targetBandI);
                }
                sourceRasterMap.put(targetBandI, srcBandI);

                if (srcBandQ == masterBand1) {
                    qBandName = srcBandQ.getName();
                } else {
                    qBandName = "q_ifg" + cnt + "_" +
                            masterBand1.getName() + "_" +
                            srcBandQ.getName();
                }

                Band targetBandQ;
                if (srcBandQ == masterBand1) {
                    targetBandQ = ProductUtils.copyBand(srcBandQ.getName(), sourceProduct, targetProduct);
                    targetBandQ.setSourceImage(srcBandQ.getSourceImage());
                } else {
                    targetBandQ = targetProduct.addBand(qBandName, ProductData.TYPE_FLOAT32);
                    ProductUtils.copyRasterDataNodeProperties(srcBandQ, targetBandQ);
                }
                sourceRasterMap.put(targetBandQ, srcBandQ);

                complexSrcMapQ.put(srcBandI, srcBandQ);
                complexSrcMapI.put(srcBandQ, srcBandI);

                String suffix = "";
                if (srcBandI != masterBand0) {
                    suffix = "_ifg" + cnt++;
                    //System.out.println(suffix);
                }

                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, suffix);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, suffix);

            }

        }

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
//        targetProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 2000);

    }

/*
    private static String getBandName(final String name) {
        if(name.contains("::"))
            return name.substring(0, name.indexOf("::"));
        return name;
    }

    private static String getBandTimeStamp(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION, "");
        String dateString = OperatorUtils.getAcquisitionDate(absRoot);
        if (!dateString.isEmpty())
            dateString = '_' + dateString;

        return StringUtils.createValidName(mission + dateString, new char[]{'_', '.'}, '_');
    }
*/

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {

            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final Band srcBand = sourceRasterMap.get(targetBand);

            final Tile masterRasterI = getSourceTile(masterBand0, targetTileRectangle, pm);
            final ProductData masterDataI = masterRasterI.getDataBuffer();

            final Tile masterRasterQ = getSourceTile(masterBand1, targetTileRectangle, pm);
            final ProductData masterDataQ = masterRasterQ.getDataBuffer();

//          complexSrcMapQ.put(srcBandI, srcBandQ);
//          complexSrcMapI.put(srcBandQ, srcBandI);

            final ProductData targetData = targetTile.getDataBuffer();

            if (srcBand.getUnit().equals(Unit.REAL)) {

                final Tile slaveRasterI = getSourceTile(srcBand, targetTileRectangle, pm);
                final ProductData slaveDataI = slaveRasterI.getDataBuffer();

                final Band cplxSlaveBandQ = complexSrcMapQ.get(srcBand);
                final Tile slaveRasterQ = getSourceTile(cplxSlaveBandQ, targetTileRectangle, pm);
                final ProductData slaveDataQ = slaveRasterQ.getDataBuffer();

                for (int y = y0; y < y0 + h; y++) {
                    for (int x = x0; x < x0 + w; x++) {

                        final int index = slaveRasterQ.getDataBufferIndex(x, y);

                        // cplx(a).*conj(cplx(b))
                        targetData.setElemFloatAt(index,
                                (masterDataI.getElemFloatAt(index) * slaveDataI.getElemFloatAt(index)) -
                                        (masterDataQ.getElemFloatAt(index) * (-1) * slaveDataQ.getElemFloatAt(index)));

                    }
                }
            } else if (srcBand.getUnit().equals(Unit.IMAGINARY)) {

                final Tile slaveRasterQ = getSourceTile(srcBand, targetTileRectangle, pm);
                final ProductData slaveDataQ = slaveRasterQ.getDataBuffer();

                final Band cplxSlaveBandI = complexSrcMapI.get(srcBand);
                final Tile slaveRasterI = getSourceTile(cplxSlaveBandI, targetTileRectangle, pm);
                final ProductData slaveDataI = slaveRasterI.getDataBuffer();

                for (int y = y0; y < y0 + h; y++) {
                    for (int x = x0; x < x0 + w; x++) {

                        final int index = slaveRasterI.getDataBufferIndex(x, y);

                        // cplx(a).*conj(cplx(b))
                        targetData.setElemFloatAt(index,
                                (masterDataI.getElemFloatAt(index) * (-1) * slaveDataQ.getElemFloatAt(index)) +
                                        (masterDataQ.getElemFloatAt(index) * slaveDataI.getElemFloatAt(index)));

                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CplxIfgOp.class);
        }
    }
}