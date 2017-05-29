package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * @author Marco Peters
 */
public class EnviHeaderTest {

    @Test
    public void writeMapProjectionInfo() throws Exception {
        Product product = createMultiSizeProduct();
        Band[] bands = product.getBands();
        for (Band band : bands) {
            String infoText = getProjectionInfoString(band);
            System.out.println(infoText);
        }

    }

    private String getProjectionInfoString(RasterDataNode raster) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        EnviHeader.writeMapProjectionInfo(writer, raster);
        writer.flush();
        return baos.toString();
    }

    private Product createMultiSizeProduct() throws FactoryException, TransformException {
        Product product = new Product("test-multiSize", "TMS");
        Band band60m = new Band("60m", ProductData.TYPE_INT8, 10, 20);
        Band band20m = new Band("20m", ProductData.TYPE_INT8, 30, 60);
        Band band10m = new Band("10m", ProductData.TYPE_INT8, 60, 120);
        product.addBand(band60m);
        product.addBand(band20m);
        product.addBand(band10m);
        CoordinateReferenceSystem utm36s = CRS.decode("EPSG:32736");
        band60m.setGeoCoding(new CrsGeoCoding(utm36s, 10, 20, 500000.0, 10000000.0, 60, 60, 0,0));
        band20m.setGeoCoding(new CrsGeoCoding(utm36s, 30, 60, 500000.0, 10000000.0, 20, 20, 0,0));
        band10m.setGeoCoding(new CrsGeoCoding(utm36s, 60, 120, 500000.0, 10000000.0, 10, 10, 0,0));


        return product;
    }

}