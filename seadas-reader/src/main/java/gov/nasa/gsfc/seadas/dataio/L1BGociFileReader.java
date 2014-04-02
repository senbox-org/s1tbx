package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.dataio.netcdf.GenericNetCdfReader;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.util.DebugFlagsImpl;

import java.io.IOException;

public class L1BGociFileReader extends GenericNetCdfReader {

    public L1BGociFileReader(L1BGociProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        H5iosp.setDebugFlags(new DebugFlagsImpl("HdfEos/turnOff"));
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        Product product = super.readProductNodesImpl();
        addGeocoding(product);
        return product;
    }

    public void addGeocoding(Product product) {

        //This gets VERY close, based on one scene's coastline matching.  Not quite as good as the l2gen output,
        // but can't figure out where the discrepancy lies...so going with this as good enough.

        double pixelX = 0.0;
        double pixelY = 0.0;

        String westName = "HDFEOS_POINTS_Scene_Header_Scene_upper-left_longitude";
        String northName = "HDFEOS_POINTS_Scene_Header_Scene_upper-left_latitude";
        String centralLatName = "HDFEOS_POINTS_Map_Projection_Central_Latitude_(parallel)";
        String centralLonName = "HDFEOS_POINTS_Map_Projection_Central_Longitude_(meridian)";
        String sceneCenterLatName = "HDFEOS_POINTS_Scene_Header_Scene_center_latitude";
        String sceneCenterLonName = "HDFEOS_POINTS_Scene_Header_Scene_center_longitude";

        final MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
        float projCentralLon = (float) globalAttributes.getAttribute(centralLonName).getData().getElemDouble();
        float projCentralLat = (float) globalAttributes.getAttribute(centralLatName).getData().getElemDouble();
        float sceneCenterLon = (float) globalAttributes.getAttribute(sceneCenterLonName).getData().getElemDouble();
        float sceneCenterLat = (float) globalAttributes.getAttribute(sceneCenterLatName).getData().getElemDouble();
        float east = (float) globalAttributes.getAttribute(westName).getData().getElemDouble();
        float north = (float) globalAttributes.getAttribute(northName).getData().getElemDouble();

        // The projection information contained in the GOCI data file suggests slightly odd
        // Earth sphericity values, so not quite WGS84.  Using these, the transform needs the "lenient" keyword
        // set to true
        // the WKT used was derived from gdal/proj4 using the projection info (and ESRI format):
        // gdalsrsinfo -o wkt_esri -p "+proj=ortho +lon_0=130 +lat_0=36 +a=6378169.0 +b=6356584.0"
        //
        //        PROJCS["Orthographic",
        //                GEOGCS["GCS_unnamed ellipse",
        //                DATUM["D_unknown",
        //                SPHEROID["Unknown",6378169,295.4908037989359]],
        //        PRIMEM["Greenwich",0],
        //        UNIT["Degree",0.017453292519943295]],
        //        PROJECTION["Orthographic"],
        //                PARAMETER["Latitude_Of_Center",36],
        //        PARAMETER["Longitude_Of_Center",130],
        //        PARAMETER["false_easting",0],
        //        PARAMETER["false_northing",0],
        //        UNIT["Meter",1]]

        // OK, using WGS84...seems to not make a hill 'o beans difference - but stickin' with the ugly.
        String wkt =
            "PROJCS[\"Orthographic\","
                + "  GEOGCS[\"GCS_unnamed ellipse\","
                + "    DATUM["+"\"D_unknown\","
                + "      SPHEROID[\"Unknown\",6378169,295.4908037989359]],"
                + "    PRIMEM[\"Greenwich\", 0.0],"
                + "    UNIT[\"Degree\", 0.017453292519943295]],"
                + "  PROJECTION[\"Orthographic\"],"
                + "  PARAMETER[\"Latitude_Of_Center\","+projCentralLat +"],"
                + "  PARAMETER[\"Longitude_Of_Center\","+ projCentralLon +"],"
                + "  PARAMETER[\"false_easting\", 0.0],"
                + "  PARAMETER[\"false_northing\", 0.0],"
                + "  UNIT[\"Meter\", 1]]";

        try {
            CoordinateReferenceSystem orthoCRS = CRS.parseWKT(wkt);
            CoordinateReferenceSystem wgs84crs = org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
            MathTransform transform = CRS.findMathTransform(wgs84crs, orthoCRS,true);
            double[] sceneCenter = new double[2];
            double [] sceneTopRight = new double[2];
//            transform.transform(new double[] {projCentralLon,projCentralLat}, 0, sceneCenter, 0, 1);
            transform.transform(new double[] {sceneCenterLon,sceneCenterLat}, 0, sceneCenter, 0, 1);
            transform.transform(new double[] {east,north}, 0, sceneTopRight, 0, 1);
            double northing = Math.round(sceneTopRight[1]);
            double easting = Math.round(sceneTopRight[0]);

            double pixelSize = Math.round((sceneCenter[0]-sceneTopRight[0])/(product.getSceneRasterWidth()/2.0));

            product.setGeoCoding(new CrsGeoCoding(orthoCRS,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    easting, northing,pixelSize,pixelSize,
                    pixelX, pixelY));
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        } catch (TransformException e) {
            throw new IllegalStateException(e);
        }
    }
}