/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.io.stac;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.stac.StacItem;
import org.esa.s1tbx.stac.extensions.EO;
import org.esa.s1tbx.stac.extensions.Sat;
import org.esa.s1tbx.stac.extensions.View;
import org.esa.s1tbx.stac.support.JSONSupport;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class STACProductWriter extends AbstractProductWriter {

    private static final GeoTiffProductWriterPlugIn geoTiffProductWriterPlugIn = new GeoTiffProductWriterPlugIn();

    private final Map<Band, ProductWriter> bandWriterMap = new HashMap<>();
    private final Map<Band, Band> bandMap = new HashMap<>();
    private final boolean singleBand;

    public STACProductWriter(final ProductWriterPlugIn writerPlugIn) {
        this(writerPlugIn, false);
    }

    public STACProductWriter(final ProductWriterPlugIn writerPlugIn, final boolean singleBand) {
        super(writerPlugIn);
        this.singleBand = singleBand;
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {

        final Product srcProduct = getSourceProduct();

        File imageFile;
        if (getOutput() instanceof String) {
            imageFile = new File((String) getOutput());
        } else {
            imageFile = (File) getOutput();
        }
        imageFile.getParentFile().mkdirs();

        writeProductMetadata(imageFile, srcProduct);

        String baseName = imageFile.getName();
        if (baseName.endsWith(STACProductConstants.IMAGE_GEOTIFF_EXT)) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        if (singleBand) {
            for (Band srcBand : srcProduct.getBands()) {
                ProductWriter bandWriter = geoTiffProductWriterPlugIn.createWriterInstance();
                imageFile = new File(imageFile.getParentFile(), baseName + "_" + srcBand.getName() + STACProductConstants.IMAGE_GEOTIFF_EXT);

                Product trgProduct = new Product(srcProduct.getName(), srcProduct.getProductType(), srcProduct.getSceneRasterWidth(), srcProduct.getSceneRasterHeight());
                ProductUtils.copyMetadata(srcProduct, trgProduct);
                ProductUtils.copyTiePointGrids(srcProduct, trgProduct);
                ProductUtils.copyFlagCodings(srcProduct, trgProduct);
                //ProductUtils.copyFlagBands(srcProduct, trgProduct, true);
                ProductUtils.copyGeoCoding(srcProduct, trgProduct);
                ProductUtils.copyMasks(srcProduct, trgProduct);
                ProductUtils.copyVectorData(srcProduct, trgProduct);
                ProductUtils.copyIndexCodings(srcProduct, trgProduct);
                ProductUtils.copyQuicklookBandName(srcProduct, trgProduct);
                trgProduct.setStartTime(srcProduct.getStartTime());
                trgProduct.setEndTime(srcProduct.getEndTime());
                trgProduct.setDescription(srcProduct.getDescription());
                trgProduct.setAutoGrouping(srcProduct.getAutoGrouping());

                Band trgBand = ProductUtils.copyBand(srcBand.getName(), srcProduct, trgProduct, true);

                bandWriter.writeProductNodes(trgProduct, imageFile);

                bandWriterMap.put(srcBand, bandWriter);
                bandMap.put(srcBand, trgBand);
            }
        } else {
            ProductWriter bandWriter = geoTiffProductWriterPlugIn.createWriterInstance();
            bandWriter.writeProductNodes(srcProduct, imageFile);

            for (Band srcBand : srcProduct.getBands()) {
                bandWriterMap.put(srcBand, bandWriter);
                bandMap.put(srcBand, srcBand);
            }
        }
    }

    private void writeProductMetadata(final File imageFile, final Product product) throws IOException {

        final File metadataFile = FileUtils.exchangeExtension(imageFile, STACProductConstants.METADATA_EXT);
        final FileWriter metaStringWriter = new FileWriter(metadataFile);

        final JSONObject json = new JSONObject();

        final StacItem stacItem = new StacItem(json, product.getName());
        stacItem.addExtension(EO.eo, View.view, Sat.sat);
        stacItem.addKeywords(EO.KeyWords.earth_observation, EO.KeyWords.satellite);
        json.put(StacItem.description, product.getDescription());

        metaStringWriter.write(JSONSupport.prettyPrint(json));
        metaStringWriter.close();
    }

    @Override
    public void writeBandRasterData(final Band sourceBand,
                                    final int sourceOffsetX,
                                    final int sourceOffsetY,
                                    final int sourceWidth,
                                    final int sourceHeight,
                                    final ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        if (sourceBand instanceof VirtualBand) {

        } else {
            ProductWriter bandWriter = bandWriterMap.get(sourceBand);
            bandWriter.writeBandRasterData(bandMap.get(sourceBand),
                    sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceBuffer, pm);
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && !(node instanceof FilterBand);
    }

    @Override
    public void flush() throws IOException {
        for (ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.close();
        }
    }

    @Override
    public void deleteOutput() throws IOException {
        for (ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.deleteOutput();
        }
    }
}
