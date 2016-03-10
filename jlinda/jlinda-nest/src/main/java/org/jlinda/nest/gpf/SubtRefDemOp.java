package org.jlinda.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jlinda.core.Constants;
import org.jlinda.core.GeoPoint;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.geom.DemTile;
import org.jlinda.core.geom.TopoPhase;
import org.jlinda.core.utils.GeoUtils;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.nest.utils.BandUtilsDoris;
import org.jlinda.nest.utils.CplxContainer;
import org.jlinda.nest.utils.ProductContainer;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OperatorMetadata(alias = "TopoPhaseRemoval",
        category = "Radar/Interferometric/Products",
        authors = "Petar Marinkovic",
        version = "1.0",
        copyright = "Copyright (C) 2013 by PPO.labs",
        description = "Compute and subtract TOPO phase")
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

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec",
            label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(
            label = "Tile Extension [%]",
            description = "Define extension of tile for DEM simulation (optimization parameter).",
            defaultValue = "100")
    private String tileExtensionPercent = "100";

    @Parameter(description = "The topographic phase band name.",
            defaultValue = "topo_phase",
            label = "Topo Phase Band Name")
    private String topoPhaseBandName = "topo_phase";

    private ElevationModel dem = null;
    private double demNoDataValue = 0;
    private double demSamplingLat;
    private double demSamplingLon;

    // source maps
    private HashMap<Integer, CplxContainer> masterMap = new HashMap<>();
    private HashMap<Integer, CplxContainer> slaveMap = new HashMap<>();

    // target maps
    private HashMap<String, ProductContainer> targetMap = new HashMap<>();

    // operator tags
    public String productTag;

    private static final boolean CREATE_VIRTUAL_BAND = true;
    private static final String PRODUCT_SUFFIX = "_DInSAR";

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException
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

            defineDEM();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void checkUserInput() {

        final InputProductValidator validator = new InputProductValidator(sourceProduct);
        validator.checkIfCoregisteredStack();
        validator.checkIfSLC();
        validator.checkIfTOPSARBurstProduct(false);

        productTag = "_ifg_srd";
        if (validator.isSentinel1Product()) {
            final String topsarTag = getTOPSARTag(sourceProduct);
            productTag = productTag + '_' + topsarTag;
        }
    }

    private static String getTOPSARTag(final Product sourceProduct) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        final Band[] bands = sourceProduct.getBands();
        for (Band band:bands) {
            final String bandName = band.getName();
            if (bandName.contains(acquisitionMode)) {
                final int idx = bandName.indexOf(acquisitionMode);
                return bandName.substring(idx, idx + 6);
            }
        }
        return "";
    }

    private void defineDEM() throws IOException {

        Resampling resampling = Resampling.BILINEAR_INTERPOLATION;
        final ElevationModelRegistry elevationModelRegistry;
        final ElevationModelDescriptor demDescriptor;

        if (externalDEMFile == null) {
            elevationModelRegistry = ElevationModelRegistry.getInstance();
            demDescriptor = elevationModelRegistry.getDescriptor(demName);

            if (demDescriptor == null) {
                throw new OperatorException("The DEM '" + demName + "' is not supported.");
            }

            dem = demDescriptor.createDem(resampling);
            if (dem == null) {
                throw new OperatorException("The DEM '" + demName + "' has not been installed.");
            }

            demNoDataValue = demDescriptor.getNoDataValue();
            demSamplingLat = demDescriptor.getTileWidthInDegrees() * (1.0f / demDescriptor.getTileWidth()) * Constants.DTOR;
            demSamplingLon = demSamplingLat;
        }

        if (externalDEMFile != null) { // if external DEM file is specified by user
            dem = new FileElevationModel(externalDEMFile, resampling.getName(), externalDEMNoDataValue);
            demName = externalDEMFile.getPath();
            demNoDataValue = externalDEMNoDataValue;

            // assume the same sampling in X and Y direction?
            try {
                demSamplingLat = (dem.getGeoPos(new PixelPos(1, 0)).getLat() - dem.getGeoPos(new PixelPos(0, 0)).getLat()) * Constants.DTOR;
                demSamplingLon = (dem.getGeoPos(new PixelPos(0, 1)).getLat() - dem.getGeoPos(new PixelPos(0, 0)).getLat()) * Constants.DTOR;
            } catch (Exception e) {
                throw new OperatorException("The DEM '" + demName + "' cannot be properly interpreted.");
            }
        }
    }

    private void constructSourceMetadata() throws Exception {

        // define sourceMaster/sourceSlave name tags
        final String masterTag = "ifg";
        final String slaveTag = "ifg";

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
                String productName = keyMaster.toString() + '_' + keySlave.toString();

                final CplxContainer slave = slaveMap.get(keySlave);
                final ProductContainer product = new ProductContainer(productName, master, slave, true);

                product.targetBandName_I = 'i' + productTag + '_' + master.date + '_' + slave.date;
                product.targetBandName_Q = 'q' + productTag + '_' + master.date + '_' + slave.date;

                product.masterSubProduct.name = topoPhaseBandName;
                product.masterSubProduct.targetBandName_I = topoPhaseBandName + '_' + master.date + '_' + slave.date;

                // put ifg-product bands into map
                targetMap.put(productName, product);
            }
        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : targetMap.keySet()) {
            final List<String> targetBandNames = new ArrayList<>();
            final ProductContainer container = targetMap.get(key);

            String targetBandName_I = container.targetBandName_I;
            String targetBandName_Q = container.targetBandName_Q;
            Band iBand = targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT32);
            iBand.setUnit(Unit.REAL);
            targetBandNames.add(iBand.getName());
            Band qBand = targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT32);
            qBand.setUnit(Unit.IMAGINARY);
            targetBandNames.add(qBand.getName());

            final String tag0 = container.sourceMaster.date;
            final String tag1 = container.sourceSlave.date;
            if (CREATE_VIRTUAL_BAND) {
                String countStr = productTag + '_' + tag0 + '_' + tag1;

                Band intensityBand = ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I),
                        targetProduct.getBand(targetBandName_Q), countStr);
                targetBandNames.add(intensityBand.getName());

                Band phaseBand = ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I),
                        targetProduct.getBand(targetBandName_Q), countStr);
                targetBandNames.add(phaseBand.getName());

                targetProduct.setQuicklookBandName(phaseBand.getName());
            }

            if (container.subProductsFlag) {
                String topoBandName = container.masterSubProduct.targetBandName_I;
                Band topoBand = targetProduct.addBand(topoBandName, ProductData.TYPE_FLOAT32);
                topoBand.setNoDataValue(demNoDataValue);
                topoBand.setUnit(Unit.PHASE);
                topoBand.setDescription("topographic_phase");
                targetBandNames.add(topoBand.getName());
            }

            // copy other bands through
            String tagStr = tag0 + '_' + tag1;
            for(Band srcBand : sourceProduct.getBands()) {
                if(srcBand instanceof VirtualBand) {
                    continue;
                }

                String srcBandName = srcBand.getName();
                if(srcBandName.endsWith(tagStr)) {
                    if (srcBandName.startsWith("coh")|| srcBandName.startsWith("elev")) {
                        Band band = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                        targetBandNames.add(band.getName());
                    }
                }
            }

            String slvProductName = StackUtils.findOriginalSlaveProductName(sourceProduct, container.sourceSlave.realBand);
            StackUtils.saveSlaveProductBandNames(targetProduct, slvProductName,
                                                 targetBandNames.toArray(new String[targetBandNames.size()]));
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            int y0 = targetRectangle.y;
            int yN = y0 + targetRectangle.height - 1;
            int x0 = targetRectangle.x;
            int xN = targetRectangle.x + targetRectangle.width - 1;
            final Window tileWindow = new Window(y0, yN, x0, xN);

            DemTile demTile = getDEMTile(
                    tileWindow, targetMap, dem, demNoDataValue, demSamplingLat, demSamplingLon, tileExtensionPercent);

            Band topoPhaseBand, targetBand_I, targetBand_Q;

            // TODO: smarter extension of search space : foreshortening extension? can I calculate how bit tile I
            // need (extra space) for the coverage, taking into the consideration only height of the tile?
            for (String ifgKey : targetMap.keySet()) {

                ProductContainer product = targetMap.get(ifgKey);

                TopoPhase topoPhase = computeTopoPhase(product, tileWindow, demTile);

                /// check out results from source ///
                Tile tileReal = getSourceTile(product.sourceMaster.realBand, targetRectangle);
                Tile tileImag = getSourceTile(product.sourceMaster.imagBand, targetRectangle);
                ComplexDoubleMatrix complexIfg = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                final ComplexDoubleMatrix cplxTopoPhase = new ComplexDoubleMatrix(
                        MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                        MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                complexIfg.muli(cplxTopoPhase.conji());

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

    public static DemTile getDEMTile(final org.jlinda.core.Window tileWindow,
                                     final HashMap<String, ProductContainer> targetMap,
                                     final ElevationModel dem,
                                     final double demNoDataValue,
                                     final double demSamplingLat,
                                     final double demSamplingLon,
                                     final String tileExtensionPercent) {

        try {
            ProductContainer mstContainer = targetMap.values().iterator().next();

            // compute tile geo-corners ~ work on ellipsoid
            GeoPoint[] geoCorners = org.jlinda.core.utils.GeoUtils.computeCorners(
                    mstContainer.sourceMaster.metaData, mstContainer.sourceMaster.orbit, tileWindow);

            // get corners as DEM indices
            PixelPos[] pixelCorners = new PixelPos[2];
            pixelCorners[0] = dem.getIndex(new GeoPos(geoCorners[0].lat, geoCorners[0].lon));
            pixelCorners[1] = dem.getIndex(new GeoPos(geoCorners[1].lat, geoCorners[1].lon));

            final int x0DEM = (int)Math.round(pixelCorners[0].x);
            final int y0DEM = (int)Math.round(pixelCorners[0].y);
            final int x1DEM = (int)Math.round(pixelCorners[1].x);
            final int y1DEM = (int)Math.round(pixelCorners[1].y);
            final Rectangle demTileRect = new Rectangle(x0DEM, y0DEM, x1DEM - x0DEM + 1, y1DEM - y0DEM + 1);

            // get max/min height of tile ~ uses 'fast' GCP based interpolation technique
            final double[] tileHeights = computeMaxHeight(
                    pixelCorners, demTileRect, tileExtensionPercent, dem, demNoDataValue);

            // compute extra lat/lon for dem tile
            GeoPoint geoExtent = org.jlinda.core.utils.GeoUtils.defineExtraPhiLam(tileHeights[0], tileHeights[1],
                    tileWindow, mstContainer.sourceMaster.metaData,
                    mstContainer.sourceMaster.orbit);

            // extend corners
            geoCorners = org.jlinda.core.utils.GeoUtils.extendCorners(geoExtent, geoCorners);

            // update corners
            pixelCorners[0] = dem.getIndex(new GeoPos(geoCorners[0].lat, geoCorners[0].lon));
            pixelCorners[1] = dem.getIndex(new GeoPos(geoCorners[1].lat, geoCorners[1].lon));

            pixelCorners[0] = new PixelPos(Math.floor(pixelCorners[0].x), Math.floor(pixelCorners[0].y));
            pixelCorners[1] = new PixelPos(Math.ceil(pixelCorners[1].x), Math.ceil(pixelCorners[1].y));

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
                        double elev = dem.getSample(x, y);
                        if (Double.isNaN(elev)) {
                            elev = demNoDataValue;
                        }
                        elevation[i][j] = elev;
                    } catch (Exception e) {
                        elevation[i][j] = demNoDataValue;
                    }
                }
            }

            DemTile demTile = new DemTile(upperLeftGeo.lat * org.jlinda.core.Constants.DTOR,
                    upperLeftGeo.lon * org.jlinda.core.Constants.DTOR,
                    nLatPixels, nLonPixels, Math.abs(demSamplingLat),
                    Math.abs(demSamplingLon), (long)demNoDataValue);

            demTile.setData(elevation);

            return demTile;

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    public static TopoPhase computeTopoPhase(
            final ProductContainer product, final Window tileWindow, final DemTile demTile) {

        try {
            final TopoPhase topoPhase = new TopoPhase(product.sourceMaster.metaData, product.sourceMaster.orbit,
                    product.sourceSlave.metaData, product.sourceSlave.orbit, tileWindow, demTile);

            topoPhase.radarCode();

            topoPhase.gridData();

            return topoPhase;

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private static double[] computeMaxHeight(
            final PixelPos[] corners, final Rectangle rectangle, final String tileExtensionPercent,
            final ElevationModel dem, final double demNoDataValue) throws Exception {

        /* Notes:
          - The scaling and extensions of extreme values of DEM tiles has to be performed to guarantee the overlap
            between SAR and DEM tiles, and avoid blanks in the simulated Topo phase.

          - More conservative, while also more reliable parameters are introduced that guarantee good results even
            in some extreme cases.

          - Parameters are defined for the reliability, not(!) the performance.
         */

        int tileExtPercent = Integer.parseInt(tileExtensionPercent);
        final float extraTileX = (float) (1 + tileExtPercent / 100.0); // = 1.5f
        final float extraTileY = (float) (1 + tileExtPercent / 100.0); // = 1.5f
        final float scaleMaxHeight = (float) (1 + tileExtPercent/ 100.0); // = 1.25f

        double[] heightArray = new double[2];

        // double square root : scales with the size of tile
        final int numberOfPoints = (int) (10 * Math.sqrt(Math.sqrt(rectangle.width * rectangle.height)));

        // extend tiles for which statistics is computed
        final int offsetX = (int) (extraTileX * rectangle.width);
        final int offsetY = (int) (extraTileY * rectangle.height);

        // define window
        final Window window = new Window((long)(corners[0].y - offsetY),
                                         (long)(corners[1].y + offsetY),
                                         (long)(corners[0].x - offsetX),
                                         (long)(corners[1].x + offsetX));

        // distribute points
        final int[][] points = MathUtils.distributePoints(numberOfPoints, window);
        final ArrayList<Double> heights = new ArrayList();

        // then for number of extra points
        for (int[] point : points) {
            double height = dem.getSample(point[1], point[0]);
            if (!Double.isNaN(height) && height != demNoDataValue) {
                heights.add(height);
            }
        }

        // get max/min and add extras ~ just to be sure
        if (heights.size() > 2) {
            // set minimum to 'zero', eg, what if there's small lake in tile?
            // heightArray[0] = Collections.min(heights);
            heightArray[0] = Collections.min(heights);
            heightArray[1] = Collections.max(heights) * scaleMaxHeight;
        } else { // if nodatavalues return 0s ~ tile in the sea
            heightArray[0] = 0;
            heightArray[1] = 0;
        }

        return heightArray;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SubtRefDemOp.class);
        }
    }
}
