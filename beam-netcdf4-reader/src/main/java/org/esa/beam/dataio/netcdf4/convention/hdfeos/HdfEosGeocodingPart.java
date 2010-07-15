package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.Model;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Element;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.List;


public class HdfEosGeocodingPart implements ModelPart {

    private static final double PIXEL_CENTER = 0.5;

    @Override
    public void read(Product p, Model model) throws IOException {
        NetcdfFile netcdfFile = model.getReaderParameters().getNetcdfFile();
        Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, netcdfFile.getRootGroup());
        Element gridStructure = eosElement.getChild("GridStructure");
        Element gridElem = (Element) gridStructure.getChildren().get(0);
        Element projectionElem = gridElem.getChild("Projection");
        Element ulPointElem = gridElem.getChild("UpperLeftPointMtrs");
        Element lrPointElem = gridElem.getChild("LowerRightMtrs");
        if (projectionElem == null || ulPointElem == null || lrPointElem == null) {
            return;
        }
        String projection = projectionElem.getValue();
        if (!projection.equals("GCTP_GEO")) {
            return;
        }
        List<Element> ulList = ulPointElem.getChildren();
        String ulLon = ulList.get(0).getValue();
        String ulLat = ulList.get(1).getValue();
        double upperLeftLon = Double.parseDouble(ulLon);
        if (upperLeftLon < -180 || upperLeftLon > 180) {
            return;
        }
        double upperLeftLat = Double.parseDouble(ulLat);
        if (upperLeftLat < -90 || upperLeftLat > 90) {
            return;
        }

        List<Element> lrList = lrPointElem.getChildren();
        String lrLon = lrList.get(0).getValue();
        String lrLat = lrList.get(1).getValue();
        double lowerRightLon = Double.parseDouble(lrLon);
        if (lowerRightLon < -180 || lowerRightLon > 180) {
            return;
        }
        double lowerRightLat = Double.parseDouble(lrLat);
        if (lowerRightLat < -90 || lowerRightLat > 90) {
            return;
        }

        double pixelSizeX = (lowerRightLon - upperLeftLon) / p.getSceneRasterWidth();
        double pixelSizeY = (upperLeftLat - lowerRightLat) / p.getSceneRasterHeight();

        AffineTransform transform = new AffineTransform();
        transform.translate(upperLeftLon, upperLeftLat);
        transform.scale(pixelSizeX, -pixelSizeY);
        transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);
        Rectangle imageBounds = new Rectangle(p.getSceneRasterWidth(), p.getSceneRasterHeight());
        try {
            p.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, transform));
        } catch (FactoryException ignore) {
        } catch (TransformException ignore) {
        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw, Model model) throws IOException {
        throw new IllegalStateException();
    }
}
