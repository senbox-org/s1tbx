package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Element;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.*;
import java.util.List;


public class HdfEosGeocodingPart implements ModelPart {
    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        NetcdfFile netcdfFile = rp.getNetcdfFile();
        Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, netcdfFile.getRootGroup());
        Element gridStructure = eosElement.getChild("GridStructure");
        Element gridElem = (Element) gridStructure.getChildren().get(0);
        Element projectionElem = gridElem.getChild("Projection");
        String projection = projectionElem.getValue();
        Element ulPointElem = gridElem.getChild("UpperLeftPointMtrs");
        System.out.println("ulPointElem = " + ulPointElem);
        List list = ulPointElem.getAttributes();
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            System.out.println("o = " + o);
        }
        Element lrPointElem = gridElem.getChild("LowerRightMtrs");
        System.out.println("lrPointElem = " + lrPointElem);


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
