package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.NetcdfFileWriteable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;


public class HdfEosGeocodingPart implements ModelPart {
    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
//        AffineTransform transform = new AffineTransform();
//        transform.translate(upperLeftLon, upperLeftLat);
//        transform.scale(pixelSize, -pixelSize);
//        transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);
//        Rectangle rect = new Rectangle(p.getSceneRasterWidth(), p.getSceneRasterHeight());
//        CrsGeoCoding geoCoding = null;
//        try {
//            geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, rect, transform);
//            p.setGeoCoding(geoCoding);
//        } catch (FactoryException ignore) {
//        } catch (TransformException ignore) {
//        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        throw new IllegalStateException();
    }
}
