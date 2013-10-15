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
package org.esa.nest.gpf.filtering;

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
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.UndersamplingOp;

import java.awt.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * The sample operator implementation for an algorithm
 * that can compute bands independently of each other.
 */
@OperatorMetadata(alias="Image-Filter",
        category = "Image Processing",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Common Image Processing Filters")
public class FilterOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;
    
    @Parameter
    private String selectedFilterName = null;

    @Parameter(description = "The kernel file", label="Kernel File")
    private File userDefinedKernelFile = null;

    private transient Map<Band, Band> bandMap;
    private transient final Map<String, Filter> filterMap = new HashMap<String, Filter>(5);

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public FilterOperator() {
        populateFilterMap(LINE_DETECTION_FILTERS);
        populateFilterMap(GRADIENT_DETECTION_FILTERS);
        populateFilterMap(SMOOTHING_FILTERS);
        populateFilterMap(SHARPENING_FILTERS);
        populateFilterMap(LAPLACIAN_FILTERS);
        populateFilterMap(NON_LINEAR_FILTERS);
    }

    private void populateFilterMap(Filter[] filters) {
        for(Filter f : filters) {
            filterMap.put(f.toString(), f);
        }
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
            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            Filter selectedFilter = null;
            if (userDefinedKernelFile != null) {
                selectedFilter = getUserDefinedFilter(userDefinedKernelFile);
            } else if(selectedFilterName != null) {
                selectedFilter = filterMap.get(selectedFilterName);
            }

            if(selectedFilter == null)
                return;

            bandMap = new HashMap<Band, Band>(5);
            final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

            for(Band srcBand : sourceBands) {
                final Band targetBand = new Band(srcBand.getName(), ProductData.TYPE_FLOAT32,
                        sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
                targetBand.setUnit(srcBand.getUnit());
                targetProduct.addBand(targetBand);

                final String filterBandName = "tmp_filter_" + srcBand.getName();
                final FilterBand filterBand = createFilterBand(selectedFilter, filterBandName, srcBand);
                bandMap.put(targetBand, filterBand);

                final Band existingBand = sourceProduct.getBand(filterBandName);
                if(existingBand != null)
                    sourceProduct.removeBand(existingBand);
                sourceProduct.addBand(filterBand);
            }

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            targetProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 512);
        } catch(Throwable e) {
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final Tile sourceRaster = getSourceTile(bandMap.get(targetBand), targetTileRectangle);

        final ProductData masterData = sourceRaster.getDataBuffer();
        final ProductData targetData = targetTile.getDataBuffer();
        for (int y = targetTileRectangle.y; y < targetTileRectangle.y + targetTileRectangle.height; y++) {
            for (int x = targetTileRectangle.x; x < targetTileRectangle.x + targetTileRectangle.width; x++) {
                final int index = sourceRaster.getDataBufferIndex(x, y);
                targetData.setElemFloatAt(index, masterData.getElemFloatAt(index));
            }
        }
    }

    private static FilterBand createFilterBand(Filter filter, String bandName, RasterDataNode raster) {

        final FilterBand filterBand;
        if (filter instanceof KernelFilter) {
            final KernelFilter kernelFilter = (KernelFilter) filter;
            filterBand = new ConvolutionFilterBand(bandName, raster, kernelFilter.kernel);
        } else {
            final GeneralFilter generalFilter = (GeneralFilter) filter;
            filterBand = new GeneralFilterBand(bandName, raster, generalFilter.width, generalFilter.operator);
        }
        final String descr = MessageFormat.format("Filter ''{0}''", filter.toString());
        filterBand.setDescription(descr);
        return filterBand;
    }

    private static KernelFilter getUserDefinedFilter(File userDefinedKernelFile) {
        final float[][] kernelData = UndersamplingOp.readFile(userDefinedKernelFile.getAbsolutePath());
        final int filterWidth = kernelData.length;
        final int filterHeight = kernelData[0].length;
        final double[] data = new double[filterWidth*filterHeight];
        int k = 0;
        for (int r = 0; r < filterHeight; r++) {
            for (int c = 0; c < filterWidth; c++) {
                data[k++] = (double)kernelData[r][c];
            }
        }
        return new KernelFilter("User Defined Filter", new Kernel(filterWidth, filterHeight, data));
    }


    static abstract class Filter {

        private final String name;

        public Filter(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public abstract boolean equals(Object obj);
    }

    private static class KernelFilter extends Filter {

        private final Kernel kernel;

        public KernelFilter(String name, Kernel kernel) {
            super(name);
            this.kernel = kernel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof KernelFilter) {
                final KernelFilter other = (KernelFilter) obj;
                return toString().equals(other.toString()) && kernel.equals(other.kernel);
            }
            return false;
        }
    }

    private static class GeneralFilter extends Filter {

        final int width;
        final int height;
        final GeneralFilterBand.Operator operator;

        public GeneralFilter(String name, int width, int height, GeneralFilterBand.Operator operator) {
            super(name);
            this.width = width;
            this.height = height;
            this.operator = operator;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof GeneralFilter) {
                final GeneralFilter other = (GeneralFilter) obj;
                return toString().equals(other.toString()) && operator == other.operator;
            }
            return false;
        }
    }

    static final Filter[] LINE_DETECTION_FILTERS = {
            new KernelFilter("Horizontal Edges", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    +2, +2, +2,
                    -1, -1, -1
            })),
            new KernelFilter("Vertical Edges", new Kernel(3, 3, new double[]{
                    -1, +2, -1,
                    -1, +2, -1,
                    -1, +2, -1
            })),
            new KernelFilter("Left Diagonal Edges", new Kernel(3, 3, new double[]{
                    +2, -1, -1,
                    -1, +2, -1,
                    -1, -1, +2
            })),
            new KernelFilter("Right Diagonal Edges", new Kernel(3, 3, new double[]{
                    -1, -1, +2,
                    -1, +2, -1,
                    +2, -1, -1
            })),

            new KernelFilter("Compass Edge Detector", new Kernel(3, 3, new double[]{
                    -1, +1, +1,
                    -1, -2, +1,
                    -1, +1, +1,
            })),

            new KernelFilter("Diagonal Compass Edge Detector", new Kernel(3, 3, new double[]{
                    +1, +1, +1,
                    -1, -2, +1,
                    -1, -1, +1,
            })),

            new KernelFilter("Roberts Cross North-West", new Kernel(2, 2, new double[]{
                    +1, 0,
                    0, -1,
            })),

            new KernelFilter("Roberts Cross North-East", new Kernel(2, 2, new double[]{
                    0, +1,
                    -1, 0,
            })),
    };
    static final Filter[] GRADIENT_DETECTION_FILTERS = {
            new KernelFilter("Sobel North", new Kernel(3, 3, new double[]{
                    -1, -2, -1,
                    +0, +0, +0,
                    +1, +2, +1,
            })),
            new KernelFilter("Sobel South", new Kernel(3, 3, new double[]{
                    +1, +2, +1,
                    +0, +0, +0,
                    -1, -2, -1,
            })),
            new KernelFilter("Sobel West", new Kernel(3, 3, new double[]{
                    -1, 0, +1,
                    -2, 0, +2,
                    -1, 0, +1,
            })),
            new KernelFilter("Sobel East", new Kernel(3, 3, new double[]{
                    +1, 0, -1,
                    +2, 0, -2,
                    +1, 0, -1,
            })),
            new KernelFilter("Sobel North East", new Kernel(3, 3, new double[]{
                    +0, -1, -2,
                    +1, +0, -1,
                    +2, +1, -0,
            })),
    };
    static final Filter[] SMOOTHING_FILTERS = {
            new KernelFilter("Arithmetic 3x3 Mean", new Kernel(3, 3, 1.0 / 9.0, new double[]{
                    +1, +1, +1,
                    +1, +1, +1,
                    +1, +1, +1,
            })),

            new KernelFilter("Arithmetic 4x4 Mean", new Kernel(4, 4, 1.0 / 16.0, new double[]{
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
            })),

            new KernelFilter("Arithmetic 5x5 Mean", new Kernel(5, 5, 1.0 / 25.0, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            })),

            new KernelFilter("Low-Pass 3x3", new Kernel(3, 3, 1.0 / 16.0, new double[]{
                    +1, +2, +1,
                    +2, +4, +2,
                    +1, +2, +1,
            })),
            new KernelFilter("Low-Pass 5x5", new Kernel(5, 5, 1.0 / 60.0, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +4, +4, +4, +1,
                    +1, +4, 12, +4, +1,
                    +1, +4, +4, +4, +1,
                    +1, +1, +1, +1, +1,
            })),
    };
    static final Filter[] SHARPENING_FILTERS = {
            new KernelFilter("High-Pass 3x3 #1", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    -1, +9, -1,
                    -1, -1, -1
            })),


            new KernelFilter("High-Pass 3x3 #2", new Kernel(3, 3, new double[]{
                    +0, -1, +0,
                    -1, +5, -1,
                    +0, -1, +0
            })),

            new KernelFilter("High-Pass 5x5", new Kernel(5, 5, new double[]{
                    +0, -1, -1, -1, +0,
                    -1, +2, -4, +2, -1,
                    -1, -4, 13, -4, -1,
                    -1, +2, -4, +2, -1,
                    +0, -1, -1, -1, +0,
            })),

    };
    static final Filter[] LAPLACIAN_FILTERS = {
            new KernelFilter("Laplace 3x3", new Kernel(3, 3, new double[]{
                    +0, -1, +0,
                    -1, +4, -1,
                    +0, -1, +0,
            })),
            new KernelFilter("Laplace 5x5", new Kernel(5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, 24, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            })),
    };

    static final Filter[] NON_LINEAR_FILTERS = {
            new GeneralFilter("Minimum 3x3", 3, 3, GeneralFilterBand.MIN),
            new GeneralFilter("Minimum 5x5", 5, 5, GeneralFilterBand.MIN),
            new GeneralFilter("Maximum 3x3", 3, 3, GeneralFilterBand.MAX),
            new GeneralFilter("Maximum 5x5", 5, 5, GeneralFilterBand.MAX),
            new GeneralFilter("Mean 3x3", 3, 3, GeneralFilterBand.MEAN),
            new GeneralFilter("Mean 5x5", 5, 5, GeneralFilterBand.MEAN),
            new GeneralFilter("Median 3x3", 3, 3, GeneralFilterBand.MEDIAN),
            new GeneralFilter("Median 5x5", 5, 5, GeneralFilterBand.MEDIAN),
            //new GeneralFilter("Standard Deviation 3x3", 3, 3, GeneralFilterBand.STDDEV),
            //new GeneralFilter("Standard Deviation 5x5", 5, 5, GeneralFilterBand.STDDEV),
            //new GeneralFilter("Root-Mean-Square 3x3", 3, 3, GeneralFilterBand.RMS),
            //new GeneralFilter("Root-Mean-Square 5x5", 5, 5, GeneralFilterBand.RMS),
    };

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
            super(FilterOperator.class);
            setOperatorUI(FilterOpUI.class);
        }
    }
}
