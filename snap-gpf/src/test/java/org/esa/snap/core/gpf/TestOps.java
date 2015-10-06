/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.annotations.TargetProperty;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.geotools.referencing.CRS;

import java.awt.Rectangle;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration")
public class TestOps {

    public static final int RASTER_WIDTH = 3;
    public static final int RASTER_HEIGHT = 2;
    static String calls = "";

    public static void registerCall(String str) {
        calls += str;
    }

    public static void clearCalls() {
        calls = "";
    }

    public static String getCalls() {
        return calls;
    }

    @OperatorMetadata(alias = "Op1")
    public static class Op1 extends Operator {

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {
            targetProduct = new Product("Op1Name", "Op1Type", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand(new Band("Op1A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            //System.out.println("=====>>>>>> Op1.computeBand  start");
            registerCall("Op1;");
            //System.out.println("=====>>>>>> Op1.computeBand  end");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(Op1.class);
            }
        }
    }

    @OperatorMetadata(alias = "Op2")
    public static class Op2 extends Operator {

        @Parameter
        public double threshold;

        @SourceProduct(bands = {"Op1A"})
        public Product input;

        @TargetProduct
        public Product output;

        @Override
        public void initialize() {
            output = new Product("Op2Name", "Op2Type", RASTER_WIDTH, RASTER_HEIGHT);
            output.addBand(new Band("Op2A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("Op2B", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws
                                                                                                           OperatorException {
            //System.out.println("=====>>>>>> Op2.computeAllBands  start");
            getSourceTile(input.getBand("Op1A"), rectangle);

            targetTiles.get(output.getBand("Op2A"));
            targetTiles.get(output.getBand("Op2B"));
            //System.out.println("=====>>>>>> Op2.computeAllBands end");

            registerCall("Op2;");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(Op2.class);
            }
        }
    }

    @OperatorMetadata(alias = "Op3")
    public static class Op3 extends Operator {

        @Parameter
        public boolean ignoreSign;

        @Parameter(description = "The valid mask expression")
        public String expression;

        @Parameter(valueSet = {"NN", "BQ", "CC"}, defaultValue = "NN")
        public String interpolMethod;

        @Parameter(defaultValue = "1.5", interval = "[-10,10)")
        public double factor;

        @SourceProduct(bands = {"Op1A"})
        public Product input1;

        @SourceProduct(bands = {"Op2A", "Op2B"})
        public Product input2;

        @SourceProducts
        public Product[] inputs;

        @TargetProduct
        public Product output;

        @Override
        public void initialize() {
            output = new Product("Op3Name", "Op3Type", RASTER_WIDTH, RASTER_HEIGHT);
            output.addBand(new Band("Op3A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("Op3B", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("Op3C", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            output.addBand(new Band("Op3D", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws
                                                                                                           OperatorException {
            //System.out.println("=====>>>>>> Op3.computeAllBands  start");

            getSourceTile(input1.getBand("Op1A"), rectangle);
            getSourceTile(input2.getBand("Op2A"), rectangle);
            getSourceTile(input2.getBand("Op2B"), rectangle);

            targetTiles.get(output.getBand("Op3A"));
            targetTiles.get(output.getBand("Op3B"));
            targetTiles.get(output.getBand("Op3C"));
            targetTiles.get(output.getBand("Op3D"));
            registerCall("Op3;");

            //System.out.println("=====>>>>>> Op3.computeAllBands  end");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(Op3.class);
            }
        }
    }

    @OperatorMetadata(alias = "Op4")
    public static class Op4 extends Operator {

        @TargetProduct
        private Product targetProduct;

        @TargetProperty(alias = "PI", description = "The ratio of any circle's circumference to its diameter")
        private double pi;

        @TargetProperty
        private String[] names;

        @Override
        public void initialize() {
            targetProduct = new Product("Op1Name", "Op1Type", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand(new Band("Op1A", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
            pi = 3.142;
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            //System.out.println("=====>>>>>> Op4.computeBand  start");
            registerCall("Op4;");
            //System.out.println("=====>>>>>> Op4.computeBand  end");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(Op4.class);
            }
        }
    }


    @OperatorMetadata(alias = "Op5")
    public static class Op5 extends Operator {

        @SourceProducts
        Product[] sourceProducts;

        @SourceProduct(alias = "Vincent")
        Product namedProduct;

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {
            targetProduct = new Product("Op5", "Op5Type", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand(new Band("Op5", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            //System.out.println("=====>>>>>> Op4.computeBand  start");
            registerCall("Op5;");
            //System.out.println("=====>>>>>> Op4.computeBand  end");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(Op5.class);
            }
        }
    }

    @OperatorMetadata(alias = "OutputOp", autoWriteDisabled = true)
    public static class OpImplementingOutput extends Operator {

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {
            targetProduct = new Product("OutputOp", "OutputOp", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand(new Band("OutputOp", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            //System.out.println("=====>>>>>> OutputOp.computeBand  start");
            registerCall("OutputOp;");
            //System.out.println("=====>>>>>> OutputOp.computeBand  end");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(OpImplementingOutput.class);
            }
        }
    }

    @OperatorMetadata(alias = "OpWithConverter")
    public static class OpParameterConverter extends Operator {

        @Parameter(converter = JtsGeometryConverter.class)
        private Geometry parameterWithConverter;

        @Parameter(domConverter = TestDomConverter.class)
        private CRS parameterWithDomConverter;

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {
            targetProduct = new Product("OpWithConverter", "Converting", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand(new Band("region", ProductData.TYPE_INT8, RASTER_WIDTH, RASTER_HEIGHT));
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            registerCall("OpWithConverter;");
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(OpParameterConverter.class);
            }
        }

        public static class TestDomConverter implements DomConverter {


            @Override
            public Class<?> getValueType() {
                return null;
            }

            @Override
            public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
                return null;
            }

            @Override
            public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {

            }
        }
    }
}
