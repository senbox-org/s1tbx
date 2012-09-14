/*
 * Copyright (C) 2002-2007 by ?
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package $groupId;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.datamodel.AbstractMetadata;

import java.awt.*;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * The sample operator implementation for an algorithm
 * that can compute bands independently of each other.
 */
@OperatorMetadata(alias="$artifactId")
public class SingleTileOperator extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    String[] sourceBandNames;

    private final HashMap<Band, Band> targetBandToSourceBandMap = new HashMap<Band, Band>();

    /**
	     * Default constructor. The graph processing framework
	     * requires that an operator has a default constructor.
	 */
    public SingleTileOperator() {
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
            getSourceMetadata();

            // create target product
            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            // Add target bands
            addSelectedBands();

            // copy or create product nodes for metadata, tiepoint grids, geocoding, start/end times, etc.
            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            // update the metadata with the affect of the processing
            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute mean pixel spacing (in m).
     * @throws Exception The exception.
     */
    private void getSourceMetadata() throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final double azimuthSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);

    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        //AbstractMetadata.setAttribute(absTgt, AbstractMetadata.multilook_flag, 1);
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        // if user did not select any band then add all
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final ArrayList<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        for (String srcBandName : sourceBandNames) {
            final Band srcBand = sourceProduct.getBand(srcBandName);

            final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct);

            targetBandToSourceBandMap.put(targetBand, srcBand);
        }
    }

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
        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final Band srcBand = targetBandToSourceBandMap.get(targetBand);
        final Tile sourceRaster = getSourceTile(srcBand, targetTileRectangle);

        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                final GeoCoding geoCoding = sourceProduct.getGeoCoding();
                final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
                // Some algorithms may require geoPos...

                final double v = sourceRaster.getSampleDouble(x, y);
                final double v1 = 0.1 * v; // Place your transformation math here
                targetTile.setSample(x, y, v1);
            }
        }
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
 /*   @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        Tile sourceRaster = getSourceTile(sourceBand, targetRectangle, pm);
        Tile targetRaster1 = getSourceTile(targetBand1, targetRectangle, pm);
        Tile targetRaster2 = getSourceTile(targetBand2, targetRectangle, pm);

        int x0 = targetRectangle.x;
        int y0 = targetRectangle.y;
        int w = targetRectangle.width;
        int h = targetRectangle.height;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                GeoCoding geoCoding = sourceProduct.getGeoCoding();
                GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
                // Some algorithms may require geoPos...

                double v = sourceRaster.getSampleDouble(x, y);
                double v1 = 0.1 * v; // Place your transformation math here
                double v2 = 0.2 * v; // Place your transformation math here
                targetRaster1.setSample(x, y, v1);
                targetRaster2.setSample(x, y, v2);
            }
        }
    }*/
	
	
    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SingleTileOperator.class);
        }
    }
}
