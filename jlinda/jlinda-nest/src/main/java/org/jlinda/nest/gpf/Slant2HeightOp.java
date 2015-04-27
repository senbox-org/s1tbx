package org.jlinda.nest.gpf;

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
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.gpf.OperatorUtils;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.geocode.Slant2Height;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.HashMap;

@OperatorMetadata(alias = "Phase2Height",
        category = "SAR Processing/Interferometric/Products",
        authors = "Petar Marinkovic",
        copyright = "Copyright (C) 2013 by PPO.labs",
        description = "Phase to Height conversion")
public class Slant2HeightOp extends Operator {

    @SourceProduct(description = "Source product that contains unwrapped phase.")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"100", "200", "300", "400", "500"},
            description = "Number of points for evaluation of flat earth phase at different altitudes",
            defaultValue = "200",
            label = "Number of estimation points")
    private int nPoints; // where ref.phase is evaluated

    @Parameter(valueSet = {"2", "3", "4", "5"},
            description = "Number of height samples in range [0,5000)",
            defaultValue = "3",
            label = "Number of height samples")
    private int nHeights;

    @Parameter(valueSet = {"1", "2", "3", "4", "5"},
            description = "Degree of the 1D polynomial to fit reference phase through.",
            defaultValue = "2",
            label = "Degree of 1D polynomial")
    private int degree1D; // only possible now.

    @Parameter(valueSet = {"1", "2", "3", "4", "5", "6", "7", "8"},
            description = "Degree of the 2D polynomial to fit reference phase through.",
            defaultValue = "5",
            label = "Degree of 2D polynomial")
    private int degree2D; // only possible now.

    @Parameter(valueSet = {"2", "3", "4", "5"},
            description = "Degree of orbit (polynomial) interpolator",
            defaultValue = "3",
            label = "Orbit interpolation degree")
    private int orbitDegree = 3;

    // source maps
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();

    // target maps
    private HashMap<String, ProductContainer> targetMap = new HashMap<String, ProductContainer>();

    // classes map: for multi-band support
    private HashMap<String, Slant2Height> slant2HeightMap = new HashMap<String, Slant2Height>();

    // operator tags
    private static final String PRODUCT_NAME = "slant2h";

    public static final String PRODUCT_TAG = "slant2h";

    private int sourceImageWidth;
    private int sourceImageHeight;

    private Band referenceBand = null;
//    private Slant2Height slant2Height;


    @Override
    public void initialize() throws OperatorException {

        try {

            // work out which is which product: loop through source product and check which product has a 'unwrapped phase' band
            // JL: check source product for band with abs_phase unit
            sortOutSourceProducts(); // -> declares topoProduct and defoProduct

            // JL: Create masterMap that maps master orbit number to a CplxContainer
            constructSourceMetadata();

            // JL: For every master/slave pair, create a targetMap that maps a product name to a ProductContainer that
            //     contains both master and slave products.
            constructTargetMetadata();

            // JL: For every master/slave pair, add one target band with meter unit.
            createTargetProduct();

            // JL: get source image dimension
            getSourceImageDimension();

            // does the math for slant2height conversion
            // JL: create a Slant2Height object
            slant2Height();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void slant2Height() throws Exception {

        for (Integer keyMaster : masterMap.keySet()) {
            CplxContainer master = masterMap.get(keyMaster);
            for (Integer keySlave : slaveMap.keySet()) {
                CplxContainer slave = slaveMap.get(keySlave);

                // JL: for every master/slave pair, create a Slant2Height object
                Slant2Height slant2Height = new Slant2Height(nPoints, nHeights, degree1D, degree2D,
                        master.metaData, master.orbit, slave.metaData, slave.orbit);

                // JL: set a window of image size
                slant2Height.setDataWindow(new Window(0, sourceImageHeight, 0, sourceImageWidth));

                // JL: Compute 3 2-d polynomials (21 coefficients): ci = C_i(line_n, pixel_n), for i = 1,2,3,
                //     where line_n and pixel_n are normalized line and pixel indices, and c_i is the ith coefficient
                //     for reference phase polynomial ph(h) = c0 + c1*h + c2*h*h.
                slant2Height.schwabisch();

                // JL: create slant2HeightMap that maps slave date to the slant2Height object
                slant2HeightMap.put(slave.date, slant2Height);
            }
        }
    }

    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    // JL: create targetMap for every master/slave pair. It maps a product name (masterKey_slaveKey) to
    //     a ProductContainer that contains both master and slave products.
    private void constructTargetMetadata() {

        for (Integer keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (Integer keySlave : slaveMap.keySet()) {

                // generate name for product bands
                String productName = keyMaster.toString() + "_" + keySlave.toString();

                final CplxContainer slave = slaveMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, master, slave, false);

                product.targetBandName_I = PRODUCT_TAG + "_" + master.date + "_" + slave.date;

                // put ifg-product bands into map
                targetMap.put(productName, product);

            }
        }
    }

    // JL: looping through all source bands to check if there is a band with abs_phase unit.
    //     If not, throw an exception.
    private void sortOutSourceProducts() {

        // check whether there are absolute phases bands in the product
        Band tempBand = null;
        for (Band band : sourceProduct.getBands()) {
            if (band.getUnit() != null && band.getUnit().equals(Unit.ABS_PHASE)) {
                tempBand = band;
            }
        }
        if (tempBand == null) {
            throw new OperatorException("Slant2HeightOp requires minimum one 'unwrapped' phase band");
        }
    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "ifg";
        final String slaveTag = "dummy";
        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;
        MetadataElement[] slaveRoot;

        /* organize metadata */
        // put sourceMaster metadata into the masterMap
        // JL: create masterMap that maps master orbit number to a CplxContainer which contains
        //     master metadata, orbit and I/Q bands.
        metaMapPut(masterTag, masterMeta, sourceProduct, masterMap);

        // pug sourceSlave metadata into slaveDefoMap
        // JL: create slaveMap which is similar to masterMap. For this operator, there is no slave product,
        //     there is only one interferogram product. So don't understand why this is needed. Maybe never used.
        slaveRoot = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot).getElements();
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
        final Orbit orbit = new Orbit(root, orbitDegree);

        // TODO: mlook factores are hard-coded for now
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
                } else if (product.getBandAt(i).getUnit().contains(Unit.ABS_PHASE)) {
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

    private void createTargetProduct() {

        // construct target product
        targetProduct = new Product(PRODUCT_NAME,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (final Band band : targetProduct.getBands()) {
            targetProduct.removeBand(band);
        }

        for (String key : targetMap.keySet()) {
            String bandName = targetMap.get(key).targetBandName_I;
            targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            targetProduct.getBand(bandName).setUnit(Unit.METERS);
        }

//        targetProduct.setPreferredTileSize(1,1);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle rect = targetTile.getRectangle();
//            System.out.println("Original: x0 = " + rect.x + ", y = " + rect.y + ", w = " + rect.width + ", h = " + rect.height);
            final int x0 = rect.x;
            final int y0 = rect.y;
            final int w = rect.width;
            final int h = rect.height;

            // JL: create a window of tile size
            Window tileWindow = new Window(y0, y0 + h - 1, x0, x0 + w - 1);

            for (String absPhaseKey : targetMap.keySet()) {

                final ProductContainer product = targetMap.get(absPhaseKey);

                if (targetBand.getName().equals(product.targetBandName_I)) {

                    // check out from source
                    // JL: get source tile for source band (with abs_phase unit)
                    Tile tileRealMaster = getSourceTile(product.sourceMaster.realBand, rect);

                    // JL: get source samples in the source tile and initialize dataMaster matrix
                    final DoubleMatrix dataMaster = TileUtilsDoris.pullDoubleMatrix(tileRealMaster);// check out from source

                    // get class for this slave from the map
                    // JL: get the Slant2Height object created in initialize()
                    Slant2Height slant2Height = slant2HeightMap.get(product.sourceSlave.date);

                    // JL:
                    slant2Height.applySchwabisch(tileWindow, dataMaster);

                    // JL: save samples in dataMaster to target band
                    TileUtilsDoris.pushDoubleMatrix(dataMaster, targetTile, targetTile.getRectangle());
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(Slant2HeightOp.class);
        }
    }
}
