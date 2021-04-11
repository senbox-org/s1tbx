package org.jlinda.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
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
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.geom.DemTile;
import org.jlinda.core.geom.TopoPhase;
import org.jlinda.core.utils.BandUtilsDoris;
import org.jlinda.core.utils.CplxContainer;
import org.jlinda.core.utils.ProductContainer;
import org.jlinda.core.utils.TileUtilsDoris;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    @Parameter(description = "Output topographic phase band.", defaultValue = "false", label = "Output topographic phase band")
    private Boolean outputTopoPhaseBand = false;

    @Parameter(description = "Output elevation band.", defaultValue = "false", label = "Output elevation band")
    private Boolean outputElevationBand = false;

    @Parameter(description = "Output lat/lon bands.", defaultValue = "false", label = "Output lat/lon band")
    private Boolean outputLatLonBands = false;

    private ElevationModel dem = null;
    private double demNoDataValue = 0;
    private double demSamplingLat;
    private double demSamplingLon;
    private boolean demDefined = false;

    // source maps
    private Map<String, CplxContainer> masterMap = new HashMap<>();
    private Map<String, CplxContainer> slaveMap = new HashMap<>();

    // target maps
    private Map<String, ProductContainer> targetMap = new HashMap<>();

    private String[] polarisations;

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

            if(outputTopoPhaseBand == null)
                outputTopoPhaseBand = false;

            if(outputElevationBand == null)
                outputElevationBand = false;

            if (outputLatLonBands == null)
                outputLatLonBands = false;

            constructSourceMetadata();
            constructTargetMetadata();

            createTargetProduct();

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

        polarisations = OperatorUtils.getPolarisations(sourceProduct);
        if (polarisations.length == 0) {
            polarisations = new String[]{""};
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

    private synchronized void defineDEM() throws IOException {
        if(demDefined)
            return;

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
            //((FileElevationModel)dem).applyEarthGravitionalModel(true);
            demName = externalDEMFile.getPath();
            demNoDataValue = externalDEMNoDataValue;

            // assume the same sampling in X and Y direction?
            try {
                demSamplingLat = (dem.getGeoPos(new PixelPos(0, 1)).getLat() -
                        dem.getGeoPos(new PixelPos(0, 0)).getLat()) * Constants.DTOR;

                demSamplingLon = (dem.getGeoPos(new PixelPos(1, 0)).getLon() -
                        dem.getGeoPos(new PixelPos(0, 0)).getLon()) * Constants.DTOR;

            } catch (Exception e) {
                throw new OperatorException("The DEM '" + demName + "' cannot be properly interpreted.");
            }
        }
        if(outputElevationBand) {
            Band elevBand = targetProduct.getBand("elevation");
            if(elevBand != null) {
                elevBand.setNoDataValue(demNoDataValue);
            }
        }

        if (outputLatLonBands) {
            Band latBand = targetProduct.getBand("orthorectifiedLat");
            if (latBand != null) {
               latBand.setNoDataValue(Double.NaN);
            }
            Band lonBand = targetProduct.getBand("orthorectifiedLon");
            if (lonBand != null) {
                lonBand.setNoDataValue(Double.NaN);
            }
        }

        demDefined = true;
    }

    private void constructSourceMetadata() throws Exception {

        final MetadataElement masterMeta = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        masterMetaMapPut(masterMeta, sourceProduct, masterMap);

        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;
        MetadataElement[] slaveRoot = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot).getElements();
        for (MetadataElement slaveMeta : slaveRoot) {
            if (!slaveMeta.getName().equals(AbstractMetadata.ORIGINAL_PRODUCT_METADATA))
                slaveMetaMapPut(masterMeta, slaveMeta, sourceProduct, slaveMap);
        }
    }

    private void masterMetaMapPut(final MetadataElement mstRoot, final Product product,
                                  final Map<String, CplxContainer> map) throws Exception {

        final String mapKey = Integer.toString(mstRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT));
        final String date = OperatorUtils.getAcquisitionDate(mstRoot);
        final Orbit orbit = new Orbit(mstRoot, orbitDegree);
        final SLCImage meta = new SLCImage(mstRoot, product);
        meta.setMlAz(1);
        meta.setMlRg(1);

        Band bandReal = null;
        Band bandImag = null;

        final String pol = polarisations[0].isEmpty() ? "" : '_' + polarisations[0].toUpperCase();
        for (String bandName : product.getBandNames()) {
            if (bandName.contains("ifg") && bandName.contains(date)) {
                if (pol.isEmpty() || bandName.contains(pol)) {
                    final Band band = product.getBand(bandName);
                    if (BandUtilsDoris.isBandReal(band)) {
                        bandReal = band;
                    } else if (BandUtilsDoris.isBandImag(band)) {
                        bandImag = band;
                    }

                    if (bandReal != null && bandImag != null) {
                        break;
                    }
                }
            }
        }
        if (bandReal == null || bandImag == null) {
            throw new OperatorException("Product must be interferogram");
        }
        map.put(mapKey, new CplxContainer(date, meta, orbit, bandReal, bandImag));
    }

    private void slaveMetaMapPut(final MetadataElement mstRoot, final MetadataElement slvRoot,
                                 final Product product, final Map<String, CplxContainer> map) throws Exception {

        final String mstAcqDate = OperatorUtils.getAcquisitionDate(mstRoot);

        for (String polarisation : polarisations) {
            final String pol = polarisation.isEmpty() ? "" : '_' + polarisation.toUpperCase();
            final String mapKey = slvRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT) + pol;
            final String slvAcqDate = OperatorUtils.getAcquisitionDate(slvRoot);
            final Orbit orbit = new Orbit(slvRoot, orbitDegree);
            final SLCImage meta = new SLCImage(slvRoot, product);
            meta.setMlAz(1);
            meta.setMlRg(1);

            final String mstAcqDate_slvAcqDate = mstAcqDate + "_" + slvAcqDate;
            Band bandReal = null;
            Band bandImag = null;

            for (String bandName : product.getBandNames()) {
                if (bandName.contains("ifg") && bandName.contains(mstAcqDate_slvAcqDate)) {
                    if (pol.isEmpty() || bandName.contains(pol)) {
                        final Band band = product.getBand(bandName);
                        if (BandUtilsDoris.isBandReal(band)) {
                            bandReal = band;
                        } else if (BandUtilsDoris.isBandImag(band)) {
                            bandImag = band;
                        }
                        if (bandReal != null && bandImag != null) {
                            break;
                        }
                    }
                }
            }

            map.put(mapKey, new CplxContainer(slvAcqDate, meta, orbit, bandReal, bandImag));
        }
    }

    private void constructTargetMetadata() {

        for (String keyMaster : masterMap.keySet()) {

            CplxContainer master = masterMap.get(keyMaster);

            for (String keySlave : slaveMap.keySet()) {
                final CplxContainer slave = slaveMap.get(keySlave);

                if (master.polarisation == null || master.polarisation.equals(slave.polarisation)) {
                    String productName = keyMaster + '_' + keySlave;
                    final ProductContainer product = new ProductContainer(productName, master, slave, true);
                    targetMap.put(productName, product);
                }
            }
        }
    }

    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (String key : targetMap.keySet()) {
            final List<String> targetBandNames = new ArrayList<>();
            final ProductContainer container = targetMap.get(key);
            final CplxContainer master = container.sourceMaster;
            final CplxContainer slave = container.sourceSlave;

            final String pol = (master.polarisation == null || master.polarisation.isEmpty()) ? "" :
                    '_' + master.polarisation.toUpperCase();
            final String tag = pol + '_' + master.date + '_' + slave.date;

            String targetBandName_I = "i_ifg" + tag;
            Band iBand = targetProduct.addBand(targetBandName_I, ProductData.TYPE_FLOAT32);
            container.addBand(Unit.REAL, iBand.getName());
            iBand.setUnit(Unit.REAL);
            targetBandNames.add(iBand.getName());

            String targetBandName_Q = "q_ifg" + tag;
            Band qBand = targetProduct.addBand(targetBandName_Q, ProductData.TYPE_FLOAT32);
            container.addBand(Unit.IMAGINARY, qBand.getName());
            qBand.setUnit(Unit.IMAGINARY);
            targetBandNames.add(qBand.getName());

            if (CREATE_VIRTUAL_BAND) {
                String countStr = productTag + tag;

                Band intensityBand = ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand(targetBandName_I),
                        targetProduct.getBand(targetBandName_Q), countStr);
                targetBandNames.add(intensityBand.getName());

                Band phaseBand = ReaderUtils.createVirtualPhaseBand(targetProduct, targetProduct.getBand(targetBandName_I),
                        targetProduct.getBand(targetBandName_Q), countStr);
                targetBandNames.add(phaseBand.getName());

                targetProduct.setQuicklookBandName(phaseBand.getName());
            }

            if (container.subProductsFlag) {
                if(outputTopoPhaseBand) {
                    String topoBandName = "topo_phase" + tag;
                    Band topoBand = targetProduct.addBand(topoBandName, ProductData.TYPE_FLOAT32);
                    container.addBand(Unit.PHASE, topoBand.getName());
                    topoBand.setNoDataValueUsed(true);
                    topoBand.setNoDataValue(0);
                    topoBand.setUnit(Unit.PHASE);
                    topoBand.setDescription("topographic_phase");
                    targetBandNames.add(topoBand.getName());
                }
            }

            // copy other bands through
            for(Band srcBand : sourceProduct.getBands()) {
                if(srcBand instanceof VirtualBand) {
                    continue;
                }

                String srcBandName = srcBand.getName();
                if(srcBandName.endsWith(tag)) {
                    if (srcBandName.startsWith("coh") || srcBandName.startsWith("elev")) {
                        Band band = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                        targetBandNames.add(band.getName());
                    }
                }
            }

            String slvProductName = StackUtils.findOriginalSlaveProductName(sourceProduct, container.sourceSlave.realBand);
            StackUtils.saveSlaveProductBandNames(targetProduct, slvProductName,
                                                 targetBandNames.toArray(new String[targetBandNames.size()]));
        }

        if (outputElevationBand) {
            Band elevBand = targetProduct.addBand("elevation", ProductData.TYPE_FLOAT32);
            elevBand.setNoDataValue(demNoDataValue);
            elevBand.setNoDataValueUsed(true);
            elevBand.setUnit(Unit.METERS);
            elevBand.setDescription("elevation");
        }

        if (outputLatLonBands) {
            Band latBand = targetProduct.addBand("orthorectifiedLat", ProductData.TYPE_FLOAT32);
            latBand.setNoDataValue(Double.NaN);
            latBand.setNoDataValueUsed(true);
            latBand.setUnit(Unit.DEGREES);
            latBand.setDescription("Orthorectified latitude");
            Band lonBand = targetProduct.addBand("orthorectifiedLon", ProductData.TYPE_FLOAT32);
            lonBand.setNoDataValue(Double.NaN);
            lonBand.setNoDataValueUsed(true);
            lonBand.setUnit(Unit.DEGREES);
            lonBand.setDescription("Orthorectified longitude");
        }
    }

    private static void convertToDegree(double[][] a) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                  a[i][j] = a[i][j] * 180.0/Math.PI;
            }
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

            if(!demDefined) {
                defineDEM();
            }

            DemTile demTile = TopoPhase.getDEMTile(
                    tileWindow, targetMap, dem, demNoDataValue, demSamplingLat, demSamplingLon, tileExtensionPercent);
            if(demTile == null) {
                return;
            }

            Band topoPhaseBand, targetBand_I, targetBand_Q, elevBand, latBand, lonBand;

            for (String ifgKey : targetMap.keySet()) {

                ProductContainer product = targetMap.get(ifgKey);

                TopoPhase topoPhase = TopoPhase.computeTopoPhase(product, tileWindow, demTile, outputElevationBand, false);

                Tile tileReal = getSourceTile(product.sourceSlave.realBand, targetRectangle);
                Tile tileImag = getSourceTile(product.sourceSlave.imagBand, targetRectangle);
                ComplexDoubleMatrix complexIfg = TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);

                final ComplexDoubleMatrix cplxTopoPhase = new ComplexDoubleMatrix(
                        MatrixFunctions.cos(new DoubleMatrix(topoPhase.demPhase)),
                        MatrixFunctions.sin(new DoubleMatrix(topoPhase.demPhase)));

                complexIfg.muli(cplxTopoPhase.conji());

                targetBand_I = targetProduct.getBand(product.getBandName(Unit.REAL));
                Tile tileOutReal = targetTileMap.get(targetBand_I);
                TileUtilsDoris.pushDoubleMatrix(complexIfg.real(), tileOutReal, targetRectangle);

                targetBand_Q = targetProduct.getBand(product.getBandName(Unit.IMAGINARY));
                Tile tileOutImag = targetTileMap.get(targetBand_Q);
                TileUtilsDoris.pushDoubleMatrix(complexIfg.imag(), tileOutImag, targetRectangle);

                if(outputTopoPhaseBand) {
                    topoPhaseBand = targetProduct.getBand(product.getBandName(Unit.PHASE));
                    Tile tileOutTopoPhase = targetTileMap.get(topoPhaseBand);
                    TileUtilsDoris.pushDoubleArray2D(topoPhase.demPhase, tileOutTopoPhase, targetRectangle);
                }

                if (outputElevationBand) {
                    elevBand = targetProduct.getBand("elevation");
                    Tile tileElevBand = targetTileMap.get(elevBand);
                    TileUtilsDoris.pushDoubleArray2D(topoPhase.elevation, tileElevBand, targetRectangle);
                }

                if (outputLatLonBands) {
                    TopoPhase topoPhase1 = TopoPhase.computeTopoPhase(product, tileWindow, demTile, false, true);
                    latBand = targetProduct.getBand("orthorectifiedLat");
                    Tile tileLatBand = targetTileMap.get(latBand);
                    convertToDegree(topoPhase1.latitude);
                    TileUtilsDoris.pushDoubleArray2D(topoPhase1.latitude, tileLatBand, targetRectangle);
                    lonBand = targetProduct.getBand("orthorectifiedLon");
                    Tile tileLonBand = targetTileMap.get(lonBand);
                    convertToDegree(topoPhase1.longitude);
                    TileUtilsDoris.pushDoubleArray2D(topoPhase1.longitude, tileLonBand, targetRectangle);
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
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
