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

package org.esa.beam.gpf.operators.standard;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.ui.SourceUI;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

/**
 * Reads the specified file as product. This operator may serve as a source node in processing graphs,
 * especially if multiple data products need to be read in.
 * <p/>
 * Here is a sample of how the <code>Read</code> operator can be integrated as a node within a processing graph:
 * <pre>
 *    &lt;node id="readNode"&gt;
 *        &lt;operator&gt;Read&lt;/operator&gt;
 *        &lt;parameters&gt;
 *            &lt;file&gt;/eodata/SST.nc&lt;/file&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "Read",
                  version = "1.1",
                  authors = "Marco Zuehlke, Norman Fomferra",
                  copyright = "(c) 2010 by Brockmann Consult",
                  description = "Reads a product from disk.")
public class ReadOp extends Operator {

    private ProductReader beamReader;

    @Parameter(description = "The file from which the data product is read.", notNull = true, notEmpty = true)
    private File file;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        try {
            targetProduct = ProductIO.readProduct(file);
            if (targetProduct == null) {
                throw new OperatorException("No product reader found for file " + file);
            }
            targetProduct.setFileLocation(file);
            beamReader = targetProduct.getProductReader();

            updateMetadata();
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private void updateMetadata() {
        final MetadataElement root = targetProduct.getMetadataRoot();
        if (root == null) {
            return;
        }
        MetadataElement abstractedMetadata = root.getElement("Abstracted_Metadata");
        if(abstractedMetadata == null) {
            return;
        }
        MetadataElement productElem =  abstractedMetadata.getElement("Product_Information");
        if(productElem == null) {
            productElem = new MetadataElement("Product_Information");
            abstractedMetadata.addElement(productElem);
        }
        MetadataElement inputElem =  productElem.getElement("InputProducts");
        if(inputElem == null) {
            inputElem = new MetadataElement("InputProducts");
            productElem.addElement(inputElem);
        }

        MetadataAttribute[] inputProductAttrbList = inputElem.getAttributes();
        boolean found = false;
        for(MetadataAttribute attrib : inputProductAttrbList) {
            if(attrib.getData().getElemString().equals(targetProduct.getName()))
                found = true;
        }
        if(!found) {
            final MetadataAttribute inputAttrb = addAttribute(inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
            inputAttrb.getData().setElems(targetProduct.getName());
        }
    }

    public static MetadataAttribute addAttribute(final MetadataElement dest, final String tag, final int dataType,
                                                 final String unit, final String desc) {
        final MetadataAttribute attribute = new MetadataAttribute(tag, dataType, 1);
        attribute.setUnit(unit);
        attribute.setDescription(desc);
        attribute.setReadOnly(true);
        dest.addAttribute(attribute);
        return attribute;
    }

        @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        ProductData dataBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            beamReader.readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width,
                                          rectangle.height, dataBuffer, pm);
            targetTile.setRawSamples(dataBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ReadOp.class);
            setOperatorUI(SourceUI.class);
        }
    }
}
