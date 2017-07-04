/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.raster.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.HashMap;

/**
 * Converts bands to dB
 */

@OperatorMetadata(alias = "LinearToFromdB",
        category = "Raster/Data Conversion",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Converts bands to/from dB")
public final class LinearTodBOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    private static final String dBStr = '_' + Unit.DB;
    private static final double underFlowFloat = 1.0e-30;
    private static final String PRODUCT_SUFFIX = "_dB";

    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<>();

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;

            Tile sourceTile1;
            Tile sourceTile2 = null;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand1;
            if (srcBandNames.length == 1) {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                sourceTile1 = getSourceTile(sourceBand1, targetTileRectangle);
                if (sourceTile1 == null) {
                    throw new OperatorException("Cannot get source tile");
                }
            } else {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
                sourceTile1 = getSourceTile(sourceBand1, targetTileRectangle);
                sourceTile2 = getSourceTile(sourceBand2, targetTileRectangle);
                if (sourceTile1 == null || sourceTile2 == null) {
                    throw new OperatorException("Cannot get source tile");
                }
            }

            final ProductData srcData1 = sourceTile1.getDataBuffer();
            ProductData srcData2 = null;
            if (sourceTile2 != null)
                srcData2 = sourceTile2.getDataBuffer();

            final TileIndex srcIndex = new TileIndex(sourceTile1);
            final boolean linearTodB = !sourceBand1.getUnit().endsWith(Unit.DB);
            final Double nodatavalue = sourceBand1.getNoDataValue();

            final ProductData tgtData = targetTile.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(targetTile);

            double value, i, q;
            final int maxY = y0 + h;
            final int maxX = x0 + w;
            for (int y = y0; y < maxY; ++y) {
                tgtIndex.calculateStride(y);
                srcIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int srcIdx = srcIndex.getIndex(x);
                    if (srcData2 != null) {
                        i = srcData1.getElemDoubleAt(srcIdx);
                        q = srcData2.getElemDoubleAt(srcIdx);
                        value = i * i + q * q;
                    } else {
                        value = srcData1.getElemDoubleAt(srcIdx);
                    }
                    if (nodatavalue.equals(value)) {
                        tgtData.setElemDoubleAt(tgtIdx, value);
                        continue;
                    }
                    if (linearTodB) {
                        if (value < underFlowFloat) {
                            value = -underFlowFloat;
                        } else {
                            value = 10.0 * Math.log10(value);
                        }
                    } else {
                        value = Math.pow(10, value / 10.0);
                    }
                    tgtData.setElemDoubleAt(tgtIdx, value);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final boolean isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;

        String targetBandName;
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            String unit = srcBand.getUnit();
            if (unit == null) {
                unit = Unit.AMPLITUDE;  // assume amplitude
            }

            String targetUnit = "";

            final String[] srcBandNames;
            if (unit.equals(Unit.REAL) || unit.equals(Unit.IMAGINARY)) {

                if (i == sourceBands.length - 1) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !((unit.equals(Unit.REAL) && nextUnit.equals(Unit.IMAGINARY)) ||
                        (unit.equals(Unit.IMAGINARY) && nextUnit.equals(Unit.REAL)))) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                srcBandNames = new String[]{srcBand.getName(), sourceBands[i + 1].getName()};
                targetBandName = "Intensity";
                final String suff = OperatorUtils.getSuffixFromBandName(srcBandNames[0]);
                if (suff != null) {
                    targetBandName += "_" + suff;
                }
                final String pol = OperatorUtils.getBandPolarization(srcBandNames[0], absRoot);
                if (pol != null && !pol.isEmpty() && !targetBandName.toLowerCase().contains(pol)) {
                    targetBandName += "_" + pol.toUpperCase();
                }
                if (isPolsar) {
                    final String pre = OperatorUtils.getPrefixFromBandName(srcBandNames[0]);
                    targetBandName = "Intensity_" + pre;
                }
                targetBandName += dBStr;
                targetUnit = Unit.INTENSITY_DB;

                ++i;
            } else {

                final String srcBandName = srcBand.getName();
                srcBandNames = new String[]{srcBandName};
                if (unit.endsWith(Unit.DB)) {
                    if (srcBandName.endsWith(dBStr)) {
                        targetBandName = srcBandName.substring(0, srcBandName.lastIndexOf(dBStr));
                    } else {
                        targetBandName = srcBandName;
                    }
                    targetUnit = unit.substring(0, unit.indexOf(dBStr));
                } else {
                    targetBandName = srcBandName + dBStr;
                    targetUnit = unit + dBStr;
                }
            }

            if (targetProduct.getBand(targetBandName) == null) {

                final Band targetBand = new Band(targetBandName,
                        ProductData.TYPE_FLOAT32,
                        srcBand.getRasterWidth(),
                        srcBand.getRasterHeight());

                targetBand.setUnit(targetUnit);
                targetBand.setDescription(srcBand.getDescription());
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetBand.setNoDataValueUsed(true);
                targetProduct.addBand(targetBand);

                targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
            }
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
            super(LinearTodBOp.class);
        }
    }
}
