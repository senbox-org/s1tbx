/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.eo.Constants;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a new product with only selected bands
 */

@OperatorMetadata(alias = "TOPSAR-Split",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Creates a new product with only the selected subswath")
public final class TOPSARSplitOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", label = "Subswath")
    private String subswath = null;

    @Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwathInfo = null;
    private int subSwathIndex = 0;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSentinel1Product();
            validator.checkIfTOPSARBurstProduct(true);
            validator.checkProductType(new String[]{"SLC"});
            validator.checkAcquisitionMode(new String[]{"IW","EW"});

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            if (subswath == null) {
                final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
                subswath = acquisitionMode + '1';
            }

            su = new Sentinel1Utils(sourceProduct);
            subSwathInfo = su.getSubSwath();
            for (int i = 0; i < subSwathInfo.length; i++) {
                if (subSwathInfo[i].subSwathName.contains(subswath)) {
                    subSwathIndex = i + 1;
                    break;
                }
            }

            if (selectedPolarisations == null || selectedPolarisations.length == 0) {
                selectedPolarisations = Sentinel1Utils.getProductPolarizations(absRoot);
            }

            final List<Band> selectedBands = new ArrayList<>();
            for (Band srcBand : sourceProduct.getBands()) {
                if (srcBand.getName().contains(subswath)) {
                    for (String pol : selectedPolarisations) {
                        if (srcBand.getName().contains(pol)) {
                            selectedBands.add(srcBand);
                        }
                    }
                }
            }

            targetProduct = new Product(sourceProduct.getName() + '_' + subswath,
                    sourceProduct.getProductType(),
                    selectedBands.get(0).getSceneRasterWidth(),
                    selectedBands.get(0).getSceneRasterHeight());

            for (Band srcBand : selectedBands) {
                if (srcBand instanceof VirtualBand) {
                    final VirtualBand sourceBand = (VirtualBand) srcBand;
                    final VirtualBand targetBand = new VirtualBand(sourceBand.getName(),
                            sourceBand.getDataType(),
                            sourceBand.getRasterWidth(),
                            sourceBand.getRasterHeight(),
                            sourceBand.getExpression());
                    ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                    targetProduct.addBand(targetBand);
                } else {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                }
            }
            for (TiePointGrid srcTPG : sourceProduct.getTiePointGrids()) {
                if (srcTPG.getName().contains(subswath)) {
                    final TiePointGrid dstTPG = srcTPG.cloneTiePointGrid();
                    dstTPG.setName(srcTPG.getName().replace(subswath+'_', ""));
                    targetProduct.addTiePointGrid(dstTPG);
                }
            }

            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            ProductUtils.copyMasks(sourceProduct, targetProduct);
            ProductUtils.copyVectorData(sourceProduct, targetProduct);
            ProductUtils.copyIndexCodings(sourceProduct, targetProduct);
            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());
            targetProduct.setDescription(sourceProduct.getDescription());

            addGeocoding();
            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void addGeocoding() {
        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(targetProduct.getTiePointGrid(OperatorUtils.TPG_LATITUDE),
                                                                    targetProduct.getTiePointGrid(OperatorUtils.TPG_LONGITUDE), Datum.WGS_84);
        targetProduct.setGeoCoding(tpGeoCoding);
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);

        absRoot.setAttributeUTC(AbstractMetadata.first_line_time,
                new ProductData.UTC(subSwathInfo[subSwathIndex - 1].firstLineTime / Constants.secondsInDay));

        absRoot.setAttributeUTC(AbstractMetadata.last_line_time,
                new ProductData.UTC(subSwathInfo[subSwathIndex - 1].lastLineTime / Constants.secondsInDay));

        absRoot.setAttributeDouble(AbstractMetadata.line_time_interval,
                subSwathInfo[subSwathIndex - 1].azimuthTimeInterval);

        absRoot.setAttributeDouble(AbstractMetadata.slant_range_to_first_pixel,
                subSwathInfo[subSwathIndex - 1].slrTimeToFirstPixel * Constants.lightSpeed);

        absRoot.setAttributeDouble(AbstractMetadata.range_spacing,
                subSwathInfo[subSwathIndex - 1].rangePixelSpacing);

        absRoot.setAttributeDouble(AbstractMetadata.azimuth_spacing,
                subSwathInfo[subSwathIndex - 1].azimuthPixelSpacing);

        final int rows = subSwathInfo[subSwathIndex - 1].latitude.length;
        final int cols = subSwathInfo[subSwathIndex - 1].latitude[0].length;
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat,
                subSwathInfo[subSwathIndex - 1].latitude[0][0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long,
                subSwathInfo[subSwathIndex - 1].longitude[0][0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat,
                subSwathInfo[subSwathIndex - 1].latitude[0][cols - 1]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long,
                subSwathInfo[subSwathIndex - 1].longitude[0][cols - 1]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat,
                subSwathInfo[subSwathIndex - 1].latitude[rows - 1][0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long,
                subSwathInfo[subSwathIndex - 1].longitude[rows - 1][0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat,
                subSwathInfo[subSwathIndex - 1].latitude[rows - 1][cols - 1]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long,
                subSwathInfo[subSwathIndex - 1].longitude[rows - 1][cols - 1]);

        final MetadataElement[] bandMetadataList = AbstractMetadata.getBandAbsMetadataList(absRoot);
        for (MetadataElement bandMeta : bandMetadataList) {
            boolean include = false;

            if (bandMeta.getName().contains(subswath)) {

                for (String pol : selectedPolarisations) {
                    if (bandMeta.getName().contains(pol)) {
                        include = true;
                        break;
                    }
                }
            }
            if (!include) {
                // remove band metadata if polarization or subswath is not included
                absRoot.removeElement(bandMeta);
            }
        }

        final MetadataElement origMeta = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        removeElements(origMeta, "annotation");
        removeElements(origMeta, "calibration");
        removeElements(origMeta, "noise");
    }

    private void removeElements(final MetadataElement origMeta, final String parent) {
        final MetadataElement parentElem = origMeta.getElement(parent);
        if(parentElem != null) {
            final MetadataElement[] elemList = parentElem.getElements();
            for (MetadataElement elem : elemList) {
                if (!elem.getName().toUpperCase().contains(subswath)) {
                    parentElem.removeElement(elem);
                }
            }
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
            super(TOPSARSplitOp.class);
        }
    }
}