package org.jlinda.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jlinda.core.Constants;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.geom.DemTile;
import org.jlinda.core.geom.TopoPhase;
import org.jlinda.core.utils.GeoUtils;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "SubtRefDem",
        category = "InSAR\\Products",
        description = "Compute and subtract TOPO phase", internal = false)
public final class SubtRefDemOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(interval = "(1, 10]",
            description = "Degree of orbit interpolation polynomial",
            defaultValue = "3",
            label = "Orbit Interpolation Degree")
    private int orbitDegree = 3;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec",
            label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(description = "The topographic phase band name.",
            defaultValue = "topo_phase",
            label = "Topo Phase Band Name")
    private String topoPhaseBandName = "topo_phase";

    private ElevationModel dem = null;
    private float demNoDataValue = 0;
    private double demSampling;

    // source maps
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<Integer, CplxContainer>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<Integer, CplxContainer>();

    // target maps
    private HashMap<String, ProductContainer> targetMap = new HashMap<String, ProductContainer>();

    // operator tags
    private static final boolean CREATE_VIRTUAL_BAND = true;
    private static final String PRODUCT_NAME = "srd_ifgs";
    public static final String PRODUCT_TAG = "_ifg_srd";


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
            constructSourceMetadata();
            constructTargetMetadata();
            createTargetProduct();

            // define DEM
            defineDEM();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void defineDEM() throws IOException {

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);

        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        if (demDescriptor.isInstallingDem()) {
            throw new OperatorException("The DEM '" + demName + "' is currently being installed.");
        }

        Resampling resampling = Resampling.BILINEAR_INTERPOLATION;

        if (externalDEMFile != null) { // if external DEM file is specified by user
            dem = new FileElevationModel(externalDEMFile, resampling, (float) externalDEMNoDataValue);

            demNoDataValue = (float) externalDEMNoDataValue;
            demName = externalDEMFile.getPath();

        } else {
            dem = demDescriptor.createDem(resampling);
            if (dem == null)
                throw new OperatorException("The DEM '" + demName + "' has not been installed.");

            demNoDataValue = demDescriptor.getNoDataValue();
            demSampling = demDescriptor.getDegreeRes() * (1.0f / demDescriptor.getPixelRes()) * Constants.DTOR;
        }


    }

    private void checkUserInput() {
        // TODO: use jlinda input.coherence class to check user input
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

        // TODO: include polarization flags/checks!
        // pull out band names for this product
        final String[] bandNames = product.getBandNames();
        final int numOfBands = bandNames.length;

        // map key: ORBIT NUMBER
        int mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT);

        // metadata: construct classes and define bands
        final String date = OperatorUtils.getAcquisitionDate(root);
        final SLCImage meta = new SLCImage(root);
        final Orbit orbit = new Orbit(root, orbitDegree);

        // TODO: resolve multilook factors
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

                product.targetBandName_I = "i" + PRODUCT_TAG + "_" + master.date + "_" + slave.date;
                product.targetBandName_Q = "q" + PRODUCT_TAG + "_" + master.date + "_" + slave.date;

                product.masterSubProduct.name = topoPhaseBandName;
                product.masterSubProduct.targetBandName_I = topoPhaseBandName + "_" + master.date + "_" + slave.date;

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

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for (final Band band : targetProduct.getBands()) {
            targetProduct.removeBand(band);
        }

        for (String key : targetMap.keySet()) {

            String targetBandName_I = targetMap.get(key).targetBandName_I;
            String targetBandName_Q = targetMap.get(key).targetBandName_Q;
            targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT64);
            targetProduct.getBand(targetBandName_I).setUnit(Unit.REAL);
            targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT64);
            targetProduct.getBand(targetBandName_Q).setUnit(Unit.IMAGINARY);

            final String tag0 = targetMap.get(key).sourceMaster.date;
            final String tag1 = targetMap.get(key).sourceSlave.date;
            if (CREATE_VIRTUAL_BAND) {
//                String countStr = "_" + PRODUCT_TAG + "_" + tag0 + "_" + tag1;
                String countStr = PRODUCT_TAG + "_" + tag0 + "_" + tag1;
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I), targetProduct.getBand(targetBandName_Q), countStr);
            }

            if (targetMap.get(key).subProductsFlag) {
                String topoBandName = targetMap.get(key).masterSubProduct.targetBandName_I;
                targetProduct.addBand(topoBandName, ProductData.TYPE_FLOAT32);
                targetProduct.getBand(topoBandName).setSynthetic(true);
                targetProduct.getBand(topoBandName).setNoDataValue(demNoDataValue);
                targetProduct.getBand(topoBandName).setUnit(Unit.PHASE);
                targetProduct.getBand(topoBandName).setDescription("topographic_phase");
            }
        }

        // For testing: the optimal results with 1024x1024 pixels tiles, not clear whether it's platform dependent?
        // targetProduct.setPreferredTileSize(512, 512);

    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {

            int y0 = targetRectangle.y;
            int yN = y0 + targetRectangle.height - 1;
            int x0 = targetRectangle.x;
            int xN = targetRectangle.x + targetRectangle.width - 1;
            final Window tileWindow = new Window(y0, yN, x0, xN);

            Band topoPhaseBand;
            Band targetBand_I;
            Band targetBand_Q;

            // TODO: Smarter extension of search space:: use BEAM interpolation to get approximate heights of edges -- should be better then working on ellipsoid!???

            /*
                final GeoCoding geoCoding = targetProduct.getGeoCoding();
                final ProductData trgData = targetTile.getDataBuffer();

                pm.beginTask("Computing elevations from " + demName + "...", h);
                try {
                     final GeoPos geoPos = new GeoPos();
                     final PixelPos pixelPos = new PixelPos();
                     float elevation;

                     for (int y = y0; y < y0 + h; ++y) {
                         for (int x = x0; x < x0 + w; ++x) {
                             pixelPos.setLocation(x + 0.5f, y + 0.5f);
                             geoCoding.getGeoPos(pixelPos, geoPos);
                             try {
                                    if(fileElevationModel != null) {
                                        elevation = fileElevationModel.getElevation(geoPos);
                                    } else {
                                        elevation = dem.getElevation(geoPos);
                                    }
                                } catch (Exception e) {
                                elevation = noDataValue;
                            }
                            trgData.setElemDoubleAt(targetTile.getDataBufferIndex(x, y), (short) Math.round(elevation));
                        }
                        pm.worked(1);
                    }

             */

            // TODO: smarter extension of search space : foreshortening extension? can I calculate how bit tile I
            // need (extra space) for the coverage, taking into the consideration only height of the tile?
            for (String ifgKey : targetMap.keySet()) {

                ProductContainer product = targetMap.get(ifgKey);

                /// get dem of tile ///

                // compute tile geo-corners ~ work on ellipsoid
                GeoPos[] geoCorners = GeoUtils.computeCorners(product.sourceMaster.metaData,
                        product.sourceMaster.orbit,
                        tileWindow);

                // get corners as DEM indices
                PixelPos[] pixelCorners = new PixelPos[2];
                pixelCorners[0] = dem.getIndex(geoCorners[0]);
                pixelCorners[1] = dem.getIndex(geoCorners[1]);

                // get max/min height of tile ~ uses 'fast' GCP based interpolation technique
                double[] tileHeights = computeMaxHeight(pixelCorners, targetRectangle);

                // compute extra lat/lon for dem tile
                GeoPos geoExtent = GeoUtils.defineExtraPhiLam(tileHeights[0], tileHeights[1],
                        tileWindow, product.sourceMaster.metaData, product.sourceMaster.orbit);

                // extend corners
                geoCorners = GeoUtils.extendCorners(geoExtent, geoCorners);

                // update corners
                pixelCorners[0] = dem.getIndex(geoCorners[0]);
                pixelCorners[1] = dem.getIndex(geoCorners[1]);

                pixelCorners[0] = new PixelPos((float) Math.ceil(pixelCorners[0].x), (float) Math.floor(pixelCorners[0].y));
                pixelCorners[1] = new PixelPos((float) Math.floor(pixelCorners[1].x), (float) Math.ceil(pixelCorners[1].y));

                GeoPos upperLeftGeo = dem.getGeoPos(pixelCorners[0]);

                int nLatPixels = (int) Math.abs(pixelCorners[1].y - pixelCorners[0].y);
                int nLonPixels = (int) Math.abs(pixelCorners[1].x - pixelCorners[0].x);

                int startX = (int) pixelCorners[0].x;
                int endX = startX + nLonPixels;
                int startY = (int) pixelCorners[0].y;
                int endY = startY + nLatPixels;

                double[][] elevation = new double[nLatPixels][nLonPixels];
                for (int y = startY, i = 0; y < endY; y++, i++) {
                    for (int x = startX, j = 0; x < endX; x++, j++) {
                        try {
                            float elev = dem.getSample(x, y);
                            if (Float.isNaN(elev))
                                elev = demNoDataValue;
                            elevation[i][j] = elev;
                        } catch (Exception e) {
                            elevation[i][j] = demNoDataValue;
                        }
                    }
                }

                DemTile demTile = new DemTile(upperLeftGeo.lat * Constants.DTOR, upperLeftGeo.lon * Constants.DTOR,
                        nLatPixels, nLonPixels, demSampling, demSampling, (long) demNoDataValue);
                demTile.setData(elevation);

                final TopoPhase topoPhase = new TopoPhase(product.sourceMaster.metaData, product.sourceMaster.orbit,
                        product.sourceSlave.metaData, product.sourceSlave.orbit, tileWindow, demTile);
                topoPhase.radarCode();
                topoPhase.gridData();

                /// check out results from source ///
                Tile tileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle);
                Tile tileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle);
                ComplexDoubleMatrix complexIfg = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                final ComplexDoubleMatrix cplxTopoPhase = new ComplexDoubleMatrix(
                        MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                        MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                SarUtils.computeIfg_inplace(complexIfg, cplxTopoPhase.conji());

                /// commit to target ///
                targetBand_I = targetProduct.getBand(product.targetBandName_I);
                Tile tileOutReal = targetTileMap.get(targetBand_I);
                TileUtilsDoris.pushDoubleMatrix(complexIfg.real(), tileOutReal, targetRectangle);

                targetBand_Q = targetProduct.getBand(product.targetBandName_Q);
                Tile tileOutImag = targetTileMap.get(targetBand_Q);
                TileUtilsDoris.pushDoubleMatrix(complexIfg.imag(), tileOutImag, targetRectangle);

                topoPhaseBand = targetProduct.getBand(product.masterSubProduct.targetBandName_I);
                Tile tileOutTopoPhase = targetTileMap.get(topoPhaseBand);
                TileUtilsDoris.pushDoubleArray2D(topoPhase.demPhase, tileOutTopoPhase, targetRectangle);

            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }


    private double[] computeMaxHeight(PixelPos[] corners, Rectangle rectangle) throws Exception {

        double[] heightArray = new double[2];

        // double square root : scales with the size of tile
        final int numberOfPoints = (int) (10 * Math.sqrt(Math.sqrt(rectangle.width * rectangle.height)));

        // work with 1.5x size of tile
        int offsetX = (int) (1.5 * rectangle.width);
        int offsetY = (int) (1.5 * rectangle.height);

        // define window
        final Window window = new Window((long) (corners[0].y - offsetY), (long) (corners[1].y + offsetY),
                (long) (corners[0].x - offsetX), (long) (corners[1].x + offsetX));

        // distribute points
        final int[][] points = MathUtils.distributePoints(numberOfPoints, window);
        final ArrayList<Float> heights = new ArrayList();

        // then for number of extra points
        for (int[] point : points) {
            float height = dem.getSample(point[1], point[0]);
            if (!Float.isNaN(height) && height != demNoDataValue) {
                heights.add(height);
            }
        }

        // get max/min and add extra 25% to max height ~ just to be sure
        if (heights.size() > 2) {
            heightArray[0] = Collections.min(heights);
            heightArray[1] = Collections.max(heights) * 1.25;
        } else { // if nodatavalues return 0s ~ tile in the sea
            heightArray[0] = 0;
            heightArray[1] = 0;
        }

        return heightArray;
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
            super(SubtRefDemOp.class);
            setOperatorUI(SubtRefDemOpUI.class);
        }
    }
}
