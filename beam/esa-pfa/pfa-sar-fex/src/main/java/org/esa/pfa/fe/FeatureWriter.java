/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.pfa.fe;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.fe.op.out.PatchOutput;
import org.esa.pfa.fe.op.out.PatchWriter;
import org.esa.pfa.fe.op.out.PatchWriterFactory;

import javax.media.jai.JAI;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Output features into patches
 */
@OperatorMetadata(alias = "FeatureWriter",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Writes features into patches.",
        category = "Classification\\Feature Extraction")
public abstract class FeatureWriter extends Operator implements Output {

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(description = "The output folder to which the data is written.", label="Output Folder", notNull = true, notEmpty = true)
    private File targetPath;

    @Parameter(defaultValue = "false", description = "Disposes all global image caches after a patch has been completed")
    protected boolean disposeGlobalCaches;

    @Parameter(defaultValue = "true")
    protected boolean overwriteMode;

    @Parameter(defaultValue = "false")
    protected boolean skipFeaturesOutput;

    @Parameter(defaultValue = "false")
    protected boolean skipQuicklookOutput;

    @Parameter(defaultValue = "false")
    protected boolean skipProductOutput;

    protected HashMap<String, Object> patchWriterConfig;

    private String patchWriterFactoryClassName = "org.esa.pfa.fe.op.out.DefaultPatchWriterFactory";

    private transient PatchWriterFactory patchWriterFactory;
    private PatchWriter patchWriter = null;

    @Parameter(description = "Patch size in km", interval = "(0, *)", defaultValue = "12.0", label="Patch Size (km)")
    private double patchSizeKm = 12.0;

    @Parameter(description = "Minimum percentage of valid pixels", label="Minimum valid pixels (%)", defaultValue = "0.1")
    protected float minValidPixels = 0.1f;

    protected int patchWidth = 0;
    protected int patchHeight = 0;

    public static final AttributeType[] STX_ATTRIBUTE_TYPES = new AttributeType[]{
            new AttributeType("mean", "Mean value of valid feature pixels", Double.class),
            new AttributeType("stdev", "Standard deviation of valid feature pixels", Double.class),
            new AttributeType("cvar", "Coefficient of variation of valid feature pixels", Double.class),
            new AttributeType("min", "Minimim value of valid feature pixels", Double.class),
            new AttributeType("max", "Maximum value of valid feature pixels", Double.class),
            new AttributeType("p10", "The threshold such that 10% of the sample values are below the threshold", Double.class),
            new AttributeType("p50", "The threshold such that 50% of the sample values are below the threshold (=median)", Double.class),
            new AttributeType("p90", "The threshold such that 90% of the sample values are below the threshold", Double.class),
            new AttributeType("skewness", "A measure of the extent to which the histogram \"leans\" to one side of the mean. The skewness value can be positive or negative, or even undefined.", Double.class),
            new AttributeType("count", "Sample count (number of valid feature pixels)", Integer.class),
    };

    public FeatureWriter() {
        setRequiresAllBands(true);
    }

    protected abstract FeatureType[] getFeatureTypes();

    protected abstract boolean processPatch(Patch patch, PatchOutput sink) throws IOException;

    @Override
    public void initialize() throws OperatorException {
        try {
            if(targetPath == null || !targetPath.isAbsolute()) {
                throw new OperatorException("Please specify an output folder");
            }

            getLogger().warning("Processing source product " + sourceProduct.getFileLocation());

            if (patchWriterFactory == null) {
                initPatchWriterFactory();
            }

            if (patchWriterConfig == null) {
                patchWriterConfig = new HashMap<>(5);
            }
            patchWriterConfig.put(PatchWriterFactory.PROPERTY_TARGET_PATH, targetPath.getAbsolutePath());
            patchWriterConfig.put(PatchWriterFactory.PROPERTY_OVERWRITE_MODE, overwriteMode);
            patchWriterConfig.put(PatchWriterFactory.PROPERTY_SKIP_QUICKLOOK_OUTPUT, skipQuicklookOutput);
            patchWriterConfig.put(PatchWriterFactory.PROPERTY_SKIP_PRODUCT_OUTPUT, skipProductOutput);
            patchWriterConfig.put(PatchWriterFactory.PROPERTY_SKIP_FEATURE_OUTPUT, skipFeaturesOutput);
            patchWriterFactory.configure(patchWriterConfig);

            if (overwriteMode) {
                getLogger().warning("FexOperator: Overwrite mode is on.");
            }
            if (skipFeaturesOutput) {
                getLogger().warning("FexOperator: Feature output skipped.");
            }
            if (skipProductOutput) {
                getLogger().warning("FexOperator: Product output skipped.");
            }
            if (skipQuicklookOutput) {
                getLogger().warning("FexOperator: RGB image output skipped.");
            }

            setTargetProduct(sourceProduct);

            computePatchDimension();

            getTargetProduct().setPreferredTileSize(patchWidth, patchHeight);

            final FeatureType[] featureTypes = getFeatureTypes();

            patchWriter = patchWriterFactory.createFeatureOutput(sourceProduct);
            patchWriter.initialize(patchWriterFactory.getConfiguration(), getSourceProduct(), featureTypes);

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private void initPatchWriterFactory() {
        try {
            Class<?> featureOutputFactoryClass = getClass().getClassLoader().loadClass(patchWriterFactoryClassName);
            this.patchWriterFactory = (PatchWriterFactory) featureOutputFactoryClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Compute patch dimension for given patch size in kilometer.
     */
    private void computePatchDimension() {

        /*final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        final double azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        patchWidth = (int)(patchSizeKm*1000.0/rangeSpacing);
        patchHeight = (int)(patchSizeKm*1000.0/azimuthSpacing);  */
        patchWidth = 200;
        patchHeight = 200;
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {
            final Product patchProduct = createSubset(sourceProduct, targetRectangle);
            patchProduct.setName("patch");

            final int patchX = (int)(targetRectangle.getMinX()/targetRectangle.getWidth());
            final int patchY = (int)(targetRectangle.getMinY()/targetRectangle.getHeight());

            final Patch patch = new Patch(patchX, patchY, patchProduct);
            processPatch(patch, patchWriter);

            patchProduct.dispose();

            if (disposeGlobalCaches) {
                ImageManager.getInstance().dispose();
                JAI.getDefaultInstance().getTileCache().flush();
            }

        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private static Product createSubset(Product sourceProduct, Rectangle subsetRegion) throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        subsetDef.addNodeNames(sourceProduct.getBandNames());
        subsetDef.setRegion(subsetRegion);
        subsetDef.setSubSampling(1, 1);
        subsetDef.setIgnoreMetadata(false);

        ProductSubsetBuilder subsetBuilder = new ProductSubsetBuilder();
        return subsetBuilder.readProductNodes(sourceProduct, subsetDef);
    }

    protected static RenderedImage createColoredBandImage(RasterDataNode band, double minSample, double maxSample) {
        return ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band}, new ImageInfo(new ColorPaletteDef(minSample, maxSample)), 0);
    }

    protected static Feature createStxFeature(FeatureType featureType, Band band) {
        Guardian.assertSame("invalid feature type", featureType.getAttributeTypes(), STX_ATTRIBUTE_TYPES);
        final Stx stx = band.getStx(true, ProgressMonitor.NULL);
        double p10 = stx.getHistogram().getPTileThreshold(0.1)[0];
        double p50 = stx.getHistogram().getPTileThreshold(0.5)[0];
        double p90 = stx.getHistogram().getPTileThreshold(0.9)[0];
        double mean = stx.getMean();
        double skewness = (p90 - 2 * p50 + p10) / (p90 - p10);
        return new Feature(featureType,
                null,
                mean,
                stx.getStandardDeviation(),
                stx.getStandardDeviation() / mean,
                stx.getMinimum(),
                stx.getMaximum(),
                p10,
                p50,
                p90,
                skewness,
                stx.getSampleCount());
    }
}