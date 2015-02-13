/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import com.bc.ceres.binding.ConversionException;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.ProductCustomizer;
import org.esa.beam.binning.Reprojector;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.support.BinningContextImpl;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ProductTemporalBinRendererTest {

    @Test
    public void testRenderBin() throws Exception {
        File tempFile = File.createTempFile("BEAM", ".nc");
        tempFile.deleteOnExit();
        BinningContextImpl binningContext = new BinningContextImpl(new SEAGrid(10), new BinManager(),
                                                                   CompositingType.BINNING, 1, null, null);
        ProductTemporalBinRenderer binRenderer = createBinRenderer(tempFile, binningContext, null);
        Rectangle region = binRenderer.getRasterRegion();
        Product product = renderBins(tempFile, binRenderer, region);

        Band numObs = product.getBand("num_obs");
        numObs.loadRasterData();
        int[] actualObsLine = new int[region.width];
        int[] expectedObsLine = new int[region.width];
        for (int y = 0; y < region.height; y++) {
            numObs.readPixels(0, y, region.width, 1, actualObsLine);
            Arrays.fill(expectedObsLine, y);
            if (y == 3) {
                expectedObsLine[0] = -1;
            }
            assertArrayEquals(expectedObsLine, actualObsLine);
        }

        Band numPasses = product.getBand("num_passes");
        numPasses.loadRasterData();
        int[] actualPassesLine = new int[region.width];
        int[] expectedPassesLine = new int[region.width];
        for (int y = 0; y < region.height; y++) {
            numPasses.readPixels(0, y, region.width, 1, actualPassesLine);
            Arrays.fill(expectedPassesLine, y+1);
            if (y == 3) {
                expectedPassesLine[0] = -1;
            }
            assertArrayEquals("row=" + y, expectedPassesLine, actualPassesLine);
        }
    }

    @Test
    public void testRenderBinWithCustomizer() throws Exception {
        File tempFile = File.createTempFile("BEAM", ".nc");
        tempFile.deleteOnExit();
        BinningContextImpl binningContext = new BinningContextImpl(new SEAGrid(10), new BinManager(),
                                                                   CompositingType.BINNING, 1, null, null);
        ProductTemporalBinRenderer binRenderer = createBinRenderer(tempFile, binningContext, new MyProductCustomizer());
        Rectangle region = binRenderer.getRasterRegion();
        Product product = renderBins(tempFile, binRenderer, region);

        Band numObs = product.getBand("num_obs");
        numObs.loadRasterData();
        int[] actualObsLine = new int[region.width];
        int[] expectedObsLine = new int[region.width];
        for (int y = 0; y < region.height; y++) {
            numObs.readPixels(0, y, region.width, 1, actualObsLine);
            Arrays.fill(expectedObsLine, y);
            if (y == 3) {
                expectedObsLine[0] = -1;
            }
            assertArrayEquals(expectedObsLine, actualObsLine);
        }

        Band numPasses = product.getBand("num_passes");
        assertNull(numPasses);

        Band const3 = product.getBand("const3");
        assertNotNull(const3);
        const3.loadRasterData();
        int[] actualConst3Line = new int[region.width];
        int[] expectedConst3Line = new int[region.width];
        Arrays.fill(expectedConst3Line, 3);
        for (int y = 0; y < region.height; y++) {
            const3.readPixels(0, y, region.width, 1, actualConst3Line);
            assertArrayEquals(expectedConst3Line, actualConst3Line);
        }
    }

    private Product renderBins(File tempFile, ProductTemporalBinRenderer binRenderer, Rectangle region) throws IOException {
        binRenderer.begin();
        TemporalBin temporalBin = new TemporalBin(0, 11);
        for (int y = 0; y < region.height; y++) {
            temporalBin.setNumObs(y);
            temporalBin.setNumPasses(y + 1);
            for (int x = 0; x < region.width; x++) {
                if (x == 0 && y == 3) {
                    binRenderer.renderMissingBin(x, y);
                } else {
                    binRenderer.renderBin(x, y, temporalBin, null);
                }
            }
        }
        binRenderer.end();
        return ProductIO.readProduct(tempFile);
    }


    private ProductTemporalBinRenderer createBinRenderer(File tempFile,
                                                         BinningContextImpl binningContext,
                                                         ProductCustomizer productCustomizer) throws IOException, ConversionException, ParseException {
        String worldWKT = "POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))";
        Rectangle region = Reprojector.computeRasterSubRegion(binningContext.getPlanetaryGrid(),
                                                              new JtsGeometryConverter().parse(worldWKT));
        ProductData.UTC startTime = ProductData.UTC.parse("12-May-2006 11:50:10");
        ProductData.UTC endTime = ProductData.UTC.parse("12-May-2006 11:55:15");
        String[] resultFeatureNames = binningContext.getBinManager().getResultFeatureNames();
        return new ProductTemporalBinRenderer(resultFeatureNames, tempFile, "NetCDF-BEAM", region, 1.0, startTime, endTime, productCustomizer);
    }

    private class MyProductCustomizer extends ProductCustomizer {
        @Override
        public void customizeProduct(Product product) {
            product.removeBand(product.getBand("num_passes"));

            Band const3 = product.addBand("const3", ProductData.TYPE_INT32);
            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();
            const3.setSourceImage(ConstantDescriptor.create((float) width, (float) height, new Integer[]{3}, null));
        }
    }
}
