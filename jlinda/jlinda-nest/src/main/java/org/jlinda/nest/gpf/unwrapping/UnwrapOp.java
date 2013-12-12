package org.jlinda.nest.gpf.unwrapping;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.unwrapping.mcf.Unwrapper;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "Unwrap",
        category = "InSAR\\Unwrapping",
        description = "Unwrap input complex data",
        internal = false)
public class UnwrapOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    // source maps
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<>();

    // target maps
    private HashMap<String, ProductContainer> targetMap = new HashMap<>();

    // operator tags
    private static final boolean CREATE_VIRTUAL_BAND = true;
    private static final String PRODUCT_NAME = "unw_ifgs";
    public static final String PRODUCT_TAG = "_ifg_unw";
    private static final String UNW_PHASE_BAND_NAME = "unwrapped_phase";
    private int tileWidth = 16;
    private int tileHeight = 16;


    @Override
    public void initialize() throws OperatorException {
        try {
            constructSourceMetadata();
            constructTargetMetadata();
            createTargetProduct();
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void constructSourceMetadata() throws Exception {
        // define sourceMaster/sourceSlave name tags
        final String masterTag = "ifg";
        final String slaveTag = "dummy";

        // get sourceMaster & sourceSlave MetadataElement
        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;

        /* organize metadata */

        // put sourceMaster metadata into the masterMap
        metaMapPut(masterTag, masterMeta, sourceProduct, masterMap);

        // pug sourceSlave metadata into slaveMap
        MetadataElement[] slaveRoot = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot).getElements();
        for (MetadataElement meta : slaveRoot) {
            metaMapPut(slaveTag, meta, sourceProduct, slaveMap);
        }

    }

    private void metaMapPut(final String tag,
                            final MetadataElement root,
                            final Product product,
                            final HashMap<Integer, CplxContainer> map) throws Exception {

        // pull out band names for this product
        final String[] bandNames = product.getBandNames();
        final int numOfBands = bandNames.length;

        // map key: ORBIT NUMBER
        int mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT);

        // metadata: construct classes and define bands
        final String date = OperatorUtils.getAcquisitionDate(root);
        final SLCImage meta = new SLCImage(root);
        final Orbit orbit = null; // ORBIT not needed

        meta.setMlAz(1);
        meta.setMlRg(1);

        Band bandReal = null;
        Band bandImag = null;

        for (int i = 0; i < numOfBands; i++) {
            String bandName = bandNames[i];
            if (bandName.contains(tag) && bandName.contains(date)) {
                final Band band = product.getBandAt(i);
                if (BandUtilsDoris.isBandReal(band)) {
                    bandReal = band;
                } else if (BandUtilsDoris.isBandImag(band)) {
                    bandImag = band;
                }
            }
        }
        try {
            map.put(mapKey, new CplxContainer(date, meta, orbit, bandReal, bandImag));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void constructTargetMetadata() {

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                // generate name for product bands
                String productName = keyMaster.toString() + "_" + keySlave.toString();

                final CplxContainer slave = slaveMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, master, slave, true);

                product.targetBandName_I = master.realBand.getName(); // "i" + PRODUCT_TAG + "_" + master.date + "_" + slave.date;
                product.targetBandName_Q = master.imagBand.getName(); // "q" + PRODUCT_TAG + "_" + master.date + "_" + slave.date;

                product.masterSubProduct.name = UNW_PHASE_BAND_NAME;
                product.masterSubProduct.targetBandName_I = UNW_PHASE_BAND_NAME + "_" + master.date + "_" + slave.date;

                // put ifg-product bands into map
                targetMap.put(productName, product);

            }

        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(PRODUCT_NAME,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        for (String key : targetMap.keySet()) {

            final String targetBandName_I = targetMap.get(key).targetBandName_I;
            targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT64);
            targetProduct.getBand(targetBandName_I).setUnit(Unit.REAL);

            final String targetBandName_Q = targetMap.get(key).targetBandName_Q;
            targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT64);
            targetProduct.getBand(targetBandName_Q).setUnit(Unit.IMAGINARY);

            final String tag0 = targetMap.get(key).sourceMaster.date;
            final String tag1 = targetMap.get(key).sourceSlave.date;
            if (CREATE_VIRTUAL_BAND) {
                String countStr = PRODUCT_TAG + "_" + tag0 + "_" + tag1;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
            }

            if (targetMap.get(key).subProductsFlag) {
                final String unwPhaseBandName = targetMap.get(key).masterSubProduct.targetBandName_I;
                targetProduct.addBand(unwPhaseBandName, ProductData.TYPE_FLOAT32);
                targetProduct.getBand(unwPhaseBandName).setUnit(Unit.ABS_PHASE);
                targetProduct.getBand(unwPhaseBandName).setDescription("unwrapped_phase");
            }
        }

        targetProduct.setPreferredTileSize(tileWidth, tileHeight);
        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {

            for (String ifgKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(ifgKey);

                final Tile tileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle);
                final Tile tileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle);

                final ComplexDoubleMatrix cplxData = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
                DoubleMatrix phaseData = SarUtils.angle(cplxData);

                Unwrapper unwrapper = new Unwrapper(phaseData);
                unwrapper.unwrap();
                phaseData = unwrapper.getUnwrappedPhase();

                // commit to target
                final Band targetBand_I = targetProduct.getBand(product.targetBandName_I);
                final Tile tileOutReal = targetTileMap.get(targetBand_I);
                TileUtilsDoris.pushDoubleMatrix(cplxData.real(), tileOutReal, targetRectangle);

                final Band targetBand_Q = targetProduct.getBand(product.targetBandName_Q);
                final Tile tileOutImag = targetTileMap.get(targetBand_Q);
                TileUtilsDoris.pushDoubleMatrix(cplxData.imag(), tileOutImag, targetRectangle);

                final Band targetBand_UnwPhase = targetProduct.getBand(product.masterSubProduct.targetBandName_I);
                final Tile tileOutUnwPhase = targetTileMap.get(targetBand_UnwPhase);
                TileUtilsDoris.pushDoubleMatrix(phaseData, tileOutUnwPhase, targetRectangle);
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(UnwrapOp.class);
        }
    }

}
