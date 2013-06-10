package org.esa.beam.binning.operator;

import com.bc.ceres.binding.ConversionException;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.CompositingType;
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
                                                                   CompositingType.BINNING, 1);
        ProductTemporalBinRenderer binRenderer = createBinRenderer(tempFile, binningContext);
        Rectangle region = binRenderer.getRasterRegion();

        binRenderer.begin(binningContext);
        TemporalBin temporalBin = new TemporalBin(0, 11);
        for (int y = 0; y < region.height; y++) {
            temporalBin.setNumObs(y);
            for (int x = 0; x < region.width; x++) {
                if (x == 0 && y == 3) {
                    binRenderer.renderMissingBin(x, y);
                } else {
                    binRenderer.renderBin(x, y, temporalBin, null);
                }
            }
        }
        binRenderer.end(binningContext);

        Product product = ProductIO.readProduct(tempFile);
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
    }


    private ProductTemporalBinRenderer createBinRenderer(File tempFile, BinningContextImpl binningContext) throws
                                                                                                           IOException,
                                                                                                           ConversionException,
                                                                                                           ParseException {
        String worldWKT = "POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))";
        Rectangle region = Reprojector.computeRasterSubRegion(binningContext.getPlanetaryGrid(),
                                                              new JtsGeometryConverter().parse(worldWKT));
        ProductData.UTC startTime = ProductData.UTC.parse("12-May-2006 11:50:10");
        ProductData.UTC endTime = ProductData.UTC.parse("12-May-2006 11:55:15");
        return new ProductTemporalBinRenderer(binningContext, tempFile, "NetCDF-BEAM", region, 1.0, startTime, endTime, null);
    }

}
