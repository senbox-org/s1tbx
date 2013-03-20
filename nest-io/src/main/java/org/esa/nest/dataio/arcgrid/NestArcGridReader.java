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
package org.esa.nest.dataio.arcgrid;

import org.esa.beam.dataio.arcbin.ArcBinGridReader;
import org.esa.beam.dataio.arcbin.ArcBinGridReaderPlugIn;
import org.esa.beam.dataio.arcbin.GeorefBounds;
import org.esa.beam.dataio.arcbin.Header;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.eo.GeoUtils;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

public class NestArcGridReader extends ArcBinGridReader {

    public NestArcGridReader(ArcBinGridReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected void initMetadata(final Product product, final File inputFile) throws IOException {

        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, product.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, product.getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, product.getSceneRasterHeight());
        
        if(!AbstractMetadata.loadExternalMetadata(product, absRoot, inputFile))
            AbstractMetadata.loadExternalMetadata(product, absRoot, new File(inputFile.getParentFile(), "PolSARPro_NEST_metadata.xml"));

        // set name from metadata if found
        product.setName(absRoot.getAttributeString(AbstractMetadata.PRODUCT, product.getName()));
    }
}