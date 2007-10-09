/*
 * $Id: ProductMergeOp.java,v 1.3 2007/05/14 12:25:40 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.operators.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.awt.Rectangle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.3 $ $Date: 2007/05/14 12:25:40 $
 */
public class ProductMergeOp extends Operator implements ParameterConverter {

    public static class Configuration {
        private String productType = "no";
        private String baseGeoInfo;
        private List<BandDesc> bands;

        public Configuration() {
            bands = new ArrayList<BandDesc>();
        }
    }

    @Parameter
    private Configuration config;
    private Map<Band, Band> sourceBands;

    public ProductMergeOp() {
        config = new Configuration();
        sourceBands = new HashMap<Band, Band>();
    }

    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement
    }

    public void setParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        XStream xStream = new XStream();
        xStream.setClassLoader(this.getClass().getClassLoader());
        xStream.alias(configuration.getName(), Configuration.class);
        xStream.alias("band", BandDesc.class);
        xStream.addImplicitCollection(Configuration.class, "bands");
        xStream.unmarshal(new XppDomReader(configuration), config);
    }

    @Override
    public Product initialize() throws OperatorException {

        Product outputProduct;
        if (StringUtils.isNotNullAndNotEmpty(config.baseGeoInfo)) {
            Product baseGeoProduct = getSourceProduct(config.baseGeoInfo);
            final int sceneRasterWidth = baseGeoProduct.getSceneRasterWidth();
            final int sceneRasterHeight = baseGeoProduct.getSceneRasterHeight();
            outputProduct = new Product("mergedName", config.productType,
                                        sceneRasterWidth, sceneRasterHeight);

            copyBaseGeoInfo(baseGeoProduct, outputProduct);
        } else {
            BandDesc bandDesc = config.bands.get(0);
            Product srcProduct = getSourceProduct(bandDesc.product);
            final int sceneRasterWidth = srcProduct.getSceneRasterWidth();
            final int sceneRasterHeight = srcProduct.getSceneRasterHeight();
            outputProduct = new Product("mergedName", config.productType,
                                        sceneRasterWidth, sceneRasterHeight);
        }

        Set<Product> allSrcProducts = new HashSet<Product>();
        for (BandDesc bandDesc : config.bands) {
            Product srcProduct = getSourceProduct(bandDesc.product);
            if (StringUtils.isNotNullAndNotEmpty(bandDesc.name)) {
                if (StringUtils.isNotNullAndNotEmpty(bandDesc.newName)) {
                    copyBandWithFeatures(srcProduct, outputProduct, bandDesc.name, bandDesc.newName);
                } else {
                    copyBandWithFeatures(srcProduct, outputProduct, bandDesc.name);
                }
                allSrcProducts.add(srcProduct);
            } else if (StringUtils.isNotNullAndNotEmpty(bandDesc.nameExp)) {
                Pattern pattern = Pattern.compile(bandDesc.nameExp);
                for (String bandName : srcProduct.getBandNames()) {
                    Matcher matcher = pattern.matcher(bandName);
                    if (matcher.matches()) {
                        copyBandWithFeatures(srcProduct, outputProduct, bandName);
                        allSrcProducts.add(srcProduct);
                    }
                }
            }
        }

        for (Product srcProduct : allSrcProducts) {
            ProductUtils.copyBitmaskDefsAndOverlays(srcProduct, outputProduct);
        }

        return outputProduct;
    }

    /**
     * Copies the tie point data, geocoding and the start and stop time.
     *
     * @param sourceProduct
     * @param destinationProduct
     */
    private static void copyBaseGeoInfo(Product sourceProduct,
                                        Product destinationProduct) {
        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(sourceProduct, destinationProduct);
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, destinationProduct);
        destinationProduct.setStartTime(sourceProduct.getStartTime());
        destinationProduct.setEndTime(sourceProduct.getEndTime());
    }

    private void copyBandWithFeatures(Product srcProduct, Product outputProduct, String oldBandName, String newBandName) {
        Band destBand = copyBandWithFeatures(srcProduct, outputProduct, oldBandName);
        destBand.setName(newBandName);
    }

    private Band copyBandWithFeatures(Product srcProduct, Product outputProduct, String bandName) {
        Band destBand = ProductUtils.copyBand(bandName, srcProduct, outputProduct);
        Band srcBand = srcProduct.getBand(bandName);
        sourceBands.put(destBand, srcBand);
        if (srcBand.getFlagCoding() != null) {
            FlagCoding srcFlagCoding = srcBand.getFlagCoding();
            ProductUtils.copyFlagCoding(srcFlagCoding, outputProduct);
            destBand.setFlagCoding(outputProduct.getFlagCoding(srcFlagCoding.getName()));
        }
        return destBand;
    }

    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        Band sourceBand = sourceBands.get(band);
        Tile sourceTile = getSourceTile(sourceBand, rectangle);

        // TODO replace copy with OpImage delegation
        final int length = rectangle.width * rectangle.height;
        System.arraycopy(sourceTile.getRawSampleData().getElems(), 0, targetTile.getRawSampleData().getElems(), 0, length);
    }

    @Override
    public void dispose() {
        sourceBands.clear();
    }

    public class BandDesc {
        String product;
        String name;
        String nameExp;
        String newName;
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ProductMergeOp.class, "ProductMerger");
        }
    }
}