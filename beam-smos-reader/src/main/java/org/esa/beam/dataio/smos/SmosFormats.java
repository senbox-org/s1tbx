package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.binx.BinX;
import com.bc.ceres.binio.binx.BinXException;
import static com.bc.ceres.binio.util.TypeBuilder.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Defines the formats of all supported SOMS product types.
 */
public class SmosFormats {
    public static final CompoundType SNAPSHOT_INFO_TYPE =
            COMPOUND("Snapshot_Information",
                 MEMBER("Snapshot_Time", SEQUENCE(UINT, 3)),
                 MEMBER("Snapshot_ID", UINT),
                 MEMBER("Snapshot_OBET", SEQUENCE(UBYTE, 8)),
                 MEMBER("Position", SEQUENCE(DOUBLE, 3)),
                 MEMBER("Velocity", SEQUENCE(DOUBLE, 3)),
                 MEMBER("Vector_Source", UBYTE),
                 MEMBER("Q0", DOUBLE),
                 MEMBER("Q1", DOUBLE),
                 MEMBER("Q2", DOUBLE),
                 MEMBER("Q3", DOUBLE),
                 MEMBER("TEC", DOUBLE),
                 MEMBER("Geomag_F", DOUBLE),
                 MEMBER("Geomag_D", DOUBLE),
                 MEMBER("Geomag_I", DOUBLE),
                 MEMBER("Sun_RA", FLOAT),
                 MEMBER("Sun_DEC", FLOAT),
                 MEMBER("Sun_BT", FLOAT),
                 MEMBER("Accuracy", FLOAT),
                 MEMBER("Radiometric_Accuracy", SEQUENCE(FLOAT, 2)));

    public static final CompoundType D1C_BT_DATA_TYPE =
            COMPOUND("Bt_Data",
                 MEMBER("Flags", USHORT),
                 MEMBER("BT_Value", FLOAT),
                 MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                 MEMBER("Incidence_Angle", USHORT),
                 MEMBER("Azimuth_Angle", USHORT),
                 MEMBER("Faraday_Rotation_Angle", USHORT),
                 MEMBER("Geometric_Rotation_Angle", USHORT),
                 MEMBER("Snapshot_ID_of_Pixel", UINT),
                 MEMBER("Footprint_Axis1", USHORT),
                 MEMBER("Footprint_Axis2", USHORT));

    public static final CompoundType F1C_BT_DATA_TYPE =
            COMPOUND("Bt_Data",
                 MEMBER("Flags", USHORT),
                 MEMBER("BT_Value_Real", FLOAT),
                 MEMBER("BT_Value_Imag", FLOAT),
                 MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                 MEMBER("Incidence_Angle", USHORT),
                 MEMBER("Azimuth_Angle", USHORT),
                 MEMBER("Faraday_Rotation_Angle", USHORT),
                 MEMBER("Geometric_Rotation_Angle", USHORT),
                 MEMBER("Snapshot_ID_of_Pixel", UINT),
                 MEMBER("Footprint_Axis1", USHORT),
                 MEMBER("Footprint_Axis2", USHORT));

    public static final CompoundType BROWSE_BT_DATA_TYPE =
            COMPOUND("Bt_Data",
                 MEMBER("Flags", USHORT),
                 MEMBER("BT_Value", FLOAT),
                 MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                 MEMBER("Azimuth_Angle", USHORT),
                 MEMBER("Footprint_Axis1", USHORT),
                 MEMBER("Footprint_Axis2", USHORT));

    public static final CompoundType D1C_GRID_POINT_DATA_TYPE =
            COMPOUND("Grid_Point_Data",
                 MEMBER("Grid_Point_ID", UINT), /*4*/
                 MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                 MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                 MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                 MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                 MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                 MEMBER("BT_Data_List", VAR_SEQUENCE(D1C_BT_DATA_TYPE, "BT_Data_Counter")));

    public static final CompoundType F1C_GRID_POINT_DATA_TYPE =
            COMPOUND("Grid_Point_Data",
                 MEMBER("Grid_Point_ID", UINT), /*4*/
                 MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                 MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                 MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                 MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                 MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                 MEMBER("BT_Data_List", VAR_SEQUENCE(F1C_BT_DATA_TYPE, "BT_Data_Counter")));

    public static final CompoundType BROWSE_GRID_POINT_DATA_TYPE =
            COMPOUND("Grid_Point_Data",
                 MEMBER("Grid_Point_ID", UINT), /*4*/
                 MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                 MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                 MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                 MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                 MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                 MEMBER("BT_Data_List", VAR_SEQUENCE(BROWSE_BT_DATA_TYPE, "BT_Data_Counter")));

    public static final CompoundType MIR_SCXD1C_TYPE =
            COMPOUND("MIR_SCXD1C",
                 MEMBER("Snapshot_Counter", UINT),
                 MEMBER("Snapshot_List", VAR_SEQUENCE(SNAPSHOT_INFO_TYPE, "Snapshot_Counter")),
                 MEMBER("Grid_Point_Counter", UINT),
                 MEMBER("Grid_Point_List", VAR_SEQUENCE(D1C_GRID_POINT_DATA_TYPE, "Grid_Point_Counter")));

    public static final CompoundType MIR_SCXF1C_TYPE =
            COMPOUND("MIR_SCXF1C",
                 MEMBER("Snapshot_Counter", UINT),
                 MEMBER("Snapshot_List", VAR_SEQUENCE(SNAPSHOT_INFO_TYPE, "Snapshot_Counter")),
                 MEMBER("Grid_Point_Counter", UINT),
                 MEMBER("Grid_Point_List", VAR_SEQUENCE(F1C_GRID_POINT_DATA_TYPE, "Grid_Point_Counter")));

    public static final CompoundType MIR_BROWSE_TYPE =
            COMPOUND("Temp_Browse",
                 MEMBER("Grid_Point_Counter", UINT),
                 MEMBER("Grid_Point_List", VAR_SEQUENCE(BROWSE_GRID_POINT_DATA_TYPE, "Grid_Point_Counter")));


    private static final SmosFormats INSTANCE = new SmosFormats();
    private final ConcurrentMap<String, DataFormat> formatMap;

    private SmosFormats() {
        formatMap = new ConcurrentHashMap<String, DataFormat>(17);
//        registerSmosFormat("MIR_BWLD1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
//        registerSmosFormat("MIR_BWLF1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
//        registerSmosFormat("MIR_BWSD1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
//        registerSmosFormat("MIR_BWSF1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
//
//        registerSmosFormat("MIR_SCLD1C", MIR_SCXD1C_TYPE, D1C_GRID_POINT_DATA_TYPE, false);
//        registerSmosFormat("MIR_SCLF1C", MIR_SCXF1C_TYPE, F1C_GRID_POINT_DATA_TYPE, false);
//        registerSmosFormat("MIR_SCSD1C", MIR_SCXD1C_TYPE, D1C_GRID_POINT_DATA_TYPE, false);
//        registerSmosFormat("MIR_SCSF1C", MIR_SCXF1C_TYPE, F1C_GRID_POINT_DATA_TYPE, false);
    }

    public static SmosFormats getInstance() {
        return INSTANCE;
    }

    public String[] getFormatNames() {
        final Set<String> names = formatMap.keySet();
        return names.toArray(new String[names.size()]);
    }

    public DataFormat getFormat(String name) {
        if (!formatMap.containsKey(name)) {
            final URL schemaUrl = getSchemaResource(name);

            try {
                final DataFormat format = new BinX(schemaUrl.toURI()).getFormat(name);
                formatMap.putIfAbsent(name, format);
            } catch (BinXException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Cannot convert URL '" + schemaUrl + "' into an URI");
            }
        }

        return formatMap.get(name);
    }

//    private void registerSmosFormat(String formatName, CompoundType type, CompoundType gridPointType, boolean browse) {
//        final Format smosFormat = new Format(type);
//        smosFormat.setName(formatName);
//        smosFormat.setByteOrder(ByteOrder.LITTLE_ENDIAN);
//        if (!browse) {
//            smosFormat.addSequenceElementCountResolver("Snapshot_List", "Snapshot_Counter");
//        }
//        smosFormat.addSequenceElementCountResolver("Grid_Point_List", "Grid_Point_Counter");
//        smosFormat.addSequenceElementCountResolver(gridPointType,
//                                                   "BT_Data_List", "BT_Data_Counter");
//        formatMap.put(formatName, smosFormat);
//    }

    static URL getSchemaResource(String name) {
        final StringBuilder sb = new StringBuilder("/smos-schemas/");
        final String fc = name.substring(12, 16);
        final String sd = name.substring(16, 22);

        return SmosFormats.class.getResource(sb.append(fc).append("/").append(sd).append("/").append(name).toString());
    }
}
